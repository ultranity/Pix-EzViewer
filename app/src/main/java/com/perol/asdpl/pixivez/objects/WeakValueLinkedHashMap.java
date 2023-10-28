package com.perol.asdpl.pixivez.objects;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Similar with WeakValueHashMap, This implementation of
 * a LinkedHashMap uses a {@link WeakReference} to the value objects.
 * Once the garbage collector decides it wants to finalize a value object,
 * it will be removed from the map automatically.
 * !! Note that entries and values will be sorted !!reversely!! as they added.
 *
 * @param <K> - the type of the key object
 * @param <V> - the type of the value object
 */
public class WeakValueLinkedHashMap<K, V> extends AbstractMap<K, V> {

    // the internal hash map to the weak references of the actual value objects
    protected HashMap<K, WeakLinkedValue> references;
    // the garbage collector's removal queue
    protected ReferenceQueue<V> gcQueue;

    private WeakLinkedValue headRef = new WeakLinkedValue(null, null, null);
    private WeakLinkedValue tailRef = headRef;

    /**
     * Creates a WeakValueHashMap with a desired initial capacity
     *
     * @param capacity - the initial capacity
     */
    public WeakValueLinkedHashMap(int capacity) {
        references = new HashMap<>(capacity);
        gcQueue = new ReferenceQueue<>();
    }

    /**
     * Creates a WeakValueLinkedHashMap with an initial capacity of 1
     */
    public WeakValueLinkedHashMap() {
        this(1);
    }

    /**
     * Creates a WeakValueLinkedHashMap and copies the content from an existing map
     *
     * @param map - the map to copy from
     */
    public WeakValueLinkedHashMap(Map<? extends K, ? extends V> map) {
        this(map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V put(K key, V value) {
        processQueue();
        WeakLinkedValue valueRef = new WeakLinkedValue(key, value, gcQueue, tailRef);
        return getReferenceValue(references.put(key, valueRef));
    }

    ;

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
        for (Map.Entry<K, WeakLinkedValue> entry : references.entrySet()) {
            if (value == getReferenceValue(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

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

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        processQueue();

        Set<Map.Entry<K, V>> entries = new LinkedHashSet<>();
        WeakLinkedValue entry = tailRef;
        while (entry != null) {
            entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.get()));
            entry = entry.beforeRef;
        }
        return entries;
    }

    public Collection<V> values() {
        processQueue();

        Collection<V> values = new ArrayList<>();
        WeakLinkedValue entry = tailRef;
        while (entry != null) {
            if (entry.get() != null) {
                values.add(entry.get());
            }
            entry = entry.beforeRef;
        }
        return values;
    }

    private V getReferenceValue(WeakLinkedValue valueRef) {
        return valueRef == null ? null : valueRef.get();
    }

    // remove entries once their value is scheduled for removal by the garbage collector
    @SuppressWarnings("unchecked")
    public void processQueue() {
        WeakLinkedValue valueRef;
        while ((valueRef = (WeakLinkedValue) gcQueue.poll()) != null) {
            valueRef.detach();
            references.remove(valueRef.getKey());
        }
    }

    // for faster removal in {@link #processQueue()} we need to keep track of the key for a value
    class WeakLinkedValue extends WeakReference<V> {
        public WeakLinkedValue beforeRef;
        public WeakLinkedValue afterRef;
        final K key;

        private WeakLinkedValue(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }

        private WeakLinkedValue(K key, V value, ReferenceQueue<V> queue, WeakLinkedValue beforeRef) {
            super(value, queue);
            this.key = key;
            this.beforeRef = beforeRef;
            beforeRef.afterRef = this;
            tailRef = this;
        }

        private K getKey() {
            return key;
        }

        private void detach() {
            beforeRef.afterRef = afterRef;
            if (afterRef != null) {
                afterRef.beforeRef = beforeRef;
            }
            afterRef = beforeRef = null;
        }
    }
}