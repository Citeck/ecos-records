package ru.citeck.ecos.records3.exception

class LanguageNotSupportedException(val sourceId: String, val language: String) :
    RecordsException("Language '$language' is not supported by source: '$sourceId'")
