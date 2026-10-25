package io.github.allbegray.pebbleforge.sandbox;

import io.pebbletemplates.pebble.attributes.methodaccess.MethodAccessValidator;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Blocks methods that can reach the process, the filesystem, class loading, or reflection.
 */
public final class SandboxMethodAccessValidator implements MethodAccessValidator {

    /**
     * Creates a validator with the built-in deny lists.
     */
    public SandboxMethodAccessValidator() {
    }

    private static final Set<String> DENIED_PACKAGES = Set.of(
            "java.io.",
            "java.nio.file.",
            "java.net.",
            "java.lang.reflect.",
            "java.lang.invoke.",
            "java.lang.management.",
            "java.util.concurrent.",
            "javax.script.",
            "javax.naming.",
            "javax.management.",
            "javax.tools.",
            "jdk.internal.",
            "sun.",
            "com.sun.");

    private static final Set<String> DENIED_TYPES = Set.of(
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.Runtime",
            "java.lang.Process",
            "java.lang.ProcessBuilder",
            "java.lang.System",
            "java.lang.Thread",
            "java.lang.ThreadGroup",
            "java.lang.reflect.AccessibleObject");

    private static final Set<String> DENIED_METHODS = Set.of(
            "getClass",
            "wait",
            "notify",
            "notifyAll",
            "clone",
            "finalize",
            "exec",
            "load",
            "loadLibrary",
            "exit",
            "halt",
            "getRuntime",
            "forName",
            "newInstance",
            "invoke",
            "setAccessible",
            "readAllBytes",
            "readAllLines",
            "transferTo",
            "delete",
            "deleteOnExit",
            "start",
            "stop",
            "suspend",
            "resume",
            "setSecurityManager",
            "getSecurityManager",
            "getSystemClassLoader",
            "getContextClassLoader",
            "getProtectionDomain",
            "getResource",
            "getResourceAsStream",
            "getClassLoader",
            "defineClass",
            "findClass",
            "getDeclaredMethod",
            "getDeclaredField",
            "getDeclaredConstructor",
            "getMethod",
            "getField",
            "getConstructor");

    @Override
    public boolean isMethodAccessAllowed(Object object, Method method) {
        if (object == null || method == null) {
            return false;
        }
        if (isDeniedObject(object) || isDeniedType(object.getClass())) {
            return false;
        }
        if (isDeniedType(method.getDeclaringClass())) {
            return false;
        }
        if (DENIED_METHODS.contains(method.getName())) {
            return false;
        }
        if (isDeniedType(method.getReturnType())) {
            return false;
        }
        for (Class<?> parameterType : method.getParameterTypes()) {
            if (isDeniedType(parameterType)) {
                return false;
            }
        }
        return !isDeniedPackage(method.getDeclaringClass().getName());
    }

    static boolean isDeniedObject(Object object) {
        return object instanceof Class<?>
                || object instanceof ClassLoader
                || object instanceof Runtime
                || object instanceof Process
                || object instanceof ProcessBuilder
                || object instanceof Thread
                || object instanceof ThreadGroup
                || object instanceof System
                || object instanceof java.lang.reflect.AccessibleObject;
    }

    private static boolean isDeniedType(Class<?> type) {
        if (type == null || type.isPrimitive()) {
            return false;
        }
        if (type.isArray()) {
            return isDeniedType(type.getComponentType());
        }
        if (DENIED_TYPES.contains(type.getName())) {
            return true;
        }
        return isDeniedPackage(type.getName());
    }

    private static boolean isDeniedPackage(String className) {
        for (String prefix : DENIED_PACKAGES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
