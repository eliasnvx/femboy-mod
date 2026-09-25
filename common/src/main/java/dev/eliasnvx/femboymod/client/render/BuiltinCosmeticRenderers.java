package dev.eliasnvx.femboymod.client.render;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderContext;
import dev.eliasnvx.femboymod.api.client.CosmeticRenderer;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.registry.ApiRegistry;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import dev.eliasnvx.femboymod.client.render.model.Groups;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Renderers for the SPEC §5.1 items. Default colors match the item icons. */
public final class BuiltinCosmeticRenderers {

    private static final Identifier FUR = texture("fur");
    private static final Identifier KNIT = texture("knit");
    private static final Identifier FABRIC = texture("fabric");
    private static final Identifier FISHNET = texture("fishnet");

    private static final int PINK = 0xF291BE;
    private static final int LAVENDER = 0xC8A2E8;
    private static final int SKIRT_DARK = 0x3A3A48;
    private static final int SOCK_PINK = 0xF5A9B8;
    private static final int SOCK_WHITE = 0xFFFFFF;
    private static final int FISHNET_BLACK = 0x2A2A2A;
    private static final int CHOKER_BLACK = 0x2A2A33;

    private static final int FUR_WHITE = 0xFFF6F8;
    private static final int CORD_WHITE = 0xF4F1EE;
    private static final int BOW_PINK = 0xFF8FB8;
    private static final int DARK = 0x2A2328;
    private static final int GOLD = 0xF2C94C;

    /** Accent = main mixed toward white (>0) or black (<0) by this amount. */
    private static final float LIGHTER = 0.45F;
    private static final float DARKER = -0.22F;

    /** Hair clips are not dyeable; each shape has its own color (same as its icon). */
    private static final Map<String, Integer> HAIR_CLIP_COLORS = Map.of(
            "heart", 0xE65082, "star", 0xFAD746, "bow", 0xF078AA, "flower", 0xF5AAC8, "moon", 0xEBE18C,
            "cherry", 0xD7283C, "bunny", 0xFAFAFA, "fish", 0x6EBEF0, "lightning", 0xFADC3C, "butterfly", 0xAA82F0);

    private BuiltinCosmeticRenderers() {
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosmetic/" + name + ".png");
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, path);
    }

    public static void register(ApiRegistry<CosmeticRenderer.Factory> registry) {
        registry.register(id("cat_ears"), models -> new GroupedRenderer(
                models, CosmeticModels.CAT_EARS, CosmeticModels.CatEarsModel::new, FUR, PINK, LIGHTER, FUR_WHITE));
        registry.register(id("tail"), models -> new GroupedRenderer(
                models, CosmeticModels.TAIL, CosmeticModels.TailModel::new, FUR, PINK, LIGHTER, FUR_WHITE));
        registry.register(id("oversized_hoodie"), models -> new GroupedRenderer(
                models, CosmeticModels.HOODIE, CosmeticModels.HoodieModel::new, KNIT, LAVENDER, DARKER, CORD_WHITE));
        registry.register(id("pleated_skirt"), models -> new GroupedRenderer(
                models, CosmeticModels.SKIRT, CosmeticModels.SkirtModel::new, FABRIC, SKIRT_DARK, DARKER, CORD_WHITE));
        registry.register(id("programming_socks"), models -> new LegwearRenderer(
                models, CosmeticModels.SOCKS, KNIT, SOCK_PINK, SOCK_WHITE, BOW_PINK));
        registry.register(id("fishnet_tights"), models -> new LegwearRenderer(
                models, CosmeticModels.FISHNET, FISHNET, FISHNET_BLACK, FISHNET_BLACK, BOW_PINK));
        registry.register(id("uwu_choker"), models -> new GroupedRenderer(
                models, CosmeticModels.CHOKER, Groups.GroupModelFactory.PLAIN, FABRIC, CHOKER_BLACK, LIGHTER, FUR_WHITE));
        registry.register(id("hair_clip"), models -> {
            Map<String, GroupedRenderer> byShape = new HashMap<>();
            CosmeticModels.HAIR_CLIPS.forEach((shape, layer) -> byShape.put(shape, new GroupedRenderer(
                    models, layer, Groups.GroupModelFactory.PLAIN, FABRIC, HAIR_CLIP_COLORS.get(shape), DARKER * 2, FUR_WHITE)));
            return ctx -> {
                String path = BuiltInRegistries.ITEM.getKey(ctx.stack().getItem()).getPath();
                GroupedRenderer renderer = byShape.get(path.substring(path.lastIndexOf('_') + 1));
                if (renderer != null) {
                    renderer.submit(ctx);
                }
            };
        });
    }

    static void submit(CosmeticRenderContext ctx, Model<AvatarRenderState> model, RenderType type, int rgb) {
        ctx.collector().submitModel(model, ctx.state(), ctx.poseStack(), type, ctx.light(), ctx.overlay(),
                ARGB.opaque(rgb), null, ctx.state().outlineColor);
    }

    /** Mixes a color toward white ({@code amount > 0}) or black ({@code amount < 0}). */
    static int shade(int rgb, float amount) {
        int target = amount >= 0 ? 0xFFFFFF : 0x000000;
        float t = Math.abs(amount);
        int r = (int) (ARGB.red(rgb) + (ARGB.red(target) - ARGB.red(rgb)) * t);
        int g = (int) (ARGB.green(rgb) + (ARGB.green(target) - ARGB.green(rgb)) * t);
        int b = (int) (ARGB.blue(rgb) + (ARGB.blue(target) - ARGB.blue(rgb)) * t);
        return ARGB.color(r, g, b) & 0xFFFFFF;
    }

    /** Fixed colors of the non-tinted groups. */
    static int fixedColor(Groups group, int detail) {
        return switch (group) {
            case DETAIL -> detail;
            case DARK -> DARK;
            case METAL -> GOLD;
            default -> throw new IllegalArgumentException(group.name());
        };
    }

    /** One model instance per color group, each submitted with its color. */
    static final class GroupedRenderer implements CosmeticRenderer {
        private final Map<Groups, Model<AvatarRenderState>> models = new EnumMap<>(Groups.class);
        private final RenderType type;
        private final int defaultMain;
        private final float accentShade;
        private final int detailColor;

        GroupedRenderer(EntityModelSet set, ModelLayerLocation layer, BiFunction<net.minecraft.client.model.geom.ModelPart, Groups, ? extends Model<AvatarRenderState>> factory,
                        Identifier texture, int defaultMain, float accentShade, int detailColor) {
            for (Groups group : Groups.values()) {
                models.put(group, factory.apply(set.bakeLayer(layer), group));
            }
            this.type = RenderTypes.entityCutout(texture);
            this.defaultMain = defaultMain;
            this.accentShade = accentShade;
            this.detailColor = detailColor;
        }

        @Override
        public void submit(CosmeticRenderContext ctx) {
            Colorway colorway = ctx.colorway();
            int main = colorway == null ? defaultMain : colorway.stripeColor(0);
            for (Map.Entry<Groups, Model<AvatarRenderState>> entry : models.entrySet()) {
                Groups group = entry.getKey();
                int rgb = switch (group) {
                    case MAIN -> main;
                    case ACCENT -> shade(main, accentShade);
                    default -> fixedColor(group, detailColor);
                };
                BuiltinCosmeticRenderers.submit(ctx, entry.getValue(), type, rgb);
            }
        }
    }

    /**
     * Socks/tights: stripe bands (≤3 stripes repeat like classic striped socks, flags stretch
     * top to bottom) plus cuff/toe/heel in a darker shade and a bow.
     */
    static final class LegwearRenderer implements CosmeticRenderer {
        private static final int REPEAT_MAX_STRIPES = 3;
        private final Model<AvatarRenderState> allBands;
        private final Model<AvatarRenderState>[] bands;
        private final Model<AvatarRenderState> accent;
        private final Model<AvatarRenderState> detail;
        private final RenderType type;
        private final int defaultBase;
        private final int defaultSecondary;
        private final int bowColor;

        @SuppressWarnings("unchecked")
        LegwearRenderer(EntityModelSet set, ModelLayerLocation layer, Identifier texture, int defaultBase, int defaultSecondary, int bowColor) {
            Function<Integer, Model<AvatarRenderState>> bandModel =
                    band -> new CosmeticModels.LegwearModel(set.bakeLayer(layer), Groups.MAIN, band);
            this.allBands = bandModel.apply(-1);
            this.bands = new Model[CosmeticModels.SOCK_BANDS];
            for (int i = 0; i < bands.length; i++) {
                bands[i] = bandModel.apply(i);
            }
            this.accent = new CosmeticModels.LegwearModel(set.bakeLayer(layer), Groups.ACCENT, -1);
            this.detail = new CosmeticModels.LegwearModel(set.bakeLayer(layer), Groups.DETAIL, -1);
            this.type = RenderTypes.entityCutout(texture);
            this.defaultBase = defaultBase;
            this.defaultSecondary = defaultSecondary;
            this.bowColor = bowColor;
        }

        @Override
        public void submit(CosmeticRenderContext ctx) {
            Colorway colorway = ctx.colorway();
            int stripes = colorway == null ? 2 : colorway.stripeCount();
            int base = colorway == null ? defaultBase : colorway.stripeColor(0);
            if (stripes == 1) {
                BuiltinCosmeticRenderers.submit(ctx, allBands, type, base);
            } else {
                for (int band = 0; band < bands.length; band++) {
                    int stripe = stripes <= REPEAT_MAX_STRIPES ? band % stripes : band * stripes / bands.length;
                    int rgb = colorway == null ? (stripe == 0 ? defaultBase : defaultSecondary) : colorway.stripeColor(stripe);
                    BuiltinCosmeticRenderers.submit(ctx, bands[band], type, rgb);
                }
            }
            BuiltinCosmeticRenderers.submit(ctx, accent, type, shade(base, DARKER));
            BuiltinCosmeticRenderers.submit(ctx, detail, type, bowColor);
        }
    }
}
