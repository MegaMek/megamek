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
import megamek.common.EntityMovementMode;
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
        return correctEntity(buff, getEntity().getTechLevel());
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
    	Infantry inf = (Infantry)getEntity();
    	boolean correct = true;
    	if (skip()) {
    		return true;
    	}
    	
    	if (inf.getSecondaryN() > 2) {
            buff.append("Number of secondary weapons exceeds maximum of 2").append("\n\n");
            correct = false;
    	}
    	if (inf.getMovementMode() == EntityMovementMode.VTOL) {
    		if (inf.getSecondaryN() > 1) {
                buff.append("Number of secondary weapons exceeds maximum of 1").append("\n\n");
                correct = false;
    		}
    	}
    	
    	if (inf.getSecondaryWeapon().getCrew() * inf.getSecondaryN() > inf.getSquadSize()) {
            buff.append("Secondary weapon crew requirement exceeds squad size.").append("\n\n");
            correct = false;
    	}

    	if (inf.getMovementMode() == EntityMovementMode.HOVER
    			&& inf.getSquadSize() > 5) {
            buff.append("Maximum squad size for Mechanized/Hover is 5.\n\n");
            correct = false;
    	} else if (inf.getMovementMode() == EntityMovementMode.TRACKED
    			&& inf.getSquadSize() > 7) {
            buff.append("Maximum squad size for Mechanized/Tracked is 7.\n\n");
            correct = false;
    	} else if (inf.getMovementMode() == EntityMovementMode.WHEELED
    			&& inf.getSquadSize() > 6) {
            buff.append("Maximum squad size for Mechanized/Wheeled is 6.\n\n");
            correct = false;
    	} else if (inf.getMovementMode() == EntityMovementMode.VTOL) {
    		
    	}

        return correct;
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