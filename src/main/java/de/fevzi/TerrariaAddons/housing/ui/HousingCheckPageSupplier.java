package de.fevzi.TerrariaAddons.housing.ui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class HousingCheckPageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {
    public static final BuilderCodec<HousingCheckPageSupplier> CODEC;

    static {
        CODEC = BuilderCodec.builder(HousingCheckPageSupplier.class, HousingCheckPageSupplier::new).build();
    }

    @Override
    public CustomUIPage tryCreate(@Nonnull Ref<EntityStore> ref,
                                  @Nonnull ComponentAccessor<EntityStore> componentAccessor,
                                  @Nonnull PlayerRef playerRef,
                                  @Nonnull InteractionContext context) {
        return HousingCheckPage.create(ref, componentAccessor, playerRef, context);
    }
}
