/*
 * MegaAero - Copyright (C) 2007 Jay Lawson This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * Created on Jun 17, 2007
 */
package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import megamek.common.weapons.BayWeapon;

/**
 * @author Jay Lawson
 */
public class Dropship extends SmallCraft implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1528728632696989565L;
    // escape pods and lifeboats
    int escapePods = 0;
    int lifeBoats = 0;

    // what needs to go here?
    // loading and unloading of units?
    private boolean dockCollarDamaged = false;

    public boolean isDockCollarDamaged() {
        return dockCollarDamaged;
    }

    public void setDamageDockCollar(boolean b) {
        dockCollarDamaged = b;
    }

    public void setEscapePods(int n) {
        escapePods = n;
    }

    public int getEscapePods() {
        return escapePods;
    }

    public void setLifeBoats(int n) {
        lifeBoats = n;
    }

    public int getLifeBoats() {
        return lifeBoats;
    }

    public int getFuelPerTon() {

        int points = 80;

        if (weight >= 40000) {
            points = 10;
            return points;
        } else if (weight >= 20000) {
            points = 20;
            return points;
        } else if (weight >= 3000) {
            points = 30;
            return points;
        } else if (weight >= 1900) {
            points = 40;
            return points;
        } else if (weight >= 1200) {
            points = 50;
            return points;
        } else if (weight >= 800) {
            points = 60;
            return points;
        } else if (weight >= 400) {
            points = 70;
            return points;
        }

        return points;
    }

    @Override
    public double getCost(boolean ignoreAmmo) {

        double cost = 0;

        // add in controls
        // bridge
        cost += 200000 + 10 * weight;
        // computer
        cost += 200000;
        // life support
        cost += 5000 * (getNCrew() + getNPassenger());
        // sensors
        cost += 80000;
        // fcs
        cost += 100000;
        // gunnery/control systems
        cost += 10000 * getArcswGuns();

        // structural integrity
        cost += 100000 * getSI();

        // additional flight systems (attitude thruster and landing gear)
        cost += 25000 + 10 * getWeight();

        // docking collar
        cost += 10000;

        // engine
        double engineMultiplier = 0.065;
        if (isClan()) {
            engineMultiplier = 0.061;
        }
        double engineWeight = getOriginalWalkMP() * weight * engineMultiplier;
        cost += engineWeight * 1000;
        // drive unit
        cost += 500 * getOriginalWalkMP() * weight / 100.0;

        // fuel tanks
        cost += 200 * getFuel() / getFuelPerTon();

        // armor
        cost += getArmorWeight() * EquipmentType.getArmorCost(armorType);

        // heat sinks
        int sinkCost = 2000 + 4000 * getHeatType();// == HEAT_DOUBLE ? 6000:
                                                   // 2000;
        cost += sinkCost * getHeatSinks();

        // weapons
        cost += getWeaponsAndEquipmentCost(ignoreAmmo);

        // get bays
        int baydoors = 0;
        int bayCost = 0;
        for (Bay next : getTransportBays()) {
            baydoors += next.getDoors();
            if ((next instanceof MechBay) || (next instanceof ASFBay) || (next instanceof SmallCraftBay)) {
                bayCost += 20000 * next.totalSpace;
            }
            if ((next instanceof LightVehicleBay) || (next instanceof HeavyVehicleBay)) {
                bayCost += 10000 * next.totalSpace;
            }
        }

        cost += bayCost + baydoors * 1000;

        // life boats and escape pods
        cost += 5000 * (getLifeBoats() + getEscapePods());

        double weightMultiplier = 36.0;
        if (isSpheroid()) {
            weightMultiplier = 28.0;
        }

        return Math.round(cost * weightMultiplier);

    }

    private String getArcName(int loc) {
        if (loc < locations()) {
            return getLocationName(loc);
        } else {
            return getLocationName(loc - 3) + " (R)";
        }
    }

    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {

        bvText = new StringBuffer("<HTML><BODY><CENTER><b>Battle Value Calculations For ");

        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append("<b>Defensive Battle Rating Calculation:</b>");
        bvText.append(nl);

        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        int modularArmor = 0;
        for (Mounted mounted : getEquipment()) {
            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        // no use for armor mods right now but in case we need one later
        double armorMod = 1.0;

        bvText.append(startTable);
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Armor Factor x 2.5 x");
        bvText.append(armorMod);
        bvText.append(endColumn);
        bvText.append(startColumn);

        dbv += (getTotalArmor() + modularArmor);

        bvText.append(dbv);
        bvText.append(" x 2.5 x ");
        bvText.append(armorMod);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");

        dbv *= 2.5 * armorMod;

        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total SI x 2");
        bvText.append(endColumn);
        bvText.append(startColumn);

        double dbvSI = getSI() * 2.0;
        dbv += dbvSI;

        bvText.append(getSI());
        bvText.append(" x 2");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("= ");
        bvText.append(dbvSI);
        bvText.append(endColumn);
        bvText.append(endRow);

        // add defensive equipment
        double amsBV = 0;
        double amsAmmoBV = 0;
        double screenBV = 0;
        double screenAmmoBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }
            if (etype instanceof BayWeapon) {
                continue;
            }
            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS)))) {
                amsBV += etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS)) {
                // we need to deal with cases where ammo is loaded in multi-ton
                // increments
                // (on dropships and jumpships) - lets take the ratio of shots
                // to shots left
                double ratio = mounted.getShotsLeft() / ((AmmoType) etype).getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since
                // we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }
                amsAmmoBV += ratio * etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(ratio * etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)) {
                // we need to deal with cases where ammo is loaded in multi-ton
                // increments
                // (on dropships and jumpships) - lets take the ratio of shots
                // to shots left
                double ratio = mounted.getShotsLeft() / ((AmmoType) etype).getShots();

                // if the ratio is less than one, we will treat as a full ton
                // since
                // we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }
                screenAmmoBV += ratio * etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(ratio * etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            } else if ((etype instanceof WeaponType) && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN)) {
                screenBV += etype.getBV(this);
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(etype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+");
                bvText.append(etype.getBV(this));
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        if (amsBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total AMS BV:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(amsBV);
            dbv += amsBV;
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (screenBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total Screen BV:");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(screenBV);
            dbv += screenBV;
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (amsAmmoBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total AMS Ammo BV (to a maximum of AMS BV):");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(Math.min(amsBV, amsAmmoBV));
            dbv += Math.min(amsBV, amsAmmoBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        if (screenAmmoBV > 0) {
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Total Screen Ammo BV (to a maximum of Screen BV):");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append(Math.min(screenBV, screenAmmoBV));
            dbv += Math.min(screenBV, screenAmmoBV);
            bvText.append(endColumn);
            bvText.append(endRow);
        }

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        // unit type multiplier
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Multiply by Unit type Modifier");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(getBVTypeModifier());
        dbv *= getBVTypeModifier();
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("x" + getBVTypeModifier());
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(dbv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("<b>Offensive Battle Rating Calculation:</b>");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(endRow);

        // calculate heat efficiency
        int aeroHeatEfficiency = getHeatCapacity();

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Base Heat Efficiency ");

        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(aeroHeatEfficiency);

        bvText.append(endColumn);
        bvText.append(endRow);

        // get arc BV and heat
        // I am going to redo how I do this to cycle through locations, so I can
        // spit it out
        // to bvText. It will require a little trickery for rear LS/RS since
        // those technically are not
        // locations
        double[] arcBVs = new double[locations() + 2];
        double[] arcHeats = new double[locations() + 2];
        double[] ammoBVs = new double[locations() + 2];
        
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Arc BV and Heat");
        bvText.append(endColumn);
        bvText.append(endRow);
      
        // cycle through locations
        for (int loc = 0; loc < (locations() + 2); loc++) {
            int l = loc;
            boolean isRear = (loc >= locations());
            String rear = "";
            if (isRear) {
                l = l - 3;
                rear = " (R)";
            }
            this.getLocationName(l);

            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<i>" + getLocationName(l) + rear + "</i>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<i>BV</i>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<i>Heat</i>");
            bvText.append(endColumn);
            bvText.append(endRow);
            double arcBV = 0.0;
            double arcHeat = 0.0;
            double arcAmmoBV = 0.0;
            TreeMap<String, Double> weaponsForExcessiveAmmo = new TreeMap<String, Double>();
            for (Mounted mounted : getTotalWeaponList()) {
                if (mounted.getLocation() != l) {
                    continue;
                }
                if (mounted.isRearMounted() != isRear) {
                    continue;
                }
                // only count non-damaged equipment
                if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                    continue;
                }
                WeaponType wtype = (WeaponType) mounted.getType();
                double weaponHeat = wtype.getHeat();
                double dBV = wtype.getBV(this);
                // skip bays
                if (wtype instanceof BayWeapon) {
                    continue;
                }
                // don't count defensive weapons
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                // don't count screen launchers, they are defensive
                if (wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                    continue;
                }
                // double heat for ultras
                if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                    weaponHeat *= 2;
                }
                // Six times heat for RAC
                if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                    weaponHeat *= 6;
                }
                // calc MG Array here:
                if (wtype.hasFlag(WeaponType.F_MGA)) {
                    double mgaBV = 0;
                    for (Mounted possibleMG : getTotalWeaponList()) {
                        if (possibleMG.getType().hasFlag(WeaponType.F_MG) && (possibleMG.getLocation() == mounted.getLocation())) {
                            mgaBV += possibleMG.getType().getBV(this);
                        }
                    }
                    dBV = mgaBV * 0.67;
                }
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if (hasTargComp()) {
                        dBV *= 1.25;
                    }
                }
                // artemis bumps up the value
                if (mounted.getLinkedBy() != null) {
                    Mounted mLinker = mounted.getLinkedBy();
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        dBV *= 1.2;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                        dBV *= 1.3;
                    }
                    if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                        dBV *= 1.15;
                    }
                }
                // add up BV of ammo-using weapons for each type of weapon,
                // to compare with ammo BV later for excessive ammo BV rule
                if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA)) || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype.getAmmoType() == AmmoType.T_NA))) {
                    String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                    if (!weaponsForExcessiveAmmo.containsKey(key)) {
                        weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                    } else {
                        weaponsForExcessiveAmmo.put(key, wtype.getBV(this) + weaponsForExcessiveAmmo.get(key));
                    }
                }
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(wtype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + dBV);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + weaponHeat);
                bvText.append(endColumn);
                bvText.append(endRow);
                arcBV += dBV;
                arcHeat += weaponHeat;
            }
            //now ammo
            Map<String, Double> ammo = new HashMap<String, Double>();
            ArrayList<String> keys = new ArrayList<String>();
            for (Mounted mounted : getAmmo()) {               
                if (mounted.getLocation() != l) {
                    continue;
                }
                if (mounted.isRearMounted() != isRear) {
                    continue;
                }
                AmmoType atype = (AmmoType) mounted.getType();
                // we need to deal with cases where ammo is loaded in multi-ton
                // increments
                // (on dropships and jumpships) - lets take the ratio of shots to
                // shots left
                double ratio = mounted.getShotsLeft() / atype.getShots();

                // if the ratio is less than one, we will treat as a full ton since
                // we don't make that adjustment elsewhere
                if (ratio < 1.0) {
                    ratio = 1.0;
                }

                // don't count depleted ammo
                if (mounted.getShotsLeft() == 0) {
                    continue;
                }

                // don't count AMS, it's defensive
                if (atype.getAmmoType() == AmmoType.T_AMS) {
                    continue;
                }
                // don't count screen launchers, they are defensive
                if (atype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) {
                    continue;
                }

                // don't count oneshot ammo, it's considered part of the launcher.
                if (mounted.getLocation() == Entity.LOC_NONE) {
                    // assumption: ammo without a location is for a oneshot weapon
                    continue;
                }
                double abv = ratio * atype.getBV(this); 
                String key = atype.getAmmoType() + ":" + atype.getRackSize();
                String key2 = atype.getName() + ";" + key;
                if (!keys.contains(key2)) {
                    keys.add(key2);
                }
                if (!ammo.containsKey(key)) {
                    ammo.put(key, abv);
                } else {
                    ammo.put(key, abv + ammo.get(key));
                }
            }
            //now cycle through ammo hash and deal with excessive ammo issues
            for (String fullkey : keys) {
                String[] k = fullkey.split(";");
                String key = k[1];
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(k[0]);
                bvText.append(endColumn);
                bvText.append(startColumn);
                if (weaponsForExcessiveAmmo.get(key) != null) {
                    if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                        bvText.append("+" + weaponsForExcessiveAmmo.get(key) + "*");
                        arcAmmoBV += weaponsForExcessiveAmmo.get(key);
                    } else {
                        bvText.append("+" + ammo.get(key));
                        arcAmmoBV += ammo.get(key);
                    }
                }
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("");
                bvText.append(endColumn);
                bvText.append(endRow);
            }
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<b>" + getLocationName(l) + rear + " Weapon Totals</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<b>" + arcBV + "</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<b>" + arcHeat + "</b>");
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<b>" + getLocationName(l) + rear + " Ammo Totals</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("<b>" + arcAmmoBV + "</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("");
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("<b>" + getLocationName(l) + rear + " Totals</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            double tempBV = arcBV + arcAmmoBV;
            bvText.append("<b>" + tempBV + "</b>");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("");
            bvText.append(endColumn);
            bvText.append(endRow);
            arcBVs[loc] = arcBV;
            arcHeats[loc] = arcHeat;
            ammoBVs[loc] = arcAmmoBV;
        }

        double weaponBV = 0.0;
        // ok, now lets loop through the arcs and find the highest value BV arc
        int highArc = Integer.MIN_VALUE;
        int adjArcH = Integer.MIN_VALUE;
        int adjArcL = Integer.MIN_VALUE;
        double adjArcHMult = 1.0;
        double adjArcLMult = 0.5;
        double highBV = 0.0;
        double heatUsed = 0.0;
        for (int loc = 0; loc < arcBVs.length; loc++) {
            if (arcBVs[loc] > highBV) {
                highArc = loc;
                highBV = arcBVs[loc];
            }
        }
        // now lets identify the adjacent arcs
        if (highArc > Integer.MIN_VALUE) {
            heatUsed += arcHeats[highArc];
            // now get the BV and heat for the two adjacent arcs
            int adjArcCW = getAdjacentLocCW(highArc);
            int adjArcCCW = getAdjacentLocCCW(highArc);
            double adjArcCWBV = 0.0;
            double adjArcCWHeat = 0.0;
            if (adjArcCW > Integer.MIN_VALUE) {
                adjArcCWBV = arcBVs[adjArcCW];
                adjArcCWHeat = arcHeats[adjArcCW];
            }
            double adjArcCCWBV = 0.0;
            double adjArcCCWHeat = 0.0;
            if (adjArcCCW > Integer.MIN_VALUE) {
                adjArcCCWBV = arcBVs[adjArcCCW];
                adjArcCCWHeat = arcHeats[adjArcCCW];
            }
            if (adjArcCWBV > adjArcCCWBV) {
                adjArcH = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcHMult = 0.5;
                }
                heatUsed += adjArcCWHeat;
                adjArcL = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcLMult = 0.25;
                }
                heatUsed += adjArcCCWHeat;
            } else {
                adjArcH = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcHMult = 0.5;
                }
                heatUsed += adjArcCCWHeat;
                adjArcL = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcLMult = 0.25;
                }
                heatUsed += adjArcCWHeat;
            }
        }

        //ok now add in ammo to arc bvs
        for(int i=0; i<arcBVs.length; i++) {
            arcBVs[i] = arcBVs[i]+ammoBVs[i];
        }
        
        // ok now lets go in and add the arcs
        double totalHeat = 0.0;
        if (highArc > Integer.MIN_VALUE) {
            // ok now add the BV from this arc and reset to zero
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Highest BV Arc (" + getArcName(highArc) + ")" + arcBVs[highArc] + "*1.0");
            bvText.append(endColumn);
            bvText.append(startColumn);
            bvText.append("+" + arcBVs[highArc]);
            bvText.append(endColumn);
            bvText.append(endRow);
            bvText.append(startColumn);
            totalHeat = arcHeats[highArc];
            bvText.append("Total Heat: " + totalHeat);
            bvText.append(endColumn);
            bvText.append(endRow);
            weaponBV += arcBVs[highArc];
            arcBVs[highArc] = 0.0;
            if (adjArcH > Integer.MIN_VALUE) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append("Adjacent High BV Arc (" + getArcName(adjArcH) + ") " + arcBVs[adjArcH] + "*" + adjArcHMult);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + arcBVs[adjArcH] * adjArcHMult);
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                totalHeat += arcHeats[adjArcH];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvText.append("Total Heat: " + totalHeat + over);
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += adjArcHMult * arcBVs[adjArcH];
                arcBVs[adjArcH] = 0.0;
            }
            if (adjArcL > Integer.MIN_VALUE) {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append("Adjacent Low BV Arc (" + getArcName(adjArcL) + ") " + arcBVs[adjArcL] + "*" + adjArcLMult);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + adjArcLMult * arcBVs[adjArcL]);
                bvText.append(endColumn);
                bvText.append(endRow);
                bvText.append(startRow);
                bvText.append(startColumn);
                totalHeat += arcHeats[adjArcL];
                String over = "";
                if (totalHeat > aeroHeatEfficiency) {
                    over = " (Greater than heat efficiency)";
                }
                bvText.append("Total Heat: " + totalHeat + over);
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += adjArcLMult * arcBVs[adjArcL];
                arcBVs[adjArcL] = 0.0;
            }
            // ok now we can cycle through the rest and add 25%
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append("Remaining Arcs");
            bvText.append(endColumn);
            bvText.append(endRow);
            for (int loc = 0; loc < arcBVs.length; loc++) {
                if (arcBVs[loc] <= 0) {
                    continue;
                }
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(getArcName(loc) + " " + arcBVs[loc] + "*0.25");
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append("+" + 0.25 * arcBVs[loc]);
                bvText.append(endColumn);
                bvText.append(endRow);
                weaponBV += (0.25 * arcBVs[loc]);
            }
        }

        bvText.append("Total Weapons BV Adjusted For Heat:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(weaponBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        // add offensive misc. equipment BV
        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_TARGCOMP)) {
                continue;
            }
            double bv = mtype.getBV(this);
            if (bv > 0) {
                bvText.append(startRow);
                bvText.append(startColumn);

                bvText.append(mtype.getName());
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(endColumn);
                bvText.append(startColumn);
                bvText.append(bv);
                bvText.append(endColumn);
                bvText.append(endRow);
            }
            oEquipmentBV += bv;
        }
        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Total Misc Offensive Equipment BV: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(oEquipmentBV);
        bvText.append(endColumn);
        bvText.append(endRow);
        weaponBV += oEquipmentBV;

        // adjust

        // adjust further for speed factor
        double speedFactor = Math.pow(1 + (((double) getRunMP() - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Final Speed Factor: ");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(endRow);

        obv = weaponBV * speedFactor;

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Weapons BV * Speed Factor ");
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(weaponBV);
        bvText.append(" * ");
        bvText.append(speedFactor);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);

        bvText.append("Offensive BV + Defensive BV");
        bvText.append(endColumn);
        bvText.append(startColumn);

        double finalBV = dbv + obv;

        bvText.append(dbv);
        bvText.append(" + ");
        bvText.append(obv);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(" = ");
        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        finalBV = Math.round(finalBV);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Final BV");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);

        bvText.append(finalBV);
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");

        // we get extra bv from some stuff
        double xbv = 0.0;

        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        if (!ignoreC3 && (game != null)) {
            xbv += getExtraC3BV((int)Math.round(finalBV));
        }
        finalBV += xbv;

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = getCrew().getBVSkillMultiplier();
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        return retVal;

    }

    /**
     * need to check bay location before loading ammo
     */
    @Override
    public boolean loadWeapon(Mounted mounted, Mounted mountedAmmo) {
        boolean success = false;
        WeaponType wtype = (WeaponType) mounted.getType();
        AmmoType atype = (AmmoType) mountedAmmo.getType();

        if (mounted.getLocation() != mountedAmmo.getLocation()) {
            return success;
        }

        // for large craft, ammo must be in the same ba
        Mounted bay = whichBay(getEquipmentNum(mounted));
        if ((bay != null) && !bay.ammoInBay(getEquipmentNum(mountedAmmo))) {
            return success;
        }

        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT) && (atype.getAmmoType() == wtype.getAmmoType()) && (atype.getRackSize() == wtype.getRackSize())) {
            mounted.setLinked(mountedAmmo);
            success = true;
        }
        return success;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getIniBonus()
     */
    @Override
    public int getHQIniBonus() {
        // large craft are considered to have > 7 tons comm equipment
        // hence they get +2 ini bonus as a mobile hq
        return 2;
    }

    /**
     * find the adjacent firing arc on this vessel clockwise
     */
    public int getAdjacentArcCW(int arc) {
        switch (arc) {
            case Compute.ARC_NOSE:
                if (isSpheroid()) {
                    return Compute.ARC_RIGHTSIDE_SPHERE;
                } else {
                    return Compute.ARC_RWING;
                }
            case Compute.ARC_LWING:
                return Compute.ARC_NOSE;
            case Compute.ARC_RWING:
                return Compute.ARC_RWINGA;
            case Compute.ARC_LWINGA:
                return Compute.ARC_LWING;
            case Compute.ARC_RWINGA:
                return Compute.ARC_AFT;
            case Compute.ARC_LEFTSIDE_SPHERE:
                return Compute.ARC_NOSE;
            case Compute.ARC_RIGHTSIDE_SPHERE:
                return Compute.ARC_RIGHTSIDEA_SPHERE;
            case Compute.ARC_LEFTSIDEA_SPHERE:
                return Compute.ARC_LEFTSIDE_SPHERE;
            case Compute.ARC_RIGHTSIDEA_SPHERE:
                return Compute.ARC_AFT;
            case Compute.ARC_AFT:
                if (isSpheroid()) {
                    return Compute.ARC_LEFTSIDEA_SPHERE;
                } else {
                    return Compute.ARC_LWINGA;
                }
            default:
                return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc on this vessel counter-clockwise
     */
    public int getAdjacentArcCCW(int arc) {
        switch (arc) {
            case Compute.ARC_NOSE:
                if (isSpheroid()) {
                    return Compute.ARC_LEFTSIDE_SPHERE;
                } else {
                    return Compute.ARC_LWING;
                }
            case Compute.ARC_LWING:
                return Compute.ARC_LWINGA;
            case Compute.ARC_RWING:
                return Compute.ARC_NOSE;
            case Compute.ARC_LWINGA:
                return Compute.ARC_AFT;
            case Compute.ARC_RWINGA:
                return Compute.ARC_RWING;
            case Compute.ARC_LEFTSIDE_SPHERE:
                return Compute.ARC_LEFTSIDEA_SPHERE;
            case Compute.ARC_RIGHTSIDE_SPHERE:
                return Compute.ARC_NOSE;
            case Compute.ARC_LEFTSIDEA_SPHERE:
                return Compute.ARC_AFT;
            case Compute.ARC_RIGHTSIDEA_SPHERE:
                return Compute.ARC_RWING;
            case Compute.ARC_AFT:
                if (isSpheroid()) {
                    return Compute.ARC_RIGHTSIDEA_SPHERE;
                } else {
                    return Compute.ARC_RWINGA;
                }
            default:
                return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc location on this vessel clockwise
     */
    public int getAdjacentLocCW(int loc) {
        switch (loc) {
            case LOC_NOSE:
                return LOC_RWING;
            case LOC_LWING:
                return LOC_NOSE;
            case LOC_RWING:
                return (LOC_RWING + 3);
            case LOC_AFT:
                return (LOC_LWING + 3);
            case 4:
                return LOC_LWING;
            case 5:
                return LOC_AFT;
            default:
                return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc on this vessel counter-clockwise
     */
    public int getAdjacentLocCCW(int loc) {
        switch (loc) {
            case LOC_NOSE:
                return LOC_LWING;
            case LOC_LWING:
                return (LOC_LWING + 3);
            case LOC_RWING:
                return LOC_NOSE;
            case LOC_AFT:
                return (LOC_RWING + 3);
            case 4:
                return LOC_AFT;
            case 5:
                return LOC_RWING;
            default:
                return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc location on this vessel clockwise
     */
    public int getOppositeLoc(int loc) {
        switch (loc) {
            case LOC_NOSE:
                return LOC_AFT;
            case LOC_LWING:
                return (LOC_RWING + 3);
            case LOC_RWING:
                return (LOC_LWING + 3);
            case LOC_AFT:
                return LOC_NOSE;
            case 4:
                return LOC_RWING;
            case 5:
                return LOC_LWING;
            default:
                return Integer.MIN_VALUE;
        }
    }

    /**
     * All military dropships automatically have ECM if in space
     */
    @Override
    public boolean hasActiveECM() {
        if (!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.hasActiveECM();
        }
        return getECMRange() > Entity.NONE;
    }

    /**
     * What's the range of the ECM equipment?
     *
     * @return the <code>int</code> range of this unit's ECM. This value will be
     *         <code>Entity.NONE</code> if no ECM is active.
     */
    @Override
    public int getECMRange() {
        if (!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.getECMRange();
        }
        if (!isMilitary()) {
            return Entity.NONE;
        }
        int range = 1;
        // the range might be affected by sensor/FCS damage
        range = range - getFCSHits() - getSensorHits();
        return range;
    }

    /**
     * Return the height of this dropship above the terrain.
     */
    @Override
    public int height() {
        if (isAirborne()) {
            return 0;
        }
        if (isSpheroid()) {
            return 10;
        }
        return 5;
    }

    @Override
    public void setPosition(Coords position) {
        super.setPosition(position);
        if ((getAltitude() == 0) && !game.getBoard().inSpace() && (position != null)) {
            secondaryPositions.put(0, position.translated(0));
            secondaryPositions.put(1, position.translated(1));
            secondaryPositions.put(2, position.translated(2));
            secondaryPositions.put(3, position.translated(3));
            secondaryPositions.put(4, position.translated(4));
            secondaryPositions.put(5, position.translated(5));
        }
    }
    
    public int getLandingLength() {
        return 15;
    }
}
