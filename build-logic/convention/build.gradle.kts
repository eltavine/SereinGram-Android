plugins {
    `kotlin-dsl`
}

group = "com.eltavine.sereingram.buildlogic"

dependencies {
    compileOnly(libs.android.gradleApi)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("sereinJvmLibrary") {
            id = "serein.jvm.library"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinJvmLibraryPlugin"
        }
        register("sereinAndroidApplication") {
            id = "serein.android.application"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinApplicationPlugin"
        }
    }
}
