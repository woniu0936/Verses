import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.verses.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension

class VersesPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.findPlugin("vanniktech-maven-publish").get().get().pluginId)
            pluginManager.apply(libs.findPlugin("dokka").get().get().pluginId)

            // 1. Configure standard Maven Publish for GitHub Packages
            extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/woniu0936/Verses")
                        credentials {
                            username = System.getenv("GITHUB_ACTOR")
                            password = System.getenv("GITHUB_TOKEN")
                        }
                    }
                }
            }

            // 2. Configure Vanniktech plugin for Maven Central
            extensions.configure<MavenPublishBaseExtension> {
                val myGroup = providers.gradleProperty("GROUP").getOrElse("io.github.woniu0936")
                val myVersion = providers.gradleProperty("VERSION_NAME").getOrElse("1.0.0")

                coordinates(myGroup, project.name, myVersion)

                pom {
                    name.set("Verses")
                    description.set("A minimalist, high-performance declarative RecyclerView adapter for Modern Kotlin.")
                    url.set("https://github.com/woniu0936/Verses")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("woniu0936")
                            name.set("woniu0936")
                            email.set("woniu0936@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/woniu0936/Verses.git")
                        developerConnection.set("scm:git:ssh://github.com/woniu0936/Verses.git")
                        url.set("https://github.com/woniu0936/Verses.git")
                    }
                }

                // Automatically configure Dokka and Sources JARs
                configure(com.vanniktech.maven.publish.AndroidSingleVariantLibrary("release", true, true))

                // Modern Sonatype (S01) configuration with automatic release
                publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.S01, true)
                
                // Enhanced signing logic:
                // Supports both file-based (local) and memory-based (CI) signing
                val hasGpgKey = project.hasProperty("signing.keyId") || project.hasProperty("signingInMemoryKey")
                if (hasGpgKey) {
                    signAllPublications()
                }
            }

            // Migrating to Dokka V2 configuration
            extensions.configure<DokkaExtension> {
                dokkaPublications.configureEach {
                    outputDirectory.set(layout.buildDirectory.dir("dokka"))
                }
            }
        }
    }
}