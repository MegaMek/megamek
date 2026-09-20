/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.JsonReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Mek;
import megamek.common.units.BipedMek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;

/** Native review uses the production assembler and actual loaded units, including an unbaked same-name refit. */
final class GpuMekAssemblyReview {
    private GpuMekAssemblyReview() { }

    static void verify(GpuUnitModels library, ModelBatch batch) throws Exception {
        EquipmentType.initializeTypes();
        var tileset = new MekTileset(new File(Configuration.dataDir(), "images/units"));
        tileset.loadFromFile("mekset.txt");
        List<ModelInstance> instances = new ArrayList<>();
        List<Model> references = new ArrayList<>();
        List<ModelInstance> comparisons = new ArrayList<>();
        List<Object> evidence = new ArrayList<>();
        File referenceRoot = new File(System.getProperty("megamek.gpu.referenceModels"), "units");
        var original = new JsonReader().parse(new FileHandle(new File(referenceRoot, "manifest.json"))).get("variants");
        String[][] cases = {
              { "warhammer", "3039u/Warhammer WHM-6R.mtf" },
              { "mad-cat", "3050U/Mad Cat (Timber Wolf) Prime.mtf" },
              { "atlas", "3039u/Atlas AS7-D.mtf" },
              { "archer", "3039u/Archer ARC-2R.mtf" },
              { "mackie", "3075/Mackie MSK-6S.mtf" }
        };
        int id = 700;
        for (String[] entry : cases) {
            Mek mek = (Mek) new MekFileParser(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"),
                  "meks/" + entry[1]).getEntity();
            assertNotNull(mek, entry[1]);
            mek.setId(id++);
            mek.setExternalSearchlight(entry[0].equals("warhammer") || entry[0].equals("mackie"));
            BoardScene.UnitModel selected = selection(mek, tileset, entry[0]);
            GpuMeeple visual = library.get(selected, mek.getId());
            assertNotNull(visual, entry[0]);
            assertTrue(visual.modularCoordinates());
            assertTrue(visual.turnsUpperBody());
            assertSame(visual, library.get(selected, mek.getId()));
            for (var mount : selected.state().structure().equipment()) {
                if (mount.policy().allowsFallback()) {
                    assertEquals(1, visual.equipment().stream().filter(binding -> binding.index() == mount.index()).count(),
                          entry[0] + ": " + mount.internalName());
                }
            }
            assertTrue(visual.equipment().stream().noneMatch(UnitEquipmentAssembly.Binding::embedded), entry[0]);
            var drawn = new ModelInstance(visual.instance.model);
            visual.showEquipment(drawn, selected.state().appearance());
            instances.add(drawn);
            var previous = original.get(mek.getShortNameRaw());
            assertNotNull(previous, mek.getShortNameRaw());
            Model old = new G3dModelLoader(new JsonReader()).loadModel(new FileHandle(new File(referenceRoot,
                  previous.getString("asset"))));
            references.add(old);
            var oldInstance = new ModelInstance(old);
            oldInstance.transform.scale(1, 1, 54);
            comparisons.add(oldInstance);
            comparisons.add(new ModelInstance(visual.instance.model));
            evidence.add(Map.of("chassis", entry[0], "bindings", visual.equipment()));
            if (entry[0].equals("warhammer")) {
                String name = mek.getShortNameRaw();
                var added = mek.addEquipment(EquipmentType.get("ISSmallLaser"), Mek.LOC_LEFT_ARM, true);
                assertEquals(name, mek.getShortNameRaw());
                var refit = library.get(selection(mek, tileset, entry[0]), mek.getId());
                assertNotSame(visual, refit);
                assertEquals(visual.equipment().size() + 1, refit.equipment().size());
                var binding = refit.equipment().stream().filter(item -> item.index() == added.getEquipmentNum())
                      .findFirst().orElseThrow();
                var posed = new ModelInstance(refit.instance.model);
                var before = new Vector3();
                var after = new Vector3();
                var direction = new Vector3();
                UnitModelAttachment.emitter(posed, binding.emitters().getFirst(), before, direction);
                assertTrue(direction.y < -.99f, direction.toString());
                refit.turnUpperBody(posed, 60);
                posed.getNode("LA-forearm").rotation.set(Vector3.X, 20);
                posed.calculateTransforms();
                UnitModelAttachment.emitter(posed, binding.emitters().getFirst(), after, direction);
                assertFalse(before.epsilonEquals(after, .01f));
                UnitDamageDisplay.show(posed, new BoardScene.LocationDamage(Set.of("LA"), Set.of()));
                assertTrue(UnitDamageDisplay.locationParts(posed, "LA").stream().noneMatch(part -> part.enabled));
                instances.add(new ModelInstance(refit.instance.model));
            }
        }
        for (int index = 0; index < instances.size(); index++) {
            instances.get(index).transform.setToTranslation((1 - index % 3) * 82, (index / 3 == 0 ? 1 : -1) * 70, 0);
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(System.getProperty("megamek.gpu.screenshots"),
              "runtime-mek-bindings.json"), evidence);
        GpuModularUnitModelsSmokeTest.renderReview(batch, instances, "runtime-meks", 310, 20);
        try {
            for (int pair = 0; pair < comparisons.size() / 2; pair++) {
                var before = comparisons.get(pair * 2);
                var after = comparisons.get(pair * 2 + 1);
                before.transform.setTranslation(40, 0, 0);
                after.transform.setTranslation(-40, 0, 0);
                GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(before, after),
                      "runtime-compare-" + cases[pair][0], 180, 25);
            }
        } finally {
            references.forEach(Model::dispose);
        }
        verifyFallbacks(library, tileset, batch);
        verifyVariants(library, tileset);
    }

    private static void verifyFallbacks(GpuUnitModels library, MekTileset tileset, ModelBatch batch) throws Exception {
        List<ModelInstance> gallery = new ArrayList<>();
        int id = 800;
        for (Mek mek : List.of(new BipedMek(), new QuadMek(), new TripodMek())) {
            mek.setId(id++);
            mek.setChassis("Uncatalogued custom chassis");
            mek.addEquipment(EquipmentType.get("PPC"), Mek.LOC_RIGHT_TORSO);
            float previous = 0;
            for (int weight : new int[] { 20, 40, 60, 80, 150 }) {
                mek.setWeight(weight);
                var selected = UnitModelSelection.capture(mek, -1, false, tileset);
                var visual = library.get(selected, mek.getId());
                assertNotNull(visual, mek.getClass().getSimpleName());
                assertTrue(visual.modularCoordinates());
                assertEquals(1, visual.equipment().size());
                assertFalse(visual.equipment().getFirst().embedded());
                float size = visual.instance.calculateBoundingBox(new com.badlogic.gdx.math.collision.BoundingBox())
                      .getDimensions(new Vector3()).len();
                assertTrue(size > previous);
                previous = size;
                var drawn = new ModelInstance(visual.instance.model);
                drawn.transform.setTranslation((2 - gallery.size() % 5) * 77, (1 - gallery.size() / 5) * 96, 0);
                gallery.add(drawn);
            }
            var missing = new BoardScene.UnitModel("units/missing.json",
                  UnitModelSelection.capture(mek, -1, false, tileset).asset(), "custom", 1, 0,
                  BoardScene.LocationDamage.NONE, UnitModelState.capture(mek));
            var recovered = library.get(missing, mek.getId());
            assertNotNull(recovered);
            assertSame(recovered, library.get(missing, mek.getId()));
        }
        GpuModularUnitModelsSmokeTest.renderReview(batch, gallery, "runtime-mek-fallbacks", 470, 14);
    }

    private static void verifyVariants(GpuUnitModels library, MekTileset tileset) throws Exception {
        List<Object> report = new ArrayList<>();
        try (ZipFile zip = new ZipFile(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"))) {
            for (var entry : zip.stream().filter(item -> !item.isDirectory()
                  && item.getName().matches(".*/(Warhammer WHM-|Mad Cat \\(Timber Wolf\\) ).*\\.mtf")).toList()) {
                Mek mek = (Mek) new MekFileParser(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"),
                      entry.getName()).getEntity();
                mek.setId(999);
                var selected = UnitModelSelection.capture(mek, -1, false, tileset);
                var visual = library.get(selected, mek.getId());
                assertNotNull(visual, entry.getName());
                assertTrue(visual.modularCoordinates(), entry.getName());
                long required = selected.state().structure().equipment().stream().filter(m -> m.policy().allowsFallback()).count();
                long bound = visual.equipment().stream().filter(binding -> binding.index() >= 0)
                      .filter(binding -> selected.state().structure().equipment().stream().anyMatch(m ->
                            m.index() == binding.index() && m.policy().allowsFallback())).count();
                assertEquals(required, bound, entry.getName());
                report.add(Map.of("unit", mek.getShortNameRaw(), "source", entry.getName(),
                      "modules", visual.equipment().size(), "embedded", visual.equipment().stream()
                            .filter(UnitEquipmentAssembly.Binding::embedded).map(UnitEquipmentAssembly.Binding::index).toList()));
            }
        }
        assertFalse(report.isEmpty());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(System.getProperty("megamek.gpu.screenshots"),
              "runtime-variant-coverage.json"), report);
    }

    private static BoardScene.UnitModel selection(Mek mek, MekTileset tileset, String chassis) {
        var selected = UnitModelSelection.capture(mek, -1, false, tileset);
        assertEquals("units/modular/meks/" + chassis + ".json", selected.asset());
        return selected;
    }
}
