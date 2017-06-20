/**
 * 
 */
package megamek.common;

/**
 * Quad Mek that can convert into either tracked or wheeled vehicle mode.
 * 
 * @author Neoancient
 *
 */
public class QuadVee extends QuadMech {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1283551018632228647L;

    public static final int SYSTEM_CONVERSION_GEAR = 15;
    
    public static final int SYSTEM_TRACK = 16;

    public static final String systemNames[] = { "Life Support", "Sensors",
            "Cockpit", "Engine", "Gyro", null, null, "Shoulder", "Upper Arm",
            "Lower Arm", "Hand", "Hip", "Upper Leg", "Lower Leg", "Foot",
            "Conversion Gear", "Track"};
    
    public static final int MOTIVE_UNKNOWN = -1;
    public static final int MOTIVE_TRACK = 0;
    public static final int MOTIVE_WHEEL = 1;
    
    public static final String[] MOTIVE_STRING = { "Track", "Wheel" };
    
    protected int motiveType;

    public QuadVee() {
        this(GYRO_STANDARD, MOTIVE_TRACK);
    }
    
    public QuadVee(int inGyroType, int inMotiveType) {
        super(inGyroType, COCKPIT_QUADVEE);
        
        motiveType = inMotiveType;

        setCritical(LOC_RARM, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_RARM, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TRACK));

        setCritical(LOC_LARM, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_LARM, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TRACK));

        setCritical(LOC_RLEG, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_RLEG, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TRACK));

        setCritical(LOC_LLEG, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_CONVERSION_GEAR));
        setCritical(LOC_LLEG, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TRACK));
    }

    @Override
    public String getSystemName(int index) {
        if (index == SYSTEM_GYRO) {
            return Mech.getGyroDisplayString(gyroType);
        }
        if (index == SYSTEM_COCKPIT) {
            return Mech.getCockpitDisplayString(cockpitType);
        }
        if (index == SYSTEM_TRACK) {
            return MOTIVE_STRING[motiveType];
        }
        return systemNames[index];
    }

    @Override
    public String getRawSystemName(int index) {
        return systemNames[index];
    }
    
    /**
     * @return MOTIVE_TRACK or MOTIVE_WHEEL
     */
    public int getMotiveType() {
        return motiveType;
    }

    public String getMotiveTypeString(int motiveType) {
        if (motiveType < 0 || motiveType >= MOTIVE_STRING.length) {
            return MOTIVE_STRING[MOTIVE_UNKNOWN];
        }
        return MOTIVE_STRING[motiveType];
    }
    
    public String getMotiveTypeString() {
        return getMotiveTypeString(getMotiveType());
    }

    public static int getMotiveTypeForString(String inType) {
        if ((inType == null) || (inType.length() < 1)) {
            return MOTIVE_UNKNOWN;
        }
        for (int x = 0; x < MOTIVE_STRING.length; x++) {
            if (inType.equals(MOTIVE_STRING[x])) {
                return x;
            }
        }
        return MOTIVE_UNKNOWN;
    }
    
    @Override
    public EntityMovementMode nextConversionMode() {
        if (movementMode == EntityMovementMode.TRACKED
                || movementMode == EntityMovementMode.WHEELED) {
            return originalMovementMode;
        } else if (motiveType == MOTIVE_WHEEL) {
            return EntityMovementMode.WHEELED;
        } else {
            return EntityMovementMode.TRACKED;
        }
    }
    
    @Override
    public boolean isEligibleForPavementBonus() {
        return isInVehicleMode() && !convertingNow;
    }
    
    @Override
    public boolean canFall(boolean gyroLegDamage) {
        //QuadVees cannot fall due to failed PSR in vehicle mode.
        return !isInVehicleMode();
    }
    
    /**
     * The cost to convert between quad and vehicle modes.
     * @return
     */
    public int conversionCost() {
        int cost = 2;
        //Base cost 2, +1 for each damaged leg actuator, conversion equipment, or track slot
        for (int loc = LOC_RARM; loc <= LOC_LLEG; loc++) {
            for (int slot = 0; slot < 6; slot++) {
                if (getCritical(loc, slot).isHit()) {
                    cost++;
                }
            }
        }
        return cost;
    }

    public boolean isInVehicleMode() {
        return movementMode == EntityMovementMode.TRACKED
                || movementMode == EntityMovementMode.WHEELED;
    }
    
    @Override
    public int getMaxElevationChange() {
        if (isInVehicleMode() || convertingNow) {
            return 1;
        }
        return 2;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return true;
    }
    
    /**
     * Can this mech torso twist in the given direction?
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        if (!canChangeSecondaryFacing()) {
            return dir == 0;
        }
        //Turret rotation always works in vehicle mode.
        if (isInVehicleMode()) {
            return true;
        }
        
        //In 'Mech mode the torso rotation can be limited by gyro damage.
        int gyroHits = getGyroHits();
        if (getGyroType() == GYRO_HEAVY_DUTY) {
            gyroHits--;
        }
        //No damage gives full rotation
        if (gyroHits <= 0) {
            return true;
        }
        int rotate = Math.abs(dir - getFacing());
        //The first hit prevents rotating directly to the rear
        if (gyroHits == 1) {
            return rotate != 3;
        }
        //Destroyed gyro limits to normal biped torso rotation
        return rotate <= 1 || rotate == 5;
    }

    /**
     * Add in any piloting skill mods
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        if (getCrew().hasDedicatedPilot()) {
            roll.addModifier(-1, "dedicated pilot");
        } else {
            roll.addModifier(2, "pilot incapacitated");
        }
        
        return super.addEntityBonuses(roll);
    }
    
    @Override
    public String getTilesetModeString() {
        if (isInVehicleMode()) {
            return "_VEHICLE";
        } else {
            return "";
        }
    }
    
    @Override
    public long getEntityType() {
        return Entity.ETYPE_MECH | Entity.ETYPE_QUAD_MECH | Entity.ETYPE_QUADVEE;
    }
}