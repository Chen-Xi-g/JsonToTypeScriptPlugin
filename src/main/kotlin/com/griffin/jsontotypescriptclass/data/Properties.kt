package com.griffin.jsontotypescriptclass.data

import com.intellij.ide.util.PropertiesComponent
import kotlin.reflect.KProperty

/**
 * @author Hoshiiro
 */
object Properties {

    private const val KEY_PREFIX = "com.griffin.jsontotypescriptclass."

    var isOptional by booleanProperties()
    var indentSpace by intProperties(4)

    private fun booleanProperties() = PropertiesComponentBooleanDelegate()
    private fun stringProperties() = PropertiesComponentStringDelegate()
    private fun intProperties(defaultValue: Int = 0) = PropertiesComponentIntDelegate(defaultValue)

    private class PropertiesComponentBooleanDelegate {

        private var value: Boolean? = null

        operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
            if (value == null) {
                value = PropertiesComponent.getInstance().getBoolean("${KEY_PREFIX}${property.name}", false)
            }
            return value!!
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            this.value = value
            PropertiesComponent.getInstance().setValue("${KEY_PREFIX}${property.name}", value)
        }

    }

    private class PropertiesComponentStringDelegate {

        private var value: String? = null
        private var isInit = false

        operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
            if (!isInit) {
                this.value = PropertiesComponent.getInstance().getValue("${KEY_PREFIX}${property.name}")
                this.isInit = true
            }
            return value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            this.value = value
            this.isInit = true
            PropertiesComponent.getInstance().setValue("${KEY_PREFIX}${property.name}", value)
        }

    }

    private class PropertiesComponentIntDelegate(private val defaultValue: Int) {

        private var value: Int = defaultValue
        private var isInit = false

        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            if (!isInit) {
                this.value = PropertiesComponent.getInstance().getInt("${KEY_PREFIX}${property.name}", this.value)
                this.isInit = true
            }
            return value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            this.value = value
            this.isInit = true
            PropertiesComponent.getInstance().setValue("${KEY_PREFIX}${property.name}", value, defaultValue)
        }

    }

}