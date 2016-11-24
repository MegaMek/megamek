/**
 * 
 */
package megamek.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @author Neoancient
 *
 */
public class AlphaStrikeElement extends BattleForceElement {
    
    // AP weapon mounts have a set damage value.
    static final double AP_MOUNT_DAMAGE = 0.05;

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

    protected ASUnitType asUnitType;
    protected LinkedHashMap<String,Integer> asMovement = new LinkedHashMap<>();
    
    public AlphaStrikeElement(Entity en) {
        super(en);
        asUnitType = ASUnitType.getUnitType(en);
        en.setAlphaStrikeMovement(asMovement);
        if (en.getEntityType() == Entity.ETYPE_INFANTRY) {
            double divisor = ((Infantry)en).getDamageDivisor();
            if (((Infantry)en).isMechanized()) {
                divisor /= 2.0;
            }
            armor *= divisor;
        }
        //Armored Glove counts as an additional AP mounted weapon
        if (en instanceof BattleArmor && en.hasWorkingMisc(MiscType.F_ARMORED_GLOVE)) {
            double apDamage = AP_MOUNT_DAMAGE * (TROOP_FACTOR[Math.min(((BattleArmor)en).getShootingStrength(), 30)] + 0.5);
            weaponLocations[0].addDamage(0, apDamage);
            weaponLocations[0].addDamage(WeaponType.BFCLASS_STANDARD, 0, apDamage);
        }
    }

    protected void initWeaponLocations(Entity en) {
        weaponLocations = new WeaponLocation[en.getNumAlphaStrikeWeaponsLocations()];
        locationNames = new String[weaponLocations.length];
        for (int loc = 0; loc < locationNames.length; loc++) {
            weaponLocations[loc] = new WeaponLocation();
            locationNames[loc] = en.getAlphaStrikeLocationName(loc);
            if (locationNames[loc].length() > 0) {
                locationNames[loc] += ":";
            }
        }
    }
    
    protected static final int[] TROOP_FACTOR = {
        0, 0, 1, 2, 3, 3, 4, 4, 5, 5, 6,
        7, 8, 8, 9, 9, 10, 10, 11, 11, 12,
        13, 14, 15, 16, 16, 17, 17, 17, 18, 18
    };
    
    @Override
    protected double getConvInfantryStandardDamage(int range, Infantry inf) {
        if (inf.getPrimaryWeapon() == null) {
            return inf.getDamagePerTrooper() * TROOP_FACTOR[Math.min(inf.getShootingStrength(), 30)]
                    / 10.0;
        } else {
            return 0;
        }
    }
    
    @Override
    protected double getBattleArmorDamage(WeaponType weapon, int range, BattleArmor ba, boolean apmMount) {
        double dam = 0;
        if (apmMount) {
            if (range == 0) {
                dam = AP_MOUNT_DAMAGE;
            }
        } else {
            dam = weapon.getBattleForceDamage(range);
        }
        return dam * (TROOP_FACTOR[Math.min(ba.getShootingStrength(), 30)] + 0.5);        
    }
    
    public ASUnitType getUnitType() {
        return asUnitType;
    }
    
    public String getASDamageString(int loc) {
        StringBuilder str = new StringBuilder(locationNames[loc]);
        if (locationNames[loc].length() > 0) {
            str.append("(");
        }
        str.append(weaponLocations[loc].formatDamageRounded(true));
        for (int i = WeaponType.BFCLASS_CAPITAL; i < WeaponType.BFCLASS_NUM; i++) {
            if (weaponLocations[loc].hasDamageClass(i)) {
                str.append(";").append(WeaponType.BF_CLASS_NAMES[i])
                    .append(weaponLocations[loc].formatDamageRounded(i, true));
            }
        }
        for (int i = 1; i < WeaponType.BFCLASS_CAPITAL; i++) {
            if (weaponLocations[loc].hasDamageClass(i)) {
                str.append(";").append(WeaponType.BF_CLASS_NAMES[i])
                    .append(weaponLocations[loc].formatDamageRounded(i, true));
            }
        }
        if (weaponLocations[loc].getIF() >= 0.5) {
            str.append(";IF").append((int)Math.round(weaponLocations[loc].getIF()));
        }
        if (locationNames[loc].length() > 0) {
            str.append(")");
        }
        return str.toString();
    }
    
    @Override
    public void writeCsv(BufferedWriter w) throws IOException {
        w.write(name);
        w.write("\t");
        w.write(asUnitType.toString());
        w.write("\t");
        w.write(Integer.toString(size));
        w.write("\t");
        w.write(asMovement.entrySet().stream()
                .map(e -> (e.getKey().equals("k")?"0." + e.getValue():e.getValue())
                        + "\"" + e.getKey())
                .collect(Collectors.joining("/")));
        w.write("\t");
        w.write(Integer.toString((int)Math.round(armor)));
        if (threshold >= 0) {
            w.write("-" + (int)Math.ceil(threshold));//TODO: threshold
        }
        w.write("\t");
        w.write(Integer.toString(structure));
        w.write("\t");
        StringJoiner sj = new StringJoiner(", ");
        for (int loc = 0; loc < weaponLocations.length; loc++) {
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
        for (int loc = 0; loc < weaponLocations.length; loc++) {
            if (weaponLocations[loc].getOverheat() >= 10) {
                sj.add(locationNames[loc] + Math.max(4, (int)Math.round(weaponLocations[loc].getOverheat() / 10.0)));
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
                .filter(spa -> spa.usedByAlphaStrike()
                        && !spa.isDoor())
                .map(spa -> formatSPAString(spa))
                .collect(Collectors.joining(", ")));
        w.newLine();
    }
    
    protected String formatSPAString(BattleForceSPA spa) {
        /* BOMB rating for ASFs and CFs is one less than for BF */
        if (spa.equals(BattleForceSPA.BOMB)
                && (asUnitType.equals(ASUnitType.AF) || asUnitType.equals(ASUnitType.CF))) {
            return spa.toString() + (specialAbilities.get(spa) - 1);
        }
        return super.formatSPAString(spa);
    }
}
