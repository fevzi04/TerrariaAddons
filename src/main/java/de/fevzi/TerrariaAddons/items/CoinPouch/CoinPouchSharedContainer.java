package de.fevzi.TerrariaAddons.items.CoinPouch;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer.ItemContainerChangeEvent;
import com.hypixel.hytale.event.EventRegistration;
import de.fevzi.TerrariaAddons.data.PlayerData;
import de.fevzi.TerrariaAddons.data.PlayerDataManager;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages shared coin pouch containers for all players.
 * Handles container creation, loading, saving, and capacity upgrades.
*/

public final class CoinPouchSharedContainer {
    private static final String POUCH_ID = "CoinPouch";
    private static final short BASE_CAPACITY = 4;
    private static final Map<UUID, SimpleItemContainer> CONTAINERS = new ConcurrentHashMap<>();
    private static final Map<UUID, EventRegistration> REGISTRATIONS = new ConcurrentHashMap<>();

    private CoinPouchSharedContainer() {
    }

    public static boolean hasPouch(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        ItemContainer[] sections = new ItemContainer[]{
                inventory.getHotbar(),
                inventory.getStorage(),
                inventory.getBackpack(),
                inventory.getUtility(),
                inventory.getTools(),
                inventory.getArmor()
        };

        for (ItemContainer section : sections) {
            if (section == null) {
                continue;
            }
            short capacity = section.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = section.getItemStack(slot);
                if (stack == null || ItemStack.isEmpty(stack)) {
                    continue;
                }
                if (POUCH_ID.equals(stack.getItemId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static short getCapacity(Inventory inventory, UUID uuid) {
        ItemContainer container = getOrCreateContainer(inventory, uuid);
        return container == null ? 0 : container.getCapacity();
    }

    public static ItemContainer getOrCreateContainer(Inventory inventory, UUID uuid) {
        if (inventory == null || !hasPouch(inventory)) {
            return null;
        }
        if (uuid == null) {
            return null;
        }
        SimpleItemContainer container = CONTAINERS.get(uuid);
        if (container == null) {
            container = loadContainer(uuid);
            CONTAINERS.put(uuid, container);
            
            convertCoinsInPouch(container, uuid);
        }

        registerAutoSave(uuid, container);
        return container;
    }

    private static final String COPPER_COIN_ID = CoinPouchCurrency.COPPER_COIN_ID;
    private static final String SILVER_COIN_ID = CoinPouchCurrency.SILVER_COIN_ID;
    private static final String GOLD_COIN_ID = CoinPouchCurrency.GOLD_COIN_ID;
    private static final String PLATINUM_COIN_ID = CoinPouchCurrency.PLATINUM_COIN_ID;
    private static final int CONVERSION_RATE = CoinPouchCurrency.CONVERSION_RATE;
    private static final Map<UUID, Boolean> CONVERTING = new ConcurrentHashMap<>();

    private static void registerAutoSave(UUID uuid, SimpleItemContainer container) {
        EventRegistration existing = REGISTRATIONS.get(uuid);
        if (existing != null) {
            return;
        }

        EventRegistration registration = container.registerChangeEvent((ItemContainerChangeEvent event) -> {
            saveContainer(uuid, container);
            
            if (!CONVERTING.containsKey(uuid) || !CONVERTING.get(uuid)) {
                convertCoinsInPouch(container, uuid);
            }
        });
        REGISTRATIONS.put(uuid, registration);
    }


    private static void convertCoinsInPouch(ItemContainer coinPouch, UUID uuid) {
        if (CONVERTING.getOrDefault(uuid, false)) {
            return;
        }
        
        CONVERTING.put(uuid, true);
        try {
            convertCoins(coinPouch, COPPER_COIN_ID, SILVER_COIN_ID);
            convertCoins(coinPouch, SILVER_COIN_ID, GOLD_COIN_ID);
            convertCoins(coinPouch, GOLD_COIN_ID, PLATINUM_COIN_ID);
        } finally {
            CONVERTING.put(uuid, false);
        }
    }

    private static void convertCoins(ItemContainer coinPouch,
                                     String lowerTierId,
                                     String higherTierId) {
        int totalLowerTier = 0;
        short capacity = coinPouch.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = coinPouch.getItemStack(i);
            if (stack != null && !stack.isEmpty() && lowerTierId.equals(stack.getItemId())) {
                totalLowerTier += stack.getQuantity();
            }
        }

        if (totalLowerTier < CONVERSION_RATE) {
            return;
        }

        int higherTierCount = totalLowerTier / CONVERSION_RATE;
        int remainingLowerTier = totalLowerTier % CONVERSION_RATE;

        for (short i = 0; i < capacity; i++) {
            ItemStack stack = coinPouch.getItemStack(i);
            if (stack != null && !stack.isEmpty() && lowerTierId.equals(stack.getItemId())) {
                coinPouch.setItemStackForSlot(i, ItemStack.EMPTY);
            }
        }

        if (remainingLowerTier > 0) {
            ItemStack remainingStack = new ItemStack(lowerTierId, remainingLowerTier);
            coinPouch.addItemStack(remainingStack);
        }

        if (higherTierCount > 0) {
            ItemStack higherTierStack = new ItemStack(higherTierId, higherTierCount);
            coinPouch.addItemStack(higherTierStack);
        }
    }

    public static int getTotalCoinsInCopper(Inventory inventory, UUID uuid) {
        ItemContainer container = getOrCreateContainer(inventory, uuid);
        if (container == null) {
            return 0;
        }
        return getTotalCoinsInCopper(container);
    }

    public static int getTotalCoinsInCopperCombined(Inventory inventory, UUID uuid) {
        return getInventoryCoinsInCopper(inventory) + getTotalCoinsInCopper(inventory, uuid);
    }

    public static int[] getCoinCounts(Inventory inventory, UUID uuid) {
        ItemContainer container = getOrCreateContainer(inventory, uuid);
        if (container == null) {
            return new int[]{0, 0, 0, 0};
        }
        return getCoinCounts(container);
    }

    public static int[] getCoinCountsCombined(Inventory inventory, UUID uuid) {
        int[] counts = new int[]{0, 0, 0, 0};
        addInventoryCoinCounts(inventory, counts);
        if (inventory != null && hasPouch(inventory)) {
            int[] pouchCounts = getCoinCounts(inventory, uuid);
            counts[0] += pouchCounts[0];
            counts[1] += pouchCounts[1];
            counts[2] += pouchCounts[2];
            counts[3] += pouchCounts[3];
        }
        return counts;
    }

    public static int[] getCoinCountsCombinedForDisplay(Inventory inventory, UUID uuid) {
        int[] counts = new int[]{0, 0, 0, 0};
        addInventoryCoinCounts(inventory, counts);

        if (inventory == null || !hasPouch(inventory)) {
            return counts;
        }
        SimpleItemContainer container = CONTAINERS.get(uuid);
        if (container == null) {
            container = loadContainer(uuid);
        }
        if (container != null) {
            int[] pouchCounts = getCoinCounts(container);
            counts[0] += pouchCounts[0];
            counts[1] += pouchCounts[1];
            counts[2] += pouchCounts[2];
            counts[3] += pouchCounts[3];
        }
        return counts;
    }

    public static boolean trySpendCoinsPreferInventory(Inventory inventory, UUID uuid, int costCopper) {
        if (costCopper <= 0) {
            return true;
        }
        if (inventory == null) {
            return false;
        }
        int inventoryTotal = getInventoryCoinsInCopper(inventory);
        if (inventoryTotal >= costCopper) {
            setInventoryCoinsFromCopper(inventory, inventoryTotal - costCopper);
            return true;
        }

        if (inventoryTotal > 0) {
            setInventoryCoinsFromCopper(inventory, 0);
        }
        return trySpendCoins(inventory, uuid, costCopper - inventoryTotal);
    }

    public static boolean trySpendCoins(Inventory inventory, UUID uuid, int costCopper) {
        if (costCopper <= 0) {
            return true;
        }
        ItemContainer container = getOrCreateContainer(inventory, uuid);
        if (container == null) {
            return false;
        }
        int total = getTotalCoinsInCopper(container);
        if (total < costCopper) {
            return false;
        }
        int remaining = total - costCopper;
        setCoinsFromCopper(container, uuid, remaining);
        return true;
    }

    public static boolean addCoins(Inventory inventory, UUID uuid, int amountCopper) {
        if (amountCopper <= 0) {
            return true;
        }
        ItemContainer container = getOrCreateContainer(inventory, uuid);
        if (container == null) {
            return false;
        }
        int total = getTotalCoinsInCopper(container);
        int newTotal = total + amountCopper;
        setCoinsFromCopper(container, uuid, newTotal);
        return true;
    }

    private static int getTotalCoinsInCopper(ItemContainer coinPouch) {
        int total = 0;
        short capacity = coinPouch.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = coinPouch.getItemStack(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            total += CoinPouchCurrency.getValueInCopper(stack.getItemId(), stack.getQuantity());
        }
        return total;
    }

    private static int[] getCoinCounts(ItemContainer coinPouch) {
        int copper = 0;
        int silver = 0;
        int gold = 0;
        int platinum = 0;
        short capacity = coinPouch.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = coinPouch.getItemStack(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String itemId = stack.getItemId();
            int qty = stack.getQuantity();
            if (COPPER_COIN_ID.equals(itemId)) {
                copper += qty;
            } else if (SILVER_COIN_ID.equals(itemId)) {
                silver += qty;
            } else if (GOLD_COIN_ID.equals(itemId)) {
                gold += qty;
            } else if (PLATINUM_COIN_ID.equals(itemId)) {
                platinum += qty;
            }
        }
        return new int[]{copper, silver, gold, platinum};
    }

    private static int getInventoryCoinsInCopper(Inventory inventory) {
        if (inventory == null) {
            return 0;
        }
        int total = 0;
        ItemContainer[] sections = new ItemContainer[]{
                inventory.getHotbar(),
                inventory.getStorage(),
                inventory.getBackpack(),
                inventory.getUtility(),
                inventory.getTools(),
                inventory.getArmor()
        };
        for (ItemContainer section : sections) {
            if (section == null) {
                continue;
            }
            short capacity = section.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = section.getItemStack(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                total += CoinPouchCurrency.getValueInCopper(stack.getItemId(), stack.getQuantity());
            }
        }
        return total;
    }

    private static void addInventoryCoinCounts(Inventory inventory, int[] counts) {
        if (inventory == null || counts == null || counts.length < 4) {
            return;
        }
        ItemContainer[] sections = new ItemContainer[]{
                inventory.getHotbar(),
                inventory.getStorage(),
                inventory.getBackpack(),
                inventory.getUtility(),
                inventory.getTools(),
                inventory.getArmor()
        };
        for (ItemContainer section : sections) {
            if (section == null) {
                continue;
            }
            short capacity = section.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = section.getItemStack(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                String itemId = stack.getItemId();
                int qty = stack.getQuantity();
                if (COPPER_COIN_ID.equals(itemId)) {
                    counts[0] += qty;
                } else if (SILVER_COIN_ID.equals(itemId)) {
                    counts[1] += qty;
                } else if (GOLD_COIN_ID.equals(itemId)) {
                    counts[2] += qty;
                } else if (PLATINUM_COIN_ID.equals(itemId)) {
                    counts[3] += qty;
                }
            }
        }
    }

    private static void setInventoryCoinsFromCopper(Inventory inventory, int totalCopper) {
        if (inventory == null) {
            return;
        }

        ItemContainer[] sections = new ItemContainer[]{
                inventory.getHotbar(),
                inventory.getStorage(),
                inventory.getBackpack(),
                inventory.getUtility(),
                inventory.getTools(),
                inventory.getArmor()
        };
        for (ItemContainer section : sections) {
            if (section == null) {
                continue;
            }
            short capacity = section.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = section.getItemStack(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                if (CoinPouchCurrency.isCoin(stack.getItemId())) {
                    section.setItemStackForSlot(slot, ItemStack.EMPTY);
                }
            }
        }

        int remaining = Math.max(totalCopper, 0);
        int platinum = remaining / CoinPouchCurrency.PLATINUM_VALUE;
        remaining %= CoinPouchCurrency.PLATINUM_VALUE;
        int gold = remaining / CoinPouchCurrency.GOLD_VALUE;
        remaining %= CoinPouchCurrency.GOLD_VALUE;
        int silver = remaining / CoinPouchCurrency.SILVER_VALUE;
        remaining %= CoinPouchCurrency.SILVER_VALUE;
        int copper = remaining;

        ItemContainer target = inventory.getCombinedHotbarFirst();
        if (platinum > 0) {
            target.addItemStack(new ItemStack(PLATINUM_COIN_ID, platinum));
        }
        if (gold > 0) {
            target.addItemStack(new ItemStack(GOLD_COIN_ID, gold));
        }
        if (silver > 0) {
            target.addItemStack(new ItemStack(SILVER_COIN_ID, silver));
        }
        if (copper > 0) {
            target.addItemStack(new ItemStack(COPPER_COIN_ID, copper));
        }
    }

    private static void setCoinsFromCopper(ItemContainer coinPouch, UUID uuid, int totalCopper) {
        if (CONVERTING.getOrDefault(uuid, false)) {
            return;
        }
        CONVERTING.put(uuid, true);
        try {
            short capacity = coinPouch.getCapacity();
            for (short i = 0; i < capacity; i++) {
                ItemStack stack = coinPouch.getItemStack(i);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                if (CoinPouchCurrency.isCoin(stack.getItemId())) {
                    coinPouch.setItemStackForSlot(i, ItemStack.EMPTY);
                }
            }

            int remaining = Math.max(totalCopper, 0);
            int platinum = remaining / CoinPouchCurrency.PLATINUM_VALUE;
            remaining %= CoinPouchCurrency.PLATINUM_VALUE;
            int gold = remaining / CoinPouchCurrency.GOLD_VALUE;
            remaining %= CoinPouchCurrency.GOLD_VALUE;
            int silver = remaining / CoinPouchCurrency.SILVER_VALUE;
            remaining %= CoinPouchCurrency.SILVER_VALUE;
            int copper = remaining;

            if (platinum > 0) {
                coinPouch.addItemStack(new ItemStack(PLATINUM_COIN_ID, platinum));
            }
            if (gold > 0) {
                coinPouch.addItemStack(new ItemStack(GOLD_COIN_ID, gold));
            }
            if (silver > 0) {
                coinPouch.addItemStack(new ItemStack(SILVER_COIN_ID, silver));
            }
            if (copper > 0) {
                coinPouch.addItemStack(new ItemStack(COPPER_COIN_ID, copper));
            }
        } finally {
            CONVERTING.put(uuid, false);
        }
    }

    private static SimpleItemContainer loadContainer(UUID uuid) {
        PlayerData playerData = PlayerDataManager.getPlayerData(uuid);
        
        int capacity = playerData.getCoinPouchCapacity();
        if (capacity <= 0) {
            capacity = BASE_CAPACITY;
        }
        
        SimpleItemContainer container = new SimpleItemContainer((short) capacity);

        try {
            String itemsJson = playerData.getCoinPouchItemsJson();
            if (itemsJson == null || itemsJson.isEmpty() || itemsJson.equals("[]")) {
                return container;
            }
            
            BsonDocument doc = BsonDocument.parse("{\"items\":" + itemsJson + "}");
            BsonArray items = doc.getArray("items", new BsonArray());
            
            for (int i = 0; i < items.size(); i++) {
                BsonDocument itemDoc = items.get(i).asDocument();
                short slot = (short) itemDoc.getInt32("slot").getValue();
                String itemId = itemDoc.getString("itemId").getValue();
                int quantity = itemDoc.getInt32("quantity").getValue();
                BsonDocument metadata = itemDoc.containsKey("metadata")
                        ? itemDoc.getDocument("metadata")
                        : null;
                ItemStack stack = new ItemStack(itemId, quantity, metadata);
                container.setItemStackForSlot(slot, stack);
            }
        } catch (Exception ignored) {
        }

        return container;
    }

    private static void saveContainer(UUID uuid, SimpleItemContainer container) {
        if (container == null) {
            return;
        }

        PlayerDataManager.updatePlayerData(uuid, (playerData) -> {
            playerData.setCoinPouchCapacity(container.getCapacity());
            
            BsonArray items = new BsonArray();
            short capacity = container.getCapacity();
            for (short slot = 0; slot < capacity; slot++) {
                ItemStack stack = container.getItemStack(slot);
                if (stack == null || ItemStack.isEmpty(stack)) {
                    continue;
                }

                BsonDocument itemDoc = new BsonDocument();
                itemDoc.put("slot", new BsonInt32(slot));
                itemDoc.put("itemId", new BsonString(stack.getItemId()));
                itemDoc.put("quantity", new BsonInt32(stack.getQuantity()));
                String metadataJson = stack.toPacket().metadata;
                if (metadataJson != null && !metadataJson.isBlank()) {
                    try {
                        itemDoc.put("metadata", BsonDocument.parse(metadataJson));
                    } catch (Exception ignored) {
                    }
                }
                items.add(itemDoc);
            }
            
            BsonDocument wrapper = new BsonDocument("items", items);
            String itemsJson = wrapper.toJson();
            if (itemsJson.startsWith("{\"items\":")) {
                itemsJson = itemsJson.substring(10, itemsJson.length() - 1); // Remove {"items": and }
            }
            playerData.setCoinPouchItemsJson(itemsJson);
        });
    }
}
