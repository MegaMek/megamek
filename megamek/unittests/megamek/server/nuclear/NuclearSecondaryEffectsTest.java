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
package megamek.server.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.server.Server;
import megamek.server.nuclear.NuclearSecondaryEffects.SecondaryEffectBand;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Real units rolling on the Secondary Nuclear Effects Table (IO:AE pp. 165-166).
 */
class NuclearSecondaryEffectsTest {
    private static final String UNIT_FILES = "testresources/megamek/common/units/";
    private static final int BOARD_SIZE = 40;
    private static final Coords GROUND_ZERO = new Coords(20, 20);
    private static final Coords UNIT_HEX = new Coords(20, 5);

    private TWGameManager gameManager;
    private Game game;
    private Server server;
    private Player player;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(anyInt(), any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());
        game = gameManager.getGame();
        game.setOptions(new GameOptions());
        server = ServerFactory.createServer(gameManager);
        player = new Player(0, "Test");
        game.addPlayer(0, player);

        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex(0);
        }
        Board board = new Board();
        board.newData(BOARD_SIZE, BOARD_SIZE, hexes, null);
        game.setBoard(board);
    }

    @AfterEach
    void afterEach() {
        server.die();
    }

    private Entity placeUnit(String fileName) throws Exception {
        Entity entity = new MekFileParser(new File(UNIT_FILES + fileName)).getEntity();
        entity.setId(game.getNextEntityId());
        game.addEntity(entity);
        entity.setOwner(player);
        entity.setPosition(UNIT_HEX);
        entity.setDeployed(true);
        return entity;
    }

    /** The AFFS platoon with its encumbering armor kit taken off, so it fights unarmored. */
    private Entity placeUnarmoredPlatoon() throws Exception {
        ConvInfantry platoon = (ConvInfantry) placeUnit("Foot Platoon (AFFS) (Laser 3067+).blk");
        platoon.setArmorKit(null);
        platoon.setArmorEncumbering(false);
        assertFalse(platoon.hasArmor());
        return platoon;
    }

    private static Roll fixedRoll(int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        when(roll.getReport()).thenReturn(String.valueOf(value));
        return roll;
    }

    /** Applies the secondary effects with every 2d6 roll forced to {@code value}. */
    private void applyWithRoll(Entity entity, int value) {
        Roll roll = fixedRoll(value);
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.rollD6(2)).thenReturn(roll);
            new NuclearSecondaryEffects(gameManager).apply(entity, GROUND_ZERO, new Vector<Report>());
        }
    }

    private int shutdownPilotingRolls(Entity entity) {
        int rolls = 0;
        for (Enumeration<PilotingRollData> psrs = game.getPSRs(); psrs.hasMoreElements(); ) {
            PilotingRollData psr = psrs.nextElement();
            if ((psr.getEntityId() == entity.getId()) && psr.getPlainDesc().contains("reactor shutdown")) {
                rolls++;
            }
        }
        return rolls;
    }

    @Test
    void unitTypeBonusGoesToCombatUnitsOnly() throws Exception {
        assertTrue(NuclearSecondaryEffects.hasUnitTypeBonus(placeUnit("Atlas AS7-D.mtf")), "Mek");
        assertTrue(NuclearSecondaryEffects.hasUnitTypeBonus(placeUnit("Bulldog Medium Tank.blk")), "combat vehicle");
        assertTrue(NuclearSecondaryEffects.hasUnitTypeBonus(placeUnit("Elemental BA [Laser] (Sqd5).blk")),
              "battle armor");
        assertTrue(NuclearSecondaryEffects.hasUnitTypeBonus(placeUnit("Cheetah F-11.blk")), "aerospace fighter");
        assertTrue(NuclearSecondaryEffects.hasUnitTypeBonus(placeUnit("Union (3055).blk")), "DropShip");

        assertFalse(NuclearSecondaryEffects.hasUnitTypeBonus(placeUnit("Air Car.blk")), "support vehicle");
        assertFalse(NuclearSecondaryEffects.hasUnitTypeBonus(placeUnit("Mosquito Light Fighter.blk")),
              "fixed-wing support vehicle");
        assertFalse(NuclearSecondaryEffects.hasUnitTypeBonus(placeUnit("Centaur.blk")), "ProtoMek");
        assertFalse(NuclearSecondaryEffects.hasUnitTypeBonus(placeUnit("Foot Platoon (AFFS) (Laser 3067+).blk")),
              "conventional infantry");
    }

    @Test
    void hardenedBuildingAddsTwoMore() {
        assertEquals(0, NuclearSecondaryEffects.rollBonus(false, false));
        assertEquals(2, NuclearSecondaryEffects.rollBonus(true, false));
        assertEquals(2, NuclearSecondaryEffects.rollBonus(false, true));
        assertEquals(4, NuclearSecondaryEffects.rollBonus(true, true));
    }

    @Test
    void rollsFallIntoTheTableRows() {
        assertEquals(SecondaryEffectBand.DESTROYED, SecondaryEffectBand.forRoll(4));
        assertEquals(SecondaryEffectBand.SEVERE, SecondaryEffectBand.forRoll(5));
        assertEquals(SecondaryEffectBand.SEVERE, SecondaryEffectBand.forRoll(6));
        assertEquals(SecondaryEffectBand.MODERATE, SecondaryEffectBand.forRoll(7));
        assertEquals(SecondaryEffectBand.MODERATE, SecondaryEffectBand.forRoll(10));
        assertEquals(SecondaryEffectBand.NONE, SecondaryEffectBand.forRoll(11));
    }

    @Test
    void atlasRollingThreePlusTwoTakesTheSevereRowAndShutsDown() throws Exception {
        // 3 + 2 = 5: two crits, four pilot hits, and the same 3 fails the 10+ Shutdown Avoid Check
        Entity atlas = placeUnit("Atlas AS7-D.mtf");

        applyWithRoll(atlas, 3);

        Mockito.verify(gameManager).damageCrew(atlas, 4);
        assertTrue(atlas.isShutDown(), "failed the 10+ Shutdown Avoid Check");
        assertEquals(1, shutdownPilotingRolls(atlas), "a shut-down Mek rolls to stay standing");
        assertTrue(atlas.isSufferingEMI());
    }

    @Test
    void atlasRollingSixPlusTwoTakesTheModerateRowAndStaysRunning() throws Exception {
        // 6 + 2 = 8: one crit, two pilot hits, and 6 passes the 6+ check
        Entity atlas = placeUnit("Atlas AS7-D.mtf");

        applyWithRoll(atlas, 6);

        Mockito.verify(gameManager).damageCrew(atlas, 2);
        assertFalse(atlas.isShutDown(), "passed the 6+ Shutdown Avoid Check");
        assertTrue(atlas.isSufferingEMI());
    }

    @Test
    void protoMekTakesTheMekRowWithoutAShutdownCheck() throws Exception {
        // No +2 for a ProtoMek: a 5 stays a 5, the severe row
        Entity centaur = placeUnit("Centaur.blk");

        applyWithRoll(centaur, 5);

        Mockito.verify(gameManager).damageCrew(centaur, 4);
        assertFalse(centaur.isShutDown(), "ProtoMeks have no shutdown rules");
    }

    @Test
    void fighterTakesCritsPilotHitsAndAShutdownCheck() throws Exception {
        // 3 + 2 = 5 for an aerospace fighter: the severe row, and 3 fails the 10+ check
        Entity cheetah = placeUnit("Cheetah F-11.blk");

        applyWithRoll(cheetah, 3);

        Mockito.verify(gameManager).damageCrew(cheetah, 4);
        assertTrue(cheetah.isShutDown(), "fighters make the Shutdown Avoid Check");
    }

    @Test
    void dropShipSkipsTheShutdownCheck() throws Exception {
        // Nothing restarts a DropShip in the heat phase, so it never makes the check
        Entity union = placeUnit("Union (3055).blk");

        new NuclearSecondaryEffects(gameManager).rollShutdownAvoid(union,
              NuclearSecondaryEffects.SEVERE_SHUTDOWN_TARGET, fixedRoll(2), new Vector<Report>());

        assertFalse(union.isShutDown());
    }

    @Test
    void unarmoredInfantryOutsideABuildingIsKilledOnTheSevereRow() throws Exception {
        Entity platoon = placeUnarmoredPlatoon();

        applyWithRoll(platoon, 5);

        assertTrue(platoon.isDoomed() || platoon.isDestroyed());
    }

    @Test
    void armoredInfantryLosesHalfOnTheSevereRow() throws Exception {
        Entity cavalry = placeUnit("Fast Recon Cavalry Point.blk");
        assertTrue(((ConvInfantry) cavalry).hasArmor(), "the point wears Clan armor kits");
        int troopers = cavalry.getInternal(ConvInfantry.LOC_INFANTRY);

        applyWithRoll(cavalry, 5);

        assertFalse(cavalry.isDoomed() || cavalry.isDestroyed(), "armored infantry is not killed outright");
        assertEquals(troopers - (int) Math.ceil(troopers / 2.0), cavalry.getInternal(ConvInfantry.LOC_INFANTRY));
    }

    @Test
    void armoredInfantryLosesAQuarterOnTheModerateRow() throws Exception {
        Entity cavalry = placeUnit("Fast Recon Cavalry Point.blk");
        int troopers = cavalry.getInternal(ConvInfantry.LOC_INFANTRY);

        applyWithRoll(cavalry, 7);

        assertEquals(troopers - (int) Math.ceil(troopers / 4.0), cavalry.getInternal(ConvInfantry.LOC_INFANTRY));
    }

    @Test
    void unarmoredInfantryLosesHalfOnTheModerateRow() throws Exception {
        Entity platoon = placeUnarmoredPlatoon();
        int troopers = platoon.getInternal(ConvInfantry.LOC_INFANTRY);

        applyWithRoll(platoon, 7);

        assertEquals(troopers - (int) Math.ceil(troopers / 2.0), platoon.getInternal(ConvInfantry.LOC_INFANTRY));
    }

    @Test
    void lowRollDestroysTheUnitAndItCannotRoll1d6Crits() throws Exception {
        // A support vehicle has no +2, so a 4 is the destroyed row
        Entity airCar = placeUnit("Air Car.blk");

        applyWithRoll(airCar, 4);

        assertTrue(airCar.isDoomed() || airCar.isDestroyed());
        Mockito.verify(gameManager, Mockito.never()).oneCriticalEntity(any(Entity.class), anyInt(),
              Mockito.anyBoolean(), anyInt());
    }

    @Test
    void highRollDoesNoDamageButLeavesEmi() throws Exception {
        Entity atlas = placeUnit("Atlas AS7-D.mtf");
        int armorBefore = atlas.getTotalArmor();

        applyWithRoll(atlas, 9);

        assertEquals(armorBefore, atlas.getTotalArmor());
        Mockito.verify(gameManager, Mockito.never()).damageCrew(any(Entity.class), anyInt());
        assertTrue(atlas.isSufferingEMI(), "EMI lasts the rest of the scenario even on 11+");
    }
}
