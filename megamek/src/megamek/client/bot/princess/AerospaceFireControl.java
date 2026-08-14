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

import java.util.ArrayList;
import java.util.List;

import megamek.common.HexTarget;
import megamek.common.TargetRollModifier;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * Gunnery for aerospace units flying in an atmosphere.
 *
 * <p>Two corrections to the stock guess, both of which make the bot's expectations match what the server
 * will actually allow:</p>
 *
 * <ul>
 *     <li><b>The dead zone.</b> The stock guess never checks it, so the bot plans shots the server answers
 *     with {@code IMPOSSIBLE}, books the damage it expected to do, and moves somewhere on the strength of an
 *     attack it was never going to make. Over a ground mapsheet, where one level of altitude blocks fire
 *     within seventeen hexes, that describes most of the board.</li>
 *     <li><b>Range.</b> The stock guess converts ground hexes to aerospace range with integer division while
 *     the engine rounds up, and never adds the altitude difference the rules charge for (TW p.241). Both make
 *     the bot believe it is closer than it is.</li>
 * </ul>
 */
public class AerospaceFireControl extends FireControl {

    /** Refusal reason for a shot the geometry forbids, mirroring the server's own wording. */
    static final TargetRollModifier TH_IN_DEAD_ZONE = new TargetRollModifier(TargetRoll.IMPOSSIBLE,
          "target in dead zone");

    public AerospaceFireControl(Princess owningPrincess) {
        super(owningPrincess);
    }

    /**
     * Rejects a shot the dead zone forbids before the stock guess prices it.
     *
     * <p>Only air-to-air geometry is consulted. A ground attack is made along the flight path rather than at
     * a range, and spheroids are exempt from the block entirely - they fire nose weapons at what is above
     * them and aft weapons at what is below (TW p.241), which is exactly the shot the cone would otherwise
     * deny them.</p>
     */
    @Override
    ToHitData guessToHitModifierForWeapon(final Entity shooter,
          @Nullable EntityState shooterState,
          final Targetable target,
          @Nullable EntityState targetState,
          final WeaponMounted weapon,
          @Nullable final AmmoMounted ammo,
          final Game game) {
        EntityState resolvedShooterState = (shooterState == null) ? new EntityState(shooter) : shooterState;
        EntityState resolvedTargetState = (targetState == null) ? new EntityState(target) : targetState;

        if (isBlockedByDeadZone(shooter, resolvedShooterState, target, resolvedTargetState, game)) {
            return new ToHitData(TH_IN_DEAD_ZONE);
        }

        return super.guessToHitModifierForWeapon(shooter, resolvedShooterState, target, resolvedTargetState,
              weapon, ammo, game);
    }

    /**
     * The range this shot is resolved at, measured the way the rules measure it.
     *
     * <p>Falls back to the stock calculation for anything that is not one airborne aerospace unit shooting at
     * another, so ground gunnery and ground-to-air are untouched.</p>
     */
    /**
     * Drops on the best blast-footprint hex of the flown line, not on the target's own hex.
     *
     * <p>The movement half steers the fighter over the seam of a formation; this half makes the drop
     * actually land there. Worked from the pilot's seat: against a box lance with two hexes of
     * spacing, cluster bombs (5 damage across all seven hexes, no falloff) on a corner mek's hex
     * reach one target - its neighbors are empty - while the seam hex between two meks delivers full
     * damage to both. Every hex the fighter physically flew through is a legal aim point
     * ({@code passedOver}), so the drop target is a search over the flown line with the same ring
     * tables the ranker's footprint credit uses. When the search finds nothing better than the
     * offered target's own hex - single targets, spread formations, plain HE - the stock plan stands
     * unchanged.</p>
     */
    @Override
    protected FiringPlan getDiveBombPlan(final Entity shooter, final MovePath flightPath,
          final Targetable target, final Game game, final boolean passedOverTarget, final boolean guess) {
        Coords bestAim = bestFootprintAimHex(shooter, flightPath, game);
        if ((bestAim != null) && (target.getPosition() != null) && !bestAim.equals(target.getPosition())) {
            HexTarget seam = new HexTarget(bestAim, shooter.getBoardId(), Targetable.TYPE_HEX_AERO_BOMB);
            // The seam hex is on the flown line by construction, so the fly-over requirement holds.
            return super.getDiveBombPlan(shooter, flightPath, seam, game, true, guess);
        }
        return super.getDiveBombPlan(shooter, flightPath, target, game, passedOverTarget, guess);
    }

    /**
     * The hex on the flown line whose blast footprint delivers the most total damage across every
     * enemy ground unit, or {@code null} when no flown hex delivers anything. Uses the executed
     * move's own hexes ({@link Entity#getPassedThrough}) in preference to the planning-time path,
     * because at firing time the flown line is a fact.
     */
    private Coords bestFootprintAimHex(Entity shooter, MovePath flightPath, Game game) {
        // The candidate path FIRST: during movement-phase plan evaluation, getPassedThrough() is the
        // PREVIOUS round's flown line, so preferring it priced candidate paths' bombing utility
        // against stale hexes. At actual firing time the callers pass a null flightPath and the
        // executed move's passedThrough is exactly right.
        List<Coords> flownLine = (flightPath != null)
              ? new ArrayList<>(flightPath.getCoordsSet())
              : shooter.getPassedThrough();
        List<BombMounted> groundBombs = shooter.getBombs(AmmoType.F_GROUND_BOMB);
        if ((flownLine == null) || flownLine.isEmpty() || groundBombs.isEmpty()) {
            return null;
        }
        List<Entity> targets = new ArrayList<>();
        for (Entity enemy : game.getEntitiesVector()) {
            if (enemy.getOwner().isEnemyOf(shooter.getOwner())
                  && !enemy.isAirborne()
                  && (enemy.getPosition() != null)
                  && (enemy.getBoardId() == shooter.getBoardId())
                  && !enemy.isDestroyed()) {
                targets.add(enemy);
            }
        }
        if (targets.isEmpty()) {
            return null;
        }
        Coords bestAimHex = null;
        double bestFootprint = 0;
        for (Coords aimPoint : flownLine) {
            double footprint = 0;
            for (Entity target : targets) {
                int ring = aimPoint.distance(target.getPosition());
                for (BombMounted bomb : groundBombs) {
                    footprint += AerospacePathRanker.bombRingDamage(bomb, ring);
                }
            }
            if (footprint > bestFootprint) {
                bestFootprint = footprint;
                bestAimHex = aimPoint;
            }
        }
        return bestAimHex;
    }

    @Override
    protected int guessDistance(final Entity shooter, final EntityState shooterState, final Targetable target,
          final EntityState targetState, final Game game) {
        if (!isAtmosphericAirToAir(shooter, shooterState, target, targetState, game)) {
            return super.guessDistance(shooter, shooterState, target, targetState, game);
        }
        return AerospaceGeometry.effectiveRange(AerospaceVenue.of(game, shooter),
              shooterState.getPosition(), shooterState.getAltitude(),
              targetState.getPosition(), targetState.getAltitude());
    }

    /**
     * Whether the dead zone bars this particular shot.
     *
     * @return {@code true} if the shot is air-to-air in an atmosphere and the geometry forbids it
     */
    private boolean isBlockedByDeadZone(Entity shooter, EntityState shooterState, Targetable target,
          EntityState targetState, Game game) {
        if (!isAtmosphericAirToAir(shooter, shooterState, target, targetState, game)) {
            return false;
        }
        return AerospaceGeometry.deadZoneBlocksAttack(AerospaceVenue.of(game, shooter),
              shooterState.getPosition(), shooterState.getAltitude(),
              Compute.useSpheroidAtmosphere(game, shooter),
              targetState.getPosition(), targetState.getAltitude());
    }

    /**
     * Whether this is one airborne aerospace unit shooting at another, in an atmosphere and on the same board.
     *
     * <p>Space has no altitude levels and therefore no dead zone, and a cross-board shot has its positions
     * substituted by the engine in ways this geometry does not model.</p>
     */
    private boolean isAtmosphericAirToAir(Entity shooter, EntityState shooterState, Targetable target,
          EntityState targetState, Game game) {
        if ((shooterState.getPosition() == null) || (targetState.getPosition() == null)) {
            return false;
        }
        if (!shooterState.isAero() || !shooterState.isAirborne() || shooter.isSpaceborne()) {
            return false;
        }
        if (!targetState.isAirborneAero()) {
            return false;
        }
        return shooter.getBoardId() == target.getBoardId();
    }
}
