/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI.boardview.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Works on the part tree alone, which needs no graphics context. */
class UnitDamageDisplayTest {
    private final Material paint = new Material("paint");

    private Node part(String id, Node... children) {
        Node node = new Node();
        node.id = id;
        node.parts.add(new NodePart(new MeshPart(), paint));
        for (Node child : children) {
            node.addChild(child);
        }
        return node;
    }

    /** The shape every Mek body has: legs under the hips, everything else under the center torso. */
    private ModelInstance biped() {
        Model model = new Model();
        model.materials.add(paint);
        model.nodes.add(part("pelvis",
              part("CT", part("LT"), part("RT"), part("HD"), part("LA"), part("RA")),
              part("LL", part("LL-shin")),
              part("RL", part("RL-shin"))));
        return new ModelInstance(model);
    }

    private static boolean isWrecked(ModelInstance instance, String id) {
        return instance.getNode(id).parts.first().material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX);
    }

    @Test
    void aLostArmIsNotDrawnAndNothingElseChanges() {
        ModelInstance instance = biped();
        List<String> missing = UnitDamageDisplay.show(instance,
              new BoardScene.LocationDamage(Set.of("RA"), Set.of()));
        assertTrue(missing.isEmpty());
        assertFalse(instance.getNode("RA").parts.first().enabled);
        assertTrue(instance.getNode("LA").parts.first().enabled);
        assertFalse(isWrecked(instance, "RT"));
    }

    @Test
    void aLostLegIsBurntOutWithItsShinButStillDrawn() {
        ModelInstance instance = biped();
        UnitDamageDisplay.show(instance, new BoardScene.LocationDamage(Set.of(), Set.of("LL")));
        assertTrue(isWrecked(instance, "LL"));
        assertTrue(isWrecked(instance, "LL-shin"));
        assertTrue(instance.getNode("LL").parts.first().enabled);
        assertFalse(isWrecked(instance, "RL"));
        ColorAttribute diffuse = (ColorAttribute) instance.getNode("LL").parts.first().material
              .get(ColorAttribute.Diffuse);
        assertEquals(UnitDamageDisplay.WRECKED, diffuse.color);
    }

    @Test
    void aLostCenterTorsoDoesNotBurnOutTheLocationsHangingFromIt() {
        ModelInstance instance = biped();
        UnitDamageDisplay.show(instance, new BoardScene.LocationDamage(Set.of(), Set.of("CT")));
        assertTrue(isWrecked(instance, "CT"));
        assertFalse(isWrecked(instance, "LA"));
        assertFalse(isWrecked(instance, "HD"));
    }

    @Test
    void theSharedModelAndOtherUnitsAreLeftAlone() {
        ModelInstance damaged = biped();
        Material before = damaged.model.nodes.first().getChild("LL", true, false).parts.first().material;
        UnitDamageDisplay.show(damaged, new BoardScene.LocationDamage(Set.of(), Set.of("LL")));
        assertEquals("paint", before.id);
        assertFalse(before.has(ColorAttribute.Diffuse));
    }

    @Test
    void aLocationTheModelHasNoPartForIsReported() {
        ModelInstance instance = biped();
        List<String> missing = UnitDamageDisplay.show(instance,
              new BoardScene.LocationDamage(Set.of("CL"), Set.of("FLL")));
        assertEquals(Set.of("CL", "FLL"), Set.copyOf(missing));
    }

    @ParameterizedTest
    @CsvSource({
          "false, 0,", "false, 0.24,", "false, 0.25, BODY_25", "false, 0.49, BODY_25",
          "false, 0.50, BODY_50", "false, 0.74, BODY_50", "false, 0.75, BODY_75",
          "false, 0.99, BODY_75", "false, 1, BODY_100",
          "true, 0,", "true, 0.24,", "true, 0.25, ARMOR_WORN", "true, 0.49, ARMOR_WORN",
          "true, 0.50, ARMOR_STRIPPED", "true, 0.74, ARMOR_STRIPPED",
          "true, 0.75, STRUCTURE_BATTERED", "true, 0.99, STRUCTURE_BATTERED"
    })
    void previewUsesExistingThresholdsForTheWholeUnit(boolean mek, float loss, UnitDamageDisplay.Stage stage) {
        var damage = UnitDamageDisplay.preview(BoardScene.LocationDamage.NONE, mek, loss);
        assertEquals(stage == null ? Map.of() : Map.of("*", stage), damage.stages());
        assertTrue(damage.removed().isEmpty());
        assertTrue(damage.wrecked().isEmpty());
    }

    @Test
    void disablingPreviewRestoresActualDamageWithoutChangingTheSnapshot() {
        var actual = new BoardScene.LocationDamage(Set.of("LA"), Set.of("RL"),
              Map.of("LT", UnitDamageDisplay.Stage.ARMOR_WORN));
        var preview = UnitDamageDisplay.preview(actual, true, .75f);
        assertEquals(actual.removed(), preview.removed());
        assertEquals(actual.wrecked(), preview.wrecked());
        assertEquals(Map.of("*", UnitDamageDisplay.Stage.STRUCTURE_BATTERED), preview.stages());
        assertTrue(UnitDamageDisplay.preview(actual, true, 0).stages().isEmpty());
        assertSame(actual, UnitDamageDisplay.preview(actual, true, -1));
        assertEquals(Map.of("LT", UnitDamageDisplay.Stage.ARMOR_WORN), actual.stages());
    }

    @Test
    void fullMekPreviewBurnsEveryPartWithoutDetachingIntactLocations() {
        ModelInstance instance = biped();
        var damage = UnitDamageDisplay.preview(new BoardScene.LocationDamage(Set.of("LA"), Set.of()), true, 1);
        assertTrue(damage.stages().isEmpty(), "Destroyed damage has no armor or structure overlay");
        assertTrue(UnitDamageDisplay.show(instance, damage).isEmpty());
        assertTrue(UnitDamageDisplay.locationParts(instance, "*").stream()
              .allMatch(part -> part.material.id.endsWith(UnitDamageDisplay.WRECKED_SUFFIX)));
        assertFalse(instance.getNode("LA").parts.first().enabled);
        assertTrue(instance.getNode("RA").parts.first().enabled);
        assertTrue(instance.getNode("LL-shin").parts.first().enabled);
    }

    @Test
    void selectedLocationPreservesActualDamageElsewhereAndCanBeRepaired() {
        var actual = new BoardScene.LocationDamage(Set.of("LA"), Set.of("RL"),
              Map.of("LT", UnitDamageDisplay.Stage.ARMOR_STRIPPED, "RA", UnitDamageDisplay.Stage.ARMOR_WORN));
        var preview = UnitDamageDisplay.preview(actual, true, .75f, "RA");
        assertEquals(Map.of("LT", UnitDamageDisplay.Stage.ARMOR_STRIPPED, "RA", UnitDamageDisplay.Stage.STRUCTURE_BATTERED),
              preview.stages());
        assertEquals(actual.removed(), preview.removed());
        assertEquals(actual.wrecked(), preview.wrecked());
        var repaired = UnitDamageDisplay.preview(actual, true, 0, "RA");
        assertEquals(Map.of("LT", UnitDamageDisplay.Stage.ARMOR_STRIPPED), repaired.stages());
        assertSame(actual, UnitDamageDisplay.preview(actual, true, -1, "RA"));

        var destroyed = UnitDamageDisplay.preview(actual, true, 1, "RA");
        assertEquals(Set.of("RL", "RA"), destroyed.wrecked());
        assertEquals(repaired.stages(), destroyed.stages());
        ModelInstance instance = biped();
        UnitDamageDisplay.show(instance, destroyed);
        assertTrue(isWrecked(instance, "RA"));
        assertFalse(isWrecked(instance, "CT"));
        assertFalse(instance.getNode("LA").parts.first().enabled);
        assertEquals(UnitDamageDisplay.Stage.ARMOR_WORN, actual.stages().get("RA"));
    }

    @Test
    void structureWinsOverArmorAndDestroyedRemovesTheOverlay() {
        assertEquals(UnitDamageDisplay.Stage.STRUCTURE_BATTERED, UnitDamageDisplay.locationStage(1, .5f));
        Material material = new Material("paint", new UnitDamageDisplay.Overlay(mock(Texture.class)));
        Material wrecked = UnitDamageDisplay.wrecked(material);
        assertFalse(wrecked.has(UnitDamageDisplay.Overlay.TYPE));
        assertTrue(material.has(UnitDamageDisplay.Overlay.TYPE), "Shared artwork remains untouched");
    }
}
