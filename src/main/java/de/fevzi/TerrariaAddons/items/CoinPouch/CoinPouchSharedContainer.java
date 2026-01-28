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

    private static final String COPPER_COIN_ID = "Ingredient_Coin_Copper";
    private static final String SILVER_COIN_ID = "Ingredient_Coin_Silver";
    private static final String GOLD_COIN_ID = "Ingredient_Coin_Gold";
    private static final String PLATINUM_COIN_ID = "Ingredient_Coin_Platinum";
    private static final int CONVERSION_RATE = 100;
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
