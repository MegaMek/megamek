/*
 * MegaMek - Copyright (C) 2019 Megamek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.*;
import megamek.common.options.OptionsConstants;
import org.apache.logging.log4j.LogManager;

/**
 * Princess-Bot fire control class used to calculate firing plans for units that
 * can shoot at multiple targets without incurring a penalty.
 * @author NickAragua
 *
 */
public class MultiTargetFireControl extends FireControl {

    public MultiTargetFireControl(Princess owningPrincess) {
        super(owningPrincess);
    }

    /**
     * Calculates the best firing plan for a particular entity, assuming that everybody has already moved.
     * Assumes no restriction on number of units that may be targeted.
     */
    @Override
    public FiringPlan getBestFiringPlan(final Entity shooter,
            final IHonorUtil honorUtil,
            final Game game,
            final Map<Mounted, Double> ammoConservation) {
        FiringPlan bestPlan = new FiringPlan();

        // optimal firing patterns for units such as dropships, Thunderbolts with multi-trac
        // units with 'multi-tasker' quirk, multi-gunner vehicles, etc.
        // are different (and easier to calculate) than optimal firing patterns for other units
        // because there is no secondary target penalty.
        //
        // So, the basic algorithm is as follows:
        // For each weapon, calculate the easiest shot.
        // Then, solve the backpack problem.

        List<Mounted> weaponList;

        if (shooter.usesWeaponBays()) {
            weaponList = shooter.getWeaponBayList();
        } else {
            weaponList = shooter.getWeaponList();
        }

        int originalFacing = shooter.getSecondaryFacing();

        // check all valid secondary facings (turret rotations/torso twists) and arm/flip combination
        // to see if there's a better firing plan
        List<Integer> facingChanges = getValidFacingChanges(shooter);
        facingChanges.add(0); // "no facing change"

        for (int currentTwist : facingChanges) {
            shooter.setSecondaryFacing(correctFacing(originalFacing + currentTwist), false);

            FiringPlan currentPlan = calculateFiringPlan(shooter, weaponList);
            currentPlan.setTwist(currentTwist);

            if (currentPlan.getUtility() > bestPlan.getUtility()) {
                bestPlan = currentPlan;
            }

            // check the plan where the shooter flips its arms
            if (shooter.canFlipArms()) {
                shooter.setArmsFlipped(true);

                currentPlan = calculateFiringPlan(shooter, weaponList);
                currentPlan.setFlipArms(true);

                if (currentPlan.getUtility() > bestPlan.getUtility()) {
                    bestPlan = currentPlan;
                }

                // put it back as we found it
                shooter.setArmsFlipped(false);
            }
        }

        // put it back as we found it
        shooter.setSecondaryFacing(originalFacing, false);

        return bestPlan;
    }

    /**
     * Get me the best shot that this particular weapon can take.
     * @param weapon Weapon to fire.
     * @return The weapon fire info with the most expected damage. Null if no such thing.
     */
    WeaponFireInfo getBestShot(Entity shooter, Mounted weapon) {
        WeaponFireInfo bestShot = null;

        for (Targetable target : getTargetableEnemyEntities(shooter, owner.getGame(), owner.getFireControlState())) {
            final int ownerID = (target instanceof Entity) ? ((Entity) target).getOwnerId() : -1;
            if (owner.getHonorUtil().isEnemyBroken(target.getId(), ownerID, owner.getBehaviorSettings().isForcedWithdrawal())) {
                LogManager.getLogger().info(target.getDisplayName() + " is broken - ignoring");
                continue;
            }

            ArrayList<Mounted> ammos;
            ammos = shooter.getAmmo(weapon);

            for (Mounted ammo: ammos) {
                WeaponFireInfo shot = buildWeaponFireInfo(shooter, target, weapon, ammo, owner.getGame(), false);

                // this is a better shot if it has a chance of doing damage and the damage is better than the previous best shot
                if ((shot.getExpectedDamage() > 0) &&
                        ((bestShot == null) || (shot.getExpectedDamage() > bestShot.getExpectedDamage()))) {
                    bestShot = shot;
                }
            }
        }

        return bestShot;
    }

    /**
     * calculates the 'utility' of a firing plan. This particular function
     * ignores any characteristics of the firing plan that depend on having a single target.
     *
     * @param firingPlan
     *            The {@link FiringPlan} to be calculated.
     * @param overheatTolerance
     *            How much overheat we're willing to forgive.
     * @param shooterIsAero
     *            Set TRUE if the shooter is an Aero unit. Overheating Aeros
     *            take stiffer penalties.
     */
    @Override
    void calculateUtility(final FiringPlan firingPlan,
                          final int overheatTolerance,
                          final boolean shooterIsAero) {
        int overheat = 0;
        if (firingPlan.getHeat() > overheatTolerance) {
            overheat = firingPlan.getHeat() - overheatTolerance;
        }

        double modifier = 1;
        // eliminated calls to calcCommandUtility, calcStrategicBuildingTargetUtility, calcPriorityUnitTargetUtility

        double expectedDamage = firingPlan.getExpectedDamage();
        double utility = 0;
        utility += DAMAGE_UTILITY * expectedDamage;
        utility += CRITICAL_UTILITY * firingPlan.getExpectedCriticals();
        utility += KILL_UTILITY * firingPlan.getKillProbability();
        // eliminated calcTargetPotentialDamageMultiplier, calcDamageAllocationUtility, calcCivilianTargetDisutility
        // Multiply the combined damage/crit/kill utility for a target by a log-scaled factor based on the target's damage potential.
        utility *= modifier;
        utility -= (shooterIsAero ? OVERHEAT_DISUTILITY_AERO : OVERHEAT_DISUTILITY) * overheat;
        // eliminated ejected pilot disutility, as it's superflous - we will ignore ejected mechwarriors altogether.
        firingPlan.setUtility(utility);
    }

    FiringPlan calculateFiringPlan(Entity shooter, List<Mounted> weaponList) {
        FiringPlan retVal = new FiringPlan();

        List<WeaponFireInfo> shotList = new ArrayList<>();
        for (Mounted weapon : weaponList) {
            WeaponFireInfo shot = getBestShot(shooter, weapon);
            if (shot != null) {
                shotList.add(shot);
            }
        }

        boolean shooterIsLarge =
                shooter.hasETypeFlag(Entity.ETYPE_DROPSHIP) ||
                shooter.hasETypeFlag(Entity.ETYPE_JUMPSHIP) ||
                shooter.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT);

        // the logic is significantly different when heat is generated by firing arc, rather than by individual weapon/bay
        if (!owner.getGame().getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY) && shooterIsLarge) {
            retVal = calculatePerArcFiringPlan(shooter, shotList);
        } else {
            retVal = calculateIndividualWeaponFiringPlan(shooter, shotList, shooterIsLarge);
        }

        calculateUtility(retVal, calcHeatTolerance(shooter, shooter.isAero()), true);
        return retVal;
    }

    /**
     * Worker function that calculates a firing plan for a shooter under the "heat per weapon arc" rules
     * (which are the default), given a list of optimal shots for each weapon.
     * @param shooter The unit doing the shooting.
     * @param shotList The list of optimal weapon shots.
     * @return An optimal firing plan.
     */
    FiringPlan calculatePerArcFiringPlan(Entity shooter, List<WeaponFireInfo> shotList) {
        FiringPlan retVal = new FiringPlan();

        // Arc # < 0 indicates that same arc, but rear firing
        // organize weapon fire infos: arc #, list of weapon fire info
        Map<Integer, List<WeaponFireInfo>> arcShots = new HashMap<>();
        // heat values by arc: arc #, arc heat.
        Map<Integer, Integer> arcHeat = new HashMap<>();
        // damage values by arc: arc #, arc damage
        Map<Integer, Double> arcDamage = new HashMap<>();

        // assemble the data we'll need to solve the backpack problem
        for (WeaponFireInfo shot : shotList) {
            int arc = shooter.getWeaponArc(shooter.getEquipmentNum(shot.getWeapon()));
            // flip the # if it's a rear-mounted weapon
            if (shot.getWeapon().isRearMounted()) {
                arc = -arc;
            }

            if (!arcShots.containsKey(arc)) {
                arcShots.put(arc, new ArrayList<>());
                arcHeat.put(arc, shooter.getHeatInArc(shot.getWeapon().getLocation(), shot.getWeapon().isRearMounted()));
                arcDamage.put(arc, 0.0);
            }

            arcShots.get(arc).add(shot);
            arcDamage.put(arc, arcDamage.get(arc) + shot.getExpectedDamage());
        }

        // initialize the backpack
        Map<Integer, Map<Integer, List<Integer>>> arcBackpack = new HashMap<>();
        for (int x = 0; x < arcShots.keySet().size(); x++) {
            arcBackpack.put(x, new HashMap<>());

            for (int y = 0; y < shooter.getHeatCapacity(); y++) {
                arcBackpack.get(x).put(y, new ArrayList<>());
            }
        }

        double[][] damageBackpack = new double[arcShots.keySet().size()][shooter.getHeatCapacity()];
        Integer[] arcHeatKeyArray = new Integer[arcHeat.keySet().size()];
        System.arraycopy(arcHeat.keySet().toArray(), 0, arcHeatKeyArray, 0, arcHeat.keySet().size());

        // now, we essentially solve the backpack problem, where the arcs are the items:
        // arc expected damage is the "value", and arc heat is the "weight", while the backpack capacity is the unit's heat capacity.
        // while we're at it, we assemble the list of arcs fired for each cell
        for (int arcIndex = 0; arcIndex < arcHeatKeyArray.length; arcIndex++) {
            for (int heatIndex = 0; heatIndex < shooter.getHeatCapacity(); heatIndex++) {
                int previousArc = arcIndex > 0 ? arcHeatKeyArray[arcIndex - 1] : 0;

                if (arcIndex == 0 || heatIndex == 0) {
                    damageBackpack[arcIndex][heatIndex] = 0;
                } else if (arcHeat.get(previousArc) <= heatIndex) {
                    int previousHeatIndex = heatIndex - arcHeat.get(previousArc);
                    double currentArcDamage = arcDamage.get(previousArc) + damageBackpack[arcIndex - 1][previousHeatIndex];
                    double accumulatedPreviousArcDamage = damageBackpack[arcIndex - 1][heatIndex];

                    if (currentArcDamage > accumulatedPreviousArcDamage) {
                        // we can add this arc to the list and it'll improve the damage done
                        // so let's do it
                        damageBackpack[arcIndex][heatIndex] = currentArcDamage;
                        // make sure we don't accidentally update the cell we're examining
                        List<Integer> appendedArcList = new ArrayList<>(arcBackpack.get(arcIndex - 1).get(previousHeatIndex));
                        appendedArcList.add(previousArc);
                        arcBackpack.get(arcIndex).put(heatIndex, appendedArcList);
                    } else {
                        // we *can* add this arc to the list, but it won't take us past the damage
                        // provided by the previous arc, so carry value from left to right
                        damageBackpack[arcIndex][heatIndex] = accumulatedPreviousArcDamage;
                        arcBackpack.get(arcIndex).put(heatIndex, arcBackpack.get(arcIndex - 1).get(heatIndex));
                    }

                } else {
                    // in this case, we're simply carrying the value from the left to the right
                    damageBackpack[arcIndex][heatIndex] = damageBackpack[arcIndex - 1][heatIndex];
                    arcBackpack.get(arcIndex).put(heatIndex, arcBackpack.get(arcIndex - 1).get(heatIndex));
                }
            }
        }

        // now, we look at the bottom right cell, which contains our optimal firing solution
        // unless there is no firing solution at all, in which case we skip this part
        if (!arcBackpack.isEmpty()) {
            for (int arc : arcBackpack.get(arcBackpack.size() - 1).get(shooter.getHeatCapacity() - 1)) {
                retVal.addAll(arcShots.get(arc));
            }
        }

        return retVal;
    }

    /**
     * Worker function that calculates a firing plan for a shooter under the "individual weapon heat" rules,
     * given a list of optimal shots for each weapon.
     * @param shooter The unit doing the shooting.
     * @param shotList The list of optimal weapon shots.
     * @return An optimal firing plan.
     */
    FiringPlan calculateIndividualWeaponFiringPlan(Entity shooter, List<WeaponFireInfo> shotList, boolean shooterIsLarge) {
        FiringPlan retVal = new FiringPlan();

        // the 'heat capacity' is affected negatively by having existing heat and by being an aerospace fighter
        // it is affected positively by being a mech (you can overheat a little)
        // and by having the combat computer quirk
        int heatCapacityModifier = -shooter.getHeat();
        heatCapacityModifier += shooter.isAero() ? 0 : 4;
        heatCapacityModifier += shooter.hasQuirk(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER) ? 4 : 0;

        // if firing every gun won't bring heat above the shooter's heat capacity (this includes non-heat-tracking units)
        // then we just return every shot to save ourselves a backpack problem
        int alphaStrikeHeat = 0;
        for (WeaponFireInfo shot : shotList) {
            alphaStrikeHeat += shot.getHeat();
        }

        if (alphaStrikeHeat < shooter.getHeatCapacity() - shooter.getHeat() + heatCapacityModifier) {
            for (WeaponFireInfo shot : shotList) {
                retVal.add(shot);
            }

            return retVal;
        }

        // if we are a "large" craft that can't overheat, we simply cannot fire more weapons than heat capacity
        // if we are an aerospace fighter or ground-based unit that tracks heat, we totally can overheat and the "heat capacity"
        int actualHeatCapacity = shooter.getHeatCapacity();

        if (!shooterIsLarge) {
            actualHeatCapacity += heatCapacityModifier;
        }

        // initialize the backpack
        Map<Integer, Map<Integer, List<Integer>>> shotBackpack = new HashMap<>();
        for (int x = 0; x <= shotList.size(); x++) {
            shotBackpack.put(x, new HashMap<>());

            for (int y = 0; y < actualHeatCapacity; y++) {
                shotBackpack.get(x).put(y, new ArrayList<>());
            }
        }

        double[][] damageBackpack = new double[shotList.size() + 1][actualHeatCapacity];

        // like the above method, we solve the backpack problem here:
        // WeaponFireInfo are the items
        // expected damage is the "value", heat is the "weight", backpack capacity is the unit's heat capacity
        // while we're at it, we assemble the list of shots fired for each cell
        for (int shotIndex = 0; shotIndex <= shotList.size(); shotIndex++) {
            for (int heatIndex = 0; heatIndex < actualHeatCapacity; heatIndex++) {
                if (shotIndex == 0 || heatIndex == 0) {
                    damageBackpack[shotIndex][heatIndex] = 0;
                } else if (shotList.get(shotIndex - 1).getHeat() <= heatIndex) {
                    int previousHeatIndex = heatIndex - shotList.get(shotIndex - 1).getHeat();
                    double currentShotDamage = shotList.get(shotIndex - 1).getExpectedDamage() +
                            damageBackpack[shotIndex - 1][previousHeatIndex];
                    double accumulatedPreviousShotDamage = damageBackpack[shotIndex - 1][heatIndex];

                    if (currentShotDamage > accumulatedPreviousShotDamage) {
                        // we can add this shot to the list and it'll improve the damage done
                        // so let's do it
                        damageBackpack[shotIndex][heatIndex] = currentShotDamage;
                        // make sure we don't accidentally update the cell we're examining
                        List<Integer> appendedShotList = new ArrayList<>(shotBackpack.get(shotIndex - 1).get(previousHeatIndex));
                        appendedShotList.add(shotIndex - 1);
                        shotBackpack.get(shotIndex).put(heatIndex, appendedShotList);
                    } else {
                        // we *can* add this arc to the list, but it won't take us past the damage
                        // provided by the previous arc, so carry value from left to right
                        damageBackpack[shotIndex][heatIndex] = accumulatedPreviousShotDamage;
                        shotBackpack.get(shotIndex).put(heatIndex, shotBackpack.get(shotIndex - 1).get(heatIndex));
                    }

                } else {
                    // in this case, we're simply carrying the value from the left to the right
                    damageBackpack[shotIndex][heatIndex] = damageBackpack[shotIndex - 1][heatIndex];
                    shotBackpack.get(shotIndex).put(heatIndex, shotBackpack.get(shotIndex - 1).get(heatIndex));
                }
            }
        }

        // now, we look at the bottom right cell, which contains our optimal firing solution
        for (int shotIndex : shotBackpack.get(shotBackpack.size() - 1).get(actualHeatCapacity - 1)) {
            retVal.add(shotList.get(shotIndex));
        }

        return retVal;
    }
}
