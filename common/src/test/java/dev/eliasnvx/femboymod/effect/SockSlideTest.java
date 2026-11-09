package dev.eliasnvx.femboymod.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SockSlideTest {

    private static final float EPS = 1.0E-5F;
    private static final float PLANKS = 0.6F;
    private static final float ICE = 0.98F;
    /** Vanilla keeps friction * 0.91 of the speed per tick on the ground. */
    private static final double AIR_DRAG = 0.91;

    @Test
    void gripScalesTheFloorsFriction() {
        assertEquals(PLANKS, SockSlide.effectiveFriction(PLANKS, 1.0F), EPS);
        assertEquals(0.96F, SockSlide.effectiveFriction(PLANKS, 0.1F), EPS);
        assertEquals(1.0F, SockSlide.effectiveFriction(PLANKS, 0.0F), EPS);
        // already slippery floors stay slippery and never go past 1
        assertTrue(SockSlide.effectiveFriction(ICE, 0.1F) >= ICE);
        assertTrue(SockSlide.effectiveFriction(ICE, 0.1F) <= 1.0F);
    }

    @Test
    void socksOnPlanksSlideLikeAlmostIce() {
        float socks = SockSlide.effectiveFriction(PLANKS, 0.1F);
        assertTrue(socks > 0.9F && socks < ICE, "between slime and ice: " + socks);
    }

    /** Distance over one window when every tick keeps {@code keep} of the last one, starting at {@code speed}. */
    private static double window(double speed, double keep) {
        double sum = 0;
        for (int i = 0; i < SockSlide.WINDOW; i++) {
            speed *= keep;
            sum += speed;
        }
        return sum;
    }

    @Test
    void coastingIsTheSlowFadeOfASlide() {
        double socks = SockSlide.effectiveFriction(PLANKS, 0.1F) * AIR_DRAG;
        double walking = 0.2 * SockSlide.WINDOW;
        assertTrue(SockSlide.isCoasting(walking, window(0.2, socks)), "sliding on in socks");
        assertTrue(SockSlide.isCoasting(walking, window(0.2, ICE * AIR_DRAG)), "sliding on ice counts too");
        assertFalse(SockSlide.isCoasting(walking, walking), "walking keeps the speed");
        assertFalse(SockSlide.isCoasting(walking, window(0.2, PLANKS * AIR_DRAG)), "stopping on a normal floor is quick");
        assertFalse(SockSlide.isCoasting(0.02 * SockSlide.WINDOW, window(0.02, socks)), "too slow to count");
        assertFalse(SockSlide.isCoasting(walking, walking * 1.5), "speeding up is not coasting");
    }
}
