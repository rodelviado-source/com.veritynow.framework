package com.veritynow.core.context;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal MDC adapter to avoid compile-time dependency on SLF4J.
 */
interface MdcAdapter {
    boolean isAvailable();
    Optional<String> get(String key);
    void put(String key, String value);
    void remove(String key);
    void clear();
    Optional<Map<String, String>> getCopyOfContextMap();
    void setContextMap(Map<String, String> map);
}

/**
 * Reflection-based SLF4J MDC adapter (org.slf4j.MDC).
 */
final class ReflectionSlf4jMdcAdapter implements MdcAdapter {

    private final boolean available;

    private final Method mGet;
    private final Method mPut;
    private final Method mRemove;
    private final Method mClear;
    private final Method mGetCopy;
    private final Method mSetMap;

    ReflectionSlf4jMdcAdapter() {
        Method get = null, put = null, remove = null, clear = null, getCopy = null, setMap = null;
        boolean ok = false;
        try {
            Class<?> mdc = Class.forName("org.slf4j.MDC");
            get = mdc.getMethod("get", String.class);
            put = mdc.getMethod("put", String.class, String.class);
            remove = mdc.getMethod("remove", String.class);
            clear = mdc.getMethod("clear");

            // optional methods
            try { getCopy = mdc.getMethod("getCopyOfContextMap"); } catch (NoSuchMethodException ignored) {}
            try { setMap = mdc.getMethod("setContextMap", Map.class); } catch (NoSuchMethodException ignored) {}
            ok = true;
        } catch (Throwable t) {
            ok = false;
        }
        this.available = ok;
        this.mGet = get;
        this.mPut = put;
        this.mRemove = remove;
        this.mClear = clear;
        this.mGetCopy = getCopy;
        this.mSetMap = setMap;
    }

    @Override public boolean isAvailable() { return available; }

    @Override
    public Optional<String> get(String key) {
        if (!available) return Optional.empty();
        try {
            Object v = mGet.invoke(null, key);
            return Optional.ofNullable((String) v);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, String value) {
        if (!available) return;
        try { mPut.invoke(null, key, value); } catch (Throwable ignored) {}
    }

    @Override
    public void remove(String key) {
        if (!available) return;
        try { mRemove.invoke(null, key); } catch (Throwable ignored) {}
    }

    @Override
    public void clear() {
        if (!available) return;
        try { mClear.invoke(null); } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Map<String, String>> getCopyOfContextMap() {
        if (!available || mGetCopy == null) return Optional.empty();
        try {
            Object v = mGetCopy.invoke(null);
            return Optional.ofNullable((Map<String, String>) v);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    @Override
    public void setContextMap(Map<String, String> map) {
        if (!available || mSetMap == null) return;
        try { mSetMap.invoke(null, map); } catch (Throwable ignored) {}
    }
}
