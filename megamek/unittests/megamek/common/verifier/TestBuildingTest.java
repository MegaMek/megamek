/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import megamek.common.TechConstants;
import megamek.common.bays.FirstClassQuartersCargoBay;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.BuildingEntity;
import megamek.common.units.IBuilding;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestBuildingTest {
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private BuildingEntity building(BuildingType type, int classification, int height, int cf, int armor,
          List<CubeCoords> hexes) {
        var building = new BuildingEntity(type, classification);
        building.setEngine(new Engine(0, Engine.NONE, 0));
        building.setTechLevel(TechConstants.T_IS_ADVANCED);
        building.setYear(3145);
        building.configureConstruction(type, classification, height, cf, armor, hexes);
        return building;
    }

    private TestBuilding verifier(BuildingEntity building) {
        return new TestBuilding(building, new TestXMLOption(), "");
    }

    @Test
    void defaultExternalPowerAndHangarLimitsDifferFromStandardBuildings() {
        var hangar = building(BuildingType.MEDIUM, IBuilding.HANGAR, 1, 16, 0, List.of(CubeCoords.ZERO));
        assertTrue(hangar.hasPower());
        assertTrue(verifier(hangar).constructionIssues().isEmpty());
        assertEquals(48, hangar.getWeight());
        hangar.configureConstruction(BuildingType.MEDIUM, IBuilding.HANGAR, 1, 40, 0, List.of(CubeCoords.ZERO));
        assertTrue(verifier(hangar).constructionIssues().stream().anyMatch(s -> s.contains("between 9 and 16")));
        hangar.configureConstruction(BuildingType.HARDENED, IBuilding.STANDARD, 1, 100, 0, List.of(CubeCoords.ZERO));
        assertFalse(verifier(hangar).constructionIssues().isEmpty());
    }

    @Test
    void armorIsPurchasedOncePerHexAndCostDoesNotChangeWhenDamaged() {
        var building = building(BuildingType.HEAVY, IBuilding.FORTRESS, 3, 80, 32, List.of(CubeCoords.ZERO, EAST));
        assertEquals(4, building.getArmorWeight());
        double cost = (6 * 80 * 20000 + 4 * 10000) * 1.8;
        assertEquals(cost, building.getCost(false));
        building.setInternal(1, 0);
        building.setArmor(0, 0);
        assertEquals(cost, building.getCost(false));
        assertEquals(4, building.getArmorWeight());
    }

    @Test
    void derivedAmplifiersTurretsAndQuartersCountAgainstCapacity() throws Exception {
        var building = building(BuildingType.HEAVY, IBuilding.FORTRESS, 2, 80, 0, List.of(CubeCoords.ZERO, EAST));
        var laser = building.addEquipment(EquipmentType.get("ISMediumLaser"), 1);
        laser.setSponsonTurretMounted(true);
        assertEquals(.1, building.getPowerAmplifierWeight(CubeCoords.ZERO), .00001);
        assertEquals(.5, building.getTurretWeight(CubeCoords.ZERO), .00001);
        assertEquals(1.6, verifier(building).calculateWeight(), .00001);
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(s -> s.contains("heat sinks")));
        var generator = building.addEquipment(EquipmentType.get("FUSION PowerGenerator"), 0);
        generator.setSize(4.1);
        assertTrue(building.hasPower());
        assertEquals(0, building.getPowerAmplifierWeight(CubeCoords.ZERO));
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(issue -> issue.contains("whole tons")));
        generator.setSize(5);
        assertTrue(verifier(building).constructionIssues().isEmpty());
        building.addTransporter(new FirstClassQuartersCargoBay(1));
        assertEquals(16.5, verifier(building).calculateWeight(), .00001);
        assertTrue(BayData.CARGO.isLegalFor(building));
        generator.setDestroyed(true);
        assertFalse(building.hasPower(), "Destroyed generators must not fall back to implicit external power");
    }

    @Test
    void checksCapacityPerHexAndBuildingArmorRestrictions() throws Exception {
        var building = building(BuildingType.LIGHT, IBuilding.STANDARD, 1, 15, 0, List.of(CubeCoords.ZERO, EAST));
        for (int i = 0; i < 16; i++) {
            building.addEquipment(EquipmentType.get("Heat Sink"), 0);
        }
        assertTrue(verifier(building).calculateWeight() < building.getWeight());
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(s -> s.contains("this hex's carrying capacity")));
        building.initializeArmor(16, 0);
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(s -> s.contains("armor must be between 0 and 0")));
    }
}
