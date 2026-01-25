package de.fevzi.TerrariaAddons.items.Starfury;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.asset.type.projectile.config.Projectile;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.collision.CollisionMath;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.function.predicate.BiIntPredicate;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.List;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("deprecation")
public class StarfuryInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<StarfuryInteraction> CODEC =
            BuilderCodec.builder(StarfuryInteraction.class, StarfuryInteraction::new, SimpleInstantInteraction.CODEC)
                    .build();

    private static final String PROJECTILE_ID = "Starfury_Projectile";
    private static final double SPAWN_HEIGHT = 30.0;
    private static final double RAYCAST_RANGE = 20.0;
    private static final double ENTITY_HITBOX_PADDING = 0.6;
    private static final Duration PROJECTILE_COOLDOWN = Duration.ofSeconds(1);
    private static final Map<UUID, Instant> LAST_PROJECTILE_TIME = new ConcurrentHashMap<>();

    @Override
    public boolean needsRemoteSync() {
        return true;
    }

    @Override
    @Nonnull
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Client;
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {
        Projectile projectile = Projectile.getAssetMap().getAsset(PROJECTILE_ID);
        if (projectile == null) {
            return;
        }

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Ref<EntityStore> shooterRef = context.getEntity();
        Position hit = null;
        EntityStore entityStore = (EntityStore) commandBuffer.getExternalData();
        World world = entityStore.getWorld();
        Vector3d origin = TargetUtil.getLook(shooterRef, commandBuffer).getPosition();
        Vector3f lookRotation = TargetUtil.getLook(shooterRef, commandBuffer).getRotation();
        Vector3d direction = new Vector3d();
        PhysicsMath.vectorFromAngles(lookRotation.getYaw(), lookRotation.getPitch(), direction);

        BiIntPredicate solidBlock = (blockId, fluidId) -> {
            if (blockId == 0) {
                return false;
            }
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            return blockType != null && !blockType.isUnknown() && blockType.getMaterial() == BlockMaterial.Solid;
        };
        Vector3i blockHitPos = TargetUtil.getTargetBlock(
                world,
                solidBlock,
                origin.x,
                origin.y,
                origin.z,
                direction.x,
                direction.y,
                direction.z,
                RAYCAST_RANGE
        );
        Vector3d blockHit = null;
        if (blockHitPos != null) {
            blockHit = new Vector3d(blockHitPos.x + 0.5, blockHitPos.y + 0.5, blockHitPos.z + 0.5);
        }

        Vector3d entityHit = null;
        double closestEntityT = Double.MAX_VALUE;
        List<Ref<EntityStore>> candidates = TargetUtil.getAllEntitiesInSphere(origin, RAYCAST_RANGE, commandBuffer);
        for (Ref<EntityStore> candidate : candidates) {
            if (candidate == null || !candidate.isValid() || candidate.equals(shooterRef)) {
                continue;
            }

            TransformComponent transform = (TransformComponent) commandBuffer.getComponent(
                    candidate,
                    TransformComponent.getComponentType()
            );
            BoundingBox boundingBox = (BoundingBox) commandBuffer.getComponent(
                    candidate,
                    BoundingBox.getComponentType()
            );
            if (transform == null || boundingBox == null) {
                continue;
            }

            Vector3d entityPos = transform.getPosition();
            Box expanded = new Box(boundingBox.getBoundingBox());
            expanded.min.x -= ENTITY_HITBOX_PADDING;
            expanded.min.y -= ENTITY_HITBOX_PADDING;
            expanded.min.z -= ENTITY_HITBOX_PADDING;
            expanded.max.x += ENTITY_HITBOX_PADDING;
            expanded.max.y += ENTITY_HITBOX_PADDING;
            expanded.max.z += ENTITY_HITBOX_PADDING;

            double t = CollisionMath.intersectRayAABB(origin, direction, entityPos.x, entityPos.y, entityPos.z, expanded);
            if (t >= 0.0 && t <= RAYCAST_RANGE && t < closestEntityT) {
                closestEntityT = t;
                entityHit = new Vector3d(
                        origin.x + direction.x * t,
                        origin.y + direction.y * t,
                        origin.z + direction.z * t
                );
            }
        }


        if (blockHit != null && entityHit != null) {
            double blockDist2 = origin.distanceSquaredTo(blockHit.x, blockHit.y, blockHit.z);
            double entityDist2 = origin.distanceSquaredTo(entityHit.x, entityHit.y, entityHit.z);
            Vector3d closer = blockDist2 <= entityDist2 ? blockHit : entityHit;
            hit = new Position(closer.x, closer.y, closer.z);
        } else if (blockHit != null) {
            hit = new Position(blockHit.x, blockHit.y, blockHit.z);
        } else if (entityHit != null) {
            hit = new Position(entityHit.x, entityHit.y, entityHit.z);
        }

        if (hit == null) {
            Vector3d maxRangePos = new Vector3d(
                    origin.x + direction.x * RAYCAST_RANGE,
                    origin.y + direction.y * RAYCAST_RANGE,
                    origin.z + direction.z * RAYCAST_RANGE
            );
            hit = new Position(maxRangePos.x, maxRangePos.y, maxRangePos.z);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double offsetRadius = 15.0;
        double randomAngle = random.nextDouble() * Math.PI * 2.0;
        double offsetX = Math.cos(randomAngle) * offsetRadius;
        double offsetZ = Math.sin(randomAngle) * offsetRadius;

        Vector3d spawnPosition = new Vector3d(
            hit.x + offsetX,
            hit.y + SPAWN_HEIGHT,
            hit.z + offsetZ
        );

        TimeResource timeResource = (TimeResource) commandBuffer.getResource(TimeResource.getResourceType());
        if (timeResource == null) {
            return;
        }

        UUIDComponent shooterUuid = (UUIDComponent) commandBuffer.getComponent(shooterRef, UUIDComponent.getComponentType());
        if (shooterUuid == null) {
            return;
        }

        Instant now = timeResource.getNow();
        UUID shooterId = shooterUuid.getUuid();
        Instant lastShot = LAST_PROJECTILE_TIME.get(shooterId);
        if (lastShot != null && now.isBefore(lastShot.plus(PROJECTILE_COOLDOWN))) {
            return;
        }
        LAST_PROJECTILE_TIME.put(shooterId, now);

        double deltaX = hit.x - spawnPosition.x;
        double deltaY = hit.y - spawnPosition.y;
        double deltaZ = hit.z - spawnPosition.z;

        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        if (distance > 0.0) {
            deltaX /= distance;
            deltaY /= distance;
            deltaZ /= distance;
        }

        float yaw = (float) Math.atan2(-deltaX, deltaZ);
        float pitch = (float) -Math.asin(deltaY);

        Vector3f rotation = new Vector3f(yaw, pitch, 0.0f);
        Holder<EntityStore> holder = ProjectileComponent.assembleDefaultProjectile(
                timeResource,
                PROJECTILE_ID,
                spawnPosition,
                rotation
        );

        ProjectileComponent projectileComponent = (ProjectileComponent) holder.getComponent(ProjectileComponent.getComponentType());
        if (projectileComponent == null) {
            return;
        }
        if (projectileComponent.getProjectile() == null && !projectileComponent.initialize()) {
            return;
        }
        if (projectileComponent.getProjectile() == null) {
            return;
        }

        double muzzleVelocity = projectileComponent.getProjectile().getMuzzleVelocity();

        Vector3d velocity = new Vector3d(
            deltaX * muzzleVelocity,
            deltaY * muzzleVelocity,
            deltaZ * muzzleVelocity
        );

        projectileComponent.getSimplePhysicsProvider().setVelocity(velocity);
        projectileComponent.getSimplePhysicsProvider().setCreatorId(shooterUuid.getUuid());

        try {
            Field creatorUuidField = ProjectileComponent.class.getDeclaredField("creatorUuid");
            creatorUuidField.setAccessible(true);
            creatorUuidField.set(projectileComponent, shooterUuid.getUuid());
        } catch (Exception e) {
        }

        commandBuffer.addEntity(holder, AddReason.SPAWN);
    }
}
