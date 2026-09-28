package dev.eliasnvx.femboymod.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.client.render.ColorwayClock;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Colorway tinting of item icons on 1.21.1. 26.3 used item model definitions ({@code assets/<ns>/items/*.json})
 * with the {@code femboymod:colorway} tint source; 1.21.1 has no such definitions, so this reads the same files
 * on resource reload and serves their {@code tints} lists (index = model layer = tint index) through an
 * {@link net.minecraft.client.color.item.ItemColor} registered for every femboymod item.
 * <p>
 * Supported tint types: {@code femboymod:colorway} ({@code default}, {@code stripe}) and {@code minecraft:constant}
 * ({@code value}); anything else draws untinted.
 */
public final class ItemTints {

    private static final String DIRECTORY = "items";
    private static final String COLORWAY_TYPE = FemboyMod.MOD_ID + ":colorway";
    private static final String CONSTANT_TYPE = "minecraft:constant";
    private static final int UNTINTED = -1;

    /** One entry of a {@code tints} list. */
    private record Tint(boolean colorway, int color, int stripe) {
        int calculate(ItemStack stack) {
            if (!colorway) {
                return color;
            }
            Colorway value = Colorways.effective(stack).orElse(null);
            return FastColor.ARGB32.opaque(value == null ? color : value.stripeColor(stripe, ColorwayClock.ticks()));
        }
    }

    private static volatile Map<Item, Tint[]> tints = Map.of();

    private ItemTints() {
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        List<RegistrySupplier<Item>> items = new ArrayList<>();
        FemboyItems.REGISTER.forEach(items::add);
        ColorHandlerRegistry.registerItemColors(ItemTints::color, items.toArray(RegistrySupplier[]::new));
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new Loader(),
                new ResourceLocation(FemboyMod.MOD_ID, "item_tints"));
    }

    private static int color(ItemStack stack, int tintIndex) {
        Tint[] list = tints.get(stack.getItem());
        if (list == null || tintIndex < 0 || tintIndex >= list.length || list[tintIndex] == null) {
            return UNTINTED;
        }
        return list[tintIndex].calculate(stack);
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {

        Loader() {
            super(new Gson(), DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<Item, Tint[]> loaded = new IdentityHashMap<>();
            files.forEach((id, json) -> {
                if (!FemboyMod.MOD_ID.equals(id.getNamespace()) || !BuiltInRegistries.ITEM.containsKey(id)) {
                    return;
                }
                try {
                    JsonArray list = findTints(json);
                    if (list != null) {
                        loaded.put(BuiltInRegistries.ITEM.get(id), parse(list));
                    }
                } catch (RuntimeException e) {
                    FemboyMod.LOGGER.error("Bad item tints in {}", id, e);
                }
            });
            tints = loaded;
        }

        /** First {@code tints} array anywhere in the definition (all femboymod items use a plain model). */
        private static JsonArray findTints(JsonElement json) {
            if (json.isJsonObject()) {
                JsonObject object = json.getAsJsonObject();
                if (object.has("tints") && object.get("tints").isJsonArray()) {
                    return object.getAsJsonArray("tints");
                }
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    JsonArray found = findTints(entry.getValue());
                    if (found != null) {
                        return found;
                    }
                }
            } else if (json.isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray()) {
                    JsonArray found = findTints(element);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }

        private static Tint[] parse(JsonArray list) {
            Tint[] result = new Tint[list.size()];
            for (int i = 0; i < result.length; i++) {
                JsonObject tint = GsonHelper.convertToJsonObject(list.get(i), "tint");
                String type = GsonHelper.getAsString(tint, "type");
                if (COLORWAY_TYPE.equals(type)) {
                    result[i] = new Tint(true, GsonHelper.getAsInt(tint, "default"), GsonHelper.getAsInt(tint, "stripe", 0));
                } else if (CONSTANT_TYPE.equals(type)) {
                    result[i] = new Tint(false, GsonHelper.getAsInt(tint, "value"), 0);
                }
            }
            return result;
        }
    }
}
