package de.robv.android.xposed;

public class XposedHelpers {
    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        return null;
    }
    public static XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        return null;
    }
    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return null;
    }
    public static Object callMethod(Object obj, String methodName, Object... args) {
        return null;
    }
    public static Object callMethod(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
        return null;
    }
    public static Object getObjectField(Object obj, String fieldName) {
        return null;
    }
    public static void setObjectField(Object obj, String fieldName, Object value) {}
    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        return null;
    }
}