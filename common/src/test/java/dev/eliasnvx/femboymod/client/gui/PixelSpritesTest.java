package dev.eliasnvx.femboymod.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PixelSpritesTest {

    private static final int BOX = 16;

    @Test
    void sixteenPixelSpritesAreDrawnAtTheGuiScale() {
        for (int scale = 1; scale <= 10; scale++) {
            assertEquals(scale, PixelSprites.multiple(16, BOX, scale), "scale " + scale);
        }
    }

    @Test
    void thirtyTwoPixelSpritesGetAWholeMultipleThatFitsTheSlot() {
        int[] expected = {0, 1, 2, 2, 3, 3, 4, 4, 5, 5};
        for (int scale = 1; scale <= expected.length; scale++) {
            int multiple = PixelSprites.multiple(32, BOX, scale);
            assertEquals(expected[scale - 1], multiple, "scale " + scale);
            // 24 pixels of artwork never leave the slot (the 16-unit box plus its 1-unit frame on each side)
            assertTrue(multiple * 24 <= (BOX + 2) * scale, "fits at scale " + scale);
        }
    }

    @Test
    void multiplesNeverShrinkAsTheGuiScaleGrows() {
        int previous = 0;
        for (int scale = 1; scale <= 16; scale++) {
            int multiple = PixelSprites.multiple(32, BOX, scale);
            assertTrue(multiple >= previous, "scale " + scale);
            previous = Math.max(previous, multiple);
        }
    }
}
