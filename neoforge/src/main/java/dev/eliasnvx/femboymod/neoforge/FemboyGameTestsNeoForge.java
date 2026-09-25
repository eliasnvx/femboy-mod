package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.gametest.CosmeticGameTests;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Registers the shared {@link CosmeticGameTests} on NeoForge, only when GameTests are enabled. */
final class FemboyGameTestsNeoForge {

    private static final int MAX_TICKS = 100;
    private static final Identifier EMPTY_STRUCTURE = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "empty");

    private FemboyGameTestsNeoForge() {
    }

    static void register(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, FemboyMod.MOD_ID);
        List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> holders = new ArrayList<>();
        for (CosmeticGameTests.Entry entry : CosmeticGameTests.ALL) {
            holders.add(functions.register(entry.name(), entry::body));
        }
        functions.register(modBus);

        modBus.addListener((RegisterGameTestsEvent event) -> {
            Holder<TestEnvironmentDefinition<?>> environment =
                    event.registerEnvironment(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "default"));
            for (var holder : holders) {
                event.registerTest(holder.getId(), new FunctionGameTestInstance(holder.getKey(),
                        new TestData<>(environment, EMPTY_STRUCTURE, MAX_TICKS, 0, true)));
            }
        });
    }
}
