package de.fevzi.TerrariaAddons.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Method;

public final class MultipleHudBridge {
    private static final String[] CLASS_CANDIDATES = new String[]{
            "com.buuz135.multiplehud.MultipleHUD",
            "com.buuz135.mhud.MultipleHUD"
    };

    private static volatile boolean resolved;
    private static volatile Object instance;
    private static volatile Method setCustomHudMethod;

    private MultipleHudBridge() {
    }

    public static boolean isAvailable() {
        ensureResolved();
        return setCustomHudMethod != null;
    }

    public static boolean setCustomHud(Player player, PlayerRef playerRef, String key, CustomUIHud hud) {
        ensureResolved();
        if (setCustomHudMethod == null || instance == null) {
            return false;
        }

        try {
            setCustomHudMethod.invoke(instance, player, playerRef, key, hud);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void ensureResolved() {
        if (resolved) {
            return;
        }
        synchronized (MultipleHudBridge.class) {
            if (resolved) {
                return;
            }

            for (String className : CLASS_CANDIDATES) {
                try {
                    Class<?> clazz = Class.forName(className);
                    Method getInstance = clazz.getMethod("getInstance");
                    Object foundInstance = getInstance.invoke(null);
                    Method foundMethod = findSetCustomHud(clazz);
                    if (foundInstance != null && foundMethod != null) {
                        instance = foundInstance;
                        setCustomHudMethod = foundMethod;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }

            resolved = true;
        }
    }

    private static Method findSetCustomHud(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (!"setCustomHud".equals(method.getName())) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 4) {
                continue;
            }
            if (!String.class.equals(params[2])) {
                continue;
            }
            return method;
        }
        return null;
    }
}