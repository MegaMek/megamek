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

import megamek.common.weapons.ISERPPC;
import megamek.common.weapons.ISHeavyPPC;
import megamek.common.weapons.ISLightPPC;
import megamek.common.weapons.ISPPC;
import megamek.common.weapons.ISSnubNosePPC;

/**
 * @author Ben
 * @version
 */
public class MiscType extends EquipmentType {
    // equipment flags (okay, like every type of equipment has its own flag)
    public static final long F_HEAT_SINK = 1L << 0;
    public static final long F_DOUBLE_HEAT_SINK = 1L << 1;
    public static final long F_JUMP_JET = 1L << 2;
    public static final long F_CASE = 1L << 3;
    public static final long F_MASC = 1L << 4;
    public static final long F_TSM = 1L << 5;
    public static final long F_LASER_HEAT_SINK = 1L << 6;
    public static final long F_C3S = 1L << 7;
    public static final long F_C3I = 1L << 8;
    public static final long F_ARTEMIS = 1L << 9;
    public static final long F_TARGCOMP = 1L << 10;
    public static final long F_ANGEL_ECM = 1L << 11;
    public static final long F_BOARDING_CLAW = 1L << 12;
    public static final long F_VACUUM_PROTECTION = 1L << 13;
    public static final long F_ASSAULT_CLAW = 1L << 14;
    public static final long F_FIRE_RESISTANT = 1L << 15;
    public static final long F_STEALTH = 1L << 16;
    public static final long F_MINE = 1L << 17;
    public static final long F_TOOLS = 1L << 18;
    public static final long F_MAGNETIC_CLAMP = 1L << 19;
    public static final long F_PARAFOIL = 1L << 20;
    public static final long F_FERRO_FIBROUS = 1L << 21;
    public static final long F_ENDO_STEEL = 1L << 22;
    public static final long F_AP_POD = 1L << 23;
    public static final long F_SEARCHLIGHT = 1L << 24;
    public static final long F_CLUB = 1L << 25;
    public static final long F_HAND_WEAPON = 1L << 26;
    public static final long F_COWL = 1L << 27;
    public static final long F_JUMP_BOOSTER = 1L << 28;
    public static final long F_HARJEL = 1L << 29;
    public static final long F_UMU = 1L << 30;
    public static final long F_COOLANT_SYSTEM = 1L << 31;
    public static final long F_SPIKES = 1L << 32;
    public static final long F_COMMUNICATIONS = 1L << 33;
    public static final long F_PPC_CAPACITOR = 1L << 34;
    public static final long F_REFLECTIVE = 1L << 35;
    public static final long F_REACTIVE = 1L << 36;
    public static final long F_CASEII = 1L << 37;
    public static final long F_LIFTHOIST = 1L << 38;
    public static final long F_ENVIRONMENTAL_SEALING = 1L << 39;
    public static final long F_ARMORED_CHASSIS = 1L << 40;
    public static final long F_TRACTOR_MODIFICATION = 1L << 41;
    public static final long F_ACTUATOR_ENHANCEMENT_SYSTEM = 1L << 42;
    public static final long F_ECM = 1L << 43;
    public static final long F_BAP = 1L << 44;
    public static final long F_MODULAR_ARMOR = 1L << 45;
    public static final long F_TALON = 1L << 46;
    public static final long F_VISUAL_CAMO = 1L << 47;
    public static final long F_APOLLO = 1L << 48;
    public static final long F_INDUSTRIAL_TSM = 1L << 49;
    public static final long F_NULLSIG = 1L << 50;
    public static final long F_VOIDSIG = 1L << 51;
    public static final long F_CHAMELEON_SHIELD = 1L << 52;
    public static final long F_VIBROCLAW = 1L << 53;
    public static final long F_SINGLE_HEX_ECM = 1L << 54;
    public static final long F_EJECTION_SEAT = 1L << 55;
    public static final long F_SALVAGE_ARM = 1L << 56;
    public static final long F_TRACKS = 1L << 57; // TODO: Implement me, so far
    // only construction data
    public static final long F_MASS = 1L << 58; // TODO: Implement me, so far
    // only construction data
    public static final long F_BA_EQUIPMENT = 1L << 59;
    public static final long F_MECH_EQUIPMENT = 1L << 60;
    public static final long F_TANK_EQUIPMENT = 1L << 61;

    // Secondary Flags for Physical Weapons
    public static final long S_CLUB = 1L << 0; // BMR
    public static final long S_TREE_CLUB = 1L << 1;// BMR
    public static final long S_HATCHET = 1L << 2; // BMR
    public static final long S_SWORD = 1L << 3; // BMR
    public static final long S_MACE_THB = 1L << 4;// Tac Handbook version
    public static final long S_CLAW_THB = 1L << 5; // Not used yet, but...
    // Hey, it's all for
    // fun.
    public static final long S_MACE = 1L << 6;
    public static final long S_DUAL_SAW = 1L << 7;
    public static final long S_FLAIL = 1L << 8;
    public static final long S_PILE_DRIVER = 1L << 9;
    public static final long S_SHIELD_SMALL = 1L << 10;
    public static final long S_SHIELD_MEDIUM = 1L << 11;
    public static final long S_SHIELD_LARGE = 1L << 12;
    public static final long S_LANCE = 1L << 13;
    public static final long S_VIBRO_SMALL = 1L << 14;
    public static final long S_VIBRO_MEDIUM = 1L << 15;
    public static final long S_VIBRO_LARGE = 1L << 16;
    public static final long S_WRECKING_BALL = 1L << 17;
    public static final long S_BACKHOE = 1L << 18;
    public static final long S_COMBINE = 1L << 19; // TODO
    public static final long S_CHAINSAW = 1L << 20;
    public static final long S_ROCK_CUTTER = 1L << 21;
    // TODO
    public static final long S_BUZZSAW = 1L << 22; // Unbound;
    public static final long S_RETRACTABLE_BLADE = 1L << 23;
    public static final long S_CHAIN_WHIP = 1L << 24;
    public static final long S_SPOT_WELDER = 1L << 25; // TODO: add game rules

    public static final String S_ACTIVE_SHIELD = "Active";
    public static final String S_PASSIVE_SHIELD = "Passive";
    public static final String S_NO_SHIELD = "None";

    // Secondary damage for hand weapons.
    // These are differentiated from Physical Weapons using the F_CLUB flag
    // because the following weapons are treated as a punch attack, while
    // the above weapons are treated as club or hatchet attacks.
    // these are subtypes of F_HAND_WEAPON
    public static final long S_CLAW = 1L << 0; // Solaris 7
    public static final long S_MINING_DRILL = 1L << 1; // Miniatures
    // Rulebook; TODO

    // Secondary flags for infantry tools
    public static final long S_VIBROSHOVEL = 1L << 0; // can fortify hexes
    public static final long S_DEMOLITION_CHARGE = 1L << 1; // can demolish
    // buildings
    public static final long S_BRIDGE_KIT = 1L << 2; // can build a bridge
    public static final long S_MINESWEEPER = 1L << 3; // can clear mines
    public static final long S_HEAVY_ARMOR = 1L << 4;

    // Secondary flags for MASC
    public static final long S_SUPERCHARGER = 1L << 0;

    // Secondary flags for Jump Jets
    public static final long S_STANDARD = 1L << 0;
    public static final long S_IMPROVED = 1L << 1;

    public static final int T_TARGSYS_UNKNOWN = -1;
    public static final int T_TARGSYS_STANDARD = 0;
    public static final int T_TARGSYS_TARGCOMP = 1;
    public static final int T_TARGSYS_LONGRANGE = 2;
    public static final int T_TARGSYS_SHORTRANGE = 3;
    public static final int T_TARGSYS_VARIABLE_RANGE = 4;
    public static final int T_TARGSYS_ANTI_AIR = 5;
    public static final int T_TARGSYS_MULTI_TRAC = 6;
    public static final int T_TARGSYS_MULTI_TRAC_II = 7;
    public static final int T_TARGSYS_HEAT_SEEKING_THB = 8;
    public static final String[] targSysNames = { "Standard Targeting System", "Targeting Computer", "Long-Range Targeting System", "Short-Range Targeting System", "Variable-Range Taretting System", "Anti-Air Targeting System", "Multi-Trac Targeting System", "Multi-Trac II Targeting System" };

    // New stuff for shields
    protected int baseDamageAbsorptionRate = 0;
    protected int baseDamageCapacity = 0;
    protected int damageTaken = 0;

    /** Creates new MiscType */
    public MiscType() {
    }

    public boolean isShield() {
        if (hasFlag(MiscType.F_CLUB) && (hasSubType(MiscType.S_SHIELD_LARGE) || hasSubType((MiscType.S_SHIELD_MEDIUM)) || hasSubType(MiscType.S_SHIELD_SMALL))) {
            return true;
        }
        // else
        return false;
    }

    public boolean isVibroblade() {
        if (hasFlag(MiscType.F_CLUB) && (hasSubType(MiscType.S_VIBRO_LARGE) || hasSubType((MiscType.S_VIBRO_MEDIUM)) || hasSubType(MiscType.S_VIBRO_SMALL))) {
            return true;
        }
        // else
        return false;
    }

    @Override
    public float getTonnage(Entity entity) {
        if (tonnage != TONNAGE_VARIABLE) {
            return tonnage;
        }
        // check for known formulas
        if (hasFlag(F_JUMP_JET)) {
            if (hasSubType(S_IMPROVED)) {
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
        } else if (hasFlag(F_CLUB) && (hasSubType(S_HATCHET) || hasSubType(S_MACE_THB))) {
            return (float) Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_LANCE)) {
            return (float) Math.ceil(entity.getWeight() / 20.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_SWORD)) {
            return (float) (Math.ceil(entity.getWeight() / 20.0 * 2.0) / 2.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE)) {
            return (float) (Math.ceil(entity.getWeight() / 10.0));
        } else if (hasFlag(F_CLUB) && hasSubType(S_RETRACTABLE_BLADE)) {
            return 0.5f + (float) Math.ceil(entity.getWeight() / 20.0);
        } else if (hasFlag(F_MASC)) {
            if (hasSubType(S_SUPERCHARGER)) {
                Engine e = entity.getEngine();
                if (e == null) {
                    return 0.0f;
                }
                return (float) (Math.ceil(e.getWeightEngine() / 10.0 * 2.0) / 2.0);
            }
            if (entity.isClan()) {
                return Math.round(entity.getWeight() / 25.0f);
            }
            return Math.round(entity.getWeight() / 20.0f);
        } else if (hasFlag(F_TARGCOMP)) {
            // based on tonnage of direct_fire weaponry
            double fTons = 0.0;
            for (Mounted m : entity.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += wt.getTonnage(entity);
                }
            }
            if (entity.isClan()) {
                return (float) Math.ceil(fTons / 5.0f);
            }
            return (float) Math.ceil(fTons / 4.0f);
        } else if (EquipmentType.getArmorTypeName(T_ARMOR_FERRO_FIBROUS).equals(internalName)) {
            double tons = 0.0;
            if (entity.isClanArmor()) {
                tons = entity.getTotalOArmor() / (16 * 1.2);
            } else {
                tons = entity.getTotalOArmor() / (16 * 1.12);
            }
            tons = Math.ceil(tons * 2.0) / 2.0;
            return (float) tons;
        } else if (EquipmentType.getArmorTypeName(T_ARMOR_LIGHT_FERRO).equals(internalName)) {
            double tons = entity.getTotalOArmor() / (16 * 1.06);
            tons = Math.ceil(tons * 2.0) / 2.0;
            return (float) tons;
        } else if (EquipmentType.getArmorTypeName(T_ARMOR_HEAVY_FERRO).equals(internalName)) {
            double tons = entity.getTotalOArmor() / (16 * 1.24);
            tons = Math.ceil(tons * 2.0) / 2.0;
            return (float) tons;
        } else if (EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL).equals(internalName)) {
            double tons = 0.0;
            tons = Math.ceil(entity.getWeight() / 10.0) / 2.0;
            return (float) tons;
        } else if (EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE).equals(internalName)) {
            double tons = 0.0;
            tons = Math.ceil(entity.getWeight() / 10.0) / 2.0;
            return (float) tons;
        } else if (EquipmentType.getStructureTypeName(T_STRUCTURE_REINFORCED).equals(internalName)) {
            double tons = 0.0;
            tons = Math.ceil(entity.getWeight() / 10.0) * 2.0;
            return (float) tons;
        } else if (EquipmentType.getStructureTypeName(T_STRUCTURE_COMPOSITE).equals(internalName)) {
            double tons = 0.0;
            tons = Math.ceil(entity.getWeight() / 10.0) / 2.0;
            return (float) tons;
        } else if (EquipmentType.getStructureTypeName(T_STRUCTURE_INDUSTRIAL).equals(internalName)) {
            double tons = 0.0;
            tons = Math.ceil(entity.getWeight() / 10.0) * 2.0;
            return (float) tons;
        } else if (hasFlag(F_VACUUM_PROTECTION)) {
            return (float) Math.ceil(entity.getWeight() / 10.0);
        } else if (hasFlag(F_ENVIRONMENTAL_SEALING)) {
            return entity.getWeight() / 10.0f;
        } else if (hasFlag(F_JUMP_BOOSTER)) {
            return (float) (Math.ceil(entity.getWeight() * entity.getOriginalJumpMP() / 10.0) / 2.0);
        } else if ((hasFlag(F_HAND_WEAPON) && hasSubType(S_CLAW)) || hasFlag(F_TALON)) {
            return (int) Math.ceil(entity.getWeight() / 15);
        } else if (hasFlag(F_ACTUATOR_ENHANCEMENT_SYSTEM)) {

            float tonnage = 0;
            if (entity instanceof BipedMech) {
                tonnage = entity.getWeight() / 35;
            } else {
                tonnage = entity.getWeight() / 50;
            }

            if (tonnage == Math.round(tonnage)) {
                return tonnage;
            }

            if (Math.floor(tonnage) < Math.round(tonnage)) {
                return Math.round(tonnage);
            }

            return (float) (Math.floor(tonnage) + 0.5);
        } else if (hasFlag(F_TRACKS)) {
            return entity.getWeight() / 10;
        }
        // okay, I'm out of ideas
        return 1.0f;
    }

    @Override
    public double getCost(Entity entity, boolean isArmored) {
        if (isArmored) {
            double armoredCost = cost;

            armoredCost += 150000 * getCriticals(entity);

            return armoredCost;
        }

        return super.getCost(entity, isArmored);
    }

    @Override
    public int getCriticals(Entity entity) {
        if (criticals != CRITICALS_VARIABLE) {
            return criticals;
        }
        // check for known formulas
        if (hasFlag(F_CLUB) && (hasSubType(S_HATCHET) || hasSubType(S_SWORD) || hasSubType(S_MACE_THB) || hasSubType(S_CHAIN_WHIP))) {
            return (int) Math.ceil(entity.getWeight() / 15.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_LANCE)) {
            return (int) Math.ceil(entity.getWeight() / 20.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE)) {
            return (int) Math.ceil(entity.getWeight() / 10.0);
        } else if (hasFlag(F_CLUB) && hasSubType(S_RETRACTABLE_BLADE)) {
            return 1 + (int) Math.ceil(entity.getWeight() / 20.0);
        } else if (hasFlag(F_MASC)) {
            if (entity.isClan()) {
                return (int) Math.round(entity.getWeight() / 25.0);
            }
            return (int) Math.round(entity.getWeight() / 20.0);
        } else if (hasFlag(F_TARGCOMP)) {
            // based on tonnage of direct_fire weaponry
            double fTons = 0.0;
            for (Mounted m : entity.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    fTons += wt.getTonnage(entity);
                }
            }
            if (entity.isClan()) {
                return (int) Math.ceil(fTons / 5.0f);
            }
            return (int) Math.ceil(fTons / 4.0f);
        } else if (EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_FERRO_FIBROUS).equals(internalName) || EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE).equals(internalName)) {
            if (entity.isClanArmor()) {
                return 7;
            }
            return 14;
        } else if (EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE).equals(internalName)) {
            if (entity.isClanArmor()) {
                return 5;
            }
            return 10;
        } else if (EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL).equals(internalName)) {
            if (entity.isClan()) {
                return 7;
            }
            return 14;
        } else if (hasFlag(F_JUMP_BOOSTER)) {
            return (entity instanceof QuadMech) ? 8 : 4; // all slots in all
            // legs
        } else if (hasFlag(F_HAND_WEAPON) && hasSubType(S_CLAW)) {
            return (int) Math.ceil(entity.getWeight() / 15);
        } else if (hasFlag(F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
            return entity.getWeightClass() + 1;
        } else if (hasFlag(F_TRACKS)) {
            if (entity instanceof QuadMech) {
                return 4;
            }
            if (entity instanceof BipedMech) {
                return 2;
            }
        }
        // right, well I'll just guess then
        return 1;
    }

    public double getBV(Entity entity, Mounted mount) {

        if (hasFlag(F_PPC_CAPACITOR) && (mount != null) && (mount.getLinked() != null)) {

            if (mount.getLinked().getType() instanceof ISLightPPC) {
                return 44;
            }

            if (mount.getLinked().getType() instanceof ISPPC) {
                return 88;
            }

            if (mount.getLinked().getType() instanceof ISHeavyPPC) {
                return 53;
            }

            if (mount.getLinked().getType() instanceof ISSnubNosePPC) {
                return 90;
            }

            if (mount.getLinked().getType() instanceof ISERPPC) {
                return 114;
            }
        }

        return this.getBV(entity);
    }

    @Override
    public double getBV(Entity entity) {
        double returnBV = 0.0;
        if (bv != BV_VARIABLE) {
            returnBV = bv;
            return returnBV;
        }
        // check for known formulas
        if (hasFlag(F_CLUB) && hasSubType(S_HATCHET)) {
            returnBV = Math.ceil(entity.getWeight() / 5.0) * 1.5;
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE_THB)) {
            returnBV = Math.ceil(entity.getWeight() / 5.0) * 1.5;
        } else if (hasFlag(F_CLUB) && hasSubType(S_LANCE)) {
            returnBV = Math.ceil(entity.getWeight() / 5.0) * 1.0;
        } else if (hasFlag(F_CLUB) && hasSubType(S_MACE)) {
            returnBV = Math.ceil(entity.getWeight() / 4.0);
        } else if (hasFlag(F_CLUB) && (hasSubType(S_SWORD) || hasSubType(S_CHAIN_WHIP))) {
            returnBV = (Math.ceil(entity.getWeight() / 10.0) + 1.0) * 1.725;
        } else if (hasFlag(F_CLUB) && hasSubType(S_RETRACTABLE_BLADE)) {
            returnBV = Math.ceil(entity.getWeight() / 10.0) * 1.725;
        } else if (hasFlag(F_TARGCOMP)) {
            // 20% of direct_fire weaponry BV (half for rear-facing)
            double fFrontBV = 0.0, fRearBV = 0.0;
            for (Mounted m : entity.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if (m.isRearMounted()) {
                        fRearBV += wt.getBV(entity);
                    } else {
                        fFrontBV += wt.getBV(entity);
                    }
                }
            }
            if (fFrontBV > fRearBV) {
                returnBV = fFrontBV * 0.2 + fRearBV * 0.1;
            }
            returnBV = fRearBV * 0.2 + fFrontBV * 0.1;
        } else if (hasFlag(F_HAND_WEAPON) && hasSubType(S_CLAW)) {
            returnBV = (Math.ceil(entity.getWeight() / 7.0)) * 1.275;
        }

        return returnBV;
    }

    /**
     * Add all the types of misc eq we can create to the list
     */
    public static void initializeTypes() {
        // all tech level 1 stuff
        EquipmentType.addType(MiscType.createHeatSink());
        EquipmentType.addType(MiscType.createJumpJet());
        EquipmentType.addType(MiscType.createTreeClub());
        EquipmentType.addType(MiscType.createGirderClub());
        EquipmentType.addType(MiscType.createLimbClub());
        EquipmentType.addType(MiscType.createHatchet());
        EquipmentType.addType(MiscType.createVacuumProtection());
        EquipmentType.addType(MiscType.createStandard());

        // Start of Level2 stuff
        EquipmentType.addType(MiscType.createISDoubleHeatSink());
        EquipmentType.addType(MiscType.createCLDoubleHeatSink());
        EquipmentType.addType(MiscType.createISCASE());
        EquipmentType.addType(MiscType.createCLCASE());
        EquipmentType.addType(MiscType.createISMASC());
        EquipmentType.addType(MiscType.createCLMASC());
        EquipmentType.addType(MiscType.createTSM());
        EquipmentType.addType(MiscType.createC3S());
        EquipmentType.addType(MiscType.createC3I());
        EquipmentType.addType(MiscType.createISArtemis());
        EquipmentType.addType(MiscType.createCLArtemis());
        EquipmentType.addType(MiscType.createGECM());
        EquipmentType.addType(MiscType.createCLECM());
        EquipmentType.addType(MiscType.createISTargComp());
        EquipmentType.addType(MiscType.createCLTargComp());
        EquipmentType.addType(MiscType.createMekStealth());
        EquipmentType.addType(MiscType.createFerroFibrous());
        EquipmentType.addType(MiscType.createEndoSteel());
        EquipmentType.addType(MiscType.createBeagleActiveProbe());
        EquipmentType.addType(MiscType.createBloodhoundActiveProbe());
        EquipmentType.addType(MiscType.createTHBBloodhoundActiveProbe());
        EquipmentType.addType(MiscType.createCLActiveProbe());
        EquipmentType.addType(MiscType.createCLLightActiveProbe());
        EquipmentType.addType(MiscType.createISAPPod());
        EquipmentType.addType(MiscType.createCLAPPod());
        EquipmentType.addType(MiscType.createSword());
        EquipmentType.addType(MiscType.createISPPCCapacitor());
        EquipmentType.addType(MiscType.createRetractableBlade());
        EquipmentType.addType(MiscType.createChainWhip());
        EquipmentType.addType(MiscType.createISApolloFCS());
        EquipmentType.addType(MiscType.createEjectionSeat());
        EquipmentType.addType(MiscType.createIndustrialTSM());
        EquipmentType.addType(MiscType.createSalvageArm());
        EquipmentType.addType(MiscType.createSpotWelder());
        EquipmentType.addType(MiscType.createLiftHoist());
        EquipmentType.addType(MiscType.createTracks());
        EquipmentType.addType(MiscType.createISMASS());
        EquipmentType.addType(MiscType.createCLMASS());

        // Start of level 3 stuff
        EquipmentType.addType(MiscType.createImprovedJumpJet());
        EquipmentType.addType(MiscType.createCLImprovedJumpJet());
        EquipmentType.addType(MiscType.createJumpBooster());
        EquipmentType.addType(MiscType.createFerroFibrousPrototype());
        EquipmentType.addType(MiscType.createLightFerroFibrous());
        EquipmentType.addType(MiscType.createHeavyFerroFibrous());
        EquipmentType.addType(MiscType.createHardenedArmor());
        EquipmentType.addType(MiscType.createIndustrialArmor());
        EquipmentType.addType(MiscType.createHeavyIndustrialArmor());
        EquipmentType.addType(MiscType.createCommercialArmor());
        EquipmentType.addType(MiscType.createEndoSteelPrototype());
        EquipmentType.addType(MiscType.createReinforcedStructure());
        EquipmentType.addType(MiscType.createCompositeStructure());
        EquipmentType.addType(MiscType.createIndustrialStructure());
        EquipmentType.addType(MiscType.createIS1CompactHeatSink());
        EquipmentType.addType(MiscType.createIS2CompactHeatSinks());
        EquipmentType.addType(MiscType.createCLLaserHeatSink());
        EquipmentType.addType(MiscType.createISAngelECM());
        EquipmentType.addType(MiscType.createISTHBAngelECM());
        EquipmentType.addType(MiscType.createCLAngelECM());
        EquipmentType.addType(MiscType.createWatchdogECM());
        EquipmentType.addType(MiscType.createTHBMace());
        EquipmentType.addType(MiscType.createMace());
        EquipmentType.addType(MiscType.createDualSaw());
        EquipmentType.addType(MiscType.createChainsaw());
        EquipmentType.addType(MiscType.createRockCutter());
        EquipmentType.addType(MiscType.createBackhoe());
        EquipmentType.addType(MiscType.createPileDriver());
        EquipmentType.addType(MiscType.createArmoredCowl());
        EquipmentType.addType(MiscType.createNullSignatureSystem());
        EquipmentType.addType(MiscType.createVoidSignatureSystem());
        EquipmentType.addType(MiscType.createChameleonLightPolarizationField());
        EquipmentType.addType(MiscType.createLightMinesweeper());
        EquipmentType.addType(MiscType.createBridgeKit());
        EquipmentType.addType(MiscType.createVibroShovel());
        EquipmentType.addType(MiscType.createDemolitionCharge());
        EquipmentType.addType(MiscType.createISSuperCharger());
        EquipmentType.addType(MiscType.createCLSuperCharger());
        EquipmentType.addType(MiscType.createISMediumShield());
        EquipmentType.addType(MiscType.createISSmallShield());
        EquipmentType.addType(MiscType.createISLargeShield());
        EquipmentType.addType(MiscType.createISClaw());
        EquipmentType.addType(MiscType.createCLHarJel());
        EquipmentType.addType(MiscType.createISHarJel());
        EquipmentType.addType(MiscType.createISUMU());
        EquipmentType.addType(MiscType.createCLUMU());
        EquipmentType.addType(MiscType.createISLance());
        EquipmentType.addType(MiscType.createISWreckingBall());
        EquipmentType.addType(MiscType.createCLWreckingBall());
        EquipmentType.addType(MiscType.createISFlail());
        EquipmentType.addType(MiscType.createISMediumVibroblade());
        EquipmentType.addType(MiscType.createISSmallVibroblade());
        EquipmentType.addType(MiscType.createISLargeVibroblade());
        EquipmentType.addType(MiscType.createISBuzzsaw());
        EquipmentType.addType(MiscType.createCLBuzzsaw());
        EquipmentType.addType(MiscType.createCoolantSystem());
        EquipmentType.addType(MiscType.createHeavyArmor());
        EquipmentType.addType(MiscType.createSpikes());
        EquipmentType.addType(MiscType.createTalons());
        EquipmentType.addType(MiscType.createReactive());
        EquipmentType.addType(MiscType.createReflective());
        EquipmentType.addType(MiscType.createISCASEII());
        EquipmentType.addType(MiscType.createCLCASEII());
        EquipmentType.addType(MiscType.createISAES());
        EquipmentType.addType(MiscType.createCLAES());
        EquipmentType.addType(MiscType.createISModularArmor());
        EquipmentType.addType(MiscType.createCLModularArmor());
        EquipmentType.addType(MiscType.createCommsGear1());
        EquipmentType.addType(MiscType.createCommsGear2());
        EquipmentType.addType(MiscType.createCommsGear3());
        EquipmentType.addType(MiscType.createCommsGear4());
        EquipmentType.addType(MiscType.createCommsGear5());
        EquipmentType.addType(MiscType.createCommsGear6());
        EquipmentType.addType(MiscType.createCommsGear7());
        EquipmentType.addType(MiscType.createCommsGear8());
        EquipmentType.addType(MiscType.createCommsGear9());
        EquipmentType.addType(MiscType.createCommsGear10());
        EquipmentType.addType(MiscType.createCommsGear11());
        EquipmentType.addType(MiscType.createCommsGear12());
        EquipmentType.addType(MiscType.createCommsGear13());
        EquipmentType.addType(MiscType.createCommsGear14());
        EquipmentType.addType(MiscType.createCommsGear15());

        // Start BattleArmor equipment
        EquipmentType.addType(MiscType.createBABoardingClaw());
        EquipmentType.addType(MiscType.createBAAssaultClaws());
        EquipmentType.addType(MiscType.createBAFireResistantArmor());
        EquipmentType.addType(MiscType.createBasicStealth());
        EquipmentType.addType(MiscType.createStandardStealth());
        EquipmentType.addType(MiscType.createImprovedStealth());
        EquipmentType.addType(MiscType.createMine());
        EquipmentType.addType(MiscType.createMinesweeper());
        EquipmentType.addType(MiscType.createBAMagneticClamp());
        EquipmentType.addType(MiscType.createSingleHexECM());
        EquipmentType.addType(MiscType.createMimeticCamo());
        EquipmentType.addType(MiscType.createSimpleCamo());
        EquipmentType.addType(MiscType.createParafoil());
        EquipmentType.addType(MiscType.createSearchlight());
        EquipmentType.addType(MiscType.createISImprovedSensors());
        EquipmentType.addType(MiscType.createCLImprovedSensors());
        EquipmentType.addType(MiscType.createBAVibroClaw());
        EquipmentType.addType(MiscType.createCLBALightActiveProbe());
        EquipmentType.addType(MiscType.createISBALightActiveProbe());

        // support vee stuff
        EquipmentType.addType(MiscType.createEnvironmentalSealing());
        EquipmentType.addType(MiscType.createTractorModification());
        EquipmentType.addType(MiscType.createArmoredChassis());
    }

    public static MiscType createHeatSink() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Heat Sink";
        misc.setInternalName(misc.name);
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_HEAT_SINK;
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
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_JUMP_JET;
        misc.subType |= S_STANDARD;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createImprovedJumpJet() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Improved Jump Jet";
        misc.setInternalName(misc.name);
        misc.addLookupName("ImprovedJump Jet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 2;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_JUMP_JET;
        misc.subType |= S_IMPROVED;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createCLImprovedJumpJet() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "Improved Jump Jet";
        misc.setInternalName("Clan Improved Jump Jet");
        misc.addLookupName("Clan Improved Jump Jet");
        misc.addLookupName("CLImprovedJump Jet");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 2;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_JUMP_JET;
        misc.subType |= S_IMPROVED;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createTractorModification() {
        MiscType misc = new MiscType();

        misc.name = "Tractor Modification";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TRACTOR_MODIFICATION;
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

        misc.techLevel = TechConstants.T_INTRO_BOXSET;
        misc.name = "Hatchet";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_HATCHET;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    // Start of Level2 stuff

    public static MiscType createISDoubleHeatSink() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Double Heat Sink";
        misc.setInternalName("ISDoubleHeatSink");
        misc.addLookupName("IS Double Heat Sink");
        misc.addLookupName("ISDouble Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 3;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_DOUBLE_HEAT_SINK;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createCLDoubleHeatSink() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "Double Heat Sink";
        misc.setInternalName("CLDoubleHeatSink");
        misc.addLookupName("Clan Double Heat Sink");
        misc.addLookupName("CLDouble Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_DOUBLE_HEAT_SINK;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISCASE() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "CASE";
        misc.setInternalName("ISCASE");
        misc.addLookupName("IS CASE");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_CASE;
        misc.cost = 50000;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createCLCASE() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "CASE";
        misc.setInternalName("CLCASE");
        misc.addLookupName("Clan CASE");
        misc.tonnage = 0.0f;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_CASE;
        misc.cost = 50000;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISCASEII() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "CASE II";
        misc.setInternalName("ISCASEII");
        misc.addLookupName("IS CASE II");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_CASEII;
        misc.cost = 175000;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createCLCASEII() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        misc.name = "CASE II";
        misc.setInternalName("CLCASEII");
        misc.addLookupName("Clan CASE II");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.hittable = false;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_CASEII;
        misc.cost = 175000;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISMASC() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "MASC";
        misc.setInternalName("ISMASC");
        misc.addLookupName("IS MASC");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_MASC;
        misc.bv = 0;

        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);

        return misc;
    }

    public static MiscType createCLMASC() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "MASC";
        misc.setInternalName("CLMASC");
        misc.addLookupName("Clan MASC");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_MASC;
        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);

        return misc;
    }

    public static MiscType createISSuperCharger() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Supercharger";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS Super Charger");
        misc.addLookupName("ISSuperCharger");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_MASC;
        misc.subType |= S_SUPERCHARGER;
        misc.bv = 0;

        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);

        return misc;
    }

    public static MiscType createCLSuperCharger() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        misc.name = "Supercharger";
        misc.setInternalName("CL Super Charger");
        misc.addLookupName("CLSuperCharger");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_MASC;
        misc.subType |= S_SUPERCHARGER;
        misc.bv = 0;

        String[] saModes = { "Armed", "Off" };
        misc.setModes(saModes);

        return misc;
    }

    public static MiscType createTSM() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "TSM";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS TSM");
        misc.addLookupName("Triple Strength Myomer");
        misc.tonnage = 0;
        misc.criticals = 6;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TSM;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createIndustrialTSM() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Industrial TSM";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS Industrial TSM");
        misc.addLookupName("Industrial Triple Strength Myomer");
        misc.tonnage = 0;
        misc.criticals = 12;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_INDUSTRIAL_TSM;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createC3S() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "C3 Slave";
        misc.setInternalName("ISC3SlaveUnit");
        misc.addLookupName("IS C3 Slave");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 250000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_C3S;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createC3I() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "C3i Computer";
        misc.setInternalName("ISC3iUnit");
        misc.addLookupName("ISImprovedC3CPU");
        misc.addLookupName("IS C3i Computer");
        misc.tonnage = 2.5f;
        misc.criticals = 2;
        misc.cost = 750000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_C3I;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISArtemis() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Artemis IV FCS";
        misc.setInternalName("ISArtemisIV");
        misc.addLookupName("IS Artemis IV FCS");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.cost = 100000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ARTEMIS;

        return misc;
    }

    public static MiscType createCLArtemis() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "Artemis IV FCS";
        misc.setInternalName("CLArtemisIV");
        misc.addLookupName("Clan Artemis IV FCS");
        misc.tonnage = 1.0f;
        misc.cost = 100000;
        misc.criticals = 1;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ARTEMIS;

        return misc;
    }

    public static MiscType createISApolloFCS() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "MRM Apollo FCS";
        misc.setInternalName("ISApollo");
        misc.addLookupName("IS MRM Apollo Fire Control System");
        misc.addLookupName("IS MRM Apollo FCS");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.cost = 125000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_APOLLO;

        return misc;
    }

    public static MiscType createGECM() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Guardian ECM Suite";
        misc.setInternalName("ISGuardianECMSuite");
        misc.addLookupName("IS Guardian ECM");
        misc.addLookupName("ISGuardianECM");
        misc.addLookupName("IS Guardian ECM Suite");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 200000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ECM;
        misc.bv = 61;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCLECM() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "ECM Suite";
        misc.setInternalName("CLECMSuite");
        misc.addLookupName("Clan ECM Suite");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 200000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ECM;
        misc.bv = 61;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createISAngelECM() {
        MiscType misc = new MiscType();

        // Don't forget, this will eventually count double for ECCM.
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Angel ECM Suite";
        misc.setInternalName("ISAngelECMSuite");
        misc.addLookupName("IS Angel ECM Suite");
        misc.addLookupName("ISAngelECM");
        misc.tonnage = 2;
        misc.criticals = 2;
        misc.cost = 750000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ECM | F_ANGEL_ECM;
        misc.bv = 100;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createISTHBAngelECM() {
        MiscType misc = new MiscType();

        // Don't forget, this will eventually count double for ECCM.
        misc.techLevel = TechConstants.T_IS_UNOFFICIAL;
        misc.name = "THB Angel ECM Suite";
        misc.setInternalName("ISTHBAngelECMSuite");
        misc.addLookupName("IS THB Angel ECM Suite");
        misc.addLookupName("ISTHBAngelECM");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 1000000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ECM | F_ANGEL_ECM;
        misc.bv = 100;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCLAngelECM() {
        MiscType misc = new MiscType();

        // Don't forget, this will eventually count double for ECCM.
        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        misc.name = "Angel ECM Suite";
        misc.setInternalName("CLAngelECMSuite");
        misc.addLookupName("Clan Angel ECM Suite");
        misc.addLookupName("CLAngelECM");
        misc.addLookupName("Clan Angel ECM Suite");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 750000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ECM | F_ANGEL_ECM;
        misc.bv = 100;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createWatchdogECM() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        misc.name = "Watchdog ECM Suite";
        misc.setInternalName(Sensor.WATCHDOG);
        misc.addLookupName("Watchdog ECM Suite");
        misc.addLookupName("WatchdogECM");
        misc.addLookupName("CLWatchdogECM");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 500000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ECM | F_BAP;
        misc.bv = 73;
        misc.setModes(new String[] { "ECM" });
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createSword() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Sword";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_SWORD;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createChainWhip() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "Chain Whip";
        misc.setInternalName(misc.name);
        misc.tonnage = 3;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = 120000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_CHAIN_WHIP;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createRetractableBlade() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Retractable Blade";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_RETRACTABLE_BLADE;
        misc.bv = BV_VARIABLE;
        misc.setInstantModeSwitch(true);
        String[] modes = { "retracted", "extended" };
        misc.setModes(modes);

        return misc;
    }

    public static MiscType createSpotWelder() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Spot Welder";
        misc.setInternalName(misc.name);
        misc.tonnage = 2;
        misc.criticals = 1;
        misc.cost = 75000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_SPOT_WELDER;
        misc.bv = 5;

        return misc;
    }

    public static MiscType createTHBMace() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_UNOFFICIAL;
        misc.name = "Mace (THB)";
        misc.setInternalName(misc.name);
        misc.addLookupName("THB Mace");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_MACE_THB;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createMace() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "Mace";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = 130000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_MACE;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createBackhoe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Backhoe";
        misc.setInternalName(misc.name);
        misc.tonnage = 5;
        misc.criticals = 6;
        misc.cost = 50000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_BACKHOE;
        misc.bv = 8;

        return misc;
    }

    public static MiscType createLiftHoist() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Lift Hoist";
        misc.setInternalName(misc.name);
        misc.tonnage = 3;
        misc.criticals = 3;
        misc.cost = 50000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_LIFTHOIST;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createDualSaw() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Dual Saw";
        misc.setInternalName(misc.name);
        misc.tonnage = 7;
        misc.criticals = 7;
        misc.cost = 100000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_DUAL_SAW;
        misc.bv = 9;

        return misc;
    }

    public static MiscType createPileDriver() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Pile Driver";
        misc.setInternalName(misc.name);
        misc.addLookupName("PileDriver");
        misc.tonnage = 10;
        misc.criticals = 8;
        misc.cost = 100000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_PILE_DRIVER;
        misc.bv = 5;

        return misc;
    }

    public static MiscType createChainsaw() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Chainsaw";
        misc.setInternalName(misc.name);
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.cost = 100000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_CHAINSAW;
        misc.bv = 7;

        return misc;
    }

    public static MiscType createRockCutter() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.name = "Rock Cutter";
        misc.setInternalName(misc.name);
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.cost = 100000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_ROCK_CUTTER;
        misc.bv = 6;

        return misc;
    }

    public static MiscType createEjectionSeat() {
        MiscType misc = new MiscType();

        misc.name = "Ejection Seat";
        misc.setInternalName(misc.name);
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.cost = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_EJECTION_SEAT;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createArmoredChassis() {
        MiscType misc = new MiscType();

        misc.name = "Armored Chassis";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags |= F_TANK_EQUIPMENT | F_ARMORED_CHASSIS;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createArmoredCowl() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_UNOFFICIAL;
        misc.name = "Armored Cowl";
        misc.setInternalName(misc.name);
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 10000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COWL;
        misc.bv = 10;

        return misc;
    }

    /**
     * Targeting comps should NOT be spreadable. However, I've set them such as
     * a temp measure to overcome the following bug: TC space allocation is
     * calculated based on tonnage of direct-fire weaponry. However, since meks
     * are loaded location-by-location, when the TC is loaded it's very unlikely
     * that all of the weaponry will be attached, resulting in undersized comps.
     * Any remaining TC crits after the last expected one are being handled as a
     * 2nd TC, causing LocationFullExceptions.
     */

    public static MiscType createISTargComp() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Targeting Computer";
        misc.setInternalName("ISTargeting Computer");
        misc.addLookupName("IS Targeting Computer");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = BV_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TARGCOMP;
        // see note above
        misc.spreadable = true;
        String[] modes = { "Normal", "Aimed shot" };
        misc.setModes(modes);

        return misc;
    }

    public static MiscType createCLTargComp() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "Targeting Computer";
        misc.setInternalName("CLTargeting Computer");
        misc.addLookupName("Clan Targeting Computer");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.bv = BV_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TARGCOMP;
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
        misc.flags |= F_BOARDING_CLAW | F_BA_EQUIPMENT;
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
        misc.flags |= F_ASSAULT_CLAW | F_BA_EQUIPMENT;
        misc.bv = 3;

        return misc;
    }

    public static MiscType createBAFireResistantArmor() {
        MiscType misc = new MiscType();

        misc.name = "Fire Resistant Armor";
        misc.setInternalName("BA-Fire Resistant Armor");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags |= F_FIRE_RESISTANT | F_BA_EQUIPMENT;
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
        misc.flags |= F_STEALTH | F_BA_EQUIPMENT;
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
        misc.flags |= F_STEALTH | F_BA_EQUIPMENT;
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
        misc.flags |= F_STEALTH | F_BA_EQUIPMENT;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createMine() {
        MiscType misc = new MiscType();

        misc.name = "Mine";
        misc.setInternalName("Mine");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.flags |= F_BA_EQUIPMENT;
        misc.bv = 4;

        return misc;
    }

    public static MiscType createMinesweeper() {
        MiscType misc = new MiscType();

        misc.name = "Minesweeper";
        misc.setInternalName("Minesweeper");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags |= F_TOOLS;
        misc.subType |= S_MINESWEEPER;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createLightMinesweeper() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "Light Minesweeper";
        misc.setInternalName("Light Minesweeper");
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
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
        misc.flags |= F_MAGNETIC_CLAMP | F_BA_EQUIPMENT;
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
        misc.flags |= F_ECM | F_SINGLE_HEX_ECM | F_BA_EQUIPMENT;
        misc.bv = 0;
        misc.setModes(new String[] { "ECM" });
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
        misc.flags |= F_STEALTH | F_VISUAL_CAMO | F_BA_EQUIPMENT;
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
        misc.flags |= F_STEALTH | F_VISUAL_CAMO | F_BA_EQUIPMENT;
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
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_PARAFOIL;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createMekStealth() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_STEALTH));
        misc.addLookupName("Stealth Armor");
        misc.tonnage = 0; // ???
        misc.criticals = 12;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_STEALTH;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.bv = 0;

        return misc;
    }

    public static MiscType createNullSignatureSystem() {
        MiscType misc = new MiscType();

        misc.name = "Null Signature System";
        misc.setInternalName("Mek Null Signature System");
        misc.addLookupName("Null Signature System");
        misc.addLookupName("NullSignatureSystem");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_NULLSIG;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;

        return misc;
    }

    public static MiscType createVoidSignatureSystem() {
        MiscType misc = new MiscType();

        misc.name = "Void Signature System";
        misc.setInternalName("Mek Void Signature System");
        misc.addLookupName("Void Signature System");
        misc.addLookupName("VoidSignatureSystem");
        misc.tonnage = 0;
        misc.criticals = 7;
        misc.spreadable = true;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.flags |= F_MECH_EQUIPMENT | F_VOIDSIG;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;

        return misc;
    }

    public static MiscType createChameleonLightPolarizationField() {
        MiscType misc = new MiscType();

        misc.name = "Chameleon Light Polarization Field";
        misc.setInternalName("Chameleon Light Polarization Field");
        misc.addLookupName("Chameleon Light Polarization Field");
        misc.addLookupName("ChameleonLightPolarizationField");
        misc.tonnage = 0;
        misc.criticals = 6;
        misc.spreadable = true;
        String[] saModes = { "Off", "On" };
        misc.setModes(saModes);
        misc.setInstantModeSwitch(false);
        misc.flags |= F_MECH_EQUIPMENT | F_BA_EQUIPMENT;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;

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
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_FERRO_FIBROUS;
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
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_FERRO_FIBROUS;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;

        return misc;
    }

    public static MiscType createLightFerroFibrous() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_LIGHT_FERRO));
        misc.addLookupName("Light Ferro-Fibrous Armor");
        misc.addLookupName("LightFerro");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 7;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_FERRO_FIBROUS;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;

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
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_FERRO_FIBROUS;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;

        return misc;
    }

    public static MiscType createHardenedArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HARDENED);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HARDENED));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT;

        return misc;
    }

    public static MiscType createCommercialArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_COMMERCIAL));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT;
        return misc;
    }

    public static MiscType createIndustrialArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_INDUSTRIAL);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_INDUSTRIAL));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT;
        return misc;
    }

    public static MiscType createHeavyIndustrialArmor() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL));
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.hittable = false;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT;
        return misc;
    }

    public static MiscType createEndoSteel() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_STEEL));
        misc.addLookupName("Endo-Steel");
        misc.addLookupName("EndoSteel");
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_ENDO_STEEL;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createEndoSteelPrototype() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_ENDO_PROTOTYPE));
        misc.addLookupName("Endo-Steel Prototype");
        misc.addLookupName("EndoSteelPrototype");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 16;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_ENDO_STEEL;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;

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
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT;
        return misc;
    }

    public static MiscType createCommsGear1() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (1 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:1");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear2() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (2 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:2");
        misc.tonnage = 2;
        misc.criticals = 2;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear3() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (3 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:3");
        misc.tonnage = 3;
        misc.criticals = 3;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear4() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (4 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:4");
        misc.tonnage = 4;
        misc.criticals = 4;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear5() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (5 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:51");
        misc.tonnage = 5;
        misc.criticals = 5;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear6() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (6 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:6");
        misc.tonnage = 6;
        misc.criticals = 6;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear7() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (7 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:7");
        misc.tonnage = 7;
        misc.criticals = 7;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear8() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (8 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:8");
        misc.tonnage = 8;
        misc.criticals = 8;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear9() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (9 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:9");
        misc.tonnage = 9;
        misc.criticals = 9;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear10() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (10 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:10");
        misc.tonnage = 10;
        misc.criticals = 10;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear11() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (11 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:11");
        misc.tonnage = 11;
        misc.criticals = 11;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear12() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (12 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:12");
        misc.tonnage = 12;
        misc.criticals = 12;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear13() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (13 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:13");
        misc.tonnage = 13;
        misc.criticals = 13;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear14() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (14 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:14");
        misc.tonnage = 14;
        misc.criticals = 14;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

        return misc;
    }

    public static MiscType createCommsGear15() {
        MiscType misc = new MiscType();

        misc.name = "Communications Equipment (15 ton)";
        misc.setInternalName(misc.name);
        misc.addLookupName("CommsGear:15");
        misc.tonnage = 15;
        misc.criticals = 15;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COMMUNICATIONS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;
        String[] modes = { "Default", "ECCM", "Ghost Targets" };
        misc.setModes(modes);
        misc.setInstantModeSwitch(false);

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
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;

        return misc;
    }

    public static MiscType createIndustrialStructure() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_INDUSTRIAL);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_INDUSTRIAL));
        misc.addLookupName("Industrial");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_TW_ALL;

        return misc;
    }

    public static MiscType createCLLaserHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "Laser Heat Sink";
        misc.setInternalName(misc.name);
        misc.addLookupName("CLLaser Heat Sink");
        misc.tonnage = 1.0f;
        misc.criticals = 2;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_DOUBLE_HEAT_SINK | F_LASER_HEAT_SINK;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_CLAN_ADVANCED;

        return misc;
    }

    // It is possible to have 1 or 2 compact heat sinks
    // in a single critical slot - this is addressed by
    // creating 1 slot single and 1 slot double heat sinks
    // with the weight of 1 or 2 compact heat sinks
    public static MiscType createIS1CompactHeatSink() {
        MiscType misc = new MiscType();

        misc.name = "1 Compact Heat Sink";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS1 Compact Heat Sink");
        misc.tonnage = 1.5f;
        misc.criticals = 1;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_HEAT_SINK;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;

        return misc;
    }

    public static MiscType createIS2CompactHeatSinks() {
        MiscType misc = new MiscType();

        misc.name = "2 Compact Heat Sinks";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS2 Compact Heat Sinks");
        misc.tonnage = 3.0f;
        misc.criticals = 1;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_DOUBLE_HEAT_SINK;
        misc.bv = 0;
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;

        return misc;
    }

    public static MiscType createISImprovedSensors() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Improved Sensors";
        misc.setInternalName(Sensor.ISIMPROVED);
        misc.addLookupName("BAP (2 Hex)");
        misc.tonnage = 0.0f;
        misc.cost = 200000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_BAP | F_BA_EQUIPMENT;

        return misc;
    }

    public static MiscType createCLImprovedSensors() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "Improved Sensors";
        misc.setInternalName(Sensor.CLIMPROVED);
        misc.addLookupName("BAP (3 Hex)");
        misc.tonnage = 0.0f;
        misc.cost = 200000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_BAP | F_BA_EQUIPMENT;

        return misc;
    }

    public static MiscType createBeagleActiveProbe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Beagle Active Probe";
        misc.setInternalName(Sensor.BAP);
        misc.addLookupName("Beagle Active Probe");
        misc.addLookupName("ISBeagleActiveProbe");
        misc.addLookupName("IS Beagle Active Probe");
        misc.tonnage = 1.5f;
        misc.criticals = 2;
        misc.cost = 200000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_BAP;
        misc.bv = 10;

        return misc;
    }

    public static MiscType createBloodhoundActiveProbe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Bloodhound Active Probe";
        misc.setInternalName(Sensor.BLOODHOUND);
        misc.addLookupName("Bloodhound Active Probe");
        misc.addLookupName("ISBloodhoundActiveProbe");
        misc.addLookupName("IS Bloodhound Active Probe");
        misc.tonnage = 2;
        misc.criticals = 3;
        misc.cost = 500000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_BAP;
        misc.bv = 25;

        return misc;
    }

    public static MiscType createTHBBloodhoundActiveProbe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_UNOFFICIAL;
        misc.name = "Bloodhound Active Probe (THB)";
        misc.setInternalName("THBBloodhoundActiveProbe");
        misc.addLookupName("THB Bloodhound Active Probe");
        misc.addLookupName("ISTHBBloodhoundActiveProbe");
        misc.addLookupName("IS THB Bloodhound Active Probe");
        misc.tonnage = 5;
        misc.criticals = 2;
        misc.cost = 750000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_BAP;
        misc.bv = 25;

        return misc;
    }

    public static MiscType createCLActiveProbe() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "Active Probe";
        misc.setInternalName(Sensor.CLAN_AP);
        misc.addLookupName("Active Probe");
        misc.addLookupName("Clan Active Probe");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 200000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_BAP;
        misc.bv = 12;

        return misc;
    }

    public static MiscType createCLLightActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Light Active Probe";
        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.setInternalName(Sensor.LIGHT_AP);
        misc.addLookupName("CL Light Active Probe");
        misc.addLookupName("Light Active Probe");
        misc.addLookupName("Clan Light Active Probe");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.cost = 150000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_BAP;
        misc.bv = 7;

        return misc;
    }

    public static MiscType createCLBALightActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Light Active Probe";
        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.setInternalName(Sensor.CLBALIGHT_AP);
        misc.tonnage = 0.15f;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_BAP | F_BA_EQUIPMENT;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISBALightActiveProbe() {
        MiscType misc = new MiscType();

        misc.name = "Light Active Probe";
        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.setInternalName(Sensor.ISBALIGHT_AP);
        misc.tonnage = 0.25f;
        misc.criticals = 1;
        misc.cost = 50000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_BAP | F_BA_EQUIPMENT;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISAPPod() {
        MiscType misc = new MiscType();

        misc.name = "A-Pod";
        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.setInternalName("ISAntiPersonnelPod");
        misc.addLookupName("ISAPod");
        misc.addLookupName("IS A-Pod");
        misc.addLookupName("IS AP Pod");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.cost = 1500;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_AP_POD;
        misc.bv = 1;

        return misc;
    }

    public static MiscType createCLAPPod() {
        MiscType misc = new MiscType();

        misc.name = "A-Pod";
        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.setInternalName("CLAntiPersonnelPod");
        misc.addLookupName("Clan A-Pod");
        misc.addLookupName("CL AP Pod");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.cost = 1500;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_AP_POD;
        misc.bv = 1;

        return misc;
    }

    public static MiscType createSearchlight() {
        MiscType misc = new MiscType();

        misc.name = "Searchlight";
        misc.setInternalName("Searchlight");
        misc.addLookupName("BASearchlight");
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_SEARCHLIGHT;
        misc.bv = 0;
        misc.cost = 2000;

        return misc;
    }

    public static MiscType createBAVibroClaw() {
        MiscType misc = new MiscType();

        misc.name = "Vibroclaw";
        misc.setInternalName("BAVibroClaw");
        misc.tonnage = 0.0f;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags |= F_VIBROCLAW;
        misc.bv = 1;

        return misc;
    }

    public static MiscType createVacuumProtection() {
        MiscType misc = new MiscType();

        misc.name = "Vacuum Protection";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 0;
        misc.cost = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_VACUUM_PROTECTION;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createEnvironmentalSealing() {
        MiscType misc = new MiscType();

        misc.name = "Environmental Sealing";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 8;
        misc.cost = 0;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ENVIRONMENTAL_SEALING;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createJumpBooster() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Jump Booster";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.bv = 0;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_JUMP_BOOSTER;
        // see note above
        misc.spreadable = true;

        return misc;
    }

    public static MiscType createDemolitionCharge() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "Demolition Charge";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TOOLS;
        misc.subType |= S_DEMOLITION_CHARGE;
        misc.toHitModifier = 1;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createVibroShovel() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "Vibro-Shovel";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TOOLS;
        misc.subType |= S_VIBROSHOVEL;
        misc.toHitModifier = 1;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createBridgeKit() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "Bridge Kit";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.hittable = false;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TOOLS;
        misc.subType |= S_BRIDGE_KIT;
        misc.toHitModifier = 1;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISSmallShield() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Small Shield";
        misc.setInternalName("ISSmallShield");
        misc.addLookupName("Small Shield");
        misc.tonnage = 2;
        misc.criticals = 3;
        misc.cost = 50000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_SHIELD_SMALL;
        misc.bv = 50;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD };
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 3;
        misc.baseDamageCapacity = 11;

        return misc;
    }

    /**
     * Creates a claw MiscType Object
     * 
     * @return MiscType
     */
    public static MiscType createISClaw() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "Claw";
        misc.setInternalName("ISClaw");
        misc.addLookupName("Claw");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_HAND_WEAPON;
        misc.subType |= S_CLAW;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createISMediumShield() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Medium Shield";
        misc.setInternalName("ISMediumShield");
        misc.addLookupName("Medium Shield");
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.cost = 100000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_SHIELD_MEDIUM;
        misc.bv = 135;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD };
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 5;
        misc.baseDamageCapacity = 18;

        return misc;
    }

    public static MiscType createISLargeShield() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Large Shield";
        misc.setInternalName("ISLargeShield");
        misc.addLookupName("Large Shield");
        misc.tonnage = 6;
        misc.criticals = 7;
        misc.cost = 300000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_SHIELD_LARGE;
        misc.bv = 263;
        misc.setInstantModeSwitch(true);
        String[] modes = { S_NO_SHIELD, S_ACTIVE_SHIELD, S_PASSIVE_SHIELD };
        misc.setModes(modes);
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 7;
        misc.baseDamageCapacity = 25;

        return misc;
    }

    public static MiscType createCLHarJel() {
        MiscType misc = new MiscType();
        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        misc.name = "HarJel";
        misc.setInternalName("Clan HarJel");
        misc.addLookupName("Clan HarJel");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 120000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_HARJEL;
        // can't enter BV here, because it's location dependendent,
        // and MiscType has no idea where a certain equipment may be
        // mounted
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISHarJel() {
        MiscType misc = new MiscType();
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "HarJel";
        misc.setInternalName("IS HarJel");
        misc.addLookupName("IS HarJel");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 120000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_HARJEL;
        // can't enter BV here, because it's location dependendent,
        // and MiscType has no idea where a certain equipment may be
        // mounted
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISAES() {
        MiscType misc = new MiscType();
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "AES";
        misc.setInternalName("ISAES");
        misc.addLookupName("IS Actuator Enhancement System");
        misc.addLookupName("ISActuatorEnhancementSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ACTUATOR_ENHANCEMENT_SYSTEM;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createCLAES() {
        MiscType misc = new MiscType();
        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        misc.name = "AES";
        misc.setInternalName("CLAES");
        misc.addLookupName("CL Actuator Enhancement System");
        misc.addLookupName("CLActuatorEnhancementSystem");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_ACTUATOR_ENHANCEMENT_SYSTEM;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createISUMU() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "UMU";
        misc.setInternalName("ISUMU");
        misc.addLookupName("IS Underwater Maneuvering Unit");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_UMU;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createCLUMU() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        misc.name = "UMU";
        misc.setInternalName("CLUMU");
        misc.addLookupName("Clan Underwater Maneuvering Unit");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 1;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_UMU;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISLance() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Lance";
        misc.setInternalName("IS Lance");
        misc.addLookupName("ISLance");
        misc.addLookupName("Lance");
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_LANCE;
        misc.bv = BV_VARIABLE;

        return misc;
    }

    public static MiscType createISFlail() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Flail";
        misc.setInternalName("IS Flail");
        misc.addLookupName("Flail");
        misc.tonnage = 5;
        misc.criticals = 4;
        misc.cost = 110000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_FLAIL;
        misc.bv = 11;

        return misc;
    }

    public static MiscType createISWreckingBall() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "Wrecking Ball";
        misc.setInternalName("IS Wrecking Ball");
        misc.addLookupName("WreckingBall");
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.cost = 110000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_WRECKING_BALL;
        misc.bv = 8;

        return misc;
    }

    public static MiscType createCLWreckingBall() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_TW;
        misc.name = "Wrecking Ball";
        misc.setInternalName("Clan Wrecking Ball");
        misc.addLookupName("CLWrecking Ball");
        misc.tonnage = 4;
        misc.criticals = 5;
        misc.cost = 110000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_WRECKING_BALL;
        misc.bv = 8;

        return misc;
    }

    public static MiscType createISSmallVibroblade() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Small Vibroblade";
        misc.setInternalName("ISSmallVibroblade");
        misc.addLookupName("Small Vibroblade");
        misc.tonnage = 3;
        misc.criticals = 1;
        misc.cost = 150000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_VIBRO_SMALL;
        misc.bv = 12;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive", "Active" };
        misc.setModes(modes);

        return misc;
    }

    public static MiscType createISMediumVibroblade() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Medium Vibroblade";
        misc.setInternalName("ISMediumVibroblade");
        misc.addLookupName("Medium Vibroblade");
        misc.tonnage = 5;
        misc.criticals = 2;
        misc.cost = 400000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_VIBRO_MEDIUM;
        misc.bv = 17;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive", "Active" };
        misc.setModes(modes);

        return misc;
    }

    public static MiscType createISLargeVibroblade() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Large Vibroblade";
        misc.setInternalName("ISLargeVibroblade");
        misc.addLookupName("Large Vibroblade");
        misc.tonnage = 7;
        misc.criticals = 4;
        misc.cost = 750000;
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_VIBRO_LARGE;
        misc.bv = 24;
        misc.setInstantModeSwitch(true);
        String[] modes = { "Inactive", "Active" };
        misc.setModes(modes);

        return misc;
    }

    public static MiscType createISBuzzsaw() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Buzzsaw";
        misc.setInternalName(misc.name);
        misc.addLookupName("IS Buzzsaw");
        misc.tonnage = 4;
        misc.criticals = 2;
        misc.cost = 100000;// From the Ask the Writer Forum
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_BUZZSAW;
        misc.bv = 67;// From the Ask the Writer Forum

        return misc;
    }

    public static MiscType createCLBuzzsaw() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        misc.name = "Buzzsaw";
        misc.setInternalName("CLBuzzsaw");
        misc.addLookupName("Clan Buzzsaw");
        misc.tonnage = 4;
        misc.criticals = 2;
        misc.cost = 100000;// From the Ask the Writer Forum
        misc.flags |= F_MECH_EQUIPMENT | F_CLUB;
        misc.subType |= S_BUZZSAW;
        misc.bv = 6;// From the Ask the Writer Forum

        return misc;
    }

    public static MiscType createCoolantSystem() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "Coolant System";
        misc.setInternalName(misc.name);
        misc.tonnage = 9;
        misc.criticals = 2;
        misc.cost = 90000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_COOLANT_SYSTEM;
        misc.bv = 15;

        return misc;
    }

    public static MiscType createSpikes() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Spikes";
        misc.setInternalName(misc.name);
        misc.tonnage = 0.5f;
        misc.criticals = 1;
        misc.cost = 90000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_SPIKES;
        misc.bv = 3;

        return misc;
    }

    public static MiscType createTalons() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_ADVANCED;
        misc.name = "Talons";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = 2;
        misc.cost = 90000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TALON;
        misc.bv = 3;

        return misc;
    }

    public static MiscType createHeavyArmor() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_ADVANCED;
        misc.name = "Heavy Armor";
        misc.setInternalName(misc.name);
        misc.tonnage = 0;
        misc.criticals = 0;
        misc.cost = 100000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TOOLS;
        misc.subType = S_HEAVY_ARMOR;
        misc.bv = 15;

        return misc;
    }

    public static MiscType createStandard() {
        // This is not really a single piece of equipment, it is used to
        // identify "standard" internal structure, armor, whatever.
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getStructureTypeName(T_STRUCTURE_STANDARD);
        misc.setInternalName(EquipmentType.getStructureTypeName(T_STRUCTURE_STANDARD));
        misc.addLookupName("Regular");
        misc.addLookupName("Standard Armor");

        return misc;
    }

    public static MiscType createISPPCCapacitor() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_TW_NON_BOX;
        misc.name = "PPC Capacitor";
        misc.setInternalName(misc.name);
        misc.addLookupName("ISPPCCapacitor");
        misc.addLookupName("LPPC Capacitor");
        misc.addLookupName("ISLightPPCCapacitor");
        misc.addLookupName("SNPPC Capacitor");
        misc.addLookupName("ISSNPPCCapacitor");
        misc.addLookupName("ERPPC Capacitor");
        misc.addLookupName("ISERPPCCapacitor");
        misc.addLookupName("HPPC Capacitor");
        misc.addLookupName("ISHeavyPPCCapacitor");
        misc.tonnage = 1.0f;
        misc.criticals = 1;
        misc.cost = 150000;
        misc.setModes(new String[] { "Off", "Charge" });
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_PPC_CAPACITOR;
        misc.setInstantModeSwitch(false);
        misc.explosive = true;
        // misc.bv = 88;
        misc.bv = 0;
        return misc;
    }

    public static MiscType createReflective() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REFLECTIVE));
        misc.addLookupName("Reflective Armor");
        misc.addLookupName("Reflective");
        misc.tonnage = 0;
        misc.criticals = CRITICALS_VARIABLE;
        misc.hittable = false;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_REFLECTIVE;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createReactive() {
        MiscType misc = new MiscType();

        misc.name = EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE);
        misc.setInternalName(EquipmentType.getArmorTypeName(EquipmentType.T_ARMOR_REACTIVE));
        misc.addLookupName("Reactive Armor");
        misc.addLookupName("Reactive");
        misc.tonnage = 0;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_REACTIVE;
        misc.bv = 0;

        return misc;
    }

    public static MiscType createISModularArmor() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;
        misc.name = "Modular Armor";
        misc.setInternalName("ISModularArmor");
        misc.setInternalName("IS Modular Armor");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 100000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_MODULAR_ARMOR;
        misc.bv = BV_VARIABLE;
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 10;
        misc.baseDamageCapacity = 10;

        return misc;
    }

    public static MiscType createCLModularArmor() {
        MiscType misc = new MiscType();

        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;
        misc.name = "Modular Armor";
        misc.setInternalName("CLModularArmor");
        misc.addLookupName("Clan Modular Armor");
        misc.tonnage = 1;
        misc.criticals = 1;
        misc.cost = 100000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_MODULAR_ARMOR;
        misc.bv = BV_VARIABLE;
        misc.damageTaken = 0;
        misc.baseDamageAbsorptionRate = 10;
        misc.baseDamageCapacity = 10;

        return misc;
    }

    public static MiscType createSalvageArm() {
        MiscType misc = new MiscType();

        misc.name = "Salvage Arm";
        misc.setInternalName(misc.name);
        misc.addLookupName("SalvageArm");
        misc.tonnage = 3;
        misc.criticals = 2;
        misc.bv = 0;
        misc.cost = 50000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_SALVAGE_ARM;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;

        return misc;
    }

    public static MiscType createTracks() {
        MiscType misc = new MiscType();

        misc.name = "Tracks";
        misc.setInternalName(misc.name);
        misc.tonnage = TONNAGE_VARIABLE;
        misc.criticals = CRITICALS_VARIABLE;
        misc.spreadable = true;
        misc.bv = 0;
        misc.cost = COST_VARIABLE;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_TRACKS;
        misc.techLevel = TechConstants.T_ALLOWED_ALL;

        return misc;
    }

    public static MiscType createISMASS() {
        MiscType misc = new MiscType();

        misc.name = "MASS";
        misc.setInternalName("ISMASS");
        misc.addLookupName("IS Mass");
        misc.tonnage = 1.5f;
        misc.criticals = 1;
        misc.bv = 9;
        misc.cost = 4000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_MASS;
        misc.techLevel = TechConstants.T_IS_EXPERIMENTAL;

        return misc;
    }

    public static MiscType createCLMASS() {
        MiscType misc = new MiscType();

        misc.name = "MASS";
        misc.setInternalName("CLMASS");
        misc.addLookupName("Clan Mass");
        misc.tonnage = 1.5f;
        misc.criticals = 1;
        misc.bv = 9;
        misc.cost = 4000;
        misc.flags |= F_MECH_EQUIPMENT | F_TANK_EQUIPMENT | F_MASS;
        misc.techLevel = TechConstants.T_CLAN_EXPERIMENTAL;

        return misc;
    }

    public static String getTargetSysName(int targSysType) {
        if ((targSysType < 0) || (targSysType >= targSysNames.length)) {
            return null;
        }
        return targSysNames[targSysType];
    }

    public static int getTargetSysType(String targSysName) {
        for (int x = 0; x < targSysNames.length; x++) {
            if (targSysNames[x].compareTo(targSysName) == 0) {
                return x;
            }
        }
        return -1;
    }
}
