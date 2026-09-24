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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.equipment.WeaponMounted;
import org.mockito.Mockito;
import java.util.List;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.BombMounted;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Covers the two ways the stock guess disagrees with the server about an air-to-air shot: it never checks the
 * dead zone, and it measures range with integer division and no altitude term.
 */
class AerospaceFireControlTest {

    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 80;

    private record Setup(Game game, AerospaceFireControl fireControl) {
    }

    private static Setup setup(BoardType boardType) {
        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes);
        board.setBoardType(boardType);
        Game game = new Game();
        game.setBoard(board);
        return new Setup(game, new AerospaceFireControl(mock(Princess.class)));
    }

    private static Entity fighter(Game game, int id, Coords position, int altitude) {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setId(id);
        fighter.setGame(game);
        fighter.setPosition(position);
        fighter.setAltitude(altitude);
        fighter.setDeployed(true);
        game.addEntity(fighter);
        return fighter;
    }

    private static Entity groundMek(Game game, int id, Coords position) {
        BipedMek mek = new BipedMek();
        mek.setId(id);
        mek.setGame(game);
        mek.setPosition(position);
        mek.setDeployed(true);
        game.addEntity(mek);
        return mek;
    }

    // --- range ---------------------------------------------------------------------------------------

    /** TW p.241's worked example: ten hexes apart at altitudes 3 and 5 is an effective twelve. */
    @Test
    void lowAltitudeRangeAddsTheAltitudeDifference() {
        Setup setup = setup(BoardType.SKY);
        Entity shooter = fighter(setup.game(), 1, new Coords(0, 0), 5);
        Entity target = fighter(setup.game(), 2, new Coords(0, 10), 3);

        int distance = setup.fireControl().guessDistance(shooter, new EntityState(shooter), target,
              new EntityState(target), setup.game());

        assertEquals(12, distance);
    }

    /**
     * Over a ground map the conversion rounds up and then adds altitude. The stock guess uses integer
     * division and adds nothing, so it would answer 2 here and believe the target two brackets closer than
     * the server does.
     */
    @Test
    void groundMapRangeRoundsUpThenAddsAltitude() {
        Setup setup = setup(BoardType.GROUND);
        Entity shooter = fighter(setup.game(), 1, new Coords(0, 0), 5);
        Entity target = fighter(setup.game(), 2, new Coords(0, 33), 3);

        int distance = setup.fireControl().guessDistance(shooter, new EntityState(shooter), target,
              new EntityState(target), setup.game());

        // 33 ground hexes rounds up to 3 low-altitude hexes, plus 2 levels of altitude.
        assertEquals(5, distance);
    }

    @Test
    void groundToAirStillUsesTheStockCalculation() {
        Setup setup = setup(BoardType.GROUND);
        Entity shooter = groundMek(setup.game(), 1, new Coords(0, 0));
        Entity target = fighter(setup.game(), 2, new Coords(0, 4), 3);

        int distance = setup.fireControl().guessDistance(shooter, new EntityState(shooter), target,
              new EntityState(target), setup.game());

        // Stock rule: raw hex distance plus two per altitude of the target.
        assertEquals(4 + (2 * 3), distance);
    }

    // --- the dead zone -------------------------------------------------------------------------------

    /**
     * The stock guess plans this shot, the server refuses it, and the bot has already moved on the strength
     * of damage it was never going to do.
     */
    @Test
    void aShotIntoTheDeadZoneIsRefusedOutright() {
        Setup setup = setup(BoardType.GROUND);
        Entity shooter = fighter(setup.game(), 1, new Coords(0, 0), 5);
        Entity target = fighter(setup.game(), 2, new Coords(0, 10), 3);

        ToHitData toHit = setup.fireControl().guessToHitModifierForWeapon(shooter, new EntityState(shooter),
              target, new EntityState(target), mock(WeaponMounted.class), null, setup.game());

        assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue());
        assertTrue(toHit.getDesc().toLowerCase().contains("dead zone"));
    }

    @Test
    void aShotFromMatchedAltitudeIsNotRefusedByGeometry() {
        Setup setup = setup(BoardType.GROUND);
        Entity shooter = fighter(setup.game(), 1, new Coords(0, 0), 5);
        Entity target = fighter(setup.game(), 2, new Coords(0, 10), 5);

        ToHitData toHit = setup.fireControl().guessToHitModifierForWeapon(shooter, new EntityState(shooter),
              target, new EntityState(target), mock(WeaponMounted.class), null, setup.game());

        // It may still be refused for other reasons - no weapon, no arc - but not for the dead zone.
        assertTrue(!toHit.getDesc().toLowerCase().contains("dead zone"),
              "matched altitude clears the cone: " + toHit.getDesc());
    }

    /**
     * The salvo is sized to the victim AND chosen by type (Dave): ten HE on a Locust is dumb, ten on
     * an Atlas is smart, and a mixed rack spends its heaviest ordnance on the hard target while the
     * small stuff stays racked. Zero-damage ordnance never releases as generic tonnage.
     */
    @Test
    void salvoIsSizedToTheVictimAndChosenByType() {
        BombLoadout tenHE = new BombLoadout();
        tenHE.addBombs(BombType.BombTypeEnum.HE, 10);
        assertEquals(7, AerospaceFireControl.rationSelection(tenHE, 0.77, 50).getTotalBombs(),
              "a light mek asks for a fraction of the rack");
        assertEquals(10, AerospaceFireControl.rationSelection(tenHE, 0.77, 300).getTotalBombs(),
              "an assault mek honestly asks for the full rack");
        assertEquals(1, AerospaceFireControl.rationSelection(tenHE, 0.95, 1).getTotalBombs(),
              "never fewer than one damaging bomb");

        BombLoadout mixed = new BombLoadout();
        mixed.addBombs(BombType.BombTypeEnum.HE, 5);
        mixed.addBombs(BombType.BombTypeEnum.CLUSTER, 5);
        BombLoadout pick = AerospaceFireControl.rationSelection(mixed, 0.8, 30);
        assertEquals(4, pick.getCount(BombType.BombTypeEnum.HE),
              "the heavy ordnance funds the kill first");
        assertEquals(0, pick.getCount(BombType.BombTypeEnum.CLUSTER),
              "the cluster bombs stay racked for softer work");

        BombLoadout withTag = new BombLoadout();
        withTag.addBombs(BombType.BombTypeEnum.TAG, 2);
        withTag.addBombs(BombType.BombTypeEnum.HE, 2);
        assertEquals(0, AerospaceFireControl.rationSelection(withTag, 0.8, 100)
                    .getCount(BombType.BombTypeEnum.TAG),
              "zero-damage ordnance is never released as generic tonnage");
    }

    /**
     * The seam-drop bypass, pinned: Princess builds one candidate plan per enemy, so every enemy
     * not standing on the best footprint hex used to produce an unrationed full-load twin aimed
     * there, and the auction always picked the twin - the live game showed straight alpha dumps.
     * Seam drops now ration against the summed hit points of everything under the blast rings:
     * cluster rings reach both meks flanking the seam, HE reaches only the hex it lands on.
     */
    @Test
    void aSeamDropIsFundedByEveryTargetUnderTheFootprint() {
        Setup setup = setup(BoardType.GROUND);
        Coords seam = new Coords(21, 20);
        // Two meks flanking the seam hex, a third well outside any blast ring.
        Entity left = groundMek(setup.game(), 10, new Coords(20, 20));
        Entity right = groundMek(setup.game(), 11, new Coords(22, 20));
        Entity distant = groundMek(setup.game(), 12, new Coords(30, 30));
        List<Entity> enemies = List.of(left, right, distant);
        left.initializeInternal(10, megamek.common.units.Mek.LOC_CENTER_TORSO);
        right.initializeInternal(6, megamek.common.units.Mek.LOC_CENTER_TORSO);
        int leftHitPoints = left.getTotalArmor() + left.getTotalInternal();
        int rightHitPoints = right.getTotalArmor() + right.getTotalInternal();
        assertTrue((leftHitPoints > 0) && (leftHitPoints != rightHitPoints),
              "fixture meks need distinct nonzero hit points or the sums prove nothing");

        BombMounted cluster =
              mock(BombMounted.class);
        BombType clusterType =
              mock(BombType.class);
        when(clusterType.getBombType())
              .thenReturn(BombType.BombTypeEnum.CLUSTER);
        when(cluster.getType()).thenReturn(clusterType);

        assertEquals(leftHitPoints + rightHitPoints, AerospaceFireControl.footprintHitPoints(
                    List.of(cluster), seam, enemies),
              "cluster rings reach both flanking meks and nothing else");

        BombMounted highExplosive =
              mock(BombMounted.class);
        BombType heType =
              mock(BombType.class);
        when(heType.getBombType())
              .thenReturn(BombType.BombTypeEnum.HE);
        when(heType.getDamagePerShot()).thenReturn(10);
        when(highExplosive.getType()).thenReturn(heType);

        assertEquals(0, AerospaceFireControl.footprintHitPoints(
                    List.of(highExplosive), seam, enemies),
              "HE on an empty seam hex funds nothing - the drop stays minimal");
        assertEquals(leftHitPoints, AerospaceFireControl.footprintHitPoints(
                    List.of(highExplosive), left.getPosition(), enemies),
              "HE on an occupied hex funds exactly that one victim");
    }

    /**
     * The value-of-the-target question (Dave): a Locust, a Rifleman, a Centurion and an Atlas on
     * the board - rationing must not degrade into "10, 5, 3, 1". The stock auction scores raw
     * expected damage, so the ten-bomb Atlas dent always outbid the four-bomb Locust kill. With the
     * opportunity-cost adjustment the kill wins: overkill damage is refunded, the kill fraction
     * squared times battle value credits removing a unit from the fight, and each released bomb
     * charges its future use against the targets still standing.
     */
    @Test
    void aLocustKillOutbidsAnAtlasDent() {
        // Both plans carry the pre-ration estimate of ten HE at 0.77: 77 expected damage.
        // Locust: 30 effective HP, BV 432, ration releases 4. Atlas: 307 HP, BV 1897, releases 10.
        double locustUtility = 77.0 + AerospaceFireControl.bombPlanUtilityAdjustment(
              77.0, 77.0, 4, 30, 432, true);
        double atlasUtility = 77.0 + AerospaceFireControl.bombPlanUtilityAdjustment(
              77.0, 77.0, 10, 307, 1897, true);

        assertTrue(locustUtility > atlasUtility,
              "the four-bomb Locust kill (%.1f) must outbid the ten-bomb Atlas dent (%.1f)"
                    .formatted(locustUtility, atlasUtility));
        assertTrue(locustUtility < 77.0,
              "the Locust plan must not keep phantom damage past the victim's hit points");
    }

    /** When the Atlas is the last bombable enemy standing, the rack is spent freely - no charge. */
    @Test
    void theLastBombableTargetIsBombedWithoutCharge() {
        double withOthers = AerospaceFireControl.bombPlanUtilityAdjustment(77.0, 77.0, 10, 307, 1897, true);
        double lastTarget = AerospaceFireControl.bombPlanUtilityAdjustment(77.0, 77.0, 10, 307, 1897, false);

        assertEquals(AerospaceFireControl.BOMB_OPPORTUNITY_COST_PER_BOMB * 10,
              lastTarget - withOthers, 0.001,
              "the only difference between mid-battle and last-target is the per-bomb charge");
        assertTrue(lastTarget > 0, "a dent in the last target still earns its kill-fraction credit");
    }

    /**
     * The live-game regression of 2026-08-14 (20:09 game), pinned: the stock code scores hex-aimed
     * bomb plans at ZERO expected damage, and the first auction build trusted it - zero kill
     * credit, full opportunity cost, every bomb plan at -50 to -85, and the Huscarl died at round
     * 24 with fifteen bombs still racked. The payload's own worth (17 bombs, ~150 damage at real
     * odds, a 195-HP / 1242-BV footprint) must carry the bid: strongly positive, and higher than
     * an honest smaller drop.
     */
    @Test
    void aSeamPlanTheStockCodeScoresAtZeroStillBidsItsPayload() {
        double seventeenBombSeam = AerospaceFireControl.bombPlanUtilityAdjustment(
              0.0, 153.0, 17, 195, 1242, true);
        double smallerDrop = AerospaceFireControl.bombPlanUtilityAdjustment(
              0.0, 90.0, 10, 195, 1242, true);

        assertTrue(seventeenBombSeam > 100,
              "a base-zero seam plan must bid its payload's worth, not sit at pure penalty: "
                    + seventeenBombSeam);
        assertTrue(seventeenBombSeam > smallerDrop,
              "more delivered damage on the same footprint must outbid less");
    }
}
