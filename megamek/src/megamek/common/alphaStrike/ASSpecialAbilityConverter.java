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
package megamek.common.alphaStrike;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.options.OptionsConstants;

import java.util.HashMap;

import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSPA.*;

final class ASSpecialAbilityConverter {

    static void convertSpecialUnitAbilities(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity;
        CalculationReport report = conversionData.conversionReport;
        AlphaStrikeElement element = conversionData.element;

        boolean hasExplosiveComponent = false;
        for (Mounted m : entity.getEquipment()) {

            if (isExplosive(m)) {
                hasExplosiveComponent = true;
            }

            if (!(m.getType() instanceof MiscType)) {
                continue;
            }

            if (m.getType().getInternalName().equals(Sensor.BAP)
                    || m.getType().getInternalName().equals(Sensor.BAPP)
                    || m.getType().getInternalName().equals(Sensor.CLAN_AP)) {
                element.getSpecialAbs().addSPA(PRB);
            } else if (m.getType().getInternalName().equals(Sensor.LIGHT_AP)
                    || m.getType().getInternalName().equals(Sensor.ISBALIGHT_AP)
                    || m.getType().getInternalName().equals(Sensor.EW_EQUIPMENT)) {
                element.getSpecialAbs().addSPA(LPRB);
            } else if (m.getType().getInternalName().equals(Sensor.BLOODHOUND)) {
                element.getSpecialAbs().addSPA(BH);
            } else if (m.getType().getInternalName().equals(Sensor.WATCHDOG)) {
                element.getSpecialAbs().addSPA(LPRB);
                element.getSpecialAbs().addSPA(ECM);
                element.getSpecialAbs().addSPA(WAT);
            } else if (m.getType().getInternalName().equals(Sensor.NOVA)) {
                element.getSpecialAbs().addSPA(NOVA);
                element.getSpecialAbs().addSPA(PRB);
                element.getSpecialAbs().addSPA(ECM);
                element.getSpecialAbs().addSPA(MHQ, 1.5);
            } else if (m.getType().hasFlag(MiscType.F_ECM)) {
                if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                    element.getSpecialAbs().addSPA(AECM);
                } else if (m.getType().hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                    element.getSpecialAbs().addSPA(LECM);
                } else {
                    element.getSpecialAbs().addSPA(ECM);
                }
            } else if (m.getType().hasFlag(MiscType.F_BOOBY_TRAP) && !element.isAnyTypeOf(PM, CI, BA)) {
                element.getSpecialAbs().addSPA(BT);
            } else if (m.getType().hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || m.getType().hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || m.getType().hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)) {
                element.getSpecialAbs().addSPA(BRID);
            } else if (m.getType().hasFlag(MiscType.F_C3S)) {
                element.getSpecialAbs().addSPA(C3S);
                element.getSpecialAbs().addSPA(MHQ, 1);
                if (m.getType().hasFlag(MiscType.F_C3EM)) {
                    element.getSpecialAbs().addSPA(C3EM, 1);
                }
            } else if (m.getType().hasFlag(MiscType.F_C3SBS)) {
                element.getSpecialAbs().addSPA(C3BSS, 1);
                element.getSpecialAbs().addSPA(MHQ, 2);
            } else if (m.getType().hasFlag(MiscType.F_C3I)) {
                if ((entity.getEntityType() & Entity.ETYPE_AERO) == Entity.ETYPE_AERO) {
                    element.getSpecialAbs().addSPA(NC3);
                } else {
                    element.getSpecialAbs().addSPA(C3I);
                    if (m.getType().hasFlag(MiscType.F_BA_EQUIPMENT)) {
                        element.getSpecialAbs().addSPA(MHQ, 2);
                    } else {
                        element.getSpecialAbs().addSPA(MHQ, 2.5);
                    }
                }
            } else if (m.getType().hasFlag(MiscType.F_CASE) && !element.isAnyTypeOf(AF, CF, CI, BA, PM)) {
                element.getSpecialAbs().addSPA(CASE);
            } else if (m.getType().hasFlag(MiscType.F_CASEP) && !element.isAnyTypeOf(AF, CF, CI, BA, PM)) {
                element.getSpecialAbs().addSPA(CASEP);
            } else if (m.getType().hasFlag(MiscType.F_CASEII) && !element.isAnyTypeOf(AF, CF, CI, BA, PM)) {
                element.getSpecialAbs().addSPA(CASEII);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                element.getSpecialAbs().addSPA(DRO);
            } else if (m.getType().hasFlag(MiscType.F_SRCS)
                    || m.getType().hasFlag(MiscType.F_SASRCS)
                    || m.getType().hasFlag(MiscType.F_CASPAR)
                    || m.getType().hasFlag(MiscType.F_CASPARII)) {
                element.getSpecialAbs().addSPA(RBT);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_CARRIER_CONTROL)) {
                element.getSpecialAbs().addSPA(DCC, (int) m.getSize());
            } else if (m.getType().hasFlag(MiscType.F_REMOTE_DRONE_COMMAND_CONSOLE)) {
                element.getSpecialAbs().addSPA(DCC, 1);
            } else if (m.getType().hasFlag(MiscType.F_EJECTION_SEAT)) {
                element.getSpecialAbs().addSPA(ES);
            } else if (m.getType().hasFlag(MiscType.F_BULLDOZER)) {
                element.getSpecialAbs().addSPA(ENG);
            } else if (m.getType().hasFlag(MiscType.F_HAND_WEAPON)) {
                element.getSpecialAbs().addSPA(MEL);
            } else if (m.getType().getInternalName().equals("ProtoQuadMeleeSystem")) {
                element.getSpecialAbs().addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_TALON)) {
                element.getSpecialAbs().addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_CLUB)) {
                element.getSpecialAbs().addSPA(MEL);
                if ((m.getType().getSubType() &
                        (MiscType.S_BACKHOE | MiscType.S_PILE_DRIVER
                                | MiscType.S_MINING_DRILL | MiscType.S_ROCK_CUTTER
                                | MiscType.S_WRECKING_BALL)) != 0) {
                    element.getSpecialAbs().addSPA(ENG);
                } else if ((m.getType().getSubType() &
                        (MiscType.S_DUAL_SAW | MiscType.S_CHAINSAW
                                | MiscType.S_BUZZSAW | MiscType.S_RETRACTABLE_BLADE)) != 0) {
                    element.getSpecialAbs().addSPA(SAW);
                }
            } else if (m.getType().hasFlag(MiscType.F_SPIKES)) {
                element.getSpecialAbs().addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_FIRE_RESISTANT)) {
                element.getSpecialAbs().addSPA(FR);
            } else if (m.getType().hasFlag(MiscType.F_MOBILE_HPG)) {
                element.getSpecialAbs().addSPA(HPG);
            } else if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                element.getSpecialAbs().addSPA(MHQ, (int) m.getTonnage());
                if (m.getTonnage() >= entity.getWeight() / 20.0) {
                    element.getSpecialAbs().addSPA(RCN);
                }
            } else if (m.getType().hasFlag(MiscType.F_SENSOR_DISPENSER)) {
                element.getSpecialAbs().addSPA(RSD, 1);
                element.getSpecialAbs().addSPA(RCN);
            } else if (m.getType().hasFlag(MiscType.F_LOOKDOWN_RADAR)
                    || m.getType().hasFlag(MiscType.F_RECON_CAMERA)
                    || m.getType().hasFlag(MiscType.F_HIRES_IMAGER)
                    || m.getType().hasFlag(MiscType.F_HYPERSPECTRAL_IMAGER)
                    || m.getType().hasFlag(MiscType.F_INFRARED_IMAGER)) {
                element.getSpecialAbs().addSPA(RCN);
            } else if (m.getType().hasFlag(MiscType.F_SEARCHLIGHT)) {
                element.getSpecialAbs().addSPA(SRCH);
            } else if (m.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                element.getSpecialAbs().addSPA(RHS);
            } else if (m.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                element.getSpecialAbs().addSPA(ECS);
            } else if (m.getType().hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)) {
                element.getSpecialAbs().addSPA(DJ);
            } else if (m.getType().hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)) {
                element.getSpecialAbs().addSPA(HJ);
            } else if (m.getType().hasFlag(MiscType.F_CARGO)) {
                element.getSpecialAbs().addSPA(CT, m.getTonnage());
            }

            if (m.getType().hasFlag(MiscType.F_SPACE_MINE_DISPENSER) && (entity instanceof Aero)) {
                element.getSpecialAbs().addSPA(MDS, 2);
            }

            if (entity instanceof Mech) {
                if (m.getType().hasFlag(MiscType.F_HARJEL)) {
                    element.getSpecialAbs().addSPA(BHJ);
                } else if (m.getType().hasFlag(MiscType.F_HARJEL_II)) {
                    element.getSpecialAbs().addSPA(BHJ2);
                } else if (m.getType().hasFlag(MiscType.F_HARJEL_III)) {
                    element.getSpecialAbs().addSPA(BHJ3);
                } else if (((MiscType)m.getType()).isShield()) {
                    element.getSpecialAbs().addSPA(SHLD);
                } else if (m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                    element.getSpecialAbs().addSPA(ITSM);
                } else if (m.getType().hasFlag(MiscType.F_TSM)) {
                    element.getSpecialAbs().addSPA(TSM);
                } else if (m.getType().hasFlag(MiscType.F_VOIDSIG)) {
                    element.getSpecialAbs().addSPA(MAS);
                } else if (((Mech) entity).isIndustrial() && m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                    element.getSpecialAbs().addSPA(SEAL);
                    if (entity.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE) {
                        element.getSpecialAbs().addSPA(SOA);
                    }
                } else if (m.getType().hasFlag(MiscType.F_NULLSIG)
                        || m.getType().hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                    element.getSpecialAbs().addSPA(STL);
                    element.getSpecialAbs().addSPA(ECM);
                } else if (m.getType().hasFlag(MiscType.F_UMU)) {
                    element.getSpecialAbs().addSPA(UMU);
                } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_INTERFACE) {
                    element.getSpecialAbs().addSPA(DN);
                } else if (m.getType().hasFlag(MiscType.F_EW_EQUIPMENT)) {
                    element.getSpecialAbs().addSPA(ECM);
                }
            }

            if (entity instanceof Protomech) {
                if (m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    if (entity.getWeight() < 10) {
                        element.getSpecialAbs().addSPA(MCS);
                    } else {
                        element.getSpecialAbs().addSPA(UCS);
                    }
                }
            }

            if (entity instanceof Tank) {
                if (m.getType().hasFlag(MiscType.F_ADVANCED_FIRECONTROL)) {
                    element.getSpecialAbs().addSPA(AFC);
                } else if (m.getType().hasFlag(MiscType.F_BASIC_FIRECONTROL)) {
                    element.getSpecialAbs().addSPA(BFC);
                } else if (m.getType().hasFlag(MiscType.F_AMPHIBIOUS) || m.getType().hasFlag(MiscType.F_FULLY_AMPHIBIOUS)
                        || m.getType().hasFlag(MiscType.F_LIMITED_AMPHIBIOUS)) {
                    element.getSpecialAbs().addSPA(AMP);
                } else if (m.getType().hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)) {
                    element.getSpecialAbs().addSPA(ARS);
                } else if (m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                    element.getSpecialAbs().addSPA(SEAL);
                    if (entity.hasEngine() && entity.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE
                            && entity.getEngine().getEngineType() != Engine.STEAM) {
                        element.getSpecialAbs().addSPA(SOA);
                    }
                } else if (m.getType().hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
                    element.getSpecialAbs().addSPA(MDS, 2);
                } else if (m.getType().hasFlag(MiscType.F_MINESWEEPER)) {
                    element.getSpecialAbs().addSPA(MSW);
                } else if (m.getType().hasFlag(MiscType.F_MASH)) {
                    element.getSpecialAbs().addSPA(MASH, (int) m.getSize());
                } else if (m.getType().hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                    element.getSpecialAbs().addSPA(MFB);
                } else if (m.getType().hasFlag(MiscType.F_OFF_ROAD)) {
                    element.getSpecialAbs().addSPA(ORO);
                } else if (m.getType().hasFlag(MiscType.F_DUNE_BUGGY)) {
                    element.getSpecialAbs().addSPA(DUN);
                } else if (m.getType().hasFlag(MiscType.F_TRACTOR_MODIFICATION)
                        || m.getType().hasFlag(MiscType.F_TRAILER_MODIFICATION)
                        || m.getType().hasFlag(MiscType.F_HITCH)) {
                    element.getSpecialAbs().addSPA(HTC);
                } else if (m.getType().hasFlag(MiscType.F_COMMAND_CONSOLE)) {
                    element.getSpecialAbs().addSPA(MHQ, 1);
                }
            }

            if (entity instanceof BattleArmor) {
                if (m.getType().hasFlag(MiscType.F_VISUAL_CAMO)
                        && !m.getType().getName().equals(BattleArmor.MIMETIC_ARMOR)) {
                    element.getSpecialAbs().addSPA(LMAS);
                } else if (m.getType().hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
                    element.getSpecialAbs().addSPA(MDS, 1);
                } else if (m.getType().hasFlag(MiscType.F_TOOLS)
                        && (m.getType().getSubType() & MiscType.S_MINESWEEPER) == MiscType.S_MINESWEEPER) {
                    element.getSpecialAbs().addSPA(BattleForceSPA.MSW);
                } else if (m.getType().hasFlag(MiscType.F_SPACE_ADAPTATION)) {
                    element.getSpecialAbs().addSPA(BattleForceSPA.SOA);
                } else if (m.getType().hasFlag(MiscType.F_PARAFOIL)) {
                    element.getSpecialAbs().addSPA(BattleForceSPA.PARA);
                } else if (m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    element.getSpecialAbs().addSPA(BattleForceSPA.XMEC);
                }
            }
        }

        // TODO: why doesnt this work?
        if (element.hasQuirk(OptionsConstants.QUIRK_POS_TRAILER_HITCH)) {
            element.getSpecialAbs().addSPA(HTC);
        }

        if (entity.isOmni() && ((entity instanceof Mech) || (entity instanceof Tank))) {
            element.getSpecialAbs().addSPA(OMNI);
        }

        if (entity.getBARRating(0) >= 1 && entity.getBARRating(0) <= 9) {
            element.getSpecialAbs().addSPA(BAR);
        }

        //TODO: Variable Range targeting is not implemented
        if (!entity.hasPatchworkArmor()) {
            switch (entity.getArmorType(0)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    element.getSpecialAbs().addSPA(BAR);
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_HARDENED:
                    element.getSpecialAbs().addSPA(CR);
                    break;
                case EquipmentType.T_ARMOR_STEALTH:
                case EquipmentType.T_ARMOR_STEALTH_VEHICLE:
                case EquipmentType.T_ARMOR_BA_STEALTH:
                case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
                case EquipmentType.T_ARMOR_BA_STEALTH_IMP:
                case EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE:
                    element.getSpecialAbs().addSPA(STL);
                    break;
                case EquipmentType.T_ARMOR_BA_MIMETIC:
                    element.getSpecialAbs().addSPA(MAS);
                    break;
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    element.getSpecialAbs().addSPA(ABA);
                    break;
                case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                    element.getSpecialAbs().addSPA(BRA);
                    break;
                case EquipmentType.T_ARMOR_BA_FIRE_RESIST:
                case EquipmentType.T_ARMOR_HEAT_DISSIPATING:
                    element.getSpecialAbs().addSPA(FR);
                    break;
                case EquipmentType.T_ARMOR_IMPACT_RESISTANT:
                    element.getSpecialAbs().addSPA(IRA);
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                    element.getSpecialAbs().addSPA(RCA);
                    break;
                case EquipmentType.T_ARMOR_REFLECTIVE:
                    element.getSpecialAbs().addSPA(RFA);
                    break;
            }
        }

        if (!element.isInfantry()) {
            if (!hasExplosiveComponent) {
                element.getSpecialAbs().addSPA(ENE);
            } else if (entity.isClan() && element.isAnyTypeOf(BM, IM, SV, CV, MS)) {
                element.getSpecialAbs().addSPA(CASE);
            }
        }

        if (entity.getAmmo().stream().map(m -> (AmmoType)m.getType())
                .anyMatch(at -> at.hasFlag(AmmoType.F_TELE_MISSILE))) {
            element.getSpecialAbs().addSPA(TELE);
        }

        if (entity.hasEngine() && !element.isLargeAerospace()) {
            if (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) {
                element.getSpecialAbs().addSPA(EE);
            } else if (entity.getEngine().getEngineType() == Engine.FUEL_CELL) {
                element.getSpecialAbs().addSPA(FC);
            }
        }

        for (Transporter t : entity.getTransports()) {
            if (t instanceof ASFBay) {
                element.getSpecialAbs().addSPA(AT, (int)((ASFBay)t).getCapacity());
                element.getSpecialAbs().addSPA(ATxD, ((ASFBay)t).getDoors());
                element.getSpecialAbs().addSPA(MFB);
            } else if (t instanceof CargoBay) {
                if (((CargoBay)t).getCapacity() >= 1000) {
                    element.getSpecialAbs().addSPA(CK, ((CargoBay) t).getCapacity() / 1000);
                    element.getSpecialAbs().addSPA(CKxD, ((CargoBay) t).getDoors());
                } else {
                    element.getSpecialAbs().addSPA(CT, ((CargoBay) t).getCapacity());
                    element.getSpecialAbs().addSPA(CTxD, ((CargoBay) t).getDoors());
                }
            } else if (t instanceof DockingCollar) {
                element.getSpecialAbs().addSPA(DT, 1);
            } else if (t instanceof InfantryBay) {
                element.getSpecialAbs().addSPA(IT, ((InfantryBay)t).getCapacity());
            } else if (t instanceof TroopSpace) {
                element.getSpecialAbs().addSPA(IT, t.getUnused());
            } else if (t instanceof MechBay) {
                element.getSpecialAbs().addSPA(MT, (int)((MechBay)t).getCapacity());
                element.getSpecialAbs().addSPA(MTxD, ((MechBay)t).getDoors());
                element.getSpecialAbs().addSPA(MFB);
            } else if (t instanceof ProtomechBay) {
                element.getSpecialAbs().addSPA(PT, (int)((ProtomechBay)t).getCapacity());
                element.getSpecialAbs().addSPA(PTxD, ((ProtomechBay)t).getDoors());
                element.getSpecialAbs().addSPA(MFB);
            } else if (t instanceof SmallCraftBay) {
                element.getSpecialAbs().addSPA(ST, (int)((SmallCraftBay)t).getCapacity());
                element.getSpecialAbs().addSPA(STxD, ((SmallCraftBay)t).getDoors());
                element.getSpecialAbs().addSPA(MFB);
            } else if (t instanceof LightVehicleBay) {
                element.getSpecialAbs().addSPA(VTM, (int)((LightVehicleBay)t).getCapacity());
                element.getSpecialAbs().addSPA(VTMxD, ((LightVehicleBay)t).getDoors());
                element.getSpecialAbs().addSPA(MFB);
            } else if (t instanceof HeavyVehicleBay) {
                element.getSpecialAbs().addSPA(VTH, (int)((HeavyVehicleBay)t).getCapacity());
                element.getSpecialAbs().addSPA(VTHxD, ((HeavyVehicleBay)t).getDoors());
                element.getSpecialAbs().addSPA(MFB);
            }
        }

        topLoop: for (int location = 0; location < entity.locations(); location++) {
            for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
                CriticalSlot crit = entity.getCritical(location, slot);
                if (null != crit) {
                    if (crit.isArmored()) {
                        element.getSpecialAbs().addSPA(ARM);
                        break topLoop;
                    } else if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted mount = crit.getMount();
                        if (mount.isArmored()) {
                            element.getSpecialAbs().addSPA(ARM);
                            break topLoop;
                        }
                    }
                }
            }
        }

        if (entity instanceof Aero) {
            if (((Aero) entity).getCockpitType() == Aero.COCKPIT_COMMAND_CONSOLE) {
                element.getSpecialAbs().addSPA(MHQ, 1);
            }
            if (entity.hasWorkingMisc(MiscType.F_COMMAND_CONSOLE)) {
                element.getSpecialAbs().addSPA(MHQ, 1);
            }
            if (entity.isFighter()) {
                element.getSpecialAbs().addSPA(BOMB, element.getSize());
            }
            if ((entity.getEntityType() & (Entity.ETYPE_JUMPSHIP | Entity.ETYPE_CONV_FIGHTER)) == 0) {
                element.getSpecialAbs().addSPA(SPC);
            }
            if (((Aero) entity).isVSTOL()) {
                element.getSpecialAbs().addSPA(VSTOL);
            }
            if (element.isType(AF)) {
                element.getSpecialAbs().addSPA(FUEL, (int) Math.round(0.05 * ((Aero) entity).getFuel()));
            }
        }

        if (entity instanceof Infantry) {
            element.getSpecialAbs().addSPA(CAR, (int)Math.ceil(entity.getWeight()));
            if (entity.getMovementMode().equals(EntityMovementMode.INF_UMU)) {
                element.getSpecialAbs().addSPA(UMU);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.FIRE_ENGINEERS)) {
                element.getSpecialAbs().addSPA(FF);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.MINE_ENGINEERS)) {
                element.getSpecialAbs().addSPA(MSW);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.MOUNTAIN_TROOPS)) {
                element.getSpecialAbs().addSPA(MTN);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.PARATROOPS)) {
                element.getSpecialAbs().addSPA(PARA);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.SCUBA)) {
                element.getSpecialAbs().addSPA(UMU);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.TRENCH_ENGINEERS)) {
                element.getSpecialAbs().addSPA(TRN);
            }
            if (entity.hasAbility("tsm_implant")) {
                element.getSpecialAbs().addSPA(TSI);
            }
            if ((entity instanceof BattleArmor) && ((BattleArmor) entity).canDoMechanizedBA()) {
                element.getSpecialAbs().addSPA(MEC);
            }
        }

        if (entity instanceof Mech) {
            if (((Mech) entity).getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
                element.getSpecialAbs().addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE) {
                element.getSpecialAbs().addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_SMALL_COMMAND_CONSOLE) {
                element.getSpecialAbs().addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_VRRP) {
                element.getSpecialAbs().addSPA(VR, 1);
            }
            if (((Mech) entity).isIndustrial()) {
                if (((Mech) entity).getCockpitType() == Mech.COCKPIT_STANDARD) {
                    element.getSpecialAbs().addSPA(AFC);
                } else {
                    element.getSpecialAbs().addSPA(BFC);
                }
            } else {
                element.getSpecialAbs().addSPA(SOA);
                element.getSpecialAbs().addSPA(SRCH);
            }
        }

        if (entity instanceof Protomech) {
            element.getSpecialAbs().addSPA(SOA);
            if (entity.getMovementMode().equals(EntityMovementMode.WIGE)) {
                element.getSpecialAbs().addSPA(GLD);
            }
        }

        if (entity instanceof Tank && !entity.isSupportVehicle()) {
            element.getSpecialAbs().addSPA(SRCH);
        }

        if (element.isAnyTypeOf(SC, DS, DA)) {
            if (element.getSize() == 1) {
                element.getSpecialAbs().addSPA(LG);
            } else if (element.getSize() == 2) {
                element.getSpecialAbs().addSPA(VLG);
            } else {
                element.getSpecialAbs().addSPA(SLG);
            }
        }

        if (element.getMovementModes().contains("j") && element.getMovementModes().contains("")) {
            int jumpTMM = ASConverter.tmmForMovement(element.getMovement("j"));
            int walkTMM = ASConverter.tmmForMovement(element.getMovement(""));
            if (jumpTMM > walkTMM) {
                element.getSpecialAbs().addSPA(JMPS, jumpTMM - walkTMM);
            } else if (jumpTMM < walkTMM) {
                element.getSpecialAbs().addSPA(JMPW, walkTMM - jumpTMM);
            }
        }

        if (element.getMovementModes().contains("s") && element.getMovementModes().contains("")) {
            int umuTMM = ASConverter.tmmForMovement(element.getMovement("s"));
            int walkTMM = ASConverter.tmmForMovement(element.getMovement(""));
            if (umuTMM > walkTMM) {
                element.getSpecialAbs().addSPA(SUBS, umuTMM - walkTMM);
            } else if (umuTMM < walkTMM) {
                element.getSpecialAbs().addSPA(SUBW, walkTMM - umuTMM);
            }
        }

        if (element.isType(CF) || (entity instanceof VTOL)) {
            element.getSpecialAbs().addSPA(ATMO);
        }

        if (entity instanceof LandAirMech) {
            LandAirMech lam = (LandAirMech) entity;
            double bombs = entity.countWorkingMisc(MiscType.F_BOMB_BAY);
            int bombValue = ASConverter.roundUp(bombs / 5);
            if (bombValue > 0) {
                element.getSpecialAbs().addSPA(BOMB, bombValue);
            }
            element.getSpecialAbs().addSPA(FUEL, (int) Math.round(0.05 * lam.getFuel()));
            var lamMoves = new HashMap<String, Integer>();
            lamMoves.put("g", lam.getAirMechCruiseMP(false, false) * 2);
            lamMoves.put("a", lam.getCurrentThrust());
            if (lam.getLAMType() == LandAirMech.LAM_BIMODAL) {
                element.addBimSPA(lamMoves);
            } else {
                element.addLamSPA(lamMoves);
            }
        }

        if (entity instanceof QuadVee) {
            element.getSpecialAbs().addSPA(QV);
        }

        if (element.hasAutoSeal()) {
            element.getSpecialAbs().addSPA(SEAL);
        }

        if (entity instanceof Jumpship) {
            element.getSpecialAbs().addSPA(KF);
            if (((Jumpship) entity).hasLF()) {
                element.getSpecialAbs().addSPA(LF);
            }
            if (entity.getNCrew() >= 60) {
                element.getSpecialAbs().addSPA(CRW, (int) Math.round(entity.getNCrew() / 120.0));
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
        if (element.getSPA(MHQ) instanceof Double) {
            double mhqValue = (double) element.getSPA(MHQ);
            element.getSpecialAbs().replaceSPA(MHQ, (int) mhqValue);
        }

        // Cannot have both CASEII and CASE
        if (element.hasSPA(CASEII)) {
            element.getSpecialAbs().removeSPA(CASE);
        }

        // Implicit rule: XMEC overrides MEC
        if (element.hasSPA(XMEC)) {
            element.getSpecialAbs().removeSPA(MEC);
        }

        // Implicit rule: AECM overrides ECM
        if (element.hasSPA(AECM)) {
            element.getSpecialAbs().removeSPA(ECM);
        }

        // Some SUAs are accompanied by RCN
        if (element.hasAnySPAOf(PRB, LPRB, NOVA, BH, WAT)) {
            element.getSpecialAbs().addSPA(RCN);
        }

        // CT/IT value may be decimal but replace it with an integer value if it is integer
        if (element.hasSPA(CT) && (element.getSPA(CT) instanceof Double)) {
            double ctValue = (double) element.getSPA(CT);
            if ((int) ctValue == ctValue) {
                element.getSpecialAbs().replaceSPA(CT, (int) ctValue);
            }
        }
        if (element.hasSPA(IT) && (element.getSPA(IT) instanceof Double)) {
            double ctValue = (double) element.getSPA(IT);
            if ((int) ctValue == ctValue) {
                element.getSpecialAbs().replaceSPA(IT, (int) ctValue);
            }
        }

        // For CI, replace the placeholder HT with the S damage value
        if (element.isType(CI) && element.hasSPA(HT)) {
            int dmg = element.getStandardDamage().S.damage;
            if (dmg > 0) {
                element.getSpecialAbs().addSPA(HT, ASDamageVector.createNormRndDmg(Math.min(2, dmg), 0, 0));
            } else {
                element.getSpecialAbs().removeSPA(HT);
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
