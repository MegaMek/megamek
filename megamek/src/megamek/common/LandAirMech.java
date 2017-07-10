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

import java.util.Map;

public class LandAirMech extends BipedMech {

    /**
     *
     */
    private static final long serialVersionUID = -8118673802295814548L;

    public static final int LAM_AVIONICS = 15;

    public static final int LAM_LANDING_GEAR = 16;

    public static final String systemNames[] =
        { "Life Support", "Sensors", "Cockpit", "Engine", "Gyro", null, null, "Shoulder", "Upper Arm", "Lower Arm", "Hand", "Hip", "Upper Leg", "Lower Leg", "Foot", "Avionics", "Landing Gear" };
    
    public static final int LAM_UNKNOWN  = -1;
    public static final int LAM_STANDARD = 0;
    public static final int LAM_BIMODAL  = 1;
    
    public static final String[] LAM_STRING = { "Standard", "Bimodal" };

    private int fuel;
    private int lamType;
    private EntityMovementMode previousMovementMode;

    public LandAirMech(int inGyroType, int inCockpitType, int inLAMType) {
        super(inGyroType, inCockpitType);
        lamType = inLAMType;

        setTechLevel(TechConstants.T_IS_ADVANCED);
        setCritical(Mech.LOC_HEAD, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_LT, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_RT, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_AVIONICS));
        setCritical(Mech.LOC_LT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
        setCritical(Mech.LOC_RT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));
        setCritical(Mech.LOC_CT, 10, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, LAM_LANDING_GEAR));

        previousMovementMode = movementMode;
        setFuel(80);
        
        setCrew(new LAMPilot(this));
    }
    
    public void setCrew(Crew newCrew) {
        if (newCrew instanceof LAMPilot) {
            super.setCrew(newCrew);
        } else {
            super.setCrew(LAMPilot.convertToLAMPilot(this, newCrew));
        }
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
    
    public int getLAMType() {
        return lamType;
    }
    
    public void setLAMType(int lamType) {
        this.lamType = lamType;
    }
    
    public String getLAMTypeString(int lamType) {
        if (lamType < 0 || lamType >= LAM_STRING.length) {
            return LAM_STRING[LAM_UNKNOWN];
        }
        return LAM_STRING[lamType];
    }
    
    public String getLAMTypeString() {
        return getLAMTypeString(getLAMType());
    }

    public static int getLAMTypeForString(String inType) {
        if ((inType == null) || (inType.length() < 1)) {
            return LAM_UNKNOWN;
        }
        for (int x = 0; x < LAM_STRING.length; x++) {
            if (inType.equals(LAM_STRING[x])) {
                return x;
            }
        }
        return LAM_UNKNOWN;
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
    
    @Override
    public boolean usesTurnMode() {
        // Turn mode rule is not optional for LAMs in AirMech mode.
        return movementMode == EntityMovementMode.AIRMECH;
    }

    /**
     * When cycling through possible movement modes, we need to know if we've returned to the previous
     * mode, which means that no conversion is actually going to take place.
     * 
     * @return The movement mode on the previous turn.
     */
    public EntityMovementMode getPreviousMovementMode() {
        return previousMovementMode;
    }
    
    /**
     * Start a new round
     *
     * @param roundNumber the <code>int</code> number of the new round
     */
    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);
        previousMovementMode = movementMode;
    }
    
    /*
     * Cycling through conversion modes for LAMs in 'Mech or fighter mode is simple toggling between
     * two states. LAMs in AirMech mode have three possible states.
     */
    @Override
    public EntityMovementMode nextConversionMode(EntityMovementMode afterMode) {
        if (previousMovementMode == EntityMovementMode.AIRMECH) {
            if (afterMode == EntityMovementMode.AIRMECH) {
                return EntityMovementMode.AERODYNE;
            } else if (afterMode == EntityMovementMode.AERODYNE) {
                return originalMovementMode;
            } else {
                return EntityMovementMode.AIRMECH;
            }
        } else if (afterMode == EntityMovementMode.AIRMECH) {
            return previousMovementMode;
        } else if (afterMode == EntityMovementMode.AERODYNE) {
            return lamType == LAM_BIMODAL? originalMovementMode : EntityMovementMode.AIRMECH;
        } else {
            return lamType == LAM_BIMODAL? EntityMovementMode.AERODYNE : EntityMovementMode.AIRMECH;
        }
    }
    
    @Override
    public boolean canFall(boolean gyroLegDamage) {
        //TODO: in AirMech mode it is possible to fall if using walk/run (or standing) but not cruise/flank
        return movementMode != EntityMovementMode.AERODYNE;
    }
    
    @Override
    public void setBattleForceMovement(Map<String,Integer> movement) {
    	super.setBattleForceMovement(movement);
    	movement.put("g", getAirMechWalkMP(true, false));
    	movement.put("a", getFighterModeWalkMP(true, false));
    }
    
    @Override
    public void setAlphaStrikeMovement(Map<String,Integer> movement) {
    	super.setBattleForceMovement(movement);
    	movement.put("g", getAirMechWalkMP(true, false) * 2);
    	movement.put("a", getFighterModeWalkMP(true, false));
    }
    
    @Override
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        int bombs = (int)getEquipment().stream().filter(m -> m.getType().hasFlag(MiscType.F_BOMB_BAY))
                .count();
        if (bombs > 0) {
            specialAbilities.put(BattleForceSPA.BOMB, bombs / 5);
        }
        specialAbilities.put(BattleForceSPA.LAM, null);
    }

    public long getEntityType(){
        return Entity.ETYPE_MECH | Entity.ETYPE_BIPED_MECH | Entity.ETYPE_LAND_AIR_MECH;
    }
}
