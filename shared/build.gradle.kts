plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        nodejs()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.github.cosinekitty:astronomy:2.1.19")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty(
        "referenceCorpusDir",
        rootProject.file("reference-source/reference/panchanga/v1").absolutePath,
    )
}
