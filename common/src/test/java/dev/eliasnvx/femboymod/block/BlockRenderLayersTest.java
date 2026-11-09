package dev.eliasnvx.femboymod.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BlockRenderLayers} lists exactly the blocks whose models show transparent texture pixels: a face of an element
 * samples a pixel with alpha below 255 within its UV, or the model has no elements of ours (a vanilla parent such as
 * {@code cross}) and one of our textures has any. Without the listing such a block draws those pixels black on 1.21.1.
 */
class BlockRenderLayersTest {

    private static final String ASSETS = "/assets/femboymod/";
    private static final String NAMESPACE = "femboymod:";
    private static final int OPAQUE = 255;
    private static final double UNITS = 16.0;
    /** UV edges that sit on a pixel border must not pull in the neighboring pixel. */
    private static final double EDGE = 1.0E-4;

    private final Map<String, BufferedImage> textures = new HashMap<>();

    @Test
    void listedBlocksAreTheOnesWithTransparentPixels() throws IOException, URISyntaxException {
        Set<String> needed = new TreeSet<>();
        Set<String> blocks = new TreeSet<>();
        for (Path file : blockstates()) {
            String block = file.getFileName().toString().replace(".json", "");
            blocks.add(block);
            Set<String> models = new TreeSet<>();
            collectModels(JsonParser.parseString(Files.readString(file)), models);
            for (String model : models) {
                if (showsTransparency(model)) {
                    needed.add(block);
                }
            }
        }
        Set<String> listed = new TreeSet<>(BlockRenderLayers.CUTOUT);
        listed.addAll(BlockRenderLayers.CUTOUT_MIPPED);
        assertEquals(BlockRenderLayers.CUTOUT.size() + BlockRenderLayers.CUTOUT_MIPPED.size(), listed.size(), "a block is listed twice");
        Set<String> unknown = new TreeSet<>(listed);
        unknown.removeAll(blocks);
        assertTrue(unknown.isEmpty(), "listed but no such block: " + unknown);
        assertEquals(needed, listed, "blocks with transparent texture pixels vs BlockRenderLayers");
    }

    private static Iterable<Path> blockstates() throws IOException, URISyntaxException {
        URL folder = BlockRenderLayersTest.class.getResource(ASSETS + "blockstates");
        assertNotNull(folder, "blockstates folder");
        try (Stream<Path> files = Files.list(Path.of(folder.toURI()))) {
            return files.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static void collectModels(JsonElement json, Set<String> models) {
        if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                if (entry.getKey().equals("model") && entry.getValue().isJsonPrimitive()) {
                    models.add(entry.getValue().getAsString());
                } else {
                    collectModels(entry.getValue(), models);
                }
            }
        } else if (json.isJsonArray()) {
            json.getAsJsonArray().forEach(element -> collectModels(element, models));
        }
    }

    /** Whether any face of the model (with its parents of ours) samples a texture pixel that isn't opaque. */
    private boolean showsTransparency(String model) throws IOException {
        Map<String, String> variables = new HashMap<>();
        JsonArray elements = null;
        // child first: its textures win, the first elements found are the ones used
        for (String ref = model; ref != null && ref.startsWith(NAMESPACE); ) {
            JsonObject json = read(ASSETS + "models/" + ref.substring(NAMESPACE.length()) + ".json");
            if (json.has("textures")) {
                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
                    variables.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
                }
            }
            if (elements == null && json.has("elements")) {
                elements = json.getAsJsonArray("elements");
            }
            ref = json.has("parent") ? json.get("parent").getAsString() : null;
        }
        if (elements == null) {
            // a vanilla parent draws the textures: any of ours with a transparent pixel counts (but not the particle)
            for (Map.Entry<String, String> variable : variables.entrySet()) {
                if (variable.getKey().equals("particle")) {
                    continue;
                }
                String texture = resolve(variables, variable.getValue());
                if (texture.startsWith(NAMESPACE) && transparentIn(texture, 0, 0, UNITS, UNITS)) {
                    return true;
                }
            }
            return false;
        }
        for (JsonElement element : elements) {
            JsonObject box = element.getAsJsonObject();
            JsonArray from = box.getAsJsonArray("from");
            JsonArray to = box.getAsJsonArray("to");
            for (Map.Entry<String, JsonElement> face : box.getAsJsonObject("faces").entrySet()) {
                JsonObject data = face.getValue().getAsJsonObject();
                String texture = resolve(variables, data.get("texture").getAsString());
                if (!texture.startsWith(NAMESPACE)) {
                    continue;
                }
                double[] uv = data.has("uv") ? doubles(data.getAsJsonArray("uv")) : defaultUv(face.getKey(), doubles(from), doubles(to));
                if (transparentIn(texture, uv[0], uv[1], uv[2], uv[3])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String resolve(Map<String, String> variables, String value) {
        for (int depth = 0; value.startsWith("#") && depth < variables.size() + 1; depth++) {
            value = variables.getOrDefault(value.substring(1), value);
        }
        return value.contains(":") ? value : "minecraft:" + value;
    }

    /** Vanilla's UV for a face without one: the element's extent on that face. */
    private static double[] defaultUv(String face, double[] from, double[] to) {
        return switch (face) {
            case "north" -> new double[]{UNITS - to[0], UNITS - to[1], UNITS - from[0], UNITS - from[1]};
            case "south" -> new double[]{from[0], UNITS - to[1], to[0], UNITS - from[1]};
            case "east" -> new double[]{UNITS - to[2], UNITS - to[1], UNITS - from[2], UNITS - from[1]};
            case "west" -> new double[]{from[2], UNITS - to[1], to[2], UNITS - from[1]};
            case "up" -> new double[]{from[0], from[2], to[0], to[2]};
            default -> new double[]{from[0], UNITS - to[2], to[0], UNITS - from[2]};
        };
    }

    private static double[] doubles(JsonArray array) {
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsDouble();
        }
        return values;
    }

    /** Whether the texture has a pixel that isn't opaque within the UV rectangle (model units, 0..16). */
    private boolean transparentIn(String texture, double u1, double v1, double u2, double v2) throws IOException {
        BufferedImage image = texture(texture);
        int width = image.getWidth();
        int height = Math.min(image.getHeight(), width); // animated textures: the first frame
        int x1 = (int) Math.floor(Math.min(u1, u2) * width / UNITS + EDGE);
        int x2 = Math.max(x1 + 1, (int) Math.ceil(Math.max(u1, u2) * width / UNITS - EDGE));
        int y1 = (int) Math.floor(Math.min(v1, v2) * height / UNITS + EDGE);
        int y2 = Math.max(y1 + 1, (int) Math.ceil(Math.max(v1, v2) * height / UNITS - EDGE));
        for (int y = Math.max(0, y1); y < Math.min(height, y2); y++) {
            for (int x = Math.max(0, x1); x < Math.min(width, x2); x++) {
                if (image.getRGB(x, y) >>> 24 < OPAQUE) {
                    return true;
                }
            }
        }
        return false;
    }

    private BufferedImage texture(String texture) throws IOException {
        BufferedImage cached = textures.get(texture);
        if (cached != null) {
            return cached;
        }
        String path = ASSETS + "textures/" + texture.substring(NAMESPACE.length()) + ".png";
        try (InputStream in = BlockRenderLayersTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing texture " + path);
            BufferedImage image = ImageIO.read(in);
            textures.put(texture, image);
            return image;
        }
    }

    private static JsonObject read(String path) throws IOException {
        try (InputStream in = BlockRenderLayersTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
