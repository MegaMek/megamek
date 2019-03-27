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
import megamek.common.Tank;
import megamek.common.util.StringUtil;

import java.util.EnumSet;
import java.util.Set;

/**
 * Author: arlith
 */
public class TestSupportVehicle extends TestTank {

    /**
     * Types of support vehicles used for determining compatibility with chassis mods and engine types.
     * This is nearly identical with motive type, but there some differences in naval, rail, and aerospace
     * units.
     */
    public enum SVType {
        AIRSHIP,
        FIXED_WING,
        HOVERCRAFT,
        NAVAL,
        TRACKED,
        VTOL,
        WHEELED,
        WIGE,
        RAIL,
        SATELLITE;

        static Set<SVType> allBut(SVType type) {
            return EnumSet.complementOf(EnumSet.of(type));
        }

        static Set<SVType> allBut(SVType first, SVType... rest) {
            return EnumSet.complementOf(EnumSet.of(first, rest));
        }
    };

    public enum ChassisModification {
        AMPHIBIOUS ("AmphibiousChassisMod", SVType.allBut(SVType.HOVERCRAFT, SVType.NAVAL)),
        ARMORED ("ArmoredChassisMod", SVType.allBut(SVType.AIRSHIP)),
        BICYCLE ("BicycleChassisMod", EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED)),
        CONVERTIBLE ("ConvertibleChassisMod", EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED, SVType.TRACKED), true),
        DUNE_BUGGY ("DuneBuggyChassisMod", EnumSet.of(SVType.WHEELED)),
        ENVIRONMENTAL_SEALING ("EnvironmentalSealingChassisMod", EnumSet.allOf(SVType.class)),
        HYDROFOIL ("HydrofoilChassisMod", EnumSet.of(SVType.NAVAL)),
        MONOCYCLE ("MonocycleChassisMod", EnumSet.of(SVType.HOVERCRAFT, SVType.WHEELED), true),
        OFFROAD ("OffroadChassisMod", EnumSet.of(SVType.WHEELED)),
        OMNI ("OmniChassisMod"),
        PROP ("PropChassisMod", EnumSet.of(SVType.FIXED_WING)),
        SNOWMOBILE ("SnowmobileChassisMod", EnumSet.of(SVType.WHEELED, SVType.TRACKED)),
        STOL ("STOLChassisMod", EnumSet.of(SVType.FIXED_WING)),
        SUBMERSIBLE ("SubmersibleChassisMod", EnumSet.of(SVType.NAVAL)),
        TRACTOR ("TractorChassisMod", EnumSet.of(SVType.WHEELED, SVType.TRACKED, SVType.NAVAL)),
        TRAILER ("TrailerChassisMod", EnumSet.of(SVType.WHEELED, SVType.TRACKED)),
        ULTRA_LIGHT ("UltraLightChassisMod", true),
        VSTOL ("VSTOLChassisMod", EnumSet.of(SVType.FIXED_WING));

        public final String eqTypeKey;
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
            this.eqTypeKey = eqTypeKey;
            this.allowedTypes = allowedTypes;
            this.smallOnly = smallOnly;
        }
    }

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
