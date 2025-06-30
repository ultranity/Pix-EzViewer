package com.perol.asdpl.pixivez.objects;

import androidx.annotation.NonNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

// inspired by http://www.java2s.com/Code/Java/Collections-Data-Structure/WeakValueHashMap.htm
// implementation from https://github.com/hzulla/WeakValueHashMap/blob/master/WeakValueHashMap.java


// for faster removal in {@link #processQueue()} we need to keep track of the key for a value
class WeakValue<K, T> extends WeakReference<T> {
    final K key;

    protected WeakValue(K key, T value, ReferenceQueue<T> queue) {
        super(value, queue);
        this.key = key;
    }

    public K getKey() {
        return key;
    }
}
/**
 * The desired behaviour of an in-memory cache is to keep a weak reference to the cached object,
 * this will allow the garbage collector to remove an object from memory once it isn't needed
 * anymore.
 * <p>
 * A {@link HashMap} doesn't help here since it will keep hard references for key and
 * value objects. A {@link WeakHashMap} doesn't either, because it keeps weak references to the
 * key objects, but we want to track the value objects.
 * <p>
 * This implementation of a Map uses a {@link WeakReference} to the value objects. Once the
 * garbage collector decides it wants to finalize a value object, it will be removed from the
 * map automatically.
 *
 * @param <K> - the type of the key object
 * @param <V> - the type of the value object
 */
public class WeakValueHashMap<K, V> extends AbstractMap<K, V> {

    // the internal hash map to the weak references of the actual value objects
    protected final HashMap<K, WeakValue<K, V>> references;
    // the garbage collector's removal queue
    protected final ReferenceQueue<V> gcQueue;


    /**
     * Creates a WeakValueHashMap with a desired initial capacity
     *
     * @param capacity - the initial capacity
     */
    public WeakValueHashMap(int capacity) {
        references = new HashMap<>(capacity);
        gcQueue = new ReferenceQueue<>();
    }

    /**
     * Creates a WeakValueHashMap with an initial capacity of 1
     */
    public WeakValueHashMap() {
        this(1);
    }

    /**
     * Creates a WeakValueHashMap and copies the content from an existing map
     *
     * @param map - the map to copy from
     */
    public WeakValueHashMap(Map<? extends K, ? extends V> map) {
        this(map.size());
        putAll(map);
    }

    @Override
    public V put(K key, V value) {
        processQueue();
        WeakValue<K, V> valueRef = new WeakValue<>(key, value, gcQueue);
        return getReferenceValue(references.put(key, valueRef));
    }

    @Override
    public V get(Object key) {
        processQueue();
        return getReferenceValue(references.get(key));
    }

    @Override
    public V remove(Object key) {
        return getReferenceValue(references.get(key));
    }

    @Override
    public void clear() {
        references.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        processQueue();
        return references.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        processQueue();
        for (Map.Entry<K, WeakValue<K, V>> entry : references.entrySet()) {
            if (value == getReferenceValue(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        processQueue();
        return references.keySet();
    }

    @Override
    public int size() {
        processQueue();
        return references.size();
    }

    @NonNull
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        processQueue();

        Set<Map.Entry<K, V>> entries = new LinkedHashSet<>();
        for (Map.Entry<K, WeakValue<K, V>> entry : references.entrySet()) {
            entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), getReferenceValue(entry.getValue())));
        }
        return entries;
    }

    @NonNull
    public Collection<V> values() {
        processQueue();

        Collection<V> values = new ArrayList<>();
        for (WeakValue<K, V> valueRef : references.values()) {
            values.add(getReferenceValue(valueRef));
        }
        return values;
    }

    private V getReferenceValue(WeakValue<K, V> valueRef) {
        return valueRef == null ? null : valueRef.get();
    }

    // remove entries once their value is scheduled for removal by the garbage collector
    @SuppressWarnings("unchecked")
    public void processQueue() {
        WeakValue<K, V> valueRef;
        while ((valueRef = (WeakValue<K, V>) gcQueue.poll()) != null) {
            references.remove(valueRef.getKey());
        }
    }
}