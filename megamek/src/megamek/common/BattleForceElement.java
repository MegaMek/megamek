/**
 * 
 */
package megamek.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.ReengineeredLaserWeapon;

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
    
    public enum SpecialAbilities {
    };

    private String name;
    private int size;
    private int walkPoints;
    private int jumpPoints;
    private String movementMode;
    private String movement;
    private double armor;
    private double threshold = -1;
    private int structure;
    private WeaponLocation[] damage;
    private String[] locationNames;
    private int points;
    private EnumMap<BattleForceSPA,Integer> specialAbilities = new EnumMap<>(BattleForceSPA.class);
    
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
        en.addBattleForceSpecialAbilities(specialAbilities);
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
    
    public void computeDamage(Entity en) {
        double[] baseDamage = new double[4];
        double totalHeat = 0;
        boolean hasTC = en.hasTargComp();
        int[] ranges;
        int heat = 0;
        double pointDefense = 0;
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
            
            if (weapon.getAmmoType() == AmmoType.T_INARC) {
               specialAbilities.merge(BattleForceSPA.INARC, 1, Integer::sum);
               continue;
            }
            if (weapon.getAmmoType() == AmmoType.T_NARC) {
                if (weapon.hasFlag(WeaponType.F_BA_WEAPON)) {
                    specialAbilities.merge(BattleForceSPA.CNARC, 1, Integer::sum);
                } else {
                    specialAbilities.merge(BattleForceSPA.SNARC, 1, Integer::sum);
                }
                continue;
            }
            
            if (weapon.getAtClass() == WeaponType.CLASS_SCREEN) {
                specialAbilities.merge(BattleForceSPA.SCR, 1, Integer::sum);
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_AMS)) {
                specialAbilities.put(BattleForceSPA.AMS, null);
                continue;
            }

            if (weapon.hasFlag(WeaponType.F_TAG)) {
                if (weapon.hasFlag(WeaponType.F_C3MBS)) {
                    specialAbilities.merge(BattleForceSPA.C3BSM, 1, Integer::sum);
                    specialAbilities.merge(BattleForceSPA.MHQ, 12, Integer::sum); //count half-tons
                } else if (weapon.hasFlag(WeaponType.F_C3M)) {
                    specialAbilities.merge(BattleForceSPA.C3M, 1, Integer::sum);
                    specialAbilities.merge(BattleForceSPA.MHQ, 10, Integer::sum);
                }
                if (weapon.getShortRange() < 5) {
                    specialAbilities.put(BattleForceSPA.LTAG, null);
                } else {
                    specialAbilities.put(BattleForceSPA.TAG, null);
                }
                continue;
            }
            
            if (weapon.getDamage() == WeaponType.DAMAGE_ARTILLERY) {
                BattleForceSPA artType = null;
                switch (weapon.getAmmoType()) {
                case AmmoType.T_ARROW_IV:
                    if (weapon.getInternalName().substring(0, 1).equals("C")) {
                        artType = BattleForceSPA.ARTAC;
                    } else {
                        artType = BattleForceSPA.ARTAIS;
                    }
                    break;
                case AmmoType.T_LONG_TOM:
                    artType = BattleForceSPA.ARTLT;
                    break;
                case AmmoType.T_SNIPER:
                    artType = BattleForceSPA.ARTS;
                    break;
                case AmmoType.T_THUMPER:
                    artType = BattleForceSPA.ARTT;
                    break;
                case AmmoType.T_LONG_TOM_CANNON:
                    artType = BattleForceSPA.ARTLTC;
                    break;
                case AmmoType.T_SNIPER_CANNON:
                    artType = BattleForceSPA.ARTSC;
                    break;
                case AmmoType.T_THUMPER_CANNON:
                    artType = BattleForceSPA.ARTTC;
                    break;
                case AmmoType.T_CRUISE_MISSILE:
                    switch(weapon.getRackSize()) {
                    case 50:
                        artType = BattleForceSPA.ARTCM5;
                        break;
                    case 70:
                        artType = BattleForceSPA.ARTCM7;
                        break;
                    case 90:
                        artType = BattleForceSPA.ARTCM9;
                        break;
                    case 120:
                        artType = BattleForceSPA.ARTCM12;
                        break;
                    }
                }
                if (artType != null) {
                    specialAbilities.merge(artType, weapon.getRackSize(), Integer::sum);
                }
                continue;
            }
            
            if (weapon.getAmmoType() == AmmoType.T_BA_MICRO_BOMB) {
                specialAbilities.merge(BattleForceSPA.BOMB, 1, Integer::sum);
                continue;
            }
            
            if (weapon.getAmmoType() == AmmoType.T_TASER) {
                if (en instanceof BattleArmor) {
                    specialAbilities.merge(BattleForceSPA.BTA, 1, Integer::sum);
                } else {
                    specialAbilities.merge(BattleForceSPA.MTA, 1, Integer::sum);
                }
                continue;
            }
            
            if (weapon.hasFlag(WeaponType.F_TSEMP)) {
                if (weapon.hasFlag(WeaponType.F_ONESHOT)) {
                    specialAbilities.merge(BattleForceSPA.TSEMPO, 1, Integer::sum);
                } else {
                    specialAbilities.merge(BattleForceSPA.TSEMP, 1, Integer::sum);
                }
                continue;
            }
            
            if (weapon instanceof InfantryAttack) {
                specialAbilities.put(BattleForceSPA.AM, null);
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
            
            if (weapon.hasFlag(WeaponType.F_FLAMER)) {
                heat += 2;
            } else if (weapon.hasFlag(WeaponType.F_PLASMA)) {
                if (weapon.getInternalName().contains("Rifle")) {
                    heat += 3;
                } else {
                    heat += 7;
                }
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
                                break;
                            case AmmoType.T_SRM_TORPEDO:
                            case AmmoType.T_LRM_TORPEDO:
                                damage[loc].torpDamage[r] += dam;
                                break;
                            case AmmoType.T_IATM:
                                damage[loc].iatmDamage[r] += dam;
                                break;
                            case AmmoType.T_ATM:
                                //Fussilade
                                if (weapon.hasFlag(WeaponType.F_PROTO_WEAPON)) {
                                    damage[loc].iatmDamage[r] += dam;
                                }
                                break;
                            case AmmoType.T_AC_LBX:
                            case AmmoType.T_AC_LBX_THB:
                            case AmmoType.T_HAG:
                                damage[loc].flakDamage[r] += dam;
                                break;
                            case AmmoType.T_PLASMA:
                                if (weapon.getRackSize() == 1 && r <= 1) {//rifle
                                    damage[loc].heatDamage[r] += 3;
                                } else if (weapon.getRackSize() == 2 && r <= 2) {//cannon
                                    damage[loc].heatDamage[r] += 7;
                                }
                                break;
                            }
                            if (weapon.hasFlag(WeaponType.F_FLAMER)
                                    && ranges[r] < weapon.getLongRange()) {
                                damage[loc].heatDamage[r] += weapon.getDamage();
                            }
                            if (weapon instanceof ReengineeredLaserWeapon) {
                                damage[loc].relDamage[r] += weapon.getDamage();
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
                        || weapon.getAmmoType() == AmmoType.T_IATM
                        || weapon.getAmmoType() == AmmoType.T_MEK_MORTAR)) {
                    damage[loc].indirect += baseDamage[2] * damageModifier
                            * en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
                }
                if (!(en instanceof Aero)
                        && (weapon.getAmmoType() == AmmoType.T_AC_LBX
                        || weapon.getAmmoType() == AmmoType.T_AC_LBX_THB
                        || weapon.getAmmoType() == AmmoType.T_HAG)) {
                    damage[loc].flak += baseDamage[1] * damageModifier
                            * en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
                }
            }
            if (en instanceof Aero && weapon.getAtClass() == WeaponType.CLASS_POINT_DEFENSE) {
                pointDefense += baseDamage[0] * damageModifier;
            }
        }
        
        if (en.getEntityType() == Entity.ETYPE_INFANTRY) {
            for (int r = 0; r < STANDARD_RANGES.length; r++) {
                damage[0].allDamage[r] = en.getBattleForceStandardWeaponsDamage(STANDARD_RANGES[r], 0);
            }
        }
        
        if (en instanceof Aero && pointDefense > 0) {
            specialAbilities.put(BattleForceSPA.PNT, (int)Math.ceil(pointDefense / 10.0));
        }

        totalHeat = en.getBattleForceTotalHeatGeneration(false) - 4;
        if (totalHeat > en.getHeatCapacity()) {
            double adjustment = en.getHeatCapacity() / totalHeat;
            for (int loc = 0; loc < en.getNumBattleForceWeaponsLocations(); loc++) {
                if (en.isBattleForceRearLocation(loc)) {
                    continue;
                }
                if (en instanceof Mech || en.getEntityType() == Entity.ETYPE_AERO) {
                    damage[loc].overheat = damage[loc].allDamage[1] - damage[loc].allDamage[1] * adjustment;
                }
                for (int r = 0; r < 4; r++) {
                    damage[loc].allDamage[r] *= adjustment;
                    damage[loc].acDamage[r] *= adjustment;
                    damage[loc].srmDamage[r] *= adjustment;
                    damage[loc].lrmDamage[r] *= adjustment;
                    damage[loc].torpDamage[r] *= adjustment;
                    damage[loc].flakDamage[r] *= adjustment;
                    damage[loc].iatmDamage[r] *= adjustment;
                    damage[loc].heatDamage[r] *= adjustment;
                    damage[loc].relDamage[r] *= adjustment;
                    damage[loc].capital[r] *= adjustment;
                    damage[loc].subCapital[r] *= adjustment;
                    damage[loc].missiles[r] *= adjustment;
                    damage[loc].indirect *= adjustment;
                    damage[loc].flak *= adjustment;
                }
            }
        }
        //Rules state that all flamer and plasma weapons on the unit contribute to the heat rating, so we don't separate by arc
        if (heat > 10) {
            specialAbilities.put(BattleForceSPA.HT, 2);
        } else if (heat > 5) {
            specialAbilities.put(BattleForceSPA.HT, 1);
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
        if (damage[loc].hasCapitalDamage()) {
            str.append(";CAP").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(damage[loc].getCapital(0) / 10.0),
                    (int)Math.round(damage[loc].getCapital(1) / 10.0),
                    (int)Math.round(damage[loc].getCapital(2) / 10.0),
                    (int)Math.round(damage[loc].getCapital(3) / 10.0)));
        }
        if (damage[loc].hasSubcapitalDamage()) {
            str.append(";SCAP").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(damage[loc].getSubcapital(0) / 10.0),
                    (int)Math.round(damage[loc].getSubcapital(1) / 10.0),
                    (int)Math.round(damage[loc].getSubcapital(2) / 10.0),
                    (int)Math.round(damage[loc].getSubcapital(3) / 10.0)));
        }
        if (damage[loc].hasCapitalMissileDamage()) {
            str.append(";CMIS").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(damage[loc].getCapitalMissile(0) / 10.0),
                    (int)Math.round(damage[loc].getCapitalMissile(1) / 10.0),
                    (int)Math.round(damage[loc].getCapitalMissile(2) / 10.0),
                    (int)Math.round(damage[loc].getCapitalMissile(3) / 10.0)));
        }
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
        if (damage[loc].hasTorpDamage()) {
            str.append(";TORP").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(damage[loc].getTorpDamage(0) / 10.0),
                    (int)Math.round(damage[loc].getTorpDamage(1) / 10.0),
                    (int)Math.round(damage[loc].getTorpDamage(2) / 10.0),
                    (int)Math.round(damage[loc].getTorpDamage(3) / 10.0)));
        }
        if (damage[loc].indirect >= 5) {
            str.append(";IF").append((int)Math.round(damage[loc].getIndirect() / 10.0));
        }
        if (damage[loc].flak >= 5) {
            str.append(";FLAK").append((int)Math.round(damage[loc].getFlak() / 10.0));
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
        w.write(Integer.toString((int)Math.round(armor)));
        if (threshold >= 0) {
            w.write("-" + (int)Math.ceil(threshold));//TODO: threshold
        }
        w.write("\t");
        w.write(Integer.toString(structure));
        w.write("\t");
        StringJoiner sj = new StringJoiner(", ");
        for (int loc = 0; loc < damage.length; loc++) {
            StringBuilder str = new StringBuilder();
            String damStr = getBFDamageString(loc);
            if (!damStr.startsWith("REAR") && !damStr.contains("(0/0/0/0)")) {
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
        sj = new StringJoiner(", ");
        for (int loc = 0; loc < damage.length; loc++) {
            if (damage[loc].getOverheat() >= 10) {
                sj.add(locationNames[loc] + Math.max(4, (int)Math.round(damage[loc].getOverheat() / 10.0)));
            }
        }
        if (sj.length() > 0) {
            w.write(sj.toString());
        } else {
            w.write("-");
        }
        w.write("\t");
        w.write(Integer.toString(points));
        w.write("\t");
        w.write(specialAbilities.keySet().stream()
                .filter(spa -> spa.usedByBattleForce() && !spa.isDoor())
                .map(spa -> formatSPAString(spa, false))
                .collect(Collectors.joining(", ")));
        w.newLine();
    }
    
    private String formatSPAString(BattleForceSPA spa, boolean alphaStrike) {
        Integer val = specialAbilities.get(spa);
        if (val == null) {
            return spa.toString();
        }
        switch (spa) {
        case AT:
        case MT:
        case PT:
        case ST:
        case VTM:
        case VTH:
            return spa.toString() + val + "D" + specialAbilities.get(spa.getDoor());
        case CT:
            if (val >= 1000) {
                return "CK" + (val / 1000.0) + "D" + specialAbilities.get(spa.getDoor());
            } else {
                return spa.toString() + val + "D" + specialAbilities.get(spa.getDoor());
            }
        case MHQ:
            return spa.toString() + (val / 2);
        default:
            return spa.toString() + val;
        }
    }

    private class WeaponLocation {
        double[] allDamage = new double[4];
        double[] lrmDamage = new double[4];
        double[] srmDamage = new double[4];
        double[] acDamage = new double[4];
        double[] torpDamage = new double[4];
        double[] flakDamage = new double[4];
        double[] heatDamage = new double[4];
        double[] iatmDamage = new double[4];
        double[] relDamage = new double[4];
        double[] capital = new double[4];
        double[] subCapital = new double[4];
        double[] missiles = new double[4];
        double indirect;
        double flak;
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
        
        public boolean hasTorpDamage() {
            for (int i = 0; i < torpDamage.length; i++) {
                if (torpDamage[i] >= 10) {
                    return true;
                }
            }
            return false;
        }
        
        public double getTorpDamage(int index) {
            if (torpDamage[index] >= 10) {
                return torpDamage[index];
            }
            return 0;
        }
        
        public boolean hasFlakDamage() {
            for (int i = 0; i < flakDamage.length; i++) {
                if (flakDamage[i] >= 10) {
                    return true;
                }
            }
            return false;
        }
        
        public double getFlakDamage(int index) {
            if (flakDamage[index] >= 10) {
                return flakDamage[index];
            }
            return 0;
        }
        
        public boolean hasHeatDamage() {
            for (int i = 0; i < heatDamage.length; i++) {
                if (heatDamage[i] >= 10) {
                    return true;
                }
            }
            return false;
        }
        
        public double getHeatDamage(int index) {
            if (heatDamage[index] >= 10) {
                return heatDamage[index];
            }
            return 0;
        }
        
        public boolean hasIATMDamage() {
            for (int i = 0; i < iatmDamage.length; i++) {
                if (iatmDamage[i] >= 10) {
                    return true;
                }
            }
            return false;
        }
        
        public double getIATMDamage(int index) {
            if (iatmDamage[index] >= 10) {
                return iatmDamage[index];
            }
            return 0;
        }
        
        public boolean hasRELDamage() {
            for (int i = 0; i < relDamage.length; i++) {
                if (relDamage[i] >= 10) {
                    return true;
                }
            }
            return false;
        }
        
        public double getRELDamage(int index) {
            if (relDamage[index] >= 10) {
                return relDamage[index];
            }
            return 0;
        }
        
        public boolean hasCapitalDamage() {
            for (int i = 0; i < capital.length; i++) {
                if (capital[i] > 0) {
                    return true;
                }
            }
            return false;
        }
        
        public double getCapital(int index) {
            return capital[index];
        }
        
        public boolean hasSubcapitalDamage() {
            for (int i = 0; i < subCapital.length; i++) {
                if (subCapital[i] > 0) {
                    return true;
                }
            }
            return false;
        }
        
        public double getSubcapital(int index) {
            return subCapital[index];
        }
        
        public boolean hasCapitalMissileDamage() {
            for (int i = 0; i < missiles.length; i++) {
                if (missiles[i] > 0) {
                    return true;
                }
            }
            return false;
        }
        
        public double getCapitalMissile(int index) {
            return missiles[index];
        }
        
        public double getOverheat() {
            return overheat;
        }
        
        public double getIndirect() {
            return indirect;
        }
        
        public double getFlak() {
            return flak;
        }
    }

}
