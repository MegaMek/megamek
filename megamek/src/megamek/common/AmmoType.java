/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003 Ben Mazur (bmazur@sev.org)
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

import java.util.Enumeration;
import java.util.Vector;
import megamek.common.equip.AmmoState;
import megamek.common.equip.EquipmentState;
import megamek.common.equip.UsesAmmoType;

public class AmmoType extends EquipmentType {
    // ammo types
    public static final int     T_BA_SMALL_LASER    = -3; // !usesAmmo(), 3 damage per hit
    public static final int     T_BA_MG             = -2; // !usesAmmo(), 2 damage per hit
    public static final int     T_NA                = -1;
    public static final int     T_AC                = 1;
    public static final int     T_VEHICLE_FLAMER    = 2;
    public static final int     T_MG                = 3;
    public static final int     T_MG_HEAVY          = 4;
    public static final int     T_MG_LIGHT          = 5;
    public static final int     T_GAUSS             = 6;
    public static final int     T_LRM               = 7;
    public static final int     T_LRM_TORPEDO       = 8;
    public static final int     T_SRM               = 9;
    public static final int     T_SRM_TORPEDO       = 10;
    public static final int     T_SRM_STREAK        = 11;
    public static final int     T_MRM               = 12;
    public static final int     T_NARC              = 13;
    public static final int     T_AMS               = 14;
    public static final int     T_ARROW_IV          = 15;
    public static final int     T_LONG_TOM          = 16;
    public static final int     T_SNIPER            = 17;
    public static final int     T_THUMPER           = 18;
    public static final int     T_AC_LBX            = 19;
    public static final int     T_AC_ULTRA          = 20;
    public static final int     T_GAUSS_LIGHT       = 21;
    public static final int     T_GAUSS_HEAVY       = 22;
    public static final int     T_AC_ROTARY         = 23;
    public static final int     T_SRM_ADVANCED      = 24;
    public static final int     T_BA_INFERNO        = 25;
    public static final int     T_BA_MICRO_BOMB     = 26;
    public static final int     T_LRM_TORPEDO_COMBO = 27;
    public static final int     T_MINE              = 28;
    public static final int     T_ATM               = 29; // Clan ATM missile systems
    public static final int     NUM_TYPES           = 30;

    // ammo flags
    public static final int     F_MG                = 0x0001;
    public static final int     F_BATTLEARMOR       = 0x1000; // only used by BA squads
    public static final int     F_PROTOMECH         = 0x0010; //No protomech ammos/mech weaps/vise versa

    // ammo munitions, used for custom loadouts
    public static final int     M_STANDARD          = 0;
    public static final int     M_CLUSTER           = 1;
    public static final int     M_ARMOR_PIERCING    = 2;
    public static final int     M_FLECHETTE         = 3;
    public static final int     M_INCENDIARY        = 4;
    public static final int     M_PRECISION         = 5;
    public static final int     M_EXTENDED_RANGE    = 6;
    public static final int     M_HIGH_EXPLOSIVE    = 7;
    public static final int     M_FLARE             = 8;
    public static final int     M_FRAGMENTATION     = 9;
    public static final int     M_INFERNO           = 10;
    public static final int     M_SEMIGUIDED        = 11;
    public static final int     M_SWARM             = 12;
    public static final int     M_SWARM_I           = 13;
    public static final int     M_THUNDER           = 14;
    public static final int     M_THUNDER_AUGMENTED = 15;
    public static final int     M_THUNDER_INFERNO   = 16;
    public static final int     M_THUNDER_VIBRABOMB = 17;
    public static final int     M_THUNDER_ACTIVE    = 18;
    public static final int     M_EXPLOSIVE         = 19;
    public static final int     M_ECM               = 20;
    public static final int     M_HAYWIRE           = 21;
    public static final int     M_NEMESIS           = 22;
    public static final int     M_NARC_EX           = 23;
    
    /*public static final String[] MUNITION_NAMES = { "Standard", 
        "Cluster", "Armor Piercing", "Flechette", "Incendiary", "Precision", 
        "Extended Range", "High Explosive", "Flare", "Fragmentation", "Inferno",
        "Semiguided", "Swarm", "SwarmI", "Thunder", "Thunder/Augmented", 
        "Thunder/Inferno", "Thunder/Vibrabomb", "Thunder/Active", "Explosive", 
        "ECM", "Haywire", "Nemesis" };
        */
    private static Vector[] m_vaMunitions = new Vector[NUM_TYPES];
    
    public static Vector getMunitionsFor(int nAmmoType) {
        return m_vaMunitions[nAmmoType];
    }

    protected int damagePerShot;
    protected int rackSize;
    private int ammoType;
    private int munitionType;
    protected int shots;

    public AmmoType() {
        criticals = 1;
        tonnage = 1.0f;
        explosive = true;
    }

    public int getAmmoType() {
        return ammoType;
    }
    
    public int getMunitionType() {
        return munitionType;
    }

    // 2003-07-13 Suvarov454 : Added xylaan's weapon refactoring

    protected int heat;
    protected RangeType range;
    protected int tech;

        //override removed, most != all.  Not sure what the intent is...--LDE 2003-09-02
    // Most ammo is explosive
//    public boolean isExplosive() {
//	return true;
//    }

    public int getHeat() {
	return heat;
    }
    public int getShotDamage(Entity en, Targetable targ) {
	return damagePerShot;
    }

    public RangeType getRange() {
	return range;
    }

    // By default, all ballistic type weapons are 9.  If some are impossible 
    // (i.e. Gauss Rifle or SRM-2, they will override)
    public int getFireTN() {
	return 9;
    }
    
    // By default, adds no new modifiers (these are for ammo based modifiers)
    public TargetRoll getModifiersFor(Game game, Entity en, Targetable targ) {
	return new TargetRoll();
    }
    
    // Note, we don't do any pre-stuff here, as that's done by the weapon
/* TODO: uncomment me delete the empty method.
    public abstract void resolveAttack(Game game, 
				       WeaponResult wr, 
				       UsesAmmoType weap, 
				       EquipmentState weap_state);
*/
    public void resolveAttack(Game game, 
				       WeaponResult wr, 
				       UsesAmmoType weap, 
        EquipmentState weap_state) {}

    // Created using the base type, using the default number of shots
    public EquipmentState getNewState(Mounted location) {
	return new AmmoState(location, this, shots);
    }
    
    // Be default, all ammo explodes, with shots remaining * damagePerShot
    public void doCriticalDamage(EquipmentState state) {
	if (isExplosive()) {
	    AmmoState as = (AmmoState)state;
	    // Get the amount of damage.
	    int damage = this.getDamagePerShot() * as.shotsLeft();
/* TODO : implement me
	    super.doCriticalDamage(state); // Set it as destroyed
*/
	    // Do weapon explosion damage 
// ########### 
	}
    }

    public int getDamagePerShot() {
        return damagePerShot;
    }

    public int getRackSize() {
        return rackSize;
    }

    public int getShots() {
        return shots;
    }

    public static void initializeTypes() {
        // all level 1 ammo
        EquipmentType.addType(createISAC2Ammo());
        EquipmentType.addType(createISAC5Ammo());
        EquipmentType.addType(createISAC10Ammo());
        EquipmentType.addType(createISAC20Ammo());
        EquipmentType.addType(createISVehicleFlamerAmmo());
        EquipmentType.addType(createISMGAmmo());
        EquipmentType.addType(createISMGAmmoHalf());
        EquipmentType.addType(createISLRM5Ammo());
        EquipmentType.addType(createISLRM10Ammo());
        EquipmentType.addType(createISLRM15Ammo());
        EquipmentType.addType(createISLRM20Ammo());
        EquipmentType.addType(createISSRM2Ammo());
        EquipmentType.addType(createISSRM4Ammo());
        EquipmentType.addType(createISSRM6Ammo());

        // Start of Level2 Ammo
        EquipmentType.addType(createISSRM2InfernoAmmo());
        EquipmentType.addType(createISSRM4InfernoAmmo());
        EquipmentType.addType(createISSRM6InfernoAmmo());
        EquipmentType.addType(createISSRM2FragAmmo());
        EquipmentType.addType(createISSRM4FragAmmo());
        EquipmentType.addType(createISSRM6FragAmmo());
        EquipmentType.addType(createISLB2XAmmo());
        EquipmentType.addType(createISLB5XAmmo());
        EquipmentType.addType(createISLB10XAmmo());
        EquipmentType.addType(createISLB20XAmmo());
        EquipmentType.addType(createISLB2XClusterAmmo());
        EquipmentType.addType(createISLB5XClusterAmmo());
        EquipmentType.addType(createISLB10XClusterAmmo());
        EquipmentType.addType(createISLB20XClusterAmmo());
        EquipmentType.addType(createISUltra2Ammo());
        EquipmentType.addType(createISUltra5Ammo());
        EquipmentType.addType(createISUltra10Ammo());
        EquipmentType.addType(createISUltra20Ammo());
        EquipmentType.addType(createISRotary2Ammo());
        EquipmentType.addType(createISRotary5Ammo());
        EquipmentType.addType(createISPrecision2Ammo());
        EquipmentType.addType(createISPrecision5Ammo());
        EquipmentType.addType(createISPrecision10Ammo());
        EquipmentType.addType(createISPrecision20Ammo());
        EquipmentType.addType(createISArmorPiercing2Ammo());
        EquipmentType.addType(createISArmorPiercing5Ammo());
        EquipmentType.addType(createISArmorPiercing10Ammo());
        EquipmentType.addType(createISArmorPiercing20Ammo());
        EquipmentType.addType(createISFlechette2Ammo());
        EquipmentType.addType(createISFlechette5Ammo());
        EquipmentType.addType(createISFlechette10Ammo());
        EquipmentType.addType(createISFlechette20Ammo());
        EquipmentType.addType(createISGaussAmmo());
        EquipmentType.addType(createISLTGaussAmmo());
        EquipmentType.addType(createISHVGaussAmmo());
        EquipmentType.addType(createISStreakSRM2Ammo());
        EquipmentType.addType(createISStreakSRM4Ammo());
        EquipmentType.addType(createISStreakSRM6Ammo());
        EquipmentType.addType(createISMRM10Ammo());
        EquipmentType.addType(createISMRM20Ammo());
        EquipmentType.addType(createISMRM30Ammo());
        EquipmentType.addType(createISMRM40Ammo());
        EquipmentType.addType(createISAMSAmmo());
        EquipmentType.addType(createISNarcAmmo());
        EquipmentType.addType(createISNarcExplosiveAmmo());
        EquipmentType.addType(createISFragLRM5Ammo());
        EquipmentType.addType(createISFragLRM10Ammo());
        EquipmentType.addType(createISFragLRM15Ammo());
        EquipmentType.addType(createISFragLRM20Ammo());
        EquipmentType.addType(createISThunderLRM5Ammo());
        EquipmentType.addType(createISThunderLRM10Ammo());
        EquipmentType.addType(createISThunderLRM15Ammo());
        EquipmentType.addType(createISThunderLRM20Ammo());
        EquipmentType.addType(createISThunderAugmentedLRM5Ammo());
        EquipmentType.addType(createISThunderAugmentedLRM10Ammo());
        EquipmentType.addType(createISThunderAugmentedLRM15Ammo());
        EquipmentType.addType(createISThunderAugmentedLRM20Ammo());
        EquipmentType.addType(createISThunderInfernoLRM5Ammo());
        EquipmentType.addType(createISThunderInfernoLRM10Ammo());
        EquipmentType.addType(createISThunderInfernoLRM15Ammo());
        EquipmentType.addType(createISThunderInfernoLRM20Ammo());
        EquipmentType.addType(createISThunderActiveLRM5Ammo());
        EquipmentType.addType(createISThunderActiveLRM10Ammo());
        EquipmentType.addType(createISThunderActiveLRM15Ammo());
        EquipmentType.addType(createISThunderActiveLRM20Ammo());
        EquipmentType.addType(createISThunderVibraLRM5Ammo());
        EquipmentType.addType(createISThunderVibraLRM10Ammo());
        EquipmentType.addType(createISThunderVibraLRM15Ammo());
        EquipmentType.addType(createISThunderVibraLRM20Ammo());

        EquipmentType.addType(createCLLB2XAmmo());
        EquipmentType.addType(createCLLB5XAmmo());
        EquipmentType.addType(createCLLB10XAmmo());
        EquipmentType.addType(createCLLB20XAmmo());
        EquipmentType.addType(createCLLB2XClusterAmmo());
        EquipmentType.addType(createCLLB5XClusterAmmo());
        EquipmentType.addType(createCLLB10XClusterAmmo());
        EquipmentType.addType(createCLLB20XClusterAmmo());
        EquipmentType.addType(createCLUltra2Ammo());
        EquipmentType.addType(createCLUltra5Ammo());
        EquipmentType.addType(createCLUltra10Ammo());
        EquipmentType.addType(createCLUltra20Ammo());
        EquipmentType.addType(createCLGaussAmmo());
        EquipmentType.addType(createCLSRM2InfernoAmmo());
        EquipmentType.addType(createCLSRM4InfernoAmmo());
        EquipmentType.addType(createCLSRM6InfernoAmmo());
        EquipmentType.addType(createCLSRM2FragAmmo());
        EquipmentType.addType(createCLSRM4FragAmmo());
        EquipmentType.addType(createCLSRM6FragAmmo());
        EquipmentType.addType(createCLStreakSRM2Ammo());
        EquipmentType.addType(createCLStreakSRM4Ammo());
        EquipmentType.addType(createCLStreakSRM6Ammo());
        EquipmentType.addType(createCLVehicleFlamerAmmo());
        EquipmentType.addType(createCLMGAmmo());
        EquipmentType.addType(createCLMGAmmoHalf());
        EquipmentType.addType(createCLHeavyMGAmmo());
        EquipmentType.addType(createCLHeavyMGAmmoHalf());
        EquipmentType.addType(createCLLightMGAmmo());
        EquipmentType.addType(createCLLightMGAmmoHalf());
        EquipmentType.addType(createCLLRM5Ammo());
        EquipmentType.addType(createCLLRM10Ammo());
        EquipmentType.addType(createCLLRM15Ammo());
        EquipmentType.addType(createCLLRM20Ammo());
        EquipmentType.addType(createCLSRM2Ammo());
        EquipmentType.addType(createCLSRM4Ammo());
        EquipmentType.addType(createCLSRM6Ammo());
        EquipmentType.addType(createCLAMSAmmo());
        EquipmentType.addType(createCLNarcAmmo());
        EquipmentType.addType(createCLNarcExplosiveAmmo());
        EquipmentType.addType(createCLATM3Ammo());
        EquipmentType.addType(createCLATM3ERAmmo());
        EquipmentType.addType(createCLATM3HEAmmo());
        EquipmentType.addType(createCLATM6Ammo());
        EquipmentType.addType(createCLATM6ERAmmo());
        EquipmentType.addType(createCLATM6HEAmmo());
        EquipmentType.addType(createCLATM9Ammo());
        EquipmentType.addType(createCLATM9ERAmmo());
        EquipmentType.addType(createCLATM9HEAmmo());
        EquipmentType.addType(createCLATM12Ammo());
        EquipmentType.addType(createCLATM12ERAmmo());
        EquipmentType.addType(createCLATM12HEAmmo());
        EquipmentType.addType(createCLFragLRM5Ammo());
        EquipmentType.addType(createCLFragLRM10Ammo());
        EquipmentType.addType(createCLFragLRM15Ammo());
        EquipmentType.addType(createCLFragLRM20Ammo());
        EquipmentType.addType(createCLThunderLRM5Ammo());
        EquipmentType.addType(createCLThunderLRM10Ammo());
        EquipmentType.addType(createCLThunderLRM15Ammo());
        EquipmentType.addType(createCLThunderLRM20Ammo());
        EquipmentType.addType(createCLThunderAugmentedLRM5Ammo());
        EquipmentType.addType(createCLThunderAugmentedLRM10Ammo());
        EquipmentType.addType(createCLThunderAugmentedLRM15Ammo());
        EquipmentType.addType(createCLThunderAugmentedLRM20Ammo());
        EquipmentType.addType(createCLThunderInfernoLRM5Ammo());
        EquipmentType.addType(createCLThunderInfernoLRM10Ammo());
        EquipmentType.addType(createCLThunderInfernoLRM15Ammo());
        EquipmentType.addType(createCLThunderInfernoLRM20Ammo());
        EquipmentType.addType(createCLThunderActiveLRM5Ammo());
        EquipmentType.addType(createCLThunderActiveLRM10Ammo());
        EquipmentType.addType(createCLThunderActiveLRM15Ammo());
        EquipmentType.addType(createCLThunderActiveLRM20Ammo());
        EquipmentType.addType(createCLThunderVibraLRM5Ammo());
        EquipmentType.addType(createCLThunderVibraLRM10Ammo());
        EquipmentType.addType(createCLThunderVibraLRM15Ammo());
        EquipmentType.addType(createCLThunderVibraLRM20Ammo());

        // Start of BattleArmor ammo
        EquipmentType.addType( createBASRM2Ammo() );
        EquipmentType.addType( createBASRM2OSAmmo() );
        EquipmentType.addType( createBAInfernoSRMAmmo() );
        EquipmentType.addType( createBAAdvancedSRM2Ammo() );
        EquipmentType.addType( createBAMicroBombAmmo() );
        EquipmentType.addType( createCLTorpedoLRM5Ammo() );
        EquipmentType.addType( createFenrirSRM4Ammo() );
        EquipmentType.addType( createBACompactNarcAmmo() );
        EquipmentType.addType( createBAMineLauncherAmmo() );
        EquipmentType.addType( createBALRM5Ammo() );

        //Protomech ammo
        EquipmentType.addType(createCLPROSRM1Ammo());
        EquipmentType.addType(createCLPROStreakSRM1Ammo());
        EquipmentType.addType(createCLPROSRM2Ammo());
        EquipmentType.addType(createCLPROStreakSRM2Ammo());
        EquipmentType.addType(createCLPROSRM3Ammo());
        EquipmentType.addType(createCLPROStreakSRM3Ammo());
        EquipmentType.addType(createCLPROSRM4Ammo());
        EquipmentType.addType(createCLPROStreakSRM4Ammo());
        EquipmentType.addType(createCLPROSRM5Ammo());
        EquipmentType.addType(createCLPROStreakSRM5Ammo());
        EquipmentType.addType(createCLPROSRM6Ammo());
        EquipmentType.addType(createCLPROStreakSRM6Ammo());
        EquipmentType.addType(createCLPROLRM1Ammo());
        EquipmentType.addType(createCLPROLRM2Ammo());
        EquipmentType.addType(createCLPROLRM3Ammo());
        EquipmentType.addType(createCLPROLRM4Ammo());
        EquipmentType.addType(createCLPROLRM5Ammo());
        EquipmentType.addType(createCLPROLRM6Ammo());
        EquipmentType.addType(createCLPROLRM7Ammo());
        EquipmentType.addType(createCLPROLRM8Ammo());
        EquipmentType.addType(createCLPROLRM9Ammo());
        EquipmentType.addType(createCLPROLRM10Ammo());
        EquipmentType.addType(createCLPROLRM11Ammo());
        EquipmentType.addType(createCLPROLRM12Ammo());
        EquipmentType.addType(createCLPROLRM13Ammo());
        EquipmentType.addType(createCLPROLRM14Ammo());
        EquipmentType.addType(createCLPROLRM15Ammo());
        EquipmentType.addType(createCLPROLRM16Ammo());
        EquipmentType.addType(createCLPROLRM17Ammo());
        EquipmentType.addType(createCLPROLRM18Ammo());
        EquipmentType.addType(createCLPROLRM19Ammo());
        EquipmentType.addType(createCLPROLRM20Ammo());
        
        // cache types that share a launcher for loadout purposes
        for (Enumeration e = EquipmentType.getAllTypes(); e.hasMoreElements(); ) {
            EquipmentType et = (EquipmentType)e.nextElement();
            if (! (et instanceof AmmoType)) {
                continue;
            }
            AmmoType at = (AmmoType)et;
            int nType = at.getAmmoType();
            if (m_vaMunitions[nType] == null) {
                m_vaMunitions[nType] = new Vector();
            }
            m_vaMunitions[nType].addElement(at);
        }   
    }
    
    public static AmmoType createISAC2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/2 Ammo";
        ammo.internalName = "IS Ammo AC/2";
        ammo.mepName = "IS Ammo AC/2";
        ammo.mtfName = "ISAC2 Ammo";
        ammo.tdbName = "IS Autocannon/2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 45;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createISAC5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/5 Ammo";
        ammo.internalName = "IS Ammo AC/5";
        ammo.mepName = "IS Ammo AC/5";
        ammo.mtfName = "ISAC5 Ammo";
        ammo.tdbName = "IS Autocannon/5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 20;
        ammo.bv = 9;
        
        return ammo;
    }
    
    public static AmmoType createISAC10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/10 Ammo";
        ammo.internalName = "IS Ammo AC/10";
        ammo.mepName = "IS Ammo AC/10";
        ammo.mtfName = "ISAC10 Ammo";
        ammo.tdbName = "IS Autocannon/10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 10;
        ammo.bv = 15;
        
        return ammo;
    }
    
    public static AmmoType createISAC20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AC/20 Ammo";
        ammo.internalName = "IS Ammo AC/20";
        ammo.mepName = "IS Ammo AC/20";
        ammo.mtfName = "ISAC20 Ammo";
        ammo.tdbName = "IS Autocannon/20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC;
        ammo.shots = 5;
        ammo.bv = 20;
        
        return ammo;
    }
    
    public static AmmoType createISVehicleFlamerAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Vehicle Flamer Ammo";
        ammo.internalName = "IS Vehicle Flamer Ammo";
        ammo.mepName = "IS Ammo Vehicle Flamer";
        ammo.mtfName = "ISVehicleFlamer Ammo";
        ammo.tdbName = "IS Vehicle Flamer Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_VEHICLE_FLAMER;
        ammo.shots = 20;
        ammo.bv = 1;
        
        return ammo;
    }
    
    public static AmmoType createISMGAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Machine Gun Ammo";
        ammo.internalName = "IS Ammo MG - Full";
        ammo.mepName = "IS Ammo MG - Full";
        ammo.mtfName = "ISMG Ammo (200)";
        ammo.tdbName = "IS Machine Gun Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags |= F_MG;
        ammo.shots = 200;
        ammo.bv = 1;
        
        return ammo;
    }
    
    public static AmmoType createISMGAmmoHalf() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Half Machine Gun Ammo";
        ammo.internalName = "IS Machine Gun Ammo - Half";
        ammo.mepName = "IS Ammo MG - Half";
        ammo.mtfName = "ISMG Ammo (100)";
        ammo.tdbName = "IS Machine Gun Ammo (1/2 ton)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags |= F_MG;
        ammo.shots = 100;
        ammo.bv = 0.5f;
        ammo.tonnage = 0.5f;
        
        return ammo;
    }
    
    public static AmmoType createISLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 5 Ammo";
        ammo.internalName = "IS Ammo LRM-5";
        ammo.mepName = "IS Ammo LRM-5";
        ammo.mtfName = "ISLRM5 Ammo";
        ammo.tdbName = "IS LRM 5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 24;
        ammo.bv = 6;
        
        return ammo;
    }

    public static AmmoType createISFragLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 5 Fragmentation Ammo";
        ammo.internalName = "IS Fragmentation Ammo LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_FRAGMENTATION;
        ammo.shots = 24;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISThunderLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder LRM 5 Ammo";
        ammo.internalName = "IS Ammo Thunder LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER;
        ammo.shots = 24;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISThunderAugmentedLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder Augmented LRM 5 Ammo";
        ammo.internalName = "IS Ammo Thunder Augmented LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_AUGMENTED;
        ammo.shots = 12;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createISThunderInfernoLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Inferno LRM 5 Ammo";
        ammo.internalName = "IS Ammo Thunder-Inferno LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_INFERNO;
        ammo.shots = 12;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createISThunderActiveLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Active LRM 5 Ammo";
        ammo.internalName = "IS Ammo Thunder-Active LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_ACTIVE;
        ammo.shots = 12;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createISThunderVibraLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Vibrabomb LRM 5 Ammo";
        ammo.internalName = "IS Ammo Thunder-Vibrabomb LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_VIBRABOMB;
        ammo.shots = 12;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createISLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 10 Ammo";
        ammo.internalName = "IS Ammo LRM-10";
        ammo.mepName = "IS Ammo LRM-10";
        ammo.mtfName = "ISLRM10 Ammo";
        ammo.tdbName = "IS LRM 10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 12;
        ammo.bv = 11;
        
        return ammo;
    }

    public static AmmoType createISFragLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 10 Fragmentation Ammo";
        ammo.internalName = "IS Fragmentation Ammo LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_FRAGMENTATION;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.techType = TechConstants.T_IS_LEVEL_2;

        return ammo;
    }
    
    public static AmmoType createISThunderLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder LRM 10 Ammo";
        ammo.internalName = "IS Ammo Thunder LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISThunderAugmentedLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder Augmented LRM 10 Ammo";
        ammo.internalName = "IS Ammo Thunder Augmented LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_AUGMENTED;
        ammo.shots = 6;
        ammo.bv = 11;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISThunderInfernoLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Inferno LRM 10 Ammo";
        ammo.internalName = "IS Ammo Thunder-Inferno LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_INFERNO;
        ammo.shots = 6;
        ammo.bv = 11;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISThunderActiveLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Active LRM 10 Ammo";
        ammo.internalName = "IS Ammo Thunder-Active LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_ACTIVE;
        ammo.shots = 6;
        ammo.bv = 11;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISThunderVibraLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Vibrabomb LRM 10 Ammo";
        ammo.internalName = "IS Ammo Thunder-Vibrabomb LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_VIBRABOMB;
        ammo.shots = 6;
        ammo.bv = 11;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 15 Ammo";
        ammo.internalName = "IS Ammo LRM-15";
        ammo.mepName = "IS Ammo LRM-15";
        ammo.mtfName = "ISLRM15 Ammo";
        ammo.tdbName = "IS LRM 15 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 8;
        ammo.bv = 17;
        
        return ammo;
    }

    public static AmmoType createISFragLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 15 Fragmentation Ammo";
        ammo.internalName = "IS Fragmentation Ammo LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_FRAGMENTATION;
        ammo.shots = 8;
        ammo.bv = 17;
        ammo.techType = TechConstants.T_IS_LEVEL_2;

        return ammo;
    }
    
    public static AmmoType createISThunderLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder LRM 15 Ammo";
        ammo.internalName = "IS Ammo Thunder LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER;
        ammo.shots = 8;
        ammo.bv = 17;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
     
    public static AmmoType createISThunderAugmentedLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder Augmented LRM 15 Ammo";
        ammo.internalName = "IS Ammo Thunder Augmented LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_AUGMENTED;
        ammo.shots = 4;
        ammo.bv = 17;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
   
    public static AmmoType createISThunderInfernoLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Inferno LRM 15 Ammo";
        ammo.internalName = "IS Ammo Thunder-Inferno LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_INFERNO;
        ammo.shots = 4;
        ammo.bv = 17;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISThunderActiveLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Active LRM 15 Ammo";
        ammo.internalName = "IS Ammo Thunder-Active LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_ACTIVE;
        ammo.shots = 4;
        ammo.bv = 17;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISThunderVibraLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Vibrabomb LRM 15 Ammo";
        ammo.internalName = "IS Ammo Thunder-Vibrabomb LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_VIBRABOMB;
        ammo.shots = 4;
        ammo.bv = 17;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 20 Ammo";
        ammo.internalName = "IS Ammo LRM-20";
        ammo.mepName = "IS Ammo LRM-20";
        ammo.mtfName = "ISLRM20 Ammo";
        ammo.tdbName = "IS LRM 20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 6;
        ammo.bv = 23;
        
        return ammo;
    }

    public static AmmoType createISFragLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 20 Fragmentation Ammo";
        ammo.internalName = "IS Fragmentation Ammo LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_FRAGMENTATION;
        ammo.shots = 12;
        ammo.bv = 23;
        ammo.techType = TechConstants.T_IS_LEVEL_2;

        return ammo;
    }
    
    public static AmmoType createISThunderLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder LRM 20 Ammo";
        ammo.internalName = "IS Ammo Thunder LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER;
        ammo.shots = 6;
        ammo.bv = 23;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createISThunderAugmentedLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder Augmented LRM 20 Ammo";
        ammo.internalName = "IS Ammo Thunder Augmented LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_AUGMENTED;
        ammo.shots = 3;
        ammo.bv = 23;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createISThunderInfernoLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Inferno LRM 20 Ammo";
        ammo.internalName = "IS Ammo Thunder-Inferno LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_INFERNO;
        ammo.shots = 3;
        ammo.bv = 23;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createISThunderActiveLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Active LRM 20 Ammo";
        ammo.internalName = "IS Ammo Thunder-Active LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_ACTIVE;
        ammo.shots = 3;
        ammo.bv = 23;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createISThunderVibraLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Vibrabomb LRM 20 Ammo";
        ammo.internalName = "IS Ammo Thunder-Vibrabomb LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_VIBRABOMB;
        ammo.shots = 3;
        ammo.bv = 23;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createISSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 2 Ammo";
        ammo.internalName = "IS Ammo SRM-2";
        ammo.mepName = "IS Ammo SRM-2";
        ammo.mtfName = "ISSRM2 Ammo";
        ammo.tdbName = "IS SRM 2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 50;
        ammo.bv = 3;
        
        return ammo;
    }
    
    public static AmmoType createISSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 4 Ammo";
        ammo.internalName = "IS Ammo SRM-4";
        ammo.mepName = "IS Ammo SRM-4";
        ammo.mtfName = "ISSRM4 Ammo";
        ammo.tdbName = "IS SRM 4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 25;
        ammo.bv = 5;
        
        return ammo;
    }
    
    public static AmmoType createISSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 6 Ammo";
        ammo.internalName = "IS Ammo SRM-6";
        ammo.mepName = "IS Ammo SRM-6";
        ammo.mtfName = "ISSRM6 Ammo";
        ammo.tdbName = "IS SRM 6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 15;
        ammo.bv = 7;
        
        return ammo;
    }

    // Start of Level2 Ammo
    public static AmmoType createISSRM2InfernoAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 2 Inferno Ammo";
        ammo.internalName = "IS Ammo SRM-2 Inferno";
        ammo.mepName = "IS Ammo SRM-2 Inferno";
        ammo.mtfName = "ISSRM2 Inferno Ammo";
        ammo.tdbName = "IS SRM 2 Ammo - Inferno";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_INFERNO;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.techType = TechConstants.T_IS_LEVEL_2;

        return ammo;
    }

    public static AmmoType createISSRM4InfernoAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 4 Inferno Ammo";
        ammo.internalName = "IS Ammo SRM-4 Inferno";
        ammo.mepName = "IS Ammo SRM-4 Inferno";
        ammo.mtfName = "ISSRM4 Inferno Ammo";
        ammo.tdbName = "IS SRM 4 Ammo - Inferno";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_INFERNO;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_IS_LEVEL_2;

        return ammo;
    }

    public static AmmoType createISSRM6InfernoAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 6 Inferno Ammo";
        ammo.internalName = "IS Ammo SRM-6 Inferno";
        ammo.mepName = "IS Ammo SRM-6 Inferno";
        ammo.mtfName = "ISSRM6 Inferno Ammo";
        ammo.tdbName = "IS SRM 6 Ammo - Inferno";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_INFERNO;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_IS_LEVEL_2;

        return ammo;
    }

    public static AmmoType createISSRM2FragAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 2 Fragmentation Ammo";
        ammo.internalName = "IS Ammo SRM-2 Fragmentation";
        ammo.mepName = "IS Ammo SRM-2 Fragmentation";
        ammo.mtfName = "ISSRM2 Fragmentation Ammo";
        ammo.tdbName = "IS SRM 2 Ammo - Fragmentation";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_FRAGMENTATION;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.techType = TechConstants.T_IS_LEVEL_2;

        return ammo;
    }

    public static AmmoType createISSRM4FragAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 4 Fragmentation Ammo";
        ammo.internalName = "IS Ammo SRM-4 Fragmentation";
        ammo.mepName = "IS Ammo SRM-4 Fragmentation";
        ammo.mtfName = "ISSRM4 Fragmentation Ammo";
        ammo.tdbName = "IS SRM 4 Ammo - Fragmentation";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_FRAGMENTATION;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_IS_LEVEL_2;

        return ammo;
    }

    public static AmmoType createISSRM6FragAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 6 Fragmentation Ammo";
        ammo.internalName = "IS Ammo SRM-6 Fragmentation";
        ammo.mepName = "IS Ammo SRM-6 Fragmentation";
        ammo.mtfName = "ISSRM6 Fragmentation Ammo";
        ammo.tdbName = "IS SRM 6 Ammo - Fragmentation";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_FRAGMENTATION;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_IS_LEVEL_2;

        return ammo;
    }

    public static AmmoType createISLB2XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 2-X AC Ammo";
        ammo.internalName = "IS LB 2-X AC Ammo";
        ammo.mepName = "IS Ammo 2-X";
        ammo.mtfName = "ISLBXAC2 Ammo";
        ammo.tdbName = "IS LB 2-X AC Ammo - Slug";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLB5XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 5-X AC Ammo";
        ammo.internalName = "IS LB 5-X AC Ammo";
        ammo.mepName = "IS Ammo 5-X";
        ammo.mtfName = "ISLBXAC5 Ammo";
        ammo.tdbName = "IS LB 5-X AC Ammo - Slug";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 20;
        ammo.bv = 10;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLB10XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 10-X AC Ammo";
        ammo.internalName = "IS LB 10-X AC Ammo";
        ammo.mepName = "IS Ammo 10-X";
        ammo.mtfName = "ISLBXAC10 Ammo";
        ammo.tdbName = "IS LB 10-X AC Ammo - Slug";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLB20XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 20-X AC Ammo";
        ammo.internalName = "IS LB 20-X AC Ammo";
        ammo.mepName = "IS Ammo 20-X";
        ammo.mtfName = "ISLBXAC20 Ammo";
        ammo.tdbName = "IS LB 20-X AC Ammo - Slug";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 5;
        ammo.bv = 27;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 2-X Cluster Ammo";
        ammo.internalName = "IS LB 2-X Cluster Ammo";
        ammo.mepName = "IS Ammo 2-X (CL)";
        // this isn't a true mtf code
        ammo.mtfName = "ISLBXAC2 CL Ammo";
        ammo.tdbName = "IS LB 2-X AC Ammo - Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 5-X Cluster Ammo";
        ammo.internalName = "IS LB 5-X Cluster Ammo";
        ammo.mepName = "IS Ammo 5-X (CL)";
        // this isn't a true mtf code
        ammo.mtfName = "ISLBXAC5 CL Ammo";
        ammo.tdbName = "IS LB 5-X AC Ammo - Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 20;
        ammo.bv = 10;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 10-X Cluster Ammo";
        ammo.internalName = "IS LB 10-X Cluster Ammo";
        ammo.mepName = "IS Ammo 10-X (CL)";
        // this isn't a true mtf code
        ammo.mtfName = "ISLBXAC10 CL Ammo";
        ammo.tdbName = "IS LB 10-X AC Ammo - Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 20-X Cluster Ammo";
        ammo.internalName = "IS LB 20-X Cluster Ammo";
        ammo.mepName = "IS Ammo 20-X (CL)";
        // this isn't a true mtf code
        ammo.mtfName = "ISLBXAC20 CL Ammo";
        ammo.tdbName = "IS LB 20-X AC Ammo - Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 5;
        ammo.bv = 27;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISUltra2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/2 Ammo";
        ammo.internalName = "IS Ultra AC/2 Ammo";
        ammo.mepName = "IS Ammo Ultra AC/2";
        ammo.mtfName = "ISUltraAC2 Ammo";
        ammo.tdbName = "IS Ultra AC/2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 45;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISUltra5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/5 Ammo";
        ammo.internalName = "IS Ultra AC/5 Ammo";
        ammo.mepName = "IS Ammo Ultra AC/5";
        ammo.mtfName = "ISUltraAC5 Ammo";
        ammo.tdbName = "IS Ultra AC/5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISUltra10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/10 Ammo";
        ammo.internalName = "IS Ultra AC/10 Ammo";
        ammo.mepName = "IS Ammo Ultra AC/10";
        ammo.mtfName = "ISUltraAC10 Ammo";
        ammo.tdbName = "IS Ultra AC/10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 10;
        ammo.bv = 29;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISUltra20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/20 Ammo";
        ammo.internalName = "IS Ultra AC/20 Ammo";
        ammo.mepName = "IS Ammo Ultra AC/20";
        ammo.mtfName = "ISUltraAC20 Ammo";
        ammo.tdbName = "IS Ultra AC/20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 5;
        ammo.bv = 32;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISRotary2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Rotary AC/2 Ammo";
        ammo.internalName = "ISRotaryAC2 Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = ammo.internalName;
        ammo.tdbName = "IS Rotary AC/2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 45;
        ammo.bv = 15;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISRotary5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Rotary AC/5 Ammo";
        ammo.internalName = "ISRotaryAC5 Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = ammo.internalName;
        ammo.tdbName = "IS Rotary AC/5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_ROTARY;
        ammo.shots = 20;
        ammo.bv = 31;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISPrecision2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Precision AC/2 Ammo";
        ammo.internalName = "IS Precision Ammo AC/2";
        ammo.mepName = "IS Precision Ammo AC/2";
        ammo.mtfName = "ISAC2 Precision Ammo";
        ammo.tdbName = "IS Autocannon/2 Ammo - Precision";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_PRECISION;
        ammo.shots = 22;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISArmorPiercing2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Armor Piercing AC/2 Ammo";
        ammo.internalName = "IS Armor Piercing Ammo AC/2";
        ammo.mepName = "IS Armor Piercing Ammo AC/2";
        ammo.mtfName = "ISAC2 Armor Piercing Ammo";
        ammo.tdbName = "IS Autocannon/2 Ammo - Armor Piercing";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_ARMOR_PIERCING;
        ammo.shots = 22;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISFlechette2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Flechette AC/2 Ammo";
        ammo.internalName = "IS Flechette Ammo AC/2";
        ammo.mepName = "IS Flechette Ammo AC/2";
        ammo.mtfName = "ISAC2 Flechette Ammo";
        ammo.tdbName = "IS Autocannon/2 Ammo - Flechette";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_FLECHETTE;
        ammo.shots = 45;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISPrecision5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Precision AC/5 Ammo";
        ammo.internalName = "IS Precision Ammo AC/5";
        ammo.mepName = "IS Precision Ammo AC/5";
        ammo.mtfName = "ISAC5 Precision Ammo";
        ammo.tdbName = "IS Autocannon/5 Ammo - Precision";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_PRECISION;
        ammo.shots = 10;
        ammo.bv = 9;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISArmorPiercing5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Armor Piercing AC/5 Ammo";
        ammo.internalName = "IS Armor Piercing Ammo AC/5";
        ammo.mepName = "IS Armor Piercing Ammo AC/5";
        ammo.mtfName = "ISAC5 Armor Piercing Ammo";
        ammo.tdbName = "IS Autocannon/5 Ammo - Armor Piercing";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_ARMOR_PIERCING;
        ammo.shots = 10;
        ammo.bv = 9;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISFlechette5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Flechette AC/5 Ammo";
        ammo.internalName = "IS Flechette Ammo AC/5";
        ammo.mepName = "IS Flechette Ammo AC/5";
        ammo.mtfName = "ISAC5 Flechette Ammo";
        ammo.tdbName = "IS Autocannon/5 Ammo - Flechette";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_FLECHETTE;
        ammo.shots = 20;
        ammo.bv = 9;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISPrecision10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Precision AC/10 Ammo";
        ammo.internalName = "IS Precision Ammo AC/10";
        ammo.mepName = "IS Precision Ammo AC/10";
        ammo.mtfName = "ISAC10 Precision Ammo";
        ammo.tdbName = "IS Autocannon/10 Ammo - Precision";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_PRECISION;
        ammo.shots = 5;
        ammo.bv = 15;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISArmorPiercing10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Armor Piercing AC/10 Ammo";
        ammo.internalName = "IS Armor Piercing Ammo AC/10";
        ammo.mepName = "IS Armor Piercing Ammo AC/10";
        ammo.mtfName = "ISAC10 Armor Piercing Ammo";
        ammo.tdbName = "IS Autocannon/10 Ammo - Armor Piercing";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_ARMOR_PIERCING;
        ammo.shots = 5;
        ammo.bv = 15;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISFlechette10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Flechette AC/10 Ammo";
        ammo.internalName = "IS Flechette Ammo AC/10";
        ammo.mepName = "IS Flechette Ammo AC/10";
        ammo.mtfName = "ISAC10 Flechette Ammo";
        ammo.tdbName = "IS Autocannon/10 Ammo - Flechette";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_FLECHETTE;
        ammo.shots = 10;
        ammo.bv = 15;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISPrecision20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Precision AC/20 Ammo";
        ammo.internalName = "IS Precision Ammo AC/20";
        ammo.mepName = "IS Precision Ammo AC/20";
        ammo.mtfName = "ISAC20 Precision Ammo";
        ammo.tdbName = "IS Autocannon/20 Ammo - Precision";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_PRECISION;
        ammo.shots = 2;
        ammo.bv = 20;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISArmorPiercing20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Armor Piercing AC/20 Ammo";
        ammo.internalName = "IS Armor Piercing Ammo AC/20";
        ammo.mepName = "IS Armor Piercing Ammo AC/20";
        ammo.mtfName = "ISAC20 Armor Piercing Ammo";
        ammo.tdbName = "IS Autocannon/20 Ammo - Armor Piercing";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_ARMOR_PIERCING;
        ammo.shots = 2;
        ammo.bv = 20;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISFlechette20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Flechette AC/20 Ammo";
        ammo.internalName = "IS Flechette Ammo AC/20";
        ammo.mepName = "IS Flechette Ammo AC/20";
        ammo.mtfName = "ISAC20 Flechette Ammo";
        ammo.tdbName = "IS Autocannon/20 Ammo - Flechette";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC;
        ammo.munitionType = AmmoType.M_FLECHETTE;
        ammo.shots = 5;
        ammo.bv = 20;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISGaussAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Gauss Ammo";
        ammo.internalName = "IS Gauss Ammo";
        ammo.mepName = "IS Ammo Gauss";
        ammo.mtfName = "ISGauss Ammo";
        ammo.tdbName = "IS Gauss Rifle Ammo";
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS;
        ammo.shots = 8;
        ammo.bv = 37;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISLTGaussAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Light Gauss Ammo";
        ammo.internalName = "IS Light Gauss Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISLightGauss Ammo";
        ammo.tdbName = "IS Light Gauss Rifle Ammo";
        ammo.damagePerShot = 8;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS_LIGHT;
        ammo.shots = 16;
        ammo.bv = 20;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISHVGaussAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Heavy Gauss Ammo";
        ammo.internalName = "ISHeavyGauss Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = ammo.internalName;
        ammo.tdbName = "IS Heavy Gauss Rifle Ammo";
        ammo.damagePerShot = 25;  // actually variable
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS_HEAVY;
        ammo.shots = 4;
        ammo.bv = 43;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 2 Ammo";
        ammo.internalName = "IS Streak SRM 2 Ammo";
        ammo.mepName = "IS Ammo Streak-2";
        ammo.mtfName = "ISStreakSRM2 Ammo";
        ammo.tdbName = "IS Streak SRM 2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 50;
        ammo.bv = 4;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 4 Ammo";
        ammo.internalName = "IS Streak SRM 4 Ammo";
        ammo.mepName = "IS Ammo Streak-4";
        ammo.mtfName = "ISStreakSRM4 Ammo";
        ammo.tdbName = "IS Streak SRM 4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 25;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 6 Ammo";
        ammo.internalName = "IS Streak SRM 6 Ammo";
        ammo.mepName = "IS Ammo Streak-6";
        ammo.mtfName = "ISStreakSRM6 Ammo";
        ammo.tdbName = "IS Streak SRM 6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 15;
        ammo.bv = 11;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISMRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "MRM 10 Ammo";
        ammo.internalName = "IS MRM 10 Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISMRM10 Ammo";
        ammo.tdbName = "IS MRM 10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISMRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "MRM 20 Ammo";
        ammo.internalName = "IS MRM 20 Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISMRM20 Ammo";
        ammo.tdbName = "IS MRM 20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISMRM30Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "MRM 30 Ammo";
        ammo.internalName = "IS MRM 30 Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISMRM30 Ammo";
        ammo.tdbName = "IS MRM 30 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 30;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISMRM40Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "MRM 40 Ammo";
        ammo.internalName = "IS MRM 40 Ammo";
        ammo.mepName = "N/A";
        ammo.mtfName = "ISMRM40 Ammo";
        ammo.tdbName = "IS MRM 40 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 40;
        ammo.ammoType = AmmoType.T_MRM;
        ammo.shots = 6;
        ammo.bv = 28;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISAMSAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AMS Ammo";
        ammo.internalName = "ISAMS Ammo";
        ammo.mepName = "IS Ammo AMS";
        ammo.mtfName = ammo.internalName;
        ammo.tdbName = "IS AMS Ammo";
        ammo.damagePerShot = 1; // only used for ammo crits
        ammo.rackSize = 2; // only used for ammo crits
        ammo.ammoType = AmmoType.T_AMS;
        ammo.shots = 12;
        ammo.bv = 11;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISNarcAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Narc Pods";
        ammo.internalName = "ISNarc Pods";
        ammo.mepName = "IS Ammo Narc";
        ammo.mtfName = ammo.internalName;
        ammo.tdbName = "IS Narc Missile Beacon Ammo";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createISNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Narc Explosive Pods";
        ammo.internalName = "ISNarc ExpolsivePods";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 4;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.munitionType = M_NARC_EX;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.techType = TechConstants.T_IS_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLGaussAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Gauss Ammo";
        ammo.internalName = "Clan Gauss Ammo";
        ammo.mepName = "Clan Ammo Gauss";
        ammo.mtfName = "CLGauss Ammo";
        ammo.tdbName = "Clan Gauss Rifle Ammo";
        ammo.damagePerShot = 15;
        ammo.explosive = false;
        ammo.ammoType = AmmoType.T_GAUSS;
        ammo.shots = 8;
        ammo.bv = 33;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLB2XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 2-X AC Ammo";
        ammo.internalName = "Clan LB 2-X AC Ammo";
        ammo.mepName = "Clan Ammo 2-X";
        ammo.mtfName = "CLLBXAC2 Ammo";
        ammo.tdbName = "Clan LB 2-X AC Ammo - Slug";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 45;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLB5XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 5-X AC Ammo";
        ammo.internalName = "Clan LB 5-X AC Ammo";
        ammo.mepName = "Clan Ammo 5-X";
        ammo.mtfName = "CLLBXAC5 Ammo";
        ammo.tdbName = "Clan LB 5-X AC Ammo - Slug";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 20;
        ammo.bv = 12;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLB10XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 10-X AC Ammo";
        ammo.internalName = "Clan LB 10-X AC Ammo";
        ammo.mepName = "Clan Ammo 10-X";
        ammo.mtfName = "CLLBXAC10 Ammo";
        ammo.tdbName = "Clan LB 10-X AC Ammo - Slug";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLB20XAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 20-X AC Ammo";
        ammo.internalName = "Clan LB 20-X AC Ammo";
        ammo.mepName = "Clan Ammo 20-X";
        ammo.mtfName = "CLLBXAC20 Ammo";
        ammo.tdbName = "Clan LB 20-X AC Ammo - Slug";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.shots = 5;
        ammo.bv = 33;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLB2XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 2-X Cluster Ammo";
        ammo.internalName = "Clan LB 2-X Cluster Ammo";
        ammo.mepName = "Clan Ammo 2-X (CL)";
        // this isn't a true mtf code
        ammo.mtfName = "CLLBXAC2 CL Ammo";
        ammo.tdbName = "Clan LB 2-X AC Ammo - Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 45;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLB5XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 5-X Cluster Ammo";
        ammo.internalName = "Clan LB 5-X Cluster Ammo";
        ammo.mepName = "Clan Ammo 5-X (CL)";
        // this isn't a true mtf code
        ammo.mtfName = "CLLBXAC5 CL Ammo";
        ammo.tdbName = "Clan LB 5-X AC Ammo - Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 20;
        ammo.bv = 12;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLB10XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 10-X Cluster Ammo";
        ammo.internalName = "Clan LB 10-X Cluster Ammo";
        ammo.mepName = "Clan Ammo 10-X (CL)";
        // this isn't a true mtf code
        ammo.mtfName = "CLLBXAC10 CL Ammo";
        ammo.tdbName = "Clan LB 10-X AC Ammo - Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 10;
        ammo.bv = 19;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLB20XClusterAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LB 20-X Cluster Ammo";
        ammo.internalName = "Clan LB 20-X Cluster Ammo";
        ammo.mepName = "Clan Ammo 20-X (CL)";
        // this isn't a true mtf code
        ammo.mtfName = "CLLBXAC20 CL Ammo";
        ammo.tdbName = "Clan LB 20-X AC Ammo - Cluster";
        ammo.damagePerShot = 1;
        ammo.toHitModifier = -1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_LBX;
        ammo.munitionType = M_CLUSTER;
        ammo.shots = 5;
        ammo.bv = 33;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLVehicleFlamerAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Vehicle Flamer Ammo";
        ammo.internalName = "Clan Vehicle Flamer Ammo";
        ammo.mepName = "Clan Ammo Vehicle Flamer";
        ammo.mtfName = "CLVehicleFlamer Ammo";
        ammo.tdbName = "Clan Vehicle Flamer Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_VEHICLE_FLAMER;
        ammo.shots = 20;
        ammo.bv = 1;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLHeavyMGAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Heavy Machine Gun Ammo";
        ammo.internalName = "Clan Heavy Machine Gun Ammo - Full";
        ammo.mepName = "N/A";
        ammo.mtfName = "CLHeavyMG Ammo (100)";
        ammo.tdbName = "Clan Heavy Machine Gun Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MG_HEAVY;
        ammo.flags |= F_MG;
        ammo.shots = 100;
        ammo.bv = 1;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLHeavyMGAmmoHalf() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Half Heavy Machine Gun Ammo";
        ammo.internalName = "Clan Heavy Machine Gun Ammo - Half";
        ammo.mepName = "N/A";
        ammo.mtfName = "CLHeavyMG Ammo (50)";
        ammo.tdbName = "Clan Heavy Machine Gun Ammo (1/2 ton)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_MG_HEAVY;
        ammo.flags |= F_MG;
        ammo.shots = 50;
        ammo.tonnage = 0.5f;
        ammo.bv = 1;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLMGAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Machine Gun Ammo";
        ammo.internalName = "Clan Machine Gun Ammo - Full";
        ammo.mepName = "Clan Ammo MG - Full";
        ammo.mtfName = "CLMG Ammo (200)";
        ammo.tdbName = "Clan Machine Gun Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags |= F_MG;
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLMGAmmoHalf() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Half Machine Gun Ammo";
        ammo.internalName = "Clan Machine Gun Ammo - Half";
        ammo.mepName = "Clan Ammo MG - Half";
        ammo.mtfName = "CLMG Ammo (100)";
        ammo.tdbName = "Clan Machine Gun Ammo (1/2 ton)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_MG;
        ammo.flags |= F_MG;
        ammo.shots = 100;
        ammo.tonnage = 0.5f;
        ammo.bv = 1;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLightMGAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Light Machine Gun Ammo";
        ammo.internalName = "Clan Light Machine Gun Ammo - Full";
        ammo.mepName = "N/A";
        ammo.mtfName = "CLLightMG Ammo (200)";
        ammo.tdbName = "Clan Light Machine Gun Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MG_LIGHT;
        ammo.flags |= F_MG;
        ammo.shots = 200;
        ammo.bv = 1;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLLightMGAmmoHalf() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Half Light Machine Gun Ammo";
        ammo.internalName = "Clan Light Machine Gun Ammo - Half";
        ammo.mepName = "N/A";
        ammo.mtfName = "CLLightMG Ammo (100)";
        ammo.tdbName = "Clan Light Machine Gun Ammo (1/2 ton)";
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MG_LIGHT;
        ammo.flags |= F_MG;
        ammo.shots = 100;
        ammo.bv = 1;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLUltra2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/2 Ammo";
        ammo.internalName = "Clan Ultra AC/2 Ammo";
        ammo.mepName = "Clan Ammo Ultra AC/2";
        ammo.mtfName = "CLUltraAC2 Ammo";
        ammo.tdbName = "Clan Ultra AC/2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 45;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLUltra5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/5 Ammo";
        ammo.internalName = "Clan Ultra AC/5 Ammo";
        ammo.mepName = "Clan Ammo Ultra AC/5";
        ammo.mtfName = "CLUltraAC5 Ammo";
        ammo.tdbName = "Clan Ultra AC/5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 20;
        ammo.bv = 15;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLUltra10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/10 Ammo";
        ammo.internalName = "Clan Ultra AC/10 Ammo";
        ammo.mepName = "Clan Ammo Ultra AC/10";
        ammo.mtfName = "CLUltraAC10 Ammo";
        ammo.tdbName = "Clan Ultra AC/10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLUltra20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Ultra AC/20 Ammo";
        ammo.internalName = "Clan Ultra AC/20 Ammo";
        ammo.mepName = "Clan Ammo Ultra AC/20";
        ammo.mtfName = "CLUltraAC20 Ammo";
        ammo.tdbName = "Clan Ultra AC/20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_AC_ULTRA;
        ammo.shots = 5;
        ammo.bv = 35;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 5 Ammo";
        ammo.internalName = "Clan Ammo LRM-5";
        ammo.mepName = "Clan Ammo LRM-5";
        ammo.mtfName = "CLLRM5 Ammo";
        ammo.tdbName = "Clan LRM 5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLFragLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 5 Fragmentation Ammo";
        ammo.internalName = "CL Fragmentation Ammo LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_FRAGMENTATION;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLThunderLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder LRM 5 Ammo";
        ammo.internalName = "Clan Ammo Thunder LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER;
        ammo.shots = 24;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderAugmentedLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder Augmented LRM 5 Ammo";
        ammo.internalName = "Clan Ammo Thunder Augmented LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_AUGMENTED;
        ammo.shots = 12;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderInfernoLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Inferno LRM 5 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Inferno LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_INFERNO;
        ammo.shots = 12;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderActiveLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Active LRM 5 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Active LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_ACTIVE;
        ammo.shots = 12;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderVibraLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Vibrabomb LRM 5 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Vibrabomb LRM-5";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_VIBRABOMB;
        ammo.shots = 12;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 10 Ammo";
        ammo.internalName = "Clan Ammo LRM-10";
        ammo.mepName = "Clan Ammo LRM-10";
        ammo.mtfName = "CLLRM10 Ammo";
        ammo.tdbName = "Clan LRM 10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLFragLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 10 Fragmentation Ammo";
        ammo.internalName = "CL Fragmentation Ammo LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_FRAGMENTATION;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder LRM 10 Ammo";
        ammo.internalName = "Clan Ammo Thunder LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER;
        ammo.shots = 12;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderAugmentedLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder Augmented LRM 10 Ammo";
        ammo.internalName = "Clan Ammo Thunder Augmented LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_AUGMENTED;
        ammo.shots = 6;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderInfernoLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Inferno LRM 10 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Inferno LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_INFERNO;
        ammo.shots = 6;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderActiveLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Active LRM 10 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Active LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_ACTIVE;
        ammo.shots = 6;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderVibraLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Vibrabomb LRM 10 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Vibrabomb LRM-10";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_VIBRABOMB;
        ammo.shots = 6;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 15 Ammo";
        ammo.internalName = "Clan Ammo LRM-15";
        ammo.mepName = "Clan Ammo LRM-15";
        ammo.mtfName = "CLLRM15 Ammo";
        ammo.tdbName = "Clan LRM 15 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLFragLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 15 Fragmentation Ammo";
        ammo.internalName = "CL Fragmentation Ammo LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_FRAGMENTATION;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLThunderLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder LRM 15 Ammo";
        ammo.internalName = "Clan Ammo Thunder LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER;
        ammo.shots = 8;
        ammo.bv = 21;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderAugmentedLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder Augmented LRM 15 Ammo";
        ammo.internalName = "Clan Ammo Thunder Augmented LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_AUGMENTED;
        ammo.shots = 4;
        ammo.bv = 21;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderInfernoLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Inferno LRM 15 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Inferno LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_INFERNO;
        ammo.shots = 4;
        ammo.bv = 21;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderActiveLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Active LRM 15 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Active LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_ACTIVE;
        ammo.shots = 4;
        ammo.bv = 21;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderVibraLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Vibrabomb LRM 15 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Vibrabomb LRM-15";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_VIBRABOMB;
        ammo.shots = 4;
        ammo.bv = 21;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 20 Ammo";
        ammo.internalName = "Clan Ammo LRM-20";
        ammo.mepName = "Clan Ammo LRM-20";
        ammo.mtfName = "CLLRM20 Ammo";
        ammo.tdbName = "Clan LRM 20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 6;
        ammo.bv = 27;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLFragLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 20 Fragmentation Ammo";
        ammo.internalName = "CL Fragmentation Ammo LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_FRAGMENTATION;
        ammo.shots = 6;
        ammo.bv = 27;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLThunderLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder LRM 20 Ammo";
        ammo.internalName = "Clan Ammo Thunder LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER;
        ammo.shots = 6;
        ammo.bv = 27;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLThunderAugmentedLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder Augmented LRM 20 Ammo";
        ammo.internalName = "Clan Ammo Thunder Augmented LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_AUGMENTED;
        ammo.shots = 3;
        ammo.bv = 27;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLThunderInfernoLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Inferno LRM 20 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Inferno LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_INFERNO;
        ammo.shots = 3;
        ammo.bv = 27;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLThunderActiveLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Active LRM 20 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Active LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_ACTIVE;
        ammo.shots = 3;
        ammo.bv = 27;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLThunderVibraLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Thunder-Vibrabomb LRM 20 Ammo";
        ammo.internalName = "Clan Ammo Thunder-Vibrabomb LRM-20";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.munitionType = AmmoType.M_THUNDER_VIBRABOMB;
        ammo.shots = 3;
        ammo.bv = 27;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 2 Ammo";
        ammo.internalName = "Clan Ammo SRM-2";
        ammo.mepName = "Clan Ammo SRM-2";
        ammo.mtfName = "CLSRM2 Ammo";
        ammo.tdbName = "Clan SRM 2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 4 Ammo";
        ammo.internalName = "Clan Ammo SRM-4";
        ammo.mepName = "Clan Ammo SRM-4";
        ammo.mtfName = "CLSRM4 Ammo";
        ammo.tdbName = "Clan SRM 4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 6 Ammo";
        ammo.internalName = "Clan Ammo SRM-6";
        ammo.mepName = "Clan Ammo SRM-6";
        ammo.mtfName = "CLSRM6 Ammo";
        ammo.tdbName = "Clan SRM 6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }

    public static AmmoType createCLSRM2InfernoAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 2 Inferno Ammo";
        ammo.internalName = "Clan Ammo SRM-2 Inferno";
        ammo.mepName = "Clan Ammo SRM-2 Inferno";
        ammo.mtfName = "CLSRM2 Inferno Ammo";
        ammo.tdbName = "Clan SRM 2 Ammo - Inferno";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_INFERNO;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLSRM4InfernoAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 4 Inferno Ammo";
        ammo.internalName = "Clan Ammo SRM-4 Inferno";
        ammo.mepName = "Clan Ammo SRM-4 Inferno";
        ammo.mtfName = "CLSRM4 Inferno Ammo";
        ammo.tdbName = "Clan SRM 4 Ammo - Inferno";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_INFERNO;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLSRM6InfernoAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 6 Inferno Ammo";
        ammo.internalName = "Clan Ammo SRM-6 Inferno";
        ammo.mepName = "Clan Ammo SRM-6 Inferno";
        ammo.mtfName = "CLSRM6 Inferno Ammo";
        ammo.tdbName = "Clan SRM 6 Ammo - Inferno";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_INFERNO;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLSRM2FragAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 2 Fragmentation Ammo";
        ammo.internalName = "Clan Ammo SRM-2 Fragmentation";
        ammo.mepName = "Clan Ammo SRM-2 Fragmentation";
        ammo.mtfName = "CLSRM2 Fragmentation Ammo";
        ammo.tdbName = "Clan SRM 2 Ammo - Fragmentation";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_FRAGMENTATION;
        ammo.shots = 50;
        ammo.bv = 3;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLSRM4FragAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 4 Fragmentation Ammo";
        ammo.internalName = "Clan Ammo SRM-4 Fragmentation";
        ammo.mepName = "Clan Ammo SRM-4 Fragmentation";
        ammo.mtfName = "CLSRM4 Fragmentation Ammo";
        ammo.tdbName = "Clan SRM 4 Ammo - Fragmentation";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_FRAGMENTATION;
        ammo.shots = 25;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLSRM6FragAmmo() {
        AmmoType ammo = new AmmoType();

        ammo.name = "SRM 6 Fragmentation Ammo";
        ammo.internalName = "Clan Ammo SRM-6 Fragmentation";
        ammo.mepName = "Clan Ammo SRM-6 Fragmentation";
        ammo.mtfName = "CLSRM6 Fragmentation Ammo";
        ammo.tdbName = "Clan SRM 6 Ammo - Fragmentation";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.munitionType = M_FRAGMENTATION;
        ammo.shots = 15;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 2 Ammo";
        ammo.internalName = "Clan Streak SRM 2 Ammo";
        ammo.mepName = "Clan Ammo Streak-2";
        ammo.mtfName = "CLStreakSRM2 Ammo";
        ammo.tdbName = "Clan Streak SRM 2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 50;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 4 Ammo";
        ammo.internalName = "Clan Streak SRM 4 Ammo";
        ammo.mepName = "Clan Ammo Streak-4";
        ammo.mtfName = "CLStreakSRM4 Ammo";
        ammo.tdbName = "Clan Streak SRM 4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 25;
        ammo.bv = 10;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Streak SRM 6 Ammo";
        ammo.internalName = "Clan Streak SRM 6 Ammo";
        ammo.mepName = "Clan Ammo Streak-6";
        ammo.mtfName = "CLStreakSRM6 Ammo";
        ammo.tdbName = "Clan Streak SRM 6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 15;
        ammo.bv = 15;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLAMSAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "AMS Ammo";
        ammo.internalName = "CLAMS Ammo";
        ammo.mepName = "Clan Ammo AMS";
        ammo.mtfName = ammo.internalName;
        ammo.tdbName = "Clan AMS Ammo";
        ammo.damagePerShot = 1; // only used for ammo crits
        ammo.rackSize = 2; // only used for ammo crits
        ammo.ammoType = AmmoType.T_AMS;
        ammo.shots = 24;
        ammo.bv = 21;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        
        return ammo;
    }
    
    public static AmmoType createCLNarcAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Narc Pods";
        ammo.internalName = "CLNarc Pods";
        ammo.mepName = "Clan Ammo Narc";
        ammo.mtfName = ammo.internalName;
        ammo.tdbName = "Clan Narc Missile Beacon Ammo";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }
 
    public static AmmoType createCLNarcExplosiveAmmo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Narc Explosive Pods";
        ammo.internalName = "CLNarc Explosive Pods";
        ammo.mepName = "N/A";
        ammo.mtfName = "N/A";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 4;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.munitionType = AmmoType.M_NARC_EX;
        ammo.shots = 6;
        ammo.bv = 0;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLATM3Ammo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 3 Ammo";
        ammo.internalName = "Clan Ammo ATM-3";
        ammo.mepName = "Clan Ammo ATM-3";
        ammo.mtfName = "CLATM3 Ammo";
        ammo.tdbName = "Clan ATM-3 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM3ERAmmo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 3 ER Ammo";
        ammo.internalName = "Clan Ammo ATM-3 ER";
        ammo.mepName = "Clan Ammo ATM-3 ER";
        ammo.mtfName = "CLATM3 ER Ammo";
        ammo.tdbName = "Clan ATM-3 ER Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM3HEAmmo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 3 HE Ammo";
        ammo.internalName = "Clan Ammo ATM-3 HE";
        ammo.mepName = "Clan Ammo ATM-3 HE";
        ammo.mtfName = "CLATM3 HE Ammo";
        ammo.tdbName = "Clan ATM-3 HE Ammo";
        ammo.damagePerShot = 3;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 20;
        ammo.bv = 14;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM6Ammo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 6 Ammo";
        ammo.internalName = "Clan Ammo ATM-6";
        ammo.mepName = "Clan Ammo ATM-6";
        ammo.mtfName = "CLATM6 Ammo";
        ammo.tdbName = "Clan ATM-6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM6ERAmmo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 6 ER Ammo";
        ammo.internalName = "Clan Ammo ATM-6 ER";
        ammo.mepName = "Clan Ammo ATM-6 ER";
        ammo.mtfName = "CLATM6 ER Ammo";
        ammo.tdbName = "Clan ATM-6 ER Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM6HEAmmo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 6 HE Ammo";
        ammo.internalName = "Clan Ammo ATM-6 HE";
        ammo.mepName = "Clan Ammo ATM-6 HE";
        ammo.mtfName = "CLATM6 HE Ammo";
        ammo.tdbName = "Clan ATM-6 HE Ammo";
        ammo.damagePerShot = 3;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 10;
        ammo.bv = 26;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM9Ammo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 9 Ammo";
        ammo.internalName = "Clan Ammo ATM-9";
        ammo.mepName = "Clan Ammo ATM-9";
        ammo.mtfName = "CLATM9 Ammo";
        ammo.tdbName = "Clan ATM-9 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.shots = 7;
        ammo.bv = 36;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM9ERAmmo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 9 ER Ammo";
        ammo.internalName = "Clan Ammo ATM-9 ER";
        ammo.mepName = "Clan Ammo ATM-9 ER";
        ammo.mtfName = "CLATM9 ER Ammo";
        ammo.tdbName = "Clan ATM-9 ER Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 7;
        ammo.bv = 36;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM9HEAmmo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 9 HE Ammo";
        ammo.internalName = "Clan Ammo ATM-9 HE";
        ammo.mepName = "Clan Ammo ATM-9 HE";
        ammo.mtfName = "CLATM9 HE Ammo";
        ammo.tdbName = "Clan ATM-9 HE Ammo";
        ammo.damagePerShot = 3;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 7;
        ammo.bv = 36;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM12Ammo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 12 Ammo";
        ammo.internalName = "Clan Ammo ATM-12";
        ammo.mepName = "Clan Ammo ATM-12";
        ammo.mtfName = "CLATM12 Ammo";
        ammo.tdbName = "Clan ATM-12 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.shots = 5;
        ammo.bv = 52;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM12ERAmmo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 12 ER Ammo";
        ammo.internalName = "Clan Ammo ATM-12 ER";
        ammo.mepName = "Clan Ammo ATM-12 ER";
        ammo.mtfName = "CLATM12 ER Ammo";
        ammo.tdbName = "Clan ATM-12 ER Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_EXTENDED_RANGE;
        ammo.shots = 5;
        ammo.bv = 52;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
         
        return ammo;
    }
 
    public static AmmoType createCLATM12HEAmmo() {
        AmmoType ammo = new AmmoType();
         
        ammo.name = "ATM 12 HE Ammo";
        ammo.internalName = "Clan Ammo ATM-12 HE";
        ammo.mepName = "Clan Ammo ATM-12 HE";
        ammo.mtfName = "CLATM12 HE Ammo";
        ammo.tdbName = "Clan ATM-12 HE Ammo";
        ammo.damagePerShot = 3;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_ATM;
        ammo.munitionType = M_HIGH_EXPLOSIVE;
        ammo.shots = 5;
        ammo.bv = 52;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    // Start BattleArmor ammo
    public static AmmoType createBASRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 2 Ammo";
        ammo.internalName = "BA-SRM2 Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = "BASRM2 Ammo";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 2;
        ammo.hittable = false;
        ammo.bv = 0;
        
        return ammo;
    }
    public static AmmoType createBASRM2OSAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 2 Ammo";
        ammo.internalName = BattleArmor.IS_DISPOSABLE_SRM2_AMMO;
        ammo.mepName = ammo.internalName;
        ammo.mtfName = "BASRM2OS Ammo";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 1;
        ammo.hittable = false;
        ammo.bv = 0;
        
        return ammo;
    }
    public static AmmoType createBAInfernoSRMAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Inferno SRM Ammo";
        ammo.internalName = "BA-Inferno SRM Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = "BAInfernoSRM Ammo";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_BA_INFERNO;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 1;
        ammo.hittable = false;
        ammo.bv = 0;
        ammo.munitionType = M_INFERNO;
        
        return ammo;
    }
    public static AmmoType createBAAdvancedSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Advanced SRM 2 Ammo";
        ammo.internalName = "BA-Advanced SRM2 Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = "BAAdvancedSRM2 Ammo";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_ADVANCED;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 2;
        ammo.hittable = false;
        ammo.bv = 0;
        
        return ammo;
    }
    public static AmmoType createBAMicroBombAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Micro Bomb Ammo";
        ammo.internalName = "BA-Micro Bomb Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = "BAMicroBomb Ammo";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_BA_MICRO_BOMB;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 1;
        ammo.hittable = false;
        ammo.bv = 0;
        
        return ammo;
    }
    public static AmmoType createCLTorpedoLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Torpedo/LRM 5 Ammo";
        ammo.internalName = "Clan Torpedo/LRM5 Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = "CLTorpedoLRM5 Ammo";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM_TORPEDO_COMBO;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 1;
        ammo.hittable = false;
        ammo.bv = 0;
        
        return ammo;
    }
    public static AmmoType createFenrirSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "SRM 4 Ammo";
        ammo.internalName = "Fenrir SRM-4 Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = "FenrirSRM4 Ammo";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 4;
        ammo.hittable = false;
        ammo.bv = 0;
        
        return ammo;
    }
    public static AmmoType createBACompactNarcAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Compact Narc Ammo";
        ammo.internalName = BattleArmor.IS_DISPOSABLE_NARC_AMMO;
        ammo.mepName = ammo.internalName;
        ammo.mtfName = "BACompactNarc Ammo";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 2; // only used for ammo crits
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_NARC;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 2;
        ammo.hittable = false;
        ammo.bv = 0;
        
        return ammo;
    }
    public static AmmoType createBAMineLauncherAmmo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "Mine Launcher Ammo";
        ammo.internalName = "BA-Mine Launcher Ammo";
        ammo.mepName = ammo.internalName;
        ammo.mtfName = "BAMineLauncher Ammo";
        ammo.tdbName = "N/A";
        ammo.damagePerShot = 4;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_MINE;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 1;
        ammo.bv = 0;
        
        return ammo;
    }

    public static AmmoType createBALRM5Ammo() {
        AmmoType ammo = new AmmoType();
        
        ammo.name = "LRM 5 Ammo";
        ammo.internalName = "BA Ammo LRM-5";
        ammo.mepName = "BA Ammo LRM-5";
        ammo.mtfName = "BALRM5 Ammo";
        ammo.tdbName = "BA LRM 5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.flags |= F_BATTLEARMOR;
        ammo.shots = 6;
        ammo.bv = 0;
        
        return ammo;
    }

    //Proto Ammos
    public static AmmoType createCLPROLRM1Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 1 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-1";
        ammo.mtfName = "CLPRMLRM1 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM2Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 2 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-2";
        ammo.mtfName = "CLPRMLRM2 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 3;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM3Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 3 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-3";
        ammo.mtfName = "CLPRMLRM3 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 4;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM4Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 4 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-4";
        ammo.mtfName = "CLPRMLRM4 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 5;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM5Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 5 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-5";
        ammo.mtfName = "CLPRMLRM5 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM6Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 6 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-6";
        ammo.mtfName = "CLPRMLRM6 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 7;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM7Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 7 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-7";
        ammo.mtfName = "CLPRMLRM7 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 7;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 1245104;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM8Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 8 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-8";
        ammo.mtfName = "CLPRMLRM8 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 8;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 4328425;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM9Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 9 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-9";
        ammo.mtfName = "CLPRMLRM9 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 9;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 0;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM10Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 10 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-10";
        ammo.mtfName = "CLPRMLRM10 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 10;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 1245120;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM11Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 11 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-11";
        ammo.mtfName = "CLPRMLRM11 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 11;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 4241785;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM12Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 12 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-12";
        ammo.mtfName = "CLPRMLRM12 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 12;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 1;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM13Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 13 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-13";
        ammo.mtfName = "CLPRMLRM13 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 13;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 4460144;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM14Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 14 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-14";
        ammo.mtfName = "CLPRMLRM14 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 14;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 4459952;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM15Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 15 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-15";
        ammo.mtfName = "CLPRMLRM15 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 15;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 34603536;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM16Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 16 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-16";
        ammo.mtfName = "CLPRMLRM16 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 16;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 58851856;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM17Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 17 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-17";
        ammo.mtfName = "CLPRMLRM17 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 17;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 2147348480;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM18Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 18 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-18";
        ammo.mtfName = "CLPRMLRM18 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 18;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 0;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM19Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 19 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-19";
        ammo.mtfName = "CLPRMLRM19 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 19;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = -2141754256;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }

    public static AmmoType createCLPROLRM20Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "LRM 20 Ammo";
        ammo.internalName ="Clan Ammo Protomech LRM-20";
        ammo.mtfName = "CLPRMLRM20 Ammo";
        ammo.damagePerShot = 1;
        ammo.rackSize = 20;
        ammo.ammoType = AmmoType.T_LRM;
        ammo.shots = 100;
        ammo.bv = 1245076;
        ammo.flags |=F_PROTOMECH;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;
        return ammo;

    }




    public static AmmoType createCLPROSRM1Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "SRM 1 Ammo";
        ammo.internalName = "Clan Ammo Protomech SRM-1";
        ammo.mtfName = "CLPRMSRM1 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 100;
        ammo.bv = 2;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }
    public static AmmoType createCLPROSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "SRM 2 Ammo";
        ammo.internalName = "Clan Ammo Protomech SRM-2";
        ammo.mtfName = "CLPRMSRM2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 100;
        ammo.bv = 3;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }
    public static AmmoType createCLPROSRM3Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "SRM 3 Ammo";
        ammo.internalName = "Clan Ammo Protomech SRM-3";
        ammo.mtfName = "CLPRMSRM3 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 100;
        ammo.bv = 4;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }
    public static AmmoType createCLPROSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "SRM 4 Ammo";
        ammo.internalName = "Clan Ammo Protomech SRM-4";
        ammo.mtfName = "CLPRMSRM4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 100;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }
    public static AmmoType createCLPROSRM5Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "SRM 5 Ammo";
        ammo.internalName = "Clan Ammo Protomech SRM-5";
        ammo.mtfName = "CLPRMSRM5 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 100;
        ammo.bv = 6;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }
    public static AmmoType createCLPROSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "SRM 6 Ammo";
        ammo.internalName = "Clan Ammo Protomech SRM-6";
        ammo.mtfName = "CLPRMSRM6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM;
        ammo.shots = 100;
        ammo.bv = 7;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }
    public static AmmoType createCLPROStreakSRM1Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Streak SRM 1 Ammo";
        ammo.internalName = "Clan Protomech Streak SRM 1 Ammo";
        ammo.mtfName = "CLPRMStreakSRM1 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 1;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 3;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLPROStreakSRM2Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Streak SRM 2 Ammo";
        ammo.internalName = "Clan Protomech Streak SRM 2 Ammo";
        ammo.mtfName = "CLPRMStreakSRM2 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 2;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 5;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLPROStreakSRM3Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Streak SRM 3 Ammo";
        ammo.internalName = "Clan Protomech Streak SRM 3 Ammo";
        ammo.mtfName = "CLPRMStreakSRM3 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 3;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 8;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLPROStreakSRM4Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Streak SRM 4 Ammo";
        ammo.internalName = "Clan Protomech Streak SRM 4 Ammo";
        ammo.mtfName = "CLPRMStreakSRM4 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 4;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 10;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLPROStreakSRM5Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Streak SRM 5 Ammo";
        ammo.internalName = "Clan Protomech Streak SRM 5 Ammo";
        ammo.mtfName = "CLPRMStreakSRM5 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 5;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 13;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public static AmmoType createCLPROStreakSRM6Ammo() {
        AmmoType ammo = new AmmoType();
        ammo.name = "Streak SRM 6 Ammo";
        ammo.internalName = "Clan Protomech Streak SRM 6 Ammo";
        ammo.mtfName = "CLPRMStreakSRM6 Ammo";
        ammo.damagePerShot = 2;
        ammo.rackSize = 6;
        ammo.ammoType = AmmoType.T_SRM_STREAK;
        ammo.shots = 100;
        ammo.bv = 15;
        ammo.techType = TechConstants.T_CLAN_LEVEL_2;

        return ammo;
    }

    public String toString() {
        return "Ammo: " + name;
    }

    public static boolean canClearMinefield(AmmoType at) {
	
        if (at != null &&
            (at.getAmmoType() == T_LRM ||
             at.getAmmoType() == T_MRM) &&
            at.getRackSize() >= 20 &&
            at.getMunitionType() == M_STANDARD) {
            return true;
        }
		
        return false;
    }

    public static boolean canDeliverMinefield(AmmoType at) {
	
        if (at != null &&
            at.getAmmoType() == T_LRM &&
            ( (at.getMunitionType() == M_THUNDER)
              || (at.getMunitionType() == M_THUNDER_INFERNO)
              || (at.getMunitionType() == M_THUNDER_AUGMENTED)
              || (at.getMunitionType() == M_THUNDER_VIBRABOMB)
              || (at.getMunitionType() == M_THUNDER_ACTIVE) ) ) {
            return true;
        }
		
        return false;
    }

}
