package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.gametest.CosmeticGameTests;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

import java.util.List;

/**
 * Registers the shared {@link CosmeticGameTests} on NeoForge, only when GameTests are enabled.
 * Public because the game test registry invokes {@link #tests()} reflectively.
 */
public final class FemboyGameTestsNeoForge {

    /** Only for the Curios check; the shared tests bring their own ({@link CosmeticGameTests.Entry#maxTicks()}). */
    private static final int MAX_TICKS = CosmeticGameTests.Entry.DEFAULT_MAX_TICKS;
    private static final long SETUP_TICKS = 0L;
    /** 8x8x8 of air in {@code data/femboymod/structure/empty.nbt}, the same size as Fabric's empty template. */
    private static final String EMPTY_STRUCTURE = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "empty").toString();

    private FemboyGameTestsNeoForge() {
    }

    static void register(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(FemboyGameTestsNeoForge.class));
    }

    /** One test function per shared test, all in the {@code femboymod} batch. */
    @GameTestGenerator
    public static List<TestFunction> tests() {
        List<TestFunction> tests = new java.util.ArrayList<>(CosmeticGameTests.all().stream()
                .map(entry -> new TestFunction(FemboyMod.MOD_ID, FemboyMod.MOD_ID + "." + entry.name(), EMPTY_STRUCTURE,
                        entry.maxTicks(), SETUP_TICKS, true, entry.body()))
                .toList());
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            // -Pcompat_slots=true: only the Curios check; Curios can't sync to the shared tests' mock players
            tests.clear();
            tests.add(new TestFunction(FemboyMod.MOD_ID, FemboyMod.MOD_ID + ".curios_slots_count_as_worn", EMPTY_STRUCTURE,
                    MAX_TICKS, SETUP_TICKS, true, dev.eliasnvx.femboymod.compat.curios.CuriosGameTest::cosmeticsInCuriosSlotsCount));
        }
        return tests;
    }
}
