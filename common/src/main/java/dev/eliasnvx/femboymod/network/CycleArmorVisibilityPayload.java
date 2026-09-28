package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/** C2S: cycle how one of your armor pieces is drawn (auto → hide → show). Only the look changes. */
public record CycleArmorVisibilityPayload(EquipmentSlot armorSlot) implements FemboyPacket {

    public static final ResourceLocation ID = new ResourceLocation(FemboyMod.MOD_ID, "cycle_armor_visibility");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(armorSlot.ordinal());
    }

    /** An unknown id decodes to MAINHAND, which the handler rejects. */
    public static CycleArmorVisibilityPayload read(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        EquipmentSlot[] values = EquipmentSlot.values();
        return new CycleArmorVisibilityPayload(id >= 0 && id < values.length ? values[id] : EquipmentSlot.MAINHAND);
    }

    public static void handle(CycleArmorVisibilityPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player && ArmorHiding.ARMOR_SLOTS.contains(payload.armorSlot())) {
                CosmeticsManager.setArmorVisibility(player, payload.armorSlot(),
                        CosmeticsManager.get(player).armorVisibility(payload.armorSlot()).next());
            }
        });
    }
}
