dependencies {
    implementation(project(":embabel-lesson-with-agent"))
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
}

kotlin {
    jvmToolchain(21)
}