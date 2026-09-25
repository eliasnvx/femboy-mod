package dev.eliasnvx.femboymod.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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

    private FemboySounds() {
    }

    private static RegistrySupplier<SoundEvent> event(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, name);
        return REGISTER.register(id, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
