package de.fevzi.TerrariaAddons.housing.npc;

import com.hypixel.hytale.math.vector.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Data class representing an NPC's spawn information.
 * Stores the NPC type, assigned housing position, entity UUID,
 * alive status, and death timestamp for respawn calculations.
 */
public class NPCSpawnData {
    private final String npcTypeId;
    private final Vector3i housingPosition;
    private UUID entityUuid;
    private boolean alive;
    private long deathTimestamp;

    public NPCSpawnData(@Nonnull String npcTypeId, @Nonnull Vector3i housingPosition) {
        this.npcTypeId = npcTypeId;
        this.housingPosition = housingPosition;
        this.entityUuid = null;
        this.alive = false;
        this.deathTimestamp = 0L;
    }

    @Nonnull
    public String getNpcTypeId() {
        return npcTypeId;
    }

    @Nonnull
    public Vector3i getHousingPosition() {
        return housingPosition;
    }

    @Nullable
    public UUID getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(@Nullable UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public long getDeathTimestamp() {
        return deathTimestamp;
    }

    public void setDeathTimestamp(long deathTimestamp) {
        this.deathTimestamp = deathTimestamp;
    }
}
