package dev.eliasnvx.femboymod.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstantPrintTest {

    private static final float W = 100;
    private static final float H = 120;
    private static final float R = 10;

    @Test
    void roundedCornersAreCutOff() {
        // the very corner pixel is outside, the middle and the middle of each side are inside
        assertEquals(0, InstantPrint.coverage(InstantPrint.distance(0.5F, 0.5F, 0, 0, W, H, R)));
        assertEquals(1, InstantPrint.coverage(InstantPrint.distance(W / 2, H / 2, 0, 0, W, H, R)));
        assertEquals(1, InstantPrint.coverage(InstantPrint.distance(W / 2, 1.5F, 0, 0, W, H, R)));
        assertEquals(1, InstantPrint.coverage(InstantPrint.distance(1.5F, H / 2, 0, 0, W, H, R)));
    }

    @Test
    void edgesAreAntiAliased() {
        // a pixel centred exactly on the straight edge is half covered
        assertEquals(0.5F, InstantPrint.coverage(InstantPrint.distance(W / 2, 0, 0, 0, W, H, R)), 1e-5F);
        // on the arc too: the corner circle's centre is (R, R)
        float onArc = R - R / (float) Math.sqrt(2);
        assertEquals(0.5F, InstantPrint.coverage(InstantPrint.distance(onArc, onArc, 0, 0, W, H, R)), 1e-4F);
    }

    @Test
    void zeroRadiusIsAPlainRectangle() {
        assertEquals(1, InstantPrint.coverage(InstantPrint.distance(0.5F, 0.5F, 0, 0, W, H, 0)));
    }

    @Test
    void shadowFadesOut() {
        int color = 0xFF000000;
        int atEdge = InstantPrint.shadow(0, 10, color, 0.5F) >>> 24;
        int halfway = InstantPrint.shadow(5, 10, color, 0.5F) >>> 24;
        int beyond = InstantPrint.shadow(10, 10, color, 0.5F) >>> 24;
        assertEquals(128, atEdge);
        assertTrue(halfway > 0 && halfway < atEdge, "fades: " + halfway);
        assertEquals(0, beyond);
    }

    @Test
    void sourceOverBlending() {
        int paper = 0xFFFAF6EE;
        assertEquals(paper, InstantPrint.over(paper, 1, 0));
        assertEquals(paper, InstantPrint.over(paper, 1, 0x80000000));
        assertEquals(0x80000000, InstantPrint.over(paper, 0, 0x80000000));
        assertEquals(0, InstantPrint.over(paper, 0, 0));
        // half paper over nothing: same colour, half alpha (straight alpha keeps the colour)
        assertEquals(0x80FAF6EE, InstantPrint.over(paper, 0.5F, 0));
        // half paper over opaque black: half way between
        assertEquals(0xFF7D7B77, InstantPrint.over(paper, 0.5F, 0xFF000000));
    }

    @Test
    void mixIsLinear() {
        assertEquals(0xFF000000, InstantPrint.mix(0xFF000000, 0xFFFFFFFF, 0));
        assertEquals(0xFFFFFFFF, InstantPrint.mix(0xFF000000, 0xFFFFFFFF, 1));
        assertEquals(0xFF808080, InstantPrint.mix(0xFF000000, 0xFFFFFFFF, 0.5F));
    }
}
