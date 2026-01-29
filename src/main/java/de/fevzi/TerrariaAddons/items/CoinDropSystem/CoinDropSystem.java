package de.fevzi.TerrariaAddons.items.CoinDropSystem;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.fevzi.TerrariaAddons.config.TerrariaAddonsConfig;
import de.fevzi.TerrariaAddons.items.CoinPouch.CoinPouchCurrency;
import de.fevzi.TerrariaAddons.npc.NPCSpawnManager;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * System that drops coins from slain hostile mobs, similar to Terraria.
 * When a hostile NPC (not a player or friendly NPC) dies, this system
 * drops coins at the entity's death location based on the enemy's max health.
 */
public class CoinDropSystem extends EntityTickingSystem<EntityStore> {

    private static final Map<UUID, Boolean> PROCESSED_DEATHS = new ConcurrentHashMap<>();
    private static final float DROP_VELOCITY = 0.3f;
    private static final float DROP_SPREAD = 0.2f;
    private static final long CLEANUP_INTERVAL_MS = 60000L; // Clean up every minute
    private static long lastCleanupTime = 0;

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

        boolean isDead = commandBuffer.getArchetype(entityRef).contains(DeathComponent.getComponentType());
        if (!isDead) {
            return;
        }

        UUIDComponent uuidComponent = store.getComponent(entityRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }

        UUID entityUuid = uuidComponent.getUuid();
        if (entityUuid == null) {
            return;
        }

        if (PROCESSED_DEATHS.containsKey(entityUuid)) {
            return;
        }

        Player player = store.getComponent(entityRef, Player.getComponentType());
        if (player != null) {
            return;
        }

        NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return;
        }

        
        EntityStore entityStore = (EntityStore) store.getExternalData();
        if (entityStore == null) {
            return;
        }

        com.hypixel.hytale.server.core.universe.world.World world = entityStore.getWorld();
        if (world == null) {
            return;
        }

        String worldKey = world.getName();
        if (worldKey != null && !worldKey.isEmpty()) {
            if (NPCSpawnManager.isRegisteredNpc(worldKey, entityUuid)) {
                return;
            }
        }

        PROCESSED_DEATHS.put(entityUuid, true);

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null) {
            PROCESSED_DEATHS.remove(entityUuid);
            return;
        }

        Vector3d deathPosition = transform.getPosition();

        EntityStatMap stats = store.getComponent(entityRef, EntityStatMap.getComponentType());
        float maxHealth = 0.0f;
        if (stats != null) {
            int healthStatIndex = DefaultEntityStatTypes.getHealth();
            if (healthStatIndex >= 0) {
                var healthStat = stats.get(healthStatIndex);
                if (healthStat != null) {
                    maxHealth = healthStat.getMax();
                }
            }
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int coinAmount = calculateCoinAmount(maxHealth, random);

        if (coinAmount <= 0) {
            PROCESSED_DEATHS.remove(entityUuid);
            return;
        }

        dropConvertedCoins(store, commandBuffer, deathPosition, coinAmount, random);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL_MS) {
            cleanupProcessedDeaths(store);
            lastCleanupTime = currentTime;
        }
    }


    private int calculateCoinAmount(float maxHealth, @Nonnull ThreadLocalRandom random) {
        if (maxHealth <= 0) {
            int minCoins = TerrariaAddonsConfig.getInstance().getMinCoinsPerKill();
            int maxCoins = TerrariaAddonsConfig.getInstance().getMaxCoinsPerKill();
            
            if (minCoins < 0) minCoins = 0;
            if (maxCoins < minCoins) maxCoins = minCoins;
            
            if (minCoins == 0 && maxCoins == 0) {
                return 0;
            }
            
            return random.nextInt(minCoins, maxCoins + 1);
        }
        
        float coinsPerHealth = TerrariaAddonsConfig.getInstance().getCoinsPerHealth();
        if (coinsPerHealth <= 0) {
            coinsPerHealth = 10.0f;
        }
        
        int baseCoins = (int) (maxHealth / coinsPerHealth);
        
        float variation = 0.2f;
        int minCoins = (int) (baseCoins * (1.0f - variation));
        int maxCoins = (int) (baseCoins * (1.0f + variation));
        
        if (minCoins < 1) minCoins = 1;
        if (maxCoins < minCoins) maxCoins = minCoins;
        
        return random.nextInt(minCoins, maxCoins + 1);
    }

    private void dropConvertedCoins(@Nonnull Store<EntityStore> store,
                                    @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                    @Nonnull Vector3d position,
                                    int totalCopper,
                                    @Nonnull ThreadLocalRandom random) {
        if (totalCopper <= 0) {
            return;
        }

        int remaining = totalCopper;
        int platinum = remaining / CoinPouchCurrency.PLATINUM_VALUE;
        remaining %= CoinPouchCurrency.PLATINUM_VALUE;
        int gold = remaining / CoinPouchCurrency.GOLD_VALUE;
        remaining %= CoinPouchCurrency.GOLD_VALUE;
        int silver = remaining / CoinPouchCurrency.SILVER_VALUE;
        remaining %= CoinPouchCurrency.SILVER_VALUE;
        int copper = remaining;

        dropCoinStack(store, commandBuffer, position, CoinPouchCurrency.PLATINUM_COIN_ID, platinum, random);
        dropCoinStack(store, commandBuffer, position, CoinPouchCurrency.GOLD_COIN_ID, gold, random);
        dropCoinStack(store, commandBuffer, position, CoinPouchCurrency.SILVER_COIN_ID, silver, random);
        dropCoinStack(store, commandBuffer, position, CoinPouchCurrency.COPPER_COIN_ID, copper, random);
    }

    private void dropCoinStack(@Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer,
                               @Nonnull Vector3d position,
                               @Nonnull String coinItemId,
                               int amount,
                               @Nonnull ThreadLocalRandom random) {
        if (amount <= 0) {
            return;
        }

        double offsetX = (random.nextDouble() - 0.5) * DROP_SPREAD;
        double offsetZ = (random.nextDouble() - 0.5) * DROP_SPREAD;
        double offsetY = random.nextDouble() * 0.1;

        Vector3d dropPosition = new Vector3d(
            position.x + offsetX,
            position.y + offsetY,
            position.z + offsetZ
        );

        float velocityX = (float) (random.nextGaussian() * DROP_VELOCITY);
        float velocityY = (float) (random.nextDouble() * DROP_VELOCITY + 0.1f);
        float velocityZ = (float) (random.nextGaussian() * DROP_VELOCITY);

        Vector3f rotation = Vector3f.ZERO;

        try {
            ItemStack itemStack = new ItemStack(coinItemId, amount);
            Holder<EntityStore> itemHolder = ItemComponent.generateItemDrop(
                store,
                itemStack,
                dropPosition,
                rotation,
                velocityX,
                velocityY,
                velocityZ
            );

            if (itemHolder != null) {
                commandBuffer.addEntity(itemHolder, AddReason.SPAWN);
            }
        } catch (Exception e) {
        }
    }

    private void cleanupProcessedDeaths(@Nonnull Store<EntityStore> store) {
        EntityStore entityStore = (EntityStore) store.getExternalData();
        if (entityStore == null) {
            return;
        }

        PROCESSED_DEATHS.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            Ref<EntityStore> ref = entityStore.getRefFromUUID(uuid);
            if (ref == null || !ref.isValid()) {
                return true;
            }
            boolean stillDead = store.getArchetype(ref).contains(DeathComponent.getComponentType());
            if (!stillDead) {
                return true;
            }
            return false;
        });
    }

}
