package dev.eliasnvx.femboymod.client.render.model;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.FemboyClientApi;
import dev.eliasnvx.femboymod.api.client.CosmeticMotion;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.Map;

import static dev.eliasnvx.femboymod.client.render.model.Groups.ACCENT;
import static dev.eliasnvx.femboymod.client.render.model.Groups.BANDED;
import static dev.eliasnvx.femboymod.client.render.model.Groups.DARK;
import static dev.eliasnvx.femboymod.client.render.model.Groups.DETAIL;
import static dev.eliasnvx.femboymod.client.render.model.Groups.MAIN;
import static dev.eliasnvx.femboymod.client.render.model.Groups.METAL;

/**
 * Detailed placeholder cosmetic geometry, built in code (voxel-extruded pixel masks + color groups).
 * Each model is a {@link PlayerModel} with a cleared mesh plus parts under head/body/legs/arms, so vanilla
 * posing moves them; procedural animation happens in {@code setupAnim} (runs at flush time with the right
 * player's state). Units are model pixels. Blockbench/GeckoLib models ({@code femboymod:geo}) can replace
 * any of these per item later.
 */
public final class CosmeticModels {

    private static final int TEXTURE_SIZE = 64;
    /** Overlay inflation above the skin's own second layer (0.25) to avoid z-fighting (SPEC §4.5). */
    private static final float SOCK_INFLATE = 0.32F;
    private static final float FISHNET_INFLATE = 0.29F;
    private static final CubeDeformation SOCK_CUFF_INFLATE = new CubeDeformation(0.45F);
    private static final float HOODIE_BODY_INFLATE = 0.42F;
    private static final CubeDeformation HOODIE_HEM_INFLATE = new CubeDeformation(0.5F);
    private static final float HOODIE_ARM_INFLATE = 0.38F;
    private static final CubeDeformation HOODIE_CUFF_INFLATE = new CubeDeformation(0.46F);
    /** Sits above the hoodie (0.42) so the choker stays visible over clothing. */
    private static final CubeDeformation CHOKER_INFLATE = new CubeDeformation(0.52F);

    /** Socks cover the leg from this y (0 = hip) down to the foot. */
    public static final int SOCK_TOP = 2;

    public static final ModelLayerLocation CAT_EARS = layer("cat_ears");
    public static final ModelLayerLocation TAIL = layer("tail");
    public static final ModelLayerLocation SOCKS = layer("socks");
    public static final ModelLayerLocation FISHNET = layer("fishnet");
    public static final ModelLayerLocation SKIRT = layer("skirt");
    public static final ModelLayerLocation HOODIE = layer("hoodie");
    public static final ModelLayerLocation CHOKER = layer("choker");
    /** Flat cell grid over the hoodie chest for 2D patterns (Progress chevron). */
    public static final ModelLayerLocation HOODIE_CHEST_PANEL = layer("hoodie_chest_panel");
    /** Sleeves drawn over the first-person hand; same geometry as the hoodie's sleeves. */
    public static final ModelLayerLocation HOODIE_FIRST_PERSON = layer("hoodie_first_person");
    public static final ModelLayerLocation BACKPACK = layer("backpack");
    /** Charm hangers on the backpack's side; static relative to the body. */
    public static final ModelLayerLocation BACKPACK_CHARMS = layer("backpack_charms");
    public static final int CHARM_SLOTS = 3;
    public static final int PANEL_COLUMNS = 12;
    public static final int PANEL_ROWS = 16;
    public static final Map<String, ModelLayerLocation> HAIR_CLIPS = new LinkedHashMap<>();

    static {
        HairClipShapes.MASKS.keySet().forEach(shape -> HAIR_CLIPS.put(shape, layer("hair_clip_" + shape)));
    }

    private CosmeticModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, name), "main");
    }

    public static void registerLayers() {
        EntityModelLayerRegistry.register(CAT_EARS, CosmeticModels::catEars);
        EntityModelLayerRegistry.register(TAIL, CosmeticModels::tail);
        EntityModelLayerRegistry.register(SOCKS, () -> legwear(SOCK_INFLATE, true));
        EntityModelLayerRegistry.register(FISHNET, () -> legwear(FISHNET_INFLATE, false));
        EntityModelLayerRegistry.register(SKIRT, CosmeticModels::skirt);
        EntityModelLayerRegistry.register(HOODIE, CosmeticModels::hoodie);
        EntityModelLayerRegistry.register(CHOKER, CosmeticModels::choker);
        EntityModelLayerRegistry.register(HOODIE_CHEST_PANEL, CosmeticModels::hoodieChestPanel);
        EntityModelLayerRegistry.register(HOODIE_FIRST_PERSON, CosmeticModels::hoodieFirstPerson);
        EntityModelLayerRegistry.register(BACKPACK, CosmeticModels::backpack);
        EntityModelLayerRegistry.register(BACKPACK_CHARMS, CosmeticModels::backpackCharms);
        HAIR_CLIPS.forEach((shape, location) -> EntityModelLayerRegistry.register(location, () -> hairClip(shape)));
    }

    private static MeshDefinition emptyPlayerMesh() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        mesh.getRoot().clearRecursively();
        return mesh;
    }

    private static PartDefinition pivot(PartDefinition parent, String name, PartPose pose) {
        return parent.addOrReplaceChild(name, CubeListBuilder.create(), pose);
    }

    private static LayerDefinition layerOf(MeshDefinition mesh) {
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // ------------------------------------------------------------------ cat ears

    /** One ear seen from the front, bottom row sits on the headband. M outer, A inner, F fur tuft. */
    private static final String[] EAR = {
            "...MM...",
            "...MM...",
            "..MMMM..",
            "..MAAM..",
            ".MMAAMM.",
            ".MAAAAM.",
            "MMAAAAMM",
            "MAAAAAAM",
            "MAFAAFAM",
            "MFFAAFFM",
    };
    private static final float EAR_PIXEL = 0.45F;
    /** Distance of each ear's pivot from the head's center line. */
    private static final float EAR_SPACING = 2.9F;
    /** Outward tilt; a positive zRot leans the top toward +x, so the -x ear needs a negative angle. */
    private static final float EAR_TILT = 0.3F;

    private static LayerDefinition catEars() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");

        Groups.Builder band = new Groups.Builder(pivot(head, "headband", PartPose.ZERO));
        band.box(MAIN, -4.5F, -8.6F, -1.2F, 9, 0.8F, 1.6F);
        band.box(MAIN, -4.6F, -8.2F, -1.1F, 0.6F, 3.2F, 1.4F); // band ends go down to the ears' sides
        band.box(MAIN, 4.0F, -8.2F, -1.1F, 0.6F, 3.2F, 1.4F);
        band.build();

        float width = EAR[0].length() * EAR_PIXEL;
        float height = EAR.length * EAR_PIXEL;
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition ear = pivot(head, side < 0 ? "left_ear" : "right_ear",
                    PartPose.offsetAndRotation(side * EAR_SPACING, -8.3F, -0.2F, 0.0F, 0.0F, side * EAR_TILT));
            Groups.Builder b = new Groups.Builder(ear);
            // back shell (full silhouette), inner ear in front, fur tufts furthest front
            b.bandRange(-height, 0.0F);
            b.extrude(EAR, Map.of('M', BANDED, 'A', BANDED, 'F', BANDED), -width / 2, -height, -0.4F, EAR_PIXEL, 0.9F);
            b.extrude(EAR, Map.of('A', ACCENT, 'F', ACCENT), -width / 2, -height, -0.6F, EAR_PIXEL, 0.2F);
            b.extrude(EAR, Map.of('F', DETAIL), -width / 2, -height, -0.75F, EAR_PIXEL, 0.2F);
            b.build();
        }
        return layerOf(mesh);
    }

    // ------------------------------------------------------------------ tail

    private static final int TAIL_SEGMENTS = 8;
    private static final float[] TAIL_WIDTH = {2.0F, 2.3F, 2.5F, 2.5F, 2.4F, 2.2F, 2.0F, 1.6F};
    private static final float TAIL_SEGMENT_LENGTH = 1.7F;
    /** Last segments are the (white) fur tip. */
    private static final int TAIL_TIP_FROM = 6;

    private static LayerDefinition tail() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition parent = mesh.getRoot().getChild("body");
        for (int i = 0; i < TAIL_SEGMENTS; i++) {
            float w = TAIL_WIDTH[i];
            PartPose pose = i == 0 ? PartPose.offset(0.0F, 9.5F, 2.6F) : PartPose.offset(0.0F, 0.0F, TAIL_SEGMENT_LENGTH);
            parent = pivot(parent, "segment" + i, pose);
            Groups body = i >= TAIL_TIP_FROM ? DETAIL : MAIN;
            Groups fluff = i >= TAIL_TIP_FROM ? DETAIL : (i == TAIL_TIP_FROM - 1 ? DETAIL : ACCENT);
            Groups.Builder b = new Groups.Builder(parent).bandRange(0.0F, TAIL_SEGMENTS);
            if (body == MAIN) {
                b.boxInBand(i + 0.5F, -w / 2, -w / 2, 0.0F, w, w, TAIL_SEGMENT_LENGTH + 0.2F); // one pattern ring per segment
            } else {
                b.box(body, -w / 2, -w / 2, 0.0F, w, w, TAIL_SEGMENT_LENGTH + 0.2F);
            }
            // fluff: slightly offset, thinner slabs so the silhouette looks furry, not boxy
            b.box(fluff, -w / 2 - 0.2F, -w / 2 + 0.3F, 0.3F, 0.3F, w - 0.6F, TAIL_SEGMENT_LENGTH - 0.4F);
            b.box(fluff, w / 2 - 0.1F, -w / 2 + 0.3F, 0.3F, 0.3F, w - 0.6F, TAIL_SEGMENT_LENGTH - 0.4F);
            b.box(fluff, -w / 2 + 0.3F, -w / 2 - 0.2F, 0.2F, w - 0.6F, 0.3F, TAIL_SEGMENT_LENGTH - 0.3F);
            if (i == TAIL_SEGMENTS - 1) {
                b.box(DETAIL, -w / 2 + 0.3F, -w / 2 + 0.3F, TAIL_SEGMENT_LENGTH, w - 0.6F, w - 0.6F, 0.8F); // rounded tip
            }
            b.build();
        }
        return layerOf(mesh);
    }

    // ------------------------------------------------------------------ socks / tights

    private static LayerDefinition legwear(float inflate, boolean withBow) {
        MeshDefinition mesh = emptyPlayerMesh();
        for (String leg : new String[]{"left_leg", "right_leg"}) {
            PartDefinition legPart = mesh.getRoot().getChild(leg);
            Groups.Builder b = new Groups.Builder(legPart).bandRange(SOCK_TOP, 12.0F);
            b.bandedBox(-2.0F, SOCK_TOP, -2.0F, 4, 12 - SOCK_TOP, 4, inflate);
            b.box(ACCENT, -2.0F, SOCK_TOP - 0.8F, -2.0F, 4, 1.4F, 4, SOCK_CUFF_INFLATE); // ribbed top
            b.box(ACCENT, -2.0F, 11.0F, -2.45F, 4, 1.0F, 0.4F);                           // toe cap
            b.box(ACCENT, -2.0F, 10.6F, 2.05F, 4, 1.4F, 0.4F);                            // heel
            if (withBow) {
                float outer = leg.equals("left_leg") ? 2.35F : -2.85F;
                b.box(DETAIL, outer, SOCK_TOP - 1.2F, -0.9F, 0.5F, 0.9F, 0.8F);      // bow knot
                b.box(DETAIL, outer, SOCK_TOP - 1.4F, -1.9F, 0.4F, 1.3F, 0.9F);      // bow loops
                b.box(DETAIL, outer, SOCK_TOP - 1.4F, 0.0F, 0.4F, 1.3F, 0.9F);
            }
            b.build();
        }
        return layerOf(mesh);
    }

    // ------------------------------------------------------------------ skirt

    /** Pleat strips per side; alternate strips sit deeper for the pleated look. */
    private static final int FRONT_PLEATS = 6;
    private static final int SIDE_PLEATS = 3;
    private static final float WAIST_Y = 10.0F;
    private static final int SKIRT_LENGTH = 7;

    private static LayerDefinition skirt() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition body = mesh.getRoot().getChild("body");
        Groups.Builder waist = new Groups.Builder(pivot(body, "waist", PartPose.ZERO));
        waist.box(ACCENT, -4.6F, WAIST_Y - 0.6F, -2.6F, 9.2F, 1.2F, 5.2F);
        waist.box(DETAIL, -0.5F, WAIST_Y - 0.4F, -2.75F, 1.0F, 0.8F, 0.2F); // button
        waist.build();

        float pleat = 9.0F / FRONT_PLEATS;
        for (int i = 0; i < FRONT_PLEATS; i++) {
            float x = -4.5F + i * pleat;
            float depthOffset = i % 2 == 0 ? 0.0F : 0.25F;
            skirtStrip(body, "front" + i, PartPose.offset(x + pleat / 2, WAIST_Y, -2.3F - depthOffset), pleat, false);
            skirtStrip(body, "back" + i, PartPose.offset(x + pleat / 2, WAIST_Y, 2.3F + depthOffset), pleat, false);
        }
        float sidePleat = 5.0F / SIDE_PLEATS;
        for (int i = 0; i < SIDE_PLEATS; i++) {
            float z = -2.5F + i * sidePleat;
            float depthOffset = i % 2 == 0 ? 0.0F : 0.25F;
            skirtStrip(body, "left" + i, PartPose.offset(4.3F + depthOffset, WAIST_Y, z + sidePleat / 2), sidePleat, true);
            skirtStrip(body, "right" + i, PartPose.offset(-4.3F - depthOffset, WAIST_Y, z + sidePleat / 2), sidePleat, true);
        }
        return layerOf(mesh);
    }

    private static void skirtStrip(PartDefinition body, String name, PartPose pose, float width, boolean side) {
        Groups.Builder b = new Groups.Builder(pivot(body, name, pose)).bandRange(0.0F, SKIRT_LENGTH);
        if (side) {
            b.bandedBox(-0.3F, 0.0F, -width / 2, 0.6F, SKIRT_LENGTH, width, 0.0F);
            b.box(DETAIL, -0.35F, SKIRT_LENGTH - 1.2F, -width / 2, 0.7F, 0.4F, width); // hem stripe
        } else {
            b.bandedBox(-width / 2, 0.0F, -0.3F, width, SKIRT_LENGTH, 0.6F, 0.0F);
            b.box(DETAIL, -width / 2, SKIRT_LENGTH - 1.2F, -0.35F, width, 0.4F, 0.7F);
        }
        b.build();
    }

    // ------------------------------------------------------------------ hoodie

    private static LayerDefinition hoodie() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition root = mesh.getRoot();
        Groups.Builder body = new Groups.Builder(pivot(root.getChild("body"), "hoodie", PartPose.ZERO)).bandRange(0.0F, 12.0F);
        body.bandedBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, HOODIE_BODY_INFLATE);
        body.box(ACCENT, -4.0F, 12.0F, -2.0F, 8, 1.5F, 4, HOODIE_HEM_INFLATE);       // ribbed hem
        body.box(ACCENT, -3.2F, 7.2F, -2.75F, 6.4F, 3.3F, 0.35F);                    // kangaroo pocket
        body.box(DARK, -2.4F, 7.4F, -2.85F, 4.8F, 0.3F, 0.15F);                      // pocket opening
        // hood: back panel, side flaps and rim lying on the shoulders
        body.box(MAIN, -4.2F, -2.4F, 1.9F, 8.4F, 4.2F, 1.6F);
        body.box(MAIN, -4.7F, -1.8F, -0.6F, 0.8F, 3.0F, 2.6F);
        body.box(MAIN, 3.9F, -1.8F, -0.6F, 0.8F, 3.0F, 2.6F);
        body.box(ACCENT, -4.3F, -2.6F, 3.4F, 8.6F, 0.5F, 0.4F);
        // drawstrings with aglets
        for (float x : new float[]{-2.1F, 1.7F}) {
            body.box(DETAIL, x, 0.2F, -2.75F, 0.4F, 4.2F, 0.3F);
            body.box(DARK, x - 0.05F, 4.4F, -2.8F, 0.5F, 0.8F, 0.4F);
        }
        body.build();

        for (String arm : new String[]{"right_arm", "left_arm"}) {
            float x0 = arm.equals("right_arm") ? -3.0F : -1.0F;
            Groups.Builder sleeve = new Groups.Builder(pivot(root.getChild(arm), "sleeve", PartPose.ZERO)).bandRange(-2.0F, 9.0F);
            sleeve.bandedBox(x0, -2.0F, -2.0F, 4, 11, 4, HOODIE_ARM_INFLATE);
            sleeve.box(ACCENT, x0, 9.0F, -2.0F, 4, 1.5F, 4, HOODIE_CUFF_INFLATE);    // ribbed cuff
            sleeve.build();
            Groups.Builder cuff = new Groups.Builder(pivot(root.getChild(arm), "cuff", PartPose.ZERO));
            cuff.box(ACCENT, x0, 10.5F, -2.0F, 4, 2.2F, 4, HOODIE_CUFF_INFLATE);     // hands hidden in sleeves
            cuff.build();
        }
        return layerOf(mesh);
    }

    /** Stand-alone sleeves; their pose is copied from the first-person arm each frame. */
    private static LayerDefinition hoodieFirstPerson() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (String side : new String[]{"right", "left"}) {
            float x0 = side.equals("right") ? -3.0F : -1.0F;
            root.addOrReplaceChild(side + "_sleeve", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(x0, -2.0F, -2.0F, 4, 9, 4, new CubeDeformation(HOODIE_ARM_INFLATE)), PartPose.ZERO);
            root.addOrReplaceChild(side + "_cuff", CubeListBuilder.create().texOffs(16, 16)
                    .addBox(x0, 7.0F, -2.0F, 4, 1.5F, 4, HOODIE_CUFF_INFLATE), PartPose.ZERO);
        }
        return layerOf(mesh);
    }

    /** Cells in body space just in front of the hoodie torso's front face (x -4.42..4.42, y -0.42..12.42). */
    private static LayerDefinition hoodieChestPanel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float left = -4.0F - HOODIE_BODY_INFLATE;
        float top = -HOODIE_BODY_INFLATE;
        float width = 8.0F + 2 * HOODIE_BODY_INFLATE;
        float height = 12.0F + 2 * HOODIE_BODY_INFLATE;
        float cw = width / PANEL_COLUMNS;
        float ch = height / PANEL_ROWS;
        float front = -2.0F - HOODIE_BODY_INFLATE - 0.06F;
        for (int row = 0; row < PANEL_ROWS; row++) {
            for (int col = 0; col < PANEL_COLUMNS; col++) {
                root.addOrReplaceChild("cell" + (row * PANEL_COLUMNS + col),
                        CubeListBuilder.create().texOffs(col * 3 % 48, row * 3 % 48)
                                .addBox(left + col * cw, top + row * ch, front, cw, ch, 0.05F),
                        PartPose.ZERO);
            }
        }
        return layerOf(mesh);
    }

    // ------------------------------------------------------------------ backpack

    /** Creeper face patch on the front pocket (SPEC §1.1: creeper is a Minecraft asset, fine to use). */
    private static final String[] CREEPER_FACE = {
            "GGGGGGGG",
            "GDDGGDDG",
            "GDDGGDDG",
            "GGGDDGGG",
            "GGDDDDGG",
            "GGDDDDGG",
            "GGDGGDGG",
            "GGGGGGGG",
    };
    private static final float BACK_Z = 2.55F;
    private static final float PACK_DEPTH = 4.0F;

    private static LayerDefinition backpack() {
        MeshDefinition mesh = emptyPlayerMesh();
        Groups.Builder b = new Groups.Builder(pivot(mesh.getRoot().getChild("body"), "backpack", PartPose.ZERO)).bandRange(1.0F, 10.5F);
        b.bandedBox(-3.6F, 1.0F, BACK_Z, 7.2F, 9.5F, PACK_DEPTH, 0.0F);                  // main bag
        b.box(ACCENT, -3.8F, 0.4F, BACK_Z - 0.1F, 7.6F, 2.2F, PACK_DEPTH + 0.3F);        // top flap
        b.box(ACCENT, -2.6F, 5.2F, BACK_Z + PACK_DEPTH, 5.2F, 4.4F, 0.8F);                // front pocket
        b.box(DARK, -0.4F, 2.4F, BACK_Z + PACK_DEPTH + 0.2F, 0.8F, 0.8F, 0.2F);          // flap buckle
        b.box(ACCENT, -3.9F, 9.8F, BACK_Z - 0.1F, 7.8F, 0.8F, PACK_DEPTH + 0.2F);        // reinforced bottom
        float face = 0.5F;
        b.extrude(CREEPER_FACE, Map.of('G', DETAIL, 'D', DARK), -2.0F, 5.4F, BACK_Z + PACK_DEPTH + 0.8F, face, 0.15F);
        // shoulder straps: over the shoulders and down the chest (over clothing)
        for (float x : new float[]{-2.9F, 1.7F}) {
            b.box(ACCENT, x, -0.55F, -2.6F, 1.2F, 0.5F, BACK_Z + 2.7F);
            b.box(ACCENT, x, -0.3F, -2.65F, 1.2F, 8.0F, 0.2F);
            b.box(METAL, x + 0.2F, 6.0F, -2.75F, 0.8F, 0.6F, 0.15F);                        // strap buckle
        }
        b.build();
        return layerOf(mesh);
    }

    /** Charm hangers in body space on the backpack's right side (+x): chain + charm per slot. */
    private static LayerDefinition backpackCharms() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < CHARM_SLOTS; i++) {
            float z = BACK_Z + 0.8F + i * 1.3F;
            root.addOrReplaceChild("chain" + i, CubeListBuilder.create().texOffs(0, 0)
                    .addBox(3.6F, 2.0F, z, 0.3F, 1.6F, 0.3F), PartPose.ZERO);
            root.addOrReplaceChild("charm" + i, CubeListBuilder.create().texOffs(4 * i, 8)
                    .addBox(3.45F, 3.6F, z - 0.35F, 1.0F, 1.2F, 1.0F), PartPose.ZERO);
        }
        return layerOf(mesh);
    }

    // ------------------------------------------------------------------ choker

    private static LayerDefinition choker() {
        MeshDefinition mesh = emptyPlayerMesh();
        Groups.Builder b = new Groups.Builder(pivot(mesh.getRoot().getChild("body"), "choker", PartPose.ZERO));
        b.box(MAIN, -4.0F, 0.0F, -2.0F, 8, 1, 4, CHOKER_INFLATE);
        b.box(METAL, -0.9F, 0.6F, -2.95F, 1.8F, 0.4F, 0.3F);   // O-ring top
        b.box(METAL, -0.9F, 1.4F, -2.95F, 1.8F, 0.4F, 0.3F);   // O-ring bottom
        b.box(METAL, -0.9F, 0.6F, -2.95F, 0.4F, 1.2F, 0.3F);
        b.box(METAL, 0.5F, 0.6F, -2.95F, 0.4F, 1.2F, 0.3F);
        b.box(METAL, -0.55F, 1.8F, -3.2F, 1.1F, 1.0F, 1.0F);   // bell
        b.box(METAL, -0.4F, 2.8F, -3.1F, 0.8F, 0.25F, 0.8F);   // bell rim
        b.box(DARK, -0.4F, 2.45F, -3.25F, 0.8F, 0.15F, 0.1F);  // bell slit
        b.build();
        return layerOf(mesh);
    }

    // ------------------------------------------------------------------ hair clips

    private static final float CLIP_PIXEL = 0.3F;

    private static LayerDefinition hairClip(String shape) {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition clip = pivot(mesh.getRoot().getChild("head"), "clip",
                PartPose.offsetAndRotation(2.2F, -7.2F, -4.3F, 0.0F, 0.0F, -0.3F));
        String[] rows = HairClipShapes.MASKS.get(shape);
        float w = rows[0].length() * CLIP_PIXEL;
        float h = rows.length * CLIP_PIXEL;
        Groups.Builder b = new Groups.Builder(clip);
        b.extrude(rows, Map.of('R', MAIN, 'O', ACCENT, 'Y', METAL, 'G', DARK, 'W', DETAIL), -w / 2, -h / 2, -0.45F, CLIP_PIXEL, 0.45F);
        b.box(DARK, -w / 2, h / 2 - 0.1F, -0.2F, w, 0.3F, 0.5F); // clip bar
        b.build();
        return layerOf(mesh);
    }

    // ================================================================== models

    static CosmeticMotion motion(AvatarRenderState state) {
        return FemboyClientApi.get().motion(state);
    }

    /** Base: shows one color group of the layer. */
    public static class GroupModel extends PlayerModel {
        public GroupModel(ModelPart root, Groups group, int band) {
            super(root, false);
            Groups.show(root, group, band);
        }
    }

    /** Ears flick independently now and then; phase differs per player. */
    public static final class CatEarsModel extends GroupModel {
        private static final float TWITCH_SPEED = 0.35F;
        private static final float TWITCH_SHARPNESS = 12.0F;
        private static final float TWITCH_AMOUNT = 0.35F;
        private static final float BASE_TILT = EAR_TILT;
        private static final float WALK_BOUNCE = 0.08F;
        private final ModelPart leftEar;
        private final ModelPart rightEar;

        public CatEarsModel(ModelPart root, Groups group, int band) {
            super(root, group, band);
            this.leftEar = head.getChild("left_ear");
            this.rightEar = head.getChild("right_ear");
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            CosmeticMotion motion = motion(state);
            float t = state.ageInTicks * TWITCH_SPEED / Mth.PI + motion.phase();
            float left = (float) Math.pow(Math.max(Mth.sin(t), 0.0F), TWITCH_SHARPNESS) * TWITCH_AMOUNT;
            float right = (float) Math.pow(Math.max(Mth.sin(t * 0.83F + 1.7F), 0.0F), TWITCH_SHARPNESS) * TWITCH_AMOUNT;
            float bounce = Mth.sin(state.walkAnimationPos * 0.6662F * 2) * WALK_BOUNCE * motion.walkAmount();
            // "left_ear" sits at -x: tilt it outward (negative zRot); twitches flick further outward.
            leftEar.zRot = -BASE_TILT - left - bounce;
            rightEar.zRot = BASE_TILT + right + bounce;
            leftEar.xRot = -left * 0.5F;
            rightEar.xRot = -right * 0.5F;
        }
    }

    /** Tail droops down-back, waves along its length, swings out when turning (SPEC §4.5). */
    public static final class TailModel extends GroupModel {
        private static final float DROOP = -0.5F;
        private static final float CURL = 0.14F;
        private static final float IDLE_SWAY = 0.08F;
        private static final float WALK_SWAY = 0.2F;
        private static final float SWAY_SPEED = 0.16F;
        private static final float SEGMENT_PHASE = 0.45F;
        private static final float WALK_LIFT = 0.25F;
        private final ModelPart[] segments = new ModelPart[TAIL_SEGMENTS];

        public TailModel(ModelPart root, Groups group, int band) {
            super(root, group, band);
            ModelPart part = body;
            for (int i = 0; i < TAIL_SEGMENTS; i++) {
                part = part.getChild("segment" + i);
                segments[i] = part;
            }
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            CosmeticMotion motion = motion(state);
            float amplitude = IDLE_SWAY + WALK_SWAY * motion.walkAmount();
            for (int i = 0; i < TAIL_SEGMENTS; i++) {
                ModelPart segment = segments[i];
                segment.xRot = i == 0 ? DROOP + WALK_LIFT * motion.walkAmount() : CURL;
                float wave = Mth.sin(state.ageInTicks * SWAY_SPEED + motion.phase() - i * SEGMENT_PHASE);
                segment.yRot = wave * amplitude + motion.turnSway() / TAIL_SEGMENTS;
            }
        }
    }

    /** Pleat strips follow the legs individually; outer strips flare more (SPEC §4.5). */
    public static final class SkirtModel extends GroupModel {
        private static final float REST_FLARE = 0.1F;
        private static final float LEG_FOLLOW = 0.9F;
        private static final float OUTER_EXTRA = 0.25F;
        private final ModelPart[] front = new ModelPart[FRONT_PLEATS];
        private final ModelPart[] back = new ModelPart[FRONT_PLEATS];
        private final ModelPart[] left = new ModelPart[SIDE_PLEATS];
        private final ModelPart[] right = new ModelPart[SIDE_PLEATS];

        public SkirtModel(ModelPart root, Groups group, int band) {
            super(root, group, band);
            for (int i = 0; i < FRONT_PLEATS; i++) {
                front[i] = body.getChild("front" + i);
                back[i] = body.getChild("back" + i);
            }
            for (int i = 0; i < SIDE_PLEATS; i++) {
                left[i] = body.getChild("left" + i);
                right[i] = body.getChild("right" + i);
            }
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            float half = (FRONT_PLEATS - 1) / 2.0F;
            for (int i = 0; i < FRONT_PLEATS; i++) {
                // strips on the left half follow the left leg, right half the right leg
                float leg = i < FRONT_PLEATS / 2 ? rightLeg.xRot : leftLeg.xRot;
                float outer = 1.0F + OUTER_EXTRA * Math.abs(i - half) / half;
                front[i].xRot = (-REST_FLARE + Math.min(0.0F, leg) * LEG_FOLLOW) * outer;
                back[i].xRot = (REST_FLARE + Math.max(0.0F, leg) * LEG_FOLLOW) * outer;
                if (state.isCrouching) {
                    back[i].xRot += REST_FLARE * 3;
                }
            }
            for (int i = 0; i < SIDE_PLEATS; i++) {
                left[i].zRot = -REST_FLARE - Math.abs(leftLeg.xRot) * 0.2F;
                right[i].zRot = REST_FLARE + Math.abs(rightLeg.xRot) * 0.2F;
            }
        }
    }

    /** Oversized hoodie; hands disappear into the sleeves while crouching (SPEC §5.1). */
    public static final class HoodieModel extends GroupModel {
        private final ModelPart leftCuff;
        private final ModelPart rightCuff;

        public HoodieModel(ModelPart root, Groups group, int band) {
            super(root, group, band);
            this.leftCuff = leftArm.getChild("cuff");
            this.rightCuff = rightArm.getChild("cuff");
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            leftCuff.visible = state.isCrouching;
            rightCuff.visible = state.isCrouching;
        }
    }
}
