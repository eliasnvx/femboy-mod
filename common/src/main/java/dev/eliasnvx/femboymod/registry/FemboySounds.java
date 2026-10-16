package dev.eliasnvx.femboymod.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events (SPEC §5.7). sounds.json points them at vanilla sound files with our own pitch/volume,
 * so no audio files are shipped (nothing licensed, nothing to attribute).
 */
public final class FemboySounds {

    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> PLUSH_SQUEAK = event("plush.squeak");
    public static final RegistrySupplier<SoundEvent> CHOKER_JINGLE = event("choker.jingle");
    public static final RegistrySupplier<SoundEvent> SKIRT_RUSTLE = event("skirt.rustle");
    public static final RegistrySupplier<SoundEvent> CAN_OPEN = event("can.open");
    public static final RegistrySupplier<SoundEvent> CONFETTI_POP = event("confetti.pop");
    public static final RegistrySupplier<SoundEvent> NYA = event("nya");
    /** Footsteps of the shoes (cosmetic_stats step_sound effects). */
    public static final RegistrySupplier<SoundEvent> STEP_SNEAKERS = event("step.sneakers");
    public static final RegistrySupplier<SoundEvent> STEP_HIGH_TOPS = event("step.high_tops");
    public static final RegistrySupplier<SoundEvent> STEP_PLATFORM = event("step.platform");
    public static final RegistrySupplier<SoundEvent> STEP_SLIPPERS = event("step.slippers");
    /** A soft pat-pat and a little sparkle (synthesized, tools/sounds/make_sounds.py). */
    public static final RegistrySupplier<SoundEvent> HEADPAT = event("headpat");
    // Bug: own synthesized sounds (tools/sounds/make_sounds.py)
    public static final RegistrySupplier<SoundEvent> BUG_AMBIENT = event("bug.ambient");
    public static final RegistrySupplier<SoundEvent> BUG_HURT = event("bug.hurt");
    public static final RegistrySupplier<SoundEvent> BUG_DEATH = event("bug.death");
    public static final RegistrySupplier<SoundEvent> BUG_STEP = event("bug.step");
    public static final RegistrySupplier<SoundEvent> GOBLIN_GIGGLE = event("sock_goblin.giggle");
    public static final RegistrySupplier<SoundEvent> GOBLIN_HURT = event("sock_goblin.hurt");
    public static final RegistrySupplier<SoundEvent> GOBLIN_DEATH = event("sock_goblin.death");
    public static final RegistrySupplier<SoundEvent> GOBLIN_STEAL = event("sock_goblin.steal");
    public static final RegistrySupplier<SoundEvent> DUCK_QUACK = event("rubber_duck.quack");
    public static final RegistrySupplier<SoundEvent> DUCK_SQUEAK = event("rubber_duck.squeak");
    public static final RegistrySupplier<SoundEvent> DUCK_DEATH = event("rubber_duck.death");

    private FemboySounds() {
    }

    private static RegistrySupplier<SoundEvent> event(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, name);
        return REGISTER.register(id, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
