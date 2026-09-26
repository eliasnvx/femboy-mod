package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.api.drip.DripRules;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Compact "Femboy Level" panel in the top-left corner (SPEC §4.6, client config {@code femboy_level_panel}): the Drip
 * Level and tier name on one line, a thin bar over the whole range with a notch per tier, and the first active set.
 * <p>
 * The text is rebuilt only when the level, tier or sets change (checked once per client tick), so a frame only
 * draws. The bar eases toward its target and a soft highlight drifts along it; nothing blinks.
 */
public final class DripHud {

    private static final int MARGIN = 4;
    private static final int PAD = 5;
    private static final int MIN_WIDTH = 64;
    private static final int LINE = 9;
    private static final int HEART_W = 9;
    private static final int GAP = 6;
    private static final int BAR_H = 4;

    private static final int BACKGROUND = 0xC0201A26;
    private static final int BORDER = 0xFFE07AB0;
    private static final int BORDER_SOFT = 0x80E07AB0;
    private static final int LEVEL = 0xFFFFFFFF;
    private static final int SET = 0xFFC9A3F0;
    private static final int BAR_BACK = 0xFF3A2C38;
    private static final int BAR_FILL = 0xFFE07AB0;
    private static final int BAR_TOP = 0xFFF4B6D6;
    private static final int NOTCH = 0xFF201A26;
    private static final int GLOW = 0x40FFFFFF;
    private static final int GLOW_W = 6;
    /** Tier colors from soft pink to lavender; tiers past the end reuse the last one. */
    private static final int[] TIER_COLORS = {0xFFD8C8D2, 0xFFFFC2DF, 0xFFFF9FD0, 0xFFF07AC0, 0xFFD58CF0, 0xFFB58CFF};
    /** Share of the remaining distance the bar covers per tick. */
    private static final float EASE_PER_TICK = 0.25F;
    private static final float GLOW_PERIOD_TICKS = 60.0F;

    private static int lastTick = Integer.MIN_VALUE;
    private static int level = -1;
    private static int tier = -1;
    private static List<Identifier> sets = List.of();
    private static float target;
    private static float shown;
    private static float glowTicks;

    private static String levelText = "";
    private static Component tierName = Component.empty();
    /** The first active set, plus "+N" when more are active; empty without sets. */
    private static Component setLine = Component.empty();
    private static int[] notches = new int[0];
    private static int width = MIN_WIDTH;

    private DripHud() {
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!FemboyConfig.client().dripHud() || player == null || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        if (player.tickCount != lastTick) {
            lastTick = player.tickCount;
            refresh(minecraft.font, player);
        }
        float step = delta.getRealtimeDeltaTicks();
        shown += (target - shown) * Math.min(1.0F, step * EASE_PER_TICK);
        glowTicks = (glowTicks + step) % GLOW_PERIOD_TICKS;
        draw(g, minecraft.font);
    }

    /** Re-evaluates the worn outfit and rebuilds the text when something changed. */
    private static void refresh(Font font, Player player) {
        WornEvaluator.Evaluation evaluation = WornEvaluator.evaluate(player);
        int newLevel = evaluation.drip().level();
        int newTier = evaluation.drip().tier();
        List<Identifier> newSets = evaluation.activeSets().stream().sorted().toList();
        if (newLevel == level && newTier == tier && newSets.equals(sets)) {
            return;
        }
        boolean first = level < 0;
        level = newLevel;
        tier = newTier;
        sets = newSets;
        DripRules rules = player.level().registryAccess().lookup(DripRules.REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(DripRules.DEFAULT)).orElse(DripRules.FALLBACK);
        int max = Math.max(1, rules.maxLevel());
        target = Mth.clamp(level / (float) max, 0.0F, 1.0F);
        if (first) {
            shown = target;
        }

        levelText = String.valueOf(level);
        tierName = tierName(tier);
        List<Integer> thresholds = rules.tierThresholds();
        setLine = Component.empty();
        if (!sets.isEmpty()) {
            var line = Component.literal("✦ ").append(Component.translatable(sets.getFirst().toLanguageKey("set_bonus")));
            if (sets.size() > 1) {
                line.append(" ").append(Component.translatable("hud.femboymod.drip.more_sets", sets.size() - 1));
            }
            setLine = line;
        }

        width = Math.max(MIN_WIDTH, HEART_W + font.width(levelText) + GAP + font.width(tierName) + PAD * 2);
        width = Math.max(width, font.width(setLine) + PAD * 2);
        int barW = width - PAD * 2;
        notches = new int[thresholds.size()];
        for (int i = 0; i < notches.length; i++) {
            notches[i] = Math.round((barW - 2) * Mth.clamp(thresholds.get(i) / (float) max, 0.0F, 1.0F));
        }
    }

    private static Component tierName(int tier) {
        return Component.translatableWithFallback("hud.femboymod.drip.tier." + tier,
                "Tier " + tier);
    }

    private static void draw(GuiGraphicsExtractor g, Font font) {
        int x = MARGIN;
        int y = MARGIN;
        boolean hasSet = sets.size() > 0;
        int height = PAD + LINE + 1 + BAR_H + (hasSet ? 2 + LINE : 0) + PAD;
        frame(g, x, y, width, height);

        int cx = x + PAD;
        int cy = y + PAD;
        heart(g, cx, cy);
        g.text(font, levelText, cx + HEART_W, cy, LEVEL, true);
        int tierColor = TIER_COLORS[Math.min(tier, TIER_COLORS.length - 1)];
        g.text(font, tierName, x + width - PAD - font.width(tierName), cy, tierColor, true);
        cy += LINE + 1;

        bar(g, cx, cy, width - PAD * 2);
        cy += BAR_H + 2;
        if (hasSet) {
            g.text(font, setLine, cx, cy, SET, true);
        }
    }

    /** Translucent card with a pink border and cut corners. */
    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BACKGROUND);
        g.fill(x + 2, y, x + w - 2, y + 1, BORDER);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, BORDER);
        g.fill(x, y + 2, x + 1, y + h - 2, BORDER);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, BORDER);
        g.fill(x + 1, y + 1, x + 2, y + 2, BORDER);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, BORDER);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, BORDER);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, BORDER);
        g.fill(x + 2, y + 1, x + w - 2, y + 2, BORDER_SOFT);
    }

    /** A 7x6 pixel heart. */
    private static void heart(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 1, y + 1, x + 3, y + 2, BORDER);
        g.fill(x + 4, y + 1, x + 6, y + 2, BORDER);
        g.fill(x, y + 2, x + 7, y + 4, BORDER);
        g.fill(x + 1, y + 4, x + 6, y + 5, BORDER);
        g.fill(x + 2, y + 5, x + 5, y + 6, BORDER);
        g.fill(x + 3, y + 6, x + 4, y + 7, BORDER);
        g.fill(x + 1, y + 2, x + 2, y + 3, BAR_TOP);
    }

    /** Whole-range bar with a notch at every tier threshold and a slow highlight drifting over the filled part. */
    private static void bar(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + BAR_H, BAR_BACK);
        int inner = w - 2;
        int filled = Math.round(inner * shown);
        if (filled > 0) {
            g.fill(x + 1, y + 1, x + 1 + filled, y + BAR_H - 1, BAR_FILL);
            g.fill(x + 1, y + 1, x + 1 + filled, y + 2, BAR_TOP);
            if (FemboyConfig.client().rgbAnimations() && filled > GLOW_W) {
                // Ping-pong along a cosine, so the band never jumps back to the start.
                float sweep = (1.0F - Mth.cos(glowTicks / GLOW_PERIOD_TICKS * Mth.TWO_PI)) * 0.5F;
                int glow = x + 1 + Math.round((filled - GLOW_W) * sweep);
                g.fill(glow, y + 1, glow + GLOW_W, y + BAR_H - 1, GLOW);
            }
        }
        for (int notch : notches) {
            if (notch > 0 && notch < inner) {
                g.fill(x + 1 + notch, y, x + 2 + notch, y + BAR_H, NOTCH);
            }
        }
    }
}
