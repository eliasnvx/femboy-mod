package dev.eliasnvx.femboymod.mixin.client;

import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Armor under the outfit: visual only, the armor still protects (ArmorHiding, game rule allow_hidden_armor).
 * The armor layer sees an empty slot for hidden pieces. Elytra are drawn by their own layer and stay visible.
 *
 * <p>{@code renderArmorPiece*} matches both the vanilla method (Fabric) and NeoForge's overload with the extra
 * animation arguments, which is the one that reads the slot there.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Redirect(method = "renderArmorPiece*", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack femboymod$hideArmor(LivingEntity entity, EquipmentSlot slot) {
        ItemStack worn = entity.getItemBySlot(slot);
        if (entity instanceof Player player && !worn.isEmpty()) {
            int index = ArmorHiding.ARMOR_SLOTS.indexOf(slot);
            if (index >= 0 && (ArmorHiding.cachedMask(player) & (1 << index)) != 0) {
                return ItemStack.EMPTY;
            }
        }
        return worn;
    }
}
