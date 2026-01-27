package de.fevzi.TerrariaAddons.housing.ui;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceBasePage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.fevzi.TerrariaAddons.housing.HousingCheckerSystem;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnData;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnDataManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * UI page that displays housing validation results.
 * Shows whether a housing structure is valid or invalid,
 * along with the reason for failure if applicable.
 * For valid housings, also displays a list of NPCs that can be assigned.
 */
public class HousingCheckPage extends ChoiceBasePage {
    private static final String UI_LAYOUT = "Pages/HousingCheckPage.ui";
    private static final String HOUSING_CHECKER_BLOCK_ID = "Housing_Block";

    private HousingCheckPage(@Nonnull PlayerRef playerRef, @Nonnull ChoiceElement[] elements) {
        super(playerRef, elements, UI_LAYOUT);
    }

    public static HousingCheckPage create(@Nonnull Ref<EntityStore> ref,
                                          @Nonnull ComponentAccessor<EntityStore> componentAccessor,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull InteractionContext context) {
        String status = "INVALID";
        String reason = "no target block";
        boolean isValid = false;
        Vector3i housingPos = null;
        String worldKey = null;

        World world = null;
        Store<EntityStore> store = ref.getStore();
        if (store != null) {
            EntityStore entityStore = store.getExternalData();
            if (entityStore != null) {
                world = entityStore.getWorld();
                worldKey = world != null ? world.getName() : null;
            }
        }

        BlockPosition target = context.getTargetBlock();
        if (world != null && target != null) {
            housingPos = new Vector3i(target.x, target.y, target.z);
            BlockAccessor accessor = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(housingPos.x, housingPos.z));
            BlockType blockType = accessor != null ? accessor.getBlockType(housingPos) : null;
            if (blockType == null || blockType.isUnknown() || !HOUSING_CHECKER_BLOCK_ID.equals(blockType.getId())) {
                reason = "target is not the housing checker block";
            } else {
                HousingCheckerSystem.HousingResult result = HousingCheckerSystem.validate(world, housingPos);
                if (result == HousingCheckerSystem.HousingResult.VALID) {
                    status = "VALID";
                    reason = "house is valid";
                    isValid = true;
                } else {
                    reason = HousingCheckerSystem.describeResult(result);
                }
            }
        }

        ObjectArrayList<ChoiceElement> elements = new ObjectArrayList<>();
        elements.add(new HousingCheckElement(status, reason));

        if (isValid && worldKey != null && housingPos != null) {
            String currentResident = NPCSpawnDataManager.getNpcTypeAtHousing(worldKey, housingPos);
            List<NPCSpawnData> aliveNpcs = NPCSpawnDataManager.getAllAliveNpcs(worldKey);

            if (!aliveNpcs.isEmpty()) {
                for (NPCSpawnData npcData : aliveNpcs) {
                    String npcTypeId = npcData.getNpcTypeId();
                    String displayName = formatNpcName(npcTypeId);
                    boolean isCurrentResident = npcTypeId.equals(currentResident);
                    elements.add(new NPCAssignElement(npcTypeId, displayName, housingPos, isCurrentResident));
                }
            }
        }

        return new HousingCheckPage(playerRef, elements.toArray(new ChoiceElement[0]));
    }


    private static String formatNpcName(String npcTypeId) {
        if (npcTypeId == null || npcTypeId.isEmpty()) {
            return "Unknown NPC";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < npcTypeId.length(); i++) {
            char c = npcTypeId.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                result.append(' ');
            }
            result.append(c);
        }
        return result.toString();
    }
}
