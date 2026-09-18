plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
}

// AGP 9 charge déjà kotlin-android sur le classpath (runtime dependency interne, sans
// version déclarée) pour son "built-in Kotlin" — même désactivé via android.builtInKotlin=false.
// alias(libs.plugins.kotlin.android) imposerait une version précise et Gradle refuse de
// vérifier la compatibilité avec la version déjà présente ("already on the classpath with
// an unknown version"). apply(plugin = ...) contourne ce conflit en utilisant directement
// les classes déjà chargées, sans repasser par la résolution de version.
apply(plugin = "org.jetbrains.kotlin.android")

// AUTO-VERSION: 2.6.5
// ── Auto-incrémentation de version à chaque compilation ──────────────────
// Format X.Y.Z : Z va de 0 à 9 puis repasse à 0 en incrémentant Y ; Y suit
// la même règle sur X ; X n'a pas de limite.
//
// Le numéro "officiel" vit dans le commentaire "AUTO-VERSION" juste
// au-dessus — c'est cette ligne (texte brut, lisible même sans exécuter
// Gradle) que ce script relit et réécrit à chaque build, et que l'app va
// aussi lire à distance sur GitHub pour détecter une mise à jour. Pas de
// fichier séparé à suivre ni à pousser en plus : un simple commit+push de
// build.gradle.kts suffit à publier la nouvelle version.
//
// N'incrémente que pour une VRAIE compilation (assemble/bundle/install —
// bouton Run inclus), pas pour une simple synchronisation Gradle dans
// Android Studio (qui ré-évalue aussi ce script mais ne doit pas compter
// comme une compilation).
val versionRegex = Regex("""AUTO-VERSION: (\d+)\.(\d+)\.(\d+)""")
val currentBuildFileText = buildFile.readText()
val versionMatch = versionRegex.find(currentBuildFileText)
    ?: error("Impossible de trouver le marqueur AUTO-VERSION dans build.gradle.kts")

var versionMajor = versionMatch.groupValues[1].toInt()
var versionMinor = versionMatch.groupValues[2].toInt()
var versionPatch = versionMatch.groupValues[3].toInt()

val isRealBuild = gradle.startParameter.taskNames.any { taskName ->
    listOf("assemble", "bundle", "install").any { taskName.contains(it, ignoreCase = true) }
}

if (isRealBuild) {
    versionPatch += 1
    if (versionPatch > 9) {
        versionPatch = 0
        versionMinor += 1
        if (versionMinor > 9) {
            versionMinor = 0
            versionMajor += 1
        }
    }

    val updatedText = currentBuildFileText.replaceFirst(
        versionRegex,
        "AUTO-VERSION: $versionMajor.$versionMinor.$versionPatch",
    )
    buildFile.writeText(updatedText)
}

val computedVersionName = "$versionMajor.$versionMinor.$versionPatch"
val computedVersionCode = versionMajor * 10_000 + versionMinor * 100 + versionPatch

println("Version de build : $computedVersionName (code $computedVersionCode)")

android {
    namespace = "com.jc2.jdrcompagnon"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jc2.jdrcompagnon"
        minSdk = 24
        targetSdk = 36
        versionCode = computedVersionCode
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.21"
    }

    buildFeatures {
        buildConfig = true
    }
}

// kotlin-android est appliqué via apply(plugin = ...) plus haut (contournement du conflit
// AGP 9 / built-in Kotlin), donc l'accesseur "kotlin { }" généré par le plugins{} block
// n'est pas disponible ici — on configure le jvmTarget via tasks.withType à la place, pour
// qu'il corresponde à compileOptions ci-dessus (Java 11) plutôt que le 21 par défaut.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // Core / AppCompat / Material (legacy, conservés)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Compose (BOM aligne toutes les versions)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Google Fonts téléchargeables (Playfair Display / Inter — cf. Type.kt)
    // Pas d'entrée de version catalog existante : version alignée via le BOM Compose ci-dessus.
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Activity / Navigation / Lifecycle
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // DataStore + Serialization
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // Markdown Renderer
    implementation(libs.markdown.renderer)
    implementation(libs.markdown.renderer.m3)

    // Coil pour le chargement d'images (nécessaire pour WorldHeroImage)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // WorkManager — vérification périodique des mises à jour en arrière-plan
    // (pas de version catalog entry existante : ajoutée en direct)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Room — persistance locale de la feature Boutique (première utilisation de Room
    // dans le projet ; jusqu'ici SharedPreferences + fichiers .md)
    // Room 2.6.1 plante avec KSP2 sur les fonctions suspend retournant Unit
    // ("unexpected jvm signature V") — corrigé à partir de 2.7.0, on prend la dernière stable.
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}