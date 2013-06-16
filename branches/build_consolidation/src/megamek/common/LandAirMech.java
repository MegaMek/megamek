/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common;

public class LandAirMech extends BipedMech {

    /**
     *
     */
    private static final long serialVersionUID = -8118673802295814548L;

    public static final int LAM_AVIONICS = 15;

    public static final int LAM_LANDING_GEAR = 16;

    public static final String systemNames[] =
        { "Life Support", "Sensors", "Cockpit", "Engine", "Gyro", null, null, "Shoulder", "Upper Arm", "Lower Arm", "Hand", "Hip", "Upper Leg", "Lower Leg", "Foot", "Avionics", "Landing Gear" };

    private int fuel;

    public LandAirMech(int inGyroType, int inCockpitType) {
        super(inGyroType, inCockpitType);
        setTechLevel(TechConstants.T_IS_ADVANCED);
        setCritical(Mech.LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_LT, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_RT, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_LT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
        setCritical(Mech.LOC_RT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
        setCritical(Mech.LOC_CT, 10, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
        setFuel(80);
    }

    public void setFuel(int fuel) {
        this.fuel = fuel;
    }

    public int getFuel() {
        return fuel;
    }

    @Override
    public String getSystemName(int index) {
        if (index == SYSTEM_GYRO) {
            return Mech.getGyroDisplayString(gyroType);
        }
        if (index == SYSTEM_COCKPIT) {
            return Mech.getCockpitDisplayString(cockpitType);
        }
        return systemNames[index];
    }

    @Override
    public String getRawSystemName(int index) {
        return systemNames[index];
    }

    public int getAirMechWalkMP(boolean gravity, boolean ignoremodulararmor) {
        return getJumpMP(gravity, ignoremodulararmor) * 3;
    }

    public int getAirMechRunMP(boolean gravity, boolean ignoremodulararmor) {
        return (int)Math.ceil(getAirMechWalkMP(gravity, ignoremodulararmor) * 1.5);
    }

    public int getFighterModeWalkMP(boolean gravity, boolean ignoremodulararmor) {
        return getJumpMP(gravity, ignoremodulararmor);
    }

    public int getFighterModeRunMP(boolean gravity, boolean ignoremodulararmor) {
        return (int)Math.ceil(getFighterModeWalkMP(gravity, ignoremodulararmor) * 1.5);
    }

    public int getAirMechWalkMP() {
        return getAirMechWalkMP(true,false);
    }

    public int getAirMechRunMP() {
        return getAirMechRunMP(true, false);
    }

    public int getFighterModeWalkMP() {
        return getFighterModeWalkMP(true, false);
    }

    public int getFighterModeRunMP() {
        return getFighterModeRunMP(true, false);
    }
}
