
plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
    id("com.gradleup.gr8") version "0.10"
}

repositories {
    mavenCentral()
}

val shadeConfiguration = configurations.create("shade")
dependencies {
    add("shade", "com.google.guava:guava:33.2.0-jre")
    add("shade", "com.google.j2objc:j2objc-annotations:3.0.0")

    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.hamcrest:hamcrest:2.2")
}

gr8 {
    val shadowedJar = create("gr8") {
        archiveName("net-biesemeyer-funlock-${project.version}.jar")
        exclude("META-INF/?.*")
        proguardFile("packaging-rules.pro")
        configuration("shade")
    }
    replaceOutgoingJar(shadowedJar)
}
// Make the shadowed dependencies available during compilation/tests
configurations.named("compileOnly").configure {
    extendsFrom(shadeConfiguration)
}
configurations.named("testImplementation").configure {
    extendsFrom(shadeConfiguration)
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.10.1")
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.biesemeyer"
            artifactId = "funlock"
            version = "1.0.0"

            from(components["java"])

            pom {
                name = "FunLock"
                description = "Functional interfaces for Locks"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name = "Rye Biesemeyer"
                        email = "rye@biesemeyer.net"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com:biesemeyer/funlock.git"
                    developerConnection = "scm:git:git@github.com:biesemeyer/funlock.git"
                    url = "https://github.com/biesemeyer/funlock"
                }
            }
        }
    }
}
