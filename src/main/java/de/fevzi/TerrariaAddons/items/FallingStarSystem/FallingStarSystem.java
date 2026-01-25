package de.fevzi.TerrariaAddons.items.FallingStarSystem;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;


public class FallingStarSystem extends EntityTickingSystem<EntityStore> {

    private static final double SPAWN_CHANCE = 0.0004;
    private static final double SPAWN_HEIGHT = 50.0;
    private static final double SPAWN_RADIUS = 50.0;
    private static final double NIGHT_THRESHOLD = 0.3;
    private static final float DOWN_PITCH = -1.5707964f;
    private static final long SPAWN_COOLDOWN_MS = 5000L;
    private static final String FALLEN_STAR_ITEM_ID = "Ingredient_FallenStar";
    private static final Map<UUID, Long> LAST_SPAWN_TIME = new ConcurrentHashMap<>();

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

        WorldTimeResource worldTime = commandBuffer.getResource(WorldTimeResource.getResourceType());
        if (worldTime == null) {
            return;
        }

        double sunlightFactor = worldTime.getSunlightFactor();
        boolean isDay = sunlightFactor > NIGHT_THRESHOLD;

        ItemComponent itemComponent = store.getComponent(entityRef, ItemComponent.getComponentType());
        if (itemComponent != null) {
            ItemStack itemStack = itemComponent.getItemStack();
            if (itemStack != null && FALLEN_STAR_ITEM_ID.equals(itemStack.getItemId())) {
                if (isDay) {
                    commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);
                }
                return;
            }
        }

        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player == null || player.isWaitingForClientReady()) {
            return;
        }

        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        if (isDay) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() > SPAWN_CHANCE) {
            return;
        }

        UUID playerUuid = playerRef.getUuid();
        TimeResource timeResource = commandBuffer.getResource(TimeResource.getResourceType());
        if (timeResource == null) {
            return;
        }

        long currentTime = timeResource.getNow().toEpochMilli();
        Long lastSpawnTime = LAST_SPAWN_TIME.get(playerUuid);
        if (lastSpawnTime != null && (currentTime - lastSpawnTime) < SPAWN_COOLDOWN_MS) {
            return;
        }

        TransformComponent transform = commandBuffer.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        Vector3d playerPos = transform.getPosition();

        if (playerPos.y < 60.0) {
            return;
        }

        double offsetX = (random.nextDouble() - 0.5) * 2 * SPAWN_RADIUS;
        double offsetZ = (random.nextDouble() - 0.5) * 2 * SPAWN_RADIUS;

        Vector3d spawnPos = new Vector3d(
            playerPos.x + offsetX,
            playerPos.y + SPAWN_HEIGHT,
            playerPos.z + offsetZ
        );

        float yaw = (float) (random.nextDouble() * Math.PI * 2);
        Vector3f rotation = new Vector3f(yaw, DOWN_PITCH, 0f);

        ItemStack itemStack = new ItemStack("Ingredient_FallenStar", 1);

        float speed = 0.5f;
        float velocityX = (float) Math.cos(yaw) * speed;
        float velocityZ = (float) Math.sin(yaw) * speed;
        float velocityY = -1.0f;

        try {
            Holder<EntityStore> itemHolder = ItemComponent.generateItemDrop(
                store,
                itemStack,
                spawnPos,
                rotation,
                velocityX,
                velocityY,
                velocityZ
            );

            if (itemHolder != null) {
                commandBuffer.addEntity(itemHolder, AddReason.SPAWN);
                LAST_SPAWN_TIME.put(playerUuid, currentTime);
            }
        } catch (Exception e) {
        }
    }
}
