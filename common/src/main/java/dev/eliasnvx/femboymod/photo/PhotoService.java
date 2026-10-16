package dev.eliasnvx.femboymod.photo;

import dev.architectury.networking.NetworkManager;
import dev.eliasnvx.femboymod.config.CommonConfig;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.network.PhotoPayloads;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Server side of printed photos (config {@code photos}). A selfie upload costs paper, respects a cooldown and must
 * decode as a valid picture; the photo item only carries an id, the picture lives in {@link PhotoStore}. Picture
 * requests are rate limited per player.
 */
public final class PhotoService {

    private static final int TICKS_PER_SECOND = 20;
    private static final Map<UUID, Long> LAST_PRINT = new HashMap<>();
    /** Requests per player in the current second: [second, count]. */
    private static final Map<UUID, long[]> REQUESTS = new HashMap<>();

    private PhotoService() {
    }

    public static void upload(ServerPlayer player, byte[] data) {
        CommonConfig.Photos config = FemboyConfig.common().photos();
        if (!config.enabled()) {
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_PRINT.get(player.getUUID());
        if (last != null && now - last < config.cooldownTicks()) {
            return; // the client shows the selfie anyway; just no second print
        }
        if (PhotoCodec.decode(data).isEmpty()) {
            return;
        }
        boolean free = player.getAbilities().instabuild || config.paperCost() == 0;
        if (!free && !takePaper(player.getInventory(), config.paperCost())) {
            player.displayClientMessage(Component.translatable("message.femboymod.photo.no_paper", config.paperCost()), true);
            return;
        }
        LAST_PRINT.put(player.getUUID(), now);
        UUID id = UUID.randomUUID();
        PhotoStore.save(player.getServer(), id, data);
        ItemStack photo = new ItemStack(FemboyItems.PHOTO.get());
        photo.set(FemboyComponents.PHOTO.get(), new PhotoData(id, player.getGameProfile().getName()));
        player.getInventory().placeItemBackInInventory(photo); // drops it if the inventory is full
        player.displayClientMessage(Component.translatable("message.femboymod.photo.printed"), true);
    }

    /** Takes {@code count} paper if the player has that much; otherwise takes nothing. */
    static boolean takePaper(Inventory inventory, int count) {
        int have = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Items.PAPER)) {
                have += inventory.getItem(slot).getCount();
            }
        }
        if (have < count) {
            return false;
        }
        int left = count;
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.PAPER)) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
        inventory.setChanged();
        return true;
    }

    public static void request(ServerPlayer player, UUID id) {
        long second = player.level().getGameTime() / TICKS_PER_SECOND;
        long[] window = REQUESTS.computeIfAbsent(player.getUUID(), uuid -> new long[]{second, 0});
        if (window[0] != second) {
            window[0] = second;
            window[1] = 0;
        }
        if (++window[1] > FemboyConfig.common().photos().requestsPerSecond()) {
            return; // the client asks again later
        }
        MinecraftServer server = player.getServer();
        PhotoStore.load(server, id).thenAccept(data -> server.execute(() -> {
            if (!player.hasDisconnected()) {
                NetworkManager.sendToPlayer(player, new PhotoPayloads.Picture(id, data.orElse(new byte[0])));
            }
        }));
    }

    public static void forget(ServerPlayer player) {
        LAST_PRINT.remove(player.getUUID());
        REQUESTS.remove(player.getUUID());
    }
}
