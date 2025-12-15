package agdesigns.elevatefitness.shared


enum class Equipment(val equipmentResKey: String){
    EVERYTHING("equipments_see_all"), // Used when filtering by muscle to get everything
    BARBELL("equipments_barbell"),
    BODY_WEIGHT("equipments_body_weight"),
    CABLES("equipments_cables"),
    DUMBBELL("equipments_dumbbell"),
    MACHINE("equipments_machine");

    val equipmentNameResource: Int
        get() = when (this) {
            EVERYTHING -> R.string.equipments_see_all
            BARBELL -> R.string.equipments_barbell
            BODY_WEIGHT -> R.string.equipments_body_weight
            CABLES -> R.string.equipments_cables
            DUMBBELL -> R.string.equipments_dumbbell
            MACHINE -> R.string.equipments_machine
        }

    companion object {
        fun fromResKey(resKey: String?): Equipment? {
            return entries.firstOrNull { it.equipmentResKey == resKey }
        }
    }
}