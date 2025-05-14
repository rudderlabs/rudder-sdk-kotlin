import org.gradle.api.JavaVersion

object RudderStackBuildConfig {

    object Build {

        val JAVA_VERSION = JavaVersion.VERSION_17
        const val JVM_TARGET = "17"
        const val JVM_TOOLCHAIN = 17
    }

    object Android {

        const val COMPILE_SDK = 35
        const val MIN_SDK = 21
    }

    object Version {

        const val VERSION_NAME = "1.0.0"
        const val VERSION_CODE = "2"
    }

    object PackageName {

        const val PACKAGE_NAME = "com.rudderstack.sdk.kotlin"

        object Integrations {

            const val BRAZE = "com.rudderstack.integration.kotlin.braze"
            const val ADJUST = "com.rudderstack.integration.kotlin.adjust"
            const val FIREBASE = "com.rudderstack.integration.kotlin.firebase"
            const val FACEBOOK = "com.rudderstack.integration.kotlin.facebook"
        }
    }

    object Kotlin {

        const val COMPILER_EXTENSION_VERSION = "1.5.1"
    }

    object ReleaseInfo {

        val VERSION_NAME = ""
        val GROUP_NAME = ""
    }

    object POM {

        const val NAME = "Analytics Kotlin SDK"
        const val DESCRIPTION = "RudderStack\'s SDK for android"

        const val URL = "https://github.com/rudderlabs/rudder-sdk-kotlin"
        const val SCM_URL = "https://github.com/rudderlabs/rudder-sdk-kotlin/tree/main"
        const val SCM_CONNECTION = "scm:git:git://github.com/rudderlabs/rudder-sdk-kotlin.git"
        const val SCM_DEV_CONNECTION = "scm:git:git://github.com:rudderlabs/rudder-sdk-kotlin.git"

        const val LICENCE_NAME = "Elastic License 2.0 (ELv2)"
        const val LICENCE_URL = "https://github.com/rudderlabs/rudder-sdk-kotlin/blob/main/LICENSE.md"
        const val LICENCE_DIST = "repo"

        const val DEVELOPER_ID = "Rudderstack"
        const val DEVELOPER_NAME = "Rudderstack, Inc."
    }

    object Modules {
        object Android : ModuleConfig {

            override val artifactId = "android"
            override val pomPackaging = "aar"
        }

        object Core : ModuleConfig {

            override val artifactId = "core"
            override val pomPackaging = "jar"
        }
    }
}

interface ModuleConfig {

    val artifactId: String
    val pomPackaging: String
}
