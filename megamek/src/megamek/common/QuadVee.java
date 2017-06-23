/**
 * 
 */
package megamek.common;

import java.text.NumberFormat;

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
    
    private int motiveType;
    
    //Certain events, such as damage from charges or failed advance maneuvers, can cause motive
    //system damage apart from 
    private boolean minorMotiveDamage;
    private boolean moderateMotiveDamage;
    private boolean heavyMotiveDamage;
    private int motivePenalty = 0;
    private int motiveDamage = 0;
    private boolean movementHitPending = false;
    private boolean movementHit = false;

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
    
    /**
     * Current MP is calculated differently depending on whether the QuadVee is in Mech
     * or vehicle mode. During conversion we use the mode we started in:
     * bg.battletech.com/forums/index.php?topic=55261.msg1271935#msg1271935
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (startedInVehicleMode()) {
            return getCruiseMP(gravity, ignoreheat, ignoremodulararmor);
        } else {
            return super.getWalkMP(gravity, ignoreheat, ignoremodulararmor);
        }
    }
    
    /**
     * In vehicle mode the QuadVee ignores actuator and hip criticals, but is subject to track/wheel
     * damage and various effects of vehicle motive damage.
     */
    public int getCruiseMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int wmp = getOriginalWalkMP() - motiveDamage;
        //Bonus for wheeled movement
        if (getMotiveType() == MOTIVE_WHEEL) {
            wmp++;
        }
        
        //If a leg or its track/wheel is destroyed, it is treated as major motive system damage,
        //in addition to what has been assigned on the motive system damage table.
        //bg.battletech.com/forums/index.php?topic=55261.msg1271935#msg1271935

        for (int loc = 0; loc < locations(); loc++) {
            if (locationIsLeg(loc)) {
                if (isLocationBad(loc) && getCritical(loc, 5).isHit()) {
                    wmp = (int)Math.ceil(wmp / 2.0);
                }
            }
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
     * UMUs do not function in vehicle mode
     */
    public int getActiveUMUCount() {
        if (isInVehicleMode() || convertingNow) {
            return 0;
        }
        return super.getActiveUMUCount();
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
        //Since pavement bonus only applies if driving on pavement the entire turn,
        //there is no pavement bonus unless it spends the entire turn in vehicle mode.
        return isInVehicleMode() && !convertingNow;
    }
    
    @Override
    public boolean canFall(boolean gyroLegDamage) {
        //QuadVees cannot fall due to failed PSR in vehicle mode.
        //When converting, the conversion completes regardless of anything else that happens
        //in the turn; since vehicles cannot be prone, the unit cannot fall while converting
        //to vehicle mode.
        //http://bg.battletech.com/forums/index.php?topic=55261.msg1274233#msg1274233
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

    /**
     * @return Whether the QuadVee is currently in vehicle mode. During this movement phase
     *         this is based on the final mode if converting.
     */
    public boolean isInVehicleMode() {
        return movementMode == EntityMovementMode.TRACKED
                || movementMode == EntityMovementMode.WHEELED;
    }
    
    /**
     * @return Whether the QuadVee started the turn in vehicle mode.
     */
    public boolean startedInVehicleMode() {
        return (movementMode == EntityMovementMode.TRACKED
                || movementMode == EntityMovementMode.WHEELED) != convertingNow;
    }
    
    public boolean isMovementHit() {
        return movementHit;
    }

    public boolean isMovementHitPending() {
        return movementHitPending;
    }
    
    @Override
    public boolean isImmobile() {
        if (!isInVehicleMode() || (game != null
                && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_NO_IMMOBILE_VEHICLES))) {
            return super.isImmobile();
        }
        return super.isImmobile() || movementHit;
    }
    
    @Override
    public void applyDamage() {
        movementHit |= movementHitPending;
        super.applyDamage();
    }

    /**
     * In vehicle mode the QuadVee is at the same level as the terrain.
     */
    public int height() {
        if (isInVehicleMode()) {
            return 0;
        }
        return super.height();
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
        
        if (startedInVehicleMode()) {
            if (motivePenalty > 0) {
                roll.addModifier(motivePenalty, "motive system damage");                
            }
            // are we wheeled and in light snow?
            IHex hex = game.getBoard().getHex(getPosition());
            if ((null != hex) && (getMovementMode() == EntityMovementMode.WHEELED)
                    && (hex.terrainLevel(Terrains.SNOW) == 1)) {
                roll.addModifier(1, "thin snow");
            }
            // VDNI bonus?
            if (getCrew().getOptions().booleanOption(OptionsConstants.MD_VDNI)
                    && !getCrew().getOptions().booleanOption(OptionsConstants.MD_BVDNI)) {
                roll.addModifier(-1, "VDNI");
            }
            if (hasQuirk(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT)) {
                roll.addModifier(1, "cramped cockpit");
            }

            if (hasHardenedArmor()) {
                roll.addModifier(1, "Hardened Armor");
            }

            if (hasModularArmor()) {
                roll.addModifier(1, "Modular Armor");
            }
            return roll;
        } else {
            return super.addEntityBonuses(roll);
        }
    }
    
    /**
     * adds minor, moderate or heavy movement system damage
     *
     * @param level
     *            a <code>int</code> representing minor damage (1), moderate
     *            damage (2), heavy damage (3), or immobilized (4)
     */
    @Override
    public void addMovementDamage(int level) {
        switch (level) {
            case 1:
                if (!minorMotiveDamage) {
                    minorMotiveDamage = true;
                    motivePenalty += level;
                }
                break;
            case 2:
                if (!moderateMotiveDamage) {
                    moderateMotiveDamage = true;
                    motivePenalty += level;
                }
                motiveDamage++;
                break;
            case 3:
                if (!heavyMotiveDamage) {
                    heavyMotiveDamage = true;
                    motivePenalty += level;
                }
                int nMP = getOriginalWalkMP() - motiveDamage;
                if (nMP > 0) {
                    motiveDamage = getOriginalWalkMP()
                            - (int) Math.ceil(nMP / 2.0);
                }
                break;
            case 4:
                motiveDamage = getOriginalWalkMP();
                movementHitPending = true;
        }
    }

    public boolean hasMinorMovementDamage() {
        return minorMotiveDamage;
    }

    public boolean hasModerateMovementDamage() {
        return moderateMotiveDamage;
    }

    public boolean hasHeavyMovementDamage() {
        return heavyMotiveDamage;
    }

    /**
     * If the QuadVee is in vehicle mode (or converting to it) then it follows
     * the rules for tanks going hull-down, which requires a fortified hex.
     *
     *  @return True if hull-down is enabled and the QuadVee is in a fortified hex.
     */
    @Override
    public boolean canGoHullDown() {
        if (isInVehicleMode()) {
            IHex occupiedHex = game.getBoard().getHex(getPosition());
            return occupiedHex.containsTerrain(Terrains.FORTIFIED)
                    && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_HULL_DOWN);
        }
        return super.canGoHullDown();
    }

    /**
     * Cannot make any physical attacks in vehicle mode except charging, which is
     * handled in the movement phase.
     */
    @Override
    public boolean isEligibleForPhysical() {
        return !isInVehicleMode() && super.isEligibleForPhysical();
    }
    
    /**
     * Adds the C-bill cost of tracks or wheels to the weapons and equipment cost.
     */
    @Override
    public long getWeaponsAndEquipmentCost(boolean ignoreAmmo) {
        long cost = super.getWeaponsAndEquipmentCost(ignoreAmmo);
        
        int baseCost = getMotiveType() == QuadVee.MOTIVE_WHEEL?
                750 : 500;
        long itemCost = (long) Math.ceil(baseCost * getEngine().getRating() * getWeight() / 75.0);
        cost += itemCost;
        
        if ((bvText != null) && (itemCost > 0)) {
            NumberFormat commafy = NumberFormat.getInstance();
            bvText.append(startRow);
            bvText.append(startColumn);
            bvText.append(getMotiveTypeString());
            bvText.append(endColumn);

            bvText.append(startColumn);
            bvText.append(commafy.format(itemCost));
            bvText.append(endColumn);
            bvText.append(endRow);
        }
        return cost;
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