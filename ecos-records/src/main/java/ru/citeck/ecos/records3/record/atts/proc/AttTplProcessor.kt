package ru.citeck.ecos.records3.record.atts.proc

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.TmplUtils
import ru.citeck.ecos.records3.record.atts.schema.utils.AttStrUtils

/**
 * Processor to evaluate templates with attributes placeholders.
 *
 * Template may be defined as a literal:
 * ```
 * ?id|tpl('{{$webUrl}}v2/dashboard?recordRef={{_}}&ws={{_workspace?localId}}')
 * ```
 * or may be evaluated from an attribute. In this case attributes which should be
 * available in the template are declared by additional arguments:
 * ```
 * ?id|tpl(a:templateAtt, '_workspace?localId')
 * ```
 * Additional arguments may declare a short alias for the attribute
 * to keep placeholders in the template readable:
 * ```
 * ?id|tpl(a:templateAtt, 'ws:_workspace?localId|presuf("workspace-")')
 * ```
 *
 * Placeholder `{{_}}` (or `${_}`) will be replaced by the value which is processed by this processor.
 * Placeholders of attributes which are not declared will be replaced by `{attName}`
 * to make it clear that the template was applied, but the attribute is unknown.
 *
 * Values of the attributes are loaded with the default scalar type, so numbers and objects
 * will be converted to text. Use `?raw` or `?json` in placeholders (e.g. `{{someNum?raw}}`)
 * to keep the original type of the value.
 *
 * Template is not applied to null values. Empty string is a normal value,
 * so `{{_}}` will be replaced by it. Use `!` or `|or(...)` before this
 * processor if another behaviour is required.
 */
class AttTplProcessor : AttProcessor {

    companion object {
        const val TYPE = "tpl"

        const val SELF_ATT = "_"

        private const val ALIAS_DELIM = ":"
    }

    override fun process(attributes: ObjectData, value: DataValue, args: List<DataValue>): Any? {

        if (args.isEmpty() || value.isNull()) {
            return value
        }
        val parsedArgs = parseArgs(args)
        val template = if (parsedArgs.templateAtt != null) {
            attributes[parsedArgs.templateAtt]
        } else {
            parsedArgs.template
        }
        if (template.isNull()) {
            return DataValue.NULL
        }

        val data = ObjectData.create()
        parsedArgs.atts.forEach { (alias, att) ->
            data[alias] = attributes[att]
        }
        setUnknownAttsPlaceholders(template, parsedArgs.atts.keys, data)

        if (!value.isArray()) {
            data[SELF_ATT] = value
            return TmplUtils.applyAtts(template, data)
        }
        val result = DataValue.createArr()
        for (element in value) {
            if (element.isNull()) {
                result.add(element)
                continue
            }
            data[SELF_ATT] = element
            result.add(TmplUtils.applyAtts(template, data))
        }
        return result
    }

    override fun getAttsToLoad(arguments: List<DataValue>): Collection<String> {
        if (arguments.isEmpty()) {
            return emptySet()
        }
        val parsedArgs = parseArgs(arguments)
        val result = LinkedHashSet<String>()
        if (parsedArgs.templateAtt != null) {
            result.add(parsedArgs.templateAtt)
        }
        result.addAll(parsedArgs.atts.values)
        return result
    }

    private fun parseArgs(args: List<DataValue>): Args {

        val templateAtt = getTemplateAtt(args[0])

        // attributes from the literal template may be overridden by aliases from additional arguments
        val atts = LinkedHashMap<String, String>()
        if (templateAtt == null) {
            for (att in TmplUtils.getAtts(args[0])) {
                if (isValidAtt(att)) {
                    atts[att] = att
                }
            }
        }
        for (i in 1 until args.size) {
            val (alias, att) = parseAttArg(args[i].asText())
            if (isValidAtt(alias) && isValidAtt(att)) {
                atts[alias] = att
            }
        }
        return Args(args[0], templateAtt, atts)
    }

    /**
     * Parse argument like "alias:attribute" or "attribute".
     */
    private fun parseAttArg(arg: String): Pair<String, String> {
        val trimmedArg = arg.trim()
        val delimIdx = AttStrUtils.indexOf(trimmedArg, ALIAS_DELIM)
        if (delimIdx <= 0) {
            return trimmedArg to trimmedArg
        }
        val alias = trimmedArg.substring(0, delimIdx).trim()
        val att = trimmedArg.substring(delimIdx + ALIAS_DELIM.length).trim()
        if (alias.isEmpty() || att.isEmpty()) {
            return trimmedArg to trimmedArg
        }
        return alias to att
    }

    /**
     * Return attribute name with template or null if template is defined as a literal.
     */
    private fun getTemplateAtt(firstArg: DataValue): String? {
        if (!firstArg.isTextual()) {
            return null
        }
        val textArg = firstArg.asText()
        if (!textArg.startsWith(AttOrElseProcessor.ATT_PREFIX)) {
            return null
        }
        val att = textArg.substring(AttOrElseProcessor.ATT_PREFIX.length).trim()
        return att.ifBlank { null }
    }

    /**
     * Placeholders of attributes which are not declared should be replaced by "{attName}".
     * To achieve this we put this text as a value of the attribute.
     */
    private fun setUnknownAttsPlaceholders(template: DataValue, declaredAtts: Set<String>, data: ObjectData) {
        for (att in TmplUtils.getAtts(template)) {
            if (att != SELF_ATT && !declaredAtts.contains(att)) {
                data[att] = "{$att}"
            }
        }
    }

    private fun isValidAtt(att: String): Boolean {
        return att.isNotBlank() && att != SELF_ATT
    }

    override fun getType(): String = TYPE

    private class Args(
        val template: DataValue,
        val templateAtt: String?,
        val atts: Map<String, String>
    )
}
