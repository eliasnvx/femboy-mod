package dev.eliasnvx.femboymod.tools;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.eliasnvx.femboymod.client.render.model.CosmeticModels;
import dev.eliasnvx.femboymod.client.render.model.Groups;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Dev tool: exports every code cosmetic model ({@link CosmeticModels}) as a STARTING Blockbench/GeckoLib
 * model: {@code <item>.geo.json} plus a flat-colored texture, into {@code art/blockbench/<item>/}.
 * Nothing goes into {@code src/}: an artist polishes the files first (docs/art/blockbench.md, SPEC §11).
 *
 * <p>Run: {@code ./gradlew :common:exportBlockbenchStarters}. Conversion: Java model space (y down, neck
 * at y=0) to Bedrock (y up, neck at y=24): positions keep x and z, y becomes {@code 24 - y}; rotations
 * negate x and y and keep z (Java entity models render with a (-1, -1, 1) scale, GeckoLib models don't).
 * Checked in-game by comparing geo and code renders of the ears, hair clip, tail and skirt.
 */
public final class BlockbenchStarterExport {

    private static final int TEXTURE_SIZE = 64;
    private static final int ZONE = 4;
    private static final float NECK_Y = 24.0F;
    private static final Map<String, String> ARMOR_BONES = Map.of(
            "head", "armorHead", "body", "armorBody", "left_arm", "armorLeftArm",
            "right_arm", "armorRightArm", "left_leg", "armorLeftLeg", "right_leg", "armorRightLeg");
    private static final Map<String, Integer> ARMOR_ORDER = Map.of(
            "armorHead", 0, "armorBody", 1, "armorLeftArm", 2, "armorRightArm", 3, "armorLeftLeg", 4, "armorRightLeg", 5);
    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};

    /** Code layer -> item id + default colors (main, detail); same look as the built-in renderers. */
    private record Target(String item, int main, int detail) {
    }

    private static final Map<String, Target> TARGETS = new LinkedHashMap<>();

    static {
        TARGETS.put("cat_ears", new Target("cat_ears", 0xF291BE, 0xFFF6F8));
        TARGETS.put("tail", new Target("tail", 0xF291BE, 0xFFF6F8));
        TARGETS.put("socks", new Target("programming_socks", 0xF5A9B8, 0xFF8FB8));
        TARGETS.put("fishnet", new Target("fishnet_tights", 0x2A2A2A, 0xFF8FB8));
        TARGETS.put("skirt", new Target("pleated_skirt", 0x3A3A48, 0xF4F1EE));
        TARGETS.put("hoodie", new Target("oversized_hoodie", 0xC8A2E8, 0xF4F1EE));
        TARGETS.put("cat_ear_hoodie", new Target("cat_ear_hoodie", 0xC8A2E8, 0xF4F1EE));
        TARGETS.put("choker", new Target("uwu_choker", 0x2A2A33, 0xFFF6F8));
        TARGETS.put("backpack", new Target("backpack", 0xD9C9A3, 0x62B14F));
        // hair_clip_<shape> layers map to items of the same name (added below)
    }

    private BlockbenchStarterExport() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "art/blockbench");
        for (Map.Entry<ModelLayerLocation, Supplier<LayerDefinition>> entry : CosmeticModels.definitions().entrySet()) {
            String layer = entry.getKey().model().getPath();
            Target target = TARGETS.get(layer);
            if (target == null && layer.startsWith("hair_clip_")) {
                target = new Target(layer, 0xFF8FB8, 0xFFF6F8);
            }
            if (target == null) {
                System.out.println("skip " + layer + " (helper layer, no item of its own)");
                continue;
            }
            ModelPart root = entry.getValue().get().bakeRoot();
            // Procedural models get their motionless pose; geo models have no procedural motion yet.
            if (entry.getKey().equals(CosmeticModels.TAIL)) {
                CosmeticModels.TailModel.applyRestPose(root);
            } else if (entry.getKey().equals(CosmeticModels.SKIRT)) {
                CosmeticModels.SkirtModel.applyRestPose(root);
            }
            export(root, target, out.resolve(target.item()));
        }
    }

    private static void export(ModelPart root, Target target, Path dir) throws IOException {
        Map<Groups, int[]> zones = zones();
        Map<String, JsonObject> bones = new LinkedHashMap<>();
        Set<String> usedNames = new LinkedHashSet<>();
        int[] cubeCount = {0};
        collect(root, "", null, 0, 0, 0, null, zones, bones, usedNames, cubeCount);

        List<JsonObject> ordered = new ArrayList<>(bones.values());
        ordered.sort((a, b) -> Integer.compare(order(a), order(b)));
        JsonArray boneArray = new JsonArray();
        ordered.stream().filter(bone -> bone.has("cubes") || isArmor(bone) || hasChildren(bone, ordered))
                .forEach(boneArray::add);

        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry.femboymod." + target.item());
        description.addProperty("texture_width", TEXTURE_SIZE);
        description.addProperty("texture_height", TEXTURE_SIZE);
        description.addProperty("visible_bounds_width", 3);
        description.addProperty("visible_bounds_height", 3);
        description.add("visible_bounds_offset", array(0, 1.5F, 0));
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", boneArray);
        JsonArray geometries = new JsonArray();
        geometries.add(geometry);
        JsonObject file = new JsonObject();
        file.addProperty("format_version", "1.12.0");
        file.add("minecraft:geometry", geometries);

        Files.createDirectories(dir);
        Files.writeString(dir.resolve(target.item() + ".geo.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(file));
        ImageIO.write(texture(zones, target, false), "png", dir.resolve(target.item() + ".png").toFile());
        ImageIO.write(texture(zones, target, true), "png", dir.resolve(target.item() + "_dyeable.png").toFile());
        System.out.printf(Locale.ROOT, "%s: %d bones, %d cubes -> %s%n", target.item(), boneArray.size(), cubeCount[0], dir);
    }

    /**
     * Walks the part tree. Color holders ({@code g_<group>}, {@code g_band<i>}) are not bones: their cubes
     * go to the parent bone with that group's UV zone. {@code absX/Y/Z} is the part's pivot in Java space.
     */
    private static void collect(ModelPart part, String name, String parentBone, float absX, float absY, float absZ,
                                Groups group, Map<Groups, int[]> zones, Map<String, JsonObject> bones,
                                Set<String> usedNames, int[] cubeCount) {
        String bone = parentBone;
        if (!name.isEmpty() && !name.startsWith("g_")) {
            bone = boneName(name, parentBone, usedNames);
            JsonObject json = new JsonObject();
            json.addProperty("name", bone);
            if (parentBone != null) {
                json.addProperty("parent", parentBone);
            }
            json.add("pivot", array(absX, NECK_Y - absY, absZ));
            if (part.xRot != 0 || part.yRot != 0 || part.zRot != 0) {
                json.add("rotation", array((float) -Math.toDegrees(part.xRot), (float) -Math.toDegrees(part.yRot),
                        (float) Math.toDegrees(part.zRot)));
            }
            bones.put(bone, json);
        }
        if (bone != null && group != null) {
            JsonObject json = bones.get(bone);
            JsonArray cubes = json.has("cubes") ? json.getAsJsonArray("cubes") : new JsonArray();
            for (ModelPart.Cube cube : BlockbenchStarterExport.<List<ModelPart.Cube>>field(part, "cubes")) {
                cubes.add(cube(cube, absX, absY, absZ, zones.get(group)));
                cubeCount[0]++;
            }
            if (!cubes.isEmpty()) {
                json.add("cubes", cubes);
            }
        }
        for (String child : childNames(part)) {
            ModelPart c = part.getChild(child);
            Groups childGroup = groupOf(child);
            if (child.startsWith("g_") && (c.x != 0 || c.y != 0 || c.z != 0 || c.xRot != 0 || c.yRot != 0 || c.zRot != 0)) {
                throw new IllegalStateException("Color holder with a pose: " + child);
            }
            collect(c, child, bone, absX + c.x, absY + c.y, absZ + c.z, childGroup, zones, bones, usedNames, cubeCount);
        }
    }

    private static String boneName(String name, String parentBone, Set<String> used) {
        String base = parentBone == null ? ARMOR_BONES.getOrDefault(name, name) : name;
        String unique = base;
        for (int i = 2; !used.add(unique); i++) {
            unique = base + "_" + i;
        }
        return unique;
    }

    private static Groups groupOf(String holder) {
        if (holder.startsWith("g_band")) {
            return Groups.MAIN;
        }
        for (Groups group : Groups.values()) {
            if (group.holderName().equals(holder)) {
                return group;
            }
        }
        return null;
    }

    private static List<String> childNames(ModelPart part) {
        return new ArrayList<>(BlockbenchStarterExport.<Map<String, ModelPart>>field(part, "children").keySet());
    }

    /** Dev tool only: ModelPart keeps its cubes and children private. */
    @SuppressWarnings("unchecked")
    private static <T> T field(ModelPart part, String name) {
        try {
            var field = ModelPart.class.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(part);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ModelPart." + name + " not found; update the exporter", e);
        }
    }

    private static JsonObject cube(ModelPart.Cube cube, float px, float py, float pz, int[] zone) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (ModelPart.Polygon polygon : cube.polygons) {
            for (ModelPart.Vertex v : polygon.vertices()) {
                minX = Math.min(minX, v.x());
                minY = Math.min(minY, v.y());
                minZ = Math.min(minZ, v.z());
                maxX = Math.max(maxX, v.x());
                maxY = Math.max(maxY, v.y());
                maxZ = Math.max(maxZ, v.z());
            }
        }
        JsonObject json = new JsonObject();
        json.add("origin", array(px + minX, NECK_Y - (py + maxY), pz + minZ));
        json.add("size", array(maxX - minX, maxY - minY, maxZ - minZ));
        JsonObject uv = new JsonObject();
        for (String face : FACES) {
            JsonObject f = new JsonObject();
            f.add("uv", array(zone[0] + 1, zone[1] + 1));
            f.add("uv_size", array(1, 1));
            uv.add(face, f);
        }
        json.add("uv", uv);
        return json;
    }

    private static Map<Groups, int[]> zones() {
        Map<Groups, int[]> zones = new EnumMap<>(Groups.class);
        int u = 0;
        for (Groups group : Groups.values()) {
            if (group != Groups.BANDED) {
                zones.put(group, new int[]{u, 0});
                u += ZONE;
            }
        }
        return zones;
    }

    private static BufferedImage texture(Map<Groups, int[]> zones, Target target, boolean dyeable) {
        Map<Groups, Integer> colors = new EnumMap<>(Groups.class);
        colors.put(Groups.MAIN, dyeable ? 0xFFFFFF : target.main());
        colors.put(Groups.ACCENT, dyeable ? 0xD8D8D8 : shade(target.main(), 1.15F));
        colors.put(Groups.DETAIL, target.detail());
        colors.put(Groups.DARK, 0x2A2328);
        colors.put(Groups.METAL, 0xF2C94C);
        BufferedImage image = new BufferedImage(TEXTURE_SIZE, TEXTURE_SIZE, BufferedImage.TYPE_INT_ARGB);
        zones.forEach((group, zone) -> {
            for (int y = 0; y < ZONE; y++) {
                for (int x = 0; x < ZONE; x++) {
                    image.setRGB(zone[0] + x, zone[1] + y, 0xFF000000 | colors.get(group));
                }
            }
        });
        return image;
    }

    private static int shade(int rgb, float factor) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
        return r << 16 | g << 8 | b;
    }

    private static boolean isArmor(JsonObject bone) {
        return ARMOR_ORDER.containsKey(bone.get("name").getAsString());
    }

    private static boolean hasChildren(JsonObject bone, List<JsonObject> all) {
        String name = bone.get("name").getAsString();
        return all.stream().anyMatch(other -> other.has("parent") && other.get("parent").getAsString().equals(name));
    }

    private static int order(JsonObject bone) {
        return ARMOR_ORDER.getOrDefault(bone.get("name").getAsString(), bone.has("parent") ? 100 : 50);
    }

    private static JsonArray array(float... values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(Math.round(value * 10000.0F) / 10000.0F);
        }
        return array;
    }
}
