/*
* MegaMek - Copyright (C) 2019 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package megamek.client.bot.princess;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.server.ServerHelper;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;

import static megamek.common.WeaponType.F_ARTILLERY;

/**
 * This class is intended to help the bot calculate firing plans for infantry
 * units.
 *
 * @author NickAragua
 */
public class InfantryFireControl extends FireControl {

    private enum InfantryFiringPlanType {
        Standard,
        FieldGuns,
        Swarm,
        Leg
    }

    public InfantryFireControl(Princess owner) {
        super(owner);
    }

    /**
     * Calculates the maximum damage a unit can do at a given range. Chance to hit is not a factor.
     * @param range The range to be checked.
     * @param useExtremeRange Is the extreme range optional rule in effect?
     * @return The most damage done at that range.
     */
    public double getMaxDamageAtRange(final MovePath shooterPath, final MovePath targetPath,
                                      final int range, final boolean useExtremeRange,
                                      final boolean useLOSRange) {
        double maxFGDamage = 0;
        double maxInfantryWeaponDamage = 0;
        Entity shooter = shooterPath.getEntity();
        Entity target = targetPath.getEntity();
        Hex targetHex = target.getGame().getBoard().getHex(targetPath.getFinalCoords());

        // some preliminary computations
        // whether the target is an infantry platoon
        boolean targetIsPlatoon = target.hasETypeFlag(Entity.ETYPE_INFANTRY) && !((Infantry) target).isSquad();
        // whether the target is infantry (and not battle armor)
        boolean targetIsActualInfantry = target.hasETypeFlag(Entity.ETYPE_INFANTRY)
                && !target.hasETypeFlag(Entity.ETYPE_BATTLEARMOR);
        boolean shooterIsActualInfantry = shooter.hasETypeFlag(Entity.ETYPE_INFANTRY)
                && !shooter.hasETypeFlag(Entity.ETYPE_BATTLEARMOR);
        // field guns can't fire if the unit in question moved
        boolean otherWeaponsMayShoot = !shooterIsActualInfantry || shooterPath.getMpUsed() == 0;
        boolean inBuilding = Compute.isInBuilding(target.getGame(), targetPath.getFinalElevation(),
                targetPath.getFinalCoords());
        boolean inOpen = ServerHelper.infantryInOpen(target, targetHex, target.getGame(), targetIsPlatoon, false,
                false);
        boolean nonInfantryVsMechanized = !shooter.hasETypeFlag(Entity.ETYPE_INFANTRY)
                && target.hasETypeFlag(Entity.ETYPE_INFANTRY) && ((Infantry) target).isMechanized();

        // cycle through my weapons
        for (final Mounted weapon : shooter.getWeaponList()) {
            final WeaponType weaponType = (WeaponType) weapon.getType();

            final int bracket = RangeType.rangeBracket(range, weaponType.getRanges(weapon), useExtremeRange,
                    useLOSRange);

            if (RangeType.RANGE_OUT == bracket) {
                continue;
            }

            // there are N ways this can go, currently we handle these:
            // 1. Shooter is infantry using infantry weapon, target is infantry.
            // Use infantry damage. Track damage separately.
            // 2. Shooter is non-infantry, target is infantry in open. Use
            // "directBlowInfantryDamage", multiply by 2.
            // 3. Shooter is non-infantry, target is infantry in building. Use
            // weapon damage, multiply by building dmg reduction.
            // 4. Shooter is non-infantry, target is infantry in "cover". Use
            // "directBlowInfantryDamage".
            // 5. Shooter is infantry with field gun / field artillery and needs special damage calc.
            // 6. Shooter is non-infantry, target is non-infantry. Use base class.

            // case 1
            if (weaponType.hasFlag(WeaponType.F_INFANTRY)) {
                int infantryCount = 1;

                if (shooter.isConventionalInfantry()) {
                    infantryCount = shooter.getInternal(Infantry.LOC_INFANTRY);
                } else if (shooter instanceof BattleArmor) {
                    infantryCount = ((BattleArmor) shooter).getNumberActiverTroopers();
                }

                maxInfantryWeaponDamage += ((InfantryWeapon) weaponType).getInfantryDamage()
                        * infantryCount;
                // field guns can't fire if the infantry unit has done anything
                // other than turning
            } else if (targetIsActualInfantry && otherWeaponsMayShoot) {
                double damage = 0;

                // if we're outside, use the direct blow infantry damage
                // calculation
                // cases 2, 4
                if (!inBuilding) {
                    damage = Compute.directBlowInfantryDamage(weaponType.getDamage(), 0,
                            weaponType.getInfantryDamageClass(), nonInfantryVsMechanized, false);

                    // if we're in the open, multiply damage by 2
                    damage *= inOpen ? 2 : 1;
                } else {
                    // Otherwise, we take the regular weapon damage and divide
                    // it by the building "toughness level"
                    // case 3
                    damage = weaponType.getDamage() * shooter.getGame().getBoard()
                            .getBuildingAt(targetPath.getFinalCoords()).getDamageReductionFromOutside();
                }

                maxFGDamage += damage;
            } else if (otherWeaponsMayShoot) {
                // case 5
                if (shooter.isInfantry()) {
                    // field guns can't fire if the infantry unit has done anything
                    // other than turning, so we only get here if infantry has not used MP.
                    // All valid Infantry Field Weapons can consider rackSize as their damage.
                    maxFGDamage += weaponType.rackSize;
                // Case 6: all other unit types / weapons
                } else {
                    maxFGDamage += weaponType.getDamage();
                }
            }
        }

        return Math.max(maxFGDamage, maxInfantryWeaponDamage);
    }

    /**
     * Guesses the 'best' firing plan under a certain heat, except this is infantry so we ignore heat
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param shooterState
     *            The current state of the shooting unit.
     * @param target
     *            The unit being shot at.
     * @param targetState
     *            The current state of the target unit.
     * @param maxHeat
     *            How much heat we're willing to tolerate. Ignored, since infantry doesn't track heat.
     * @param game The current {@link Game}
     * @return the 'best' firing plan under a certain heat.
     */
    @Override
    protected FiringPlan guessBestFiringPlanUnderHeat(final Entity shooter, @Nullable EntityState shooterState,
            final Targetable target, @Nullable EntityState targetState, int maxHeat, final Game game) {
        FiringPlan bestPlan = new FiringPlan(target);

        // Shooting isn't possible if one of us isn't on the board.
        if ((null == shooter.getPosition()) || shooter.isOffBoard()
                || !game.getBoard().contains(shooter.getPosition())) {
            LogManager.getLogger().error("Shooter's position is NULL/Off Board!");
            return bestPlan;
        }
        if ((null == target.getPosition()) || target.isOffBoard() || !game.getBoard().contains(target.getPosition())) {
            LogManager.getLogger().error("Target's position is NULL/Off Board!");
            return bestPlan;
        }

        // if it's not infantry, then we shouldn't be here, let's redirect to the base method.
        if (!(shooter instanceof Infantry)) {
            return super.guessBestFiringPlanUnderHeat(shooter, shooterState, target, targetState, maxHeat, game);
        }

        if (null == shooterState) {
            shooterState = new EntityState(shooter);
        }
        if (null == targetState) {
            targetState = new EntityState(target);
        }

        // Infantry can do the following things, which are mutually exclusive:
        // 1. Fire standard infantry weapons, including "need to stay still" support weapons
        // 2. Fire field guns - the unit needs to remain still to fire these
        // 3. Swarm
        // 4. Leg Attack
        // Start with an alpha strike. If it falls under our heat limit, use it.
        List<FiringPlan> firingPlans = new ArrayList<>();

        // case 1: infantry weapons
        FiringPlan standardPlan = guessFiringPlan(shooter, shooterState, target, targetState, game, InfantryFiringPlanType.Standard);
        firingPlans.add(standardPlan);

        // case 2: field guns if we didn't move
        if (shooterState.getHexesMoved() == 0) {
            FiringPlan fieldGunPlan = guessFiringPlan(shooter, shooterState, target, targetState, game, InfantryFiringPlanType.FieldGuns);
            firingPlans.add(fieldGunPlan);
        }

        // case 3: leg attack
        FiringPlan legPlan = guessFiringPlan(shooter, shooterState, target, targetState, game, InfantryFiringPlanType.Leg);
        firingPlans.add(legPlan);

        // case 4: swarm attack
        FiringPlan swarmPlan = guessFiringPlan(shooter, shooterState, target, targetState, game, InfantryFiringPlanType.Swarm);
        firingPlans.add(swarmPlan);

        // now we'll pick the best of the plans
        for (final FiringPlan firingPlan : firingPlans) {
            if ((bestPlan.getUtility() < firingPlan.getUtility())) {
                bestPlan = firingPlan;
            }
        }
        return bestPlan;
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at
     * a target ignoring heat, and using best guess from different states. Does
     * not change facing.
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param shooterState
     *            The current state of the shooter.
     * @param target
     *            The unit being fired on.
     * @param targetState
     *            The current state of the target.
     * @param game The current {@link Game}
     * @return The {@link FiringPlan} containing all weapons to be fired.
     */
    private FiringPlan guessFiringPlan(final Entity shooter, @Nullable EntityState shooterState,
            final Targetable target, @Nullable EntityState targetState, final Game game, InfantryFiringPlanType firingPlanType) {

        final FiringPlan myPlan = new FiringPlan(target);

        // cycle through my field guns
        for (final Mounted weapon : shooter.getWeaponList()) {
            if (weaponIsAppropriate(weapon, firingPlanType)) {
                final WeaponFireInfo shoot = buildWeaponFireInfo(shooter, shooterState, target, targetState, weapon,
                        game, true);

                if (0 < shoot.getProbabilityToHit()) {
                    myPlan.add(shoot);
                }
            }
        }

        // Rank how useful this plan is.
        calculateUtility(myPlan, calcHeatTolerance(shooter, null), shooterState.isAero());

        return myPlan;
    }

    /**
     * Helper method that determines whether a weapon type is appropriate for a given firing plan type,
     * e.g. field guns cannot be fired when we're going to do a swarm attack, etc.
     */
    private boolean weaponIsAppropriate(Mounted weapon, InfantryFiringPlanType firingPlanType) {
        boolean weaponIsSwarm = (weapon.getType()).getInternalName().equals(Infantry.SWARM_MEK);
        boolean weaponIsLegAttack = (weapon.getType()).getInternalName().equals(Infantry.LEG_ATTACK);
        boolean weaponIsFieldGuns = weapon.getLocation() == Infantry.LOC_FIELD_GUNS;

        switch (firingPlanType) {
            case FieldGuns:
                return weaponIsFieldGuns;
            case Swarm:
                return weaponIsSwarm;
            case Leg:
                return weaponIsLegAttack;
            case Standard:
                return !weaponIsFieldGuns && !weaponIsSwarm && !weaponIsLegAttack;
            default:
                return false;
        }
    }
}
