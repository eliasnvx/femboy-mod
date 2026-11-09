package dev.eliasnvx.femboymod.entity;

import dev.eliasnvx.femboymod.block.DebugFlytrapBlock;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.social.TouchGrass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the 0.3.0 plants: catnip phases, flytrap reach, the touch-grass timers. */
class PlantsLogicTest {

    private static final CommonConfig.Plants CONFIG = CommonConfig.Plants.DEFAULTS;

    @Test
    void catnipPhasesRunInOrder() {
        int total = Catnip.duration(CONFIG);
        int tolerant = CONFIG.catnipToleranceTicks();
        int purr = tolerant + CONFIG.catnipPurrTicks();
        int mischief = purr + CONFIG.catnipMischiefTicks();
        assertEquals(mischief + CONFIG.catnipZoomiesTicks(), total);
        assertEquals(Catnip.Phase.ZOOMIES, Catnip.phase(total, CONFIG));
        assertEquals(Catnip.Phase.ZOOMIES, Catnip.phase(mischief + 1, CONFIG));
        assertEquals(Catnip.Phase.MISCHIEF, Catnip.phase(mischief, CONFIG));
        assertEquals(Catnip.Phase.MISCHIEF, Catnip.phase(purr + 1, CONFIG));
        assertEquals(Catnip.Phase.PURR, Catnip.phase(purr, CONFIG));
        assertEquals(Catnip.Phase.TOLERANT, Catnip.phase(tolerant, CONFIG));
        assertEquals(Catnip.Phase.TOLERANT, Catnip.phase(1, CONFIG));
        assertEquals(Catnip.Phase.NONE, Catnip.phase(0, CONFIG));
    }

    @Test
    void catnipPhasesCanBeSwitchedOff() {
        // no mischief and no tolerance: zoomies straight into purring, then over
        CommonConfig.Plants calm = new CommonConfig.Plants(10, 1.5F, 1.0F, 600, 3, 100, 0.6F, 0, 40, 200, 50, 0, 0.5F, 120, 200, 6000);
        assertEquals(Catnip.Phase.ZOOMIES, Catnip.phase(51, calm));
        assertEquals(Catnip.Phase.PURR, Catnip.phase(50, calm));
        assertEquals(Catnip.Phase.PURR, Catnip.phase(1, calm));
    }

    @Test
    void flytrapReachGrowsPerStage() {
        assertEquals(CONFIG.flytrapReach(), DebugFlytrapBlock.reach(0, CONFIG), 1e-6);
        assertTrue(DebugFlytrapBlock.reach(1, CONFIG) > DebugFlytrapBlock.reach(0, CONFIG));
        assertEquals(CONFIG.flytrapReach() + DebugFlytrapBlock.MAX_STAGE * CONFIG.flytrapReachPerStage(),
                DebugFlytrapBlock.reach(DebugFlytrapBlock.MAX_STAGE, CONFIG), 1e-6);
    }

    @Test
    void touchGrassTimers() {
        assertEquals(CONFIG.touchGrassReminderMinutes() * 1200L, TouchGrass.reminderTicks(CONFIG));
        assertTrue(TouchGrass.buffReady(null, 0, CONFIG), "never had it");
        assertFalse(TouchGrass.buffReady(1000L, 1000 + CONFIG.touchGrassBuffCooldownTicks() - 1, CONFIG));
        assertTrue(TouchGrass.buffReady(1000L, 1000 + CONFIG.touchGrassBuffCooldownTicks(), CONFIG));
        CommonConfig.Plants off = new CommonConfig.Plants(10, 1.5F, 1.0F, 600, 3, 200, 0.6F, 1200, 40, 200, 300, 6000, 0.5F, 0, 200, 6000);
        assertEquals(0, TouchGrass.reminderTicks(off), "0 minutes turns reminders off");
    }
}
