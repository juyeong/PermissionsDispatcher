package permissions.dispatcher.processor.impl.java

import com.squareup.javapoet.MethodSpec
import permissions.dispatcher.processor.util.typeMirrorOf
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements

/**
 * ProcessorUnit implementation for Fragment.
 */
class JavaFragmentProcessorUnit(private val elementUtils: Elements) : JavaBaseProcessorUnit() {

    override fun getTargetType(): TypeMirror = elementUtils.typeMirrorOf("androidx.fragment.app.Fragment")

    override fun getActivityName(targetParam: String): String = "$targetParam.requireActivity()"

    override fun addShouldShowRequestPermissionRationaleCondition(builder: MethodSpec.Builder, targetParam: String, permissionField: String, isPositiveCondition: Boolean) {
        builder.beginControlFlow("if (\$N\$T.shouldShowRequestPermissionRationale(\$N, \$N))", if (isPositiveCondition) "" else "!", permissionUtils, targetParam, permissionField)
    }

    override fun addRequestPermissionsStatement(builder: MethodSpec.Builder, targetParam: String, permissionField: String, requestCodeField: String) {
        builder.addStatement("\$N.requestPermissions(\$N, \$N)", targetParam, permissionField, requestCodeField)
    }
}
