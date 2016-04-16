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

/*
 * Author: Jay Lawson (Taharqa)
 */

package megamek.common.verifier;

import megamek.common.Entity;
import megamek.common.Infantry;


public class TestInfantry extends TestEntity {
    private Infantry infantry;
    
    public TestInfantry(Infantry infantry, TestEntityOption option, String fileString) {
        super(option, null, null, null);
        this.infantry = infantry;
        this.fileString = fileString;
    }
    
    @Override
    public Entity getEntity() {
        return infantry;
    }

    @Override
    public boolean isTank() {
        return false;
    }

    @Override
    public boolean isMech() {
        return false;
    }
    
    @Override
    public boolean isAero() {
        return false;
    }

    @Override
    public double getWeightControls() {
        return 0;
    }

    @Override
    public double getWeightMisc() {
        return 0;
    }

    @Override
    public double getWeightHeatSinks() {
        return 0;
    }

    @Override
    public double getWeightEngine() {
        return 0;
    }
    
    @Override
    public double getWeightStructure() {
        return 0;
    }
    
    @Override
    public double getWeightArmor() {
        return 0;
    }
    
    @Override
    public boolean hasDoubleHeatSinks() {
        return false;
    }

    @Override
    public int getCountHeatSinks() {
        return 0;
    }

    @Override
    public String printWeightMisc() {
        return null;
    }

    @Override
    public String printWeightControls() {
        return null;
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
        return null;
    }

    @Override
    public String getName() {
        return "Infantry: " + infantry.getDisplayName();    
    }

    @Override
    public double getWeightPowerAmp() {
        return 0;
    }
    
    @Override
    public double calculateWeight() {
        return infantry.getWeight();
    }
    
}