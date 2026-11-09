package dev.eliasnvx.femboymod.config;

import dev.eliasnvx.femboymod.world.FemboyGameRules;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The server's settings that clients act on too: whether set bonuses are on (config and game rule together), which
 * cosmetic slots are disabled, what a Style Coupon costs. The common config lives on the server; a client shows and
 * predicts with the server's values, not with its own config file ({@code ServerSettingsPayload}, sent on join and
 * whenever they change). In singleplayer both sides read the same config anyway.
 */
public record ServerSettings(boolean setBonuses, List<Identifier> disabledSlots, int couponCost) {

    public static final StreamCodec<ByteBuf, ServerSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ServerSettings::setBonuses,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), ServerSettings::disabledSlots,
            ByteBufCodecs.VAR_INT, ServerSettings::couponCost,
            ServerSettings::new);

    /** The client's copy of the server's settings; null when not connected. */
    private static volatile @Nullable ServerSettings onClient;

    public ServerSettings {
        disabledSlots = List.copyOf(disabledSlots);
    }

    /** What this server says (its config and game rules). */
    public static ServerSettings of(MinecraftServer server) {
        CommonConfig config = FemboyConfig.common();
        boolean sets = config.setBonusesEnabled() && server.overworld().getGameRules().get(FemboyGameRules.SET_BONUSES.get());
        return new ServerSettings(sets, config.disabledSlots(), config.stylePoints().couponCost());
    }

    /** This side's config, for a client that hasn't heard from a server (menus before joining). */
    public static ServerSettings local() {
        CommonConfig config = FemboyConfig.common();
        return new ServerSettings(config.setBonusesEnabled(), config.disabledSlots(), config.stylePoints().couponCost());
    }

    /** Disabled cosmetic slots in {@code level}: the config on the server, the server's copy on a client. */
    public static List<Identifier> disabledSlots(Level level) {
        return level.isClientSide() ? client().disabledSlots() : FemboyConfig.common().disabledSlots();
    }

    public static ServerSettings client() {
        ServerSettings settings = onClient;
        return settings != null ? settings : local();
    }

    public static void setClient(@Nullable ServerSettings settings) {
        onClient = settings;
    }
}
