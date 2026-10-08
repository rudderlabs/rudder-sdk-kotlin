import org.gradle.api.JavaVersion

private typealias LibraryInfoContract = LibraryInfo

object RudderStackBuildConfig {

    object Build {

        val JAVA_VERSION = JavaVersion.VERSION_17
        const val JVM_TARGET = "17"
        const val JVM_TOOLCHAIN = 17
    }

    object CoreBuild {

        val JAVA_VERSION = JavaVersion.VERSION_1_8
        const val JVM_TARGET = "1.8"
        const val JAVA_RELEASE = 8
    }

    object AndroidBuild {

        const val COMPILE_SDK = 35
        const val MIN_SDK = 21
    }

    object SDK {

        const val PACKAGE_NAME = "com.rudderstack.sdk.kotlin"

        object Core {

            const val VERSION_NAME = "1.8.0"

            object LibraryInfo : LibraryInfoContract {

                override val name: String = "$PACKAGE_NAME.core"
            }

            object PublishConfig : MavenPublishConfig {

                override val artifactId = "core"
                override val pomPackaging = "jar"
                override val pomDescription = "Kotlin JVM (server-side, public beta) and core of the Android SDK"
            }
        }

        object Android {

            const val VERSION_NAME = "2.0.0"
            const val VERSION_CODE = "15"

            object LibraryInfo : LibraryInfoContract {

                override val name: String = "$PACKAGE_NAME.android"
            }

            object PublishConfig : MavenPublishConfig {

                override val artifactId = "android"
                override val pomPackaging = "aar"
            }
        }
    }

    object Integrations {

        const val PACKAGE_NAME = "com.rudderstack.integration.kotlin"

        object Adjust : IntegrationModuleInfo {

            override val moduleName: String = "adjust"
            override val versionName: String = "2.0.0"
            override val versionCode: String = "9"

            override val artifactId = "adjust"
            override val pomPackaging = "aar"
        }

        object AppsFlyer : IntegrationModuleInfo {

            override val moduleName: String = "appsflyer"
            override val versionName: String = "3.0.0"
            override val versionCode: String = "9"

            override val artifactId = "appsflyer"
            override val pomPackaging = "aar"
        }

        object Braze : IntegrationModuleInfo {

            override val moduleName: String = "braze"
            override val versionName: String = "2.0.0"
            override val versionCode: String = "10"

            override val artifactId = "braze"
            override val pomPackaging = "aar"
        }

        object CleverTap : IntegrationModuleInfo {

            override val moduleName: String = "clevertap"
            override val versionName: String = "2.0.0"
            override val versionCode: String = "3"

            override val artifactId = "clevertap"
            override val pomPackaging = "aar"
        }

        object Facebook : IntegrationModuleInfo {

            override val moduleName: String = "facebook"
            override val versionName: String = "2.0.0"
            override val versionCode: String = "9"

            override val artifactId = "facebook"
            override val pomPackaging = "aar"
        }

        object Firebase : IntegrationModuleInfo {

            override val moduleName: String = "firebase"
            override val versionName: String = "2.0.0"
            override val versionCode: String = "10"

            override val artifactId = "firebase"
            override val pomPackaging = "aar"
        }

        object Sprig : IntegrationModuleInfo {

            override val moduleName: String = "sprig"
            override val versionName: String = "2.0.0"
            override val versionCode: String = "5"

            override val artifactId = "sprig"
            override val pomPackaging = "aar"
        }

        fun getModuleInfo(projectName: String): IntegrationModuleInfo = when (projectName) {
            "adjust" -> Adjust
            "braze" -> Braze
            "clevertap" -> CleverTap
            "facebook" -> Facebook
            "firebase" -> Firebase
            "appsflyer" -> AppsFlyer
            "sprig" -> Sprig
            else -> throw IllegalArgumentException("Unknown integration module: $projectName")
        }
    }

    object POM {

        const val NAME = "Analytics Kotlin SDK"
        const val DESCRIPTION = "RudderStack\'s SDK for android"

        const val URL = "https://github.com/rudderlabs/rudder-sdk-kotlin"
        const val SCM_URL = "https://github.com/rudderlabs/rudder-sdk-kotlin/tree/main"
        const val SCM_CONNECTION = "scm:git:git://github.com/rudderlabs/rudder-sdk-kotlin.git"
        const val SCM_DEV_CONNECTION = "scm:git:git://github.com:rudderlabs/rudder-sdk-kotlin.git"

        const val LICENCE_NAME = "MIT License"
        const val LICENCE_URL = "https://github.com/rudderlabs/rudder-sdk-kotlin/blob/main/LICENSE.md"
        const val LICENCE_DIST = "repo"

        const val DEVELOPER_ID = "Rudderstack"
        const val DEVELOPER_NAME = "Rudderstack, Inc."
    }
}

interface LibraryInfo {

    val name: String
}

interface MavenPublishConfig {

    val artifactId: String
    val pomPackaging: String
    val pomDescription: String
        get() = RudderStackBuildConfig.POM.DESCRIPTION
}

interface IntegrationModuleInfo : MavenPublishConfig {

    val moduleName: String
    val versionName: String
    val versionCode: String

    val namespace: String
        get() = "${RudderStackBuildConfig.Integrations.PACKAGE_NAME}.$moduleName"
}
