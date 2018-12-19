package megamek.client.bot.princess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.HexTarget;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;

/**
 * This class handles the creation of firing plans for indirect-fire artillery and other weapons that get used during the
 * targeting phase.
 * @author NickAragua
 *
 */
public class ArtilleryTargetingControl {
    private static final int NO_AMMO = -1;
    private static final int MAX_ARTILLERY_BLAST_RADIUS = 1; // biggest known kaboom is the 120 cruise missile with a 4-hex radius
    
    // The main principle here isn't to try to anticipate enemy movement: that's unlikely, especially for faster or jump-capable units.
    // The main principle instead is to put down fire that a) may land on enemy units
    // b) is less likely to land on my units
    
    // Each potential hex is evaluated as follows:
    // (summed over all units within blast radius of hex) (1/unit run speed + 1) * odds of hitting hex * unit friendliness factor (1 for enemy, -1 for ally)
    // repeat and sum over all hexes within scatter pattern
    // for simplicity, all artillery weapons on the 
    
    // this is a data structure that maps artillery damage value (which directly correlates with blast radius)
    // to a dictionary containing sets of coordinates and the damage value if one of those coordinates were hit by a shell
    // does not take into account hit odds or anything like that
    private Map<Integer, HashMap<Coords, Double>> damageValues;
    
    private Set<Targetable> targetList;
    
    /**
     * Worker function that calculates the total damage that would be done if a shot with the given damage value
     * would hit the target coordinates.
     * 
     * Caches computation results to avoid repeat 
     * @param damage
     * @param coords
     * @param shooter
     * @param game
     */
    private double calculateDamageValue(int damage, Coords coords, Entity shooter, IGame game, Princess owner) {
        if(getDamageValue(damage, coords) != null) {
            return getDamageValue(damage, coords);
        }
        
        // calculate blast radius = ceiling(damage / 10) - 1
        // for each hex in blast radius, value is 
        // (damage - (distance from center * 10)) * [over all units] 1/(unit run MP + 1) * +/-1 (depending on if unit is friendly or not
        // it's not correct for cruise missiles, but I don't think the bot will be using those.
        int blastRadius = (int) Math.ceil(damage / 10.0) - 1; 
        double totalDamage = calculateDamageValueForHex(damage, coords, shooter, game, owner);
        
        // loop around each concentric hex centered on the given coords
        for(int distanceFromCenter = 1; distanceFromCenter <= blastRadius; distanceFromCenter++) {
            // the damage done is actual damage - 10 * # hexes from center
            int currentDamage = damage - distanceFromCenter * 10;

            for(Coords currentCoords : BotGeometry.getHexDonut(coords, distanceFromCenter)) {
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
     * @param game Game pointer
     */
    private double calculateDamageValueForHex(int damage, Coords coords, Entity shooter, IGame game, Princess owner) {
        double value = 0;
        
        for(Entity entity : game.getEntitiesVector(coords, false)) {
            // ignore aircraft for now
            if(entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
                continue;
            }
            
            int friendlyMultiplier = -1;
            
            // try to avoid shooting at friendlies
            // ignore routed enemies
            if(entity.isEnemyOf(shooter)) {
                if(!owner.getHonorUtil().isEnemyBroken(entity.getId(), 
                            shooter.getOwnerId(), 
                            owner.getBehaviorSettings().isForcedWithdrawal())) {
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
        if(!damageValues.containsKey(damage)) {
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
        if(damageValues.containsKey(damage)) {
            return damageValues.get(damage).get(coords);
        }
        
        return null;
    }
    
    /**
     * Clears out all cached elements in preparation for a new targeting phase.
     */
    public void initializeForTargetingPhase() {
        damageValues = new HashMap<>();
        targetList = new HashSet<>();
    }
    
    //TODO: Add code to BasicPathRanker.RankPath() to take into account known incoming artillery shots and add them to the expectedDamageTaken
    // value. Do so via function "evaluateIncomingArtillery".
    
    /**
     * Builds a list of eligible targets for artillery strikes.
     * This includes hexes on and within the max radius of all non-airborne enemy entities
     * and hexes on and within the max radius of all strategic targets.
     * @param shooter Entity doing the shooting
     * @param game Game pointer
     * @param owner Bot pointer
     */
    private void buildTargetList(Entity shooter, IGame game, Princess owner) {
        targetList = new HashSet<>();
        
        for(Iterator<Entity> enemies = game.getAllEnemyEntities(shooter); enemies.hasNext();) {
            Entity e = enemies.next();
            
            // skip airborne entities
            if(!e.isAirborne() && !e.isAirborneVTOLorWIGE()) {
                targetList.add(new HexTarget(e.getPosition(), game.getBoard(), Targetable.TYPE_HEX_ARTILLERY));
                
                // while we're here, consider shooting at hexes within "MAX_BLAST_RADIUS"
                // of the entity. 
                for(int radius = 1; radius <= MAX_ARTILLERY_BLAST_RADIUS; radius++) {
                    for(Coords donutHex : BotGeometry.getHexDonut(e.getPosition(), radius)) {
                        targetList.add(new HexTarget(donutHex, game.getBoard(), Targetable.TYPE_HEX_ARTILLERY));
                    }
                }
            }
        }
        
        for(Coords coords : owner.getStrategicBuildingTargets()) {
            targetList.add(new HexTarget(coords, game.getBoard(), Targetable.TYPE_HEX_ARTILLERY));
            
            // while we're here, consider shooting at hexes within "MAX_BLAST_RADIUS"
            // of the entity. 
            for(int radius = 1; radius <= MAX_ARTILLERY_BLAST_RADIUS; radius++) {
                for(Coords donutHex : BotGeometry.getHexDonut(coords, radius)) {
                    targetList.add(new HexTarget(donutHex, game.getBoard(), Targetable.TYPE_HEX_ARTILLERY));
                }
            }
        }
    }
    
    /**
     * Calculate an indirect artillery "fire plan", taking into account the possibility of rotating the turret.
     * @param shooter Entity doing the shooting
     * @param game Game pointer
     * @param owner Princess pointer
     * @return Firing plan
     */
    public FiringPlan calculateIndirectArtilleryPlan(Entity shooter, IGame game, Princess owner) {
        FiringPlan bestPlan = calculateIndirectArtilleryPlan(shooter, game, owner, 0);
        
        // simply loop through all possible facings and see if any of those is better than the no-turning plan
        if(!shooter.isOffBoard()) {
            for(int facingChange : FireControl.getValidFacingChanges(shooter)) {
                FiringPlan twistPlan =  calculateIndirectArtilleryPlan(shooter, game, owner, facingChange);
                if(twistPlan.getUtility() > bestPlan.getUtility()) {
                    bestPlan = twistPlan;
                }
            }
        }
        
        return bestPlan;
    }
    
    /**
     * Put together an indirect artillery "fire plan".
     * @param shooter Entity doing the shooting
     * @param game Game pointer
     * @param owner Princess pointer
     * @return Firing plan
     */
    private FiringPlan calculateIndirectArtilleryPlan(Entity shooter, IGame game, Princess owner, int facingChange) {
        FiringPlan retval = new FiringPlan();
        
        // set the plan's torso twist/turret rotation
        // also set the 
        // make sure to remember the entity's original rotation as we're manipulating it directly
        retval.setTwist(facingChange);
        int originalFacing = shooter.getSecondaryFacing();
        shooter.setSecondaryFacing(FireControl.correctFacing(originalFacing + facingChange));
        
        // if we haven't built a target list yet, do so now.
        // potential target list is the same regardless of the entity doing the shooting
        if(targetList == null || targetList.size() == 0) {
            buildTargetList(shooter, game, owner);
        }
        
        // loop through all weapons on entity
        // each indirect artillery piece randomly picks a target from the priority list
        // by the end of this loop, we either have 0 max damage/0 top valued coordinates, which indicates there's nothing worth shooting at
        // or we have a 1+ top valued coordinates.
        for(Mounted currentWeapon : shooter.getWeaponList()) {
            if(currentWeapon.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                WeaponType wType = (WeaponType) currentWeapon.getType();
                int damage = wType.getRackSize(); // crazy, but rack size appears to correspond to given damage values for arty pieces in tacops
                
                List<WeaponFireInfo> topValuedFireInfos = new ArrayList<>();
                double maxDamage = 0;
                
                // for each enemy unit, evaluate damage value of firing at its hex.
                // keep track of top target hexes with the same value and fire at them
                for(Targetable hexTarget : targetList) {                    
                    double damageValue = calculateDamageValue(damage, hexTarget.getPosition(), shooter, game, owner);
                    WeaponFireInfo wfi = new WeaponFireInfo(shooter, hexTarget,
                            currentWeapon, game, false, owner);
                    
                    // factor the chance to hit when picking a target - if we've got a spotted hex or an auto-hit hex
                    // we should prefer to hit that over something that may scatter to who knows where
                    if(wfi.getProbabilityToHit() > 0) {
                        damageValue *= wfi.getProbabilityToHit();
                        
                        if(damageValue > maxDamage) {
                            topValuedFireInfos.clear();
                            maxDamage = damageValue;
                            topValuedFireInfos.add(wfi);
                        } else if((damageValue == maxDamage) && (damageValue > 0)) {
                            topValuedFireInfos.add(wfi);
                        }
                    }
                }
                
                // this section is long and obnoxious:
                // Pick a random fire info out of the ones with the top damage level
                // Use that to create an artillery attack action, set the action's ammo
                // then set the fire info's attack action to the created attack action
                // add the fire info to the firing plan
                if(!topValuedFireInfos.isEmpty()) {
                    WeaponFireInfo actualFireInfo = topValuedFireInfos.get(Compute.randomInt(topValuedFireInfos.size()));
                    ArtilleryAttackAction aaa = (ArtilleryAttackAction) actualFireInfo.buildWeaponAttackAction();
                    int ammoID = findAmmo(shooter, currentWeapon, game);
                    aaa.setAmmoId(ammoID);
                    actualFireInfo.setAction(aaa);
                    retval.add(actualFireInfo);
                    retval.setUtility(retval.getUtility() + maxDamage);
                    owner.sendAmmoChange(shooter.getId(), shooter.getEquipmentNum(currentWeapon), ammoID);
                }
            } else if(currentWeapon.getType().hasFlag(WeaponType.F_TAG)) {
                WeaponFireInfo tagInfo = getTAGInfo(currentWeapon, shooter, game, owner);
                
                if(tagInfo != null) {
                    retval.add(tagInfo);
                }
            }
        }
        
        shooter.setSecondaryFacing(originalFacing);
        
        return retval;
    }
    
    /**
     * Worker function that calculates the shooter's "best" actions that result in a TAG being fired.
     * @param shooter
     * @param game
     * @param owner
     * @return
     */
    private WeaponFireInfo getTAGInfo(Mounted weapon, Entity shooter, IGame game, Princess owner) {
        WeaponFireInfo retval = null;
        double hitOdds = 0.0;
        
        // pretty simple logic here: take the best shot that you have
        // if multiple best shots, pick randomly
        for(Targetable target : FireControl.getAllTargetableEnemyEntities(owner.getLocalPlayer(), game, owner.getFireControlState())) {
            WeaponFireInfo wfi = new WeaponFireInfo(shooter, target, weapon, game, false, owner);
            if(wfi.getProbabilityToHit() > hitOdds) {
                retval = wfi;
            }
        }
        
        return retval;
    }
    
    /**
     * Worker function that selects the appropriate ammo for the given entity and weapon.
     * @param shooter
     * @param currentWeapon
     * @param game
     * @return
     */
    private int findAmmo(Entity shooter, Mounted currentWeapon, IGame game) {
        int ammoEquipmentNum = NO_AMMO;
        
        // simply grab the first valid ammo and let 'er rip.
        for(Mounted ammo : shooter.getAmmo()) {            
            if(!ammo.isAmmoUsable() || !AmmoType.isAmmoValid(ammo, (WeaponType) currentWeapon.getType())) {
                continue;
            }
            
            ammoEquipmentNum = shooter.getEquipmentNum(ammo);
            break;
            
            // TODO: Attempt to select homing ammo if the target is tagged. 
            // To do so, check ammoType.getMunitionType() == AmmoType.M_HOMING
        }
        
        return ammoEquipmentNum;
    }
}
