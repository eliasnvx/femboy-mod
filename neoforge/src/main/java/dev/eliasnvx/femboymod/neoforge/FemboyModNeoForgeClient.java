package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.client.ConfigScreen;
import dev.eliasnvx.femboymod.client.FemboyModClient;
import dev.eliasnvx.femboymod.client.photo.PhotoItemRenderer;
import dev.eliasnvx.femboymod.client.render.CosmeticLayer;
import dev.eliasnvx.femboymod.client.render.pet.PetOutfitLayer;
import dev.eliasnvx.femboymod.client.render.pet.PetSpecies;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = FemboyMod.MOD_ID, dist = Dist.CLIENT)
public final class FemboyModNeoForgeClient {

    public FemboyModNeoForgeClient(IEventBus modBus, ModContainer container) {
        FemboyModClient.init();
        if (net.neoforged.fml.ModList.get().isLoaded("accessories")) {
            // after registration: the constructor runs before RegisterEvent, so our items and components aren't there yet
            modBus.addListener((FMLClientSetupEvent event) ->
                    event.enqueueWork(dev.eliasnvx.femboymod.compat.accessories.AccessoriesSlots::registerClient));
        }
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(parent));
        // Item colorway tints come from client/ItemTints (an ItemColor); 1.21.1 has no item tint sources.
        // Printed photos: 1.21.1 has no special item models; the photo item model extends builtin/entity
        modBus.addListener((RegisterClientExtensionsEvent event) -> event.registerItem(new PhotoExtensions(), FemboyItems.PHOTO.get()));
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                    renderer.addLayer(new CosmeticLayer(renderer, event.getEntityModels()));
                }
            }
            for (EntityType<?> type : PetSpecies.types()) {
                if (event.getRenderer(type) instanceof LivingEntityRenderer<?, ?> renderer) {
                    addPetOutfitLayer(renderer, event.getEntityModels(), PetSpecies.forType(type));
                }
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addPetOutfitLayer(LivingEntityRenderer renderer, EntityModelSet models, PetSpecies species) {
        renderer.addLayer(new PetOutfitLayer(renderer, models, species));
    }

    /** Draws the photo item through {@link PhotoItemRenderer}; the renderer is created on first use. */
    private static final class PhotoExtensions implements IClientItemExtensions {

        private BlockEntityWithoutLevelRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) {
                Minecraft minecraft = Minecraft.getInstance();
                renderer = new BlockEntityWithoutLevelRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels()) {
                    @Override
                    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                                             int light, int overlay) {
                        PhotoItemRenderer.render(stack, context, pose, buffers, light, overlay);
                    }
                };
            }
            return renderer;
        }
    }
}
