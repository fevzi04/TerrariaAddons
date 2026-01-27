package de.fevzi.TerrariaAddons.housing.ui;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * UI element representing a single NPC in the assignment dropdown list.
 * Displays the NPC name and allows clicking to assign them to the current housing.
 */
public class NPCAssignElement extends ChoiceElement {
    private final String displayName;
    private final boolean isCurrentResident;

    public NPCAssignElement(@Nonnull String npcTypeId, @Nonnull String displayName,
                            @Nonnull Vector3i targetHousingPos, boolean isCurrentResident) {
        this.displayName = displayName;
        this.isCurrentResident = isCurrentResident;

        if (!isCurrentResident) {
            this.interactions = new ChoiceInteraction[] { new AssignNpcInteraction(npcTypeId, targetHousingPos) };
        } else {
            this.interactions = new ChoiceInteraction[0];
        }
    }

    @Override
    public void addButton(@Nonnull UICommandBuilder commandBuilder,
                          UIEventBuilder eventBuilder,
                          String selector,
                          PlayerRef playerRef) {
        commandBuilder.append("#ElementList", "Pages/NPCAssignElement.ui");
        commandBuilder.set(selector + " #NPCName.Text", displayName);

        if (isCurrentResident) {
            commandBuilder.set(selector + " #Action.Text", "(Current Resident)");
        } else {
            commandBuilder.set(selector + " #Action.Text", "Click to assign");
        }
    }
}
