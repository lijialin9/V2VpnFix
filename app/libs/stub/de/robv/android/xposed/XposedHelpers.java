package de.robv.android.xposed;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public final class XposedHelpers {
    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        return null;
    }

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        return null;
    }

    public static Object getObjectField(Object obj, String fieldName) throws NoSuchFieldException {
        return null;
    }

    public static void setObjectField(Object obj, String fieldName, Object value) throws NoSuchFieldException {
    }

    public static int getIntField(Object obj, String fieldName) throws NoSuchFieldException {
        return 0;
    }

    public static void setIntField(Object obj, String fieldName, int value) throws NoSuchFieldException {
    }

    public static boolean getBooleanField(Object obj, String fieldName) throws NoSuchFieldException {
        return false;
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) throws NoSuchFieldException {
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Object... parameterTypes) throws NoSuchMethodException {
        return null;
    }

    public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) throws NoSuchMethodException {
        return null;
    }

    public static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        return null;
    }
}