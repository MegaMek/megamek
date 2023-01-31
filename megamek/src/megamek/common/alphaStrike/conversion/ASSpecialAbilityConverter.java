/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.alphaStrike.conversion;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.AlphaStrikeHelper;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.capitalweapons.ScreenLauncherWeapon;

import java.util.Arrays;

import static megamek.client.ui.swing.calculationReport.CalculationReport.formatForReport;
import static megamek.common.MiscType.*;
import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ASSpecialAbilityConverter {

    protected Entity entity;
    protected AlphaStrikeElement element;
    protected CalculationReport report;

    static ASSpecialAbilityConverter getConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        if (element.isLargeAerospace()) { // SC, DS, DA, JS, SS, WS
            return new ASLargeAeroSpecialAbilityConverter(entity, element, report);
        } else if (element.isProtoMek()) { // PM
            return new ASProtoMekSpecialAbilityConverter(entity, element, report);
        } else if (element.isMek()) {   // BM, IM
            return new ASMekSpecialAbilityConverter(entity, element, report);
        } else if (entity instanceof Tank) { // CV and ground SV
            return new ASVehicleSpecialAbilityConverter(entity, element, report);
        } else if (element.isAerospace()) { // CF, AF and aero SV
            return new ASAeroSpecialAbilityConverter(entity, element, report);
        } else if (element.isInfantry()) { // CI, BA
            return new ASInfantrySpecialAbilityConverter(entity, element, report);
        } else { // MS - NYI
            return new ASSpecialAbilityConverter(entity, element, report);
        }
    }

    /**
     * Do not call this directly. Use the static getASDamageConverter instead.
     * Constructs a damage converter for ground units.
     *
     * @param entity The entity to convert damage for
     * @param element The partially-converted element corresponding to the entity
     * @param report The calculation report to write to
     */
    protected ASSpecialAbilityConverter(Entity entity, AlphaStrikeElement element, CalculationReport report) {
        this.element = element;
        this.entity = entity;
        this.report = report;
    }

    void processAbilities() {
        report.addEmptyLine();
        report.addSubHeader("Further Special Abilities:");

        processENE();
        processEquipment();
        processUnitFeatures();
        finalizeSpecials();
    }

    protected void processENE() {
        for (Mounted equipment : entity.getEquipment()) {
            if (isExplosive(equipment)) {
                report.addLine(equipment.getName(), "Explosive equipment", "No ENE");
                if (entity.isClan() && element.isType(BM, IM, SV, CV, MS)) {
                    report.addLine(equipment.getName(), "Explosive equipment", "CASE");
                    element.getSpecialAbilities().setSUA(CASE);
                }
                return;
            }
        }
        assign("No Explosive Component", ENE);
    }

    protected void processEquipment() {
        for (Mounted equipment : entity.getEquipment()) {
            if ((equipment.getType() instanceof MiscType)) {
                processMiscMounted(equipment);
            }
            if (equipment.getType() instanceof ScreenLauncherWeapon) {
                assign(equipment, SCR, 1);
            }
        }
    }

    protected void processMiscMounted(Mounted misc) {
        MiscType miscType = (MiscType) misc.getType();
        if (miscType.is(Sensor.EW_EQUIPMENT)) {
            assign(misc, ECM);
            assign(misc, LPRB);
        } else if (miscType.isAnyOf(Sensor.BAP, Sensor.BAPP, Sensor.CLAN_AP)) {
            assign(misc, PRB);
        } else if (miscType.isAnyOf(Sensor.CLIMPROVED, Sensor.ISIMPROVED)) {
            assign(misc, RCN);
        } else if (miscType.isAnyOf(Sensor.LIGHT_AP, Sensor.ISBALIGHT_AP)) {
            assign(misc, LPRB);
        } else if (miscType.isAnyOf(Sensor.BLOODHOUND)) {
            assign(misc, BH);
        } else if (miscType.isAnyOf(Sensor.WATCHDOG)) {
            assign(misc, LPRB, ECM, WAT);
        } else if (miscType.isAnyOf(Sensor.NOVA)) {
            assign(misc, PRB, ECM, NOVA);
            assign(misc, MHQ, 1.5);
        } else if (miscType.hasFlag(F_ECM)) {
            if (miscType.hasFlag(F_ANGEL_ECM)) {
                assign(misc, AECM);
            } else if (miscType.hasFlag(F_SINGLE_HEX_ECM)) {
                assign(misc, LECM);
            } else {
                assign(misc, ECM);
            }
        } else if (miscType.hasFlag(F_BOOBY_TRAP) && !element.isType(PM, CI, BA)) {
            assign(misc, BT);
        } else if (miscType.hasFlag(F_LIGHT_BRIDGE_LAYER)
                || miscType.hasFlag(F_MEDIUM_BRIDGE_LAYER)
                || miscType.hasFlag(F_HEAVY_BRIDGE_LAYER)) {
            assign(misc, BRID);
        } else if (miscType.hasFlag(F_C3S)) {
            assign(misc, C3S);
            assign(misc, MHQ, 1);
            if (miscType.hasFlag(F_C3EM)) {
                assign(misc, C3EM, 1);
            }
        } else if (miscType.hasFlag(F_C3SBS)) {
            assign(misc, C3BSS, 1);
            assign(misc, MHQ, 2);
        } else if (miscType.hasFlag(F_NAVAL_C3)) {
            assign(misc, NC3);
        } else if (miscType.hasFlag(F_C3I)) {
            assign(misc, C3I);
            if (miscType.hasFlag(F_BA_EQUIPMENT)) {
                assign(misc, MHQ, 2);
            } else {
                assign(misc, MHQ, 2.5);
            }
        } else if (miscType.hasFlag(F_CASE) && eligibleForCASE()) {
            assign(misc, CASE);
        } else if (miscType.hasFlag(F_CASEP) && eligibleForCASE()) {
            assign(misc, CASEP);
        } else if (miscType.hasFlag(F_CASEII) && eligibleForCASE()) {
            assign(misc, CASEII);
        } else if (miscType.hasFlag(F_DRONE_OPERATING_SYSTEM)) {
            assign(misc, DRO);
        } else if (miscType.hasFlag(F_SRCS)
                || miscType.hasFlag(F_SASRCS)
                || miscType.hasFlag(F_CASPAR)
                || miscType.hasFlag(F_CASPARII)) {
            assign(misc, RBT);
            if (miscType.hasFlag(F_CASPAR)) {
                assign(misc, SDCS);
            }
        } else if (miscType.hasFlag(F_DRONE_CARRIER_CONTROL)) {
            assign(misc, DCC, (int) misc.getSize());
        } else if (miscType.hasFlag(F_REMOTE_DRONE_COMMAND_CONSOLE)) {
            assign(misc, DCC, 1);
        } else if (miscType.hasFlag(F_EJECTION_SEAT)) {
            assign(misc, ES);
        } else if (miscType.hasFlag(F_BULLDOZER)) {
            assign(misc, ENG);
        } else if (miscType.hasFlag(F_HAND_WEAPON)) {
            assign(misc, MEL);
        } else if (miscType.isAnyOf("ProtoQuadMeleeSystem")) {
            assign(misc, MEL);
        } else if (miscType.hasFlag(F_TALON)) {
            assign(misc, MEL);
        } else if (miscType.hasFlag(F_CLUB)) {
            assign(misc, MEL);
            if ((miscType.getSubType() & (S_BACKHOE | S_PILE_DRIVER
                    | S_MINING_DRILL | S_ROCK_CUTTER
                    | S_WRECKING_BALL)) != 0) {
                assign(misc, ENG);
            } else if ((miscType.getSubType() & (S_DUAL_SAW | S_CHAINSAW
                    | S_BUZZSAW | S_RETRACTABLE_BLADE)) != 0) {
                assign(misc, SAW);
            }
            if (miscType.isShield()) {
                assign(misc, SHLD);
            }
        } else if (miscType.hasFlag(F_SPIKES)) {
            assign(misc, MEL);
        } else if (miscType.hasFlag(F_FIRE_RESISTANT)) {
            assign(misc, FR);
        } else if (miscType.hasFlag(F_MOBILE_HPG)) {
            assign(misc, HPG);
        } else if (miscType.hasFlag(F_COMMUNICATIONS)) {
            assign(misc, MHQ, (int) misc.getTonnage());
            if (misc.getTonnage() >= entity.getWeight() / 20.0) {
                assign(misc, RCN);
            }
        } else if (miscType.hasFlag(F_SENSOR_DISPENSER)) {
            assign(misc, RSD, 1);
            assign(misc, RCN);
        } else if (miscType.hasFlag(F_LOOKDOWN_RADAR)
                || miscType.hasFlag(F_RECON_CAMERA)
                || miscType.hasFlag(F_HIRES_IMAGER)
                || miscType.hasFlag(F_HYPERSPECTRAL_IMAGER)
                || miscType.hasFlag(F_INFRARED_IMAGER)) {
            assign(misc, RCN);
        } else if (miscType.hasFlag(F_SEARCHLIGHT)) {
            assign(misc, SRCH);
        } else if (miscType.hasFlag(F_RADICAL_HEATSINK)) {
            assign(misc, RHS);
        } else if (miscType.hasFlag(F_EMERGENCY_COOLANT_SYSTEM)) {
            assign(misc, ECS);
        } else if (miscType.hasFlag(F_VIRAL_JAMMER_DECOY)) {
            assign(misc, DJ);
        } else if (miscType.hasFlag(F_VIRAL_JAMMER_HOMING)) {
            assign(misc, HJ);
        } else if (miscType.hasFlag(F_CARGO)) {
            assign(misc, CT, (int) misc.getTonnage());
        } else if (miscType.hasFlag(F_HARJEL)) {
            assign(misc, BHJ);
        } else if (miscType.hasFlag(F_HARJEL_II)) {
            assign(misc, BHJ2);
        } else if (miscType.hasFlag(F_HARJEL_III)) {
            assign(misc, BHJ3);
        } else if (miscType.is(EquipmentTypeLookup.P_TSM)) {
            assign(misc, TSMX);
        } else if (miscType.hasFlag(F_INDUSTRIAL_TSM)) {
            assign(misc, ITSM);
        } else if (miscType.hasFlag(F_TSM)) {
            assign(misc, TSM);
        } else if (miscType.hasFlag(F_VOIDSIG)) {
            assign(misc, MAS);
        } else if (miscType.hasFlag(F_NULLSIG)
                || miscType.hasFlag(F_CHAMELEON_SHIELD)) {
            assign(misc, STL);
        } else if (miscType.hasFlag(F_UMU)) {
            assign(misc, UMU);
        } else if (miscType.hasFlag(F_EW_EQUIPMENT)) {
            assign(misc, ECM);
        } else if (miscType.hasFlag(F_ADVANCED_FIRECONTROL)) {
            assign(misc, AFC);
        } else if (miscType.hasFlag(F_BASIC_FIRECONTROL)) {
            assign(misc, BFC);
        } else if (miscType.hasFlag(F_AMPHIBIOUS) || miscType.hasFlag(F_FULLY_AMPHIBIOUS)
                || miscType.hasFlag(F_LIMITED_AMPHIBIOUS)) {
            assign(misc, AMP);
        } else if (miscType.hasFlag(F_ARMORED_MOTIVE_SYSTEM)) {
            assign(misc, ARS);
        } else if (miscType.hasFlag(F_VEHICLE_MINE_DISPENSER)) {
            assign(misc, MDS, 2);
        } else if (miscType.hasFlag(F_MINESWEEPER)) {
            assign(misc, MSW);
        } else if (miscType.hasFlag(F_MASH)) {
            assign(misc, MASH, (int) misc.getSize());
        } else if (miscType.hasFlag(F_MOBILE_FIELD_BASE)) {
            assign(misc, MFB);
        } else if (miscType.hasFlag(F_OFF_ROAD)) {
            assign(misc, ORO);
        } else if (miscType.hasFlag(F_DUNE_BUGGY)) {
            assign(misc, DUN);
        } else if (miscType.hasFlag(F_TRACTOR_MODIFICATION)
                || miscType.hasFlag(F_TRAILER_MODIFICATION)
                || miscType.hasFlag(F_HITCH)) {
            assign(misc, HTC);
        } else if (miscType.hasFlag(F_COMMAND_CONSOLE)) {
            assign(misc, MHQ, 1);
        } else if (miscType.hasFlag(F_SPACE_MINE_DISPENSER)) {
            assign(misc, MDS, 2);
        }

        // TODO : Variable Range targeting (VRT) is not implemented: assign(misc, VRT);

        processSEALandSOA(misc);
    }

    protected boolean eligibleForCASE() {
        return !(element.isInfantry() || element.isProtoMek() || element.isFighter() || element.isLargeAerospace());
    }

    protected void processSEALandSOA(Mounted misc) { }

    protected void processATMO() { }

    protected boolean hasSoaCapableEngine() {
        if (!entity.hasEngine()) {
            return false;
        }
        int engineType = entity.getEngine().getEngineType();
        return entity.getEngine().isFusion() || (engineType == Engine.FUEL_CELL) || (engineType == Engine.FISSION);
    }

    protected void processUnitFeatures() {
        if (entity.hasQuirk(OptionsConstants.QUIRK_POS_TRAILER_HITCH)) {
            assign("Trailer Hitch Quirk", HTC);
        }

        if ((entity.getBARRating(0) >= 1) && (entity.getBARRating(0) <= 9)) {
            assign("BAR Rating " + entity.getBARRating(0), BAR);
        }

        if (!entity.hasPatchworkArmor()) {
            String armorType = "Armor: " + EquipmentType.getArmorTypeName(entity.getArmorType(0));
            switch (entity.getArmorType(0)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    assign(armorType, BAR);
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_HARDENED:
                    assign(armorType, CR);
                    break;
                case EquipmentType.T_ARMOR_STEALTH:
                case EquipmentType.T_ARMOR_STEALTH_VEHICLE:
                case EquipmentType.T_ARMOR_BA_STEALTH:
                case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
                case EquipmentType.T_ARMOR_BA_STEALTH_IMP:
                case EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE:
                    assign(armorType, STL);
                    break;
                case EquipmentType.T_ARMOR_BA_MIMETIC:
                    assign(armorType, MAS);
                    break;
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    assign(armorType, ABA);
                    break;
                case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                    assign(armorType, BRA);
                    break;
                case EquipmentType.T_ARMOR_BA_FIRE_RESIST:
                case EquipmentType.T_ARMOR_HEAT_DISSIPATING:
                    assign(armorType, FR);
                    break;
                case EquipmentType.T_ARMOR_IMPACT_RESISTANT:
                    assign(armorType, IRA);
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                case EquipmentType.T_ARMOR_BA_REACTIVE:
                    assign(armorType, RCA);
                    break;
                case EquipmentType.T_ARMOR_REFLECTIVE:
                case EquipmentType.T_ARMOR_BA_REFLECTIVE:
                    assign(armorType, RFA);
                    break;
            }
        }

        if (entity.hasEngine() && !element.isLargeAerospace()) {
            String engineName = "Engine: " + entity.getEngine().getEngineName();
            if (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) {
                assign(engineName, EE);
            } else if (entity.getEngine().getEngineType() == Engine.FUEL_CELL) {
                assign(engineName, FC);
            }
        }

        if (element.getMovementModes().contains("j") && !element.getPrimaryMovementMode().equals("j")) {
            int jumpTMM = ASConverter.tmmForMovement(element.getMovement("j"));
            int walkTMM = ASConverter.tmmForMovement(element.getPrimaryMovementValue());
            if (jumpTMM > walkTMM) {
                assign("Strong Jumper", JMPS, jumpTMM - walkTMM);
            } else if (jumpTMM < walkTMM) {
                assign("Weak Jumper", JMPW, walkTMM - jumpTMM);
            }
        }

        if (element.getMovementModes().contains("s") && !element.getPrimaryMovementMode().equals("s")) {
            int umuTMM = ASConverter.tmmForMovement(element.getMovement("s"));
            int walkTMM = ASConverter.tmmForMovement(element.getPrimaryMovementValue());
            if (umuTMM > walkTMM) {
                assign("Strong UMU", SUBS, umuTMM - walkTMM);
            } else if (umuTMM < walkTMM) {
                assign("Weak UMU", SUBW, walkTMM - umuTMM);
            }
        }

        processATMO();
        processTransports();
        processARM();

        if (AlphaStrikeHelper.hasAutoSeal(element)) {
            assign("Sealed", SEAL);
        }

        if (element.isSupportVehicle()) {
            if (element.getSize() == 3) {
                assign("SIZE 3", LG);
            } else if (element.getSize() == 4) {
                assign("SIZE 4", VLG);
            } else if (element.getSize() == 5) {
                assign("SIZE 5", SLG);
            }
        }
    }

    protected void processARM() {
        for (int location = 0; location < entity.locations(); location++) {
            for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
                CriticalSlot crit = entity.getCritical(location, slot);
                if (null != crit) {
                    if (crit.isArmored()) {
                        assign("Armored Critical Slot", ARM);
                        return;
                    } else if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted mount = crit.getMount();
                        if (mount.isArmored()) {
                            assign(mount, ARM);
                            return;
                        }
                    }
                }
            }
        }
    }

    protected void processTransports() {
        for (Transporter t : entity.getTransports()) {
            if (t instanceof ASFBay) {
                assign("Fighter Bay", AT, (int) ((ASFBay) t).getCapacity());
                assign("Fighter Bay", ATxD, ((ASFBay) t).getDoors());
                processMFB(t);
            } else if (t instanceof CargoBay) {
                assign("Cargo Bay", CT, ((CargoBay) t).getCapacity());
                assign("Cargo Bay", CTxD, ((CargoBay) t).getDoors());
            } else if (t instanceof DockingCollar) {
                assign("Docking Collar", DT, 1);
            } else if (t instanceof InfantryBay) {
                assign("Infantry Bay", IT, ((InfantryBay) t).getCapacity());
            } else if (t instanceof TroopSpace) {
                assign("Troop Space", IT, t.getUnused());
            } else if (t instanceof MechBay) {
                assign("Mek Bay", MT, (int) ((MechBay) t).getCapacity());
                assign("Mek Bay", MTxD, ((MechBay) t).getDoors());
                processMFB(t);
            } else if (t instanceof ProtomechBay) {
                assign("ProtoMek Bay", PT, (int) ((ProtomechBay) t).getCapacity());
                assign("ProtoMek Bay", PTxD, ((ProtomechBay) t).getDoors());
                processMFB(t);
            } else if (t instanceof SmallCraftBay) {
                assign("SmallCraft Bay", ST, (int) ((SmallCraftBay) t).getCapacity());
                assign("SmallCraft Bay", STxD, ((SmallCraftBay) t).getDoors());
                processMFB(t);
            } else if (t instanceof LightVehicleBay) {
                assign("Light Vehicle Bay", VTM, (int) ((LightVehicleBay) t).getCapacity());
                assign("Light Vehicle Bay", VTMxD, ((LightVehicleBay) t).getDoors());
                processMFB(t);
            } else if (t instanceof HeavyVehicleBay) {
                assign("Heavy Vehicle Bay", VTH, (int) ((HeavyVehicleBay) t).getCapacity());
                assign("Heavy Vehicle Bay", VTHxD, ((HeavyVehicleBay) t).getDoors());
                processMFB(t);
            }
        }
    }

    /** Assign MFB for certain types of Bay. Overridden for Large Aero that do not assign MFB. */
    protected void processMFB(Transporter transporter) {
        assign(transporter.toString(), MFB);
    }

    /** Returns true when the given Mounted blocks ENE. */
    protected static boolean isExplosive(Mounted equipment) {
        // LAM Bomb Bays are explosive
        if ((equipment.getType() instanceof MiscType) && equipment.getType().hasFlag(F_BOMB_BAY)) {
            return true;
        }
        // According to ASC p.123 Booby Traps count as explosive contrary to TO AUE p.109
        if ((equipment.getType() instanceof MiscType) && equipment.getType().hasFlag(F_BOOBY_TRAP)) {
            return true;
        }
        // Oneshot weapons internally have normal ammo allocated to them which must
        // be disqualified as explosive; such ammo has no location
        return equipment.getType().isExplosive(null) && (equipment.getExplosionDamage() > 0)
                && (equipment.getLocation() != Entity.LOC_NONE);
    }

    protected void finalizeSpecials() {
        // For MHQ, the values may contain decimals, but the the final MHQ value is rounded down to an int.
        if (element.getSUA(MHQ) instanceof Double) {
            double mhqValue = (double) element.getSUA(MHQ);
            element.getSpecialAbilities().replaceSUA(MHQ, (int) mhqValue);
        }

        // Cannot have both CASEII and CASE
        if (element.hasSUA(CASEII) && element.hasSUA(CASE)) {
            element.getSpecialAbilities().removeSUA(CASE);
            report.addLine("Has CASEII", "Remove CASE");
        }

        // Implicit rule: XMEC overrides MEC
        if (element.hasSUA(XMEC) && element.hasSUA(MEC)) {
            element.getSpecialAbilities().removeSUA(MEC);
            report.addLine("Has XMEC", "Remove MEC");
        }

        // Implicit rule: AECM overrides ECM
        if (element.hasSUA(AECM) && element.hasSUA(ECM)) {
            element.getSpecialAbilities().removeSUA(ECM);
            report.addLine("Has AECM", "Remove ECM");
        }

        // Some SUAs are accompanied by RCN
        if (element.hasAnySUAOf(PRB, LPRB, NOVA, BH, WAT)) {
            assign("PRB, LPRB, NOVA, BH or WAT", RCN);
        }

        // IT value may be decimal but replace it with an integer value if it is integer
        if (element.hasSUA(IT) && (element.getSUA(IT) instanceof Double)) {
            double ctValue = (double) element.getSUA(IT);
            if ((int) ctValue == ctValue) {
                element.getSpecialAbilities().replaceSUA(IT, (int) ctValue);
            }
        }

        // High CT values get converted to CK
        if (element.hasSUA(CT)) {
            double ctValue = 0;
            if (element.getSUA(CT) instanceof Double) {
                ctValue = (double) element.getSUA(CT);
            } else if (element.getSUA(CT) instanceof Integer) {
                ctValue = (int) element.getSUA(CT);
            }

            if (ctValue > 1000) {
                element.getSpecialAbilities().mergeSUA(CK, (int) Math.round(ctValue / 1000));
                element.getSpecialAbilities().mergeSUA(CKxD, (int) element.getSUA(CTxD));
                element.getSpecialAbilities().removeSUA(CT);
                element.getSpecialAbilities().removeSUA(CTxD);
                report.addLine("Replace CT with CK",
                        AlphaStrikeHelper.formatAbility(CK, element.getSpecialAbilities(), element, ", "));
            } else if (ctValue > 1) {
                element.getSpecialAbilities().replaceSUA(CT, (int) Math.round(ctValue));
                if (ctValue != (int) ctValue) {
                    report.addLine("Final CT value",
                            AlphaStrikeHelper.formatAbility(CT, element.getSpecialAbilities(), element, ", "));
                }
            }
        }

        // Armor 0 elements cannot get BAR
        if (element.getFullArmor() == 0) {
            element.getSpecialAbilities().removeSUA(BAR);
        }

        // A unit with ENE doesn't need any type of CASE
        if (element.hasSUA(ENE)) {
            element.getSpecialAbilities().removeSUA(CASE);
            element.getSpecialAbilities().removeSUA(CASEII);
            element.getSpecialAbilities().removeSUA(CASEP);
        }
    }

    /** Adds the sua(s) to the element and writes a report line for each, if it is not yet present. */
    protected void assign(Mounted equipment, BattleForceSUA firstSua, BattleForceSUA... moreSuas) {
        assign(equipment.getType().getName(), firstSua);
        Arrays.stream(moreSuas).forEach(sua -> assign(equipment.getType().getName(), sua));
    }

    /** Adds the sua to the element and writes a report line using the name of the given equipment if the sua is not yet present. */
    protected void assign(Mounted equipment, BattleForceSUA sua) {
        assign(equipment.getType().getName(), sua);
    }

    /** Adds the sua to the element and writes a report line using the given text - only if the sua is not yet present. */
    protected void assign(String text, BattleForceSUA sua) {
        if (!element.hasSUA(sua)) {
            addReportLine(text, sua, "");
            element.getSpecialAbilities().setSUA(sua);
        }
    }

    /** Writes a report line for adding the sua using the name of the given equipment. */
    protected void addReportLine(Mounted equipment, BattleForceSUA sua) {
        addReportLine(equipment, sua, "");
    }

    /** Adds the sua with the given value to the element and writes a report line using the name of the given equipment. */
    protected void assign(Mounted equipment, BattleForceSUA sua, double doubleAbilityValue) {
        assign(equipment.getType().getName(), sua, doubleAbilityValue);
    }

    /** Adds the sua with the given value to the element and writes a report line using the given text. */
    protected void assign(String text, BattleForceSUA sua, double doubleAbilityValue) {
        addReportLine(text, sua, formatForReport(doubleAbilityValue));
        element.getSpecialAbilities().mergeSUA(sua, doubleAbilityValue);
    }

    /** Adds the sua with the given value to the element and writes a report line using the name of the given equipment. */
    protected void assign(Mounted equipment, BattleForceSUA sua, int intAbilityValue) {
        assign(equipment.getType().getName(), sua, intAbilityValue);
    }

    /** Adds the sua with the given value to the element and writes a report line using the given text. */
    protected void assign(String text, BattleForceSUA sua, int intAbilityValue) {
        addReportLine(text, sua, intAbilityValue + "");
        element.getSpecialAbilities().mergeSUA(sua, intAbilityValue);
    }

    /** Writes a report line for adding the sua with the given value using the name of the given equipment. */
    protected void addReportLine(Mounted equipment, BattleForceSUA sua, String abilityValue) {
        addReportLine(equipment.getType().getName(), sua, abilityValue);
    }

    /** Writes a report line for adding the sua with the given value using the given text. */
    protected void addReportLine(String text, BattleForceSUA sua, String abilityValue) {
        String hiddenText = AlphaStrikeHelper.hideSpecial(sua, element) ? "(hidden)" : "";
        if (text.length() > 32) {
            text = text.substring(0, 31) + "...";
        }
        report.addLine(text, hiddenText, sua + abilityValue);
    }
}