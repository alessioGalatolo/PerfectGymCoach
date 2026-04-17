package agdesigns.elevatefitness.shared

enum class BarbellType(val barbellResKey: String, val weight: Map<Boolean, Float>){
    // name, weight[false] = kg, true = lbs
    EZ_CURL_LIGHT("barbells_light_ez_curl_bar", mapOf(Pair(false, 5f), Pair(true, 15f))),
    EZ_CURL("barbells_ez_curl_bar", mapOf(Pair(false, 10f), Pair(true, 25f))),
    YOUNG_OLYMPIC("barbells_young_s_olympic_bar", mapOf(Pair(false, 10f), Pair(true, 22f))),
    WOMEN_OLYMPIC("barbells_women_s_olympic_bar", mapOf(Pair(false, 15f), Pair(true, 33f))),
    MEN_OLYMPIC("barbells_men_s_olympic_bar", mapOf(Pair(false, 20f), Pair(true, 44f))),
    SQUAT("barbells_squat_bar", mapOf(Pair(false, 25f), Pair(true, 60f))),
    OTHER("barbells_other", mapOf(Pair(false, 0f), Pair(true, 0f)));

    val barbellResource: Int
        get() = when(this) {
            EZ_CURL_LIGHT -> R.string.barbells_light_ez_curl_bar
            EZ_CURL -> R.string.barbells_ez_curl_bar
            YOUNG_OLYMPIC -> R.string.barbells_young_s_olympic_bar
            WOMEN_OLYMPIC -> R.string.barbells_women_s_olympic_bar
            MEN_OLYMPIC -> R.string.barbells_men_s_olympic_bar
            SQUAT -> R.string.barbells_squat_bar
            OTHER -> R.string.barbells_other
        }
}