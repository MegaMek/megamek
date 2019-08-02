package megamek.client.bot.princess;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.RangeType;
import megamek.common.WeaponType;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.server.ServerHelper;

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
    public double getMaxDamageAtRange(final MovePath shooterPath, final MovePath targetPath, final int range,
            final boolean useExtremeRange, final boolean useLOSRange) {
        double maxFGDamage = 0;
        double maxInfantryWeaponDamage = 0;
        Entity shooter = shooterPath.getEntity();
        Entity target = targetPath.getEntity();
        IHex targetHex = target.getGame().getBoard().getHex(targetPath.getFinalCoords());
        
        // some preliminary computations 
        // whether the target is an infantry platoon
        boolean targetIsPlatoon = target.hasETypeFlag(Entity.ETYPE_INFANTRY) && !((Infantry) target).isSquad();
        // whether the target is infantry (and not battle armor)
        boolean targetIsActualInfantry = target.hasETypeFlag(Entity.ETYPE_INFANTRY)
                && !target.hasETypeFlag(Entity.ETYPE_BATTLEARMOR);
        boolean shooterIsActualInfantry = shooter.hasETypeFlag(Entity.ETYPE_INFANTRY)
                && !shooter.hasETypeFlag(Entity.ETYPE_BATTLEARMOR);
        // field guns can't fire if the unit in question moved
        boolean fieldGunsDoDamage = (shooterIsActualInfantry && shooterPath.getMpUsed() == 0) || !shooterIsActualInfantry;
        boolean inBuilding = Compute.isInBuilding(target.getGame(), targetPath.getFinalElevation(), targetPath.getFinalCoords());
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
            // field guns can't fire if the infantry unit has done anything other than turning
            } else if (targetIsActualInfantry && fieldGunsDoDamage) {
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
                            .getBuildingAt(targetPath.getFinalCoords()).getDamageReductionFromOutside();
                }
                
                maxFGDamage += damage;
            // field guns can't fire if the infantry unit has done anything other than turning
            } else if (fieldGunsDoDamage) {
            	maxFGDamage += weaponType.getDamage();
            }
        }

        return Math.max(maxFGDamage, maxInfantryWeaponDamage);
    }
}
