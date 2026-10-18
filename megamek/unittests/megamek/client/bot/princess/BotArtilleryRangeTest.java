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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Artillery ranges are rated in map sheets, and the bots must judge them in hexes the way the engine does (#9229): a
 * gun on the board fires directly at 7 to 17 hexes this turn, and indirectly out to its full rated range.
 */
class BotArtilleryRangeTest {

    private static final String ARROW_FOUR = "ISArrowIV";
    private static final String ARROW_FOUR_AMMO = "ISArrowIV Ammo";
    private static final String LONG_TOM = "ISLongTom";
    private static final String LONG_TOM_AMMO = "ISLongTom Ammo";
    private static final String MEDIUM_LASER = "ISMediumLaser";

    private Game game;
    private Princess princess;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Gunners"));
        game.addPlayer(1, new Player(1, "Targets"));
        game.getPlayer(1).setTeam(2);
        game.getPlayer(0).setTeam(1);
        // No connection is made; the constructor only builds local state.
        princess = new Princess("Battery", "localhost", 0);
    }

    @AfterEach
    void tearDown() {
        princess.die();
    }

    @Test
    @DisplayName("an Arrow IV reaches 17 hexes this turn, not its 8 map sheets read as hexes")
    void arrowFourReachesSeventeenHexes() throws Exception {
        Tank battery = createTank(0, new Coords(5, 5));
        mountArtillery(battery, ARROW_FOUR, ARROW_FOUR_AMMO);

        assertEquals(17, princess.getMaxWeaponRange(battery),
              "direct-fire artillery reaches one map sheet, 17 hexes");
    }

    @Test
    @DisplayName("a Long Tom reaches 17 hexes this turn, not its 30 map sheets read as hexes")
    void longTomReachesSeventeenHexes() throws Exception {
        Tank battery = createTank(0, new Coords(5, 5));
        mountArtillery(battery, LONG_TOM, LONG_TOM_AMMO);

        assertEquals(17, princess.getMaxWeaponRange(battery),
              "beyond 17 hexes a Long Tom can only fire indirectly, which lands on a later turn");
    }

    @Test
    @DisplayName("a unit without artillery keeps its ordinary weapon range")
    void ordinaryWeaponsAreUnchanged() throws Exception {
        Tank laserTank = createTank(0, new Coords(5, 5));
        WeaponMounted laser = (WeaponMounted) Mounted.createMounted(laserTank, EquipmentType.get(MEDIUM_LASER));
        laserTank.addEquipment(laser, Tank.LOC_FRONT, false);

        assertEquals(laserTank.getMaxWeaponRange(false), princess.getMaxWeaponRange(laserTank),
              "only artillery is read differently");
    }

    @Test
    @DisplayName("artillery damage counts only inside the 7 to 17 hex direct-fire band")
    void artilleryDamageCountsOnlyInsideTheDirectFireBand() throws Exception {
        Tank battery = createTank(0, new Coords(5, 5));
        mountArtillery(battery, ARROW_FOUR, ARROW_FOUR_AMMO);

        assertEquals(20, FireControl.getMaxDamageAtRange(battery, 12, false, false), 0.001,
              "an Arrow IV can fire directly at 12 hexes");
        assertEquals(0, FireControl.getMaxDamageAtRange(battery, 5, false, false), 0.001,
              "direct-fire artillery cannot fire at six hexes or less");
        assertEquals(0, FireControl.getMaxDamageAtRange(battery, 20, false, false), 0.001,
              "beyond 17 hexes artillery cannot fire this turn");
    }

    @Test
    @DisplayName("a gun still counts as a threat when choosing targets, though it cannot fire at 1 hex")
    void artilleryKeepsItsTargetThreatWeight() throws Exception {
        Tank battery = createTank(0, new Coords(5, 5));
        mountArtillery(battery, ARROW_FOUR, ARROW_FOUR_AMMO);

        assertEquals(20, new FireControl(princess).calcTargetPotentialDamage(battery), 0.001,
              "the threat weight measures what the target could deal, and an Arrow IV deals 20 from 7 to 17 hexes");
    }

    @Test
    @DisplayName("a Long Tom can take part in the fight from its full indirect range")
    void longTomEngagementRangeIsItsIndirectRange() throws Exception {
        Tank battery = createTank(0, new Coords(5, 5));
        mountArtillery(battery, LONG_TOM, LONG_TOM_AMMO);

        assertEquals(510, ArtilleryReach.engagementRange(game, battery, 17),
              "a Long Tom is rated at 30 map sheets, which is 510 hexes");
    }

    @Test
    @DisplayName("an artillery bay keeps its old reading, not 20 map sheets read as 340 hexes")
    void artilleryBayIsNotReadAsMapSheets() {
        WeaponMounted artilleryBay = mock(WeaponMounted.class);
        when(artilleryBay.getType()).thenReturn((WeaponType) EquipmentType.get(EquipmentTypeLookup.ARTILLERY_BAY));
        when(artilleryBay.isReady()).thenReturn(true);
        Entity dropShip = mock(Entity.class);
        when(dropShip.getWeaponList()).thenReturn(List.of(artilleryBay));

        assertFalse(ArtilleryReach.isDirectFireArtillery(artilleryBay),
              "a bay's range of 20 is not a map-sheet range; bays are left for a follow-up");
        assertEquals(17, ArtilleryReach.engagementRange(game, dropShip, 17),
              "a bay must not stretch the unit's engagement range");
    }

    @Test
    @DisplayName("a Long Tom 25 hexes from the enemy may still back away")
    void longTomOutsideDirectFireRangeMayBackAway() throws Exception {
        Tank battery = createTank(0, new Coords(5, 5));
        mountArtillery(battery, LONG_TOM, LONG_TOM_AMMO);
        Tank enemy = createTank(1, new Coords(30, 5));

        Princess mockPrincess = mockPrincess(enemy);
        BasicPathRanker ranker = spy(new BasicPathRanker(mockPrincess));
        doReturn(1.0).when(ranker).getMovePathSuccessProbability(any(MovePath.class));

        MovePath backAway = pathEndingAt(battery, new Coords(2, 5));
        MovePath closeIn = pathEndingAt(battery, new Coords(8, 5));
        List<MovePath> validPaths = ranker.validatePaths(List.of(backAway, closeIn), game, 17, 0.5);

        assertTrue(validPaths.contains(backAway),
              "at 25 hexes the Long Tom is in indirect range, so moving to 28 hexes is a legal choice");
    }

    private Princess mockPrincess(Entity enemy) {
        BehaviorSettings behaviorSettings = mock(BehaviorSettings.class);
        when(behaviorSettings.getNumberOfEnemiesToConsiderFacing()).thenReturn(1);
        IHonorUtil honorUtil = mock(IHonorUtil.class);
        when(honorUtil.isEnemyBroken(anyInt(), anyInt(), anyBoolean())).thenReturn(false);
        Princess mockPrincess = mock(Princess.class);
        when(mockPrincess.getBehaviorSettings()).thenReturn(behaviorSettings);
        when(mockPrincess.getHonorUtil()).thenReturn(honorUtil);
        when(mockPrincess.getEnemyEntities()).thenReturn(List.of(enemy));
        FireControlState fireControlState = mock(FireControlState.class);
        when(fireControlState.getAdditionalTargets()).thenReturn(new ArrayList<>());
        when(mockPrincess.getFireControlState()).thenReturn(fireControlState);
        return mockPrincess;
    }

    private MovePath pathEndingAt(Entity mover, Coords finalCoords) {
        MovePath path = mock(MovePath.class);
        when(path.isMoveLegal()).thenReturn(true);
        when(path.getEntity()).thenReturn(mover);
        when(path.getFinalCoords()).thenReturn(finalCoords);
        when(path.getSteps()).thenAnswer(invocation -> new ArrayList<MoveStep>().listIterator());
        return path;
    }

    private Tank createTank(int playerId, Coords position) {
        Tank tank = new Tank();
        tank.setGame(game);
        tank.setId(game.getNextEntityId());
        tank.setChassis("Test Tank");
        tank.setOwner(game.getPlayer(playerId));
        tank.setCrew(new Crew(CrewType.CREW));
        tank.setPosition(position);
        tank.setDeployed(true);
        game.addEntity(tank);
        return tank;
    }

    private void mountArtillery(Tank battery, String weaponName, String ammoName) throws Exception {
        WeaponMounted weapon = (WeaponMounted) Mounted.createMounted(battery, EquipmentType.get(weaponName));
        battery.addEquipment(weapon, Tank.LOC_FRONT, false);
        AmmoMounted ammo = (AmmoMounted) Mounted.createMounted(battery, EquipmentType.get(ammoName));
        battery.addEquipment(ammo, Tank.LOC_BODY, false);
        weapon.setLinked(ammo);
    }
}
