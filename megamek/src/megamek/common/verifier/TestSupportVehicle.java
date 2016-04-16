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
