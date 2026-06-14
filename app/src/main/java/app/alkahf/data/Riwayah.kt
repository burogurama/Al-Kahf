package app.alkahf.data

/** A Qur'an reading. [key] is the persisted/serialized form. */
enum class Riwayah(val key: String) {
    HAFS("hafs"),
    WARSH("warsh");

    companion object {
        fun fromKey(key: String?): Riwayah = entries.firstOrNull { it.key == key } ?: HAFS
    }
}
