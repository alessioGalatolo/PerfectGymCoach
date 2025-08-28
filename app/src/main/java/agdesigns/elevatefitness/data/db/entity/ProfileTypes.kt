package agdesigns.elevatefitness.data.db.entity

import agdesigns.elevatefitness.R

enum class Sex (val nameResKey: String){
        MALE("sexes_male"),
        FEMALE("sexes_female"),
        OTHER("sexes_other");

        val displayRes: Int
                get() = when(this){
                        MALE -> R.string.sexes_male
                        FEMALE -> R.string.sexes_female
                        OTHER -> R.string.sexes_other
                }

        companion object {
                fun fromResKey(resKey: String?): Sex{
                        for (sex in entries){
                                if (sex.nameResKey == resKey)
                                        return sex
                        }
                        return OTHER
                }

        }
}

enum class Theme (val nameResKey: String){
        SYSTEM("themes_system"),
        LIGHT("themes_light"),
        DARK("themes_dark");

        val displayRes: Int
                get() = when(this){
                        SYSTEM -> R.string.themes_system
                        LIGHT -> R.string.themes_light
                        DARK -> R.string.themes_dark
                }

        companion object {
                fun fromResKey(resKey: String?): Theme{
                        for (theme in entries){
                                if (theme.nameResKey == resKey)
                                        return theme
                        }
                        return SYSTEM
                }
        }
}