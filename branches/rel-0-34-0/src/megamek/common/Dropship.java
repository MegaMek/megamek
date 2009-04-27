/*
* MegaAero - Copyright (C) 2007 Jay Lawson
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
/*
 * Created on Jun 17, 2007
 *
 */
package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
    //escape pods and lifeboats
    int escapePods = 0;
    int lifeBoats = 0;

    //what needs to go here?
    //loading and unloading of units?
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

        if(weight >= 40000) {
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
    public double getCost() {

        double cost = 0;

        //add in controls
        //bridge
        cost += 200000 + 10 * weight;
        //computer
        cost += 200000;
        //life support
        cost += 5000 * (getNCrew() + getNPassenger());
        //sensors
        cost += 80000;
        //fcs
        cost += 100000;
        //gunnery/control systems
        cost += 10000 * getArcswGuns();

        //structural integrity
        cost += 100000 * getSI();

        //additional flight systems (attitude thruster and landing gear)
        cost += 25000 + 10 * getWeight();

        //docking collar
        cost += 10000;

        //engine
        double engineMultiplier = 0.065;
        if(isClan()) {
            engineMultiplier = 0.061;
        }
        double engineWeight = getOriginalWalkMP() * weight * engineMultiplier;
        cost += engineWeight * 1000;
        //drive unit
        cost += 500 * getOriginalWalkMP() * weight / 100.0;

        //fuel tanks
        cost += 200 * getFuel() / getFuelPerTon();

        //armor
        cost += getArmorWeight()*EquipmentType.getArmorCost(armorType);

        //heat sinks
        int sinkCost = 2000 + 4000 * getHeatType();// == HEAT_DOUBLE ? 6000: 2000;
        cost += sinkCost*getHeatSinks();

        //weapons
        cost += getWeaponsAndEquipmentCost();

        //get bays
        int baydoors = 0;
        int bayCost = 0;
        for(Bay next:getTransportBays()) {
            baydoors += next.getDoors();
            if((next instanceof MechBay) || (next instanceof ASFBay) || (next instanceof SmallCraftBay)) {
                bayCost += 20000 * next.totalSpace;
            }
            if((next instanceof LightVehicleBay) || (next instanceof HeavyVehicleBay)) {
                bayCost += 10000 * next.totalSpace;
            }
        }

        cost += bayCost + baydoors * 1000;

        //life boats and escape pods
        cost += 5000 * (getLifeBoats() + getEscapePods());

        double weightMultiplier = 36.0;
        if(isSpheroid() ) {
            weightMultiplier = 28.0;
        }

        return Math.round(cost * weightMultiplier);

    }
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        int modularArmor = 0;
        for (Mounted mounted : getEquipment()) {
            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        dbv += (getTotalArmor()+modularArmor) * 2.5;

        dbv += getSI() * 2.0;

        // add defensive equipment
        double dEquipmentBV = 0;

        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS)))
                    || ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS))
                    || ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER))
                    || ((etype instanceof WeaponType) && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN))) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;

        //unit type multiplier
        dbv *= getBVTypeModifier();

        // calculate heat efficiency
        int aeroHeatEfficiency = getHeatCapacity();

        //get arc BV and heat
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        TreeMap<String, Double> weaponsForExcessiveAmmo = new TreeMap<String, Double>();
        TreeMap<Integer, Double> arcBVs = new TreeMap<Integer, Double>();
        TreeMap<Integer, Double> arcHeat = new TreeMap<Integer, Double>();
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double weaponHeat = wtype.getHeat();
            int arc = getWeaponArc(getEquipmentNum(mounted));
            double dBV = wtype.getBV(this);
            //skip bays
            if(wtype instanceof BayWeapon) {
                continue;
            }
            //don't count defensive weapons
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            //don't count screen launchers, they are defensive
            if(wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                continue;
            }
            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
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
            //add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA)) || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize() + ";" + arc;
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this) + weaponsForExcessiveAmmo.get(key));
                }
            }
            //calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgaBV = 0;
                for (Mounted possibleMG : getTotalWeaponList()) {
                    if (possibleMG.getType().hasFlag(WeaponType.F_MG)
                            && (possibleMG.getLocation() == mounted.getLocation())) {
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
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                }
            }

            double currentArcBV = 0.0;
            double currentArcHeat = 0.0;
            if(null != arcBVs.get(arc)) {
                currentArcBV = arcBVs.get(arc);
            }
            if(null != arcHeat.get(arc)) {
                currentArcHeat = arcHeat.get(arc);
            }
            arcBVs.put(arc, currentArcBV + dBV);
            arcHeat.put(arc, currentArcHeat + weaponHeat);
        }
        double weaponBV = 0.0;
        //lets traverse the hash and find the highest value BV arc
        int highArc = Integer.MIN_VALUE;
        int adjArc = Integer.MIN_VALUE;
        int oppArc = Integer.MIN_VALUE;
        double adjArcMult = 1.0;
        double oppArcMult = 0.5;
        double highBV = 0.0;
        double heatUsed = 0.0;
        Set<Integer> set= arcBVs.keySet();
        Iterator<Integer> iter = set.iterator();
        while(iter.hasNext()) {
            int key = iter.next();
            if(arcBVs.get(key) > highBV) {
                highArc = key;
                highBV = arcBVs.get(key);
            }
        }
        //now lets identify the adjacent and opposite arcs
        if(highArc > Integer.MIN_VALUE) {
            heatUsed += arcHeat.get(highArc);
            //now get the BV and heat for the two adjacent arcs
            int adjArcCW = getAdjacentArcCW(highArc);
            int adjArcCCW = getAdjacentArcCCW(highArc);
            double adjArcCWBV = 0.0;
            double adjArcCWHeat = 0.0;
            if((adjArcCW > Integer.MIN_VALUE) && (null != arcBVs.get(adjArcCW))) {
                adjArcCWBV = arcBVs.get(adjArcCW);
                adjArcCWHeat = arcHeat.get(adjArcCW);
            }
            double adjArcCCWBV = 0.0;
            double adjArcCCWHeat = 0.0;
            if((adjArcCCW > Integer.MIN_VALUE) && (null != arcBVs.get(adjArcCCW))) {
                adjArcCCWBV = arcBVs.get(adjArcCCW);
                adjArcCCWHeat = arcHeat.get(adjArcCCW);
            }
            if(adjArcCWBV > adjArcCCWBV) {
                adjArc = adjArcCW;
                if((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcMult = 0.5;
                }
                heatUsed += adjArcCWHeat;
                oppArc = adjArcCCW;
                if((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    oppArcMult = 0.25;
                }
            } else {
                adjArc = adjArcCCW;
                if((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcMult = 0.5;
                }
                heatUsed += adjArcCCWHeat;
                oppArc = adjArcCW;
                if((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    oppArcMult = 0.25;
                }
            }
        }
        //According to an email with Welshman, ammo should be now added into each arc BV
        //for the final calculation of BV, including the excessive ammo rule
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>();
        for (Mounted mounted : getAmmo()) {
            int arc = getWeaponArc(getEquipmentNum(mounted));
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }
            //don't count screen launchers, they are defensive
            if(atype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) {
                continue;
            }
            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }

            String key = atype.getAmmoType() + ":" + atype.getRackSize() + ";" + arc;
            double weight = mounted.getShotsLeft() / atype.getShots();
            if(atype.isCapital()) {
                weight = mounted.getShotsLeft() * atype.getAmmoRatio();
            }
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, weight*atype.getBV(this));
            } else {
                ammo.put(key, weight*atype.getBV(this) + ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons
        // in that arc is reached
        for (String key : keys) {
            double ammoBV = 0.0;
            int arc= Integer.parseInt(key.split(";")[1]);
            //get the arc
            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                } else {
                    ammoBV += ammo.get(key);
                }
            }
            double currentArcBV = 0.0;
            if(null != arcBVs.get(arc)) {
                currentArcBV = arcBVs.get(arc);
            }
            arcBVs.put(arc, currentArcBV + ammoBV);
        }


        //ok now lets go in and add the arcs
        if(highArc > Integer.MIN_VALUE) {
            //ok now add the BV from this arc and reset to zero
            weaponBV += arcBVs.get(highArc);
            arcBVs.put(highArc, 0.0);
            if((adjArc > Integer.MIN_VALUE) && (null != arcBVs.get(adjArc))) {
                weaponBV += adjArcMult * arcBVs.get(adjArc);
                arcBVs.put(adjArc, 0.0);
            }
            if((oppArc > Integer.MIN_VALUE) && (null != arcBVs.get(oppArc))) {
                weaponBV += oppArcMult * arcBVs.get(oppArc);
                arcBVs.put(oppArc, 0.0);
            }
            //ok now we can cycle through the rest and add 25%
            set= arcBVs.keySet();
            iter = set.iterator();
            while(iter.hasNext()) {
                int key = iter.next();
                weaponBV += (0.25 * arcBVs.get(key));
            }
        }

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
            oEquipmentBV += bv;
        }
        weaponBV += oEquipmentBV;

        // adjust further for speed factor
        double speedFactor = Math.pow(1 + (((double) getRunMP()  - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        obv = weaponBV * speedFactor;

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        if (((hasC3MM() && (calculateFreeC3MNodes() < 2)) || (hasC3M() && (calculateFreeC3Nodes() < 3)) || (hasC3S() && (c3Master > NONE)) || (hasC3i() && (calculateFreeC3Nodes() < 5))) && !ignoreC3 && (game != null)) {
            int totalForceBV = 0;
            totalForceBV += this.calculateBattleValue(true, true);
            for (Entity e : game.getC3NetworkMembers(this)) {
                if (!equals(e) && onSameC3NetworkAs(e)) {
                    totalForceBV += e.calculateBattleValue(true, true);
                }
            }
            xbv += totalForceBV *= 0.05;
        }

        int finalBV = (int) Math.round(dbv + obv + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = crew.getBVSkillMultiplier();
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        // don't factor pilot in if we are just calculating BV for C3 extra BV
        if (ignoreC3) {
            return finalBV;
        }
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

        if(mounted.getLocation() != mountedAmmo.getLocation()) {
            return success;
        }

        //for large craft, ammo must be in the same ba
        Mounted bay = whichBay(getEquipmentNum(mounted));
        if((bay != null) && !bay.ammoInBay(getEquipmentNum(mountedAmmo))) {
            return success;
        }


        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT)
                && (atype.getAmmoType() == wtype.getAmmoType())
                && (atype.getRackSize() == wtype.getRackSize())) {
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
        switch(arc) {
        case Compute.ARC_NOSE:
            if(isSpheroid()) {
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
            if(isSpheroid()) {
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
        switch(arc) {
        case Compute.ARC_NOSE:
            if(isSpheroid()) {
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
            if(isSpheroid()) {
                return Compute.ARC_RIGHTSIDEA_SPHERE;
            } else {
                return Compute.ARC_RWINGA;
            }
        default:
            return Integer.MIN_VALUE;
        }
    }

    /**
     * All military dropships automatically have ECM if in space
     */
    @Override
    public boolean hasActiveECM() {
        if(!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.hasActiveECM();
        }
        return getECMRange() > Entity.NONE;
    }

    /**
     * What's the range of the ECM equipment?
     *
     * @return the <code>int</code> range of this unit's ECM. This value will
     *         be <code>Entity.NONE</code> if no ECM is active.
     */
    @Override
    public int getECMRange() {
        if(!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.getECMRange();
        }
        if(!isMilitary()) {
            return Entity.NONE;
        }
        int range = 1;
        //the range might be affected by sensor/FCS damage
        range = range - getFCSHits() - getSensorHits();
        return range;
    }
}
