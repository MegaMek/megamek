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
import megamek.common.options.OptionsConstants;


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
    	
    	int max = maxSecondaryWeapons(inf);
    	if (inf.getSecondaryN() > max) {
            buff.append("Number of secondary weapons exceeds maximum of " + max).append("\n\n");
            correct = false;
    	}
    	
    	if (inf.getSecondaryWeapon() != null) {
        	int secondaryCrew = inf.getSecondaryWeapon().getCrew();
        	if (inf.getCrew() != null) {
        		if (inf.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
        			secondaryCrew--;
        		}
        		if (inf.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
        			secondaryCrew--;
        		}
        	}
        	secondaryCrew = Math.max(secondaryCrew, 1);
        	if (secondaryCrew * inf.getSecondaryN() > inf.getSquadSize()) {
                buff.append("Secondary weapon crew requirement exceeds squad size.").append("\n\n");
                correct = false;
        	}
    	}
    	
    	max = maxSquadSize(inf.getMovementMode(), inf.hasMicrolite() || (inf.getAllUMUCount() > 1));
    	if (inf.getSquadSize() > max) {
            buff.append("Maximum squad size is " + max + "\n\n");
            correct = false;    		
    	}

    	max = maxUnitSize(inf.getMovementMode(), inf.hasMicrolite() || (inf.getAllUMUCount() > 1),
    	        inf.hasSpecialization(Infantry.COMBAT_ENGINEERS | Infantry.MOUNTAIN_TROOPS));
    	if (inf.getShootingStrength() > max) {
            buff.append("Maximum platoon size is " + max + "\n\n");
            correct = false;    		
    	}

        return correct;
    }
    
    public static int maxSecondaryWeapons(Infantry inf) {
    	int max = 2;
    	if (inf.getMovementMode() == EntityMovementMode.VTOL) {
    		max = inf.hasMicrolite()?0 : 1;
    	} else if (inf.getMovementMode() == EntityMovementMode.INF_UMU) {
    		max = inf.getAllUMUCount();
    	}
    	if (inf.hasSpecialization(Infantry.COMBAT_ENGINEERS)) {
    		max = 0;
    	}
    	if (inf.hasSpecialization(Infantry.MOUNTAIN_TROOPS | Infantry.PARAMEDICS)) {
    		max = 1;
    	}
    	if (inf.getCrew() != null) {
    		if (inf.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
    			max++;
    		}
    		if (inf.hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
    			max++;
    		}
    	}
    	return max;
    }
    
    /**
     * Maximum squad size based on motive type
     * 
     * @param movementMode  The platoon's movement mode
     * @param alt           True indicates that VTOL is microlite and INF_UMU is motorized.
     * @return              The maximum size of a squad.
     */
    public static int maxSquadSize(EntityMovementMode movementMode, boolean alt) {
    	switch(movementMode) {
        	case HOVER:
        	case SUBMARINE:
        		return 5;
        	case WHEELED:
        		return 6;
        	case TRACKED:
        		return 7;
        	case INF_UMU:
        		 return alt? 6 : 10;
        	case VTOL:
        		return alt? 2 : 4;
        	default:
        		return 10;
    	}
    }
    
    public static int maxUnitSize(EntityMovementMode movementMode, boolean alt, boolean engOrMountain) {
    	int max;
    	switch(movementMode) {
        	case INF_UMU:
        		if (alt) {
        			max = 12;
        		} else {
        			max = 30;
        		}
        		break;
        	case HOVER:
        	case SUBMARINE:
        		max = 20;
        		break;
        	case WHEELED:
        		max = 24;
        		break;
        	case TRACKED:
        		max = 28;
        		break;
        	case VTOL:
        		max = maxSquadSize(movementMode, alt) * 4;
        		break;
        	default:
        		max = 30;
        		break;
    	}
    	if (engOrMountain) {
    		max = Math.min(max, 20);
    	}
    	return max;
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