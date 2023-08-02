package com.perol.asdpl.pixivez.objects

import android.util.Log
import com.perol.asdpl.pixivez.services.PxEZApp
import io.fastkv.FastKV
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object FastKVUtil {
    val Default: FastKV = FastKV.Builder(PxEZApp.instance.filesDir.path, "fkv").build();
}

operator fun <T> FastKV.set(key: String, value: T?) {
    when (value) {
        is Boolean -> putBoolean(key, value)
        is Int -> putInt(key, value)
        is Long -> putLong(key, value)
        is Float -> putFloat(key, value)
        is Double -> putDouble(key, value)
        is String? -> putString(key, value)
        is ByteArray? -> putArray(key, value)
        else -> throw IllegalStateException("Type $key is not supported")
    }
}

class FastKVLogger : FastKV.Logger {
    override fun i(name: String, message: String) {
        Log.i(name, message)
    }

    override fun w(name: String, e: Exception) {
        Log.w(name, e)
    }

    override fun e(name: String, e: Exception) {
        Log.e(name, "FastKV error", e)
    }
}

open class KVData(name: String) {
    val kv: FastKV by lazy {
        buildKV(PxEZApp.instance.filesDir.path, name)
    }
    val flag = HashMap<String, Boolean>()

    protected open fun encoders(): Array<FastKV.Encoder<*>>? {
        return null
    }

    protected fun buildKV(path: String, name: String): FastKV {
        return FastKV.Builder(path, name)
            .encoder(encoders())
            .build()
    }

    fun contains(key: String): Boolean {
        return kv.contains(key)
    }

    fun clear() {
        kv.clear()
    }

    protected fun trilean(key: String, defValue: Trilean = Trilean.Undefined) =
        TrileanProperty(key, defValue)

    protected fun boolean(key: String, defValue: Boolean = false) = BooleanProperty(key, defValue)
    protected fun int(key: String, defValue: Int = 0) = IntProperty(key, defValue)
    protected fun float(key: String, defValue: Float = 0f) = FloatProperty(key, defValue)
    protected fun long(key: String, defValue: Long = 0L) = LongProperty(key, defValue)
    protected fun double(key: String, defValue: Double = 0.0) = DoubleProperty(key, defValue)
    protected fun string(key: String, defValue: String = "") = StringProperty(key, defValue)
    protected fun array(key: String, defValue: ByteArray = EMPTY_ARRAY) =
        ArrayProperty(key, defValue)

    protected fun stringSet(key: String, defValue: Set<String>) = StringSetProperty(key, defValue)
    protected fun <T> obj(key: String, encoder: FastKV.Encoder<T>) = ObjectProperty(key, encoder)
    protected fun <T> stringEnum(key: String, converter: StringEnumConverter<T>) =
        StringEnumProperty(key, converter)

    protected fun <T> intEnum(key: String, converter: IntEnumConverter<T>) =
        IntEnumProperty(key, converter)
    //protected fun combineKey(key: String) = CombineKeyProperty(key)

    interface BaseProperty<V> : ReadWriteProperty<KVData, V> {}
    abstract class FlagPProperty<V : Any>() : BaseProperty<V> {
        abstract val defValue: V
        var inited = false
        fun checkInitValue(thisRef: KVData, property: KProperty<*>, value: V) {
            if (!inited && (value == defValue)) return
            inited = true
        }

    }

    enum class Trilean {
        True, False, Undefined
    }

    class TrileanProperty(private val key: String, override val defValue: Trilean) :
        FlagPProperty<Trilean>() {
        val notKey = "not$key"
        override fun getValue(thisRef: KVData, property: KProperty<*>): Trilean {
            return if (thisRef.kv.getBoolean(key, false)) Trilean.True
            else if (thisRef.kv.getBoolean(notKey, false)) Trilean.False
            else Trilean.Undefined
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: Trilean) {
            when (value) {
                Trilean.True -> {
                    thisRef.kv.putBoolean(key, true)
                    thisRef.kv.putBoolean(notKey, false)
                }

                Trilean.False -> {
                    thisRef.kv.putBoolean(key, false)
                    thisRef.kv.putBoolean(notKey, true)
                }

                Trilean.Undefined -> {
                    thisRef.kv.putBoolean(key, false)
                    thisRef.kv.putBoolean(notKey, false)
                }
            }
        }
    }

    class BooleanProperty(private val key: String, override val defValue: Boolean) :
        FlagPProperty<Boolean>() {
        override fun getValue(thisRef: KVData, property: KProperty<*>): Boolean {
            return thisRef.kv.getBoolean(key, defValue)
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: Boolean) {
            checkInitValue(thisRef, property, value)
            thisRef.kv.putBoolean(key, value)
        }
    }

    class IntProperty(private val key: String, override val defValue: Int) :
        FlagPProperty<Int>() {
        override fun getValue(thisRef: KVData, property: KProperty<*>): Int {
            return thisRef.kv.getInt(key, defValue)
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: Int) {
            checkInitValue(thisRef, property, value)
            thisRef.kv.putInt(key, value)
        }
    }

    class FloatProperty(private val key: String, override val defValue: Float) :
        FlagPProperty<Float>() {
        override fun getValue(thisRef: KVData, property: KProperty<*>): Float {
            return thisRef.kv.getFloat(key, defValue)
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: Float) {
            checkInitValue(thisRef, property, value)
            thisRef.kv.putFloat(key, value)
        }
    }

    class LongProperty(private val key: String, override val defValue: Long) :
        FlagPProperty<Long>() {
        override fun getValue(thisRef: KVData, property: KProperty<*>): Long {
            return thisRef.kv.getLong(key, defValue)
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: Long) {
            checkInitValue(thisRef, property, value)
            thisRef.kv.putLong(key, value)
        }
    }

    class DoubleProperty(private val key: String, override val defValue: Double) :
        FlagPProperty<Double>() {
        override fun getValue(thisRef: KVData, property: KProperty<*>): Double {
            return thisRef.kv.getDouble(key, defValue)
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: Double) {
            checkInitValue(thisRef, property, value)
            thisRef.kv.putDouble(key, value)
        }
    }

    class StringProperty(private val key: String, override val defValue: String) :
        FlagPProperty<String>() {
        override fun getValue(thisRef: KVData, property: KProperty<*>): String {
            return thisRef.kv.getString(key, null) ?: defValue
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: String) {
            checkInitValue(thisRef, property, value)
            thisRef.kv.putString(key, value)
        }
    }

    class ArrayProperty(private val key: String, override val defValue: ByteArray) :
        FlagPProperty<ByteArray>() {
        override fun getValue(thisRef: KVData, property: KProperty<*>): ByteArray {
            return thisRef.kv.getArray(key, defValue)
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: ByteArray) {
            checkInitValue(thisRef, property, value)
            thisRef.kv.putArray(key, value)
        }
    }

    class StringSetProperty(private val key: String, override val defValue: Set<String>) :
        FlagPProperty<Set<String>>() {
        override fun getValue(thisRef: KVData, property: KProperty<*>): Set<String> {
            return thisRef.kv.getStringSet(key) ?: defValue
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: Set<String>) {
            setValue(thisRef, property, value)
            thisRef.kv.putStringSet(key, value)
        }
    }

    class ObjectProperty<T>(private val key: String, private val encoder: FastKV.Encoder<T>) :
        BaseProperty<T> {
        override fun getValue(thisRef: KVData, property: KProperty<*>): T {
            return thisRef.kv.getObject(key)
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: T) {
            thisRef.kv.putObject(key, value, encoder)
        }
    }

    class StringEnumProperty<T>(
        private val key: String,
        private val converter: StringEnumConverter<T>
    ) :
        BaseProperty<T> {
        override fun getValue(thisRef: KVData, property: KProperty<*>): T {
            return converter.stringToType(thisRef.kv.getString(key))
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: T) {
            thisRef.kv.putString(key, converter.typeToString(value))
        }
    }

    class IntEnumProperty<T>(
        private val key: String,
        private val converter: IntEnumConverter<T>
    ) :
        BaseProperty<T> {
        override fun getValue(thisRef: KVData, property: KProperty<*>): T {
            return converter.intToType(thisRef.kv.getInt(key))
        }

        override fun setValue(thisRef: KVData, property: KProperty<*>, value: T) {
            thisRef.kv.putInt(key, converter.typeToInt(value))
        }
    }

    inner class CombineKeyProperty(preKey: String) : ReadOnlyProperty<KVData, CombineKV> {
        private var combineKV = CombineKV(preKey)
        override fun getValue(thisRef: KVData, property: KProperty<*>): CombineKV {
            return combineKV
        }
    }

    inner class CombineKV(private val preKey: String) {
        private fun combineKey(key: String): String {
            return "$preKey&$key"
        }

        fun containsKey(key: String): Boolean {
            return kv.contains(combineKey(key))
        }

        fun remove(key: String) {
            kv.remove(combineKey(key))
        }

        fun putBoolean(key: String, value: Boolean) {
            kv.putBoolean(combineKey(key), value)
        }

        fun getBoolean(key: String, defValue: Boolean = false): Boolean {
            return kv.getBoolean(combineKey(key), defValue)
        }

        fun putInt(key: String, value: Int) {
            kv.putInt(combineKey(key), value)
        }

        fun getInt(key: String, defValue: Int = 0): Int {
            return kv.getInt(combineKey(key), defValue)
        }

        fun putFloat(key: String, value: Float) {
            kv.putFloat(combineKey(key), value)
        }

        fun getFloat(key: String, defValue: Float = 0f): Float {
            return kv.getFloat(combineKey(key), defValue)
        }

        fun putLong(key: String, value: Long) {
            kv.putLong(combineKey(key), value)
        }

        fun getLong(key: String, defValue: Long = 0L): Long {
            return kv.getLong(combineKey(key), defValue)
        }

        fun putDouble(key: String, value: Double) {
            kv.putDouble(combineKey(key), value)
        }

        fun getDouble(key: String, defValue: Double = 0.0): Double {
            return kv.getDouble(combineKey(key), defValue)
        }

        fun putString(key: String, value: String) {
            kv.putString(combineKey(key), value)
        }

        fun getString(key: String, defValue: String = ""): String {
            return kv.getString(combineKey(key), null) ?: defValue
        }

        fun putArray(key: String, value: ByteArray) {
            kv.putArray(combineKey(key), value)
        }

        fun getArray(key: String, defValue: ByteArray = EMPTY_ARRAY): ByteArray {
            return kv.getArray(combineKey(key), defValue)
        }

        fun <T> putObject(key: String, value: T, encoder: FastKV.Encoder<T>) {
            kv.putObject(combineKey(key), value, encoder)
        }

        fun <T> getObject(key: String): T? {
            return kv.getObject(combineKey(key))
        }
    }

    companion object {
        val EMPTY_ARRAY = ByteArray(0)
    }
}

interface StringEnumConverter<T> {
    fun stringToType(str: String?): T
    fun typeToString(type: T): String
}

interface IntEnumConverter<T> {
    fun intToType(value: Int): T
    fun typeToInt(type: T): Int
}
