package de.fevzi.TerrariaAddons.npc;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import de.fevzi.TerrariaAddons.housing.HousingRegistrySystem;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnDataManager;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System that handles NPC spawning logic.
 * Periodically checks for valid housing locations and spawns appropriate NPCs
 * when conditions are met (player nearby, valid housing available).
 */
public class NPCSpawnSystem extends EntityTickingSystem<EntityStore> {
    private static final long SPAWN_CHECK_INTERVAL_MS = 5000L;
    private static final long HOMELESS_CHECK_INTERVAL_MS = 30000L;
    private static final long NPC_VALIDATE_INTERVAL_MS = 5000L;
    private static final Map<String, Long> LAST_SPAWN_CHECK = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_HOMELESS_CHECK = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_NPC_VALIDATE = new ConcurrentHashMap<>();

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void tick(float delta,
                     int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> entityRef = chunk.getReferenceTo(index);
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null || player.isWaitingForClientReady()) {
            return;
        }

        EntityStore entityStore = (EntityStore) store.getExternalData();
        if (entityStore == null) {
            return;
        }

        World world = entityStore.getWorld();
        if (world == null) {
            return;
        }

        String worldKey = world.getName();
        if (worldKey == null || worldKey.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        Long lastValidate = LAST_NPC_VALIDATE.get(worldKey);
        if (lastValidate == null || (now - lastValidate) >= NPC_VALIDATE_INTERVAL_MS) {
            LAST_NPC_VALIDATE.put(worldKey, now);
            validateNpcLiveness(store, entityStore, worldKey);
        }

        Set<Vector3i> validHousings = HousingRegistrySystem.getValidHousings(world);
        if (validHousings.isEmpty()) {
            return;
        }

        Long lastHomelessCheck = LAST_HOMELESS_CHECK.get(worldKey);
        if (lastHomelessCheck == null || (now - lastHomelessCheck) >= HOMELESS_CHECK_INTERVAL_MS) {
            LAST_HOMELESS_CHECK.put(worldKey, now);

            List<String> homelessNpcs = NPCSpawnDataManager.getHomelessNpcs(worldKey, validHousings);
            if (!homelessNpcs.isEmpty()) {
                Set<Vector3i> unoccupiedHousings = NPCSpawnDataManager.getUnoccupiedHousings(worldKey, validHousings);
                if (!unoccupiedHousings.isEmpty()) {
                    Iterator<Vector3i> housingIterator = unoccupiedHousings.iterator();
                    Iterator<String> homelessIterator = homelessNpcs.iterator();

                    if (housingIterator.hasNext() && homelessIterator.hasNext()) {
                        Vector3i housing = housingIterator.next();
                        String npcType = homelessIterator.next();
                        NPCSpawnDataManager.assignHousingToHomelessNpc(worldKey, npcType, housing);
                        return;
                    }
                }
            }
        }

        Long lastCheck = LAST_SPAWN_CHECK.get(worldKey);
        if (lastCheck != null && (now - lastCheck) < SPAWN_CHECK_INTERVAL_MS) {
            return;
        }
        LAST_SPAWN_CHECK.put(worldKey, now);

        WorldTimeResource worldTime = commandBuffer.getResource(WorldTimeResource.getResourceType());
        if (worldTime == null) {
            return;
        }

        double sunlightFactor = worldTime.getSunlightFactor();
        if (sunlightFactor <= NPCSpawnManager.DAYTIME_THRESHOLD) {
            return;
        }

        Set<Vector3i> unoccupiedHousings = NPCSpawnDataManager.getUnoccupiedHousings(worldKey, validHousings);
        if (unoccupiedHousings.isEmpty()) {
            return;
        }

        // Priority 1: Assign housing to homeless NPCs
        List<String> homelessNpcs = NPCSpawnDataManager.getHomelessNpcs(worldKey, validHousings);
        if (!homelessNpcs.isEmpty()) {
            Iterator<Vector3i> housingIterator = unoccupiedHousings.iterator();
            Iterator<String> homelessIterator = homelessNpcs.iterator();

            if (housingIterator.hasNext() && homelessIterator.hasNext()) {
                Vector3i housing = housingIterator.next();
                String npcType = homelessIterator.next();
                NPCSpawnDataManager.assignHousingToHomelessNpc(worldKey, npcType, housing);
                return;
            }
        }

        // Priority 2: Spawn new NPCs if there are still available slots
        List<String> availableNpcs = NPCSpawnManager.getAllAvailableNpcTypes(worldKey);
        if (availableNpcs.isEmpty()) {
            return;
        }

        Iterator<Vector3i> housingIterator = unoccupiedHousings.iterator();
        Iterator<String> npcIterator = availableNpcs.iterator();

        if (housingIterator.hasNext() && npcIterator.hasNext()) {
            Vector3i housing = housingIterator.next();
            String npcType = npcIterator.next();
            NPCSpawnManager.spawnNpc(commandBuffer, world, worldKey, npcType, housing);
        }
    }

    private void validateNpcLiveness(@Nonnull Store<EntityStore> store,
                                     @Nonnull EntityStore entityStore,
                                     @Nonnull String worldKey) {
        for (var data : NPCSpawnDataManager.getAllNpcData(worldKey)) {
            if (data == null || !data.isAlive()) {
                continue;
            }
            var uuid = data.getEntityUuid();
            if (uuid == null) {
                NPCSpawnDataManager.markNpcDead(worldKey, data.getNpcTypeId());
                continue;
            }
            Ref<EntityStore> ref = entityStore.getRefFromUUID(uuid);
            if (ref == null || !ref.isValid()) {
                continue;
            }
            if (store.getArchetype(ref).contains(DeathComponent.getComponentType())) {
                NPCSpawnDataManager.markNpcDead(worldKey, data.getNpcTypeId());
            }
        }
    }
}
