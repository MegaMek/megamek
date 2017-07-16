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

    /**
     * Current MP is calculated differently depending on the LAM's mode. AirMech mode returns
     * cruise/flank; walk/run is treated as a special case of WiGE ground movement.
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp;
        if (movementMode == EntityMovementMode.AERODYNE) {
            mp = getFighterModeWalkMP(gravity, ignoremodulararmor);
        } else if (movementMode == EntityMovementMode.WIGE) {
            mp = getAirMechCruiseMP(gravity, ignoremodulararmor);
        } else {
            mp = super.getWalkMP(gravity, ignoreheat, ignoremodulararmor);
        }
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }
    
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp;
        if (movementMode == EntityMovementMode.AERODYNE) {
            mp = getFighterModeRunMP(gravity, ignoremodulararmor);
        } else if (movementMode == EntityMovementMode.WIGE) {
            mp = getAirMechFlankMP(gravity, ignoremodulararmor);
        } else {
            mp = super.getRunMP(gravity, ignoreheat, ignoremodulararmor);
        }
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }
    
    /**
     * This value should only be used for biped and airmech ground movement.
     */
    @Override
    public int getSprintMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (movementMode == EntityMovementMode.WIGE) {
            if (hasHipCrit()) {
                return getAirMechRunMP(gravity, ignoreheat, ignoremodulararmor);
            }
            return (int)Math.ceil(getAirMechWalkMP(gravity, ignoreheat, ignoremodulararmor)
                    * (hasArmedMASC()? 2.5 : 2.0));
        }
        return super.getSprintMP(gravity, ignoreheat, ignoremodulararmor);
    }

    /**
     * This value should only be used for biped and airmech ground movement.
     */
    @Override
    public int getSprintMPwithoutMASC(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        if (movementMode == EntityMovementMode.WIGE) {
            if (hasHipCrit()) {
                return getAirMechRunMP(gravity, ignoreheat, ignoremodulararmor);
            }
            return getAirMechWalkMP(gravity, ignoreheat, ignoremodulararmor)  * 2;
        }
        return super.getSprintMPwithoutMASC(gravity, ignoreheat, ignoremodulararmor);
    }

    public int getOriginalSprintMPwithoutMASC() {
        if (movementMode == EntityMovementMode.AERODYNE
                || movementMode == EntityMovementMode.WIGE) {
            return getOriginalRunMP();
        } else {
            return (int) Math.ceil(getOriginalWalkMP() * 2.0);
        }
    }

    public int getAirMechCruiseMP(boolean gravity, boolean ignoremodulararmor) {
        return getJumpMP(gravity, ignoremodulararmor) * 3;
    }

    public int getAirMechFlankMP(boolean gravity, boolean ignoremodulararmor) {
        return (int)Math.ceil(getAirMechCruiseMP(gravity, ignoremodulararmor) * 1.5);
    }
    
    public int getAirMechWalkMP() {
        return getAirMechWalkMP(true, false, false);
    }

    public int getAirMechWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp = (int)Math.ceil(super.getWalkMP(gravity, ignoreheat, ignoremodulararmor) * 0.33);
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }
    
    public int getAirMechRunMP() {
        return getAirMechRunMP(true, false, false);
    }

    public int getAirMechRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp = (int)Math.ceil(getAirMechWalkMP(gravity, ignoreheat, ignoremodulararmor) * 1.5);
        if (convertingNow) {
            mp /= 2;
        }
        return mp;
    }
    
    public int getFighterModeWalkMP(boolean gravity, boolean ignoremodulararmor) {
        return getJumpMP(gravity, ignoremodulararmor);
    }

    public int getFighterModeRunMP(boolean gravity, boolean ignoremodulararmor) {
        return (int)Math.ceil(getFighterModeWalkMP(gravity, ignoremodulararmor) * 1.5);
    }

    public int getAirMechCruiseMP() {
        return getAirMechCruiseMP(true, false);
    }

    public int getAirMechFlankMP() {
        return getAirMechFlankMP(true, false);
    }

    public int getFighterModeWalkMP() {
        return getFighterModeWalkMP(true, false);
    }

    public int getFighterModeRunMP() {
        return getFighterModeRunMP(true, false);
    }
    
    /**
     * LAMs cannot benefit from MASC in AirMech or fighter mode and cannot mount a supercharger.
     */
    @Override
    public boolean hasArmedMASC() {
        if (movementMode == EntityMovementMode.AERODYNE
                || movementMode == EntityMovementMode.WIGE) {
            return false;
        }
        return super.hasArmedMASC();
    }
    
    @Override
    public int getWalkHeat() {
        if (moved == EntityMovementType.MOVE_VTOL_WALK) {
            return getAirMechHeat();
        }
        return super.getWalkHeat();
    }
    
    @Override
    public int getRunHeat() {
        if (moved == EntityMovementType.MOVE_VTOL_RUN) {
            return getAirMechHeat();
        }
        return super.getRunHeat();
    }
    
    public int getAirMechHeat() {
        int mod = bDamagedCoolantSystem?1:0;
        return mod + (int)Math.round(getJumpHeat(mpUsed) / 3.0);
    }

    @Override
    public int getEngineCritHeat() {
        // Engine crit heat follows ASF rules in fighter mode. 
        if (movementMode == EntityMovementMode.AERODYNE) {
            return 2 * getEngineHits();
        } else {
            return super.getEngineCritHeat();
        }
    }

    @Override
    public boolean usesTurnMode() {
        // Turn mode rule is not optional for LAMs in AirMech mode.
        return movementMode == EntityMovementMode.WIGE;
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
    
    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        // Fighter mode has the same terrain restrictions as ASFs.
        if (movementMode == EntityMovementMode.AERODYNE) {
            IHex hex = game.getBoard().getHex(c);
            if (isAirborne()) {
                if (hex.containsTerrain(Terrains.IMPASSABLE)) {
                    return true;
                }
                return false;
            }

            // Additional restrictions for hidden units
            if (isHidden()) {
                // Can't deploy in paved hexes
                if (hex.containsTerrain(Terrains.PAVEMENT)
                        || hex.containsTerrain(Terrains.ROAD)) {
                    return true;
                }
                // Can't deploy on a bridge
                if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation)
                        && hex.containsTerrain(Terrains.BRIDGE)) {
                    return true;
                }
                // Can't deploy on the surface of water
                if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                    return true;
                }
            }

            // grounded aeros have the same prohibitions as wheeled tanks
            return hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.ROUGH)
                    || ((hex.terrainLevel(Terrains.WATER) > 0) 
                            && !hex.containsTerrain(Terrains.ICE))
                    || hex.containsTerrain(Terrains.RUBBLE)
                    || hex.containsTerrain(Terrains.MAGMA)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || (hex.terrainLevel(Terrains.SNOW) > 1)
                    || (hex.terrainLevel(Terrains.GEYSER) == 2);
        } else if (movementMode == EntityMovementMode.WIGE && currElevation > 0) {
            // Cannot enter woods or a building hex in AirMech mode unless using ground movement
            // or flying over the terrain.
            IHex hex = game.getBoard().getHex(c);
            return (hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || hex.containsTerrain(Terrains.BLDG_ELEV))
                    && hex.ceiling() > currElevation;
        } else {
            // Mech mode or AirMech mode using ground MP have the same restrictions as Biped Mech.
            return super.isLocationProhibited(c, currElevation);
        }
    }
    
    @Override
    public int getMaxElevationDown(int currElevation) {
        // Cannot spend AirMech MP above altitude 3 (level 30) so we use that as max descent.
        if ((currElevation > 0)
                && (getMovementMode() == EntityMovementMode.WIGE)) {
            return 30;
        }
        return super.getMaxElevationDown(currElevation);
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
        if (previousMovementMode == EntityMovementMode.WIGE) {
            if (afterMode == EntityMovementMode.WIGE) {
                return EntityMovementMode.AERODYNE;
            } else if (afterMode == EntityMovementMode.AERODYNE) {
                return originalMovementMode;
            } else {
                return EntityMovementMode.WIGE;
            }
        } else if (afterMode == EntityMovementMode.WIGE) {
            return previousMovementMode;
        } else if (afterMode == EntityMovementMode.AERODYNE) {
            return lamType == LAM_BIMODAL? originalMovementMode : EntityMovementMode.WIGE;
        } else {
            return lamType == LAM_BIMODAL? EntityMovementMode.AERODYNE : EntityMovementMode.WIGE;
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
    	movement.put("g", getAirMechCruiseMP(true, false));
    	movement.put("a", getFighterModeWalkMP(true, false));
    }
    
    @Override
    public void setAlphaStrikeMovement(Map<String,Integer> movement) {
    	super.setBattleForceMovement(movement);
    	movement.put("g", getAirMechCruiseMP(true, false) * 2);
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

    @Override
    public String getTilesetModeString() {
        if (movementMode == EntityMovementMode.AERODYNE) {
            return "_FIGHTER";
        } else if (movementMode == EntityMovementMode.WIGE) {
            return "_AIRMECH";
        } else {
            return "";
        }
    }
    
    @Override
    public boolean isAero() {
        return movementMode == EntityMovementMode.AERODYNE;
    }
    
    public long getEntityType(){
        return Entity.ETYPE_MECH | Entity.ETYPE_BIPED_MECH | Entity.ETYPE_LAND_AIR_MECH;
    }
}
