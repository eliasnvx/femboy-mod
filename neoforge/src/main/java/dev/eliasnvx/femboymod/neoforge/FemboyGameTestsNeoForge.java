package dev.eliasnvx.femboymod.neoforge;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.compat.curios.CuriosGameTest;
import dev.eliasnvx.femboymod.gametest.CosmeticGameTests;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Registers the shared {@link CosmeticGameTests} on NeoForge, only when GameTests are enabled. */
final class FemboyGameTestsNeoForge {

    private static final Identifier EMPTY_STRUCTURE = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "empty");

    private FemboyGameTestsNeoForge() {
    }

    static void register(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, FemboyMod.MOD_ID);
        List<DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> holders = new ArrayList<>();
        List<Integer> maxTicks = new ArrayList<>();
        List<CosmeticGameTests.Entry> entries = ModList.get().isLoaded("curios")
                // -Pcompat_slots=true: only the Curios check; Curios can't sync to the shared tests' mock players
                ? List.of(new CosmeticGameTests.Entry("curios_slots_count_as_worn", CuriosGameTest::cosmeticsInCuriosSlotsCount))
                : CosmeticGameTests.all();
        for (CosmeticGameTests.Entry entry : entries) {
            holders.add(functions.register(entry.name(), entry::body));
            maxTicks.add(entry.maxTicks());
        }
        functions.register(modBus);

        modBus.addListener((RegisterGameTestsEvent event) -> {
            Holder<TestEnvironmentDefinition<?>> environment =
                    event.registerEnvironment(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "default"));
            for (int i = 0; i < holders.size(); i++) {
                var holder = holders.get(i);
                event.registerTest(holder.getId(), new FunctionGameTestInstance(holder.key(),
                        new TestData<>(environment, EMPTY_STRUCTURE, maxTicks.get(i), 0, true)));
            }
        });
    }
}
