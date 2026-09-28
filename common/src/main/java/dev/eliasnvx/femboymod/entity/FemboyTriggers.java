package dev.eliasnvx.femboymod.entity;

import com.google.gson.JsonObject;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * One generic advancement trigger {@code femboymod:event} (SPEC §5.8):
 * {@code {"trigger": "femboymod:event", "conditions": {"event": "confetti_survivor", "min": 1}}}.
 * <p>
 * 1.20.1: there is no trigger registry; triggers go into {@link CriteriaTriggers#register} and read their
 * conditions from JSON (no codecs).
 */
public final class FemboyTriggers {

    public static final String CONFETTI_SURVIVOR = "confetti_survivor";
    public static final String PROGRAMMING_SOCKS = "programming_socks";
    public static final String DRIP_TIER = "drip_tier";
    public static final String ENERGY_DRINKS = "energy_drinks";
    public static final String SET_BONUS = "set_bonus";
    /** Stars of the gamer setup when sitting down (value = stars). */
    public static final String SETUP_RATING = "setup_rating";
    /** Sat in a Gamer Chair at one heart or less. */
    public static final String CHAIR_BREAK = "chair_break";
    /** Cosmetics put on during the current day (value = count). */
    public static final String OUTFIT_CHANGES = "outfit_changes";
    /** Drank Byte Energy while wearing programming socks. */
    public static final String READY_TO_DEPLOY = "ready_to_deploy";
    /** Explained your code to the rubber duck and got Insight. */
    public static final String DUCK_DEBUGGING = "duck_debugging";
    /** A Vibe Check Scanner rated the player (value = score). */
    public static final String VIBE_CHECK = "vibe_check";

    public static final ResourceLocation EVENT_ID = new ResourceLocation(FemboyMod.MOD_ID, "event");
    private static final String EVENT_KEY = "event";
    private static final String MIN_KEY = "min";
    private static final int DEFAULT_MIN = 1;

    /** Keeps the 1.21 call site {@code FemboyTriggers.REGISTER.register()} working. */
    public static final Registration REGISTER = new Registration();

    @Nullable
    private static EventTrigger event;

    private FemboyTriggers() {
    }

    /** Registers the trigger with vanilla; call once during mod init. */
    public static synchronized void init() {
        if (event == null) {
            event = CriteriaTriggers.register(new EventTrigger());
        }
    }

    /** The registered trigger, or null before {@link #init()}. */
    @Nullable
    public static EventTrigger event() {
        return event;
    }

    public static void fire(ServerPlayer player, String event) {
        fire(player, event, 1);
    }

    public static void fire(ServerPlayer player, String name, int value) {
        EventTrigger trigger = event;
        if (trigger != null) {
            trigger.trigger(player, name, value);
        }
    }

    /** See {@link #REGISTER}. */
    public static final class Registration {
        private Registration() {
        }

        public void register() {
            init();
        }
    }

    public static final class EventTrigger extends SimpleCriterionTrigger<EventTrigger.Instance> {
        @Override
        public ResourceLocation getId() {
            return EVENT_ID;
        }

        @Override
        protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
            return new Instance(player, GsonHelper.getAsString(json, EVENT_KEY), GsonHelper.getAsInt(json, MIN_KEY, DEFAULT_MIN));
        }

        void trigger(ServerPlayer player, String event, int value) {
            trigger(player, instance -> instance.event().equals(event) && value >= instance.min());
        }

        public static final class Instance extends AbstractCriterionTriggerInstance {
            private final String event;
            private final int min;

            public Instance(ContextAwarePredicate player, String event, int min) {
                super(EVENT_ID, player);
                this.event = event;
                this.min = min;
            }

            public String event() {
                return event;
            }

            public int min() {
                return min;
            }

            @Override
            public JsonObject serializeToJson(SerializationContext context) {
                JsonObject json = super.serializeToJson(context);
                json.addProperty(EVENT_KEY, event);
                json.addProperty(MIN_KEY, min);
                return json;
            }
        }
    }
}
