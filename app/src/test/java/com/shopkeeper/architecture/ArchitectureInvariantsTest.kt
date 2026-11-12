package com.shopkeeper.architecture

import com.shopkeeper.data.local.ProductEntity
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.ShopInfo
import com.shopkeeper.ui.viewmodel.AuthViewModel
import com.shopkeeper.ui.viewmodel.ProductViewModel
import com.shopkeeper.ui.viewmodel.SettingsViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier

/**
 * Machine-enforced architectural guardrails for agentic and human development.
 * Any pull request or automated commit that attempts to violate core architectural
 * invariants will immediately fail this test suite during pre-push or CI.
 */
class ArchitectureInvariantsTest {

    private val entityClasses = listOf(
        ProductEntity::class.java
    )

    private val domainModelClasses = listOf(
        Product::class.java,
        ShopInfo::class.java
    )

    @Test
    fun `room entities must never use Double or Float for database persistence`() {
        entityClasses.forEach { entityClass ->
            for (field in entityClass.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) continue

                val type = field.type
                val isFloatingPoint = type == java.lang.Double.TYPE ||
                    type == java.lang.Double::class.java ||
                    type == java.lang.Float.TYPE ||
                    type == java.lang.Float::class.java

                assertFalse(
                    "Architecture Violation: Entity '${entityClass.simpleName}' has field '${field.name}' " +
                        "of floating-point type '${type.simpleName}'. " +
                        "All monetary and numeric quantities must be stored as 64-bit integer minor units (Long) " +
                        "per the Monetary Precision Invariant.",
                    isFloatingPoint
                )
            }
        }
    }

    @Test
    fun `monetary fields in data models must be Long minor units`() {
        domainModelClasses.forEach { modelClass ->
            for (field in modelClass.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) continue

                val name = field.name.lowercase()
                val isMonetaryField = name.endsWith("minor") || name.endsWith("cents")

                if (isMonetaryField) {
                    val isLong = field.type == java.lang.Long.TYPE || field.type == java.lang.Long::class.java
                    assertTrue(
                        "Architecture Violation: Model '${modelClass.simpleName}' field '${field.name}' " +
                            "must be 64-bit Long minor units (cents), found: ${field.type.simpleName}.",
                        isLong
                    )
                }
            }
        }
    }

    @Test
    fun `inventory viewmodels must observe Room SSOT via Repositories and never reference Firestore directly`() {
        val forbiddenTypes = listOf(
            "com.google.firebase.firestore.FirebaseFirestore",
            "com.shopkeeper.data.remote.FirestoreService"
        )

        val vmClass = ProductViewModel::class.java

        // Check constructor parameters
        for (constructor in vmClass.constructors) {
            for (paramType in constructor.parameterTypes) {
                assertFalse(
                    "Architecture Violation: Inventory ViewModel '${vmClass.simpleName}' accepts forbidden remote type " +
                        "'${paramType.name}' in constructor. UI must observe Room SSOT via Repository.",
                    forbiddenTypes.contains(paramType.name)
                )
            }
        }

        // Check declared fields
        for (field in vmClass.declaredFields) {
            assertFalse(
                "Architecture Violation: Inventory ViewModel '${vmClass.simpleName}' declares field '${field.name}' " +
                    "of forbidden remote type '${field.type.name}'.",
                forbiddenTypes.contains(field.type.name)
            )
        }
    }

    @Test
    fun `format utilities must never accept floating-point types`() {
        val formatUtilsClass = Class.forName("com.shopkeeper.ui.util.FormatUtilsKt")
        for (method in formatUtilsClass.declaredMethods) {
            for (paramType in method.parameterTypes) {
                val isFloatingPoint = paramType == java.lang.Double.TYPE ||
                    paramType == java.lang.Double::class.java ||
                    paramType == java.lang.Float.TYPE ||
                    paramType == java.lang.Float::class.java
                assertFalse(
                    "Architecture Violation: FormatUtils method '${method.name}' accepts floating-point type '${paramType.simpleName}'. " +
                        "All monetary formatting must strictly operate on 64-bit Long minor units.",
                    isFloatingPoint
                )
            }
        }
    }

    @Test
    fun `viewmodels must never expose raw String flows for user-facing feedback - must use UiText`() {
        val viewModelClasses = listOf(
            ProductViewModel::class.java,
            SettingsViewModel::class.java,
            AuthViewModel::class.java
        )

        for (vm in viewModelClasses) {
            for (field in vm.declaredFields) {
                val genericName = field.genericType.typeName
                val isFlow = genericName.contains("StateFlow") || genericName.contains("SharedFlow")
                val isStringFlow = genericName.contains("<java.lang.String>") || genericName.contains("<kotlin.String>")
                val isFeedbackField = field.name.contains("message", ignoreCase = true) ||
                    field.name.contains("toast", ignoreCase = true) ||
                    field.name.contains("error", ignoreCase = true)

                if (isFlow && isFeedbackField) {
                    assertFalse(
                        "Architecture Violation: ViewModel '${vm.simpleName}' exposes raw String flow '${field.name}'. " +
                            "All user-facing feedback must strictly use typed UiText.",
                        isStringFlow
                    )
                }
            }
        }
    }

    @Test
    fun `auth state error payload must be typed UiText`() {
        val authErrorClass = Class.forName("com.shopkeeper.ui.viewmodel.AuthState\$Error")
        val constructor = authErrorClass.constructors.first()
        val paramType = constructor.parameterTypes.first()
        assertTrue(
            "Architecture Violation: AuthState.Error must accept UiText, found: ${paramType.simpleName}",
            paramType.name == "com.shopkeeper.ui.util.UiText"
        )
    }

    @Test
    fun `presentation layer must never import static alert color tokens directly`() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val srcDir = if (File(projectDir, "src").exists()) File(projectDir, "src") else File(projectDir, "app/src")
        val uiDir = File(srcDir, "main/java/com/shopkeeper/ui")

        val forbiddenImports = listOf(
            "com.shopkeeper.ui.theme.RedAlert",
            "com.shopkeeper.ui.theme.RedLight",
            "com.shopkeeper.ui.theme.RedAlertDark",
            "com.shopkeeper.ui.theme.RedContainerDark"
        )

        uiDir.walkTopDown()
            .filter { it.extension == "kt" && !it.path.contains("/theme/") }
            .forEach { file ->
                val lines = file.readLines()
                for (forbidden in forbiddenImports) {
                    assertFalse(
                        "Architecture Violation in '${file.name}': imports forbidden static alert color '$forbidden'. " +
                            "All error styling must strictly use MaterialTheme.colorScheme.error or errorContainer.",
                        lines.any { it.contains("import $forbidden") }
                    )
                }
            }
    }

    @Test
    fun `presentation layer must never import raw Material AlertDialog directly`() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val srcDir = if (File(projectDir, "src").exists()) File(projectDir, "src") else File(projectDir, "app/src")
        val uiDir = File(srcDir, "main/java/com/shopkeeper/ui")

        val forbiddenDialogs = listOf(
            "androidx.compose.material3.AlertDialog",
            "androidx.compose.material3.BasicAlertDialog"
        )

        uiDir.walkTopDown()
            .filter { it.extension == "kt" && !it.name.contains("ShopDialog") }
            .forEach { file ->
                val lines = file.readLines()
                for (forbidden in forbiddenDialogs) {
                    assertFalse(
                        "Architecture Violation in '${file.name}': imports raw Material '$forbidden'. " +
                            "All dialogs must use the canonical ShopDialog or ShopConfirmationDialog component.",
                        lines.any { it.contains("import $forbidden") }
                    )
                }
            }
    }

    @Test
    fun `all supported locales must have 100 percent string translation parity with default strings xml`() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val resDir = if (File(projectDir, "src/main/res").exists()) {
            File(projectDir, "src/main/res")
        } else {
            File(projectDir, "app/src/main/res")
        }

        val defaultStringsFile = File(resDir, "values/strings.xml")
        assertTrue("Default strings.xml must exist", defaultStringsFile.exists())

        fun extractKeys(file: File): Set<String> {
            val stringTagRegex = """<string\s+name="([^"]+)"""".toRegex()
            return file.readLines()
                .mapNotNull { line -> stringTagRegex.find(line)?.groupValues?.get(1) }
                .toSet()
        }

        val defaultKeys = extractKeys(defaultStringsFile)
        assertTrue("Default strings.xml must have keys", defaultKeys.isNotEmpty())

        val localeDirs = listOf(
            File(resDir, "values-bn"),
            File(resDir, "values-es")
        )

        for (localeDir in localeDirs) {
            val localeStringsFile = File(localeDir, "strings.xml")
            assertTrue("Locale file '${localeStringsFile.path}' must exist", localeStringsFile.exists())

            val localeKeys = extractKeys(localeStringsFile)
            val missingKeys = defaultKeys - localeKeys

            assertTrue(
                "Localization Invariant Violation: Locale '${localeDir.name}' is missing ${missingKeys.size} " +
                    "string translations present in default values/strings.xml: $missingKeys",
                missingKeys.isEmpty()
            )
        }
    }
}
