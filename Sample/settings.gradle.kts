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
        maven {
            //url = uri("${rootProject.projectDir}/../aar/1.0.0")
            url = uri("https://raw.githubusercontent.com/web3user1/OpenWeb3-Android/main/aar/1.0.5") // 本地 Maven 仓库路径
        }
    }
}

rootProject.name = "Sample"
include(":app")
//include(":core")
include(":walletproviders")
