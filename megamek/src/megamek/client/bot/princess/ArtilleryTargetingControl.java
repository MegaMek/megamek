/*  
* MegaMek - Copyright (C) 2018 - The MegaMek Team  
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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.options.OptionsConstants;

/**
 * This class handles the creation of firing plans for indirect-fire artillery and other weapons that get used during the
 * targeting phase.
 * @author NickAragua
 *
 */
public class ArtilleryTargetingControl {
    private static final int NO_AMMO = -1;
    // biggest known kaboom is the 120 cruise missile with a 4-hex radius, but it's not very common
    // and greatly increases the number of spaces we need to check
    private static final int MAX_ARTILLERY_BLAST_RADIUS = 2; 
    
    // per TacOps, this is the to-hit modifier for indirect artillery attacks.
    private static final int ARTILLERY_ATTACK_MODIFIER = 7;
    
    // The main principle here isn't to try to anticipate enemy movement: that's unlikely, especially for faster or jump-capable units.
    // The main principle instead is to put down fire that a) may land on enemy units
    // b) is less likely to land on my units
    
    // Each potential hex is evaluated as follows:
    // (summed over all units within blast radius of hex) (1/unit run speed + 1) * odds of hitting hex * unit friendliness factor (1 for enemy, -1 for ally)
    // repeat and sum over all hexes within scatter pattern
    
    // this is a data structure that maps artillery damage value (which directly correlates with blast radius)
    // to a dictionary containing sets of coordinates and the damage value if one of those coordinates were hit by a shell
    // does not take into account hit odds or anything like that
    private Map<Integer, HashMap<Coords, Double>> damageValues = new HashMap<>();
    
    private Set<Targetable> targetSet;
    
    /**
     * Worker function that calculates the total damage that would be done if a shot with the given damage value
     * would hit the target coordinates.
     * 
     * Caches computation results to avoid repeat 
     * @param damage
     * @param coords
     * @param shooter
     * @param game The current {@link Game}
     * @param owner the {@link Princess} bot to calculate for
     */
    public double calculateDamageValue(int damage, Coords coords, Entity shooter, Game game,
                                       Princess owner) {
        if (getDamageValue(damage, coords) != null) {
            return getDamageValue(damage, coords);
        }
        
        // calculate blast radius = ceiling(damage / 10) - 1
        // for each hex in blast radius, value is 
        // (damage - (distance from center * 10)) * [over all units] 1/(unit run MP + 1) * +/-1 (depending on if unit is friendly or not
        // it's not correct for cruise missiles, but I don't think the bot will be using those.
        int blastRadius = (int) Math.ceil(damage / 10.0) - 1; 
        double totalDamage = calculateDamageValueForHex(damage, coords, shooter, game, owner);
        
        // loop around each concentric hex centered on the given coords
        for (int distanceFromCenter = 1; distanceFromCenter <= blastRadius; distanceFromCenter++) {
            // the damage done is actual damage - 10 * # hexes from center
            int currentDamage = damage - distanceFromCenter * 10;

            for (Coords currentCoords : coords.allAtDistance(distanceFromCenter)) {
                totalDamage += calculateDamageValueForHex(currentDamage, currentCoords, shooter, game, owner);
            }
        }
        
        cacheDamageValue(damage, coords, totalDamage);
        return totalDamage;
    }
    
    /**
     * Worker function that calculates the "damage value" of a single hex.
     * The formula is (summed over all units in target hex) 
     *  [incoming damage] * [1 / (unit run mp + 1)] * [-1 if friendly, +1 if enemy] 
     * @param damage How much damage will we do
     * @param coords Coordinates to hit
     * @param shooter Entity doing the shooting
     * @param game The current {@link Game}
     */
    private double calculateDamageValueForHex(int damage, Coords coords, Entity shooter, Game game, Princess owner) {
        double value = 0;
        
        for (Entity entity : game.getEntitiesVector(coords, true)) {
            // ignore aircraft for now, and also transported entities
            if (entity.isAirborne() || entity.isAirborneVTOLorWIGE() || entity.getTransportId() != Entity.NONE) {
                continue;
            }
            
            int friendlyMultiplier = -1;
            
            // try to avoid shooting at friendlies
            // ignore routed enemies who haven't resumed fire
            if (entity.isEnemyOf(shooter)) {
                boolean enemyUnitBroken = owner.getHonorUtil().isEnemyBroken(entity.getId(), 
                        shooter.getOwnerId(), 
                        owner.getBehaviorSettings().isForcedWithdrawal());
                
                boolean enemyDishonored = owner.getHonorUtil().isEnemyDishonored(entity.getOwnerId());
                
                if (!enemyUnitBroken || enemyDishonored) {
                    friendlyMultiplier = 1;
                } else {
                    friendlyMultiplier = 0;
                }
            }
            
            double speedMultiplier = (double) 1 / (entity.getRunMP() + 1);
            value += damage * speedMultiplier * friendlyMultiplier;
        }
        
        return value;
    }
    
    /**
     * Cache a calculated damage value for the given damage/coordinates combo
     * @param damage
     * @param coords
     * @param value
     */
    private void cacheDamageValue(int damage, Coords coords, Double value) {
        if (!damageValues.containsKey(damage)) {
            damageValues.put(damage, new HashMap<>());
        }
        
        damageValues.get(damage).put(coords, value);
    }
    
    /**
     * Retrieve a calculated damage value for the given damage/coords combo
     * @param damage
     * @param coords
     * @return
     */
    private Double getDamageValue(int damage, Coords coords) {
        if (damageValues.containsKey(damage)) {
            return damageValues.get(damage).get(coords);
        }
        
        return null;
    }
    
    /**
     * Clears out all cached elements in preparation for a new targeting phase.
     */
    public void initializeForTargetingPhase() {
        damageValues = new HashMap<>();
        targetSet = null;
    }
    
    /**
     * Builds a list of eligible targets for artillery strikes.
     * This includes hexes on and within the max radius of all non-airborne enemy entities
     * and hexes on and within the max radius of all strategic targets.
     * @param shooter Entity doing the shooting
     * @param game The current {@link Game}
     * @param owner Bot pointer
     */
    private void buildTargetList(Entity shooter, Game game, Princess owner) {
        targetSet = new HashSet<>();
        
        for (Iterator<Entity> enemies = game.getAllEnemyEntities(shooter); enemies.hasNext();) {
            Entity e = enemies.next();
            
            // skip airborne entities, and those off board - we'll handle them later and don't target ignored units
            if (!e.isAirborne()
                    && !e.isAirborneVTOLorWIGE()
                    && !e.isOffBoard()
                    && !owner.getBehaviorSettings().getIgnoredUnitTargets().contains(e.getId())) {

                targetSet.add(new HexTarget(e.getPosition(), Targetable.TYPE_HEX_ARTILLERY));
                
                // while we're here, consider shooting at hexes within "MAX_BLAST_RADIUS"
                // of the entity. 
                addHexDonuts(e.getPosition(), targetSet, game);
            }
        }
        
        for (Entity enemy : game.getAllOffboardEnemyEntities(shooter.getOwner())) {
            if (enemy.isOffBoardObserved(shooter.getOwner().getTeam())) {
                targetSet.add(enemy);
            }
        }
        
        for (Coords coords : owner.getStrategicBuildingTargets()) {
            targetSet.add(new HexTarget(coords, Targetable.TYPE_HEX_ARTILLERY));
            
            // while we're here, consider shooting at hexes within "MAX_BLAST_RADIUS"
            // of the strategic targets.
            addHexDonuts(coords, targetSet, game);
        }
    }
    
    /**
     * Adds on-board HexTargets within the MAX_ARTILLERY_BLAST_RADIUS of the given coordinates
     * to the given HexTarget set. 
     * @param coords Center coordinates
     * @param targetList List of target hexes
     * @param game The current {@link Game}
     */
    private void addHexDonuts(Coords coords, Set<Targetable> targetList, Game game) {
        // while we're here, consider shooting at hexes within "MAX_BLAST_RADIUS"
        // of the designated coordinates 
        for (int radius = 1; radius <= MAX_ARTILLERY_BLAST_RADIUS; radius++) {
            for (Coords donutHex : coords.allAtDistance(radius)) {
                // don't bother adding off-board donuts.
                if (game.getBoard().contains(donutHex)) {
                    targetList.add(new HexTarget(donutHex, Targetable.TYPE_HEX_ARTILLERY));
                }
            }
        }
    }
    
    /**
     * Calculate an indirect artillery "fire plan", taking into account the possibility of rotating the turret.
     * @param shooter Entity doing the shooting
     * @param game The current {@link Game}
     * @param owner Princess pointer
     * @return Firing plan
     */
    public FiringPlan calculateIndirectArtilleryPlan(Entity shooter, Game game, Princess owner) {
        FiringPlan bestPlan = calculateIndirectArtilleryPlan(shooter, game, owner, 0);
        
        return bestPlan;
    }
    
    /**
     * Put together an indirect artillery "fire plan".
     * @param shooter Entity doing the shooting
     * @param game The current {@link Game}
     * @param owner Princess pointer
     * @return Firing plan
     */
    private FiringPlan calculateIndirectArtilleryPlan(Entity shooter, Game game, Princess owner, int facingChange) {
        FiringPlan retval = new FiringPlan();
        
        // if we're fleeing and haven't been shot at, then try not to agitate guys that may pursue us.
        if (owner.isFallingBack(shooter) && !owner.canShootWhileFallingBack(shooter)) {
            return retval;
        }
        
        // set the plan's torso twist/turret rotation
        // also set the 
        // make sure to remember the entity's original rotation as we're manipulating it directly
        retval.setTwist(facingChange);
        int originalFacing = shooter.getSecondaryFacing();
        shooter.setSecondaryFacing(FireControl.correctFacing(originalFacing + facingChange));
        
        // if we haven't built a target list yet, do so now.
        // potential target list is the same regardless of the entity doing the shooting
        if (targetSet == null) {
            buildTargetList(shooter, game, owner);
        }
        
        // loop through all weapons on entity
        // each indirect artillery piece randomly picks a target from the priority list
        // by the end of this loop, we either have 0 max damage/0 top valued coordinates, which indicates there's nothing worth shooting at
        // or we have a 1+ top valued coordinates.
        for (Mounted currentWeapon : shooter.getWeaponList()) {
            if (currentWeapon.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                WeaponType wType = (WeaponType) currentWeapon.getType();
                int damage = wType.getRackSize(); // crazy, but rack size appears to correspond to given damage values for arty pieces in tacops
                
                List<WeaponFireInfo> topValuedFireInfos = new ArrayList<>();
                double maxDamage = 0;
                
                // for each enemy unit, evaluate damage value of firing at its hex.
                // keep track of top target hexes with the same value and fire at them
                for (Targetable hexTarget : targetSet) {
                    double damageValue = 0.0;
                    if (hexTarget.getTargetType() == Targetable.TYPE_ENTITY) {
                        damageValue = damage;
                    } else {
                        damageValue = calculateDamageValue(damage, hexTarget.getPosition(), shooter, game, owner);
                    }
                    
                    WeaponFireInfo wfi = new WeaponFireInfo(shooter, hexTarget,
                            currentWeapon, game, false, owner);
                    
                    // factor the chance to hit when picking a target - if we've got a spotted hex or an auto-hit hex
                    // we should prefer to hit that over something that may scatter to who knows where
                    if (wfi.getProbabilityToHit() > 0) {
                        damageValue *= wfi.getProbabilityToHit();
                        
                        if (damageValue > maxDamage) {
                            topValuedFireInfos.clear();
                            maxDamage = damageValue;
                            topValuedFireInfos.add(wfi);
                        } else if ((damageValue == maxDamage) && (damageValue > 0)) {
                            topValuedFireInfos.add(wfi);
                        }
                    }
                }
                
                // this section is long and obnoxious:
                // Pick a random fire info out of the ones with the top damage level
                // Use that to create an artillery attack action, set the action's ammo
                // then set the fire info's attack action to the created attack action
                // add the fire info to the firing plan
                if (!topValuedFireInfos.isEmpty()) {
                    WeaponFireInfo actualFireInfo = topValuedFireInfos.get(Compute.randomInt(topValuedFireInfos.size()));
                    ArtilleryAttackAction aaa = (ArtilleryAttackAction) actualFireInfo.buildWeaponAttackAction();
                    HelperAmmo ammo= findAmmo(shooter, currentWeapon);

                    if (ammo.equipmentNum > NO_AMMO) {
                        //This can happen if princess is towing ammo trailers, which she really shouldn't be doing...
                        aaa.setAmmoId(ammo.equipmentNum);
                        aaa.setAmmoMunitionType(ammo.munitionType);
                        aaa.setAmmoCarrier(shooter.getId());
                        actualFireInfo.setAction(aaa);
                        retval.add(actualFireInfo);
                        retval.setUtility(retval.getUtility() + maxDamage);
                        owner.sendAmmoChange(shooter.getId(), shooter.getEquipmentNum(currentWeapon), ammo.equipmentNum);
                    }
                }
            } else if (currentWeapon.getType().hasFlag(WeaponType.F_TAG)) {
                WeaponFireInfo tagInfo = getTAGInfo(currentWeapon, shooter, game, owner);
                
                if (tagInfo != null) {
                    retval.add(tagInfo);
                    retval.setUtility(retval.getUtility() + tagInfo.getProbabilityToHit());
                }
            }
        }
        
        shooter.setSecondaryFacing(originalFacing);
        
        return retval;
    }
    
    /**
     * Worker function that calculates the shooter's "best" actions that result in a TAG being fired.
     * @param shooter
     * @param game The current {@link Game}
     * @param owner
     * @return
     */
    private WeaponFireInfo getTAGInfo(Mounted weapon, Entity shooter, Game game, Princess owner) {
        WeaponFireInfo retval = null;
        double hitOdds = 0.0;
        
        // pretty simple logic here: take the best shot that you have
        for (Targetable target : FireControl.getAllTargetableEnemyEntities(owner.getLocalPlayer(), game, owner.getFireControlState())) {
            WeaponFireInfo wfi = new WeaponFireInfo(shooter, target, weapon, game, false, owner);
            if (wfi.getProbabilityToHit() > hitOdds) {
                hitOdds = wfi.getProbabilityToHit();
                retval = wfi;
            }
        }
        
        return retval;
    }

    private static class HelperAmmo {
        public int equipmentNum;
        public long munitionType;

        public HelperAmmo(int equipmentNum, long munitionType) {
            this.equipmentNum = equipmentNum;
            this.munitionType = munitionType;
        }
    }

    /**
     * Worker function that selects the appropriate ammo for the given entity and weapon.
     * @param shooter
     * @param currentWeapon
     * @return
     */
    private HelperAmmo findAmmo(Entity shooter, Mounted currentWeapon) {
        int ammoEquipmentNum = NO_AMMO;
        long ammoMunitionType = -1;
        
        // simply grab the first valid ammo and let 'er rip.
        for (Mounted ammo : shooter.getAmmo()) {
            if (!ammo.isAmmoUsable() || !AmmoType.isAmmoValid(ammo, (WeaponType) currentWeapon.getType())) {
                continue;
            }
            
            ammoEquipmentNum = shooter.getEquipmentNum(ammo);
            ammoMunitionType = ((AmmoType) ammo.getType()).getMunitionType();
            break;
            
            // TODO: Attempt to select homing ammo if the target is tagged. 
            // To do so, check ammoType.getMunitionType() == AmmoType.M_HOMING
        }
        
        return new HelperAmmo(ammoEquipmentNum, ammoMunitionType);
    }

    /**
     * Function that calculates the potential damage if an artillery attack
     * were to land on target. 
     * @param coords
     * @param operator
     * @return
     */
    public static double evaluateIncomingArtilleryDamage(Coords coords, Princess operator) {
        double sum = 0;
        
        for (Enumeration<ArtilleryAttackAction> attackEnum = operator.getGame().getArtilleryAttacks(); attackEnum.hasMoreElements();) {
            ArtilleryAttackAction aaa = attackEnum.nextElement();
            
            // calculate damage: damage - (10 * distance to me), floored at 0
            // we only say that it will actually be damage if the attack coming in is landing right after the movement phase
            double actualDamage = 0.0;
            
            if (aaa.getTurnsTilHit() == 0) {
                // damage for artillery weapons is, for some reason, derived from the weapon type's rack size
                Mounted weapon = aaa.getEntity(operator.getGame()).getEquipment(aaa.getWeaponId());
                int damage = ((WeaponType) weapon.getType()).getRackSize();
                
                // distance from given coordinates reduces damage
                Coords attackDestination = aaa.getTarget(operator.getGame()).getPosition();
                int distance = coords.distance(attackDestination);
                
                // calculate odds of attack actually hitting
                // artillery skill may be gunnery or artillery depending on game options
                int artySkill = aaa.getEntity(operator.getGame()).getCrew().getGunnery();
                if (operator.getGame().getOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
                    artySkill = aaa.getEntity(operator.getGame()).getCrew().getArtillery();
                }
                
                double hitOdds = 0.0;
                if (operator.getArtilleryAutoHit() != null &&
                        operator.getArtilleryAutoHit().contains(coords)) {
                    hitOdds = 1.0;
                } else {
                    hitOdds = Compute.oddsAbove(artySkill + ARTILLERY_ATTACK_MODIFIER);
                }
            
                actualDamage = Math.max(damage - (10 * distance), 0) * hitOdds;
            }
            
            sum += actualDamage;
        }
        
        return sum;
    }
}
