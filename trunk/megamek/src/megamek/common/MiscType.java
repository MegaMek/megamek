/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
    //public static final int     F_OTHER             = 0x8000;
    public static final int     F_BAP               = 0x00010000;
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
    public static final int     F_SEARCHLIGHT       = 0x20000000;
    public static final int     F_SWORD             = 0x40000000;
    public static final int   F_FERRO_FIBROUS_PROTO = 0x80000000;
    public static final int   F_ENDO_STEEL_PROTO    = 0x8000;
    public static final int     T_TARGSYS_UNKNOWN           = -1;
    public static final int     T_TARGSYS_STANDARD          = 0;
    public static final int     T_TARGSYS_TARGCOMP          = 1;
    public static final int     T_TARGSYS_LONGRANGE         = 2;
    public static final int     T_TARGSYS_SHORTRANGE        = 3;
    public static final int     T_TARGSYS_VARIABLE_RANGE    = 4;
    public static final int     T_TARGSYS_ANTI_AIR          = 5;
    public static final int     T_TARGSYS_MULTI_TRAC        = 6;
    public static final int     T_TARGSYS_MULTI_TRAC_II     = 7;
    public static final String[] targSysNames = {"Standard Targetting System",
                                                    "Targetting Computer",
                                                    "Long-Range Targetting System",
                                                    "Short-Range Targetting System",
                                                    "Variable-Range Targetting System",
                                                    "Anti-Air Targetting System",
                                                    "Multi-Trac Targetting System",
                                                    "Multi-Trac II Targetting System"};
    
    /** Creates new MiscType */
    public MiscType() {

    }
    
    
    public float getTonnage(Entity entity) {
        if (tonnage != TONNAGE_VARIABLE) {
            return tonnage;
        }
        // check for known formulas
        if (hasFlag(F_JUMP_JET)) {
            if (entity.getWeight() <= 55.0) {
                return 0.5f;
            } else if (entity.getWeight() <= 85.0) {
                return 1.0f;
            } else {
                return 2.0f;
            }
        } else if (hasFlag(F_HATCHET)) {
            return (float)Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_SWORD)) {
            return (float)(Math.ceil(entity.getWeight() / 20.0 * 2.0) / 2.0);
        } else if (hasFlag(F_MASC)) {
            if (entity.isClan()) {
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
            if (entity.isClan()) {
                return (float)Math.ceil(fTons / 5.0f);
            }
            else {
                return (float)Math.ceil(fTons / 4.0f);
            }
        } else if ( EquipmentType.getArmorTypeName(T_ARMOR_FERRO_FIBROUS).equals(internalName) ) {
            double tons = 0.0;
            if ( entity.isClan()) {
                tons = entity.getTotalOArmor() / ( 16 * 1.2 );
            } else {
                tons = entity.getTotalOArmor() / ( 16 * 1.12 );
            }
            tons = (double) Math.ceil( tons * 2.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getArmorTypeName(T_ARMOR_LIGHT_FERRO).equals(internalName) ) {
            double tons = entity.getTotalOArmor() / (16*1.06);
            tons = (double) Math.ceil( tons * 2.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getArmorTypeName(T_ARMOR_HEAVY_FERRO).equals(internalName) ) {
            double tons = entity.getTotalOArmor() / (16*1.24);
            tons = (double) Math.ceil( tons * 2.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL).equals(internalName) ) {
            double tons = 0.0;
            tons = (double)Math.ceil( entity.getWeight() / 10.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE).equals(internalName) ) {
            double tons = 0.0;
            tons = (double)Math.ceil( entity.getWeight() / 10.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_REINFORCED).equals(internalName) ) {
            double tons = 0.0;
            tons = (double)Math.ceil( entity.getWeight() / 10.0 ) * 2.0;
            return (float) tons;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_COMPOSITE).equals(internalName) ) {
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
        if (hasFlag(F_HATCHET) || hasFlag(F_SWORD)) {
            return (int)Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_MASC)) {
            if (entity.isClan()) {
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
            if (entity.isClan()) {
                return (int)Math.ceil(fTons / 5.0f);
            }
            else {
                return (int)Math.ceil(fTons / 4.0f);
            }
        } else if ( EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS).equals(internalName) ) {
            if ( entity.isClan() ) {
                return 7;
            } else {
                return 14;
            }
        } else if ( EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO).equals(internalName) ) {
            return 16;
        } else if ( EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO).equals(internalName) ) {
            return 7;
        } else if ( EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO).equals(internalName) ) {
            return 21;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL).equals(internalName) ) {
            if ( entity.isClan() ) {
                return 7;
            } else {
                return 14;
            }
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE).equals(internalName) ) {
            return 16;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_REINFORCED).equals(internalName) ) {
            return 0;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_COMPOSITE).equals(internalName) ) {
            return 0;
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
            return Math.ceil(entity.getWeight() / 5.0) * 1.5;
        } else if (hasFlag(F_SWORD)) {
            return (Math.ceil(entity.getWeight() / 10.0) + 1.0) * 1.725;
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
                return fFrontBV * 0.2 + fRearBV * 0.1;
            } else {
                return fRearBV * 0.2 + fFrontBV * 0.1;
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
        EquipmentType.addType(createBeagleActiveProbe());
        EquipmentType.addType(createCLActiveProbe());
        EquipmentType.addType(createCLLightActiveProbe());
        EquipmentType.addType(createISAPPod());
        EquipmentType.addType(createCLAPPod());
        EquipmentType.addType(createSword());

        // Start of level 3 stuff
        EquipmentType.addType(createFerroFibrousPrototype());
        EquipmentType.addType(createLightFerroFibrous());
        EquipmentType.addType(createHeavyFerroFibrous());
        EquipmentType.addType(createEndoSteelPrototype());
        EquipmentType.addType(createReinforcedStructure());
        EquipmentType.addType(createCompositeStructure());

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
        EquipmentType.addType( createSimpleCamo() );
        EquipmentType.addType( createParafoil() );
        EquipmentType.addType( createBASearchlight() );
    }
    
    public static MiscType createHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Heat Sink";
        misc.setInternalName(misc.name);
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.flags |= F_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createJumpJet() {
        MiscType misc = new MiscType();
        
        misc.name = "Jump Jet";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags |= F_JUMP_JET;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createTreeClub() {
        MiscType misc = new MiscType();
        
        misc.name = "Tree Club";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags |= F_TREE_CLUB | F_CLUB;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createGirderClub() {
        MiscType misc = new MiscType();
        
        misc.name = "Girder Club";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags |= F_CLUB;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createLimbClub() {
        MiscType misc = new MiscType();
        
        misc.name = "Limb Club";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags |= F_CLUB;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createHatchet() {
        MiscType misc = new MiscType();
        
        misc.name = "Hatchet";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_HATCHET;
        misc.bv = BV_VARIABLE;
        
        return misc;
    }
    
    // Start of Level2 stuff
    
    public static MiscType createISDoubleHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Double Heat Sink";
        misc.setInternalName("ISDoubleHeatSink");
        misc.addLookupName("IS Double Heat Sink");
        misc.addLookupName("ISDouble Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createCLDoubleHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Double Heat Sink";
        misc.setInternalName("CLDoubleHeatSink");
        misc.addLookupName("Clan Double Heat Sink");
        misc.addLookupName("CLDouble Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISCASE() {
        MiscType misc = new MiscType();
        
        misc.name = "CASE";
        misc.setInternalName("ISCASE");
        misc.addLookupName("IS CASE");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags |= F_CASE;
        misc.cost=50000;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createCLCASE() {
        MiscType misc = new MiscType();
        
        misc.name = "CASE";
        misc.setInternalName("CLCASE");
        misc.addLookupName("Clan CASE");
        misc.tonnage = 0.0f;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags |= F_CASE;
        misc.cost=50000;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISMASC() {
        MiscType misc = new MiscType();
        
        misc.name = "MASC";
        misc.setInternalName("ISMASC");
        misc.addLookupName("IS MASC");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MASC;
        misc.bv = 0;
        
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);
        
        return misc;
    }
    
    public static MiscType createCLMASC() {
        MiscType misc = new MiscType();
        
        misc.name = "MASC";
        misc.setInternalName("CLMASC");
        misc.addLookupName("Clan MASC");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.cost = COST_VARIABLE;
        misc.spreadable = true;
        misc.flags |= F_MASC;
        misc.bv = 0;
        
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);
        
        return misc;
    }
    
    public static MiscType createTSM() {
        MiscType misc = new MiscType();
        
        misc.name = "TSM";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS TSM");
        misc.addLookupName("Triple Strength Myomer");
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
        misc.setInternalName("ISC3SlaveUnit");
        misc.addLookupName("IS C3 Slave");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.cost = 250000;
        misc.flags |= F_C3S;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createC3M() {
        MiscType misc = new MiscType();
        
        misc.name = "C3 Master";
        misc.setInternalName("ISC3MasterUnit");
        misc.addLookupName("IS C3 Computer");
        misc.addLookupName("ISC3MasterComputer");
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.hittable = true;
        misc.spreadable = false;
        misc.cost = 1500000;
        misc.flags |= F_C3M;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createC3I() {
        MiscType misc = new MiscType();
        
        misc.name = "C3i Computer";
        misc.setInternalName("ISC3iUnit");
        misc.addLookupName("ISImprovedC3CPU");
        misc.addLookupName("IS C3i Computer");
        misc.tonnage = 2.5f;
        misc.criticals = 2;
        misc.hittable = true;
        misc.spreadable = false;
        misc.cost = 750000;
        misc.flags |= F_C3I;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISArtemis() {
        MiscType misc = new MiscType();
        misc.name = "Artemis IV FCS";
        misc.setInternalName("ISArtemisIV");
        misc.addLookupName("IS Artemis IV FCS");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.cost = 100000;
        misc.flags |= F_ARTEMIS;
        return misc;
    }
    
    public static MiscType createCLArtemis() {
        MiscType misc = new MiscType();
        misc.name = "Artemis IV FCS";
        misc.setInternalName("CLArtemisIV");
        misc.addLookupName("Clan Artemis IV FCS");
        misc.tonnage = 1.0f;
        misc.cost = 100000;
        misc.criticals = 1;
        misc.flags |= F_ARTEMIS;
        return misc;
    }
        
    public static MiscType createGECM() {
        MiscType misc = new MiscType();
        
        misc.name = "Guardian ECM Suite";
        misc.setInternalName("ISGuardianECMSuite");
        misc.addLookupName("IS Guardian ECM");
        misc.addLookupName("ISGuardianECM");
        misc.addLookupName("IS Guardian ECM Suite");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.hittable = true;
        misc.cost = 200000;
        misc.spreadable = false;
        misc.flags |= F_ECM;
        misc.bv = 61;
        
        return misc;
    }

    public static MiscType createCLECM() {
        MiscType misc = new MiscType();
        
        misc.name = "ECM Suite";
        misc.setInternalName("CLECMSuite");
        misc.addLookupName("Clan ECM Suite");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 200000;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_ECM;
        misc.bv = 61;
        
        return misc;
    }

    public static MiscType createSword() {
        MiscType misc = new MiscType();
        
        misc.name = "Sword";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_SWORD;
        misc.bv = BV_VARIABLE;
        
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
        misc.setInternalName("ISTargeting Computer");
        misc.addLookupName("IS Targeting Computer");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = BV_VARIABLE;
        misc.flags |= F_TARGCOMP;
        // see note above
        misc.spreadable = true;
        String[] modes = { "Normal", "Aimed shot" };
        misc.setModes(modes);

        return misc;
    }
    
    public static MiscType createCLTargComp() {
        MiscType misc = new MiscType();
        
        misc.name = "Targeting Computer";
        misc.setInternalName("CLTargeting Computer");
        misc.addLookupName("Clan Targeting Computer");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = BV_VARIABLE;
        misc.flags |= F_TARGCOMP;
        // see note above
        misc.spreadable = true;
        String[] modes = { "Normal", "Aimed shot" };
        misc.setModes(modes);
        
        return misc;
    }

    // Start BattleArmor equipment
    public static MiscType createBABoardingClaw() {
        MiscType misc = new MiscType();
        
        misc.name = "Boarding Claw";
        misc.setInternalName(BattleArmor.BOARDING_CLAW);
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
        misc.setInternalName(BattleArmor.ASSAULT_CLAW);
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
        misc.setInternalName(BattleArmor.FIRE_PROTECTION);
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
        misc.setInternalName(BattleArmor.STEALTH);
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
        misc.setInternalName(BattleArmor.ADVANCED_STEALTH);
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
        misc.setInternalName(BattleArmor.EXPERT_STEALTH);
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
        misc.setInternalName("Mine");
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
        misc.setInternalName("Minesweeper");
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
        misc.setInternalName(BattleArmor.MAGNETIC_CLAMP);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_MAGNETIC_CLAMP;
        String[] saModes = { "On", "Off" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(true);
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createSingleHexECM() {
        MiscType misc = new MiscType();
        
        misc.name = BattleArmor.SINGLE_HEX_ECM;
        misc.setInternalName(BattleArmor.SINGLE_HEX_ECM);
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
        misc.setInternalName(BattleArmor.MIMETIC_CAMO);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_STEALTH;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createSimpleCamo() {
        MiscType misc = new MiscType();
        
        misc.name = BattleArmor.SIMPLE_CAMO;
        misc.setInternalName(BattleArmor.SIMPLE_CAMO);
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
        misc.setInternalName("Parafoil");
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
        misc.setInternalName(Mech.STEALTH);
        misc.addLookupName("Stealth Armor");
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
        
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS));
        misc.addLookupName("Ferro-Fibrous Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_FERRO_FIBROUS;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createFerroFibrousPrototype() {
        MiscType misc = new MiscType();
        
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO));
        misc.addLookupName("Ferro-Fibrous Armor Prototype");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_FERRO_FIBROUS;
        misc.bv = 0;
        misc.bv = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createLightFerroFibrous() {
        MiscType misc = new MiscType();
        
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO));
        misc.addLookupName("Light Ferro-Fibrous Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 7;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_FERRO_FIBROUS;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createHeavyFerroFibrous() {
        MiscType misc = new MiscType();
        
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_FERRO));
        misc.addLookupName("Heavy Ferro-Fibrous Armor");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 21;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_FERRO_FIBROUS;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createEndoSteel() {
        MiscType misc = new MiscType();
        
        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL));
        misc.addLookupName("Endo-Steel");
        misc.addLookupName("EndoSteel");
        misc.addLookupName("Endosteel");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_ENDO_STEEL;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createEndoSteelPrototype() {
        MiscType misc = new MiscType();
        
        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE));
        misc.addLookupName("Endo-Steel Prototype");
        misc.addLookupName("EndoSteelPrototype");
        misc.addLookupName("Endosteelprototype");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_ENDO_STEEL;
        misc.bv = 0;
        misc.bv = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createReinforcedStructure() {
        MiscType misc = new MiscType();
        
        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_REINFORCED);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_REINFORCED));
        misc.addLookupName("Reinforced");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = true;
        misc.bv = 0;
        misc.bv = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createCompositeStructure() {
        MiscType misc = new MiscType();
        
        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_COMPOSITE);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_COMPOSITE));
        misc.addLookupName("Composite");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = true;
        misc.bv = 0;
        misc.bv = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createBeagleActiveProbe() {
        MiscType misc = new MiscType();
        
        misc.name = "Beagle Active Probe";
        misc.setInternalName("BeagleActiveProbe");
        misc.addLookupName("Beagle Active Probe");
        misc.addLookupName("ISBeagleActiveProbe");
        misc.addLookupName("IS Beagle Active Probe");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.hittable = true;
        misc.cost = 200000;
        misc.spreadable = false;
        misc.flags |= F_BAP;
        misc.bv = 10;
        
        return misc;
    }

    public static MiscType createCLActiveProbe() {
        MiscType misc = new MiscType();
        
        misc.name = "Clan Active Probe";
        misc.setInternalName("CLActiveProbe");
        misc.addLookupName("Active Probe");
        misc.addLookupName("Clan Active Probe");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.hittable = true;
        misc.spreadable = false;
        misc.cost = 200000;
        misc.flags |= F_BAP;
        misc.bv = 12;
        
        return misc;
    }

    public static MiscType createCLLightActiveProbe() {
        MiscType misc = new MiscType();
        
        misc.name = "Light Active Probe";
        misc.setInternalName("CLLightActiveProbe");
        misc.addLookupName("CL Light Active Probe");
        misc.addLookupName("Light Active Probe");
        misc.addLookupName("Clan Light Active Probe");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = true;
        misc.cost = 150000;
        misc.spreadable = false;
        misc.flags |= F_BAP;
        misc.bv = 7;
        
        return misc;
    }

    public static MiscType createISAPPod() {
        MiscType misc = new MiscType();
        
        misc.name = "IS AP Pod";
        misc.setInternalName("ISAntiPersonnelPod");
        misc.addLookupName("IS A-Pod");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = true;
        misc.cost = 1500;
        misc.spreadable = false;
        misc.flags |= F_AP_POD;
        misc.bv = 1;
        
        return misc;
    }

    public static MiscType createCLAPPod() {
        MiscType misc = new MiscType();
        
        misc.name = "CL AP Pod";
        misc.setInternalName("CLAntiPersonnelPod");
        misc.addLookupName("Clan A-Pod");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = true;
        misc.cost = 1500;
        misc.spreadable = false;
        misc.flags |= F_AP_POD;
        misc.bv = 1;
        
        return misc;
    }

    public static MiscType createBASearchlight() {
        MiscType misc = new MiscType();
        
        misc.name = "Searchlight";
        misc.setInternalName("BASearchlight");
        misc.tonnage = 0.0f;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_SEARCHLIGHT;
        misc.bv = 0;
        
        return misc;
    }

    public static String getTargetSysName(int targSysType) {
        if ((targSysType < 0) || (targSysType >= targSysNames.length))
            return null;
        return targSysNames[targSysType];
    }

    public static int getTargetSysType(String targSysName) {
        for (int x=0; x<targSysNames.length; x++) {
            if (targSysNames[x].compareTo(targSysName) == 0)
                return x;
        }
        return -1;
    }
}
