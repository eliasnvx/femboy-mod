package dev.eliasnvx.femboymod.halloween;

import dev.eliasnvx.femboymod.config.CommonConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HalloweenSeasonTest {

    @Test
    void seasonIsOctober20ToNovember3() {
        assertFalse(HalloweenSeason.isHalloween(LocalDate.of(2026, 10, 19)));
        assertTrue(HalloweenSeason.isHalloween(LocalDate.of(2026, 10, 20)));
        assertTrue(HalloweenSeason.isHalloween(LocalDate.of(2026, 10, 31)));
        assertTrue(HalloweenSeason.isHalloween(LocalDate.of(2026, 11, 3)));
        assertFalse(HalloweenSeason.isHalloween(LocalDate.of(2026, 11, 4)));
        assertFalse(HalloweenSeason.isHalloween(LocalDate.of(2026, 3, 25)));
    }

    @Test
    void configOverridesTheDate() {
        LocalDate summer = LocalDate.of(2026, 7, 1);
        LocalDate halloween = LocalDate.of(2026, 10, 31);
        assertFalse(HalloweenSeason.active(CommonConfig.Season.AUTO, summer));
        assertTrue(HalloweenSeason.active(CommonConfig.Season.AUTO, halloween));
        assertTrue(HalloweenSeason.active(CommonConfig.Season.ALWAYS, summer));
        assertFalse(HalloweenSeason.active(CommonConfig.Season.NEVER, halloween));
    }
}
