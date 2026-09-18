/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;

/** One unit's token: the unit artwork extruded into a flat-topped solid that occupies the unit's height. */
final class GpuMeeple implements Disposable {
    private final Model model;
    final ModelInstance instance;
    private final BoundingBox bounds;

    GpuMeeple(BoardScene.Pixels pixels, TextureRegion region) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder caps = builder.part("cutout", GL20.GL_TRIANGLES,
              VertexAttributes.Usage.Position | VertexAttributes.Usage.TextureCoordinates | VertexAttributes.Usage.ColorPacked
                  | VertexAttributes.Usage.Normal,
              new Material(TextureAttribute.createDiffuse(region.getTexture()), IntAttribute.createCullFace(GL20.GL_NONE)));
        MeshPartBuilder sides = builder.part("sides", GL20.GL_TRIANGLES,
              VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
              new Material(ColorAttribute.createDiffuse(GpuCutout.averageColor(pixels)),
                    IntAttribute.createCullFace(GL20.GL_NONE)));
        // The token is built one height unit tall; the placement scales it to the occupied height.
        GpuCutout.extrude(pixels, region, Vector3.Zero, 1, 1, true, caps, sides);
        model = builder.end();
        instance = new ModelInstance(model);
        bounds = instance.calculateBoundingBox(new BoundingBox());
        if (!bounds.isValid()) {
            bounds.set(Vector3.Zero, Vector3.Zero);
        }
    }

    Vector3 place(ModelInstance placed, Camera camera, Vector3 ground, float facing, int height) {
        float thickness = height * BoardGeometry.LEVEL * BoardGeometry.UNIT_HEIGHT_SCALE;
        // The cutout spans z 0 to 1, so the placement keeps its bottom half a world unit above the ground.
        placed.transform.set(ground, new Quaternion(Vector3.Z, -facing))
              .translate(0, 0, 0.5f)
              .scale(BoardGeometry.UNIT_SCALE, BoardGeometry.UNIT_SCALE, thickness);
        float top = -Float.MAX_VALUE;
        for (float horizontal : new float[] { bounds.min.x, bounds.max.x }) {
            for (float vertical : new float[] { bounds.min.y, bounds.max.y }) {
                for (float depth : new float[] { bounds.min.z, bounds.max.z }) {
                    top = Math.max(top, new Vector3(horizontal, vertical, depth).rot(placed.transform).dot(camera.up));
                }
            }
        }
        return placed.transform.getTranslation(new Vector3()).mulAdd(camera.up, top);
    }

    @Override
    public void dispose() {
        model.dispose();
    }
}
