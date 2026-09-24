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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import megamek.common.Hex;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Why a bot fighter over a ground mapsheet never fires, traced on a real airframe.
 *
 * <p>An earlier version of this probe built its fighter by hand and reached a wrong conclusion: the plan was
 * being discarded by the heat logic, which looked like a defect until the fixture turned out to have no heat
 * sinks and therefore a heat capacity of zero. A real unit carries real heat sinks, so nothing here rests on
 * that any more.</p>
 */
class AeroWeaponFireInfoProbeTest {

    /** A heavy fighter with a full weapon fit, including aft-mounted guns, and heat sinks to fire it. */
    private static final String HEAVY_FIGHTER = "Stuka STU-D6";

    /** A light fighter whose guns are all nose and wing - nothing bears behind it. */
    private static final String LIGHT_FIGHTER = "Cheetah F-11";

    private static final int BOARD_SIZE = 60;

    private static Board board(BoardType boardType) {
        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(BOARD_SIZE, BOARD_SIZE, hexes);
        board.setBoardType(boardType);
        return board;
    }

    private static Entity loadFighter(String name) {
        MekSummary summary = MekSummaryCache.getInstance().getMek(name);
        if (summary == null) {
            return null;
        }
        try {
            return new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
        } catch (Exception exception) {
            return null;
        }
    }

    private static Entity placeFighter(Game game, int id, Player owner, Coords position, int altitude,
          int facing) {
        return placeFighter(game, id, owner, position, altitude, facing, HEAVY_FIGHTER);
    }

    private static Entity placeFighter(Game game, int id, Player owner, Coords position, int altitude,
          int facing, String name) {
        Entity fighter = loadFighter(name);
        if (fighter == null) {
            return null;
        }
        fighter.setGame(game);
        fighter.setId(id);
        fighter.setOwner(owner);
        fighter.setPosition(position);
        fighter.setAltitude(altitude);
        fighter.setFacing(facing);
        fighter.setSecondaryFacing(facing);
        fighter.setDeployed(true);
        game.addEntity(fighter, false);
        return fighter;
    }

    private static Game gameOn(BoardType boardType) {
        Game game = new Game();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.setBoard(board(boardType));
        Player us = new Player(1, "Us");
        us.setTeam(1);
        Player them = new Player(2, "Them");
        them.setTeam(2);
        game.addPlayer(1, us);
        game.addPlayer(2, them);
        return game;
    }

    private static Princess princessFor(Game game, Entity target) {
        Princess princess = mock(Princess.class);
        when(princess.getGame()).thenReturn(game);
        when(princess.getBehaviorSettings()).thenReturn(new BehaviorSettings());
        when(princess.getFireControlState()).thenReturn(new FireControlState());
        when(princess.getPriorityUnitTargets()).thenReturn(new HashSet<>());
        when(princess.getEnemyEntities()).thenReturn(List.of(target));
        when(princess.getMaxWeaponRange(any(Entity.class), any(Boolean.class))).thenCallRealMethod();
        Precognition precognition = mock(Precognition.class);
        when(precognition.getECMInfo()).thenReturn(new ArrayList<>());
        when(princess.getPrecognition()).thenReturn(precognition);
        when(princess.getHonorUtil()).thenReturn(new HonorUtil());
        // Unstubbed, Mockito hands back null and the ranker switches on it: calculateAerospaceMod reads
        // the standing focus order to weight its two credit sets. A live Princess starts at AUTO.
        when(princess.getAerospaceFocus()).thenReturn(AerospaceFocus.AUTO);
        return princess;
    }

    private static Map<WeaponMounted, Double> noAmmoConservation(Entity shooter) {
        Map<WeaponMounted, Double> ammoConservation = new HashMap<>();
        for (WeaponMounted weapon : shooter.getWeaponList()) {
            ammoConservation.put(weapon, 0.0);
        }
        return ammoConservation;
    }

    /**
     * With a real airframe, a fighter that ends its move pointing at an opponent at the same altitude over a
     * ground map builds a firing plan and expects to do damage with it.
     */
    @Test
    void aRealHeavyFighterBuildsAPlanAgainstAMatchedAltitudeTarget() {
        Game game = gameOn(BoardType.GROUND);
        Entity shooter = placeFighter(game, 1, game.getPlayer(1), new Coords(10, 10), 5, 3);
        assumeTrue(shooter != null, HEAVY_FIGHTER + " is not in the unit cache");
        Entity target = placeFighter(game, 2, game.getPlayer(2), new Coords(10, 20), 5, 0);
        assumeTrue(target != null);

        assertTrue(shooter.getHeatCapacity() > 0,
              "a real airframe must have heat sinks, or this probe is measuring its own fixture");

        FireControl fireControl = new FireControl(princessFor(game, target));
        FiringPlan plan = fireControl.getBestFiringPlan(shooter, target, game, noAmmoConservation(shooter));

        assertTrue(plan.size() > 0, "expected at least one shot, got none");
        assertTrue(plan.getExpectedDamage() > 0, "expected non-zero damage, got " + plan.getExpectedDamage());
    }

    /**
     * A heavy airframe carries guns in the aft arc, so it can still shoot at something behind it.
     */
    @Test
    void anAirframeWithAftWeaponsCanStillShootBehindIt() {
        Game game = gameOn(BoardType.GROUND);
        Entity shooter = placeFighter(game, 1, game.getPlayer(1), new Coords(10, 10), 5, 0, HEAVY_FIGHTER);
        assumeTrue(shooter != null, HEAVY_FIGHTER + " is not in the unit cache");
        Entity target = placeFighter(game, 2, game.getPlayer(2), new Coords(10, 20), 5, 0, HEAVY_FIGHTER);
        assumeTrue(target != null);

        FireControl fireControl = new FireControl(princessFor(game, target));
        FiringPlan plan = fireControl.getBestFiringPlan(shooter, target, game, noAmmoConservation(shooter));

        assertTrue(plan.size() > 0, "a Stuka has aft guns and should still have a shot");
    }

    /**
     * A light fighter with nothing in the aft arc has no attack at all against something behind it,
     * however close and however level the opponent is.
     *
     * <p>This is why bot Cheetahs over a ground map produced 756 firing turns and no plans. Aerospace arcs
     * are four discrete wedges, every weapon that does not bear is dropped from the plan, and a fighter
     * covering 48 to 80 hexes a turn that may only change facing every 8 to 52 of them (TW p.92) hardly
     * ever finishes its move pointing at anybody. Nothing in path ranking tries to arrange otherwise, so
     * whether there is a shot at all is decided by where the flight path happened to end.</p>
     */
    @Test
    void anAirframeWithoutAftWeaponsCannotShootBehindIt() {
        Game game = gameOn(BoardType.GROUND);
        Entity shooter = placeFighter(game, 1, game.getPlayer(1), new Coords(10, 10), 5, 0, LIGHT_FIGHTER);
        assumeTrue(shooter != null, LIGHT_FIGHTER + " is not in the unit cache");
        Entity target = placeFighter(game, 2, game.getPlayer(2), new Coords(10, 20), 5, 0, LIGHT_FIGHTER);
        assumeTrue(target != null);

        FireControl fireControl = new FireControl(princessFor(game, target));
        FiringPlan plan = fireControl.getBestFiringPlan(shooter, target, game, noAmmoConservation(shooter));

        assertEquals(0, plan.size(), "a Cheetah has nothing that bears behind it");
    }

    /**
     * The velocity penalty prices excess speed from the damage the fighter could deliver up close. If this
     * is zero for a real airframe, the whole term silently disappears - which is exactly what the live TSV
     * showed (35,187 ranked paths, every aeroVelocityPenalty 0.00), so pin it here.
     */
    @Test
    void closeRangeDamageIsNonZeroForRealAirframes() {
        Game game = gameOn(BoardType.GROUND);
        Entity cheetah = placeFighter(game, 1, game.getPlayer(1), new Coords(10, 10), 5, 3, LIGHT_FIGHTER);
        assumeTrue(cheetah != null, LIGHT_FIGHTER + " is not in the unit cache");

        for (int range : new int[] { 1, 2, 3, 5, 10 }) {
            System.out.printf("Cheetah maxDamageAtRange(%d) = %.1f%n", range,
                  FireControl.getMaxDamageAtRange(cheetah, range, false, false));
        }
        for (WeaponMounted weapon : cheetah.getWeaponList()) {
            System.out.printf("  weapon %s crippled=%s ranges=%s%n", weapon.getName(), weapon.isCrippled(),
                  java.util.Arrays.toString(weapon.getType().getRanges(weapon)));
        }

        assertTrue(FireControl.getMaxDamageAtRange(cheetah, 1, false, false) > 0,
              "a Cheetah at range 1 must have nonzero max damage or the velocity penalty is dead");
    }

    /**
     * Reproduces the live calculateAerospaceMod call end to end, because the batch TSV shows the velocity
     * penalty at 0.00 on rows where every input the term needs is verifiably present.
     */
    @Test
    void velocityPenaltyFiresOnALiveStyleRanking() {
        Game game = gameOn(BoardType.GROUND);
        Entity mover = placeFighter(game, 1, game.getPlayer(1), new Coords(10, 10), 5, 3, LIGHT_FIGHTER);
        assumeTrue(mover != null, LIGHT_FIGHTER + " is not in the unit cache");
        Entity enemy = placeFighter(game, 2, game.getPlayer(2), new Coords(10, 40), 5, 0, LIGHT_FIGHTER);
        assumeTrue(enemy != null);
        ((megamek.common.units.IAero) mover).setCurrentVelocity(3);

        Princess princess = princessFor(game, enemy);
        when(princess.getEntitiesOwned()).thenReturn(java.util.List.of(mover));
        AerospacePathRanker ranker = new AerospacePathRanker(princess);

        megamek.common.moves.MovePath path = new megamek.common.moves.MovePath(game, mover, null);
        System.out.println("finalVelocity=" + path.getFinalVelocity()
              + " finalAltitude=" + path.getFinalAltitude()
              + " venue=" + AerospaceVenue.of(game, mover)
              + " isAirborneAeroOnGroundMap=" + mover.isAirborneAeroOnGroundMap());

        double mod = ranker.calculateAerospaceMod(path, game, java.util.List.of(enemy));
        java.util.Map<String, Double> scores = ranker.doctrineScores();
        System.out.println("aerospaceMod=" + mod);
        for (String key : new String[] { "aeroAirEnemies", "aeroEngageableEnemies", "aeroVelocityPenalty",
                                         "aeroEngagementCredit" }) {
            System.out.println(key + "=" + scores.get(key));
        }

        assertTrue(scores.get("aeroVelocityPenalty") > 0,
              "velocity 3 with enemy air on a ground map must be charged; the live TSV says it is not");
    }

    /** Low altitude behaves the same way, which is the point: arc is not a ground-map-specific problem. */
    @Test
    void lowAltitudeBehavesTheSame() {
        Game game = gameOn(BoardType.SKY);
        Entity shooter = placeFighter(game, 1, game.getPlayer(1), new Coords(10, 10), 5, 3);
        assumeTrue(shooter != null, HEAVY_FIGHTER + " is not in the unit cache");
        Entity target = placeFighter(game, 2, game.getPlayer(2), new Coords(10, 14), 5, 0);
        assumeTrue(target != null);

        FireControl fireControl = new FireControl(princessFor(game, target));
        FiringPlan plan = fireControl.getBestFiringPlan(shooter, target, game, noAmmoConservation(shooter));

        assertTrue(plan.size() > 0, "expected at least one shot at low altitude, got none");
    }
}
