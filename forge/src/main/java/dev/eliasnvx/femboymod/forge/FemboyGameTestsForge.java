package dev.eliasnvx.femboymod.forge;

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
public final class FemboyGameTestsForge {

    private static final int MAX_TICKS = 100;
    private static final long SETUP_TICKS = 0L;
    /** 8x8x8 of air in {@code data/femboymod/structure/empty.nbt}, the same size as Fabric's empty template. */
    private static final String EMPTY_STRUCTURE = new ResourceLocation(FemboyMod.MOD_ID, "empty").toString();

    private FemboyGameTestsForge() {
    }

    static void register(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(FemboyGameTestsForge.class));
    }

    /** One test function per shared test, all in the {@code femboymod} batch. */
    @GameTestGenerator
    public static List<TestFunction> tests() {
        return CosmeticGameTests.all().stream()
                .map(entry -> new TestFunction(FemboyMod.MOD_ID, FemboyMod.MOD_ID + "." + entry.name(), EMPTY_STRUCTURE,
                        MAX_TICKS, SETUP_TICKS, true, entry.body()))
                .toList();
    }
}
