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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import org.junit.jupiter.api.Test;

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
}
