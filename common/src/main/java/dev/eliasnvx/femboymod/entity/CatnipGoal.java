package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import java.util.EnumSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The two active parts of a catnip high ({@link Catnip}): zoomies (sprinting from spot to spot with a speed boost,
 * hopping now and then) and purring (lying down; vanilla cats purr while lying). Also notices catnip nearby and starts
 * the high. Mischief in between is {@link CatKnockGoal}'s, with livelier numbers. Not while ordered to sit.
 */
public class CatnipGoal extends Goal {

    public static final ResourceLocation ZOOMIES_SPEED = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "catnip_zoomies");
    /** How often an idle cat sniffs for catnip (ticks). */
    private static final int SNIFF_INTERVAL = 20;
    /** Zoomies dash this far per leg and pick a new spot at least this often. */
    private static final int DASH_RANGE = 8;
    private static final int DASH_HEIGHT = 3;
    private static final int MAX_LEG_TICKS = 30;
    /** One in this many ticks a zooming cat hops. */
    private static final int HOP_CHANCE = 25;

    private final Cat cat;
    private Catnip.Phase running = Catnip.Phase.NONE;
    private int sniffIn;
    private int legTicks;

    public CatnipGoal(Cat cat) {
        this.cat = cat;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        sniffIn = cat.getId() % SNIFF_INTERVAL; // spread the sniffing of many cats over ticks
    }

    @Override
    public boolean canUse() {
        if (cat.isOrderedToSit() || cat.isPassenger() || cat.level().isClientSide()) {
            return false;
        }
        Catnip.Phase phase = Catnip.phase(cat);
        if (phase == Catnip.Phase.NONE && --sniffIn <= 0) {
            sniffIn = SNIFF_INTERVAL;
            if (Catnip.smells(cat.level(), cat.blockPosition(), FemboyConfig.common().plants().catnipSniffRadius()) && Catnip.start(cat)) {
                phase = Catnip.phase(cat);
            }
        }
        return phase == Catnip.Phase.ZOOMIES || phase == Catnip.Phase.PURR;
    }

    @Override
    public boolean canContinueToUse() {
        return !cat.isOrderedToSit() && Catnip.phase(cat) == running;
    }

    @Override
    public void start() {
        running = Catnip.phase(cat);
        cat.setInSittingPose(false);
        if (running == Catnip.Phase.ZOOMIES) {
            cat.setLying(false);
            CommonConfig.Plants config = FemboyConfig.common().plants();
            AttributeInstance speed = cat.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(ZOOMIES_SPEED, config.catnipZoomiesSpeed(),
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            dash();
        } else {
            cat.getNavigation().stop();
            cat.setLying(true);
        }
    }

    @Override
    public void stop() {
        AttributeInstance speed = cat.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ZOOMIES_SPEED);
        }
        if (running == Catnip.Phase.PURR) {
            cat.setLying(false);
        }
        cat.getNavigation().stop();
        running = Catnip.Phase.NONE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (running != Catnip.Phase.ZOOMIES) {
            return;
        }
        if (cat.getNavigation().isDone() || ++legTicks >= MAX_LEG_TICKS) {
            dash();
        }
        if (cat.onGround() && cat.getRandom().nextInt(HOP_CHANCE) == 0) {
            cat.getJumpControl().jump();
        }
    }

    /** Off to a random spot nearby, at a sprint (vanilla gives cats the running pose at exactly this speed). */
    private void dash() {
        legTicks = 0;
        @Nullable Vec3 spot = LandRandomPos.getPos(cat, DASH_RANGE, DASH_HEIGHT);
        if (spot != null) {
            cat.getNavigation().moveTo(spot.x, spot.y, spot.z, Cat.SPRINT_SPEED_MOD);
        }
    }
}
