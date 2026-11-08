package com.shopkeeper.architecture

import com.shopkeeper.data.local.ProductEntity
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.ShopInfo
import com.shopkeeper.ui.viewmodel.ProductViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
}
