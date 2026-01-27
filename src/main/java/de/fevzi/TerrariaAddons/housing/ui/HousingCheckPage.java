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

import javax.annotation.Nonnull;

/**
 * UI page that displays housing validation results.
 * Shows whether a housing structure is valid or invalid,
 * along with the reason for failure if applicable.
 */
public class HousingCheckPage extends ChoiceBasePage {
    private static final String UI_LAYOUT = "Pages/HousingCheckPage.ui";
    private static final String HOUSING_CHECKER_BLOCK_ID = "HousingChecker_Block";

    private HousingCheckPage(@Nonnull PlayerRef playerRef, @Nonnull ChoiceElement[] elements) {
        super(playerRef, elements, UI_LAYOUT);
    }

    public static HousingCheckPage create(@Nonnull Ref<EntityStore> ref,
                                          @Nonnull ComponentAccessor<EntityStore> componentAccessor,
                                          @Nonnull PlayerRef playerRef,
                                          @Nonnull InteractionContext context) {
        String status = "INVALID";
        String reason = "no target block";

        World world = null;
        Store<EntityStore> store = ref.getStore();
        if (store != null) {
            EntityStore entityStore = store.getExternalData();
            if (entityStore != null) {
                world = entityStore.getWorld();
            }
        }

        BlockPosition target = context.getTargetBlock();
        if (world != null && target != null) {
            Vector3i pos = new Vector3i(target.x, target.y, target.z);
            BlockAccessor accessor = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
            BlockType blockType = accessor != null ? accessor.getBlockType(pos) : null;
            if (blockType == null || blockType.isUnknown() || !HOUSING_CHECKER_BLOCK_ID.equals(blockType.getId())) {
                reason = "target is not the housing checker block";
            } else {
                HousingCheckerSystem.HousingResult result = HousingCheckerSystem.validate(world, pos);
                if (result == HousingCheckerSystem.HousingResult.VALID) {
                    status = "VALID";
                    reason = "house is valid";
                } else {
                    reason = HousingCheckerSystem.describeResult(result);
                }
            }
        }

        ChoiceElement[] elements = new ChoiceElement[] { new HousingCheckElement(status, reason) };
        return new HousingCheckPage(playerRef, elements);
    }
}
