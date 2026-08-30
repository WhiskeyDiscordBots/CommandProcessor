package com.github.mirko0.commandprocessor.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflection shared by the slash-command and message-command parsers.
 *
 * <p><strong>Internal.</strong> Not part of the public API and may change in any release.
 *
 * @since 0.3.0
 */
public final class Annotations {

    private static final Logger log = LoggerFactory.getLogger(Annotations.class);

    private Annotations() {
    }

    /**
     * Finds an annotation on {@code clazz} or any of its superclasses.
     *
     * <p>Needed because a framework may hand us a generated subclass — Spring's CGLIB proxies being
     * the common case — and an annotation that is not {@code @Inherited} is invisible on it. The
     * command then registered nothing, silently. The annotations this library defines are
     * {@code @Inherited} as well; both are needed, because inheritance does not carry an annotation
     * from an interface.
     *
     * @param  clazz the class to search, upwards
     * @param  type  the annotation to find
     * @param  <A>   the annotation type
     * @return the annotation, or null
     */
    public static <A extends Annotation> A find(Class<?> clazz, Class<A> type) {
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            A found = current.getDeclaredAnnotation(type);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Every method a handler annotation could be on, subclass first, deduplicated by signature.
     *
     * <p>{@code getDeclaredMethods()} alone only sees the methods the class itself declares, so a
     * handler on a shared base class registered nothing at all and gave no indication why. Walking
     * the hierarchy and keeping the first occurrence of each signature means an override replaces
     * the method it overrides rather than being registered alongside it.
     *
     * @param  clazz the class to search, upwards
     * @return the candidate methods
     */
    public static List<Method> handlerMethods(Class<?> clazz) {
        Map<String, Method> bySignature = new LinkedHashMap<>();
        for (Class<?> type = clazz; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isSynthetic() || method.isBridge()) continue;
                bySignature.putIfAbsent(signatureOf(method), method);
            }
        }
        return new ArrayList<>(bySignature.values());
    }

    private static String signatureOf(Method method) {
        StringBuilder signature = new StringBuilder(method.getName());
        for (Class<?> parameter : method.getParameterTypes()) {
            signature.append('|').append(parameter.getName());
        }
        return signature.toString();
    }

    /**
     * Opens a handler for reflective invocation once, at registration.
     *
     * <p>Doing it per invocation repeated the module and access checks on every command a user ran,
     * and meant a class the library cannot reflect into failed in front of a user rather than at
     * startup.
     *
     * @param method the handler
     * @param origin the declaring class, for the log
     */
    public static void makeAccessible(Method method, String origin) {
        try {
            method.setAccessible(true);
        } catch (InaccessibleObjectException | SecurityException failure) {
            log.error("Cannot access handler '{}' in {}. Make the method or its module accessible, "
                    + "or the command will fail when it is used.", method.getName(), origin, failure);
        }
    }
}
