package com.eltavine.sereingram

import com.eltavine.sereingram.app.SereinApp
import com.tngtech.archunit.base.DescribedPredicate.not
import com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarFile

/**
 * The layering that keeps SereinGram mergeable: upstream code reaches
 * SereinGram only through the hook facade, and features stay independent.
 */
class ArchitectureTest {
    @Test
    fun upstreamCodeCallsOnlyTheHookFacade() {
        assertTrue(
            "ApplicationLoader starts SereinApp, so it must be among the checked classes",
            upstreamCallers.contain("org.telegram.messenger.ApplicationLoader"),
        )
        noClasses()
            .that().resideOutsideOfPackage("$SEREIN..")
            .should().dependOnClassesThat(
                resideInAPackage("$SEREIN..")
                    .and(not(resideInAPackage("$SEREIN.hooks..")))
                    .and(not(equivalentTo(SereinApp::class.java))),
            )
            .because("an upstream file may only call com.eltavine.sereingram.hooks or start SereinApp")
            .check(upstreamCallers)
    }

    @Test
    fun featuresDoNotDependOnEachOther() {
        slices().matching("$SEREIN.features.(*)..")
            .should().notDependOnEachOther()
            .because("features share state only through options and hooks")
            .check(sereinClasses)
    }

    @Test
    fun featuresDoNotReachIntoTheCompositionRoot() {
        noClasses()
            .that().resideInAPackage("$SEREIN.features..")
            .should().dependOnClassesThat().resideInAnyPackage("$SEREIN.app..", "$SEREIN.adapters..")
            .because("only the composition root wires modules to adapters")
            .check(sereinClasses)
    }

    private companion object {
        const val SEREIN = "com.eltavine.sereingram"
        val SEREIN_BINARY_NAME = SEREIN.replace('.', '/').toByteArray()

        // Only a class file that names a SereinGram class can break the rule, and
        // importing every upstream class does not fit in a test JVM's heap.
        val upstreamCallers: JavaClasses by lazy {
            val appCode = Paths.get(SereinApp::class.java.protectionDomain.codeSource.location.toURI())
            val importer = ClassFileImporter().withImportOption { location ->
                location.asURI().toURL().openStream().use { it.readBytes() }.contains(SEREIN_BINARY_NAME)
            }
            if (Files.isDirectory(appCode)) importer.importPath(appCode) else importer.importJar(JarFile(appCode.toFile()))
        }

        val sereinClasses: JavaClasses by lazy {
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages(SEREIN)
        }

        fun ByteArray.contains(needle: ByteArray): Boolean =
            (0..size - needle.size).any { start -> needle.indices.all { this[start + it] == needle[it] } }
    }
}
