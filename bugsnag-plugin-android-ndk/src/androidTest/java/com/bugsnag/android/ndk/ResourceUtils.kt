package com.bugsnag.android.ndk

internal fun loadJson(resourceName: String): String {
    val classLoader = requireNotNull(NativeJsonSerializeTest::class.java.classLoader)
    val resource = classLoader.getResource(resourceName)
        ?: throw IllegalArgumentException("Could not find $resourceName")
    val json = resource.readText()
    if (json.isEmpty()) {
        throw IllegalStateException("Failed to read JSON from $resourceName")
    }
    return json
}
