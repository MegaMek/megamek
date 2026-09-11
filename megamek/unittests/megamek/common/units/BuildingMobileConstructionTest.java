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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import megamek.common.TechConstants;
import megamek.common.bays.CargoBay;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.PowerGeneratorType;
import megamek.common.equipment.enums.StructureEngine;
import megamek.common.verifier.TestBuilding;
import megamek.common.verifier.TestXMLOption;
import megamek.common.weapons.infantry.support.srm.InfantrySupportSRMStandardWeapon;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Construction table, rounding and placement regressions from TO:AUE pp. 76-84 and 124. */
class BuildingMobileConstructionTest {
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);
    private static final CubeCoords WEST = new CubeCoords(-1, 0, 1);

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private MobileStructure mobile() {
        var mobile = new MobileStructure(BuildingType.MEDIUM, IBuilding.FORTRESS);
        mobile.setChassis("Construction review");
        mobile.setModel("Test");
        mobile.setYear(3145);
        mobile.setTechLevel(TechConstants.T_IS_ADVANCED);
        mobile.configureConstruction(BuildingType.MEDIUM, IBuilding.FORTRESS, 4, 40, 0, List.of(CubeCoords.ZERO, EAST));
        mobile.setPowerSystem(StructureEngine.COMBUSTION_LIQUID);
        return mobile;
    }

    private List<String> issues(AbstractBuildingEntity unit) {
        return new TestBuilding(unit, new TestXMLOption(), "").constructionIssues();
    }

    @Test void mobileAmplifiersRoundHalfTonsWhileStaticBuildingsRoundTenths() throws Exception {
        var mobile = mobile();
        mobile.addEquipment(new ISLaserMedium(), 0);
        assertEquals(.5, mobile.getPowerAmplifierWeight(CubeCoords.ZERO));
        mobile.setPowerSystem(StructureEngine.FUSION);
        assertEquals(0, mobile.getPowerAmplifierWeight(CubeCoords.ZERO));
        var building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.FORTRESS);
        building.configureConstruction(BuildingType.MEDIUM, IBuilding.FORTRESS, 4, 40, 0, List.of(CubeCoords.ZERO));
        building.addEquipment(new ISLaserMedium(), 0);
        assertEquals(.1, building.getPowerAmplifierWeight(CubeCoords.ZERO));
    }

    @Test void eachMobileHexPaysHalfTonRoundingForDistributedEquipment() throws Exception {
        var mobile = mobile();
        mobile.configureConstruction(BuildingType.MEDIUM, IBuilding.FORTRESS, 4, 40, 0, List.of(CubeCoords.ZERO, EAST, WEST));
        var generator = mobile.addEquipment(PowerGeneratorType.createPowerGenerator(StructureEngine.FUSION), 0);
        generator.setSize(4);
        mobile.getDesign().getEquipmentSpace().put(generator, List.of(new BuildingDesign.Position(CubeCoords.ZERO, 0),
              new BuildingDesign.Position(EAST, 0), new BuildingDesign.Position(WEST, 0)));
        assertEquals(1.5, BuildingConstruction.equipmentWeightInHex(mobile, generator, CubeCoords.ZERO));
        assertEquals(1.5, BuildingConstruction.equipmentWeightInHex(mobile, generator, EAST));
        assertEquals(1.5, BuildingConstruction.equipmentWeightInHex(mobile, generator, WEST));
    }

    @Test void smallWeaponsAndPintlesAreRoundedTogetherInEachHexAndUseRealInfantryGunners() throws Exception {
        var mobile = mobile();
        double baseline = BuildingConstruction.installedWeightInHex(mobile, CubeCoords.ZERO);
        int baseGunners = mobile.calculateMinimumCrew();
        var first = mobile.addEquipment(new InfantrySupportSRMStandardWeapon(), 0);
        first.setPintleTurretMounted(true);
        assertEquals(.5, BuildingConstruction.installedWeightInHex(mobile, CubeCoords.ZERO) - baseline, 1e-9);
        assertTrue(mobile.calculateMinimumCrew() > baseGunners);
        var second = mobile.addEquipment(new InfantrySupportSRMStandardWeapon(), 0);
        second.setPintleTurretMounted(true);
        assertEquals(.5, BuildingConstruction.installedWeightInHex(mobile, CubeCoords.ZERO) - baseline, 1e-9,
              "two small items in the same hex share the half-ton lot");
        double total = mobile.getInternalBuilding().getOriginalCoordsList().stream()
              .mapToDouble(hex -> BuildingConstruction.installedWeightInHex(mobile, hex)).sum();
        assertEquals(total, new TestBuilding(mobile, new TestXMLOption(), "").calculateWeight());
    }

    @Test void absentMobileFloorsCannotReceiveEquipmentOrDefaultBaySpace() throws Exception {
        var mobile = mobile();
        mobile.getInternalBuilding().setHeight(2, EAST);
        assertEquals(Entity.LOC_NONE, BuildingConstruction.location(mobile, new BuildingDesign.Position(EAST, 2)));
        assertEquals(5, BuildingConstruction.location(mobile, new BuildingDesign.Position(EAST, 1)));
        var cargo = new CargoBay(60, 1, 1);
        mobile.addTransporter(cargo);
        assertEquals(6, BuildingConstruction.baySpaces(mobile, cargo).size());
        assertTrue(BuildingConstruction.baySpaces(mobile, cargo).stream().allMatch(space ->
              !space.position().hex().equals(EAST) || space.position().level() < 2));
        mobile.getDesign().getDoors().add(new BuildingDesign.Door(new BuildingDesign.Position(EAST, 1), 2, 2));
        assertTrue(issues(mobile).stream().anyMatch(issue -> issue.startsWith("Large doors")));
        mobile.getDesign().getDoors().clear();
        mobile.getDesign().getElevators().add(new BuildingDesign.Elevator(EAST, 10, Map.of(1, 32, 2, 32, 3, 32)));
        assertTrue(issues(mobile).stream().anyMatch(issue -> issue.startsWith("Industrial elevators")));
    }

    @Test void concentratedFuelFollowsRotationAndDoesNotReappearWhenItsHexIsDeleted() {
        var mobile = mobile();
        mobile.setOperatingRange(100);
        mobile.setFuelLocations(Map.of(EAST, mobile.getFuelWeight()));
        var rotated = new CubeCoords(0, 1, -1);
        mobile.configureConstruction(BuildingType.MEDIUM, IBuilding.FORTRESS, 4, 40, 0,
              List.of(CubeCoords.ZERO, rotated), hex -> new CubeCoords(-hex.r(), -hex.s(), -hex.q()), side -> (side + 1) % 6);
        assertEquals(mobile.getFuelWeight(), mobile.fuelWeightInHex(rotated));
        assertEquals(0, mobile.fuelWeightInHex(CubeCoords.ZERO));
        mobile.configureConstruction(BuildingType.MEDIUM, IBuilding.FORTRESS, 4, 40, 0, List.of(CubeCoords.ZERO, EAST));
        assertFalse(mobile.getFuelLocations().isEmpty());
        assertEquals(0, mobile.fuelWeightInHex(EAST));
        assertTrue(issues(mobile).stream().anyMatch(issue -> issue.startsWith("Fuel locations")));
    }

    @Test void equipmentQuantityLimitsApplyPerHexAndBayDoorLimitsUseTheOuterPerimeter() throws Exception {
        var mobile = mobile();
        for (int n = 0; n < 4; n++) {
            mobile.addEquipment(EquipmentType.get("Lift Hoist"), 0);
            mobile.addEquipment(EquipmentType.get("Lift Hoist"), 4);
        }
        assertFalse(issues(mobile).stream().anyMatch(issue -> issue.contains("maximum of four lift hoists")));
        mobile.addEquipment(EquipmentType.get("Lift Hoist"), 1);
        assertTrue(issues(mobile).stream().anyMatch(issue -> issue.contains("maximum of four lift hoists")));
        var bay = new CargoBay(10, 0, 1);
        mobile.addTransporter(bay);
        assertTrue(issues(mobile).stream().anyMatch(issue -> issue.contains("at least one bay door")));
        mobile.removeTransporter(bay);
        mobile.addTransporter(new CargoBay(10, 6, 1));
        assertTrue(issues(mobile).stream().anyMatch(issue -> issue.contains("5 available exterior door positions")));
    }

    @Test void actualFlightDeckRuleAndTableOverrideTheConflictingWorkedExample() throws Exception {
        var mobile = mobile();
        mobile.configureConstruction(BuildingType.MEDIUM, IBuilding.FORTRESS, 4, 40, 0, List.of(CubeCoords.ZERO, EAST, WEST));
        var deck = mobile.addEquipment(EquipmentType.get("Building Flight Deck"), 3);
        mobile.getDesign().getEquipmentSpace().put(deck, List.of(new BuildingDesign.Position(CubeCoords.ZERO, 3),
              new BuildingDesign.Position(EAST, 3), new BuildingDesign.Position(WEST, 3)));
        assertEquals(1500, deck.getTonnage(), "AUE equipment table lists 1500; p84 example lists obsolete 2500");
        assertFalse(issues(mobile).stream().anyMatch(issue -> issue.startsWith("A flight deck requires")),
              "the explicit p124 construction rule requires three consecutive hexes");
    }
}
