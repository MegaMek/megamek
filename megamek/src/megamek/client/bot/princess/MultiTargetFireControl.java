package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.logging.LogLevel;
import megamek.common.options.OptionsConstants;

public class MultiTargetFireControl extends FireControl {

    private Map<Integer, Integer> shotModifierCache;
    
    public MultiTargetFireControl(Princess owningPrincess) {
        super(owningPrincess);
        // TODO Auto-generated constructor stub
    }

    @Override
    public FiringPlan getBestFiringPlan(final Entity shooter,
            final IHonorUtil honorUtil,
            final IGame game,
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
        
        if(shooter.usesWeaponBays()) {
            weaponList = shooter.getWeaponBayList();
        } else {
            weaponList = shooter.getWeaponList();
        }
        
        List<WeaponFireInfo> shotList = new ArrayList<>();
        for(Mounted weapon : weaponList) {
            WeaponFireInfo shot = getBestShot(weapon);
            if(shot != null) {
                shotList.add(shot);
            }
        }
        
        if(!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_HEAT_BY_BAY) &&
                (shooter.hasETypeFlag(Entity.ETYPE_DROPSHIP) || shooter.hasETypeFlag(Entity.ETYPE_JUMPSHIP))) {
            bestPlan = calculatePerArcFiringPlan(shooter, shotList);
        } else {
            bestPlan = calculateIndividualWeaponFiringPlan(shooter, shotList);
        }
        
        calculateUtility(bestPlan, calcHeatTolerance(shooter, shooter.isAero()), true);
        return bestPlan;
    }
    
    /**
     * Get me the best shot that this particular weapon can take. 
     * @param weapon Weapon to fire.
     * @return The weapon fire info with the most expected damage. Null if no such thing.
     */
    WeaponFireInfo getBestShot(Mounted weapon) {
        WeaponFireInfo bestShot = null;
        
        for(Targetable target : getTargetableEnemyEntities(weapon.getEntity(), owner.getGame(), owner.getFireControlState())) {
            final int ownerID = (target instanceof Entity) ? ((Entity) target).getOwnerId() : -1;
            if(owner.getHonorUtil().isEnemyBroken(target.getTargetId(), ownerID, owner.getBehaviorSettings().isForcedWithdrawal())) {
                owner.log(getClass(), "MultiTargetFireControl.getBestShot", LogLevel.INFO, target.getDisplayName() + " is broken - ignoring");
                continue;
            }
            
            WeaponFireInfo shot = buildWeaponFireInfo(weapon.getEntity(), target, weapon, owner.getGame(), false);
            // this is a better shot if it has a chance of doing damage and the damage is better than the previous best shot
            if((shot.getExpectedDamage() > 0) &&
                    ((bestShot == null) || (shot.getExpectedDamage() > bestShot.getExpectedDamage()))) {
                bestShot = shot;
            }
        }
        
        return bestShot;
    }
    
    /**
     * calculates the 'utility' of a firing plan. override this function if you
     * have a better idea about what firing plans are good
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
        //modifier += calcCommandUtility(firingPlan.getTarget());
        //modifier += calcStrategicBuildingTargetUtility(firingPlan.getTarget());
        //modifier += calcPriorityUnitTargetUtility(firingPlan.getTarget());

        double expectedDamage = firingPlan.getExpectedDamage();
        double utility = 0;
        utility += DAMAGE_UTILITY * expectedDamage;
        utility += CRITICAL_UTILITY * firingPlan.getExpectedCriticals();
        utility += KILL_UTILITY * firingPlan.getKillProbability();
        // Multiply the combined damage/crit/kill utility for a target by a log-scaled factor based on the target's damage potential.
        //utility *= calcTargetPotentialDamageMultiplier(firingPlan.getTarget());
        //utility += TARGET_HP_FRACTION_DEALT_UTILITY * calcDamageAllocationUtility(firingPlan.getTarget(), expectedDamage);
        //utility -= calcCivilianTargetDisutility(firingPlan.getTarget());
        utility *= modifier;
        utility -= (shooterIsAero ? OVERHEAT_DISUTILITY_AERO : OVERHEAT_DISUTILITY) * overheat;
        //utility -= (firingPlan.getTarget() instanceof MechWarrior) ? EJECTED_PILOT_DISUTILITY : 0;
        firingPlan.setUtility(utility);
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
        for(WeaponFireInfo shot : shotList) {
            int arc = shooter.getWeaponArc(shooter.getEquipmentNum(shot.getWeapon()));
            // flip the # if it's a rear-mounted weapon
            if(shot.getWeapon().isRearMounted()) {
                arc = -arc;
            }
            
            if(!arcShots.containsKey(arc)) {
                arcShots.put(arc, new ArrayList<>());
                arcHeat.put(arc, shooter.getHeatInArc(shot.getWeapon().getLocation(), shot.getWeapon().isRearMounted()));
                arcDamage.put(arc, 0.0);
            }
            
            arcShots.get(arc).add(shot);
            arcDamage.put(arc, arcDamage.get(arc) + shot.getExpectedDamage());
        }
        
        // initialize the backpack
        Map<Integer, Map<Integer, List<Integer>>> arcBackpack = new HashMap<>();
        for(int x = 0; x < arcShots.keySet().size(); x++) {
            arcBackpack.put(x, new HashMap<>());
            
            for(int y = 0; y < shooter.getHeatCapacity(); y++) {
                arcBackpack.get(x).put(y, new ArrayList<>());
            }
        }
        
        double[][] damageBackpack = new double[arcShots.keySet().size()][shooter.getHeatCapacity()];
        Integer[] arcHeatKeyArray = new Integer[arcHeat.keySet().size()];
        System.arraycopy(arcHeat.keySet().toArray(), 0, arcHeatKeyArray, 0, arcHeat.keySet().size());       
        
        // now, we essentially solve the backpack problem, where the arcs are the items:
        // arc expected damage is the "value", and arc heat is the "weight", while the backpack capacity is the unit's heat capacity.
        // while we're at it, we assemble the list of arcs fired for each cell
        for(int arcIndex = 0; arcIndex < arcHeatKeyArray.length; arcIndex++) {
            for(int heatIndex = 0; heatIndex < shooter.getHeatCapacity(); heatIndex++) {
                int previousArc = arcIndex > 0 ? arcHeatKeyArray[arcIndex - 1] : 0;
                
                if(arcIndex == 0 || heatIndex == 0) {
                    damageBackpack[arcIndex][heatIndex] = 0;
                } else if(arcHeat.get(previousArc) <= heatIndex) {
                    int previousHeatIndex = heatIndex - arcHeat.get(previousArc);
                    double currentArcDamage = arcDamage.get(previousArc) + damageBackpack[arcIndex - 1][previousHeatIndex];
                    double accumulatedPreviousArcDamage = damageBackpack[arcIndex - 1][heatIndex];
                    
                    if(currentArcDamage > accumulatedPreviousArcDamage) {
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
        for(int arc : arcBackpack.get(arcBackpack.size() - 1).get(shooter.getHeatCapacity() - 1)) {
            retVal.addAll(arcShots.get(arc));
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
    FiringPlan calculateIndividualWeaponFiringPlan(Entity shooter, List<WeaponFireInfo> shotList) {
        FiringPlan retVal = new FiringPlan();
        
        // initialize the backpack
        Map<Integer, Map<Integer, List<Integer>>> shotBackpack = new HashMap<>();
        for(int x = 0; x < shotList.size(); x++) {
            shotBackpack.put(x, new HashMap<>());
            
            for(int y = 0; y < shooter.getHeatCapacity(); y++) {
                shotBackpack.get(x).put(y, new ArrayList<>());
            }
        }
        
        double[][] damageBackpack = new double[shotList.size()][shooter.getHeatCapacity()];     
        
        // like the above method, we solve the backpack problem here:
        // WeaponFireInfo are the items
        // expected damage is the "value", heat is the "weight", backpack capacity is the unit's heat capacity
        // while we're at it, we assemble the list of shots fired for each cell
        for(int shotIndex = 0; shotIndex < shotList.size(); shotIndex++) {
            for(int heatIndex = 0; heatIndex < shooter.getHeatCapacity(); heatIndex++) {
                if(shotIndex == 0 || heatIndex == 0) {
                    damageBackpack[shotIndex][heatIndex] = 0;
                } else if(shotList.get(shotIndex - 1).getHeat() <= heatIndex) {
                    int previousHeatIndex = heatIndex - shotList.get(shotIndex - 1).getHeat();
                    double currentShotDamage = shotList.get(shotIndex - 1).getExpectedDamage() + 
                            damageBackpack[shotIndex - 1][previousHeatIndex];
                    double accumulatedPreviousShotDamage = damageBackpack[shotIndex - 1][heatIndex];
                    
                    if(currentShotDamage > accumulatedPreviousShotDamage) {
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
        for(int shotIndex : shotBackpack.get(shotBackpack.size() - 1).get(shooter.getHeatCapacity() - 1)) {
            retVal.add(shotList.get(shotIndex));
        }
        
        return retVal;
    }
}
