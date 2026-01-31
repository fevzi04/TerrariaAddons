package de.fevzi.TerrariaAddons.npc;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MapMarkerTracker;
import com.hypixel.hytale.server.core.util.PositionUtil;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnData;
import de.fevzi.TerrariaAddons.housing.npc.NPCSpawnDataManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public class NPCMapMarkerProvider implements WorldMapManager.MarkerProvider {
    public static final NPCMapMarkerProvider INSTANCE = new NPCMapMarkerProvider();
    private static final String MARKER_ICON = "NPC.png";
    private static final String MARKER_PREFIX = "NPC-";

    @Override
    public void update(@Nonnull World world,
                       @Nonnull MapMarkerTracker tracker,
                       int chunkViewRadius,
                       int playerChunkX,
                       int playerChunkZ) {
        String worldKey = world.getName();
        if (worldKey == null || worldKey.isEmpty()) {
            return;
        }

        List<NPCSpawnData> aliveNpcs = NPCSpawnDataManager.getAllAliveNpcs(worldKey);
        if (aliveNpcs.isEmpty()) {
            return;
        }

        for (NPCSpawnData data : aliveNpcs) {
            if (data == null) {
                continue;
            }

            Vector3d position = null;
            UUID uuid = data.getEntityUuid();
            if (uuid != null) {
                position = NPCSpawnDataManager.getNpcPosition(uuid);
            }

            if (position == null) {
                Vector3i housingPos = data.getHousingPosition();
                position = new Vector3d(housingPos.x + 0.5, housingPos.y + 1.0, housingPos.z + 0.5);
            }

            String npcTypeId = data.getNpcTypeId();
            if (npcTypeId == null || npcTypeId.isEmpty()) {
                continue;
            }

            String markerId = MARKER_PREFIX + npcTypeId;
            String markerName = formatNpcName(npcTypeId);

            Vector3d markerPos = position;
            float markerYaw = 0.0f;
            tracker.trySendMarker(
                -1,
                playerChunkX,
                playerChunkZ,
                markerPos,
                markerYaw,
                markerId,
                markerName,
                markerPos,
                (id, name, pos) -> new MapMarker(
                    id,
                    name,
                    MARKER_ICON,
                    PositionUtil.toTransformPacket(new Transform(pos)),
                    null
                )
            );
        }
    }

    private static String formatNpcName(@Nonnull String npcTypeId) {
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
