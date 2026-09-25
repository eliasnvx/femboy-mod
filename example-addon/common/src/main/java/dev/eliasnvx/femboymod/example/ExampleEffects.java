package dev.eliasnvx.femboymod.example;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.effect.CosmeticCondition;
import dev.eliasnvx.femboymod.api.effect.CosmeticEffect;
import dev.eliasnvx.femboymod.api.effect.EffectSource;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * A custom effect type and a custom condition type, usable from any set bonus or charm JSON:
 * {@code {"effect": {"type": "femboymod_example:xp_trickle", "interval": 200}, "when": {"type": "femboymod_example:daytime"}}}.
 */
public final class ExampleEffects {

    public static final Identifier XP_TRICKLE = Identifier.fromNamespaceAndPath(ExampleAddon.MOD_ID, "xp_trickle");
    public static final Identifier DAYTIME = Identifier.fromNamespaceAndPath(ExampleAddon.MOD_ID, "daytime");

    private ExampleEffects() {
    }

    static void register(FemboyApi api) {
        api.cosmeticEffectTypes().register(XP_TRICKLE, XpTrickle.CODEC);
        api.cosmeticConditionTypes().register(DAYTIME, Daytime.CODEC);
    }

    /** Gives {@code amount} experience every {@code interval} ticks while active (server side). */
    public record XpTrickle(int interval, int amount) implements CosmeticEffect {
        public static final MapCodec<XpTrickle> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("interval").forGetter(XpTrickle::interval),
                Codec.intRange(1, 100).optionalFieldOf("amount", 1).forGetter(XpTrickle::amount)
        ).apply(instance, XpTrickle::new));

        @Override
        public MapCodec<XpTrickle> codec() {
            return CODEC;
        }

        @Override
        public void tick(ServerPlayer player, EffectSource source) {
            if (player.tickCount % interval == 0) {
                player.giveExperiencePoints(amount);
            }
        }
    }

    /** True while the sky is bright where the player is. */
    public enum Daytime implements CosmeticCondition {
        INSTANCE;
        public static final MapCodec<Daytime> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<Daytime> codec() {
            return CODEC;
        }

        @Override
        public boolean test(Player player) {
            return player.level().isBrightOutside();
        }
    }
}
