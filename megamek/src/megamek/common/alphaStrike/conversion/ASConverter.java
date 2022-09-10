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
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import java.util.*;

/**
 * Static AlphaStrike Conversion class; contains all information for conversion except for some weapon specifics
 * handled in WeaponType/AmmoType/MiscType.
 *
 * @author neoancient
 * @author Simon (Juliez)
 */
public final class ASConverter {

    //TODO: LG, SLG, VLG support vehicles, MS

    public static AlphaStrikeElement convertForMechCache(Entity entity) {
        return performConversion(entity, false, new DummyCalculationReport());
    }

    public static AlphaStrikeElement convert(Entity entity, CalculationReport conversionReport) {
        return standardConversion(entity, true, conversionReport);
    }

    public static AlphaStrikeElement convert(Entity entity) {
        return standardConversion(entity, true, new DummyCalculationReport());
    }

    public static AlphaStrikeElement convert(Entity entity, boolean includePilot) {
        return standardConversion(entity, includePilot, new DummyCalculationReport());
    }

    public static AlphaStrikeElement convert(Entity entity, boolean includePilot, CalculationReport conversionReport) {
        return standardConversion(entity, includePilot, conversionReport);
    }

    private static AlphaStrikeElement standardConversion(Entity entity, boolean includePilot,
                                                             CalculationReport conversionReport) {
        Entity undamagedEntity = getUndamagedEntity(entity);
        if (undamagedEntity == null) {
            LogManager.getLogger().error("Could not obtain clean Entity for AlphaStrike conversion.");
            return null;
        }
        return performConversion(undamagedEntity, includePilot, conversionReport);
    }

    private static AlphaStrikeElement performConversion(Entity entity, boolean includePilot, CalculationReport conversionReport) {
        Objects.requireNonNull(entity);
        if (!canConvert(entity)) {
            LogManager.getLogger().error("Cannot convert this type of Entity: " + entity.getShortName());
            return null; 
        }

        var element = new AlphaStrikeElement();
        final ConversionData conversionData = new ConversionData(entity, element, conversionReport);

        // Basic info
        element.setName(entity.getShortName());
        element.setQuirks(entity.getQuirks());
        element.setModel(entity.getModel());
        element.setChassis(entity.getChassis());
        element.setMulId(entity.getMulId());
        element.setRole(UnitRoleHandler.getRoleFor(entity));

        if (entity.getShortName().length() < 15) {
            conversionReport.addHeader("Alpha Strike Conversion for " + entity.getShortName());
        } else {
            conversionReport.addHeader("Alpha Strike Conversion for");
            conversionReport.addHeader(entity.getShortName());
        }
        conversionReport.addEmptyLine();

        conversionReport.addSubHeader("Basic Info:");
        conversionReport.addLine("Chassis:", element.getChassis(), "");
        conversionReport.addLine("Model:", element.getModel(), "");
        conversionReport.addLine("MUL ID:", Integer.toString(element.getMulId()), "");
        conversionReport.addLine("Unit Role:", element.getRole().toString(), "");

        // Type
        element.setType(ASUnitType.getUnitType(entity));
        String unitType = Entity.getEntityTypeName(entity.getEntityType());
        if ((entity instanceof Mech) && ((Mech) entity).isIndustrial()) {
            unitType += " / Industrial";
        }
        conversionReport.addLine("Unit Type:", unitType, element.getASUnitType().toString());
        if (entity instanceof BattleArmor) {
            element.setSquadSize(((BattleArmor) entity).getSquadSize());
        }

        // Size
        element.setSize(ASSizeConverter.convertSize(conversionData));

        // Skill
        if (includePilot) {
            if ((entity instanceof Infantry) && element.isConventionalInfantry()) {
                // CI only use their Gunnery skill, as there is no piloting (only Anti-Mek at a base of 8)
                element.setSkill(entity.getCrew().getGunnery());
            } else {
                //TODO: multi-crew units
                element.setSkill((entity.getCrew().getPiloting() + entity.getCrew().getGunnery()) / 2);
            }
        }
        conversionReport.addLine("Skill:", Integer.toString(element.getSkill()));

        element.setMovement(ASMovementConverter.convertMovement(conversionData));
        if (element.getMovementModes().size() == 1) {
            element.setPrimaryMovementMode(element.getMovementModes().iterator().next());
        }
        element.setTMM(ASMovementConverter.convertTMM(conversionData));
        element.setFullArmor(ASArmStrConverter.convertArmor(conversionData));
        element.setFullStructure(ASArmStrConverter.convertStructure(conversionData));
        element.setThreshold(ASArmStrConverter.convertThreshold(conversionData));
        ASDamageConverter.getASDamageConverter(entity, element, conversionReport).convert();
        ASSpecialAbilityConverter.getConverter(entity, element, conversionReport).processAbilities();
        ASPointValueConverter pvConverter = ASPointValueConverter.getPointValueConverter(element, conversionReport);
        element.setPointValue(pvConverter.getSkillAdjustedPointValue());
        element.setConversionReport(conversionReport);
        return element;
    }

    /** A helper class that stores the entity to be converted, the resulting ASElement and the conversion report. */
    static class ConversionData {
        final Entity entity;
        final AlphaStrikeElement element;
        final CalculationReport conversionReport;

        public ConversionData(Entity entity, AlphaStrikeElement element, CalculationReport conversionReport) {
            this.entity = entity;
            this.element = element;
            this.conversionReport = conversionReport;
        }
    }

    /**
     * Returns true if the given entity can be converted to AlphaStrike. This is only
     * false for entities of some special types such as TeleMissile or GunEmplacement.
     * Also returns false if entity is null.
     */
    public static boolean canConvert(@Nullable Entity entity) {
        return !(entity == null) && !((entity instanceof TeleMissile) || (entity instanceof FighterSquadron)
                || (entity instanceof EscapePods) || (entity instanceof EjectedCrew)
                || (entity instanceof ArmlessMech) || (entity instanceof GunEmplacement));
    }

    /**
     * Returns the TMM for the given movement value in inches. Writes report entries.
     * AlphaStrike Companion Errata v1.4, p.8
     */
    static int tmmForMovement(int movement, CalculationReport report) {
        if (movement > 34) {
            report.addLine("TMM", "of " + movement, "5");
            return 5;
        } else if (movement > 18) {
            report.addLine("TMM", "of " + movement, "4");
            return 4;
        } else if (movement > 12) {
            report.addLine("TMM", "of " + movement, "3");
            return 3;
        } else if (movement > 8) {
            report.addLine("TMM", "of " + movement, "2");
            return 2;
        } else if (movement > 4) {
            report.addLine("TMM", "of " + movement, "1");
            return 1;
        } else {
            report.addLine("TMM", "of " + movement, "0");
            return 0;
        }
    }

    /**
     * Returns the TMM for the given movement value in inches.
     * AlphaStrike Companion Errata v1.4, p.8
     */
    static int tmmForMovement(int movement) {
        return tmmForMovement(movement, new DummyCalculationReport());
    }

    /** Retrieves a fresh (undamaged && unmodified) copy of the given entity. */
    private static Entity getUndamagedEntity(Entity entity) {
        try {
            MechSummary ms = MechSummaryCache.getInstance().getMech(entity.getShortNameRaw());
            return new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Returns the given number, rounded up to the nearest integer, based on the first decimal only. */
    public static int roundUp(double number) {
        return (int) Math.round(number + 0.4); 
    }

    private ASConverter() { }

}
