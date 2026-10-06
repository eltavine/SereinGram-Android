plugins {
    `kotlin-dsl`
}

group = "com.eltavine.sereingram.buildlogic"

// Its own jar: settings plugins load into the settings class loader, which does
// not see the Android Gradle plugin the convention plugins need.
gradlePlugin {
    plugins {
        register("sereinSettings") {
            id = "serein.settings"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinSettingsPlugin"
        }
    }
}
