/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

/** Flat artwork for the existing animated poses. Owns no game state or animation clock. */
final class GpuUnitIcons implements Disposable {
    static final boolean DEFAULT_ENABLED = false;
    /** Switch when one hex is this wide in window pixels; zooming back in has a 15% margin. */
    static final float DEFAULT_HEX_PIXELS = 0; // 0 means always
    static final float ZOOM_HYSTERESIS = .15f;
    static final float SIZE_IN_HEXES = .9f;
    private final GpuTextures<BoardScene.Pixels> textures = new GpuTextures<>(true);
    private final Map<BoardScene.Pixels, GpuUnitModel> models = new HashMap<>();
    private final Map<String, ModelInstance> instances = new HashMap<>();
    private boolean active;

    static boolean useIcons(boolean enabled, Camera camera, float hexPixels, float threshold, boolean previous) {
        return enabled && ((threshold <= 0) || hexPixels <= threshold * (previous ? 1 + ZOOM_HYSTERESIS : 1)) && GpuTactical.flat(camera);
    }

    boolean active() { return active; }

    ModelInstance instance(BoardScene.Unit unit) { return instances.get(unit.id() + ":" + unit.part()); }

    Collection<ModelInstance> instances() { return instances.values(); }

    /** Returns true when cached picking meshes must be released along with an obsolete atlas layout. */
    boolean update(boolean enabled, float threshold, OrthographicCamera camera, BoardScene scene,
          Map<BoardScene.Unit, UnitFootprint.Pose> poses, Map<BoardScene.Unit, Vector3> anchors, BoardSurface.Cache surfaces) {
        active = useIcons(enabled, camera, BoardGeometry.WIDTH / camera.zoom, threshold, active);
        if (!active) { return false; }
        var images = scene.units().stream().map(BoardScene.Unit::image).distinct()
              .collect(Collectors.toMap(image -> image, image -> image));
        boolean changed = textures.update(images);
        if (changed) {
            models.values().forEach(GpuUnitModel::dispose);
            models.clear();
            instances.clear();
        }
        instances.keySet().retainAll(scene.units().stream().map(unit -> unit.id() + ":" + unit.part()).collect(Collectors.toSet()));
        for (var unit : scene.units()) {
            var pose = poses.get(unit);
            if (pose == null) { continue; }
            var model = models.computeIfAbsent(unit.image(), image -> GpuUnitModel.sprite(image, textures.region(image)));
            String key = unit.id() + ":" + unit.part();
            var instance = instances.get(key);
            if (instance == null || instance.model != model.instance.model) {
                instance = new ModelInstance(model.instance.model);
                // Tactical symbols remain readable over roofs, without writing artificial scene depth.
                instance.materials.forEach(material -> material.set(new DepthTestAttribute(GL20.GL_ALWAYS, false)));
                instances.put(key, instance);
            }
            var position = pose.position();
            float ground = UnitLandingSupports.surface(scene, position.x, position.y, surfaces);
            var tile = scene.tile(unit.location().coords());
            if (!Float.isFinite(ground)) { ground = tile == null ? 0 : BoardGeometry.surfaceZ(tile); }
            float scale = SIZE_IN_HEXES * Math.min(BoardGeometry.WIDTH / unit.image().width(),
                  BoardGeometry.HEIGHT / unit.image().height());
            instance.transform.setToTranslation(position.x, position.y, ground + .25f * BoardGeometry.HEX_SCALE)
                  .rotate(Vector3.Z, -pose.facing()).scale(scale, scale, 1);
            anchors.put(unit, model.anchor(instance, camera));
        }
        return changed;
    }

    @Override public void dispose() {
        models.values().forEach(GpuUnitModel::dispose);
        models.clear();
        instances.clear();
        textures.dispose();
    }
}
