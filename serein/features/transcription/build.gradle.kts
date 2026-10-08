plugins {
    id("serein.jvm.library")
}

dependencies {
    api(project(":serein:core"))
    implementation(libs.guava)
}
