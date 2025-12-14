package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LangUtils {
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_TYPE;
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE_TYPE;

    static {
        final var primToWrap = new LinkedHashMap<Class<?>, Class<?>>(16);
        final var wrapToPrim = new LinkedHashMap<Class<?>, Class<?>>(16);
        add(primToWrap, wrapToPrim, Boolean.TYPE, Boolean.class);
        add(primToWrap, wrapToPrim, Byte.TYPE, Byte.class);
        add(primToWrap, wrapToPrim, Character.TYPE, Character.class);
        add(primToWrap, wrapToPrim, Double.TYPE, Double.class);
        add(primToWrap, wrapToPrim, Float.TYPE, Float.class);
        add(primToWrap, wrapToPrim, Integer.TYPE, Integer.class);
        add(primToWrap, wrapToPrim, Long.TYPE, Long.class);
        add(primToWrap, wrapToPrim, Short.TYPE, Short.class);
        add(primToWrap, wrapToPrim, Void.TYPE, Void.class);
        PRIMITIVE_TO_WRAPPER_TYPE = Collections.unmodifiableMap(primToWrap);
        WRAPPER_TO_PRIMITIVE_TYPE = Collections.unmodifiableMap(wrapToPrim);
    }

    private LangUtils() {}

    /**
     * Unwraps the given wrapper type to its corresponding primitive type. If the given type is not a wrapper type, it
     * is returned as is. E.g. Integer.class is unwrapped to int.class, but String.class remains String.class.
     *
     * @param type The type to unwrap
     * @param <T> Type of the objects.
     * @return The unwrapped type, or the original type if it is not a wrapper
     * @throws NullPointerException If the given type is null
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> unwrapToPrimitive(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Type must not be null");
        }
        final var unwrapped = (Class<T>) WRAPPER_TO_PRIMITIVE_TYPE.get(type);
        return unwrapped == null ? type : unwrapped;
    }

    /**
     * Wraps the given primitive type to its corresponding wrapper type. If the given type is not a primitive type, it
     * is returned as is. E.g. int.class is wrapped to Integer.class, but String.class remains String.class.
     *
     * @param type The type to wrap
     * @param <T> Type of the objects.
     * @return The wrapped type, or the original type if it is not primitive
     * @throws NullPointerException If the given type is null
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> wrapPrimitive(Class<T> type) {
        if (type == null) {
            throw new NullPointerException("Type must not be null");
        }
        final var wrapped = (Class<T>) PRIMITIVE_TO_WRAPPER_TYPE.get(type);
        return wrapped == null ? type : wrapped;
    }

    private static void add(
            Map<Class<?>, Class<?>> forward, Map<Class<?>, Class<?>> backward, Class<?> key, Class<?> value) {
        forward.put(key, value);
        backward.put(value, key);
    }
}
