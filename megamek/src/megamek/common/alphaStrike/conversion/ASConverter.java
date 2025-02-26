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
import megamek.common.alphaStrike.ASArcs;
import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

import java.util.Objects;

/**
 * Static AlphaStrike Conversion class; contains all information for conversion
 * except for some weapon specifics
 * handled in WeaponType/AmmoType/MiscType.
 *
 * @author neoancient
 * @author Simon (Juliez)
 */
public final class ASConverter {
    private static final MMLogger logger = MMLogger.create(ASConverter.class);

    // TODO: LG, SLG, VLG support vehicles, MS

    /**
     * Performs Alpha Strike conversion for the MekSummaryCache (without trying to
     * get a clean unit first,
     * without storing a conversion report and without considering pilot skill).
     */
    public static AlphaStrikeElement convertForMekCache(Entity entity) {
        return performConversion(entity, false, new DummyCalculationReport(), entity.getCrew());
    }

    /**
     * Performs Alpha Strike conversion for use in MML (without trying to get a
     * clean unit first,
     * without considering pilot skill but with storing a conversion report).
     */
    public static AlphaStrikeElement convertInMML(Entity entity, CalculationReport report) {
        return performConversion(entity, false, report, entity.getCrew());
    }

    public static AlphaStrikeElement convert(Entity entity, CalculationReport conversionReport) {
        return standardConversion(entity, false, true, conversionReport);
    }

    public static AlphaStrikeElement convert(Entity entity) {
        return standardConversion(entity, false, true, new DummyCalculationReport());
    }

    public static AlphaStrikeElement convert(Entity entity, boolean includePilot) {
        return standardConversion(entity, false, includePilot, new DummyCalculationReport());
    }

    public static AlphaStrikeElement convert(Entity entity, boolean includePilot, CalculationReport conversionReport) {
        return standardConversion(entity, false, includePilot, conversionReport);
    }

    /**
     * Converts the given entity to Alpha Strike, keeping the references to the original Entity
     * @param entity The entity to convert
     * @return The converted Alpha Strike element
     */
    public static AlphaStrikeElement convertAndKeepRefs(Entity entity) {
        return standardConversion(entity, true, true, new DummyCalculationReport());
    }

    private static AlphaStrikeElement standardConversion(Entity entity, boolean keepRefs, boolean includePilot,
            CalculationReport conversionReport) {
        Entity undamagedEntity = getUndamagedEntity(entity);
        if (undamagedEntity == null) {
            logger.error("Could not obtain clean Entity for AlphaStrike conversion.");
            return null;
        }
        if (entity.getGame() != null) {
            undamagedEntity.setGame(entity.getGame());
        }
        if (keepRefs) {
            undamagedEntity.setId(entity.getId());
            undamagedEntity.setOwner(entity.getOwner());
        }
        return performConversion(undamagedEntity, includePilot, conversionReport, entity.getCrew());
    }

    private static AlphaStrikeElement performConversion(Entity entity, boolean includePilot,
            CalculationReport conversionReport, Crew originalCrew) {
        Objects.requireNonNull(entity);
        if (!canConvert(entity)) {
            logger.error("Cannot convert this type of Entity: " + entity.getShortName());
            return null;
        }

        var element = new AlphaStrikeElement();
        final ConversionData conversionData = new ConversionData(entity, element, conversionReport);

        // Basic info
        element.setName(entity.getShortName());
        element.setQuirks(entity.getQuirks());
        element.setModel(entity.getModel());
        element.setChassis(entity.getFullChassis());
        element.setMulId(entity.getMulId());
        element.setRole(entity.getRole());
        element.setFluff(entity.getFluff());
        element.setId(entity.getId());

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
        if ((entity instanceof Mek) && ((Mek) entity).isIndustrial()) {
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
            if (element.isConventionalInfantry() || element.isProtoMek()) {
                // CI and PM have no Piloting skill and so use only their Gunnery
                element.setSkill(originalCrew.getGunnery());
            } else {
                element.setSkill((originalCrew.getPiloting() + originalCrew.getGunnery()) / 2);
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
        if (entity instanceof TripodMek) {
            element.getSpecialAbilities().setSUA(BattleForceSUA.TRI);
        } else if (entity instanceof QuadMek) {
            element.getSpecialAbilities().setSUA(BattleForceSUA.QUAD);
        } else if ((entity instanceof SmallCraft) && !(entity instanceof Dropship) && !((IAero) entity).isSpheroid()) {
            element.getSpecialAbilities().setSUA(BattleForceSUA.AERODYNESC);
        }
        ASPointValueConverter pvConverter = ASPointValueConverter.getPointValueConverter(element, conversionReport);
        element.setPointValue(pvConverter.getSkillAdjustedPointValue());
        element.setBasePointValue(pvConverter.getBasePointValue());
        element.setConversionReport(conversionReport);
        return element;
    }

    /**
     * A helper class that stores the entity to be converted, the resulting
     * ASElement and the conversion report.
     */
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
     * Returns true if the given entity can be converted to AlphaStrike. This is
     * only
     * false for entities of some special types such as TeleMissile.
     * GunEmplacement is being allowed conversion as of 50.02 even though its not
     * currently officially supported in rules as written, but it generates a valid unit that can be used in
     * auto-resolution and other parts of the game. (Luana Coppio)
     * Also returns false if entity is null.
     */
    public static boolean canConvert(@Nullable Entity entity) {
        return (entity != null) && !((entity instanceof TeleMissile) || (entity instanceof FighterSquadron)
                || (entity instanceof EscapePods) || (entity instanceof EjectedCrew));
    }

    /**
     * Returns the TMM for the given movement value in inches. Writes report
     * entries.
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

    /**
     * @return Retrieves a fresh (undamaged and unmodified) copy of the given entity.
     */
    public static @Nullable Entity getUndamagedEntity(Entity entity) {
        try {
            MekSummary ms = MekSummaryCache.getInstance().getMek(entity.getShortNameRaw());
            return new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (Exception e) {
            logger.error("Could not obtain clean Entity for entity {}.", entity, e);
        }
        return null;
    }

    /**
     * Returns the given number, rounded up to the nearest integer, based on the
     * first decimal only.
     */
    public static int roundUp(double number) {
        return (int) Math.round(number + 0.4);
    }

    public static int toInt(ASArcs arc) {
        return switch (arc) {
            case LEFT -> 1;
            case RIGHT -> 2;
            case REAR -> 3;
            default -> 0;
        };
    }

    /**
     * Re-calculates those values of the element that are purely calculated from its
     * other values without
     * needing the original TW unit. These are TMM, threshold and PV. This means
     * that the conversion methods
     * called herein do not need the entity and this, entity in conversionData may
     * be null.
     */
    static void updateCalculatedValues(ConversionData conversionData) {
        CalculationReport report = conversionData.conversionReport;
        AlphaStrikeElement element = conversionData.element;
        element.setTMM(ASMovementConverter.convertTMM(conversionData));
        element.setThreshold(ASArmStrConverter.convertThreshold(conversionData));
        ASPointValueConverter pvConverter = ASPointValueConverter.getPointValueConverter(element, report);
        element.setPointValue(pvConverter.getSkillAdjustedPointValue());
    }

    /**
     * Re-calculates those values of the element that are purely calculated from its
     * other values without
     * needing the original TW unit. These are TMM, threshold and PV. May be used
     * e.g. after deserialization.
     */
    public static void updateCalculatedValues(AlphaStrikeElement element) {
        updateCalculatedValues(new ConversionData(null, element, new DummyCalculationReport()));
    }

    private ASConverter() {
    }
}
