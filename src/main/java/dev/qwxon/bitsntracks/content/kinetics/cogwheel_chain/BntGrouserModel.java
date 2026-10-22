package dev.qwxon.bitsntracks.content.kinetics.cogwheel_chain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.physics.BntDebugLog;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BntGrouserModel implements ResourceManagerReloadListener {
    public static final BntGrouserModel INSTANCE = new BntGrouserModel();
    private static final ResourceLocation LOCATION = BitsNTracks.asResource("models/track/grouser.json");
    private static final String[] FACES = {"up", "down", "north", "south", "east", "west"};
    private static final float[] MISSING = {0.0F, 0.0F, 0.0F, 0.0F, 0.0F};
    private static final Model EMPTY = new Model(List.of(), BntChainTextures.GROUSER);

    private static volatile Model model = EMPTY;

    private BntGrouserModel() {
    }

    public record Element(float[] from, float[] to, float[][] faces) {
    }

    public record Model(List<Element> elements, ResourceLocation texture) {
    }

    public static Model model() {
        return model;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        try (BufferedReader reader = manager.openAsReader(LOCATION)) {
            model = parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException exception) {
            BntDebugLog.LOG.error("Could not load {}", LOCATION, exception);
            model = EMPTY;
        }
    }

    private static Model parse(JsonObject json) {
        List<Element> elements = new ArrayList<>();
        for (JsonElement entry : json.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            JsonObject faces = element.has("faces") ? element.getAsJsonObject("faces") : new JsonObject();
            float[][] uv = new float[FACES.length][];
            for (int face = 0; face < FACES.length; face++) {
                uv[face] = faces.has(FACES[face]) ? face(faces.getAsJsonObject(FACES[face])) : MISSING;
            }
            elements.add(new Element(vector(element.getAsJsonArray("from")), vector(element.getAsJsonArray("to")), uv));
        }
        return new Model(List.copyOf(elements), texture(json));
    }

    private static float[] face(JsonObject face) {
        JsonArray uv = face.getAsJsonArray("uv");
        int rotation = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
        return new float[]{
            uv.get(0).getAsFloat() / 16.0F,
            uv.get(1).getAsFloat() / 16.0F,
            uv.get(2).getAsFloat() / 16.0F,
            uv.get(3).getAsFloat() / 16.0F,
            Math.floorMod(rotation / 90, 4)
        };
    }

    private static float[] vector(JsonArray array) {
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    private static ResourceLocation texture(JsonObject json) {
        if (!json.has("textures")) {
            return BntChainTextures.GROUSER;
        }
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
            String name = entry.getValue().getAsString();
            if (!entry.getKey().equals("particle") && !name.startsWith("#")) {
                ResourceLocation sprite = ResourceLocation.parse(name);
                return sprite.withPath("textures/" + sprite.getPath() + ".png");
            }
        }
        return BntChainTextures.GROUSER;
    }
}
