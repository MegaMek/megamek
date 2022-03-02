/*
 * Copyright (c) 2021, 2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.swing.calculationReport.HTMLCalculationReport;
import megamek.common.*;
import megamek.common.alphaStrike.BattleForceElement.WeaponLocation;
import org.apache.logging.log4j.LogManager;

import java.util.Objects;

import static megamek.common.alphaStrike.BattleForceElement.RANGEBANDS_SMLE;

/**
 * Static AlphaStrike Conversion class; contains all information for conversion except for some weapon specifics
 * handled in WeaponType/AmmoType/MiscType.
 *
 * @author neoancient
 * @author Simon (Juliez)
 */
public final class ASConverter {

    public static AlphaStrikeElement convert(Entity entity) {
        return convert(entity, false);
    }

    public static AlphaStrikeElement convert(Entity entity, boolean includePilot) {
        Objects.requireNonNull(entity);
        if (!canConvert(entity)) {
            LogManager.getLogger().error("Cannot convert this type of Entity: " + entity.getShortName());
            return null; 
        }
        Entity undamagedEntity = getUndamagedEntity(entity);
        if (undamagedEntity == null) {
            LogManager.getLogger().error("Could not obtain clean Entity for AlphaStrike conversion.");
            return null;
        }

        FlexibleCalculationReport conversionReport = new FlexibleCalculationReport();
        var element = new AlphaStrikeElement();
        final ConversionData conversionData = new ConversionData(undamagedEntity, element, conversionReport);

        // Basic info
        element.name = undamagedEntity.getShortName();
        element.setQuirks(undamagedEntity.getQuirks());
        element.model = undamagedEntity.getModel();
        element.chassis = undamagedEntity.getChassis();
        element.setMulId(undamagedEntity.getMulId());
        element.role = UnitRoleHandler.getRoleFor(undamagedEntity);

        conversionReport.addHeader("AlphaStrike Conversion for " + undamagedEntity.getShortName());
        conversionReport.addSubHeader("Basic Info:");
        conversionReport.addLine("Model:", element.model);
        conversionReport.addLine("Chassis:", element.chassis);
        conversionReport.addLine("MUL ID:", Integer.toString(element.getMulId()));
        conversionReport.addLine("Unit Role:", element.role.toString());

        // Type
        element.asUnitType = ASUnitType.getUnitType(undamagedEntity);
        String unitType = Entity.getEntityTypeName(undamagedEntity.getEntityType());
        if ((undamagedEntity instanceof Mech) && ((Mech) undamagedEntity).isIndustrial()) {
            unitType += " / Industrial";
        }
        conversionReport.addLine("Unit Type:", unitType, element.asUnitType.toString());

        // Size
        element.size = ASSizeConverter.convertSize(conversionData);

        // Skill
        if (includePilot) {
            element.setSkill((entity.getCrew().getPiloting() + entity.getCrew().getGunnery()) / 2);
        }
        conversionReport.addLine("Skill:", Integer.toString(element.getSkill()));

        element.movement = ASMovementConverter.convertMovement(conversionData);
        element.tmm = ASMovementConverter.convertTMM(conversionData);
        element.armor = ASArmStrConverter.convertArmor(conversionData);
        element.structure = ASArmStrConverter.convertStructure(conversionData);
        element.threshold = ASArmStrConverter.convertThreshold(conversionData);
        ASSpecialAbilityConverter.convertSpecialUnitAbilities(conversionData);
        initWeaponLocations(undamagedEntity, element);
        element.heat = new int[element.rangeBands];
        ASDamageConverter.convertDamage(undamagedEntity, element);
        ASSpecialAbilityConverter.finalizeSpecials(element);
        element.points = ASPointValueConverter.getPointValue(conversionData);
        ASPointValueConverter.adjustPVforSkill(element);
        System.out.println(conversionReport);
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
     */
    public static boolean canConvert(Entity entity) {
        return !((entity instanceof TeleMissile) || (entity instanceof FighterSquadron)
                || (entity instanceof EscapePods) || (entity instanceof EjectedCrew)
                || (entity instanceof ArmlessMech) || (entity instanceof GunEmplacement));
    }

    /**
     * Returns the TMM for the given movement value in inches.
     * AlphaStrike Companion Errata v1.4, p.8
     */
    static int tmmForMovement(int movement, ConversionData conversionData) {
        CalculationReport report = conversionData.conversionReport;
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

    private static void initWeaponLocations(Entity entity, AlphaStrikeElement result) {
        if (entity instanceof Aero) {
            result.rangeBands = RANGEBANDS_SMLE;
        }
        result.weaponLocations = new WeaponLocation[entity.getNumBattleForceWeaponsLocations()];
        result.locationNames = new String[result.weaponLocations.length];
        for (int loc = 0; loc < result.locationNames.length; loc++) {
            result.weaponLocations[loc] = result.new WeaponLocation();
            result.locationNames[loc] = entity.getBattleForceLocationName(loc);
        }
    }

    /** Returns the given number, rounded up to the nearest integer or x.5, based on the first decimal only. */
    static double roundUpToHalf(double number) {
        return 0.5 * Math.round(number * 2 + 0.3);
    }

    /** Returns the given number, rounded up to the nearest integer, based on the first decimal only. */
    static int roundUp(double number) {
        return (int) Math.round(number + 0.4); 
    }

}
