package dev.eliasnvx.femboymod.fabric.gametest;

import dev.eliasnvx.femboymod.client.ConfigScreen;
import dev.eliasnvx.femboymod.client.FemboyModClient;
import dev.eliasnvx.femboymod.client.GlowHostilesClient;
import dev.eliasnvx.femboymod.config.FemboyConfig;
import dev.eliasnvx.femboymod.config.ServerSettings;
import dev.eliasnvx.femboymod.cosmetic.ArmorHiding;
import dev.eliasnvx.femboymod.cosmetic.PvpCombat;
import dev.eliasnvx.femboymod.social.HeadpatConsent;
import dev.eliasnvx.femboymod.social.Headpats;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
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
    /** The chest piece in {@link ArmorHiding#mask}. */
    private static final int CHEST_BIT = 1 << ArmorHiding.ARMOR_SLOTS.indexOf(EquipmentSlot.CHEST);

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

            // 4. Others see Friend's backpack, not what's inside
            server.runOnServer(s -> {
                ItemStack backpack = new ItemStack(FemboyItems.CANVAS_BACKPACK.get());
                backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
                CosmeticsManager.set(friend(s), FemboySlots.BACK, backpack);
            });
            waitUntil(context, "Friend's backpack", inv -> inv.get(FemboySlots.BACK).is(FemboyItems.CANVAS_BACKPACK.get()));
            if (context.computeOnClient(mc -> find(mc).get(FemboySlots.BACK).has(DataComponents.CONTAINER))) {
                throw new AssertionError("another player's backpack contents reached this client");
            }

            // 5. The server's settings reach the client, and so do changes (the set bonus game rule)
            server.runCommand("gamerule femboymod:set_bonuses false");
            context.waitFor(mc -> !ServerSettings.client().setBonuses(), SYNC_TIMEOUT);
            server.runCommand("gamerule femboymod:set_bonuses true");
            context.waitFor(mc -> ServerSettings.client().setBonuses(), SYNC_TIMEOUT);

            // 6. Armor hidden under the hoodie shows while Friend fights another player
            server.runOnServer(s -> friend(s).setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE)));
            context.waitFor(mc -> friendPlayer(mc) != null && (ArmorHiding.cachedMask(friendPlayer(mc)) & CHEST_BIT) != 0, SYNC_TIMEOUT);
            server.runOnServer(s -> PvpCombat.fight(friend(s), me(s)));
            try {
                context.waitFor(mc -> (ArmorHiding.cachedMask(friendPlayer(mc)) & CHEST_BIT) == 0, SYNC_TIMEOUT);
            } catch (RuntimeException e) {
                throw new AssertionError("Friend's hidden chestplate didn't show during the fight", e);
            }

            // 7. Heart Glasses (no team): Friend glows in plain sight, not through a wall
            server.runOnServer(s -> CosmeticsManager.set(me(s), FemboySlots.FACE, new ItemStack(FemboyItems.HEART_GLASSES.get())));
            context.waitFor(mc -> friendPlayer(mc) != null && GlowHostilesClient.shouldGlow(friendPlayer(mc)), SYNC_TIMEOUT);
            server.runOnServer(s -> {
                ServerPlayer me = me(s);
                BlockPos wall = me.blockPosition().south(2);
                for (int x = -2; x <= 2; x++) {
                    for (int y = 0; y <= 2; y++) {
                        me.level().setBlockAndUpdate(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState());
                    }
                }
            });
            try {
                context.waitFor(mc -> !GlowHostilesClient.shouldGlow(friendPlayer(mc)), SYNC_TIMEOUT);
            } catch (RuntimeException e) {
                throw new AssertionError("Heart Glasses showed Friend through a wall", e);
            }
            server.runOnServer(s -> {
                ServerPlayer me = me(s);
                BlockPos wall = me.blockPosition().south(2);
                for (int x = -2; x <= 2; x++) {
                    for (int y = 0; y <= 2; y++) {
                        me.level().setBlockAndUpdate(wall.offset(x, y, 0), Blocks.AIR.defaultBlockState());
                    }
                }
                CosmeticsManager.set(me, FemboySlots.FACE, ItemStack.EMPTY);
                friend(s).setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            });

            // 8. Headpat consent: the option changed on this client reaches the server, which turns Friend's headpat away
            setHeadpats(context, server, HeadpatConsent.NOBODY);
            if (server.computeOnServer(s -> Headpats.pat((ServerLevel) me(s).level(), friend(s), me(s)))) {
                throw new AssertionError("Friend's headpat landed although this player takes none");
            }
            context.setScreen(() -> new ConfigScreen(null));
            context.waitTicks(5);
            // the list scrolls: down to the last option, the cursor over the list
            context.getInput().setCursorPos(context.computeOnClient(mc -> mc.getWindow().getScreenWidth()) / 2.0,
                    context.computeOnClient(mc -> mc.getWindow().getScreenHeight()) / 2.0);
            context.getInput().scroll(-20);
            context.waitTicks(5);
            context.takeScreenshot(TestScreenshotOptions.of("multiplayer_config_headpats"));
            context.setScreen(() -> null);
            setHeadpats(context, server, HeadpatConsent.EVERYONE);
            if (!server.computeOnServer(s -> Headpats.pat((ServerLevel) me(s).level(), friend(s), me(s)))) {
                throw new AssertionError("Friend's headpat didn't land after allowing everyone again");
            }

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

    private static void setHeadpats(ClientGameTestContext context, TestDedicatedServerContext server, HeadpatConsent consent) {
        context.runOnClient(mc -> {
            FemboyConfig.setClient(FemboyConfig.client().withHeadpats(consent));
            FemboyModClient.sendHeadpatConsent();
        });
        try {
            server.waitFor(s -> Headpats.consent(me(s)) == consent, SYNC_TIMEOUT);
        } catch (RuntimeException e) {
            throw new AssertionError("The server never heard headpats_from_players = " + consent.getSerializedName(), e);
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

    private static net.minecraft.world.entity.player.Player friendPlayer(Minecraft mc) {
        return mc.level == null ? null : mc.level.players().stream()
                .filter(p -> p.getName().getString().equals(FRIEND))
                .findFirst().orElse(null);
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
