package permissions.dispatcher.processor

import permissions.dispatcher.RuntimePermissions
import permissions.dispatcher.processor.impl.java.JavaActivityProcessorUnit
import permissions.dispatcher.processor.impl.java.JavaBaseProcessorUnit
import permissions.dispatcher.processor.impl.java.JavaFragmentProcessorUnit
import permissions.dispatcher.processor.impl.kotlin.KotlinActivityProcessorUnit
import permissions.dispatcher.processor.impl.kotlin.KotlinBaseProcessorUnit
import permissions.dispatcher.processor.impl.kotlin.KotlinFragmentProcessorUnit
import permissions.dispatcher.processor.util.findAndValidateProcessorUnit
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.properties.Delegates

class PermissionsProcessor : AbstractProcessor() {
    private var javaProcessorUnits: List<JavaBaseProcessorUnit> by Delegates.notNull()
    private var kotlinProcessorUnits: List<KotlinBaseProcessorUnit> by Delegates.notNull()
    /* Element Utilities, obtained from the processing environment */
    private var elementUtils: Elements by Delegates.notNull()
    /* Type Utilities, obtained from the processing environment */
    private var typeUtils: Types by Delegates.notNull()
    /* Processing Environment helpers */
    private var filer: Filer by Delegates.notNull()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        filer = processingEnv.filer
        elementUtils = processingEnv.elementUtils
        typeUtils = processingEnv.typeUtils
        javaProcessorUnits = listOf(JavaActivityProcessorUnit(elementUtils), JavaFragmentProcessorUnit(elementUtils))
        kotlinProcessorUnits = listOf(KotlinActivityProcessorUnit(elementUtils), KotlinFragmentProcessorUnit(elementUtils))
    }

    override fun getSupportedSourceVersion(): SourceVersion? {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return hashSetOf(RuntimePermissions::class.java.canonicalName)
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Create a RequestCodeProvider which guarantees unique request codes for each permission request
        val requestCodeProvider = RequestCodeProvider()

        // The Set of annotated elements needs to be ordered
        // in order to achieve Deterministic, Reproducible Builds
        roundEnv.getElementsAnnotatedWith(RuntimePermissions::class.java)
                .sortedBy { it.simpleName.toString() }
                .forEach {
                    val rpe = RuntimePermissionsElement(elementUtils, typeUtils, it as TypeElement)
                    val kotlinMetadata = it.getAnnotation(Metadata::class.java)
                    if (kotlinMetadata != null) {
                        processKotlin(it, rpe, requestCodeProvider)
                    } else {
                        processJava(it, rpe, requestCodeProvider)
                    }
                }
        return true
    }

    private fun processKotlin(element: Element, rpe: RuntimePermissionsElement, requestCodeProvider: RequestCodeProvider) {
        val processorUnit = findAndValidateProcessorUnit(kotlinProcessorUnits, typeUtils, element)
        val kotlinFile = processorUnit.createFile(rpe, requestCodeProvider)
        kotlinFile.writeTo(filer)
    }

    private fun processJava(element: Element, rpe: RuntimePermissionsElement, requestCodeProvider: RequestCodeProvider) {
        val processorUnit = findAndValidateProcessorUnit(javaProcessorUnits, typeUtils, element)
        val javaFile = processorUnit.createFile(rpe, requestCodeProvider)
        javaFile.writeTo(filer)
    }
}
