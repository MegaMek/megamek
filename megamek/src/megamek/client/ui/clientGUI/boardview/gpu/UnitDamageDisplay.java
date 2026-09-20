/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import megamek.common.Configuration;
import megamek.logging.MMLogger;

/**
 * Shows a unit's lost locations on its model. Each location is its own part of the model, named after the game's
 * abbreviation for it. Detached parts are hidden; destroyed attached locations receive burnt armor artwork.
 * Presentation only; works on one unit's own instance and never on the shared model.
 */
final class UnitDamageDisplay implements Disposable {
    static final float ARMOR_WORN_LOSS = .5f;
    static final float ARMOR_STRIPPED_LOSS = 1f;
    static final float STRUCTURE_BATTERED_LOSS = .5f;
    static final float BODY_DAMAGE_1 = .25f;
    static final float BODY_DAMAGE_2 = .5f;
    static final float BODY_DAMAGE_3 = .75f;
    static final float BODY_DAMAGE_4 = 1f;

    enum Stage {
        ARMOR_WORN("armor-worn"), ARMOR_STRIPPED("armor-stripped"), STRUCTURE_BATTERED("structure-battered"),
        BODY_25("body-damage-25"), BODY_50("body-damage-50"), BODY_75("body-damage-75"), BODY_100("body-damage-100");
        final String file;
        Stage(String file) { this.file = file; }
    }

    /** Select one overlay: structural damage always takes precedence over armor damage. */
    static Stage locationStage(float armorLoss, float structureLoss) {
        if (structureLoss >= STRUCTURE_BATTERED_LOSS) { return Stage.STRUCTURE_BATTERED; }
        if (armorLoss >= ARMOR_STRIPPED_LOSS) { return Stage.ARMOR_STRIPPED; }
        return armorLoss >= ARMOR_WORN_LOSS ? Stage.ARMOR_WORN : null;
    }

    static Stage bodyStage(float loss) {
        if (loss >= BODY_DAMAGE_4) { return Stage.BODY_100; }
        if (loss >= BODY_DAMAGE_3) { return Stage.BODY_75; }
        if (loss >= BODY_DAMAGE_2) { return Stage.BODY_50; }
        return loss >= BODY_DAMAGE_1 ? Stage.BODY_25 : null;
    }

    /** Render-thread preview only. Actual destroyed/detached locations retain priority over the slider. */
    static BoardScene.LocationDamage preview(BoardScene.LocationDamage actual, boolean mek, float loss) {
        if (loss < 0) { return actual; }
        if (mek && loss >= 1) {
            return new BoardScene.LocationDamage(actual.removed(), Set.of("*"));
        }
        Stage stage = mek ? locationStage(Math.min(1, loss * 2), Math.max(0, loss * 2 - 1)) : bodyStage(loss);
        return new BoardScene.LocationDamage(actual.removed(), actual.wrecked(), stage == null ? Map.of() : Map.of("*", stage));
    }

    /** Alpha is a paint mask, never mesh transparency. Instances borrow these view-owned textures. */
    static final class Overlay extends Attribute {
        static final long TYPE = register("unitDamageOverlay");
        final Texture texture;
        final int seed;
        final float cos, sin, offsetU, offsetV;

        Overlay(Texture texture) { this(texture, 0); }

        Overlay(Texture texture, int seed) {
            super(TYPE);
            this.texture = texture;
            this.seed = seed;
            float angle = (seed & 0xFFFF) * (MathUtils.PI2 / 65536);
            float scale = .8f + ((seed >>> 16) & 255) * (.4f / 255);
            cos = MathUtils.cos(angle) * scale;
            sin = MathUtils.sin(angle) * scale;
            offsetU = ((seed >>> 8) & 0xFFF) / 64f;
            offsetV = (seed >>> 20) / 64f;
        }

        @Override public Attribute copy() { return new Overlay(texture, seed); }
        @Override public int compareTo(Attribute other) {
            if (type != other.type) { return Long.compare(type, other.type); }
            Overlay overlay = (Overlay) other;
            int comparison = Integer.compare(texture.getTextureObjectHandle(), overlay.texture.getTextureObjectHandle());
            return comparison == 0 ? Integer.compare(seed, overlay.seed) : comparison;
        }
    }
    /** The flat color of a burnt-out location. Dark, but not so dark that its shape is lost against a shadow. */
    static final Color WRECKED = new Color(0.22f, 0.22f, 0.22f, 1);
    static final String WRECKED_SUFFIX = "-wrecked";
    private static final MMLogger LOGGER = MMLogger.create(UnitDamageDisplay.class);
    private Texture texture;
    private boolean attempted;
    private final EnumMap<Stage, Texture> overlays = new EnumMap<>(Stage.class);
    private final EnumSet<Stage> attemptedOverlays = EnumSet.noneOf(Stage.class);

    void applyTexture(ModelInstance instance, BoardScene.LocationDamage damage) {
        applyTexture(instance, damage, 0);
    }

    void applyTexture(ModelInstance instance, BoardScene.LocationDamage damage, int unitId) {
        applyTexture(instance, unitId);
        damage.stages().forEach((location, stage) -> {
            Texture overlay = overlay(stage);
            if (overlay == null) { return; }
            paint(instance, location, overlay, unitId);
        });
    }

    private static void paint(ModelInstance instance, String location, Texture texture, int unitId) {
        forParts(instance, location, (node, part) -> {
            // Destroyed artwork already occupies the single damage slot and wins over all other stages.
            if (!part.material.id.endsWith(WRECKED_SUFFIX)) {
                setOverlay(part, texture, seed(unitId, node.id));
            }
        });
    }

    private static void setOverlay(NodePart part, Texture texture, int seed) {
        if (!part.enabled) { return; }
        var current = part.material.get(Overlay.class, Overlay.TYPE);
        if (current != null && current.texture == texture && current.seed == seed) { return; }
        part.material = part.material.copy();
        part.material.set(new Overlay(texture, seed));
    }

    /** Stable across instance rebuilds, damage stages, camera changes and animation; distinct for each named part. */
    private static int seed(int unitId, String node) {
        int seed = 31 * unitId + node.hashCode();
        seed = (seed ^ (seed >>> 16)) * 0x7FEB352D;
        seed = (seed ^ (seed >>> 15)) * 0x846CA68B;
        return seed ^ (seed >>> 16);
    }

    private Texture overlay(Stage stage) {
        if (attemptedOverlays.add(stage)) {
            try {
                Texture loaded = new Texture(new FileHandle(new File(Configuration.dataDir(),
                      "models/units/textures/" + stage.file + ".png")), true);
                loaded.setWrap(Texture.TextureWrap.MirroredRepeat, Texture.TextureWrap.MirroredRepeat);
                loaded.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                overlays.put(stage, loaded);
            } catch (RuntimeException error) {
                LOGGER.warn("Cannot load unit damage overlay {}: {}", stage.file, error.getMessage());
            }
        }
        return overlays.get(stage);
    }

    /** GL-thread only. One view owns the shared damage texture; model instances merely borrow it. */
    void applyTexture(ModelInstance instance) {
        applyTexture(instance, 0);
    }

    private void applyTexture(ModelInstance instance, int unitId) {
        for (Node node : instance.nodes) {
            applyTexture(node, unitId);
        }
    }

    void wreck(ModelInstance instance, GpuUnitModel model) {
        wreck(instance, model, 0);
    }

    void wreck(ModelInstance instance, GpuUnitModel model, int unitId) {
        if (model.rigs().stream().anyMatch(UnitRig::trooper)) { return; }
        if (model.rigs().stream().noneMatch(UnitRig::mek)) {
            Texture overlay = overlay(Stage.BODY_100);
            if (overlay != null) { paint(instance, "*", overlay, unitId); }
            return;
        }
        for (var node : instance.nodes) { wreck(node); }
        applyTexture(instance, unitId);
    }

    private static void wreck(Node node) {
        for (var part : node.parts) {
            if (!part.material.id.endsWith(WRECKED_SUFFIX)) {
                part.material = wrecked(part.material);
            }
        }
        node.getChildren().forEach(UnitDamageDisplay::wreck);
    }

    private void applyTexture(Node node, int unitId) {
        for (NodePart part : node.parts) {
            if (part.enabled && part.material.id.endsWith(WRECKED_SUFFIX)) {
                if (!attempted) {
                    attempted = true;
                    try {
                        texture = new Texture(new FileHandle(new File(Configuration.dataDir(),
                              "models/units/textures/destroyed-armor.png")), true);
                        texture.setWrap(Texture.TextureWrap.MirroredRepeat, Texture.TextureWrap.MirroredRepeat);
                        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                    } catch (RuntimeException error) {
                        LOGGER.warn("Cannot load destroyed armor texture; using dark gray: {}", error.getMessage());
                    }
                }
                if (texture != null) {
                    // Apply after baked vertex color, exactly like the other damage stages; do not tint it black.
                    setOverlay(part, texture, seed(unitId, node.id));
                }
            }
        }
        node.getChildren().forEach(child -> applyTexture(child, unitId));
    }

    @Override
    public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        attempted = false;
        overlays.values().forEach(Texture::dispose);
        overlays.clear();
        attemptedOverlays.clear();
    }

    /**
     * Applies the damage to a fresh instance. It does not undo earlier damage, so give it an instance that shows
     * none.
     *
     * @return the abbreviations the model has no part for, which therefore show nothing; empty when all were found
     */
    static List<String> show(ModelInstance instance, BoardScene.LocationDamage damage) {
        List<String> missing = new ArrayList<>();
        for (String location : damage.removed()) {
            List<NodePart> parts = locationParts(instance, location);
            if (parts.isEmpty()) {
                missing.add(location);
            }
            for (NodePart part : parts) {
                part.enabled = false;
            }
        }
        for (String location : damage.wrecked()) {
            List<NodePart> parts = locationParts(instance, location);
            if (parts.isEmpty()) {
                missing.add(location);
            }
            for (NodePart part : parts) {
                part.material = wrecked(part.material);
            }
        }
        return missing;
    }

    /**
     * The drawn pieces of one location: the part named after it and its own sub-parts, such as {@code LL-shin} under
     * {@code LL}. Other locations that merely hang from it, as the arms hang from the center torso, are left out.
     * The wildcard {@code *} includes every part for whole-unit damage previews and non-Mek overlays.
     *
     * @return the pieces, empty if the model has no part of that name
     */
    static List<NodePart> locationParts(ModelInstance instance, String location) {
        List<NodePart> parts = new ArrayList<>();
        forParts(instance, location, (node, part) -> parts.add(part));
        return parts;
    }

    private static void forParts(ModelInstance instance, String location, BiConsumer<Node, NodePart> action) {
        if ("*".equals(location)) {
            for (Node root : instance.nodes) { collect(root, null, action); }
            return;
        }
        Node node = instance.getNode(location);
        if (node != null) {
            collect(node, location, action);
        }
    }

    private static void collect(Node node, String location, BiConsumer<Node, NodePart> action) {
        for (NodePart part : node.parts) {
            action.accept(node, part);
        }
        for (Node child : node.getChildren()) {
            boolean isSubPart = location == null || child.id.startsWith(location + "-") || child.id.startsWith(location + "@");
            if (isSubPart) {
                collect(child, location, action);
            }
        }
    }

    /** A copy, so that neither the shared model nor the unit's player tint on the other parts is touched. */
    static Material wrecked(Material material) {
        Material copy = material.copy();
        if (!copy.id.endsWith(WRECKED_SUFFIX)) {
            copy.id += WRECKED_SUFFIX;
        }
        copy.remove(TextureAttribute.Diffuse | ColorAttribute.Emissive | TextureAttribute.Emissive | Overlay.TYPE);
        copy.set(ColorAttribute.createDiffuse(WRECKED));
        return copy;
    }
}
