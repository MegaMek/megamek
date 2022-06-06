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
                element.addSPA(PRB);
            } else if (m.getType().getInternalName().equals(Sensor.LIGHT_AP)
                    || m.getType().getInternalName().equals(Sensor.ISBALIGHT_AP)
                    || m.getType().getInternalName().equals(Sensor.EW_EQUIPMENT)) {
                element.addSPA(LPRB);
            } else if (m.getType().getInternalName().equals(Sensor.BLOODHOUND)) {
                element.addSPA(BH);
            } else if (m.getType().getInternalName().equals(Sensor.WATCHDOG)) {
                element.addSPA(LPRB);
                element.addSPA(ECM);
                element.addSPA(WAT);
            } else if (m.getType().getInternalName().equals(Sensor.NOVA)) {
                element.addSPA(NOVA);
                element.addSPA(PRB);
                element.addSPA(ECM);
                element.addSPA(MHQ, 1.5);
            } else if (m.getType().hasFlag(MiscType.F_ECM)) {
                if (m.getType().hasFlag(MiscType.F_ANGEL_ECM)) {
                    element.addSPA(AECM);
                } else if (m.getType().hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                    element.addSPA(LECM);
                } else {
                    element.addSPA(ECM);
                }
            } else if (m.getType().hasFlag(MiscType.F_BOOBY_TRAP) && !element.isAnyTypeOf(PM, CI, BA)) {
                element.addSPA(BT);
            } else if (m.getType().hasFlag(MiscType.F_LIGHT_BRIDGE_LAYER)
                    || m.getType().hasFlag(MiscType.F_MEDIUM_BRIDGE_LAYER)
                    || m.getType().hasFlag(MiscType.F_HEAVY_BRIDGE_LAYER)) {
                element.addSPA(BRID);
            } else if (m.getType().hasFlag(MiscType.F_C3S)) {
                element.addSPA(C3S);
                element.addSPA(MHQ, 1);
                if (m.getType().hasFlag(MiscType.F_C3EM)) {
                    element.addSPA(C3EM, 1);
                }
            } else if (m.getType().hasFlag(MiscType.F_C3SBS)) {
                element.addSPA(C3BSS, 1);
                element.addSPA(MHQ, 2);
            } else if (m.getType().hasFlag(MiscType.F_C3I)) {
                if ((entity.getEntityType() & Entity.ETYPE_AERO) == Entity.ETYPE_AERO) {
                    element.addSPA(NC3);
                } else {
                    element.addSPA(C3I);
                    if (m.getType().hasFlag(MiscType.F_BA_EQUIPMENT)) {
                        element.addSPA(MHQ, 2);
                    } else {
                        element.addSPA(MHQ, 2.5);
                    }
                }
            } else if (m.getType().hasFlag(MiscType.F_CASE) && !element.isAnyTypeOf(AF, CF, CI, BA, PM)) {
                element.addSPA(CASE);
            } else if (m.getType().hasFlag(MiscType.F_CASEP) && !element.isAnyTypeOf(AF, CF, CI, BA, PM)) {
                element.addSPA(CASEP);
            } else if (m.getType().hasFlag(MiscType.F_CASEII) && !element.isAnyTypeOf(AF, CF, CI, BA, PM)) {
                element.addSPA(CASEII);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                element.addSPA(DRO);
            } else if (m.getType().hasFlag(MiscType.F_SRCS)
                    || m.getType().hasFlag(MiscType.F_SASRCS)
                    || m.getType().hasFlag(MiscType.F_CASPAR)
                    || m.getType().hasFlag(MiscType.F_CASPARII)) {
                element.addSPA(RBT);
            } else if (m.getType().hasFlag(MiscType.F_DRONE_CARRIER_CONTROL)) {
                element.addSPA(DCC, (int) m.getSize());
            } else if (m.getType().hasFlag(MiscType.F_REMOTE_DRONE_COMMAND_CONSOLE)) {
                element.addSPA(DCC, 1);
            } else if (m.getType().hasFlag(MiscType.F_EJECTION_SEAT)) {
                element.addSPA(ES);
            } else if (m.getType().hasFlag(MiscType.F_BULLDOZER)) {
                element.addSPA(ENG);
            } else if (m.getType().hasFlag(MiscType.F_HAND_WEAPON)) {
                element.addSPA(MEL);
            } else if (m.getType().getInternalName().equals("ProtoQuadMeleeSystem")) {
                element.addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_TALON)) {
                element.addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_CLUB)) {
                element.addSPA(MEL);
                if ((m.getType().getSubType() &
                        (MiscType.S_BACKHOE | MiscType.S_PILE_DRIVER
                                | MiscType.S_MINING_DRILL | MiscType.S_ROCK_CUTTER
                                | MiscType.S_WRECKING_BALL)) != 0) {
                    element.addSPA(ENG);
                } else if ((m.getType().getSubType() &
                        (MiscType.S_DUAL_SAW | MiscType.S_CHAINSAW
                                | MiscType.S_BUZZSAW | MiscType.S_RETRACTABLE_BLADE)) != 0) {
                    element.addSPA(SAW);
                }
            } else if (m.getType().hasFlag(MiscType.F_SPIKES)) {
                element.addSPA(MEL);
            } else if (m.getType().hasFlag(MiscType.F_FIRE_RESISTANT)) {
                element.addSPA(FR);
            } else if (m.getType().hasFlag(MiscType.F_MOBILE_HPG)) {
                element.addSPA(HPG);
            } else if (m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                element.addSPA(MHQ, (int) m.getTonnage());
                if (m.getTonnage() >= entity.getWeight() / 20.0) {
                    element.addSPA(RCN);
                }
            } else if (m.getType().hasFlag(MiscType.F_SENSOR_DISPENSER)) {
                element.addSPA(RSD, 1);
                element.addSPA(RCN);
            } else if (m.getType().hasFlag(MiscType.F_LOOKDOWN_RADAR)
                    || m.getType().hasFlag(MiscType.F_RECON_CAMERA)
                    || m.getType().hasFlag(MiscType.F_HIRES_IMAGER)
                    || m.getType().hasFlag(MiscType.F_HYPERSPECTRAL_IMAGER)
                    || m.getType().hasFlag(MiscType.F_INFRARED_IMAGER)) {
                element.addSPA(RCN);
            } else if (m.getType().hasFlag(MiscType.F_SEARCHLIGHT)) {
                element.addSPA(SRCH);
            } else if (m.getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
                element.addSPA(RHS);
            } else if (m.getType().hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                element.addSPA(ECS);
            } else if (m.getType().hasFlag(MiscType.F_VIRAL_JAMMER_DECOY)) {
                element.addSPA(DJ);
            } else if (m.getType().hasFlag(MiscType.F_VIRAL_JAMMER_HOMING)) {
                element.addSPA(HJ);
            } else if (m.getType().hasFlag(MiscType.F_CARGO)) {
                //System.out.println("Tonnage: "+ m.getTonnage());
                element.addSPA(CT, m.getTonnage());
            }

            if (m.getType().hasFlag(MiscType.F_SPACE_MINE_DISPENSER) && (entity instanceof Aero)) {
                element.addSPA(MDS, 2);
            }

            if (entity instanceof Mech) {
                if (m.getType().hasFlag(MiscType.F_HARJEL)) {
                    element.addSPA(BHJ);
                } else if (m.getType().hasFlag(MiscType.F_HARJEL_II)) {
                    element.addSPA(BHJ2);
                } else if (m.getType().hasFlag(MiscType.F_HARJEL_III)) {
                    element.addSPA(BHJ3);
                } else if (((MiscType)m.getType()).isShield()) {
                    element.addSPA(SHLD);
                } else if (m.getType().hasFlag(MiscType.F_INDUSTRIAL_TSM)) {
                    element.addSPA(ITSM);
                } else if (m.getType().hasFlag(MiscType.F_TSM)) {
                    element.addSPA(TSM);
                } else if (m.getType().hasFlag(MiscType.F_VOIDSIG)) {
                    element.addSPA(MAS);
                } else if (((Mech) entity).isIndustrial() && m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                    element.addSPA(SEAL);
                    if (entity.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE) {
                        element.addSPA(SOA);
                    }
                } else if (m.getType().hasFlag(MiscType.F_NULLSIG)
                        || m.getType().hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                    element.addSPA(STL);
                    element.addSPA(ECM);
                } else if (m.getType().hasFlag(MiscType.F_UMU)) {
                    element.addSPA(UMU);
                } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_INTERFACE) {
                    element.addSPA(DN);
                } else if (m.getType().hasFlag(MiscType.F_EW_EQUIPMENT)) {
                    element.addSPA(ECM);
                }
            }

            if (entity instanceof Protomech) {
                if (m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    if (entity.getWeight() < 10) {
                        element.addSPA(MCS);
                    } else {
                        element.addSPA(UCS);
                    }
                }
            }

            if (entity instanceof Tank) {
                if (m.getType().hasFlag(MiscType.F_ADVANCED_FIRECONTROL)) {
                    element.addSPA(AFC);
                } else if (m.getType().hasFlag(MiscType.F_BASIC_FIRECONTROL)) {
                    element.addSPA(BFC);
                } else if (m.getType().hasFlag(MiscType.F_AMPHIBIOUS) || m.getType().hasFlag(MiscType.F_FULLY_AMPHIBIOUS)
                        || m.getType().hasFlag(MiscType.F_LIMITED_AMPHIBIOUS)) {
                    element.addSPA(AMP);
                } else if (m.getType().hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)) {
                    element.addSPA(ARS);
                } else if (m.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)) {
                    element.addSPA(SEAL);
                    if (entity.hasEngine() && entity.getEngine().getEngineType() != Engine.COMBUSTION_ENGINE
                            && entity.getEngine().getEngineType() != Engine.STEAM) {
                        element.addSPA(SOA);
                    }
                } else if (m.getType().hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
                    element.addSPA(MDS, 2);
                } else if (m.getType().hasFlag(MiscType.F_MINESWEEPER)) {
                    element.addSPA(MSW);
                } else if (m.getType().hasFlag(MiscType.F_MASH)) {
                    element.addSPA(MASH, (int) m.getSize());
                } else if (m.getType().hasFlag(MiscType.F_MOBILE_FIELD_BASE)) {
                    element.addSPA(MFB);
                } else if (m.getType().hasFlag(MiscType.F_OFF_ROAD)) {
                    element.addSPA(ORO);
                } else if (m.getType().hasFlag(MiscType.F_DUNE_BUGGY)) {
                    element.addSPA(DUN);
                } else if (m.getType().hasFlag(MiscType.F_TRACTOR_MODIFICATION)
                        || m.getType().hasFlag(MiscType.F_TRAILER_MODIFICATION)
                        || m.getType().hasFlag(MiscType.F_HITCH)) {
                    element.addSPA(HTC);
                } else if (m.getType().hasFlag(MiscType.F_COMMAND_CONSOLE)) {
                    element.addSPA(MHQ, 1);
                }
            }

            if (entity instanceof BattleArmor) {
                if (m.getType().hasFlag(MiscType.F_VISUAL_CAMO)
                        && !m.getType().getName().equals(BattleArmor.MIMETIC_ARMOR)) {
                    element.addSPA(LMAS);
                } else if (m.getType().hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
                    element.addSPA(MDS, 1);
                } else if (m.getType().hasFlag(MiscType.F_TOOLS)
                        && (m.getType().getSubType() & MiscType.S_MINESWEEPER) == MiscType.S_MINESWEEPER) {
                    element.addSPA(BattleForceSPA.MSW);
                } else if (m.getType().hasFlag(MiscType.F_SPACE_ADAPTATION)) {
                    element.addSPA(BattleForceSPA.SOA);
                } else if (m.getType().hasFlag(MiscType.F_PARAFOIL)) {
                    element.addSPA(BattleForceSPA.PARA);
                } else if (m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    element.addSPA(BattleForceSPA.XMEC);
                }
            }
        }

        // TODO: why doesnt this work?
        if (element.hasQuirk(OptionsConstants.QUIRK_POS_TRAILER_HITCH)) {
            element.addSPA(HTC);
        }

        if (entity.isOmni() && ((entity instanceof Mech) || (entity instanceof Tank))) {
            element.addSPA(OMNI);
        }

        if (entity.getBARRating(0) >= 1 && entity.getBARRating(0) <= 9) {
            element.addSPA(BAR);
        }

        //TODO: Variable Range targeting is not implemented
        if (!entity.hasPatchworkArmor()) {
            switch (entity.getArmorType(0)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    element.addSPA(BAR);
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                case EquipmentType.T_ARMOR_HARDENED:
                    element.addSPA(CR);
                    break;
                case EquipmentType.T_ARMOR_STEALTH:
                case EquipmentType.T_ARMOR_STEALTH_VEHICLE:
                case EquipmentType.T_ARMOR_BA_STEALTH:
                case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
                case EquipmentType.T_ARMOR_BA_STEALTH_IMP:
                case EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE:
                    element.addSPA(STL);
                    break;
                case EquipmentType.T_ARMOR_BA_MIMETIC:
                    element.addSPA(MAS);
                    break;
                case EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION:
                    element.addSPA(ABA);
                    break;
                case EquipmentType.T_ARMOR_BALLISTIC_REINFORCED:
                    element.addSPA(BRA);
                    break;
                case EquipmentType.T_ARMOR_BA_FIRE_RESIST:
                case EquipmentType.T_ARMOR_HEAT_DISSIPATING:
                    element.addSPA(FR);
                    break;
                case EquipmentType.T_ARMOR_IMPACT_RESISTANT:
                    element.addSPA(IRA);
                    break;
                case EquipmentType.T_ARMOR_REACTIVE:
                    element.addSPA(RCA);
                    break;
                case EquipmentType.T_ARMOR_REFLECTIVE:
                    element.addSPA(RFA);
                    break;
            }
        }

        if (!element.isInfantry()) {
            if (!hasExplosiveComponent) {
                element.addSPA(ENE);
            } else if (entity.isClan() && element.isAnyTypeOf(BM, IM, SV, CV, MS)) {
                element.addSPA(CASE);
            }
        }

        if (entity.getAmmo().stream().map(m -> (AmmoType)m.getType())
                .anyMatch(at -> at.hasFlag(AmmoType.F_TELE_MISSILE))) {
            element.addSPA(TELE);
        }

        if (entity.hasEngine()) {
            if (entity.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE) {
                element.addSPA(EE);
            } else if (entity.getEngine().getEngineType() == Engine.FUEL_CELL) {
                element.addSPA(FC);
            }
        }

        for (Transporter t : entity.getTransports()) {
            if (t instanceof ASFBay) {
                element.addSPA(AT, (int)((ASFBay)t).getCapacity());
                element.addSPA(ATxD, ((ASFBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof CargoBay) {
                element.addSPA(CT, ((CargoBay)t).getCapacity());
                element.addSPA(CTxD, ((CargoBay)t).getDoors());
            } else if (t instanceof DockingCollar) {
                element.addSPA(DT, 1);
            } else if (t instanceof InfantryBay) {
                element.addSPA(IT, ((InfantryBay)t).getCapacity());
            } else if (t instanceof TroopSpace) {
                element.addSPA(IT, t.getUnused());
            } else if (t instanceof MechBay) {
                element.addSPA(MT, (int)((MechBay)t).getCapacity());
                element.addSPA(MTxD, ((MechBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof ProtomechBay) {
                element.addSPA(PT, (int)((ProtomechBay)t).getCapacity());
                element.addSPA(PTxD, ((ProtomechBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof SmallCraftBay) {
                element.addSPA(ST, (int)((SmallCraftBay)t).getCapacity());
                element.addSPA(STxD, ((SmallCraftBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof LightVehicleBay) {
                element.addSPA(VTM, (int)((LightVehicleBay)t).getCapacity());
                element.addSPA(VTMxD, ((LightVehicleBay)t).getDoors());
                element.addSPA(MFB);
            } else if (t instanceof HeavyVehicleBay) {
                element.addSPA(VTH, (int)((HeavyVehicleBay)t).getCapacity());
                element.addSPA(VTHxD, ((HeavyVehicleBay)t).getDoors());
                element.addSPA(MFB);
            }
        }

        topLoop: for (int location = 0; location < entity.locations(); location++) {
            for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
                CriticalSlot crit = entity.getCritical(location, slot);
                if (null != crit) {
                    if (crit.isArmored()) {
                        element.addSPA(ARM);
                        break topLoop;
                    } else if (crit.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                        Mounted mount = crit.getMount();
                        if (mount.isArmored()) {
                            element.addSPA(ARM);
                            break topLoop;
                        }
                    }
                }
            }
        }

        if (entity instanceof Aero) {
            if (((Aero) entity).getCockpitType() == Aero.COCKPIT_COMMAND_CONSOLE) {
                element.addSPA(MHQ, 1);
            }
            if (entity.hasWorkingMisc(MiscType.F_COMMAND_CONSOLE)) {
                element.addSPA(MHQ, 1);
            }
            if (entity.isFighter()) {
                element.addSPA(BOMB, element.getSize());
            }
            if ((entity.getEntityType() & (Entity.ETYPE_JUMPSHIP | Entity.ETYPE_CONV_FIGHTER)) == 0) {
                element.addSPA(SPC);
            }
            if (((Aero) entity).isVSTOL()) {
                element.addSPA(VSTOL);
            }
            if (element.isType(AF)) {
                element.addSPA(FUEL, (int) Math.round(0.05 * ((Aero) entity).getFuel()));
            }
        }

        if (entity instanceof Infantry) {
            element.addSPA(CAR, (int)Math.ceil(entity.getWeight()));
            if (entity.getMovementMode().equals(EntityMovementMode.INF_UMU)) {
                element.addSPA(UMU);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.FIRE_ENGINEERS)) {
                element.addSPA(FF);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.MINE_ENGINEERS)) {
                element.addSPA(MSW);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.MOUNTAIN_TROOPS)) {
                element.addSPA(MTN);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.PARATROOPS)) {
                element.addSPA(PARA);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.SCUBA)) {
                element.addSPA(UMU);
            }
            if (((Infantry) entity).hasSpecialization(Infantry.TRENCH_ENGINEERS)) {
                element.addSPA(TRN);
            }
            if (entity.hasAbility("tsm_implant")) {
                element.addSPA(TSI);
            }
            if ((entity instanceof BattleArmor) && ((BattleArmor) entity).canDoMechanizedBA()) {
                element.addSPA(MEC);
            }
        }

        if (entity instanceof Mech) {
            if (((Mech) entity).getCockpitType() == Mech.COCKPIT_COMMAND_CONSOLE) {
                element.addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE) {
                element.addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_SMALL_COMMAND_CONSOLE) {
                element.addSPA(MHQ, 1);
            } else if (((Mech) entity).getCockpitType() == Mech.COCKPIT_VRRP) {
                element.addSPA(VR, 1);
            }
            if (((Mech) entity).isIndustrial()) {
                if (((Mech) entity).getCockpitType() == Mech.COCKPIT_STANDARD) {
                    element.addSPA(AFC);
                } else {
                    element.addSPA(BFC);
                }
            } else {
                element.addSPA(SOA);
                element.addSPA(SRCH);
            }
        }

        if (entity instanceof Protomech) {
            element.addSPA(SOA);
            if (entity.getMovementMode().equals(EntityMovementMode.WIGE)) {
                element.addSPA(GLD);
            }
        }

        if (entity instanceof Tank && !entity.isSupportVehicle()) {
            element.addSPA(SRCH);
        }

        if (element.isAnyTypeOf(SC, DS, DA)) {
            if (element.getSize() == 1) {
                element.addSPA(LG);
            } else if (element.getSize() == 2) {
                element.addSPA(VLG);
            } else {
                element.addSPA(SLG);
            }
        }

        if (element.getMovementModes().contains("j") && element.getMovementModes().contains("")) {
            int jumpTMM = ASConverter.tmmForMovement(element.getMovement("j"), conversionData);
            int walkTMM = ASConverter.tmmForMovement(element.getMovement(""), conversionData);
            if (jumpTMM > walkTMM) {
                element.addSPA(JMPS, jumpTMM - walkTMM);
            } else if (jumpTMM < walkTMM) {
                element.addSPA(JMPW, walkTMM - jumpTMM);
            }
        }

        if (element.getMovementModes().contains("s") && element.getMovementModes().contains("")) {
            int umuTMM = ASConverter.tmmForMovement(element.getMovement("s"), conversionData);
            int walkTMM = ASConverter.tmmForMovement(element.getMovement(""), conversionData);
            if (umuTMM > walkTMM) {
                element.addSPA(SUBS, umuTMM - walkTMM);
            } else if (umuTMM < walkTMM) {
                element.addSPA(SUBW, walkTMM - umuTMM);
            }
        }

        if (element.isType(CF) || (entity instanceof VTOL)) {
            element.addSPA(ATMO);
        }

        if (entity instanceof LandAirMech) {
            LandAirMech lam = (LandAirMech) entity;
            double bombs = entity.countWorkingMisc(MiscType.F_BOMB_BAY);
            int bombValue = ASConverter.roundUp(bombs / 5);
            if (bombValue > 0) {
                element.addSPA(BOMB, bombValue);
            }
            element.addSPA(FUEL, (int) Math.round(0.05 * lam.getFuel()));
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
            element.addSPA(QV);
        }

        if (element.hasAutoSeal()) {
            element.addSPA(SEAL);
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
            element.replaceSPA(MHQ, (int) mhqValue);
        }

        // Cannot have both CASEII and CASE
        if (element.hasSPA(CASEII)) {
            element.removeSPA(CASE);
        }

        // Implicit rule: AECM overrides ECM
        if (element.hasSPA(AECM)) {
            element.removeSPA(ECM);
        }

        // Some SUAs are accompanied by RCN
        if (element.hasAnySPAOf(PRB, LPRB, NOVA, BH, WAT)) {
            element.addSPA(RCN);
        }

        // CT/IT value may be decimal but replace it with an integer value if it is integer
        if (element.hasSPA(CT) && (element.getSPA(CT) instanceof Double)) {
            double ctValue = (double) element.getSPA(CT);
            if ((int) ctValue == ctValue) {
                element.replaceSPA(CT, (int) ctValue);
            }
        }
        if (element.hasSPA(IT) && (element.getSPA(IT) instanceof Double)) {
            double ctValue = (double) element.getSPA(IT);
            if ((int) ctValue == ctValue) {
                element.replaceSPA(IT, (int) ctValue);
            }
        }
    }

    // Make non-instantiable
    private ASSpecialAbilityConverter() { }

}
