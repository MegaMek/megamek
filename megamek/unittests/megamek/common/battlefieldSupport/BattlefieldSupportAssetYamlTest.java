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
package megamek.common.battlefieldSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitRole;
import org.junit.jupiter.api.Test;

class BattlefieldSupportAssetYamlTest {

    private static BattlefieldSupportAssetData maxim() {
        BattlefieldSupportAssetData asset = new BattlefieldSupportAssetData();
        asset.setChassis("Maxim Heavy Hover Transport");
        asset.setModel("");
        asset.setAssetType(BFSAssetType.VEHICLE);
        asset.setCardTitle("Maxim");
        asset.setCardSubtitle("Hover Transport");
        asset.setMp(8);
        asset.setMovementMode(EntityMovementMode.HOVER);
        asset.setTmm(3);
        asset.setRange(new BFSRange(3, 6, 9));
        asset.setSkill(6);
        asset.setVeteranSkill(5);
        asset.setDamage(new BFSDamage(5, 4));
        asset.setDestroyCheck(7);
        asset.setThreshold(5);
        asset.setCost(23);
        asset.setVeteranCost(27);
        asset.setSpecials(List.of(BFSSpecial.of("APC", 1), BFSSpecial.of("IF", 2)));
        asset.setRole(UnitRole.SCOUT);
        asset.setYear(3151);
        asset.setTechBase(BattlefieldSupportAssetData.TECH_BASE_IS);
        asset.setSource("BattleTech: Mercenaries");
        return asset;
    }

    private static void assertRoundTrips(BattlefieldSupportAssetData original) throws Exception {
        BattlefieldSupportAssetData restored = BattlefieldSupportAssetYaml.fromYaml(
              BattlefieldSupportAssetYaml.toYaml(original));

        assertEquals(original.getChassis(), restored.getChassis());
        assertEquals(original.getModel(), restored.getModel());
        assertEquals(original.getAssetType(), restored.getAssetType());
        assertEquals(original.getCardTitle(), restored.getCardTitle());
        assertEquals(original.getCardSubtitle(), restored.getCardSubtitle());
        assertEquals(original.getMp(), restored.getMp());
        assertEquals(original.getMovementMode(), restored.getMovementMode());
        assertEquals(original.getTmm(), restored.getTmm());
        assertEquals(original.getRange(), restored.getRange());
        assertEquals(original.getSkill(), restored.getSkill());
        assertEquals(original.getVeteranSkill(), restored.getVeteranSkill());
        assertEquals(original.getDamage(), restored.getDamage());
        assertEquals(original.getDestroyCheck(), restored.getDestroyCheck());
        assertEquals(original.getThreshold(), restored.getThreshold());
        assertEquals(original.getCost(), restored.getCost());
        assertEquals(original.getVeteranCost(), restored.getVeteranCost());
        assertEquals(original.getSpecials(), restored.getSpecials());
        assertEquals(original.getRole(), restored.getRole());
        assertEquals(original.getFluffImageEncoded(), restored.getFluffImageEncoded());
        assertEquals(original.getYear(), restored.getYear());
        assertEquals(original.getTechBase(), restored.getTechBase());
        assertEquals(original.getSource(), restored.getSource());
        assertEquals(original.getIconEncoded(), restored.getIconEncoded());
    }

    @Test
    void linkedVehicleRoundTrips() throws Exception {
        assertRoundTrips(maxim());
    }

    @Test
    void standaloneArtilleryAssetRoundTrips() throws Exception {
        BattlefieldSupportAssetData asset = new BattlefieldSupportAssetData();
        asset.setChassis("Mobile Long Tom");
        asset.setModel("LT-MOB-25");
        asset.setAssetType(BFSAssetType.VEHICLE);
        asset.setMp(2);
        asset.setMovementMode(EntityMovementMode.TRACKED);
        asset.setTmm(0);
        asset.setRange(BFSRange.KEYWORD);
        asset.setSkill(7);
        asset.setDamage(BFSDamage.NONE);
        asset.setDestroyCheck(7);
        asset.setThreshold(5);
        asset.setCost(66);
        asset.setSpecials(List.of(BFSSpecial.of("Artillery", "LT"), BFSSpecial.of("No Turret")));
        assertRoundTrips(asset);
    }

    @Test
    void unknownSpecialsArePreserved() throws Exception {
        BattlefieldSupportAssetData asset = maxim();
        asset.setSpecials(List.of(BFSSpecial.of("ZZZ", 9), BFSSpecial.of("Made Up")));
        BattlefieldSupportAssetData restored = BattlefieldSupportAssetYaml.fromYaml(
              BattlefieldSupportAssetYaml.toYaml(asset));
        assertEquals(asset.getSpecials(), restored.getSpecials());
        assertTrue(restored.getSpecials().stream().noneMatch(BFSSpecial::isKnown));
    }

    @Test
    void optionalFieldsOmittedWhenUnset() throws Exception {
        BattlefieldSupportAssetData asset = new BattlefieldSupportAssetData();
        asset.setChassis("Bare Asset");
        asset.setCost(5);
        String yaml = BattlefieldSupportAssetYaml.toYaml(asset);
        assertTrue(yaml.contains("chassis:"));
        assertTrue(!yaml.contains("cardTitle"));
        assertTrue(!yaml.contains("role:"));
        assertTrue(!yaml.contains("fluffImage"));
        assertTrue(!yaml.contains("veteran"));

        BattlefieldSupportAssetData restored = BattlefieldSupportAssetYaml.fromYaml(yaml);
        assertNull(restored.getVeteranCost());
        assertNull(restored.getCardTitle());
        assertEquals(UnitRole.UNDETERMINED, restored.getRole());
    }

    @Test
    void uuidAndLinkedUnitIdRoundTrip() throws Exception {
        BattlefieldSupportAssetData asset = maxim();
        asset.setUuid("019f5efd-5a93-78c3-b4f2-dd83804af175");
        asset.setLinkedUnitId("019f583e-e2c6-7b99-a188-ba0759db128e");
        String yaml = BattlefieldSupportAssetYaml.toYaml(asset);
        // The uuid is written at the top of the document, before chassis.
        assertTrue((yaml.indexOf("uuid:") >= 0) && (yaml.indexOf("uuid:") < yaml.indexOf("chassis:")));
        assertTrue(yaml.contains("linkedUnitId:"));

        BattlefieldSupportAssetData restored = BattlefieldSupportAssetYaml.fromYaml(yaml);
        assertEquals("019f5efd-5a93-78c3-b4f2-dd83804af175", restored.getUuid());
        assertEquals("019f583e-e2c6-7b99-a188-ba0759db128e", restored.getLinkedUnitId());
    }

    @Test
    void linkedUnitIdOmittedForStandaloneAsset() throws Exception {
        String yaml = BattlefieldSupportAssetYaml.toYaml(maxim());
        assertTrue(!yaml.contains("linkedUnitId"));
        assertNull(BattlefieldSupportAssetYaml.fromYaml(yaml).getLinkedUnitId());
    }
}
