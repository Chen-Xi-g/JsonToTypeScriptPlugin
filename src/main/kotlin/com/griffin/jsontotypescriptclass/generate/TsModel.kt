package com.griffin.jsontotypescriptclass.generate

import com.google.gson.*
import java.util.*

object TsModel {

    private val code = mutableMapOf<String, String>()

    fun jsonToTypeScriptClass(json: String, className: String): String {
        code.clear()
        val jsonElement = Gson().fromJson(json, JsonElement::class.java)
        val content = StringBuilder()
        content.append(generateModelClass(className, jsonElement))
        content.append("\n\n")
        content.append(code.values.joinToString("\n\n"))
        return content.toString()
    }

    private fun generateModelClass(
            className: String,
            jsonElement: JsonElement
    ): String {
        val fieldsText = when {
            jsonElement.isJsonObject -> {
                val jsonObject = jsonElement.asJsonObject
                jsonObject.entrySet().joinToString("\n") { (fieldName, fieldElement) ->
                    val fieldType = getFieldType(fieldElement, fieldName)
                    // 判断是否为第一个元素，如果是，则前面不需要空格占位符
                    if (jsonObject.entrySet().first().key == fieldName) {
                        "$fieldName: $fieldType;"
                    } else {
                        "    $fieldName: $fieldType;"
                    }
                }
            }

            jsonElement.isJsonArray -> {
                val jsonArray = jsonElement.asJsonArray
                val firstElement = jsonArray.firstOrNull()
                if (firstElement != null && firstElement.isJsonObject) {
                    val nestedClassText = generateModelClass(className, firstElement)
                    return """
export class $className {
    $nestedClassText[];
}
                """.trimIndent()
                } else {
                    // Array of basic types, use 'any[]'
                    return """
export class $className {
    items: any[];
}
                """.trimIndent()
                }
            }

            else -> {
                // Unable to determine the structure, use 'any'
                "    // Unable to determine the structure of the JSON. Use 'any'."
            }
        }

        return """
export class $className {
    $fieldsText
}
    """.trimIndent()
    }

    private fun getFieldType(jsonElement: JsonElement, fieldName: String): String {
        return when {
            jsonElement.isJsonNull -> "any"
            jsonElement.isJsonPrimitive -> {
                when {
                    jsonElement.asJsonPrimitive.isBoolean -> "boolean"
                    jsonElement.asJsonPrimitive.isNumber -> "number"
                    else -> "string"
                }
            }

            jsonElement.isJsonObject -> {
                val nestedClassName = fieldName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val nestedClassText = generateModelClass(nestedClassName, jsonElement)
                code[nestedClassName] = nestedClassText
                nestedClassName
            }

            jsonElement.isJsonArray -> {
                val arrayElement = jsonElement.asJsonArray.firstOrNull()
                if (arrayElement != null && arrayElement.isJsonObject) {
                    val nestedClassName = fieldName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    val nestedClassText = generateModelClass(nestedClassName, arrayElement)
                    code[nestedClassName] = nestedClassText
                    "$nestedClassName[]"
                } else if (arrayElement != null && arrayElement.isJsonPrimitive) {
                    val primitiveType = when {
                        arrayElement.asJsonPrimitive.isBoolean -> "boolean"
                        arrayElement.asJsonPrimitive.isNumber -> "number"
                        else -> "string"
                    }
                    "$primitiveType[]"
                } else if (arrayElement != null && arrayElement.isJsonArray) {
                    val nestedClassName = fieldName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    arrayElement.asJsonArray.firstOrNull()?.let {
                        if (it.isJsonPrimitive){
                            return when {
                                it.asJsonPrimitive.isBoolean -> "boolean[]"
                                it.asJsonPrimitive.isNumber -> "number[]"
                                it.asJsonPrimitive.isString -> "string[]"
                                else -> "any[]"
                            }
                        }
                    }
                    val nestedClassText = generateModelClass(nestedClassName, arrayElement)
                    code[nestedClassName] = nestedClassText
                    "$nestedClassName[]"
                } else {
                    "any[]"
                }
            }

            else -> "any"
        }
    }

}