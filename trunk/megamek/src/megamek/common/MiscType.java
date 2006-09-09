/**
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

/**
 * MiscType.java
 *
 * Created on April 2, 2002, 12:15 PM
 */

package megamek.common;

/**
 *
 * @author  Ben
 * @version
 */
public class MiscType extends EquipmentType {
    // equipment flags (okay, like every type of equipment has its own flag)
    public static final long     F_HEAT_SINK         = 0x000000001L;
    public static final long     F_DOUBLE_HEAT_SINK  = 0x000000002L;
    public static final long     F_JUMP_JET          = 0x000000004L;
    public static final long     F_CASE              = 0x000000008L;
    public static final long     F_MASC              = 0x000000010L;
    public static final long     F_TSM               = 0x000000020L;
    public static final long     F_LASER_HEAT_SINK   = 0x000000040L;
    public static final long     F_C3S               = 0x000000080L;
    public static final long     F_C3I               = 0x000000100L;
    public static final long     F_ARTEMIS           = 0x000000200L;
    public static final long     F_ECM               = 0x000000400L;
    public static final long     F_TARGCOMP          = 0x000000800L;
    public static final long     F_ANGEL_ECM         = 0x000001000L;
    public static final long     F_BAP               = 0x000002000L;
    public static final long     F_BOARDING_CLAW     = 0x000004000L;
    public static final long     F_VACUUM_PROTECTION = 0x000008000L;
    public static final long     F_ASSAULT_CLAW      = 0x000010000L;
    public static final long     F_FIRE_RESISTANT    = 0x000020000L;
    public static final long     F_STEALTH           = 0x000040000L;
    public static final long     F_MINE              = 0x000080000L;
    public static final long     F_TOOLS             = 0x000100000L;
    public static final long     F_MAGNETIC_CLAMP    = 0x000200000L;
    public static final long     F_PARAFOIL          = 0x000400000L;
    public static final long     F_FERRO_FIBROUS     = 0x000800000L;
    public static final long     F_ENDO_STEEL        = 0x001000000L;
    public static final long     F_AP_POD            = 0x002000000L;
    public static final long     F_SEARCHLIGHT       = 0x004000000L;
    public static final long     F_CLUB              = 0x008000000L;
    public static final long     F_HAND_WEAPON       = 0x010000000L;
    public static final long     F_COWL              = 0x020000000L;
    public static final long     F_JUMP_BOOSTER      = 0x040000000L;
    public static final long     F_HARJEL            = 0x080000000L;
    public static final long     F_UMU               = 0x100000000L;
    public static final long     F_COOLANT_SYSTEM    = 0x200000000L;
    public static final long     F_SPIKES            = 0x400000000L;

    // Secondary Flags for Physical Weapons
    public static final int     S_CLUB              = 0x00000001; // BMR
    public static final int     S_TREE_CLUB         = 0x00000002; // BMR
    public static final int     S_HATCHET           = 0x00000004; // BMR
    public static final int     S_SWORD             = 0x00000008; // BMR
    public static final int     S_MACE_THB          = 0x00000010; // Tac Handbook version
    public static final int     S_CLAW_THB          = 0x00000020; // Not used yet, but...  Hey, it's all for fun.
    public static final int     S_MACE              = 0x00000040; // Solaris 7
    public static final int     S_DUAL_SAW          = 0x00000080; // Solaris 7                
    public static final int     S_FLAIL             = 0x00000100; // Solaris 7
    public static final int     S_PILE_DRIVER       = 0x00000200; // Solaris 7
    public static final int     S_SHIELD_SMALL      = 0x00000400; // Solaris 7
    public static final int     S_SHIELD_MEDIUM     = 0x00000800; // Solaris 7
    public static final int     S_SHIELD_LARGE      = 0x00001000; // Solaris 7
    public static final int     S_LANCE             = 0x00002000; // Solaris 7 
    public static final int     S_VIBRO_SMALL       = 0x00004000; // Solaris 7
    public static final int     S_VIBRO_MEDIUM      = 0x00008000; // Solaris 7
    public static final int     S_VIBRO_LARGE       = 0x00010000; // Solaris 7
    public static final int     S_WRECKING_BALL     = 0x00020000; // Solaris 7
    public static final int     S_BACKHOE           = 0x00040000; // Miniatures Rulebook
    public static final int     S_COMBINE           = 0x00080000; // Miniatures Rulebook; TODO
    public static final int     S_CHAINSAW          = 0x00100000; // Miniatures Rulebook
    public static final int     S_ROCK_CUTTER       = 0x00200000; // Miniatures Rulebook; TODO
    public static final int     S_BUZZSAW           = 0x00400000; // Unbound;

    public static final String  S_ACTIVE_SHIELD     = "Active";
    public static final String  S_PASSIVE_SHIELD    = "Passive";
    public static final String  S_NO_SHIELD         = "None";

    // Secondary damage for hand weapons.
    // These are differentiated from Physical Weapons using the F_CLUB flag
    // because the following weapons are treated as a punch attack, while
    // the above weapons are treated as club or hatchet attacks.
    // these are subtypes of F_HAND_WEAPON
    public static final int     S_CLAW              = 0x00000001; // Solaris 7
    public static final int     S_MINING_DRILL      = 0x00000002; // Miniatures Rulebook; TODO

    // Secondary flags for infantry tools
    public static final int     S_VIBROSHOVEL       = 0x00000001; // can fortify hexes
    public static final int     S_DEMOLITION_CHARGE = 0x00000002; // can demolish buildings
    public static final int     S_BRIDGE_KIT        = 0x00000004; // can build a bridge
    public static final int     S_MINESWEEPER       = 0x00000008; // can clear mines
    public static final int     S_HEAVY_ARMOR       = 0x00000010; 

    // Secondary flags for MASC
    public static final int     S_SUPERCHARGER      = 0x00000001;

    public static final int     T_TARGSYS_UNKNOWN           = -1;
    public static final int     T_TARGSYS_STANDARD          = 0;
    public static final int     T_TARGSYS_TARGCOMP          = 1;
    public static final int     T_TARGSYS_LONGRANGE         = 2;
    public static final int     T_TARGSYS_SHORTRANGE        = 3;
    public static final int     T_TARGSYS_VARIABLE_RANGE    = 4;
    public static final int     T_TARGSYS_ANTI_AIR          = 5;
    public static final int     T_TARGSYS_MULTI_TRAC        = 6;
    public static final int     T_TARGSYS_MULTI_TRAC_II     = 7;
    public static final int     T_TARGSYS_HEAT_SEEKING_THB  = 8;
    public static final String[] targSysNames = {"Standard Targeting System",
                                                    "Targeting Computer",
                                                    "Long-Range Targeting System",
                                                    "Short-Range Targeting System",
                                                    "Variable-Range Taretting System",
                                                    "Anti-Air Targeting System",
                                                    "Multi-Trac Targeting System",
                                                    "Multi-Trac II Targeting System"};

    //New stuff for shields
    protected int baseDamageAbsorptionRate = 0;
    protected int baseDamageCapacity = 0;
    protected int damageTaken = 0;

    /** Creates new MiscType */
    public MiscType() {
    }

    public boolean isShield(){
        if ( this.hasFlag(MiscType.F_CLUB)
                && (this.hasSubType(MiscType.S_SHIELD_LARGE)
                || this.hasSubType((MiscType.S_SHIELD_MEDIUM))
                || this.hasSubType(MiscType.S_SHIELD_SMALL)) )
            return true;
        //else
        return false;
    }

    public boolean isVibroblade(){
        if ( this.hasFlag(MiscType.F_CLUB)
                && (this.hasSubType(MiscType.S_VIBRO_LARGE)
                || this.hasSubType((MiscType.S_VIBRO_MEDIUM))
                || this.hasSubType(MiscType.S_VIBRO_SMALL)) )
            return true;
        //else
        return false;
    }

    public float getTonnage(Entity entity) {
        if (tonnage != TONNAGE_VARIABLE) {
            return tonnage;
        }
        // check for known formulas
        if (hasFlag(F_JUMP_JET)) {
            if ((getTechLevel() == TechConstants.T_IS_LEVEL_3)
                    || (getTechLevel() == TechConstants.T_CLAN_LEVEL_3)) {
                if (entity.getWeight() <= 55.0) {
                    return 1.0f;
                } else if (entity.getWeight() <= 85.0) {
                    return 2.0f;
                } else {
                    return 4.0f;
                }
            }
			if (entity.getWeight() <= 55.0) {
			    return 0.5f;
			} else if (entity.getWeight() <= 85.0) {
			    return 1.0f;
			} else {
			    return 2.0f;
			}
        } else if (hasFlag(F_UMU)) {
            if (entity.getWeight() <= 55.0) {
                return 0.5f;
            } else if (entity.getWeight() <= 85.0) {
                return 1.0f;
            } else {
                return 2.0f;
            }
        } else if (hasFlag(F_CLUB)
                && (hasSubType(S_HATCHET)
                || hasSubType(S_MACE_THB))) {
            return (float)Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_CLUB)
                && hasSubType(S_LANCE)) {
                    return (float)Math.ceil(entity.getWeight() / 20.0);
        } else if (hasFlag(F_CLUB)
                && hasSubType(S_SWORD)) {
            return (float)(Math.ceil(entity.getWeight() / 20.0 * 2.0) / 2.0);
        } else if (hasFlag(F_CLUB)
                && hasSubType(S_MACE)) {
            return (float)(Math.ceil(entity.getWeight() / 10.0));
        } else if (hasFlag(F_MASC)) {
            if (hasSubType(S_SUPERCHARGER)) {
                Engine e = entity.getEngine();
                if(e == null) return 0.0f;
                return (float)(Math.ceil(e.getWeightEngine() / 10.0 * 2.0) / 2.0);
            }
			if (entity.isClan()) {
			    return Math.round(entity.getWeight() / 25.0f);
			}
			return Math.round(entity.getWeight() / 20.0f);
        } else if (hasFlag(F_TARGCOMP)) {
            // based on tonnage of direct_fire weaponry
            double fTons = 0.0;
            for (Mounted m : entity.getWeaponList()) {
                WeaponType wt = (WeaponType)m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += wt.getTonnage(entity);
                }
            }
            if (entity.isClan()) {
                return (float)Math.ceil(fTons / 5.0f);
            }
			return (float)Math.ceil(fTons / 4.0f);
        } else if ( EquipmentType.getArmorTypeName(T_ARMOR_FERRO_FIBROUS).equals(internalName) ) {
            double tons = 0.0;
            if ( entity.isClanArmor()) {
                tons = entity.getTotalOArmor() / ( 16 * 1.2 );
            } else {
                tons = entity.getTotalOArmor() / ( 16 * 1.12 );
            }
            tons = Math.ceil( tons * 2.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getArmorTypeName(T_ARMOR_LIGHT_FERRO).equals(internalName) ) {
            double tons = entity.getTotalOArmor() / (16*1.06);
            tons = Math.ceil( tons * 2.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getArmorTypeName(T_ARMOR_HEAVY_FERRO).equals(internalName) ) {
            double tons = entity.getTotalOArmor() / (16*1.24);
            tons = Math.ceil( tons * 2.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL).equals(internalName) ) {
            double tons = 0.0;
            tons = Math.ceil( entity.getWeight() / 10.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE).equals(internalName) ) {
            double tons = 0.0;
            tons = Math.ceil( entity.getWeight() / 10.0 ) / 2.0;
            return (float) tons;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_REINFORCED).equals(internalName) ) {
            double tons = 0.0;
            tons = Math.ceil( entity.getWeight() / 10.0 ) * 2.0;
            return (float) tons;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_COMPOSITE).equals(internalName) ) {
            double tons = 0.0;
            tons = Math.ceil( entity.getWeight() / 10.0 ) / 2.0;
            return (float) tons;
        } else if (hasFlag(F_VACUUM_PROTECTION)) {
            return (float)Math.ceil(entity.getWeight() / 10.0);
        } else if (hasFlag(F_JUMP_BOOSTER)) {
            return (float)(Math.ceil(entity.getWeight() * entity.getOriginalJumpMP() / 10.0) / 2.0);
        } else if (hasFlag(F_HAND_WEAPON)
                && hasSubType(S_CLAW)) {
            return (int)Math.ceil(entity.getWeight() / 15);
        }
        // okay, I'm out of ideas
        return 1.0f;
    }
    
    public int getCriticals(Entity entity) {
        if (criticals != CRITICALS_VARIABLE) {
            return criticals;
        }
        // check for known formulas
        if (hasFlag(F_CLUB)
                && (hasSubType(S_HATCHET)
                || hasSubType(S_SWORD)
                || hasSubType(S_MACE_THB))) {
            return (int)Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_CLUB)
                && hasSubType(S_LANCE)) {
            return (int)Math.ceil(entity.getWeight() / 20.0);
        }else if (hasFlag(F_CLUB)
                && hasSubType(S_MACE)) {
            return (int)Math.ceil(entity.getWeight() / 10.0);
        } else if (hasFlag(F_MASC)) {
            if (entity.isClan()) {
                return Math.round(entity.getWeight() / 25.0f);
            }
			return Math.round(entity.getWeight() / 20.0f);
        } else if (hasFlag(F_TARGCOMP)) {
            // based on tonnage of direct_fire weaponry
            double fTons = 0.0;
            for (Mounted m : entity.getWeaponList()) {
                WeaponType wt = (WeaponType)m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += wt.getTonnage(entity);
                }
            }
            if (entity.isClan()) {
                return (int)Math.ceil(fTons / 5.0f);
            }
			return (int)Math.ceil(fTons / 4.0f);
        } else if ( EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS).equals(internalName) ) {
            if ( entity.isClanArmor() ) {
                return 7;
            }
			return 14;
        } else if ( EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL).equals(internalName) ) {
            if ( entity.isClan() ) {
                return 7;
            }
			return 14;
        } else if (hasFlag(F_JUMP_BOOSTER)) {
            return (entity instanceof QuadMech) ? 8 : 4; // all slots in all legs
        } else if (hasFlag(F_HAND_WEAPON)
                && hasSubType(S_CLAW)) {
            return (int)Math.ceil(entity.getWeight() / 15);
        }
        // right, well I'll just guess then
        return 1;
    }
    
    public double getBV(Entity entity) {
        if (bv != BV_VARIABLE) {
            return bv;
        }
        // check for known formulas
        if (hasFlag(F_CLUB)
                && hasSubType(S_HATCHET)) {
            return Math.ceil(entity.getWeight() / 5.0) * 1.5;
        } else if (hasFlag(F_CLUB)
                && hasSubType(S_MACE_THB)) {
            return Math.ceil(entity.getWeight() / 5.0) * 1.5;
        } else if (hasFlag(F_CLUB)
                && hasSubType(S_LANCE)) {
            return Math.ceil(entity.getWeight() / 5.0) * 1.0;
        } else if (hasFlag(F_CLUB)
                && hasSubType(S_MACE)) {
            return Math.ceil(entity.getWeight() / 4.0);
        } else if (hasFlag(F_CLUB)
                && hasSubType(S_SWORD)) {
            return (Math.ceil(entity.getWeight() / 10.0) + 1.0) * 1.725;
        } else if (hasFlag(F_TARGCOMP)) {
            // 20% of direct_fire weaponry BV (half for rear-facing)
            double fFrontBV = 0.0, fRearBV = 0.0;
            for (Mounted m : entity.getWeaponList()) {
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
            }
			return fRearBV * 0.2 + fFrontBV * 0.1;
        } else if (hasFlag(F_HAND_WEAPON)
                && hasSubType(S_CLAW)) {
            return (Math.ceil(entity.getWeight() / 7.0)) * 1.275;
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
        EquipmentType.addType(createVacuumProtection());
        EquipmentType.addType(createStandard());
        
        // Start of Level2 stuff
        EquipmentType.addType(createISDoubleHeatSink());
        EquipmentType.addType(createCLDoubleHeatSink());
        EquipmentType.addType(createISCASE());
        EquipmentType.addType(createCLCASE());
        EquipmentType.addType(createISMASC());
        EquipmentType.addType(createCLMASC());
        EquipmentType.addType(createTSM());
        EquipmentType.addType(createC3S());
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
        EquipmentType.addType(createBloodhoundActiveProbe());
        EquipmentType.addType(createTHBBloodhoundActiveProbe());
        EquipmentType.addType(createCLActiveProbe());
        EquipmentType.addType(createCLLightActiveProbe());
        EquipmentType.addType(createISAPPod());
        EquipmentType.addType(createCLAPPod());
        EquipmentType.addType(createSword());

        // Start of level 3 stuff
        EquipmentType.addType(createImprovedJumpJet());
        EquipmentType.addType(createCLImprovedJumpJet());
        EquipmentType.addType(createJumpBooster());
        EquipmentType.addType(createFerroFibrousPrototype());
        EquipmentType.addType(createLightFerroFibrous());
        EquipmentType.addType(createHeavyFerroFibrous());
        EquipmentType.addType(createHardened());
        EquipmentType.addType(createEndoSteelPrototype());
        EquipmentType.addType(createReinforcedStructure());
        EquipmentType.addType(createCompositeStructure());
        EquipmentType.addType(createIS1CompactHeatSink());
        EquipmentType.addType(createIS2CompactHeatSinks());
        EquipmentType.addType(createCLLaserHeatSink());
        EquipmentType.addType(createISAngelECM());
        EquipmentType.addType(createISTHBAngelECM());
        EquipmentType.addType(createCLAngelECM());
        EquipmentType.addType(createWatchdogECM());
        EquipmentType.addType(createTHBMace());
        EquipmentType.addType(createMace());
        EquipmentType.addType(createDualSaw());
        EquipmentType.addType(createChainsaw());
        EquipmentType.addType(createBackhoe());
        EquipmentType.addType(createPileDriver());
        EquipmentType.addType(createArmoredCowl());
        EquipmentType.addType(createNullSignatureSystem());
        EquipmentType.addType(createLightMinesweeper());
        EquipmentType.addType(createBridgeKit());
        EquipmentType.addType(createVibroShovel());
        EquipmentType.addType(createDemolitionCharge());
        EquipmentType.addType(createSuperCharger());
        EquipmentType.addType(createISMediumShield());
        EquipmentType.addType(createISSmallShield());
        EquipmentType.addType(createISLargeShield());
        EquipmentType.addType(createISClaw());
        EquipmentType.addType(createCLHarJel());
        EquipmentType.addType(createISHarJel());
        EquipmentType.addType(createCLMediumShield());
        EquipmentType.addType(createCLSmallShield());
        EquipmentType.addType(createCLLargeShield());
        EquipmentType.addType(createCLClaw());
        EquipmentType.addType(createISUMU());
        EquipmentType.addType(createCLUMU());
        EquipmentType.addType(createISLance());
        EquipmentType.addType(createCLLance());
        EquipmentType.addType(createISWreckingBall());
        EquipmentType.addType(createCLWreckingBall());
        EquipmentType.addType(createISFlail());
        EquipmentType.addType(createCLFlail());
        EquipmentType.addType(createISMediumVibroblade());
        EquipmentType.addType(createISSmallVibroblade());
        EquipmentType.addType(createISLargeVibroblade());
        EquipmentType.addType(createCLMediumVibroblade());
        EquipmentType.addType(createCLSmallVibroblade());
        EquipmentType.addType(createCLLargeVibroblade());
        EquipmentType.addType(createISBuzzsaw());
        EquipmentType.addType(createCLBuzzsaw());
        EquipmentType.addType(createCoolantSystem());
        EquipmentType.addType(createHeavyArmor());
        EquipmentType.addType(createSpikes());
        
        // Start BattleArmor equipment
        EquipmentType.addType(createBABoardingClaw());
        EquipmentType.addType(createBAAssaultClaws());
        EquipmentType.addType(createBAFireResistantArmor());
        EquipmentType.addType(createBasicStealth());
        EquipmentType.addType(createStandardStealth());
        EquipmentType.addType(createImprovedStealth());
        EquipmentType.addType(createMine());
        EquipmentType.addType(createMinesweeper());
        EquipmentType.addType(createBAMagneticClamp());
        EquipmentType.addType(createSingleHexECM());
        EquipmentType.addType(createMimeticCamo());
        EquipmentType.addType(createSimpleCamo());
        EquipmentType.addType(createParafoil());
        EquipmentType.addType(createBASearchlight());
        
    }
    
    public static MiscType createHeatSink() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
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

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Jump Jet";
        misc.setInternalName(misc.name);
        misc.addLookupName("JumpJet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags |= F_JUMP_JET;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createImprovedJumpJet() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Improved Jump Jet";
        misc.setInternalName(misc.name);
        misc.addLookupName("ImprovedJump Jet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 2;
        misc.flags |= F_JUMP_JET;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createCLImprovedJumpJet() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Clan Improved Jump Jet";
        misc.setInternalName(misc.name);
        misc.addLookupName("CLImprovedJump Jet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 2;
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
        misc.flags |= F_CLUB;
        misc.subType |= S_TREE_CLUB | S_CLUB;
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
        misc.subType |= S_CLUB;
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
        misc.subType |= S_CLUB;
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createHatchet() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_1;
        misc.name = "Hatchet";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_CLUB;
        misc.subType |= S_HATCHET;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    // Start of Level2 stuff
    
    public static MiscType createISDoubleHeatSink() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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

        misc.techLevel = TechConstants.T_CLAN_LEVEL_2;
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

        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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

        misc.techLevel = TechConstants.T_CLAN_LEVEL_2;
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

        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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

        misc.techLevel = TechConstants.T_CLAN_LEVEL_2;
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
    
    public static MiscType createSuperCharger() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Supercharger";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS Super Charger");
        misc.addLookupName("SuperCharger");
        misc.addLookupName("Supercharger");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MASC;
        misc.subType |= S_SUPERCHARGER;
        misc.bv = 0;
        
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);
        
        return misc;
    }
    
    public static MiscType createTSM() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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
        
        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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

    public static MiscType createC3I() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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

        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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

        misc.techLevel = TechConstants.T_CLAN_LEVEL_2;
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

        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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
        misc.setModes(new String[] {"ECM", "ECCM"});
        misc.setInstantModeSwitch(false);
        
        return misc;
    }

    public static MiscType createCLECM() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_LEVEL_2;
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
        misc.setModes(new String[] {"ECM", "ECCM"});
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createISAngelECM() {
        MiscType misc = new MiscType();

        // Don't forget, this will eventually count double for ECCM.
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Angel ECM Suite";
        misc.setInternalName("ISAngelECMSuite");
        misc.addLookupName("IS Angel ECM Suite");
        misc.addLookupName("ISAngelECM");
        misc.tonnage = 2;
        misc.criticals = 2;
        misc.cost = 750000;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_ECM | F_ANGEL_ECM;
        misc.bv = 100;
        misc.setModes(new String[] {"ECM", "ECCM"});
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createISTHBAngelECM() {
        MiscType misc = new MiscType();

        // Don't forget, this will eventually count double for ECCM.
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "THB Angel ECM Suite";
        misc.setInternalName("ISTHBAngelECMSuite");
        misc.addLookupName("IS THB Angel ECM Suite");
        misc.addLookupName("ISTHBAngelECM");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 1000000;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_ECM | F_ANGEL_ECM;
        misc.bv = 100;
        misc.setModes(new String[] {"ECM", "ECCM"});
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCLAngelECM() {
        MiscType misc = new MiscType();
        
        // Don't forget, this will eventually count double for ECCM.
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Clan Angel ECM Suite";
        misc.setInternalName("CLAngelECMSuite");
        misc.addLookupName("Clan Angel ECM Suite");
        misc.addLookupName("CLAngelECM");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 750000;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_ECM | F_ANGEL_ECM;
        misc.bv = 100;
        misc.setModes(new String[] {"ECM", "ECCM"});
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createWatchdogECM() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Watchdog ECM Suite";
        misc.setInternalName("WatchdogECMSuite");
        misc.addLookupName("Watchdog ECM Suite");
        misc.addLookupName("WatchdogECM");
        misc.addLookupName("CLWatchdogECM");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 500000;
        misc.hittable = true;
        misc.spreadable = false;
        misc.flags |= F_ECM | F_BAP;
        misc.bv = 73;
        misc.setModes(new String[] {"ECM", "ECCM"});
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createSword() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_2;
        misc.name = "Sword";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_CLUB;
        misc.subType |= S_SWORD;
        misc.bv = BV_VARIABLE;
        
        return misc;
    }

    public static MiscType createTHBMace() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Mace (THB)";
        misc.setInternalName(misc.name);
        misc.addLookupName("THB Mace");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_CLUB;
        misc.subType |= S_MACE_THB;
        misc.bv = BV_VARIABLE;
        
        return misc;
    }

    public static MiscType createMace() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Mace";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = 130000;
        misc.flags |= F_CLUB;
        misc.subType |= S_MACE;
        misc.bv = BV_VARIABLE;
        
        return misc;
    }

    public static MiscType createBackhoe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Backhoe";
        misc.setInternalName(misc.name);
        misc.tonnage = 5;
        misc.criticals = 6;
        misc.cost = 50000;
        misc.flags |= F_CLUB;
        misc.subType |= S_BACKHOE;
        misc.bv = 8;

        return misc;
    }

    public static MiscType createDualSaw() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Dual Saw";
        misc.setInternalName(misc.name);
        misc.tonnage = 7;
        misc.criticals = 7;
        misc.cost = 100000;
        misc.flags |= F_CLUB;
        misc.subType |= S_DUAL_SAW;
        misc.bv = 9;
        
        return misc;
    }

    public static MiscType createPileDriver() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Pile Driver";
        misc.setInternalName(misc.name);
        misc.addLookupName("PileDriver");
        misc.tonnage = 10;
        misc.criticals = 8;
        misc.cost = 100000;
        misc.flags |= F_CLUB;
        misc.subType |= S_PILE_DRIVER;
        misc.bv = 5;
        
        return misc;
    }

    public static MiscType createChainsaw() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Chainsaw";
        misc.setInternalName(misc.name);
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.cost = 100000;
        misc.flags |= F_CLUB;
        misc.subType |= S_CHAINSAW;
        misc.bv = 7;
        
        return misc;
    }

    public static MiscType createArmoredCowl() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name="Armored Cowl";
        misc.setInternalName(misc.name);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 10000;
        misc.flags |= F_COWL;
        misc.bv = 10;
        
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
        
        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_2;
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
        misc.flags |= F_TOOLS;
        misc.subType |= S_MINESWEEPER;
        misc.bv = 0;
        
        return misc;
    }
    public static MiscType createLightMinesweeper() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Light Minesweeper";
        misc.setInternalName("Light Minesweeper");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_TOOLS;
        misc.subType |= S_MINESWEEPER;
        misc.toHitModifier = 1;
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
        misc.setModes(new String[] {"ECM", "ECCM"});
        misc.setInstantModeSwitch(false);

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

    public static MiscType createNullSignatureSystem() {
        MiscType misc = new MiscType();
        
        misc.name = "Null Signature System";
        misc.setInternalName(Mech.NULLSIG);
        misc.addLookupName("Null Signature System");
        misc.addLookupName("NullSignatureSystem");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.hittable = true;
        misc.spreadable = true;
        misc.flags |= F_STEALTH;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.bv = 0;            //???
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createFerroFibrous() {
        MiscType misc = new MiscType();
        
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS));
        misc.addLookupName("Ferro-Fibrous Armor");
        misc.addLookupName("Ferro Fibre");
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
        misc.criticals = 16;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_FERRO_FIBROUS;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        
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

    public static MiscType createHardened() {
        MiscType misc = new MiscType();
        
        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HARDENED);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HARDENED));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        
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
        misc.criticals = 16;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_ENDO_STEEL;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        
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
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        
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
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createCLLaserHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "Laser Heat Sink";
        misc.setInternalName(misc.name);
        misc.addLookupName("CLLaser Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags |= F_DOUBLE_HEAT_SINK | F_LASER_HEAT_SINK;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        
        return misc;
    }

    //It is possible to have 1 or 2 compact heat sinks
    //in a single critical slot - this is addressed by
    //creating 1 slot single and 1 slot double heat sinks
    //with the weight of 1 or 2 compact heat sinks
    public static MiscType createIS1CompactHeatSink() {
        MiscType misc = new MiscType();
        
        misc.name = "1 Compact Heat Sink";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS1 Compact Heat Sink");
        misc.tonnage = 1.5f;
        misc.criticals = 1;
        misc.flags |= F_HEAT_SINK;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createIS2CompactHeatSinks() {
        MiscType misc = new MiscType();
        
        misc.name = "2 Compact Heat Sinks";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS2 Compact Heat Sinks");
        misc.tonnage = 3.0f;
        misc.criticals = 1;
        misc.flags |= F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        
        return misc;
    }

    public static MiscType createBeagleActiveProbe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_2;
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

    public static MiscType createBloodhoundActiveProbe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Bloodhound Active Probe";
        misc.setInternalName("BloodhoundActiveProbe");
        misc.addLookupName("Bloodhound Active Probe");
        misc.addLookupName("ISBloodhoundActiveProbe");
        misc.addLookupName("IS Bloodhound Active Probe");
        misc.tonnage = 2;
        misc.criticals = 3;
        misc.hittable = true;
        misc.cost = 500000;
        misc.spreadable = false;
        misc.flags |= F_BAP;
        misc.bv = 25;

        return misc;
    }

    public static MiscType createTHBBloodhoundActiveProbe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Bloodhound Active Probe (THB)";
        misc.setInternalName("THBBloodhoundActiveProbe");
        misc.addLookupName("THB Bloodhound Active Probe");
        misc.addLookupName("ISTHBBloodhoundActiveProbe");
        misc.addLookupName("IS THB Bloodhound Active Probe");
        misc.tonnage = 5;
        misc.criticals = 2;
        misc.hittable = true;
        misc.cost = 750000;
        misc.spreadable = false;
        misc.flags |= F_BAP;
        misc.bv = 25;

        return misc;
    }

    public static MiscType createCLActiveProbe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_LEVEL_2;
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
    
    public static MiscType createVacuumProtection() {
        MiscType misc = new MiscType();
        
        misc.name = "Vacuum Protection";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags |= F_VACUUM_PROTECTION;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createJumpBooster() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Jump Booster";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.bv = 0;
        misc.flags |= F_JUMP_BOOSTER;
        // see note above
        misc.spreadable = true;

        return misc;
    }

    public static MiscType createDemolitionCharge() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Demolition Charge";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_TOOLS;
        misc.subType |= S_DEMOLITION_CHARGE;
        misc.toHitModifier = 1;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createVibroShovel() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Vibro-Shovel";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_TOOLS;
        misc.subType |= S_VIBROSHOVEL;
        misc.toHitModifier = 1;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createBridgeKit() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Bridge Kit";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.spreadable = false;
        misc.flags |= F_TOOLS;
        misc.subType |= S_BRIDGE_KIT;
        misc.toHitModifier = 1;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createISSmallShield() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Small Shield";
        misc.setInternalName("ISSmallShield");
        misc.addLookupName("Small Shield");
        misc.tonnage = 2;
        misc.criticals = 3;
        misc.cost = 50000;
        misc.flags |= F_CLUB;
        misc.subType |= S_SHIELD_SMALL;
        misc.bv = 50;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD};
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 3;
        misc.baseDamageCapacity = 11;
        
        return misc;
    }

    /**
     * Creates a claw MiscType Object
     * @return MiscType
     */
    public static MiscType createISClaw() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Claw";
        misc.setInternalName("ISClaw");
        misc.addLookupName("Claw");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_HAND_WEAPON;
        misc.subType |= S_CLAW;
        misc.bv = BV_VARIABLE;
        
        return misc;
    }


    public static MiscType createISMediumShield() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Medium Shield";
        misc.setInternalName("ISMediumShield");
        misc.addLookupName("Medium Shield");
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.cost = 100000;
        misc.flags |= F_CLUB;
        misc.subType |= S_SHIELD_MEDIUM;
        misc.bv = 135;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD};
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 5;
        misc.baseDamageCapacity = 18;
        
        return misc;
    }

    public static MiscType createISLargeShield() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Large Shield";
        misc.setInternalName("ISLargeShield");
        misc.addLookupName("Large Shield");
        misc.tonnage = 6;
        misc.criticals = 7;
        misc.cost = 300000;
        misc.flags |= F_CLUB;
        misc.subType |= S_SHIELD_LARGE;
        misc.bv = 263;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD};
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 7;
        misc.baseDamageCapacity = 25;

        return misc;
    }
    
    public static MiscType createCLSmallShield() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Small Shield";
        misc.setInternalName("CLSmallShield");
        misc.addLookupName("Clan Small Shield");
        misc.tonnage = 2;
        misc.criticals = 3;
        misc.cost = 50000;
        misc.flags |= F_CLUB;
        misc.subType |= S_SHIELD_SMALL;
        misc.bv = 50;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD};
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 3;
        misc.baseDamageCapacity = 11;
        
        return misc;
    }

    /**
     * Creates a claw MiscType Object
     * @return MiscType
     */
    public static MiscType createCLClaw() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Claw";
        misc.setInternalName("CLClaw");
        misc.addLookupName("Clan Claw");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_HAND_WEAPON;
        misc.subType |= S_CLAW;
        misc.bv = BV_VARIABLE;
        
        return misc;
    }


    public static MiscType createCLMediumShield() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Medium Shield";
        misc.setInternalName("CLMediumShield");
        misc.addLookupName("Clan Medium Shield");
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.cost = 100000;
        misc.flags |= F_CLUB;
        misc.subType |= S_SHIELD_MEDIUM;
        misc.bv = 135;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD};
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 5;
        misc.baseDamageCapacity = 18;
        
        return misc;
    }

    public static MiscType createCLLargeShield() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Large Shield";
        misc.setInternalName("CLLargeShield");
        misc.addLookupName("Clan Large Shield");
        misc.tonnage = 6;
        misc.criticals = 7;
        misc.cost = 300000;
        misc.flags |= F_CLUB;
        misc.subType |= S_SHIELD_LARGE;
        misc.bv = 263;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD};
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 7;
        misc.baseDamageCapacity = 25;

        return misc;
    }
    
    public static MiscType createCLHarJel() {
        //TODO: make the verifier only accept this in non-head locations
        MiscType misc = new MiscType();
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Clan HarJel";
        misc.setInternalName(misc.getName());
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 120000;
        misc.flags |= F_HARJEL;
        // can't enter BV here, because it's location dependendent,
        // and MiscType has no idea where a certain equipment may be
        // mounted
        misc.bv = 0;
        
        return misc;
    }
    
    public static MiscType createISHarJel() {
        //TODO: make the verifier only accept this in non-head locations
        MiscType misc = new MiscType();
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "IS HarJel";
        misc.setInternalName(misc.getName());
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 120000;
        misc.flags |= F_HARJEL;
        // can't enter BV here, because it's location dependendent,
        // and MiscType has no idea where a certain equipment may be
        // mounted
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createISUMU() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "UMU";
        misc.setInternalName("ISUMU");
        misc.addLookupName("IS Underwater Maneuvering Unit");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags |= F_UMU;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createCLUMU() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "UMU";
        misc.setInternalName("CLUMU");
        misc.addLookupName("Clan Underwater Maneuvering Unit");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags |= F_UMU;
        misc.bv = 0;
        
        return misc;
    }

    public static MiscType createISLance() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Lance";
        misc.setInternalName("IS Lance");
        misc.addLookupName("ISLance");
        misc.addLookupName("Lance");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_CLUB;
        misc.subType |= S_LANCE;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createCLLance() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Lance";
        misc.setInternalName("Clan Lance");
        misc.addLookupName("CLLance");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_CLUB;
        misc.subType |= S_LANCE;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createISFlail() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Flail";
        misc.setInternalName("IS Flail");
        misc.addLookupName("Flail");
        misc.tonnage = 5;
        misc.criticals = 4;
        misc.cost = 110000;
        misc.flags |= F_CLUB;
        misc.subType |= S_FLAIL;
        misc.bv = 11;

        return misc;
    }

    public static MiscType createCLFlail() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Flail";
        misc.setInternalName("Clan Flail");
        misc.addLookupName("CLFLail");
        misc.tonnage = 5;
        misc.criticals = 4;
        misc.cost = 110000;
        misc.flags |= F_CLUB;
        misc.subType |= S_FLAIL;
        misc.bv = 11;

        return misc;
    }

    public static MiscType createISWreckingBall() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Wrecking Ball";
        misc.setInternalName("IS Wrecking Ball");
        misc.addLookupName("WreckingBall");
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.cost = 110000;
        misc.flags |= F_CLUB;
        misc.subType |= S_WRECKING_BALL;
        misc.bv = 8;

        return misc;
    }

    public static MiscType createCLWreckingBall() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Wrecking Ball";
        misc.setInternalName("Clan Wrecking Ball");
        misc.addLookupName("CLWrecking Ball");
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.cost = 110000;
        misc.flags |= F_CLUB;
        misc.subType |= S_WRECKING_BALL;
        misc.bv = 8;

        return misc;
    }

    public static MiscType createISSmallVibroblade() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Small Vibroblade";
        misc.setInternalName("ISSmallVibroblade");
        misc.addLookupName("Small Vibroblade");
        misc.tonnage = 3;
        misc.criticals = 1;
        misc.cost = 150000;
        misc.flags |= F_CLUB;
        misc.subType |= S_VIBRO_SMALL;
        misc.bv = 12;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive","Active"};
        misc.setModes(modes);
        
        return misc;
    }

    public static MiscType createISMediumVibroblade() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Medium Vibroblade";
        misc.setInternalName("ISMediumVibroblade");
        misc.addLookupName("Medium Vibroblade");
        misc.tonnage = 5;
        misc.criticals = 2;
        misc.cost = 400000;
        misc.flags |= F_CLUB;
        misc.subType |= S_VIBRO_MEDIUM;
        misc.bv = 17;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive","Active"};
        misc.setModes(modes);
        
        return misc;
    }

    public static MiscType createISLargeVibroblade() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Large Vibroblade";
        misc.setInternalName("ISLargeVibroblade");
        misc.addLookupName("Large Vibroblade");
        misc.tonnage = 7;
        misc.criticals = 4;
        misc.cost = 750000;
        misc.flags |= F_CLUB;
        misc.subType |= S_VIBRO_LARGE;
        misc.bv = 24;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive","Active"};
        misc.setModes(modes);

        return misc;
    }
    
    public static MiscType createCLSmallVibroblade() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Small Vibroblade";
        misc.setInternalName("CLSmallVibroblade");
        misc.addLookupName("Clan Small Vibroblade");
        misc.tonnage = 3;
        misc.criticals = 1;
        misc.cost = 150000;
        misc.flags |= F_CLUB;
        misc.subType |= S_VIBRO_SMALL;
        misc.bv = 12;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive","Active"};
        misc.setModes(modes);
        
        return misc;
    }

    public static MiscType createCLMediumVibroblade() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Medium Vibroblade";
        misc.setInternalName("CLMediumVibroblade");
        misc.addLookupName("Clan Medium Vibroblade");
        misc.tonnage = 5;
        misc.criticals = 2;
        misc.cost = 400000;
        misc.flags |= F_CLUB;
        misc.subType |= S_VIBRO_MEDIUM;
        misc.bv = 17;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive","Active"};
        misc.setModes(modes);
        
        return misc;
    }

    public static MiscType createCLLargeVibroblade() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Large Vibroblade";
        misc.setInternalName("CLLargeVibroblade");
        misc.addLookupName("Clan Large Vibroblade");
        misc.tonnage = 7;
        misc.criticals = 4;
        misc.cost = 750000;
        misc.flags |= F_CLUB;
        misc.subType |= S_VIBRO_LARGE;
        misc.bv = 24;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive","Active"};
        misc.setModes(modes);

        return misc;
    }
    
    public static MiscType createISBuzzsaw() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Buzzsaw";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS Buzzsaw");
        misc.tonnage = 4;
        misc.criticals = 2;
        misc.cost = 100000;//From the Ask the Writer Forum
        misc.flags |= F_CLUB;
        misc.subType |= S_BUZZSAW;
        misc.bv =67;//From the Ask the Writer Forum
        
        return misc;
    }

    public static MiscType createCLBuzzsaw() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_CLAN_LEVEL_3;
        misc.name = "Buzzsaw";
        misc.setInternalName("CLBuzzsaw");
        misc.addLookupName("Clan Buzzsaw");
        misc.tonnage = 4;
        misc.criticals = 2;
        misc.cost = 100000;//From the Ask the Writer Forum
        misc.flags |= F_CLUB;
        misc.subType |= S_BUZZSAW;
        misc.bv = 6;//From the Ask the Writer Forum
        
        return misc;
    }
    
    public static MiscType createCoolantSystem() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Coolant System";
        misc.setInternalName(misc.name);
        misc.tonnage = 9;
        misc.criticals = 2;
        misc.cost = 90000;
        misc.flags |= F_COOLANT_SYSTEM;
        misc.bv = 15;
        
        return misc;
    }

    public static MiscType createSpikes() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Spikes";
        misc.setInternalName(misc.name);
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.cost = 90000;
        misc.flags |= F_SPIKES;
        misc.bv = 3;
        
        return misc;
    }

    public static MiscType createHeavyArmor() {
        MiscType misc = new MiscType();
        
        misc.techLevel = TechConstants.T_IS_LEVEL_3;
        misc.name = "Heavy Armor";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 100000;
        misc.flags |= F_TOOLS;
        misc.subType = S_HEAVY_ARMOR;
        misc.bv = 15;
        
        return misc;
    }

    public static MiscType createStandard() {
        //This is not really a single piece of equipment, it is used to
        // identify "standard" internal structure, armor, whatever.
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_STANDARD);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_STANDARD));
        misc.addLookupName("Regular");
        misc.addLookupName("Standard Armor");

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
