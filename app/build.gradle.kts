plugins {
    // Reglas del equipo (spotless, checkstyle, JUnit 5, JaCoCo >= 80%, Java 21) desde ci-kit.
    id("edu.austral.ingsis.java-conventions") version "1.0.0"
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

// TODO: borrar cuando se publique ci-kit 1.0.1 (alinea JUnit con Spring Boot).
extra["junit-jupiter.version"] = "5.10.3"

repositories {
    mavenCentral()
    // Libreria PrintScript. GitHub Packages pide login; mismas credenciales que el plugin de ci-kit.
    maven {
        url = uri("https://maven.pkg.github.com/IngSis-MAK-LR-DK/PrintScript")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

val printscriptVersion = "1.1.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Modulos de PrintScript que usa el servicio (el CLI de la libreria no se publica).
    for (module in listOf("common", "lexer", "parser", "interpreter", "formatter", "analyzer")) {
        implementation("edu.austral.ingsis.printscript:$module:$printscriptVersion")
    }

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

// Un unico jar ejecutable con nombre fijo (lo usa el Dockerfile); sin el "-plain.jar".
tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

// La clase con main() no se puede cubrir con tests unitarios; se excluye de JaCoCo.
val jacocoExcludes = listOf("**/PrintScriptServiceApplication.class")

tasks.named<JacocoReport>("jacocoTestReport") {
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } }),
    )
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } }),
    )
}
