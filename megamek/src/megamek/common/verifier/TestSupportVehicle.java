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

/**
 * Author: arlith
 */
public class TestSupportVehicle extends TestTank {

    /**
     * Gives the weight of a single point of armor at a particular BAR for a 
     * given tech level.
     */
    public static final float[][] SV_ARMOR_WEIGHT = 
        {{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
         {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
         {.040f, .025f, .016f, .013f, .012f, .011f},
         {.060f, .038f, .024f, .019f, .017f, .016f},
         {.000f, .050f, .032f, .026f, .023f, .021f},
         {.000f, .063f, .040f, .032f, .028f, .026f},
         {.000f, .000f, .048f, .038f, .034f, .032f},
         {.000f, .000f, .056f, .045f, .040f, .037f},
         {.000f, .000f, .000f, .051f, .045f, .042f},
         {.000f, .000f, .000f, .057f, .051f, .047f},
         {.000f, .000f, .000f, .063f, .056f, .052f},};
    
    public TestSupportVehicle(Tank sv, TestEntityOption options,
            String fileString) {
        super(sv, options, fileString);
    }
    
    public String printWeightStructure() {
        return StringUtil.makeLength(
                "Chassis: ", getPrintSize() - 5)
                + TestEntity.makeWeightString(getWeightStructure()) + "\n";
    }
    
    public float getWeightControls() {
        return 0;
    }
    
    public float getTankWeightLifting() {
        return 0;
    }
        
    public float getWeightArmor() {
        int totalArmorPoints = 0;
        for (int loc = 0; loc < getEntity().locations(); loc++) {
            totalArmorPoints += getEntity().getOArmor(loc);
        }
        int bar = getEntity().getBARRating(Tank.LOC_BODY);
        int techRating = getEntity().getStructuralTechRating();
        float weight = totalArmorPoints * SV_ARMOR_WEIGHT[bar][techRating];
        if (getEntity().getWeight() < 5) {
            return TestEntity.floor(weight, CEIL_KILO);
        } else {
            return TestEntity.ceil(weight, CEIL_HALFTON);
        }
        
    }    

}
