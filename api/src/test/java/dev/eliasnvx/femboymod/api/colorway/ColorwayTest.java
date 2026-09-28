package dev.eliasnvx.femboymod.api.colorway;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorwayTest {

    @org.junit.jupiter.api.BeforeAll
    static void bootstrapMinecraft() {
        // 1.20.1 registries refuse to create keys before Minecraft is bootstrapped
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void alphaIsDroppedSoNetworkDecodedColorsCompareEqual() {
        // ByteBufCodecs.RGB_COLOR decodes 0xFFB6D9 as 0xFFFFB6D9 (opaque alpha)
        assertEquals(Colorway.solid(0xFFB6D9), Colorway.solid(0xFFFFB6D9));
        assertEquals(new Colorway(1, Optional.empty(), Optional.of(0x123456)),
                new Colorway(0xFF000001, Optional.empty(), Optional.of(0xFF123456)));
    }

    @Test
    void solidColorwayHasOneStripe() {
        assertArrayEquals(new int[]{0xFFB6D9}, Colorway.solid(0xFFB6D9).resolveStripes());
    }
}
