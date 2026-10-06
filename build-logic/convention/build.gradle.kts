plugins {
    `kotlin-dsl`
}

group = "com.eltavine.sereingram.buildlogic"

dependencies {
    compileOnly(libs.android.gradleApi)
    compileOnly(libs.kotlin.gradlePlugin)
    // The root build has no KSP on its classpath, so the conventions bring it.
    implementation(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("sereinJvmLibrary") {
            id = "serein.jvm.library"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinJvmLibraryPlugin"
        }
        register("sereinAndroidLibrary") {
            id = "serein.android.library"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinAndroidLibraryPlugin"
        }
        register("sereinAndroidRoom") {
            id = "serein.android.room"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinAndroidRoomPlugin"
        }
        register("sereinAndroidApplication") {
            id = "serein.android.application"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinApplicationPlugin"
        }
    }
}
