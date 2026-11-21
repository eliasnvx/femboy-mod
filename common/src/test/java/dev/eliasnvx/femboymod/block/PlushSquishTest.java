package dev.eliasnvx.femboymod.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlushSquishTest {

    @Test
    void idleReturnsUnitScale() {
        assertEquals(1.0F, PlushSquish.scaleY(null, 2.0F));
        assertEquals(1.0F, PlushSquish.scaleY(PlushSquish.Kind.CLICK, -1.0F));
        assertEquals(1.0F, PlushSquish.scaleY(PlushSquish.Kind.CLICK, 0.0F));
        assertEquals(1.0F, PlushSquish.scaleY(PlushSquish.Kind.CLICK, PlushSquish.Kind.CLICK.durationTicks()));
        assertEquals(1.0F, PlushSquish.scaleY(PlushSquish.Kind.PLACE, PlushSquish.Kind.PLACE.durationTicks() + 5.0F));
        assertEquals(1.0F, PlushSquish.scaleXZ(1.0F));
    }

    @Test
    void clickSquishesThenBouncesBack() {
        float minY = 1.0F;
        float maxY = 1.0F;
        for (float t = 0.25F; t < PlushSquish.Kind.CLICK.durationTicks(); t += 0.25F) {
            float y = PlushSquish.scaleY(PlushSquish.Kind.CLICK, t);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        assertTrue(minY < 0.70F, "click should noticeably compress Y, got " + minY);
        assertTrue(maxY > 1.05F, "click should bounce above 1.0, got " + maxY);
    }

    @Test
    void placeIsSofterThanClick() {
        float minClick = 1.0F;
        float minPlace = 1.0F;
        for (float t = 0.25F; t < PlushSquish.Kind.CLICK.durationTicks(); t += 0.25F) {
            minClick = Math.min(minClick, PlushSquish.scaleY(PlushSquish.Kind.CLICK, t));
            minPlace = Math.min(minPlace, PlushSquish.scaleY(PlushSquish.Kind.PLACE, t));
        }
        assertTrue(minPlace < 0.85F, "place should visibly compress Y, got " + minPlace);
        assertTrue(minClick < minPlace, "click (" + minClick + ") should compress more than place (" + minPlace + ")");
    }

    @Test
    void horizontalScalePreservesVolume() {
        float y = PlushSquish.scaleY(PlushSquish.Kind.CLICK, 1.5F);
        float xz = PlushSquish.scaleXZ(y);
        assertTrue(xz > 1.0F, "compressing vertically should bulge horizontally");
        assertEquals(1.0F, xz * xz * y, 0.01F);
    }
}
