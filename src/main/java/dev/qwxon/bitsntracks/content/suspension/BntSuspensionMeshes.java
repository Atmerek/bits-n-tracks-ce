package dev.qwxon.bitsntracks.content.suspension;

import dev.qwxon.bitsntracks.BitsNTracks;
import dev.qwxon.bitsntracks.physics.BntDebugLog;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BntSuspensionMeshes implements ResourceManagerReloadListener {
    public static final BntSuspensionMeshes INSTANCE = new BntSuspensionMeshes();
    static final int STRIDE = 24;
    private static final String[] FILES = {"suspension_arm", "bogie", "wide_bogie"};
    private static final String[] BOGIE_PARTS = {
        "frame", "drop", "lead_swing", "trail_swing", "lead_lean", "trail_lean", "lead_reach", "trail_reach", "lead_hub", "trail_hub"
    };
    private static final String[][] PARTS = {{"hub", "rod", "pivot"}, BOGIE_PARTS, BOGIE_PARTS};
    private static final Mesh EMPTY = new Mesh(new float[0], new int[0][]);

    private static volatile Mesh[] meshes = {EMPTY, EMPTY, EMPTY};

    private BntSuspensionMeshes() {
    }

    public record Mesh(float[] quads, int[][] parts) {
        public int[] part(int part) {
            return part < this.parts.length ? this.parts[part] : new int[0];
        }
    }

    public static Mesh mesh(int model) {
        return meshes[model];
    }

    public static int parts(int model) {
        return PARTS[model].length;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Mesh[] loaded = new Mesh[FILES.length];
        for (int model = 0; model < FILES.length; model++) {
            ResourceLocation location = BitsNTracks.asResource("models/suspension/" + FILES[model] + ".obj");
            try (BufferedReader reader = manager.openAsReader(location)) {
                loaded[model] = parse(reader, PARTS[model], location);
            } catch (IOException | RuntimeException exception) {
                BntDebugLog.LOG.error("Could not load {}", location, exception);
                loaded[model] = EMPTY;
            }
        }
        meshes = loaded;
    }

    private static Mesh parse(BufferedReader reader, String[] names, ResourceLocation location) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<List<float[]>> byPart = new ArrayList<>();
        for (int part = 0; part < names.length; part++) {
            byPart.add(new ArrayList<>());
        }
        int part = -1;
        String line;
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.trim().split("\\s+");
            switch (tokens[0]) {
                case "v" -> positions.add(floats(tokens, 3));
                case "vt" -> {
                    float[] uv = floats(tokens, 2);
                    uvs.add(new float[]{uv[0], 1.0F - uv[1]});
                }
                case "vn" -> normals.add(floats(tokens, 3));
                case "o", "g" -> {
                    part = tokens.length > 1 ? partIndex(names, tokens[1]) : -1;
                    if (part < 0) {
                        BntDebugLog.LOG.warn("{} has an unknown part {}, expected one of {}", location, tokens.length > 1 ? tokens[1] : "", Arrays.toString(names));
                    }
                }
                case "f" -> {
                    if (part >= 0) {
                        byPart.get(part).add(face(tokens, positions, uvs, normals, part));
                    }
                }
                default -> {
                }
            }
        }
        int count = 0;
        for (List<float[]> quads : byPart) {
            count += quads.size();
        }
        float[] quads = new float[count * STRIDE];
        int[][] parts = new int[names.length][];
        int at = 0;
        for (int index = 0; index < names.length; index++) {
            parts[index] = new int[byPart.get(index).size()];
            for (int quad = 0; quad < parts[index].length; quad++) {
                System.arraycopy(byPart.get(index).get(quad), 0, quads, at, STRIDE);
                parts[index][quad] = at;
                at += STRIDE;
            }
        }
        return new Mesh(quads, parts);
    }

    private static int partIndex(String[] names, String name) {
        int part = Arrays.asList(names).indexOf(name);
        if (part < 0 && name.matches(".+_\\d+")) {
            part = Arrays.asList(names).indexOf(name.substring(0, name.lastIndexOf('_')));
        }
        return part;
    }

    private static float[] face(String[] tokens, List<float[]> positions, List<float[]> uvs, List<float[]> normals, int part) {
        int corners = tokens.length - 1;
        if (corners != 3 && corners != 4) {
            throw new IllegalArgumentException("faces must have 3 or 4 corners, found " + corners);
        }
        float[] quad = new float[STRIDE];
        float[] normal = null;
        for (int k = 0; k < 4; k++) {
            String[] indices = tokens[1 + Math.min(k, corners - 1)].split("/");
            float[] position = positions.get(index(indices[0], positions.size()));
            float[] uv = indices.length > 1 && !indices[1].isEmpty() ? uvs.get(index(indices[1], uvs.size())) : new float[2];
            System.arraycopy(position, 0, quad, k * 5, 3);
            System.arraycopy(uv, 0, quad, k * 5 + 3, 2);
            if (normal == null && indices.length > 2 && !indices[2].isEmpty()) {
                normal = normals.get(index(indices[2], normals.size()));
            }
        }
        System.arraycopy(normal != null ? normal : computedNormal(quad), 0, quad, 20, 3);
        quad[23] = part;
        return quad;
    }

    private static int index(String token, int size) {
        int index = Integer.parseInt(token);
        return index < 0 ? size + index : index - 1;
    }

    private static float[] computedNormal(float[] quad) {
        float ax = quad[5] - quad[0];
        float ay = quad[6] - quad[1];
        float az = quad[7] - quad[2];
        float bx = quad[10] - quad[0];
        float by = quad[11] - quad[1];
        float bz = quad[12] - quad[2];
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        return length < 1.0E-6F ? new float[]{0.0F, 1.0F, 0.0F} : new float[]{nx / length, ny / length, nz / length};
    }

    private static float[] floats(String[] tokens, int count) {
        float[] values = new float[count];
        for (int i = 0; i < count; i++) {
            values[i] = Float.parseFloat(tokens[1 + i]);
        }
        return values;
    }
}
