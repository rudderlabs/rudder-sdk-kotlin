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

        const val VERSION_NAME = "\"1.0.0\""
        const val VERSION_CODE = "1"
    }

    object PacakageName {
        const val ANDROID = "com.rudderstack.android.sdk"
        const val KOTLIN = "\"com.rudderstack.kotlin.sdk\""
    }

    object Kotlin {

        const val COMPILER_EXTENSION_VERSION = "1.5.1"
    }

    object ReleaseInfo {

        val VERSION_NAME = ""
        val GROUP_NAME = ""
    }
}
