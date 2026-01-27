package de.fevzi.TerrariaAddons.housing.ui;

import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * UI element that displays a single housing check result.
 * Shows the validation status (VALID/INVALID) and the reason message.
 */
public class HousingCheckElement extends ChoiceElement {
    private final String status;
    private final String reason;

    public HousingCheckElement(@Nonnull String status, @Nonnull String reason) {
        this.status = status;
        this.reason = reason;
        this.interactions = new com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction[0];
    }

    @Override
    public void addButton(@Nonnull UICommandBuilder commandBuilder,
                          UIEventBuilder eventBuilder,
                          String selector,
                          PlayerRef playerRef) {
        commandBuilder.append("#ElementList", "Pages/HousingCheckElement.ui");
        commandBuilder.set(selector + " #Status.Text", this.status);
        commandBuilder.set(selector + " #Reason.Text", this.reason);
    }
}
