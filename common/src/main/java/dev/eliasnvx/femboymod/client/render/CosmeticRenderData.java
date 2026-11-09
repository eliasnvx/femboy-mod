package dev.eliasnvx.femboymod.client.render;

import dev.eliasnvx.femboymod.api.client.CosmeticMotion;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.Cosmetic;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import dev.eliasnvx.femboymod.config.ClientConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-player render data, captured by the cosmetic layer each frame. One instance per entity is reused
 * across frames: the worn list is rebuilt only when the (immutable) cosmetic inventory snapshot changes,
 * and motion values are updated in place, so capturing does not allocate.
 * Reactions (ears and tails) get their targets from {@link ReactiveCosmetics} on the client tick and are
 * smoothed here per frame.
 */
public final class CosmeticRenderData implements CosmeticMotion {

    /** Neutral motion for states without data. */
    public static final CosmeticMotion STILL = new CosmeticMotion() {
        @Override
        public float turnSway() {
            return 0;
        }

        @Override
        public float walkAmount() {
            return 0;
        }

        @Override
        public float phase() {
            return 0;
        }
    };

    /** Radians of tail sway per degree/tick of body turn. */
    private static final float TURN_GAIN = 0.035F;
    private static final float MAX_TURN_SWAY = 0.9F;
    /** Higher = snappier smoothing (per tick). */
    private static final float SMOOTHING = 0.25F;
    private static final float GOLDEN_ANGLE = 2.39996F;
    /** How far ears turn toward what they noticed (radians); further and they'd be edge-on from the front. */
    private static final float MAX_EAR_TURN = 1.0F;
    /** Smoothing per tick of each reaction, when it grows / when it fades: flinches are quick, recovery is slow. */
    private static final float ALERT_RISE = 0.35F;
    private static final float ALERT_FALL = 0.08F;
    private static final float EAR_TURN_RATE = 0.3F;
    private static final float FLATTEN_RISE = 0.6F;
    private static final float FLATTEN_FALL = 0.1F;
    private static final float DROOP_RATE = 0.1F;
    private static final float WAG_RISE = 0.2F;
    private static final float WAG_FALL = 0.05F;
    private static final float PUFF_RISE = 0.6F;
    private static final float PUFF_FALL = 0.04F;
    private static final float JITTER_RISE = 0.2F;
    private static final float JITTER_FALL = 0.05F;

    public record Worn(ResourceLocation slot, ItemStack stack, ResourceLocation itemId, ResourceLocation renderer, @Nullable Colorway colorway) {
    }

    private static final Map<Entity, CosmeticRenderData> CACHE = new WeakHashMap<>();

    private CosmeticInventory source;
    private List<Worn> worn = List.of();
    private final float phase;
    private float turnSway;
    private float walkAmount;
    private float lastAge = Float.NaN;
    private float lastBodyRot;

    private @Nullable EarHearing hearing;
    private int hearingGeneration = -1;
    private CosmeticInventory hearingSource;
    /** Reaction targets, set on the client tick; NaN yaw = nothing noticed. */
    private float alertTarget;
    private float noticeYaw = Float.NaN;
    private float flattenTarget;
    private float droopTarget;
    private float wagTarget;
    private float puffTarget;
    private float jitterTarget;
    /** Smoothed reactions. */
    private float lastReactionAge = Float.NaN;
    private float alert;
    private float earYaw;
    private float flatten;
    private float droop;
    private float wag;
    private float puff;
    private float jitter;

    private CosmeticRenderData(int entityId) {
        this.phase = (entityId * GOLDEN_ANGLE) % Mth.TWO_PI;
    }

    /** Called from the cosmetic layer once per frame per player. Returns null if nothing is worn. */
    public static @Nullable CosmeticRenderData capture(LivingEntity entity, float partialTick) {
        if (!(entity instanceof Player player)) {
            return null;
        }
        CosmeticRenderData data = of(player);
        if (data != null) {
            data.updateMotion(entity, partialTick);
        }
        return data;
    }

    /** The player's data with an up-to-date worn list (created if needed), or null if nothing is worn. */
    public static @Nullable CosmeticRenderData of(Player player) {
        CosmeticInventory inventory = CosmeticsManager.worn(player);
        if (inventory.isEmpty()) {
            return null;
        }
        CosmeticRenderData data = CACHE.computeIfAbsent(player, e -> new CosmeticRenderData(e.getId()));
        if (data.source != inventory) {
            data.rebuild(inventory);
        }
        return data;
    }

    /**
     * Motion of an entity for procedural animation (FemboyClientApi#motion). Neutral for entities that were
     * never captured (nothing worn, or not a player).
     */
    public static CosmeticMotion motion(LivingEntity entity) {
        CosmeticRenderData data = CACHE.get(entity);
        return data == null ? STILL : data;
    }

    private void rebuild(CosmeticInventory inventory) {
        List<Worn> list = new ArrayList<>(inventory.all().size());
        inventory.all().forEach((slot, stack) -> {
            if (inventory.isHidden(slot)) {
                return; // worn but hidden by the wearer
            }
            Cosmetic cosmetic = FemboyComponents.COSMETIC.get(stack);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            ResourceLocation renderer = cosmetic != null && cosmetic.renderer().isPresent() ? cosmetic.renderer().get() : itemId;
            list.add(new Worn(slot, stack, itemId, renderer, Colorways.effective(stack).orElse(null)));
        });
        this.worn = List.copyOf(list);
        this.source = inventory;
    }

    private void updateMotion(LivingEntity entity, float partialTick) {
        float age = entity.tickCount + partialTick;
        float bodyRot = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        if (Float.isNaN(lastAge)) {
            lastAge = age;
            lastBodyRot = bodyRot;
            return;
        }
        float dt = age - lastAge;
        if (dt <= 0) {
            return; // same frame (e.g. inventory preview) or paused
        }
        float turnRate = Mth.wrapDegrees(bodyRot - lastBodyRot) / dt;
        float targetSway = Mth.clamp(-turnRate * TURN_GAIN, -MAX_TURN_SWAY, MAX_TURN_SWAY);
        float alpha = 1.0F - (float) Math.exp(-dt * SMOOTHING);
        turnSway += (targetSway - turnSway) * alpha;
        walkAmount += (Math.min(entity.walkAnimation.speed(partialTick), 1.0F) - walkAmount) * alpha;
        lastAge = age;
        lastBodyRot = bodyRot;
        updateReactions(age, Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot));
    }

    /** How this wearer's reactive cosmetics hear, or null if nothing worn reacts. */
    public @Nullable EarHearing hearing() {
        if (hearingSource != source || hearingGeneration != EarHearing.generation()) {
            hearing = EarHearing.find(worn);
            hearingSource = source;
            hearingGeneration = EarHearing.generation();
        }
        return hearing;
    }

    /** New reaction targets from the client tick; {@code noticeYaw} is a world yaw, or NaN for none. */
    void setReactionTargets(float alert, float noticeYaw, float flatten, float droop, float wag, float puff, float jitter) {
        this.alertTarget = alert;
        this.noticeYaw = noticeYaw;
        this.flattenTarget = flatten;
        this.droopTarget = droop;
        this.wagTarget = wag;
        this.puffTarget = puff;
        this.jitterTarget = jitter;
    }

    /**
     * Smooths the reactions toward their targets. Called per frame from the cosmetic layer, and from the first-person
     * ear tips for the local player (whose layer isn't drawn in first person); a second call in the same frame does
     * nothing.
     *
     * @param headYaw world yaw the wearer's head faces (degrees)
     */
    void updateReactions(float age, float headYaw) {
        if (Float.isNaN(lastReactionAge)) {
            lastReactionAge = age;
            return;
        }
        float dt = age - lastReactionAge;
        if (dt <= 0) {
            return;
        }
        lastReactionAge = age;
        float yawTarget = Float.isNaN(noticeYaw) ? 0.0F : ReactionMath.earYaw(noticeYaw, headYaw, MAX_EAR_TURN);
        alert = ReactionMath.approach(alert, alertTarget, ALERT_RISE, ALERT_FALL, dt);
        earYaw = ReactionMath.approach(earYaw, yawTarget, EAR_TURN_RATE, EAR_TURN_RATE, dt);
        flatten = ReactionMath.approach(flatten, flattenTarget, FLATTEN_RISE, FLATTEN_FALL, dt);
        droop = ReactionMath.approach(droop, droopTarget, DROOP_RATE, DROOP_RATE, dt);
        wag = ReactionMath.approach(wag, wagTarget, WAG_RISE, WAG_FALL, dt);
        puff = ReactionMath.approach(puff, puffTarget, PUFF_RISE, PUFF_FALL, dt);
        jitter = ReactionMath.approach(jitter, jitterTarget, JITTER_RISE, JITTER_FALL, dt);
    }

    public List<Worn> worn() {
        return worn;
    }

    @Override
    public float turnSway() {
        return FemboyConfig.client().physics() == ClientConfig.Physics.FULL ? turnSway : 0.0F;
    }

    @Override
    public float walkAmount() {
        return FemboyConfig.client().physics() == ClientConfig.Physics.OFF ? 0.0F : walkAmount;
    }

    /** Idle animation strength for the physics option: full 1, simple 0.5, off 0. */
    public static float idleScale() {
        return switch (FemboyConfig.client().physics()) {
            case FULL -> 1.0F;
            case SIMPLE -> 0.5F;
            case OFF -> 0.0F;
        };
    }

    @Override
    public float phase() {
        return phase;
    }

    private static boolean reactive() {
        return FemboyConfig.client().reactiveCosmetics();
    }

    @Override
    public float earAlert() {
        return reactive() ? alert : 0.0F;
    }

    @Override
    public float earYaw() {
        return reactive() ? earYaw : 0.0F;
    }

    @Override
    public float earFlatten() {
        return reactive() ? flatten : 0.0F;
    }

    @Override
    public float droop() {
        return reactive() ? droop : 0.0F;
    }

    @Override
    public float tailWag() {
        return reactive() ? wag : 0.0F;
    }

    @Override
    public float tailPuff() {
        return reactive() ? puff : 0.0F;
    }

    @Override
    public float jitter() {
        return reactive() ? jitter : 0.0F;
    }
}
