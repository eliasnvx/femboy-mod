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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

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
    private static final int FOX_ORANGE = 0xE8863A;
    private static final int BUNNY_PINK = 0xF6A9C2;
    private static final int BEAR_BROWN = 0x8B5A3C;
    private static final int BEAR_TAN = 0xD4AC82;
    private static final int WOLF_GREY = 0x8E8E98;
    private static final int CANVAS = 0xD9C9A3;
    private static final int LEATHER = 0x8B5A2B;
    private static final int NETHERITE = 0x4A444A;
    private static final int CREEPER_GREEN = 0x62B14F;
    private static final int CORD_WHITE = 0xF4F1EE;
    private static final int BOW_PINK = 0xFF8FB8;
    private static final int DARK = 0x2A2328;
    private static final int GOLD = 0xF2C94C;
    private static final int ROSE_QUARTZ = 0xF7B8CF;
    private static final int MOONSTONE = 0xDCE8FF;
    /** Default headset pink; black and white come as creative presets (any dye works). */
    private static final int HEADPHONES_PINK = 0xF7A8CC;
    private static final int NEON_CYAN = 0x7CF7FF;
    private static final int SHADES_LENS = 0x121218;

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
                models, CosmeticModels.CAT_EARS, CosmeticModels.EarsModel.factory(CosmeticModels.EarShape.CAT), FUR, PINK, LIGHTER, FUR_WHITE));
        registry.register(id("fox_ears"), models -> new GroupedRenderer(
                models, CosmeticModels.FOX_EARS, CosmeticModels.EarsModel.factory(CosmeticModels.EarShape.FOX), FUR, FOX_ORANGE, LIGHTER, FUR_WHITE));
        registry.register(id("bunny_ears"), models -> new GroupedRenderer(
                models, CosmeticModels.BUNNY_EARS, CosmeticModels.EarsModel.factory(CosmeticModels.EarShape.BUNNY), FUR, FUR_WHITE, DARKER, BUNNY_PINK));
        registry.register(id("bear_ears"), models -> new GroupedRenderer(
                models, CosmeticModels.BEAR_EARS, CosmeticModels.EarsModel.factory(CosmeticModels.EarShape.BEAR), FUR, BEAR_BROWN, LIGHTER, BEAR_TAN));
        registry.register(id("wolf_ears"), models -> new GroupedRenderer(
                models, CosmeticModels.WOLF_EARS, CosmeticModels.EarsModel.factory(CosmeticModels.EarShape.WOLF), FUR, WOLF_GREY, LIGHTER, FUR_WHITE));
        registry.register(id("tail"), models -> new GroupedRenderer(
                models, CosmeticModels.TAIL, CosmeticModels.TailModel::new, FUR, PINK, LIGHTER, FUR_WHITE));
        registry.register(id("oversized_hoodie"), models -> new GroupedRenderer(
                models, CosmeticModels.HOODIE, CosmeticModels.HoodieModel::new, KNIT, LAVENDER, DARKER, CORD_WHITE)
                .withChevronPanel(new ChevronPanel(models.bakeLayer(CosmeticModels.HOODIE_CHEST_PANEL), KNIT)));
        registry.register(id("cat_ear_hoodie"), models -> new GroupedRenderer(
                models, CosmeticModels.CAT_EAR_HOODIE, CosmeticModels.HoodieModel::new, KNIT, LAVENDER, DARKER, CORD_WHITE)
                .withChevronPanel(new ChevronPanel(models.bakeLayer(CosmeticModels.HOODIE_CHEST_PANEL), KNIT)));
        registry.register(id("pleated_skirt"), models -> new GroupedRenderer(
                models, CosmeticModels.SKIRT, CosmeticModels.SkirtModel::new, FABRIC, SKIRT_DARK, DARKER, CORD_WHITE));
        registry.register(id("programming_socks"), models -> new GroupedRenderer(
                models, CosmeticModels.SOCKS, Groups.GroupModelFactory.PLAIN, KNIT, SOCK_PINK, DARKER, BOW_PINK)
                .withDefaultSecondary(SOCK_WHITE));
        registry.register(id("striped_mittens"), models -> new GroupedRenderer(
                models, CosmeticModels.MITTENS, Groups.GroupModelFactory.PLAIN, KNIT, SOCK_PINK, DARKER, BOW_PINK)
                .withDefaultSecondary(SOCK_WHITE));
        registry.register(id("fishnet_tights"), models -> new GroupedRenderer(
                models, CosmeticModels.FISHNET, Groups.GroupModelFactory.PLAIN, FISHNET, FISHNET_BLACK, DARKER, BOW_PINK));
        registry.register(id("uwu_choker"), models -> new GroupedRenderer(
                models, CosmeticModels.CHOKER, Groups.GroupModelFactory.PLAIN, FABRIC, CHOKER_BLACK, LIGHTER, FUR_WHITE));
        registry.register(id("cat_ear_headphones"), models -> new GroupedRenderer(
                models, CosmeticModels.HEADPHONES, Groups.GroupModelFactory.PLAIN, FABRIC, HEADPHONES_PINK, LIGHTER, 0xFFB6DA)
                .withDynamicDetail(HeadphonesLight::color));
        registry.register(id("heart_glasses"), models -> new GroupedRenderer(
                models, CosmeticModels.HEART_GLASSES, Groups.GroupModelFactory.PLAIN, FABRIC, PINK, LIGHTER, FUR_WHITE));
        registry.register(id("arm_warmers"), models -> new GroupedRenderer(
                models, CosmeticModels.ARM_WARMERS, Groups.GroupModelFactory.PLAIN, KNIT, LAVENDER, DARKER, BOW_PINK)
                .withDefaultSecondary(SOCK_WHITE));
        registry.register(id("crop_sweater"), models -> new GroupedRenderer(
                models, CosmeticModels.CROP_SWEATER, Groups.GroupModelFactory.PLAIN, KNIT, SOCK_PINK, DARKER, BOW_PINK));
        registry.register(id("belt_chains"), models -> new GroupedRenderer(
                models, CosmeticModels.BELT_CHAINS, Groups.GroupModelFactory.PLAIN, FABRIC, CHOKER_BLACK, LIGHTER, BOW_PINK));
        registry.register(id("nail_polish"), models -> new GroupedRenderer(
                models, CosmeticModels.NAIL_POLISH, Groups.GroupModelFactory.PLAIN, FABRIC, PINK, LIGHTER, PINK));
        registry.register(id("moonstone_pendant"), models -> new GroupedRenderer(
                models, CosmeticModels.MOONSTONE_PENDANT, Groups.GroupModelFactory.PLAIN, FABRIC, GOLD, DARKER, MOONSTONE));
        registry.register(id("dark_shades"), models -> new GroupedRenderer(
                models, CosmeticModels.DARK_SHADES, Groups.GroupModelFactory.PLAIN, FABRIC, CHOKER_BLACK, 0.55F, SHADES_LENS));
        registry.register(id("cyber_visor"), models -> new GroupedRenderer(
                models, CosmeticModels.CYBER_VISOR, Groups.GroupModelFactory.PLAIN, FABRIC, CHOKER_BLACK, LIGHTER, NEON_CYAN));
        registry.register(id("rose_quartz_earrings"), models -> new GroupedRenderer(
                models, CosmeticModels.EARRINGS, Groups.GroupModelFactory.PLAIN, FABRIC, ROSE_QUARTZ, LIGHTER, GOLD));
        registry.register(id("rose_quartz_bracelet"), models -> new GroupedRenderer(
                models, CosmeticModels.BRACELET, Groups.GroupModelFactory.PLAIN, FABRIC, ROSE_QUARTZ, LIGHTER, GOLD));
        registerBackpack(registry, "canvas_backpack", CANVAS);
        registerBackpack(registry, "leather_backpack", LEATHER);
        registerBackpack(registry, "netherite_backpack", NETHERITE);
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

    private static void registerBackpack(ApiRegistry<CosmeticRenderer.Factory> registry, String item, int color) {
        registry.register(id(item), models -> {
            GroupedRenderer bag = new GroupedRenderer(models, CosmeticModels.BACKPACK, Groups.GroupModelFactory.PLAIN,
                    FABRIC, color, DARKER, CREEPER_GREEN);
            BackpackCharmsRenderer charms = new BackpackCharmsRenderer(models.bakeLayer(CosmeticModels.BACKPACK_CHARMS));
            return ctx -> {
                // Elytra occupy the back (SPEC §4.5): hide the backpack while wearing a glider.
                if (ctx.state().chestEquipment.has(DataComponents.GLIDER)) {
                    return;
                }
                bag.submit(ctx);
                charms.submit(ctx);
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

    /**
     * One model instance per color group (and per pattern band), each submitted with its color.
     * Patterns with ≤3 stripes repeat band by band (classic striped socks/sweaters); longer ones
     * (flags) are stretched top to bottom. Undyed items use the default colors.
     */
    static final class GroupedRenderer implements CosmeticRenderer {
        private static final int REPEAT_MAX_STRIPES = 3;
        private final Map<Groups, Model<AvatarRenderState>> groupModels = new EnumMap<>(Groups.class);
        private final Model<AvatarRenderState> mainAll;
        private final Model<AvatarRenderState> mainNoBands;
        private final Model<AvatarRenderState>[] bands;
        private final RenderType type;
        private final int defaultMain;
        private final float accentShade;
        private final int detailColor;
        private int defaultSecondary = -1;
        private ChevronPanel chevronPanel;
        private java.util.function.IntSupplier dynamicDetail;

        @SuppressWarnings("unchecked")
        GroupedRenderer(EntityModelSet set, ModelLayerLocation layer, Groups.GroupModelFactory.Factory factory,
                        Identifier texture, int defaultMain, float accentShade, int detailColor) {
            for (Groups group : new Groups[]{Groups.ACCENT, Groups.DETAIL, Groups.DARK, Groups.METAL}) {
                groupModels.put(group, factory.create(set.bakeLayer(layer), group, Groups.NO_BANDS));
            }
            this.mainAll = factory.create(set.bakeLayer(layer), Groups.MAIN, Groups.ALL_BANDS);
            this.mainNoBands = factory.create(set.bakeLayer(layer), Groups.MAIN, Groups.NO_BANDS);
            this.bands = new Model[Groups.BANDS];
            for (int i = 0; i < bands.length; i++) {
                bands[i] = factory.create(set.bakeLayer(layer), Groups.MAIN, i);
            }
            this.type = RenderTypes.entityCutout(texture);
            this.defaultMain = defaultMain;
            this.accentShade = accentShade;
            this.detailColor = detailColor;
        }

        /** Undyed items show two-tone stripes (default main + this color). */
        GroupedRenderer withDefaultSecondary(int color) {
            this.defaultSecondary = color;
            return this;
        }

        /** DETAIL color computed every frame (Cat-ear Headphones light ring). */
        GroupedRenderer withDynamicDetail(java.util.function.IntSupplier color) {
            this.dynamicDetail = color;
            return this;
        }

        /** Draws a Progress-style chevron as a patch when the pattern has one. */
        GroupedRenderer withChevronPanel(ChevronPanel panel) {
            this.chevronPanel = panel;
            return this;
        }

        @Override
        public void submit(CosmeticRenderContext ctx) {
            Colorway colorway = ctx.colorway();
            int main = colorway == null ? defaultMain : colorway.stripeColor(0, ColorwayClock.ticks());
            int stripes = colorway != null ? colorway.stripeCount() : (defaultSecondary >= 0 ? 2 : 1);
            if (stripes == 1) {
                BuiltinCosmeticRenderers.submit(ctx, mainAll, type, main);
            } else {
                BuiltinCosmeticRenderers.submit(ctx, mainNoBands, type, main);
                for (int band = 0; band < bands.length; band++) {
                    BuiltinCosmeticRenderers.submit(ctx, bands[band], type, bandColor(colorway, stripes, band));
                }
            }
            for (Map.Entry<Groups, Model<AvatarRenderState>> entry : groupModels.entrySet()) {
                Groups group = entry.getKey();
                int detail = dynamicDetail != null ? dynamicDetail.getAsInt() : detailColor;
                int rgb = group == Groups.ACCENT ? shade(main, accentShade) : fixedColor(group, detail);
                BuiltinCosmeticRenderers.submit(ctx, entry.getValue(), type, rgb);
            }
            if (chevronPanel != null && colorway != null && colorway.hasChevron()) {
                chevronPanel.submit(ctx, colorway);
            }
        }

        private int bandColor(Colorway colorway, int stripes, int band) {
            if (colorway == null) {
                return band % 2 == 0 ? defaultMain : defaultSecondary;
            }
            if (stripes <= REPEAT_MAX_STRIPES) {
                return colorway.stripeColor(band % stripes, ColorwayClock.ticks());
            }
            return colorway.stripeColor(band * stripes / bands.length, ColorwayClock.ticks());
        }
    }
}
