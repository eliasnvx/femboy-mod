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

/**
 * Placeholder cosmetic geometry (SPEC §16 Phase 2: "placeholder geometry first"). Each model is a
 * {@link PlayerModel} with a cleared mesh plus extra parts under head/body/legs/arms, so vanilla
 * posing (walk, crouch, swim) moves them, and procedural animation happens in {@code setupAnim},
 * which runs at flush time with the right player's state. Units are model pixels.
 * Replace by GeckoLib Blockbench models (renderer {@code femboymod:geo}) once art exists.
 */
public final class CosmeticModels {

    private static final int TEXTURE_SIZE = 64;
    /** Overlay inflation above the skin's own second layer (0.25) to avoid z-fighting (SPEC §4.5). */
    public static final CubeDeformation SOCK_INFLATE = new CubeDeformation(0.32F);
    public static final CubeDeformation FISHNET_INFLATE = new CubeDeformation(0.29F);
    private static final CubeDeformation HOODIE_BODY_INFLATE = new CubeDeformation(0.42F);
    private static final CubeDeformation HOODIE_ARM_INFLATE = new CubeDeformation(0.38F);
    private static final CubeDeformation CUFF_INFLATE = new CubeDeformation(0.44F);
    /** Sits above the hoodie (0.42) so the choker stays visible over clothing. */
    private static final CubeDeformation CHOKER_INFLATE = new CubeDeformation(0.52F);

    /** Socks cover the leg from this y (0 = hip) down to the foot. */
    public static final int SOCK_TOP = 2;
    public static final int SOCK_BANDS = 12 - SOCK_TOP;

    public static final ModelLayerLocation CAT_EARS = layer("cat_ears");
    public static final ModelLayerLocation TAIL = layer("tail");
    public static final ModelLayerLocation SOCKS = layer("socks");
    public static final ModelLayerLocation FISHNET = layer("fishnet");
    public static final ModelLayerLocation SKIRT = layer("skirt");
    public static final ModelLayerLocation HOODIE = layer("hoodie");
    public static final ModelLayerLocation CHOKER = layer("choker");
    public static final ModelLayerLocation HAIR_CLIP = layer("hair_clip");

    private CosmeticModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, name), "main");
    }

    public static void registerLayers() {
        EntityModelLayerRegistry.register(CAT_EARS, CosmeticModels::catEars);
        EntityModelLayerRegistry.register(TAIL, CosmeticModels::tail);
        EntityModelLayerRegistry.register(SOCKS, () -> legwear(SOCK_INFLATE));
        EntityModelLayerRegistry.register(FISHNET, () -> legwear(FISHNET_INFLATE));
        EntityModelLayerRegistry.register(SKIRT, CosmeticModels::skirt);
        EntityModelLayerRegistry.register(HOODIE, CosmeticModels::hoodie);
        EntityModelLayerRegistry.register(CHOKER, CosmeticModels::choker);
        EntityModelLayerRegistry.register(HAIR_CLIP, CosmeticModels::hairClip);
    }

    private static MeshDefinition emptyPlayerMesh() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        mesh.getRoot().clearRecursively();
        return mesh;
    }

    private static CubeListBuilder box(float x, float y, float z, float w, float h, float d) {
        return CubeListBuilder.create().texOffs(0, 0).addBox(x, y, z, w, h, d);
    }

    private static CubeListBuilder box(float x, float y, float z, float w, float h, float d, CubeDeformation inflate) {
        return CubeListBuilder.create().texOffs(0, 0).addBox(x, y, z, w, h, d, inflate);
    }

    // ---- layer definitions ----

    private static LayerDefinition catEars() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("headband", box(-4.5F, -8.6F, -1.0F, 9, 1, 2), PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition ear = head.addOrReplaceChild(side < 0 ? "left_ear" : "right_ear",
                    box(-1.5F, -3.0F, -0.5F, 3, 3, 1), PartPose.offset(side * 2.5F, -8.3F, 0.0F));
            ear.addOrReplaceChild("tip", box(-0.5F, -1.0F, -0.5F, 1, 1, 1), PartPose.offset(side * 0.5F, -3.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static LayerDefinition tail() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition parent = mesh.getRoot().getChild("body");
        float[] widths = {2.4F, 2.2F, 2.0F, 1.8F, 1.4F};
        for (int i = 0; i < widths.length; i++) {
            float w = widths[i];
            // Root sits just outside the back (and any hoodie), above the skirt's waist.
            PartPose pose = i == 0 ? PartPose.offset(0.0F, 9.5F, 2.6F) : PartPose.offset(0.0F, 0.0F, 2.6F);
            parent = parent.addOrReplaceChild("segment" + i, box(-w / 2, -w / 2, 0.0F, w, w, 3.0F), pose);
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static LayerDefinition legwear(CubeDeformation inflate) {
        MeshDefinition mesh = emptyPlayerMesh();
        for (String leg : new String[]{"left_leg", "right_leg"}) {
            PartDefinition legPart = mesh.getRoot().getChild(leg);
            for (int band = 0; band < SOCK_BANDS; band++) {
                legPart.addOrReplaceChild("band" + band, box(-2.0F, SOCK_TOP + band, -2.0F, 4, 1, 4, inflate), PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static LayerDefinition skirt() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition body = mesh.getRoot().getChild("body");
        float waist = 10.0F;
        int length = 7;
        body.addOrReplaceChild("waistband", box(-4.5F, waist - 0.5F, -2.5F, 9, 1, 5), PartPose.ZERO);
        body.addOrReplaceChild("front", box(-4.5F, 0.0F, -0.5F, 9, length, 1), PartPose.offset(0.0F, waist, -2.2F));
        body.addOrReplaceChild("back", box(-4.5F, 0.0F, -0.5F, 9, length, 1), PartPose.offset(0.0F, waist, 2.2F));
        body.addOrReplaceChild("left", box(-0.5F, 0.0F, -2.5F, 1, length, 5), PartPose.offset(4.2F, waist, 0.0F));
        body.addOrReplaceChild("right", box(-0.5F, 0.0F, -2.5F, 1, length, 5), PartPose.offset(-4.2F, waist, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static LayerDefinition hoodie() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("torso", box(-4.0F, 0.0F, -2.0F, 8, 13, 4, HOODIE_BODY_INFLATE), PartPose.ZERO);
        body.addOrReplaceChild("hood", box(-4.0F, -1.5F, 1.7F, 8, 4, 2), PartPose.ZERO);
        body.addOrReplaceChild("pocket", box(-3.0F, 8.0F, -2.9F, 6, 2, 1), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("sleeve", box(-3.0F, -2.0F, -2.0F, 4, 12, 4, HOODIE_ARM_INFLATE), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("sleeve", box(-1.0F, -2.0F, -2.0F, 4, 12, 4, HOODIE_ARM_INFLATE), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("cuff", box(-3.0F, 9.5F, -2.0F, 4, 3, 4, CUFF_INFLATE), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("cuff", box(-1.0F, 9.5F, -2.0F, 4, 3, 4, CUFF_INFLATE), PartPose.ZERO);
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static LayerDefinition choker() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("band", box(-4.0F, 0.0F, -2.0F, 8, 1, 4, CHOKER_INFLATE), PartPose.ZERO);
        body.addOrReplaceChild("bell", box(-0.75F, 1.0F, -3.4F, 1.5F, 1.5F, 1.2F), PartPose.ZERO);
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private static LayerDefinition hairClip() {
        MeshDefinition mesh = emptyPlayerMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("clip", box(1.5F, -7.8F, -4.6F, 2.5F, 1.2F, 1.0F), PartPose.rotation(0.0F, 0.0F, -0.35F));
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    // ---- models ----

    static CosmeticMotion motion(AvatarRenderState state) {
        return FemboyClientApi.get().motion(state);
    }

    /** Ears twitch now and then; phase differs per player. */
    public static final class CatEarsModel extends PlayerModel {
        private static final float BASE_TILT = 0.12F;
        private static final float TWITCH_SPEED = 0.35F;
        private static final float TWITCH_SHARPNESS = 12.0F;
        private static final float TWITCH_AMOUNT = 0.3F;
        private final ModelPart leftEar;
        private final ModelPart rightEar;

        public CatEarsModel(ModelPart root) {
            super(root, false);
            this.leftEar = head.getChild("left_ear");
            this.rightEar = head.getChild("right_ear");
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            float phase = motion(state).phase();
            // Mostly still; a short flick when the (sharpened) sine peaks.
            float wave = Mth.sin(state.ageInTicks * TWITCH_SPEED / Mth.PI + phase);
            float twitch = (float) Math.pow(Math.max(wave, 0.0F), TWITCH_SHARPNESS) * TWITCH_AMOUNT;
            leftEar.zRot = -BASE_TILT - twitch;
            rightEar.zRot = BASE_TILT + twitch * 0.5F;
        }
    }

    /** Tail droops down-back, sways with walking and swings out when turning (SPEC §4.5). */
    public static final class TailModel extends PlayerModel {
        private static final int SEGMENTS = 5;
        private static final float DROOP = -0.45F;
        private static final float CURL = 0.2F;
        private static final float IDLE_SWAY = 0.12F;
        private static final float WALK_SWAY = 0.28F;
        private static final float SWAY_SPEED = 0.18F;
        private static final float SEGMENT_PHASE = 0.55F;
        private final ModelPart[] segments = new ModelPart[SEGMENTS];

        public TailModel(ModelPart root) {
            super(root, false);
            ModelPart part = body;
            for (int i = 0; i < SEGMENTS; i++) {
                part = part.getChild("segment" + i);
                segments[i] = part;
            }
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            CosmeticMotion motion = motion(state);
            float amplitude = IDLE_SWAY + WALK_SWAY * motion.walkAmount();
            for (int i = 0; i < SEGMENTS; i++) {
                ModelPart segment = segments[i];
                segment.xRot = i == 0 ? DROOP : CURL;
                float wave = Mth.sin(state.ageInTicks * SWAY_SPEED + motion.phase() - i * SEGMENT_PHASE);
                segment.yRot = wave * amplitude + motion.turnSway() / SEGMENTS;
            }
        }
    }

    /** One band of socks/tights; {@code visibleBand} = -1 shows all bands (solid color). */
    public static final class LegwearModel extends PlayerModel {
        public LegwearModel(ModelPart root, int visibleBand) {
            super(root, false);
            for (ModelPart leg : new ModelPart[]{leftLeg, rightLeg}) {
                for (int band = 0; band < SOCK_BANDS; band++) {
                    leg.getChild("band" + band).visible = visibleBand < 0 || visibleBand == band;
                }
            }
        }
    }

    /** Skirt panels flare with the legs so running legs never poke through (SPEC §4.5). */
    public static final class SkirtModel extends PlayerModel {
        private static final float REST_FLARE = 0.12F;
        private static final float LEG_FOLLOW = 0.85F;
        private final ModelPart front;
        private final ModelPart back;
        private final ModelPart left;
        private final ModelPart right;

        public SkirtModel(ModelPart root) {
            super(root, false);
            this.front = body.getChild("front");
            this.back = body.getChild("back");
            this.left = body.getChild("left");
            this.right = body.getChild("right");
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            // Leg xRot < 0 means the leg swings forward.
            float forward = Math.min(0.0F, Math.min(leftLeg.xRot, rightLeg.xRot));
            float backward = Math.max(0.0F, Math.max(leftLeg.xRot, rightLeg.xRot));
            front.xRot = -REST_FLARE + forward * LEG_FOLLOW;
            back.xRot = REST_FLARE + backward * LEG_FOLLOW;
            left.zRot = -REST_FLARE - Math.abs(leftLeg.zRot);
            right.zRot = REST_FLARE + Math.abs(rightLeg.zRot);
            if (state.isCrouching) {
                // Crouching pushes legs back (z+4) under a tilted body: open the back panel more.
                back.xRot += REST_FLARE * 3;
            }
        }
    }

    /** Oversized hoodie; hands disappear into the sleeves while crouching (SPEC §5.1). */
    public static final class HoodieModel extends PlayerModel {
        private final ModelPart leftCuff;
        private final ModelPart rightCuff;

        public HoodieModel(ModelPart root) {
            super(root, false);
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

    /** Choker band ({@code bell = false}) or its bell ({@code bell = true}), drawn with different textures. */
    public static final class ChokerModel extends PlayerModel {
        public ChokerModel(ModelPart root, boolean bell) {
            super(root, false);
            body.getChild("band").visible = !bell;
            body.getChild("bell").visible = bell;
        }
    }

    public static final class HairClipModel extends PlayerModel {
        public HairClipModel(ModelPart root) {
            super(root, false);
        }
    }
}
