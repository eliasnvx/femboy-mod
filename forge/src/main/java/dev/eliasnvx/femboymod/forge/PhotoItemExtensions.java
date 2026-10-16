package dev.eliasnvx.femboymod.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.eliasnvx.femboymod.client.photo.PhotoItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Client extensions of the printed photo on Forge: its custom item renderer (Fabric uses BuiltinItemRendererRegistry).
 * Only built on the physical client, from {@code Item#initializeClient}.
 */
public final class PhotoItemExtensions implements IClientItemExtensions {

    private @Nullable BlockEntityWithoutLevelRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            // built late: the item is created before the client's renderers exist
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
