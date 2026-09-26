package dev.eliasnvx.femboymod.fabric.gametest;

import com.mojang.authlib.GameProfile;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.cosmetic.CosmeticInventory;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyComponents;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Other players see your look: a dedicated server, our client, and a second server-side player ("Friend",
 * joined over an in-memory connection like vanilla's mock game test player). Checks both sync paths:
 * a change while the client already tracks Friend, and tracking starting again after Friend walked away.
 */
public final class MultiplayerLookClientTest implements FabricClientGameTest {

    private static final String FRIEND = "Friend";
    private static final int SYNC_TIMEOUT = 100;
    private static final int FAR = 400;
    private static final int HOODIE_COLOR = 0xF7B8D2;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamerule advance_time false");

            // 1. Friend joins next to us and gets dressed: the change reaches everyone tracking Friend
            server.runOnServer(s -> {
                ServerPlayer friend = join(s);
                ServerPlayer me = me(s);
                friend.teleportTo(me.level(), me.getX(), me.getY(), me.getZ() + 3, Set.of(), 180, 0, false);
                CosmeticsManager.set(friend, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.CAT_EARS.get()));
                CosmeticsManager.set(friend, FemboySlots.OUTFIT_TOP, colored(FemboyItems.OVERSIZED_HOODIE.get(), HOODIE_COLOR));
                CosmeticsManager.set(friend, FemboySlots.OUTFIT_BOTTOM, new ItemStack(FemboyItems.PLEATED_SKIRT.get()));
                CosmeticsManager.set(friend, FemboySlots.LEGS_OVERLAY, new ItemStack(FemboyItems.PROGRAMMING_SOCKS.get()));
                CosmeticsManager.set(friend, FemboySlots.TAIL, new ItemStack(FemboyItems.TAIL.get()));
            });
            waitUntil(context, "Friend's outfit after a change", inv ->
                    inv.get(FemboySlots.HEAD_ACCESSORY).is(FemboyItems.CAT_EARS.get())
                            && inv.get(FemboySlots.OUTFIT_BOTTOM).is(FemboyItems.PLEATED_SKIRT.get())
                            && inv.get(FemboySlots.LEGS_OVERLAY).is(FemboyItems.PROGRAMMING_SOCKS.get())
                            && inv.get(FemboySlots.TAIL).is(FemboyItems.TAIL.get())
                            && hasColor(inv.get(FemboySlots.OUTFIT_TOP), HOODIE_COLOR));

            // 2. Friend walks out of range and swaps ears there; coming back starts tracking again
            server.runOnServer(s -> {
                ServerPlayer friend = friend(s);
                friend.teleportTo((ServerLevel) friend.level(), friend.getX() + FAR, friend.getY(), friend.getZ(), Set.of(), 180, 0, false);
            });
            context.waitFor(mc -> find(mc) == null, SYNC_TIMEOUT);
            server.runOnServer(s -> {
                ServerPlayer friend = friend(s);
                CosmeticsManager.set(friend, FemboySlots.HEAD_ACCESSORY, new ItemStack(FemboyItems.FOX_EARS.get()));
                ServerPlayer me = me(s);
                friend.teleportTo(me.level(), me.getX(), me.getY(), me.getZ() + 3, Set.of(), 180, 0, false);
            });
            waitUntil(context, "Friend's outfit after tracking starts again", inv ->
                    inv.get(FemboySlots.HEAD_ACCESSORY).is(FemboyItems.FOX_EARS.get())
                            && inv.get(FemboySlots.OUTFIT_BOTTOM).is(FemboyItems.PLEATED_SKIRT.get())
                            && hasColor(inv.get(FemboySlots.OUTFIT_TOP), HOODIE_COLOR));

            // 3. Hiding a piece syncs too
            server.runOnServer(s -> CosmeticsManager.setHidden(friend(s), FemboySlots.TAIL, true));
            waitUntil(context, "Friend's hidden tail", inv -> inv.isHidden(FemboySlots.TAIL));
            server.runOnServer(s -> CosmeticsManager.setHidden(friend(s), FemboySlots.TAIL, false));
            waitUntil(context, "Friend's tail shown again", inv -> !inv.isHidden(FemboySlots.TAIL));

            // Look at Friend for a screenshot
            server.runOnServer(s -> {
                ServerPlayer me = me(s);
                me.teleportTo(me.level(), me.getX(), me.getY(), me.getZ(), Set.of(), 0, 15, false);
            });
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            context.waitTicks(20);
            context.takeScreenshot(TestScreenshotOptions.of("multiplayer_friend_look"));

            server.runOnServer(s -> s.getPlayerList().remove(friend(s)));
        }
    }

    private static ServerPlayer join(MinecraftServer server) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), FRIEND);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer friend = new ServerPlayer(server, server.overworld(), profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, friend, cookie);
        return friend;
    }

    private static ServerPlayer friend(MinecraftServer server) {
        return server.getPlayerList().getPlayerByName(FRIEND);
    }

    private static ServerPlayer me(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .filter(p -> !p.getName().getString().equals(FRIEND))
                .findFirst().orElseThrow();
    }

    private static CosmeticInventory find(Minecraft mc) {
        return mc.level == null ? null : mc.level.players().stream()
                .filter(p -> p.getName().getString().equals(FRIEND))
                .findFirst().map(CosmeticsManager::get).orElse(null);
    }

    private static void waitUntil(ClientGameTestContext context, String what, java.util.function.Predicate<CosmeticInventory> check) {
        try {
            context.waitFor(mc -> {
                CosmeticInventory inv = find(mc);
                return inv != null && check.test(inv);
            }, SYNC_TIMEOUT);
        } catch (RuntimeException e) {
            throw new AssertionError("Client never saw " + what, e);
        }
    }

    private static ItemStack colored(Item item, int rgb) {
        ItemStack stack = new ItemStack(item);
        stack.set(FemboyComponents.COLORWAY.get(), Colorway.solid(rgb));
        return stack;
    }

    private static boolean hasColor(ItemStack stack, int rgb) {
        return Colorway.solid(rgb).equals(stack.get(FemboyComponents.COLORWAY.get()));
    }
}
