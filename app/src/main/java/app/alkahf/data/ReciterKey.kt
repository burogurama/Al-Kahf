package app.alkahf.data

/**
 * Reciter keys are either a built-in reciter's everyayah path or an imported
 * profile encoded as "custom:<id>". These helpers are the one place that
 * encoding is parsed.
 */
private const val CUSTOM_PREFIX = "custom:"

fun isCustomReciter(key: String): Boolean = key.startsWith(CUSTOM_PREFIX)

fun customReciterKey(id: Long): String = "$CUSTOM_PREFIX$id"

/** The numeric id of an imported reciter, or null when [key] is a built-in. */
fun customReciterId(key: String): Long? =
    if (key.startsWith(CUSTOM_PREFIX)) key.removePrefix(CUSTOM_PREFIX).toLongOrNull() else null
