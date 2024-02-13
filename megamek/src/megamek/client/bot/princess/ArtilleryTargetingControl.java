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

import java.util.*;

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
     * Wrapper for calculateDamageValue that accounts for leading with artillery shots by accounting for both
     * the original target hex and the computed target hex in damage calculations.
     * @param damage
     * @param hex
     * @param shooter
     * @param game
     * @param owner
     * @return
     */
    public double calculateDamageValue(int damage, HexTarget hex, Entity shooter, Game game, Princess owner) {
        // For leading shots, this will be the computed end point.  Might contain friendlies.
        // For non-leading shots, this is the original target hex.
        double totalDamage = calculateDamageValue(damage, hex.getPosition(), shooter, game, owner);
        if (null != hex.getOriginalTarget()) {
            // For leading shots, the expected damage is based on the units in and around the _current_ location,
            // which is stored as the "getOriginalTarget".
            totalDamage += calculateDamageValue(damage, hex.getOriginalTarget().getPosition(), shooter, game, owner);
        }
        return totalDamage;
    }

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

            // Disincentivize hitting friendlies _strongly_.
            int friendlyMultiplier = -2;

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

    private boolean getAmmoTypeAvailable(Entity shooter, AmmoType.Munitions mtype) {
        boolean available = false;
        for (Mounted weapon: shooter.getWeaponList()){
            if (weapon.getType().hasFlag(WeaponType.F_ARTILLERY)){
                for (Mounted ammo: shooter.getAmmo(weapon)) {
                    if (((AmmoType) ammo.getType()).getMunitionType().contains(mtype)
                            && !weapon.isFired() && ammo.getUsableShotsLeft() > 0) {
                        available = true;
                        break;
                    }
                }
            }
        }
        return available;

    }
    /**
     * Iterates over all artillery weapons and checks if it can make an ADA attack later in the turn.
     * @param shooter who will make the attack
     * @return true if ADA rounds are available for any weapons, false otherwise
     */
    private boolean getADAAvailable(Entity shooter){
        return getAmmoTypeAvailable(shooter, AmmoType.Munitions.M_ADA);
    }

    private boolean getHomingAvailable(Entity shooter) {
        return getAmmoTypeAvailable(shooter, AmmoType.Munitions.M_HOMING);
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
        boolean adaAvailable = getADAAvailable(shooter);
        boolean homingAvailable = getHomingAvailable(shooter);

        for (Iterator<Entity> enemies = game.getAllEnemyEntities(shooter); enemies.hasNext();) {
            Entity e = enemies.next();

            // Given how accurate and long-ranged ADA missiles are, prioritize airborne targets if ADA is available
            if(adaAvailable){
                // We will check these first, but still look at other possible shots.
                if (e.isAirborne() || e.isAirborneVTOLorWIGE() || e.isAirborneAeroOnGroundMap()){
                    targetSet.add(e);
               }
            }
            // Otherwise skip airborne entities, and those off board - we'll handle them later and don't target ignored units
            if (!e.isAirborne()
                    && !e.isAirborneVTOLorWIGE()
                    && !e.isOffBoard()
                    && !owner.getBehaviorSettings().getIgnoredUnitTargets().contains(e.getId())) {

                HexTarget hex = new HexTarget(e.getPosition(), Targetable.TYPE_HEX_ARTILLERY);

                // Add leading hex for standard rounds
                HexTarget leadHex = new HexTarget(Compute.calculateArtilleryLead(game, shooter, e, false),
                        Targetable.TYPE_HEX_ARTILLERY);
                leadHex.setOriginalTarget(hex);

                // Add leading hex for homing rounds
                HexTarget homingHex = new HexTarget(Compute.calculateArtilleryLead(game, shooter, e, homingAvailable),
                        Targetable.TYPE_HEX_ARTILLERY);
                homingHex.setOriginalTarget(hex);

                // Decide which target to use; if all are the same position, use the first hex position
                if (!(leadHex.getPosition().equals(hex.getPosition())
                        && homingHex.getPosition().equals(hex.getPosition()))) {
                    if (leadHex.getPosition().equals(homingHex.getPosition())) {
                        // Either the target is hella fast, or we're not going to use homing
                        hex = leadHex;
                    } else {
                        // Homing is in play, lead farther (probably)
                        hex = homingHex;
                    }
                }
                targetSet.add(hex);

                // while we're here, consider shooting at hexes within "MAX_BLAST_RADIUS"
                // of the final target.
                addHexDonuts(hex.getPosition(), targetSet, game);
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
        FiringPlan TAGPlan = new FiringPlan();

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
        // TODO: allow for counter-battery fire on spotted off-board shooters.
        if (targetSet == null) {
            buildTargetList(shooter, game, owner);
            // If we decided not to shoot this phase, no reason to continue calculating.
            if (targetSet == null) {
                return retval;
            }
        }

        // loop through all weapons on entity
        // each indirect artillery piece randomly picks a target from the priority list
        // by the end of this loop, we either have 0 max damage/0 top valued coordinates, which indicates there's nothing worth shooting at
        // or we have a 1+ top valued coordinates.
        // Track ADA WFIs separately.
        List<WeaponFireInfo> topValuedADAInfos = new ArrayList<>();
        for (Mounted currentWeapon : shooter.getWeaponList()) {
            List<WeaponFireInfo> topValuedFireInfos = new ArrayList<>();
            double maxDamage = 0;
            if (currentWeapon.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                WeaponType wType = (WeaponType) currentWeapon.getType();
                int damage = wType.getRackSize(); // crazy, but rack size appears to correspond to given damage values for arty pieces in tacops

                // Iterate over all loaded Artillery ammo so we can compare various options
                for (final Mounted ammo : shooter.getAmmo(currentWeapon)) {
                    // for each enemy unit, evaluate damage value of firing at its hex.
                    // keep track of top target hexes with the same value and fire at them
                    boolean isADA = ((AmmoType) ammo.getType()).getMunitionType().contains(AmmoType.Munitions.M_ADA);
                    boolean isSmoke = ((AmmoType) ammo.getType()).getMunitionType().contains(AmmoType.Munitions.M_SMOKE);
                    for (Targetable target : targetSet) {
                        WeaponFireInfo wfi;
                        double damageValue = 0.0;
                        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
                            damageValue = damage;
                        } else {
                            if (!(isADA || isSmoke)) {
                                damageValue = calculateDamageValue(damage, (HexTarget) target, shooter, game, owner);
                            } else if (isSmoke) {
                                damageValue = 0;
                            }
                            else {
                                // No ADA attacks except at Entities.
                                continue;
                            }
                        }

                        // ADA attacks should be handled as Direct Fire but we'll calc hits here for comparison.
                        wfi = new WeaponFireInfo(shooter, target, currentWeapon, ammo, game, false, owner);

                        // factor the chance to hit when picking a target - if we've got a spotted hex or an auto-hit hex
                        // we should prefer to hit that over something that may scatter to who knows where
                        if (wfi.getProbabilityToHit() > 0) {
                            damageValue *= wfi.getProbabilityToHit();

                            if (damageValue > maxDamage) {
                                if (((AmmoType) (wfi.getAmmo().getType())).getMunitionType()
                                        .contains(AmmoType.Munitions.M_HOMING)){
                                    wfi.getAmmo().setSwitchedReason(1505);
                                } else {
                                    wfi.getAmmo().setSwitchedReason(1503);
                                }
                                if (isADA) {
                                    topValuedADAInfos.clear();
                                    maxDamage = damage;  // Actual expected damage will be higher
                                    topValuedADAInfos.add(wfi);
                                } else {
                                    topValuedFireInfos.clear();
                                    maxDamage = damageValue;
                                    topValuedFireInfos.add(wfi);
                                }
                            } else if ((damageValue == maxDamage) && (damageValue > 0)) {
                                if (((AmmoType) wfi.getAmmo().getType()).getMunitionType().contains(AmmoType.Munitions.M_ADA)){
                                    topValuedADAInfos.add(wfi);
                                } else {
                                    topValuedFireInfos.add(wfi);
                                }
                            }
                        }
                    }
                }
                // this section is long and obnoxious:
                // Pick a random fire info out of the ones with the top damage level
                // Use that to create an artillery attack action, set the action's ammo
                // then set the fire info's attack action to the created attack action
                // add the fire info to the firing plan
                if (!topValuedFireInfos.isEmpty()) {
                    WeaponFireInfo actualFireInfo;
                    if (topValuedFireInfos.size() == 1) {
                        actualFireInfo = topValuedFireInfos.get(0);
                    } else {
                        actualFireInfo = topValuedFireInfos.get(Compute.randomInt(topValuedFireInfos.size()));
                        if (actualFireInfo.getAmmo() != actualFireInfo.getWeapon().getLinked()) {
                            // Announce why we switched
                            actualFireInfo.getAmmo().setSwitchedReason(1507);
                        }
                    }
                    ArtilleryAttackAction aaa = (ArtilleryAttackAction) actualFireInfo.buildWeaponAttackAction();
                    HelperAmmo ammo = findAmmo(shooter, actualFireInfo.getWeapon(), actualFireInfo.getAmmo());

                    if (ammo.equipmentNum > NO_AMMO) {
                        //This can happen if princess is towing ammo trailers, which she really shouldn't be doing...
                        aaa.setAmmoId(ammo.equipmentNum);
                        aaa.setAmmoMunitionType(ammo.munitionType);
                        aaa.setAmmoCarrier(shooter.getId());
                        actualFireInfo.setAction(aaa);
                        retval.add(actualFireInfo);
                        retval.setUtility(retval.getUtility() + maxDamage);
                        owner.sendAmmoChange(
                                shooter.getId(),
                                shooter.getEquipmentNum(actualFireInfo.getWeapon()),
                                ammo.equipmentNum,
                                actualFireInfo.getAmmo().getSwitchedReason());
                    }
                }
            } else if (currentWeapon.getType().hasFlag(WeaponType.F_TAG)) {
                WeaponFireInfo tagInfo = getTAGInfo(currentWeapon, shooter, game, owner);

                if (tagInfo != null) {
                    TAGPlan.add(tagInfo);
                    TAGPlan.setUtility(retval.getUtility() + tagInfo.getProbabilityToHit());
                }
            }
        }

        // Clear all artillery attacks if we have valid ADA attacks that do damage, but keep any TAG attacks.
        if (!topValuedADAInfos.isEmpty()) {
            if (topValuedADAInfos.get(0).getExpectedDamage() > 0) {
                retval = TAGPlan;
            }
        } else {
            for (WeaponFireInfo tagInfo : TAGPlan) {
                retval.add(tagInfo);
                retval.setUtility(retval.getUtility() + tagInfo.getProbabilityToHit());
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
            WeaponFireInfo wfi = new WeaponFireInfo(shooter, target, weapon, null, game, false, owner);
            if (wfi.getProbabilityToHit() > hitOdds) {
                hitOdds = wfi.getProbabilityToHit();
                retval = wfi;
            }
        }

        return retval;
    }

    private static class HelperAmmo {
        public int equipmentNum;
        public EnumSet<AmmoType.Munitions> munitionType = EnumSet.noneOf(AmmoType.Munitions.class);

        public HelperAmmo(int equipmentNum, EnumSet<AmmoType.Munitions> munitionType) {
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
    private HelperAmmo findAmmo(Entity shooter, Mounted currentWeapon, Mounted preferredAmmo) {
        int ammoEquipmentNum = NO_AMMO;
        EnumSet<AmmoType.Munitions> ammoMunitionType = EnumSet.noneOf(AmmoType.Munitions.class);

        if (preferredAmmo != null && preferredAmmo.isAmmoUsable() && AmmoType.isAmmoValid(preferredAmmo, (WeaponType) currentWeapon.getType())){
            // Use the ammo we used for calculations.
            ammoEquipmentNum = shooter.getEquipmentNum(preferredAmmo);
            ammoMunitionType = ((AmmoType) preferredAmmo.getType()).getMunitionType();
        } else {
            // simply grab the first valid ammo and let 'er rip.
            for (Mounted ammo : shooter.getAmmo()) {
                if (!ammo.isAmmoUsable() || !AmmoType.isAmmoValid(ammo, (WeaponType) currentWeapon.getType())) {
                    continue;
                }

                ammoEquipmentNum = shooter.getEquipmentNum(ammo);
                ammoMunitionType = ((AmmoType) ammo.getType()).getMunitionType();
                break;

                // TODO: Attempt to select homing ammo if the target is tagged.
                // To do so, check ammoType.getMunitionType().contains(AmmoType.Munitions.M_HOMING
            }
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

            if ((aaa.getTurnsTilHit() == 0) && (aaa.getTarget(operator.getGame()) != null)) {
                // damage for artillery weapons is, for some reason, derived from the weapon type's rack size
                int damage = 0;
                Mounted weapon = aaa.getEntity(operator.getGame()).getEquipment(aaa.getWeaponId());
                if (null == weapon) {
                    // The weaponId couldn't get us a weapon; probably a bomb Arrow IV dropped on a prior turn.
                    BombType btype = BombType.createBombByType(BombType.getBombTypeFromName("Arrow IV Missile"));
                    damage = btype.getRackSize();
                } else {
                    if (weapon.getType() instanceof BombType) {
                        damage = (weapon.getExplosionDamage());
                    } else {
                        damage = ((WeaponType) weapon.getType()).getRackSize();
                    }
                }

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
