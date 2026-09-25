package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.registry.FemboySounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/** "Nya" when jumping in the full set (SPEC §5.7). Off by default; only you hear it. */
public final class NyaSound {

    private static final Identifier FULL_SET = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "full_femboy_mode");
    private static final float VOLUME = 0.6F;
    private static boolean wasOnGround = true;

    private NyaSound() {
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        boolean onGround = player.onGround();
        boolean jumped = wasOnGround && !onGround && player.getDeltaMovement().y > 0;
        wasOnGround = onGround;
        if (jumped && FemboyConfig.client().nyaSound() && WornEvaluator.evaluate(player).activeSets().contains(FULL_SET)) {
            player.playSound(FemboySounds.NYA.get(), VOLUME, 1.0F + (player.getRandom().nextFloat() - 0.5F) * 0.2F);
        }
    }
}
