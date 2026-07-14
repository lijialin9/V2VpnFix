package com.fix.v2vpn_wsafix;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class V2VpnFix {

    private static final String TAG = "V2VpnFix";
    private static final String VPN_SERVICE_CLASS = "com.wrongchao.v2vpn.service.SeTunnelVpnService";
    private static final String CONNECTOR_CLASS = "n7.b";
    private static final String HOME_FRAGMENT_CLASS = "com.wrongchao.v2vpn.ui.home.HomeFragment";
    private static final String VIEW_MODEL_CLASS = "hc.b";

    private static volatile Object vpnServiceInstance = null;
    private static volatile Object connectorInstance = null;
    private static volatile Object homeFragmentInstance = null;
    private static volatile View connectButton = null;
    private static volatile Object viewModelInstance = null;
    private static AtomicBoolean hasWindowFocus = new AtomicBoolean(false);
    private static AtomicBoolean isReconnecting = new AtomicBoolean(false);

    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.wrongchao.v2vpn".equals(lpparam.packageName)) {
            return;
        }

        ClassLoader cl = lpparam.classLoader;
        Log.e(TAG, ">>> Hook v2vpn app");

        Class<?> vpnServiceClass = cl.loadClass(VPN_SERVICE_CLASS);
        Class<?> vpnServiceApiClass = cl.loadClass("android.net.VpnService");
        Class<?> connectorClass = cl.loadClass(CONNECTOR_CLASS);
        Class<?> activityClass = cl.loadClass("android.app.Activity");
        Class<?> homeFragmentClass = cl.loadClass(HOME_FRAGMENT_CLASS);
        Class<?> viewModelClass = cl.loadClass(VIEW_MODEL_CLASS);

        // Hook onCreate - inject prepare()
        XposedHelpers.findAndHookMethod(vpnServiceClass, "onCreate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context ctx = (Context) param.thisObject;
                Log.e(TAG, "v2vpn onCreate - injecting prepare");
                try {
                    XposedHelpers.callStaticMethod(vpnServiceApiClass, "prepare", ctx);
                } catch (Exception e) {
                    Log.e(TAG, "prepare failed, trying 2-arg version", e);
                    XposedHelpers.callStaticMethod(vpnServiceApiClass, "prepare", ctx, (String) null);
                }
                Log.e(TAG, "prepare(null) injected");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                vpnServiceInstance = param.thisObject;
                Log.e(TAG, "v2vpn onCreate completed - instance saved");
                try {
                    Field oField = vpnServiceClass.getDeclaredField("O");
                    oField.setAccessible(true);
                    connectorInstance = oField.get(vpnServiceInstance);
                    Log.e(TAG, "Connector instance saved from onCreate");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get connector O from onCreate:", e);
                }
            }
        });

        // Hook onWindowFocusChanged
        XposedHelpers.findAndHookMethod(activityClass, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                boolean hasFocus = (boolean) param.args[0];
                String className = param.thisObject.getClass().getName();
                if (className.startsWith("com.wrongchao.v2vpn")) {
                    hasWindowFocus.set(hasFocus);
                    Log.e(TAG, "Activity focus changed: " + className + ", hasFocus: " + hasFocus);
                }
            }
        });

        // Hook HomeFragment.A() to get binding and connect button
        Class<?> layoutInflaterClass = cl.loadClass("android.view.LayoutInflater");
        Class<?> viewGroupClass = cl.loadClass("android.view.ViewGroup");
        XposedHelpers.findAndHookMethod(homeFragmentClass, "A", layoutInflaterClass, viewGroupClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                homeFragmentInstance = param.thisObject;
                try {
                    Field a0Field = homeFragmentClass.getDeclaredField("A0");
                    a0Field.setAccessible(true);
                    Object binding = a0Field.get(homeFragmentInstance);
                    Log.e(TAG, "Binding instance saved from HomeFragment.A()");
                    if (binding != null) {
                        Field dField = binding.getClass().getDeclaredField("d");
                        dField.setAccessible(true);
                        connectButton = (View) dField.get(binding);
                        Log.e(TAG, "Connect button saved (A0.d ImageView): " + (connectButton != null ? "success" : "null"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get binding/button from HomeFragment:", e);
                }
            }
        });

        // Hook all ViewModel constructors
        for (Constructor<?> ctor : viewModelClass.getDeclaredConstructors()) {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            Object[] args = new Object[paramTypes.length + 1];
            System.arraycopy(paramTypes, 0, args, 0, paramTypes.length);
            args[paramTypes.length] = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    viewModelInstance = param.thisObject;
                    Log.e(TAG, "ViewModel instance saved: " + viewModelInstance.getClass().getName());
                }
            };
            XposedHelpers.findAndHookConstructor(viewModelClass, args);
        }

        // Hook connector.onStateChanged
        XposedHelpers.findAndHookMethod(connectorClass, "onStateChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                connectorInstance = param.thisObject;
                boolean state = (boolean) param.args[0];

                if (state) {
                    isReconnecting.set(false);
                    Log.e(TAG, "onStateChanged(true) called - connection started");
                } else {
                    Log.e(TAG, "onStateChanged(false) called");
                    Log.e(TAG, "  hasWindowFocus: " + hasWindowFocus.get());

                    if (!hasWindowFocus.get() && !isReconnecting.get()) {
                        Log.e(TAG, "onStateChanged(false) - SYSTEM TRIGGERED, will reconnect via simulate click");
                        isReconnecting.set(true);

                        try {
                            Object service = vpnServiceInstance;
                            if (service != null) {
                                Field bField = service.getClass().getDeclaredField("B");
                                bField.setAccessible(true);
                                bField.setBoolean(service, false);
                                Log.e(TAG, "Reset state B to false");
                            }

                            // Post UI operations to main thread
                            final Object finalViewModel = viewModelInstance;
                            final View finalButton = connectButton;
                            final Object finalHomeFragment = homeFragmentInstance;
                            final Object finalService = service;
                            final ClassLoader finalCl = cl;

                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                // Step 1: Set LiveData to disconnected state first
                                boolean stateUpdated = false;
                                Log.e(TAG, "Step 1: Updating LiveData, viewModel: " + (finalViewModel != null));
                                if (finalViewModel != null) {
                                    try {
                                        Field gField = finalViewModel.getClass().getDeclaredField("g");
                                        gField.setAccessible(true);
                                        Object liveData = gField.get(finalViewModel);
                                        Log.e(TAG, "  g field retrieved, liveData: " + (liveData != null) + ", class: " + (liveData != null ? liveData.getClass().getName() : "null"));
                                        if (liveData != null) {
                                            Class<?> stateEnumClass = finalCl.loadClass("hc.n");
                                            Field xField = stateEnumClass.getDeclaredField("x");
                                            xField.setAccessible(true);
                                            Object disconnectedState = xField.get(null);
                                            Log.e(TAG, "  disconnectedState: " + (disconnectedState != null) + ", class: " + (disconnectedState != null ? disconnectedState.getClass().getName() : "null"));

                                            try {
                                                java.lang.reflect.Method kMethod = liveData.getClass().getDeclaredMethod("k", Object.class);
                                                kMethod.setAccessible(true);
                                                kMethod.invoke(liveData, disconnectedState);
                                                Log.e(TAG, "Set LiveData g to disconnected via method k() (obfuscated setValue)");
                                                stateUpdated = true;
                                            } catch (Throwable e2) {
                                                Log.e(TAG, "Method k() failed:", e2);
                                                try {
                                                    Field eField = liveData.getClass().getDeclaredField("e");
                                                    eField.setAccessible(true);
                                                    eField.set(liveData, disconnectedState);
                                                    java.lang.reflect.Method cMethod = liveData.getClass().getDeclaredMethod("c", Object.class);
                                                    cMethod.setAccessible(true);
                                                    cMethod.invoke(liveData, new Object[]{null});
                                                    Log.e(TAG, "Set LiveData g to disconnected via e field + c()");
                                                    stateUpdated = true;
                                                } catch (Throwable e3) {
                                                    Log.e(TAG, "e field + c() approach failed:", e3);
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, "  liveData is null!");
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to update LiveData:", e);
                                    }
                                } else {
                                    Log.e(TAG, "  viewModel is null!");
                                }

                                // Step 2: After state update, simulate click
                                final boolean finalStateUpdated = stateUpdated;
                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    if (finalButton != null) {
                                        Log.e(TAG, "Simulating click on connect button (A0.d), stateUpdated: " + finalStateUpdated);
                                        finalButton.performClick();
                                        Log.e(TAG, "Simulated click completed");
                                    } else {
                                        Log.e(TAG, "Cannot simulate click: connectButton is null");
                                        if (connectorInstance != null) {
                                            Log.e(TAG, "Fallback: triggering reconnection via connector.onStateChanged(true)");
                                            try {
                                                XposedHelpers.callMethod(connectorInstance, "onStateChanged", true);
                                                Log.e(TAG, "Reconnection triggered via connector");
                                            } catch (Exception e) {
                                                Log.e(TAG, "Failed to call onStateChanged(true):", e);
                                            }
                                        }
                                        if (finalService != null) {
                                            Log.e(TAG, "Fallback: restarting SeTunnelVpnService");
                                            Context ctx = (Context) finalService;
                                            Intent intent = new Intent(ctx, vpnServiceClass);
                                            ctx.startService(intent);
                                            Log.e(TAG, "Service restarted");
                                        }
                                    }
                                }, 300);
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Reconnect error:", e);
                            isReconnecting.set(false);
                        }
                    } else {
                        Log.e(TAG, "onStateChanged(false) - USER TRIGGERED, allowing disconnect");
                        isReconnecting.set(false);
                    }
                }
            }
        });

        // Hook onRevoke
        XposedHelpers.findAndHookMethod(vpnServiceClass, "onRevoke", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.e(TAG, "v2vpn onRevoke triggered!");
            }
        });
    }

    private static java.lang.reflect.Method findMethodInHierarchy(Class<?> clazz, String name, Class<?>... paramTypes) {
        while (clazz != null) {
            try {
                java.lang.reflect.Method m = clazz.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}