/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import megamek.client.ui.tileset.EntityImage;
import megamek.common.icons.Camouflage;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Compare the actual GPU projection with the classic board's camouflage sampler on an unlit white unit. */
@Tag("on-demand")
class GpuCamouflageMappingSmokeTest {
    @Test
    void oneImageSpansTheRestUnitAndMatchesClassicRotationAndScale() {
        var failure = new AtomicReference<Throwable>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var paints = new GpuUnitCamouflage();
                var batch = new ModelBatch(GpuUnitShader.provider());
                var buffer = new FrameBuffer(Pixmap.Format.RGBA8888, 84, 72, true);
                var model = splitBox();
                try {
                    var image = pattern();
                    var pixels = new BoardScene.Pixels(image);
                    for (float size : new float[] { 1, 2, .01f }) {
                        var rest = new ModelInstance(model);
                        for (var node : rest.nodes) {
                            node.translation.scl(size);
                            node.scale.scl(size);
                        }
                        rest.calculateTransforms();
                        for (int[] settings : List.of(new int[] { 0, 10 }, new int[] { 90, 10 },
                              new int[] { 0, 20 }, new int[] { 35, 15 })) {
                            var instance = new ModelInstance(rest);
                            paints.apply(instance, rest, appearance(pixels, settings[0], settings[1]));
                            var classic = classic(image, settings[0], settings[1]);
                            for (int side = 0; side < 3; side++) {
                                var camera = camera(side, size);
                                var rendered = pixels(batch, buffer, camera, instance);
                                compare(classic, rendered, "size=" + size + ", rotation=" + settings[0]
                                      + ", scale=" + settings[1] + ", side=" + side);
                                if (side == 1 && settings[0] == 0) {
                                    instance.transform.setToRotation(Vector3.Z, 30);
                                    camera.rotateAround(Vector3.Zero, Vector3.Z, 30);
                                    camera.update();
                                    compare(classic, pixels(batch, buffer, camera, instance), "unit facing");
                                    instance.transform.idt();
                                }
                            }
                            // Pose the rigid nodes themselves, then follow them with the camera.
                            for (var node : instance.nodes) {
                                node.translation.rotate(Vector3.Z, 90);
                                node.rotation.mulLeft(new Quaternion(Vector3.Z, 90));
                            }
                            instance.calculateTransforms();
                            var camera = camera(1, size);
                            camera.rotateAround(Vector3.Zero, Vector3.Z, 90);
                            camera.update();
                            compare(classic, pixels(batch, buffer, camera, instance), "rigid animation");
                        }
                    }
                    assertEquals(1, paints.textureCount(), "Unit dimensions and camo settings share the same image");
                    preservesAuthoredTextureUVs(batch, buffer, model, paints, pixels);
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    model.dispose();
                    buffer.dispose();
                    batch.dispose();
                    paints.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Camouflage mapping failed", failure.get()); }
    }

    private static Model splitBox() {
        var builder = new ModelBuilder();
        builder.begin();
        // Two independently transformed rigid parts form a single 84 x 72 x 72 unit.
        // No authored UVs: the camo is projected in the complete unit's coordinate system.
        long attributes = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        var left = builder.node();
        left.id = "left";
        left.translation.set(-21, 0, 36);
        left.scale.set(2, 1, .5f);
        builder.part("left", GL20.GL_TRIANGLES, attributes,
              new Material("paint", ColorAttribute.createDiffuse(Color.WHITE))).box(21, 72, 144);
        var right = builder.node();
        right.id = "right";
        right.translation.set(21, 0, 36);
        right.scale.set(1, 2, .5f);
        right.rotation.set(Vector3.Z, 90);
        builder.part("right", GL20.GL_TRIANGLES, attributes,
              new Material("paint", ColorAttribute.createDiffuse(Color.WHITE))).box(72, 21, 144);
        return builder.end();
    }

    private static BufferedImage pattern() {
        var image = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 72; y++) {
            for (int x = 0; x < 84; x++) {
                // Asymmetric, smooth color ramps expose repetition, flips, seams and distorted rotation.
                image.setRGB(x, y, 0xFF000000 | ((32 + x * 2) << 16) | ((40 + y * 2) << 8) | (48 + x + y));
            }
        }
        return image;
    }

    private static UnitModelState.Appearance appearance(BoardScene.Pixels pixels, int rotation, int scale) {
        return new UnitModelState.Appearance(Set.of(), false,
              new UnitModelState.Camo("review", "pattern", rotation, scale, 0xFFFFFF, pixels, null));
    }

    private static BufferedImage classic(BufferedImage pattern, int rotation, int scale) {
        var camo = new Camouflage("review", "pattern") {
            @Override public Image getBaseImage() { return pattern; }
        };
        camo.setRotationAngle(rotation);
        camo.setScale(scale);
        var white = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        var graphics = white.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, 84, 72);
        graphics.dispose();
        var icon = EntityImage.createIcon(white, camo, mock(Infantry.class), false);
        icon.loadFacings();
        return (BufferedImage) icon.getBase();
    }

    private static OrthographicCamera camera(int side, float size) {
        var camera = new OrthographicCamera((side == 2 ? 72 : 84) * size, 72 * size);
        camera.near = 1;
        camera.far = 1000;
        camera.up.set(side == 0 ? Vector3.Y : Vector3.Z);
        camera.position.set(side == 2 ? 200 : 0, side == 1 ? -200 : 0, side == 0 ? 200 : 36 * size);
        camera.lookAt(0, 0, 36 * size);
        camera.update();
        return camera;
    }

    private static int[] pixels(ModelBatch batch, FrameBuffer buffer, OrthographicCamera camera, ModelInstance instance) {
        buffer.begin();
        try {
            Gdx.gl.glClearColor(1, 0, 1, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            batch.begin(camera);
            batch.render(instance);
            batch.end();
            var capture = Pixmap.createFromFrameBuffer(0, 0, 84, 72);
            try {
                int[] result = new int[84 * 72];
                for (int y = 0; y < 72; y++) {
                    for (int x = 0; x < 84; x++) { result[y * 84 + x] = capture.getPixel(x, 71 - y); }
                }
                return result;
            } finally { capture.dispose(); }
        } finally { buffer.end(); }
    }

    private static void compare(BufferedImage classic, int[] rendered, String context) {
        int maximum = 0;
        // Ignore rasterized silhouette edges; compare all interior pixels, including the seam between parts.
        for (int y = 2; y < 70; y++) {
            for (int x = 2; x < 82; x++) {
                int expected = classic.getRGB(x, y), actual = rendered[y * 84 + x] >>> 8;
                for (int shift : new int[] { 0, 8, 16 }) {
                    maximum = Math.max(maximum, Math.abs(((expected >>> shift) & 255) - ((actual >>> shift) & 255)));
                }
            }
        }
        // Classic CPU interpolation rounds twice; GL texture filtering may round differently by a few levels.
        assertTrue(maximum <= 4, context + ": maximum channel error " + maximum);
    }

    private static void preservesAuthoredTextureUVs(ModelBatch batch, FrameBuffer buffer, Model model,
          GpuUnitCamouflage paints, BoardScene.Pixels pixels) {
        var painted = new ModelInstance(model);
        paints.apply(painted, new ModelInstance(model), appearance(pixels, 0, 10));
        var texture = painted.nodes.first().parts.first().material.get(TextureAttribute.class, TextureAttribute.Diffuse);
        var builder = new ModelBuilder();
        var sprite = builder.createBox(84, 72, 72,
              new Material("sprite", ColorAttribute.createDiffuse(Color.WHITE), texture.copy()),
              VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
        var standard = new ModelBatch();
        try {
            var instance = new ModelInstance(sprite);
            instance.transform.setToTranslation(0, 0, 36);
            var camera = camera(0, 1);
            assertArrayEquals(pixels(standard, buffer, camera, instance), pixels(batch, buffer, camera, instance),
                  "Materials without unit paint retain their original texture UVs");
        } finally { standard.dispose(); sprite.dispose(); }
    }
}
