package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.block.MirrorBlock;
import dev.eliasnvx.femboymod.block.MirrorBlockEntity;
import dev.eliasnvx.femboymod.block.VanityTableBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Live reflections of nearby players in a {@link MirrorBlock}. Each reflection is the player mirrored in the glass
 * (position and facing), slid along the view rays into the mirror (same picture on screen,
 * {@link MirrorMath#slideScale}) and squashed into its few pixels of depth ({@link MirrorMath#squash}). Players and
 * pets ({@code #femboymod:mirror_reflected}, tamed) are reflected, not the room. Reflections are cut to what is seen through the glass ({@link ClippingSubmitCollector}), so
 * nothing spills over the frame. A translucent pane with a sheen goes on top. Models are not mirrored left to right
 * (that would turn them inside out with backface culling).
 */
public final class MirrorRenderer implements BlockEntityRenderer<MirrorBlockEntity, MirrorRenderer.State> {

    public static final ModelLayerLocation GLASS_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "mirror_glass"), "main");
    private static final RenderType GLASS = RenderTypes.entityTranslucentCull(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/mirror_glass.png"));
    private static final RenderType FLOOR = RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/mirror_floor.png"));

    /**
     * The mirror stands at the back of its block (4 px deep): glass plane, its depth to the back panel and the
     * frame width, in blocks (model: 16 = one block).
     */
    private static final double GLASS_INSET = 12.5 / 16;
    private static final double CAVITY = 2.5 / 16;
    /** Standing mirror glass: 2 px frame, two blocks tall. */
    private static final double STANDING_LEFT = 2.0 / 16;
    private static final double STANDING_BOTTOM = 2.0 / 16;
    private static final double STANDING_TOP = 2.0 - 2.0 / 16;
    /** Vanity table glass: in the upper half, above the table top (model: tools/decor/make_interior.py). */
    private static final double VANITY_LEFT = 3.0 / 16;
    private static final double VANITY_BOTTOM = 1.0 + 2.0 / 16;
    private static final double VANITY_TOP = 1.0 + 14.0 / 16;
    /** Gap between the layers in the cavity: glass, reflections, reflected floor, back panel. */
    private static final double DEPTH_MARGIN = 0.01;
    /**
     * Reflections fill the cavity from just behind the glass to just in front of the reflected floor, which lies
     * just in front of the back panel; so the floor and the panel never cover a reflection.
     */
    private static final double REFLECTION_NEAR = DEPTH_MARGIN;
    private static final double REFLECTION_FAR = CAVITY - 2 * DEPTH_MARGIN;
    private static final double REFLECTION_MIDDLE = (REFLECTION_NEAR + REFLECTION_FAR) / 2;
    private static final double FLOOR_BEHIND_GLASS = CAVITY - DEPTH_MARGIN;
    /**
     * How deep a posed model can be, at full size: legs mid-stride, arms held out, held items, tails and backpacks
     * reach well past the bounding box. Reflections are squashed so that much fits between the glass and the floor.
     */
    private static final double MODEL_DEPTH = 1.5;
    /** How far a player model reaches out from its middle (arms, backpacks, tails), for the quick visibility check. */
    private static final double MODEL_HALF_WIDTH = 0.5;
    private static final double RANGE = 12.0;
    /** Players closer than this to the glass are not reflected (the reflection would be inside the frame). */
    private static final double MIN_FRONT = 0.2;
    private static final int MAX_REFLECTIONS = 6;
    private static final int GLASS_COLOR = 0xFFFFFFFF;
    /**
     * The glass goes in a later submit order than the reflections: within one order translucent models are drawn
     * before translucent custom geometry (what the clipped reflections become), and the sheen, which writes depth,
     * would hide translucent parts such as player skins behind it. Past the orders entity layers use (vanilla: up
     * to 2), so reflected layers come first too.
     */
    private static final int GLASS_ORDER = 3;
    private static final java.util.function.Predicate<LivingEntity> REFLECTED_PETS = entity ->
            !(entity instanceof Player) && entity.typeHolder().is(FemboyTags.MIRROR_REFLECTED)
                    && (!(entity instanceof TamableAnimal pet) || pet.isTame());
    /** The reflected floor reaches this far behind the glass (a room, not the horizon). */
    private static final double FLOOR_DEPTH = 6.0;
    /** The reflection of the floor is a little darker than the floor. */
    private static final float FLOOR_SHADE = -0.15F;

    private final EntityRenderDispatcher entities;
    /** Who may be reflected this frame; reused (render thread). */
    private final List<LivingEntity> candidates = new ArrayList<>();
    private final ModelPart floor;
    /** The standing mirrors' glass (2 px frame, two blocks tall) and the vanity table's smaller one. */
    private final Shape standing;
    private final Shape vanity;
    /** Scratch vectors for {@link #windowPlanes}, render thread only. */
    private final Vector3f eye = new Vector3f();
    private final Vector3f[] corners = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    private final Vector3f center = new Vector3f();
    private final Vector3f edgeA = new Vector3f();
    private final Vector3f edgeB = new Vector3f();
    private final Vector3f normal = new Vector3f();

    public MirrorRenderer(BlockEntityRendererProvider.Context context) {
        this.entities = context.entityRenderer();
        ModelPart root = context.bakeLayer(GLASS_LAYER);
        this.floor = root.getChild("floor");
        this.standing = new Shape(STANDING_LEFT, 1.0 - STANDING_LEFT, STANDING_BOTTOM, STANDING_TOP, true, root.getChild("glass"));
        this.vanity = new Shape(VANITY_LEFT, 1.0 - VANITY_LEFT, VANITY_BOTTOM, VANITY_TOP, false, root.getChild("vanity_glass"));
    }

    /**
     * Where the glass is, in blocks, for a mirror facing north: across from {@code left} to {@code right}, up from
     * {@code bottom} to {@code top} above the lower half's bottom. {@code floor}: the reflected floor shows at its
     * bottom (a standing mirror; a vanity mirror sits on a table).
     */
    record Shape(double left, double right, double bottom, double top, boolean floor, ModelPart glass) {
    }

    /**
     * Glass panes for a mirror facing north: the standing mirror's 12x28 pixels (2 px frame all around), the vanity
     * table's 10x12 in the upper half; and the floor strip.
     */
    public static LayerDefinition glassLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("glass", CubeListBuilder.create().texOffs(0, 0)
                .addBox(pixels(STANDING_LEFT), pixels(STANDING_BOTTOM), (float) (GLASS_INSET * 16),
                        pixels(1.0 - 2 * STANDING_LEFT), pixels(STANDING_TOP - STANDING_BOTTOM), 0.0F), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("vanity_glass", CubeListBuilder.create().texOffs(0, 0)
                .addBox(pixels(VANITY_LEFT), pixels(VANITY_BOTTOM), (float) (GLASS_INSET * 16),
                        pixels(1.0 - 2 * VANITY_LEFT), pixels(VANITY_TOP - VANITY_BOTTOM), 0.0F), PartPose.ZERO);
        // one pixel tall strip at the bottom of the glass, stretched up to the reflected floor's horizon
        mesh.getRoot().addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 0)
                .addBox(2.0F, 0.0F, (float) (GLASS_INSET * 16), 12.0F, 1.0F, 0.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static float pixels(double blocks) {
        return (float) (blocks * 16);
    }

    /** One reflected player, reused between frames. */
    static final class Reflection {
        EntityRenderState state;
        double x;
        double y;
        double z;
        float scale;
        float squash;
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        @Nullable Shape shape;
        final List<Reflection> reflections = new ArrayList<>();
        int count;
        double cameraX;
        double cameraY;
        double cameraZ;
        /** The middle of the reflections' layer in the cavity (block-local), squashing happens about it. */
        double middleX;
        double middleZ;
        /** Reflected floor: color (0 = none), top edge on the glass, scale that moves it to the back panel. */
        int floorColor;
        float floorTop;
        float floorScale;
        /** The planes through the eye and the glass's edges that reflections are cut to; filled in {@code submit}. */
        final float[] window = new float[16];
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true; // the block entity is in the lower half, the glass goes up into the upper one
    }

    @Override
    public void extractRenderState(MirrorBlockEntity mirror, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mirror, state, partialTicks, camera, breakProgress);
        state.facing = mirror.getBlockState().getValue(MirrorBlock.FACING);
        state.shape = mirror.getBlockState().getBlock() instanceof VanityTableBlock ? vanity : standing;
        state.count = 0;
        Level level = mirror.getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = mirror.getBlockPos();
        state.cameraX = camera.x - pos.getX();
        state.cameraY = camera.y - pos.getY();
        state.cameraZ = camera.z - pos.getZ();
        double nx = state.facing.getStepX();
        double nz = state.facing.getStepZ();
        double qx = pos.getX() + 0.5 + nx * (0.5 - GLASS_INSET);
        double qz = pos.getZ() + 0.5 + nz * (0.5 - GLASS_INSET);
        state.middleX = qx - nx * REFLECTION_MIDDLE - pos.getX();
        state.middleZ = qz - nz * REFLECTION_MIDDLE - pos.getZ();
        double cameraFront = MirrorMath.front(camera.x, camera.z, qx, qz, nx, nz);
        state.floorColor = 0;
        if (cameraFront <= 0) {
            return; // looking at the back of the mirror
        }
        if (state.shape.floor()) {
            extractFloor(level, pos, state, cameraFront);
        }
        // players, then pets (#femboymod:mirror_reflected; tameable ones once tamed)
        candidates.clear();
        candidates.addAll(level.players());
        level.getEntities(EntityTypeTest.forClass(LivingEntity.class), new AABB(pos).inflate(RANGE),
                REFLECTED_PETS, candidates);
        for (LivingEntity reflectedEntity : candidates) {
            if (state.count == MAX_REFLECTIONS) {
                break;
            }
            if (reflectedEntity.isInvisible() || reflectedEntity.isSpectator()) {
                continue;
            }
            Vec3 feet = reflectedEntity.getPosition(partialTicks);
            double front = MirrorMath.front(feet.x, feet.z, qx, qz, nx, nz);
            if (front < MIN_FRONT || feet.distanceToSqr(qx, pos.getY() + 1.0, qz) > RANGE * RANGE) {
                continue;
            }
            double rx = feet.x - 2 * front * nx;
            double rz = feet.z - 2 * front * nz;
            if (!seenThroughGlass(camera, cameraFront, rx, feet.y, rz, reflectedEntity.getBbHeight(), front, pos, state.facing, state.shape)) {
                continue;
            }
            float scale = MirrorMath.slideScale(cameraFront, front, REFLECTION_MIDDLE);
            // posed models reach past their bounding box (its diagonal for a turned one)
            double thickness = Math.max(reflectedEntity.getBbWidth() * Math.sqrt(2), MODEL_DEPTH) * scale;
            EntityRenderState reflected = entities.extractEntity(reflectedEntity, partialTicks);
            hideExtras(reflected);
            if (reflected instanceof LivingEntityRenderState living) {
                living.bodyRot = MirrorMath.reflectYaw(living.bodyRot, nx, nz);
                living.yRot = -living.yRot; // head turn relative to the body flips too
            }
            reflected.lightCoords = state.lightCoords;
            if (state.reflections.size() == state.count) {
                state.reflections.add(new Reflection());
            }
            Reflection reflection = state.reflections.get(state.count++);
            reflection.state = reflected;
            reflection.x = rx - pos.getX();
            reflection.y = feet.y - pos.getY();
            reflection.z = rz - pos.getZ();
            reflection.scale = scale;
            reflection.squash = MirrorMath.squash(thickness, REFLECTION_FAR - REFLECTION_NEAR);
        }
    }

    /**
     * The floor in front of the mirror, reflected: on the glass it fills the bottom up to where floor at
     * {@link #FLOOR_DEPTH} behind the glass appears, and it is drawn at the back panel so reflections stand on it.
     */
    private static void extractFloor(Level level, BlockPos pos, State state, double cameraFront) {
        double eye = state.cameraY;
        if (eye <= STANDING_BOTTOM) {
            return; // camera at or below the floor: no floor to see
        }
        BlockPos below = pos.below().relative(state.facing);
        int color = level.getBlockState(below).getMapColor(level, below).col;
        if (color == 0) {
            return;
        }
        state.floorColor = ARGB.opaque(BuiltinCosmeticRenderers.shade(color, FLOOR_SHADE));
        state.floorTop = (float) Math.min(STANDING_TOP, eye * FLOOR_DEPTH / (cameraFront + FLOOR_DEPTH));
        state.floorScale = (float) ((cameraFront + FLOOR_BEHIND_GLASS) / cameraFront);
    }

    /**
     * Quick check whether any of a reflection can show through the glass: its middle line from feet to head, widened
     * by the model's reach. What actually shows is cut out when it is drawn.
     */
    private static boolean seenThroughGlass(Vec3 camera, double cameraFront, double rx, double ry, double rz, double height,
                                            double front, BlockPos pos, Direction facing, Shape shape) {
        // both ends lie {@code front} behind the glass, so the view rays cross it at the same fraction of their length
        double u = MirrorMath.crossing(cameraFront, -front);
        if (u <= 0 || u > 1) {
            return false;
        }
        double reach = MODEL_HALF_WIDTH * u;
        double ix = camera.x + u * (rx - camera.x) - pos.getX();
        double iz = camera.z + u * (rz - camera.z) - pos.getZ();
        double across = facing.getAxis() == Direction.Axis.Z ? ix : iz;
        double bottom = camera.y + u * (ry - camera.y) - pos.getY();
        double top = camera.y + u * (ry + height - camera.y) - pos.getY();
        // the glass's own left/right are for a north-facing mirror; other facings turn it, so take the wider span
        double left = Math.min(shape.left(), 1.0 - shape.right());
        return across - reach < 1 - left && across + reach > left
                && bottom - reach < shape.top() && top + reach > shape.bottom();
    }

    /** A reflection has no shadow, name tag, fire, leash or glowing outline. */
    private static void hideExtras(EntityRenderState state) {
        state.shadowPieces.clear();
        state.shadowRadius = 0;
        state.nameTag = null;
        state.scoreText = null;
        state.displayFireAnimation = false;
        state.leashStates = null;
        state.outlineColor = EntityRenderState.NO_OUTLINE;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.count > 0) {
            windowPlanes(state, pose);
        }
        SubmitNodeCollector clipped = state.count > 0 ? new ClippingSubmitCollector(collector, state.window) : collector;
        for (int i = 0; i < state.count; i++) {
            Reflection reflection = state.reflections.get(i);
            pose.pushPose();
            // applied to the model last to first: slide toward the camera, then squash into the cavity
            pose.translate(state.middleX, 0.0, state.middleZ);
            boolean alongZ = state.facing.getAxis() == Direction.Axis.Z;
            pose.scale(alongZ ? 1.0F : reflection.squash, 1.0F, alongZ ? reflection.squash : 1.0F);
            pose.translate(-state.middleX, 0.0, -state.middleZ);
            pose.translate(state.cameraX, state.cameraY, state.cameraZ);
            pose.scale(reflection.scale, reflection.scale, reflection.scale);
            pose.translate(-state.cameraX, -state.cameraY, -state.cameraZ);
            entities.submit(reflection.state, camera, reflection.x, reflection.y, reflection.z, pose, clipped);
            pose.popPose();
        }
        if (state.floorColor != 0 && state.floorTop > STANDING_BOTTOM) {
            pose.pushPose();
            pose.translate(state.cameraX, state.cameraY, state.cameraZ);
            pose.scale(state.floorScale, state.floorScale, state.floorScale);
            pose.translate(-state.cameraX, -state.cameraY, -state.cameraZ);
            facing(pose, state.facing);
            pose.translate(0.0F, (float) STANDING_BOTTOM, 0.0F);
            pose.scale(1.0F, (state.floorTop - (float) STANDING_BOTTOM) * 16.0F, 1.0F);
            collector.submitModelPart(floor, pose, FLOOR, state.lightCoords, OverlayTexture.NO_OVERLAY, null, state.floorColor);
            pose.popPose();
        }
        pose.pushPose();
        facing(pose, state.facing);
        collector.order(GLASS_ORDER).submitModelPart(state.shape.glass(), pose, GLASS, state.lightCoords, OverlayTexture.NO_OVERLAY, null, GLASS_COLOR);
        pose.popPose();
    }

    /**
     * The four planes through the eye and the edges of the glass, inside facing in, in the space the reflections'
     * vertices end up in (the block entity's pose).
     */
    private void windowPlanes(State state, PoseStack pose) {
        pose.last().pose().transformPosition((float) state.cameraX, (float) state.cameraY, (float) state.cameraZ, eye);
        pose.pushPose();
        facing(pose, state.facing);
        Matrix4f glass = pose.last().pose();
        float z = (float) GLASS_INSET;
        float left = (float) state.shape.left();
        float right = (float) state.shape.right();
        float bottom = (float) state.shape.bottom();
        float top = (float) state.shape.top();
        glass.transformPosition(left, bottom, z, corners[0]);
        glass.transformPosition(right, bottom, z, corners[1]);
        glass.transformPosition(right, top, z, corners[2]);
        glass.transformPosition(left, top, z, corners[3]);
        pose.popPose();
        center.set(corners[0]).add(corners[2]).mul(0.5F).sub(eye);
        for (int i = 0; i < 4; i++) {
            corners[i].sub(eye, edgeA);
            corners[(i + 1) % 4].sub(eye, edgeB);
            edgeA.cross(edgeB, normal);
            if (normal.dot(center) < 0) {
                normal.negate();
            }
            state.window[i * 4] = normal.x;
            state.window[i * 4 + 1] = normal.y;
            state.window[i * 4 + 2] = normal.z;
            state.window[i * 4 + 3] = -normal.dot(eye);
        }
    }

    /** Turns the north-facing glass and floor parts to the mirror's facing. */
    private static void facing(PoseStack pose, Direction facing) {
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.rotateDegrees(Axis.YP, 180.0F - facing.toYRot());
        pose.translate(-0.5F, 0.0F, -0.5F);
    }
}
