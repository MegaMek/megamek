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
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.*;
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

    public static AlphaStrikeElement convert(Entity entity, CalculationReport conversionReport) {
        return convert(entity, true, conversionReport);
    }

    public static AlphaStrikeElement convert(Entity entity) {
        return convert(entity, true, new DummyCalculationReport());
    }

    public static AlphaStrikeElement convert(Entity entity, boolean includePilot) {
        return convert(entity, includePilot, new DummyCalculationReport());
    }

    public static AlphaStrikeElement convert(Entity entity, boolean includePilot, CalculationReport conversionReport) {
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

        var element = new AlphaStrikeElement();
        final ConversionData conversionData = new ConversionData(undamagedEntity, element, conversionReport);

        // Basic info
        element.setName(undamagedEntity.getShortName());
        element.setQuirks(undamagedEntity.getQuirks());
        element.setModel(undamagedEntity.getModel());
        element.setChassis(undamagedEntity.getChassis());
        element.setMulId(undamagedEntity.getMulId());
        element.setRole(UnitRoleHandler.getRoleFor(undamagedEntity));

        conversionReport.addHeader("AlphaStrike Conversion for " + undamagedEntity.getShortName());
        conversionReport.addEmptyLine();

        conversionReport.addSubHeader("Basic Info:");
        conversionReport.addLine("Chassis:", element.getChassis(), "");
        conversionReport.addLine("Model:", element.getModel(), "");
        conversionReport.addLine("MUL ID:", Integer.toString(element.getMulId()), "");
        conversionReport.addLine("Unit Role:", element.getRole().toString(), "");

        // Type
        element.setType(ASUnitType.getUnitType(undamagedEntity));
        String unitType = Entity.getEntityTypeName(undamagedEntity.getEntityType());
        if ((undamagedEntity instanceof Mech) && ((Mech) undamagedEntity).isIndustrial()) {
            unitType += " / Industrial";
        }
        conversionReport.addLine("Unit Type:", unitType, element.getType().toString());

        // Size
        element.setSize(ASSizeConverter.convertSize(conversionData));

        // Skill
        if (includePilot) {
            element.setSkill((entity.getCrew().getPiloting() + entity.getCrew().getGunnery()) / 2);
        }
        conversionReport.addLine("Skill:", Integer.toString(element.getSkill()));

        element.setMovement(ASMovementConverter.convertMovement(conversionData));
        element.setTMM(ASMovementConverter.convertTMM(conversionData));
        element.setArmor(ASArmStrConverter.convertArmor(conversionData));
        element.setStructure(ASArmStrConverter.convertStructure(conversionData));
        element.setThreshold(ASArmStrConverter.convertThreshold(conversionData));
        ASSpecialAbilityConverter.convertSpecialUnitAbilities(conversionData);
        initWeaponLocations(undamagedEntity, element);
        element.heat = new int[element.getRangeBands()];
        ASDamageConverter.convertDamage(conversionData);
        ASSpecialAbilityConverter.finalizeSpecials(element);
        element.setPointValue(ASPointValueConverter.getPointValue(conversionData));
        ASPointValueConverter.adjustPVforSkill(element, conversionReport);
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

    private static void initWeaponLocations(Entity entity, AlphaStrikeElement result) {
        result.weaponLocations = new WeaponLocation[entity.getNumBattleForceWeaponsLocations()];
        result.locationNames = new String[result.weaponLocations.length];
        for (int loc = 0; loc < result.locationNames.length; loc++) {
            result.weaponLocations[loc] = new WeaponLocation();
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

    /**
     * This class is used to gather weapon and damage values for conversion purposes.
     */
    public static class WeaponLocation {
        List<Double> standardDamage = new ArrayList<>();
        Map<Integer,List<Double>> specialDamage = new HashMap<>();
        List<Integer> heatDamage = new ArrayList<>();
        double indirect;

        WeaponLocation() {
            while (standardDamage.size() < 4) {
                standardDamage.add(0.0);
            }
            while (heatDamage.size() < 4) {
                heatDamage.add(0);
            }
        }

        public boolean hasStandardDamage() {
            return standardDamage.stream().mapToDouble(Double::doubleValue).sum() > 0;
        }

        public boolean hasDamage() {
            return hasStandardDamage()
                    || specialDamage.keySet().stream().anyMatch(this::hasDamageClass);
        }

        public boolean hasDamageClass(int damageClass) {
            if (damageClass == WeaponType.BFCLASS_FLAK) {
                return specialDamage.containsKey(damageClass)
                        && specialDamage.get(damageClass).stream().mapToDouble(Double::doubleValue).sum() > 0;
            } else {
                return specialDamage.containsKey(damageClass)
                        && specialDamage.get(damageClass).get(1) >= 1;
            }
        }

        public void addDamage(int rangeIndex, double val) {
            addDamage(standardDamage, rangeIndex, val);
        }

        public void addDamage(int damageClass, int rangeIndex, double val) {
            if (!specialDamage.containsKey(damageClass)) {
                specialDamage.put(damageClass, new ArrayList<>());
            }
            addDamage(specialDamage.get(damageClass), rangeIndex, val);
        }

        public double getIF() {
            return indirect;
        }

        public void addIF(double val) {
            indirect += val;
        }

        private void addDamage(List<Double> damage, int rangeIndex, double val) {
            while (damage.size() <= rangeIndex) {
                damage.add(0.0);
            }
            damage.set(rangeIndex, damage.get(rangeIndex) + val);
        }

    }

}
