package de.fevzi.TerrariaAddons.items.potions.wormhole;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;

import javax.annotation.Nonnull;

/**
 * Choice interaction that teleports the player to another player.
 * Used by the Wormhole Potion's player selection UI to execute
 * the actual teleportation when a target player is selected.
 */
public class TeleportInteraction extends ChoiceInteraction {
    private final Ref<EntityStore> targetRef;

    public TeleportInteraction(Ref<EntityStore> targetRef) {
        this.targetRef = targetRef;
    }

    @Override
    public void run(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef) {
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        PageManager pageManager = playerComponent.getPageManager();

        Store<EntityStore> targetStore = this.targetRef.getStore();

        if (targetStore == null) {
            playerRef.sendMessage(Message.translation("Target not found!"));
            return;
        }

        TransformComponent targetTransformComp = (TransformComponent) targetStore.getComponent(this.targetRef, TransformComponent.getComponentType());

        if (targetTransformComp != null) {
            Vector3d targetPos = targetTransformComp.getTransform().getPosition();

            World world = ((EntityStore) store.getExternalData()).getWorld();

            if (world != null) {
                world.execute(() -> {
                    if (!ref.isValid()) return;

                    Transform destTransform = new Transform(targetPos.x, targetPos.y + 1.0, targetPos.z);

                    Teleport teleportPacket = Teleport.createForPlayer(world, destTransform);

                    store.addComponent(ref, Teleport.getComponentType(), teleportPacket);


                    if (playerComponent.getInventory() != null) {
                        Inventory inventory = playerComponent.getInventory();
                        ItemStack heldItem = inventory.getItemInHand();

                        if (heldItem != null && !heldItem.isEmpty()) {
                            byte slot = inventory.getActiveHotbarSlot();
                            ItemContainer container = inventory.getCombinedHotbarFirst();

                            container.removeItemStackFromSlot(slot);

                            if (heldItem.getQuantity() > 1) {
                                container.addItemStackToSlot(slot, heldItem.withQuantity(heldItem.getQuantity() - 1));
                            }
                        }
                    }

                    playerRef.sendMessage(Message.translation("Teleported!"));
                });
            }
        } else {
            playerRef.sendMessage(Message.translation("Target location unknown!"));
        }

        pageManager.setPage(ref, store, Page.None);
    }
}