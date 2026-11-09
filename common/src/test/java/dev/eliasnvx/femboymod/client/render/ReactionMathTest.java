package dev.eliasnvx.femboymod.client.render;

import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactionMathTest {

    private static final float EPS = 1.0E-4F;
    private static final float MAX_TURN = 1.3F;

    @Test
    void worldYawFollowsMinecraftAngles() {
        assertEquals(0.0F, ReactionMath.worldYaw(0, 1), EPS);    // south
        assertEquals(90.0F, Mth.wrapDegrees(ReactionMath.worldYaw(-1, 0)), EPS);  // west
        assertEquals(180.0F, Math.abs(Mth.wrapDegrees(ReactionMath.worldYaw(0, -1))), EPS); // north
        assertEquals(-90.0F, Mth.wrapDegrees(ReactionMath.worldYaw(1, 0)), EPS); // east
    }

    @Test
    void earsTurnRightForThingsOnTheRight() {
        // facing south (yaw 0), the right hand side is west (+90): positive = right, like turnSway's sign
        float right = ReactionMath.earYaw(ReactionMath.worldYaw(-1, 0), 0.0F, MAX_TURN);
        float left = ReactionMath.earYaw(ReactionMath.worldYaw(1, 0), 0.0F, MAX_TURN);
        assertTrue(right > 0, "west is on the right when facing south");
        assertTrue(left < 0, "east is on the left when facing south");
        assertEquals(0.0F, ReactionMath.earYaw(ReactionMath.worldYaw(0, 1), 0.0F, MAX_TURN), EPS);
    }

    @Test
    void earTurnIsLimitedAndWraps() {
        assertEquals(MAX_TURN, ReactionMath.earYaw(170.0F, 0.0F, MAX_TURN), EPS);
        assertEquals(-MAX_TURN, ReactionMath.earYaw(-170.0F, 0.0F, MAX_TURN), EPS);
        // 350 and -10 are the same direction
        assertEquals(ReactionMath.earYaw(-10.0F, 0.0F, MAX_TURN), ReactionMath.earYaw(350.0F, 0.0F, MAX_TURN), EPS);
        // the head turned with the target: nothing to turn
        assertEquals(0.0F, ReactionMath.earYaw(720.0F + 30.0F, 30.0F, MAX_TURN), EPS);
    }

    @Test
    void approachUsesRiseAndFallRates() {
        float rise = ReactionMath.approach(0.0F, 1.0F, 0.6F, 0.05F, 1.0F);
        float fall = ReactionMath.approach(1.0F, 0.0F, 0.6F, 0.05F, 1.0F);
        assertTrue(rise > 0.4F, "flinches are quick");
        assertTrue(fall > 0.9F, "recovery is slow");
        assertEquals(0.5F, ReactionMath.approach(0.5F, 1.0F, 0.6F, 0.05F, 0.0F), EPS);
    }

    @Test
    void approachIsFrameRateIndependent() {
        float oneStep = ReactionMath.approach(0.0F, 1.0F, 0.3F, 0.3F, 2.0F);
        float twoSteps = ReactionMath.approach(ReactionMath.approach(0.0F, 1.0F, 0.3F, 0.3F, 1.0F), 1.0F, 0.3F, 0.3F, 1.0F);
        assertEquals(oneStep, twoSteps, EPS);
    }

    @Test
    void noticeStrengthFallsOffWithDistance() {
        assertEquals(1.0F, ReactionMath.noticeStrength(0, 16, 0.55F), EPS);
        assertEquals(0.55F, ReactionMath.noticeStrength(16, 16, 0.55F), EPS);
        assertEquals(0.55F, ReactionMath.noticeStrength(40, 16, 0.55F), EPS);
        assertEquals(0.0F, ReactionMath.noticeStrength(1, 0, 0.55F), EPS);
    }

    @Test
    void droopStartsAtLowHealth() {
        assertEquals(0.0F, ReactionMath.lowHealthDroop(20, 20, 0.4F, 0.15F), EPS);
        assertEquals(0.0F, ReactionMath.lowHealthDroop(8, 20, 0.4F, 0.15F), EPS);
        assertEquals(1.0F, ReactionMath.lowHealthDroop(3, 20, 0.4F, 0.15F), EPS);
        assertEquals(0.5F, ReactionMath.lowHealthDroop(5.5F, 20, 0.4F, 0.15F), EPS);
        assertEquals(0.0F, ReactionMath.lowHealthDroop(5, 0, 0.4F, 0.15F), EPS);
    }
}
