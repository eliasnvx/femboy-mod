package dev.eliasnvx.femboymod.client.render.entity;

import dev.eliasnvx.femboymod.entity.StrayCat;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.animal.feline.Cat;

/**
 * Stray Cat: the vanilla cat model and renderer (collar, lying, sitting) with the vanilla white coat referenced
 * (not shipped) and tinted into a pastel shade per cat.
 */
public final class StrayCatRenderer extends CatRenderer {

    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/entity/cat/cat_white.png");
    private static final ResourceLocation WHITE_BABY = ResourceLocation.withDefaultNamespace("textures/entity/cat/cat_white_baby.png");
    /** Pink, lavender, mint, peach, sky. */
    private static final int[] COATS = {0xFFD1E6, 0xE3D1FF, 0xCFF5E2, 0xFFE0C8, 0xD1ECFF};

    public StrayCatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Render state with the coat tint. */
    public static final class State extends CatRenderState {
        int tint = 0xFFFFFF;
    }

    @Override
    public CatRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Cat cat, CatRenderState state, float partialTick) {
        super.extractRenderState(cat, state, partialTick);
        state.texture = state.isBaby ? WHITE_BABY : WHITE;
        if (state instanceof State stray && cat instanceof StrayCat strayCat) {
            stray.tint = COATS[Math.floorMod(strayCat.coat(), COATS.length)];
        }
    }

    @Override
    protected int getModelTint(CatRenderState state) {
        return state instanceof State stray ? ARGB.opaque(stray.tint) : super.getModelTint(state);
    }
}
