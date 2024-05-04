package com.anexus.perfectgymcoach.data

enum class Sex (val sexName: String){
        MALE("Male"),
        FEMALE("Female"),
        OTHER("Other");

        companion object {
                fun fromName(name: String?): Sex{
                        for (sex in entries){
                                if (sex.sexName == name)
                                        return sex
                        }
                        return OTHER
                }
        }
}

enum class Theme (val themeName: String){
        SYSTEM("Same as system"),
        LIGHT("Always light"),
        DARK("Always dark");

        companion object {
                fun fromName(name: String?): Theme{
                        for (theme in entries){
                                if (theme.themeName == name)
                                        return theme
                        }
                        return SYSTEM
                }
        }
}