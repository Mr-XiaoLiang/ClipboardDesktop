import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
//            implementation(libs.androidx.foundation.desktop)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.lollipop.clipboard.MainKt"

        buildTypes.release {
            proguard {
                configurationFiles.from(file("proguard-rules.pro"))
                isEnabled = false
            }
        }

        jvmArgs += listOf("-Xmx2G")
        val appName = "Clipboard"
        val versionName = "1.0.0"
        val pkgName = "com.lollipop.clipboard"
        val sdf = SimpleDateFormat("yyyyMMdd-HHmmss")
        val buildVersion = "${versionName}-${sdf.format(Date(System.currentTimeMillis()))}"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = appName
            packageVersion = versionName
            description = appName
            macOS {
                dockName = appName
                bundleID = pkgName
                pkgPackageVersion = versionName
                pkgPackageBuildVersion = buildVersion
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }
            windows {
                menuGroup = appName
                dirChooser = true
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
            linux {
                packageName = appName
                menuGroup = appName
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }
}
