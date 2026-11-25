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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.units.EntityMovementMode;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AssetRoadMovementTest extends GameBoardTestCase {
    private final RulesManager previousRules = Game.rulesManager;

    @AfterEach
    void restoreRules() {
        Game.rulesManager = previousRules;
    }

    @ParameterizedTest
    @CsvSource({
          "VEHICLE, TRACKED, true", "VEHICLE, WHEELED, true", "VEHICLE, HOVER, true", "VEHICLE, VTOL, false",
          "CONV_INFANTRY, TRACKED, true", "CONV_INFANTRY, WHEELED, true", "CONV_INFANTRY, HOVER, true",
          "CONV_INFANTRY, INF_MOTORIZED, true", "CONV_INFANTRY, INF_LEG, false", "BATTLE_ARMOR, INF_LEG, false"
    })
    void pavedMoveCanSpendOneMoreThanThePrintedAllowance(BFSAssetType type, EntityMovementMode mode, boolean bonus) {
        BattlefieldSupportAsset asset = asset(type, mode, 1);
        MovePath path = plot(asset, "road:1", "pavement:1", 0, mode.isVTOL() ? 1 : 0,
              MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertEquals(2, path.getMpUsed());
        assertEquals(bonus, path.isMoveLegal());
    }

    @ParameterizedTest
    @CsvSource({
          "VEHICLE, HOVER, road:3, true", "VEHICLE, TRACKED, road:3, false", "VEHICLE, WHEELED, road:3, false",
          "VEHICLE, HOVER, road:4, true", "VEHICLE, TRACKED, road:4, true", "VEHICLE, WHEELED, road:4, false",
          "CONV_INFANTRY, HOVER, road:3, false", "CONV_INFANTRY, TRACKED, road:4, false"
    })
    void dirtAndGravelAwardTheirBonusOnlyToTheListedVehicles(BFSAssetType type, EntityMovementMode mode,
          String road, boolean bonus) {
        MovePath path = plot(asset(type, mode, 1), road, road, 0, 0, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertEquals(2, path.getMpUsed());
        assertEquals(bonus, path.isMoveLegal());
    }

    @ParameterizedTest
    @CsvSource({
          "VEHICLE, WHEELED, woods:1", "VEHICLE, HOVER, woods:1;foliage_elev:1", "VEHICLE, TRACKED, woods:2",
          "CONV_INFANTRY, INF_MOTORIZED, rough:1", "CONV_INFANTRY, WHEELED, rubble:1",
          "CONV_INFANTRY, HOVER, jungle:1", "CONV_INFANTRY, TRACKED, jungle:2"
    })
    void groundAssetsNeedARoadThroughProhibitedTerrain(BFSAssetType type, EntityMovementMode mode, String terrain) {
        String road = "road:1;" + terrain;
        assertFalse(plot(asset(type, mode, 8), "", road, 0, 0, MoveStepType.FORWARDS).isMoveLegal());
        assertFalse(plot(asset(type, mode, 8), road, "", 0, 0, MoveStepType.FORWARDS).isMoveLegal());
        MovePath path = plot(asset(type, mode, 8), "pavement:1", road, 0, 0, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertEquals(1, path.getMpUsed());
    }

    @ParameterizedTest
    @CsvSource({"VEHICLE, TRACKED, 0", "CONV_INFANTRY, INF_LEG, -4", "BATTLE_ARMOR, INF_LEG, 0"})
    void groundAssetCanReachConnectedPavedBridgeDeckTwoLevelsAway(BFSAssetType type, EntityMovementMode mode,
          int groundLevel) {
        MovePath path = plot(asset(type, mode, 8), "road:1", "bridge:1:9;bridge_cf:80;bridge_elev:2",
              groundLevel, 0, MoveStepType.CLIMB_MODE_ON, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertEquals(2, path.getFinalElevation());
        assertEquals(5, path.getMpUsed());
    }

    @Test
    void jumpInfantryPaysJumpCostAcrossPavement() {
        BattlefieldSupportAsset unit = asset(BFSAssetType.CONV_INFANTRY, EntityMovementMode.INF_JUMP, 2);
        assertFalse(unit.isEligibleForPavementOrRoadBonus());
        MovePath path = plot(unit, "pavement:1", "road:1", 2, 0, MoveStepType.START_JUMP, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertEquals(1, path.getMpUsed());
    }

    @Test
    void emplacementAndFootBattleArmorWithMechanizedSpecialDoNotReceiveBonus() {
        BattlefieldSupportAsset armor = asset(BFSAssetType.BATTLE_ARMOR, EntityMovementMode.INF_LEG, 1);
        armor.addSpecial(BFSSpecial.of("Mechanized"));
        assertFalse(armor.isEligibleForPavementOrRoadBonus());
        assertFalse(asset(BFSAssetType.EMPLACEMENT, EntityMovementMode.NONE, 0).isEligibleForPavementOrRoadBonus());
        BattlefieldSupportAsset immobile = asset(BFSAssetType.VEHICLE, EntityMovementMode.TRACKED, 0);
        immobile.addSpecial(BFSSpecial.of("Immobile"));
        assertFalse(immobile.isEligibleForPavementOrRoadBonus());
    }

    @Test
    void vtolMustClearTerrainEvenAboveARoad() {
        assertFalse(plot(asset(BFSAssetType.VEHICLE, EntityMovementMode.VTOL, 8), "road:1", "road:1",
              0, 0, MoveStepType.FORWARDS).isMoveLegal());
        assertFalse(plot(asset(BFSAssetType.VEHICLE, EntityMovementMode.VTOL, 8), "road:1", "woods:1;road:1",
              0, 2, MoveStepType.FORWARDS).isMoveLegal());
        assertTrue(plot(asset(BFSAssetType.VEHICLE, EntityMovementMode.VTOL, 8), "road:1", "woods:1;road:1",
              0, 3, MoveStepType.FORWARDS).isMoveLegal());
    }

    @Test
    void vtolUsesItsCurrentCardAllowanceEvenWhenItHasStoredJumpMp() {
        BattlefieldSupportAsset unit = asset(BFSAssetType.VEHICLE, EntityMovementMode.INF_JUMP, 4);
        unit.setMovementMode(EntityMovementMode.VTOL);
        unit.setMp(1);
        assertEquals(1, unit.getMp());
        assertTrue(plot(unit, "road:1", "road:1", 0, 1, MoveStepType.FORWARDS).isMoveLegal());
        assertFalse(plot(unit, "road:1", "road:1", 0, 1, MoveStepType.FORWARDS, MoveStepType.FORWARDS)
              .isMoveLegal());
    }

    @Test
    void footInfantryNeedsItsSnowSpecialToEnterSnow() {
        BattlefieldSupportAsset unit = asset(BFSAssetType.CONV_INFANTRY, EntityMovementMode.INF_LEG, 8);
        assertFalse(plot(unit, "", "snow:1", 0, 0, MoveStepType.FORWARDS).isMoveLegal());
        unit.addSpecial(BFSSpecial.of("Snow"));
        assertTrue(plot(unit, "", "snow:1", 0, 0, MoveStepType.FORWARDS).isMoveLegal());
    }

    @ParameterizedTest
    @CsvSource({
          "VEHICLE, TRACKED, woods:1, true", "VEHICLE, TRACKED, woods:2;foliage_elev:1, true",
          "VEHICLE, WHEELED, water:1, false", "VEHICLE, WHEELED, water:1;ice:1, true",
          "VEHICLE, HOVER, water:1, true", "CONV_INFANTRY, INF_LEG, water:1, false",
          "BATTLE_ARMOR, INF_LEG, water:1, false", "BATTLE_ARMOR, INF_LEG, snow:1, true"
    })
    void offRoadMotiveRestrictions(BFSAssetType type, EntityMovementMode mode, String terrain, boolean legal) {
        assertEquals(legal, plot(asset(type, mode, 8), "", terrain, 0, 0, MoveStepType.FORWARDS).isMoveLegal());
    }

    @ParameterizedTest
    @CsvSource({"road:1", "''"})
    void snowAndIceAffectTheTravelSurfaceAndRespectSnowSpecial(String surface) {
        for (String condition : new String[] { "snow:2", "ice:1" }) {
            String destination = surface.isEmpty() ? condition : surface + ";" + condition;
            BattlefieldSupportAsset vehicle = asset(BFSAssetType.VEHICLE, EntityMovementMode.WHEELED, 4);
            assertEquals(2, plot(vehicle, surface, destination, 0, 0, MoveStepType.FORWARDS).getMpUsed());
            vehicle.addSpecial(BFSSpecial.of("Snow"));
            assertEquals(1, plot(vehicle, surface, destination, 0, 0, MoveStepType.FORWARDS).getMpUsed());
        }
    }

    @ParameterizedTest
    @CsvSource({"VEHICLE, WHEELED, 2", "VEHICLE, TRACKED, 1", "CONV_INFANTRY, INF_LEG, 1"})
    void iceCostsTheSameMpWhenAnAssetMovesWithoutCare(BFSAssetType type, EntityMovementMode mode, int expectedMp) {
        MovePath path = plot(asset(type, mode, 4), "", "ice:1", 0, 0);
        path.setCareful(false);
        path.addStep(MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertEquals(expectedMp, path.getMpUsed());
    }

    @Test
    void snowCoversPavementDuringSnowfall() {
        BattlefieldSupportAsset foot = asset(BFSAssetType.CONV_INFANTRY, EntityMovementMode.INF_LEG, 4);
        assertFalse(plot(foot, "road:1", "pavement:1;snow:2", 0, 0, MoveStepType.FORWARDS).isMoveLegal());
        assertFalse(plot(foot, "road:1;snow:2", "pavement:1", 0, 0, MoveStepType.FORWARDS).isMoveLegal());
        foot.addSpecial(BFSSpecial.of("Snow"));
        assertTrue(plot(foot, "road:1", "pavement:1;snow:2", 0, 0, MoveStepType.FORWARDS).isMoveLegal());
    }

    @ParameterizedTest
    @CsvSource({"CONV_INFANTRY, INF_LEG", "CONV_INFANTRY, INF_MOTORIZED", "BATTLE_ARMOR, INF_LEG"})
    void infantryTurnsForFreeAndPaysTwoMpPerLevelWhenLeavingARoad(BFSAssetType type, EntityMovementMode mode) {
        MovePath path = plot(asset(type, mode, 3), "road:1", "", 1, 0,
              MoveStepType.TURN_LEFT, MoveStepType.TURN_RIGHT, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertEquals(3, path.getMpUsed());
    }

    @ParameterizedTest
    @CsvSource({"rough:1", "rubble:1"})
    void hoverAssetsTreatPassableTerrainAsClear(String terrain) {
        MovePath path = plot(asset(BFSAssetType.VEHICLE, EntityMovementMode.HOVER, 1), "road:1", terrain, 0, 0,
              MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal());
        assertEquals(1, path.getMpUsed());
    }

    @Test
    void reverseMovementReducesTheWholeTurnAllowanceWhileKeepingAnEarnedRoadBonus() {
        BattlefieldSupportAsset vehicle = asset(BFSAssetType.VEHICLE, EntityMovementMode.WHEELED, 2);
        MovePath path = plot(vehicle, "road:1", "road:1", 0, 0, MoveStepType.FORWARDS, MoveStepType.BACKWARDS);
        assertTrue(path.isMoveLegal());
        path.addStep(MoveStepType.FORWARDS);
        assertFalse(path.isMoveLegal());

        path = plot(vehicle, "", "", 0, 0, MoveStepType.FORWARDS, MoveStepType.BACKWARDS);
        assertFalse(path.isMoveLegal());
    }

    @Test
    void reverseAndLevelChangesCannotBeCombinedInEitherOrderOrAcrossResumedMoves() {
        BattlefieldSupportAsset vehicle = asset(BFSAssetType.VEHICLE, EntityMovementMode.WHEELED, 10);
        MovePath hill = plot(vehicle, "road:1", "road:1", 1, 0, MoveStepType.FORWARDS, MoveStepType.FORWARDS);
        assertTrue(hill.isMoveLegal());
        assertTrue(hill.getLastStep().hasAssetChangedLevel());
        hill.addStep(MoveStepType.BACKWARDS);
        assertFalse(hill.isMoveLegal());

        vehicle.setMovedBackwards(true);
        assertFalse(plot(vehicle, "road:1", "road:1", 1, 0, MoveStepType.FORWARDS).isMoveLegal());
        vehicle.setMovedBackwards(false);
        vehicle.recordLevelChange();
        MovePath flat = plot(vehicle, "road:1", "road:1", 0, 0, MoveStepType.FORWARDS, MoveStepType.BACKWARDS);
        assertFalse(flat.isMoveLegal());
    }

    private BattlefieldSupportAsset asset(BFSAssetType type, EntityMovementMode mode, int mp) {
        BattlefieldSupportAsset unit = new BattlefieldSupportAsset();
        // Supply a fully initialized unit so GameBoardTestCase keeps the card's actual walk and jump allowances.
        unit.setId(5);
        unit.setWeight(10);
        unit.setAssetType(type);
        unit.setMovementMode(mode);
        unit.setMp(mp);
        return unit;
    }

    private MovePath plot(BattlefieldSupportAsset unit, String from, String to, int level, int startElevation,
          MoveStepType... steps) {
        Game.rulesManager = new CoreRulesManager();
        getGame().setBoard(BoardLoader.initializeBoard("""
              size 1 3
              hex 0101 0 "%s" ""
              hex 0102 %d "%s" ""
              hex 0103 %d "%s" ""
              end""".formatted(from, level, to, level, to)));
        return getMovePathFor(unit, startElevation, unit.getMovementMode(), steps);
    }
}
