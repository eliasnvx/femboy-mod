package dev.eliasnvx.femboymod.drip;

import dev.eliasnvx.femboymod.api.drip.DripLevel;
import dev.eliasnvx.femboymod.api.drip.DripRules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Pure Drip Level formula (SPEC §4.6). No Minecraft state, so it is unit tested directly. */
public final class DripCalculator {

    private DripCalculator() {
    }

    /**
     * @param drip     base drip of the worn item (from its cosmetic stats)
     * @param colorKey identity of its colorway for harmony (e.g. pattern id or base color); empty if undyed
     */
    public record Worn(int drip, Optional<String> colorKey) {
    }

    public static DripLevel compute(List<Worn> worn, int completedSets, DripRules rules) {
        int level = 0;
        Map<String, Integer> colorCounts = new HashMap<>();
        for (Worn item : worn) {
            level += item.drip();
            item.colorKey().ifPresent(key -> colorCounts.merge(key, 1, Integer::sum));
        }
        for (int count : colorCounts.values()) {
            if (count > 1) {
                level += count * rules.harmonyBonus();
            }
        }
        level += completedSets * rules.setBonus();
        level = Math.clamp(level, 0, rules.maxLevel());
        return new DripLevel(level, rules.tierOf(level));
    }
}
