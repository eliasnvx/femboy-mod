package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.entity.EquipmentSlot;

/** C2S: cycle how one of your armor pieces is drawn (auto → hide → show). Only the look changes. */
public record CycleArmorVisibilityPayload(EquipmentSlot armorSlot) implements CustomPacketPayload {

    public static final Type<CycleArmorVisibilityPayload> TYPE =
            new Type<>(new ResourceLocation(FemboyMod.MOD_ID, "cycle_armor_visibility"));

    /** 1.21.1 has no EquipmentSlot stream codec; an unknown id decodes to MAINHAND, which the handler rejects. */
    public static final StreamCodec<ByteBuf, CycleArmorVisibilityPayload> STREAM_CODEC =
            ByteBufCodecs.idMapper(
                    ByIdMap.continuous(EquipmentSlot::ordinal, EquipmentSlot.values(), ByIdMap.OutOfBoundsStrategy.ZERO),
                    EquipmentSlot::ordinal)
                    .map(CycleArmorVisibilityPayload::new, CycleArmorVisibilityPayload::armorSlot);

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
