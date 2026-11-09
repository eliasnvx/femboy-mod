package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.effect.BuiltinEffects.GlowFriendsEffect;
import dev.eliasnvx.femboymod.effect.BuiltinEffects.GlowHostilesEffect;
import dev.eliasnvx.femboymod.effect.BuiltinEffects.MuffleSoundsEffect;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Client half of the wearer-only effects {@code femboymod:glow_hostiles}, {@code glow_friends} and
 * {@code muffle_sounds}: recomputed once per client tick from what the local player wears.
 *
 * <p>Friends glow without wall hacks: teammates (when the wearer is on a team) glow through walls, any other player
 * only while the wearer can see them ({@code teammates_only} leaves those out when the wearer has a team). Worked out
 * once a tick, so the per-frame glow check is a set lookup.
 */
public final class GlowHostilesClient {

    private static double hostileRadiusSq;
    private static double friendRadiusSq;
    private static boolean teammatesOnly;
    /** Players that glow as friends this tick (entity ids). */
    private static final IntOpenHashSet FRIENDS = new IntOpenHashSet();
    /** Sound id → volume factor; rebuilt only when the worn effects change. */
    private static final Map<Identifier, Float> MUFFLED = new HashMap<>();
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
        FRIENDS.clear();
        if (player != null && friendRadiusSq > 0) {
            for (Player other : player.level().players()) {
                if (other != player && !other.isSpectator() && other.distanceToSqr(player) <= friendRadiusSq && isFriend(player, other)) {
                    FRIENDS.add(other.getId());
                }
            }
        }
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
        return entity instanceof Player && FRIENDS.contains(entity.getId());
    }

    /** Teammates through walls; other players only in sight, and not at all when only teammates count and there's a team. */
    private static boolean isFriend(LocalPlayer player, Player other) {
        boolean team = player.getTeam() != null;
        if (team && player.isAlliedTo(other)) {
            return true;
        }
        return (!teammatesOnly || !team) && player.hasLineOfSight(other);
    }

    /** Volume factor for a sound the local player hears (1 = unchanged). */
    public static float volumeFactor(Identifier sound) {
        Float factor = MUFFLED.isEmpty() ? null : MUFFLED.get(sound);
        return factor == null ? 1.0F : factor;
    }
}
