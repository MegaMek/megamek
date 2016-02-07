/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common;


/**
 * weight class limits and names
 */
public class EntityWeightClass {

    // BA, Mech / Vee / Generic Weights
    public static final int WEIGHT_ULTRA_LIGHT = 0; // Also used for BA "PAL" - Not yet truly implemented for mechs
    public static final int WEIGHT_LIGHT = 1; // Conventional Fighters (50t max weight) always return this.
    public static final int WEIGHT_MEDIUM = 2;
    public static final int WEIGHT_HEAVY = 3;
    public static final int WEIGHT_ASSAULT = 4;
    public static final int WEIGHT_COLOSSAL = 5;
    public static final int WEIGHT_SUPER_HEAVY = 5;

    // AeroSpace Units
    public static final int WEIGHT_SMALL_CRAFT = 6; // Only a single weight class for Small Craft
    public static final int WEIGHT_SMALL_DROP = 7;
    public static final int WEIGHT_MEDIUM_DROP = 8;
    public static final int WEIGHT_LARGE_DROP = 9;
    public static final int WEIGHT_SMALL_WAR = 10;
    public static final int WEIGHT_LARGE_WAR = 11;
    
    // Support Vehicles
    public static final int WEIGHT_SMALL_SUPPORT = 12;
    public static final int WEIGHT_MEDIUM_SUPPORT = 13;
    public static final int WEIGHT_LARGE_SUPPORT = 14;

    // Total number of unique unit weight designations. Should be 1 more than the number above.
    public static final int SIZE = 15;
    private static String[] classAppends = { "0", "1", "2", "3", "4", "5", "SC", "DS.7", "DS.8", "DS.9", "JS.10", "JS.11", "SV.12", "SV.13", "SV.14" };


    private static float[] BAWeightLimits = { 0.4f, 0.75f, 1, 1.5f, 2 };
    private static float[] mechWeightLimits = { 15, 35, 55, 75, 100, 135 };
    private static float[] vehicleWeightLimits = { 0, 39, 59, 79, 100, 300 }; // One padding 0
    private static float[] wheeledSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 80, 160 }; // Twelve padding 0s
    private static float[] trackedSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 100, 200 }; // Twelve padding 0s
    private static float[] hoverSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 50, 100 }; // Twelve padding 0s
    private static float[] vtolSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 30, 60 }; // Twelve padding 0s
    private static float[] wigeSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 80, 160 }; // Twelve padding 0s
    //private static float[] airshipSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 300, 1000 }; // Twelve padding 0s
    //private static float[] fixedwingSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 100, 200 }; // Twelve padding 0s
    private static float[] navalSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 300, 100000 }; // Twelve padding 0s
    private static float[] railSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 300, 600 }; // Twelve padding 0s
    //private static float[] mobilestructuresSupportVehicleWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4.999f, 80, 160 }; // Twelve padding 0s
    private static float[] ASFWeightLimits = { 0, 45, 70, 100 }; // One padding 0
    private static float[] dropshipWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 2499, 9999, 100000 }; // Seven padding 0s
    private static float[] jumpshipWeightLimits = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 749999, 2500000 }; // Ten padding 0s
    private static float[] GEWeightLimits = { 0, 15, 40, 90, 150 }; // One padding 0
    private static float[] protoWeightLimits = { 0, 3, 5, 7, 9 }; // One padding 0

    public static int getWeightClass(float tonnage, String type) {
        int i;

        if (type.equals("BattleArmor")) {
            for (i = 0; i < (BAWeightLimits.length - 1); i++) {
                if (tonnage <= BAWeightLimits[i]) {
                break;
            }
        }
        } else if (type.equals("Infantry")) {
            return WEIGHT_LIGHT;
        } else if (type.equals("VTOL") || type.equals("Naval") || type.equals("Tank")) {
            for (i = WEIGHT_LIGHT; i < (vehicleWeightLimits.length - 1); i++) { // Started at late to bypass padding & save a loop execution
                if (tonnage <= vehicleWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals("Gun Emplacement")) {
            for (i = WEIGHT_LIGHT; i < (GEWeightLimits.length - 1); i++) { // Started at late to bypass padding & save a loop execution
                if (tonnage <= GEWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals("Mek")) {
            for (i = 0; i < (mechWeightLimits.length - 1); i++) {
                if (tonnage <= mechWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals("ProtoMek")) {
            for (i = WEIGHT_LIGHT; i < (protoWeightLimits.length - 1); i++) { // Started late to bypass padding & save a loop execution
                if (tonnage <= protoWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals("Space Station") || type.equals("Warship") || type.equals("Jumpship")) {
            for (i = WEIGHT_SMALL_WAR; i < (jumpshipWeightLimits.length - 1); i++) { // Started late to bypass padding & save a loop execution
                if (tonnage <= jumpshipWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals("Dropship")) {
            for (i = WEIGHT_SMALL_DROP; i < (dropshipWeightLimits.length - 1); i++) { // Started late to bypass padding & save a loop execution
                if (tonnage <= dropshipWeightLimits[i]) {
                    break;
                }
            }
        } else if (type.equals("Small Craft")) {
            return WEIGHT_SMALL_CRAFT;
        } else if (type.equals("Conventional Fighter")) {
            return WEIGHT_LIGHT;
        } else if (type.equals("Aero")) {
            for (i = WEIGHT_LIGHT; i < (ASFWeightLimits.length - 1); i++) { // Started late to bypass padding & save a loop execution
                if (tonnage <= ASFWeightLimits[i]) {
                    break;
                }
            }
        } else {
            for (i = 0; i < (mechWeightLimits.length - 1); i++) {
                if (tonnage <= mechWeightLimits[i]) {
                    break;
                }
            }
        }

        return i;
    }

    public static int getSupportWeightClass(float tonnage, String type) {
        int i = 0;

	    if (type.equals("Wheeled")) {
			for (i = WEIGHT_SMALL_SUPPORT; i < (wheeledSupportVehicleWeightLimits.length - 1); i++) {
				if (tonnage <= wheeledSupportVehicleWeightLimits[i]) {
	                break;
	            }
			}
	    } else if (type.equals("Tracked")) {
			for (i = WEIGHT_SMALL_SUPPORT; i < (trackedSupportVehicleWeightLimits.length - 1); i++) {
				if (tonnage <= trackedSupportVehicleWeightLimits[i]) {
	                break;
	            }
			}
	    } else if (type.equals("Hover")) {
			for (i = WEIGHT_SMALL_SUPPORT; i < (hoverSupportVehicleWeightLimits.length - 1); i++) {
				if (tonnage <= hoverSupportVehicleWeightLimits[i]) {
	                break;
	            }
			}
	    } else if (type.equals("VTOL")) {
			for (i = WEIGHT_SMALL_SUPPORT; i < (vtolSupportVehicleWeightLimits.length - 1); i++) {
				if (tonnage <= vtolSupportVehicleWeightLimits[i]) {
	                break;
	            }
			}
	    } else if (type.equals("WiGE")) {
			for (i = WEIGHT_SMALL_SUPPORT; i < (wigeSupportVehicleWeightLimits.length - 1); i++) {
				if (tonnage <= wigeSupportVehicleWeightLimits[i]) {
	                break;
	            }
			}
	    } else if (type.equals("Naval")) {
			for (i = WEIGHT_SMALL_SUPPORT; i < (navalSupportVehicleWeightLimits.length - 1); i++) {
				if (tonnage <= navalSupportVehicleWeightLimits[i]) {
	                break;
	            }
			}
	    } else if (type.equals("Submarine")) {
			for (i = WEIGHT_SMALL_SUPPORT; i < (navalSupportVehicleWeightLimits.length - 1); i++) {
				if (tonnage <= navalSupportVehicleWeightLimits[i]) {
	                break;
	            }
			}
	    } else if (type.equals("Rail")) {
			for (i = WEIGHT_SMALL_SUPPORT; i < (railSupportVehicleWeightLimits.length - 1); i++) {
				if (tonnage <= railSupportVehicleWeightLimits[i]) {
	                break;
	            }
			}
	    } else if (type.equals("MagLev")) {
			for (i = WEIGHT_SMALL_SUPPORT; i < (railSupportVehicleWeightLimits.length - 1); i++) {
				if (tonnage <= railSupportVehicleWeightLimits[i]) {
	                break;
	            }
			}
	    }

        return i;
    }

    public static int getWeightClass(float tonnage, Entity en) {
        int i;

        // Order of IF statements is important!! Any subclasses must come before their parent class!
        if (en instanceof Mech) {
            for (i = 0; i < (mechWeightLimits.length - 1); i++) {
                if (tonnage <= mechWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof Jumpship) { // Also handles Warships & Space Stations
            for (i = WEIGHT_SMALL_WAR; i < (jumpshipWeightLimits.length - 1); i++) { // Started late to bypass padding & save a loop execution
                if (tonnage <= jumpshipWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof Dropship) {
            for (i = WEIGHT_SMALL_DROP; i < (dropshipWeightLimits.length - 1); i++) { // Started late to bypass padding & save a loop execution
                if (tonnage <= dropshipWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof ConvFighter) {
            return WEIGHT_LIGHT;
        } else if (en instanceof SmallCraft) {
            return WEIGHT_SMALL_CRAFT;
        } else if (en instanceof Aero) {
            for (i = WEIGHT_LIGHT; i < (ASFWeightLimits.length - 1); i++) { // Started late to bypass padding & save a loop execution
                if (tonnage <= ASFWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof BattleArmor) {
            for (i = 0; i < (BAWeightLimits.length - 1); i++) {
                if (tonnage <= BAWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof Infantry) { // For now infantry don't have weights, put them all under light?
            return WEIGHT_LIGHT;
        } else if (en instanceof GunEmplacement) {
            for (i = WEIGHT_LIGHT; i < (GEWeightLimits.length - 1); i++) { // Started late to bypass padding & save a loop execution
                if (tonnage <= GEWeightLimits[i]) {
                    break;
                }
            }
        } else if ((en instanceof SupportTank) || (en instanceof SupportVTOL)) {
        	switch (en.getMovementMode()) {
        	case WHEELED:
        		for (i = WEIGHT_SMALL_SUPPORT; i < (wheeledSupportVehicleWeightLimits.length - 1); i++) {
        			if (tonnage <= wheeledSupportVehicleWeightLimits[i]) {
                        break;
                    }
        		}
        		break;
        	case TRACKED:
        		for (i = WEIGHT_SMALL_SUPPORT; i < (trackedSupportVehicleWeightLimits.length - 1); i++) {
        			if (tonnage <= trackedSupportVehicleWeightLimits[i]) {
                        break;
                    }
        		}
        		break;
        	case HOVER:
        		for (i = WEIGHT_SMALL_SUPPORT; i < (hoverSupportVehicleWeightLimits.length - 1); i++) {
        			if (tonnage <= hoverSupportVehicleWeightLimits[i]) {
                        break;
                    }
        		}
        		break;
        	case VTOL:
        		for (i = WEIGHT_SMALL_SUPPORT; i < (vtolSupportVehicleWeightLimits.length - 1); i++) {
        			if (tonnage <= vtolSupportVehicleWeightLimits[i]) {
                        break;
                    }
        		}
        		break;
        	case WIGE:
        		for (i = WEIGHT_SMALL_SUPPORT; i < (wigeSupportVehicleWeightLimits.length - 1); i++) {
        			if (tonnage <= wigeSupportVehicleWeightLimits[i]) {
                        break;
                    }
        		}
        		break;
        	case NAVAL:
        		for (i = WEIGHT_SMALL_SUPPORT; i < (navalSupportVehicleWeightLimits.length - 1); i++) {
        			if (tonnage <= navalSupportVehicleWeightLimits[i]) {
                        break;
                    }
        		}
        		break;
        	case SUBMARINE:
        		for (i = WEIGHT_SMALL_SUPPORT; i < (navalSupportVehicleWeightLimits.length - 1); i++) {
        			if (tonnage <= navalSupportVehicleWeightLimits[i]) {
                        break;
                    }
        		}
        		break;
        	case RAIL:
        		for (i = WEIGHT_SMALL_SUPPORT; i < (railSupportVehicleWeightLimits.length - 1); i++) {
        			if (tonnage <= railSupportVehicleWeightLimits[i]) {
                        break;
                    }
        		}
        		break;
        	case MAGLEV:
        		for (i = WEIGHT_SMALL_SUPPORT; i < (railSupportVehicleWeightLimits.length - 1); i++) {
        			if (tonnage <= railSupportVehicleWeightLimits[i]) {
                        break;
                    }
        		}
        		break;
        	default:
        		i = 0;
        		break;
        	}
        } else if (en instanceof Tank) {
            for (i = WEIGHT_LIGHT; i < (vehicleWeightLimits.length - 1); i++) { // Started late to bypass padding & save a loop execution
                if (tonnage <= vehicleWeightLimits[i]) {
                    break;
                }
            }
        } else if (en instanceof Protomech) {
            for (i = 0; i < (protoWeightLimits.length - 1); i++) {
                if (tonnage <= protoWeightLimits[i]) {
                    break;
                }
            }
        } else { // And... we'll default to the mech chart.
            for (i = 0; i < (mechWeightLimits.length - 1); i++) {
                if (tonnage <= mechWeightLimits[i]) {
                    break;
                }
            }
        }
        return i;
    }

    public static float getClassLimit(int wClass, Entity en) {
        // Order of IF statements is important!! Any subclasses must come before their parent class!
        if (en instanceof Mech) {
        if ((wClass >= 0) && (wClass < SIZE)) {
                return mechWeightLimits[wClass];
            }
        } else if (en instanceof Jumpship) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return jumpshipWeightLimits[wClass];
            }
        } else if (en instanceof Dropship) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return dropshipWeightLimits[wClass];
            }
        } else if (en instanceof ConvFighter) {
            return 50;
        } else if (en instanceof SmallCraft) {
            return 200;
        } else if (en instanceof Aero) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return ASFWeightLimits[wClass];
            }
        } else if (en instanceof BattleArmor) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return BAWeightLimits[wClass];
            }
        } else if (en instanceof Infantry) {
            return 500; // Not a clue for infantry, since I can't find anything for them.
        } else if (en instanceof GunEmplacement) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return GEWeightLimits[wClass];
            }
        } else if (en instanceof Tank) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return vehicleWeightLimits[wClass];
            }
        } else if (en instanceof Protomech) {
            if ((wClass >= 0) && (wClass < SIZE)) {
                return protoWeightLimits[wClass];
            }
        } else { // And... we'll default to mechs
            if ((wClass >= 0) && (wClass < SIZE)) {
                return mechWeightLimits[wClass];
            }
        }
        throw new IllegalArgumentException("Unknown Weight Class");
    }

    /**
     * Get the weight class name g iven a weight class define and a unitType.
     * The unitType is a string that matches the unit type returned from a
     * MechSummary.
     *
     * @param wClass
     * @param unitType
     * @return
     */
    public static String getClassName(int wClass, String unitType, boolean isSupport) {
        if (unitType.equals("Jumpship")) {
            return Messages.getString("EntityWeightClass.JS." + wClass);
        }
        if (unitType.equals("Dropship")) {
            return Messages.getString("EntityWeightClass.DS." + wClass);
        }
        if (unitType.equals("Small Craft")) {
            return Messages.getString("EntityWeightClass.SC");
        }
        if (isSupport && (unitType.equals("VTOL") || unitType.equals("Tank"))) {
            return Messages.getString("EntityWeightClass.SV." + wClass);
        }
        if ((wClass >= 0) && (wClass < SIZE)) {
            return Messages.getString("EntityWeightClass." + wClass);
        }
        throw new IllegalArgumentException("Unknown Weight Class in getClassName(int, en)");
    }

    public static String getClassName(int wClass, Entity en) {
        if (en instanceof Jumpship) {
            return Messages.getString("EntityWeightClass.JS." + wClass);
        }
        if (en instanceof Dropship) {
            return Messages.getString("EntityWeightClass.DS." + wClass);
        }
        if (en instanceof SmallCraft) {
            return Messages.getString("EntityWeightClass.SC");
        }
        if (en instanceof SupportTank || en instanceof SupportVTOL) {
        	return Messages.getString("EntityWeightClass.SV." + wClass);
        }
        if ((wClass >= 0) && (wClass < SIZE)) {
            return Messages.getString("EntityWeightClass." + wClass);
        }
        throw new IllegalArgumentException("Unknown Weight Class in getClassName(int, en)");
    }

    public static String getClassName(int nameVal) {
        if ((nameVal >= 0) && (nameVal < SIZE)) {
            return Messages.getString("EntityWeightClass." + classAppends[nameVal]);
        }
        throw new IllegalArgumentException("Unknown Weight Class in getClassName(int)");
    }

}
