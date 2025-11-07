import org.gradle.kotlin.dsl.flatDir

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        flatDir {
            dirs("app/libs")
        }

        maven {
            name = "badaix GitHub Packages"
            url = uri("https://maven.pkg.github.com/badaix/snapcast-deps")
            credentials {
                username = System.getenv("GITHUB_USER")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "Snapcast"
include(":app")
 