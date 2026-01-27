package de.fevzi.TerrariaAddons.items.LavaCharm;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Custom HUD element that displays the Lava Charm immunity bar.
 * Shows a progress bar indicating remaining lava immunity duration,
 * automatically hiding when not in lava.
 */
public class LavaCharmHud extends CustomUIHud {
    private boolean visible = false;
    private float progress = 1.0f;
    private boolean built = false;

    public LavaCharmHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    public void setVisible(boolean visible) {
        if (visible == this.visible) {
            return;
        }

        this.visible = visible;
        if (!built) {
            return;
        }
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#LavaCharmContainer.Visible", visible);
        try {
            update(false, builder);
        } catch (Exception ignored) {
        }
    }

    public void setProgress(float progress) {
        if (Math.abs(progress - this.progress) < 0.001f) {
            return;
        }

        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        if (!built) {
            return;
        }
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#LavaCharmBar.Value", this.progress);
        try {
            update(false, builder);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void build(UICommandBuilder commandBuilder) {
        commandBuilder.append("Hud/LavaCharm/LavaCharmHud.ui");
        commandBuilder.set("#LavaCharmContainer.Visible", visible);
        commandBuilder.set("#LavaCharmBar.Value", progress);
        built = true;
    }
}
