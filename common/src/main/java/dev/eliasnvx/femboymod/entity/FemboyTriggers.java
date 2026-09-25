package dev.eliasnvx.femboymod.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

/**
 * One generic advancement trigger {@code femboymod:event} (SPEC §5.8):
 * {@code {"trigger": "femboymod:event", "conditions": {"event": "confetti_survivor", "min": 1}}}.
 */
public final class FemboyTriggers {

    public static final String CONFETTI_SURVIVOR = "confetti_survivor";
    public static final String PROGRAMMING_SOCKS = "programming_socks";
    public static final String DRIP_TIER = "drip_tier";
    public static final String ENERGY_DRINKS = "energy_drinks";
    public static final String SET_BONUS = "set_bonus";

    public static final DeferredRegister<CriterionTrigger<?>> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.TRIGGER_TYPE);
    public static final RegistrySupplier<EventTrigger> EVENT = REGISTER.register("event", EventTrigger::new);

    private FemboyTriggers() {
    }

    public static void fire(ServerPlayer player, String event) {
        fire(player, event, 1);
    }

    public static void fire(ServerPlayer player, String event, int value) {
        if (EVENT.isPresent()) {
            EVENT.get().trigger(player, event, value);
        }
    }

    public static final class EventTrigger extends SimpleCriterionTrigger<EventTrigger.Instance> {
        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        void trigger(ServerPlayer player, String event, int value) {
            trigger(player, instance -> instance.event().equals(event) && value >= instance.min());
        }

        public record Instance(Optional<Holder<LootItemCondition>> player, String event, int min)
                implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                    LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                    Codec.STRING.fieldOf("event").forGetter(Instance::event),
                    Codec.INT.optionalFieldOf("min", 1).forGetter(Instance::min)
            ).apply(i, Instance::new));
        }
    }
}
