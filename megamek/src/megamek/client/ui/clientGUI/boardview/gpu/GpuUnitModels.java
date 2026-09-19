/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import megamek.common.Configuration;
import megamek.logging.MMLogger;

/** GL-thread ownership of shared unit assets. Failed or absent assets fall back without breaking the board. */
final class GpuUnitModels implements Disposable {
    /** Enable authored unit meshes in both GPU camera views. */
    static final boolean ENABLED = true;

    private static final MMLogger LOGGER = MMLogger.create(GpuUnitModels.class);
    private final Path root = Configuration.dataDir().toPath().resolve("models").toAbsolutePath().normalize();
    private final Map<Path, JsonValue> descriptors = new HashMap<>();
    private final Map<Path, GpuMeeple> models = new HashMap<>();
    private final Set<Path> failed = new HashSet<>();

    GpuMeeple get(BoardScene.UnitModel selection) {
        if (selection == null) {
            return null;
        }
        GpuMeeple model = load(selection.asset(), selection);
        return model != null ? model : load(selection.fallback(), selection);
    }

    private GpuMeeple load(String asset, BoardScene.UnitModel selection) {
        if (asset == null) {
            return null;
        }
        Path descriptor = root.resolve(asset).normalize();
        if (!descriptor.startsWith(root) || failed.contains(descriptor)) {
            return null;
        }
        Path modelPath = descriptor;
        try {
            JsonValue value = descriptors.computeIfAbsent(descriptor, file -> {
                JsonValue parsed = new JsonReader().parse(new FileHandle(file.toFile()));
                if (parsed.getInt("schema", 0) != 1) {
                    throw new IllegalArgumentException("Unsupported unit-model schema: " + file);
                }
                return parsed;
            });
            modelPath = descriptor.getParent().resolve(selectModel(value, selection.variant(), selection.figures()))
                  .normalize();
            if (!modelPath.startsWith(root) || failed.contains(modelPath)) {
                return null;
            }
            if (!models.containsKey(modelPath)) {
                if (!Files.isRegularFile(modelPath)) {
                    throw new IllegalArgumentException("Missing unit mesh " + modelPath);
                }
                var data = new G3dModelLoader(new JsonReader()).loadModelData(new FileHandle(modelPath.toFile()));
                GpuMeeple meeple = new GpuMeeple(new Model(data), value.getString("upperBodyNode", null));
                boolean isMek = "mek".equals(value.getString("kind", ""));
                if (isMek && !meeple.turnsUpperBody()) {
                    LOGGER.debug("[GpuTwist] {} has no upper body part to turn: a torso twist turns the whole unit",
                          modelPath);
                }
                models.put(modelPath, meeple);
            }
            return models.get(modelPath);
        } catch (RuntimeException error) {
            failed.add(modelPath);
            LOGGER.warn("Cannot load 3D unit asset {}; using its fallback: {}", modelPath, error.getMessage());
            return null;
        }
    }

    static String selectModel(JsonValue descriptor, String variant, int figures) {
        boolean formation = "formation".equals(descriptor.getString("kind", ""));
        String key = formation ? Integer.toString(figures) : variant;
        if (formation) {
            JsonValue movements = descriptor.get("movementFormations");
            JsonValue choices = movements == null ? null : movements.get(variant);
            if (choices != null && choices.has(key)) {
                return choices.getString(key);
            }
        }
        // Older/custom formation descriptors and unsupported movement types retain their base poses.
        JsonValue choices = descriptor.get(formation ? "formations" : "variants");
        return choices != null && choices.has(key) ? choices.getString(key) : descriptor.getString("fallback");
    }

    @Override
    public void dispose() {
        models.values().forEach(GpuMeeple::dispose);
        models.clear();
        descriptors.clear();
        failed.clear();
    }
}
