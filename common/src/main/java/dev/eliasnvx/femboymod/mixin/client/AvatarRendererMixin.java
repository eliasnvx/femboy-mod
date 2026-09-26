package dev.eliasnvx.femboymod.mixin.client;

import net.minecraft.world.entity.player.Player;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.client.render.CosmeticRenderData;
import dev.eliasnvx.femboymod.client.render.CosmeticRenderStateAccess;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies what a player wears into its render state (the layer only sees the state in 26.x). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void femboymod$extractCosmetics(Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ((CosmeticRenderStateAccess) state).femboymod$setCosmetics(CosmeticRenderData.capture(entity, state));
        // Armor under the outfit: visual only, the armor still protects (ArmorHiding, game rule allow_hidden_armor)
        if (entity instanceof Player player) {
            int hidden = ArmorHiding.cachedMask(player);
            if (hidden != 0) {
                state.headEquipment = ArmorHiding.visible(state.headEquipment, (hidden & 1) != 0);
                state.chestEquipment = ArmorHiding.visible(state.chestEquipment, (hidden & 2) != 0);
                state.legsEquipment = ArmorHiding.visible(state.legsEquipment, (hidden & 4) != 0);
                state.feetEquipment = ArmorHiding.visible(state.feetEquipment, (hidden & 8) != 0);
            }
        }
    }
}
