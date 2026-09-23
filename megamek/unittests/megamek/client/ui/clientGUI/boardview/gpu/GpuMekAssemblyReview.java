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

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.JsonReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.client.ui.tileset.MekTileset;
import megamek.common.Configuration;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;

/** Native review uses the production assembler and actual loaded units, including an unbaked same-name refit. */
final class GpuMekAssemblyReview {
    /** Steps in the rendered arm-flip sweep; 12 gives a frame every 15 degrees. */
    private static final int FLIP_FRAMES = 12;

    private GpuMekAssemblyReview() { }

    static void verify(GpuUnitModels library, ModelBatch batch) throws Exception {
        EquipmentType.initializeTypes();
        var tileset = new MekTileset(new File(Configuration.dataDir(), "images/units"));
        tileset.loadFromFile("mekset.txt");
        List<ModelInstance> instances = new ArrayList<>();
        List<Model> references = new ArrayList<>();
        List<ModelInstance> comparisons = new ArrayList<>();
        List<String> comparisonNames = new ArrayList<>();
        List<ModelInstance> soloReviews = new ArrayList<>();
        List<String> soloNames = new ArrayList<>();
        List<Object> evidence = new ArrayList<>();
        File referenceRoot = new File(System.getProperty("megamek.gpu.referenceModels"), "units");
        var original = new JsonReader().parse(new FileHandle(new File(referenceRoot, "manifest.json"))).get("variants");
        // Label, body descriptor, unit file. The label names the review image, so one chassis can
        // appear more than once to show how different loadouts sit on the same body.
        String[][] cases = {
              { "warhammer", "warhammer", "3039u/Warhammer WHM-6R.mtf" },
              { "mad-cat", "mad-cat", "3050U/Mad Cat (Timber Wolf) Prime.mtf" },
              { "atlas", "atlas", "3039u/Atlas AS7-D.mtf" },
              { "archer", "archer", "3039u/Archer ARC-2R.mtf" },
              { "mackie", "mackie", "3075/Mackie MSK-6S.mtf" },
              { "rifleman-3n", "rifleman", "3039u/Rifleman RFL-3N.mtf" },
              { "rifleman-4d", "rifleman", "3039u/Rifleman RFL-4D.mtf" },
              { "rifleman-3c", "rifleman", "3039u/Rifleman RFL-3C.mtf" },
              { "battlemaster-1g", "battlemaster", "3039u/BattleMaster BLR-1G.mtf" },
              { "battlemaster-3m", "battlemaster", "3085u/Phoenix/BattleMaster BLR-3M.mtf" },
              // Each holds a different family of gun in the right hand: Heavy PPC, Gauss rifle, rotary autocannon.
              { "battlemaster-6g", "battlemaster", "Rec Guides ilClan/Vol 3/BattleMaster BLR-6G.mtf" },
              { "battlemaster-4s", "battlemaster", "3085u/Phoenix/BattleMaster BLR-4S.mtf" },
              { "battlemaster-6r", "battlemaster", "Rec Guides ilClan/Vol 3/BattleMaster BLR-6R.mtf" },
              // Two guns in one hand, a Light Gauss Rifle and an ER Large Laser: only the larger is held.
              { "battlemaster-5m", "battlemaster", "3085u/Phoenix/BattleMaster BLR-5M.mtf" },
              // Guns hang under the gun pods and under the chin: machine guns, an SRM rack, a laser mix.
              { "locust-1v", "locust", "3039u/Locust LCT-1V.mtf" },
              { "locust-1v2", "locust", "3085u/Phoenix/Locust LCT-1V2.mtf" },
              { "locust-5v", "locust", "3085u/Phoenix/Locust LCT-5V.mtf" },
              { "locust-3m", "locust", "Rec Guides ilClan/Vol 16/Locust LCT-3M.mtf" },
              // The only Locust with a head weapon: it shares the chin turret with the centre torso's.
              { "locust-6m", "locust", "3085u/Phoenix/Locust LCT-6M.mtf" },
              { "panther-9r", "panther", "3039u/Panther PNT-9R.mtf" },
              // Two launchers stacked over and under in the centre torso.
              { "panther-10k2", "panther", "3085u/Cutting Edge/Panther PNT-10K2.mtf" },
              { "panther-16k", "panther", "3050U/Panther PNT-16K.mtf" },
              // An Arrow IV split across the right arm and right torso.
              { "urbanmech-aiv", "urbanmech", "3085u/ONN/UrbanMech UM-AIV.mtf" }
        };
        int id = 700;
        for (String[] entry : cases) {
            Mek mek = (Mek) new MekFileParser(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"),
                  "meks/" + entry[2]).getEntity();
            assertNotNull(mek, entry[2]);
            mek.setId(id++);
            mek.setExternalSearchlight(entry[1].equals("warhammer") || entry[1].equals("mackie"));
            BoardScene.UnitModel selected = selection(mek, tileset, entry[1]);
            GpuUnitModel visual = library.get(selected, mek.getId());
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
            // A chassis authored after the bakes were frozen has no legacy reference to sit beside.
            // The Locust hangs its guns under its pods, which only the six-view sheet shows from below.
            if (previous != null && !entry[0].startsWith("locust") && !entry[0].startsWith("panther")
                  && !entry[0].startsWith("urbanmech")) {
                Model old = new G3dModelLoader(new JsonReader()).loadModel(new FileHandle(new File(referenceRoot,
                      previous.getString("asset"))));
                references.add(old);
                var oldInstance = new ModelInstance(old);
                oldInstance.transform.scale(1, 1, 54);
                comparisons.add(oldInstance);
                comparisons.add(new ModelInstance(visual.instance.model));
                comparisonNames.add(mek.getShortNameRaw());
            } else {
                var alone = new ModelInstance(visual.instance.model);
                visual.showEquipment(alone, selected.state().appearance());
                soloReviews.add(alone);
                soloNames.add(mek.getShortNameRaw());
            }
            evidence.add(Map.of("unit", mek.getShortNameRaw(), "chassis", entry[1], "bindings", visual.equipment()));
            if (entry[0].equals("warhammer")) {
                String name = mek.getShortNameRaw();
                var added = mek.addEquipment(EquipmentType.get("ISSmallLaser"), Mek.LOC_LEFT_ARM, true);
                assertEquals(name, mek.getShortNameRaw());
                var refit = library.get(selection(mek, tileset, entry[1]), mek.getId());
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
            // Three to a row, so a seventh case lands on its own spot instead of over the fourth.
            instances.get(index).transform.setToTranslation((1 - index % 3) * 82, (1 - index / 3) * 70, 0);
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
                      "runtime-compare-" + comparisonNames.get(pair), 180, 25);
            }
            for (int index = 0; index < soloReviews.size(); index++) {
                // A twenty-tonner framed like an assault Mek fills a sliver of its cell, so it is framed closer.
                boolean small = soloNames.get(index).startsWith("Locust") || soloNames.get(index).startsWith("Panther")
                      || soloNames.get(index).startsWith("UrbanMe");
                GpuModularUnitModelsSmokeTest.renderFullReview(batch, List.of(soloReviews.get(index)),
                      "runtime-new-" + soloNames.get(index), soloNames.get(index), small ? 52 : 78, small ? 23 : 25);
            }
        } finally {
            references.forEach(Model::dispose);
        }
        verifyFallbacks(library, tileset, batch);
        verifyArmFlip(library, tileset, batch);
        verifyVariants(library, tileset);
        // Chassis name as the unit files spell it, paired with the body descriptor it resolves to.
        String[][] sheets = {
              { "Rifleman", "rifleman" }, { "BattleMaster", "battlemaster" }, { "Atlas", "atlas" },
              { "Warhammer", "warhammer" }, { "Archer", "archer" }, { "Marauder", "marauder" },
              { "Mad Cat (Timber Wolf)", "mad-cat" }, { "Locust", "locust" }, { "Mackie", "mackie" },
              { "King Crab", "king-crab" }, { "Panther", "panther" }
        };
        for (String[] sheet : sheets) {
            GpuVariantSheetReview.render(library, batch, tileset, sheet[0],
                  "units/modular/meks/" + sheet[1] + ".json");
        }
    }

    /**
     * A Rifleman carries its guns at the elbow, so flipping its arms is the only way it brings them onto a rear arc.
     * Checks the pose the renderer actually produces: each arm's guns must finish behind the shoulder they hang
     * from, and must get there without the arm leaving the space it already occupied to either side.
     */
    private static void verifyArmFlip(GpuUnitModels library, MekTileset tileset, ModelBatch batch) throws Exception {
        Mek mek = (Mek) new MekFileParser(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"),
              "meks/3039u/Rifleman RFL-3N.mtf").getEntity();
        assertNotNull(mek);
        mek.setId(760);
        assertTrue(mek.canFlipArms(), "a Rifleman has no lower arm or hand actuators, so it can flip");
        BoardScene.UnitModel selected = selection(mek, tileset, "rifleman");
        GpuUnitModel visual = library.get(selected, mek.getId());
        assertTrue(visual.flipsArms(), "the authored body must carry LA and RA nodes");

        var posed = new ModelInstance(visual.instance.model);
        visual.showEquipment(posed, selected.state().appearance());
        Vector3 forward = armReach(posed, "RA");
        float restWidth = armSpan(posed, "RA");

        visual.flipArms(posed, 180);
        Vector3 flipped = armReach(posed, "RA");
        assertTrue(flipped.y < forward.y, "flipped arms must reach behind where they reached in front: "
              + forward + " -> " + flipped);
        assertEquals(restWidth, armSpan(posed, "RA"), .01f,
              "a turn about the shoulder's own left-right axis must not move the arm sideways");

        // Half way over is the moment the arm is clear of its housing; it must still be the same arm.
        visual.flipArms(posed, 90);
        assertEquals(restWidth, armSpan(posed, "RA"), .01f);

        // The board view poses this every frame, after the animator has reset each joint to its rest
        // pose. Asking for the same angle twice must land in the same place: an arm built from the
        // previous frame's rotation rather than from the rest pose winds further round every frame.
        visual.flipArms(posed, 180);
        Vector3 once = armReach(posed, "RA");
        visual.flipArms(posed, 180);
        assertEquals(once.y, armReach(posed, "RA").y, .01f, "posing the same angle twice must not accumulate");
        assertEquals(flipped.y, once.y, .01f, "and must match the pose reached by way of 90 degrees");

        // Zero is deliberately a no-op: forward arms belong to the animator's walk cycle, and pinning
        // them to the rest pose here would flatten it on every unflipped unit on the board.
        visual.flipArms(posed, 0);
        assertEquals(once.y, armReach(posed, "RA").y, .01f, "zero must leave the arms to the animator");

        visual.flipArms(posed, 180);
        GpuModularUnitModelsSmokeTest.renderFullReview(batch, List.of(posed), "runtime-flip-Rifleman",
              "Rifleman RFL-3N arms flipped", 78, 25);

        // A frame every 15 degrees through the swing, so the movement can be watched rather than inferred.
        // This is where a clash would show: the arm leaves its housing around half way over.
        for (int step = 0; step <= FLIP_FRAMES; step++) {
            float degrees = step * 180f / FLIP_FRAMES;
            visual.flipArms(posed, degrees);
            // Wide enough to hold the whole sweep: half way over, the arms stand above the antenna.
            GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(posed),
                  String.format("flip-frame-%02d", step), 135, 28);
        }
        visual.flipArms(posed, 0);
    }

    /** @return the furthest point the named arm reaches, in the model's own coordinates */
    private static Vector3 armReach(ModelInstance posed, String arm) {
        BoundingBox box = new BoundingBox();
        posed.getNode(arm).calculateBoundingBox(box, true);
        Vector3 corner = new Vector3();
        posed.getNode(arm).globalTransform.getTranslation(corner);
        return new Vector3(box.getCenterX(), box.min.y, box.getCenterZ()).add(corner);
    }

    /** @return how wide the named arm sits, which a flip about the lateral axis must leave alone */
    private static float armSpan(ModelInstance posed, String arm) {
        BoundingBox box = new BoundingBox();
        posed.getNode(arm).calculateBoundingBox(box, true);
        return box.getWidth();
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
