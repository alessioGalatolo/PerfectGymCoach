package agdesigns.elevatefitness.data

enum class Sex (val displayName: String){
        MALE("Male"),
        FEMALE("Female"),
        OTHER("Other");

        companion object {
                fun fromName(name: String?): Sex{
                        for (sex in entries){
                                if (sex.displayName == name)
                                        return sex
                        }
                        return OTHER
                }
        }
}

enum class Theme (val displayName: String){
        SYSTEM("Same as system"),
        LIGHT("Always light"),
        DARK("Always dark");

        companion object {
                fun fromName(name: String?): Theme{
                        for (theme in entries){
                                if (theme.displayName == name)
                                        return theme
                        }
                        return SYSTEM
                }
        }
}