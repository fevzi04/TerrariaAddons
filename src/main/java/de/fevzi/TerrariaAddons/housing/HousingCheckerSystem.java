package de.fevzi.TerrariaAddons.housing;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nonnull;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;

/**
 * System that validates housing structures when a Housing Checker block is placed.
 * Performs a flood-fill search to verify that a structure meets housing requirements:
 * enclosed space, minimum volume, contains a door, contains a light source (torch).
 * Results are cached and displayed to the player via a UI page.
 */
public class HousingCheckerSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public static final String HOUSING_CHECKER_ITEM_ID = "Housing_Block";
    private static final String DOOR_ID_MARKER = "door";
    private static final String TORCH_ID_MARKER = "torch";

    private static final int MAX_SEARCH_BLOCKS = 4000; //how many blocks can be checked
    public static final int MAX_RADIUS = 24; // scanning radius
    private static final int MIN_VOLUME = 45; // minimum x*y*z interior
    private static final int MIN_HEIGHT = 2; // minimum house height

    private static final Long2ObjectOpenHashMap<HousingResult> CACHED_RESULTS = new Long2ObjectOpenHashMap<>();

    private static final int[] OFFSETS = {
            1, 0, 0,
            -1, 0, 0,
            0, 1, 0,
            0, -1, 0,
            0, 0, 1,
            0, 0, -1
    };

    public HousingCheckerSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull PlaceBlockEvent event) {

        if (event.isCancelled()) {
            return;
        }

        ItemStack itemStack = event.getItemInHand();
        if (itemStack == null || !HOUSING_CHECKER_ITEM_ID.equals(itemStack.getItemId())) {
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

        Vector3i placed = event.getTargetBlock();
        if (placed == null) {
            return;
        }

        EntityStore entityStore = (EntityStore) commandBuffer.getExternalData();
        if (entityStore == null) {
            return;
        }

        World world = entityStore.getWorld();
        if (world == null) {
            return;
        }

        HousingResult result = validate(world, placed);

        if (result == HousingResult.VALID) {
            player.sendMessage(Message.raw("Housing check: valid."));
        } else {
            player.sendMessage(Message.raw("Housing check: invalid (" + describeResult(result) + ")."));
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    private static Vector3i findInteriorStart(World world, Long2ObjectOpenHashMap<BlockAccessor> chunkCache, Vector3i placed) {

        for (int i = 0; i < OFFSETS.length; i += 3) {
            int x = placed.x + OFFSETS[i];
            int y = placed.y + OFFSETS[i + 1];
            int z = placed.z + OFFSETS[i + 2];

            if (getBlockCheck(world, chunkCache, x, y, z) == BlockCheck.AIR) {
                return new Vector3i(x, y, z);
            }
        }
        return null;
    }

    private static HousingResult checkClosedBox(World world, Long2ObjectOpenHashMap<BlockAccessor> chunkCache, Vector3i start, Vector3i origin) {

        ArrayDeque<Vector3i> queue = new ArrayDeque<>();
        HashSet<Vector3i> visited = new HashSet<>();
        HashMap<Long, int[]> columnHeights = new HashMap<>();

        boolean hasDoor = false;
        boolean hasTorch = false;

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {

            if (visited.size() > MAX_SEARCH_BLOCKS) {
                return HousingResult.TOO_LARGE;
            }

            Vector3i current = queue.removeFirst();
            updateColumnHeights(columnHeights, current);

            if (Math.abs(current.x - origin.x) > MAX_RADIUS
                    || Math.abs(current.y - origin.y) > MAX_RADIUS
                    || Math.abs(current.z - origin.z) > MAX_RADIUS) {
                return HousingResult.OPEN;
            }

            for (int i = 0; i < OFFSETS.length; i += 3) {
                int nx = current.x + OFFSETS[i];
                int ny = current.y + OFFSETS[i + 1];
                int nz = current.z + OFFSETS[i + 2];

                BlockCheck check = getBlockCheck(world, chunkCache, nx, ny, nz);

                if (check == BlockCheck.UNLOADED) {
                    return HousingResult.UNLOADED;
                }

                if (check == BlockCheck.CHECKER) {
                    if (!(nx == origin.x && ny == origin.y && nz == origin.z)) {
                        return HousingResult.MULTIPLE_CHECKERS;
                    }
                    continue;
                }

                if (check == BlockCheck.SOLID) {
                    continue;
                }

                if (check == BlockCheck.DOOR) {
                    hasDoor = true;
                    continue;
                }

                if (check == BlockCheck.TORCH) {
                    hasTorch = true;
                }

                Vector3i next = new Vector3i(nx, ny, nz);
                if (visited.add(next)) {
                    queue.addLast(next);
                }
            }
        }

        if (!allColumnsMeetMinHeight(columnHeights, MIN_HEIGHT, hasDoor ? 2 : 0)) {
            return HousingResult.TOO_SHORT;
        }

        if (visited.size() < MIN_VOLUME) {
            return HousingResult.TOO_SMALL;
        }

        if (!hasDoor) {
            return HousingResult.NO_DOOR;
        }

        if (!hasTorch) {
            return HousingResult.NO_TORCH;
        }

        return HousingResult.VALID;
    }

    private static BlockCheck getBlockCheck(World world, Long2ObjectOpenHashMap<BlockAccessor> chunkCache, int x, int y, int z) {

        BlockAccessor accessor = getBlockAccessor(world, chunkCache, x, z);
        if (accessor == null) {
            return BlockCheck.UNLOADED;
        }

        int blockId = accessor.getBlock(x, y, z);
        if (blockId == 0) {
            return BlockCheck.AIR;
        }

        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null || blockType.isUnknown()) {
            return BlockCheck.SOLID;
        }

        if (HOUSING_CHECKER_ITEM_ID.equals(blockType.getId())) {
            return BlockCheck.CHECKER;
        }

        if (isDoorBlock(blockType)) {
            return BlockCheck.DOOR;
        }

        if (isTorchBlock(blockType)) {
            return BlockCheck.TORCH;
        }

        return blockType.getMaterial() == BlockMaterial.Solid
                ? BlockCheck.SOLID
                : BlockCheck.AIR;
    }

    private static BlockAccessor getBlockAccessor(World world, Long2ObjectOpenHashMap<BlockAccessor> chunkCache, int x, int z) {

        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        BlockAccessor accessor = chunkCache.get(chunkIndex);
        if (accessor != null) {
            return accessor;
        }

        accessor = world.getChunkIfLoaded(chunkIndex);
        if (accessor != null) {
            chunkCache.put(chunkIndex, accessor);
        }
        return accessor;
    }

    private static void updateColumnHeights(HashMap<Long, int[]> columnHeights, Vector3i pos) {
        long key = (((long) pos.x) << 32) ^ (pos.z & 0xffffffffL);
        int[] bounds = columnHeights.get(key);
        if (bounds == null) {
            columnHeights.put(key, new int[]{pos.y, pos.y});
        } else {
            bounds[0] = Math.min(bounds[0], pos.y);
            bounds[1] = Math.max(bounds[1], pos.y);
        }
    }

    private static boolean allColumnsMeetMinHeight(HashMap<Long, int[]> columnHeights, int minHeight, int allowedTooShortColumns) {
        int tooShortColumns = 0;
        for (int[] bounds : columnHeights.values()) {
            if ((bounds[1] - bounds[0] + 1) < minHeight) {
                tooShortColumns++;
                if (tooShortColumns > allowedTooShortColumns) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isDoorBlock(BlockType blockType) {
        String id = blockType.getId();
        if (id != null && id.toLowerCase().contains(DOOR_ID_MARKER)) {
            return true;
        }
        String hitboxType = blockType.getHitboxType();
        if (hitboxType != null && hitboxType.toLowerCase().contains(DOOR_ID_MARKER)) {
            return true;
        }
        String interactionHitboxType = blockType.getInteractionHitboxType();
        return interactionHitboxType != null
                && interactionHitboxType.toLowerCase().contains(DOOR_ID_MARKER);
    }

    private static boolean isTorchBlock(BlockType blockType) {
        String id = blockType.getId();
        if (id != null && id.toLowerCase().contains(TORCH_ID_MARKER)) {
            return true;
        }
        String hitboxType = blockType.getHitboxType();
        if (hitboxType != null && hitboxType.toLowerCase().contains(TORCH_ID_MARKER)) {
            return true;
        }
        String interactionHitboxType = blockType.getInteractionHitboxType();
        return interactionHitboxType != null
                && interactionHitboxType.toLowerCase().contains(TORCH_ID_MARKER);
    }

    private enum BlockCheck {
        SOLID,
        AIR,
        DOOR,
        TORCH,
        CHECKER,
        UNLOADED
    }

    public enum HousingResult {
        VALID,
        NO_INTERIOR,
        OPEN,
        TOO_LARGE,
        TOO_SMALL,
        TOO_SHORT,
        MULTIPLE_CHECKERS,
        NO_DOOR,
        NO_TORCH,
        UNLOADED
    }

    public static HousingResult validate(World world, Vector3i placed) {
        if (world == null || placed == null) {
            return HousingResult.NO_INTERIOR;
        }

        Long2ObjectOpenHashMap<BlockAccessor> chunkCache = new Long2ObjectOpenHashMap<>();
        Vector3i interiorStart = findInteriorStart(world, chunkCache, placed);
        if (interiorStart == null) {
            return HousingResult.NO_INTERIOR;
        }

        return checkClosedBox(world, chunkCache, interiorStart, placed);
    }

    public static String describeResult(HousingResult result) {
        return switch (result) {
            case NO_INTERIOR -> "no interior found";
            case TOO_LARGE -> "room too large";
            case TOO_SMALL -> "room too small";
            case TOO_SHORT -> "room too short";
            case NO_DOOR -> "no door found";
            case NO_TORCH -> "no torch found";
            case MULTIPLE_CHECKERS -> "multiple housing checkers in same house";
            case OPEN -> "room not closed";
            case UNLOADED -> "area not fully loaded";
            default -> "invalid";
        };
    }

    public static void clearCachedResult(World world, Vector3i checkerPos) {
        if (world == null || checkerPos == null) {
            return;
        }
        long key = positionKey(checkerPos);
        HousingResult previous = CACHED_RESULTS.remove(key);
        if (previous != null) {
            HousingRegistrySystem.unregisterHousing(world, checkerPos);
        }
    }


    static long positionKey(Vector3i pos) {
        long x = ((long) pos.x) & 0x3FFFFFFL;
        long z = ((long) pos.z) & 0x3FFFFFFL;
        long y = ((long) pos.y) & 0xFFFL;
        return (x << 38) | (z << 12) | y;
    }

    public static void revalidateNearby(World world, Vector3i changed, Player player) {
        if (world == null || changed == null || player == null) {
            return;
        }

        Long2ObjectOpenHashMap<BlockAccessor> chunkCache = new Long2ObjectOpenHashMap<>();
        int radius = MAX_RADIUS;

        for (int dx = -radius; dx <= radius; dx++) {
            int x = changed.x + dx;
            for (int dy = -radius; dy <= radius; dy++) {
                int y = changed.y + dy;
                for (int dz = -radius; dz <= radius; dz++) {
                    int z = changed.z + dz;

                    BlockAccessor accessor = getBlockAccessor(world, chunkCache, x, z);
                    if (accessor == null) {
                        continue;
                    }

                    int blockId = accessor.getBlock(x, y, z);
                    if (blockId == 0) {
                        continue;
                    }

                    BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
                    if (blockType == null || blockType.isUnknown()) {
                        continue;
                    }

                    if (!HOUSING_CHECKER_ITEM_ID.equals(blockType.getId())) {
                        continue;
                    }

                    Vector3i checkerPos = new Vector3i(x, y, z);
                    HousingResult result = validate(world, checkerPos);

                    long key = positionKey(checkerPos);
                    HousingResult previous = CACHED_RESULTS.get(key);
                    if (previous == result) {
                        continue;
                    }

                    CACHED_RESULTS.put(key, result);
                    HousingRegistrySystem.updateHousing(world, checkerPos, result == HousingResult.VALID);

                    if (result == HousingResult.VALID) {
                        player.sendMessage(Message.raw("Housing check updated: valid."));
                    } else {
                        player.sendMessage(Message.raw("Housing check updated: invalid (" + describeResult(result) + ")."));
                    }
                }
            }
        }
    }


}
