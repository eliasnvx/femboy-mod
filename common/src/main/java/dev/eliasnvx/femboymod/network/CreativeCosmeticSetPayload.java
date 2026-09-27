package dev.eliasnvx.femboymod.network;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.menu.CosmeticPanelActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * C2S, creative only: put {@code stack} (or nothing) into a cosmetic slot. In the creative inventory the
 * cursor lives on the client, like vanilla's creative slot packet; creative players can create items anyway,
 * so the server only checks the game mode and that the item fits the slot.
 */
public record CreativeCosmeticSetPayload(ResourceLocation slot, ItemStack stack) implements CustomPacketPayload {

    public static final Type<CreativeCosmeticSetPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "creative_cosmetic_set"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeCosmeticSetPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CreativeCosmeticSetPayload::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, CreativeCosmeticSetPayload::stack,
            CreativeCosmeticSetPayload::new);

    @Override
    public Type<CreativeCosmeticSetPayload> type() {
        return TYPE;
    }

    public static void handle(CreativeCosmeticSetPayload payload, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                CosmeticPanelActions.creativeSet(player, payload.slot(), payload.stack());
            }
        });
    }
}
