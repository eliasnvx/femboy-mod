package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.effect.BuiltinEffects.GlowHostilesEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;

/** Client half of {@code femboymod:glow_hostiles}: recomputed once per client tick. */
public final class GlowHostilesClient {

    private static double radiusSq;

    private GlowHostilesClient() {
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        double radius = 0;
        if (player != null) {
            for (WornEvaluator.PlannedEffect planned : WornEvaluator.evaluate(player).effects()) {
                if (planned.configured().effect() instanceof GlowHostilesEffect glow
                        && planned.configured().when().map(c -> c.test(player)).orElse(true)) {
                    radius = Math.max(radius, glow.radius());
                }
            }
        }
        radiusSq = radius * radius;
    }

    public static boolean shouldGlow(Entity entity) {
        if (radiusSq <= 0 || !(entity instanceof Enemy)) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && entity.distanceToSqr(player) <= radiusSq;
    }
}
