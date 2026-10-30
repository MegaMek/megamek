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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.InfantryCompartment;
import megamek.common.units.Tank;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.weapons.DamageType;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Real units in a real blast: what the blast does to them, and the heat and rolls that follow (IO:AE pp. 164-165).
 */
class NuclearBlastDamageTest {
    private static final String UNIT_FILES = "testresources/megamek/common/units/";
    private static final int BOARD_SIZE = 40;
    private static final Coords GROUND_ZERO = new Coords(20, 20);
    /** 8 hexes north of Ground Zero, where a Davy Crockett deals 100 - 8 x 5 = 60. */
    private static final Coords BUILDING_HEX = new Coords(20, 12);
    private static final int BUILDING_CF = 80;

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
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());
        game = gameManager.getGame();
        game.setOptions(new GameOptions());
        server = ServerFactory.createServer(gameManager);
        player = new Player(0, "Test");
        game.addPlayer(0, player);

        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex(0);
        }
        Hex buildingHex = hexes[(BUILDING_HEX.getY() * BOARD_SIZE) + BUILDING_HEX.getX()];
        buildingHex.addTerrain(new Terrain(Terrains.BUILDING, 2));
        buildingHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 2));
        buildingHex.addTerrain(new Terrain(Terrains.BLDG_CF, BUILDING_CF));
        Board board = new Board();
        board.newData(BOARD_SIZE, BOARD_SIZE, hexes, null);
        game.setBoard(board);
    }

    @AfterEach
    void afterEach() {
        server.die();
    }

    private Entity placeUnit(String fileName, Coords position) throws Exception {
        Entity entity = new MekFileParser(new File(UNIT_FILES + fileName)).getEntity();
        entity.setId(game.getNextEntityId());
        game.addEntity(entity);
        entity.setOwner(player);
        entity.setPosition(position);
        entity.setDeployed(true);
        return entity;
    }

    /** A hex straight north of Ground Zero, so its distance is exactly {@code distance}. */
    private static Coords hexNorth(int distance) {
        return new Coords(GROUND_ZERO.getX(), GROUND_ZERO.getY() - distance);
    }

    private Vector<Integer> blast(NuclearWeaponType weaponType) {
        Vector<Integer> blastedUnits = new Vector<>();
        new NuclearBlastDamage(gameManager).apply(GROUND_ZERO, NuclearBlastProfile.groundBurst(weaponType),
              new Vector<Report>(), blastedUnits);
        return blastedUnits;
    }

    @Test
    void atlasAtGroundZeroIsAnnihilatedAndCannotBeSalvaged() throws Exception {
        // An Atlas's best location holds 47 armor, far below a Davy Crockett's 100 (IO:AE p.164)
        Entity atlas = placeUnit("Atlas AS7-D.mtf", GROUND_ZERO);

        new NuclearDetonationResolver(gameManager).resolve(GROUND_ZERO,
              NuclearBlastProfile.groundBurst(NuclearWeaponType.DAVY_CROCKETT_M), new Vector<Report>());

        assertTrue(atlas.isDoomed() || atlas.isDestroyed(), "annihilated at Ground Zero");
        assertFalse(atlas.isSalvage(), "nothing a nuke destroys can be salvaged (IO:AE p.168)");
    }

    @Test
    void troopsKilledWithTheirCarrierCannotBeSalvaged() throws Exception {
        // The Elementals die with the Bulldog and go straight to the graveyard; they are still unsalvageable
        // (IO:AE p.168)
        Entity bulldog = placeUnit("Bulldog Medium Tank.blk", GROUND_ZERO);
        bulldog.addTransporter(new InfantryCompartment(10.0));
        Entity elementals = placeUnit("Elemental BA [Laser] (Sqd5).blk", GROUND_ZERO);
        bulldog.load(elementals, false);
        elementals.setTransportId(bulldog.getId());

        new NuclearDetonationResolver(gameManager).resolve(GROUND_ZERO,
              NuclearBlastProfile.groundBurst(NuclearWeaponType.DAVY_CROCKETT_M), new Vector<Report>());

        assertTrue(elementals.isDestroyed(), "killed with the carrier");
        assertNull(game.getEntity(elementals.getId()), "already in the graveyard");
        assertFalse(elementals.isSalvage());
        assertEquals(IEntityRemovalConditions.REMOVE_DEVASTATED, elementals.getRemovalCondition());
    }

    @Test
    void bestLocationArmorIsTheStrongestSingleLocation() throws Exception {
        Entity atlas = placeUnit("Atlas AS7-D.mtf", hexNorth(30));

        assertEquals(47, NuclearBlastDamage.bestLocationArmor(atlas), "the Atlas's center torso front");
    }

    @Test
    void survivorGainsHeatAndRollsAtPlusTwo() throws Exception {
        // 17 hexes from a Davy Crockett: 100 - 17 x 5 = 15 damage, so 1 heat (IO:AE p.165)
        Entity atlas = placeUnit("Atlas AS7-D.mtf", hexNorth(17));
        int heatBefore = atlas.heatBuildup;

        Vector<Integer> blastedUnits = blast(NuclearWeaponType.DAVY_CROCKETT_M);

        assertTrue(blastedUnits.contains(atlas.getId()));
        assertEquals(heatBefore + 1, atlas.heatBuildup, "15 damage is 1 heat");
        int blastRolls = 0;
        for (Enumeration<PilotingRollData> rolls = game.getPSRs(); rolls.hasMoreElements(); ) {
            PilotingRollData roll = rolls.nextElement();
            if (roll.getEntityId() == atlas.getId()) {
                assertEquals(NuclearBlastDamage.BLAST_ROLL_MODIFIER, roll.getValue(), "+2, not the old +6");
                blastRolls++;
            }
        }
        assertEquals(1, blastRolls, "one piloting roll to stay standing");
    }

    @Test
    void blastHeatIsCappedAndOnlyForUnitsThatTrackHeat() throws Exception {
        Entity atlas = placeUnit("Atlas AS7-D.mtf", hexNorth(30));
        Entity bulldog = placeUnit("Bulldog Medium Tank.blk", hexNorth(31));

        assertEquals(8, NuclearBlastDamage.blastHeat(atlas, 85), "85 damage is 8 heat");
        assertEquals(15, NuclearBlastDamage.blastHeat(atlas, 400), "never more than 15");
        assertEquals(0, NuclearBlastDamage.blastHeat(bulldog, 400), "a tank does not track heat");
    }

    @Test
    void damageStopsOnceTheUnitIsDestroyed() throws Exception {
        // 10 hexes from a Santa Ana is 10,000 - 10 x 109 = 8,910 damage: 1,782 clusters, far past what kills an Atlas
        Entity atlas = placeUnit("Atlas AS7-D.mtf", hexNorth(10));
        boolean[] hitAfterDestroyed = { false };
        int[] depth = { 0 };
        Mockito.doAnswer(invocation -> {
            // Only count the blast's own clusters, not damage the game passes on inside one of them
            if ((depth[0] == 0) && (atlas.isDoomed() || atlas.isDestroyed())) {
                hitAfterDestroyed[0] = true;
            }
            depth[0]++;
            try {
                return invocation.callRealMethod();
            } finally {
                depth[0]--;
            }
        }).when(gameManager).damageEntity(eq(atlas), any(HitData.class), anyInt(), anyBoolean(),
              any(DamageType.class), anyBoolean(), anyBoolean());

        blast(NuclearWeaponType.SANTA_ANA);

        assertTrue(atlas.isDoomed() || atlas.isDestroyed(), "8,910 damage destroys an Atlas");
        assertFalse(hitAfterDestroyed[0], "no cluster lands on the wreck");
    }

    @Test
    void unitInsideAStrongBuildingIsShieldedByIt() throws Exception {
        // The CF 80 building takes the 60 first, leaving nothing for the Atlas inside (IO:AE p.165)
        Entity atlas = placeUnit("Atlas AS7-D.mtf", BUILDING_HEX);
        int armorBefore = atlas.getTotalArmor();

        Vector<Integer> blastedUnits = blast(NuclearWeaponType.DAVY_CROCKETT_M);

        assertEquals(armorBefore, atlas.getTotalArmor(), "the building absorbed the whole blast");
        assertFalse(blastedUnits.contains(atlas.getId()), "a unit that took nothing does not roll");
        assertEquals(BUILDING_CF - 60, game.getBoard().getBuildingAt(BUILDING_HEX).getCurrentCF(BUILDING_HEX),
              "the building took the 60");
    }

    @Test
    void airborneVtolIsStruckByTheBlast() throws Exception {
        // VTOLs used to be skipped; airborne units on the ground map take the blast by ground distance (IO:AE p.165)
        Mockito.doReturn(new Vector<Report>()).when(gameManager).crashVTOLorWiGE(any(Tank.class));
        Mockito.doReturn(new Vector<Report>()).when(gameManager).vehicleMotiveDamage(any(Tank.class), anyInt());
        Entity cobra = placeUnit("Cobra Transport VTOL.blk", hexNorth(16));
        cobra.setElevation(3);
        int armorBefore = cobra.getTotalArmor();

        Vector<Integer> blastedUnits = blast(NuclearWeaponType.DAVY_CROCKETT_M);

        assertTrue(blastedUnits.contains(cobra.getId()), "the airborne VTOL is in the blast");
        assertTrue(cobra.getTotalArmor() < armorBefore, "and takes its 20 damage");
    }

    @Test
    void airborneVtolThatFailsItsRollCrashes() throws Exception {
        // A failed roll knows nothing lower than the ground for a VTOL: it crashes rather than taking motive damage
        Mockito.doReturn(new Vector<Report>()).when(gameManager).crashVTOLorWiGE(any(Tank.class));
        // 19 hexes is 5 damage, too little to destroy the VTOL before it rolls
        Entity cobra = placeUnit("Cobra Transport VTOL.blk", hexNorth(19));
        cobra.setElevation(3);
        // Piloting 12 plus the blast's +2 needs 14 on 2d6, so the roll always fails
        cobra.getCrew().setPiloting(12, 0);

        blast(NuclearWeaponType.DAVY_CROCKETT_M);

        Mockito.verify(gameManager).crashVTOLorWiGE((Tank) cobra);
    }

    private int[] damages(NuclearWeaponType weaponType) {
        return NuclearBlastProfile.groundBurst(weaponType).blastDamageByDistance();
    }

    private Coords protectingHex(Coords unitHex, int height, NuclearWeaponType weaponType) {
        return new NuclearBlastDamage(gameManager).protectingHex(unitHex, height, GROUND_ZERO, damages(weaponType));
    }

    @Test
    void hillTallerThanTheUnitProtectsIt() {
        // A Mek (2 levels tall, top at level 1) behind a level 3 hill 9 hexes from a Davy Crockett, which deals 55
        // there: less than the hill's Terrain Factor of 200 (IO:AE p.165; HammerGS, 2026-10-28)
        game.getBoard().getHex(hexNorth(9)).setLevel(3);

        assertEquals(hexNorth(9), protectingHex(hexNorth(10), 1, NuclearWeaponType.DAVY_CROCKETT_M));
    }

    @Test
    void hillMustBeTallerThanTheUnit() {
        // A level 1 hill is not taller than a Mek standing at level 0, but it is taller than a tank
        game.getBoard().getHex(hexNorth(9)).setLevel(1);

        assertNull(protectingHex(hexNorth(10), 1, NuclearWeaponType.DAVY_CROCKETT_M), "a Mek is as tall as the hill");
        assertEquals(hexNorth(9), protectingHex(hexNorth(10), 0, NuclearWeaponType.DAVY_CROCKETT_M),
              "a tank is shorter");
    }

    @Test
    void hillThatTakesTwoHundredDoesNotProtect() {
        // An Alamo deals 1,000 - 9 x 23 = 793 at the hill, far above its Terrain Factor of 200
        game.getBoard().getHex(hexNorth(9)).setLevel(3);

        assertNull(protectingHex(hexNorth(10), 1, NuclearWeaponType.ALAMO));
    }

    @Test
    void hillOnTheFarSideDoesNotProtect() {
        // The hill must stand between the unit and Ground Zero
        game.getBoard().getHex(hexNorth(11)).setLevel(3);

        assertNull(protectingHex(hexNorth(10), 1, NuclearWeaponType.DAVY_CROCKETT_M));
    }

    @Test
    void standingBuildingProtectsButACollapsedOneDoesNot() {
        // The CF 80, 2-level building 8 hexes north rises above a Mek standing right behind it
        assertEquals(BUILDING_HEX, protectingHex(hexNorth(9), 1, NuclearWeaponType.DAVY_CROCKETT_M));

        game.getBoard().getBuildingAt(BUILDING_HEX).setCurrentCF(0, BUILDING_HEX);

        assertNull(protectingHex(hexNorth(9), 1, NuclearWeaponType.DAVY_CROCKETT_M), "a fallen building is no cover");
    }

    @Test
    void dryGroundBesideASubmergedUnitIsCover() {
        // Normal line of sight: a hex next to the unit blocks when it is higher than the unit's top (LosEffects). A Mek
        // in depth 2 water has its top at level -1, so the level 0 bank beside it counts (HammerGS, 2026-10-29)
        Hex pool = game.getBoard().getHex(hexNorth(10));
        pool.addTerrain(new Terrain(Terrains.WATER, 2));

        assertEquals(hexNorth(9), protectingHex(hexNorth(10), -1, NuclearWeaponType.DAVY_CROCKETT_M));
    }

    private Vector<Integer> blastWithRoll(NuclearWeaponType weaponType, int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        when(roll.getReport()).thenReturn(String.valueOf(value));
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.rollD6(2)).thenReturn(roll);
            return blast(weaponType);
        }
    }

    @Test
    void unitThatMakesItsHidingRollTakesNoBlastDamage() throws Exception {
        game.getBoard().getHex(hexNorth(9)).setLevel(3);
        Entity atlas = placeUnit("Atlas AS7-D.mtf", hexNorth(10));
        int armorBefore = atlas.getTotalArmor();

        Vector<Integer> blastedUnits = blastWithRoll(NuclearWeaponType.DAVY_CROCKETT_M, 12);

        assertFalse(blastedUnits.contains(atlas.getId()), "protected units are not struck");
        assertEquals(armorBefore, atlas.getTotalArmor(), "no blast damage");
    }

    @Test
    void unitThatFailsItsHidingRollTakesTheBlast() throws Exception {
        game.getBoard().getHex(hexNorth(9)).setLevel(3);
        Entity atlas = placeUnit("Atlas AS7-D.mtf", hexNorth(10));
        int armorBefore = atlas.getTotalArmor();

        Vector<Integer> blastedUnits = blastWithRoll(NuclearWeaponType.DAVY_CROCKETT_M, 3);

        assertTrue(blastedUnits.contains(atlas.getId()), "a failed roll leaves the unit in the blast");
        assertTrue(atlas.getTotalArmor() < armorBefore, "and it takes the damage");
        assertFalse(atlas.isProne(), "the hiding roll itself never knocks it down");
    }
}
