package de.robv.android.xposed.callbacks;

public class XC_LoadPackage {
    public class LoadPackageParam {
        public String packageName;
        public ClassLoader classLoader;
    }
}