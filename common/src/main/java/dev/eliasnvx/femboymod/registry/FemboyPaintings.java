package dev.eliasnvx.femboymod.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.eliasnvx.femboymod.FemboyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.decoration.PaintingVariant;

import java.util.List;

/**
 * The mod's posters. 1.21.1 loads them from {@code data/femboymod/painting_variant/*.json}; 1.20.1 painting
 * variants are a built-in registry, so they are registered here under the same ids (the painting variant tags
 * reference them). Textures: {@code assets/femboymod/textures/painting/<name>.png}.
 */
public final class FemboyPaintings {

    public static final DeferredRegister<PaintingVariant> REGISTER = DeferredRegister.create(FemboyMod.MOD_ID, Registries.PAINTING_VARIANT);

    /** 1.20.1 painting sizes are in pixels, 16 per block. */
    private static final int PIXELS_PER_BLOCK = 16;

    /** A poster and its size in blocks, as in the 1.21.1 JSON. */
    private record Poster(String name, int width, int height) {
    }

    private static final List<Poster> POSTERS = List.of(
            new Poster("btw", 1, 1),
            new Poster("byte_break", 1, 2),
            new Poster("cat_exe", 2, 2),
            new Poster("cherry_blossom", 1, 2),
            new Poster("code_with_love", 2, 2),
            new Poster("cozy_blanket", 1, 2),
            new Poster("debug_duck", 1, 1),
            new Poster("hello_code", 2, 2),
            new Poster("mochi_friends", 1, 1),
            new Poster("night_coding", 2, 2),
            new Poster("pastel_gamepad", 2, 2),
            new Poster("pastel_horizon", 2, 2),
            new Poster("pastel_waves", 2, 2),
            new Poster("pink_creeper", 1, 1),
            new Poster("pixel_heart", 1, 1),
            new Poster("plush_buddy", 1, 1),
            new Poster("rainy_window", 2, 2),
            new Poster("snack_time", 1, 1),
            new Poster("stay_hydrated", 1, 2),
            new Poster("stay_warm", 1, 2));

    static {
        for (Poster poster : POSTERS) {
            REGISTER.register(poster.name(), () -> new PaintingVariant(
                    poster.width() * PIXELS_PER_BLOCK, poster.height() * PIXELS_PER_BLOCK));
        }
    }

    private FemboyPaintings() {
    }

    /** Called from FemboyMod.init. */
    public static void init() {
        REGISTER.register();
    }
}
