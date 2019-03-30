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
import megamek.common.util.StringUtil;

import java.util.EnumSet;
import java.util.Set;

/**
 * Author: arlith
 */
public class TestSupportVehicle extends TestTank {

    /**
     * Support vehicle categories for construction purposes. Most of these match with a particular movement
     * mode, but the construction rules treat naval and rail units as single types.
     */
    public enum SVType {
        AIRSHIP (EntityMovementMode.AIRSHIP),
        FIXED_WING (EntityMovementMode.AERODYNE),
        HOVERCRAFT (EntityMovementMode.HOVER),
        NAVAL (EntityMovementMode.NAVAL),
        TRACKED (EntityMovementMode.TRACKED),
        VTOL (EntityMovementMode.VTOL),
        WHEELED (EntityMovementMode.WHEELED),
        WIGE (EntityMovementMode.WIGE),
        RAIL (EntityMovementMode.RAIL),
        SATELLITE (EntityMovementMode.STATION_KEEPING);

        public final EntityMovementMode defaultMovementMode;

        SVType(EntityMovementMode defaultMovementMode) {
            this.defaultMovementMode = defaultMovementMode;
        }

        static Set<SVType> allBut(SVType type) {
            return EnumSet.complementOf(EnumSet.of(type));
        }

        static Set<SVType> allBut(SVType first, SVType... rest) {
            return EnumSet.complementOf(EnumSet.of(first, rest));
        }
    }

    /**
     * Additional construction data for chassis mods, used to determine whether they are legal for particular
     * units.
     */
    public enum ChassisModification implements ITechnologyDelegator {
        AMPHIBIOUS ("AmphibiousChassisMod", SVType.allBut(SVType.HOVERCRAFT, SVType.NAVAL)),
        ARMORED ("ArmoredChassisMod", SVType.allBut(SVType.AIRSHIP)),
        BICYCLE ("BicycleChassisMod", EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED)),
        CONVERTIBLE ("ConvertibleChassisMod", EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED, SVType.TRACKED), true),
        DUNE_BUGGY ("DuneBuggyChassisMod", EnumSet.of(SVType.WHEELED)),
        ENVIRONMENTAL_SEALING ("EnvironmentalSealingChassisMod", EnumSet.allOf(SVType.class)),
        EXTERNAL_POWER_PICKUP ("ExternalPowerPickupChassisMod", EnumSet.of(SVType.RAIL)),
        HYDROFOIL ("HydrofoilChassisMod", EnumSet.of(SVType.NAVAL)),
        MONOCYCLE ("MonocycleChassisMod", EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED), true),
        OFFROAD ("OffroadChassisMod", EnumSet.of(SVType.WHEELED)),
        OMNI ("OmniChassisMod"),
        PROP ("PropChassisMod", EnumSet.of(SVType.FIXED_WING)),
        SNOWMOBILE ("SnowmobileChassisMod", EnumSet.of(SVType.WHEELED, SVType.TRACKED)),
        STOL ("STOLChassisMod", EnumSet.of(SVType.FIXED_WING)),
        SUBMERSIBLE ("SubmersibleChassisMod", EnumSet.of(SVType.NAVAL)),
        TRACTOR ("TractorChassisMod", EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.NAVAL, SVType.RAIL)),
        TRAILER ("TrailerChassisMod", EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.RAIL)),
        ULTRA_LIGHT ("UltraLightChassisMod", true),
        VSTOL ("VSTOLChassisMod", EnumSet.of(SVType.FIXED_WING));

        public final MiscType equipment;
        public final boolean smallOnly;
        public final Set<SVType> allowedTypes;

        ChassisModification(String eqTypeKey) {
            this(eqTypeKey, EnumSet.allOf(SVType.class), false);
        }

        ChassisModification(String eqTypeKey, boolean smallOnly) {
            this(eqTypeKey, EnumSet.allOf(SVType.class), smallOnly);
        }

        ChassisModification(String eqTypeKey, Set<SVType> allowedTypes) {
            this(eqTypeKey, allowedTypes, false);
        }

        ChassisModification(String eqTypeKey, Set<SVType> allowedTypes, boolean smallOnly) {
            this.equipment = (MiscType) EquipmentType.get(eqTypeKey);
            this.allowedTypes = allowedTypes;
            this.smallOnly = smallOnly;
        }

        @Override
        public ITechnology getTechSource() {
            return equipment;
        }
    }

    /**
     * Additional construction data for engine types, used to determine which ones are available for which
     * vehicle types.
     */
    public enum SVEngine implements ITechnologyDelegator {
        STEAM (Engine.STEAM, EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.AIRSHIP, SVType.NAVAL)),
        COMBUSTION (Engine.COMBUSTION_ENGINE),
        BATTERY (Engine.BATTERY, true),
        FUEL_CELL (Engine.FUEL_CELL, true),
        SOLAR (Engine.SOLAR, EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.AIRSHIP, SVType.FIXED_WING,
                SVType.NAVAL, SVType.WIGE), true),
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
     * Gives the weight of a single point of armor at a particular BAR for a 
     * given tech level.
     */
    public static final double[][] SV_ARMOR_WEIGHT = 
        {{0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
         {0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
         {.040, .025, .016, .013, .012, .011},
         {.060, .038, .024, .019, .017, .016},
         {.000, .050, .032, .026, .023, .021},
         {.000, .063, .040, .032, .028, .026},
         {.000, .000, .048, .038, .034, .032},
         {.000, .000, .056, .045, .040, .037},
         {.000, .000, .000, .051, .045, .042},
         {.000, .000, .000, .057, .051, .047},
         {.000, .000, .000, .063, .056, .052},};
    
    public TestSupportVehicle(Tank sv, TestEntityOption options,
            String fileString) {
        super(sv, options, fileString);
    }
    
    @Override
    public String printWeightStructure() {
        return StringUtil.makeLength(
                "Chassis: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightStructure()) + "\n";
    }
    
    @Override
    public double getWeightControls() {
        return 0;
    }
    
    @Override
    public double getTankWeightLifting() {
        return 0;
    }
        
    @Override
    public double getWeightArmor() {
        int totalArmorPoints = 0;
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            totalArmorPoints += getEntity().getOArmor(loc);
        }
        int bar = getEntity().getBARRating(Tank.LOC_BODY);
        int techRating = getEntity().getArmorTechRating();
        double weight = totalArmorPoints * SV_ARMOR_WEIGHT[bar][techRating];
        if (getEntity().getWeight() < 5) {
            return TestEntity.floor(weight, Ceil.KILO);
        } else {
            return TestEntity.ceil(weight, Ceil.HALFTON);
        }
        
    }    

}
