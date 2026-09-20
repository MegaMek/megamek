/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import megamek.client.ui.tileset.EquipmentModelPolicy;
import megamek.client.ui.tileset.UnitModelEquipment;
import megamek.common.units.EntityMovementMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Exercises socket selection, node ownership and damage together without allocating GPU buffers. */
class UnitEquipmentAssemblyTest {
    @BeforeAll
    static void loadMathNatives() { GdxNativesLoader.load(); }

    @ParameterizedTest
    @CsvSource({ "LT, LA, hand", "RT, RA, hand", "LT, LA, wrist", "RT, RA, wrist", "LT, LA, elbow", "RT, RA, elbow" })
    void splitGunFollowsActualArmAnatomyAndDamageWithoutDuplicatingEquipment(String torso, String arm, String form) {
        var anatomy = new UnitModelState.MekAnatomy("biped", form.equals("hand") ? List.of(arm) : List.of(),
              form.equals("elbow") ? List.of() : List.of(arm));
        var gun = mount(7, torso, arm);
        var model = assemble(anatomy, List.of(gun, gun, mount(8, torso, "")));
        try {
            assertEquals(2, model.equipment().size(), "One physical weapon per equipment index, including split criticals");
            var binding = model.equipment().stream().filter(item -> item.index() == 7).findFirst().orElseThrow();
            var torsoBinding = model.equipment().stream().filter(item -> item.index() == 8).findFirst().orElseThrow();
            assertEquals(arm + "@" + form, model.instance.getNode(binding.node()).getParent().id);
            assertEquals(torso, model.instance.getNode(torsoBinding.node()).getParent().id);
            assertEquals(torso, binding.location(), "Firing/damage identity still describes the authoritative mount");
            assertEquals(torso, gun.location());
            assertEquals(arm, gun.secondLocation());
            assertFalse(binding.embedded());
            assertEquals(1, binding.emitters().size());
            assertTrue(binding.emitters().getFirst().node().startsWith(arm + "-equipment-7-"));

            var damaged = new ModelInstance(model.instance.model);
            model.showEquipment(damaged, new UnitModelState.Appearance(Set.of(7), false, null));
            assertTrue(damaged.getNode(binding.emitters().getFirst().node()).parts.first().material.id
                  .endsWith(UnitDamageDisplay.WRECKED_SUFFIX), "A critical hit still finds the gun by its original index");
            assertFalse(damaged.getNode(torsoBinding.emitters().getFirst().node()).parts.first().material.id
                  .endsWith(UnitDamageDisplay.WRECKED_SUFFIX));
            UnitDamageDisplay.show(damaged, new BoardScene.LocationDamage(Set.of(arm), Set.of()));
            assertFalse(damaged.getNode(binding.emitters().getFirst().node()).parts.first().enabled,
                  "A blown-off arm cannot leave its split gun floating behind");
            assertTrue(damaged.getNode(torsoBinding.emitters().getFirst().node()).parts.first().enabled);
            assertTrue(model.instance.getNode(binding.emitters().getFirst().node()).parts.first().enabled,
                  "Damage cannot mutate the reusable assembly");
        } finally {
            model.dispose();
        }
    }

    @ParameterizedTest
    @CsvSource({ "RT, '', true", "LT, LA, false", "CT, HD, true", "LT, RA, true" })
    void unsplitTorsoGunsAndOtherLocationPairsKeepTheirPrimarySocket(String primary, String secondary, boolean mek) {
        var anatomy = mek ? new UnitModelState.MekAnatomy("biped", List.of("LA", "RA"), List.of("LA", "RA")) : null;
        var model = assemble(anatomy, List.of(mount(7, primary, secondary)));
        try {
            var binding = model.equipment().getFirst();
            assertEquals(primary, model.instance.getNode(binding.node()).getParent().id);
            assertEquals(primary, binding.location());
        } finally {
            model.dispose();
        }
    }

    private static UnitModelEquipment.Mount mount(int index, String primary, String secondary) {
        return new UnitModelEquipment.Mount(index, "Gun", primary, secondary, false, false, 0,
              EquipmentModelPolicy.WEAPON, "ballistic", List.of());
    }

    private static GpuUnitModel assemble(UnitModelState.MekAnatomy anatomy, List<UnitModelEquipment.Mount> equipment) {
        var library = mock(GpuUnitModels.class);
        when(library.descriptor("equipment.json")).thenReturn(new JsonReader().parse("""
              {"schema":2,"equipment":{"Gun":{"model":"gun.json"}},"fallbacks":{"weapon":"gun.json"}}
              """));
        var bounds = new UnitModelDescriptor.Bounds(List.of(-1f, 0f, -1f), List.of(1f, 2f, 1f));
        Model gun = new Model();
        Node barrel = node("barrel");
        MeshPart mesh = new MeshPart();
        mesh.mesh = mock(Mesh.class);
        barrel.parts.add(new NodePart(mesh, new Material("paint")));
        gun.nodes.add(barrel);
        var emitter = new UnitModelDescriptor.Emitter("muzzle", "barrel", List.of(0f, 2f, 0f), List.of(0f, 1f, 0f),
              "muzzle", "bullet");
        var weapon = new UnitModelDescriptor(2, "equipment", "ballistic", "gun.g3dj", bounds, "rigid-v1",
              Map.of("root", "barrel"), Map.of(), List.of(), List.of(emitter), List.of(), Map.of());
        when(library.modular("gun.json")).thenReturn(new GpuUnitModels.ModularAsset(weapon, gun, 1));

        Model assembled = new Model();
        List<UnitModelDescriptor.Hardpoint> points = new ArrayList<>();
        var recipe = new JsonReader().parse("{\"equipment\":\"equipment.json\",\"mounts\":[]}");
        for (String location : List.of("CT", "LT", "RT", "LA", "RA")) {
            Node part = node(location);
            assembled.nodes.add(part);
            for (String form : location.endsWith("A") ? List.of("hand", "wrist", "elbow") : List.of("")) {
                String id = location + (form.isEmpty() ? "" : "@" + form);
                if (!form.isEmpty()) { part.addChild(node(id)); }
                points.add(new UnitModelDescriptor.Hardpoint(id, location, "front", id,
                      List.of(0f, 0f, 0f), List.of(0f, 0f, 0f, 1f), List.of(20f, 20f, 20f), .5f, 1f, List.of("weapon")));
                JsonValue preference = new JsonReader().parse("{\"hardpoint\":\"" + id + "\",\"form\":\"" + form + "\"}");
                recipe.get("mounts").addChild(preference);
            }
        }
        assembled.calculateTransforms();
        var body = new UnitModelDescriptor(2, "body", "mek", "body.g3dj", bounds, "biped-v1",
              Map.of("root", "CT"), Map.of(), points, List.of(), List.of(), Map.of());
        var structure = new UnitModelState.Structure(EntityMovementMode.BIPED, equipment, List.of(), 0, false, anatomy);
        var bindings = UnitEquipmentAssembly.attachAll(library, recipe, new GpuUnitModels.ModularAsset(body, assembled, 1),
              structure, assembled);
        assembled.calculateTransforms();
        return new GpuUnitModel(assembled, "CT", true, bindings);
    }

    private static Node node(String id) {
        Node node = new Node();
        node.id = id;
        return node;
    }
}
