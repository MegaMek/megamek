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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import megamek.common.weapons.BayWeapon;

/**
 * @author Jay Lawson
 */
public class FighterSquadron extends Aero {
    
    /**
     * 
     */
    private static final long serialVersionUID = -8258929505993881445L;

    public static int MAX_SIZE = 12;
    
    public Vector<String> fighters = new Vector<String>();
    
    private int damThresh = 1;
    
    private int standard_damage = 0;
    
    //number of initial fighters
    private int n0Fighters = 0;
    //number of current fighters
    private int nFighters = 0;
//    just have a single armor value, no locations
    private int armor = 0;
    private int orig_armor = 0;
    private boolean hasTC = false;
    
    private int damageRound = 0;
    
    private double cost = 0.0;
    
    public void setCost(double d) {
        this.cost = d;
    }
    
    public double getCost() {
        return cost;
    }
    
    public void setThresh(int val) {
        this.damThresh = val;
    }
    
    public int getThresh() {
        return damThresh;
    }
    
    public void autoSetThresh()
    {
        int nThresh = (int)Math.round(getArmor() / (1.0*getN0Fighters()));
        setThresh(nThresh);
    }
    
    public void setN0Fighters(int n) {
        this.n0Fighters = n;
    }
    
    public int getN0Fighters() {
        return n0Fighters;
    }
    
    public void setNFighters(int n) {
        this.nFighters = n;
    }
    
    public int getNFighters() {
        return nFighters;
    }
    
    public void setArmor(int arm) {
        this.armor = arm;
    }
    
    public void set0Armor(int arm) {
        this.orig_armor = arm;
    }
    
    public int getArmor() {
        return armor;
    }
    
    public int getStandardDamage() {
        return standard_damage;
    }
    
    public void resetStandardDamage() {
            standard_damage = 0;
    }
    
    public void addStandardDamage(int damage, HitData hit) {
        standard_damage = standard_damage + damage;
    }
    
    public void addDamageRound(int dam) {
        this.damageRound += dam;
    }
    
    public int getDamageRound() {
        return damageRound;
    }
    
    public void resetDamageRound() {
        this.damageRound = 0;
    }
    
    public int getTotalArmor() {
        return armor;
    }
    
    public int getTotalOArmor() {
        return orig_armor;
    }
    
    public double getArmorRemainingPercent() {
        if(getTotalOArmor() == 0)
            return IArmorState.ARMOR_NA;
        return ((double)getTotalArmor() / (double)getTotalOArmor());
    }
    
    public double getInternalRemainingPercent() {
        return ((double)getNFighters() / (double)getN0Fighters());
    }
    
    public boolean hasTargComp() {
        
        /*for some reason
         * equipment doesn't seem to be loaded for the fighters
        for(Entity e : fighters) {
            //if any fighter doesn't have it, then return false
            if(e.hasTargComp()) {
                return true;
            }
        }
        */
        return hasTC;
    }
    
    public void setHasTC(boolean b) {
        this.hasTC = b;
    }
    
    /*I am getting wierd naming stuff so I am going to disable this for the time being
    * numbering of unique display names is off
    public void compileSquadron() {
        
        //if no fighters here then return
        if(fighters.size() <= 0) {
            return;
        }
        
        //cycle through the entity vector and create a fighter squadron
        String chassis = fighters.elementAt(0).getChassis();
        int si = 99;
        boolean alike = true;
        int armor = 0;
        int heat = 0;
        int safeThrust = 99;
        int n = 0;
        float weight = 0.0f;  
        int bv = 0;
        double cost = 0.0;
        int nTC = 0;
        for(Entity e : fighters) {      
            if(!chassis.equals(e.getChassis())) {
                alike = false;
            }        
            n++;
            //armor
            armor += e.getTotalArmor();
            //heat
            heat += e.getHeatCapacity();
            //weight
            weight += e.getWeight();
            bv += e.calculateBattleValue();
            cost += e.getCost();
            //safe thrust
            if(e.getWalkMP() < safeThrust) 
                safeThrust = e.getWalkMP();
            
            Aero a = (Aero)e;
            //si
            if(a.getSI() < si) {
                si = a.getSI();
            }
            
            //weapons 
            Mounted newmount;
            for(Mounted m : e.getEquipment() ) {
                
                if(m.getType() instanceof WeaponType) {    
                    //first load the weapon onto the squadron    
                    WeaponType wtype = (WeaponType)m.getType();
                    try{
                        newmount = this.addEquipment(wtype, m.getLocation());
                    } catch (LocationFullException ex) {
                        System.out.println("Unable to compile weapons"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        ex.printStackTrace();
                        return;
                    }
                    //skip to the next if it has no AT class
                    if(wtype.getAtClass() == WeaponType.CLASS_NONE) {
                        continue;
                    }
                    
                    //now find the right bay
                    Mounted bay = this.getFirstBay(wtype, newmount.getLocation(), newmount.isRearMounted());
                    //if this is null, then I should create a new bay
                    if(bay == null) {
                        EquipmentType newBay = WeaponBay.getBayType(wtype.getAtClass());
                        try{
                            bay = this.addEquipment(newBay, newmount.getLocation());
                        } catch (LocationFullException ex) {
                            System.out.println("Unable to compile weapons"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            ex.printStackTrace();
                            return;
                        }
                    }
                    //now add the weapon to the bay
                    bay.addWeapon(newmount);
                } else {
                    //check if this is a TC
                    if (m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_TARGCOMP)) {
                        nTC++;
                    }
                    //just add the equipment normally
                    try{
                        this.addEquipment(m.getType(), m.getLocation());
                    } catch (LocationFullException ex) {
                        System.out.println("Unable to add equipment"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        ex.printStackTrace();
                        return;
                    }
                }
            }
        }
        
        armor = (int)Math.round(armor / 10.0);
        
        this.setArmor(armor);
        this.set0Armor(armor);
        this.setHeatSinks(heat);
        this.setOriginalWalkMP(safeThrust);
        this.setN0Fighters(n);
        this.setNFighters(n);
        this.autoSetThresh();
        this.setWeight(weight);
        this.set0SI(si);
        
        if(nTC >= n) {
            this.hasTC = true;
        }
        
        //if all the same chassis, name by chassis
        //otherwise name by weight
        if(alike) {
            this.setChassis(chassis + " Squadron");
        } else {
            int aveWeight = Math.round(weight/n);
            if(aveWeight <= 45) {
                this.setChassis("Mixed Light Squadron");
            } else if(aveWeight < 75) {
                this.setChassis("Mixed Medium Squadron");
            } else {
                this.setChassis("Mixed Heavy Squadron");
            }
        }
        this.setModel("");
        
        this.loadAllWeapons();
        this.updateAllWeaponBays();
    }
    */
    
    /*
     * No real Canon way to do this. I could just add up the BV of the individual
     * fighters, but I want this to be able to adjust during the battle
     * to account for damage. So I follow the procedure for other craft.
     */
    
    public int calculateBattleValue(boolean assumeLinkedC3, boolean ignoreC3) {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        // total armor points
        dbv += getTotalArmor() * 25;

        // total internal structure  
        //until I know better what to do with this
        //dbv += getSI() * 2.0;

        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()){
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            if ((etype instanceof WeaponType && etype.hasFlag(WeaponType.F_AMS))
                    || (etype instanceof AmmoType && ((AmmoType)etype).getAmmoType() == AmmoType.T_AMS)
                    || (etype instanceof MiscType && (etype.hasFlag(MiscType.F_ECM)
                                            || etype.hasFlag(MiscType.F_AP_POD)
               // not yet coded:            || etype.hasFlag(MiscType.F_BRIDGE_LAYING)
                                            || etype.hasFlag(MiscType.F_BAP)
                                            || etype.hasFlag(MiscType.F_B_POD)))) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;

        //unit type multiplier
        dbv *= 1.2;
       
        
//      calculate heat efficiency
        int aeroHeatEfficiency = this.getHeatCapacity();

        // total up maximum heat generated
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<String, Double>();
        double maximumHeat = 0;
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType)mounted.getType();
            if(wtype instanceof BayWeapon) {
                continue;
            }
            double weaponHeat = wtype.getHeat();
            
            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() ||
                    mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }
            
            // one shot weapons count 1/4
            if (wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER
                    || wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weaponHeat *= 0.25;
            }

            // double heat for ultras
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weaponHeat *= 2;
            }

            // Six times heat for RAC
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weaponHeat *= 6;
            }

            // half heat for streaks
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_MRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)){
                weaponHeat *= 0.5;
            }
            maximumHeat += weaponHeat;
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA))
                        || wtype.hasFlag(WeaponType.F_ONESHOT)
                        || wtype.hasFlag(WeaponType.F_INFANTRY)
                        || wtype.getAmmoType() == AmmoType.T_NA)) {
                String key = wtype.getAmmoType()+":"+wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                }
                else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this)+weaponsForExcessiveAmmo.get(key));
                }
            }
        }
                
        double weaponBV = 0;
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        boolean hasTargComp = hasTargComp();
        
        if (maximumHeat <= aeroHeatEfficiency) {
            //count all weapons equal, adjusting for rear-firing and excessive ammo
            for (Mounted mounted : getTotalWeaponList()) {
                WeaponType wtype = (WeaponType)mounted.getType();
                if(wtype instanceof BayWeapon) {
                    continue;
                }
                double dBV = wtype.getBV(this);
                
                // don't count destroyed equipment
                if (mounted.isDestroyed())
                    continue;

                // don't count AMS, it's defensive
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if(hasTargComp)
                        dBV *= 1.25;
                }
                
                if (mounted.getLocation() == LOC_AFT) {
                    weaponsBVRear += dBV;
                } else {
                    weaponsBVFront += dBV;
                }
            }
        } else {
            // count weapons at full BV until heatefficiency is reached or passed with one weapon
            int heatAdded = 0;
            ArrayList<Mounted> weapons = this.getTotalWeaponList();
            Collections.sort(weapons, new WeaponComparator());
            for (Mounted weapon : weapons) {
                WeaponType wtype = (WeaponType)weapon.getType();
                if(wtype instanceof BayWeapon) {
                    continue;
                }
                double dBV = wtype.getBV(this);
                // don't count destroyed equipment
                if (weapon.isDestroyed())
                    continue;
                // don't count AMS, it's defensive
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                // calc MG Array here:
                if (wtype.hasFlag(WeaponType.F_MGA)) {
                    double mgaBV = 0;
                    for (Mounted possibleMG : this.getTotalWeaponList()) {
                        if (possibleMG.getType().hasFlag(WeaponType.F_MG) && possibleMG.getLocation() == weapon.getLocation()) {
                            mgaBV += possibleMG.getType().getBV(this);
                        }
                    }
                   dBV = mgaBV * 0.67;
                }
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                    dBV *= 1.25;
                }
                // artemis bumps up the value
                if (weapon.getLinkedBy() != null) {
                    Mounted mLinker = weapon.getLinkedBy();
                    if (mLinker.getType() instanceof MiscType && 
                            mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        dBV *= 1.2;
                    }
                } 
                
                if (weapon.getLinkedBy() != null) {
                    Mounted mLinker = weapon.getLinkedBy();
                    if (mLinker.getType() instanceof MiscType && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                        dBV *= 1.15;
                    }
                }

                heatAdded += ((WeaponType)weapon.getType()).getHeat();
                //changed this to greater than rather than greater or equal
                if (heatAdded > aeroHeatEfficiency && wtype.getHeat() > 0)
                    dBV /= 2;
                if (weapon.getLocation() == LOC_AFT) {
                    weaponsBVRear += dBV;
                } else {
                    weaponsBVFront += dBV;
                }
                //To be consistent with 3050U, this should go first
                //heatAdded += ((WeaponType)weapon.getType()).getHeat();
            }
        }
        if (weaponsBVFront > weaponsBVRear) {
            weaponBV += weaponsBVFront;
            weaponBV += (weaponsBVRear * 0.5);
        } else {
            weaponBV += weaponsBVRear;
            weaponBV += (weaponsBVFront * 0.5);
        }   
        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM - BMR p152)
        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType)mounted.getType();
 
            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            if (mtype.hasFlag(MiscType.F_ECM)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_AP_POD) 
                    || mtype.hasFlag(MiscType.F_B_POD)
//not yet coded:    || etype.hasFlag(MiscType.F_BRIDGE_LAYING)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)) //targ counted with weapons 
                continue;
            oEquipmentBV += mtype.getBV(this);
            // need to do this here, a MiscType does not know the location
            // where it's mounted
            if (mtype.hasFlag(MiscType.F_HARJEL)) {
                if (this.getArmor(mounted.getLocation(), false) != IArmorState.ARMOR_DESTROYED) {
                    oEquipmentBV += this.getArmor(mounted.getLocation());
                }
                if (this.hasRearArmor(mounted.getLocation())
                        && this.getArmor(mounted.getLocation(), true) != IArmorState.ARMOR_DESTROYED) {
                    oEquipmentBV += this.getArmor(mounted.getLocation(), true);
                }
            }
        }
        weaponBV += oEquipmentBV;

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on our team        
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>(); 
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType)mounted.getType();

            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0)
                continue;

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            // semiguided ammo might count double
            if (atype.getMunitionType() == AmmoType.M_SEMIGUIDED) {
                Player tmpP = getOwner();
                
                if ( tmpP != null ){
                    // Okay, actually check for friendly TAG.
                    if (tmpP.hasTAG())
                        tagBV += atype.getBV(this);
                    else if (tmpP.getTeam() != Player.TEAM_NONE && game != null) {
                       for (Enumeration<Team> e = game.getTeams(); e.hasMoreElements(); ) {
                            Team m = e.nextElement();
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(game)) {
                                    tagBV += atype.getBV(this);
                                }
                                // A player can't be on two teams.
                                // If we check his team and don't give the penalty, that's it.
                                break;
                            }
                        }
                    }
                }
            }
            String key = atype.getAmmoType()+":"+atype.getRackSize();
            if (!keys.contains(key))
                keys.add(key);
            if (!ammo.containsKey(key)) {
                ammo.put(key, atype.getBV(this));
            }
            else {
                ammo.put(key, atype.getBV(this)+ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons of that 
        // type on the mech is reached.
        for (String key : keys) {
            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key))
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                else
                    ammoBV += ammo.get(key);
            } else {
                // Ammo with no matching weapons counts 0, unless it's a coolant pod
                // because coolant pods have no matching weapon
                if (key.equals(new Integer(AmmoType.T_COOLANT_POD).toString()+"1")) {
                    ammoBV += ammo.get(key);
                }
            }
        }
        weaponBV += ammoBV;
        
        // adjust further for speed factor
        // this is a bit weird, because the formula gives
        // a different result than the table, because MASC/TSM
        // is handled differently (page 315, TM, compare
        // http://forums.classicbattletech.com/index.php/topic,20468.0.html
        double speedFactor;
        double speedFactorTableLookup = getOriginalRunMP();
        speedFactor = Math.pow(1+((speedFactorTableLookup-5)/10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        
        obv = weaponBV * speedFactor;

        // we get extra bv from some stuff
        double xbv = 0.0;
        //extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here.  could be better
        // also, each 'has' loops through all equipment.  inefficient to do it 3 times
        if (((hasC3MM() && calculateFreeC3MNodes() < 2) ||
            (hasC3M() && calculateFreeC3Nodes() < 3) ||
            (hasC3S() && C3Master > NONE) ||
            (hasC3i() && calculateFreeC3Nodes() < 5) ||
            assumeLinkedC3) && !ignoreC3 && (game != null)) {
            int totalForceBV = 0;
            totalForceBV += this.calculateBattleValue(false, true);
            for (Entity e : game.getC3NetworkMembers(this)) {
                if (!equals(e) && onSameC3NetworkAs(e)) {
                    totalForceBV+=e.calculateBattleValue(true);
                }
            }
            xbv += totalForceBV *= 0.05;
        }

        int finalBV = (int)Math.round(dbv + obv + xbv);

        // and then factor in pilot
        double pilotFactor = crew.getBVSkillMultiplier();
        
        int retVal = (int)Math.round((finalBV) * pilotFactor);
        
        // don't factor pilot in if we are just calculating BV for C3 extra BV
        if (ignoreC3)
            return finalBV;
        return retVal;
    }
    
    /**
     * Calculates the battle value of this ASF
     */
    public int calculateBattleValue(boolean assumeLinkedC3) {
        return calculateBattleValue(assumeLinkedC3, false);
    }
    
    public boolean doomedInAtmosphere() {
        return true;
    }
    
    
}