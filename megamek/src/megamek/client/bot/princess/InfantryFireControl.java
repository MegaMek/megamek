package megamek.client.bot.princess;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.RangeType;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.logging.LogLevel;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.server.ServerHelper;
import megamek.common.weapons.StopSwarmAttack;

/**
 * This class is intended to help the bot calculate firing plans for infantry
 * units.
 * 
 * @author NickAragua
 *
 */
public class InfantryFireControl extends FireControl {

    public InfantryFireControl(Princess owner) {
        super(owner);
    }

    /**
     * Calculates the maximum damage a unit can do at a given range. Chance to
     * hit is not a factor.
     *
     * @param shooter
     *            The firing unit.
     * @param range
     *            The range to be checked.
     * @param useExtremeRange
     *            Is the extreme range optional rule in effect?
     * @return The most damage done at that range.
     */
    public double getMaxDamageAtRange(final Entity shooter, final MovePath path, final int range,
            final boolean useExtremeRange, final boolean useLOSRange) {
        double maxFGDamage = 0;
        double maxInfantryWeaponDamage = 0;
        Entity target = path.getEntity();
        IHex targetHex = target.getGame().getBoard().getHex(path.getFinalCoords());

        // some preliminary computations 
        // whether the target is an infantry platoon
        boolean targetIsPlatoon = target.hasETypeFlag(Entity.ETYPE_INFANTRY) && !((Infantry) target).isSquad();
        // whether the target is infantry (and not battle armor)
        boolean targetIsActualInfantry = target.hasETypeFlag(Entity.ETYPE_INFANTRY)
                && !target.hasETypeFlag(Entity.ETYPE_BATTLEARMOR);
        boolean inBuilding = Compute.isInBuilding(target.getGame(), path.getFinalElevation(), path.getFinalCoords());
        boolean inOpen = ServerHelper.infantryInOpen(target, targetHex, target.getGame(), targetIsPlatoon, false, false);
        boolean nonInfantryVsMechanized = !shooter.hasETypeFlag(Entity.ETYPE_INFANTRY) && 
                target.hasETypeFlag(Entity.ETYPE_INFANTRY) && ((Infantry) target).isMechanized();     
        
        
        // cycle through my weapons
        for (final Mounted weapon : shooter.getWeaponList()) {
            final WeaponType weaponType = (WeaponType) weapon.getType();

            final int bracket = RangeType.rangeBracket(range, weaponType.getRanges(weapon), useExtremeRange,
                    useLOSRange);

            if (RangeType.RANGE_OUT == bracket) {
                continue;
            }       
            
            // there are three ways this can go:
            // 1. Shooter is infantry using infantry weapon, target is infantry. Use infantry damage. Track damage separately.
            // 2. Shooter is non-infantry, target is infantry in open. Use "directBlowInfantryDamage", multiply by 2.
            // 3. Shooter is non-infantry, target is infantry in building. Use weapon damage, multiply by building dmg reduction.
            // 4. Shooter is non-infantry, target is infantry in "cover". Use "directBlowInfantryDamage".
            // 5. Shooter is non-infantry, target is non-infantry. Use base class.
            
            // case 1
            if (weaponType.hasFlag(WeaponType.F_INFANTRY)) {
                maxInfantryWeaponDamage += ((InfantryWeapon) weaponType).getInfantryDamage()
                        * ((Infantry) shooter).getInternal(Infantry.LOC_INFANTRY);
            } else if (targetIsActualInfantry) {
                double damage = 0;
                
                // if we're outside, use the direct blow infantry damage calculation
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
                            .getBuildingAt(path.getFinalCoords()).getDamageReductionFromOutside();
                }
                
                maxFGDamage += damage;
            } else {
                maxFGDamage += weaponType.getDamage();
            }
        }

        return Math.max(maxFGDamage, maxInfantryWeaponDamage);
    }

    /**
     * Creates a firing plan that fires all weapons with nonzero to hit value at
     * a target ignoring heat, and using actual game rules from different states
     *
     * @param shooter
     *            The unit doing the shooting.
     * @param target
     *            The unit being fired on.
     * @param game
     *            The game being played.
     * @return The {@link FiringPlan} containing all weapons to be fired.
     */
    /*
     * FiringPlan getFullFiringPlan(final Entity shooter, final Targetable
     * target, final Map<Mounted, Double> ammoConservation, final IGame game) {
     * final String METHOD_NAME =
     * "getFullFiringPlan(Entity, Targetable, IGame)"; final NumberFormat DECF =
     * new DecimalFormat("0.000");
     * 
     * final FiringPlan myPlan = new FiringPlan(target);
     * 
     * // Shooting isn't possible if one of us isn't on the board. if ((null ==
     * shooter.getPosition()) || shooter.isOffBoard() ||
     * !game.getBoard().contains(shooter.getPosition())) { owner.log(getClass(),
     * METHOD_NAME, LogLevel.ERROR, "Shooter's position is NULL/Off Board!");
     * return myPlan; } if ((null == target.getPosition()) ||
     * target.isOffBoard() || !game.getBoard().contains(target.getPosition())) {
     * owner.log(getClass(), METHOD_NAME, LogLevel.ERROR,
     * "Target's position is NULL/Off Board!"); return myPlan; }
     * 
     * // cycle through my weapons for (final Mounted weapon :
     * shooter.getWeaponList()) { final double toHitThreshold =
     * ammoConservation.get(weapon); final WeaponFireInfo shoot =
     * buildWeaponFireInfo(shooter, target, weapon, game, false); if
     * ((shoot.getProbabilityToHit() > toHitThreshold)) { myPlan.add(shoot);
     * continue; } owner.log(getClass(), METHOD_NAME, LogLevel.DEBUG,
     * "\nTo Hit Chance (" + DECF.format(shoot.getProbabilityToHit()) + ") for "
     * + weapon.getName() + " is less than threshold (" +
     * DECF.format(toHitThreshold) + ")"); }
     * 
     * // Rank how useful this plan is. calculateUtility(myPlan,
     * calcHeatTolerance(shooter, null), shooter.isAero());
     * 
     * if(shooter.isAero()) { final FiringPlan bombingPlan =
     * this.getDiveBombPlan(shooter, null, target, game,
     * shooter.passedOver(target), false); calculateUtility(bombingPlan,
     * DOES_NOT_TRACK_HEAT, true); // bomb drops never cause heat
     * 
     * if(bombingPlan.getUtility() > myPlan.getUtility()) { return bombingPlan;
     * } }
     * 
     * return myPlan; }
     */
}
