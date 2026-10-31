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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Vector;
import java.util.stream.Stream;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.rolls.Roll;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Fuel-air munitions against infantry and low-BAR units follow IO:AE pp.159-160 and two forum rulings (issue
 * #9316): the kill roll counts vertical distance and reaches past the damage radius.
 */
class FuelAirRulesTest {

    private static final int SECOND_BOARD = 1;
    private static final int REPORT_KILL_ROLL = 9987;
    private static final Coords IMPACT = new Coords(8, 8);

    private TWGameManager gameManager;
    private Game game;
    private Player defender;
    private AeroSpaceFighter bomber;
    private BombType smallFuelAirBomb;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(anyInt(), any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());
        game = gameManager.getGame();
        game.setBoard(0, board(0));
        game.setBoard(SECOND_BOARD, board(SECOND_BOARD));

        Player attacker = new Player(0, "Attacker");
        defender = new Player(1, "Defender");
        attacker.setTeam(1);
        defender.setTeam(2);
        game.addPlayer(0, attacker);
        game.addPlayer(1, defender);

        bomber = new AeroSpaceFighter();
        bomber.setGame(game);
        bomber.setId(game.getNextEntityId());
        bomber.setChassis("Test Fighter");
        bomber.setModel("Bomber");
        bomber.setCrew(new Crew(CrewType.SINGLE));
        bomber.setOwner(attacker);
        bomber.setWeight(50);
        game.addEntity(bomber);
        bomber.setPosition(new Coords(8, 2));
        bomber.setBoardId(SECOND_BOARD);
        bomber.setAltitude(3);
        bomber.setDeployed(true);

        smallFuelAirBomb = (BombType) EquipmentType.get(BombTypeEnum.FAE_SMALL.getInternalName());
    }

    private static Board board(int boardId) {
        int width = 16;
        int height = 17;
        Board board = new Board(width, height,
              Stream.generate(Hex::new).limit((long) width * height).toArray(Hex[]::new));
        board.setBoardId(boardId);
        return board;
    }

    private ConvInfantry platoon(Coords position, int boardId) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setId(game.getNextEntityId());
        infantry.setChassis("Test Platoon");
        infantry.setModel("Foot");
        infantry.setCrew(new Crew(CrewType.INFANTRY_CREW));
        infantry.setOwner(defender);
        infantry.setPrimaryWeapon((InfantryWeapon) EquipmentType.get("InfantryAssaultRifle"));
        infantry.autoSetInternal();
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        place(infantry, position, boardId);
        return infantry;
    }

    private BattleArmor battleArmor(Coords position) {
        BattleArmor battleArmor = new BattleArmor();
        battleArmor.setGame(game);
        battleArmor.setId(game.getNextEntityId());
        battleArmor.setChassis("Test BA");
        battleArmor.setModel("Elemental");
        battleArmor.setSquadSize(5);
        battleArmor.setCrew(new Crew(CrewType.INFANTRY_CREW));
        battleArmor.setOwner(defender);
        battleArmor.setWeight(1);
        battleArmor.autoSetInternal();
        for (int location = 1; location < battleArmor.locations(); location++) {
            battleArmor.initializeInternal(2, location);
            battleArmor.initializeArmor(10, location);
        }
        place(battleArmor, position, SECOND_BOARD);
        return battleArmor;
    }

    private void place(Entity entity, Coords position, int boardId) {
        game.addEntity(entity);
        entity.setPosition(position);
        entity.setBoardId(boardId);
        entity.setDeployed(true);
    }

    private static Roll rollOf(int value) {
        Roll roll = mock(Roll.class);
        doReturn(value).when(roll).getIntValue();
        doReturn("").when(roll).getReport();
        return roll;
    }

    private void damage(Entity entity, int damage) {
        AreaEffectHelper.artilleryDamageEntity(entity, damage, null, 0, false, false, false, 0, IMPACT,
              smallFuelAirBomb, entity.getPosition(), true, bomber, game.getHexOf(entity), bomber.getId(),
              new Vector<>(), gameManager);
    }

    /** Runs the kill roll for a blast at ground level of the impact hex on the second board, with fixed dice. */
    private Vector<Report> killRoll(int diceTotal) {
        Vector<Report> reports = new Vector<>();
        Roll roll = rollOf(diceTotal);
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.rollD6(2)).thenReturn(roll);
            AreaEffectHelper.checkInfantryDestruction(IMPACT, SECOND_BOARD, 0, bomber, new Vector<>(), reports, game,
                  gameManager);
        }
        return reports;
    }

    private static int count(Vector<Report> reports, int messageId) {
        int found = 0;
        for (Report report : reports) {
            if (report.messageId == messageId) {
                found++;
            }
        }
        return found;
    }

    private static boolean destroyed(Entity entity) {
        return entity.isDoomed() || entity.isDestroyed();
    }

    /** 5 damage, x2 area effect, x2 in the open: 20 of 28 troopers. Before the fix fuel-air doubled again to 40. */
    @Test
    void conventionalInfantryInTheOpenTakeFourTimesNotEightTimes() {
        ConvInfantry infantry = platoon(IMPACT, SECOND_BOARD);

        damage(infantry, 5);

        assertEquals(8, infantry.getInternal(ConvInfantry.LOC_INFANTRY), "28 troopers less 5 x2 x2");
    }

    /**
     * Woods on the platoon's own board, clear terrain at the same hex on the first board: 5 x2 = 10, with no
     * in-the-open doubling. Before the fix the open-terrain check read the first board.
     */
    @Test
    void openTerrainIsJudgedOnTheInfantryUnitsOwnBoard() {
        game.getBoard(SECOND_BOARD).getHex(IMPACT).addTerrain(new Terrain(Terrains.WOODS, 1));
        ConvInfantry infantry = platoon(IMPACT, SECOND_BOARD);

        damage(infantry, 5);

        assertEquals(18, infantry.getInternal(ConvInfantry.LOC_INFANTRY), "28 troopers less 5 x2 in woods");
    }

    /**
     * 20 damage to BAR 5 armor is x1.5 rounded once: 30 in all. Rounding each 5-point cluster up gave 32. The damage
     * each cluster deals is captured as it is handed to the damage code, so hit location, rear armor and critical
     * hits play no part.
     */
    @Test
    void lowBarDamageIsRoundedOnceOverTheWholeAttack() {
        ArgumentCaptor<Integer> clusterDamage = ArgumentCaptor.forClass(Integer.class);
        Mockito.doReturn(new Vector<Report>()).when(gameManager).damageEntity(any(), any(), clusterDamage.capture(),
              anyBoolean(), any(), anyBoolean(), anyBoolean(), anyBoolean());
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis("Test Mek");
        mek.setModel("Commercial");
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(defender);
        mek.setWeight(50.0);
        mek.autoSetInternal();
        mek.setArmorType(EquipmentType.T_ARMOR_COMMERCIAL);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(60, location);
        }
        place(mek, IMPACT, SECOND_BOARD);
        assertEquals(5, mek.getBARRating(BipedMek.LOC_CENTER_TORSO), "commercial armor is BAR 5");

        damage(mek, 20);

        int total = 0;
        for (int dealt : clusterDamage.getAllValues()) {
            total += dealt;
        }
        assertEquals(4, clusterDamage.getAllValues().size(), "four 5-point clusters");
        assertEquals(30, total, "20 x1.5 = 30");
    }

    /** 1 hex out, a roll of 6 gives 7: battle armor die only on less than 7, so it survives. */
    @Test
    void battleArmorSurvivesAModifiedRollOfSeven() {
        BattleArmor battleArmor = battleArmor(new Coords(8, 9));

        Vector<Report> reports = killRoll(6);

        assertEquals(1, count(reports, REPORT_KILL_ROLL), "the battle armor rolls");
        assertFalse(destroyed(battleArmor), "6 + 1 = 7 is not less than 7");
    }

    /** At elevation 10 over the impact hex the distance is 2: a roll of 8 gives 10, which survives. */
    @Test
    void verticalDistanceCountsOneHexPerFiveLevels() {
        ConvInfantry infantry = platoon(IMPACT, SECOND_BOARD);
        infantry.setElevation(10);

        killRoll(8);

        assertFalse(destroyed(infantry), "8 + 0 hexes + 10 levels / 5 = 10, above 9");
    }

    /** 5 hexes out, past the small bomb's radius of 2: a roll of 4 gives 9 and destroys the platoon. */
    @Test
    void killRollReachesPastTheDamageRadius() {
        ConvInfantry infantry = platoon(new Coords(8, 13), SECOND_BOARD);

        killRoll(4);

        assertTrue(destroyed(infantry), "4 + 5 = 9, destroyed on 9 or less");
    }

    /** 8 hexes out even a roll of 2 gives 10, so the platoon cannot die and does not roll. */
    @Test
    void infantryOutOfReachDoNotRoll() {
        ConvInfantry infantry = platoon(new Coords(8, 16), SECOND_BOARD);

        Vector<Report> reports = killRoll(2);

        assertEquals(0, count(reports, REPORT_KILL_ROLL), "no roll that cannot succeed");
        assertFalse(destroyed(infantry), "the platoon is untouched");
    }

    /** Infantry at the same hex on the other board are not caught. */
    @Test
    void killRollStaysOnTheBlastsBoard() {
        ConvInfantry otherBoard = platoon(IMPACT, 0);

        Vector<Report> reports = killRoll(2);

        assertEquals(0, count(reports, REPORT_KILL_ROLL), "nothing on the blast's board rolls");
        assertFalse(destroyed(otherBoard), "the platoon on the other board is untouched");
    }
}
