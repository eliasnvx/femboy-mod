package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/** C2S: cycle how one of your armor pieces is drawn (auto → hide → show). Only the look changes. */
public record CycleArmorVisibilityPayload(EquipmentSlot armorSlot) implements CustomPacketPayload {

    public static final Type<CycleArmorVisibilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "cycle_armor_visibility"));

    public static final StreamCodec<ByteBuf, CycleArmorVisibilityPayload> STREAM_CODEC =
            EquipmentSlot.STREAM_CODEC.map(CycleArmorVisibilityPayload::new, CycleArmorVisibilityPayload::armorSlot);

    @Override
    public Type<CycleArmorVisibilityPayload> type() {
        return TYPE;
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
