plugins {
  kotlin("multiplatform")
  id("com.android.library")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
  androidTarget {
    compilations.all {
      kotlinOptions { jvmTarget = "11" }
    }
  }

  jvm("desktop") {
    compilations.all {
      kotlinOptions { jvmTarget = "11" }
    }
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "TelefamShared"
      isStatic = true
    }
    iosTarget.compilations.getByName("main") {
      cinterops.create("TelefamCore") {
        definitionFile.set(project.file("src/nativeInterop/cinterop/telefam_core.def"))
        includeDirs(project.file("../cpp/telefam/company/bridge"))
      }
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
      }
    }
    val androidMain by getting {
      dependencies {
        implementation("androidx.core:core-ktx:1.18.0")
      }
    }
    val desktopMain by getting {
      dependencies {
        implementation(compose.desktop.common)
      }
    }
    val iosMain by creating {
      dependsOn(commonMain)
    }
  }
}

android {
  namespace = "com.telefam.company.shared"
  compileSdk = 36
  defaultConfig {
    minSdk = 24
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}
