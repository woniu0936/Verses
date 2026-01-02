import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension

class VersesPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")
            pluginManager.apply("org.jetbrains.dokka")

            extensions.configure<MavenPublishBaseExtension> {
                val myGroup = providers.gradleProperty("GROUP").getOrElse("com.woniu0936")
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

                publishToMavenCentral()
                signAllPublications()
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