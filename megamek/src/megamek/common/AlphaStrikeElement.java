/*
 *  MegaMek - Copyright (C) 2016 The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Primarily concerned with calculating AlphaStrike values for an undamaged entity, and exporting
 * stats in csv form.

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
                return ((Mech) en).isIndustrial() ? IM : BM;
            } else if (en instanceof Protomech) {
                return PM;
            } else if (en instanceof Tank) {
                return en.isSupportVehicle() ? SV : CV;
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
                return ((Dropship) en).isSpheroid() ? DS : DA;
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
    }

    protected ASUnitType asUnitType;
    
    public AlphaStrikeElement(Entity en) {
        super(en);
        asUnitType = ASUnitType.getUnitType(en);
        if (en.getEntityType() == Entity.ETYPE_INFANTRY) {
            double divisor = ((Infantry) en).calcDamageDivisor();
            if (((Infantry) en).isMechanized()) {
                divisor /= 2.0;
            }
            armor *= divisor;
        }
        //Armored Glove counts as an additional AP mounted weapon
        if (en instanceof BattleArmor && en.hasWorkingMisc(MiscType.F_ARMORED_GLOVE)) {
            double apDamage = AP_MOUNT_DAMAGE * (TROOP_FACTOR[Math.min(((BattleArmor) en).getShootingStrength(), 30)] + 0.5);
            weaponLocations[0].addDamage(0, apDamage);
            weaponLocations[0].addDamage(WeaponType.BFCLASS_STANDARD, 0, apDamage);
        }
    }

    @Override
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
    
    @Override
    protected double locationMultiplier(Entity en, int loc, Mounted mount) {
    	return en.getAlphaStrikeLocationMultiplier(loc, mount.getLocation(), mount.isRearMounted());
    }
    
    @Override
    protected void computeMovement(Entity en) {
    	en.setAlphaStrikeMovement(movement);    	
    }
    
    @Override
    public String getMovementAsString() {
    	return movement.entrySet().stream()
    			.map(e -> (e.getKey().equals("k") ? "0." + e.getValue() : e.getValue())
    					+ "\"" + e.getKey())
    			.collect(Collectors.joining("/"));    	
    }
    
    public int getTargetMoveModifier() {
    	int base = getPrimaryMovementValue();
    	if (base > 34) {
    		return 5;
    	} else if (base > 18) {
    		return 4;
    	} else if (base > 12) {
    		return 3;
    	} else if (base > 8) {
    		return 2;
    	} else if (base > 4) {
    		return 1;
    	}
    	return 0;
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
    
    //TODO: Override calculatePointValue(Entity en)
    
    public String getASDamageString(int loc) {
    	return getASDamageString(loc, true);
    }
    
    public String getASDamageString(int loc, boolean showIfNoDamage) {
    	if (!weaponLocations[loc].hasDamage()) {
    		return "";
    	}
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
            str.append(";IF").append((int) Math.round(weaponLocations[loc].getIF()));
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
        w.write(getMovementAsString());
        w.write("\t");
        w.write(Integer.toString((int) Math.round(armor)));
        if (threshold >= 0) {
            w.write("-" + (int) Math.ceil(threshold));//TODO: threshold
        }
        w.write("\t");
        w.write(Integer.toString(structure));
        w.write("\t");
        StringJoiner sj = new StringJoiner(", ");
        for (int loc = 0; loc < weaponLocations.length; loc++) {
            StringBuilder str = new StringBuilder();
            String damStr = getASDamageString(loc, false);
            if (damStr.length() > 0) {
                str.append(damStr);
                sj.add(str.toString());
            }
        }
        if (sj.length() > 0) {
            w.write(sj.toString());
        } else {
            w.write(rangeBands > 3? "0/0/0/0" : "0/0/0");
        }
        w.write("\t");
        sj = new StringJoiner(", ");
        for (int loc = 0; loc < weaponLocations.length; loc++) {
            if (weaponLocations[loc].getOverheat() >= 1) {
                sj.add(locationNames[loc] + Math.max(4, (int) Math.round(weaponLocations[loc].getOverheat())));
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
                .map(this::formatSPAString)
                .collect(Collectors.joining(", ")));
        w.newLine();
    }
    
    @Override
    protected String formatSPAString(BattleForceSPA spa) {
        /* BOMB rating for ASFs and CFs is one less than for BF */
        if (spa.equals(BattleForceSPA.BOMB)
                && (asUnitType.equals(ASUnitType.AF) || asUnitType.equals(ASUnitType.CF))) {
            return spa.toString() + (specialAbilities.get(spa) - 1);
        }
        if (spa.equals(BattleForceSPA.HT)) {
            return spa
                    + IntStream.range(0, rangeBands)
                    .mapToObj(String::valueOf).collect(Collectors.joining("/"));
        }
        return super.formatSPAString(spa);
    }
}
