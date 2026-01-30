package de.fevzi.TerrariaAddons.items.accessoryPouch;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer.ItemContainerChangeEvent;
import com.hypixel.hytale.event.EventRegistration;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonString;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages shared accessory pouch containers for all players.
 * Handles container creation, loading, saving, and capacity upgrades.
 * Each player's pouch is persisted to disk and loaded on demand.
 * Provides utility methods to check for equipped accessories.
 */
public final class AccessoryPouchSharedContainer {
    private static final String POUCH_ID = "AccessoryPouch";
    private static final short BASE_CAPACITY = 2;
    private static final Map<UUID, SimpleItemContainer> CONTAINERS = new ConcurrentHashMap<>();
    private static final Map<UUID, EventRegistration> REGISTRATIONS = new ConcurrentHashMap<>();

    private AccessoryPouchSharedContainer() {
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
        }

        registerAutoSave(uuid, container);
        return container;
    }

    public static boolean upgradeContainer(Inventory inventory, UUID uuid, short targetCapacity) {
        if (inventory == null || !hasPouch(inventory)) {
            return false;
        }
        if (uuid == null) {
            return false;
        }
        SimpleItemContainer oldContainer = (SimpleItemContainer) getOrCreateContainer(inventory, uuid);
        if (oldContainer == null || oldContainer.getCapacity() >= targetCapacity) {
            return false;
        }

        SimpleItemContainer newContainer = new SimpleItemContainer(targetCapacity);
        copyContents(oldContainer, newContainer);
        CONTAINERS.put(uuid, newContainer);
        REGISTRATIONS.remove(uuid);
        registerAutoSave(uuid, newContainer);
        saveContainer(uuid, newContainer);
        return true;
    }

    public static boolean hasItemInPouch(Inventory inventory, UUID uuid, String itemId) {
        if (inventory == null || !hasPouch(inventory)) {
            return false;
        }

        ItemContainer pouchContainer = getOrCreateContainer(inventory, uuid);
        if (pouchContainer == null) {
            return false;
        }

        short capacity = pouchContainer.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = pouchContainer.getItemStack(slot);
            if (stack != null && !ItemStack.isEmpty(stack) && itemId.equals(stack.getItemId())) {
                return true;
            }
        }

        return false;
    }

    private static void registerAutoSave(UUID uuid, SimpleItemContainer container) {
        EventRegistration existing = REGISTRATIONS.get(uuid);
        if (existing != null) {
            return;
        }

        EventRegistration registration = container.registerChangeEvent((ItemContainerChangeEvent event) -> {
            saveContainer(uuid, container);
        });
        REGISTRATIONS.put(uuid, registration);
    }

    private static SimpleItemContainer loadContainer(UUID uuid) {
        Path file = getSavePath(uuid);
        if (!Files.exists(file)) {
            return new SimpleItemContainer(BASE_CAPACITY);
        }

        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            BsonDocument doc = BsonDocument.parse(json);
            int capacity = doc.getInt32("capacity", new BsonInt32(BASE_CAPACITY)).getValue();
            SimpleItemContainer container = new SimpleItemContainer((short) capacity);

            BsonArray items = doc.getArray("items", new BsonArray());
            for (int i = 0; i < items.size(); i++) {
                BsonDocument itemDoc = items.get(i).asDocument();
                short slot = (short) itemDoc.getInt32("slot").getValue();
                String itemId = itemDoc.getString("itemId").getValue();
                int quantity = itemDoc.getInt32("quantity").getValue();
                double durability = itemDoc.getDouble("durability").getValue();
                double maxDurability = itemDoc.getDouble("maxDurability").getValue();
                BsonDocument metadata = itemDoc.containsKey("metadata")
                        ? itemDoc.getDocument("metadata")
                        : null;
                ItemStack stack = new ItemStack(itemId, quantity, durability, maxDurability, metadata);
                container.setItemStackForSlot(slot, stack);
            }

            return container;
        } catch (Exception ignored) {
            return new SimpleItemContainer(BASE_CAPACITY);
        }
    }

    private static void saveContainer(UUID uuid, SimpleItemContainer container) {
        if (container == null) {
            return;
        }

        BsonDocument doc = new BsonDocument();
        doc.put("capacity", new BsonInt32(container.getCapacity()));
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
            itemDoc.put("durability", new BsonDouble(stack.getDurability()));
            itemDoc.put("maxDurability", new BsonDouble(stack.getMaxDurability()));
            String metadataJson = stack.toPacket().metadata;
            if (metadataJson != null && !metadataJson.isBlank()) {
                itemDoc.put("metadata", BsonDocument.parse(metadataJson));
            }
            items.add(itemDoc);
        }

        doc.put("items", items);

        try {
            Path file = getSavePath(uuid);
            Files.createDirectories(file.getParent());
            Files.writeString(file, doc.toJson(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private static Path getSavePath(UUID uuid) {
        Path base = Paths.get(System.getProperty("user.dir"), "AccessoryPouchData");
        return base.resolve(uuid + ".json");
    }

    private static void copyContents(SimpleItemContainer from, SimpleItemContainer to) {
        short capacity = from.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = from.getItemStack(slot);
            if (stack != null && !ItemStack.isEmpty(stack)) {
                to.setItemStackForSlot(slot, stack);
            }
        }
    }

}
