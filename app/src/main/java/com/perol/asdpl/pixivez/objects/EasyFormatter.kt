/*
 * Copyright (C) 2018 Haoge https://github.com/yjfnypeu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.perol.asdpl.pixivez.objects

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import java.io.StringWriter
import java.lang.reflect.Modifier
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * 对数据进行格式化
 *
 * @author haoge on 2018/5/10
 */
class EasyFormatter private constructor(private val builder: Builder) {

    private val indent = "    "
    private val list: MutableList<Any> = mutableListOf()   // 用于临时存放当前已被解析的类。防止出现循环引用导致栈溢出

    fun formatWithArgs(message: String, vararg args: Any): String {
        val array = arrayOfNulls<String>(args.size)
        args.forEachIndexed { index, any -> array[index] = format(any) }
        return if (array.filterNotNull().isEmpty()) {
            message
        }
        else {
            String.format("$message %s", *array)
        }
    }

    /**
     * 格式化入口
     */
    fun format(any: Any?): String {
        val format = formatAny(any)
        list.clear()
        val lines = format.lines()
        return if (isFlat(builder.maxLines, lines.size)) {
            // 总长度大于受限长度。需要进行平铺处理
            val result = StringBuilder()
            for ((index, value) in lines.withIndex()) {
                if (index < builder.maxLines - 1) {
                    result.append(value)
                    result.append("\n")
                }
                else {
                    result.append(value.trimIndent())
                }
            }
            result.toString()
        }
        else {
            format.toString()
        }
    }

    // 格式化List/Set集合数据
    private fun formatCollection(collection: Collection<*>): StringBuilder {
        val result = StringBuilder("[")
        val isFlat = isFlat(builder.maxArraySize, collection.size)
        appendIterator(result, collection.iterator(), isFlat)
        result.append("]")
        return result
    }

    // 格式化map数据
    private fun formatMap(map: Map<*, *>): StringBuilder {
        val result = StringBuilder("{")
        val isFlat = isFlat(builder.maxMapSize, map.size)
        appendIterator(result, map.iterator(), isFlat)
        result.append("}")
        return result
    }

    private fun formatString(data: String): StringBuilder {
        if (data.startsWith("{") &&
            data.endsWith("}")
        ) {
            return formatJSONObject(data)
        }
        else if (data.startsWith("[") &&
            data.endsWith("]")
        ) {
            return formatJSONArray(data)
        }
        else if (data.startsWith("<") &&
            data.endsWith(">")
        ) {
            return formatXML(data)
        }

        return StringBuilder(data)
    }

    private fun formatXML(data: String): StringBuilder {
        return try {
            val xmlInput = StreamSource(StringReader(data))
            val xmlOutput = StreamResult(StringWriter())
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            transformer.transform(xmlInput, xmlOutput)
            val output = xmlOutput.writer.toString().replaceFirst(">".toRegex(), ">\n")
            val lines = output.lines()
            val isFlat = isFlat(builder.maxMapSize, lines.size)
            val result = StringBuilder()
            if (isFlat.not()) {
                result.append(output)
            }
            else {
                lines.forEach { result.append(it.trimIndent()) }
            }
            result
        } catch (e: TransformerException) {
            StringBuilder(data)
        }
    }

    private fun formatJSONArray(data: String): StringBuilder {
        val result = StringBuilder("[")
        try {
            val array = JSONArray(data)
            val length = array.length()
            val isFlat = isFlat(builder.maxArraySize, length)

            for (index in 0 until length) {
                val sub = StringBuilder()
                if (!isFlat) {
                    result.append("\n")
                }
                sub.append(formatString(array.optString(index)))
                if (index != length - 1) {
                    sub.append(",")
                }
                appendSubString(result, sub, isFlat)
            }
            if (!isFlat) {
                result.append("\n")
            }
        } catch (e: Exception) {
            return StringBuilder(data)
        }
        result.append("]")
        return result
    }

    private fun formatJSONObject(data: String): StringBuilder {
        val result = StringBuilder("{")
        try {
            val json = JSONObject(data)
            val length = json.length()
            val keys = json.keys()
            val isFlat = isFlat(builder.maxMapSize, length)
            var hasNext = keys.hasNext()
            while (hasNext) {
                if (!isFlat) {
                    result.append("\n")
                }

                val sub = StringBuilder()
                val next = keys.next()
                sub.append(formatString(next)).append(":").append(json.optString(next))

                hasNext = keys.hasNext()
                if (hasNext) {
                    sub.append(", ")
                }
                appendSubString(result, sub, isFlat)
            }
            if (!isFlat) {
                result.append("\n")
            }
        } catch (e: Exception) {
            return StringBuilder(data)
        }
        result.append("}")
        return result
    }

    private fun isFlat(maxSize: Int, length: Int): Boolean {
        return when {
            maxSize < 0 -> false
            length <= maxSize -> false
            else -> true
        }
    }

    private fun formatException(any: Throwable): StringBuilder {
        return StringBuilder(Log.getStackTraceString(any))
    }

    private fun formatOther(any: Any): StringBuilder {
        val name = any.javaClass.canonicalName ?: ""
        if (name.startsWith("android") ||
            name.startsWith("java") ||
            name.startsWith("javax") ||
            name.startsWith("kotlin")
        ) {
            // 不对系统提供的类进行格式化
            return StringBuilder(any.toString())
        }

        val result = StringBuilder("[${any.javaClass.simpleName}]{")
        val container = mutableMapOf<String, Any>()
        scanFields(any, any.javaClass, container)

        val isFlat = isFlat(builder.maxMapSize, container.size)
        appendIterator(result, container.iterator(), isFlat)

        result.append("}")
        return result
    }

    private fun formatAny(any: Any?): StringBuilder =
        when (any) {
            null -> StringBuilder()
            is Collection<*> -> checkIfFormatted(any) { return@checkIfFormatted formatCollection(any) }
            is Map<*, *> -> checkIfFormatted(any) { return@checkIfFormatted formatMap(any) }
            is Array<*> -> checkIfFormatted(any) { return@checkIfFormatted formatCollection(listOf(*any)) }
            is String -> formatString(any)
            is Throwable -> formatException(any)
            is Int, is Boolean, is Short, is Char, is Byte, is Long, is Float, is Double
            -> StringBuilder(any.toString())
            else -> checkIfFormatted(any) { return@checkIfFormatted formatOther(any) }
        }

    private fun appendIterator(
        container: StringBuilder, /*数据存储容器*/
        iterator: Iterator<*>,
        isFlat: Boolean
    ) {
        var hasNext = iterator.hasNext()
        while (hasNext) {
            if (!isFlat) {
                container.append("\n")
            }

            val sub = StringBuilder()
            val next = iterator.next()
            if (next is Map.Entry<*, *>) {
                sub.append(formatAny(next.key))
                    .append(":")
                    .append(formatAny(next.value))
            }
            else {
                sub.append(formatAny(next))
            }

            hasNext = iterator.hasNext()
            if (hasNext) {
                sub.append(", ")
            }
            appendSubString(container, sub, isFlat)
        }
        if (!isFlat) {
            container.append("\n")
        }
    }

    private fun appendSubString(container: StringBuilder, subString: StringBuilder, isFlat: Boolean) {
        val lines = subString.lines()
        for ((index, value) in lines.withIndex()) {
            if (index == 0) {
                if (!isFlat) container.append(indent)
                container.append(value)
                if (lines.size > 1) container.append("\n")
            }
            else if (value.isNotEmpty()) {
                container.append(indent).append(value)
                    .append(if (index == lines.size - 1) "" else "\n")
            }
        }
    }

    private fun scanFields(any: Any, clazz: Class<*>, container: MutableMap<String, Any>) {
        val name = clazz.canonicalName ?: ""
        if (name.startsWith("android") ||
            name.startsWith("java") ||
            name.startsWith("javax") ||
            name.startsWith("kotlin")
        ) {
            // 不对系统提供的类进行格式化
            return
        }

        for (field in clazz.declaredFields) {
            val modifiers = field.modifiers
            if (Modifier.isNative(modifiers)) {
                continue
            }

            if (!field.isAccessible) {
                field.isAccessible = true
            }

            val value = field.get(any) ?: continue

            container[field.name] = value
        }
        scanFields(any, clazz.superclass, container)
    }

    private fun checkIfFormatted(any: Any, invoke: () -> StringBuilder): StringBuilder {
        return if (list.contains(any)) {
            StringBuilder("{(circle ref):${any.javaClass.simpleName}}")
        }
        else {
            list.add(any)
            invoke.invoke()
        }
    }

    companion object {
        @JvmStatic
        val DEFAULT by lazy { return@lazy Builder().build() }

        @JvmStatic
        fun newBuilder(): Builder {
            return Builder()
        }
    }

    class Builder internal constructor() {

        /**
         * 最大行数，当格式化后的数据行数超过此数量后，将对超出部分数据进行平铺展示
         */
        var maxLines: Int = -1

        /**
         * 最大Array尺寸，当Array(包括List/Set/Array/JSONArray)的长度超过此数量限制时：数据以平铺模式展示。
         */
        var maxArraySize: Int = -1

        /**
         * 最大Map尺寸，当Map(包括Map/JSONObject/Bean)的长度超过此数量限制时：数据以平铺模式展示
         */
        var maxMapSize: Int = -1

        fun build(): EasyFormatter {
            return EasyFormatter(this)
        }
    }
}
