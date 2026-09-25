package dev.eliasnvx.femboymod.client.render;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderContext;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderer;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.Map;
import java.util.function.Function;

/** Placeholder renderers for the SPEC §5.1 items. Default colors match the item icons. */
public final class BuiltinCosmeticRenderers {

    private static final Identifier WHITE = texture("white");
    private static final Identifier FISHNET = texture("fishnet");
    private static final Identifier GOLD = texture("gold");

    private static final int PINK = 0xF291BE;
    private static final int LAVENDER = 0xC8A2E8;
    private static final int SKIRT_DARK = 0x3A3A48;
    private static final int SOCK_PINK = 0xF5A9B8;
    private static final int SOCK_WHITE = 0xFFFFFF;
    private static final int FISHNET_BLACK = 0x2A2A2A;
    private static final int CHOKER_BLACK = 0x2A2A33;
    private static final int UNTINTED = 0xFFFFFF;

    /** Hair clips are not dyeable; each shape has its own color (same as its icon). */
    private static final Map<String, Integer> HAIR_CLIP_COLORS = Map.of(
            "hair_clip_heart", 0xE65082, "hair_clip_star", 0xFAD746, "hair_clip_bow", 0xF078AA,
            "hair_clip_flower", 0xF5AAC8, "hair_clip_moon", 0xEBE18C, "hair_clip_cherry", 0xD7283C,
            "hair_clip_bunny", 0xFAFAFA, "hair_clip_fish", 0x6EBEF0, "hair_clip_lightning", 0xFADC3C,
            "hair_clip_butterfly", 0xAA82F0);
    private static final int HAIR_CLIP_DEFAULT = 0xE65082;

    private BuiltinCosmeticRenderers() {
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosmetic/" + name + ".png");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }

    public static void register(ApiRegistry<CosmeticRenderer.Factory> registry) {
        registry.register(id("cat_ears"), models -> solid(new CosmeticModels.CatEarsModel(models.bakeLayer(CosmeticModels.CAT_EARS)), WHITE, PINK));
        registry.register(id("tail"), models -> solid(new CosmeticModels.TailModel(models.bakeLayer(CosmeticModels.TAIL)), WHITE, PINK));
        registry.register(id("oversized_hoodie"), models -> solid(new CosmeticModels.HoodieModel(models.bakeLayer(CosmeticModels.HOODIE)), WHITE, LAVENDER));
        registry.register(id("pleated_skirt"), models -> solid(new CosmeticModels.SkirtModel(models.bakeLayer(CosmeticModels.SKIRT)), WHITE, SKIRT_DARK));
        registry.register(id("programming_socks"), models -> new BandedRenderer(
                band -> new CosmeticModels.LegwearModel(models.bakeLayer(CosmeticModels.SOCKS), band), WHITE, SOCK_PINK, SOCK_WHITE));
        registry.register(id("fishnet_tights"), models -> solid(new CosmeticModels.LegwearModel(models.bakeLayer(CosmeticModels.FISHNET), -1), FISHNET, FISHNET_BLACK));
        registry.register(id("uwu_choker"), models -> {
            ModelPart root = models.bakeLayer(CosmeticModels.CHOKER);
            CosmeticRenderer band = solid(new CosmeticModels.ChokerModel(root, false), WHITE, CHOKER_BLACK);
            Model<AvatarRenderState> bell = new CosmeticModels.ChokerModel(models.bakeLayer(CosmeticModels.CHOKER), true);
            RenderType bellType = RenderTypes.entityCutout(GOLD);
            return ctx -> {
                band.submit(ctx);
                submit(ctx, bell, bellType, UNTINTED);
            };
        });
        registry.register(id("hair_clip"), models -> {
            Model<AvatarRenderState> model = new CosmeticModels.HairClipModel(models.bakeLayer(CosmeticModels.HAIR_CLIP));
            RenderType type = RenderTypes.entityCutout(WHITE);
            return ctx -> submit(ctx, model, type, HAIR_CLIP_COLORS.getOrDefault(
                    BuiltInRegistries.ITEM.getKey(ctx.stack().getItem()).getPath(), HAIR_CLIP_DEFAULT));
        });
    }

    /** One model tinted with the first stripe of the colorway (or the default color). */
    static CosmeticRenderer solid(Model<AvatarRenderState> model, Identifier texture, int defaultColor) {
        RenderType type = RenderTypes.entityCutout(texture);
        return ctx -> {
            Colorway colorway = ctx.colorway();
            submit(ctx, model, type, colorway == null ? defaultColor : colorway.stripeColor(0));
        };
    }

    static void submit(CosmeticRenderContext ctx, Model<AvatarRenderState> model, RenderType type, int rgb) {
        ctx.collector().submitModel(model, ctx.state(), ctx.poseStack(), type, ctx.light(), ctx.overlay(),
                ARGB.opaque(rgb), null, ctx.state().outlineColor);
    }

    /**
     * Horizontal stripes: one model instance per band. Patterns with up to 3 stripes repeat
     * (classic striped programming socks); longer ones (flags) are stretched top to bottom.
     */
    static final class BandedRenderer implements CosmeticRenderer {
        private static final int REPEAT_MAX_STRIPES = 3;
        private final Model<AvatarRenderState> all;
        private final Model<AvatarRenderState>[] bands;
        private final RenderType type;
        private final int defaultBase;
        private final int defaultSecondary;

        @SuppressWarnings("unchecked")
        BandedRenderer(Function<Integer, Model<AvatarRenderState>> factory, Identifier texture, int defaultBase, int defaultSecondary) {
            this.all = factory.apply(-1);
            this.bands = new Model[CosmeticModels.SOCK_BANDS];
            for (int i = 0; i < bands.length; i++) {
                bands[i] = factory.apply(i);
            }
            this.type = RenderTypes.entityCutout(texture);
            this.defaultBase = defaultBase;
            this.defaultSecondary = defaultSecondary;
        }

        @Override
        public void submit(CosmeticRenderContext ctx) {
            Colorway colorway = ctx.colorway();
            int stripes = colorway == null ? 2 : colorway.stripeCount();
            if (stripes == 1) {
                BuiltinCosmeticRenderers.submit(ctx, all, type, colorway.stripeColor(0));
                return;
            }
            for (int band = 0; band < bands.length; band++) {
                int stripe = stripes <= REPEAT_MAX_STRIPES ? band % stripes : band * stripes / bands.length;
                int rgb = colorway == null ? (stripe == 0 ? defaultBase : defaultSecondary) : colorway.stripeColor(stripe);
                BuiltinCosmeticRenderers.submit(ctx, bands[band], type, rgb);
            }
        }
    }
}
