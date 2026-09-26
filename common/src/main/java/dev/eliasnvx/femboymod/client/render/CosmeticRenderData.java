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
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-player render data, attached to the player's render state during extraction. One instance
 * per entity is reused across frames: the worn list is rebuilt only when the (immutable) cosmetic
 * inventory snapshot changes, and motion values are updated in place, so extraction does not allocate.
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

    public record Worn(Identifier slot, ItemStack stack, Identifier itemId, Identifier renderer, @Nullable Colorway colorway) {
    }

    private static final Map<Entity, CosmeticRenderData> CACHE = new WeakHashMap<>();

    private CosmeticInventory source;
    private List<Worn> worn = List.of();
    private final float phase;
    private float turnSway;
    private float walkAmount;
    private float lastAge = Float.NaN;
    private float lastBodyRot;

    private CosmeticRenderData(int entityId) {
        this.phase = (entityId * GOLDEN_ANGLE) % Mth.TWO_PI;
    }

    /** Called from the render state extraction mixin. Returns null if nothing is worn. */
    public static @Nullable CosmeticRenderData capture(Entity entity, AvatarRenderState state) {
        if (!(entity instanceof Player player)) {
            return null;
        }
        CosmeticInventory inventory = CosmeticsManager.get(player);
        if (inventory.isEmpty()) {
            return null;
        }
        CosmeticRenderData data = CACHE.computeIfAbsent(entity, e -> new CosmeticRenderData(e.getId()));
        if (data.source != inventory) {
            data.rebuild(inventory);
        }
        data.updateMotion(state);
        return data;
    }

    private void rebuild(CosmeticInventory inventory) {
        List<Worn> list = new ArrayList<>(inventory.all().size());
        inventory.all().forEach((slot, stack) -> {
            if (inventory.isHidden(slot)) {
                return; // worn but hidden by the wearer
            }
            Cosmetic cosmetic = stack.get(FemboyComponents.COSMETIC.get());
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            Identifier renderer = cosmetic != null && cosmetic.renderer().isPresent() ? cosmetic.renderer().get() : itemId;
            list.add(new Worn(slot, stack, itemId, renderer, Colorways.effective(stack).orElse(null)));
        });
        this.worn = List.copyOf(list);
        this.source = inventory;
    }

    private void updateMotion(AvatarRenderState state) {
        if (Float.isNaN(lastAge)) {
            lastAge = state.ageInTicks;
            lastBodyRot = state.bodyRot;
            return;
        }
        float dt = state.ageInTicks - lastAge;
        if (dt <= 0) {
            return; // same frame (e.g. inventory preview) or paused
        }
        float turnRate = Mth.wrapDegrees(state.bodyRot - lastBodyRot) / dt;
        float targetSway = Mth.clamp(-turnRate * TURN_GAIN, -MAX_TURN_SWAY, MAX_TURN_SWAY);
        float alpha = 1.0F - (float) Math.exp(-dt * SMOOTHING);
        turnSway += (targetSway - turnSway) * alpha;
        walkAmount += (Math.min(state.walkAnimationSpeed, 1.0F) - walkAmount) * alpha;
        lastAge = state.ageInTicks;
        lastBodyRot = state.bodyRot;
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
}
