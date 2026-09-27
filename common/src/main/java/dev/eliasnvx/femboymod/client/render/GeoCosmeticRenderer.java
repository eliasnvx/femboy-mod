package dev.eliasnvx.femboymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderContext;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderer;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer {@code femboymod:geo}: draws a Blockbench/GeckoLib model for a cosmetic item (SPEC §4.5).
 *
 * <p>Convention for an item {@code <ns>:<path>} (used automatically when the model file exists, so it
 * overrides the built-in code model; works for addon items too). GeckoLib 4 (Minecraft 1.21.1) paths:
 * <ul>
 *     <li>model: {@code assets/<ns>/geo/cosmetic/<path>.geo.json}</li>
 *     <li>texture: {@code assets/<ns>/textures/cosmetic/<path>.png} (drawn as-is while undyed)</li>
 *     <li>optional dyeable texture: {@code assets/<ns>/textures/cosmetic/<path>_dyeable.png} (light/grayscale,
 *     multiplied by the colorway's main color when the item is dyed)</li>
 *     <li>optional animation: {@code assets/<ns>/animations/cosmetic/<path>.animation.json} with a
 *     looping {@code idle} animation</li>
 * </ul>
 * Top-level bones named like GeckoLib armor ({@code armorHead}, {@code armorBody}, {@code armorLeftArm},
 * {@code armorRightArm}, {@code armorLeftLeg}, {@code armorRightLeg}) follow the player's body parts.
 */
public final class GeoCosmeticRenderer implements CosmeticRenderer {

    private static final String IDLE = "idle";
    private static final RawAnimation IDLE_LOOP = RawAnimation.begin().thenLoop(IDLE);
    private static final int TRANSITION_TICKS = 5;
    private static final String MODEL_DIR = "geo/cosmetic/";
    private static final String MODEL_SUFFIX = ".geo.json";
    private static final String ANIMATION_DIR = "animations/cosmetic/";
    private static final String ANIMATION_SUFFIX = ".animation.json";

    private final ResourceLocation model;
    private final ResourceLocation animation;
    private final ResourceLocation texture;
    private final ResourceLocation dyeableTexture;
    private final Animatable animatable = new Animatable();
    private final Renderer renderer;
    /** Per-draw values, reused (no per-frame allocation on our side). */
    private final Related related = new Related();

    public GeoCosmeticRenderer(ResourceLocation itemId) {
        this.model = modelId(itemId);
        this.animation = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), ANIMATION_DIR + itemId.getPath() + ANIMATION_SUFFIX);
        String base = "textures/cosmetic/" + itemId.getPath();
        this.texture = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), base + ".png");
        this.dyeableTexture = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), base + "_dyeable.png");
        this.renderer = new Renderer(new Model());
        FemboyMod.LOGGER.info("Using GeckoLib model {} for cosmetic {}", model, itemId);
    }

    /** Item id -> geo model id; filled once per item so the per-frame check doesn't allocate. */
    private static final Map<ResourceLocation, ResourceLocation> MODEL_IDS = new HashMap<>();

    private static ResourceLocation modelId(ResourceLocation itemId) {
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), MODEL_DIR + itemId.getPath() + MODEL_SUFFIX);
    }

    /**
     * Whether a geo model exists for the item (checked each frame; resource reloads can add/remove it).
     * Looks at the cache map directly: {@code getBakedModel} throws for a miss.
     */
    public static boolean hasModel(ResourceLocation itemId) {
        return GeckoLibCache.getBakedModels().containsKey(MODEL_IDS.computeIfAbsent(itemId, GeoCosmeticRenderer::modelId));
    }

    @Override
    public void submit(CosmeticRenderContext ctx) {
        Colorway colorway = ctx.colorway();
        boolean tinted = colorway != null && Minecraft.getInstance().getResourceManager().getResource(dyeableTexture).isPresent();
        related.entityId = ctx.entity().getId();
        related.color = tinted ? ARGB32.opaque(colorway.stripeColor(0, ColorwayClock.ticks())) : 0xFFFFFFFF;
        related.texture = tinted ? dyeableTexture : texture;
        related.overlay = ctx.overlay();
        related.parent = ctx.parentModel();
        MultiBufferSource buffers = ctx.bufferSource();
        RenderType type = RenderType.entityCutoutNoCull(related.texture);
        VertexConsumer buffer = buffers.getBuffer(type);
        // A non-null buffer keeps GeckoLib on our buffer source (it falls back to the level's otherwise).
        renderer.render(ctx.poseStack(), animatable, buffers, type, buffer, ctx.light(), ctx.partialTick());
        related.parent = null;
    }

    /** Per-draw data read by the model and renderer callbacks. */
    private static final class Related {
        int entityId;
        int color = 0xFFFFFFFF;
        int overlay;
        ResourceLocation texture;
        @Nullable HumanoidModel<?> parent;
        /** Last color handed to GeckoLib, reused while unchanged. */
        Color cachedColor = Color.WHITE;
    }

    private final class Animatable implements SingletonGeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, IDLE, TRANSITION_TICKS, test ->
                    GeckoLibCache.getBakedAnimations().containsKey(animation)
                            ? test.setAndContinue(IDLE_LOOP)
                            : PlayState.STOP));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object object) {
            return RenderUtil.getCurrentTick();
        }
    }

    private final class Model extends GeoModel<Animatable> {
        @Override
        public ResourceLocation getModelResource(Animatable animatable) {
            return model;
        }

        @Override
        public ResourceLocation getTextureResource(Animatable animatable) {
            return related.texture;
        }

        @Override
        public ResourceLocation getAnimationResource(Animatable animatable) {
            return animation;
        }
    }

    private final class Renderer extends GeoObjectRenderer<Animatable> {
        /** Same transform GeckoLib applies to armor, from player model space. */
        private static final float ARMOR_Y_OFFSET = 24.0F / 16.0F;
        /** Rest pivots of the vanilla humanoid arms and legs (GeoArmorRenderer#applyBaseTransformations). */
        private static final float ARM_X = 5.0F;
        private static final float ARM_Y = 2.0F;
        private static final float LEG_X = 2.0F;
        private static final float LEG_Y = 12.0F;

        Renderer(GeoModel<Animatable> model) {
            super(model);
        }

        /** Replaces the object renderer's block-centered offset with the armor transform and body following. */
        @Override
        public void preRender(PoseStack poseStack, Animatable animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                              @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                              int packedOverlay, int colour) {
            HumanoidModel<?> parent = related.parent;
            if (parent != null) {
                follow(model, "armorHead", parent.head, 0.0F, 0.0F);
                follow(model, "armorBody", parent.body, 0.0F, 0.0F);
                follow(model, "armorRightArm", parent.rightArm, ARM_X, ARM_Y);
                follow(model, "armorLeftArm", parent.leftArm, -ARM_X, ARM_Y);
                follow(model, "armorRightLeg", parent.rightLeg, LEG_X, LEG_Y);
                follow(model, "armorLeftLeg", parent.leftLeg, -LEG_X, LEG_Y);
            }
            poseStack.translate(0.0F, ARMOR_Y_OFFSET, 0.0F);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
        }

        /** Copies the player's (already posed) body part transform onto an armor-named bone. */
        private void follow(BakedGeoModel model, String boneName, ModelPart part, float offsetX, float offsetY) {
            GeoBone bone = model.getBone(boneName).orElse(null);
            if (bone != null) {
                RenderUtil.matchModelPartRot(part, bone);
                bone.updatePosition(part.x + offsetX, offsetY - part.y, part.z);
            }
        }

        @Override
        public long getInstanceId(Animatable animatable) {
            return related.entityId; // per-player animation state
        }

        @Override
        public Color getRenderColor(Animatable animatable, float partialTick, int packedLight) {
            if (related.cachedColor.argbInt() != related.color) {
                related.cachedColor = new Color(related.color);
            }
            return related.cachedColor;
        }

        @Override
        public int getPackedOverlay(Animatable animatable, float u, float partialTick) {
            return related.overlay;
        }
    }
}
