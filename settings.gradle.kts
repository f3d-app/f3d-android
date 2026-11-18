dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "f3d-android"
if (providers.gradleProperty("f3d.use.local.library").getOrElse("false") == "true") {
    include(":library")
}
include(":f3d")
