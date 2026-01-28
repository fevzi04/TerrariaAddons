package de.fevzi.TerrariaAddons.ui;

import com.buuz135.mhud.MultipleHUD;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Bridge class for integration with the MultipleHUD plugin.
 * MultipleHUD is treated as a hard dependency for this mod.
 */
public final class MultipleHudBridge {
    private MultipleHudBridge() {
    }

    public static void setCustomHud(Player player, PlayerRef playerRef, String key, CustomUIHud hud) {
        requireInstance().setCustomHud(player, playerRef, key, hud);
    }

    public static void hideCustomHud(Player player, PlayerRef playerRef, String key) {
        requireInstance().hideCustomHud(player, playerRef, key);
    }

    private static MultipleHUD requireInstance() {
        MultipleHUD instance = MultipleHUD.getInstance();
        if (instance == null) {
            throw new IllegalStateException("MultipleHUD is not loaded. Ensure it is installed and declared as a dependency.");
        }
        return instance;
    }
}
