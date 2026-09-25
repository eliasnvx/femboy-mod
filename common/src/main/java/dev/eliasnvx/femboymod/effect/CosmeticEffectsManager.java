package dev.eliasnvx.femboymod.effect;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.combat.DripCombat;
import dev.eliasnvx.femboymod.api.drip.DripLevel;
import dev.eliasnvx.femboymod.api.effect.ConfiguredEffect;
import dev.eliasnvx.femboymod.api.event.cosmetic.DripLevelChangedEvent;
import dev.eliasnvx.femboymod.api.event.cosmetic.SetBonusEvent;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.drip.WornEvaluator.Evaluation;
import dev.eliasnvx.femboymod.drip.WornEvaluator.PlannedEffect;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side runtime of cosmetic effects: applies what {@link WornEvaluator} plans, gates it by
 * conditions every tick, and undoes it when it no longer applies. Also posts drip/set events.
 */
public final class CosmeticEffectsManager {

    private static final class Running {
        final PlannedEffect planned;
        boolean active;

        Running(PlannedEffect planned) {
            this.planned = planned;
        }
    }

    private static final class PlayerState {
        Evaluation evaluation = Evaluation.EMPTY;
        final Map<Identifier, Running> running = new LinkedHashMap<>();
    }

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    private CosmeticEffectsManager() {
    }

    /** Called every server tick for every player. */
    public static void tick(ServerPlayer player) {
        PlayerState state = STATES.computeIfAbsent(player.getUUID(), id -> new PlayerState());
        Evaluation evaluation = WornEvaluator.evaluate(player);
        if (evaluation != state.evaluation) {
            apply(player, state, evaluation);
        }
        for (Running running : state.running.values()) {
            ConfiguredEffect configured = running.planned.configured();
            boolean shouldBeActive = configured.when().map(c -> c.test(player)).orElse(true);
            try {
                if (shouldBeActive && !running.active) {
                    configured.effect().onActivate(player, running.planned.source());
                    running.active = true;
                } else if (!shouldBeActive && running.active) {
                    configured.effect().onDeactivate(player, running.planned.source());
                    running.active = false;
                }
                if (running.active) {
                    configured.effect().tick(player, running.planned.source());
                }
            } catch (RuntimeException e) {
                FemboyMod.LOGGER.error("Cosmetic effect {} failed", running.planned.source().id(), e);
            }
        }
    }

    private static void apply(ServerPlayer player, PlayerState state, Evaluation next) {
        Evaluation previous = state.evaluation;
        Map<Identifier, PlannedEffect> wanted = new LinkedHashMap<>();
        for (PlannedEffect planned : next.effects()) {
            wanted.put(planned.source().id(), planned);
        }
        Iterator<Map.Entry<Identifier, Running>> it = state.running.entrySet().iterator();
        while (it.hasNext()) {
            Running running = it.next().getValue();
            PlannedEffect replacement = wanted.get(running.planned.source().id());
            if (running.planned.equals(replacement)) {
                wanted.remove(running.planned.source().id()); // unchanged, keep running
                continue;
            }
            deactivate(player, running);
            it.remove();
        }
        wanted.forEach((id, planned) -> state.running.put(id, new Running(planned)));
        state.evaluation = next;

        for (Identifier set : previous.activeSets()) {
            if (!next.activeSets().contains(set)) {
                FemboyMod.api().events().post(new SetBonusEvent.Deactivate(player, set));
            }
        }
        for (Identifier set : next.activeSets()) {
            if (!previous.activeSets().contains(set)) {
                FemboyMod.api().events().post(new SetBonusEvent.Activate(player, set));
            }
        }
        if (!next.drip().equals(previous.drip())) {
            FemboyMod.api().events().post(new DripLevelChangedEvent(player, previous.drip(), next.drip()));
        }
    }

    /** Logout or death: undo everything on this entity. */
    public static void stop(ServerPlayer player) {
        PlayerState state = STATES.remove(player.getUUID());
        if (state == null) {
            return;
        }
        state.running.values().forEach(running -> deactivate(player, running));
        for (Identifier set : state.evaluation.activeSets()) {
            FemboyMod.api().events().post(new SetBonusEvent.Deactivate(player, set));
        }
        if (!state.evaluation.drip().equals(DripLevel.NONE)) {
            FemboyMod.api().events().post(new DripLevelChangedEvent(player, state.evaluation.drip(), DripLevel.NONE));
        }
    }

    /** The respawned entity is fresh (no modifiers); forget state without touching it. */
    public static void forget(ServerPlayer player) {
        STATES.remove(player.getUUID());
        DripCombat.clear(player);
    }

    /** For tests: currently running (active or gated) effect source ids. */
    public static Set<Identifier> runningSources(ServerPlayer player) {
        PlayerState state = STATES.get(player.getUUID());
        return state == null ? Set.of() : Set.copyOf(state.running.keySet());
    }

    private static void deactivate(ServerPlayer player, Running running) {
        if (running.active) {
            try {
                running.planned.configured().effect().onDeactivate(player, running.planned.source());
            } catch (RuntimeException e) {
                FemboyMod.LOGGER.error("Cosmetic effect {} failed to deactivate", running.planned.source().id(), e);
            }
            running.active = false;
        }
    }
}
