package dev.eliasnvx.femboymod.client.render;

import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoArmorRenderer.ArmorSegment;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.GeckoLibUtil;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderContext;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderer;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Renderer {@code femboymod:geo}: draws a Blockbench/GeckoLib model for a cosmetic item (SPEC §4.5).
 *
 * <p>Convention for an item {@code <ns>:<path>} (used automatically when the model file exists, so it
 * overrides the built-in code model; works for addon items too):
 * <ul>
 *     <li>model: {@code assets/<ns>/geckolib/models/cosmetic/<path>.geo.json}</li>
 *     <li>texture: {@code assets/<ns>/textures/cosmetic/<path>.png} (drawn as-is while undyed)</li>
 *     <li>optional dyeable texture: {@code assets/<ns>/textures/cosmetic/<path>_dyeable.png} (light/grayscale,
 *     multiplied by the colorway's main color when the item is dyed)</li>
 *     <li>optional animation: {@code assets/<ns>/geckolib/animations/cosmetic/<path>.animation.json} with a
 *     looping {@code idle} animation</li>
 * </ul>
 * Top-level bones named like GeckoLib armor ({@code armorHead}, {@code armorBody}, {@code armorLeftArm},
 * {@code armorRightArm}, {@code armorLeftLeg}, {@code armorRightLeg}) follow the player's body parts.
 */
public final class GeoCosmeticRenderer implements CosmeticRenderer {

    private static final String IDLE = "idle";
    private static final RawAnimation IDLE_LOOP = RawAnimation.begin().thenLoop(IDLE);
    private static final int TRANSITION_TICKS = 5;
    private static final Map<ArmorSegment, String> BONES = new EnumMap<>(Map.of(
            ArmorSegment.HEAD, "armorHead", ArmorSegment.CHEST, "armorBody",
            ArmorSegment.LEFT_ARM, "armorLeftArm", ArmorSegment.RIGHT_ARM, "armorRightArm",
            ArmorSegment.LEFT_LEG, "armorLeftLeg", ArmorSegment.RIGHT_LEG, "armorRightLeg"));

    private final Identifier model;
    private final Identifier animation;
    private final Identifier texture;
    private final Identifier dyeableTexture;
    private final Animatable animatable = new Animatable();
    private final Renderer renderer;
    /** Reused per submission (no per-frame allocation). */
    private final Related related = new Related();
    private final Vector3f scratch = new Vector3f();
    private final RenderPassInfo.BoneUpdater<GeoRenderState> followBody = this::followBody;

    public GeoCosmeticRenderer(Identifier itemId) {
        String base = "cosmetic/" + itemId.getPath();
        this.model = Identifier.fromNamespaceAndPath(itemId.getNamespace(), base);
        this.animation = model;
        this.texture = Identifier.fromNamespaceAndPath(itemId.getNamespace(), "textures/" + base + ".png");
        this.dyeableTexture = Identifier.fromNamespaceAndPath(itemId.getNamespace(), "textures/" + base + "_dyeable.png");
        this.renderer = new Renderer(new Model());
        FemboyMod.LOGGER.info("Using GeckoLib model {} for cosmetic {}", model, itemId);
    }

    /** Item id -> geo model id; filled once per item so the per-frame check doesn't allocate. */
    private static final Map<Identifier, Identifier> MODEL_IDS = new HashMap<>();

    /**
     * Whether a geo model exists for the item (checked each frame; resource reloads can add/remove it).
     * Looks at the cache map directly: {@code getModel} logs an error for every miss.
     */
    public static boolean hasModel(Identifier itemId) {
        Identifier id = MODEL_IDS.computeIfAbsent(itemId,
                item -> Identifier.fromNamespaceAndPath(item.getNamespace(), "cosmetic/" + item.getPath()));
        return GeckoLibResources.getBakedModels().cache().containsKey(id);
    }

    @Override
    public void submit(CosmeticRenderContext ctx) {
        Colorway colorway = ctx.colorway();
        boolean tinted = colorway != null && Minecraft.getInstance().getResourceManager().getResource(dyeableTexture).isPresent();
        related.entityId = ctx.state().id;
        related.color = colorway == null ? 0xFFFFFFFF : ARGB.opaque(colorway.stripeColor(0, ColorwayClock.ticks()));
        related.texture = tinted ? dyeableTexture : texture;
        if (!tinted) {
            related.color = 0xFFFFFFFF;
        }
        related.overlay = ctx.overlay();
        related.parent = ctx.parentModel();
        Minecraft minecraft = Minecraft.getInstance();
        renderer.performRenderPass(animatable, related, ctx.poseStack(), ctx.collector(),
                minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState, ctx.light(),
                minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false), followBody);
    }

    /** Copies the player's (already posed) body part transforms onto the armor-named bones. */
    private void followBody(RenderPassInfo<GeoRenderState> info, com.geckolib.renderer.base.BoneSnapshots snapshots) {
        PlayerModel parent = related.parent;
        if (parent == null) {
            return;
        }
        for (Map.Entry<ArmorSegment, String> entry : BONES.entrySet()) {
            snapshots.get(entry.getValue()).ifPresent(snapshot -> {
                ModelPart part = entry.getKey().modelPartGetter.apply(parent);
                Vector3f pos = entry.getKey().modelPartMatcher.apply(scratch.set(part.x, part.y, part.z));
                snapshot.setRotX(-part.xRot).setRotY(-part.yRot).setRotZ(part.zRot)
                        .setTranslateX(pos.x).setTranslateY(pos.y).setTranslateZ(pos.z);
            });
        }
    }

    private static final class Related {
        int entityId;
        int color;
        int overlay;
        Identifier texture;
        @Nullable PlayerModel parent;
    }

    private final class Animatable implements SingletonGeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<Animatable>(IDLE, TRANSITION_TICKS, test ->
                    GeckoLibResources.getBakedAnimations().cache().containsKey(animation)
                            ? test.setAndContinue(IDLE_LOOP)
                            : PlayState.STOP));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }
    }

    private final class Model extends GeoModel<Animatable> {
        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return model;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return related.texture;
        }

        @Override
        public Identifier getAnimationResource(Animatable animatable) {
            return animation;
        }
    }

    private static final class Renderer extends GeoObjectRenderer<Animatable, Related, GeoRenderState> {
        /** Same transform GeckoLib applies to armor, from player model space. */
        private static final float ARMOR_Y_OFFSET = 24.0F / 16.0F;

        Renderer(GeoModel<Animatable> model) {
            super(model);
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<GeoRenderState> info) {
            info.poseStack().translate(0.0F, ARMOR_Y_OFFSET, 0.0F);
            info.poseStack().scale(-1.0F, -1.0F, 1.0F);
        }

        @Override
        public long getInstanceId(Animatable animatable, @Nullable Related related) {
            return related == null ? 0 : related.entityId; // per-player animation state
        }

        @Override
        public int getRenderColor(Animatable animatable, @Nullable Related related, float partialTick) {
            return related == null ? 0xFFFFFFFF : related.color;
        }

        @Override
        public int getPackedOverlay(Animatable animatable, @Nullable Related related, float u, float partialTick) {
            return related == null ? super.getPackedOverlay(animatable, null, u, partialTick) : related.overlay;
        }
    }
}
