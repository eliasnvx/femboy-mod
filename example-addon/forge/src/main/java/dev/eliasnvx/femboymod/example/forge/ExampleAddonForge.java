package dev.eliasnvx.femboymod.example.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.eliasnvx.femboymod.example.ExampleAddon;
import dev.eliasnvx.femboymod.example.ExampleAddonGameTests;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.gametest.ForgeGameTestHooks;

import java.util.List;

/**
 * Forge entry point of the example addon. The addon itself is discovered by femboymod through
 * {@code @RegisterFemboyAddon}; this class hands the mod bus to Architectury (the addon's DeferredRegister needs it
 * on Forge) and registers the addon's GameTests.
 */
@Mod(ExampleAddon.MOD_ID)
public final class ExampleAddonForge {

    private static final int MAX_TICKS = 100;
    private static final long SETUP_TICKS = 0L;
    private static final String EMPTY_STRUCTURE = new ResourceLocation(ExampleAddon.MOD_ID, "empty").toString();

    public ExampleAddonForge(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        // Architectury's DeferredRegister needs this bus. femboymod registers it itself when it runs the addon
        // (usually before this constructor), so only register it when that has not happened yet.
        if (EventBuses.getModEventBus(ExampleAddon.MOD_ID).isEmpty()) {
            try {
                EventBuses.registerModEventBus(ExampleAddon.MOD_ID, modBus);
            } catch (IllegalStateException alreadyRegistered) {
                // femboymod registered it concurrently (mods are constructed in parallel)
            }
        }
        if (!ForgeGameTestHooks.isGametestEnabled()) {
            return;
        }
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(ExampleAddonForge.class));
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
