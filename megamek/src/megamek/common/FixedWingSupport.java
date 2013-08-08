/*
 * MegaAero - Copyright (C) 2010 Jason Tighe This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * Created on 10/31/2010
 */
package megamek.common;

/**
 * @author Jason Tighe
 */
public class FixedWingSupport extends ConvFighter {


    /**
     *
     */
    private static final long serialVersionUID = 347113432982248518L;


    public static final int LOC_BODY = 5;

    private static String[] LOCATION_ABBRS =
        { "NOS", "LWG", "RWG", "AFT", "WNG", "BOD" };
    private static String[] LOCATION_NAMES =
        { "Nose", "Left Wing", "Right Wing", "Aft", "Wings", "Body" };
    private int[] barRating;

    public FixedWingSupport() {
        super();
        damThresh = new int[] { 0, 0, 0, 0, 0, 0 };
        barRating = new int[locations()];
    }

    public void setBARRating(int rating, int loc) {
        barRating[loc] = rating;
    }

    public void setBARRating(int rating) {
        for (int i = 0; i < locations(); i++) {
            barRating[i] = rating;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getBARRating()
     */
    @Override
    public int getBARRating(int loc) {
        return barRating[loc];
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#hasBARArmor()
     */
    @Override
    public boolean hasBARArmor(int loc) {
        return true;
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public int locations() {
        return 6;
    }

    @Override
    public void autoSetSI() {
        initializeSI(getOriginalWalkMP());
    }


    @Override
    public boolean isVSTOL() {
        if (hasWorkingMisc(MiscType.F_VSTOL_CHASSIS, -1)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSTOL() {
        if (hasWorkingMisc(MiscType.F_STOL_CHASSIS, -1)) {
            return true;
        }
        return false;
    }

    @Override
    public int getBattleForceSize() {
        //The tables are on page 356 of StartOps
        if (getWeight() < 5) {
            return 1;
        }
        if (getWeight() < 100) {
            return 2;
        }

        return 3;
    }

    @Override
    protected int calculateWalk() {
        return getOriginalWalkMP();
    }

    @Override
    public void autoSetMaxBombPoints() {
        // fixed wing support craft need external stores hardpoints to be able to carry bombs
        int bombpoints = 0;
        for (Mounted misc : getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_EXTERNAL_STORES_HARDPOINT)) {
                bombpoints++;
            }
        }
        maxBombPoints = bombpoints;
    }

    @Override
    public void initializeThresh(int loc) {
        int bar = getBARRating(loc);
        if (bar == 10) {
            setThresh((int) Math.ceil(getArmor(loc) / 10.0), loc);
        } else if (bar >= 2) {
            setThresh(1, loc);
        } else {
            setThresh(0, loc);
        }
    }

    public float getBaseEngineValue() {
        if (getWeight() < 5) {
            return 0.005f;
        } else if (getWeight() <= 100) {
            return 0.01f;
        } else {
            return 0.015f;
        }
    }

    public float getBaseChassisValue() {
        if (getWeight() < 5) {
            return 0.08f;
        } else if (getWeight() <= 100) {
            return 0.1f;
        } else {
            return 0.15f;
        }
    }

    public int getTotalSlots() {
        return 5 + (int) Math.floor(getWeight() / 10);
    }
    
    public long getEntityType(){
        return Entity.ETYPE_AERO | Entity.ETYPE_CONV_FIGHTER | Entity.ETYPE_FIXED_WING_SUPPORT;
    }
}