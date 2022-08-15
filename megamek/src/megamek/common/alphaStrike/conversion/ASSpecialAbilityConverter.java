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
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.BayWeapon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

final class ASSpecialAbilityConverter {

    static void convertSpecialUnitAbilities(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity;
        CalculationReport report = conversionData.conversionReport;
        AlphaStrikeElement element = conversionData.element;

        report.addEmptyLine();
        report.addSubHeader("Special Unit Abilities:");

        boolean hasExplosiveComponent = false;
        boolean[] hasExplosiveArcComponent = new boolean[4];
        Arrays.fill(hasExplosiveArcComponent, false);
        for (Mounted m : entity.getEquipment()) {

            if (isExplosive(m) && !hasExplosiveComponent) {
                // Report this only once:
                report.addLine("Explosive equipment: ", m.getName(), "- ENE");
                hasExplosiveComponent = true;
            }

            if (element.usesArcs() && m.getType() instanceof BayWeapon) {
                var bayEquipment = new ArrayList<>(m.getBayWeapons());
                bayEquipment.addAll(m.getBayAmmo());
                for (int index : bayEquipment) {
                    Mounted insideBayWeapon = entity.getEquipment(index);
                    if (isExplosive(insideBayWeapon)) {
                        for (int arc = 0; arc < 4; arc++) {
                            if (ASLocationMapper.damageLocationMultiplier(entity, arc, insideBayWeapon) > 0) {
                                hasExplosiveArcComponent[arc] = true;
                            }
                        }
                    }
                }
            }

            if (!(m.getType() instanceof MiscType)) {
                continue;
            }

            if (m.getType().getInternalName().equals(Sensor.BAP)
                    || m.getType().getInternalName().equals(Sensor.BAPP)
                    || m.getType().getInternalName().equals(Sensor.CLAN_AP)) {
                report.addLine("BAP, P-BAP or C-AP", "PRB");
                element.getSpecialAbilities().addSPA(PRB);
            } else if (m.getType().getInternalName().equals(Sensor.LIGHT_AP)
                    || m.getType().getInternalName().equals(Sensor.ISBALIGHT_AP)
                    || m.getType().getInternalName().equals(Sensor.EW_EQUIPMENT)) {
                report.addLine("LAP, BA-LAP or EQ-Equip", "LPRB");
                element.getSpecialAbilities().addSPA(LPRB);
            } else if (m.getType().getInternalName().equals(Sensor.BLOODHOUND)) {
                report.addLine("Bloodhound", "BH");
                element.getSpecialAbilities().addSPA(BH);
            } else if (m.getType().getInternalName().equals(Sensor.WATCHDOG)) {
                report.addLine("Watchdog", "LPRB");
                report.addLine("ECM");
                report.addLine("WAT");
                element.getSpecialAbilities().addSPA(LPRB);
                element.getSpecialAbilities().addSPA(ECM);
                element.getSpecialAbilities().addSPA(WAT);
            } else if (m.getType().getInternalName().equals(Sensor.NOVA)) {
                report.addLine("Nova CEWS", "NOVA");
                report.addLine("PRB");
                report.addLine("", "ECM");
                report.addLine("", "MHQ1.5");
                element.getSpecialAbilities().addSPA(NOVA);
                element.getSpecialAbilities().addSPA(PRB);
                element.getSpecialAbilities().addSPA(ECM);
                element.getSpecialAbilities().addSPA(MHQ, 1.5);
            } else if (m.getType().hasFlag(MiscType.F_ECM)) {
                if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                    report.addLine("Angel ECM", "AECM");
                    element.getSpecialAbilities().addSPA(AECM);
                } else if (m.getType().hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                    report.addLine("Single-Hex ECM", "LECM");
                    element.getSpecialAbilities().addSPA(LECM);
                } else {
                    report.addLine("General ECM", "ECM");
                    element.getSpecialAbilities().addSPA(ECM);
                }
            } else if (m.getType().hasFlag(MiscType.F_BOOBY_TRAP) && !element.isType(PM, CI, BA)) {
                report.addLine("Booby Trap", "BT");
                element.getSpecialAbilities().addSPA(BT);
            } else if (m.getType().hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || m.getType().hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || m.getType().hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)) {
                report.addLine("Bridge Layer", "BRID");
                element.getSpecialAbilities().addSPA(BRID);
            } else if (m.getType().hasFlag(MiscType.F_C3S)) {
                report.addLine("C3 Slave", "C3S");
                report.addLine("", "MHQ1");
                element.getSpecialAbilities().addSPA(C3S);
                element.getSpecialAbilities().addSPA(MHQ, 1);
                if (m.getType().hasFlag(MiscType.F_C3EM)) {
                    report.addLine("C3 EM", "C3EM1");
                    element.getSpecialAbilities().addSPA(C3EM, 1);
                }
            } else if (m.getType().hasFlag(MiscType.F_C3SBS)) {
                report.addLine("C3 Boosted Slave", "C3BSS1");
                report.addLine("", "MHQ2");
                element.getSpecialAbilities().addSPA(C3BSS, 1);
                element.getSpecialAbilities().addSPA(MHQ, 2);
            } else if (m.getType().hasFlag(MiscType.F_C3I)) {
                if ((entity.getEntityType() & Entity.ETYPE_AERO) == Entity.ETYPE_AERO) {
                    report.addLine("Naval C3", "NC3");
                    element.getSpecialAbilities().addSPA(NC3);
                } else {
                    report.addLine("Improved C3", "C3I");
                    element.getSpecialAbilities().addSPA(C3I);
                    if (m.getType().hasFlag(MiscType.F_BA_EQUIPMENT)) {
                        report.addLine("BA-Type C3i", "MHQ2");
                        element.getSpecialAbilities().addSPA(MHQ, 2);
                    } else {
                        report.addLine("", "MHQ2.5");
                        element.getSpecialAbilities().addSPA(MHQ, 2.5);
                    }
                }
            } else if (m.getType().hasFlag(MiscType.F_CASE) && !element.isType(AF, CF, CI, BA, PM)) {
                report.addLine("CASE", "CASE");
                element.getSpecialAbilities().addSPA(CASE);
            } else if (m.getType().hasFlag(MiscType.F_CASEP) && !element.isType(AF, CF, CI, BA, PM)) {
                report.addLine("P-CASE", "CASEP");
                element.getSpecialAbilities().addSPA(CASEP);
            } else if (m.getType().hasFlag(MiscType.F_CASEII) && !element.isType(AF, CF, CI, BA, PM)) {
                report.addLine("CASE II", "CASEII");
                element.getSpecialAbilities().addSPA(CASEII);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                report.addLine("Drone Op. Sys.", "DRO");
                element.getSpecialAbilities().addSPA(DRO);
            } else if (m.getType().hasFlag(MiscType.F_SRCS)
                    || m.getType().hasFlag(MiscType.F_SASRCS)
                    || m.getType().hasFlag(MiscType.F_CASPAR)
                    || m.getType().hasFlag(MiscType.F_CASPARII)) {
                report.addLine("SRCS or Caspar", "RBT");
                element.getSpecialAbilities().addSPA(RBT);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_CARRIER_CONTROL)) {
                report.addLine("Drone Carrier C.", "DCC" + (int) m.getSize());
                element.getSpecialAbilities().addSPA(DCC, (int) m.getSize());
            } else if (m.getType().hasFlag(MiscType.F_REMOTE_DRONE_COMMAND_CONSOLE)) {
                report.addLine("Remote Drone C. C.", "DCC1");
                element.getSpecialAbilities().addSPA(DCC, 1);
            } else if (m.getType().hasFlag(MiscType.F_EJECTION_SEAT)) {
                report.addLine("Ejection Seat", "ES");
                element.getSpecialAbilities().addSPA(ES);
            } else if (m.getType().hasFlag(MiscType.F_BULLDOZER)) {
                report.addLine("Bulldozer", "ENG");
                element.getSpecialAbilities().addSPA(ENG);
            } else if (m.getType().hasFlag(MiscType.F_HAND_WEAPON)) {
                report.addLine("Melee Weapon", "MEL");
                element.getSpecialAbilities().addSPA(MEL);
            } else if (m.getType().getInternalName().equals("ProtoQuadMeleeSystem")) {
                report.addLine("Quad PM Melee Sys.", "MEL");
                element.getSpecialAbilities().addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_TALON)) {
                report.addLine("Talons", "MEL");
                element.getSpecialAbilities().addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_CLUB)) {
                report.addLine("Melee Weapon", "MEL");
                element.getSpecialAbilities().addSPA(MEL);
                if ((m.getType().getSubType() &
                        (MiscType.S_BACKHOE | MiscType.S_PILE_DRIVER
                                | MiscType.S_MINING_DRILL | MiscType.S_ROCK_CUTTER
                                | MiscType.S_WRECKING_BALL)) != 0) {
                    report.addLine("builder-type", "ENG");
                    element.getSpecialAbilities().addSPA(ENG);
                } else if ((m.getType().getSubType() &
                        (MiscType.S_DUAL_SAW | MiscType.S_CHAINSAW
                                | MiscType.S_BUZZSAW | MiscType.S_RETRACTABLE_BLADE)) != 0) {
                    report.addLine("saw-type", "SAW");
                    element.getSpecialAbilities().addSPA(SAW);
                }
            } else if (m.getType().hasFlag(MiscType.F_SPIKES)) {
                report.addLine("Spikes", "MEL");
                element.getSpecialAbilities().addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_FIRE_RESISTANT)) {
                report.addLine("Fire Resistant", "FR");
                element.getSpecialAbilities().addSPA(FR);
            } else if (m.getType().hasFlag(MiscType.F_MOBILE_HPG)) {
                report.addLine("Mobile HPG", "HPG");
                element.getSpecialAbilities().addSPA(HPG);
            } else if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                report.addLine("Comm. Equip.", "MHQ" + (int) m.getTonnage());
                element.getSpecialAbilities().addSPA(MHQ, (int) m.getTonnage());
                if (m.getTonnage() >= entity.getWeight() / 20.0) {
                    report.addLine("+ High relative weight", "RCN");
                    element.getSpecialAbilities().addSPA(RCN);
                }
            } else if (m.getType().hasFlag(MiscType.F_SENSOR_DISPENSER)) {
                report.addLine("Sensor Dispenser", "RSD1");
                report.addLine("", "RCN");
                element.getSpecialAbilities().addSPA(RSD, 1);
                element.getSpecialAbilities().addSPA(RCN);
            } else if (m.getType().hasFlag(MiscType.F_LOOKDOWN_RADAR)
                    || m.getType().hasFlag(MiscType.F_RECON_CAMERA)
                    || m.getType().hasFlag(MiscType.F_HIRES_IMAGER)
                    || m.getType().hasFlag(MiscType.F_HYPERSPECTRAL_IMAGER)
                    || m.getType().hasFlag(MiscType.F_INFRARED_IMAGER)) {
                report.addLine("Camera, Radar, Imager", "RCN");
                element.getSpecialAbilities().addSPA(RCN);
            } else if (m.getType().hasFlag(MiscType.F_SEARCHLIGHT)) {
                report.addLine("Searchlight", "SRCH");
                element.getSpecialAbilities().addSPA(SRCH);
            } else if (m.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                report.addLine("Radical Heatsinks", "RHS");
                element.getSpecialAbilities().addSPA(RHS);
            } else if (m.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                report.addLine("Emergency Cool. Sys.", "ECS");
                element.getSpecialAbilities().addSPA(ECS);
            } else if (m.getType().hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)) {
                report.addLine("Viral Jammer Decoy", "DJ");
                element.getSpecialAbilities().addSPA(DJ);
            } else if (m.getType().hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)) {
                report.addLine("Viral Jammer Homing", "HJ");
                element.getSpecialAbilities().addSPA(HJ);
            } else if (m.getType().hasFlag(MiscType.F_CARGO)) {
                report.addLine("Cargo Space", "CT" + (int) m.getTonnage());
                element.getSpecialAbilities().addSPA(CT, m.getTonnage());
            }

            if (m.getType().hasFlag(MiscType.F_SPACE_MINE_DISPENSER) && (entity instanceof Aero)) {
                report.addLine("Space Mine Disp.", "MDS2");
                element.getSpecialAbilities().addSPA(MDS, 2);
            }

            if (entity instanceof Mech) {
                if (m.getType().hasFlag(MiscType.F_HARJEL)) {
                    report.addLine("Harjel", "BHJ");
                    element.getSpecialAbilities().addSPA(BHJ);
                } else if (m.getType().hasFlag(MiscType.F_HARJEL_II)) {
                    report.addLine("Harjel II", "BHJ2");
                    element.getSpecialAbilities().addSPA(BHJ2);
                } else if (m.getType().hasFlag(MiscType.F_HARJEL_III)) {
                    report.addLine("Harjel III", "BHJ3");
                    element.getSpecialAbilities().addSPA(BHJ3);
                } else if (((MiscType)m.getType()).isShield()) {
                    report.addLine("Shield", "SHLD");
                    element.getSpecialAbilities().addSPA(SHLD);
                } else if (m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                    report.addLine("Indust. TSM", "ITSM");
                    element.getSpecialAbilities().addSPA(ITSM);
                } else if (m.getType().hasFlag(MiscType.F_TSM)) {
                    report.addLine("TSM", "TSM");
                    element.getSpecialAbilities().addSPA(TSM);
                } else if (m.getType().hasFlag(MiscType.F_VOIDSIG)) {
                    report.addLine("Void Sig. Sys.", "MAS");
                    element.getSpecialAbilities().addSPA(MAS);
                } else if (((Mech) entity).isIndustrial() && m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                    report.addLine("IM with Sealing", "SEAL");
                    element.getSpecialAbilities().addSPA(SEAL);
                    if (entity.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE) {
                        report.addLine("+ no ICE", "SOA");
                        element.getSpecialAbilities().addSPA(SOA);
                    }
                } else if (m.getType().hasFlag(MiscType.F_NULLSIG)
                        || m.getType().hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                    report.addLine("Null Sig. or Chameleon S.", "STL");
                    report.addLine("", "ECM");
                    element.getSpecialAbilities().addSPA(STL);
                    element.getSpecialAbilities().addSPA(ECM);
                } else if (m.getType().hasFlag(MiscType.F_UMU)) {
                    report.addLine("UMU", "UMU");
                    element.getSpecialAbilities().addSPA(UMU);
                } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_INTERFACE) {
                    report.addLine("Cockpit Interface", "DN");
                    element.getSpecialAbilities().addSPA(DN);
                } else if (m.getType().hasFlag(MiscType.F_EW_EQUIPMENT)) {
                    report.addLine("Mek with EW Equipment", "ECM");
                    element.getSpecialAbilities().addSPA(ECM);
                }
            }

            if (entity instanceof Protomech) {
                if (m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    if (entity.getWeight() < 10) {
                        report.addLine("Light PM with clamps", "MCS");
                        element.getSpecialAbilities().addSPA(MCS);
                    } else {
                        report.addLine("Heavy PM with clamps", "UCS");
                        element.getSpecialAbilities().addSPA(UCS);
                    }
                }
            }

            if (entity instanceof Tank) {
                if (m.getType().hasFlag(MiscType.F_ADVANCED_FIRECONTROL)) {
                    report.addLine("Advanced Fire Control", "AFC");
                    element.getSpecialAbilities().addSPA(AFC);
                } else if (m.getType().hasFlag(MiscType.F_BASIC_FIRECONTROL)) {
                    report.addLine("Basic Fire Control", "BFC");
                    element.getSpecialAbilities().addSPA(BFC);
                } else if (m.getType().hasFlag(MiscType.F_AMPHIBIOUS) || m.getType().hasFlag(MiscType.F_FULLY_AMPHIBIOUS)
                        || m.getType().hasFlag(MiscType.F_LIMITED_AMPHIBIOUS)) {
                    report.addLine("Amphib. Chassis Mod", "AMP");
                    element.getSpecialAbilities().addSPA(AMP);
                } else if (m.getType().hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)) {
                    report.addLine("Armored Motive Sys.", "ARS");
                    element.getSpecialAbilities().addSPA(ARS);
                } else if (m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                    report.addLine("Env. Sealing", "SEAL");
                    element.getSpecialAbilities().addSPA(SEAL);
                    if (entity.hasEngine() && entity.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE
                            && entity.getEngine().getEngineType() != Engine.STEAM) {
                        report.addLine("+ no ICE/Steam", "SOA");
                        element.getSpecialAbilities().addSPA(SOA);
                    }
                } else if (m.getType().hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
                    report.addLine("Vehicle Mine Disp.", "MDS2");
                    element.getSpecialAbilities().addSPA(MDS, 2);
                } else if (m.getType().hasFlag(MiscType.F_MINESWEEPER)) {
                    report.addLine("Minesweeper", "MSW");
                    element.getSpecialAbilities().addSPA(MSW);
                } else if (m.getType().hasFlag(MiscType.F_MASH)) {
                    report.addLine("M.A.S.H.", "MASH" + (int) m.getSize());
                    element.getSpecialAbilities().addSPA(MASH, (int) m.getSize());
                } else if (m.getType().hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                    report.addLine("Mobile Field Base", "MFB");
                    element.getSpecialAbilities().addSPA(MFB);
                } else if (m.getType().hasFlag(MiscType.F_OFF_ROAD)) {
                    report.addLine("Off-road enabled", "ORO");
                    element.getSpecialAbilities().addSPA(ORO);
                } else if (m.getType().hasFlag(MiscType.F_DUNE_BUGGY)) {
                    report.addLine("Dune buggy", "DUN");
                    element.getSpecialAbilities().addSPA(DUN);
                } else if (m.getType().hasFlag(MiscType.F_TRACTOR_MODIFICATION)
                        || m.getType().hasFlag(MiscType.F_TRAILER_MODIFICATION)
                        || m.getType().hasFlag(MiscType.F_HITCH)) {
                    report.addLine("Tractor or Trailer Mod.", "HTC");
                    element.getSpecialAbilities().addSPA(HTC);
                } else if (m.getType().hasFlag(MiscType.F_COMMAND_CONSOLE)) {
                    report.addLine("Command Console", "MHQ1");
                    element.getSpecialAbilities().addSPA(MHQ, 1);
                }
            }

            if (entity instanceof BattleArmor) {
                if (m.getType().hasFlag(MiscType.F_VISUAL_CAMO)
                        && !m.getType().getName().equals(BattleArmor.MIMETIC_ARMOR)) {
                    report.addLine("Visual Camo, not Mimetic", "LMAS");
                    element.getSpecialAbilities().addSPA(LMAS);
                } else if (m.getType().hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
                    report.addLine("Vehicle Mine Disp.", "MDS1");
                    element.getSpecialAbilities().addSPA(MDS, 1);
                } else if (m.getType().hasFlag(MiscType.F_TOOLS)
                        && (m.getType().getSubType() & MiscType.S_MINESWEEPER) == MiscType.S_MINESWEEPER) {
                    report.addLine("Minesweeper", "MSW");
                    element.getSpecialAbilities().addSPA(MSW);
                } else if (m.getType().hasFlag(MiscType.F_SPACE_ADAPTATION)) {
                    report.addLine("BA Space Adapt.", "SOA");
                    element.getSpecialAbilities().addSPA(SOA);
                } else if (m.getType().hasFlag(MiscType.F_PARAFOIL)) {
                    report.addLine("Parafoil", "PARA");
                    element.getSpecialAbilities().addSPA(PARA);
                } else if (m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    report.addLine("BA Mag. Clamp", "XMEC");
                    element.getSpecialAbilities().addSPA(XMEC);
                }
            }
        }

        // TODO: why doesnt this work?
        if (element.hasQuirk(OptionsConstants.QUIRK_POS_TRAILER_HITCH)) {
            element.getSpecialAbilities().addSPA(HTC);
        }

        if (entity.isOmni() && ((entity instanceof Mech) || (entity instanceof Tank))) {
            report.addLine("Command Console", "MHQ1");
            element.getSpecialAbilities().addSPA(OMNI);
        }

        if (entity.getBARRating(0) >= 1 && entity.getBARRating(0) <= 9) {
            report.addLine("BAR Rating " + entity.getBARRating(0), "BAR");
            element.getSpecialAbilities().addSPA(BAR);
        }

        //TODO: Variable Range targeting is not implemented
        if (!entity.hasPatchworkArmor()) {
            switch (entity.getArmorType(0)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    report.addLine("Commercial Armor", "BAR");
                    element.getSpecialAbilities().addSPA(BAR);
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_HARDENED:
                    report.addLine("Ferro-Lam./Hardened Armor", "CR");
                    element.getSpecialAbilities().addSPA(CR);
                    break;
                case EquipmentType.T_ARMOR_STEALTH:
                case EquipmentType.T_ARMOR_STEALTH_VEHICLE:
                case EquipmentType.T_ARMOR_BA_STEALTH:
                case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
                case EquipmentType.T_ARMOR_BA_STEALTH_IMP:
                case EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE:
                    report.addLine("Stealth Armor", "STL");
                    element.getSpecialAbilities().addSPA(STL);
                    break;
                case EquipmentType.T_ARMOR_BA_MIMETIC:
                    report.addLine("Mimetic Armor", "MAS");
                    element.getSpecialAbilities().addSPA(MAS);
                    break;
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    report.addLine("Anti-Pen. Abl. Armor", "ABA");
                    element.getSpecialAbilities().addSPA(ABA);
                    break;
                case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                    report.addLine("Ballistic Reinf. Armor", "BRA");
                    element.getSpecialAbilities().addSPA(BRA);
                    break;
                case EquipmentType.T_ARMOR_BA_FIRE_RESIST:
                case EquipmentType.T_ARMOR_HEAT_DISSIPATING:
                    report.addLine("Fire Res./Ht. Diss. Armor", "FR");
                    element.getSpecialAbilities().addSPA(FR);
                    break;
                case EquipmentType.T_ARMOR_IMPACT_RESISTANT:
                    report.addLine("Impact Res. Armor", "IRA");
                    element.getSpecialAbilities().addSPA(IRA);
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                    report.addLine("Reactive Armor", "RCA");
                    element.getSpecialAbilities().addSPA(RCA);
                    break;
                case EquipmentType.T_ARMOR_REFLECTIVE:
                    report.addLine("Reflective Armor", "RFA");
                    element.getSpecialAbilities().addSPA(RFA);
                    break;
            }
        }

        if (element.usesArcs()) {
            if (!hasExplosiveArcComponent[0]) {
                element.getFrontArc().getSpecials().addSPA(ENE);
            }
            if (!hasExplosiveArcComponent[1]) {
                element.getLeftArc().getSpecials().addSPA(ENE);
            }
            if (!hasExplosiveArcComponent[2]) {
                element.getRightArc().getSpecials().addSPA(ENE);
            }
            if (!hasExplosiveArcComponent[3]) {
                element.getRearArc().getSpecials().addSPA(ENE);
            }
        } else {
            if (!element.isInfantry()) {
                if (!hasExplosiveComponent) {
                    report.addLine("No Explosive Comp.", "ENE");
                    element.getSpecialAbilities().addSPA(ENE);
                } else if (entity.isClan() && element.isType(BM, IM, SV, CV, MS)) {
                    report.addLine("Explosive Comp.", "CASE");
                    element.getSpecialAbilities().addSPA(CASE);
                }
            }
        }

        if (entity.getAmmo().stream().map(m -> (AmmoType)m.getType())
                .anyMatch(at -> at.hasFlag(AmmoType.F_TELE_MISSILE))) {
            report.addLine("Tele-Missile", "TELE");
            element.getSpecialAbilities().addSPA(TELE);
        }

        if (entity.hasEngine() && !element.isLargeAerospace()) {
            if (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) {
                report.addLine("Combustion Engine", "EE");
                element.getSpecialAbilities().addSPA(EE);
            } else if (entity.getEngine().getEngineType() == Engine.FUEL_CELL) {
                report.addLine("Fuel Cell", "FC");
                element.getSpecialAbilities().addSPA(FC);
            }
        }

        for (Transporter t : entity.getTransports()) {
            if (t instanceof ASFBay) {
                element.getSpecialAbilities().addSPA(AT, (int)((ASFBay)t).getCapacity());
                element.getSpecialAbilities().addSPA(ATxD, ((ASFBay)t).getDoors());
                if (!element.usesArcs()) {
                    element.getSpecialAbilities().addSPA(MFB);
                }
            } else if (t instanceof CargoBay) {
                element.getSpecialAbilities().addSPA(CT, ((CargoBay) t).getCapacity());
                element.getSpecialAbilities().addSPA(CTxD, ((CargoBay) t).getDoors());
            } else if (t instanceof DockingCollar) {
                element.getSpecialAbilities().addSPA(DT, 1);
            } else if (t instanceof InfantryBay) {
                element.getSpecialAbilities().addSPA(IT, ((InfantryBay)t).getCapacity());
            } else if (t instanceof TroopSpace) {
                element.getSpecialAbilities().addSPA(IT, t.getUnused());
            } else if (t instanceof MechBay) {
                element.getSpecialAbilities().addSPA(MT, (int)((MechBay)t).getCapacity());
                element.getSpecialAbilities().addSPA(MTxD, ((MechBay)t).getDoors());
                if (!element.usesArcs()) {
                    element.getSpecialAbilities().addSPA(MFB);
                }
            } else if (t instanceof ProtomechBay) {
                element.getSpecialAbilities().addSPA(PT, (int)((ProtomechBay)t).getCapacity());
                element.getSpecialAbilities().addSPA(PTxD, ((ProtomechBay)t).getDoors());
                if (!element.usesArcs()) {
                    element.getSpecialAbilities().addSPA(MFB);
                }
            } else if (t instanceof SmallCraftBay) {
                element.getSpecialAbilities().addSPA(ST, (int)((SmallCraftBay)t).getCapacity());
                element.getSpecialAbilities().addSPA(STxD, ((SmallCraftBay)t).getDoors());
                if (!element.usesArcs()) {
                    element.getSpecialAbilities().addSPA(MFB);
                }
            } else if (t instanceof LightVehicleBay) {
                element.getSpecialAbilities().addSPA(VTM, (int)((LightVehicleBay)t).getCapacity());
                element.getSpecialAbilities().addSPA(VTMxD, ((LightVehicleBay)t).getDoors());
                if (!element.usesArcs()) {
                    element.getSpecialAbilities().addSPA(MFB);
                }
            } else if (t instanceof HeavyVehicleBay) {
                element.getSpecialAbilities().addSPA(VTH, (int)((HeavyVehicleBay)t).getCapacity());
                element.getSpecialAbilities().addSPA(VTHxD, ((HeavyVehicleBay)t).getDoors());
                if (!element.usesArcs()) {
                    element.getSpecialAbilities().addSPA(MFB);
                }
            }
        }

        topLoop: for (int location = 0; location < entity.locations(); location++) {
            for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
                CriticalSlot crit = entity.getCritical(location, slot);
                if (null != crit) {
                    if (crit.isArmored()) {
                        element.getSpecialAbilities().addSPA(ARM);
                        break topLoop;
                    } else if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted mount = crit.getMount();
                        if (mount.isArmored()) {
                            element.getSpecialAbilities().addSPA(ARM);
                            break topLoop;
                        }
                    }
                }
            }
        }

        if (entity instanceof Aero) {
            if (((Aero) entity).getCockpitType() == Aero.COCKPIT_COMMAND_CONSOLE) {
                report.addLine("Command Console", "MHQ1");
                element.getSpecialAbilities().addSPA(MHQ, 1);
            }
            if (entity.hasWorkingMisc(MiscType.F_COMMAND_CONSOLE)) {
                report.addLine("Command Console", "MHQ1");
                element.getSpecialAbilities().addSPA(MHQ, 1);
            }
            if (entity instanceof FixedWingSupport)
                if (((FixedWingSupport) entity).getMaxBombPoints() > 0) {
                    report.addLine("Fixed Wing Sup. Bombs",
                            "Bomb Points:" + ((FixedWingSupport) entity).getMaxBombPoints(),
                            "BOMB");
                    element.getSpecialAbilities().addSPA(BOMB,
                            ASConverter.roundUp(0.2d * ((FixedWingSupport) entity).getMaxBombPoints()));
                } else if (entity.isFighter()) {
                    report.addLine("Fighter Bombs", "BOMB");
                    element.getSpecialAbilities().addSPA(BOMB, element.getSize());
                }
            if (!(entity instanceof ConvFighter)) {
                report.addLine("Space Capable Aero", "SPC");
                element.getSpecialAbilities().addSPA(SPC);
            }
            if (((Aero) entity).isVSTOL()) {
                report.addLine("VSTOL gear or capable", "VSTOL");
                element.getSpecialAbilities().addSPA(VSTOL);
            }
            if (element.isType(AF)) {
                report.addLine("Aerospace Fighter Fuel", "Fuel: " + ((Aero) entity).getFuel(), "FUEL");
                element.getSpecialAbilities().addSPA(FUEL, (int) Math.round(0.05 * ((Aero) entity).getFuel()));
            }
        }

        if (entity instanceof Infantry) {
            element.getSpecialAbilities().addSPA(CAR, (int)Math.ceil(entity.getWeight()));
            if (entity.getMovementMode().equals(EntityMovementMode.INF_UMU)) {
                report.addLine("UMU Gear", "UMU");
                element.getSpecialAbilities().addSPA(UMU);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.FIRE_ENGINEERS)) {
                report.addLine("Fire Engineers", "FF");
                element.getSpecialAbilities().addSPA(FF);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.MINE_ENGINEERS)) {
                report.addLine("Mine Engineers", "MSW");
                element.getSpecialAbilities().addSPA(MSW);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.MOUNTAIN_TROOPS)) {
                report.addLine("Mountain Troops", "MTN");
                element.getSpecialAbilities().addSPA(MTN);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.PARATROOPS)) {
                report.addLine("Paratroopers", "PARA");
                element.getSpecialAbilities().addSPA(PARA);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.SCUBA)) {
                report.addLine("Scuba Gear", "UMU");
                element.getSpecialAbilities().addSPA(UMU);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.TRENCH_ENGINEERS)) {
                report.addLine("Trench Engineers", "TRN");
                element.getSpecialAbilities().addSPA(TRN);
            }
            if (entity.hasAbility("tsm_implant")) {
                report.addLine("TSM implants", "TSI");
                element.getSpecialAbilities().addSPA(TSI);
            }
            if ((entity instanceof BattleArmor) && ((BattleArmor) entity).canDoMechanizedBA()) {
                report.addLine("BA / Mech.", "MEC");
                element.getSpecialAbilities().addSPA(MEC);
            }
        }

        if (entity instanceof Mech) {
            if (((Mech) entity).getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
                report.addLine("Command Console", "MHQ1");
                element.getSpecialAbilities().addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE) {
                report.addLine("SH Command Console", "MHQ1");
                element.getSpecialAbilities().addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_SMALL_COMMAND_CONSOLE) {
                report.addLine("Sm. Command Console", "MHQ1");
                element.getSpecialAbilities().addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_VRRP) {
                report.addLine("VRRP Cockpi", "VR1");
                element.getSpecialAbilities().addSPA(VR, 1);
            }
            if (((Mech) entity).isIndustrial()) {
                if (((Mech) entity).getCockpitType() == Mech.COCKPIT_STANDARD) {
                    report.addLine("IM Standard Cockpit", "AFC");
                    element.getSpecialAbilities().addSPA(AFC);
                } else {
                    report.addLine("Industrial Cockpit", "BFC");
                    element.getSpecialAbilities().addSPA(BFC);
                }
            } else {
                report.addLine("BattleMek", "SOA, SRCH");
                element.getSpecialAbilities().addSPA(SOA);
                element.getSpecialAbilities().addSPA(SRCH);
            }
        }

        if (entity instanceof Protomech) {
            report.addLine("ProtoMek", "SOA");
            element.getSpecialAbilities().addSPA(SOA);
            if (entity.getMovementMode().equals(EntityMovementMode.WIGE)) {
                report.addLine("Glider ProtoMek", "GLD");
                element.getSpecialAbilities().addSPA(GLD);
            }
        }

        if (entity instanceof Tank && !entity.isSupportVehicle()) {
            report.addLine("Combat Vehicle", "SRCH");
            element.getSpecialAbilities().addSPA(SRCH);
        }

        if (element.isType(SC, DS, DA)) {
            if (element.getSize() == 1) {
                report.addLine("Size 1 SC/Dropship", "LG");
                element.getSpecialAbilities().addSPA(LG);
            } else if (element.getSize() == 2) {
                report.addLine("Size 2 SC/Dropship", "VLG");
                element.getSpecialAbilities().addSPA(VLG);
            } else {
                report.addLine("Size 3 SC/Dropship", "SLG");
                element.getSpecialAbilities().addSPA(SLG);
            }
        }

        if (element.getMovementModes().contains("j") && element.getMovementModes().contains("")) {
            int jumpTMM = ASConverter.tmmForMovement(element.getMovement("j"));
            int walkTMM = ASConverter.tmmForMovement(element.getMovement(""));
            if (jumpTMM > walkTMM) {
                report.addLine("Strong Jumper", "JMPS");
                element.getSpecialAbilities().addSPA(JMPS, jumpTMM - walkTMM);
            } else if (jumpTMM < walkTMM) {
                report.addLine("Weak Jumper", "JMPW");
                element.getSpecialAbilities().addSPA(JMPW, walkTMM - jumpTMM);
            }
        }

        if (element.getMovementModes().contains("s") && element.getMovementModes().contains("")) {
            int umuTMM = ASConverter.tmmForMovement(element.getMovement("s"));
            int walkTMM = ASConverter.tmmForMovement(element.getMovement(""));
            if (umuTMM > walkTMM) {
                report.addLine("Strong UMU", "SUBS");
                element.getSpecialAbilities().addSPA(SUBS, umuTMM - walkTMM);
            } else if (umuTMM < walkTMM) {
                report.addLine("Weak UMU", "SUBW");
                element.getSpecialAbilities().addSPA(SUBW, walkTMM - umuTMM);
            }
        }

        if (element.isType(CF) || (entity instanceof VTOL) || (entity instanceof FixedWingSupport)) {
            report.addLine("CF, VTOL or Fixed Wing Sup.", "ATMO");
            element.getSpecialAbilities().addSPA(ATMO);
        }

        if (entity instanceof LandAirMech) {
            LandAirMech lam = (LandAirMech) entity;
            double bombs = entity.countWorkingMisc(MiscType.F_BOMB_BAY);
            int bombValue = ASConverter.roundUp(bombs / 5);
            if (bombValue > 0) {
                report.addLine("LAM with Bombs", "BOMB");
                element.getSpecialAbilities().addSPA(BOMB, bombValue);
            }
            report.addLine("LAM Fuel", "Fuel: " + lam.getFuel(), "FUEL");
            element.getSpecialAbilities().addSPA(FUEL, (int) Math.round(0.05 * lam.getFuel()));
            var lamMoves = new HashMap<String, Integer>();
            if (lam.getLAMType() == LandAirMech.LAM_BIMODAL) {
                report.addLine("Bimodal Movement", "BIM");
                lamMoves.put("a", lam.getCurrentThrust());
                element.getSpecialAbilities().addBimSPA(lamMoves);
            } else {
                report.addLine("LAM Movement", "LAM");
                lamMoves.put("g", lam.getAirMechCruiseMP(false, false) * 2);
                lamMoves.put("a", lam.getCurrentThrust());
                element.getSpecialAbilities().addLamSPA(lamMoves);
            }
            element.getMovement().putAll(lamMoves);
        }

        if (entity instanceof QuadVee) {
            report.addLine("QuadVee", "QV");
            element.getSpecialAbilities().addSPA(QV);
        }

        if (element.hasAutoSeal()) {
            report.addLine("Sealed", "SEAL");
            element.getSpecialAbilities().addSPA(SEAL);
        }

        if (entity instanceof Jumpship) {
            if (((Jumpship) entity).getDriveCoreType() != Jumpship.DRIVE_CORE_NONE) {
                report.addLine("KF Drive", "KF");
                element.getSpecialAbilities().addSPA(KF);
            }
            if (((Jumpship) entity).hasLF()) {
                report.addLine("Lithium-Fusion Battery", "LF");
                element.getSpecialAbilities().addSPA(LF);
            }
            if (entity.getNCrew() >= 60) {
                report.addLine("Sufficient Crew", "Crew: " + entity.getNCrew(), "CRW");
                element.getSpecialAbilities().addSPA(CRW, (int) Math.round(entity.getNCrew() / 120.0));
            }
        }

        if (entity instanceof Dropship) {
            if (entity.getNCrew() >= 30) {
                report.addLine("Sufficient Crew", "Crew: " + entity.getNCrew(), "CRW");
                element.getSpecialAbilities().addSPA(CRW, (int) Math.round(entity.getNCrew() / 60.0));
            }
        }
    }

    /** Returns true when the given Mounted blocks ENE. */
    private static boolean isExplosive(Mounted m) {
        // LAM Bomb Bays are explosive
        if ((m.getType() instanceof MiscType) && m.getType().hasFlag(MiscType.F_BOMB_BAY)) {
            return true;
        }
        // Oneshot weapons internally have normal ammo allocated to them which must
        // be disqualified as explosive; such ammo has no location
        return m.getType().isExplosive(null) && (m.getExplosionDamage() > 0)
                && (m.getLocation() != Entity.LOC_NONE);
    }

    static void finalizeSpecials(AlphaStrikeElement element) {
        // For MHQ, the values may contain decimals, but the the final MHQ value is rounded down to an int.
        if (element.getSUA(MHQ) instanceof Double) {
            double mhqValue = (double) element.getSUA(MHQ);
            element.getSpecialAbilities().replaceSPA(MHQ, (int) mhqValue);
        }

        // Cannot have both CASEII and CASE
        if (element.hasSUA(CASEII)) {
            element.getSpecialAbilities().removeSPA(CASE);
        }

        // Implicit rule: XMEC overrides MEC
        if (element.hasSUA(XMEC)) {
            element.getSpecialAbilities().removeSPA(MEC);
        }

        // Implicit rule: AECM overrides ECM
        if (element.hasSUA(AECM)) {
            element.getSpecialAbilities().removeSPA(ECM);
        }

        // Some SUAs are accompanied by RCN
        if (element.hasAnySUAOf(PRB, LPRB, NOVA, BH, WAT)) {
            element.getSpecialAbilities().addSPA(RCN);
        }

        // IT value may be decimal but replace it with an integer value if it is integer
        if (element.hasSUA(IT) && (element.getSUA(IT) instanceof Double)) {
            double ctValue = (double) element.getSUA(IT);
            if ((int) ctValue == ctValue) {
                element.getSpecialAbilities().replaceSPA(IT, (int) ctValue);
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
                element.getSpecialAbilities().addSPA(CK, (int) Math.round(ctValue / 1000));
                element.getSpecialAbilities().addSPA(CKxD, (int) element.getSUA(CTxD));
                element.getSpecialAbilities().removeSPA(CT);
                element.getSpecialAbilities().removeSPA(CTxD);
            } else {
                element.getSpecialAbilities().replaceSPA(CT, (int) Math.round(ctValue));
            }
        }

        // For CI, replace the placeholder HT with the S damage value
        if (element.isType(CI) && element.hasSUA(HT)) {
            int dmg = element.getStandardDamage().S.damage;
            if (dmg > 0) {
                element.getSpecialAbilities().addSPA(HT, ASDamageVector.createNormRndDmg(Math.min(2, dmg), 0, 0));
            } else {
                element.getSpecialAbilities().removeSPA(HT);
            }
        }

        // Round up fractional PNT values in arcs
        if (element.getFrontArc().hasSPA(PNT)) {
            double pntValue = (double) element.getFrontArc().getSPA(PNT);
            element.getFrontArc().getSpecials().replaceSPA(PNT, ASConverter.roundUp(pntValue));
        }
        if (element.getLeftArc().hasSPA(PNT)) {
            double pntValue = (double) element.getLeftArc().getSPA(PNT);
            element.getLeftArc().getSpecials().replaceSPA(PNT, ASConverter.roundUp(pntValue));
        }
        if (element.getRightArc().hasSPA(PNT)) {
            double pntValue = (double) element.getRightArc().getSPA(PNT);
            element.getRightArc().getSpecials().replaceSPA(PNT, ASConverter.roundUp(pntValue));
        }
        if (element.getRearArc().hasSPA(PNT)) {
            double pntValue = (double) element.getRearArc().getSPA(PNT);
            element.getRearArc().getSpecials().replaceSPA(PNT, ASConverter.roundUp(pntValue));
        }

    }

    // Make non-instantiable
    private ASSpecialAbilityConverter() { }

}
