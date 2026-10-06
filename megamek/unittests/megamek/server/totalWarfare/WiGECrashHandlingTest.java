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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import megamek.common.CriticalSlot;
import megamek.common.GameBoardTestCase;
import megamek.common.Hex;
import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.InfantryCompartment;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * WiGE crashes that are not sideslips: engine hits, MP reduced to 0 and lost crews (issue #9104; TW pp.55, 68, 199
 * and 224). Driven through the {@link TWGameManager} entry points the game uses.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class WiGECrashHandlingTest extends GameBoardTestCase {

    private static final double WIGE_TONNAGE = 30.0;
    private static final int ARMOR_PER_LOCATION = 40;
    private static final int SOUTH = 3;
    private static final int WIGE_ID = 5;
    private static final int INFANTRY_ID = 6;

    static {
        initializeBoard("CLEAR", """
              size 1 1
              hex 0101 0 "" ""
              end""");

        initializeBoard("DEEP_WATER", """
              size 1 1
              hex 0101 0 "water:2" ""
              end""");

        initializeBoard("ROUGH", """
              size 1 1
              hex 0101 0 "rough:1" ""
              end""");

        initializeBoard("SAND", """
              size 1 1
              hex 0101 0 "sand:1" ""
              end""");

        // A one-level building far too weak to carry a 30-ton WiGE
        initializeBoard("WEAK_BUILDING", """
              size 1 1
              hex 0101 0 "bldg_elev:1;building:1:0;bldg_cf:10" ""
              end""");
    }

    private TWGameManager gameManager;

    /** Every d6 rolls a 4, so 2D6 is 8: forced landings succeed and no critical hit fires. */
    private static final class MiddleDice extends MMRandom {
        @Override
        public int randomInt(int maxValue) {
            return maxValue / 2;
        }

        @Override
        public float randomFloat() {
            return 0.5f;
        }
    }

    /** Every d6 rolls a 1, so 2D6 is 2: every infantry survival roll succeeds. */
    private static final class LowDice extends MMRandom {
        @Override
        public int randomInt(int maxValue) {
            return 0;
        }

        @Override
        public float randomFloat() {
            return 0.0f;
        }
    }

    @AfterEach
    void restoreDice() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    @BeforeEach
    void setUp() throws IOException {
        Compute.setRNG(new MiddleDice());
        gameManager = new TWGameManager();
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        if (getGame().getPlayer(0) == null) {
            getGame().addPlayer(0, new Player(0, "Test"));
        }
    }

    /** A 30-ton WiGE airborne at elevation 1 in hex 0101 facing south, with armor deep enough to survive a crash. */
    private SupportTank airborneWiGE(int armorPerLocation) {
        SupportTank wige = new SupportTank();
        wige.setChassis("Test");
        wige.setModel("WiGE");
        wige.setMovementMode(EntityMovementMode.WIGE);
        wige.setWeight(WIGE_TONNAGE);
        wige.setOriginalWalkMP(8);
        wige.autoSetInternal();
        for (int location = 0; location < wige.locations(); location++) {
            wige.initializeArmor(armorPerLocation, location);
        }
        wige.setCrew(new Crew(CrewType.SINGLE));
        wige.setOwner(getGame().getPlayer(0));
        wige.setId(WIGE_ID);
        getGame().addEntity(wige);
        wige.setPosition(new Coords(0, 0));
        wige.setFacing(SOUTH);
        wige.setElevation(1);
        return wige;
    }

    /** Loads a conventional infantry platoon into the WiGE. */
    private ConvInfantry loadInfantry(SupportTank wige) {
        wige.addTransporter(new InfantryCompartment(10.0));
        ConvInfantry infantry = new ConvInfantry();
        infantry.setChassis("Test");
        infantry.setModel("Platoon");
        infantry.setOwner(getGame().getPlayer(0));
        infantry.setId(INFANTRY_ID);
        getGame().addEntity(infantry);
        wige.load(infantry, false);
        infantry.setTransportId(wige.getId());
        return infantry;
    }

    private void hitEngine(Tank tank) {
        gameManager.applyCriticalHit(tank, Tank.LOC_FRONT,
              new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Tank.CRIT_ENGINE), true, 0, false);
    }

    private boolean reported(Iterable<Report> reports, int messageId) {
        for (Report report : reports) {
            if (report.messageId == messageId) {
                return true;
            }
        }
        return false;
    }

    // Water counts as clear terrain for a WiGE (TW p.55)

    @Test
    void engineHitOverWaterRollsAndLandsOnTheSurface() {
        setBoard("DEEP_WATER");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);

        hitEngine(wige);

        assertFalse(wige.isDoomed(), "a WiGE treats water as clear, so the engine hit is a roll, not a crash");
        assertEquals(0, wige.getElevation(), "it lands on the water");
    }

    @Test
    void crashOverWaterComesDownOnTheSurface() {
        setBoard("DEEP_WATER");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);

        Vector<Report> reports = gameManager.crashVTOLorWiGE(wige);

        assertFalse(wige.isDoomed(), "a WiGE floats, so a crash over water is not fatal");
        assertEquals(0, wige.getElevation());
        assertTrue(reported(reports, 6276), "the report says it came down on the water");
        assertFalse(reported(reports, 6275), "it did not fall into the water");
    }

    @Test
    void vtolCrashIntoWaterIsStillFatal() {
        setBoard("DEEP_WATER");
        SupportTank vtol = airborneWiGE(ARMOR_PER_LOCATION);
        vtol.setMovementMode(EntityMovementMode.VTOL);

        gameManager.crashVTOLorWiGE(vtol);

        assertTrue(vtol.isDoomed(), "VTOL crashes are unchanged");
    }

    // Engine hit terrain (TW p.199)

    @Test
    void engineHitOverSandIsAnAutomaticCrash() {
        setBoard("SAND");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);

        hitEngine(wige);

        assertTrue(wige.isDoomed(), "no roll over sand: the WiGE crashes, and cannot land in sand (TW p.68)");
    }

    @Test
    void engineHitOverRoughRollsAndLands() {
        setBoard("ROUGH");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);

        hitEngine(wige);

        assertFalse(wige.isDoomed(), "TW p.199 gives a roll over rough terrain, and a success lands the WiGE");
        assertEquals(0, wige.getElevation());
    }

    @Test
    void onlyClearPavedRoughBuildingAndWaterGiveALandingRoll() {
        Coords coords = new Coords(0, 0);
        assertTrue(AirborneVehicleCrashHandler.canWiGERollToLand(new Hex(0)), "clear");
        assertTrue(AirborneVehicleCrashHandler.canWiGERollToLand(new Hex(0, "pavement:1", "", coords)), "paved");
        assertTrue(AirborneVehicleCrashHandler.canWiGERollToLand(new Hex(0, "rough:1", "", coords)), "rough");
        assertTrue(AirborneVehicleCrashHandler.canWiGERollToLand(new Hex(0, "water:3", "", coords)), "water");
        assertTrue(AirborneVehicleCrashHandler.canWiGERollToLand(new Hex(0, "water:1;ice:1", "", coords)),
              "frozen water");
        assertTrue(AirborneVehicleCrashHandler.canWiGERollToLand(
              new Hex(0, "bldg_elev:1;building:1:0;bldg_cf:10", "", coords)), "building");
        String[] crashTerrain = { "rough:2", "swamp:1", "sand:1", "snow:1", "planted_fields:1", "woods:1", "rubble:1",
                                  "mud:1", "tundra:1", "ice:1" };
        for (String terrain : crashTerrain) {
            assertFalse(AirborneVehicleCrashHandler.canWiGERollToLand(new Hex(0, terrain, "", coords)), terrain);
        }
    }

    // Forced landing on a roof (TW p.199 Buildings)

    @Test
    void forcedLandingOnAWeakRoofCollapsesTheBuilding() {
        setBoard("WEAK_BUILDING");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);
        wige.setElevation(2);

        hitEngine(wige);

        assertTrue(getGame().getBoard().getHex(0, 0).containsTerrain(Terrains.RUBBLE)
                    || (getGame().getBoard().getBuildingAt(new Coords(0, 0)) == null),
              "a 30-ton WiGE on a CF 10 roof collapses the building");
    }

    // Crash site, no attacks and no explosion (TW p.68, p.199)

    @Test
    void crashInAHexItCannotLandInDestroysTheWiGE() {
        setBoard("ROUGH");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);

        gameManager.crashVTOLorWiGE(wige);

        assertTrue(wige.isDoomed(), "a WiGE can only land in clear, paved or water hexes (TW p.55, p.68)");
    }

    @Test
    void crashedWiGECannotAttackThatTurn() {
        setBoard("CLEAR");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);

        gameManager.crashVTOLorWiGE(wige);

        assertFalse(wige.isDoomed());
        assertTrue(wige.hasCrashedThisTurn(), "the vehicle may not attack in the turn it crashes (TW p.68)");
    }

    @Test
    void wigeDoesNotExplodeWhenTheCrashDamagesInternalStructure() {
        setBoard("CLEAR");
        // No armor, so the crash damage goes straight to the internal structure
        SupportTank wige = airborneWiGE(0);
        for (int location = 0; location < wige.locations(); location++) {
            wige.initializeInternal(20, location);
        }
        int internalBefore = wige.getTotalInternal();

        Vector<Report> reports = gameManager.crashVTOLorWiGE(wige);

        assertNotEquals(internalBefore, wige.getTotalInternal(), "the crash reached the internal structure");
        assertFalse(reported(reports, 6285), "VTOL explosions do not apply to WiGEs (TW p.199)");
        assertFalse(wige.isDoomed(), "the WiGE survives with damaged internal structure");
    }

    // Carried infantry (TW p.224)

    @Test
    void infantryRollsToSurviveWhenTheWiGESurvives() {
        setBoard("CLEAR");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);
        ConvInfantry infantry = loadInfantry(wige);

        // Every d6 rolls a 4: the infantry is destroyed
        Vector<Report> reports = gameManager.crashVTOLorWiGE(wige);

        assertFalse(wige.isDoomed());
        assertTrue(reported(reports, 6378), "each infantry unit rolls 1D6");
        assertTrue(infantry.isDestroyed(), "a 4 kills the infantry (1-3 survives)");
    }

    @Test
    void infantrySurvivesOnALowRoll() {
        setBoard("CLEAR");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);
        ConvInfantry infantry = loadInfantry(wige);
        Compute.setRNG(new LowDice());

        gameManager.crashVTOLorWiGE(wige);

        assertFalse(wige.isDoomed());
        assertFalse(infantry.isDestroyed(), "a 1 lets the infantry survive");
        assertEquals(wige.getId(), infantry.getTransportId(), "it is still aboard");
    }

    @Test
    void infantryDiesWithAWiGEDestroyedInTheCrash() {
        setBoard("ROUGH");
        SupportTank wige = airborneWiGE(ARMOR_PER_LOCATION);
        ConvInfantry infantry = loadInfantry(wige);
        // Every d6 rolls a 1, which would let the infantry escape a wreck that was not destroyed in a crash
        Compute.setRNG(new LowDice());

        gameManager.crashVTOLorWiGE(wige);

        assertTrue(wige.isDoomed(), "the WiGE cannot land in rough terrain, so the crash destroys it");
        assertTrue(infantry.isDestroyed(), "all infantry in a vehicle destroyed in a crash die (TW p.224)");
    }
}
