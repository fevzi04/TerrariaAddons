package de.fevzi.TerrariaAddons.housing;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class HousingRevalidationBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    public HousingRevalidationBreakSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Vector3i changed = event.getTargetBlock();
        if (changed == null) {
            return;
        }

        BlockType brokenType = event.getBlockType();
        if (brokenType != null && HousingCheckerSystem.HOUSING_CHECKER_ITEM_ID.equals(brokenType.getId())) {
            HousingCheckerSystem.clearCachedResult(changed);
            return;
        }

        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) {
            return;
        }

        Player player = (Player) commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        HousingRevalidationTickSystem.enqueue(ref, changed);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
