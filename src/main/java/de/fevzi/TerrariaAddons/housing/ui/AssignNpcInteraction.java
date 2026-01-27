package de.fevzi.TerrariaAddons.housing.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.fevzi.TerrariaAddons.housing.HousingRegistrySystem;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnDataManager;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Choice interaction that assigns an NPC to a specific housing.
 * When selected, moves the NPC from their current housing to the target housing.
 */
public class AssignNpcInteraction extends ChoiceInteraction {
    private final String npcTypeId;
    private final Vector3i targetHousingPos;

    public AssignNpcInteraction(@Nonnull String npcTypeId, @Nonnull Vector3i targetHousingPos) {
        this.npcTypeId = npcTypeId;
        this.targetHousingPos = targetHousingPos;
    }

    @Override
    public void run(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        PageManager pageManager = playerComponent.getPageManager();

        EntityStore entityStore = store.getExternalData();
        World world = entityStore.getWorld();

        String worldKey = world.getName();
        if (worldKey == null || worldKey.isEmpty()) {
            playerRef.sendMessage(Message.raw("Invalid world!"));
            pageManager.setPage(ref, store, Page.None);
            return;
        }

        String displacedNpcType = NPCSpawnDataManager.getNpcTypeAtHousing(worldKey, targetHousingPos);

        boolean success = NPCSpawnDataManager.reassignNpcToHousing(worldKey, npcTypeId, targetHousingPos);

        if (success) {
            playerRef.sendMessage(Message.raw("§a" + npcTypeId + " has been assigned to this house!"));

            if (displacedNpcType != null && !displacedNpcType.equals(npcTypeId)) {
                Set<Vector3i> validHousings = HousingRegistrySystem.getValidHousings(world);
                NPCSpawnDataManager.tryImmediateHousingAssignment(worldKey, displacedNpcType, validHousings);
            }
        } else {
            playerRef.sendMessage(Message.raw("§cFailed to assign " + npcTypeId + " to this house."));
        }

        pageManager.setPage(ref, store, Page.None);
    }
}
