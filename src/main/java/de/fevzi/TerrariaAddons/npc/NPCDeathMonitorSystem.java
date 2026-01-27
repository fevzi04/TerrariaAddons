package de.fevzi.TerrariaAddons.npc;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NPCDeathMonitorSystem extends DamageEventSystem {
    private static final Map<UUID, Boolean> PENDING_DEATH_CHECK = new ConcurrentHashMap<>();

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled()) {
            return;
        }

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        UUIDComponent uuidComponent = store.getComponent(targetRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }

        UUID entityUuid = uuidComponent.getUuid();

        EntityStore entityStore = store.getExternalData();
        World world = entityStore.getWorld();

        String worldKey = world.getName();
        if (worldKey == null || worldKey.isEmpty()) {
            return;
        }

        if (!NPCSpawnManager.isRegisteredNpc(worldKey, entityUuid)) {
            return;
        }

        float damageAmount = damage.getAmount();
        if (damageAmount <= 0) {
            return;
        }

        if (!PENDING_DEATH_CHECK.containsKey(entityUuid)) {
            PENDING_DEATH_CHECK.put(entityUuid, true);
            NPCSpawnManager.onNpcDeath(worldKey, entityUuid);
            PENDING_DEATH_CHECK.remove(entityUuid);
        }
    }
}
