package ru.citeck.ecos.records2.spring.web.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.citeck.ecos.records2.QueryContext;
import ru.citeck.ecos.records2.RecordsServiceFactory;
import ru.citeck.ecos.records2.request.rest.DeletionBody;
import ru.citeck.ecos.records2.request.rest.MutationBody;
import ru.citeck.ecos.records2.request.rest.QueryBody;
import ru.citeck.ecos.records2.request.rest.RestHandler;
import ru.citeck.ecos.records2.spring.utils.web.WebUtils;

import java.util.Locale;

@Api(
    description = "Service for universal querying an arbitrary data set (record) from any available data source",
    tags = "Records API"
)
@Slf4j
@RestController
@RequestMapping("/api/records")
public class RecordsRestApi {

    private RestHandler restHandler;
    private WebUtils webUtils;

    @Autowired
    public RecordsRestApi(RestHandler restHandler,
                          @Qualifier("jacksonWebUtils") WebUtils webUtils) {
        this.restHandler = restHandler;
        this.webUtils = webUtils;
    }

    @ApiOperation(
        value = "Query arbitrary set of attributes from arbitrary sources",
        notes = "Full description of query syntax "
            + "see [here](https://citeck.atlassian.net/wiki/spaces/knowledgebase/pages/369557505/11.+ECOS+Records) .\n"
            + "#### Существует четыре операции, которые можно проделывать над записями:\n"
            + "* Поиск записей\n"
            + "Методы: queryRecords\n"
            + "Для поиска записей всегда передается RecordsQuery, который содержит параметры поиска "
            + "(аналогично SearchParameters из Alfresco). "
            + "Помимо самого простого метода для поиска с одним параметром RecordsQuery так же есть варианты"
            + " с объединенным поиском и запросом метаданных. О вариантах запроса метаданных см. ниже.\n"
            + "* Получение метаданных записи (в данном контексте метаданные - любые данные о записи."
            + " Например - имя договора, статус или даже контент)\n"
            + "Методы: getAttributes, getAttribute, getMeta\n"
            + "\n"
            + "Существует три уровня абстрации для получения метаданных:\n"
            + "  DTO Class > Attributes > Schema\n"
            + "*DTO Class* - класс, который используется для генерации списка аттрибутов для формирования схемы"
            + " и запроса метаданных из DAO.\n"
            + "\n"
            + "После получения всех данных из DAO идет создание инстансов переданного DTO класса"
            + " и наполнение его данными с помощью библиотеки jackson;\n"
            + "Список аттрибутов формируется либо из названий полей,"
            + " либо можно добавить аннотацию MetaAtt для указания аттрибута вручную.\n"
            + "\n"
            + "*Attributes* - аттрибуты записи. Существует две нотации:"
            + " упрощенная (перед запросом преобразуется в полную)"
            + " и полная (дает полный контроль над загружаемыми данными)."
            + " Сервер отличает вид нотации по первому символу в аттрибуте. Для полной нотации - это \".\"\n"
            + "\n"
            + "### Упрощенная нотация:\n"
            + "\n"
            + "Просто аттрибут - 'cm:title' преобразуется в '.att(n:\"cm:title\"){disp}'\n"
            + "Аттрибут с типом - 'cm:title?json' преобразуется в '.att(n:\"cm:title\"){json}'\n"
            + "Метаданные аттрибута - '#cm:title?protected' преобразуется в '.edge(n:\"cm:title\"){protected}'\n"
            + "Варианты выбора -"
            + " '#cm:title?options' преобразуется в '.edge(n:\"cm:title\"){options{label:disp,value:str}}'\n"
            + "\n"
            + "Запрос вложенных полей -"
            + " 'icase:caseStatusAssoc{ .disp, .str, title: cm:title, name: cm:name, uuid: sys:node-uuid}'\n"
            + "\n"
            + "преобразуется в '.att(n:\"icase:caseStatusAssoc\"){_disp:disp,_str:str,title:att(n:\"cm:title\"){disp},"
            + "name:att(n:\"cm:name\"){disp},uuid:att(n:\"sys:node-uuid\"){disp}}'\n"
            + "\n"
            + "### Полная нотация:\n"
            + "\n"
            + "Существует две основных сущности, с которыми идет работа в полной нотации:"
            + " MetaValue и MetaEdge (интерфейсы из ecos-records).\n"
            + "\n"
            + "Запись представлена в виде MetaValue, у которого можно запросить:\n"
            + "* Вложенные аттрибуты (тоже MetaValue) через 'att(n:\"Имя_аттрибута\"){...}'\n"
            + "* Метаданные аттрибута (MetaEdge) через 'edge(n:\"Имя_аттрибута\"){...}'\n"
            + "* Скаляр (финальное значение MetaValue, у которого уже нельзя получать вложенные поля) -"
            + " str, disp, bool и др.\n"
            + "\n"
            + "Для получения массива значений следует использовать окончание 's': atts(n:\"cm:title\"){str}"
            + " или edge(n:\"cm:title\"){vals{str}}\n"
            + "\n"
            + "Запрос аттрибутов может иметь неограниченную вложенность."
            + " Например для получения имени статуса кейса можно запросить следующий аттрибут:\n"
            + "'.att(n:\"icase:caseStatusAssoc\"){att(n:\"cm:title\"){str}}'\n"
            + "\n"
            + "#### ВАЖНО:"
            + "Значения аттрибутов перед тем как вернуться проходят процесс упрощения -"
            + " все json объекты с одним ключом будут развернуты. Например:\n"
            + "\n"
            + "* Запрос: '.att(n:\"icase:caseStatusAssoc\"){att(n:\"cm:title\"){str}}'\n"
            + "DAO по правилам GraphQL вернет следующий ответ: {\"att\":{\"att\":{\"str\":\"Новый\"}}}\n"
            + "Но сервис убирает лишнюю вложенность и мы получаем просто \"Новый\"\n"
            + "\n"
            + "* Запрос: '.att(n:\"icase:caseStatusAssoc\"){att(n:\"cm:title\"){str, id}}'\n"
            + "GraphQL: {\"att\":{\"att\":{\"str\":\"Новый\", \"id\":\"workspace://SpacesStore/satus-new\"}}}\n"
            + "Упрощение: {\"str\":\"Новый\", \"id\":\"workspace://SpacesStore/satus-new\"}\n"
            + "\n"
            + "В аттрибуте мы можем так же указывать псевдоним для возвращаемого значения. Например:\n"
            + "Запрос: '.att(n:\"icase:caseStatusAssoc\"){att(n:\"cm:title\"){statusName: str, statusId: id}}'\n"
            + "Вернет: {\"statusName\":\"Новый\", \"statusId\":\"workspace://SpacesStore/satus-new\"}\n"
            + "##\n"
            + "Для получения аттрибутов есть методы с аргументом Map и Collection.\n"
            + "Если передан Map, то ключи - это псевдонимы для возвращаемых значений."
            + " Они могут быть любыми и сервис их никак особым образом не обрабатывает."
            + " Значения - запрашиваемые аттрибуты.\n"
            + "Если передана Collection, то это аналогично поведению с Map где каждый ключ равен связанному значению.\n"
            + "##\n"
            + "*Schema* - самый низкоуровневый способ описания метаданных, которые мы хотим получить."
            + " Здесь мы передаем GraphQL схему и получаем ответ полностью в том виде, в котором мы его запросили."
            + " Данный метод предназначен скорее для системных нужд (например - удаленный вызов getMeta со схемой).\n"
            + "##\n"
            + "#### Query example:\n"
            + "```\n"
            + "{\n"
            + "    \"query\": {\n"
            + "        \"query\": \"TYPE:\\\"contracts:agreement\\\"\",\n"
            + "        \"language\": \"fts-alfresco\",\n"
            + "        \"page\": {\n"
            + "            \"maxItems\": 10,\n"
            + "            \"skipCount\": 0\n"
            + "        },\n"
            + "        \"sortBy\": [{\n"
            + "            \"attribute\":\"cm:modified\",\n"
            + "            \"ascending\": false\n"
            + "        }]\n"
            + "    },\n"
            + "    'attributes': ['cm:modified']\n"
            + "}\n"
            + "```"
    )
    @PostMapping("/query")
    public byte[] recordsQuery(@ApiParam(value = "query text") @RequestBody byte[] body) {

        QueryBody queryBody = webUtils.convertRequest(body, QueryBody.class);

        RecordsServiceFactory recordsServiceFactory = restHandler.getFactory();
        if (recordsServiceFactory == null) {
            return webUtils.encodeResponse(restHandler.queryRecords(queryBody));
        } else {
            return QueryContext.withContext(recordsServiceFactory, () -> {
                Locale currentLocale = LocaleContextHolder.getLocale();
                QueryContext.getCurrent().setLocale(currentLocale);
                return webUtils.encodeResponse(restHandler.queryRecords(queryBody));
            });
        }
    }

    @ApiOperation(
        value = "Change or Create records",
        notes = "\n"
            + "#### Query examples:\n"
            + "query:\n"
            + "```\n"
            + "{\n"
            + "  \"records\": [\n"
            + "    {\n"
            + "      \"id\": \"uiserv/action@test-action-only-id\",\n"
            + "      \"attributes\": {\n"
            + "        \"title\": \"Fire\",\n"
            + "        \"type\": \"fire\",\n"
            + "        \"icon\": \"fire.png\",\n"
            + "        \"config\": {\n"
            + "        \t\"color\": \"red\",\n"
            + "        \t\"size\": 23\n"
            + "        },\n"
            + "        \"evaluator\": {\n"
            + "        \t\"id\": \"has-delete-permission\",\n"
            + "        \t\"config\": {\n"
            + "        \t\t\"permission\": \"Delete\"\n"
            + "        \t}\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}\n"
            + "```\n"
            + "response:\n"
            + "```\n"
            + "{\n"
            + "  \"records\": [\n"
            + "    {\n"
            + "      \"id\": \"uiserv/action@test-action-only-id\",\n"
            + "      \"attributes\": {}\n"
            + "    }\n"
            + "  ],\n"
            + "  \"errors\": []\n"
            + "}\n"
            + "```\n"
    )
    @PostMapping("/mutate")
    public byte[] recordsMutate(@ApiParam(value = "change query text") @RequestBody byte[] body) {

        MutationBody mutationBody = webUtils.convertRequest(body, MutationBody.class);
        Object mutatedRecords = restHandler.mutateRecords(mutationBody);
        return webUtils.encodeResponse(mutatedRecords);
    }

    @ApiOperation(
        value = "Delete record",
        notes = "\n"
            + "#### Query examples:\n"
            + "query:\n"
            + "```\n"
            + "{\n"
            + "  \"records\": [\n"
            + "  \t\t\"action@test-action-only-id\"\n"
            + "  \t]\n"
            + "}\n"
            + "```\n"
            + "response:\n"
            + "```\n"
            + "{\n"
            + "  \"records\": [\n"
            + "    {\n"
            + "      \"id\": \"action@test-action-only-id\",\n"
            + "      \"attributes\": {}\n"
            + "    }\n"
            + "  ],\n"
            + "  \"errors\": []\n"
            + "}\n"
            + "```\n"
    )
    @PostMapping("/delete")
    public byte[] recordsDelete(@ApiParam(value = "query text") @RequestBody byte[] body) {

        DeletionBody deletionBody = webUtils.convertRequest(body, DeletionBody.class);
        Object deletedRecords = restHandler.deleteRecords(deletionBody);
        return webUtils.encodeResponse(deletedRecords);
    }
}

