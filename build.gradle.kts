plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.paparazzi) apply false
    alias(libs.plugins.google.services) apply false
}

// Quali test ha eseguito la CI, leggibili da win nelle annotazioni pubbliche della run (app-scroll-master-lavoro-27-11): il log mostra
// solo i test falliti e il rapporto dei test non si carica, quindi un test nuovo verde poteva anche non essere partito. Una notice
// per task di test, col conto per classe («Classe verdi/eseguiti»). Solo sulla CI.
subprojects {
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        if (System.getenv("GITHUB_ACTIONS") != "true") return@configureEach
        val task = name
        val counts = java.util.concurrent.ConcurrentHashMap<String, IntArray>()
        addTestListener(object : org.gradle.api.tasks.testing.TestListener {
            override fun beforeSuite(suite: org.gradle.api.tasks.testing.TestDescriptor) {}
            override fun beforeTest(testDescriptor: org.gradle.api.tasks.testing.TestDescriptor) {}
            override fun afterTest(testDescriptor: org.gradle.api.tasks.testing.TestDescriptor, result: org.gradle.api.tasks.testing.TestResult) {
                val c = counts.computeIfAbsent(testDescriptor.className?.substringAfterLast('.') ?: "?") { IntArray(2) }
                synchronized(c) { c[0]++; if (result.resultType == org.gradle.api.tasks.testing.TestResult.ResultType.SUCCESS) c[1]++ }
            }
            override fun afterSuite(suite: org.gradle.api.tasks.testing.TestDescriptor, result: org.gradle.api.tasks.testing.TestResult) {
                if (suite.parent != null) return
                val classes = counts.toSortedMap().entries.joinToString(", ") { e -> "${e.key} ${e.value[1]}/${e.value[0]}" }
                println("::notice title=Test eseguiti ($task)::${result.testCount} test, ${result.successfulTestCount} verdi, ${result.failedTestCount} rossi, ${result.skippedTestCount} saltati. $classes")
            }
        })
    }
}
