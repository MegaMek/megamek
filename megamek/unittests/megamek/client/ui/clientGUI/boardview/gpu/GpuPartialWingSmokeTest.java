/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import megamek.common.Configuration;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.TripodMek;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Native evidence for universal rear attachment, using the same assembly and meshes as both board cameras. */
@Tag("on-demand")
class GpuPartialWingSmokeTest {
    private static final String ROOT = "units/modular/";

    @Test
    void wingsFitEveryMekBodyAndFollowTheirTorso() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Lwjgl3Application(new ApplicationAdapter() {
            @Override
            public void create() {
                var library = new GpuUnitModels();
                var batch = new ModelBatch();
                try {
                    EquipmentType.initializeTypes();
                    verifyAllBodies(library);
                    renderRefits(library, batch);
                    assertEquals(GL20.GL_NO_ERROR, Gdx.gl.glGetError());
                } catch (Throwable error) {
                    failure.set(error);
                } finally {
                    batch.dispose();
                    library.dispose();
                    Gdx.app.exit();
                }
            }
        }, GpuBoardWindow.configuration(false));
        if (failure.get() != null) {
            throw new AssertionError("Partial wing review failed", failure.get());
        }
    }

    private static void verifyAllBodies(GpuUnitModels library) throws Exception {
        var folder = Configuration.dataDir().toPath().resolve("models/" + ROOT + "meks");
        int checked = 0;
        try (var paths = Files.list(folder)) {
            for (var path : paths.filter(file -> file.toString().endsWith(".json")).sorted().toList()) {
                String asset = ROOT + "meks/" + path.getFileName();
                var recipe = library.descriptor(asset);
                Supplier<Mek> factory = switch (recipe.getString("configuration")) {
                    case "quad" -> QuadMek::new;
                    case "tripod" -> TripodMek::new;
                    default -> BipedMek::new;
                };
                var bare = library.get(selection(asset, factory.get()), 8000);
                assertNotNull(bare, asset);
                assertTrue(bare.equipment().isEmpty(), "A Mek without Partial Wing must stay bare: " + asset);
                Vector3 attachment = null;
                for (String type : List.of("ISPartialWing", "CLPartialWing")) {
                    for (int location : new int[] { Mek.LOC_LEFT_TORSO, Mek.LOC_RIGHT_TORSO, Mek.LOC_CENTER_TORSO }) {
                        Mek mek = factory.get();
                        var wing = mek.addEquipment(EquipmentType.get(type), location);
                        // Spread criticals still refer to one Mounted item and must not create a second wing pair.
                        mek.addEquipment(wing, location == Mek.LOC_LEFT_TORSO ? Mek.LOC_RIGHT_TORSO : Mek.LOC_LEFT_TORSO, false);
                        var visual = library.get(selection(asset, mek), 8001);
                        assertNotNull(visual, asset);
                        assertEquals(1, visual.equipment().size(), asset);
                        var binding = visual.equipment().getFirst();
                        assertFalse(binding.embedded(), asset);
                        assertTrue(binding.emitters().isEmpty(), "Wings must not create weapon or exhaust emitters");
                        assertEquals(wing.getEquipmentNum(), binding.index());
                        assertEquals(mek.getLocationAbbr(location), binding.location());
                        Node node = visual.instance.getNode(binding.node());
                        String torso = library.modular(recipe.getString("body")).descriptor().joints().get("torso");
                        assertEquals(torso, node.getParent().id, asset);
                        var position = node.globalTransform.getTranslation(new Vector3());
                        var probe = new Ray(new Vector3(position).add(0, -1, 0), Vector3.Y);
                        assertTrue(new UnitPicking().distance(bare.instance, probe) <= 4,
                              "The wing root must meet the back, not float above it: " + asset + " at " + position);
                        if (attachment == null) {
                            attachment = position;
                        } else {
                            assertTrue(attachment.epsilonEquals(position, .001f), "Critical-slot location must not move the wings");
                        }
                        var bounds = node.calculateBoundingBox(new BoundingBox());
                        assertTrue(bounds.min.x < 0 && bounds.max.x > 0 && bounds.max.y < 0, asset + ": " + bounds);
                        assertEquals(-bounds.min.x, bounds.max.x, .01f, "Both wings must straddle the back symmetrically");
                        assertTrue(parts(node).stream().anyMatch(part -> part.material.id.equals("paint")),
                              "Wing surfaces must inherit the unit's paint/camouflage");

                        var posed = new ModelInstance(visual.instance.model);
                        Node torsoNode = posed.getNode(torso);
                        var pivot = torsoNode.globalTransform.getTranslation(new Vector3());
                        torsoNode.rotation.set(Vector3.Z, 35);
                        posed.calculateTransforms();
                        var expected = new Vector3(position).sub(pivot).rotate(Vector3.Z, 35).add(pivot);
                        var actual = posed.getNode(binding.node()).globalTransform.getTranslation(new Vector3());
                        assertTrue(expected.epsilonEquals(actual, .01f), "Wings must follow torso twist: " + asset);
                        assertTrue(node.globalTransform.getTranslation(new Vector3()).epsilonEquals(position, .001f));

                        wing.setDestroyed(true);
                        var state = UnitModelState.capture(mek);
                        assertSame(visual, library.get(selection(asset, mek), 8001));
                        visual.showEquipment(posed, state.appearance());
                        assertTrue(parts(posed.getNode(binding.node())).stream()
                              .allMatch(part -> part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)));
                        assertTrue(parts(node).stream()
                              .noneMatch(part -> part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)));
                    }
                }
                checked++;
            }
        }
        assertTrue(checked >= 28, "Review named Meks plus all biped, quad, tripod and air-Mek fallback bodies");
        System.out.println("Verified partial wings on " + checked + " Mek bodies, both tech bases and all three torso locations");
    }

    private static void renderRefits(GpuUnitModels library, ModelBatch batch) throws Exception {
        int id = 8100;
        for (String[] unit : List.of(new String[] { "locust", "Locust LCT-1V" },
              new String[] { "atlas", "Atlas AS7-D" }, new String[] { "marauder", "Marauder MAD-3R" })) {
            var mek = (Mek) new MekFileParser(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"),
                  "meks/3039u/" + unit[1] + ".mtf").getEntity();
            String asset = ROOT + "meks/" + unit[0] + ".json";
            var stock = library.get(selection(asset, mek), id++);
            int equipment = stock.equipment().size();
            // Visual refits preserve the stock weapons; they are not construction-validated unit designs.
            var wing = mek.addEquipment(EquipmentType.get("ISPartialWing"), Mek.LOC_NONE);
            wing.setLocation(Mek.LOC_LEFT_TORSO);
            var refit = library.get(selection(asset, mek), id++);
            assertNotNull(refit);
            assertEquals(equipment + 1, refit.equipment().size());
            var binding = refit.equipment().stream().filter(item -> item.index() == wing.getEquipmentNum())
                  .findFirst().orElseThrow();
            assertFalse(binding.embedded());
            for (var original : stock.equipment()) {
                var current = refit.equipment().stream().filter(item -> item.index() == original.index()).findFirst().orElseThrow();
                assertEquals(original.asset(), current.asset());
                assertEquals(original.embedded(), current.embedded());
                assertArrayEquals(stock.instance.getNode(original.node()).globalTransform.val,
                      refit.instance.getNode(current.node()).globalTransform.val, .001f, "Wings cannot displace existing equipment");
            }
            var front = new ModelInstance(refit.instance.model);
            var rear = new ModelInstance(refit.instance.model);
            front.transform.setToTranslation(48, 0, 0);
            rear.transform.setToTranslation(-48, 0, 0).rotate(Vector3.Z, 180);
            for (var instance : List.of(front, rear)) {
                for (var material : instance.materials) {
                    if (material.id.equals("paint")) {
                        material.set(ColorAttribute.createDiffuse(.74f, .84f, .58f, 1));
                    }
                }
            }
            GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(front, rear), "partial-wing-" + unit[0], 200, 28);
        }
    }

    private static BoardScene.UnitModel selection(String asset, Mek mek) {
        return new BoardScene.UnitModel(asset, null, "wing-review", 1, 0, BoardScene.LocationDamage.NONE, UnitModelState.capture(mek));
    }

    private static List<NodePart> parts(Node node) {
        List<NodePart> parts = new ArrayList<>();
        node.parts.forEach(parts::add);
        node.getChildren().forEach(child -> parts.addAll(parts(child)));
        return parts;
    }
}
