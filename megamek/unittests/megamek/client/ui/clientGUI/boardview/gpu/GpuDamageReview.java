/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static megamek.client.ui.clientGUI.boardview.gpu.GpuCamouflageReview.field;
import static megamek.client.ui.clientGUI.boardview.gpu.GpuCamouflageReview.parts;
import static megamek.client.ui.clientGUI.boardview.gpu.GpuCamouflageReview.texture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.BipedMek;
import megamek.common.equipment.IArmorState;
import megamek.common.units.Mek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;

/** Damage materials and collectable limb props through the real asset library, Swing snapshots and board renderer. */
final class GpuDamageReview {
    private GpuDamageReview() { }

    static void verify(GpuUnitModels library) throws Exception {
        var damage = new UnitDamageDisplay();
        var batch = new ModelBatch(GpuUnitCamouflage.shaders());
        try {
            var mek = new BipedMek();
            for (int location = 0; location < mek.locations(); location++) {
                mek.initializeInternal(10, location);
            }
            mek.addEquipment(EquipmentType.get("PPC"), Mek.LOC_LEFT_ARM);
            mek.addEquipment(EquipmentType.get("PPC"), Mek.LOC_RIGHT_ARM);
            var model = library.get(UnitCamouflageTest.selection(mek), 6300);
            var intact = new ModelInstance(model.instance.model);
            var old = new ModelInstance(model.instance.model);
            var wrecked = new ModelInstance(model.instance.model);
            var locations = new BoardScene.LocationDamage(Set.of(), Set.of("RT", "LA", "RL"));
            UnitDamageDisplay.show(old, locations);
            for (var part : parts(old.nodes)) {
                if (part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)) {
                    part.material.set(ColorAttribute.createDiffuse(.13f, .12f, .11f, 1));
                }
            }
            UnitDamageDisplay.show(wrecked, locations);
            damage.applyTexture(wrecked);
            var texture = texture(UnitDamageDisplay.locationParts(wrecked, "RL").getFirst());
            assertNotNull(texture);
            assertEquals(128, texture.getWidth());
            assertEquals(128, texture.getHeight());
            var pixels = new Pixmap(new FileHandle(new File(Configuration.dataDir(), "models/units/textures/destroyed-armor.png")));
            try {
                Set<Integer> grays = new java.util.HashSet<>();
                for (int y = 0; y < pixels.getHeight(); y++) {
                    for (int x = 0; x < pixels.getWidth(); x++) {
                        int rgba = pixels.getPixel(x, y);
                        int gray = rgba >>> 24;
                        assertTrue(gray < 150);
                        assertEquals(gray, (rgba >>> 16) & 255);
                        assertEquals(gray, (rgba >>> 8) & 255);
                        grays.add(gray);
                    }
                }
                assertTrue(grays.size() > 16, "Scorch artwork must retain visible tonal variation");
            } finally {
                pixels.dispose();
            }
            for (var part : UnitDamageDisplay.locationParts(wrecked, "LA")) {
                assertSame(texture, texture(part), "Attached weapons and anatomy share the damage texture");
                assertEquals(Color.WHITE, part.material.get(ColorAttribute.class, ColorAttribute.Diffuse).color);
                assertFalse(part.material.has(ColorAttribute.Emissive));
            }
            assertTrue(parts(intact.nodes).stream().allMatch(part -> part.enabled && texture(part) == null));
            old.transform.setToTranslation(-43, 0, 0);
            wrecked.transform.setToTranslation(43, 0, 0);
            GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(old, wrecked), "runtime-damage-before-after", 190, 20);

            for (int location : new int[] { Mek.LOC_HEAD, Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_LEG }) {
                mek.destroyLocation(location, true);
            }
            var detached = new ModelInstance(model.instance.model);
            UnitDamageDisplay.show(detached, UnitModelSelection.damage(mek));
            for (String location : List.of("HD", "LA", "RL")) {
                assertTrue(UnitDamageDisplay.locationParts(detached, location).stream().noneMatch(part -> part.enabled),
                      "A blown-off location hides all owned anatomy and equipment: " + location);
            }
            assertTrue(UnitDamageDisplay.locationParts(detached, "RA").stream().allMatch(part -> part.enabled));
            detached.transform.setToTranslation(43, 0, 0);
            intact.transform.setToTranslation(-43, 0, 0);
            GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(intact, detached), "runtime-blown-off-locations", 190, 20);
            int handle = texture.getTextureObjectHandle();
            damage.dispose();
            assertFalse(Gdx.gl.glIsTexture(handle), "The view releases its shared damage texture");
        } finally {
            damage.dispose();
            batch.dispose();
        }
        groundRemains(library);
    }

    private static void groundRemains(GpuUnitModels library) throws Exception {
        var coords = new Coords(5, 5);
        try (var fixture = GpuBoardFixture.create()) {
            SwingUtilities.invokeAndWait(() -> {
                Hex hex = new Hex(2);
                hex.addTerrain(new Terrain(Terrains.ARMS, 2));
                hex.addTerrain(new Terrain(Terrains.LEGS, 1));
                fixture.game.getBoard().setHex(coords, hex);
                fixture.entity.destroyLocation(Mek.LOC_LEFT_ARM, true);
                fixture.entity.setInternal(IArmorState.ARMOR_DESTROYED, Mek.LOC_RIGHT_LEG);
                fixture.source.refresh();
            });
            var view = new GpuBattleView(fixture.source);
            try {
                view.create();
                view.render();
                var terrain = (GpuTerrain) field(view, "terrain");
                var unit = (ModelInstance) ((Map<?, ?>) field(view, "unitInstances")).get("1:-1");
                assertTrue(UnitDamageDisplay.locationParts(unit, "LA").stream().noneMatch(part -> part.enabled));
                assertTrue(UnitDamageDisplay.locationParts(unit, "RL").stream()
                      .allMatch(part -> texture(part) != null && texture(part).getWidth() == 128));
                var scene = (BoardScene) field(view, "scene");
                var tile = scene.tile(coords);
                assertNotNull(tile.decals(), "The classic limb marker remains available as fallback");
                assertNotNull(tile.decalsWithoutLimbs(), "An empty replacement decal is retained, not mistaken for unavailable");
                assertNotEquals(tile.decals(), tile.decalsWithoutLimbs());
                var model = (Model) field(terrain, "limbModel");
                assertNotNull(model);
                assertSame(((GpuUnitModels) field(view, "unitModels")).equipment("Limb Club").model(), model);
                var props = limbInstances(terrain, model);
                assertEquals(3, props.size());
                for (var prop : props) {
                    assertEquals(2 * BoardGeometry.LEVEL + .12f * BoardGeometry.HEX_SCALE,
                          UnitBounds.world(prop).min.z, .01f);
                }
                assertSame(tile.decalsWithoutLimbs(), selectedDecals(terrain, coords));
                view.boardCamera.center(BoardGeometry.center(coords, 2));
                view.boardCamera.zoom(.16f);
                capture(view, "runtime-limb-remains");
                var disabled = new GpuTerrain();
                var missingLibrary = new GpuUnitModels(Configuration.dataDir().toPath().resolve("missing-models"));
                var missing = new GpuTerrain(missingLibrary);
                try {
                    disabled.update(scene);
                    missing.update(scene);
                    assertNull(field(disabled, "limbModel"));
                    assertNull(field(missing, "limbModel"));
                    assertSame(tile.decals(), selectedDecals(disabled, coords));
                    assertSame(tile.decals(), selectedDecals(missing, coords));
                } finally {
                    disabled.dispose();
                    missing.dispose();
                    missingLibrary.dispose();
                }
                SwingUtilities.invokeAndWait(() -> {
                    Hex hex = fixture.game.getBoard().getHex(coords).duplicate();
                    hex.addTerrain(new Terrain(Terrains.ARMS, 1));
                    fixture.game.getBoard().setHex(coords, hex);
                    fixture.source.refresh();
                });
                view.render();
                var remaining = limbInstances(terrain, model);
                assertEquals(2, remaining.size());
                assertTrue(remaining.stream().allMatch(after -> props.stream()
                      .anyMatch(before -> java.util.Arrays.equals(before.transform.val, after.transform.val))),
                      "Picking up a limb preserves the other placements");
                SwingUtilities.invokeAndWait(() -> {
                    fixture.game.getBoard().setHex(coords, new Hex(2));
                    fixture.source.refresh();
                });
                view.render();
                assertTrue(limbInstances(terrain, model).isEmpty());
                assertTrue(parts(new ModelInstance(library.equipment("Limb Club").model()).nodes)
                      .stream().allMatch(part -> part.enabled), "Terrain must not alter shared equipment");
            } finally {
                view.dispose();
            }
        }
    }

    private static BoardScene.Pixels selectedDecals(GpuTerrain terrain, Coords coords) throws Exception {
        var decals = field(terrain, "decals");
        var entry = ((Map<?, ?>) field(decals, "entries")).get(coords);
        return (BoardScene.Pixels) field(field(entry, "images"), "color");
    }

    private static List<ModelInstance> limbInstances(GpuTerrain terrain, Model model) throws Exception {
        var result = new ArrayList<ModelInstance>();
        for (Object chunk : (List<?>) field(terrain, "chunks")) {
            for (Object prop : (List<?>) field(chunk, "props")) {
                var instance = (ModelInstance) field(prop, "instance");
                if (instance.model == model) {
                    result.add(instance);
                }
            }
        }
        return result;
    }

    private static void capture(GpuBattleView view, String name) {
        for (boolean top : new boolean[] { false, true }) {
            view.boardCamera.setIsometric(!top);
            view.render();
            var image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            try {
                PixmapIO.writePNG(new FileHandle(new File(System.getProperty("megamek.gpu.screenshots"),
                      name + "-" + (top ? "top" : "isometric") + ".png")), image, -1, true);
            } finally {
                image.dispose();
            }
        }
    }
}
