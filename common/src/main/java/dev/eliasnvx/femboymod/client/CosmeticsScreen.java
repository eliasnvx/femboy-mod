package dev.eliasnvx.femboymod.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.renderer.RenderPipelines;
import java.util.List;
import java.util.ArrayList;
import dev.eliasnvx.femboymod.network.CycleArmorVisibilityPayload;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.api.cosmetic.ArmorVisibility;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsCatalog;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.api.profile.PlayerProfile;
import dev.eliasnvx.femboymod.api.profile.FemboyProfileFields;
import dev.eliasnvx.femboymod.api.FemboyApi;
import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.api.drip.DripRules;
import dev.eliasnvx.femboymod.client.gui.FemboyGui;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.drip.WornEvaluator;
import dev.eliasnvx.femboymod.menu.CosmeticSlot;
import dev.eliasnvx.femboymod.menu.CosmeticsMenu;
import dev.eliasnvx.femboymod.network.ToggleCosmeticHiddenPayload;
import dev.eliasnvx.femboymod.wardrobe.WardrobePresets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/**
 * Wardrobe (SPEC §4.6): cosmetic slots around a player doll that follows the mouse, a Drip card with the
 * level, tier and a progress bar, the active set bonuses, and the player inventory below.
 */
public final class CosmeticsScreen extends AbstractContainerScreen<CosmeticsMenu> {

    private static final int DOLL_BACKGROUND = 0xFF1D1A22;
    private static final int INFO_BACKGROUND = 0xFFE9DDE6;
    private static final int DOLL_SCALE = 34;
    private static final float DOLL_Y_OFFSET = 0.0625F;
    private static final int INFO_PADDING = 4;
    private static final int LINE = 10;
    private static final int BAR_HEIGHT = 6;

    private static final int PRESETS_W = 4 * FemboyGui.SLOT + 6;
    private static final int PRESETS_GAP = 2;
    private static final int PRESET_BUTTON_H = 16;
    private static final int PRESET_APPLY_W = PRESETS_W - 8 - 2 - 16;
    private static final int PRESET_SAVE_W = 16;
    /** Style Points, collection and the coupon button under the presets. */
    private static final int STATS_GAP = 4;
    /** Armor toggles under the coupon button: one slot per armor piece. */
    private static final int ARMOR_ROW_GAP = 4;
    private static final int STATS_H = 2 * LINE + PRESET_BUTTON_H + ARMOR_ROW_GAP + FemboyGui.SLOT + 6;
    private static final String[] ARMOR_SPRITES = {"container/slot/helmet", "container/slot/chestplate",
            "container/slot/leggings", "container/slot/boots"};
    private static final int ARMOR_SHOWN_TEXT = 0xFFFFFFFF;
    private static final int COUPON_W = PRESET_APPLY_W + 2 + PRESET_SAVE_W;

    public CosmeticsScreen(CosmeticsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CosmeticsMenu.WIDTH, menu.height());
        this.inventoryLabelY = menu.inventoryTop() - 11;
    }

    /** Outfit presets (shared with the Wardrobe block) on a panel to the right. */
    @Override
    protected void init() {
        super.init();
        int x = leftPos + imageWidth + PRESETS_GAP + 4;
        for (int i = 0; i < WardrobePresets.COUNT; i++) {
            int index = i;
            int y = topPos + CosmeticsMenu.TOP + i * (PRESET_BUTTON_H + 2);
            addRenderableWidget(Button.builder(Component.translatable("gui.femboymod.wardrobe.preset", i + 1),
                            b -> click(CosmeticsMenu.APPLY_BUTTON + index))
                    .bounds(x, y, PRESET_APPLY_W, PRESET_BUTTON_H)
                    .tooltip(Tooltip.create(Component.translatable("gui.femboymod.outfit.apply.tooltip")))
                    .build());
            addRenderableWidget(Button.builder(Component.literal("✎"), b -> click(CosmeticsMenu.SAVE_BUTTON + index))
                    .bounds(x + PRESET_APPLY_W + 2, y, PRESET_SAVE_W, PRESET_BUTTON_H)
                    .tooltip(Tooltip.create(Component.translatable("gui.femboymod.wardrobe.save.tooltip")))
                    .build());
        }
        int couponY = topPos + presetsBottom() + STATS_GAP + 2 * LINE;
        int cost = FemboyConfig.common().stylePoints().couponCost();
        addRenderableWidget(Button.builder(Component.translatable("gui.femboymod.outfit.coupon"), b -> click(CosmeticsMenu.REDEEM_BUTTON))
                .bounds(x, couponY, COUPON_W, PRESET_BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable("gui.femboymod.outfit.coupon.tooltip", cost)))
                .build());
    }

    /** Top-left of the armor toggle for {@code index} (0 head .. 3 feet), relative to the screen origin. */
    private int armorX(int index) {
        return imageWidth + PRESETS_GAP + 3 + index * FemboyGui.SLOT;
    }

    private static int armorY() {
        return presetsBottom() + STATS_GAP + 2 * LINE + PRESET_BUTTON_H + ARMOR_ROW_GAP;
    }

    private int hoveredArmor(double mouseX, double mouseY) {
        double y = mouseY - topPos - armorY();
        if (y < 0 || y >= FemboyGui.SLOT) {
            return -1;
        }
        for (int i = 0; i < ArmorHiding.ARMOR_SLOTS.size(); i++) {
            double x = mouseX - leftPos - armorX(i);
            if (x >= 0 && x < FemboyGui.SLOT) {
                return i;
            }
        }
        return -1;
    }

    /** Worn armor (or the empty silhouette) with the chosen visibility: crossed eye = hidden, open eye = shown, A = auto. */
    private void extractArmorToggles(GuiGraphicsExtractor g, Player player, int mouseX, int mouseY) {
        var inventory = CosmeticsManager.get(player);
        int hovered = hoveredArmor(mouseX, mouseY);
        for (int i = 0; i < ArmorHiding.ARMOR_SLOTS.size(); i++) {
            EquipmentSlot armorSlot = ArmorHiding.ARMOR_SLOTS.get(i);
            int x = armorX(i);
            int y = armorY();
            FemboyGui.slotFrame(g, x, y);
            ItemStack worn = player.getItemBySlot(armorSlot);
            if (worn.isEmpty()) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, ResourceLocation.withDefaultNamespace(ARMOR_SPRITES[i]), x + 1, y + 1, 16, 16);
            } else {
                g.item(worn, x + 1, y + 1);
            }
            ArmorVisibility choice = inventory.armorVisibility(armorSlot);
            if (ArmorHiding.isHidden(inventory, armorSlot, ArmorHiding.allowedOnClient())) {
                FemboyGui.hiddenShade(g, x, y);
            }
            switch (choice) {
                case HIDE -> FemboyGui.eye(g, x, y, true);
                case SHOW -> FemboyGui.eye(g, x, y, false);
                case AUTO -> g.text(font, "A", x + FemboyGui.EYE_X, y + 1, ARMOR_SHOWN_TEXT, true);
            }
            if (i == hovered) {
                g.fill(x + 1, y + 1, x + FemboyGui.SLOT - 1, y + FemboyGui.SLOT - 1, 0x80FFFFFF);
            }
        }
    }

    private static int presetsBottom() {
        return CosmeticsMenu.TOP + WardrobePresets.COUNT * (PRESET_BUTTON_H + 2) + 4;
    }

    private void click(int buttonId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        FemboyGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        FemboyGui.panel(g, leftPos + imageWidth + PRESETS_GAP, topPos, PRESETS_W, presetsBottom());
        FemboyGui.panel(g, leftPos + imageWidth + PRESETS_GAP, topPos + presetsBottom() + STATS_GAP - 4, PRESETS_W, STATS_H + 4);
        int dollX = leftPos + CosmeticsMenu.DOLL_X;
        int top = topPos + CosmeticsMenu.TOP;
        FemboyGui.inset(g, dollX, top, CosmeticsMenu.DOLL_W, CosmeticsMenu.SIDE_H, DOLL_BACKGROUND);
        FemboyGui.inset(g, leftPos + CosmeticsMenu.INFO_X, top, CosmeticsMenu.INFO_W, CosmeticsMenu.SIDE_H, INFO_BACKGROUND);
        for (Slot slot : menu.slots) {
            FemboyGui.slotFrame(g, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, dollX + 1, top + 1, dollX + CosmeticsMenu.DOLL_W - 1,
                    top + CosmeticsMenu.SIDE_H - 1, DOLL_SCALE, DOLL_Y_OFFSET, mouseX, mouseY, player);
        }
    }

    /** Title, the Drip card and the active sets (SPEC §4.6). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.text(font, Component.translatable("gui.femboymod.wardrobe.presets"), imageWidth + PRESETS_GAP + 5, titleLabelY, FemboyGui.TEXT, false);
        Player viewer = Minecraft.getInstance().player;
        if (viewer != null) {
            PlayerProfile profile = FemboyApi.get().getProfile(viewer);
            int statsX = imageWidth + PRESETS_GAP + 5;
            int statsY = presetsBottom() + STATS_GAP;
            g.text(font, Component.translatable("gui.femboymod.outfit.style_points", profile.get(FemboyProfileFields.STYLE_POINTS)),
                    statsX, statsY, FemboyGui.DRIP, false);
            g.text(font, Component.translatable("gui.femboymod.outfit.collection", profile.get(FemboyProfileFields.COLLECTION).size(),
                    CosmeticsCatalog.size()), statsX, statsY + LINE, FemboyGui.SET, false);
            extractArmorToggles(g, viewer, mouseX, mouseY);
        }
        extractHiddenState(g, mouseX, mouseY);
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        WornEvaluator.Evaluation evaluation = WornEvaluator.evaluate(player);
        int x = CosmeticsMenu.INFO_X + INFO_PADDING;
        int width = CosmeticsMenu.INFO_W - INFO_PADDING * 2;
        int y = CosmeticsMenu.TOP + INFO_PADDING;
        g.text(font, Component.translatable("gui.femboymod.wardrobe.drip"), x, y, FemboyGui.TEXT, false);
        y += LINE;
        g.text(font, Component.literal(String.valueOf(evaluation.drip().level())), x, y, FemboyGui.DRIP, false);
        Component tier = Component.translatable("gui.femboymod.wardrobe.tier", evaluation.drip().tier());
        g.text(font, tier, x + width - font.width(tier), y, FemboyGui.TEXT, false);
        y += LINE;
        int max = player.level().registryAccess().lookup(DripRules.REGISTRY_KEY)
                .flatMap(registry -> registry.getOptional(DripRules.DEFAULT)).orElse(DripRules.FALLBACK).maxLevel();
        FemboyGui.dripBar(g, x, y, width, BAR_HEIGHT, evaluation.drip().level() / (float) max);
        y += BAR_HEIGHT + INFO_PADDING;
        if (evaluation.activeSets().isEmpty()) {
            g.text(font, Component.translatable("gui.femboymod.wardrobe.no_sets"), x, y, FemboyGui.TEXT, false);
        }
        for (ResourceLocation set : evaluation.activeSets()) {
            if (y > CosmeticsMenu.TOP + CosmeticsMenu.SIDE_H - LINE) {
                break;
            }
            for (var line : font.split(Component.literal("✦ ").append(Component.translatable(set.toLanguageKey("set_bonus"))), width)) {
                g.text(font, line, x, y, FemboyGui.SET, false);
                y += LINE;
            }
        }
    }

    /** Hidden items get shaded and a crossed eye; hovering a worn item shows its eye toggle. */
    private void extractHiddenState(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        var worn = CosmeticsManager.get(player);
        for (Slot slot : menu.slots) {
            if (slot instanceof CosmeticSlot cosmetic) {
                boolean hidden = worn.isHidden(cosmetic.slotId());
                int x = slot.x - 1;
                int y = slot.y - 1;
                if (hidden && slot.hasItem()) {
                    FemboyGui.hiddenShade(g, x, y);
                }
                if (hidden || slot == hoveredSlot && slot.hasItem()) {
                    FemboyGui.eye(g, x, y, hidden);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int armor = hoveredArmor(event.x(), event.y());
        if (armor >= 0) {
            NetworkManager.sendToServer(new CycleArmorVisibilityPayload(ArmorHiding.ARMOR_SLOTS.get(armor)));
            return true;
        }
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (Slot slot : menu.slots) {
                if (slot instanceof CosmeticSlot cosmetic && FemboyGui.onEye(event.x() - leftPos, event.y() - topPos, slot.x - 1, slot.y - 1)
                        && (slot.hasItem() || CosmeticsManager.get(player).isHidden(cosmetic.slotId()))) {
                    NetworkManager.sendToServer(new ToggleCosmeticHiddenPayload(cosmetic.slotId()));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int armor = hoveredArmor(mouseX, mouseY);
        Player viewer = Minecraft.getInstance().player;
        if (armor >= 0 && viewer != null) {
            EquipmentSlot armorSlot = ArmorHiding.ARMOR_SLOTS.get(armor);
            var inventory = CosmeticsManager.get(viewer);
            String state = inventory.armorVisibility(armorSlot).getSerializedName();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.femboymod.armor." + armorSlot.getName()));
            lines.add(Component.translatable("gui.femboymod.armor.state." + state,
                    Component.translatable(ArmorHiding.isHidden(inventory, armorSlot, ArmorHiding.allowedOnClient())
                            ? "gui.femboymod.armor.hidden" : "gui.femboymod.armor.shown")));
            if (!ArmorHiding.allowedOnClient()) {
                lines.add(Component.translatable("gui.femboymod.armor.forbidden"));
            }
            lines.add(Component.translatable("gui.femboymod.armor.click"));
            g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        if (hoveredSlot instanceof CosmeticSlot eyeSlot && eyeSlot.hasItem()
                && FemboyGui.onEye(mouseX - leftPos, mouseY - topPos, eyeSlot.x - 1, eyeSlot.y - 1)) {
            Player player = Minecraft.getInstance().player;
            boolean hidden = player != null && CosmeticsManager.get(player).isHidden(eyeSlot.slotId());
            g.setTooltipForNextFrame(font, Component.translatable(hidden ? "gui.femboymod.cosmetic.show" : "gui.femboymod.cosmetic.hide"), mouseX, mouseY);
            return;
        }
        // Name empty cosmetic slots so players know what goes where.
        if (menu.getCarried().isEmpty() && hoveredSlot instanceof CosmeticSlot slot && !slot.hasItem()) {
            g.setTooltipForNextFrame(font, FemboyGui.slotName(slot.slotId()), mouseX, mouseY);
            return;
        }
        super.extractTooltip(g, mouseX, mouseY);
    }
}
