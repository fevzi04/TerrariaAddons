package de.fevzi.TerrariaAddons.items.potions.teleportation;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.Executor;

/**
 * Interaction handler for the Teleportation Potion.
 * Teleports the player to a random location nearby.
 */
public class TeleportationPotionInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<TeleportationPotionInteraction> CODEC;

    private static final int MAX_ATTEMPTS = 40;
    private static final int SEARCH_RADIUS = 1000;
    private static final int VERTICAL_SCAN = ChunkUtil.HEIGHT_MINUS_1;

    static {
        CODEC = BuilderCodec.builder(
                TeleportationPotionInteraction.class,
                TeleportationPotionInteraction::new,
                SimpleInstantInteraction.CODEC
        ).build();
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldown) {
        Ref<EntityStore> ref = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Player player = (Player) commandBuffer.getComponent(ref, Player.getComponentType());

        if (player == null) {
            return;
        }

        TransformComponent transformComponent = (TransformComponent) commandBuffer.getComponent(
                ref,
                TransformComponent.getComponentType()
        );
        if (transformComponent == null) {
            return;
        }

        World world = player.getWorld();
        if (world == null) {
            return;
        }

        world.execute(() -> scheduleTeleportAttempt(world, ref, transformComponent.getPosition(), MAX_ATTEMPTS));
    }

    private static void scheduleTeleportAttempt(@Nonnull World world,
                                                @Nonnull Ref<EntityStore> ref,
                                                @Nonnull Vector3d origin,
                                                int attemptsLeft) {
        if (attemptsLeft <= 0) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int originX = (int) Math.floor(origin.x);
        int originZ = (int) Math.floor(origin.z);

        int x = originX + random.nextInt(-SEARCH_RADIUS, SEARCH_RADIUS + 1);
        int y = random.nextInt(0, ChunkUtil.HEIGHT_MINUS_1 + 1);
        int z = originZ + random.nextInt(-SEARCH_RADIUS, SEARCH_RADIUS + 1);

        final int targetX = x;
        final int targetY = y;
        final int targetZ = z;

        long chunkIndex = ChunkUtil.indexChunkFromBlock(targetX, targetZ);
        world.getChunkAsync(chunkIndex).thenAcceptAsync(chunk -> {
            if (chunk == null || ref == null || !ref.isValid()) {
                world.execute(() -> scheduleTeleportAttempt(world, ref, origin, attemptsLeft - 1));
                return;
            }

            Vector3d safePos = findNearestSafeInChunk(chunk, targetX, targetY, targetZ);
            if (safePos == null) {
                world.execute(() -> scheduleTeleportAttempt(world, ref, origin, attemptsLeft - 1));
                return;
            }

            Store<EntityStore> store = ref.getStore();
            if (store == null || !ref.isValid()) {
                return;
            }

            store.addComponent(
                    ref,
                    Teleport.getComponentType(),
                    Teleport.createForPlayer(world, new Transform(safePos))
            );
        }, (Executor) world);
    }

    private static Vector3d findNearestSafeInChunk(@Nonnull BlockAccessor accessor, int targetX, int targetY, int targetZ) {
        int chunkX = ChunkUtil.chunkCoordinate(targetX);
        int chunkZ = ChunkUtil.chunkCoordinate(targetZ);
        int minX = ChunkUtil.minBlock(chunkX);
        int maxX = ChunkUtil.maxBlock(chunkX);
        int minZ = ChunkUtil.minBlock(chunkZ);
        int maxZ = ChunkUtil.maxBlock(chunkZ);

        Vector3d direct = findSafeInColumn(accessor, targetX, targetY, targetZ);
        if (direct != null) {
            return direct;
        }

        int maxRadius = ChunkUtil.SIZE_MINUS_1;
        for (int radius = 1; radius <= maxRadius; radius++) {
            int startX = Math.max(minX, targetX - radius);
            int endX = Math.min(maxX, targetX + radius);
            int startZ = Math.max(minZ, targetZ - radius);
            int endZ = Math.min(maxZ, targetZ + radius);

            for (int x = startX; x <= endX; x++) {
                Vector3d pos = findSafeInColumn(accessor, x, targetY, startZ);
                if (pos != null) {
                    return pos;
                }
                if (startZ != endZ) {
                    pos = findSafeInColumn(accessor, x, targetY, endZ);
                    if (pos != null) {
                        return pos;
                    }
                }
            }

            for (int z = startZ + 1; z <= endZ - 1; z++) {
                Vector3d pos = findSafeInColumn(accessor, startX, targetY, z);
                if (pos != null) {
                    return pos;
                }
                if (startX != endX) {
                    pos = findSafeInColumn(accessor, endX, targetY, z);
                    if (pos != null) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private static Vector3d findSafeInColumn(@Nonnull BlockAccessor accessor, int x, int targetY, int z) {
        int maxY = clampY(targetY + VERTICAL_SCAN);
        int minY = clampY(targetY - VERTICAL_SCAN);
        for (int offset = 0; offset <= VERTICAL_SCAN; offset++) {
            int yUp = clampY(targetY + offset);
            if (yUp <= maxY && isValidTeleportLocation(accessor, x, yUp, z)) {
                return new Vector3d(x + 0.5, yUp + 0.1, z + 0.5);
            }
            int yDown = clampY(targetY - offset);
            if (yDown >= minY && isValidTeleportLocation(accessor, x, yDown, z)) {
                return new Vector3d(x + 0.5, yDown + 0.1, z + 0.5);
            }
        }
        return null;
    }

    private static boolean isValidTeleportLocation(@Nonnull BlockAccessor accessor, int x, int y, int z) {
        try {
            int blockBelow = accessor.getBlock(x, y - 1, z);
            int blockAtFeet = accessor.getBlock(x, y, z);
            int blockAtHead = accessor.getBlock(x, y + 1, z);
            return blockBelow != 0 && blockAtFeet == 0 && blockAtHead == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static int clampY(int y) {
        if (y < 0) {
            return 0;
        }
        if (y > ChunkUtil.HEIGHT_MINUS_1) {
            return ChunkUtil.HEIGHT_MINUS_1;
        }
        return y;
    }

}
