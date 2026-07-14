package de.robv.android.xposed;

public abstract class XC_MethodHook implements IXposedHookCallback {
    public static class MethodHookParam {
        public Object thisObject;
        public Object[] args;
        public Object result;
    }

    public static class Unhook {
        public void unhook() {}
    }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
}