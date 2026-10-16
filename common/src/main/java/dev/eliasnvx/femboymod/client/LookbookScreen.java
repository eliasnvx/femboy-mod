package dev.eliasnvx.femboymod.client;

import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.eliasnvx.femboymod.api.cosmetic.CosmeticStats;
import dev.eliasnvx.femboymod.api.cosmetic.LookbookEntry;
import dev.eliasnvx.femboymod.api.cosmetic.SetBonus;
import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.client.gui.FemboyGui;
import dev.eliasnvx.femboymod.client.gui.PixelSprites;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsCatalog;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Lookbook: every cosmetic sorted by slot (tabs on the left), what the player has collected, and for the selected
 * piece its Drip, sets and where to get it (data pack registry {@code femboymod:lookbook}). Layout is rebuilt only
 * when the tab or the selection changes; drawing reuses it.
 */
public final class LookbookScreen extends Screen {

    private static final int BOOK_W = 312;
    private static final int BOOK_H = 200;
    private static final int SPINE = 6;
    private static final int PAGE_PAD = 10;
    private static final int PAGE_W = (BOOK_W - SPINE) / 2;
    private static final int TEXT_W = PAGE_W - PAGE_PAD * 2;
    private static final int TAB_W = 24;
    private static final int TAB_H = 18;
    private static final int TAB_OVERLAP = 3;
    /** The cover's border around the pages; tab icons stay clear of it. */
    private static final int COVER_BORDER = 3;
    /** Tabs per book edge; more slots continue on the right edge. */
    private static final int TABS_PER_SIDE = (BOOK_H - 2 * PAGE_PAD) / TAB_H;
    private static final int GRID_COLUMNS = 7;
    private static final int GRID_TOP = 24;
    private static final int ICON = 16;
    private static final int ICON_STEP = 17;
    private static final int BIG_ICON_SCALE = 2;
    private static final int LINE = 10;
    private static final int GAP = 4;
    private static final int BAR_H = 5;

    private static final int COVER = 0xFF7A3B5E;
    private static final int COVER_DARK = 0xFF5A2843;
    private static final int PAGE = 0xFFFBF4EA;
    private static final int PAGE_SHADE = 0xFFEFE3D3;
    private static final int TAB_IDLE = 0xFFD9B8CC;
    private static final int TEXT = 0xFF3A2C38;
    private static final int ACCENT = 0xFFC2508E;
    private static final int MUTED = 0xFF8A7A86;
    private static final int SET = 0xFF7A3FB0;
    private static final int GOT = 0xFF3F9B5A;
    private static final int UNCOLLECTED_VEIL = 0xB0FBF4EA;
    private static final int SELECTED = 0xFFE07AB0;
    private static final int BAR_BACK = 0xFFE6D6DF;

    private final @Nullable Screen parent;
    private List<ResourceLocation> slots = List.of();
    private final List<Item> pageItems = new ArrayList<>();
    private final List<ItemStack> pageStacks = new ArrayList<>();
    private final List<Boolean> pageCollected = new ArrayList<>();
    private final List<Row> details = new ArrayList<>();
    private Set<ResourceLocation> collected = Set.of();
    private int tab;
    private String pageCount = "";
    private Component progressText = Component.empty();
    private @Nullable ItemStack selectedStack;
    private float progress;
    private @Nullable Item selected;
    /** Item tooltip found while drawing this frame, drawn last. */
    private @Nullable ItemStack tooltipStack;
    private int left;
    private int top;

    /** One line of the details page: wrapped text, or a row of item icons (crafting ingredients, a price). */
    private record Row(@Nullable FormattedCharSequence text, int color, List<ItemStack> icons) {
        int height() {
            return text != null ? LINE : ICON_STEP + 1;
        }
    }

    private @Nullable Item focus;

    public LookbookScreen(@Nullable Screen parent) {
        this(parent, null);
    }

    /** Opens the Lookbook on {@code focus}'s page with it selected. */
    public LookbookScreen(@Nullable Screen parent, @Nullable Item focus) {
        super(Component.translatable("gui.femboymod.lookbook.title"));
        this.parent = parent;
        this.focus = focus;
    }

    @Override
    protected void init() {
        top = (height - BOOK_H) / 2;
        Player player = Minecraft.getInstance().player;
        collected = player == null ? Set.of() : FemboyApi.get().getProfile(player).get(FemboyProfileFields.COLLECTION);
        List<ResourceLocation> withItems = new ArrayList<>();
        for (ResourceLocation slot : CosmeticsManager.orderedSlots()) {
            if (CosmeticsCatalog.items().stream().anyMatch(item -> slot.equals(slotOf(item)))) {
                withItems.add(slot);
            }
        }
        slots = List.copyOf(withItems);
        int tabsWidth = (TAB_W - TAB_OVERLAP) * (slots.size() > TABS_PER_SIDE ? 2 : 1);
        left = (width - BOOK_W - tabsWidth) / 2 + TAB_W - TAB_OVERLAP;
        int got = 0;
        for (Item item : CosmeticsCatalog.items()) {
            if (collected.contains(BuiltInRegistries.ITEM.getKey(item))) {
                got++;
            }
        }
        progressText = Component.translatable("gui.femboymod.lookbook.progress", got, CosmeticsCatalog.items().size());
        progress = CosmeticsCatalog.items().isEmpty() ? 0 : got / (float) CosmeticsCatalog.items().size();
        ResourceLocation focusSlot = focus == null ? null : slotOf(focus);
        if (focusSlot != null && slots.contains(focusSlot)) {
            selectTab(slots.indexOf(focusSlot));
            select(focus);
            focus = null;
        } else {
            selectTab(Math.min(tab, Math.max(0, slots.size() - 1)));
        }
    }

    private static @Nullable ResourceLocation slotOf(Item item) {
        return CosmeticsManager.slotOf(new ItemStack(item));
    }

    private void selectTab(int index) {
        tab = index;
        pageItems.clear();
        pageStacks.clear();
        pageCollected.clear();
        int got = 0;
        if (!slots.isEmpty()) {
            ResourceLocation slot = slots.get(index);
            for (Item item : CosmeticsCatalog.items()) {
                if (slot.equals(slotOf(item))) {
                    boolean has = collected.contains(BuiltInRegistries.ITEM.getKey(item));
                    pageItems.add(item);
                    pageStacks.add(item.getDefaultInstance());
                    pageCollected.add(has);
                    got += has ? 1 : 0;
                }
            }
        }
        pageCount = got + "/" + pageItems.size();
        select(pageItems.isEmpty() ? null : pageItems.getFirst());
    }

    private void select(@Nullable Item item) {
        selected = item;
        selectedStack = item == null ? null : item.getDefaultInstance();
        details.clear();
        Minecraft minecraft = Minecraft.getInstance();
        if (item == null || minecraft.level == null) {
            return;
        }
        RegistryAccess registries = minecraft.level.registryAccess();
        int drip = registries.registry(CosmeticStats.REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(CosmeticStats.keyOf(item))).map(CosmeticStats::drip).orElse(0);
        if (drip > 0) {
            text(Component.translatable("gui.femboymod.lookbook.drip", drip), FemboyGui.DRIP);
        }
        registries.registry(SetBonus.REGISTRY_KEY).ifPresent(registry -> registry.holders().forEach(set -> {
            if (set.value().pieces().stream().anyMatch(piece -> piece.contains(item.builtInRegistryHolder()))) {
                text(Component.translatable("gui.femboymod.lookbook.set",
                        Component.translatable(set.key().location().toLanguageKey("set_bonus"))), SET);
            }
        }));
        boolean got = collected.contains(BuiltInRegistries.ITEM.getKey(item));
        text(Component.translatable(got ? "gui.femboymod.lookbook.collected" : "gui.femboymod.lookbook.not_collected"),
                got ? GOT : MUTED);
        details.add(new Row(FormattedCharSequence.EMPTY, TEXT, List.of()));
        text(Component.translatable("gui.femboymod.lookbook.where"), ACCENT);
        LookbookEntry entry = registries.registry(LookbookEntry.REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(LookbookEntry.keyOf(item))).orElse(LookbookEntry.NONE);
        if (entry.sources().isEmpty()) {
            text(Component.translatable("gui.femboymod.lookbook.creative"), MUTED);
        }
        for (LookbookEntry.Source source : entry.sources()) {
            text(sourceLine(source), TEXT);
            List<ItemStack> stacks = source.items().stream().map(LookbookEntry.Count::toStack).toList();
            for (int i = 0; i < stacks.size(); i += iconsPerRow()) {
                details.add(new Row(null, TEXT, stacks.subList(i, Math.min(stacks.size(), i + iconsPerRow()))));
            }
            source.note().ifPresent(note -> text(Component.translatable(note), MUTED));
        }
    }

    private static int iconsPerRow() {
        return TEXT_W / ICON_STEP;
    }

    private void text(Component text, int color) {
        for (FormattedCharSequence line : font.split(text, TEXT_W)) {
            details.add(new Row(line, color, List.of()));
        }
    }

    private static Component sourceLine(LookbookEntry.Source source) {
        Component from = source.from().map(LookbookScreen::nameOf).orElse(Component.empty());
        return switch (source.kind()) {
            case CRAFTING -> Component.translatable("gui.femboymod.lookbook.crafting");
            case SMITHING -> Component.translatable("gui.femboymod.lookbook.smithing");
            case TRADE -> source.level() > 0
                    ? Component.translatable("gui.femboymod.lookbook.trade_level", from, source.level())
                    : Component.translatable("gui.femboymod.lookbook.trade", from);
            case DROP -> Component.translatable("gui.femboymod.lookbook.drop", from);
            case GIFT -> Component.translatable("gui.femboymod.lookbook.gift", from);
            case NOTE -> Component.empty();
        };
    }

    /** Display name of an entity type or a villager profession; the raw id for anything else. */
    private static Component nameOf(ResourceLocation id) {
        if (BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return BuiltInRegistries.ENTITY_TYPE.get(id).getDescription();
        }
        if (BuiltInRegistries.VILLAGER_PROFESSION.containsKey(id)) {
            // 1.21.1 professions have no display name; vanilla villagers use entity.minecraft.villager.<path>
            return Component.translatable(EntityType.VILLAGER.getDescriptionId() + "." + id.getPath());
        }
        return Component.literal(id.toString());
    }

    private int leftPageX() {
        return left + PAGE_PAD;
    }

    private int rightPageX() {
        return left + PAGE_W + SPINE + PAGE_PAD;
    }

    private int cellX(int index) {
        return leftPageX() + (index % GRID_COLUMNS) * FemboyGui.SLOT;
    }

    private int cellY(int index) {
        return top + GRID_TOP + (index / GRID_COLUMNS) * FemboyGui.SLOT;
    }

    private int tabY(int index) {
        return top + PAGE_PAD + (index % TABS_PER_SIDE) * TAB_H;
    }

    private boolean rightTab(int index) {
        return index >= TABS_PER_SIDE;
    }

    /** Left edge of the tab, including the part hidden under the book. */
    private int tabX(int index) {
        return rightTab(index) ? left + BOOK_W - TAB_OVERLAP : left - TAB_W + TAB_OVERLAP;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        tooltipStack = null;
        drawTabs(g);
        drawBook(g);
        drawGrid(g, mouseX, mouseY);
        drawProgress(g);
        drawDetails(g, mouseX, mouseY);
        // tooltips last, over the book (1.21.1 draws them right away, 26.3 queues them for the end of the frame)
        int hoveredTab = hoveredTab(mouseX, mouseY);
        if (hoveredTab >= 0) {
            g.renderTooltip(font, FemboyGui.slotName(slots.get(hoveredTab)), mouseX, mouseY);
        } else if (tooltipStack != null) {
            g.renderTooltip(font, tooltipStack, mouseX, mouseY);
        }
    }

    private void drawTabs(GuiGraphics g) {
        for (int i = 0; i < slots.size(); i++) {
            int x = tabX(i);
            int y = tabY(i);
            boolean active = i == tab;
            boolean right = rightTab(i);
            g.fill(x, y, x + TAB_W, y + TAB_H - 1, COVER_DARK);
            g.fill(right ? x : x + 1, y + 1, right ? x + TAB_W - 1 : x + TAB_W, y + TAB_H - 2, active ? PAGE : TAB_IDLE);
            int iconX = right ? x + TAB_OVERLAP + COVER_BORDER : x + TAB_W - TAB_OVERLAP - COVER_BORDER - ICON;
            CosmeticsManager.slotType(slots.get(i)).emptySlotIcon()
                    .ifPresent(icon -> PixelSprites.blitFitted(g, icon, iconX, y + 1, ICON));
        }
    }

    private void drawBook(GuiGraphics g) {
        g.fill(left - COVER_BORDER, top - COVER_BORDER, left + BOOK_W + COVER_BORDER, top + BOOK_H + COVER_BORDER, COVER_DARK);
        g.fill(left - 2, top - 2, left + BOOK_W + 2, top + BOOK_H + 2, COVER);
        g.fill(left, top, left + PAGE_W, top + BOOK_H, PAGE);
        g.fill(left + PAGE_W + SPINE, top, left + BOOK_W, top + BOOK_H, PAGE);
        g.fill(left + PAGE_W - 2, top, left + PAGE_W, top + BOOK_H, PAGE_SHADE);
        g.fill(left + PAGE_W + SPINE, top, left + PAGE_W + SPINE + 2, top + BOOK_H, PAGE_SHADE);
        if (!slots.isEmpty()) {
            // the active tab joins its page
            int joinX = rightTab(tab) ? left + BOOK_W : left - TAB_OVERLAP;
            g.fill(joinX, tabY(tab) + 1, joinX + TAB_OVERLAP, tabY(tab) + TAB_H - 2, PAGE);
        }
    }

    private void drawGrid(GuiGraphics g, int mouseX, int mouseY) {
        if (slots.isEmpty()) {
            return;
        }
        ResourceLocation slot = slots.get(tab);
        g.drawString(font, FemboyGui.slotName(slot), leftPageX(), top + PAGE_PAD, ACCENT, false);
        g.drawString(font, pageCount, leftPageX() + TEXT_W - font.width(pageCount), top + PAGE_PAD, MUTED, false);
        for (int i = 0; i < pageStacks.size(); i++) {
            ItemStack stack = pageStacks.get(i);
            int x = cellX(i);
            int y = cellY(i);
            FemboyGui.slotFrame(g, x, y);
            g.renderItem(stack, x + 1, y + 1);
            if (!pageCollected.get(i)) {
                // over the item: 1.21.1 draws GUI items in depth, a plain fill would end up behind them
                g.fill(RenderType.guiOverlay(), x + 1, y + 1, x + FemboyGui.SLOT - 1, y + FemboyGui.SLOT - 1, UNCOLLECTED_VEIL);
            }
            if (pageItems.get(i) == selected) {
                outline(g, x - 1, y - 1, FemboyGui.SLOT + 2, FemboyGui.SLOT + 2, SELECTED);
            }
            if (inside(mouseX, mouseY, x, y, FemboyGui.SLOT, FemboyGui.SLOT)) {
                tooltipStack = stack;
            }
        }
    }

    private void drawProgress(GuiGraphics g) {
        int y = top + BOOK_H - PAGE_PAD - LINE - BAR_H - 2;
        g.drawString(font, progressText, leftPageX(), y, TEXT, false);
        int barY = y + LINE + 1;
        g.fill(leftPageX(), barY, leftPageX() + TEXT_W, barY + BAR_H, BAR_BACK);
        int filled = Math.round(TEXT_W * progress);
        g.fill(leftPageX(), barY, leftPageX() + filled, barY + BAR_H, SELECTED);
    }

    private void drawDetails(GuiGraphics g, int mouseX, int mouseY) {
        if (selected == null || selectedStack == null) {
            return;
        }
        int x = rightPageX();
        int y = top + PAGE_PAD;
        ItemStack stack = selectedStack;
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(BIG_ICON_SCALE, BIG_ICON_SCALE, 1.0F);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        int nameX = x + ICON * BIG_ICON_SCALE + GAP;
        g.drawWordWrap(font, stack.getHoverName(), nameX, y + 2, TEXT_W - ICON * BIG_ICON_SCALE - GAP, TEXT);
        ResourceLocation slot = slotOf(selected);
        if (slot != null) {
            g.drawString(font, FemboyGui.slotName(slot), nameX, y + ICON * BIG_ICON_SCALE - LINE + 1, MUTED, false);
        }
        y += ICON * BIG_ICON_SCALE + GAP;
        int bottom = top + BOOK_H - PAGE_PAD;
        for (Row row : details) {
            if (y + row.height() > bottom) {
                break;
            }
            if (row.text() != null) {
                g.drawString(font, row.text(), x, y, row.color(), false);
            } else {
                for (int i = 0; i < row.icons().size(); i++) {
                    ItemStack icon = row.icons().get(i);
                    int ix = x + i * ICON_STEP;
                    g.renderItem(icon, ix, y);
                    g.renderItemDecorations(font, icon, ix, y);
                    if (inside(mouseX, mouseY, ix, y, ICON, ICON)) {
                        tooltipStack = icon;
                    }
                }
            }
            y += row.height();
        }
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private int hoveredTab(double mouseX, double mouseY) {
        for (int i = 0; i < slots.size(); i++) {
            int x = rightTab(i) ? tabX(i) + TAB_OVERLAP : tabX(i);
            if (inside(mouseX, mouseY, x, tabY(i), TAB_W - TAB_OVERLAP, TAB_H - 1)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int clickedTab = hoveredTab(mouseX, mouseY);
        if (clickedTab >= 0) {
            selectTab(clickedTab);
            return true;
        }
        for (int i = 0; i < pageItems.size(); i++) {
            if (inside(mouseX, mouseY, cellX(i), cellY(i), FemboyGui.SLOT, FemboyGui.SLOT)) {
                select(pageItems.get(i));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
