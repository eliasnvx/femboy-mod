package dev.eliasnvx.femboymod.drip;

import dev.eliasnvx.femboymod.api.drip.DripLevel;
import dev.eliasnvx.femboymod.api.drip.DripRules;
import dev.eliasnvx.femboymod.drip.DripCalculator.Worn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DripCalculatorTest {

    private static final DripRules RULES = new DripRules(5, 10, 100, List.of(20, 40, 60, 80, 95));

    private static Worn plain(int drip) {
        return new Worn(drip, Optional.empty());
    }

    private static Worn colored(int drip, String key) {
        return new Worn(drip, Optional.of(key));
    }

    @Test
    void nothingWornIsZero() {
        assertEquals(DripLevel.NONE, DripCalculator.compute(List.of(), 0, RULES));
    }

    @Test
    void sumsItemDrip() {
        assertEquals(new DripLevel(25, 1), DripCalculator.compute(List.of(plain(10), plain(15)), 0, RULES));
    }

    @Test
    void harmonyCountsEveryMatchingItemButNotSingles() {
        List<Worn> worn = List.of(colored(10, "trans"), colored(10, "trans"), colored(10, "pink"));
        // 30 base + 2 matching * 5
        assertEquals(40, DripCalculator.compute(worn, 0, RULES).level());
    }

    @Test
    void setsAddBonusAndLevelIsClamped() {
        assertEquals(new DripLevel(30, 1), DripCalculator.compute(List.of(plain(20)), 1, RULES));
        assertEquals(new DripLevel(100, 5), DripCalculator.compute(List.of(plain(90), plain(90)), 2, RULES));
    }

    @Test
    void tierBoundariesAreInclusive() {
        assertEquals(0, RULES.tierOf(19));
        assertEquals(1, RULES.tierOf(20));
        assertEquals(5, RULES.tierOf(95));
    }

    @Test
    void thresholdsMustAscend() {
        assertThrows(IllegalArgumentException.class, () -> new DripRules(0, 0, 100, List.of(40, 20)));
    }
}
