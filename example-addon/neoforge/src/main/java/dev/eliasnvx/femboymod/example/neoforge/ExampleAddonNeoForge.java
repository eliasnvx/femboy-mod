package dev.eliasnvx.femboymod.example.neoforge;

import dev.eliasnvx.femboymod.example.ExampleAddon;
import dev.eliasnvx.femboymod.example.ExampleAddonGameTests;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

import java.util.List;

/**
 * NeoForge entry point of the example addon. The addon itself is discovered by femboymod through
 * {@code @RegisterFemboyAddon}; this class only exists to register the addon's GameTests.
 */
@Mod(ExampleAddon.MOD_ID)
public final class ExampleAddonNeoForge {

    private static final int MAX_TICKS = 100;
    private static final long SETUP_TICKS = 0L;
    private static final String EMPTY_STRUCTURE = ResourceLocation.fromNamespaceAndPath(ExampleAddon.MOD_ID, "empty").toString();

    public ExampleAddonNeoForge(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(ExampleAddonNeoForge.class));
    }

    /** One test function per shared example addon test (invoked reflectively by the game test registry). */
    @GameTestGenerator
    public static List<TestFunction> tests() {
        return ExampleAddonGameTests.ALL.stream()
                .map(entry -> new TestFunction(ExampleAddon.MOD_ID, ExampleAddon.MOD_ID + "." + entry.name(), EMPTY_STRUCTURE,
                        MAX_TICKS, SETUP_TICKS, true, entry.body()))
                .toList();
    }
}
