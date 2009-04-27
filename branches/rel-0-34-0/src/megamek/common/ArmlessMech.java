/*
 * MegaMek -
 *  Copyright (C) 2006
 *    Ben Mazur (bmazur@sev.org)
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

public class ArmlessMech extends BipedMech {

    /**
     * 
     */
    private static final long serialVersionUID = 1333922747670982513L;

    public ArmlessMech(String inGyroType, String inCockpitType) {
        this(getGyroTypeForString(inGyroType),
                getCockpitTypeForString(inCockpitType));
    }

    public ArmlessMech() {
        this(Mech.GYRO_STANDARD, Mech.COCKPIT_STANDARD);
    }

    public ArmlessMech(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);

        for (int i = 0; i < 4; i++) {
            // remove arm actuators
            setCritical(LOC_RARM, i, null);
            setCritical(LOC_LARM, i, null);
        }
    }

    /**
     * Returns true if the entity can flip its arms
     */
    public boolean canFlipArms() {
        return false;
    }

    /**
     * Sets the internal structure for the mech.
     * 
     * @param head head
     * @param ct center torso
     * @param t right/left torso
     * @param arm right/left arm
     * @param leg right/left leg
     */
    public void setInternal(int head, int ct, int t, int arm, int leg) {
        initializeInternal(head, LOC_HEAD);
        initializeInternal(ct, LOC_CT);
        initializeInternal(t, LOC_RT);
        initializeInternal(t, LOC_LT);
        initializeInternal(0, LOC_RARM);
        initializeInternal(0, LOC_LARM);
        initializeInternal(leg, LOC_RLEG);
        initializeInternal(leg, LOC_LLEG);
    }

    protected double getArmActuatorCost() {
        return 0.0;
    }

    /**
     * Checks if the entity is getting up. If so, returns the target roll for
     * the piloting skill check.
     */
    public PilotingRollData checkGetUp(MoveStep step, int overallMoveType) {
        PilotingRollData roll = super.checkGetUp(step);
        roll.addModifier(4, "armless Mech");
        return roll;
    }

    /**
     * Gets the location that excess damage transfers to
     */
    public HitData getTransferLocation(HitData hit) {
        if (hit.getLocation() == LOC_LARM || hit.getLocation() == LOC_RARM) {
            return new HitData(LOC_NONE);
        }
        return super.getTransferLocation(hit);
    }
}
