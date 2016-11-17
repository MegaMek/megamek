/**
 * 
 */
package megamek.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.TreeSet;

/**
 * @author Neoancient
 *
 */
public class BattleForceElement {
    static final int[] STANDARD_RANGES = {Entity.BATTLEFORCESHORTRANGE,
        Entity.BATTLEFORCEMEDIUMRANGE, Entity.BATTLEFORCELONGRANGE, Entity.BATTLEFORCEEXTREMERANGE
    };
    static final int[] CAPITAL_RANGES = {Entity.BATTLEFORCE_SHORT_CAPITAL,
        Entity.BATTLEFORCE_MEDIUM_CAPITAL, Entity.BATTLEFORCE_LONG_CAPITAL, Entity.BATTLEFORCE_EXTREME_CAPITAL
    };

    private String name;
    private int size;
    private int walkPoints;
    private int jumpPoints;
    private String movementMode;
    private String movement;
    private int armor;
    private double threshold = -1;
    private int structure;
    private WeaponLocation[] damage;
    private String[] locationNames;
    private int points;
    private double overheat;
    private String specialAbilities;
    
    public BattleForceElement(Entity en) {
        name = en.getShortName();
        size = en.getBattleForceSize();
        walkPoints = (int)en.getBattleForceMovementPoints();
        jumpPoints = (int)en.getBattleForceJumpPoints();
        movementMode = en.getMovementModeAsBattleForceString();
        movement = en.getBattleForceMovement();
        armor = en.getBattleForceArmorPoints();
        if (en instanceof Aero) {
            threshold = armor / 10.0;
        }
        structure = en.getBattleForceStructurePoints();
        damage = new WeaponLocation[en.getNumBattleForceWeaponsLocations()];
        locationNames = new String[damage.length];
        for (int loc = 0; loc < locationNames.length; loc++) {
            damage[loc] = new WeaponLocation();
            locationNames[loc] = en.getBattleForceLocationName(loc);
            if (locationNames[loc].length() > 0) {
                locationNames[loc] += ":";
            }
        }
        computeDamage(en);
        points = en.getBattleForcePoints();
        specialAbilities = en.getBattleForceSpecialAbilities();
    }
    
    public String getName() {
        return name;
    }
    
    public int getSize() {
        return size;
    }
    
    public int getWalkPoints() {
        return walkPoints;
    }
    
    public int getJumpPoints() {
        return jumpPoints;
    }
    
    public String getMovementMode() {
        return movementMode;
    }
    
    public String getMovementAsString() {
        return movement;
    }
    
    public int getArmor() {
        return (int)Math.round(armor);
    }
    
    public int getThreshold() {
        return (int)Math.ceil(threshold);
    }
    
    public int getStructure() {
        return structure;
    }
    
    public String getLocationName(int loc) {
        return locationNames[loc];
    }

    public int getPoints() {
        return points;
    }
    
    public double getOverheat() {
        return overheat;
    }
    
    public String getSpecialAbilities() {
        return specialAbilities;
    }
    
    public void computeDamage(Entity en) {
        double[] baseDamage = new double[4];
        double totalHeat = 0;
        boolean hasTC = en.hasTargComp();
        int[] ranges;
        //Track weapons we've already calculated ammunition for
        HashMap<String,Boolean> ammoForWeapon = new HashMap<>();

        ArrayList<Mounted> weaponsList = en.getWeaponList();

        for (int pos = 0; pos < weaponsList.size(); pos++) {
            Arrays.fill(baseDamage, 0);
            double damageModifier = 1;
            Mounted mount = weaponsList.get(pos);
            if ((mount == null)
                    || (en.getEntityType() == Entity.ETYPE_INFANTRY
                        && mount.getLocation() == Infantry.LOC_INFANTRY)) {
                continue;
            }

            WeaponType weapon = (WeaponType) mount.getType();
            
            ranges = weapon.isCapital()? CAPITAL_RANGES : STANDARD_RANGES;
            
            if ((weapon.getAmmoType() == AmmoType.T_INARC)
                    || (weapon.getAmmoType() == AmmoType.T_NARC)) {
                continue;
            }

            if (weapon.getAtClass() == WeaponType.CLASS_SCREEN) {
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_ARTILLERY)) {
                // Each Artillery weapon is separately accounted for
                continue;
            }
            
            if (weapon.getDamage() == WeaponType.DAMAGE_SPECIAL) {
                continue;
            }

            // Check ammo weapons first since they had a hidden modifier
            if ((weapon.getAmmoType() != AmmoType.T_NA)
                    && !weapon.hasFlag(WeaponType.F_ONESHOT)) {
                if (!ammoForWeapon.containsKey(weapon.getName())) {
                    int weaponsForAmmo = 1;
                    for (int nextPos = 0; nextPos < weaponsList.size(); nextPos++) {
                        if (nextPos == pos) {
                            continue;
                        }
                        
                        Mounted nextWeapon = weaponsList.get(nextPos);
    
                        if (nextWeapon == null) {
                            continue;
                        }
    
                        if (nextWeapon.getType().equals(weapon)) {
                            weaponsForAmmo++;
                        }
    
                    }
                    int ammoCount = 0;
                    // Check if they have enough ammo for all the guns to last at
                    // least 10 rounds
                    for (Mounted ammo : en.getAmmo()) {
    
                        AmmoType at = (AmmoType) ammo.getType();
                        if ((at.getAmmoType() == weapon.getAmmoType())
                                && (at.getRackSize() == weapon.getRackSize())) {
                            // RACs are always fired on 6 shot so that means you
                            // need 6 times the ammo to avoid the ammo damage
                            // modifier
                            if (at.getAmmoType() == AmmoType.T_AC_ROTARY) {
                                ammoCount += at.getShots() / 6;
                            } else if (at.getAmmoType() == AmmoType.T_AC_ULTRA
                                    || at.getAmmoType() == AmmoType.T_AC_ULTRA_THB) {
                                ammoCount += at.getShots() / 2;
                            } else {
                                ammoCount += at.getShots();
                            }
                        }
                    }
    
                    ammoForWeapon.put(weapon.getName(), (ammoCount / weaponsForAmmo) >= 10);
                }
                if (!ammoForWeapon.get(weapon.getName())) {
                    damageModifier *= 0.75;
                }
            }

            for (int r = 0; r < ranges.length; r++) {
                baseDamage[r] = en.getBattleForceWeaponDamage(mount, ranges[r]);
            }

            if (weapon.hasFlag(WeaponType.F_ONESHOT)) {
                damageModifier *= .1;
            }

            // Targetting Computer
            if (hasTC && weapon.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX)
                    && (weapon.getAmmoType() != AmmoType.T_AC_LBX_THB)) {
                damageModifier *= 1.10;
            }
            for (int loc = 0; loc < damage.length; loc++) {
                for (int r = 0; r < ranges.length; r++) {
                    double dam = baseDamage[r] * damageModifier
                            * en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
                    if (weapon.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE) {
                        damage[loc].missiles[r] += dam;
                    } else if (weapon.isSubCapital()) {
                        damage[loc].subCapital[r] += dam;
                    } else if (weapon.isCapital()) {
                        damage[loc].capital[r] += dam;
                    } else {
                        damage[loc].allDamage[r] += dam;
                        if (!(en instanceof Infantry || en instanceof Aero)) {
                            switch (weapon.getAmmoType()) {
                            case AmmoType.T_LRM:
                                damage[loc].lrmDamage[r] += dam;
                                break;
                            case AmmoType.T_SRM:
                                damage[loc].srmDamage[r] += dam;
                                break;
                            case AmmoType.T_AC:
                                damage[loc].acDamage[r] += dam;
                                break;
                            case AmmoType.T_MML:
                                switch (r) {
                                case 0:
                                    damage[loc].srmDamage[r] += dam;
                                    break;
                                case 1:
                                    damage[loc].srmDamage[r] += dam / 2.0;
                                    damage[loc].lrmDamage[r] += dam / 2.0;
                                    break;
                                case 2:
                                    damage[loc].lrmDamage[r] += dam;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!(en instanceof Aero)
                        && (weapon.getAmmoType() == AmmoType.T_LRM
                        || weapon.getAmmoType() == AmmoType.T_NLRM
                        || weapon.getAmmoType() == AmmoType.T_EXLRM
                        || weapon.getAmmoType() == AmmoType.T_TBOLT_5
                        || weapon.getAmmoType() == AmmoType.T_TBOLT_10
                        || weapon.getAmmoType() == AmmoType.T_TBOLT_15
                        || weapon.getAmmoType() == AmmoType.T_TBOLT_20
                        || weapon.getAmmoType() == AmmoType.T_MML
                        || weapon.getAmmoType() == AmmoType.T_MEK_MORTAR)) {
                    damage[loc].indirect += baseDamage[2] * damageModifier
                            * en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
                }
            }
        }
        
        if (en.getEntityType() == Entity.ETYPE_INFANTRY) {
            for (int r = 0; r < STANDARD_RANGES.length; r++) {
                damage[0].allDamage[r] = en.getBattleForceStandardWeaponsDamage(STANDARD_RANGES[r], 0);
            }
        }

        if (en instanceof SmallCraft || en instanceof Jumpship) {
            int capacity = en.getHeatCapacity();
            for (int loc = 0; loc < en.getNumBattleForceWeaponsLocations(); loc++) {
                totalHeat = en.getBattleForceTotalHeatGeneration(loc);
                if (totalHeat > capacity) {
                    for (int r = 0; r < 4; r++) {
                        baseDamage[r] = baseDamage[r] * capacity / totalHeat;
                    }
                }
            }
        } else {
            totalHeat = en.getBattleForceTotalHeatGeneration(false) - 4;
            if (totalHeat > en.getHeatCapacity()) {
                for (int r = 0; r < 4; r++) {
                    baseDamage[r] = baseDamage[r] * en.getHeatCapacity();
                }
            }
        }
    }
    
    public String getBFDamageString(int loc) {
        StringBuilder str = new StringBuilder(locationNames[loc]);
        if (locationNames[loc].length() > 0) {
            str.append("(");
        }
        str.append(String.format("%d/%d/%d/%d",
                (int)Math.ceil(damage[loc].getBFStandardDamage(0) / 10.0),
                (int)Math.ceil(damage[loc].getBFStandardDamage(1) / 10.0),
                (int)Math.ceil(damage[loc].getBFStandardDamage(2) / 10.0),
                (int)Math.ceil(damage[loc].getBFStandardDamage(3) / 10.0)));
        if (damage[loc].hasACDamage()) {
            str.append(";AC").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(damage[loc].getACDamage(0) / 10.0),
                    (int)Math.round(damage[loc].getACDamage(1) / 10.0),
                    (int)Math.round(damage[loc].getACDamage(2) / 10.0),
                    (int)Math.round(damage[loc].getACDamage(3) / 10.0)));
        }
        if (damage[loc].hasSRMDamage()) {
            str.append(";SRM").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(damage[loc].getSRMDamage(0) / 10.0),
                    (int)Math.round(damage[loc].getSRMDamage(1) / 10.0),
                    (int)Math.round(damage[loc].getSRMDamage(2) / 10.0),
                    (int)Math.round(damage[loc].getSRMDamage(3) / 10.0)));
        }
        if (damage[loc].hasLRMDamage()) {
            str.append(";LRM").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(damage[loc].getLRMDamage(0) / 10.0),
                    (int)Math.round(damage[loc].getLRMDamage(1) / 10.0),
                    (int)Math.round(damage[loc].getLRMDamage(2) / 10.0),
                    (int)Math.round(damage[loc].getLRMDamage(3) / 10.0)));
        }
        if (damage[loc].indirect >= 0.5) {
            str.append(";IF").append((int)Math.round(damage[loc].indirect / 10.0));
        }
        if (locationNames[loc].length() > 0) {
            str.append(")");
        }
        return str.toString();
    }
    
    public void writeCsv(BufferedWriter w) throws IOException {
        w.write(name);
        w.write("\t");
        w.write(Integer.toString(size));
        w.write("\t");
        w.write(movement);
        w.write("\t");
        w.write(Integer.toString(armor));
        if (threshold >= 0) {
            w.write("-" + threshold);//TODO: threshold
        }
        w.write("\t");
        w.write(Integer.toString(structure));
        w.write("\t");
        StringJoiner sj = new StringJoiner(", ");
        for (int loc = 0; loc < damage.length; loc++) {
            StringBuilder str = new StringBuilder();
            String damStr = getBFDamageString(loc);
            if (!damStr.equals("0/0/0/0")) {
                str.append(damStr);
                sj.add(str.toString());
            }
        }
        if (sj.length() > 0) {
            w.write(sj.toString());
        } else {
            w.write("0/0/0/0");
        }
        w.write("\t");
        w.write(Integer.toString((int)Math.ceil(overheat)));
        w.write("\t");
        w.write(Integer.toString(points));
        w.write("\t");
        w.write(specialAbilities);
        w.newLine();
    }

    private class WeaponLocation {
        double[] allDamage = new double[4];
        double[] lrmDamage = new double[4];
        double[] srmDamage = new double[4];
        double[] acDamage = new double[4];
        double[] capital = new double[4];
        double[] subCapital = new double[4];
        double[] missiles = new double[4];
        double indirect;
        double overheat;
        
        public double getBFStandardDamage(int index) {
            return allDamage[index] - getLRMDamage(index)
                    - getSRMDamage(index) - getACDamage(index);
        }
        
        public double getAllStandardDamage(int index) {
            return allDamage[index];
        }
        
        public boolean hasLRMDamage() {
            for (int i = 0; i < lrmDamage.length; i++) {
                if (lrmDamage[i] >= 10) {
                    return true;
                }
            }
            return false;
        }
        
        public double getLRMDamage(int index) {
            if (lrmDamage[index] >= 10) {
                return lrmDamage[index];
            }
            return 0;
        }
        
        public boolean hasSRMDamage() {
            for (int i = 0; i < srmDamage.length; i++) {
                if (srmDamage[i] >= 10) {
                    return true;
                }
            }
            return false;
        }
        
        public double getSRMDamage(int index) {
            if (srmDamage[index] >= 10) {
                return srmDamage[index];
            }
            return 0;
        }
        
        public boolean hasACDamage() {
            for (int i = 0; i < acDamage.length; i++) {
                if (acDamage[i] >= 10) {
                    return true;
                }
            }
            return false;
        }
        
        public double getACDamage(int index) {
            if (acDamage[index] >= 10) {
                return acDamage[index];
            }
            return 0;
        }
        
        public double getCapitalDamage(int index) {
            return capital[index];
        }
        
        public double getSubCapital(int index) {
            return subCapital[index];
        }
        
        public double getCapitalMissile(int index) {
            return missiles[index];
        }
        
        public double getOverheat(int index) {
            return overheat;
        }
        
        public double getIndirect() {
            return indirect;
        }
    }

}
