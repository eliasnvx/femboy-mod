package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.block.MirrorBlock;
import dev.eliasnvx.femboymod.block.MirrorBlockEntity;
import dev.eliasnvx.femboymod.block.VanityTableBlock;
import dev.eliasnvx.femboymod.registry.FemboyTags;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Live reflections of nearby players in a {@link MirrorBlock}. Each reflection is the player mirrored in the glass
 * (position and facing), slid along the view rays into the mirror (same picture on screen,
 * {@link MirrorMath#slideScale}) and squashed into its few pixels of depth ({@link MirrorMath#squash}). Players and
 * pets ({@code #femboymod:mirror_reflected}, tamed) are reflected, not the room. Reflections are cut to what is seen
 * through the glass ({@link ClippingBufferSource}), so nothing spills over the frame. A translucent pane with a sheen
 * goes on top. Models are not mirrored left to right (that would turn them inside out with backface culling).
 * <p>
 * 1.20.1 renders immediately: the entity's own renderer draws it under the mirror's pose, with its rotations swapped
 * for the mirrored ones for the call and restored right after. Calling the renderer directly (not the dispatcher)
 * leaves out the shadow, fire and hitbox; name tags are skipped by PlayerRendererNameTagMixin (players) and
 * LivingEntityRendererNameTagMixin (pets) while {@link #drawingReflection()} is set, and {@link ClippingBufferSource}
 * drops leashes.
 */
public final class MirrorRenderer implements BlockEntityRenderer<MirrorBlockEntity> {

    public static final ModelLayerLocation GLASS_LAYER = new ModelLayerLocation(
            new ResourceLocation(FemboyMod.MOD_ID, "mirror_glass"), "main");
    private static final RenderType GLASS = RenderType.entityTranslucentCull(
            new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/mirror_glass.png"));
    private static final RenderType FLOOR = RenderType.entityCutout(
            new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/mirror_floor.png"));

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
    /** Height above the mirror's foot that the range is measured from (the middle of the glass). */
    private static final double RANGE_CENTER_Y = 1.0;
    /** Anyone closer than this to the glass is not reflected (the reflection would be inside the frame). */
    private static final double MIN_FRONT = 0.2;
    private static final int MAX_REFLECTIONS = 6;
    private static final int GLASS_COLOR = 0xFFFFFFFF;
    private static final Predicate<LivingEntity> REFLECTED_PETS = entity ->
            !(entity instanceof Player) && entity.getType().is(FemboyTags.MIRROR_REFLECTED)
                    && (!(entity instanceof TamableAnimal pet) || pet.isTame());
    /** The reflected floor reaches this far behind the glass (a room, not the horizon). */
    private static final double FLOOR_DEPTH = 6.0;
    /** The reflection of the floor is a little darker than the floor. */
    private static final float FLOOR_SHADE = -0.15F;
    private static final float MODEL_UNITS = 16.0F;

    /** True while a reflection is drawn. Render thread only. */
    private static boolean drawingReflection;

    private final BlockEntityRenderDispatcher blockEntities;
    private final EntityRenderDispatcher entities;
    private final ModelPart floor;
    /** The standing mirrors' glass (2 px frame, two blocks tall) and the vanity table's smaller one. */
    private final Shape standing;
    private final Shape vanity;
    /** Who may be reflected this frame; reused (render thread). */
    private final List<LivingEntity> candidates = new ArrayList<>();
    /** Reflections are drawn through it, cut to the glass. Render thread only, like the scratch vectors below. */
    private final ClippingBufferSource clipping = new ClippingBufferSource();
    /** The planes through the eye and the glass's edges, see {@link #windowPlanes}. */
    private final float[] window = new float[16];
    private final Vector3f eye = new Vector3f();
    private final Vector3f[] corners = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    private final Vector3f center = new Vector3f();
    private final Vector3f edgeA = new Vector3f();
    private final Vector3f edgeB = new Vector3f();
    private final Vector3f normal = new Vector3f();

    public MirrorRenderer(BlockEntityRendererProvider.Context context) {
        this.blockEntities = context.getBlockEntityRenderDispatcher();
        this.entities = context.getEntityRenderer();
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

    /** Whether a reflection is being drawn right now (no name tags then). Render thread. */
    public static boolean drawingReflection() {
        return drawingReflection;
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
        return (float) (blocks * MODEL_UNITS);
    }

    @Override
    public boolean shouldRenderOffScreen(MirrorBlockEntity mirror) {
        return true; // the block entity is in the lower half, the glass goes up into the upper one
    }

    @Override
    public void render(MirrorBlockEntity mirror, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = mirror.getBlockState().getValue(MirrorBlock.FACING);
        Shape shape = mirror.getBlockState().getBlock() instanceof VanityTableBlock ? vanity : standing;
        Level level = mirror.getLevel();
        Camera camera = blockEntities.camera;
        if (level != null && camera != null) {
            drawReflections(mirror, level, camera.getPosition(), facing, shape, partialTick, pose, buffers, light);
        }
        pose.pushPose();
        facing(pose, facing);
        ModelColors.render(shape.glass(), pose, buffers.getBuffer(GLASS), light, OverlayTexture.NO_OVERLAY, GLASS_COLOR);
        pose.popPose();
    }

    private void drawReflections(MirrorBlockEntity mirror, Level level, Vec3 camera, Direction facing, Shape shape, float partialTick,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        BlockPos pos = mirror.getBlockPos();
        double cameraX = camera.x - pos.getX();
        double cameraY = camera.y - pos.getY();
        double cameraZ = camera.z - pos.getZ();
        double nx = facing.getStepX();
        double nz = facing.getStepZ();
        double qx = pos.getX() + 0.5 + nx * (0.5 - GLASS_INSET);
        double qz = pos.getZ() + 0.5 + nz * (0.5 - GLASS_INSET);
        // the middle of the reflections' layer in the cavity (block-local), squashing happens about it
        double middleX = qx - nx * REFLECTION_MIDDLE - pos.getX();
        double middleZ = qz - nz * REFLECTION_MIDDLE - pos.getZ();
        double cameraFront = MirrorMath.front(camera.x, camera.z, qx, qz, nx, nz);
        if (cameraFront <= 0) {
            return; // looking at the back of the mirror
        }
        windowPlanes(pose, facing, shape, cameraX, cameraY, cameraZ);
        // players, then pets (#femboymod:mirror_reflected; tameable ones once tamed)
        candidates.clear();
        candidates.addAll(level.players());
        level.getEntities(EntityTypeTest.forClass(LivingEntity.class), new AABB(pos).inflate(RANGE), REFLECTED_PETS, candidates);
        int count = 0;
        for (LivingEntity reflected : candidates) {
            if (count == MAX_REFLECTIONS) {
                break;
            }
            if (reflected.isInvisible() || reflected.isSpectator()) {
                continue;
            }
            double x = Mth.lerp(partialTick, reflected.xo, reflected.getX());
            double y = Mth.lerp(partialTick, reflected.yo, reflected.getY());
            double z = Mth.lerp(partialTick, reflected.zo, reflected.getZ());
            double front = MirrorMath.front(x, z, qx, qz, nx, nz);
            double dy = y - (pos.getY() + RANGE_CENTER_Y);
            if (front < MIN_FRONT || (x - qx) * (x - qx) + dy * dy + (z - qz) * (z - qz) > RANGE * RANGE) {
                continue;
            }
            double rx = x - 2 * front * nx;
            double rz = z - 2 * front * nz;
            if (!seenThroughGlass(camera, cameraFront, rx, y, rz, reflected.getBbHeight(), front, pos, facing, shape)) {
                continue;
            }
            float scale = MirrorMath.slideScale(cameraFront, front, REFLECTION_MIDDLE);
            // posed models reach past their bounding box (its diagonal for a turned one)
            double thickness = Math.max(reflected.getBbWidth() * Math.sqrt(2), MODEL_DEPTH) * scale;
            float squash = MirrorMath.squash(thickness, REFLECTION_FAR - REFLECTION_NEAR);
            pose.pushPose();
            // applied to the model last to first: slide toward the camera, then squash into the cavity
            pose.translate(middleX, 0.0, middleZ);
            boolean alongZ = facing.getAxis() == Direction.Axis.Z;
            pose.scale(alongZ ? 1.0F : squash, 1.0F, alongZ ? squash : 1.0F);
            pose.translate(-middleX, 0.0, -middleZ);
            pose.translate(cameraX, cameraY, cameraZ);
            pose.scale(scale, scale, scale);
            pose.translate(-cameraX, -cameraY, -cameraZ);
            drawReflection(reflected, rx - pos.getX(), y - pos.getY(), rz - pos.getZ(), nx, nz, partialTick, pose, buffers, light);
            pose.popPose();
            count++;
        }
        if (shape.floor()) {
            drawFloor(level, pos, facing, cameraY, cameraX, cameraZ, cameraFront, pose, buffers, light);
        }
    }

    /**
     * Draws {@code entity} at the block-local position with its body and head turned as in the mirror. The renderer
     * reads the rotations from the entity, so they are swapped for the call and restored right after.
     */
    private void drawReflection(LivingEntity entity, double x, double y, double z, double nx, double nz, float partialTick,
                                PoseStack pose, MultiBufferSource buffers, int light) {
        EntityRenderer<? super LivingEntity> renderer = entities.getRenderer(entity);
        float bodyRotO = entity.yBodyRotO;
        float bodyRot = entity.yBodyRot;
        float headRotO = entity.yHeadRotO;
        float headRot = entity.yHeadRot;
        float body = MirrorMath.reflectYaw(Mth.rotLerp(partialTick, bodyRotO, bodyRot), nx, nz);
        // the head's turn relative to the body flips too; kept within half a turn of the body
        float head = body + Mth.wrapDegrees(MirrorMath.reflectYaw(Mth.rotLerp(partialTick, headRotO, headRot), nx, nz) - body);
        Vec3 offset = renderer.getRenderOffset(entity, partialTick);
        pose.pushPose();
        pose.translate(x + offset.x, y + offset.y, z + offset.z);
        entity.yBodyRotO = body;
        entity.yBodyRot = body;
        entity.yHeadRotO = head;
        entity.yHeadRot = head;
        drawingReflection = true;
        try {
            renderer.render(entity, body, partialTick, pose, clipping.begin(buffers, window), light);
        } finally {
            clipping.end();
            drawingReflection = false;
            entity.yBodyRotO = bodyRotO;
            entity.yBodyRot = bodyRot;
            entity.yHeadRotO = headRotO;
            entity.yHeadRot = headRot;
            pose.popPose();
        }
    }

    /**
     * The floor in front of the mirror, reflected: on the glass it fills the bottom up to where floor at
     * {@link #FLOOR_DEPTH} behind the glass appears, and it is drawn at the back panel so reflections stand on it.
     */
    private void drawFloor(Level level, BlockPos pos, Direction facing, double eye, double cameraX, double cameraZ,
                           double cameraFront, PoseStack pose, MultiBufferSource buffers, int light) {
        if (eye <= STANDING_BOTTOM) {
            return; // camera at or below the floor: no floor to see
        }
        BlockPos below = pos.below().relative(facing);
        int color = level.getBlockState(below).getMapColor(level, below).col;
        if (color == 0) {
            return;
        }
        int floorColor = ModelColors.opaque(BuiltinCosmeticRenderers.shade(color, FLOOR_SHADE));
        float floorTop = (float) Math.min(STANDING_TOP, eye * FLOOR_DEPTH / (cameraFront + FLOOR_DEPTH));
        if (floorTop <= STANDING_BOTTOM) {
            return;
        }
        float floorScale = (float) ((cameraFront + FLOOR_BEHIND_GLASS) / cameraFront);
        pose.pushPose();
        pose.translate(cameraX, eye, cameraZ);
        pose.scale(floorScale, floorScale, floorScale);
        pose.translate(-cameraX, -eye, -cameraZ);
        facing(pose, facing);
        pose.translate(0.0F, (float) STANDING_BOTTOM, 0.0F);
        pose.scale(1.0F, (floorTop - (float) STANDING_BOTTOM) * MODEL_UNITS, 1.0F);
        ModelColors.render(floor, pose, buffers.getBuffer(FLOOR), light, OverlayTexture.NO_OVERLAY, floorColor);
        pose.popPose();
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

    /**
     * The four planes through the eye and the edges of the glass, inside facing in, in the space the vertices end up
     * in (the block entity's pose, before any reflection transform).
     */
    private void windowPlanes(PoseStack pose, Direction facing, Shape shape, double cameraX, double cameraY, double cameraZ) {
        pose.last().pose().transformPosition((float) cameraX, (float) cameraY, (float) cameraZ, eye);
        pose.pushPose();
        facing(pose, facing);
        Matrix4f glassPose = pose.last().pose();
        float z = (float) GLASS_INSET;
        float left = (float) shape.left();
        float right = (float) shape.right();
        float bottom = (float) shape.bottom();
        float top = (float) shape.top();
        glassPose.transformPosition(left, bottom, z, corners[0]);
        glassPose.transformPosition(right, bottom, z, corners[1]);
        glassPose.transformPosition(right, top, z, corners[2]);
        glassPose.transformPosition(left, top, z, corners[3]);
        pose.popPose();
        center.set(corners[0]).add(corners[2]).mul(0.5F).sub(eye);
        for (int i = 0; i < 4; i++) {
            corners[i].sub(eye, edgeA);
            corners[(i + 1) % 4].sub(eye, edgeB);
            edgeA.cross(edgeB, normal);
            if (normal.dot(center) < 0) {
                normal.negate();
            }
            window[i * 4] = normal.x;
            window[i * 4 + 1] = normal.y;
            window[i * 4 + 2] = normal.z;
            window[i * 4 + 3] = -normal.dot(eye);
        }
    }

    /** Turns the north-facing glass and floor parts to the mirror's facing. */
    private static void facing(PoseStack pose, Direction facing) {
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        pose.translate(-0.5F, 0.0F, -0.5F);
    }
}
