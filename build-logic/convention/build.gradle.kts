plugins {
    `kotlin-dsl`
}

group = "com.eltavine.sereingram.buildlogic"

dependencies {
    compileOnly(libs.android.gradleApi)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.sentry.gradlePlugin)
    // The root build has no KSP on its classpath, so the conventions bring it.
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.room.gradlePlugin)
    implementation(libs.binaryCompatibilityValidator.gradlePlugin)
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
        register("sereinApi") {
            id = "serein.api"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinApiPlugin"
        }
        register("sereinAndroidApplication") {
            id = "serein.android.application"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinApplicationPlugin"
        }
    }
}
