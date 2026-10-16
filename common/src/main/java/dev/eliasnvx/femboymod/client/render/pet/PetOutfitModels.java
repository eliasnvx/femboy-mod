package dev.eliasnvx.femboymod.client.render.pet;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.client.render.model.Groups;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

import static dev.eliasnvx.femboymod.client.render.model.Groups.ACCENT;
import static dev.eliasnvx.femboymod.client.render.model.Groups.DARK;
import static dev.eliasnvx.femboymod.client.render.model.Groups.MAIN;
import static dev.eliasnvx.femboymod.client.render.model.Groups.METAL;

/**
 * Code models of the pet outfit pieces, in color groups like the cosmetics ({@link Groups}). Bow and hat are made for
 * an 8 px head and scaled per pet; collar and bandana rings are sized per pet ({@link PetSpecies}). The origin is the
 * anchor point: the bow's knot, the hat's brim center, the ring's center.
 */
public final class PetOutfitModels {

    private static final int TEXTURE_SIZE = 64;
    /** How far the strap goes up the cheeks (cat, wolf). */
    private static final float STRAP_SIDE = 1.6F;
    public static final ModelLayerLocation BOW = layer("pet_bow");
    public static final ModelLayerLocation WITCH_HAT = layer("pet_witch_hat");

    private PetOutfitModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(FemboyMod.MOD_ID, name), "main");
    }

    public static ModelLayerLocation collar(PetSpecies species) {
        return layer("pet_collar_" + species.name());
    }

    public static ModelLayerLocation bandana(PetSpecies species) {
        return layer("pet_bandana_" + species.name());
    }

    public static void registerLayers() {
        EntityModelLayerRegistry.register(BOW, PetOutfitModels::bow);
        EntityModelLayerRegistry.register(WITCH_HAT, PetOutfitModels::witchHat);
        for (PetSpecies species : PetSpecies.all()) {
            EntityModelLayerRegistry.register(collar(species), () -> ring(species, false));
            EntityModelLayerRegistry.register(bandana(species), () -> ring(species, true));
        }
    }

    private static PartDefinition piece(MeshDefinition mesh) {
        return mesh.getRoot().addOrReplaceChild("piece", net.minecraft.client.model.geom.builders.CubeListBuilder.create(), PartPose.ZERO);
    }

    /** Two loops and two short tails around a knot, facing forward. */
    private static LayerDefinition bow() {
        MeshDefinition mesh = new MeshDefinition();
        Groups.Builder b = new Groups.Builder(piece(mesh));
        b.box(MAIN, -3.2F, -1.4F, -0.4F, 2.6F, 2.8F, 0.8F);
        b.box(MAIN, 0.6F, -1.4F, -0.4F, 2.6F, 2.8F, 0.8F);
        b.box(MAIN, -1.4F, 0.6F, -0.3F, 0.9F, 1.8F, 0.6F);
        b.box(MAIN, 0.5F, 0.6F, -0.3F, 0.9F, 1.8F, 0.6F);
        b.box(ACCENT, -0.7F, -0.7F, -0.55F, 1.4F, 1.4F, 1.1F);
        b.build();
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /** A small witch hat: brim, crown with band and buckle, the tip bending back. */
    private static LayerDefinition witchHat() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition pivot = piece(mesh);
        Groups.Builder b = new Groups.Builder(pivot);
        b.box(MAIN, -4.0F, -0.5F, -4.0F, 8.0F, 0.5F, 8.0F);
        b.box(MAIN, -2.6F, -2.6F, -2.6F, 5.2F, 2.1F, 5.2F);
        b.box(ACCENT, -2.7F, -1.3F, -2.7F, 5.4F, 0.8F, 5.4F);
        b.box(METAL, -0.6F, -1.4F, -2.95F, 1.2F, 1.0F, 0.3F);
        b.build();
        PartDefinition tip = pivot.addOrReplaceChild("tip", net.minecraft.client.model.geom.builders.CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -2.6F, 0.2F, -0.45F, 0.0F, 0.0F));
        Groups.Builder t = new Groups.Builder(tip);
        t.box(MAIN, -1.7F, -2.0F, -1.7F, 3.4F, 2.0F, 3.4F);
        t.box(MAIN, -0.9F, -3.6F, -0.9F, 1.8F, 1.6F, 1.8F);
        t.box(MAIN, -0.4F, -4.6F, -0.2F, 0.8F, 1.0F, 0.8F);
        t.build();
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /**
     * The neck piece. Cats and wolves already wear a vanilla collar around the neck once tamed, so on them this is a
     * strap under the chin and up the cheeks with the bell or the bandana hanging from it; the parrot gets a whole ring lying flat under
     * its head. The anchor is the middle of the head: the strap sits {@code ringDepth / 2} below it, at the chin.
     */
    private static LayerDefinition ring(PetSpecies species, boolean bandana) {
        MeshDefinition mesh = new MeshDefinition();
        Groups.Builder b = new Groups.Builder(piece(mesh));
        float w = species.ringWidth();
        float h = species.ringDepth();
        float scale = species.flat() ? 0.5F : 1.0F;
        float t = PetSpecies.BAND * scale;
        float d = PetSpecies.THICKNESS * scale;
        if (!species.flat()) {
            // a U-shaped strap: under the chin and a little way up both cheeks, so it shows from the side too
            b.box(MAIN, -w / 2 - t, h / 2, -d / 2, w + 2 * t, t, d);
            b.box(MAIN, -w / 2 - t, h / 2 - STRAP_SIDE, -d / 2, t, STRAP_SIDE, d);
            b.box(MAIN, w / 2, h / 2 - STRAP_SIDE, -d / 2, t, STRAP_SIDE, d);
            float y = h / 2 + t;
            if (bandana) {
                float[] widths = {w * 0.8F, w * 0.55F, w * 0.3F};
                for (int row = 0; row < widths.length; row++) {
                    b.box(MAIN, -widths[row] / 2, y + row * 0.9F, -d / 2 - 0.1F, widths[row], 0.9F, d + 0.2F);
                }
            } else {
                b.box(METAL, -0.6F, y - 0.1F, -0.6F, 1.2F, 1.2F, 1.2F);
                b.box(DARK, -0.4F, y + 0.75F, -0.65F, 0.8F, 0.2F, 0.1F);
            }
        } else {
            b.box(MAIN, -w / 2 - t, -d / 2, -h / 2 - t, w + 2 * t, d, t);
            b.box(MAIN, -w / 2 - t, -d / 2, h / 2, w + 2 * t, d, t);
            b.box(MAIN, -w / 2 - t, -d / 2, -h / 2, t, d, h);
            b.box(MAIN, w / 2, -d / 2, -h / 2, t, d, h);
            float z = -h / 2 - t;
            if (bandana) {
                float[] widths = {w * 0.9F, w * 0.6F, w * 0.3F};
                for (int row = 0; row < widths.length; row++) {
                    b.box(MAIN, -widths[row] / 2, d / 2 + row * 0.5F, z - 0.15F, widths[row], 0.5F, 0.3F);
                }
            } else {
                b.box(METAL, -0.4F, -0.2F, z - 0.8F, 0.8F, 0.8F, 0.8F);
            }
        }
        b.build();
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
