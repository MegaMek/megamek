/**
 * 
 */
package megamek.common;

import megamek.common.options.OptionsConstants;

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
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int wmp = getOriginalWalkMP();
        
        //If converting, we will calculate both modes and take the lower of the two.
        int baseVee = wmp;

        if (isInVehicleMode() || convertingNow) {
            //If a leg or its track/wheel is destroyed, it is treated as major motive system damage,
            //a divides the MP in half cumulatively, rounded up.
            //bg.battletech.com/forums/index.php?topic=55261.msg1271935#msg1271935
            int badLegs = 0;
            for (int loc = 0; loc < locations(); loc++) {
                if (locationIsLeg(loc)) {
                    if (isLocationBad(loc) && getCritical(loc, 5).isHit()) {
                        badLegs++;                            
                    }
                }
            }
            if (motiveType == MOTIVE_WHEEL) {
                baseVee++;
            }
            if (badLegs == 4) {
                return baseVee = 0;
            } else if (badLegs > 1) {
                baseVee = (int)Math.ceil((float)baseVee / (1 << badLegs));
            }
        }
        if (!isInVehicleMode() || convertingNow) {
            int hipHits = 0;
            int actuatorHits = 0;
            int legsDestroyed = 0;
            for (int i = 0; i < locations(); i++) {
                if (locationIsLeg(i)) {
                    if (!isLocationBad(i)) {
                        if (legHasHipCrit(i)) {
                            hipHits++;
                            if ((game == null) || !game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                                continue;
                            }
                        }
                        actuatorHits += countLegActuatorCrits(i);
                    } else {
                        legsDestroyed++;
                    }
                }
            }
            // leg damage effects
            if (legsDestroyed > 0) {
                if (legsDestroyed == 1) {
                    wmp--;
                } else if (legsDestroyed == 2) {
                    wmp = 1;
                } else {
                    wmp = 0;
                }
            }        
            if (wmp > 0) {
                if (hipHits > 0) {
                    if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_LEG_DAMAGE)) {
                        wmp = wmp - (2 * hipHits);
                    } else {
                        for (int i = 0; i < hipHits; i++) {
                            wmp = (int) Math.ceil(wmp / 2.0);
                        }
                    }
                }
                wmp -= actuatorHits;
            }
            //Mixed-tech QuadVees that mount TSM only benefit in Mech mode.
            if (!ignoreheat && (heat >= 9) && hasTSM() && legsDestroyed < 2) {
                wmp += 2;
            }
        }
        if (convertingNow) {
            wmp = Math.min(wmp, baseVee);
        } else if (isInVehicleMode()) {
            wmp = baseVee;
        }

        //Now apply modifiers
        if (!ignoremodulararmor && hasModularArmor() ) {
            wmp--;
        }

        if (!ignoreheat) {
            // factor in heat
            if ((game != null) && game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT)) {
                if (heat < 30) {
                    wmp -= (heat / 5);
                } else if (heat >= 49) {
                    wmp -= 9;
                } else if (heat >= 43) {
                    wmp -= 8;
                } else if (heat >= 37) {
                    wmp -= 7;
                } else if (heat >= 31) {
                    wmp -= 6;
                } else {
                    wmp -= 5;
                }
            } else {
                wmp -= (heat / 5);
            }
        }
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                wmp = Math.max(wmp + weatherMod, 0);
            }
        }
        // gravity
        if (gravity) {
            wmp = applyGravityEffectsOnMP(wmp);
        }
        // For sanity sake...
        wmp = Math.max(0, wmp);
        return wmp;
    }
    
    /*
     * No jumping in vehicle mode.
     */
    public int getJumpMP(boolean gravity, boolean ignoremodulararmor) {
        if (isInVehicleMode() || convertingNow) {
            return 0;
        }
        return super.getJumpMP(gravity, ignoremodulararmor);
    }

    /*
     * In a QuadVee they're all torso jump jets. But they still don't work in vehicle mode.
     */
    public int torsoJumpJets() {
        if (isInVehicleMode() || convertingNow) {
            return 0;
        }
        return super.torsoJumpJets();
    }
    /**
     * QuadVees cannot benefit from MASC in vehicle mode, so in that case we only return true if there
     * is an armed supercharger.
     */
    @Override
    public boolean hasArmedMASC() {
        boolean superchargerOnly = isInVehicleMode() || convertingNow;
        for (Mounted m : getEquipment()) {
            if (!m.isDestroyed() && !m.isBreached()
                    && (m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_MASC)
                    && (!superchargerOnly || m.getType().getSubType() == MiscType.S_SUPERCHARGER)
                    && m.curMode().equals("Armed")) {
                return true;
            }
        }
        return false;        
    }
    
    /**
     * Cannot benefit from MASC in vehicle mode.
     */
    @Override
    public boolean hasArmedMASCAndSuperCharger() {
        if (isInVehicleMode() || convertingNow) {
            return false;
        }
        return super.hasArmedMASCAndSuperCharger();
    }

    /**
     * No movement heat generated in vehicle mode
     */
    @Override
    public int getStandingHeat() {
        if (isInVehicleMode() && !convertingNow) {
            return 0;
        }
        return super.getStandingHeat();
    }

    @Override
    public int getWalkHeat() {
        if (isInVehicleMode() && !convertingNow) {
            return 0;
        }
        return super.getWalkHeat();
    }

    @Override
    public int getRunHeat() {
        if (isInVehicleMode() && !convertingNow) {
            return 0;
        }
        return super.getRunHeat();
    }

    @Override
    public int getSprintHeat() {
        if (isInVehicleMode() && !convertingNow) {
            return 0;
        }
        return super.getSprintHeat();
    }

    /**
     * Overrides to return false in vehicle mode. Technically it still has a hip crit, but it has no
     * effect.
     */
    @Override
    public boolean hasHipCrit() {
        if (isInVehicleMode() && !convertingNow) {
            return false;
        }
        return super.hasHipCrit();
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