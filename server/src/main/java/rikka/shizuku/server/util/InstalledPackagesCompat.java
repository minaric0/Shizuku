package rikka.shizuku.server.util;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public final class InstalledPackagesCompat {

    private static final String TAG = "InstalledPackagesCompat";
    private static final int ANDROID_13 = 33;
    private static final String PARCELED_LIST_SLICE = "android.content.pm.ParceledListSlice";

    private InstalledPackagesCompat() {
    }

    public static List<PackageInfo> getInstalledPackagesNoThrow(long flags, int userId) {
        if (Build.VERSION.SDK_INT < 37) {
            return rikka.hidden.compat.PackageManagerApis.getInstalledPackagesNoThrow(flags, userId);
        }
        try {
            List<PackageInfo> packages = getInstalledPackages(flags, userId);
            return packages == null ? Collections.emptyList() : packages;
        } catch (Throwable e) {
            Log.w(TAG, "getInstalledPackages failed", e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<PackageInfo> getInstalledPackages(long flags, int userId) throws ReflectiveOperationException {
        try {
            Object packageManager = getContextPackageManager();
            Method method = packageManager.getClass().getMethod("getInstalledPackagesAsUser", int.class, int.class);
            Object result = invoke(method, packageManager, (int) flags, userId);
            return result == null ? Collections.emptyList() : (List<PackageInfo>) result;
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            Log.d(TAG, "getInstalledPackagesAsUser failed, falling back to hidden API", e);
        }

        Object packageManager = getPackageManager();
        Method method;
        Object result;

        if (Build.VERSION.SDK_INT >= 37) {
            method = findInstalledPackagesMethod(packageManager);
            Class<?>[] params = method.getParameterTypes();
            Object[] args = new Object[params.length];
            args[0] = params[0] == long.class ? flags : (int) flags;
            for (int i = 1; i < params.length - 1; i++) args[i] = 0; // deviceId
            args[params.length - 1] = userId;
            result = invoke(method, packageManager, args);
        } else if (Build.VERSION.SDK_INT >= ANDROID_13) {
            method = packageManager.getClass().getMethod("getInstalledPackages", long.class, int.class);
            result = invoke(method, packageManager, flags, userId);
        } else {
            method = packageManager.getClass().getMethod("getInstalledPackages", int.class, int.class);
            result = invoke(method, packageManager, (int) flags, userId);
        }

        if (result == null) {
            return Collections.emptyList();
        }

        String resultClassName = result.getClass().getName();
        if (resultClassName.startsWith(PARCELED_LIST_SLICE) || resultClassName.contains("PackageInfoList")) {
            Object list = result.getClass().getMethod("getList").invoke(result);
            return list == null ? Collections.emptyList() : (List<PackageInfo>) list;
        }

        throw new IllegalStateException("Unsupported getInstalledPackages return type: " + resultClassName);
    }

    private static Method findInstalledPackagesMethod(Object pm) throws NoSuchMethodException {
        for (Method m : pm.getClass().getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getName().equals("getInstalledPackages") && p.length >= 2
                    && (p[0] == int.class || p[0] == long.class)
                    && p[p.length - 1] == int.class) {
                boolean valid = true;
                for (int i = 1; i < p.length; i++) valid &= p[i] == int.class;
                if (valid) return m;
            }
        }
        throw new NoSuchMethodException("IPackageManager.getInstalledPackages");
    }

    private static Object getPackageManager() throws ReflectiveOperationException {
        Class<?> servicesClass = Class.forName("rikka.hidden.compat.Services");
        var field = servicesClass.getDeclaredField("packageManager");
        field.setAccessible(true);
        Object service = field.get(null);
        return service.getClass().getMethod("get").invoke(service);
    }

    private static Object getContextPackageManager() throws ReflectiveOperationException {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        if (activityThread != null) {
            Object application = activityThreadClass.getMethod("getApplication").invoke(activityThread);
            if (application != null) {
                return application.getClass().getMethod("getPackageManager").invoke(application);
            }
        }

        activityThread = activityThreadClass.getMethod("systemMain").invoke(null);
        Object systemContext = activityThreadClass.getMethod("getSystemContext").invoke(activityThread);
        return systemContext.getClass().getMethod("getPackageManager").invoke(systemContext);
    }

    private static Object invoke(Method method, Object receiver, Object... args) throws ReflectiveOperationException {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ReflectiveOperationException) {
                throw (ReflectiveOperationException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }
    }
}
