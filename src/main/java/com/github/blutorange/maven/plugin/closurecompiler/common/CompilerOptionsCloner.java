package com.github.blutorange.maven.plugin.closurecompiler.common;

import com.google.common.primitives.Primitives;
import com.google.javascript.jscomp.CompilerOptions;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

final class CompilerOptionsCloner {
    public static CompilerOptions cloneCompilerOptions(CompilerOptions opts) {
        // This used to be
        // SerializationUtils.clone(this.compilerOptions);
        // But they remove Serializable from CompilerOptions, with no alternative...

        final var clone = new CompilerOptions();

        for (final var field : FieldUtils.getAllFields(CompilerOptions.class)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                final var originalValue = field.get(opts);
                if (Modifier.isFinal(field.getModifiers())) {
                    final var cloneValue = field.get(clone);
                    copyFieldValueContent(originalValue, cloneValue);
                } else {
                    final var clonedValue = cloneValue(originalValue);
                    field.set(clone, clonedValue);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return clone;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copyFieldValueContent(Object from, Object to) {
        if (from instanceof Collection<?> fromColl && to instanceof Collection<?> toColl) {
            toColl.addAll((Collection) fromColl);
        } else if (from instanceof Map<?, ?> fromMap && to instanceof Map<?, ?> toMap) {
            toMap.putAll((Map) fromMap);
        } else {
            throw new IllegalStateException("Cannot copy field value content from "
                    + from.getClass().getName() + " to " + to.getClass().getName());
        }
    }

    private static Object cloneValue(Object val) {
        if (val == null) {
            return null;
        }
        final var type = val.getClass();
        if (Primitives.unwrap(type).isPrimitive() || val instanceof Path) {
            return val;
        }
        if (val instanceof Serializable serializable) {
            return SerializationUtils.clone(serializable);
        }
        throw new IllegalStateException("Cannot clone value of type " + type.getName());
    }
}
