package dev.eliasnvx.femboymod.client.chat;

import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.client.ChatTransformer;
import dev.eliasnvx.femboymod.api.cosmetic.FemboySlots;
import dev.eliasnvx.femboymod.chat.UwuRules;
import dev.eliasnvx.femboymod.chat.UwuTransformer;
import dev.eliasnvx.femboymod.cosmetic.CosmeticsManager;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** The UwU choker's chat transformer ({@code femboymod:uwu}): active while the choker is worn. */
public final class UwuChat implements ChatTransformer {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FemboyMod.MOD_ID, "uwu");
    private static volatile Map<String, UwuRules> rules = Map.of();

    @Override
    public String transform(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || rules.isEmpty() || !dev.eliasnvx.femboymod.config.FemboyConfig.client().uwuChat()
                || !CosmeticsManager.get(player).get(FemboySlots.NECK).is(FemboyItems.UWU_CHOKER.get())) {
            return message;
        }
        String language = UwuTransformer.pickLanguage(message, Minecraft.getInstance().getLanguageManager().getSelected(), rules);
        UwuRules set = rules.getOrDefault(language, rules.get("en_us"));
        if (set == null) {
            return message;
        }
        // Seeded by message and time: repeated messages vary, but one message is transformed deterministically.
        Random random = new Random(message.hashCode() * 31L + player.tickCount);
        return UwuTransformer.transform(message, set, random, SharedConstants.MAX_CHAT_LENGTH);
    }

    /** Loads {@code assets/<ns>/femboymod/chat_transform/<language>.json}; later packs override earlier ones. */
    public static final class Loader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();

        public Loader() {
            super(GSON, "femboymod/chat_transform");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> loaded, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, UwuRules> byLanguage = new HashMap<>();
            loaded.forEach((id, json) -> UwuRules.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> FemboyMod.LOGGER.warn("Bad UwU chat rules {}: {}", id, error))
                    .ifPresent(value -> byLanguage.put(id.getPath(), value)));
            rules = Map.copyOf(byLanguage);
            FemboyMod.LOGGER.debug("Loaded UwU chat rules for {}", byLanguage.keySet());
        }
    }
}
