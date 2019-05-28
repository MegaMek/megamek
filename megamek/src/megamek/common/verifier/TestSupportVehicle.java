/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.common.verifier;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.util.StringUtil;
import megamek.common.weapons.flamers.VehicleFlamerWeapon;
import megamek.common.weapons.lasers.CLChemicalLaserWeapon;

import java.math.BigInteger;
import java.util.*;

/**
 * Author: arlith
 */
public class TestSupportVehicle extends TestEntity {

    /**
     * Support vehicle categories for construction purposes. Most of these match with a particular movement
     * mode, but the construction rules treat naval and rail units as single types.
     */
    public enum SVType {
        AIRSHIP (300, EntityMovementMode.AIRSHIP,
                new double[]{0.2, 0.25, 0.3}, new double[]{0.004, 0.008, 0.012}),
        FIXED_WING (200, EntityMovementMode.AERODYNE,
                new double[]{0.8, 0.1, 0.15}, new double[]{0.005, 0.01, 0.015}),
        HOVERCRAFT (100, EntityMovementMode.HOVER,
                new double[]{0.2, 0.25, 0.3}, new double[]{0.0025, 0.004, 0.007}),
        NAVAL (300, EntityMovementMode.NAVAL,
                new double[]{0.12, 0.15, 0.17}, new double[]{0.004, 0.007, 0.009}),
        TRACKED (200, EntityMovementMode.TRACKED,
                new double[]{0.13, 0.15, 0.25}, new double[]{0.006, 0.013, 0.025}),
        VTOL (60, EntityMovementMode.VTOL,
                new double[]{0.2, 0.25, 0.3}, new double[]{0.002, 0.0025, 0.004}),
        WHEELED (160, EntityMovementMode.WHEELED,
                new double[]{0.12, 0.15, 0.18}, new double[]{0.0025, 0.0075, 0.015}),
        WIGE (240, EntityMovementMode.WIGE,
                new double[]{0.12, 0.15, 0.17}, new double[]{0.003, 0.005, 0.006}),
        RAIL (600, EntityMovementMode.RAIL,
                new double[]{0.15, 0.2, 0.3}, new double[]{0.003, 0.004, 0.005}),
        SATELLITE (300, EntityMovementMode.STATION_KEEPING,
                new double[]{0.8, 0.12, 0.16}, new double[]{0.1, 0.1, 0.1});

        /** The maximum tonnage for a large support vehicle of this type; for airship this is the
         *  maximum for a medium for now.
         */
        public final int maxTonnage;
        public final EntityMovementMode defaultMovementMode;
        /** Used to calculate chassis weight for small, medium, large weight classes */
        private final double[] baseChassisValue;
        /** Used to calculate engine weight for small, medium, large weight classes */
        private final double[] baseEngineValue;

        SVType(int maxTonnage, EntityMovementMode defaultMovementMode,
               double[] baseChassisValue, double[] baseEngineValue) {
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
         * Finds the enum value corresponding to a support vehicle based on movement mode.
         *
         * @param entity The support vehicle
         * @return       The support vehicle type, or {@code null} if the entity's movement type is not
         *               a valid one for a support vehicle.
         */
        public static @Nullable
        SVType getVehicleType(Entity entity) {
            switch (entity.getMovementMode()) {
                case AIRSHIP:
                    return AIRSHIP;
                case AERODYNE:
                    return FIXED_WING;
                case HOVER:
                    return HOVERCRAFT;
                case TRACKED:
                    return TRACKED;
                case WHEELED:
                    return WHEELED;
                case NAVAL:
                case HYDROFOIL:
                case SUBMARINE:
                    return NAVAL;
                case VTOL:
                    return VTOL;
                case WIGE:
                    return WIGE;
                case RAIL:
                case MAGLEV:
                    return RAIL;
                case STATION_KEEPING:
                    return SATELLITE;
                default:
                    return null;
            }
        }

        /**
         * The base chassis value is used for calculating the chassis weight.
         *
         * @param sizeClass The {@link EntityWeightClass} of the support vehicle.
         * @return          The base chassis value. Returns 0 if not a support vehicle weight class.
         */
        public double getBaseChassisValue(int sizeClass) {
            int index = sizeClass - EntityWeightClass.WEIGHT_SMALL_SUPPORT;
            if ((index >= 0) && (index < baseChassisValue.length)) {
                return baseChassisValue[index];
            } else {
                return 0.0;
            }
        }

        /**
         * The base chassis value is used for calculating the chassis weight.
         *
         * @param sv A support vehicle
         * @return   The base chassis value. Returns 0 if the entity is not a support vehicle.
         */
        public static double getBaseChassisValue(Entity sv) {
            SVType type = getVehicleType(sv);
            if (null != type) {
                return type.getBaseChassisValue(sv.getWeightClass());
            }
            return 0.0;
        }

        /**
         * The base engine value is used for calculating the engine weight.
         *
         * @param sizeClass The {@link EntityWeightClass} of the support vehicle.
         * @return          The base engine value. Returns 0 if not a support vehicle weight class.
         */
        public double getBaseEngineValue(int sizeClass) {
            int index = sizeClass - EntityWeightClass.WEIGHT_SMALL_SUPPORT;
            if ((index >= 0) && (index < baseEngineValue.length)) {
                return baseEngineValue[index];
            } else {
                return 0.0;
            }
        }

        /**
         * The base engine value is used for calculating the engine weight.
         *
         * @param sv A support vehicle
         * @return   The base engine value. Returns 0 if the entity is not a support vehicle.
         */
        public static double getBaseEngineValue(Entity sv) {
            SVType type = getVehicleType(sv);
            if (null != type) {
                return type.getBaseEngineValue(sv.getWeightClass());
            }
            return 0.0;
        }
    }

    /**
     * Additional construction data for chassis mods, used to determine whether they are legal for particular
     * units.
     */
    public enum ChassisModification implements ITechnologyDelegator {
        AMPHIBIOUS (1.75,"AmphibiousChassisMod", SVType.allBut(SVType.HOVERCRAFT, SVType.NAVAL)),
        ARMORED (1.5,"ArmoredChassisMod", SVType.allBut(SVType.AIRSHIP)),
        BICYCLE (0.75,"BicycleChassisMod", EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED)),
        CONVERTIBLE (1.1,"ConvertibleChassisMod", EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED, SVType.TRACKED), true),
        DUNE_BUGGY (1.5,"DuneBuggyChassisMod", EnumSet.of(SVType.WHEELED)),
        ENVIRONMENTAL_SEALING (2.0,"EnvironmentalSealingChassisMod", EnumSet.allOf(SVType.class)),
        EXTERNAL_POWER_PICKUP (1.1,"ExternalPowerPickupChassisMod", EnumSet.of(SVType.RAIL)),
        HYDROFOIL (1.7,"HydrofoilChassisMod", EnumSet.of(SVType.NAVAL)),
        MONOCYCLE (0.5,"MonocycleChassisMod", EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED), true),
        OFFROAD (1.5,"OffroadChassisMod", EnumSet.of(SVType.WHEELED)),
        OMNI (1.0,"OmniChassisMod"),
        PROP (1.2,"PropChassisMod", EnumSet.of(SVType.FIXED_WING)),
        SNOWMOBILE (1.75,"SnowmobileChassisMod", EnumSet.of(SVType.WHEELED, SVType.TRACKED)),
        STOL (1.5,"STOLChassisMod", EnumSet.of(SVType.FIXED_WING)),
        SUBMERSIBLE (1.8,"SubmersibleChassisMod", EnumSet.of(SVType.NAVAL)),
        TRACTOR (1.2,"TractorChassisMod", EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.NAVAL, SVType.RAIL)),
        TRAILER (0.8,"TrailerChassisMod", EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.RAIL)),
        ULTRA_LIGHT (0.5,"UltraLightChassisMod", true),
        VSTOL (2.0,"VSTOLChassisMod", EnumSet.of(SVType.FIXED_WING));

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
         * @param sv The support vehicle
         * @return   Whether the mod is valid for the vehicle
         */
        public boolean validFor(Entity sv) {
            return sv.isSupportVehicle() && allowedTypes.contains(SVType.getVehicleType(sv))
                    && (!smallOnly || (sv.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT));
        }

        /**
         * Checks for compatibility between different chassis modifications
         *
         * @param other Another chassis mod
         * @return      Whether this chassis mod can be installed on the same vehicle as another mod.
         */
        public boolean compatibleWith(ChassisModification other) {
            switch (this) {
                case ARMORED:
                    return other != ULTRA_LIGHT;
                case ULTRA_LIGHT:
                    return other != ARMORED;
                case BICYCLE:
                    return other != MONOCYCLE;
                case MONOCYCLE:
                    return other != BICYCLE;
                case SNOWMOBILE:
                    return other != DUNE_BUGGY
                            && other != AMPHIBIOUS
                            && other != OFFROAD;
                case DUNE_BUGGY:
                    return other != SNOWMOBILE
                            && other != AMPHIBIOUS
                            && other != OFFROAD;
                case AMPHIBIOUS:
                case OFFROAD:
                    return other != SNOWMOBILE
                            && other != DUNE_BUGGY;
                default:
                    return true;
            }
        }

        @Override
        public ITechnology getTechSource() {
            return equipment;
        }

        /**
         * Find the enum value that corresponds to an {@link EquipmentType} instance.
         *
         * @param eq The equipment to match
         * @return   The corresponding enum value, or {@code null} if there is no match.
         */
        public @Nullable
        static ChassisModification getChassisMod(EquipmentType eq) {
            for (ChassisModification mod : values()) {
                if (mod.equipment.equals(eq)) {
                    return mod;
                }
            }
            return null;
        }
    }

    /**
     * Additional construction data for engine types, used to determine which ones are available for which
     * vehicle types.
     */
    public enum SVEngine implements ITechnologyDelegator {
        STEAM (Engine.STEAM, EnumSet.of(SVType.WHEELED, SVType.TRACKED,
                SVType.AIRSHIP, SVType.NAVAL, SVType.RAIL)),
        COMBUSTION (Engine.COMBUSTION_ENGINE, SVType.allBut(SVType.SATELLITE)),
        BATTERY (Engine.BATTERY, true),
        FUEL_CELL (Engine.FUEL_CELL, true),
        SOLAR (Engine.SOLAR, EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.AIRSHIP, SVType.FIXED_WING,
                SVType.NAVAL, SVType.WIGE, SVType.SATELLITE), true),
        FISSION (Engine.FISSION),
        FUSION (Engine.NORMAL_ENGINE),
        MAGLEV (Engine.MAGLEV, EnumSet.of(SVType.RAIL)),
        EXTERNAL (Engine.NONE, EnumSet.of(SVType.RAIL), true);

        /** The engine type constant used to create a new {@link Engine}. */
        public final Engine engine;
        /** Fixed-wing must have prop chassis mod to use an electric engine */
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
         * @return       The enum value for the engine, or {@code null} if it is not a valid SV engine type.
         */
        public @Nullable
        static SVEngine getEngineType(Engine engine) {
            if (null != engine) {
                for (SVEngine e : values()) {
                    if (e.engine.getEngineType() == engine.getEngineType()) {
                        return e;
                    }
                }
            }
            return null;
        }

        /**
         * @param type The support vehicle type
         * @return     Whether the engine is valid for the support vee.
         */
        public boolean isValidFor(SVType type) {
            return allowedTypes.contains(type);
        }

        /**
         * @param entity A support vehicle
         * @return       Whether the engine is valid for the support vee.
         */
        public boolean isValidFor(Entity entity) {
            return isValidFor(SVType.getVehicleType(entity));
        }

        @Override
        public ITechnology getTechSource() {
            return engine;
        }
    }

    /**
     * Tech advancement data for structural components with variable tech levels (structure,
     * armor, engine). This is assembled from the tables on TM, p. 122 and IO, p. 49, primitive construction
     * rules (IO, p. 120-121) and a pending proposal to the rules committee for E.
     */
    public static final TechAdvancement[] TECH_LEVEL_TA = {
            new TechAdvancement(ITechnology.TECH_BASE_ALL).setTechRating(ITechnology.RATING_A)
                    .setAdvancement(ITechnology.DATE_PS, ITechnology.DATE_PS, ITechnology.DATE_PS)
                    .setAvailability(ITechnology.RATING_A, ITechnology.RATING_A,
                    ITechnology.RATING_A, ITechnology.RATING_A),
            new TechAdvancement(ITechnology.TECH_BASE_ALL).setTechRating(ITechnology.RATING_B)
                    .setAdvancement(ITechnology.DATE_ES, ITechnology.DATE_ES, ITechnology.DATE_ES)
                    .setAvailability(ITechnology.RATING_B, ITechnology.RATING_B,
                    ITechnology.RATING_B, ITechnology.RATING_A),
            new TechAdvancement(ITechnology.TECH_BASE_ALL).setTechRating(ITechnology.RATING_C)
                    .setAdvancement(2250, 2300, 2305).setApproximate(true, false, false)
                    .setPrototypeFactions(ITechnology.F_TA).setProductionFactions(ITechnology.F_TA)
                    .setAvailability(ITechnology.RATING_C, ITechnology.RATING_B,
                    ITechnology.RATING_B, ITechnology.RATING_B),
            new TechAdvancement(ITechnology.TECH_BASE_ALL).setTechRating(ITechnology.RATING_D)
                    .setAdvancement(2420, 2430, 2435).setApproximate(true, true, false)
                    .setPrototypeFactions(ITechnology.F_TH).setProductionFactions(ITechnology.F_TH)
                    .setAvailability(ITechnology.RATING_C, ITechnology.RATING_C,
                    ITechnology.RATING_C, ITechnology.RATING_B),
            new TechAdvancement(ITechnology.TECH_BASE_ALL).setTechRating(ITechnology.RATING_E)
                    .setISAdvancement(2557, 2571, 3055).setClanAdvancement(2557, 2571, 2815)
                    .setAvailability(ITechnology.RATING_D, ITechnology.RATING_F,
                    ITechnology.RATING_D, ITechnology.RATING_C),
            new TechAdvancement(ITechnology.TECH_BASE_ALL).setTechRating(ITechnology.RATING_F)
                    .setISAdvancement(ITechnology.DATE_NONE, ITechnology.DATE_NONE, 3065).setISApproximate(false, false, true)
                    .setClanAdvancement(2820, 2825, 2830).setClanApproximate(true, true, false)
                    .setAvailability(ITechnology.RATING_E, ITechnology.RATING_E,
                    ITechnology.RATING_D, ITechnology.RATING_C)
    };

    /**
     * The chassis weight multiplier for tech ratings A-F
     */
    private static final double[] STRUCTURE_TECH_MULTIPLIER = {
            1.6, 1.3, 1.15, 1.0, 0.85, 0.66
    };

    /**
     * Filters all vehicle armor according to given tech constraints. Standard armor is treated as basic
     * support vehicle armor.
     *
     * @param techManager
     * @return
     */
    public static List<EquipmentType> legalArmorsFor(ITechManager techManager) {
        if (techManager.getTechLevel().ordinal() < SimpleTechLevel.ADVANCED.ordinal()) {
            return Collections.singletonList(EquipmentType.get(EquipmentType.armorNames[EquipmentType.T_ARMOR_STANDARD]));
        }
        List<EquipmentType> retVal = new ArrayList<>();
        for (int at = 0; at < EquipmentType.armorNames.length; at++) {
            if (at == EquipmentType.T_ARMOR_PATCHWORK) {
                continue;
            }
            String name = EquipmentType.getArmorTypeName(at, techManager.useClanTechBase());
            EquipmentType eq = EquipmentType.get(name);
            if ((null != eq)
                    && eq.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT)
                    && techManager.isLegal(eq)) {
                retVal.add(eq);
            }
            if (techManager.useMixedTech()) {
                name = EquipmentType.getArmorTypeName(at, !techManager.useClanTechBase());
                EquipmentType eq2 = EquipmentType.get(name);
                if ((null != eq2) && (eq != eq2)
                        && eq2.hasFlag(MiscType.F_SUPPORT_TANK_EQUIPMENT)
                        && techManager.isLegal(eq2)) {
                    retVal.add(eq2);
                }
            }
        }
        return retVal;
    }

    /**
     * The maximum number of armor points a support vehicle is computed by multiplying the total tonnage by a factor
     * determined by the vehicle type and adding four.
     *
     * @param vee The support vehicle
     * @return    The maximum number of armor points. If the entity cannot be identified as a support vehicle, returns 0.
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
     * Calculates the weight of each point of armor. For standard SV armor this is based on the tech and BAR
     * ratings. For advanced armors this is the reciprocal of the number of points per ton.
     *
     * @param vee The support vehicle
     * @return    The weight of each armor point in tons, rounded to the kilogram.
     */
    public static double armorWeightPerPoint(Entity vee) {
        final int at = vee.getArmorType(vee.firstArmorIndex());
        if (at == EquipmentType.T_ARMOR_STANDARD) {
            return EquipmentType.getSupportVehicleArmorWeightPerPoint(vee.getBARRating(vee.firstArmorIndex()),
                    vee.getArmorTechRating());
        } else {
            final double ppt = 16.0 * EquipmentType.getArmorPointMultiplier(
                    at, vee.getArmorTechLevel(vee.firstArmorIndex()));
            return round(1.0 / ppt, Ceil.KILO);
        }
    }

    private final Entity supportVee;
    /** Used by support tanks for calculation of turret weight */
    private final TestTank testTank;
    /** Used by fixed wing, airship, and satellite for some calculations and validation */
    private final TestAero testAero;
    
    public TestSupportVehicle(Entity sv, TestEntityOption options,
            String fileString) {
        super(options, sv.getEngine(), null, null);
        this.supportVee = sv;
        testTank = sv instanceof Tank ? new TestTank((Tank) sv, options, fileString) : null;
        testAero = sv instanceof Aero ? new TestAero((Aero) sv, options, fileString) : null;
    }
    
    @Override
    public String printWeightStructure() {
        return StringUtil.makeLength(
                "Chassis: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightStructure()) + "\n";
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
    public boolean isMech() {
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
    public boolean isProtomech() {
        return false;
    }

    private double roundWeight(double val) {
        if (supportVee.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            return round(val, Ceil.KILO);
        } else {
            return ceil(val, Ceil.HALFTON);
        }
    }

    @Override
    public double getWeightStructure() {
        double weight = supportVee.getWeight();
        weight *= SVType.getBaseChassisValue(supportVee);
        weight *= STRUCTURE_TECH_MULTIPLIER[supportVee.getStructuralTechRating()];
        for (Mounted m : supportVee.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_CHASSIS_MODIFICATION)) {
                ChassisModification mod = ChassisModification.getChassisMod(m.getType());
                if (null != mod) {
                    weight *= mod.multiplier;
                } else {
                    DefaultMmLogger.getInstance().warning(getClass(), "getWeightStructure()",
                            "Could not find multiplier for " + m.getType().getName() + " chassis mod.");
                }
            }
        }
        return roundWeight(weight);
    }

    public double getFuelTonnage() {
        if (supportVee instanceof Aero) {
            return ((Aero) supportVee).getFuelTonnage();
        } else {
            return ((Tank) supportVee).getFuelTonnage();
        }
    }

    private double getWeightFireControl() {
        for (Mounted m : supportVee.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_BASIC_FIRECONTROL)
                    || m.getType().hasFlag(MiscType.F_ADVANCED_FIRECONTROL)) {
                return m.getType().getTonnage(supportVee);
            }
        }
        return 0.0;
    }

    private double getWeightCrewAccomodations() {
        double weight = 0;
        for (Transporter t : supportVee.getTransports()) {
            if ((t instanceof Bay) && ((Bay) t).isQuarters()) {
                weight += ((Bay) t).getWeight();
            }
        }
        if (supportVee instanceof FixedWingSupport) {
            weight += ((FixedWingSupport) supportVee).getExtraCrewSeats() * 0.5;
        } else {
            weight += ((Tank) supportVee).getExtraCrewSeats() * 0.5;
        }
        return roundWeight(weight);
    }

    @Override
    public double getWeightControls() {
        return getWeightFireControl() + getWeightCrewAccomodations();
    }

    @Override
    public double getWeightMisc() {
        return getTankWeightTurret()
                + getTankWeightDualTurret();
    }

    public double getTankWeightTurret() {
        if (null != testTank) {
            return testTank.getTankWeightTurret();
        } else {
            return 0.0;
        }
    }

    public double getTankWeightDualTurret() {
        if (null != testTank) {
            return testTank.getTankWeightDualTurret();
        } else {
            return 0.0;
        }
    }

    @Override
    public double getWeightPowerAmp() {
        if (supportVee.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            return 0.0;
        }

        if (!engine.isFusion() && (engine.getEngineType() != Engine.FISSION)) {
            double weight = 0;
            for (Mounted m : supportVee.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_ENERGY) && !(wt instanceof CLChemicalLaserWeapon) && !(wt instanceof VehicleFlamerWeapon)) {
                    weight += wt.getTonnage(supportVee);
                }
                if ((m.getLinkedBy() != null) && (m.getLinkedBy().getType() instanceof
                        MiscType) && m.getLinkedBy().getType().
                        hasFlag(MiscType.F_PPC_CAPACITOR)) {
                    weight += m.getLinkedBy().getType().getTonnage(supportVee);
                }
            }
            return roundWeight(weight / 10);
        }
        return 0.0;
    }

    @Override
    protected boolean includeMiscEquip(MiscType eq) {
        // fire control is counted with control system weight and chassis mods are part of
        // the structure weight
        final BigInteger exclude = MiscType.F_BASIC_FIRECONTROL.or(MiscType.F_ADVANCED_FIRECONTROL)
                .or(MiscType.F_CHASSIS_MODIFICATION);
        return !eq.hasFlag(exclude);
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
        if (supportVee.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            return 0;
        }
        return heatNeutralHSRequirement();
    }

    @Override
    public String printWeightMisc() {
        if (null != testTank) {
            return testTank.printWeightMisc();
        } else {
            return getWeightPowerAmp() != 0 ? StringUtil.makeLength(
                    "Power Amp:", getPrintSize() - 5)
                    + TestEntity.makeWeightString(getWeightPowerAmp()) + "\n" : "";
        }
    }

    @Override
    public String printWeightControls() {
        String fireCon = "";
        for (Mounted m : supportVee.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_BASIC_FIRECONTROL)
                    || m.getType().hasFlag(MiscType.F_ADVANCED_FIRECONTROL)) {
                fireCon = StringUtil.makeLength(m.getName(), getPrintSize() - 5)
                        + TestEntity.makeWeightString(m.getType().getTonnage(supportVee)) + "\n";
                break;
            }
        }
        double weight = getWeightCrewAccomodations();
        String crewStr = weight > 0 ?
                StringUtil.makeLength("Crew Accomodations:", getPrintSize() - 5)
                    + TestEntity.makeWeightString(weight) + "\n" : "";
        return fireCon + crewStr;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return false;
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        return false;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append(getName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");
        buff.append(printTechLevel());
        buff.append("Intro year: ").append(supportVee.getYear());
        buff.append(printSource());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append(" (").append(
                    calculateWeight()).append(")\n");
        }

        buff.append(printWeightCalculation()).append("\n");
        printFailedEquipment(buff);
        return buff;
    }

    @Override
    public String getName() {
        return "Support Vehicle: " + supportVee.getDisplayName();
    }

    @Override
    public double getWeightArmor() {
        if (supportVee.hasPatchworkArmor()
                || (supportVee.getArmorType(supportVee.firstArmorIndex()) != EquipmentType.T_ARMOR_STANDARD)) {
            return super.getWeightArmor();
        }
        int totalArmorPoints = 0;
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            totalArmorPoints += getEntity().getOArmor(loc);
        }
        int bar = getEntity().getBARRating(supportVee.firstArmorIndex());
        int techRating = getEntity().getArmorTechRating();
        double weight = totalArmorPoints * EquipmentType.getSupportVehicleArmorWeightPerPoint(bar, techRating);
        return roundWeight(weight);
    }

    /**
     * @return   The total slot space
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
            int at = supportVee.getArmorType(supportVee.firstArmorIndex());
            if (at == EquipmentType.T_ARMOR_STANDARD) {
                // Support vehicle armor takes slots like ferro-fibrous at BAR 10/TL E/F
                if (supportVee.getBARRating(supportVee.firstArmorIndex()) == 10) {
                    if (supportVee.getArmorTechRating() == ITechnology.RATING_E) {
                        return AdvancedSVArmor.FERRO_FIBROUS.space;
                    } else if (supportVee.getArmorTechRating() == ITechnology.RATING_F) {
                        return AdvancedSVArmor.CLAN_FERRO_FIBROUS.space;
                    }
                }
                return 0;
            } else {
                AdvancedSVArmor armor = AdvancedSVArmor.getArmor(at,
                        TechConstants.isClan(supportVee.getArmorTechLevel(supportVee.firstArmorIndex())));
                if (null != armor) {
                    return armor.space;
                } else {
                    return 0;
                }
            }
        } else {
            int space = 0;
            for (int loc = 0; loc < supportVee.locations(); loc++) {
                AdvancedSVArmor armor = AdvancedSVArmor.getArmor(supportVee.getArmorType(loc),
                        TechConstants.isClan(supportVee.getArmorTechLevel(loc)));
                if (null != armor) {
                    space += armor.patchworkSpace;
                }
            }
            return space;
        }
    }

    /**
     * @return The number of slots taken up by weapons
     */
    public int getWeaponSlots() {
        int slots = 0;
        for (Mounted m : supportVee.getWeaponList()) {
            slots += m.getType().getSupportVeeSlots(supportVee);
        }
        return slots;
    }

    /**
     * @return The number of slots taken up by ammo.
     */
    public int getAmmoSlots() {
        int slots = 0;
        Set<String> foundAmmo = new HashSet<>();
        for (Mounted ammo : supportVee.getAmmo()) {
            // don't count oneshot ammo
            if (ammo.isOneShotAmmo()) {
                continue;
            }
            AmmoType at = (AmmoType) ammo.getType();
            if (!foundAmmo.contains(at.getAmmoType() + ":" + at.getRackSize())) {
                slots++;
                foundAmmo.add(at.getAmmoType() + ":" + at.getRackSize());
            }
        }
        return slots;
    }

    /**
     * @return The number of slots taken by miscellaneous equipment.
     */
    public int getMiscEquipSlots() {
        int slots = 0;
        for (Mounted m : supportVee.getMisc()) {
            slots += m.getType().getSupportVeeSlots(supportVee);
        }
        return slots;
    }

    public int getCrewSlots() {
        int firstClass = 0;
        int secondClass = 0;
        int steerage = 0;
        int extraSeat = 0;
        for (Transporter t : supportVee.getTransports()) {
            if (t instanceof FirstClassQuartersCargoBay) {
                firstClass++;
            } else if ((t instanceof SecondClassQuartersCargoBay)
                    || (t instanceof CrewQuartersCargoBay)) {
                secondClass++;
            } else if (t instanceof SteerageQuartersCargoBay) {
                steerage++;
            }
        }
        return (int) (Math.ceil(firstClass / 5.0) + Math.ceil(secondClass / 20.0) + Math.ceil(steerage / 50.0))
                + (supportVee instanceof FixedWingSupport ?
                    ((FixedWingSupport) supportVee).getExtraCrewSeats() :
                    ((Tank) supportVee).getExtraCrewSeats());
    }

    /**
     * Each distinct bay requires a slot, regardless of size. All {@link TroopSpace} is treated as a single
     * bay.
     *
     * @return The number of slots required by transporters.
     */
    public int getTransportSlots() {
        int slots = 0;
        boolean foundTroopSpace = false;
        for (Transporter t : supportVee.getTransports()) {
            if ((t instanceof Bay) && !((Bay) t).isQuarters()) {
                slots++;
            } else if ((t instanceof TroopSpace) && !foundTroopSpace) {
                slots++;
                foundTroopSpace = true;
            }
        }
        return slots;
    }

    public enum AdvancedSVArmor {
        CLAN_FERRO_FIBROUS(EquipmentType.T_ARMOR_FERRO_FIBROUS, 1, 1, true),
        CLAN_FERRO_ALUM(EquipmentType.T_ARMOR_ALUM, 1, 1, true),
        FERRO_LAMELLOR(EquipmentType.T_ARMOR_FERRO_LAMELLOR, 2, 1, true),
        CLAN_REACTIVE(EquipmentType.T_ARMOR_REACTIVE, 1, 1, true),
        CLAN_REFLECTIVE(EquipmentType.T_ARMOR_REFLECTIVE, 1, 1, true),
        ANTI_PENETRATIVE_ABLATION(
                EquipmentType.T_ARMOR_ANTI_PENETRATIVE_ABLATION, 1, 1, false),
        BALLISTIC_REINFORCED(
                EquipmentType.T_ARMOR_BALLISTIC_REINFORCED, 2, 1, false),
        FERRO_FIBROUS(EquipmentType.T_ARMOR_ALUM, 2, 1, false),
        FERRO_ALUM(EquipmentType.T_ARMOR_ALUM, 2, 1, false),
        FERRO_FIBROUS_PROTO(EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO, 3, 1, false),
        FERRO__ALUM_PROTO(EquipmentType.T_ARMOR_FERRO_ALUM_PROTO, 3, 1, false),
        HEAVY_FERRO_FIBROUS(EquipmentType.T_ARMOR_HEAVY_FERRO, 4, 2, false),
        LIGHT_FERRO_FIBROUS(EquipmentType.T_ARMOR_LIGHT_FERRO, 1, 1, false),
        HEAVY_FERRO_ALUM(EquipmentType.T_ARMOR_HEAVY_ALUM, 4, 2, false),
        LIGHT_FERRO_ALUM(EquipmentType.T_ARMOR_LIGHT_ALUM, 1, 1, false),
        PRIMITIVE(EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER, 0, 0, false),
        REACTIVE(EquipmentType.T_ARMOR_REACTIVE, 3, 1, false),
        REFLECTIVE(EquipmentType.T_ARMOR_REFLECTIVE, 2, 1, false),
        STEALTH_VEHICLE(EquipmentType.T_ARMOR_STEALTH_VEHICLE, 2, 1, false);

        public final int armorType;
        /**
         * The type, corresponding to types defined in
         * <code>EquipmentType</code>.
         */
        public final EquipmentType eqType;

        /**
         * The number of spaces occupied by the armor type.
         */
        public final int space;

        /**
         * The number of weapon spaces occupied by patchwork armor.
         */
        public final int patchworkSpace;

        /**
         * Denotes whether this armor is Clan or not.
         */
        public final boolean isClan;

        AdvancedSVArmor(int at, int space, int patchworkSpace, boolean clan){
            this.armorType = at;
            eqType = EquipmentType.get(EquipmentType.getArmorTypeName(at, clan));
            this.space = space;
            this.patchworkSpace = patchworkSpace;
            isClan = clan;
        }

        /**
         * Given an armor type, return the <code>AdvancedSVArmor</code> instance that
         * represents that type.
         *
         * @param at The armor type.
         * @param c  Whether this armor type is Clan or not.
         * @return   The <code>AdvancedSVArmor</code> that correspondes to the given
         *              type or null if no match was found.
         */
        public static @Nullable AdvancedSVArmor getArmor(int at, boolean c){
            for (AdvancedSVArmor a : values()){
                if ((a.armorType == at) && (a.isClan == c)) {
                    return a;
                }
            }
            return null;
        }
    }
}
