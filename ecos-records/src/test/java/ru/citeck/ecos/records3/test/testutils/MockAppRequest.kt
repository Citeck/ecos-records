package ru.citeck.ecos.records3.test.testutils

import ru.citeck.ecos.commons.data.ObjectData

class MockAppRequest(
    val targetApp: String,
    val url: String,
    val bodyObj: ObjectData
)
