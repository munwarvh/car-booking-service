plugins {
    id("java")
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.4"
    id("jacoco")
    id("org.openapi.generator") version "7.4.0"
}

group = "com.velocity"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2024.0.0"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // ShedLock for distributed scheduler locking
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.10.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.10.0")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")

    // Database
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

    // OpenAPI Generator dependencies
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform {
        // Exclude integration tests by default (they require Docker)
        excludeTags("integration")
    }
    finalizedBy(tasks.jacocoTestReport)
}

// Separate task for integration tests (requires Docker to be running)
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests (requires Docker)"
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.test)
}

// Task to run all tests including integration tests
tasks.register<Test>("allTests") {
    description = "Runs all tests including integration tests (requires Docker)"
    group = "verification"
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/dto/**",
                    "**/model/**",
                    "**/exception/**",
                    "**/*Application*"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

// OpenAPI Generator configuration
openApiGenerate {
    generatorName.set("java")
    inputSpec.set("${rootDir}/src/main/resources/openapi/creditcardpayment_api.yml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    apiPackage.set("com.velocity.carservice.infrastructure.adapter.outbound.creditcard.api")
    modelPackage.set("com.velocity.carservice.infrastructure.adapter.outbound.creditcard.model")
    invokerPackage.set("com.velocity.carservice.infrastructure.adapter.outbound.creditcard")
    configOptions.set(mapOf(
        "library" to "webclient",
        "useJakartaEe" to "true",
        "dateLibrary" to "java8",
        "openApiNullable" to "false",
        "useSpringBoot3" to "true",
        "serializationLibrary" to "jackson"
    ))
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))
        }
    }
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}
