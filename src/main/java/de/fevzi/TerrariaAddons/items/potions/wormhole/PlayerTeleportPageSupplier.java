package de.fevzi.TerrariaAddons.items.potions.wormhole;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Supplier that creates PlayerTeleportPage instances for the wormhole potion UI.
 * Registered as a custom page supplier to display the player selection interface.
 */
public class PlayerTeleportPageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {
    public static final BuilderCodec<PlayerTeleportPageSupplier> CODEC;
    static {
        CODEC = BuilderCodec.builder(PlayerTeleportPageSupplier.class, PlayerTeleportPageSupplier::new).build();
    }

    @Override
    public CustomUIPage tryCreate(@Nonnull Ref<EntityStore> ref, @Nonnull ComponentAccessor<EntityStore> componentAccessor, @Nonnull PlayerRef playerRef, @Nonnull InteractionContext context) {
        return new PlayerTeleportPage(playerRef);
    }
}