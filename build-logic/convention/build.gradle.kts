plugins {
    `kotlin-dsl`
}

group = "com.eltavine.sereingram.buildlogic"

dependencies {
    compileOnly(libs.android.gradleApi)
}

gradlePlugin {
    plugins {
        register("sereinAndroidApplication") {
            id = "serein.android.application"
            implementationClass = "com.eltavine.sereingram.buildlogic.SereinApplicationPlugin"
        }
    }
}
