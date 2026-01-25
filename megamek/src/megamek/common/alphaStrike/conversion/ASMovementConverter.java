/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.alphaStrike.conversion;

import java.util.HashMap;
import java.util.Map;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.MPCalculationSetting;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.QuadVee;
import megamek.common.units.Warship;

final class ASMovementConverter {

    /**
     * Movement conversion, AlphaStrike Companion, p.92
     */
    static Map<String, Integer> convertMovement(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity();
        CalculationReport report = conversionData.conversionReport();
        report.addEmptyLine();
        report.addSubHeader("Movement:");

        if (entity instanceof Infantry) {
            return convertMovementForInfantry(conversionData);
        } else if (entity instanceof Aero) {
            return convertMovementForAero(conversionData);
        } else {
            return convertMovementForNonInfantry(conversionData);
        }
    }

    private static Map<String, Integer> convertMovementForAero(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity();
        CalculationReport report = conversionData.conversionReport();

        var result = new HashMap<String, Integer>();
        if (entity instanceof Warship) {
            result.put("", entity.getWalkMP());
            report.addLine("WarShip", "Cruise MP", Integer.toString(entity.getWalkMP()));
        } else if (entity instanceof Jumpship) {
            result.put("k", (int) (((Jumpship) entity).getStationKeepingThrust() * 10));
            report.addLine("JumpShip",
                  "Station Keeping Thrust x 10",
                  (int) (((Jumpship) entity).getStationKeepingThrust() * 10) + "k");
        } else {
            String movementCode = getMovementCode(conversionData);
            result.put(movementCode, entity.getWalkMP());
            report.addLine("Safe Thrust", entity.getWalkMP() + movementCode);
        }
        return result;
    }

    private static Map<String, Integer> convertMovementForNonInfantry(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity();
        AlphaStrikeElement element = conversionData.element();
        CalculationReport report = conversionData.conversionReport();

        var result = new HashMap<String, Integer>();
        double walkMP = entity.getOriginalWalkMP();
        report.addLine("Base Walking MP", "", Integer.toString(entity.getOriginalWalkMP()));

        int jumpMove = entity.getJumpMP(MPCalculationSetting.AS_CONVERSION) * 2;
        int mechanicalBoosterMP = entity.getMechanicalJumpBoosterMP(MPCalculationSetting.AS_CONVERSION) * 2;
        jumpMove = Math.max(mechanicalBoosterMP, jumpMove);

        if (hasSupercharger(entity) && hasMekMASC(entity)) {
            walkMP *= 1.5;
            report.addLine("MASC + Supercharger", "x 1.5", "= ", walkMP);
        } else if (hasSupercharger(entity) || hasMekMASC(entity)
              || hasJetBooster(entity) || hasMyomerBooster(entity)) {
            walkMP *= 1.25;
            report.addLine("MASC, SC, Jet or Myomer Booster", "x 1.25", "= ", walkMP);
        }
        walkMP = Math.round(walkMP);
        report.addLine("Round normal", "= " + Math.round(walkMP));

        if ((entity instanceof Mek) && ((Mek) entity).hasMPReducingHardenedArmor()) {
            walkMP--;
            report.addLine("MP reducing armor", "- 1", "= ", walkMP);
        }
        if (entity.hasModularArmor()) {
            walkMP--;
            report.addLine("Modular armor", "- 1", "= ", walkMP);
        }
        if (hasMPReducingShield(entity)) {
            walkMP--;
            report.addLine("Shield", "- 1", "= ", walkMP);
        }

        walkMP = Math.max(walkMP, 0);
        int baseMove = ((int) Math.round(walkMP * 2));
        report.addLine("Hex to inch", "x 2", "= " + baseMove);
        if (baseMove % 2 == 1) {
            baseMove++;
            report.addLine("Increase odd to even number", "= " + baseMove);
        }

        String movementCode = getMovementCode(conversionData);
        element.setPrimaryMovementMode(movementCode);

        if ((jumpMove == baseMove) && (jumpMove > 0) && movementCode.isEmpty()) {
            result.put("j", baseMove);
            report.addLine("Equal to Jump Move", entity.getAnyTypeMaxJumpMP() + " x 2", baseMove + "\"j");
        } else {
            result.put(movementCode, baseMove);
            report.addLine("Standard Movement", baseMove + "\"" + movementCode);
            if (jumpMove > 0) {
                result.put("j", jumpMove);
                report.addLine("Jump Movement", entity.getAnyTypeMaxJumpMP() + " x 2", jumpMove + "\"j");
            }
        }

        addUMUMovement(result, conversionData);
        return result;
    }

    private static Map<String, Integer> convertMovementForInfantry(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity();
        AlphaStrikeElement element = conversionData.element();
        CalculationReport report = conversionData.conversionReport();

        var result = new HashMap<String, Integer>();
        int walkingMP = entity.getWalkMP(MPCalculationSetting.AS_CONVERSION);
        int jumpingMP = entity.getJumpMP(MPCalculationSetting.AS_CONVERSION);

        report.addLine("Walking MP:", Integer.toString(walkingMP));
        report.addLine("Jumping MP:", Integer.toString(jumpingMP));
        String movementCode = getMovementCode(conversionData);
        element.setPrimaryMovementMode(movementCode);

        if ((walkingMP > jumpingMP) || (jumpingMP == 0)) {
            result.put(movementCode, walkingMP * 2);
            report.addLine("Walking MP > Jumping MP", walkingMP + " x 2", walkingMP * 2 + "\"" + movementCode);
        } else {
            result.put(movementCode.equals("v") ? movementCode : "j", jumpingMP * 2);
            report.addLine("Walking MP <= Jumping MP", jumpingMP + " x 2", jumpingMP * 2 + "\"j");
        }

        addUMUMovement(result, conversionData);
        return result;
    }

    private static void addUMUMovement(Map<String, Integer> moves, ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity();
        CalculationReport report = conversionData.conversionReport();

        int umu = entity.getAllUMUCount();
        if (umu > 0) {
            moves.put("s", umu * 2);
            report.addLine("UMU MP:", umu + " x 2", umu * 2 + "\"s");
        }
    }

    /** Returns true if the given entity has a Supercharger, regardless of its state (convert as if undamaged). */
    private static boolean hasSupercharger(Entity entity) {
        return entity.getMisc().stream()
              .map(Mounted::getType)
              .anyMatch(m -> (m.hasFlag(MiscType.F_MASC) && m.hasFlag(MiscTypeFlag.S_SUPERCHARGER)));
    }

    /** Returns true if the given entity has a Jet Booster, regardless of its state (convert as if undamaged). */
    private static boolean hasJetBooster(Entity entity) {
        return entity.getMisc().stream()
              .map(Mounted::getType)
              .anyMatch(m -> (m.hasFlag(MiscType.F_MASC) && m.hasFlag(MiscTypeFlag.S_JET_BOOSTER)));
    }

    /** Returns true if the given entity has a ProtoMek Myomer Booster. */
    private static boolean hasMyomerBooster(Entity entity) {
        return (entity instanceof ProtoMek) && entity.hasWorkingMisc(EquipmentTypeLookup.PROTOMEK_MYOMER_BOOSTER);
    }

    /** Returns true if the given entity is a Mek and has MASC, regardless of its state (convert as if undamaged). */
    private static boolean hasMekMASC(Entity entity) {
        return (entity instanceof Mek)
              && entity.getMisc().stream()
              .map(Mounted::getType)
              .anyMatch(m -> (m.hasFlag(MiscType.F_MASC) && !m.hasFlag(MiscTypeFlag.S_SUPERCHARGER)));
    }

    /**
     * Returns true if the given entity has a movement reducing shield, regardless of its state (convert as if
     * undamaged).
     */
    private static boolean hasMPReducingShield(Entity entity) {
        return entity.getMisc().stream()
              .map(Mounted::getType)
              .anyMatch(m -> (m.hasFlag(MiscType.F_CLUB)
                    && (m.hasAnyFlag(MiscTypeFlag.S_SHIELD_LARGE, MiscTypeFlag.S_SHIELD_MEDIUM))));
    }

    /** Returns the AlphaStrike movement type code letter such as "v" for VTOL. */
    public static String getMovementCode(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity();
        CalculationReport report = conversionData.conversionReport();
        String type = "Movement Code:";

        if (entity instanceof QuadVee) {
            if (((QuadVee) entity).getMotiveType() == QuadVee.MOTIVE_TRACK) {
                report.addLine(type, "Tracked Quadvee", "qt");
                return "qt";
            } else {
                report.addLine(type, "Wheeled Quadvee", "qw");
                return "qw";
            }
        }
        switch (entity.getMovementMode()) {
            case NONE:
                report.addLine(type, "No movement mode", "No code");
                return "";
            case BIPED:
                report.addLine(type, "Biped Mek", "No code");
                return "";
            case QUAD:
                report.addLine(type, "Quad", "No code");
                return "";
            case TRIPOD:
                report.addLine(type, "Tripod", "No code");
                return "";
            case TRACKED:
                report.addLine(type, "Tracked", "t");
                return "t";
            case WHEELED:
                report.addLine(type, "Wheeled", "w");
                return "w";
            case HOVER:
                report.addLine(type, "Hover", "h");
                return "h";
            case VTOL:
                report.addLine(type, "VTOL", "v");
                return "v";
            case NAVAL:
                report.addLine(type, "Naval", "n");
                return "n";
            case HYDROFOIL:
                report.addLine(type, "Hydrofoil", "n");
                return "n";
            case SUBMARINE:
                report.addLine(type, "Submarine", "s");
                return "s";
            case INF_UMU:
                report.addLine(type, "UMU", "s");
                return "s";
            case INF_LEG:
                report.addLine(type, "Infantry (walking)", "f");
                return "f";
            case INF_MOTORIZED:
                report.addLine(type, "Infantry (motorized)", "m");
                return "m";
            case INF_JUMP:
                if (entity.getJumpMP(MPCalculationSetting.AS_CONVERSION) > 0) {
                    report.addLine(type, "Infantry (jump)", "j");
                    return "j";
                } else {
                    // BA with DWP/DMP equipment may have their jump MP reduced to 0 and should count as leg
                    report.addLine(type, "Infantry (jump MP reduced to 0)", "f");
                    return "f";
                }
            case WIGE:
                report.addLine(type, "Wige", "g");
                return "g";
            case RAIL:
                report.addLine(type, "Rail", "r");
                return "r";
            case AERODYNE:
                report.addLine(type, "Aerodyne", "a");
                return "a";
            case SPHEROID:
                report.addLine(type, "Spheroid", "p");
                return "p";
            default:
                report.addLine(type, "Error: Cannot convert", "");
                return "ERROR";
        }
    }

    /**
     * Determines the element's TMM, AlphaStrike Companion Errata v1.4, p.8
     */
    static int convertTMM(ASConverter.ConversionData conversionData) {
        CalculationReport report = conversionData.conversionReport();
        AlphaStrikeElement element = conversionData.element();
        if (element.isAerospace()) {
            return 0;
        }

        int base = element.getPrimaryMovementValue();
        if (element.isInfantry()) {
            for (String moveMode : element.getMovementModes()) {
                if (!moveMode.equals("f")) {
                    base = element.getMovement(moveMode);
                    break;
                }
            }
        }
        return ASConverter.tmmForMovement(base, report);
    }

    // Make non-instantiable
    private ASMovementConverter() {}
}
