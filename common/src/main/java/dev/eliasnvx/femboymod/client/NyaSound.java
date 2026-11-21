package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.config.ClientConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** "Nya" when jumping while wearing any ears (or the cat-ear hoodie). Off by default; only you hear it. */
public final class NyaSound {

    private static final float MIN_PITCH = 0.94F;
    private static final float PITCH_RANGE = 0.14F;

    private NyaSound() {
    }

    /** Called from {@code LivingEntityJumpMixin} on the client whenever a living entity jumps. */
    public static void onJump(LivingEntity entity) {
        if (!(entity instanceof LocalPlayer player)) {
            return;
        }
        ClientConfig config = FemboyConfig.client();
        if (player != Minecraft.getInstance().player || !config.nyaSound() || config.nyaVolume() <= 0) {
            return;
        }
        if (wearingEars(player)) {
            float volume = config.nyaVolume() / 100.0F;
            float pitch = MIN_PITCH + player.getRandom().nextFloat() * PITCH_RANGE;
            player.playSound(FemboySounds.NYA.get(), volume, pitch);
        }
    }

    private static boolean wearingEars(LocalPlayer player) {
        for (ItemStack stack : CosmeticsManager.worn(player).all().values()) {
            if (stack.is(FemboyTags.EARS) || stack.is(FemboyItems.CAT_EAR_HOODIE.get())) {
                return true;
            }
        }
        return false;
    }
}
