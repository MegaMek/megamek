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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;

import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.WeaponTypeFlag;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.moves.MovePath;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.server.SmokeCloud;
import org.junit.jupiter.api.Test;

/**
 * The bot's estimate of a firing-phase artillery shot must match what the engine will actually roll, because that
 * estimate is how a bot decides how dangerous an enemy gun is. Each case builds a real game with a real Thumper and a
 * real target, then compares the bot's guess to {@link WeaponAttackAction#toHit}.
 */
class DirectArtilleryToHitGuessTest {

    private static final String THUMPER = "Thumper Artillery Vehicle TAV-1";
    private static final String TARGET = "Archer ARC-2R";
    private static final String LASER_MEK = "Wasp WSP-1A";

    private static final int BOARD_SIZE = 30;
    private static final int SHOOTER_ID = 1;
    private static final int TARGET_ID = 2;
    private static final Coords SHOOTER_POSITION = new Coords(5, 5);
    private static final Coords TARGET_AT_TWELVE_HEXES = new Coords(5, 17);
    private static final Coords TARGET_AT_FIVE_HEXES = new Coords(5, 10);

    private static Entity load(String name) {
        MekSummary summary = MekSummaryCache.getInstance().getMek(name);
        assertNotNull(summary, name + " is not in the unit cache");
        try {
            return new MekFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
        } catch (Exception exception) {
            throw new AssertionError("could not load " + name, exception);
        }
    }

    private static Game game() {
        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Game game = new Game();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.setBoard(new Board(BOARD_SIZE, BOARD_SIZE, hexes));
        Player us = new Player(1, "Us");
        us.setTeam(1);
        Player them = new Player(2, "Them");
        them.setTeam(2);
        game.addPlayer(1, us);
        game.addPlayer(2, them);
        game.setPhase(GamePhase.FIRING);
        return game;
    }

    private static Entity place(Game game, String name, int id, int ownerId, Coords position, int facing) {
        Entity entity = load(name);
        entity.setGame(game);
        entity.setId(id);
        entity.setOwner(game.getPlayer(ownerId));
        entity.setPosition(position);
        entity.setFacing(facing);
        entity.setSecondaryFacing(facing);
        entity.setDeployed(true);
        game.addEntity(entity, false);
        return entity;
    }

    /** The Thumper stood still; the target ran 5 hexes into light woods. */
    private static Entity[] thumperAndRunningTarget(Game game, Coords targetPosition) {
        Entity thumper = place(game, THUMPER, SHOOTER_ID, 2, SHOOTER_POSITION, 3);
        thumper.getCrew().setGunnery(4, 0);
        thumper.moved = EntityMovementType.MOVE_NONE;
        game.getBoard().getHex(targetPosition).addTerrain(new Terrain(Terrains.WOODS, 1));
        Entity target = place(game, TARGET, TARGET_ID, 1, targetPosition, 0);
        target.moved = EntityMovementType.MOVE_RUN;
        target.delta_distance = 5;
        return new Entity[] { thumper, target };
    }

    private static void smokeBetween(Game game) {
        game.getBoard().getHex(new Coords(5, 10)).addTerrain(new Terrain(Terrains.SMOKE, SmokeCloud.SMOKE_HEAVY));
        game.getBoard().getHex(new Coords(5, 11)).addTerrain(new Terrain(Terrains.SMOKE, SmokeCloud.SMOKE_HEAVY));
    }

    private static WeaponMounted firstWeaponWithFlag(Entity entity, WeaponTypeFlag flag) {
        for (WeaponMounted weapon : entity.getWeaponList()) {
            if (weapon.getType().hasFlag(flag)) {
                return weapon;
            }
        }
        throw new AssertionError(entity.getShortName() + " has no weapon with the expected flag");
    }

    private static Princess princessFor(Game game) {
        Princess princess = mock(Princess.class);
        when(princess.getGame()).thenReturn(game);
        when(princess.getBehaviorSettings()).thenReturn(new BehaviorSettings());
        when(princess.getFireControlState()).thenReturn(new FireControlState());
        when(princess.getPriorityUnitTargets()).thenReturn(new HashSet<>());
        when(princess.getMaxWeaponRange(any(Entity.class), any(Boolean.class))).thenCallRealMethod();
        Precognition precognition = mock(Precognition.class);
        when(precognition.getECMInfo()).thenReturn(new ArrayList<>());
        when(princess.getPrecognition()).thenReturn(precognition);
        when(princess.getHonorUtil()).thenReturn(new HonorUtil());
        return princess;
    }

    private static ToHitData engineToHit(Game game, Entity shooter, Entity target, WeaponMounted weapon) {
        return WeaponAttackAction.toHit(game, shooter.getId(), target, shooter.getEquipmentNum(weapon), false);
    }

    private static ToHitData botGuess(Game game, Entity shooter, Entity target, WeaponMounted weapon) {
        FireControl fireControl = new FireControl(princessFor(game));
        return fireControl.guessToHitModifierForWeapon(shooter, new EntityState(shooter), target,
              new EntityState(target), weapon, null, game);
    }

    @Test
    void guessMatchesTheEngineForAVisibleTargetInWoods() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);
        WeaponMounted thumperGun = firstWeaponWithFlag(units[0], WeaponType.F_ARTILLERY);

        ToHitData engine = engineToHit(game, units[0], units[1], thumperGun);
        ToHitData guess = botGuess(game, units[0], units[1], thumperGun);

        assertEquals(8, engine.getValue(), "engine: 4 gunnery + 4 direct artillery; " + engine.getDesc());
        assertEquals(engine.getValue(), guess.getValue(), "bot guess: " + guess.getDesc());
    }

    @Test
    void guessStillEstimatesTheShotThroughASmokeScreen() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);
        smokeBetween(game);
        WeaponMounted thumperGun = firstWeaponWithFlag(units[0], WeaponType.F_ARTILLERY);
        assertFalse(LosEffects.calculateLOS(game, units[0], units[1]).canSee(),
              "the smoke screen must actually block line of sight for this test to mean anything");

        ToHitData engine = engineToHit(game, units[0], units[1], thumperGun);
        ToHitData guess = botGuess(game, units[0], units[1], thumperGun);

        assertEquals(8, engine.getValue(), "engine fires without LOS at 8; " + engine.getDesc());
        assertEquals(engine.getValue(), guess.getValue(), "bot guess: " + guess.getDesc());
    }

    @Test
    void guessRefusesAShotInsideSixHexes() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_FIVE_HEXES);
        WeaponMounted thumperGun = firstWeaponWithFlag(units[0], WeaponType.F_ARTILLERY);

        assertEquals(TargetRoll.IMPOSSIBLE, engineToHit(game, units[0], units[1], thumperGun).getValue());
        assertEquals(TargetRoll.IMPOSSIBLE, botGuess(game, units[0], units[1], thumperGun).getValue());
    }

    @Test
    void anOrdinaryWeaponStillNeedsLineOfSight() {
        Game game = game();
        Entity laserMek = place(game, LASER_MEK, SHOOTER_ID, 2, new Coords(5, 8), 3);
        laserMek.moved = EntityMovementType.MOVE_NONE;
        Entity target = place(game, TARGET, TARGET_ID, 1, new Coords(5, 13), 0);
        smokeBetween(game);
        WeaponMounted laser = firstWeaponWithFlag(laserMek, WeaponType.F_ENERGY);
        assertFalse(LosEffects.calculateLOS(game, laserMek, target).canSee());

        assertEquals(TargetRoll.IMPOSSIBLE, botGuess(game, laserMek, target, laser).getValue());
    }

    private static double threatTo(Game game, Entity shooter, Entity target) {
        Princess princess = princessFor(game);
        FireControl fireControl = new FireControl(princess);
        when(princess.getFireControl(any(Entity.class))).thenReturn(fireControl);
        BasicPathRanker ranker = new BasicPathRanker(princess);
        return ranker.calculateDamagePotential(shooter, new EntityState(shooter), new MovePath(game, target),
              new EntityState(target), shooter.getPosition().distance(target.getPosition()), game);
    }

    private static FiringPlan thumperPlan(Game game, Entity thumper, Entity target) {
        Princess princess = princessFor(game);
        FireControl fireControl = new FireControl(princess);
        when(princess.getFireControl(any(Entity.class))).thenReturn(fireControl);
        return fireControl.guessFullFiringPlan(thumper, new EntityState(thumper), target, new EntityState(target),
              game);
    }

    @Test
    void pathRankerCountsAVisibleGunAsAThreat() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);

        assertTrue(threatTo(game, units[0], units[1]) > 0, "a Thumper in the open must count as a threat");
    }

    @Test
    void pathRankerCountsAHiddenGunAsAThreat() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);
        smokeBetween(game);

        assertTrue(threatTo(game, units[0], units[1]) > 0,
              "a Thumper behind smoke can still shell the target, so it must count as a threat");
    }

    @Test
    void pathRankerStillIgnoresAHiddenUnitWithoutArtillery() {
        Game game = game();
        Entity laserMek = place(game, LASER_MEK, SHOOTER_ID, 2, new Coords(5, 8), 3);
        Entity target = place(game, TARGET, TARGET_ID, 1, new Coords(5, 13), 0);
        smokeBetween(game);

        assertEquals(0, threatTo(game, laserMek, target), 0.0001,
              "a laser cannot fire through the smoke, so it is no threat");
    }

    @Test
    void anArtilleryShotWithNoFriendInTheBlastExpectsNoFriendlyDamage() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);

        assertEquals(0, thumperPlan(game, units[0], units[1]).getExpectedFriendlyDamage(), 0.0001);
    }

    @Test
    void anArtilleryShotNextToAFriendExpectsFriendlyDamage() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);
        place(game, LASER_MEK, 3, 2, TARGET_AT_TWELVE_HEXES.translated(0), 0);

        assertTrue(thumperPlan(game, units[0], units[1]).getExpectedFriendlyDamage() > 0,
              "the Thumper's own Wasp stands next to the target, inside the blast");
    }

    /**
     * The shot needs 8, so a miss scatters 1 to 6 hexes. A Wasp 4 hexes beyond the target is outside the blast at the
     * aimed hex but where a miss by 3 to 5 lands.
     */
    private static final Coords FRIEND_IN_SCATTER_RANGE = TARGET_AT_TWELVE_HEXES.translated(3, 4);

    /** Beyond the longest scatter (6 hexes) plus the blast around it. */
    private static final Coords FRIEND_BEYOND_SCATTER_RANGE = TARGET_AT_TWELVE_HEXES.translated(3, 10);

    /** Our own shot, with the real to-hit roll, as the bot builds it when it decides what to fire. */
    private static WeaponFireInfo ownShot(Game game, Entity thumper, Entity target) {
        WeaponMounted thumperGun = firstWeaponWithFlag(thumper, WeaponType.F_ARTILLERY);
        return new WeaponFireInfo(thumper, target, thumperGun, thumperGun.getLinkedAmmo(), game, false,
              princessFor(game));
    }

    @Test
    void ownShotCountsAFriendWhereAMissCanScatter() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);
        place(game, LASER_MEK, 3, 2, FRIEND_IN_SCATTER_RANGE, 0);

        WeaponFireInfo shot = ownShot(game, units[0], units[1]);

        assertEquals(8, shot.getToHit().getValue(), shot.getToHit().getDesc());
        assertEquals(0, shot.getMaxFriendlyDamage(), 0.0001, "the Wasp is outside the blast at the aimed hex");
        assertTrue(shot.getExpectedScatterFriendlyDamage() > 0, "a miss by 3 to 5 lands on or next to the Wasp");
        FiringPlan plan = new FiringPlan(units[1]);
        plan.add(shot);
        assertTrue(plan.getExpectedFriendlyDamage() > 0, "the firing decision must see the risk to the Wasp");
    }

    @Test
    void guessedShotLeavesTheScatterOut() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);
        place(game, LASER_MEK, 3, 2, FRIEND_IN_SCATTER_RANGE, 0);

        assertEquals(0, thumperPlan(game, units[0], units[1]).getExpectedFriendlyDamage(), 0.0001,
              "path ranking and enemy estimates use guesses and do not shape a blast for every missed roll");
    }

    @Test
    void ownShotIgnoresAFriendOnAnotherBoard() {
        Game game = game();
        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        game.setBoard(1, new Board(BOARD_SIZE, BOARD_SIZE, hexes));
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);
        Entity friendOnOtherBoard = place(game, LASER_MEK, 3, 2, FRIEND_IN_SCATTER_RANGE, 0);
        friendOnOtherBoard.setBoardId(1);

        assertEquals(0, ownShot(game, units[0], units[1]).getExpectedScatterFriendlyDamage(), 0.0001,
              "the Wasp stands on the same hex number of a different board, out of the shell's reach");
    }

    @Test
    void ownShotIgnoresAFriendBeyondTheLongestScatter() {
        Game game = game();
        Entity[] units = thumperAndRunningTarget(game, TARGET_AT_TWELVE_HEXES);
        place(game, LASER_MEK, 3, 2, FRIEND_BEYOND_SCATTER_RANGE, 0);

        assertEquals(0, ownShot(game, units[0], units[1]).getExpectedScatterFriendlyDamage(), 0.0001);
    }
}
