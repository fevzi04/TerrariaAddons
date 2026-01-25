package de.fevzi.TerrariaAddons.dps;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class DpsHud extends CustomUIHud {
    private String text = "DPS: 0.0";
    private boolean visible = true;
    private boolean built = false;

    public DpsHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }

    public void setText(@Nonnull String text) {
        if (text.equals(this.text)) {
            return;
        }

        this.text = text;
        if (!built) {
            return;
        }
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#DpsLabel.Text", text);
        try {
            update(false, builder);
        } catch (Exception ignored) {
        }
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
        builder.set("#DpsBox.Visible", visible);
        try {
            update(false, builder);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void build(UICommandBuilder commandBuilder) {
        commandBuilder.append("Hud/DpsHud.ui");
        commandBuilder.set("#DpsLabel.Text", text);
        commandBuilder.set("#DpsBox.Visible", visible);
        built = true;
    }
}
