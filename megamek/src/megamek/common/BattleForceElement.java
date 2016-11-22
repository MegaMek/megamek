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
import java.util.LinkedHashMap;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.MissileWeapon;
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
    
    public enum ASUnitType {
        BM, IM, PM, CV, SV, MS, BA, CI, AF, CF, SC, DS, DA, JS, WS, SS;
        
        static ASUnitType getUnitType(Entity en) {
            if (en instanceof Mech) {
                return ((Mech)en).isIndustrial()? IM : BM;
            } else if (en instanceof Protomech) {
                return PM;
            } else if (en instanceof Tank) {
                return en.isSupportVehicle()?SV : CV;
            } else if (en instanceof BattleArmor) {
                return BA;
            } else if (en instanceof Infantry) {
                return CI;
            } else if (en instanceof SpaceStation) {
                return SS;
            } else if (en instanceof Warship) {
                return WS;
            } else if (en instanceof Jumpship) {
                return JS;
            } else if (en instanceof Dropship) {
                return ((Dropship)en).isSpheroid()? DS : DA;
            } else if (en instanceof SmallCraft) {
                return SC;
            } else if (en instanceof FixedWingSupport) {
                return SV;
            } else if (en instanceof ConvFighter) {
                return CF;
            } else if (en instanceof Aero) {
                return AF;
            }
            return null;
        }
    };

    private String name;
    private ASUnitType asUnitType;
    private int size;
    private int walkPoints;
    private int jumpPoints;
    private String movementMode;
    private String movement;
    private LinkedHashMap<String,Integer> asMovement = new LinkedHashMap<>();
    private double armor;
    private double asArmorDivisor = 1.0; //used to modify conventional infantry armor value for AlphaStrike
    private double threshold = -1;
    private int structure;
    private WeaponLocation[] bfDamage;
    private WeaponLocation[] asDamage; //used for infantry, which calculate damage differently, and warship, which have fewer locations
    private String[] bfLocationNames;
    private String[] asLocationNames;
    private double points;
    private EnumMap<BattleForceSPA,Integer> specialAbilities = new EnumMap<>(BattleForceSPA.class);
    
    public BattleForceElement(Entity en) {
        name = en.getShortName();
        asUnitType = ASUnitType.getUnitType(en);
        size = en.getBattleForceSize();
        walkPoints = (int)en.getBattleForceMovementPoints();
        jumpPoints = (int)en.getBattleForceJumpPoints();
        movementMode = en.getMovementModeAsBattleForceString();
        movement = en.getBattleForceMovement();
        en.setAlphaStrikeMovement(asMovement);
        armor = en.getBattleForceArmorPoints();
        if (en.getEntityType() == Entity.ETYPE_INFANTRY) {
            asArmorDivisor = ((Infantry)en).getDamageDivisor();
            if (((Infantry)en).isMechanized()) {
                asArmorDivisor /= 2.0;
            }
        }
        if (en instanceof Aero) {
            threshold = armor / 10.0;
        }
        structure = en.getBattleForceStructurePoints();
        bfDamage = new WeaponLocation[en.getNumBattleForceWeaponsLocations()];
        bfLocationNames = new String[bfDamage.length];
        for (int loc = 0; loc < bfLocationNames.length; loc++) {
            bfDamage[loc] = new WeaponLocation();
            bfLocationNames[loc] = en.getBattleForceLocationName(loc);
            if (bfLocationNames[loc].length() > 0) {
                bfLocationNames[loc] += ":";
            }
        }
        if (en instanceof Infantry || en instanceof Warship) {
            asDamage = new WeaponLocation[en.getNumAlphaStrikeWeaponsLocations()];
            asLocationNames = new String[asDamage.length];
            for (int loc = 0; loc < asLocationNames.length; loc++) {
                asDamage[loc] = new WeaponLocation();
                asLocationNames[loc] = en.getAlphaStrikeLocationName(loc);
                if (asLocationNames[loc].length() > 0) {
                    asLocationNames[loc] += ":";
                }
            }
        } else {
            asDamage = bfDamage;
            asLocationNames = bfLocationNames;
        }
        computeDamage(en);
        points = en.calculateBattleValue(true, true) / 100.0;
        en.addBattleForceSpecialAbilities(specialAbilities);
    }
    
    public String getName() {
        return name;
    }
    
    public ASUnitType getUnitType() {
        return asUnitType;
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
    
    public int getFinalArmor() {
        return (int)Math.round(armor);
    }
    
    public int getASFinalArmor() {
        return (int)Math.round(armor * asArmorDivisor);
    }
    
    public double getArmor() {
        return armor;
    }
    
    public double getASArmorDivisor() {
        return asArmorDivisor;
    }
    
    public int getFinalThreshold() {
        return (int)Math.ceil(threshold);
    }
    
    public double getThreshold() {
        return threshold;
    }
    
    public int getStructure() {
        return structure;
    }
    
    public String getLocationName(int loc) {
        return bfLocationNames[loc];
    }

    public int getFinalPoints() {
        return Math.max(1, (int)Math.round(points));
    }
    
    public double getPoints() {
        return points;
    }
    
    public void computeDamage(Entity en) {
        double[] baseDamage = new double[4];
        double totalHeat = 0;
        boolean hasTC = en.hasTargComp();
        int[] ranges;
        int heat = 0;
        double pointDefense = 0;
        int bombRacks = 0;
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
                bombRacks++;
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
                    && !weapon.hasFlag(WeaponType.F_ONESHOT)
                    && (!(en instanceof BattleArmor) || weapon instanceof MissileWeapon)) {
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
                if (en instanceof BattleArmor) {
                    if (weapon instanceof MissileWeapon) {
                        damageModifier *= 0.75;
                    }
                } else {
                    damageModifier *= .1;
                }
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
            for (int loc = 0; loc < bfDamage.length; loc++) {
                for (int r = 0; r < ranges.length; r++) {
                    double dam = baseDamage[r] * damageModifier
                            * en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
                    assignDamage(en, ranges, weapon, bfDamage, loc, r, dam);
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
                    bfDamage[loc].indirect += baseDamage[2] * damageModifier
                            * en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
                }
                if (!(en instanceof Aero)
                        && (weapon.getAmmoType() == AmmoType.T_AC_LBX
                        || weapon.getAmmoType() == AmmoType.T_AC_LBX_THB
                        || weapon.getAmmoType() == AmmoType.T_HAG)) {
                    bfDamage[loc].flak += baseDamage[1] * damageModifier
                            * en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
                }
            }
            if (en instanceof Warship) {
                for (int loc = 0; loc < asDamage.length; loc++) {
                    for (int r = 0; r < ranges.length; r++) {
                        double dam = baseDamage[r] * damageModifier
                                * en.getBattleForceLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
                        assignDamage(en, ranges, weapon, asDamage, loc, r, dam);
                    }
                }
            } else if (en instanceof BattleArmor) {
                for (int r = 0; r < ranges.length; r++) {
                    double dam = ((BattleArmor)en).getAlphaStrikeWeaponDamage(mount, r)
                            * damageModifier;
                    assignDamage(en, ranges, weapon, asDamage, 0, r, dam);
                }
            }
            if (en instanceof Aero && weapon.getAtClass() == WeaponType.CLASS_POINT_DEFENSE) {
                pointDefense += baseDamage[0] * damageModifier;
            }
        }
        
        if (en instanceof Infantry) {
            if (!(en instanceof BattleArmor)) {
                for (int r = 0; r < STANDARD_RANGES.length; r++) {
                    bfDamage[0].allDamage[r] = en.getBattleForceStandardWeaponsDamage(STANDARD_RANGES[r], 0);
                }
            }
            asDamage[0].allDamage = ((Infantry)en).getAlphaStrikeInfantryDamage();
        }
        
        if (en instanceof BattleArmor) {
            asDamage[0].allDamage[0] += en.countWorkingMisc(MiscType.F_VIBROCLAW);
            if (bombRacks > 0) {
                specialAbilities.put(BattleForceSPA.BOMB,
                        (bombRacks * ((BattleArmor)en).getShootingStrength()) / 5);
            }
        }
        
        if (en instanceof Aero && pointDefense > 0) {
            specialAbilities.put(BattleForceSPA.PNT, (int)Math.ceil(pointDefense / 10.0));
        }

        totalHeat = en.getBattleForceTotalHeatGeneration(false) - 4;
        int heatCapacity = calcHeatCapacity(en);
        if (totalHeat > heatCapacity) {
            double adjustment = heatCapacity / totalHeat;
            for (int loc = 0; loc < en.getNumBattleForceWeaponsLocations(); loc++) {
                if (en.isBattleForceRearLocation(loc)) {
                    continue;
                }
                if (en instanceof Mech || en.getEntityType() == Entity.ETYPE_AERO) {
                    bfDamage[loc].overheat = bfDamage[loc].allDamage[1] - bfDamage[loc].allDamage[1] * adjustment;
                }
                for (int r = 0; r < 4; r++) {
                    bfDamage[loc].allDamage[r] *= adjustment;
                    bfDamage[loc].acDamage[r] *= adjustment;
                    bfDamage[loc].srmDamage[r] *= adjustment;
                    bfDamage[loc].lrmDamage[r] *= adjustment;
                    bfDamage[loc].torpDamage[r] *= adjustment;
                    bfDamage[loc].flakDamage[r] *= adjustment;
                    bfDamage[loc].iatmDamage[r] *= adjustment;
                    bfDamage[loc].heatDamage[r] *= adjustment;
                    bfDamage[loc].relDamage[r] *= adjustment;
                    bfDamage[loc].capital[r] *= adjustment;
                    bfDamage[loc].subCapital[r] *= adjustment;
                    bfDamage[loc].missiles[r] *= adjustment;
                    bfDamage[loc].indirect *= adjustment;
                    bfDamage[loc].flak *= adjustment;
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

    private void assignDamage(Entity en, int[] ranges, WeaponType weapon, WeaponLocation[] location,
            int loc, int r, double dam) {
        if (weapon.getAtClass() == WeaponType.CLASS_CAPITAL_MISSILE) {
            location[loc].missiles[r] += dam;
        } else if (weapon.isSubCapital()) {
            location[loc].subCapital[r] += dam;
        } else if (weapon.isCapital()) {
            location[loc].capital[r] += dam;
        } else {
            location[loc].allDamage[r] += dam;
            if (!(en instanceof Infantry || en instanceof Aero)) {
                switch (weapon.getAmmoType()) {
                case AmmoType.T_LRM:
                    location[loc].lrmDamage[r] += dam;
                    break;
                case AmmoType.T_SRM:
                    location[loc].srmDamage[r] += dam;
                    break;
                case AmmoType.T_AC:
                    location[loc].acDamage[r] += dam;
                    break;
                case AmmoType.T_MML:
                    switch (r) {
                    case 0:
                        location[loc].srmDamage[r] += dam;
                        break;
                    case 1:
                        location[loc].srmDamage[r] += dam / 2.0;
                        location[loc].lrmDamage[r] += dam / 2.0;
                        break;
                    case 2:
                        location[loc].lrmDamage[r] += dam;
                        break;
                    }
                    break;
                case AmmoType.T_SRM_TORPEDO:
                case AmmoType.T_LRM_TORPEDO:
                    location[loc].torpDamage[r] += dam;
                    break;
                case AmmoType.T_IATM:
                    location[loc].iatmDamage[r] += dam;
                    break;
                case AmmoType.T_ATM:
                    //Fussilade
                    if (weapon.hasFlag(WeaponType.F_PROTO_WEAPON)) {
                        location[loc].iatmDamage[r] += dam;
                    }
                    break;
                case AmmoType.T_AC_LBX:
                case AmmoType.T_AC_LBX_THB:
                case AmmoType.T_HAG:
                    location[loc].flakDamage[r] += dam;
                    break;
                case AmmoType.T_PLASMA:
                    if (weapon.getRackSize() == 1 && r <= 1) {//rifle
                        location[loc].heatDamage[r] += 3;
                    } else if (weapon.getRackSize() == 2 && r <= 2) {//cannon
                        location[loc].heatDamage[r] += 7;
                    }
                    break;
                }
                if (weapon.hasFlag(WeaponType.F_FLAMER)
                        && ranges[r] < weapon.getLongRange()) {
                    location[loc].heatDamage[r] += weapon.getDamage();
                }
                if (weapon instanceof ReengineeredLaserWeapon) {
                    location[loc].relDamage[r] += weapon.getDamage();
                }
            }
        }
    }
    
    public int calcHeatCapacity(Entity en) {
        int capacity = 0;

        for (Mounted mounted : en.getEquipment()) {
            if (mounted.getType() instanceof AmmoType
                    && ((AmmoType)mounted.getType()).getAmmoType() == AmmoType.T_COOLANT_POD) {
                capacity++;
            }
            if (!(mounted.getType() instanceof MiscType)) {
                continue;
            }
            if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                capacity += 1;
            } else if (mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                capacity += 2;
            } else if (mounted.getType().hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE)) {
                capacity += 2;
            } else if (mounted.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                capacity += 1;
            } else if (mounted.getType().hasFlag(MiscType.F_COOLANT_SYSTEM)) {
                capacity += 2;
            } else if (mounted.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                capacity += 1;
            } else if (mounted.getType().hasFlag(MiscType.F_PARTIAL_WING)) {
                capacity += 3;
            }
        }
        return capacity;
    }

    public String getBFDamageString(int loc) {
        StringBuilder str = new StringBuilder(bfLocationNames[loc]);
        if (bfLocationNames[loc].length() > 0) {
            str.append("(");
        }
        str.append(String.format("%d/%d/%d/%d",
                (int)Math.ceil(bfDamage[loc].getBFStandardDamage(0) / 10.0),
                (int)Math.ceil(bfDamage[loc].getBFStandardDamage(1) / 10.0),
                (int)Math.ceil(bfDamage[loc].getBFStandardDamage(2) / 10.0),
                (int)Math.ceil(bfDamage[loc].getBFStandardDamage(3) / 10.0)));
        if (bfDamage[loc].hasCapitalDamage(0)) {
            str.append(";CAP").append(String.format("%d/%d/%d/%d",
                    (int)Math.ceil(bfDamage[loc].getCapital(0) / 10.0),
                    (int)Math.ceil(bfDamage[loc].getCapital(1) / 10.0),
                    (int)Math.ceil(bfDamage[loc].getCapital(2) / 10.0),
                    (int)Math.ceil(bfDamage[loc].getCapital(3) / 10.0)));
        }
        if (bfDamage[loc].hasSubcapitalDamage(0)) {
            str.append(";SCAP").append(String.format("%d/%d/%d/%d",
                    (int)Math.ceil(bfDamage[loc].getSubcapital(0) / 10.0),
                    (int)Math.ceil(bfDamage[loc].getSubcapital(1) / 10.0),
                    (int)Math.ceil(bfDamage[loc].getSubcapital(2) / 10.0),
                    (int)Math.ceil(bfDamage[loc].getSubcapital(3) / 10.0)));
        }
        if (bfDamage[loc].hasCapitalMissileDamage(0)) {
            str.append(";CMIS").append(String.format("%d/%d/%d/%d",
                    (int)Math.ceil(bfDamage[loc].getCapitalMissile(0) / 10.0),
                    (int)Math.ceil(bfDamage[loc].getCapitalMissile(1) / 10.0),
                    (int)Math.ceil(bfDamage[loc].getCapitalMissile(2) / 10.0),
                    (int)Math.ceil(bfDamage[loc].getCapitalMissile(3) / 10.0)));
        }
        if (bfDamage[loc].hasACDamage(10)) {
            str.append(";AC").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(bfDamage[loc].getACDamage(0) / 10.0),
                    (int)Math.round(bfDamage[loc].getACDamage(1) / 10.0),
                    (int)Math.round(bfDamage[loc].getACDamage(2) / 10.0),
                    (int)Math.round(bfDamage[loc].getACDamage(3) / 10.0)));
        }
        if (bfDamage[loc].hasSRMDamage(10)) {
            str.append(";SRM").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(bfDamage[loc].getSRMDamage(0) / 10.0),
                    (int)Math.round(bfDamage[loc].getSRMDamage(1) / 10.0),
                    (int)Math.round(bfDamage[loc].getSRMDamage(2) / 10.0),
                    (int)Math.round(bfDamage[loc].getSRMDamage(3) / 10.0)));
        }
        if (bfDamage[loc].hasLRMDamage(10)) {
            str.append(";LRM").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(bfDamage[loc].getLRMDamage(0) / 10.0),
                    (int)Math.round(bfDamage[loc].getLRMDamage(1) / 10.0),
                    (int)Math.round(bfDamage[loc].getLRMDamage(2) / 10.0),
                    (int)Math.round(bfDamage[loc].getLRMDamage(3) / 10.0)));
        }
        if (bfDamage[loc].hasTorpDamage(10)) {
            str.append(";TORP").append(String.format("%d/%d/%d/%d",
                    (int)Math.round(bfDamage[loc].getTorpDamage(0) / 10.0),
                    (int)Math.round(bfDamage[loc].getTorpDamage(1) / 10.0),
                    (int)Math.round(bfDamage[loc].getTorpDamage(2) / 10.0),
                    (int)Math.round(bfDamage[loc].getTorpDamage(3) / 10.0)));
        }
        if (bfDamage[loc].indirect >= 5) {
            str.append(";IF").append((int)Math.round(bfDamage[loc].getIndirect() / 10.0));
        }
        if (bfDamage[loc].flak >= 5) {
            str.append(";FLAK").append((int)Math.round(bfDamage[loc].getFlak() / 10.0));
        }
        if (bfLocationNames[loc].length() > 0) {
            str.append(")");
        }
        return str.toString();
    }
    
    private String getASRangeString(double[] damage) {
        return IntStream.range(0, damage.length).mapToDouble(i -> damage[i] / 10.0)
                .mapToObj(d -> {
                    if (d > 0.5) {
                        return Integer.toString((int)Math.round(d));
                    } else if (d > 0) {
                        return "0*";
                    } else {
                        return "0";
                    }
                }).collect(Collectors.joining("/"));
    }
    
    public String getASDamageString(int loc) {
        StringBuilder str = new StringBuilder(asLocationNames[loc]);
        if (asLocationNames[loc].length() > 0) {
            str.append("(");
        }
        str.append(getASRangeString(asDamage[loc].allDamage));
        if (asDamage[loc].hasCapitalDamage(0)) {
            str.append(";CAP").append(getASRangeString(asDamage[loc].capital));
        }
        if (asDamage[loc].hasSubcapitalDamage(0)) {
            str.append(";SCAP").append(getASRangeString(asDamage[loc].subCapital));
        }
        if (asDamage[loc].hasCapitalMissileDamage(0)) {
            str.append(";CMIS").append(getASRangeString(asDamage[loc].missiles));
        }
        if (asDamage[loc].hasACDamage(0)) {
            str.append(";AC").append(getASRangeString(asDamage[loc].acDamage));
        }
        if (asDamage[loc].hasFlakDamage(0)) {
            str.append(";FLAK").append(getASRangeString(asDamage[loc].flakDamage));
        }
        if (asDamage[loc].hasSRMDamage(0)) {
            str.append(";SRM").append(getASRangeString(asDamage[loc].srmDamage));
        }
        if (asDamage[loc].hasLRMDamage(0)) {
            str.append(";LRM").append(getASRangeString(asDamage[loc].lrmDamage));
        }
        if (asDamage[loc].hasTorpDamage(0)) {
            str.append(";TORP").append(getASRangeString(asDamage[loc].torpDamage));
        }
        if (asDamage[loc].hasIATMDamage(0)) {
            str.append(";IATM").append(getASRangeString(asDamage[loc].iatmDamage));
        }
        if (asDamage[loc].hasRELDamage(0)) {
            str.append(";REL").append(getASRangeString(asDamage[loc].relDamage));
        }
        if (asDamage[loc].hasHeatDamage(0)) {
            str.append(";HEAT").append(getASRangeString(asDamage[loc].heatDamage));
        }
        if (asDamage[loc].indirect > 5) {
            str.append(";IF").append((int)Math.round(bfDamage[loc].getIndirect() / 10.0));
        } else if (asDamage[loc].indirect > 0) {
            str.append(";IF0*");            
        }
        if (bfLocationNames[loc].length() > 0) {
            str.append(")");
        }
        return str.toString();
    }
    
    public void writeCsv(BufferedWriter w, boolean alphaStrike) throws IOException {
        w.write(name);
        if (alphaStrike) {
            w.write("\t");
            w.write(asUnitType.toString());
        }
        w.write("\t");
        w.write(Integer.toString(size));
        w.write("\t");
        if (alphaStrike) {
            w.write(asMovement.entrySet().stream()
                    .map(e -> (e.getKey().equals("k")?"0." + e.getValue():e.getValue())
                            + "\"" + e.getKey())
                    .collect(Collectors.joining("/")));
        } else {
            w.write(movement);
        }
        w.write("\t");
        w.write(Integer.toString(alphaStrike? getASFinalArmor() : getFinalArmor()));
        if (threshold >= 0) {
            w.write("-" + (int)Math.ceil(threshold));//TODO: threshold
        }
        w.write("\t");
        w.write(Integer.toString(structure));
        w.write("\t");
        StringJoiner sj = new StringJoiner(", ");
        for (int loc = 0; loc < asDamage.length; loc++) {
            StringBuilder str = new StringBuilder();
            String damStr = getASDamageString(loc);
            if (!damStr.contains("(0/0/0/0)")) {
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
        for (int loc = 0; loc < asDamage.length; loc++) {
            if (asDamage[loc].getOverheat() >= 10) {
                sj.add(asLocationNames[loc] + Math.max(4, (int)Math.round(asDamage[loc].getOverheat() / 10.0)));
            }
        }
        if (sj.length() > 0) {
            w.write(sj.toString());
        } else {
            w.write("-");
        }
        w.write("\t");
        w.write(Integer.toString(getFinalPoints()));
        w.write("\t");
        w.write(specialAbilities.keySet().stream()
                .filter(spa -> alphaStrike? spa.usedByAlphaStrike() : spa.usedByBattleForce()
                        && !spa.isDoor())
                .map(spa -> formatSPAString(spa, alphaStrike))
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
        case BOMB:
            if (alphaStrike && (asUnitType.equals(ASUnitType.AF) || asUnitType.equals(ASUnitType.CF))) {
                return spa.toString() + (val - 1);
            }
            //fall through
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
        
        public boolean hasLRMDamage(double min) {
            for (int i = 0; i < lrmDamage.length; i++) {
                if (lrmDamage[i] > min) {
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
        
        public boolean hasSRMDamage(double min) {
            for (int i = 0; i < srmDamage.length; i++) {
                if (srmDamage[i] > min) {
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
        
        public boolean hasACDamage(double min) {
            for (int i = 0; i < acDamage.length; i++) {
                if (acDamage[i] > min) {
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
        
        public boolean hasTorpDamage(double min) {
            for (int i = 0; i < torpDamage.length; i++) {
                if (torpDamage[i] > min) {
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
        
        public boolean hasFlakDamage(double min) {
            for (int i = 0; i < flakDamage.length; i++) {
                if (flakDamage[i] > min) {
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
        
        public boolean hasHeatDamage(double min) {
            for (int i = 0; i < heatDamage.length; i++) {
                if (heatDamage[i] > min) {
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
        
        public boolean hasIATMDamage(double min) {
            for (int i = 0; i < iatmDamage.length; i++) {
                if (iatmDamage[i] > min) {
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
        
        public boolean hasRELDamage(double min) {
            for (int i = 0; i < relDamage.length; i++) {
                if (relDamage[i] > min) {
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
        
        public boolean hasCapitalDamage(double min) {
            for (int i = 0; i < capital.length; i++) {
                if (capital[i] > min) {
                    return true;
                }
            }
            return false;
        }
        
        public double getCapital(int index) {
            return capital[index];
        }
        
        public boolean hasSubcapitalDamage(double min) {
            for (int i = 0; i < subCapital.length; i++) {
                if (subCapital[i] > min) {
                    return true;
                }
            }
            return false;
        }
        
        public double getSubcapital(int index) {
            return subCapital[index];
        }
        
        public boolean hasCapitalMissileDamage(double min) {
            for (int i = 0; i < missiles.length; i++) {
                if (missiles[i] > min) {
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
