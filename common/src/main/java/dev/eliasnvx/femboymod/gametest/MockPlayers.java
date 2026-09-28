package dev.eliasnvx.femboymod.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 1.20.1's {@code GameTestHelper#makeMockServerPlayerInLevel} gives the player a connection without a netty
 * channel; Forge's login hooks then crash sending registry packets. This copy attaches the connection to an
 * {@link EmbeddedChannel} (what vanilla itself does from 1.20.5 on), so the player works on every loader.
 */
final class MockPlayers {

    private static final String NAME = "test-mock-player";

    private MockPlayers() {
    }

    static ServerPlayer create(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), NAME)) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection); // activates the connection: it now has a channel
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        return player;
    }
}
