/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import megamek.common.bays.FirstClassQuartersCargoBay;
import megamek.common.bays.LiquidCargoBay;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.BuildingEquipmentType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.StructureEngine;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.BLKStructureFile;
import megamek.common.loaders.BuildingDesignCodec;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.units.BuildingConstruction;
import megamek.common.units.BuildingDesign;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConstructionUtil;
import megamek.common.units.IBuilding;
import megamek.common.util.BuildingBlock;
import megamek.common.verifier.TestBuilding;
import megamek.common.verifier.TestXMLOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BuildingConstructionTest {
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private BuildingEntity building(int height) {
        var building = new BuildingEntity(BuildingType.HEAVY, IBuilding.FORTRESS);
        building.setEngine(new Engine(0, Engine.NONE, 0));
        building.setChassis("Construction test");
        building.setModel("");
        building.setTechLevel(TechConstants.T_IS_ADVANCED);
        building.setYear(3145);
        building.configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, height, 80, 0, List.of(CubeCoords.ZERO, EAST));
        return building;
    }

    private TestBuilding verifier(BuildingEntity building) {
        return new TestBuilding(building, new TestXMLOption(), "");
    }

    @Test
    void castleBrianCommandTowerUsesCapitalProtectionAndAutomaticSealing() throws Exception {
        var tower = building(4);
        tower.configureConstruction(BuildingType.HEAVY, IBuilding.CASTLE_BRIAN, 4, 50, 50, List.of(CubeCoords.ZERO));
        assertEquals(2000, tower.getWeight(), "TO:AR p. 140 command tower: 50 capital CF × 10 × 4 levels");
        assertEquals(32, tower.getArmorWeight(), "The example explicitly purchases 32 tons of IS armor");
        assertTrue(tower.hasEnvironmentalSealing());
        assertTrue(verifier(tower).constructionIssues().isEmpty(), verifier(tower).constructionIssues().toString());
        assertEquals((1000000.0 * 50 * 4 * 1.5 + 32 * 10000) * 6, tower.getCost(false));
        var loaded = (BuildingEntity) new BLKStructureFile(BLKFile.getBlock(tower)).getEntity();
        assertEquals(IBuilding.CASTLE_BRIAN, loaded.getBldgClass());
        assertEquals(50, loaded.getOInternal(0));
        assertEquals(50, loaded.getOArmor(0));
        assertEquals(tower.getCost(false), loaded.getCost(false));
        assertFalse(loaded.getDesign().hasEnvironmentalSealing(), "Automatic sealing is derived from the class");
    }

    @Test
    void openSpaceHasOne600TonBudgetAndGroundEquipmentOnly() throws Exception {
        var cave = building(2);
        cave.configureConstruction(BuildingType.HARDENED, IBuilding.CASTLE_BRIAN, 2, 150, 0, List.of(CubeCoords.ZERO, EAST));
        cave.getDesign().setOpenSpace(true);
        cave.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        assertEquals(600, cave.getWeight());
        assertEquals(600, BuildingConstruction.capacityInHex(cave, CubeCoords.ZERO));
        var equipment = cave.addEquipment(EquipmentType.get("Unspecified Building Equipment"), 0);
        equipment.setSize(550);
        assertTrue(verifier(cave).constructionIssues().isEmpty(), verifier(cave).constructionIssues().toString());
        equipment.setLocation(1);
        assertTrue(verifier(cave).constructionIssues().stream().anyMatch(s -> s.contains("ground level")));
        equipment.setLocation(0);
        equipment.setSize(601);
        assertTrue(verifier(cave).constructionIssues().stream().anyMatch(s -> s.contains("carrying capacity")));
        var loaded = (BuildingEntity) new BLKStructureFile(BLKFile.getBlock(cave)).getEntity();
        assertTrue(loaded.getDesign().isOpenSpace());
        assertEquals(600, loaded.getWeight());
        loaded.getDesign().setHeavyMetal(true);
        assertEquals(450, loaded.getWeight(), "The heavy-metal reduction applies to the total open-space budget");
    }

    @Test
    void wallsCountProtectionCapacityAndCostPerOccupiedSideAndKeepSidesOnRotation() throws Exception {
        var wall = building(4);
        wall.configureConstruction(BuildingType.MEDIUM, IBuilding.WALL, 4, 40, 32, List.of(CubeCoords.ZERO));
        wall.getDesign().getWallSides().put(CubeCoords.ZERO, 3);
        assertEquals(320, wall.getWeight());
        assertEquals(4, wall.getArmorWeight());
        assertEquals((5000 * 40 * 4 * 2 + 40000) * 1.4, wall.getCost(false));
        assertTrue(verifier(wall).constructionIssues().isEmpty());
        wall.configureConstruction(BuildingType.MEDIUM, IBuilding.WALL, 4, 40, 32, List.of(CubeCoords.ZERO),
              hex -> hex, side -> (side + 1) % 6);
        assertEquals(6, wall.getDesign().wallSides(CubeCoords.ZERO));
        var loaded = (BuildingEntity) new BLKStructureFile(BLKFile.getBlock(wall)).getEntity();
        assertEquals(6, loaded.getDesign().wallSides(CubeCoords.ZERO));
        assertEquals(320, loaded.getWeight());
    }

    @Test
    void tentsFencesAndBridgesHaveNoInteriorAndBridgeSlopeMatchesTheWorkedExample() throws Exception {
        var structure = building(1);
        structure.configureConstruction(BuildingType.LIGHT, IBuilding.TENT, 1, 2, 0, List.of(CubeCoords.ZERO));
        assertEquals(0, structure.getWeight());
        assertEquals(0, structure.getBaseGeneratorWeight());
        assertEquals(2040, structure.getCost(false));
        assertTrue(verifier(structure).constructionIssues().isEmpty());
        structure.configureConstruction(BuildingType.LIGHT, IBuilding.FENCE, 3, 1, 0, List.of(CubeCoords.ZERO));
        structure.getDesign().getWallSides().put(CubeCoords.ZERO, 3);
        assertEquals(0, structure.getWeight());
        assertEquals(4848, structure.getCost(false));
        var hexes = java.util.stream.IntStream.rangeClosed(0, 8).mapToObj(q -> new CubeCoords(q, 0, -q)).toList();
        structure.configureConstruction(BuildingType.RAIL, IBuilding.BRIDGE, 1, 650, 0, hexes);
        BuildingConstruction.setBridgeSlope(structure, 5, 7);
        assertEquals(List.of(5, 5, 6, 6, 6, 6, 7, 7, 7), hexes.stream().map(structure.getDesign()::bridgeDeck).toList());
        assertEquals(List.of(7, 6, 5), BuildingConstruction.mapLevels(structure));
        assertTrue(verifier(structure).constructionIssues().isEmpty(), verifier(structure).constructionIssues().toString());
        var loaded = (BuildingEntity) new BLKStructureFile(BLKFile.getBlock(structure)).getEntity();
        assertEquals(BuildingType.RAIL, loaded.getBuildingType());
        assertEquals(structure.getDesign().getBridgeDecks(), loaded.getDesign().getBridgeDecks());
        loaded.getDesign().getBridgeDecks().put(hexes.get(1), 6);
        assertTrue(verifier(loaded).constructionIssues().stream().anyMatch(s -> s.contains("steady linear slope")));
        loaded.addEquipment(EquipmentType.get("Heat Sink"), 0);
        assertTrue(verifier(loaded).constructionIssues().stream().anyMatch(s -> s.contains("cannot install equipment")));
    }

    @Test
    void generatorSizingFuelAndDistributedFailureFollowTheRules() throws Exception {
        var building = building(2);
        building.addEquipment(EquipmentType.get("ISMediumLaser"), 1);
        building.addEquipment(EquipmentType.get("ISMediumLaser"), 3);
        assertEquals(7, BuildingConstruction.generatorTons(building, StructureEngine.COMBUSTION_LIQUID));
        assertEquals(5, BuildingConstruction.generatorTons(building, StructureEngine.FUSION));
        assertEquals(5, BuildingConstruction.dailyFuel(building, StructureEngine.COMBUSTION_LIQUID, 2));
        var generator = building.addEquipment(EquipmentType.get("FUSION PowerGenerator"), 0);
        generator.setSize(5);
        building.getDesign().getEquipmentSpace().put(generator,
              List.of(new BuildingDesign.Position(CubeCoords.ZERO, 0), new BuildingDesign.Position(EAST, 1)));
        assertEquals(2.5, BuildingConstruction.equipmentWeightInHex(building, generator, CubeCoords.ZERO));
        assertEquals(2.5, BuildingConstruction.equipmentWeightInHex(building, generator, EAST));
        assertEquals(7, verifier(building).calculateWeight());
        assertTrue(building.hasPower());
        building.getInternalBuilding().setCurrentCF(0, EAST);
        assertFalse(building.hasPower(), "Destruction of any occupied generator hex disables the single generator");
    }

    @Test
    void militaryCrewIncludesEquipmentOperatorsGunnersAndOfficersButQuartersAreOptional() throws Exception {
        var building = building(3);
        var laser = building.addEquipment(EquipmentType.get("ISMediumLaser"), 0);
        building.addEquipment(EquipmentType.get("ISMediumLaser"), 0);
        building.addEquipment(EquipmentType.get("ISAC20"), 0);
        building.addEquipment(EquipmentType.get("ISAMS"), 0);
        building.addEquipment(EquipmentType.get("Field Kitchen"), 0);
        building.addEquipment(EquipmentType.get("MASH Equipment"), 0).setSize(2);
        var crew = BuildingConstruction.crew(building);
        assertEquals(13, crew.crew());
        assertEquals(5, crew.gunners());
        assertEquals(2, crew.officers());
        assertEquals(20, Compute.getFullCrewSize(building));
        assertEquals(5, Compute.getTotalGunnerNeeds(building));
        building.getDesign().getAutomatedWeapons().add(laser);
        assertEquals(4, BuildingConstruction.crew(building).gunners());
        assertTrue(building.getTransportBays().isEmpty());
    }

    @Test
    void bayPlacementCanOverloadOneHexWhileTotalMassStillFits() throws Exception {
        var building = building(1);
        building.configureConstruction(BuildingType.LIGHT, IBuilding.STANDARD, 1, 15, 0, List.of(CubeCoords.ZERO, EAST));
        var quarters = new FirstClassQuartersCargoBay(1);
        building.addTransporter(quarters);
        for (int i = 0; i < 10; i++) {
            building.addEquipment(EquipmentType.get("Heat Sink"), 0);
        }
        assertEquals(15, BuildingConstruction.installedWeightInHex(building, CubeCoords.ZERO));
        assertTrue(verifier(building).constructionIssues().isEmpty());
        building.getDesign().getBaySpace().put(quarters,
              List.of(new BuildingDesign.Space(new BuildingDesign.Position(CubeCoords.ZERO, 0), 10)));
        assertEquals(20, verifier(building).calculateWeight());
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(issue -> issue.contains("this hex's carrying capacity")));
    }

    @Test
    void structureMultipliersDoNotMultiplyEquipmentCostOrInventEquipmentMass() throws Exception {
        var building = building(2);
        var design = building.getDesign();
        design.setEnvironmentalSealing(true);
        design.setHeavyMetal(true);
        design.setCeiling(BuildingDesign.Ceiling.HIGH);
        assertEquals(240, building.getWeight());
        assertEquals(0, verifier(building).calculateWeight());
        assertEquals(6_400_000 * 1.5 * 1.25 * 1.1 * 1.8, building.getCost(false), .001);
        assertTrue(building.hasEnvironmentalSealing());
        building.addEquipment(EquipmentType.get(BuildingEquipmentType.Facility.UNSPECIFIED.internalName()), 0).setSize(10);
        building.addEquipment(EquipmentType.get(BuildingEquipmentType.Facility.UNSPECIFIED.internalName()), 1).setSize(5);
        assertEquals(15, verifier(building).calculateWeight());
        assertEquals((6_400_000 * 1.5 * 1.25 * 1.1 + 80 * 5000) * 1.8, building.getCost(false), .001);
    }

    @Test
    void elevatorsReserveTheirStopsAndRoofAndDoorsMustFaceOutside() throws Exception {
        var building = building(3);
        var design = building.getDesign();
        var lift = new BuildingDesign.Elevator(CubeCoords.ZERO, 60, Map.of(0, 4, 1, 4, 2, 4, 3, 4));
        design.getElevators().add(lift);
        assertEquals(9, lift.weight());
        assertEquals(9, verifier(building).calculateWeight());
        assertTrue(verifier(building).constructionIssues().isEmpty());
        var laser = building.addEquipment(EquipmentType.get("ISMediumLaser"), 2);
        laser.setSponsonTurretMounted(true);
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(issue -> issue.contains("elevator's served levels")));
        design.getDoors().add(new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 0), 2, 1));
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(issue -> issue.contains("exterior hexside")));
    }

    @Test
    void elevatorShaftCannotSkipOccupiedFloorsOrOverlapAnotherLift() throws Exception {
        var building = building(4);
        var lift = new BuildingDesign.Elevator(CubeCoords.ZERO, 60, Map.of(0, 4, 3, 4));
        building.getDesign().getElevators().add(lift);
        building.addEquipment(EquipmentType.get("Heat Sink"), 2);
        assertEquals(9, lift.weight(), "Weight includes the entire shaft, even when an imported design omits access levels");
        var issues = verifier(building).constructionIssues();
        assertTrue(issues.stream().anyMatch(issue -> issue.contains("continuous range")));
        assertTrue(issues.stream().anyMatch(issue -> issue.contains("elevator's served levels")));
        building.getDesign().getElevators().add(new BuildingDesign.Elevator(CubeCoords.ZERO, 20, Map.of(2, 4, 3, 4)));
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(issue -> issue.contains("shafts cannot overlap")));
    }

    @Test
    void nativeLiquidCargoReusesStorageMassAndNeedsNoPowerUntilOtherEquipmentIsAdded() throws Exception {
        var building = building(2);
        var storage = new LiquidCargoBay(91, 0, 1);
        building.addTransporter(storage);
        assertEquals(100, verifier(building).calculateWeight());
        assertEquals(0, BuildingConstruction.generatorTons(building, StructureEngine.FUSION));
        assertEquals(0, BuildingConstruction.dailyFuel(building, StructureEngine.COMBUSTION_LIQUID, 0));
        building.addEquipment(EquipmentType.get("Heat Sink"), 0);
        assertEquals(4, BuildingConstruction.generatorTons(building, StructureEngine.FUSION));
    }

    @Test
    void landingDeckRequiresACompleteRoofFootprintAndKeepsNativeFacilityData() throws Exception {
        var building = building(10);
        var hexes = new java.util.ArrayList<CubeCoords>();
        hexes.add(CubeCoords.ZERO);
        hexes.addAll(CubeCoords.ZERO.neighbors());
        building.configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 10, 80, 0, hexes);
        var type = EquipmentType.get(BuildingEquipmentType.Facility.LANDING_DECK.internalName());
        var deck = building.addEquipment(type, 9);
        deck.setSize(7);
        building.getDesign().getEquipmentSpace().put(deck,
              hexes.stream().map(hex -> new BuildingDesign.Position(hex, 9)).toList());
        assertEquals(3500, verifier(building).calculateWeight());
        assertEquals(21, BuildingConstruction.crew(building).crew());
        assertTrue(verifier(building).constructionIssues().isEmpty(), verifier(building).constructionIssues().toString());
        var loaded = (BuildingEntity) new BLKStructureFile(BLKFile.getBlock(building)).getEntity();
        assertEquals(type, loaded.getMisc().getFirst().getType());
        assertEquals(3500, verifier(loaded).calculateWeight());
        assertEquals(7, loaded.getDesign().getEquipmentSpace().values().iterator().next().size());
        building.getDesign().getEquipmentSpace().put(deck,
              hexes.subList(0, 6).stream().map(hex -> new BuildingDesign.Position(hex, 9)).toList());
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(issue -> issue.contains("landing deck needs")));
    }

    @Test
    void removingAutomatedEquipmentRemovesItsAdditionalCostAndNativeReferences() throws Exception {
        var building = building(1);
        var laser = building.addEquipment(EquipmentType.get("ISMediumLaser"), 0);
        building.getDesign().getAutomatedWeapons().add(laser);
        assertEquals(1000, BuildingConstruction.additionalCost(building));
        ConstructionUtil.removeMounted(building, laser);
        assertEquals(0, BuildingConstruction.additionalCost(building));
        assertFalse(BLKFile.getBlock(building).exists("building_equipment_space"));
    }

    @Test
    void capitalWeaponsHaveControlsAndCannotShareTheirHexesWithTurrets() throws Exception {
        var building = building(20);
        var gun = building.addEquipment(EquipmentType.get("Naval Autocannon (NAC/10)"), 0);
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(issue -> issue.contains("fusion or fission")));
        var generator = building.addEquipment(EquipmentType.get("FUSION PowerGenerator"), 1);
        generator.setSize(40);
        building.getDesign().getEquipmentSpace().put(gun,
              List.of(new BuildingDesign.Position(CubeCoords.ZERO, 0), new BuildingDesign.Position(EAST, 0)));
        assertEquals(gun.getTonnage() / 20, BuildingConstruction.capitalControls(building, EAST));
        assertEquals(gun.getTonnage() / 10, verifier(building).getWeightControls());
        assertTrue(verifier(building).constructionIssues().isEmpty(),
              "Capital weapons face upward, and do not require an exterior wall facing: " + verifier(building).constructionIssues());
        var laser = building.addEquipment(EquipmentType.get("ISMediumLaser"), 39);
        laser.setSponsonTurretMounted(true);
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(issue -> issue.contains("reserved roof space")));
    }

    @Test
    void nativeRoundTripPreservesFeaturesAndReferencesDespiteEquipmentInsertionOrder() throws Exception {
        var original = building(3);
        var generator = original.addEquipment(EquipmentType.get("FUSION PowerGenerator"), 4);
        generator.setSize(6);
        var laser = original.addEquipment(EquipmentType.get("ISMediumLaser"), 1);
        laser.setFacing(0);
        var quarters = new FirstClassQuartersCargoBay(2);
        original.addTransporter(quarters);
        var design = original.getDesign();
        design.setEnvironmentalSealing(true);
        design.setHeavyMetal(true);
        design.setCeiling(BuildingDesign.Ceiling.LOW);
        design.setSite(BuildingDesign.Site.UNDERWATER);
        design.setDepth(2);
        design.getAutomatedWeapons().add(laser);
        design.getEquipmentSpace().put(generator,
              List.of(new BuildingDesign.Position(EAST, 1), new BuildingDesign.Position(CubeCoords.ZERO, 2)));
        design.getBaySpace().put(quarters, List.of(new BuildingDesign.Space(new BuildingDesign.Position(EAST, 0), 20)));
        design.getDoors().add(new BuildingDesign.Door(new BuildingDesign.Position(EAST, 0), 2, 2));
        design.getElevators().add(new BuildingDesign.Elevator(CubeCoords.ZERO, 40, Map.of(0, 4, 1, 4)));
        var loaded = (BuildingEntity) new BLKStructureFile(BLKFile.getBlock(original)).getEntity();
        var result = loaded.getDesign();
        assertTrue(result.hasEnvironmentalSealing());
        assertTrue(result.hasHeavyMetal());
        assertEquals(BuildingDesign.Ceiling.LOW, result.getCeiling());
        assertEquals(BuildingDesign.Site.UNDERWATER, result.getSite());
        assertEquals(2, result.getDepth());
        assertEquals(design.getDoors(), result.getDoors());
        assertEquals(design.getElevators(), result.getElevators());
        assertEquals(1, result.getAutomatedWeapons().size());
        assertEquals(EquipmentType.get("ISMediumLaser"), result.getAutomatedWeapons().iterator().next().getType());
        assertEquals(4, result.getEquipmentSpace().keySet().iterator().next().getLocation());
        assertEquals(design.getEquipmentSpace().get(generator), result.getEquipmentSpace().values().iterator().next());
        assertEquals(20, BuildingConstruction.bayWeightInHex(loaded, EAST));
        assertEquals(verifier(original).calculateWeight(), verifier(loaded).calculateWeight());
    }

    @Test
    void distributedCapitalWeaponUsesEveryAdjacentBuildingHex() throws Exception {
        var building = building(20);
        var west = new CubeCoords(-1, 0, 1);
        building.configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 20, 80, 0, List.of(CubeCoords.ZERO, EAST, west));
        building.addEquipment(EquipmentType.get("FUSION PowerGenerator"), 1).setSize(60);
        var gun = building.addEquipment(EquipmentType.get("Naval Autocannon (NAC/10)"), 0);
        building.getDesign().getEquipmentSpace().put(gun,
              List.of(new BuildingDesign.Position(CubeCoords.ZERO, 0), new BuildingDesign.Position(EAST, 0)));
        assertTrue(verifier(building).constructionIssues().stream().anyMatch(issue -> issue.contains("include all immediately adjacent")));
        building.getDesign().getEquipmentSpace().put(gun, List.of(new BuildingDesign.Position(CubeCoords.ZERO, 0),
              new BuildingDesign.Position(EAST, 0), new BuildingDesign.Position(west, 0)));
        assertTrue(verifier(building).constructionIssues().isEmpty(), verifier(building).constructionIssues().toString());
    }

    @Test
    void malformedNativeEquipmentReferencesAndNonFiniteMassAreRejected() throws Exception {
        var building = building(2);
        building.addEquipment(EquipmentType.get("ISMediumLaser"), 0);
        var block = new BuildingBlock();
        block.writeBlockData("building_equipment_space", new String[] { "0,99;true;;0" });
        assertThrows(EntityLoadingException.class, () -> BuildingDesignCodec.read(block, building));
        var invalidMass = new BuildingBlock();
        invalidMass.writeBlockData("building_equipment_space", new String[] { "0,0;false;;NaN" });
        assertThrows(EntityLoadingException.class, () -> BuildingDesignCodec.read(invalidMass, building));
        building.addFailedEquipment("Missing mounted weapon");
        var unresolvedReference = new BuildingBlock();
        unresolvedReference.writeBlockData("building_equipment_space", new String[] { "0,0;true;;0" });
        assertThrows(EntityLoadingException.class, () -> BuildingDesignCodec.read(unresolvedReference, building),
              "An unknown earlier equipment entry must not shift automation onto a different item");
    }

    @Test
    void oneShotAmmoDoesNotChangeNativeEquipmentPlacementReferences() throws Exception {
        var building = building(2);
        building.addEquipment(EquipmentType.get("ISLRM5 (OS)"), 0);
        var laser = building.addEquipment(EquipmentType.get("ISMediumLaser"), 0);
        building.getDesign().getAutomatedWeapons().add(laser);
        var loaded = (BuildingEntity) new BLKStructureFile(BLKFile.getBlock(building)).getEntity();
        assertEquals(1, loaded.getDesign().getAutomatedWeapons().size());
        assertEquals(EquipmentType.get("ISMediumLaser"), loaded.getDesign().getAutomatedWeapons().iterator().next().getType());
        assertEquals(2, loaded.getWeaponList().size());
    }

    @Test
    void buildingsHaveNoArtificialHundredItemLimitPerFloor() throws Exception {
        var building = building(3);
        for (int i = 0; i < 120; i++) {
            building.addEquipment(EquipmentType.get("Heat Sink"), 0);
        }
        long criticals = java.util.stream.IntStream.range(0, building.getNumberOfCriticalSlots(0))
              .filter(slot -> building.getCritical(0, slot) != null).count();
        assertEquals(120, criticals);
        assertEquals(120, verifier(building).calculateWeight());
        assertTrue(verifier(building).constructionIssues().isEmpty());
        var loaded = (BuildingEntity) new BLKStructureFile(BLKFile.getBlock(building)).getEntity();
        assertEquals(120, loaded.getEquipment().size());
    }
}
