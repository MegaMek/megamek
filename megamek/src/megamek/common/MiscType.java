/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * MiscType.java
 *
 * Created on April 2, 2002, 12:15 PM
 */

package megamek.common;

import java.util.Enumeration;

/**
 *
 * @author  Ben
 * @version
 */
public class MiscType extends EquipmentType {
    // equipment flags (okay, like every type of equipment has its own flag)
    public static final int     F_HEAT_SINK         = 0x0001;
    public static final int     F_DOUBLE_HEAT_SINK  = 0x0002;
    public static final int     F_JUMP_JET          = 0x0004;
    public static final int     F_CLUB              = 0x0008;
    public static final int     F_HATCHET           = 0x0010;
    public static final int     F_TREE_CLUB         = 0x0020;
    public static final int     F_CASE              = 0x0040;
    public static final int     F_MASC              = 0x0080;
    public static final int     F_TSM               = 0x0100;
    public static final int     F_C3M               = 0x0200;
    public static final int     F_C3S               = 0x0400;
    public static final int     F_C3I               = 0x0800;
    public static final int     F_ARTEMIS           = 0x1000;
    public static final int     F_ECM               = 0x2000;
    public static final int     F_TARGCOMP          = 0x4000;
    public static final int     F_OTHER             = 0x8000;
    public static final int     F_BAP               = 0x00010000;
    public static final int     F_TAG               = 0x00020000;
    public static final int     F_BOARDING_CLAW     = 0x00040000;
    public static final int     F_ASSAULT_CLAW      = 0x00080000;
    public static final int     F_FIRE_RESISTANT    = 0x00100000;
    public static final int     F_STEALTH           = 0x00200000;
    public static final int     F_MINE              = 0x00400000;
    public static final int     F_MINESWEEPER       = 0x00800000;
    public static final int     F_MAGNETIC_CLAMP    = 0x01000000;
    public static final int     F_PARAFOIL          = 0x02000000;
    public static final int     F_FERRO_FIBROUS     = 0x04000000;
    public static final int     F_ENDO_STEEL        = 0x08000000;
    public static final int     F_AP_POD            = 0x10000000;

    // Define constants for Ferro-Fibrous and Endo-Steel.
    public static final String  FERRO_FIBROUS       = "Ferro-Fibrous";
    public static final String  ENDO_STEEL          = "Endo Steel";

    
    /** Creates new MiscType */
    public MiscType() {
        ;
    }
    
    
    public float getTonnage(Entity entity) {
        if (tonnage != TONNAGE_VARIABLE) {
            return tonnage;
        }
        // check for known formulas
        if (hasFlag(F_JUMP_JET)) {
            if (entity.getWeight() >= 55.0) {
                return 0.5f;
            } else if (entity.getWeight() >= 85.0) {
                return 1.0f;
            } else {
                return 2.0f;
            }
        } else if (hasFlag(F_HATCHET)) {
            return (float)Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_MASC)) {
            if (entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
                return (float)Math.round(entity.getWeight() / 25.0f);
            }
            else {
                return (float)Math.round(entity.getWeight() / 20.0f);
            }
        } else if (hasFlag(F_TARGCOMP)) {
            // based on tonnage of direct_fire weaponry
            double fTons = 0.0;
            for (Enumeration e = entity.getWeapons(); e.hasMoreElements(); ) {
                Mounted m = (Mounted)e.nextElement();
                WeaponType wt = (WeaponType)m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += wt.getTonnage(entity);
                }
            }
            if (entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
                return (float)Math.ceil(fTons / 5.0f);
            }
            else {
                return (float)Math.ceil(fTons / 4.0f);
            }
        } else if ( MiscType.FERRO_FIBROUS.equals(internalName) ) {
            double tons = 0.0;
            if ( entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2 ) {
                tons = entity.getTotalOArmor() / ( 16 * 1.2 );
            } else {
                tons = entity.getTotalOArmor() / ( 16 * 1.12 );
            }
            tons = (double) Math.ceil( tons * 2.0 ) / 2.0;
            return (float) tons;
        } else if ( MiscType.ENDO_STEEL.equals(internalName) ) {
            double tons = 0.0;
            tons = (double)Math.ceil( entity.getWeight() / 10.0 ) / 2.0;
            return (float) tons;
        }
        
        // okay, I'm out of ideas
        return 1.0f;
    }
    
    public int getCriticals(Entity entity) {
        if (criticals != CRITICALS_VARIABLE) {
            return criticals;
        }
        // check for known formulas
        if (hasFlag(F_HATCHET)) {
            return (int)Math.ceil(entity.getWeight() / 15.0);
        } else if ( hasFlag(F_DOUBLE_HEAT_SINK) ) {
            if ( entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2 ) {
                return 2;
            } else {
                return 3;
            }
        } else if (hasFlag(F_MASC)) {
            if (entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
                return Math.round(entity.getWeight() / 25.0f);
            }
            else {
                return Math.round(entity.getWeight() / 20.0f);
            }
        } else if (hasFlag(F_TARGCOMP)) {
           // based on tonnage of direct_fire weaponry
            double fTons = 0.0;
            for (Enumeration e = entity.getWeapons(); e.hasMoreElements(); ) {
                Mounted m = (Mounted)e.nextElement();
                WeaponType wt = (WeaponType)m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += wt.getTonnage(entity);
                }
            }
            if (entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
                return (int)Math.ceil(fTons / 5.0f);
            }
            else {
                return (int)Math.ceil(fTons / 4.0f);
            }
        } else if ( MiscType.FERRO_FIBROUS.equals(internalName) ) {
            if ( entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2 ) {
                return 7;
            } else {
                return 14;
            }
        } else if ( MiscType.ENDO_STEEL.equals(internalName) ) {
            if ( entity.getTechLevel() == TechConstants.T_CLAN_LEVEL_2 ) {
                return 7;
            } else {
                return 14;
            }
        }
        // right, well I'll just guess then
        return 1;
    }
    
    public double getBV(Entity entity) {
        if (bv != BV_VARIABLE) {
            return bv;
        }
        // check for known formulas
        if (hasFlag(F_HATCHET)) {
            return ((float)Math.ceil(entity.getWeight() / 5.0) * 1.5f);
        } else if (hasFlag(F_TARGCOMP)) {
            // 20% of direct_fire weaponry BV (half for rear-facing)
            double fFrontBV = 0.0, fRearBV = 0.0;
            for (Enumeration e = entity.getWeapons(); e.hasMoreElements(); ) {
                Mounted m = (Mounted)e.nextElement();
                WeaponType wt = (WeaponType)m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if (m.isRearMounted()) {
                        fRearBV += wt.getBV(entity);
                    } else {
                        fFrontBV += wt.getBV(entity);
                    }
                }
            }
            if (fFrontBV > fRearBV) {
                return (float)(fFrontBV * 0.2 + fRearBV * 0.1);
            } else {
                return (float)(fRearBV * 0.2 + fFrontBV * 0.1);
            }
        }
        // maybe it's 0
        return 0;
    }
    
    
    /**
     * Add all the types of misc eq we can create to the list
     */
    public static void initializeTypes() {
        // all tech level 1 stuff
        EquipmentType.addType(createHeatSink());
        EquipmentType.addType(createJumpJet());
        EquipmentType.addType(createTreeClub());
        EquipmentType.addType(createGirderClub());
        EquipmentType.addType(createLimbClub());
        EquipmentType.addType(createHatchet());
        
        // Start of Level2 stuff
        EquipmentType.addType(createDoubleHeatSink());
        EquipmentType.addType(createISDoubleHeatSink());
        EquipmentType.addType(createCLDoubleHeatSink());
        EquipmentType.addType(createISCASE());
        EquipmentType.addType(createCLCASE());
        EquipmentType.addType(createISMASC());
        EquipmentType.addType(createCLMASC());
        EquipmentType.addType(createTSM());
        EquipmentType.addType(createC3S());
        EquipmentType.addType(createC3M());
        EquipmentType.addType(createC3I());
        EquipmentType.addType(createISArtemis());
        EquipmentType.addType(createCLArtemis());
        EquipmentType.addType(createGECM());
        EquipmentType.addType(createCLECM());
        EquipmentType.addType(createISTargComp());
        EquipmentType.addType(createCLTargComp());
        EquipmentType.addType(createMekStealth());
        EquipmentType.addType(createFerroFibrous());
        EquipmentType.addType(createEndoSteel());
        EquipmentType.addType(createISEndoSteel());
        EquipmentType.addType(createBeagleActiveProbe());
        EquipmentType.addType(createCLActiveProbe());
        EquipmentType.addType(createCLLightActiveProbe());
        EquipmentType.addType(createISTAG());
        EquipmentType.addType(createISLightTAG());
        EquipmentType.addType(createCLTAG());
        EquipmentType.addType(createCLLightTAG());
        EquipmentType.addType(createISAPPod());
        EquipmentType.addType(createCLAPPod());

        // Start BattleArmor equipment
        EquipmentType.addType( createBABoardingClaw() );
        EquipmentType.addType( createBAAssaultClaws() );
        EquipmentType.addType( createBAFireResistantArmor() );
        EquipmentType.addType( createBasicStealth() );
        EquipmentType.addType( createStandardStealth() );
        EquipmentType.addType( createImprovedStealth() );
        EquipmentType.addType( createMine() );
        EquipmentType.addType( createMinesweeper() );
        EquipmentType.addType( createBAMagneticClamp() );
        EquipmentType.addType( createSingleHexECM() );
        EquipmentType.addType( createMimeticCamo() );
        EquipmentType.addType( createParafoil() );
    }
    
    public static MiscType createHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Heat Sink";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = misc.name;
        misc.tdbName = "Heat Sink";
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.flags |= F_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createJumpJet() {
        MiscType misc = new MiscType();
        
        misc.name = "Jump Jet";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = misc.name;
        misc.tdbName = "Jump Jet";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags |= F_JUMP_JET;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createTreeClub() {
        MiscType misc = new MiscType();
        
        misc.name = "Tree Club";
        misc.internalName = misc.name;
        misc.mepName = "N/A";
        misc.mtfName = misc.mepName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags |= F_TREE_CLUB | F_CLUB;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createGirderClub() {
        MiscType misc = new MiscType();
        
        misc.name = "Girder Club";
        misc.internalName = misc.name;
        misc.mepName = "N/A";
        misc.mtfName = misc.mepName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags |= F_CLUB;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createLimbClub() {
        MiscType misc = new MiscType();
        
        misc.name = "Limb Club";
        misc.internalName = misc.name;
        misc.mepName = "N/A";
        misc.mtfName = misc.mepName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags |= F_CLUB;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createHatchet() {
        MiscType misc = new MiscType();
        
        misc.name = "Hatchet";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = misc.name;
        misc.tdbName = "Hatchet";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.flags |= F_HATCHET;
        misc.bv = BV_VARIABLE;
        
        return misc;
    }
    
    // Start of Level2 stuff
    
    // REMOVE ME WHEN HMPREAD IS UPDATED!
    public static MiscType createDoubleHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Double Heat Sink";
        misc.internalName = "REMOVE MEEE!!";
        misc.mepName = "REMOVE ME!";
        misc.mtfName = "Double Heat Sink";
        misc.tdbName = "REMOVE ME!";
        misc.tonnage = 1.0f;
        misc.criticals = CRITICALS_VARIABLE;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISDoubleHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Double Heat Sink";
        misc.internalName = "ISDoubleHeatSink";
        misc.mepName = "IS Double Heat Sink";
        misc.mtfName = "ISDouble Heat Sink";
        misc.tdbName = "IS Double Heat Sink";
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createCLDoubleHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Double Heat Sink";
        misc.internalName = "CLDoubleHeatSink";
        misc.mepName = "Clan Double Heat Sink";
        misc.mtfName = "CLDouble Heat Sink";
        misc.tdbName = "Clan Double Heat Sink";
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISCASE() {
        MiscType misc = new MiscType();
        
        misc.name = "CASE";
        misc.internalName = "ISCASE";
        misc.mepName ="IS CASE";
        misc.mtfName = "ISCASE";
        misc.tdbName = "IS CASE";
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags |= F_CASE;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createCLCASE() {
        MiscType misc = new MiscType();
        
        misc.name = "CASE";
        misc.internalName = "CLCASE";
        misc.mepName = "Clan CASE";
        misc.mtfName = "CLCASE";
        misc.tdbName = "Clan CASE";
        misc.tonnage = 0.0f;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags |= F_CASE;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISMASC() {
        MiscType misc = new MiscType();
        
        misc.name = "MASC";
        misc.internalName = "ISMASC";
        misc.mepName = "IS MASC";
        misc.mtfName = misc.internalName;
        misc.tdbName = "IS MASC";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MASC;
        misc.bv = 0;
        
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        
        return misc;
    }
    
    public static MiscType createCLMASC() {
        MiscType misc = new MiscType();
        
        misc.name = "MASC";
        misc.internalName = "CLMASC";
        misc.mepName = "Clan MASC";
        misc.mtfName = misc.internalName;
        misc.tdbName = "Clan MASC";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MASC;
        misc.bv = 0;
        
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        
        return misc;
    }
    
    public static MiscType createTSM() {
        MiscType misc = new MiscType();
        
        misc.name = "TSM";
        misc.internalName = misc.name;
        misc.mepName = "IS TSM";
        misc.mtfName = "Triple Strength Myomer";
        misc.tdbName = "Triple Strength Myomer";
        misc.tonnage = 0;
        misc.criticals = 6;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_TSM;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createC3S() {
        MiscType misc = new MiscType();
        
        misc.name = "C3 Slave";
        misc.internalName = "ISC3SlaveUnit";
        misc.mepName = "IS C3 Slave";
        misc.mtfName = "ISC3SlaveUnit";
        misc.tdbName = "IS C3 Slave";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_C3S;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createC3M() {
        MiscType misc = new MiscType();
        
        misc.name = "C3 Master";
        misc.internalName = misc.name;
        misc.mepName = "IS C3 Computer";
        misc.mtfName = "ISC3MasterComputer";
        misc.tdbName = "IS C3 Computer";
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_C3M;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createC3I() {
        MiscType misc = new MiscType();
        
        misc.name = "C3i Computer";
        misc.internalName = misc.name;
        misc.mepName = misc.name;
        misc.mtfName = "ISImprovedC3CPU";
        misc.tdbName = "IS C3i Computer";
        misc.tonnage = 2.5f;
        misc.criticals = 2;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_C3I;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISArtemis() {
        MiscType misc = new MiscType();
        misc.name = "Artemis IV FCS";
        misc.mtfName = "ISArtemisIV";
        misc.tdbName = "IS Artemis IV FCS";
        misc.mepName = "IS Artemis IV FCS";
        misc.internalName = misc.mtfName;
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.flags |= F_ARTEMIS;
        return misc;
    }
    
    public static MiscType createCLArtemis() {
        MiscType misc = new MiscType();
        misc.name = "Artemis IV FCS";
        misc.mtfName = "CLArtemisIV";
        misc.tdbName = "Clan Artemis IV FCS";
        misc.mepName = "Clan Artemis IV FCS";
        misc.internalName = misc.mtfName;
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.flags |= F_ARTEMIS;
        return misc;
    }
        
    public static MiscType createGECM() {
        MiscType misc = new MiscType();
        
        misc.name = "Guardian ECM Suite";
        misc.internalName = misc.name;
        misc.mepName = "IS Guardian ECM";
        misc.mtfName = "ISGuardianECM";
        misc.tdbName = "IS Guardian ECM Suite";
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_ECM;
        misc.bv = 61;
        
        return misc;
    }

    public static MiscType createCLECM() {
        MiscType misc = new MiscType();
        
        misc.name = "ECM Suite";
        misc.internalName = misc.name;
        misc.mepName = "Clan ECM Suite";
        misc.mtfName = "CLECMSuite";
        misc.tdbName = "Clan ECM Suite";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_ECM;
        misc.bv = 61;
        
        return misc;
    }
    
    /**
     * Targeting comps should NOT be spreadable.  However, I've set them such
     * as a temp measure to overcome the following bug:
     * TC space allocation is calculated based on tonnage of direct-fire weaponry.
     * However, since meks are loaded location-by-location, when the TC is loaded
     * it's very unlikely that all of the weaponry will be attached, resulting in
     * undersized comps.  Any remaining TC crits after the last expected one are
     * being handled as a 2nd TC, causing LocationFullExceptions.
     */
    
    public static MiscType createISTargComp() {
        MiscType misc = new MiscType();
        
        misc.name = "Targeting Computer";
        misc.internalName = "ISTargeting Computer";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "IS Targeting Computer";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.bv = BV_VARIABLE;
        misc.flags |= F_TARGCOMP;
        // see note above
        misc.spreadable = true;
        
        return misc;
    }
    
    public static MiscType createCLTargComp() {
        MiscType misc = new MiscType();
        
        misc.name = "Targeting Computer";
        misc.internalName = "CLTargeting Computer";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "Clan Targeting Computer";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.bv = BV_VARIABLE;
        misc.flags |= F_TARGCOMP;
        // see note above
        misc.spreadable = true;
        
        return misc;
    }

    // Start BattleArmor equipment
    public static MiscType createBABoardingClaw() {
        MiscType misc = new MiscType();
        
        misc.name = "Boarding Claw";
        misc.internalName = BattleArmor.BOARDING_CLAW;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_BOARDING_CLAW;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createBAAssaultClaws() {
        MiscType misc = new MiscType();
        
        misc.name = "Assault Claws";
        misc.internalName = BattleArmor.ASSAULT_CLAW;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_ASSAULT_CLAW;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createBAFireResistantArmor() {
        MiscType misc = new MiscType();
        
        misc.name = "Fire Resistant Armor";
        misc.internalName = BattleArmor.FIRE_PROTECTION;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_FIRE_RESISTANT;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createBasicStealth() {
        MiscType misc = new MiscType();
        
        misc.name = BattleArmor.STEALTH;
        misc.internalName = BattleArmor.STEALTH;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_STEALTH;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createStandardStealth() {
        MiscType misc = new MiscType();
        
        misc.name = BattleArmor.ADVANCED_STEALTH;
        misc.internalName = BattleArmor.ADVANCED_STEALTH;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_STEALTH;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createImprovedStealth() {
        MiscType misc = new MiscType();
        
        misc.name = BattleArmor.EXPERT_STEALTH;
        misc.internalName = BattleArmor.EXPERT_STEALTH;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_STEALTH;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createMine() {
        MiscType misc = new MiscType();
        
        misc.name = "Mine";
        misc.internalName = "Mine";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_MINE;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createMinesweeper() {
        MiscType misc = new MiscType();
        
        misc.name = "Minesweeper";
        misc.internalName = "Minesweeper";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_MINESWEEPER;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createBAMagneticClamp() {
        MiscType misc = new MiscType();
        
        misc.name = "Magnetic Clamp";
        misc.internalName = BattleArmor.MAGNETIC_CLAMP;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_MAGNETIC_CLAMP;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createSingleHexECM() {
        MiscType misc = new MiscType();
        
        misc.name = BattleArmor.SINGLE_HEX_ECM;
        misc.internalName = BattleArmor.SINGLE_HEX_ECM;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_ECM;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createMimeticCamo() {
        MiscType misc = new MiscType();
        
        misc.name = BattleArmor.MIMETIC_CAMO;
        misc.internalName = BattleArmor.MIMETIC_CAMO;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_STEALTH;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createParafoil() {
        MiscType misc = new MiscType();
        
        misc.name = "Parafoil";
        misc.internalName = "Parafoil";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_PARAFOIL;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createMekStealth() {
        MiscType misc = new MiscType();
        
        misc.name = "Stealth Armor";
        misc.internalName = Mech.STEALTH;
        misc.mepName = misc.internalName;
        misc.mtfName = "Stealth Armor";
        misc.tdbName = "Stealth Armor";
        misc.tonnage = 0;       //???
        misc.criticals = 12;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_STEALTH;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.bv = 0;            //???
        
        return misc;
    }

    public static MiscType createFerroFibrous() {
        MiscType misc = new MiscType();
        
        misc.name = MiscType.FERRO_FIBROUS;
        misc.internalName = MiscType.FERRO_FIBROUS;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "Ferro-Fibrous Armor";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_FERRO_FIBROUS;
        misc.bv = 0;            //???
        
        return misc;
    }

    public static MiscType createEndoSteel() {
        MiscType misc = new MiscType();
        
        misc.name = MiscType.ENDO_STEEL;
        misc.internalName = MiscType.ENDO_STEEL;
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "EndoSteel";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_ENDO_STEEL;
        misc.bv = 0;            //???
        
        return misc;
    }

    public static MiscType createISEndoSteel() {
        MiscType misc = new MiscType();
        
        misc.name = MiscType.ENDO_STEEL;
        misc.internalName = MiscType.ENDO_STEEL;
        misc.mepName = misc.internalName;
        misc.mtfName = "Endo-Steel";
        misc.tdbName = "EndoSteel";
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_ENDO_STEEL;
        misc.bv = 0;            //???
        
        return misc;
    }

    public static MiscType createBeagleActiveProbe() {
        MiscType misc = new MiscType();
        
        misc.name = "Beagle Active Probe";
        misc.internalName = "BeagleActiveProbe";
        misc.mepName = "Beagle Active Probe";
        misc.mtfName = "ISBeagleActiveProbe";
        misc.tdbName = "IS Beagle Active Probe";
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_BAP;
        misc.bv = 10;
        
        return misc;
    }

    public static MiscType createCLActiveProbe() {
        MiscType misc = new MiscType();
        
        misc.name = "Clan Active Probe";
        misc.internalName = "CLActiveProbe";
        misc.mepName = "Active Probe";
        misc.mtfName = "CLActiveProbe";
        misc.tdbName = "Clan Active Probe";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_BAP;
        misc.bv = 12;
        
        return misc;
    }

    public static MiscType createCLLightActiveProbe() {
        MiscType misc = new MiscType();
        
        misc.name = "Light Active Probe";
        misc.internalName = "CLLightActiveProbe";
        misc.mepName = "CL Light Active Probe";
        misc.mtfName = "Light Active Probe";
        misc.tdbName = "Clan Light Active Probe";
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_BAP;
        misc.bv = 7;
        
        return misc;
    }

    public static MiscType createISTAG() {
        MiscType misc = new MiscType();
        
        misc.name = "IS TAG";
        misc.internalName = "ISTAG";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "IS TAG";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_TAG;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createISLightTAG() {
        MiscType misc = new MiscType();
        
        misc.name = "IS Light TAG";
        misc.internalName = "ISLightTAG";
        misc.mepName = "Light TAG";
        misc.mtfName = misc.internalName;
        misc.tdbName = "N/A";
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_TAG;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createCLTAG() {
        MiscType misc = new MiscType();
        
        misc.name = "Clan TAG";
        misc.internalName = "CLTAG";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "Clan TAG";
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_TAG;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createCLLightTAG() {
        MiscType misc = new MiscType();
        
        misc.name = "Clan Light TAG";
        misc.internalName = "CLLightTAG";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "Clan Light TAG";
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_TAG;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createISAPPod() {
        MiscType misc = new MiscType();
        
        misc.name = "IS AP Pod";
        misc.internalName = "ISAPPod";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "IS A-Pod";
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_AP_POD;
        misc.bv = 1;
        
        return misc;
    }

    public static MiscType createCLAPPod() {
        MiscType misc = new MiscType();
        
        misc.name = "CL AP Pod";
        misc.internalName = "CLAntiPersonnelPod";
        misc.mepName = misc.internalName;
        misc.mtfName = misc.internalName;
        misc.tdbName = "Clan A-Pod";
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_AP_POD;
        misc.bv = 1;
        
        return misc;
    }

}
