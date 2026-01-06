/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2015-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.verifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.common.MPCalculationSetting;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.bays.Bay;
import megamek.common.bays.CrewQuartersCargoBay;
import megamek.common.bays.FirstClassQuartersCargoBay;
import megamek.common.bays.SecondClassQuartersCargoBay;
import megamek.common.bays.StandardSeatCargoBay;
import megamek.common.bays.SteerageQuartersCargoBay;
import megamek.common.compute.Compute;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.*;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.interfaces.ITechManager;
import megamek.common.interfaces.ITechnology;
import megamek.common.interfaces.ITechnologyDelegator;
import megamek.common.options.OptionsConstants;
import megamek.common.units.*;
import megamek.common.util.RoundWeight;
import megamek.common.util.StringUtil;
import megamek.common.weapons.flamers.VehicleFlamerWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.lasers.clan.CLChemicalLaserWeapon;
import megamek.logging.MMLogger;

/**
 * Author: arlith
 */
public class TestSupportVehicle extends TestEntity {

    private final static MMLogger logger = MMLogger.create(TestSupportVehicle.class);

    /**
     * Support vehicle categories for construction purposes. Most of these match with a particular movement mode, but
     * the construction rules treat naval and rail units as single types.
     */
    public enum SVType implements ITechnologyDelegator {
        AIRSHIP(300, EntityMovementMode.AIRSHIP,
              new double[] { 0.2, 0.25, 0.3 }, new double[] { 0.004, 0.008, 0.012 }),
        FIXED_WING(200, EntityMovementMode.AERODYNE,
              new double[] { 0.08, 0.1, 0.15 }, new double[] { 0.005, 0.01, 0.015 }),
        HOVERCRAFT(100, EntityMovementMode.HOVER,
              new double[] { 0.2, 0.25, 0.3 }, new double[] { 0.0025, 0.004, 0.007 }),
        NAVAL(300, EntityMovementMode.NAVAL,
              new double[] { 0.12, 0.15, 0.17 }, new double[] { 0.004, 0.007, 0.009 }),
        TRACKED(200, EntityMovementMode.TRACKED,
              new double[] { 0.13, 0.15, 0.25 }, new double[] { 0.006, 0.013, 0.025 }),
        VTOL(60, EntityMovementMode.VTOL,
              new double[] { 0.2, 0.25, 0.3 }, new double[] { 0.002, 0.0025, 0.004 }),
        WHEELED(160, EntityMovementMode.WHEELED,
              new double[] { 0.12, 0.15, 0.18 }, new double[] { 0.0025, 0.0075, 0.015 }),
        WIGE(240, EntityMovementMode.WIGE,
              new double[] { 0.12, 0.15, 0.17 }, new double[] { 0.003, 0.005, 0.006 }),
        RAIL(600, EntityMovementMode.RAIL,
              new double[] { 0.15, 0.2, 0.3 }, new double[] { 0.003, 0.004, 0.005 }),
        SATELLITE(300, EntityMovementMode.STATION_KEEPING,
              new double[] { 0.08, 0.12, 0.16 }, new double[] { 0.1, 0.1, 0.1 });

        /**
         * The maximum tonnage for a large support vehicle of this type; for airship this is the maximum for a medium
         * for now.
         */
        public final int maxTonnage;
        public final EntityMovementMode defaultMovementMode;

        // Used to calculate chassis weight for small, medium, large weight classes
        private final double[] baseChassisValue;

        // Used to calculate engine weight for small, medium, large weight classes
        private final double[] baseEngineValue;

        SVType(int maxTonnage, EntityMovementMode defaultMovementMode, double[] baseChassisValue,
              double[] baseEngineValue) {
            this.maxTonnage = maxTonnage;
            this.defaultMovementMode = defaultMovementMode;
            this.baseChassisValue = baseChassisValue;
            this.baseEngineValue = baseEngineValue;
        }

        static Set<SVType> allBut(SVType type) {
            return EnumSet.complementOf(EnumSet.of(type));
        }

        static Set<SVType> allBut(SVType first, SVType... rest) {
            return EnumSet.complementOf(EnumSet.of(first, rest));
        }

        /**
         * Finds the enum value corresponding to a support vehicle.
         *
         * @param entity The support vehicle
         *
         * @return The support vehicle type, or {@code null} if the entity's movement type is not a valid one for a
         *       support vehicle.
         */
        public static @Nullable SVType getVehicleType(Entity entity) {
            // When grounded, FWS revert to wheeled movement mode; must be independent of
            // this
            if (entity instanceof FixedWingSupport) {
                return FIXED_WING;
            }
            return switch (entity.getMovementMode()) {
                case AIRSHIP -> AIRSHIP;
                case AERODYNE -> FIXED_WING;
                case HOVER -> HOVERCRAFT;
                case TRACKED -> TRACKED;
                case WHEELED -> WHEELED;
                case NAVAL, HYDROFOIL, SUBMARINE -> NAVAL;
                case VTOL -> VTOL;
                case WIGE -> WIGE;
                case RAIL, MAGLEV -> RAIL;
                case STATION_KEEPING -> SATELLITE;
                default -> null;
            };
        }

        /**
         * The base chassis value is used for calculating the chassis weight.
         *
         * @param sizeClass The {@link EntityWeightClass} of the support vehicle.
         *
         * @return The base chassis value. Returns 0 if not a support vehicle weight class.
         */
        public double getBaseChassisValue(int sizeClass) {
            int index = sizeClass - EntityWeightClass.WEIGHT_SMALL_SUPPORT;

            if ((index >= 0) && (index < baseChassisValue.length)) {
                return baseChassisValue[index];
            }

            return 0.0;
        }

        /**
         * The base chassis value is used for calculating the chassis weight.
         *
         * @param supportVehicle A support vehicle
         *
         * @return The base chassis value. Returns 0 if the entity is not a support vehicle.
         */
        public static double getBaseChassisValue(Entity supportVehicle) {
            SVType type = getVehicleType(supportVehicle);

            if (null != type) {
                return type.getBaseChassisValue(supportVehicle.getWeightClass());
            }

            return 0.0;
        }

        /**
         * The base engine value is used for calculating the engine weight.
         *
         * @param sizeClass The {@link EntityWeightClass} of the support vehicle.
         *
         * @return The base engine value. Returns 0 if not a support vehicle weight class.
         */
        public double getBaseEngineValue(int sizeClass) {
            int index = sizeClass - EntityWeightClass.WEIGHT_SMALL_SUPPORT;

            if ((index >= 0) && (index < baseEngineValue.length)) {
                return baseEngineValue[index];
            }

            return 0.0;
        }

        /**
         * The base engine value is used for calculating the engine weight.
         *
         * @param supportVehicle A support vehicle
         *
         * @return The base engine value. Returns 0 if the entity is not a support vehicle.
         */
        public static double getBaseEngineValue(Entity supportVehicle) {
            SVType type = getVehicleType(supportVehicle);

            if (null != type) {
                return type.getBaseEngineValue(supportVehicle.getWeightClass());
            }

            return 0.0;
        }

        @Override
        public ITechnology getTechSource() {
            /*
             * Support vehicle availability advancement can vary with the size class. The
             * small is the least restrictive, so it serves as the baseline for each type
             * as a whole.
             */
            return switch (defaultMovementMode) {
                case AERODYNE, AIRSHIP, STATION_KEEPING ->
                      FixedWingSupport.getConstructionTechAdvancement(defaultMovementMode,
                            EntityWeightClass.WEIGHT_SMALL_SUPPORT);
                case VTOL -> SupportVTOL.getConstructionTechAdvancement(EntityWeightClass.WEIGHT_SMALL_SUPPORT);
                default -> SupportTank.getConstructionTechAdvancement(defaultMovementMode,
                      EntityWeightClass.WEIGHT_SMALL_SUPPORT);
            };
        }
    }

    /**
     * Additional construction data for chassis mods, used to determine whether they are legal for particular units.
     */
    public enum ChassisModification implements ITechnologyDelegator {
        AMPHIBIOUS(1.75, EquipmentTypeLookup.AMPHIBIOUS_CHASSIS_MOD,
              SVType.allBut(SVType.HOVERCRAFT, SVType.NAVAL)),
        ARMORED(1.5, EquipmentTypeLookup.ARMORED_CHASSIS_MOD,
              SVType.allBut(SVType.AIRSHIP)),
        BICYCLE(0.75, EquipmentTypeLookup.BICYCLE_CHASSIS_MOD,
              EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED)),
        CONVERTIBLE(1.1, EquipmentTypeLookup.CONVERTIBLE_CHASSIS_MOD,
              EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED, SVType.TRACKED)),
        DUNE_BUGGY(1.5, EquipmentTypeLookup.DUNE_BUGGY_CHASSIS_MOD,
              EnumSet.of(SVType.WHEELED)),
        ENVIRONMENTAL_SEALING(2.0, EquipmentTypeLookup.SV_ENVIRONMENTAL_SEALING_CHASSIS_MOD,
              EnumSet.allOf(SVType.class)),
        EXTERNAL_POWER_PICKUP(1.1, EquipmentTypeLookup.EXTERNAL_POWER_PICKUP_CHASSIS_MOD,
              EnumSet.of(SVType.RAIL)),
        HYDROFOIL(1.7, EquipmentTypeLookup.HYDROFOIL_CHASSIS_MOD,
              EnumSet.of(SVType.NAVAL)),
        MONOCYCLE(0.5, EquipmentTypeLookup.MONOCYCLE_CHASSIS_MOD,
              EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED), true),
        OFF_ROAD(1.5, EquipmentTypeLookup.OFF_ROAD_CHASSIS_MOD,
              EnumSet.of(SVType.WHEELED)),
        OMNI(1.0, EquipmentTypeLookup.OMNI_CHASSIS_MOD),
        PROP(1.2, EquipmentTypeLookup.PROP_CHASSIS_MOD,
              EnumSet.of(SVType.FIXED_WING)),
        SNOWMOBILE(1.75, EquipmentTypeLookup.SNOWMOBILE_CHASSIS_MOD,
              EnumSet.of(SVType.WHEELED, SVType.TRACKED)),
        STOL(1.5, EquipmentTypeLookup.STOL_CHASSIS_MOD,
              EnumSet.of(SVType.FIXED_WING)),
        SUBMERSIBLE(1.8, EquipmentTypeLookup.SUBMERSIBLE_CHASSIS_MOD,
              EnumSet.of(SVType.NAVAL)),
        TRACTOR(1.2, EquipmentTypeLookup.TRACTOR_CHASSIS_MOD,
              EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.NAVAL, SVType.RAIL)),
        TRAILER(0.8, EquipmentTypeLookup.TRAILER_CHASSIS_MOD,
              EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.RAIL)),
        ULTRA_LIGHT(0.5, EquipmentTypeLookup.ULTRALIGHT_CHASSIS_MOD,
              true),
        VSTOL(2.0, EquipmentTypeLookup.VSTOL_CHASSIS_MOD,
              EnumSet.of(SVType.FIXED_WING));

        public final double multiplier;
        public final MiscType equipment;
        public final boolean smallOnly;
        public final Set<SVType> allowedTypes;

        ChassisModification(double multiplier, String eqTypeKey) {
            this(multiplier, eqTypeKey, EnumSet.allOf(SVType.class), false);
        }

        ChassisModification(double multiplier, String eqTypeKey, boolean smallOnly) {
            this(multiplier, eqTypeKey, EnumSet.allOf(SVType.class), smallOnly);
        }

        ChassisModification(double multiplier, String eqTypeKey, Set<SVType> allowedTypes) {
            this(multiplier, eqTypeKey, allowedTypes, false);
        }

        ChassisModification(double multiplier, String eqTypeKey, Set<SVType> allowedTypes, boolean smallOnly) {
            this.multiplier = multiplier;
            this.equipment = (MiscType) EquipmentType.get(eqTypeKey);
            this.allowedTypes = allowedTypes;
            this.smallOnly = smallOnly;
        }

        /**
         * Checks for compatibility with a support vehicle type and weight class
         *
         * @param supportVehicle The support vehicle
         *
         * @return Whether the mod is valid for the vehicle
         */
        public boolean validFor(Entity supportVehicle) {
            return supportVehicle.isSupportVehicle() && allowedTypes.contains(SVType.getVehicleType(supportVehicle))
                  && (!smallOnly || (isSmallSupportVehicle(supportVehicle)))
                  // Hydrofoil has a specific upper weight limit rather than a weight class.
                  && (!this.equals(HYDROFOIL) || supportVehicle.getWeight() <= 100.0)
                  // Can't put a turret on a convertible
                  && (!this.equals(CONVERTIBLE) || !(supportVehicle instanceof Tank supportVehicleTank)
                  || (supportVehicleTank.hasNoTurret()))
                  // External power pickup (rail) is only valid with the external engine type
                  && (!this.equals(EXTERNAL_POWER_PICKUP)
                  || (supportVehicle.getEngine().getEngineType() == Engine.EXTERNAL));
        }

        /**
         * Checks whether something about the vehicle requires a specific chassis mod.
         *
         * @param supportVehicle A support vehicle
         *
         * @return Whether this chassis mod is required by the vehicle.
         */
        public boolean requiredFor(Entity supportVehicle) {
            return switch (this) {
                case PROP -> supportVehicle instanceof FixedWingSupport fixedWingSupport
                      && SVEngine.getEngineType(fixedWingSupport.getEngine()).electric;
                case EXTERNAL_POWER_PICKUP -> supportVehicle.getMovementMode().equals(EntityMovementMode.RAIL)
                      && SVEngine.getEngineType(supportVehicle.getEngine()).equals(SVEngine.EXTERNAL);
                default -> false;
            };
        }

        /**
         * Checks for compatibility between different chassis modifications
         *
         * @param other Another chassis mod
         *
         * @return Whether this chassis mod can be installed on the same vehicle as another mod.
         */
        public boolean compatibleWith(ChassisModification other) {
            return switch (this) {
                case ARMORED -> other != ULTRA_LIGHT;
                case ULTRA_LIGHT -> other != ARMORED;
                case BICYCLE -> other != MONOCYCLE;
                case MONOCYCLE -> other != BICYCLE;
                case SNOWMOBILE -> other != DUNE_BUGGY && other != AMPHIBIOUS && other != OFF_ROAD;
                case DUNE_BUGGY -> other != SNOWMOBILE && other != AMPHIBIOUS && other != OFF_ROAD;
                case AMPHIBIOUS, OFF_ROAD -> other != SNOWMOBILE && other != DUNE_BUGGY;
                default -> true;
            };
        }

        @Override
        public ITechnology getTechSource() {
            return equipment;
        }

        /**
         * Find the enum value that corresponds to an {@link EquipmentType} instance.
         *
         * @param equipmentType The equipment to match
         *
         * @return The corresponding enum value, or {@code null} if there is no match.
         */
        public @Nullable
        static ChassisModification getChassisMod(EquipmentType equipmentType) {
            for (ChassisModification mod : values()) {
                if (mod.equipment.equals(equipmentType)) {
                    return mod;
                }
            }

            return null;
        }
    }

    /**
     * Additional construction data for engine types, used to determine which ones are available for which vehicle
     * types.
     */
    public enum SVEngine implements ITechnologyDelegator {
        STEAM(Engine.STEAM, EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.AIRSHIP, SVType.NAVAL, SVType.RAIL)),
        COMBUSTION(Engine.COMBUSTION_ENGINE, SVType.allBut(SVType.SATELLITE)),
        BATTERY(Engine.BATTERY, true),
        FUEL_CELL(Engine.FUEL_CELL, true),
        SOLAR(Engine.SOLAR,
              EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.AIRSHIP, SVType.FIXED_WING, SVType.NAVAL, SVType.WIGE,
                    SVType.SATELLITE),
              true),
        FISSION(Engine.FISSION),
        FUSION(Engine.NORMAL_ENGINE),
        MAGLEV(Engine.MAGLEV, EnumSet.of(SVType.RAIL)),
        EXTERNAL(Engine.EXTERNAL, EnumSet.of(SVType.RAIL), true),
        NONE(Engine.NONE, EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.RAIL), false);

        // The engine type constant used to create a new {@link Engine}.
        public final Engine engine;

        // Fixed-wing must have prop chassis mod to use an electric engine
        public final boolean electric;
        private final Set<SVType> allowedTypes;

        SVEngine(int engineType) {
            this(engineType, EnumSet.allOf(SVType.class), false);
        }

        SVEngine(int engineType, boolean electric) {
            this(engineType, EnumSet.allOf(SVType.class), electric);
        }

        SVEngine(int engineType, Set<SVType> allowedTypes) {
            this(engineType, allowedTypes, false);
        }

        SVEngine(int engineType, Set<SVType> allowedTypes, boolean electric) {
            this.engine = new Engine(0, engineType, Engine.SUPPORT_VEE_ENGINE);
            this.allowedTypes = allowedTypes;
            this.electric = electric;
        }

        /**
         * Finds the enum value corresponding to an {@link Engine}.
         *
         * @param engine The engine
         *
         * @return The enum value for the engine, or {@code null} if it is not a valid SV engine type.
         */
        public @Nullable
        static SVEngine getEngineType(Engine engine) {
            if (null != engine) {
                for (SVEngine svEngine : values()) {
                    if (svEngine.engine.getEngineType() == engine.getEngineType()) {
                        return svEngine;
                    }
                }
            }

            return null;
        }

        /**
         * @param type The support vehicle type
         *
         * @return Whether the engine is valid for the support vee.
         */
        public boolean isValidFor(SVType type) {
            return allowedTypes.contains(type);
        }

        /**
         * @param entity A support vehicle
         *
         * @return Whether the engine is valid for the support vee.
         */
        public boolean isValidFor(Entity entity) {
            if ((this == NONE) && !entity.isTrailer()) {
                return false;
            }

            return isValidFor(SVType.getVehicleType(entity));
        }

        @Override
        public ITechnology getTechSource() {
            return engine;
        }
    }

    /**
     * Tech advancement data for structural components with variable tech levels (structure, armor, engine). This is
     * assembled from the table on TM, p. 122 and IO, p. 49, primitive construction rules (IO, p. 120-121) and a pending
     * proposal to the rules committee for E.
     */
    public static final TechAdvancement[] TECH_LEVEL_TA = {
          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.A)
                .setAdvancement(ITechnology.DATE_PS, ITechnology.DATE_PS, ITechnology.DATE_PS)
                .setAvailability(AvailabilityValue.A, AvailabilityValue.A, AvailabilityValue.A,
                AvailabilityValue.A),

          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.B)
                .setAdvancement(ITechnology.DATE_PS, ITechnology.DATE_PS, ITechnology.DATE_PS)
                .setAvailability(AvailabilityValue.B, AvailabilityValue.B, AvailabilityValue.B,
                AvailabilityValue.A),

          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.C)
                .setAdvancement(ITechnology.DATE_ES, ITechnology.DATE_ES, ITechnology.DATE_ES)
                .setPrototypeFactions(Faction.TA).setProductionFactions(Faction.TA)
                .setAvailability(AvailabilityValue.C, AvailabilityValue.B, AvailabilityValue.B,
                AvailabilityValue.B),

          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.D)
                .setAdvancement(2420, 2430, 2435).setApproximate(true, true, false)
                .setPrototypeFactions(Faction.TH).setProductionFactions(Faction.TH)
                .setAvailability(AvailabilityValue.C, AvailabilityValue.C, AvailabilityValue.C,
                AvailabilityValue.B),

          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.E)
                .setISAdvancement(2557, 2571, 3055).setClanAdvancement(2557, 2571, 2815)
                .setAvailability(AvailabilityValue.D, AvailabilityValue.F, AvailabilityValue.D,
                AvailabilityValue.C),

          new TechAdvancement(TechBase.ALL).setTechRating(TechRating.F)
                .setISAdvancement(ITechnology.DATE_NONE, ITechnology.DATE_NONE, 3065)
                .setISApproximate(false, false, true)
                .setClanAdvancement(2820, 2825, 2830).setClanApproximate(true, true, false)
                .setAvailability(AvailabilityValue.E, AvailabilityValue.E, AvailabilityValue.D,
                AvailabilityValue.C)
    };

    /**
     * The chassis weight multiplier for tech ratings A-F
     */
    private static final EnumMap<TechRating, Double> STRUCTURE_TECH_MULTIPLIER = new EnumMap<>(TechRating.class);

    static {
        STRUCTURE_TECH_MULTIPLIER.put(TechRating.A, 1.6);
        STRUCTURE_TECH_MULTIPLIER.put(TechRating.B, 1.3);
        STRUCTURE_TECH_MULTIPLIER.put(TechRating.C, 1.15);
        STRUCTURE_TECH_MULTIPLIER.put(TechRating.D, 1.0);
        STRUCTURE_TECH_MULTIPLIER.put(TechRating.E, 0.85);
        STRUCTURE_TECH_MULTIPLIER.put(TechRating.F, 0.66);
    }

    /**
     * Filters all vehicle armor according to given tech constraints. Standard armor is treated as basic support vehicle
     * armor.
     *
     * @param techManager Applies the filtering criteria
     *
     * @return A list of armor equipment that meets the tech constraints
     */
    public static List<ArmorType> legalArmorsFor(ITechManager techManager) {
        List<ArmorType> retVal = new ArrayList<>();

        for (ArmorType armor : ArmorType.allArmorTypes()) {
            if (armor.getArmorType() == EquipmentType.T_ARMOR_PATCHWORK) {
                continue;
            }

            // Installing non-BAR armor on a support vehicle is advanced
            if (!armor.hasFlag(MiscType.F_SUPPORT_VEE_BAR_ARMOR)
                  && (techManager.getTechLevel().ordinal() < SimpleTechLevel.ADVANCED.ordinal())) {
                continue;
            }

            if (armor.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT) && techManager.isLegal(armor)) {
                retVal.add(armor);
            }
        }
        return retVal;
    }

    /**
     * The maximum number of armor points a support vehicle is computed by multiplying the total tonnage by a factor
     * determined by the vehicle type and adding four.
     *
     * @param vee The support vehicle
     *
     * @return The maximum number of armor points. If the entity cannot be identified as a support vehicle, returns 0.
     */
    public static int maxArmorFactor(Entity vee) {
        SVType type = SVType.getVehicleType(vee);

        if (null == type) {
            return 0;
        }

        double factor = 0;

        switch (type) {
            case AIRSHIP:
            case NAVAL:
                if (vee.getWeightClass() == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                    factor = 0.05;
                } else {
                    factor = 0.334;
                }
                break;
            case WIGE:
            case RAIL:
            case SATELLITE:
                factor = 0.5;
                break;
            case FIXED_WING:
            case HOVERCRAFT:
            case VTOL:
                factor = 1.0;
                break;
            case TRACKED:
            case WHEELED:
                factor = 2.0;
                break;
        }

        return 4 + (int) (vee.getWeight() * factor);
    }

    /**
     * Calculates the weight of each point of armor. For standard SV armor this is based on the tech and BAR ratings.
     * For advanced armors this is the reciprocal of the number of points per ton.
     *
     * @param vee The support vehicle
     *
     * @return The weight of each armor point in tons, rounded to the kilogram.
     */
    public static double armorWeightPerPoint(Entity vee) {
        final ArmorType armor = ArmorType.forEntity(vee);

        if (armor.hasFlag(MiscType.F_SUPPORT_VEE_BAR_ARMOR)) {
            return armor.getSVWeightPerPoint(vee.getArmorTechRating());
        } else {
            final double ppt = armor.getPointsPerTon(vee);
            return round(1.0 / ppt, Ceil.KILO);
        }
    }

    /**
     * Checks whether the support vehicle can mount sponson turrets (TO, p. 348)
     *
     * @param type  The support vehicle type
     * @param small Whether the {@link Entity} is a small support vehicle
     *
     * @return Whether the vehicle can use sponson turrets.
     */
    public static boolean sponsonLegal(SVType type, boolean small) {
        return !small && (type != SVType.FIXED_WING) && (type != SVType.AIRSHIP) && (type != SVType.SATELLITE);
    }

    /**
     * Checks whether the support vehicle can mount pintle turrets (TM, p. 348)
     *
     * @param type  The support vehicle type
     * @param small Whether the {@link Entity} is a small support vehicle
     *
     * @return Whether the vehicle can use pintle turrets.
     */
    public static boolean pintleLegal(SVType type, boolean small) {
        return small && (type != SVType.FIXED_WING) && (type != SVType.NAVAL);
    }

    private final Entity supportVee;

    // Used by support tanks for calculation of turret weight
    private final TestTank testTank;

    /**
     * Used by fixed wing, airship, and satellite for some calculations and validation
     */
    private final TestAero testAero;

    public TestSupportVehicle(Entity supportVehicle, TestEntityOption options, String fileString) {
        super(options, supportVehicle.getEngine(), null);
        supportVee = supportVehicle;
        testTank = supportVehicle instanceof Tank tankVehicle ? new TestTank(tankVehicle, options, fileString) : null;
        testAero = supportVehicle instanceof Aero aeroVehicle ? new TestAero(aeroVehicle, options, fileString) : null;
    }

    @Override
    public String printWeightStructure() {
        return StringUtil.makeLength(
              "Chassis: ", getPrintSize() - 5)
              + TestEntity.makeWeightString(getWeightStructure(), usesKgStandard()) + "\n";
    }

    @Override
    public String printWeightEngine() {
        return StringUtil.makeLength(String.format("Engine: %s (%s)",
                    engine.getEngineName(), getEntity().getEngineTechRating().getName()),
              getPrintSize() - 5)
              + TestEntity.makeWeightString(getWeightEngine(), usesKgStandard()) + "\n";
    }

    @Override
    public String printWeightArmor() {
        String name;

        if (getEntity().hasPatchworkArmor()) {
            name = "Patchwork";
        } else {
            name = ArmorType.forEntity(getEntity()).getName();
        }

        return StringUtil.makeLength(
              String.format("Armor: %d (%s)", getTotalOArmor(), name),
              getPrintSize() - 5)
              + TestEntity.makeWeightString(getWeightArmor(), usesKgStandard()) + "\n";

    }

    @Override
    public Entity getEntity() {
        return supportVee;
    }

    @Override
    public boolean isTank() {
        return supportVee instanceof Tank;
    }

    @Override
    public boolean isMek() {
        return false;
    }

    @Override
    public boolean isAero() {
        return supportVee.isAero();
    }

    @Override
    public boolean isSmallCraft() {
        return false;
    }

    @Override
    public boolean isAdvancedAerospace() {
        return false;
    }

    @Override
    public boolean isProtoMek() {
        return false;
    }

    /**
     * Rounds up to the nearest half ton or kilogram as appropriate to the vehicle
     *
     * @param val The weight to round, in tons
     *
     * @return The rounded weight, in tons
     */
    private double ceilWeight(double val) {
        if (isSmallSupportVehicle()) {
            // Deal first with possible variances from floating point precision by rounding
            // to the gram, then round the result up to the next kilogram. Then convert back
            // to metric tons.
            return Math.ceil(Math.round(val * 1000000.0) / 1000.0) / 1000.0;
        } else {
            return ceil(val, Ceil.HALF_TON);
        }
    }

    @Override
    public double calculateWeight() {
        return ceilWeight(calculateWeightExact());
    }

    @Override
    public double calculateWeightExact() {
        return super.calculateWeightExact() + getFuelTonnage();
    }

    @Override
    public double getWeightStructure() {
        double weight = supportVee.getWeight();
        weight *= SVType.getBaseChassisValue(supportVee);
        double structureMultiplier = STRUCTURE_TECH_MULTIPLIER.getOrDefault(supportVee.getStructuralTechRating(), 1.0);
        weight *= structureMultiplier;

        for (Mounted<?> mounted : supportVee.getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION)) {
                ChassisModification mod = ChassisModification.getChassisMod(mounted.getType());

                if (null != mod) {
                    weight *= mod.multiplier;
                } else {
                    logger.warn("Could not find multiplier for {} chassis mod.", mounted.getType().getName());
                }
            }
        }
        return ceilWeight(weight);
    }

    public double getFuelTonnage() {
        if (supportVee instanceof Aero) {
            return ((Aero) supportVee).getFuelTonnage();
        } else {
            return ((Tank) supportVee).getFuelTonnage();
        }
    }

    private double getWeightFireControl() {
        for (Mounted<?> mounted : supportVee.getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_BASIC_FIRE_CONTROL)
                  || mounted.getType().hasFlag(MiscType.F_ADVANCED_FIRE_CONTROL)) {
                return mounted.getTonnage();
            }
        }

        return 0.0;
    }

    private double getWeightCrewAccommodations() {
        double weight = 0;

        for (Transporter transporter : supportVee.getTransports()) {
            if ((transporter instanceof Bay transportBay) && transportBay.isQuarters()) {
                weight += transportBay.getWeight();
            }
        }

        return round(weight, Ceil.KILO);
    }

    @Override
    public double getWeightControls() {
        // No need to add FireControl weights here; they are included in Misc Equipment
        return getWeightCrewAccommodations();
    }

    @Override
    public double getWeightMisc() {
        return getTankWeightTurret() + getTankWeightDualTurret();
    }

    public double getTankWeightTurret() {
        if (null != testTank) {
            return testTank.getTankWeightTurret();
        }

        return 0.0;
    }

    public double getTankWeightDualTurret() {
        if (null != testTank) {
            return testTank.getTankWeightDualTurret();
        }

        return 0.0;
    }

    @Override
    public double getWeightAmmo() {
        if (isSmallSupportVehicle()) {
            double weight = 0;
            for (Mounted<?> mounted : getEntity().getWeaponList()) {
                weight += getSmallSVAmmoWeight(mounted);
            }
            return weight;
        } else {
            return super.getWeightAmmo();
        }
    }

    @Override
    public double getWeightPowerAmp() {
        // Small support vees only use infantry weapons, which do not require amplifiers
        if (isSmallSupportVehicle()) {
            return 0;
        }

        if (!engine.isFusion() && (engine.getEngineType() != Engine.FISSION)) {
            double weight = 0;

            for (Mounted<?> mounted : supportVee.getWeaponList()) {
                WeaponType weaponType = (WeaponType) mounted.getType();
                if (weaponType.hasFlag(WeaponType.F_ENERGY) && !(weaponType instanceof CLChemicalLaserWeapon)
                      && !(weaponType instanceof VehicleFlamerWeapon)) {
                    weight += mounted.getTonnage();
                }

                if ((mounted.getLinkedBy() != null) && (mounted.getLinkedBy().getType() instanceof MiscType)
                      && mounted.getLinkedBy().getType().hasFlag(MiscType.F_PPC_CAPACITOR)) {
                    weight += mounted.getLinkedBy().getTonnage();
                }
            }

            for (Mounted<?> mounted : supportVee.getMisc()) {
                if (mounted.getType().hasFlag(MiscType.F_CLUB)
                      && mounted.getType().hasFlag(MiscTypeFlag.S_SPOT_WELDER)) {
                    weight += mounted.getTonnage();
                }
            }

            return ceilWeight(weight / 10);
        }
        return 0;
    }

    private static final EquipmentBitSet EXCLUDE = MiscType.F_BASIC_FIRE_CONTROL.asEquipmentBitSet()
          .or(MiscType.F_ADVANCED_FIRE_CONTROL)
          .or(MiscType.F_CHASSIS_MODIFICATION);

    @Override
    protected boolean includeMiscEquip(MiscType eq) {
        // fire control is counted with control system weight and chassis mods are part
        // of the structure weight
        return !eq.hasFlag(EXCLUDE);
    }

    @Override
    public double getWeightHeatSinks() {
        // Unlike other units, support vehicles do not get any free engine heat sinks.
        return getCountHeatSinks();
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        // Small support vees can't mount heavy weapons, so no reason to iterate through them to check.
        return isSmallSupportVehicle() ? 0 : heatNeutralHSRequirement();
    }

    @Override
    public String printWeightMisc() {
        if (null != testTank) {
            return testTank.printWeightMisc();
        } else {
            return getWeightPowerAmp() != 0 ? StringUtil.makeLength(
                  "Power Amp:", getPrintSize() - 5)
                  + TestEntity.makeWeightString(getWeightPowerAmp(), usesKgStandard()) + "\n" : "";
        }
    }

    @Override
    public String printWeightControls() {
        String fireCon = "";
        for (Mounted<?> mounted : supportVee.getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_BASIC_FIRE_CONTROL)
                  || mounted.getType().hasFlag(MiscType.F_ADVANCED_FIRE_CONTROL)) {
                fireCon = StringUtil.makeLength(mounted.getName(), getPrintSize() - 5)
                      + TestEntity.makeWeightString(mounted.getTonnage(), usesKgStandard()) + "\n";
                break;
            }
        }
        double weight = getWeightCrewAccommodations();
        String crewStr = weight > 0 ? StringUtil.makeLength("Crew Accommodations:", getPrintSize() - 5)
              + TestEntity.makeWeightString(weight, usesKgStandard()) + "\n" : "";
        return fireCon + crewStr;
    }

    @Override
    public StringBuffer printAmmo(StringBuffer buff, int posLoc, int posWeight) {
        if (!isSmallSupportVehicle()) {
            return super.printAmmo(buff, posLoc, posWeight);
        }
        for (Mounted<?> mounted : getEntity().getWeaponList()) {
            double weight = getSmallSVAmmoWeight(mounted);
            if (weight > 0) {
                buff.append(StringUtil.makeLength("Ammo [" + mounted.getName() + "]", 20));
                buff.append(" ").append(
                            StringUtil.makeLength(getLocationAbbr(mounted.getLocation()),
                                  getPrintSize() - 5 - 20))
                      .append(TestEntity.makeWeightString(weight, true)).append("\n");
            }
        }
        return buff;
    }

    public static double getSmallSVAmmoWeight(Mounted<?> mounted) {
        // first clip is free
        if ((mounted.getSize() > 1) && (mounted.getType() instanceof InfantryWeapon)) {
            return RoundWeight.nextKg((mounted.getSize() - 1)
                  * ((InfantryWeapon) mounted.getType()).getAmmoWeight());
        }

        return 0.0;
    }

    public boolean canUseSponsonTurret() {
        return sponsonLegal(SVType.getVehicleType(getEntity()), isSmallSupportVehicle());
    }

    public boolean canUsePintleTurret() {
        return pintleLegal(SVType.getVehicleType(getEntity()), isSmallSupportVehicle());
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        boolean correct = true;

        if (skip()) {
            return true;
        }

        if (!allowOverweightConstruction() && !correctWeight(buff)) {
            correct = false;
        }

        if (!engine.engineValid) {
            buff.append(engine.problem.toString()).append("\n\n");
            correct = false;
        }

        if (engine.getEngineType() == Engine.NONE && getEntity().getWalkMP() > 0) {
            buff.append("Vehicle with no engine must not have nonzero MP\n");
            correct = false;
        }

        if ((supportVee instanceof Tank supportTank) && (supportTank.fuelTonnagePer100km() > 0.0)
              && (supportTank.getFuelTonnage() <= 0.0)) {
            buff.append("Support vehicles with ").append(engine.getEngineName())
                  .append(" engine must allocate some weight for fuel.\n");
            correct = false;
        } else if ((supportVee instanceof FixedWingSupport fixedWingSupport)
              && (fixedWingSupport.getOriginalFuel() <= 0.0)
              && (fixedWingSupport.kgPerFuelPoint() > 0)) {
            buff.append("Aerospace units must allocate some weight for fuel.\n");
            correct = false;
        }

        if (!ignoreSlotCount() && (occupiedSlotCount() > totalSlotCount())) {
            buff.append("Not enough item slots available! Using ");
            buff.append(Math.abs(occupiedSlotCount() - totalSlotCount()));
            buff.append(" slot(s) too many.\n");
            buff.append(printSlotCalculation()).append("\n");
            correct = false;
        }

        int armorLimit = maxArmorFactor(supportVee);

        if (supportVee.getTotalOArmor() > armorLimit) {
            buff.append("Armor exceeds point limit for ");
            buff.append(supportVee.getWeight());
            buff.append("-ton ");
            buff.append(supportVee.getMovementModeAsString());
            buff.append(" support vehicle: ");
            buff.append(supportVee.getTotalOArmor());
            buff.append(" points > ");
            buff.append(armorLimit);
            buff.append(".\n\n");
            correct = false;
        }

        correct &= correctArmorOverAllocation(supportVee, buff);

        if (supportVee.hasBARArmor(supportVee.firstArmorIndex())) {
            int bar = supportVee.getBARRating(supportVee.firstArmorIndex());

            if ((bar < 2) || (bar > 10)) {
                buff.append("Armor must have a BAR between 2 and 10.\n");
                correct = false;
            } else {
                double perPoint = ArmorType.forEntity(supportVee).getSVWeightPerPoint(supportVee.getArmorTechRating());

                if (perPoint < 0.001) {
                    buff.append("BAR ")
                          .append(bar)
                          .append(" exceeds maximum for armor tech rating ")
                          .append(supportVee.getArmorTechRating().getName())
                          .append(".\n");
                    correct = false;
                }
            }
        }

        if (supportVee instanceof VTOL supportVtol) {
            long mastMountCount = supportVtol.countEquipment(EquipmentTypeLookup.MAST_MOUNT);

            if (mastMountCount > 1) {
                buff.append("Cannot mount more than one mast mount\n");
                correct = false;
            } else if (mastMountCount == 0) {
                for (Mounted<?> mounted : supportVtol.getEquipment()) {
                    if (mounted.getLocation() == VTOL.LOC_ROTOR) {
                        buff.append("rotor equipment must be placed in mast mount\n");
                        correct = false;
                    }
                }
            }
            if (supportVtol.getOArmor(VTOL.LOC_ROTOR) > TestTank.VTOL_MAX_ROTOR_ARMOR) {
                buff.append(supportVtol.getOArmor(VTOL.LOC_ROTOR));
                buff.append(" points of VTOL rotor armor exceed ");
                buff.append(TestTank.VTOL_MAX_ROTOR_ARMOR);
                buff.append("-point limit.\n\n");
                correct = false;
            }
        }

        int hardPoints = 0;
        boolean sponson = false;
        boolean pintle = false;
        boolean hasAdvancedFireControl = false;

        for (Mounted<?> mounted : supportVee.getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)
                  && (getEntity() instanceof Aero || getEntity() instanceof VTOL)) {
                buff.append("Armored Motive system and incompatible movement mode!\n\n");
                correct = false;
            } else if (mounted.getType().hasFlag(MiscType.F_LIFEBOAT)
                  && mounted.getType().hasAnyFlag(MiscTypeFlag.S_MARITIME_ESCAPE_POD, MiscTypeFlag.S_MARITIME_LIFEBOAT)
                  && !SVType.NAVAL.equals(SVType.getVehicleType(supportVee))
                  && !supportVee.hasWorkingMisc(MiscType.F_AMPHIBIOUS)) {
                buff.append(mounted.getName())
                      .append(" requires naval support vehicle or amphibious chassis modification.\n");
                correct = false;
            } else if (mounted.getType().hasFlag(MiscType.F_EXTERNAL_STORES_HARDPOINT)) {
                hardPoints++;
            } else if (mounted.getType().hasFlag(MiscType.F_SPONSON_TURRET)) {
                sponson = true;
            } else if (mounted.getType().hasFlag(MiscType.F_PINTLE_TURRET)) {
                pintle = true;
            } else if (mounted.getType().hasFlag(MiscTypeFlag.F_ADVANCED_FIRE_CONTROL)) {
                hasAdvancedFireControl = true;
            }
        }

        if (hardPoints > supportVee.getWeight() / 10.0) {
            buff.append("Exceeds maximum of one external stores hard point per 10 tons.\n");
            correct = false;
        }
        if (sponson && !canUseSponsonTurret()) {
            buff.append("Only medium and large surface vehicles can use a sponson turret.\n");
            correct = false;
        }
        if (sponson && supportVee.getOriginalJumpMP() > 0) {
            buff.append("Cannot mount both jump jets and sponson turrets.\n");
            correct = false;
        }
        if (pintle && !canUsePintleTurret()) {
            buff.append("Only small surface support vehicles can use a pintle turret.\n");
            correct = false;
        }
        double weaponWeight = 0.0;

        for (WeaponMounted mounted : supportVee.getWeaponList()) {
            if (!isValidWeapon(mounted)) {
                buff.append(mounted.getType().getName()).append(" is not a valid weapon for this unit\n");
                correct = false;
            }

            weaponWeight += mounted.getTonnage();
        }

        if (supportVee.isOmni()
              && (supportVee.hasWorkingMisc(MiscType.F_BASIC_FIRE_CONTROL)
              || supportVee.hasWorkingMisc(MiscType.F_ADVANCED_FIRE_CONTROL))
              && (weaponWeight / 10.0 > supportVee.getBaseChassisFireConWeight())) {
            buff.append("Omni configuration exceeds weapon capacity of base chassis fire control system.\n");
            correct = false;
        }
        for (Mounted<?> mounted : supportVee.getEquipment()) {
            EquipmentType type = mounted.getType();
            if ((type instanceof MiscType) && !type.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT)) {
                buff.append(type.getName()).append(" cannot be used by support vehicles.\n");
                correct = false;
            } else if ((type instanceof WeaponType)
                  && !isSmallSupportVehicle()
                  && !type.hasFlag(WeaponType.F_TANK_WEAPON)) {
                buff.append(type.getName()).append(" cannot be used by support vehicles.\n");
                correct = false;
            } else if (!TestTank.legalForMotiveType(type, supportVee.getMovementMode(), true)) {
                buff.append(type.getName()).append(" is incompatible with ")
                      .append(supportVee.getMovementModeAsString());
                correct = false;
            }

            // TM p.209
            if (type.isC3Equipment() && !hasAdvancedFireControl) {
                buff.append("Support vehicles without advanced fire control cannot mount C3 equipment\n");
                correct = false;
            }
        }

        for (int loc = 0; loc < supportVee.locations(); loc++) {
            int count = 0;

            for (Mounted<?> misc : supportVee.getMisc()) {
                if ((misc.getLocation() == loc) && misc.getType().hasFlag(MiscType.F_MANIPULATOR)) {
                    count++;
                }
            }

            if (count > 2) {
                buff.append("max of 2 manipulators per location");
                correct = false;
                break;
            }
        }

        if (supportVee.isAero() && testAero.hasMismatchedLateralWeapons(buff)) {
            correct = false;
        }

        if (hasIllegalChassisMods(buff)) {
            correct = false;
        }

        if (hasInsufficientSeating(buff)) {
            correct = false;
        }

        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }

        if (hasIllegalTechLevels(buff, ammoTechLvl)) {
            correct = false;
        }

        if (showIncorrectIntroYear() && hasIncorrectIntroYear(buff)) {
            correct = false;
        }

        if (hasIllegalEquipmentCombinations(buff)) {
            correct = false;
        }

        if (!correctCriticalSlots(buff)) {
            correct = false;
        }

        if (getEntity().hasQuirk(OptionsConstants.QUIRK_NEG_ILLEGAL_DESIGN)
              || getEntity().canonUnitWithInvalidBuild()) {
            correct = true;
        }

        return correct;
    }

    private boolean isSmallSupportVehicle() {
        return isSmallSupportVehicle(getEntity());
    }

    public static boolean isSmallSupportVehicle(Entity entity) {
        return entity.isSupportVehicle() && (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT);
    }

    public static boolean isSmallSupportVehicleWeapon(EquipmentType type) {
        // I don't see an explicit rule that excludes archaic weapons, but it seems to make sense
        return (type instanceof InfantryWeapon) && !type.hasFlag(WeaponType.F_INF_ARCHAIC);
    }

    @Override
    public boolean hasIllegalEquipmentCombinations(StringBuffer buffer) {
        boolean illegal = super.hasIllegalEquipmentCombinations(buffer);
        for (Mounted<?> mounted : supportVee.getMisc()) {
            if (mounted.getType().hasFlag(MiscType.F_ARMORED_CHASSIS)
                  || mounted.getType().hasFlag(MiscType.F_AMPHIBIOUS)
                  || mounted.getType().hasFlag(MiscType.F_ENVIRONMENTAL_SEALING)
                  || mounted.getType().hasFlag(MiscType.F_SUBMERSIBLE)) {
                for (int loc = supportVee.firstArmorIndex(); loc < supportVee.locations(); loc++) {
                    // Tanks have the body location first. Aero SVs have it last, but also have the
                    // squadron wings location.
                    if (supportVee.isAero() && (loc >= FixedWingSupport.LOC_WINGS)) {
                        break;
                    }

                    if (supportVee.getOArmor(loc) == 0) {
                        buffer.append(mounted.getType().getName())
                              .append(" requires at least one point of armor in every location.\n");
                        illegal = true;
                        break;
                    }
                }

                break;
            }
        }

        return illegal;
    }

    boolean hasIllegalChassisMods(StringBuffer buff) {
        boolean illegal = false;
        final Set<ChassisModification> chassisMods = supportVee.getMisc().stream()
              .filter(m -> m.getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION))
              .map(m -> ChassisModification.getChassisMod(m.getType()))
              .filter(Objects::nonNull).collect(Collectors.toSet());

        if (!chassisMods.contains(ChassisModification.ARMORED)) {
            ArmorType armor = ArmorType.forEntity(supportVee);

            if (!armor.hasFlag(MiscType.F_SUPPORT_VEE_BAR_ARMOR)) {
                buff.append("Advanced armor requires the Armored Chassis Mod.\n");
                illegal = true;
            }

            if (armor.getSVWeightPerPoint(supportVee.getArmorTechRating()) > 0.05) {
                buff.append("Armor heavier than 50kg/point requires the Armored Chassis Mod.\n");
                illegal = true;
            }
        }

        if (supportVee.isAero() && SVEngine.getEngineType(supportVee.getEngine()).electric
              && !chassisMods.contains(ChassisModification.PROP)) {
            buff.append(
                  "Fixed Wing and Airship support vehicles with electric engines requires the Prop Chassis Mod.\n");
            illegal = true;
        }

        if ((supportVee.getEngine().getEngineType() == Engine.EXTERNAL)
              && !chassisMods.contains(ChassisModification.EXTERNAL_POWER_PICKUP)) {
            buff.append("An external engine requires the External Power Pickup Chassis Mod.\n");
            illegal = true;
        }

        if (chassisMods.contains(ChassisModification.HYDROFOIL)
              && (supportVee.getWeight() > 100.0)) {
            buff.append("The Hydrofoil Chassis Mod may not be used on naval support vehicles larger than 100 tons.\n");
            illegal = true;
        }

        if (chassisMods.contains(ChassisModification.CONVERTIBLE)
              && (supportVee instanceof Tank) && !((Tank) supportVee).hasNoTurret()) {
            buff.append("The Convertible Chassis Mod may not be used with a turret.\n");
            illegal = true;
        }

        for (ChassisModification mod : chassisMods) {
            if (!mod.allowedTypes.contains(SVType.getVehicleType(supportVee))) {
                buff.append(mod.equipment.getName())
                      .append(" is not valid for ")
                      .append(supportVee.getMovementModeAsString())
                      .append("\n");
                illegal = true;
            }

            if (mod.smallOnly && !isSmallSupportVehicle()) {
                buff.append(mod.equipment.getName())
                      .append(" is only valid with small support vehicles.\n");
                illegal = true;
            }

            if (!mod.validFor(supportVee)) {
                buff.append("Incompatible chassis mod: ")
                      .append(mod.equipment.getName())
                      .append("\n");
                illegal = true;
            }

            for (ChassisModification mod2 : chassisMods) {
                // Only compare to mods with higher ordinals to make sure each combination
                // only gets checked once.
                if ((mod2.ordinal() > mod.ordinal()) && !mod.compatibleWith(mod2)) {
                    buff.append(mod.equipment.getName())
                          .append(" is incompatible with ")
                          .append(mod2.equipment.getName())
                          .append("\n");
                    illegal = true;
                }
            }
        }
        return illegal;
    }

    boolean hasInsufficientSeating(StringBuffer buff) {
        // Small SVs are required to provide seating for all crew. M/L have
        // seating provided as part of the structure.
        if (isSmallSupportVehicle()) {
            int minCrew = Compute.getFullCrewSize(supportVee);

            // Pillion and ejection seating are subclasses of StandardSeatCargoBay
            int seating = supportVee.getTransports().stream()
                  .filter(t -> t instanceof StandardSeatCargoBay)
                  .mapToInt(t -> (int) ((Bay) t).getCapacity())
                  .sum();

            if (seating < minCrew) {
                buff.append("Minimum crew is ").append(minCrew)
                      .append(" but there is only seating for ")
                      .append(seating).append(".\n");
                return true;
            }
        }

        return false;
    }

    private boolean isValidWeapon(WeaponMounted weapon) {
        if (isSmallSupportVehicle()) {
            return isSmallSupportVehicleWeapon(weapon.getType());
        } else {
            //TODO: Align with UnitUtil.isSupportVehicleEquipment()
            return weapon.getType().hasFlag(WeaponType.F_TANK_WEAPON);
        }
    }

    public boolean correctCriticalSlots(StringBuffer buff) {
        List<Mounted<?>> unallocated = new ArrayList<>();
        boolean correct = true;

        for (Mounted<?> mount : supportVee.getMisc()) {
            if ((mount.getLocation() == Entity.LOC_NONE) && (mount.getType().getSupportVeeSlots(supportVee) != 0)) {
                unallocated.add(mount);
            }
        }

        for (Mounted<?> mount : supportVee.getWeaponList()) {
            if (mount.getLocation() == Entity.LOC_NONE) {
                unallocated.add(mount);
            }
        }

        if (!isSmallSupportVehicle()) {
            for (Mounted<?> mount : supportVee.getAmmo()) {
                if ((mount.getLocation() == Entity.LOC_NONE) && !mount.isOneShotAmmo()) {
                    unallocated.add(mount);
                }
            }
        }

        if (!unallocated.isEmpty()) {
            buff.append("Unallocated Equipment:\n");

            for (Mounted<?> mount : unallocated) {
                buff.append(mount.getType().getInternalName()).append("\n");
            }

            correct = false;
        }

        return correct;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append(getName())
              .append("\n")
              .append("Found in: ")
              .append(fileString)
              .append("\n")
              .append(printTechLevel())
              .append("Intro year: ")
              .append(supportVee.getYear())
              .append("\n")
              .append(printSource())
              .append(printShortMovement());

        if (correctWeight(buff, true, true)) {
            if (!usesKgStandard()) {
                buff.append("Weight: ")
                      .append(getWeight())
                      .append(" (")
                      .append(calculateWeight())
                      .append(")\n");
            } else {
                buff.append("Weight: ")
                      .append(getWeight() * 1000)
                      .append(" kg (")
                      .append(calculateWeight() * 1000)
                      .append(" kg)\n");
            }
        }

        buff.append(printWeightCalculation()).append("\n");
        printFailedEquipment(buff);
        return buff;
    }

    public StringBuffer printSlotCalculation() {
        StringBuffer buff = new StringBuffer();
        buff.append("Available slots: ").append(totalSlotCount()).append("\n");
        // different engines take different amounts of slots

        // JJs take just 1 slot
        if (supportVee.getJumpMP(MPCalculationSetting.NO_GRAVITY) > 0) {
            buff.append(StringUtil.makeLength("Jump Jets", 30)).append("1\n");
        }

        boolean addedCargo = false;
        for (Mounted<?> mount : supportVee.getEquipment()) {
            if ((mount.getType() instanceof MiscType mountType) && mountType.hasFlag(MiscType.F_CARGO)) {
                if (!addedCargo) {
                    buff.append(StringUtil.makeLength(mount.getName(), 30));
                    buff.append(mountType.getSupportVeeSlots(supportVee)).append("\n");
                    addedCargo = true;
                }

                continue;
            }

            if (!(mount.getType() instanceof AmmoType)
                  && (EquipmentType.getArmorType(mount.getType()) == EquipmentType.T_ARMOR_UNKNOWN)
                  && !((mount instanceof MiscMounted) && mount.getType().hasFlag(MiscType.F_JUMP_JET))) {
                buff.append(StringUtil.makeLength(mount.getName(), 30));
                buff.append(mount.getType().getSupportVeeSlots(supportVee)).append("\n");
            }
        }

        if (getCrewSlots() > 0) {
            buff.append(StringUtil.makeLength("Crew Seats/Quarters:", 30));
            buff.append(getCrewSlots()).append("\n");
        }

        // different armor types take different amount of slots
        int armorSlots = 0;
        if (!supportVee.hasPatchworkArmor()) {
            ArmorType armor = ArmorType.forEntity(supportVee);
            armorSlots += armor.getSupportVeeSlots(supportVee);
        } else {
            for (int loc = 0; loc < supportVee.locations(); loc++) {
                ArmorType armor = ArmorType.forEntity(supportVee, loc);
                armorSlots += armor.getPatchworkSlotsMekSV();
            }
        }

        if (armorSlots != 0) {
            buff.append(StringUtil.makeLength("Armor", 30))
                  .append(armorSlots)
                  .append("\n");
        }

        // for ammo, each type of ammo takes one slots, regardless of sub-munition type
        Set<String> foundAmmo = new HashSet<>();
        for (Mounted<?> ammo : supportVee.getAmmo()) {
            if (ammo.isOneShotAmmo()) {
                continue;
            }

            if (ammo.getType() instanceof AmmoType ammoType) {
                if (!foundAmmo.contains(ammoType.getAmmoType() + ":" + ammoType.getRackSize())) {
                    buff.append(StringUtil.makeLength(ammoType.getName(), 30));
                    buff.append("1\n");
                    foundAmmo.add(ammoType.getAmmoType() + ":" + ammoType.getRackSize());
                }
            }
        }

        // if a tank has an infantry bay, add 1 slots (multiple bays take 1 slot total)

        boolean troopSpaceFound = false;
        for (Transporter transport : supportVee.getTransports()) {
            if ((transport instanceof InfantryCompartment) && !troopSpaceFound) {
                buff.append(StringUtil.makeLength("Troop Space", 30));
                buff.append("1\n");
                troopSpaceFound = true;
            } else if ((transport instanceof Bay transportBay) && !transportBay.isQuarters()) {
                buff.append(StringUtil.makeLength(transportBay.getTransporterType(), 30))
                      .append("1\n");
            }
        }
        return buff;
    }

    @Override
    public String getName() {
        return "Support Vehicle: " + supportVee.getDisplayName();
    }

    @Override
    public double getWeightArmor() {
        return supportVee.getArmorWeight();
    }

    /**
     * @return The total slot space
     */
    public int totalSlotCount() {
        return 5 + (int) Math.floor(supportVee.getWeight() / 10.0);
    }

    /**
     * @return The number of slots taken up by installed equipment
     */
    public int occupiedSlotCount() {
        return getCrewSlots() + getArmorSlots() + getAmmoSlots()
              + getWeaponSlots() + getMiscEquipSlots() + getTransportSlots();
    }

    /**
     * @return The number of slots taken up by armor
     */
    public int getArmorSlots() {
        if (!supportVee.hasPatchworkArmor()) {
            ArmorType armor = ArmorType.forEntity(supportVee);
            return armor.getSupportVeeSlots(supportVee);
        } else {
            int space = 0;

            for (int loc = 0; loc < supportVee.locations(); loc++) {
                ArmorType armor = ArmorType.of(supportVee.getArmorType(loc),
                      TechConstants.isClan(supportVee.getArmorTechLevel(loc)));
                space += armor.getPatchworkSlotsMekSV();
            }

            return space;
        }
    }

    /**
     * @return The number of slots taken up by weapons
     */
    public int getWeaponSlots() {
        int slots = 0;

        for (Mounted<?> mounted : supportVee.getWeaponList()) {
            slots += mounted.getType().getSupportVeeSlots(supportVee);
        }

        return slots;
    }

    /**
     * @return The number of slots taken up by ammo.
     */
    public int getAmmoSlots() {
        // Small SV ammo occupies the same slot as the weapon.
        if (isSmallSupportVehicle()) {
            return 0;
        }

        int slots = 0;
        Set<String> foundAmmo = new HashSet<>();
        for (Mounted<?> ammo : supportVee.getAmmo()) {
            // don't count one shot
            if (ammo.isOneShotAmmo()) {
                continue;
            }

            if (ammo.getType() instanceof AmmoType ammoType) {
                if (!foundAmmo.contains(ammoType.getAmmoType() + ":" + ammoType.getRackSize())) {
                    slots++;
                    foundAmmo.add(ammoType.getAmmoType() + ":" + ammoType.getRackSize());
                }
            }
        }

        return slots;
    }

    /**
     * @return The number of slots taken by miscellaneous equipment.
     */
    public int getMiscEquipSlots() {
        int slots = 0;

        for (Mounted<?> mounted : supportVee.getMisc()) {
            // Skip armor and jump jets
            if ((EquipmentType.getArmorType(mounted.getType()) == EquipmentType.T_ARMOR_UNKNOWN)
                  && !mounted.getType().hasFlag(MiscType.F_JUMP_JET)) {
                slots += mounted.getType().getSupportVeeSlots(supportVee);
            }
        }

        // Jump jets take a single slot regardless of the number.
        if (supportVee.hasWorkingMisc(MiscType.F_JUMP_JET)) {
            slots++;
        }

        return slots;
    }

    public static final int INDEX_FIRST_CLASS = 0;
    public static final int INDEX_SECOND_CLASS = 1;
    public static final int INDEX_STD_CREW = 2;
    public static final int INDEX_STEERAGE = 3;

    /**
     * Calculates capacity of quarters above the minimum crew requirement. Only quarters above the minimum crew
     * requirement take up equipment slots. Second class quarters are considered passenger accommodations and always
     * count toward slots. Others are assigned to the least bulky type first.
     *
     * @param supportVehicle A support vehicle
     *
     * @return An array of the count of each type of quarters that require slots. See INDEX_* constants for indices.
     */
    public static int[] extraCrewQuartersCount(Entity supportVehicle) {
        int firstClass = 0;
        int stdCrew = 0;
        int steerage = 0;
        int[] retVal = { 0, 0, 0, 0 };

        for (Transporter transporter : supportVehicle.getTransports()) {
            if (transporter instanceof FirstClassQuartersCargoBay transportBay) {
                firstClass += Double.valueOf(transportBay.getCapacity()).intValue();
            } else if (transporter instanceof SecondClassQuartersCargoBay transportBay) {
                retVal[INDEX_SECOND_CLASS] += Double.valueOf(transportBay.getCapacity()).intValue();
            } else if (transporter instanceof CrewQuartersCargoBay transportBay) {
                stdCrew += Double.valueOf(transportBay.getCapacity()).intValue();
            } else if (transporter instanceof SteerageQuartersCargoBay transportBay) {
                steerage += Double.valueOf(transportBay.getCapacity()).intValue();
            }
        }

        int extraCrew = firstClass + stdCrew + steerage - Compute.getFullCrewSize(supportVehicle);

        if ((extraCrew > 0) && (steerage > 0)) {
            retVal[INDEX_STEERAGE] = Math.min(extraCrew, steerage);
            extraCrew -= retVal[INDEX_STEERAGE];
        }

        if ((extraCrew > 0) && (stdCrew > 0)) {
            retVal[INDEX_STD_CREW] = Math.min(extraCrew, stdCrew);
            extraCrew -= retVal[INDEX_STD_CREW];
        }

        if ((extraCrew > 0) && (firstClass > 0)) {
            retVal[INDEX_FIRST_CLASS] = Math.min(extraCrew, firstClass);
        }

        return retVal;
    }

    /**
     * Calculates the number of equipment slots taken up by crew quarters. Quarters for minimum crew do not take up
     * slots.
     *
     * @return The number of equipment slots required by crew quarters.
     */
    public int getCrewSlots() {
        int[] excess = extraCrewQuartersCount(getEntity());
        return (int) Math.ceil(excess[INDEX_FIRST_CLASS] / 5.0)
              + (int) Math.ceil(excess[INDEX_SECOND_CLASS] / 20.0)
              + (int) Math.ceil(excess[INDEX_STD_CREW] / 20.0)
              + (int) Math.ceil(excess[INDEX_STEERAGE] / 50.0);
    }

    /**
     * Each distinct bay requires a slot, regardless of size. All {@link InfantryCompartment} is treated as a single
     * bay.
     *
     * @return The number of slots required by transporters.
     */
    public int getTransportSlots() {
        int slots = 0;
        boolean foundTroopSpace = false;

        for (Transporter transporter : supportVee.getTransports()) {
            if ((transporter instanceof Bay transportBay) && !transportBay.isQuarters()) {
                slots++;
            } else if ((transporter instanceof InfantryCompartment) && !foundTroopSpace) {
                slots++;
                foundTroopSpace = true;
            }
        }

        return slots;
    }
}
