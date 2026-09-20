/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;
import megamek.common.Configuration;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.ProneCause;

/** Native boundaries for optional equipment, independent damage and adding art without a new baked unit. */
final class GpuEquipmentAssemblyReview {
    private static final String ROOT = "units/modular/";

    private GpuEquipmentAssemblyReview() { }

    static void verify(GpuUnitModels library, ModelBatch batch) throws Exception {
        verifySearchlights(library);
        verifyLocustSearchlights(library, batch);
        verifyAnatomyAndPhysical(library, batch);
        verifyGlobalMapping();
    }

    private static void verifySearchlights(GpuUnitModels library) throws Exception {
        var mek = new BipedMek();
        mek.setExternalSearchlight(true);
        var unlit = library.get(selection("meks/warhammer.json", UnitModelState.capture(mek)), 1100);
        assertTrue(unlit.equipment().isEmpty(), "A bare chassis and automatic rules flag must not create lamp geometry");
        mek.getQuirks().getOption(OptionsConstants.QUIRK_POS_SEARCHLIGHT).setValue(true);
        var mounted = mek.addEquipment(EquipmentType.get("Searchlight"), Mek.LOC_LEFT_TORSO);
        mek.setSearchlightState(true);
        var state = UnitModelState.capture(mek);
        var visual = library.get(selection("meks/warhammer.json", state), 1100);
        assertEquals(2, visual.equipment().size(), "Mounted and external searchlights are separate real sources");
        var external = visual.equipment().stream().filter(binding -> binding.index() == -1).findFirst().orElseThrow();
        var lamp = visual.equipment().stream().filter(binding -> binding.index() == mounted.getEquipmentNum())
              .findFirst().orElseThrow();
        var lit = new ModelInstance(visual.instance.model);
        visual.showEquipment(lit, state.appearance());
        assertTrue(emission(lit.getNode(lamp.node())) > 0);
        assertTrue(emission(lit.getNode(external.node())) > 0);
        mek.setSearchlightState(false);
        var off = new ModelInstance(visual.instance.model);
        visual.showEquipment(off, UnitModelState.capture(mek).appearance());
        assertEquals(0, emission(off.getNode(lamp.node())));
        assertTrue(emission(lit.getNode(lamp.node())) > 0, "Changing another instance cannot darken this one");
        mek.setSearchlightState(true);
        mounted.setDestroyed(true);
        var damaged = UnitModelState.capture(mek);
        assertSame(visual, library.get(selection("meks/warhammer.json", damaged), 1100));
        var burnt = new ModelInstance(visual.instance.model);
        visual.showEquipment(burnt, damaged.appearance());
        assertEquals(0, emission(burnt.getNode(lamp.node())));
        assertTrue(emission(burnt.getNode(external.node())) > 0);
        assertTrue(parts(burnt.getNode(lamp.node())).stream()
              .allMatch(part -> part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)));
        mounted.setDestroyed(false);
        var repaired = new ModelInstance(visual.instance.model);
        visual.showEquipment(repaired, UnitModelState.capture(mek).appearance());
        assertTrue(emission(repaired.getNode(lamp.node())) > 0);
        mek.setExternalSearchlight(false);
        var removed = library.get(selection("meks/warhammer.json", UnitModelState.capture(mek)), 1100);
        assertEquals(1, removed.equipment().size());
    }

    private static void verifyLocustSearchlights(GpuUnitModels library, ModelBatch batch) throws Exception {
        var locust = new MekFileParser(new File(Configuration.dataDir(), "mekfiles/unit_files.zip"),
              "meks/3039u/Locust LCT-1V.mtf").getEntity();
        assertFalse(locust.getQuirks().booleanOption(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
        locust.setExternalSearchlight(true);
        locust.setSearchlightState(true);
        var stock = library.get(selection("meks/locust.json", UnitModelState.capture(locust)), 1101);
        assertNotNull(stock);
        assertTrue(stock.equipment().stream().flatMap(binding -> binding.emitters().stream())
              .noneMatch(emitter -> emitter.role().equals("lamp")), "A deployed stock Locust must not gain a searchlight module");
        var stockInstance = new ModelInstance(stock.instance.model);
        var mounted = locust.addEquipment(EquipmentType.get("Searchlight"), Mek.LOC_LEFT_TORSO);
        var refit = library.get(selection("meks/locust.json", UnitModelState.capture(locust)), 1102);
        assertEquals(stock.equipment().size() + 1, refit.equipment().size());
        var lamps = refit.equipment().stream().filter(binding -> binding.emitters().stream()
              .anyMatch(emitter -> emitter.role().equals("lamp"))).toList();
        assertEquals(1, lamps.size(), "A mounted searchlight adds exactly its own housing");
        assertEquals(mounted.getEquipmentNum(), lamps.getFirst().index());
        assertFalse(lamps.getFirst().embedded());
        stockInstance.transform.setToTranslation(45, 0, 0);
        var refitInstance = new ModelInstance(refit.instance.model);
        refitInstance.transform.setToTranslation(-45, 0, 0);
        GpuModularUnitModelsSmokeTest.renderReview(batch, List.of(stockInstance, refitInstance),
              "runtime-locust-searchlight-presence", 200, 22);
    }

    private static void verifyAnatomyAndPhysical(GpuUnitModels library, ModelBatch batch) throws Exception {
        var mek = new BipedMek();
        mek.addEquipment(EquipmentType.get("Tree Club"), Mek.LOC_LEFT_ARM);
        mek.addEquipment(EquipmentType.get("ISGuardianECMSuite"), Mek.LOC_RIGHT_TORSO);
        var source = UnitModelState.capture(mek);
        List<ModelInstance> gallery = new ArrayList<>();
        for (String form : List.of("hand", "wrist", "elbow")) {
            var anatomy = new UnitModelState.MekAnatomy("biped", form.equals("hand") ? List.of("LA", "RA") : List.of(),
                  form.equals("elbow") ? List.of() : List.of("LA", "RA"));
            var structure = new UnitModelState.Structure(EntityMovementMode.BIPED, source.structure().equipment(),
                  List.of(), 0, false, anatomy);
            var state = new UnitModelState(structure, source.appearance(), source.pose());
            var visual = library.get(selection("meks/atlas.json", state), 1110 + gallery.size());
            assertNotNull(visual);
            assertEquals(2, visual.equipment().size());
            assertTrue(visual.equipment().stream().noneMatch(UnitEquipmentAssembly.Binding::embedded));
            var club = visual.equipment().stream().filter(binding -> binding.location().equals("LA")).findFirst().orElseThrow();
            assertEquals("contact", club.emitters().getFirst().role());
            assertTrue(parts(visual.instance.getNode(club.node())).stream().anyMatch(part ->
                  part.material.id.equals("bark") && part.material.has(TextureAttribute.Diffuse)));
            assertEquals(form.equals("hand"), visual.instance.getNode("LA@hand") != null);
            assertEquals(!form.equals("elbow"), visual.instance.getNode("LA@forearm") != null);
            if (form.equals("elbow")) {
                var origin = visual.instance.getNode(club.node()).globalTransform.getTranslation(new Vector3());
                var elbow = visual.instance.getNode("LA-forearm").globalTransform.getTranslation(new Vector3());
                assertTrue(origin.epsilonEquals(elbow, .01f), "Equipment must connect to the remaining elbow");
            }
            var drawn = new ModelInstance(visual.instance.model);
            drawn.transform.setToTranslation((1 - gallery.size()) * 90, 0, 0);
            gallery.add(drawn);
        }
        GpuModularUnitModelsSmokeTest.renderReview(batch, gallery, "runtime-physical-arm-forms", 300, 18);
    }

    /** Use a private review directory and reopen the view cache after editing the one global mapping. */
    private static void verifyGlobalMapping() throws Exception {
        Path source = Configuration.dataDir().toPath().resolve("models");
        Path root = Path.of(System.getProperty("megamek.gpu.screenshots"), "equipment-extension");
        var json = new ObjectMapper();
        for (String key : List.of("bodies/atlas", "bodies/fallback-biped", "equipment/ppc", "equipment/srm-6")) {
            Path descriptor = Path.of(ROOT + key + ".json");
            Files.createDirectories(root.resolve(descriptor).getParent());
            Files.copy(source.resolve(descriptor), root.resolve(descriptor), StandardCopyOption.REPLACE_EXISTING);
            Path mesh = descriptor.resolveSibling(json.readTree(source.resolve(descriptor).toFile()).get("mesh").asText());
            Files.copy(source.resolve(mesh), root.resolve(mesh), StandardCopyOption.REPLACE_EXISTING);
        }
        for (String key : List.of("atlas", "fallback-biped")) {
            var body = (ObjectNode) json.readTree(source.resolve(ROOT + "meks/" + key + ".json").toFile());
            body.put("equipment", "equipment.json");
            if (key.equals("atlas")) {
                body.put("compatibility", "stock");
            }
            Files.createDirectories(root.resolve(ROOT + "meks"));
            json.writeValue(root.resolve(ROOT + "meks/" + key + ".json").toFile(), body);
        }
        var mount = new UnitModelEquipment.Mount(4, "future-weapon", "RA", "RT", true, false, 0,
              EquipmentModelPolicy.WEAPON, "ppc", List.of());
        var none = new UnitModelEquipment.Mount(5, "future-weapon", "LA", "", false, false, 0,
              EquipmentModelPolicy.NONE, "ppc", List.of());
        var structure = new UnitModelState.Structure(EntityMovementMode.BIPED, List.of(mount, mount, none),
              List.of(), 0, false, new UnitModelState.MekAnatomy("biped", List.of("LA", "RA"), List.of("LA", "RA")));
        var state = new UnitModelState(structure, new UnitModelState.Appearance(Set.of(), false, null),
              new UnitModelState.Pose(ProneCause.NONE, 0, 0));
        for (String replacement : List.of("", "srm-6", "ppc")) {
            ObjectNode catalog = json.createObjectNode().put("schema", 2);
            catalog.putObject("fallbacks").put("weapon", ROOT + "equipment/ppc.json");
            var mappings = catalog.putObject("equipment");
            if (!replacement.isEmpty()) {
                mappings.putObject("future-weapon").put("model", ROOT + "equipment/" + replacement + ".json");
            }
            json.writeValue(root.resolve("equipment.json").toFile(), catalog);
            var library = new GpuUnitModels(root);
            try {
                for (String key : List.of("atlas", "fallback-biped")) {
                    var visual = library.get(selection("meks/" + key + ".json", state), 1120);
                    assertNotNull(visual);
                    assertEquals(1, visual.equipment().size(), "Split entries and excluded equipment must not add modules");
                    var binding = visual.equipment().getFirst();
                    assertEquals(ROOT + "equipment/" + (replacement.isEmpty() ? "ppc" : replacement) + ".json", binding.asset());
                    assertFalse(binding.embedded());
                    var direction = new Vector3();
                    UnitModelAttachment.emitter(visual.instance, binding.emitters().getFirst(), new Vector3(), direction);
                    assertTrue(direction.y < -.99f);
                }
                var refit = new BoardScene.UnitModel(ROOT + "meks/atlas.json", ROOT + "meks/fallback-biped.json",
                      "altered-loadout", 1, 0, BoardScene.LocationDamage.NONE, state);
                var resolved = library.get(refit, 1121);
                var fallback = library.get(selection("meks/fallback-biped.json", state), 1122);
                var expected = fallback.instance.calculateBoundingBox(new BoundingBox());
                var actual = resolved.instance.calculateBoundingBox(new BoundingBox());
                assertTrue(expected.min.epsilonEquals(actual.min, .001f) && expected.max.epsilonEquals(actual.max, .001f),
                      "Incompatible dedicated variants use the dynamic fallback");
            } finally {
                library.dispose();
            }
        }
    }

    private static BoardScene.UnitModel selection(String asset, UnitModelState state) {
        return new BoardScene.UnitModel(ROOT + asset, null, "stock", 1, 0, BoardScene.LocationDamage.NONE, state);
    }

    private static List<NodePart> parts(Node node) {
        List<NodePart> parts = new ArrayList<>();
        node.parts.forEach(parts::add);
        node.getChildren().forEach(child -> parts.addAll(parts(child)));
        return parts;
    }

    private static float emission(Node node) {
        float total = 0;
        for (var part : parts(node)) {
            var value = part.material.get(ColorAttribute.class, ColorAttribute.Emissive);
            if (value != null) {
                total += value.color.r + value.color.g + value.color.b;
            }
        }
        return total;
    }
}
