
/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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


// This class is for ranges.  It simply has a min/short/med/long ranges

public class RangeType {


    public static final int  RANGE_SHORT = 0;
    public static final int  RANGE_MED = 1;
    public static final int  RANGE_LONG = 2;
    public static final int  RANGE_OUT = 3;

    public int r_min;
    public int r_short;
    public int r_med;
    public int r_long;
    
    public RangeType(int r_min, int r_short, int r_med, int r_long) {
	this.r_min = r_min;
	this.r_short = r_short;
	this.r_med = r_med;
	this.r_long = r_long;
    }

    public RangeType(int r_short, int r_med, int r_long) {
	this(0, r_short, r_med, r_long);
    }
    
    // returns short/med/long range
    public int getRangeID(int range) {
	if (range <= r_short)
	    return RANGE_SHORT;
	else if (range <= r_med)
	    return RANGE_MED;
	else if (range <= r_long)
	    return RANGE_LONG;
	else 
	    return RANGE_OUT;
    }
    
    // This quickly returns the minimum range modifier
    public int getMinRangeMod(int range) {
	if (range <= r_min)
	    return (r_min - range + 1);
	else
	    return 0;
    }
}



