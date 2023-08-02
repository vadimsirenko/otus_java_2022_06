package ru.otus.cachehw;


import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class MyCache<K, V> implements HwCache<K, V> {

    private final WeakHashMap<K, V> innerCache;
    private final List<HwListener<K, V>> listeners;

    public MyCache() {
        this.innerCache = new WeakHashMap<>();
        this.listeners = new ArrayList<>();
    }
//Надо реализовать эти методы

    @Override
    public void put(K key, V value) {
        listeners.forEach(l->l.notify(key, value, "put"));
        if(!innerCache.containsKey(key)) {
            innerCache.put(key, value);
        }else{
            innerCache.replace(key, value);
        }
    }

    @Override
    public void remove(K key) {
        listeners.forEach(l->l.notify(key, innerCache.get(key), "remove"));
        if(innerCache.containsKey(key)) {
            innerCache.remove(key);

        }
    }

    @Override
    public void clear() {
        innerCache.clear();
    }

    @Override
    public V get(K key) {
        listeners.forEach(l->l.notify(key, innerCache.get(key), "get"));
        if(innerCache.containsKey(key)) {
            return innerCache.get(key);
        }
        return null;
    }

    @Override
    public void addListener(HwListener<K, V> listener) {
        if(!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(HwListener<K, V> listener) {
        if(listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }
}
