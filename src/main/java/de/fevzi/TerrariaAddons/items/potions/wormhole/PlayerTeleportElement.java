package de.fevzi.TerrariaAddons.items.potions.wormhole;

import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

/**
 * UI element representing a single player in the teleport selection list.
 * Displays the player name and a clickable button to teleport to them.
 */
public class PlayerTeleportElement extends ChoiceElement {
    private final String playerName;

    public PlayerTeleportElement(String playerName, TeleportInteraction interaction) {
        this.playerName = playerName;
        this.interactions = new ChoiceInteraction[] { interaction };
    }

    @Override
    public void addButton(@Nonnull UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, String selector, PlayerRef playerRef) {

        commandBuilder.append("#ElementList", "Pages/PlayerTeleportElement.ui");

        commandBuilder.set(selector + " #Name.Text", this.playerName);

        commandBuilder.set(selector + " #TP.Text", "Click to TP");
    }
}