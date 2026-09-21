/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static megamek.client.ui.clientGUI.boardview.gpu.GpuCamouflageReview.field;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import megamek.common.board.Coords;
import megamek.common.equipment.IArmorState;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** The real tuning controls replace rendered damage without changing the Swing-owned game snapshot. */
@Tag("on-demand")
class GpuDamageTuningSmokeTest {
    @Test
    void damageArtworkUsesStablePatternsAndPreservesCockpits() {
        var failure = new AtomicReference<Throwable>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var model = plate(Color.BLACK);
                var glass = plate(new Color(.21f, .67f, .73f, 1));
                var damage = new UnitDamageDisplay();
                var batch = new ModelBatch(GpuUnitShader.provider());
                var buffer = new FrameBuffer(Pixmap.Format.RGBA8888, 128, 128, true);
                try {
                    var instance = new ModelInstance(model);
                    var preview = UnitDamageDisplay.preview(BoardScene.LocationDamage.NONE, true, 1);
                    UnitDamageDisplay.show(instance, preview);
                    damage.applyTexture(instance, preview, 42);
                    var camera = new OrthographicCamera(64, 64);
                    camera.position.set(0, 0, 100);
                    camera.lookAt(0, 0, 0);
                    camera.near = 1;
                    camera.far = 200;
                    camera.update();
                    int[] original = pixels(batch, buffer, camera, instance);
                    var colors = new HashSet<Integer>();
                    for (int pixel : original) { colors.add(pixel); }
                    assertTrue(colors.size() > 8, "Destroyed artwork must retain its texture over baked black vertex colors");

                    int[] cockpit = pixels(batch, buffer, camera, new ModelInstance(glass));
                    int[] bareArmor = pixels(batch, buffer, camera, new ModelInstance(model));
                    for (var stage : UnitDamageDisplay.Stage.values()) {
                        var state = new BoardScene.LocationDamage(Set.of(), Set.of(), Map.of("*", stage));
                        var canopy = new ModelInstance(glass);
                        damage.applyTexture(canopy, state, 42);
                        assertArrayEquals(cockpit, pixels(batch, buffer, camera, canopy), stage + " must spare cockpit glass");
                        var armor = new ModelInstance(model);
                        damage.applyTexture(armor, state, 42);
                        assertFalse(Arrays.equals(bareArmor, pixels(batch, buffer, camera, armor)),
                              stage + " must still cover armor and dark details");
                    }
                    var destroyedCockpit = new ModelInstance(glass);
                    UnitDamageDisplay.show(destroyedCockpit, preview);
                    damage.applyTexture(destroyedCockpit, preview, 42);
                    assertArrayEquals(original, pixels(batch, buffer, camera, destroyedCockpit),
                          "Destroyed artwork covers glass just like armor");

                    var rebuilt = new ModelInstance(model);
                    UnitDamageDisplay.show(rebuilt, preview);
                    damage.applyTexture(rebuilt, preview, 42);
                    assertArrayEquals(original, pixels(batch, buffer, camera, rebuilt), "Rebuilding preserves the unit's splats");
                    damage.applyTexture(rebuilt, preview, 43);
                    assertFalse(Arrays.equals(original, pixels(batch, buffer, camera, rebuilt)), "Unit IDs select different splats");
                    rebuilt.nodes.first().id = "another-part";
                    damage.applyTexture(rebuilt, preview, 42);
                    assertFalse(Arrays.equals(original, pixels(batch, buffer, camera, rebuilt)), "Body parts must not repeat the same splats");
                    instance.transform.setToTranslation(8, 0, 0);
                    camera.position.x += 8;
                    camera.update();
                    assertArrayEquals(original, pixels(batch, buffer, camera, instance), "Damage stays attached during movement");
                } catch (Throwable error) { failure.set(error); }
                finally { buffer.dispose(); batch.dispose(); damage.dispose(); glass.dispose(); model.dispose(); Gdx.app.exit(); }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) { throw new AssertionError("Damage texture rendering failed", failure.get()); }
    }

    private static Model plate(Color color) {
        var builder = new ModelBuilder();
        builder.begin();
        var plate = builder.part("plate", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position
                    | VertexAttributes.Usage.Normal | VertexAttributes.Usage.ColorUnpacked
                    | VertexAttributes.Usage.TextureCoordinates,
              new Material("detail", ColorAttribute.createDiffuse(Color.WHITE)));
        plate.setColor(color);
        plate.rect(-32, -32, 0, 32, -32, 0, 32, 32, 0, -32, 32, 0, 0, 0, 1);
        return builder.end();
    }

    private static int[] pixels(ModelBatch batch, FrameBuffer buffer, OrthographicCamera camera, ModelInstance instance) {
        buffer.begin();
        Gdx.gl.glClearColor(1, 0, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera);
        batch.render(instance);
        batch.end();
        var pixels = Pixmap.createFromFrameBuffer(0, 0, 128, 128);
        try {
            int[] result = new int[112 * 112];
            for (int y = 8; y < 120; y++) {
                for (int x = 8; x < 120; x++) { result[(y - 8) * 112 + x - 8] = pixels.getPixel(x, y); }
            }
            return result;
        } finally { pixels.dispose(); buffer.end(); }
    }

    @Test
    void previewsBothDamageScalesAndRestoresActualDamage() throws Exception {
        var failure = new AtomicReference<Throwable>();
        try (var fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    fixture.entity.setArmor(0, Mek.LOC_LEFT_TORSO);
                    fixture.entity.setArmor(0, Mek.LOC_LEFT_TORSO, true);
                    fixture.entity.setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_RIGHT_LEG);
                    fixture.entity.destroyLocation(Mek.LOC_LEFT_ARM, true);
                    var tank = new MekFileParser(new File("testresources/megamek/common/units/Bulldog Medium Tank.blk")).getEntity();
                    tank.setId(2);
                    tank.setOwner(fixture.player);
                    tank.setPosition(new Coords(6, 5));
                    tank.setDeployed(true);
                    fixture.game.addEntity(tank, false);
                    var infantry = new ConvInfantry();
                    infantry.setId(3);
                    infantry.setOwner(fixture.player);
                    infantry.setPosition(new Coords(4, 5));
                    infantry.setDeployed(true);
                    infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
                    fixture.game.addEntity(infantry, false);
                    fixture.source.refresh();
                } catch (Exception error) { throw new IllegalStateException(error); }
            });
            new Lwjgl3Application(new ApplicationAdapter() {
                @Override
                public void create() {
                    var view = new GpuBattleView(fixture.source);
                    try {
                        view.create();
                        view.render();
                        verify(view, fixture);
                    } catch (Throwable error) { failure.set(error); }
                    finally { view.dispose(); Gdx.app.exit(); }
                }
            }, GpuBoardWindow.configuration(false));
        }
        if (failure.get() != null) { throw new AssertionError("Damage tuning preview failed", failure.get()); }
    }

    private static void verify(GpuBattleView view, GpuBoardFixture fixture) throws Exception {
        GpuBoardTestUi.click("tuning");
        view.render();
        CheckBox vsync = GpuBoardTestUi.stage().getRoot().findActor("tuning-vsync");
        assertTrue(vsync.isChecked());
        GpuBoardTestUi.click("tuning-vsync");
        assertFalse(vsync.isChecked());
        CheckBox override = GpuBoardTestUi.stage().getRoot().findActor("tuning-override-damage");
        Slider slider = GpuBoardTestUi.stage().getRoot().findActor("Display damage");
        SelectBox<UnitDamageDisplay.Location> location = GpuBoardTestUi.stage().getRoot().findActor("tuning-damage-location");
        assertTrue(location.isDisabled());
        assertEquals(UnitDamageDisplay.Location.ALL, location.getSelected());
        var actual = displayedDamage(view, 1);
        assertFalse(override.isChecked());
        assertTrue(slider.isDisabled());
        assertEquals(0, slider.getValue());
        assertEquals(UnitDamageDisplay.Stage.ARMOR_STRIPPED, actual.stages().get("LT"));

        GpuBoardTestUi.click("tuning-override-damage");
        assertTrue(override.isChecked());
        assertFalse(slider.isDisabled());
        assertFalse(location.isDisabled());
        ModelInstance infantryInstance = instance(view, 3);
        float[] levels = { .25f, .50f, .75f, 1, .50f, 0 };
        UnitDamageDisplay.Stage[] mekStages = { UnitDamageDisplay.Stage.ARMOR_WORN,
              UnitDamageDisplay.Stage.ARMOR_STRIPPED, UnitDamageDisplay.Stage.STRUCTURE_BATTERED,
              null, UnitDamageDisplay.Stage.ARMOR_STRIPPED, null };
        UnitDamageDisplay.Stage[] bodyStages = { UnitDamageDisplay.Stage.BODY_25, UnitDamageDisplay.Stage.BODY_50,
              UnitDamageDisplay.Stage.BODY_75, UnitDamageDisplay.Stage.BODY_100, UnitDamageDisplay.Stage.BODY_50, null };
        for (int index = 0; index < levels.length; index++) {
            slider.setValue(levels[index]);
            view.render();
            assertMaterials(view, 1, mekStages[index]);
            assertMaterials(view, 2, bodyStages[index]);
            assertInfantry(view, levels[index]);
            assertSame(infantryInstance, instance(view, 3), "Casualty preview reuses the existing formation");
            assertTrue(UnitDamageDisplay.locationParts(instance(view, 1), "LA").stream().noneMatch(part -> part.enabled));
            assertTrue(UnitDamageDisplay.locationParts(instance(view, 1), "RL").stream()
                  .allMatch(part -> part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)));
            if (levels[index] == 1) {
                assertTrue(UnitDamageDisplay.locationParts(instance(view, 1), "*").stream()
                      .allMatch(part -> part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)));
                view.boardCamera.center(UnitBounds.world(instance(view, 1)).getCenter(new Vector3()));
                view.boardCamera.zoom(.16f);
                view.boardCamera.setIsometric(true);
                view.render();
                GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"), "damage-tuning-destroyed.png"));
            }
            if (levels[index] == .75f) {
                ModelInstance before = instance(view, 1);
                SwingUtilities.invokeAndWait(fixture.source::refresh);
                for (boolean isometric : new boolean[] { true, false }) {
                    view.boardCamera.setIsometric(isometric);
                    view.render();
                    assertSame(before, instance(view, 1), "Camera and snapshot updates preserve the preview instance");
                    assertMaterials(view, 1, UnitDamageDisplay.Stage.STRUCTURE_BATTERED);
                }
                File output = new File(System.getProperty("megamek.gpu.screenshots"));
                Files.createDirectories(output.toPath());
                GpuBoardTestUi.capture(new File(output, "damage-tuning-panel.png"));
            }
        }
        location.setSelected(UnitDamageDisplay.Location.RIGHT_ARM);
        slider.setValue(.75f);
        view.render();
        var targetedStages = new HashMap<>(actual.stages());
        targetedStages.put("RA", UnitDamageDisplay.Stage.STRUCTURE_BATTERED);
        assertEquals(targetedStages, displayedDamage(view, 1).stages());
        assertNull(UnitDamageDisplay.locationParts(instance(view, 1), "CT").getFirst().material
              .get(UnitDamageDisplay.Overlay.class, UnitDamageDisplay.Overlay.TYPE));
        assertMaterials(view, 2, UnitDamageDisplay.Stage.BODY_75);
        assertInfantry(view, .75f);
        slider.setValue(1);
        view.render();
        assertEquals(Set.of("RL", "RA"), displayedDamage(view, 1).wrecked());
        assertFalse(UnitDamageDisplay.locationParts(instance(view, 1), "CT").getFirst().material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX));
        location.setSelected(UnitDamageDisplay.Location.CENTER_LEG);
        view.render();
        assertEquals(actual, displayedDamage(view, 1), "A biped has no center leg; its other locations stay unchanged");
        location.setSelected(UnitDamageDisplay.Location.RIGHT_ARM);
        slider.setValue(.5f);
        view.render();
        captureInfantry(view);
        view.render();
        GpuBoardTestUi.click("tuning-damage-location");
        assertTrue(location.getScrollPane().hasParent());
        GpuBoardTestUi.stage().act(.3f);
        GpuBoardTestUi.stage().draw();
        GpuBoardTestUi.capture(new File(System.getProperty("megamek.gpu.screenshots"), "damage-location-choices.png"));
        location.hideList();
        GpuBoardTestUi.stage().act(.3f);
        GpuBoardTestUi.click("tuning-override-damage");
        view.render();
        assertTrue(slider.isDisabled());
        assertEquals(actual, displayedDamage(view, 1), "Disabling the preview restores actual damage");
        assertTrue(displayedDamage(view, 2).isNone());
        assertInfantry(view, 0);

        GpuBoardTestUi.click("tuning-override-damage");
        slider.setValue(1);
        view.render();
        GpuBoardTestUi.click("tuning-defaults");
        view.render();
        assertTrue(vsync.isChecked(), "Defaults re-enables VSync");
        assertFalse(override.isChecked());
        assertTrue(location.isDisabled());
        assertEquals(UnitDamageDisplay.Location.ALL, location.getSelected());
        assertTrue(slider.isDisabled());
        assertEquals(0, slider.getValue());
        assertEquals(actual, displayedDamage(view, 1), "Defaults restores actual damage after the destroyed preview");
        SwingUtilities.invokeAndWait(() -> assertEquals(actual, UnitModelSelection.damage(fixture.entity)));
        SwingUtilities.invokeAndWait(() -> assertEquals(28, ((ConvInfantry) fixture.game.getEntity(3)).getActiveTroopers()));
        assertEquals(0, fixture.clicks.get(), "Tuning does not issue game commands");
        capturePatterns(view);
        assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
    }

    private static void capturePatterns(GpuBattleView view) throws Exception {
        var batch = new ModelBatch(GpuUnitShader.provider());
        try {
            var damage = (UnitDamageDisplay) field(view, "damageDisplay");
            for (float loss : new float[] { .25f, .75f, 1 }) {
                var preview = UnitDamageDisplay.preview(BoardScene.LocationDamage.NONE, true, loss);
                var first = new ModelInstance(instance(view, 1).model);
                var second = new ModelInstance(first.model);
                UnitDamageDisplay.show(first, preview);
                UnitDamageDisplay.show(second, preview);
                damage.applyTexture(first, preview, 42);
                damage.applyTexture(second, preview, 43);
                first.transform.setToTranslation(-38, 0, 0);
                second.transform.setToTranslation(38, 0, 0);
                GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(first, second),
                      "damage-pattern-" + Math.round(loss * 100), 170, 28);
            }
        } finally { batch.dispose(); }
    }

    private static void assertInfantry(GpuBattleView view, float loss) throws Exception {
        ModelInstance infantry = instance(view, 3);
        int figures = 0, fallen = 0;
        for (var container : infantry.nodes) {
            if (!container.id.startsWith("trooper-")) { continue; }
            figures++;
            var root = container.getChild(0);
            var up = new Vector3(Vector3.Z).rot(root.localTransform);
            if (Math.abs(up.z) < .1f) { fallen++; }
        }
        assertEquals(6, figures, "The full platoon uses six representative figures");
        assertEquals(Math.round(figures * loss), fallen, "Damage previews proportional casualties");
        assertTrue(displayedDamage(view, 3).isNone(), "Troop casualties never use burnt armor overlays");
    }

    private static void captureInfantry(GpuBattleView view) throws Exception {
        var preview = new ModelInstance(instance(view, 3));
        preview.transform.setTranslation(0, 0, 0);
        var batch = new ModelBatch(GpuUnitShader.provider());
        try {
            GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(preview), "infantry-half-casualties", 90, 8);
        } finally { batch.dispose(); }
    }

    private static void assertMaterials(GpuBattleView view, int id, UnitDamageDisplay.Stage expected) throws Exception {
        var overlays = (Map<?, ?>) field(field(view, "damageDisplay"), "overlays");
        if (expected != null) { assertNotNull(overlays.get(expected), "The selected overlay must load"); }
        int overlaid = 0;
        for (var part : UnitDamageDisplay.locationParts(instance(view, id), "*")) {
            if (!part.enabled) { continue; }
            var overlay = part.material.get(UnitDamageDisplay.Overlay.class, UnitDamageDisplay.Overlay.TYPE);
            if (part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)) {
                assertNotNull(overlay, "Destroyed parts must receive their artwork");
                assertSame(field(field(view, "damageDisplay"), "texture"), overlay.texture,
                      "Destroyed texture wins over armor and structure overlays");
                assertFalse(part.material.has(TextureAttribute.Diffuse), "Damage occupies only the shared damage slot");
            } else if (expected == null) {
                assertNull(overlay, "Lowering damage clears the previous overlay");
            } else {
                assertNotNull(overlay);
                assertSame(overlays.get(expected), overlay.texture, "Only the highest damage stage is applied");
                overlaid++;
            }
        }
        if (expected != null) { assertTrue(overlaid > 0); }
    }

    private static ModelInstance instance(GpuBattleView view, int id) throws Exception {
        var instance = (ModelInstance) ((Map<?, ?>) field(view, "unitInstances")).get(id + ":-1");
        assertNotNull(instance);
        return instance;
    }

    private static BoardScene.LocationDamage displayedDamage(GpuBattleView view, int id) throws Exception {
        return (BoardScene.LocationDamage) ((Map<?, ?>) field(view, "unitDamage")).get(id + ":-1");
    }
}
