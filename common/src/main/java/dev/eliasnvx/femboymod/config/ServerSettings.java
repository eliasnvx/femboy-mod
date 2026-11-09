package dev.eliasnvx.femboymod.config;

import dev.eliasnvx.femboymod.world.FemboyGameRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The server's settings that clients act on too: whether set bonuses are on (config and game rule together), which
 * cosmetic slots are disabled, what a Style Coupon costs. The common config lives on the server; a client shows and
 * predicts with the server's values, not with its own config file ({@code ServerSettingsPayload}, sent on join and
 * whenever they change). In singleplayer both sides read the same config anyway.
 */
public record ServerSettings(boolean setBonuses, List<ResourceLocation> disabledSlots, int couponCost) {

    /** The client's copy of the server's settings; null when not connected. */
    private static volatile @Nullable ServerSettings onClient;

    public ServerSettings {
        disabledSlots = List.copyOf(disabledSlots);
    }

    /** Network form (1.20.1 has no stream codecs): the flag, the slot list, the price. */
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(setBonuses);
        buf.writeCollection(disabledSlots, FriendlyByteBuf::writeResourceLocation);
        buf.writeVarInt(couponCost);
    }

    public static ServerSettings read(FriendlyByteBuf buf) {
        boolean setBonuses = buf.readBoolean();
        List<ResourceLocation> disabledSlots = buf.readCollection(ArrayList::new, FriendlyByteBuf::readResourceLocation);
        return new ServerSettings(setBonuses, disabledSlots, buf.readVarInt());
    }

    /** What this server says (its config and game rules). */
    public static ServerSettings of(MinecraftServer server) {
        CommonConfig config = FemboyConfig.common();
        boolean sets = config.setBonusesEnabled() && server.overworld().getGameRules().getBoolean(FemboyGameRules.SET_BONUSES);
        return new ServerSettings(sets, config.disabledSlots(), config.stylePoints().couponCost());
    }

    /** This side's config, for a client that hasn't heard from a server (menus before joining). */
    public static ServerSettings local() {
        CommonConfig config = FemboyConfig.common();
        return new ServerSettings(config.setBonusesEnabled(), config.disabledSlots(), config.stylePoints().couponCost());
    }

    /** Disabled cosmetic slots in {@code level}: the config on the server, the server's copy on a client. */
    public static List<ResourceLocation> disabledSlots(Level level) {
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
