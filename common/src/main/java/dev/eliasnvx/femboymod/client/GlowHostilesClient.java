package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.effect.BuiltinEffects.GlowFriendsEffect;
import dev.eliasnvx.femboymod.effect.BuiltinEffects.GlowHostilesEffect;
import dev.eliasnvx.femboymod.effect.BuiltinEffects.MuffleSoundsEffect;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Client half of the wearer-only effects {@code femboymod:glow_hostiles}, {@code glow_friends} and
 * {@code muffle_sounds}: recomputed once per client tick from what the local player wears.
 */
public final class GlowHostilesClient {

    private static double hostileRadiusSq;
    private static double friendRadiusSq;
    private static boolean teammatesOnly;
    /** Sound id → volume factor; rebuilt only when the worn effects change. */
    private static final Map<ResourceLocation, Float> MUFFLED = new HashMap<>();
    private static WornEvaluator.Evaluation lastEvaluation;

    private GlowHostilesClient() {
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        double hostile = 0;
        double friends = 0;
        boolean onlyTeam = true;
        WornEvaluator.Evaluation evaluation = player == null ? null : WornEvaluator.evaluate(player);
        if (evaluation != null) {
            for (WornEvaluator.PlannedEffect planned : evaluation.effects()) {
                if (!planned.configured().when().map(c -> c.test(player)).orElse(true)) {
                    continue;
                }
                if (planned.configured().effect() instanceof GlowHostilesEffect glow) {
                    hostile = Math.max(hostile, glow.radius());
                } else if (planned.configured().effect() instanceof GlowFriendsEffect glow) {
                    friends = Math.max(friends, glow.radius());
                    onlyTeam &= glow.teammatesOnly();
                }
            }
        }
        hostileRadiusSq = hostile * hostile;
        friendRadiusSq = friends * friends;
        teammatesOnly = onlyTeam;
        if (evaluation != lastEvaluation) {
            lastEvaluation = evaluation;
            MUFFLED.clear();
            if (evaluation != null) {
                for (WornEvaluator.PlannedEffect planned : evaluation.effects()) {
                    if (planned.configured().effect() instanceof MuffleSoundsEffect muffle) {
                        muffle.sounds().forEach(id -> MUFFLED.merge(id, muffle.volume(), Math::min));
                    }
                }
            }
        }
    }

    public static boolean shouldGlow(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || entity == player) {
            return false;
        }
        double distanceSq = entity.distanceToSqr(player);
        if (hostileRadiusSq > 0 && entity instanceof Enemy && distanceSq <= hostileRadiusSq) {
            return true;
        }
        return friendRadiusSq > 0 && entity instanceof Player other && distanceSq <= friendRadiusSq
                && (!teammatesOnly || player.getTeam() == null || player.isAlliedTo(other));
    }

    /** Volume factor for a sound the local player hears (1 = unchanged). */
    public static float volumeFactor(ResourceLocation sound) {
        Float factor = MUFFLED.isEmpty() ? null : MUFFLED.get(sound);
        return factor == null ? 1.0F : factor;
    }
}
