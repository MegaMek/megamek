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
package megamek.common.rules.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.Player;
import megamek.common.battlefieldSupport.BFSSpecial;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.ScanMission;
import megamek.common.equipment.Sensor;
import megamek.common.game.Game;
import megamek.common.planetaryConditions.EMI;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesEquipment;
import megamek.common.rules.RulesManager;
import megamek.common.rules.RulesScanning;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.Aero;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Core Rules scan check (p.233 Scanning, p.113 Sensor Checks): a Piloting roll at +3 with only the scan's own
 * modifiers, 2 hexes or the probe's range.
 */
class CoreRulesScanningTest {

    /** The rules with the ECM questions answered by the test rather than by the board. */
    private static final class RulesWithEcmAnswers extends CoreRulesScanning {
        private boolean targetInsideHostileEcm = false;
        private boolean targetInsideHostileAngelEcm = false;

        @Override
        protected boolean isTargetInsideHostileEcm(Entity scanner, Targetable target) {
            return targetInsideHostileEcm;
        }

        @Override
        protected boolean isTargetInsideHostileAngelEcm(Entity scanner, Targetable target) {
            return targetInsideHostileAngelEcm;
        }
    }

    private final RulesWithEcmAnswers rules = new RulesWithEcmAnswers();
    private Mek scanner;
    private Targetable hexTarget;

    @BeforeEach
    void setUp() {
        scanner = mock(Mek.class);
        Crew crew = mock(Crew.class);
        when(crew.getPiloting()).thenReturn(4);
        when(scanner.getCrew()).thenReturn(crew);
        when(scanner.getPosition()).thenReturn(new Coords(3, 3));
        when(scanner.getMisc()).thenReturn(List.of());
        hexTarget = mock(Targetable.class);
        when(hexTarget.getPosition()).thenReturn(new Coords(5, 3));
    }

    @Test
    void testAScanIsPilotingPlusThreeAndNothingElse() {
        TargetRoll roll = rules.scanTargetRoll(scanner, hexTarget);

        assertEquals(7, roll.getValue(), "Piloting 4 plus the scanning modifier of 3");
        assertTrue(roll.needsRoll());
    }

    @Test
    void testTheDefaultRangeIsTwoHexes() {
        assertEquals(CoreRulesScanning.DEFAULT_SCANNING_RANGE, rules.scanningRange(scanner, hexTarget));
    }

    @Test
    void hostileEcmBlocksAnAssetProbeWhilePlanetaryInterferenceAloneDoesNot() {
        Game game = new Game();
        game.setBoard(Board.createEmptyBoard(8, 12));
        BattlefieldSupportAsset asset = new BattlefieldSupportAsset();
        asset.setOwner(new Player(1, "Scout"));
        asset.addSpecial(BFSSpecial.of("PRB"));
        asset.setCrew(new Crew(asset.defaultCrewType()));
        asset.setPosition(new Coords(5, 1));
        game.addEntity(asset);
        CoreRulesScanning scanning = new CoreRulesScanning();

        assertEquals(4, scanning.scanningRange(asset, hexTarget));
        assertEquals(6, scanning.scanTargetRoll(asset, hexTarget).getValue());
        game.getPlanetaryConditions().setEMI(EMI.EMI);
        BattlefieldSupportAsset jammer = new BattlefieldSupportAsset();
        jammer.setOwner(new Player(2, "Enemy"));
        jammer.addSpecial(BFSSpecial.of("ECM"));
        jammer.setPosition(new Coords(5, 10)); // Seven hexes from the target, outside the ECM field.
        game.addEntity(jammer);
        assertEquals(4, scanning.scanningRange(asset, hexTarget));
        assertEquals(6, scanning.scanTargetRoll(asset, hexTarget).getValue());

        jammer.setPosition(new Coords(5, 9)); // Six hexes from the target, eight from the probe.
        assertEquals(4, scanning.scanningRange(asset, null));
        assertEquals(2, scanning.scanningRange(asset, hexTarget));
        assertEquals(8, scanning.scanTargetRoll(asset, hexTarget).getValue());
    }

    @Test
    void testOneSensorHitAddsTwoAndTwoSensorHitsForbidTheCheck() {
        when(scanner.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD)).thenReturn(1);
        assertEquals(9, rules.scanTargetRoll(scanner, hexTarget).getValue());

        when(scanner.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD)).thenReturn(2);
        TargetRoll blocked = rules.scanTargetRoll(scanner, hexTarget);
        assertEquals(TargetRoll.IMPOSSIBLE, blocked.getValue());
        assertEquals(0, rules.scanningRange(scanner, hexTarget), "a unit that cannot check has no range either");
    }

    @Test
    void testAWorkingProbeExtendsTheRangeAndLowersTheNumberByItsLevel() {
        mountProbe(scanner, "BeagleActiveProbe", 4);

        assertEquals(4, rules.scanningRange(scanner, hexTarget), "the probe's range replaces the default 2");
        assertEquals(5, rules.scanTargetRoll(scanner, hexTarget).getValue(), "4 + 3 - 2 for a Beagle-class probe");
    }

    @Test
    void testABloodhoundIsLevelThreeAndALightProbeLevelOne() {
        mountProbe(scanner, "BloodhoundActiveProbe", 8);
        assertEquals(4, rules.scanTargetRoll(scanner, hexTarget).getValue(), "4 + 3 - 3");
        assertEquals(8, rules.scanningRange(scanner, hexTarget));

        mountProbe(scanner, "ISLightActiveProbe", 3);
        assertEquals(6, rules.scanTargetRoll(scanner, hexTarget).getValue(), "4 + 3 - 1");
    }

    @Test
    void testAProbeInsideHostileEcmGivesNeitherRangeNorBonus() {
        mountProbe(scanner, "BeagleActiveProbe", 4);
        // hasBAP(true) is the engine's "working and not negated by ECM" answer
        when(scanner.hasBAP(true)).thenReturn(false);

        assertEquals(CoreRulesScanning.DEFAULT_SCANNING_RANGE, rules.scanningRange(scanner, hexTarget));
        assertEquals(7, rules.scanTargetRoll(scanner, hexTarget).getValue());
    }

    @Test
    void testAStandardProbeIsAlsoNegatedByEcmCoveringTheTarget() {
        // Core Rules p.197: the probe cannot scan anything if it OR the target is inside hostile ECM
        mountProbe(scanner, "BeagleActiveProbe", 4);
        rules.targetInsideHostileEcm = true;

        assertEquals(CoreRulesScanning.DEFAULT_SCANNING_RANGE, rules.scanningRange(scanner, hexTarget));
        assertEquals(7, rules.scanTargetRoll(scanner, hexTarget).getValue(), "no probe bonus either");
        assertEquals(4, rules.scanningRange(scanner, null), "the probe itself still works: only this target is jammed");
    }

    @Test
    void testABloodhoundIgnoresOrdinaryEcmOnTheTargetButNotAngel() {
        mountProbe(scanner, "BloodhoundActiveProbe", 8);
        rules.targetInsideHostileEcm = true;

        assertEquals(8, rules.scanningRange(scanner, hexTarget));
        assertEquals(4, rules.scanTargetRoll(scanner, hexTarget).getValue(), "4 + 3 - 3, ordinary ECM ignored");

        rules.targetInsideHostileAngelEcm = true;
        assertEquals(CoreRulesScanning.DEFAULT_SCANNING_RANGE, rules.scanningRange(scanner, hexTarget));
        assertEquals(7, rules.scanTargetRoll(scanner, hexTarget).getValue(), "Angel ECM negates a Bloodhound");
    }

    @Test
    void testAnActiveStealthSystemOnTheTargetAddsTwo() {
        Entity stealthyTarget = mock(Mek.class);
        when(stealthyTarget.getPosition()).thenReturn(new Coords(5, 3));
        when(stealthyTarget.isStealthActive()).thenReturn(true);

        assertEquals(9, rules.scanTargetRoll(scanner, stealthyTarget).getValue());
    }

    @Test
    void testAnyStealthSystemAddsTheModifierNotOnlyStealthArmour() {
        Entity nullSignatureTarget = mock(Mek.class);
        when(nullSignatureTarget.getPosition()).thenReturn(new Coords(5, 3));
        when(nullSignatureTarget.isStealthActive()).thenReturn(false);
        when(nullSignatureTarget.isNullSigActive()).thenReturn(true);

        assertEquals(9, rules.scanTargetRoll(scanner, nullSignatureTarget).getValue(),
              "the rulebook says stealth system, which covers more than stealth armour");
    }

    @Test
    void testAerospaceUnitsCannotScanUnderCoreRules() {
        Aero fighter = mock(Aero.class);
        when(fighter.isAero()).thenReturn(true);
        when(fighter.getCrew()).thenReturn(mock(Crew.class));

        assertEquals(TargetRoll.IMPOSSIBLE, rules.scanTargetRoll(fighter, hexTarget).getValue());
        assertEquals(0, rules.scanningRange(fighter, hexTarget));
    }

    @Test
    void testAVtolIsAVehicleAndScansLikeOne() {
        Tank vtol = mock(Tank.class);
        Crew crew = mock(Crew.class);
        when(crew.getPiloting()).thenReturn(5);
        when(vtol.getCrew()).thenReturn(crew);
        when(vtol.getMisc()).thenReturn(List.of());
        when(vtol.isAirborneVTOLorWIGE()).thenReturn(true);

        assertEquals(8, rules.scanTargetRoll(vtol, hexTarget).getValue(), "driving 5 + 3, airborne or not");
    }

    @Test
    void testInfantryScanTheirOwnHexAutomaticallyAndRollForAnythingElse() {
        Infantry platoon = mock(Infantry.class);
        Crew crew = mock(Crew.class);
        when(crew.getPiloting()).thenReturn(4);
        when(platoon.getCrew()).thenReturn(crew);
        when(platoon.isInfantry()).thenReturn(true);
        when(platoon.getPosition()).thenReturn(new Coords(3, 3));
        when(platoon.getMisc()).thenReturn(List.of());

        Targetable ownHex = mock(Targetable.class);
        when(ownHex.getPosition()).thenReturn(new Coords(3, 3));
        when(ownHex.getTargetType()).thenReturn(Targetable.TYPE_HEX_CLEAR);
        TargetRoll ownHexRoll = rules.scanTargetRoll(platoon, ownHex);
        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, ownHexRoll.getValue());
        assertFalse(ownHexRoll.needsRoll());

        Targetable unitInOwnHex = mock(Targetable.class);
        when(unitInOwnHex.getPosition()).thenReturn(new Coords(3, 3));
        when(unitInOwnHex.getTargetType()).thenReturn(Targetable.TYPE_ENTITY);
        assertEquals(8, rules.scanTargetRoll(platoon, unitInOwnHex).getValue(),
              "Core p.233: only 'any building and/or hex they occupy' is automatic, not a unit there");

        assertEquals(8, rules.scanTargetRoll(platoon, hexTarget).getValue(),
              "Core p.233: infantry scan a target outside their hex at the ground Asset TN of 8, whatever their skill");
    }

    /**
     * Gives the unit one working probe of the named type and the engine's answers for it: {@code hasBAP(true)} true
     * and the given range.
     */
    private static MiscMounted mountProbe(Entity unit, String internalName, int range) {
        MiscType probeType = mock(MiscType.class);
        when(probeType.hasFlag(MiscType.F_BAP)).thenReturn(true);
        when(probeType.hasFlag(MiscType.F_BLOODHOUND)).thenReturn(internalName.contains("Bloodhound"));
        when(probeType.getInternalName()).thenReturn(internalName);
        MiscMounted probe = mock(MiscMounted.class);
        when(probe.getType()).thenReturn(probeType);
        when(probe.isInoperable()).thenReturn(false);
        when(unit.getMisc()).thenReturn(List.of(probe));
        when(unit.hasBAP(true)).thenReturn(true);
        when(unit.getBAPRange()).thenReturn(range);
        when(unit.getBadCriticalSlots(anyInt(), anyInt(), anyInt())).thenReturn(0);
        return probe;
    }

    @Test
    void anInactiveBloodhoundCannotLendItsBonusOrEcmImmunityToAnotherProbe() {
        MiscMounted bloodhound = mountProbe(scanner, "BloodhoundActiveProbe", 8);
        when(bloodhound.isModeTurnedOff()).thenReturn(true);
        MiscMounted beagle = mountProbe(scanner, "BeagleActiveProbe", 4);
        when(scanner.getMisc()).thenReturn(List.of(bloodhound, beagle));

        assertEquals(5, rules.scanTargetRoll(scanner, hexTarget).getValue());
        rules.targetInsideHostileEcm = true;
        assertEquals(7, rules.scanTargetRoll(scanner, hexTarget).getValue());
        assertEquals(2, rules.scanningRange(scanner, hexTarget));
    }

    @Test
    void infantryAutomaticallyScansAnOccupiedHexOfABuildingEntityOnItsBoard() {
        Infantry platoon = mock(Infantry.class);
        when(platoon.getCrew()).thenReturn(mock(Crew.class));
        when(platoon.isInfantry()).thenReturn(true);
        BuildingEntity building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        building.getInternalBuilding().setBuildingHeight(2);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 0, BasementType.NONE, false);
        building.getInternalBuilding().addHex(new CubeCoords(0, 1, -1), 40, 0, BasementType.NONE, false);
        building.setPosition(new Coords(1, 3));
        Coords occupied = building.getSecondaryPositions().get(1);
        when(platoon.getPosition()).thenReturn(occupied);
        building.setBoardId(platoon.getBoardId());

        assertEquals(0, RulesScanning.scanDistance(platoon, building));
        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, rules.scanTargetRoll(platoon, building).getValue());
        when(platoon.getElevation()).thenReturn(3);
        assertEquals(8, rules.scanTargetRoll(platoon, building).getValue());
        when(platoon.getElevation()).thenReturn(-1);
        assertEquals(8, rules.scanTargetRoll(platoon, building).getValue());
        when(platoon.getElevation()).thenReturn(0);
        building.setBoardId(platoon.getBoardId() + 1);
        assertEquals(8, rules.scanTargetRoll(platoon, building).getValue());
    }

    @Test
    void missionWatchdogBonusUsesTheActiveManagersEquipmentRules() {
        EquipmentType.initializeTypes();
        MiscMounted probe = mock(MiscMounted.class);
        when(probe.getType()).thenReturn((MiscType) EquipmentType.get(Sensor.WATCHDOG));
        when(scanner.getMisc()).thenReturn(List.of(probe));
        when(scanner.hasBAP(true)).thenReturn(true);

        RulesManager previous = Game.rulesManager;
        try {
            Game game = new Game();
            Game.rulesManager = new TWRulesManager();
            assertEquals(6, ScanMission.scanningRules(game).scanTargetRoll(scanner, null).getValue());

            CoreRulesManager core = spy(new CoreRulesManager());
            Game.rulesManager = core;
            RulesScanning mission = ScanMission.scanningRules(game);
            assertEquals(5, mission.scanTargetRoll(scanner, null).getValue());

            RulesEquipment equipment = mock(RulesEquipment.class);
            when(equipment.watchdogUsesLightProbe()).thenReturn(true);
            when(core.getRulesEquipment()).thenReturn(equipment);
            assertEquals(6, mission.scanTargetRoll(scanner, null).getValue());
        } finally {
            Game.rulesManager = previous;
        }
    }
}
