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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
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
}
