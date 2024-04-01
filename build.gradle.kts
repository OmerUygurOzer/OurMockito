plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(project(mapOf("path" to ":Annotations")))
    testImplementation("junit:junit:4.13.1")
    testAnnotationProcessor(project(mapOf("path" to ":Processor")))
    //implementation(project(mapOf("path" to ":Processor")))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}