/*
 *
 *  * Copyright (c) 10.07.22, 14:15 - The MegaMek Team. All Rights Reserved.
 *  *
 *  * This file is part of MegaMek.
 *  *
 *  * MegaMek is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MegaMek is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package megamek.common.alphaStrike.conversion;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.AlphaStrikeElement;
import org.apache.logging.log4j.LogManager;

import java.util.Locale;

final class ASArmStrConverter {

    /** Mech Structure, AlphaStrike Companion, p.98 */
    private final static int[][] AS_MECH_STRUCTURE = new int[][] {
            { 1, 1, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 8, 8, 8, 8, 9, 9, 10, 10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15 },
            { 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 7, 7, 7, 8, 8, 9, 10, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20 },
            { 1, 1, 1, 2, 2, 2, 2, 3, 3, 4, 4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 11, 11, 12, 12 },
            { 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10 },
            { 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 7, 7, 7, 7, 8 ,8, 8, 8, 8, 9 },
            { 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 8 },
            { 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6 },
            { 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 7, 7 },
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5 }
    };

    static int convertArmor(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity;
        CalculationReport report = conversionData.conversionReport;
        report.addEmptyLine();
        report.addSubHeader("Armor:");

        if ((entity instanceof Infantry) && !(entity instanceof BattleArmor)) {
            double divisor = ((Infantry) entity).calcDamageDivisor();
            report.addLine("Infantry Damage Divisor:", "", divisor);
            if (((Infantry) entity).isMechanized()) {
                divisor /= 2.0;
                report.addLine("Mechanized", "/ 2", "= ", divisor);
            }
            int finalArmor = (int) Math.round(divisor / 15.0d * ((Infantry) entity).getShootingStrength());
            report.addLine("Armor:", "#Men x Divisor / 2", "= " + finalArmor);
            return finalArmor;
        }

        double armorPoints = 0;

        for (int loc = 0; loc < entity.locations(); loc++) {
            String calculation = "";
            double armorMod = 1;
            switch (entity.getArmorType(loc)) {
                case EquipmentType.T_ARMOR_COMMERCIAL:
                    armorMod = .5;
                    calculation = "0.5 (Commercial) x ";
                    break;
                case EquipmentType.T_ARMOR_FERRO_LAMELLOR:
                    armorMod = 1.2;
                    calculation = "1.2 (Ferro-Lamellor) x ";
                    break;
                case EquipmentType.T_ARMOR_HARDENED:
                    armorMod = 2;
                    calculation = "2 (Hardened) x ";
                    break;
            }

            if ((entity.getBARRating(0) < 9) && (entity.getArmorType(loc) != EquipmentType.T_ARMOR_COMMERCIAL)) {
                armorMod *= 0.1 * entity.getBARRating(0);
                calculation += 0.1 * entity.getBARRating(0) + " x ";
            }

            // Some empty locations report -1 armor!
            if (entity.getArmor(loc) > 0) {
            armorPoints += Math.max(0, armorMod * entity.getArmor(loc));
            report.addLine(entity.getLocationAbbr(loc),
                    calculation.isBlank() ? "" : calculation + entity.getArmor(loc),
                    "", + armorMod * entity.getArmor(loc));
            }
            if (entity.hasRearArmor(loc) && (entity.getArmor(loc, true) > 0)) {
                armorPoints += armorMod * entity.getArmor(loc, true);
                report.addLine(entity.getLocationAbbr(loc) + "(R)",
                        calculation.isBlank() ? "" : calculation + entity.getArmor(loc, true),
                        "", armorMod * entity.getArmor(loc, true));
            }
        }

        if (entity.hasModularArmor()) {
            // Modular armor is always "regular" armor
            int count = (int) entity.getEquipment().stream()
                    .filter(m -> m.getType() instanceof MiscType)
                    .filter(m -> m.getType().hasFlag(MiscType.F_MODULAR_ARMOR))
                    .count();
            armorPoints += 10 * count;
            report.addLine("Modular Armor", "10 x " + count, "+ " + 10 * count);
        }

        String displayArmorPoints = fmt(armorPoints);

        if (entity.isCapitalScale()) {
            int finalArmor = (int) Math.round(armorPoints * 0.33);
            report.addLine("Final Armor Value", "Capital: 0.33 x " + displayArmorPoints + ", round normal",
                    "= " + finalArmor);
            return finalArmor;
        } else {
            int finalArmor = (int) Math.round(armorPoints / 30);
            report.addLine("Final Armor Value", displayArmorPoints + " / 30" + ", round normal",
                    "= " + finalArmor);
            return finalArmor;
        }
    }

    public static String fmt(double d) {
        if (d == (long) d) {
            return String.format(Locale.US, "%1$,.0f", d);
        } else {
            return String.format(Locale.US, "%1$,.1f", d);
        }
    }

    /**
     * Determines the Aerospace Armor Threshold, AlphaStrike Companion p.95
     */
    static int convertThreshold(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity;
        CalculationReport report = conversionData.conversionReport;
        AlphaStrikeElement element = conversionData.element;

        if (entity instanceof Aero) {
            int arcs = entity.isFighter() ? 1 : 4;
            int threshold = ASConverter.roundUp((double) element.getFullArmor() / 3 / arcs);
            report.addEmptyLine();
            report.addSubHeader("Threshold:");

            report.addLine("Threshold",
                    element.getFullArmor() + " / 3" + (element.usesArcs() ? " / " + arcs : "") + ", round up",
                    "= " + threshold);
            return threshold;
        } else {
            return -1;
        }
    }

    /**
     * Calculates the Structure value, AlphaStrike Companion p.97
     */
    static int convertStructure(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity;
        CalculationReport report = conversionData.conversionReport;
        report.addEmptyLine();
        report.addSubHeader("Structure:");

        int structure;
        if (entity instanceof Mech) {
            structure = AS_MECH_STRUCTURE[getEngineIndex(conversionData)][getWeightIndex(entity)];
            report.addLine("Structure ", "", "" + structure);
            if (entity.getStructureType() == EquipmentType.T_STRUCTURE_COMPOSITE) {
                structure = (int) Math.ceil(structure * 0.5);
                report.addLine("Composite", "x 0.5, round up", "= " + structure);
            } else if (entity.getStructureType() == EquipmentType.T_STRUCTURE_REINFORCED) {
                structure *= 2;
                report.addLine("Reinforced", "x 2", "= " + structure);
            }
            return structure;
        } else if (entity instanceof Warship) {
            structure = ((Warship) entity).getSI();
            report.addLine("WS", "(SI)", "", structure);
            return structure;
        } else if (entity instanceof BattleArmor) {
            report.addLine("BA", "2");
            return 2;
        } else if ((entity instanceof Infantry) || (entity instanceof Jumpship)
                || (entity instanceof Protomech)) {
            report.addLine("CI, JS or PM", "1");
            return 1;
        } else if (entity instanceof Tank) {
            int divisor = 10;
            if (entity.isSupportVehicle()) {
                switch (entity.getMovementMode()) {
                    case NAVAL:
                    case HYDROFOIL:
                    case SUBMARINE:
                        if (entity.getWeight() >= 30000.5) {
                            divisor = 35;
                        } else if (entity.getWeight() >= 12000.5) {
                            divisor = 30;
                        } else if (entity.getWeight() >= 6000.5) {
                            divisor = 25;
                        } else if (entity.getWeight() >= 500.5) {
                            divisor = 20;
                        } else if (entity.getWeight() >= 300.5) {
                            divisor = 15;
                        }
                        break;
                    default:
                }
            }
            double struct = 0;
            for (int i = 0; i < entity.getLocationNames().length; i++) {
                struct += entity.getInternal(i);
            }
            return (int) Math.ceil(struct / divisor);
        } else if (entity instanceof Aero) {
            report.addLine("Aero", ((Aero) entity).getSI() + " x 0.5, round up", "= " + (int) Math.ceil(0.5 * ((Aero) entity).getSI()));
            return (int) Math.ceil(0.5 * ((Aero) entity).getSI());
        }

        // Error: should not arrive here
        return -1;
    }

    private static int getWeightIndex(Entity entity) {
        return ((int) entity.getWeight() / 5) - 2;
    }

    private static int getEngineIndex(ASConverter.ConversionData conversionData) {
        Entity entity = conversionData.entity;
        CalculationReport report = conversionData.conversionReport;

        if (entity.getEngine().isClan()) {
            if (entity.getEngine().hasFlag(Engine.LARGE_ENGINE)) {
                switch (entity.getEngine().getEngineType()) {
                    case Engine.XL_ENGINE:
                        report.addLine("Clan Large XL Engine", "");
                        return 4;
                    case Engine.XXL_ENGINE:
                        report.addLine("Clan Large XXL Engine", "");
                        return 7;
                }
            } else {
                switch (entity.getEngine().getEngineType()) {
                    case Engine.XL_ENGINE:
                        report.addLine("Clan XL Engine", "");
                        return 3;
                    case Engine.XXL_ENGINE:
                        report.addLine("Clan XXL Engine", "");
                        return 5;
                    default:
                        report.addLine("Clan Fusion or other Engine", "");
                        return 0;
                }
            }
        } else {
            if (entity.getEngine().hasFlag(Engine.LARGE_ENGINE)) {
                switch (entity.getEngine().getEngineType()) {
                    case Engine.XL_ENGINE:
                    case Engine.LIGHT_ENGINE:
                        report.addLine("IS Large XL or Light Engine", "");
                        return 4;
                    case Engine.XXL_ENGINE:
                        report.addLine("IS Large XXL Engine", "");
                        return 8;
                    default:
                        report.addLine("IS Large Fusion Engine", "");
                        return 2;
                }
            } else {
                switch (entity.getEngine().getEngineType()) {
                    case Engine.XL_ENGINE:
                        report.addLine("IS XL Engine", "");
                        return 4;
                    case Engine.COMPACT_ENGINE:
                        report.addLine("IS Compact Engine", "");
                        return 1;
                    case Engine.LIGHT_ENGINE:
                        report.addLine("IS Light Engine", "");
                        return 3;
                    case Engine.XXL_ENGINE:
                        report.addLine("IS CXL Engine", "");
                        return 6;
                    default:
                        report.addLine("IS Fusion or other Engine", "");
                        return 0;
                }
            }
        }
        report.addLine("Unknown Engine", "");
        LogManager.getLogger().error("Mech Engine type cannot be converted!");
        return -1;
    }

    // Make non-instantiable
    private ASArmStrConverter() { }
}
