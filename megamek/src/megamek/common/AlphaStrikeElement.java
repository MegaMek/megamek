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
    
    public ASUnitType getUnitType() {
        return asUnitType;
    }
    
    @Override
    protected void setBaseDamageForWeapon(Entity en, Mounted mount, double[] damage, int[] ranges) {
        if (en instanceof BattleArmor) {
            for (int r = 0; r < ranges.length; r++) {
                damage[r] = ((BattleArmor)en).getAlphaStrikeWeaponDamage(mount, ranges[r]);
            }
        } else {
            super.setBaseDamageForWeapon(en, mount, damage, ranges);
        }
    }
    
    public String getASDamageString(int loc) {
        StringBuilder str = new StringBuilder(locationNames[loc]);
        if (locationNames[loc].length() > 0) {
            str.append("(");
        }
        str.append(getASRangeString(weaponLocations[loc].allDamage));
        if (weaponLocations[loc].hasCapitalDamage(0)) {
            str.append(";CAP").append(getASRangeString(weaponLocations[loc].capital));
        }
        if (weaponLocations[loc].hasSubcapitalDamage(0)) {
            str.append(";SCAP").append(getASRangeString(weaponLocations[loc].subCapital));
        }
        if (weaponLocations[loc].hasCapitalMissileDamage(0)) {
            str.append(";CMIS").append(getASRangeString(weaponLocations[loc].missiles));
        }
        if (weaponLocations[loc].hasACDamage(0)) {
            str.append(";AC").append(getASRangeString(weaponLocations[loc].acDamage));
        }
        if (weaponLocations[loc].hasFlakDamage(0)) {
            str.append(";FLAK").append(getASRangeString(weaponLocations[loc].flakDamage));
        }
        if (weaponLocations[loc].hasSRMDamage(0)) {
            str.append(";SRM").append(getASRangeString(weaponLocations[loc].srmDamage));
        }
        if (weaponLocations[loc].hasLRMDamage(0)) {
            str.append(";LRM").append(getASRangeString(weaponLocations[loc].lrmDamage));
        }
        if (weaponLocations[loc].hasTorpDamage(0)) {
            str.append(";TORP").append(getASRangeString(weaponLocations[loc].torpDamage));
        }
        if (weaponLocations[loc].hasIATMDamage(0)) {
            str.append(";IATM").append(getASRangeString(weaponLocations[loc].iatmDamage));
        }
        if (weaponLocations[loc].hasRELDamage(0)) {
            str.append(";REL").append(getASRangeString(weaponLocations[loc].relDamage));
        }
        if (weaponLocations[loc].hasHeatDamage(0)) {
            str.append(";HEAT").append(getASRangeString(weaponLocations[loc].heatDamage));
        }
        if (weaponLocations[loc].indirect > 5) {
            str.append(";IF").append((int)Math.round(weaponLocations[loc].getIndirect() / 10.0));
        } else if (weaponLocations[loc].indirect > 0) {
            str.append(";IF0*");            
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
