/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Vector3;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.equipment.EquipmentType;
import megamek.common.icons.Camouflage;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;

/** Tests the actual paint shader, shared textures, rigid posing, fixed materials and GPU disposal. */
final class GpuCamouflageReview {
    private GpuCamouflageReview() { }

    static void verify(GpuUnitModels library) throws Exception {
        var paints = new GpuUnitCamouflage();
        var damage = new UnitDamageDisplay();
        var batch = new ModelBatch(GpuUnitCamouflage.shaders());
        var source = new UnitCamouflage();
        try {
            var mek = new BipedMek();
            mek.addEquipment(EquipmentType.get("PPC"), Mek.LOC_LEFT_ARM);
            mek.addEquipment(EquipmentType.get("PPC"), Mek.LOC_RIGHT_ARM);
            var selection = UnitCamouflageTest.selection(mek);
            var model = library.get(selection, 6100);
            assertNotNull(model);
            var first = new ModelInstance(model.instance.model);
            var second = new ModelInstance(model.instance.model);
            var pixels = pattern();
            paints.apply(first, model.instance, appearance(pixels, 0, 10));
            paints.apply(second, model.instance, appearance(pixels, 90, 20));
            Texture shared = texture(parts(first.nodes).stream().filter(part -> "paint".equals(part.material.id)).findFirst().orElseThrow());
            assertSame(shared, texture(parts(second.nodes).stream().filter(part -> "paint".equals(part.material.id)).findFirst().orElseThrow()));
            assertEquals(1, paints.textureCount(), "Rotation/scale must not duplicate texture uploads");
            assertTrue(parts(model.instance.nodes).stream().filter(part -> "paint".equals(part.material.id))
                  .allMatch(part -> texture(part) == null), "The shared template must remain unpainted");
            for (var part : parts(first.nodes)) {
                if ("paint".equals(part.material.id)) {
                    assertSame(shared, texture(part), "Chassis and equipment paint use the same image");
                } else {
                    assertNull(texture(part), "Cockpit, metal and weapon tips retain their fixed artwork");
                }
            }
            var rest = frame(batch, first, 0);
            assertTrue(difference(rest, frame(batch, second, 0)) > 1000, "The shader must visibly apply rotation and scale");
            first.transform.setToTranslation(75, 0, 0);
            assertTrue(difference(rest, frame(batch, first, 75)) < 300, "Moving unit and camera together must not make paint swim");
            first.transform.idt();
            model.turnUpperBody(first, 35);
            first.getNode("LA-forearm").rotation.set(Vector3.X, 20);
            first.calculateTransforms();
            assertTrue(difference(rest, frame(batch, first, 0)) > 1000);
            assertTrue(second.getNode("CT").rotation.isIdentity());

            var damaged = new ModelInstance(model.instance.model);
            UnitDamageDisplay.show(damaged, new BoardScene.LocationDamage(Set.of("LA"), Set.of("RL")));
            paints.apply(damaged, model.instance, appearance(pixels, 0, 10));
            damage.applyTexture(damaged);
            assertFalse(damaged.getNode("LA").parts.first().enabled);
            for (var part : UnitDamageDisplay.locationParts(damaged, "RL")) {
                var overlay = part.material.get(UnitDamageDisplay.Overlay.class, UnitDamageDisplay.Overlay.TYPE);
                assertNotNull(overlay, "Destroyed parts use the authored armor texture");
                assertEquals(128, overlay.texture.getWidth());
                assertEquals(128, overlay.texture.getHeight());
                assertNull(texture(part), "Burnt paint must not regain the player's camouflage");
            }
            assertTrue(parts(second.nodes).stream().allMatch(part -> part.enabled));

            first.transform.setToTranslation(-50, 0, 0);
            second.transform.setToTranslation(30, 0, 0);
            damaged.transform.setToTranslation(95, 0, 0);
            GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(first, second, damaged), "runtime-camo-transforms", 320, 20);

            var icon = new Camouflage("Word of Blake/", "TerraSec (Camo).png");
            mek.setCamouflage(icon);
            var actual = source.resolve(UnitCamouflageTest.selection(mek)).state().appearance();
            first = new ModelInstance(model.instance.model);
            paints.apply(first, model.instance, actual);
            first.transform.setToTranslation(-43, 0, 0);
            var support = new BattlefieldSupportAsset();
            icon.setRotationAngle(55);
            icon.setScale(18);
            support.setCamouflage(icon);
            second = new ModelInstance(model.instance.model);
            paints.apply(second, model.instance, source.resolve(UnitCamouflageTest.selection(support)).state().appearance());
            second.transform.setToTranslation(43, 0, 0);
            GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(first, second), "runtime-camo-image-marker", 185, 20);

            int handle = shared.getTextureObjectHandle();
            paints.retain(List.of());
            assertEquals(0, paints.textureCount());
            assertFalse(Gdx.gl.glIsTexture(handle), "Removing the last visible reference releases its GPU texture");
            for (int reopen = 0; reopen < 3; reopen++) {
                paints.apply(new ModelInstance(model.instance.model), model.instance, appearance(pixels, 0, 10));
                assertEquals(1, paints.textureCount());
                paints.dispose();
                assertEquals(0, paints.textureCount());
            }
        } finally {
            source.clear();
            batch.dispose();
            paints.dispose();
            damage.dispose();
        }
        boardView();
    }

    private static void boardView() throws Exception {
        try (var fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                fixture.entity.setCamouflage(new Camouflage("Word of Blake/", "TerraSec (Camo).png"));
                fixture.source.refresh();
            });
            var view = new GpuBattleView(fixture.source);
            try {
                view.create();
                view.render();
                var instances = (Map<?, ?>) field(view, "unitInstances");
                var before = (ModelInstance) instances.get("1:-1");
                assertNotNull(before);
                var turns = (Map<?, ?>) field(view, "upperBodyTurns");
                Object turn = turns.get("1:-1");
                assertNotNull(turn);
                view.boardCamera.center(UnitBounds.world(before).getCenter(new Vector3()));
                view.boardCamera.zoom(.12f);
                var beforeTexture = parts(before.nodes).stream().map(GpuCamouflageReview::texture)
                      .filter(java.util.Objects::nonNull).findFirst().orElseThrow();
                var paints = (GpuUnitCamouflage) field(view, "camouflage");
                assertEquals(1, paints.textureCount());
                SwingUtilities.invokeAndWait(() -> {
                    fixture.entity.getCamouflage().setRotationAngle(70);
                    fixture.entity.getCamouflage().setScale(20);
                    fixture.source.refresh();
                });
                for (boolean top : new boolean[] { false, true }) {
                    view.boardCamera.setIsometric(!top);
                    view.render();
                    var after = (ModelInstance) instances.get("1:-1");
                    assertSame(before.model, after.model, "A board appearance update must retain its assembled geometry");
                    assertSame(turn, turns.get("1:-1"), "Changing paint must preserve torso animation playback");
                    assertTrue(parts(after.nodes).stream().anyMatch(part -> texture(part) == beforeTexture));
                    var capture = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    try {
                        PixmapIO.writePNG(new FileHandle(new File(System.getProperty("megamek.gpu.screenshots"),
                              "runtime-camo-board-" + (top ? "top" : "isometric") + ".png")), capture, -1, true);
                    } finally {
                        capture.dispose();
                    }
                }
                SwingUtilities.invokeAndWait(() -> {
                    fixture.entity.setPosition(null);
                    fixture.source.refresh();
                });
                view.render();
                assertEquals(0, paints.textureCount());
            } finally {
                view.dispose();
            }
        }
    }

    static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    static Texture texture(NodePart part) {
        var attribute = part.material.get(TextureAttribute.class, TextureAttribute.Diffuse);
        return attribute == null ? null : attribute.textureDescription.texture;
    }

    static List<NodePart> parts(Iterable<Node> nodes) {
        List<NodePart> parts = new ArrayList<>();
        for (Node node : nodes) {
            node.parts.forEach(parts::add);
            parts.addAll(parts(node.getChildren()));
        }
        return parts;
    }

    private static BoardScene.Pixels pattern() {
        var image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                image.setRGB(x, y, x < 10 ? 0xFF416933 : y < 15 ? 0xFFB39B63 : 0xFF433D35);
            }
        }
        return new BoardScene.Pixels(image);
    }

    private static UnitModelState.Appearance appearance(BoardScene.Pixels pixels, int rotation, int scale) {
        return new UnitModelState.Appearance(Set.of(), false,
              new UnitModelState.Camo("review", "pattern", rotation, scale, 0xFFFFFF, pixels, null));
    }

    private static int[] frame(ModelBatch batch, ModelInstance instance, float offset) {
        var camera = new OrthographicCamera(110, 68.75f);
        camera.near = 1;
        camera.far = 1000;
        camera.position.set(50 + offset, 160, 120);
        camera.up.set(Vector3.Z);
        camera.lookAt(offset, 0, 20);
        camera.update();
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(.15f, .19f, .23f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        var environment = new Environment();
        environment.set(ColorAttribute.createAmbientLight(1, 1, 1, 1));
        batch.begin(camera);
        batch.render(instance, environment);
        batch.end();
        var pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        try {
            int[] result = new int[pixels.getWidth() * pixels.getHeight()];
            for (int y = 0; y < pixels.getHeight(); y++) {
                for (int x = 0; x < pixels.getWidth(); x++) {
                    result[y * pixels.getWidth() + x] = pixels.getPixel(x, y);
                }
            }
            return result;
        } finally {
            pixels.dispose();
        }
    }

    private static int difference(int[] first, int[] second) {
        int changed = 0;
        for (int i = 0; i < first.length; i++) {
            if (first[i] != second[i]) {
                changed++;
            }
        }
        return changed;
    }
}
