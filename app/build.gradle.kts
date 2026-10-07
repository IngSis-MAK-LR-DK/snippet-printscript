plugins {
    // Reglas del equipo (spotless, checkstyle, JUnit 5, JaCoCo >= 80%, Java 21) desde ci-kit.
    id("edu.austral.ingsis.java-conventions") version "1.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.guava)
}
