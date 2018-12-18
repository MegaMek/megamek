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
import megamek.common.TagInfo;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;

public class ArtilleryTargetingControl {
    private static final int NO_AMMO = -1;
    
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
        
        // calculate blast radius
        // for each hex in blast radius, value is 
        // (damage - (distance from center * 10)) * [over all units] 1/(unit run MP + 1) * +/-1 (depending on if unit is friendly or not
        int blastRadius = (int) Math.ceil(damage / 10); 
        double totalDamage = calculateDamageValueForHex(damage, coords, shooter, game, owner);
        
        // loop around each concentric hex centered on the given coords
        for(int distanceFromCenter = 1; distanceFromCenter < blastRadius; distanceFromCenter++) {
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
    }
    
    //TODO: Build the target list to include hexes surrounding enemy entities, and also strategic targets
    //TODO: Extend FiringPlan (call it ArtilleryFiringPlan) and override getExpectedDamage() function, so that we can:
    //TODO: Create multiple firing plans incorporating "torso twists"
    //TODO: Add code to BasicPathRanker.RankPath() to take into account known incoming artillery shots and add them to the expectedDamageTaken
    // value. Do so via function "evaluateIncomingArtillery".
    
    private void buildTargetList(IGame game, Entity shooter) {
        targetList = new HashSet<>();
        
        for(Iterator<Entity> enemies = game.getAllEnemyEntities(shooter); enemies.hasNext();) {
            Entity e = enemies.next();
            
            if(!e.isAirborne() && !e.isAirborneVTOLorWIGE()) {
                targetList.add(new HexTarget(e.getPosition(), game.getBoard(), Targetable.TYPE_HEX_ARTILLERY));
                
                // while we're here, consider shooting at a hex between two units within blast radius of both so we can nail
                // the both of them.
            }
        }
    }
    
    /**
     * Put together an indirect artillery "fire plan".
     * @param shooter
     * @param game
     * @param owner
     * @return
     */
    public Vector<EntityAction> calculateIndirectArtilleryPlan(Entity shooter, IGame game, Princess owner) {
        Vector<EntityAction> retval = new Vector<>();
        
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
                for(Iterator<Entity> enemies = game.getAllEnemyEntities(shooter); enemies.hasNext();) {
                    Entity e = enemies.next();
                    
                    double damageValue = calculateDamageValue(damage, e.getPosition(), shooter, game, owner);
                    HexTarget hexTarget = new HexTarget(e.getPosition(), game.getBoard(), Targetable.TYPE_HEX_ARTILLERY);
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
                
                if(!topValuedFireInfos.isEmpty()) {
                    WeaponFireInfo actualFireInfo = topValuedFireInfos.get(Compute.randomInt(topValuedFireInfos.size()));
                    ArtilleryAttackAction aaa = (ArtilleryAttackAction) actualFireInfo.buildWeaponAttackAction();
                    int ammoID = findAmmo(shooter, currentWeapon, game);
                    aaa.setAmmoId(ammoID);
                    retval.add(aaa);
                    owner.sendAmmoChange(shooter.getId(), shooter.getEquipmentNum(currentWeapon), ammoID);
                }
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
