package dev.eliasnvx.femboymod.example.neoforge;

import dev.eliasnvx.femboymod.example.ExampleAddon;
import dev.eliasnvx.femboymod.example.ExampleAddonGameTests;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * NeoForge entry point of the example addon. The addon itself is discovered by femboymod through
 * {@code @RegisterFemboyAddon}; this class only exists to register the addon's GameTests.
 */
@Mod(ExampleAddon.MOD_ID)
public final class ExampleAddonNeoForge {

    private static final int MAX_TICKS = 100;

    public ExampleAddonNeoForge(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, ExampleAddon.MOD_ID);
        List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> holders = new ArrayList<>();
        for (ExampleAddonGameTests.Entry entry : ExampleAddonGameTests.ALL) {
            holders.add(functions.register(entry.name(), entry::body));
        }
        functions.register(modBus);

        modBus.addListener((RegisterGameTestsEvent event) -> {
            Holder<TestEnvironmentDefinition<?>> environment =
                    event.registerEnvironment(ResourceLocation.fromNamespaceAndPath(ExampleAddon.MOD_ID, "default"));
            ResourceLocation structure = ResourceLocation.fromNamespaceAndPath(ExampleAddon.MOD_ID, "empty");
            for (var holder : holders) {
                event.registerTest(holder.getId(), new FunctionGameTestInstance(holder.getKey(),
                        new TestData<>(environment, structure, MAX_TICKS, 0, true)));
            }
        });
    }
}
