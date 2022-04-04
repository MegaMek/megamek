/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.battlevalue.ProtoMekBVCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.preference.PreferenceManager;
import org.apache.logging.log4j.LogManager;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Protomechs. Level 2 Clan equipment.
 */
public class Protomech extends Entity {
    private static final long serialVersionUID = -1376410042751538158L;

    public static final int NUM_PMECH_LOCATIONS = 7;

    private static final String[] LOCATION_NAMES = { "Body", "Head", "Torso",
            "Right Arm", "Left Arm", "Legs", "Main Gun" };

    private static final String[] LOCATION_ABBRS = { "BD", "HD", "T", "RA", "LA", "L", "MG" };

    // Crew damage caused so far by crits to this location.
    // Needed for location destruction pilot damage.
    private int[] pilotDamageTaken = { 0, 0, 0, 0, 0, 0, 0 };

    /**
     * Not every Protomech has a main gun. N.B. Regardless of the value set
     * here, the variable is initialized to <code>false</code> until after the
     * <code>Entity</code> is initialized, which is too late to allow main gun
     * armor, hence the convoluted reverse logic.
     */
    private boolean m_bHasNoMainGun = false;

    public static final int LOC_BODY = 0;

    public static final int LOC_HEAD = 1;
    public static final int LOC_TORSO = 2;

    public static final int LOC_RARM = 3;
    public static final int LOC_LARM = 4;

    public static final int LOC_LEG = 5;
    public static final int LOC_MAINGUN = 6;

    // Near miss reprs.
    public static final int LOC_NMISS = 7;

    // "Systems". These represent protomech critical hits; which remain constant
    // regardless of proto.
    // doesn't matter what gets hit in a proto section, just the number of times
    // it's been critted
    // so just have the right number of these systems and it works.
    public static final int SYSTEM_ARMCRIT = 0;
    public static final int SYSTEM_LEGCRIT = 1;
    public static final int SYSTEM_HEADCRIT = 2;
    public static final int SYSTEM_TORSOCRIT = 3;
    public static final int SYSTEM_TORSO_WEAPON_A = 4;
    public static final int SYSTEM_TORSO_WEAPON_B = 5;
    public static final int SYSTEM_TORSO_WEAPON_C = 6;
    public static final int SYSTEM_TORSO_WEAPON_D = 7;
    public static final int SYSTEM_TORSO_WEAPON_E = 8;
    public static final int SYSTEM_TORSO_WEAPON_F = 9;

    private static final int[] NUM_OF_SLOTS = { 0, 2, 3, 2, 2, 3, 0 };

    public static final int[] POSSIBLE_PILOT_DAMAGE = { 0, 1, 3, 1, 1, 1, 0 };

    public static final String[] systemNames = { "Arm", "Leg", "Head", "Torso" };

    // For grapple attacks
    private int grappled_id = Entity.NONE;

    private boolean isGrappleAttacker = false;
    
    private boolean grappledThisRound = false;

    private boolean edpCharged = true;

    private int edpChargeTurns = 0;

    private boolean isQuad = false;
    private boolean isGlider = false;
    private boolean interfaceCockpit = false;
    private int wingHits = 0;

    // for MHQ
    private boolean engineHit = false;

    /**
     * Construct a new, blank, pmech.
     */
    public Protomech() {
        super();
        setCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_HEADCRIT));
        setCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_HEADCRIT));
        setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_ARMCRIT));
        setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_ARMCRIT));
        setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_ARMCRIT));
        setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_ARMCRIT));
        setCritical(LOC_TORSO, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_TORSOCRIT));
        setCritical(LOC_TORSO, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_TORSOCRIT));
        setCritical(LOC_TORSO, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_TORSOCRIT));
        setCritical(LOC_LEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LEGCRIT));
        setCritical(LOC_LEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LEGCRIT));
        setCritical(LOC_LEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LEGCRIT));
        m_bHasNoMainGun = true;
    }

    @Override
    public int getUnitType() {
        return UnitType.PROTOMEK;
    }

    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Returns # of pilot damage points taken due to crits to the location so
     * far.
     */
    public int getPilotDamageTaken(int loc) {
        return pilotDamageTaken[loc];
    }

    /**
     * Get the weapon in the given torso location (if any).
     *
     * @param torsoNum
     *            - a <code>int</code> that corresponds to SYSTEM_TORSO_WEAPON_A
     *            through SYSTEM_TORSO_WEAPON_F
     * @return the <code>Mounted</code> weapon at the needed location. This
     *         value will be <code>null</code> if no weapon is in the indicated
     *         location.
     */
    public Mounted getTorsoWeapon(int torsoNum) {
        int index = torsoNum - SYSTEM_TORSO_WEAPON_A;
        // There are some non-weapons that take up weapon critical slots
        List<Mounted> torsoEquipment = getEquipment().stream().filter(m -> (m.getLocation() == LOC_TORSO)
                && m.getType().isHittable()).collect(Collectors.toList());
        if (index < torsoEquipment.size()) {
            return torsoEquipment.get(index);
        } else {
            return null;
        }
    }

    /**
     * Tells the Protomech to note pilot damage taken from crit damage to the
     * location
     */
    public void setPilotDamageTaken(int loc, int damage) {
        pilotDamageTaken[loc] = damage;
    }

    /**
     * Protos don't take piloting skill rolls.
     */
    // TODO: this is no longer true in TacOps. Protos sometimes make PSRs using
    // their gunnery skill
    @Override
    public PilotingRollData getBasePilotingRoll() {
        return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE,
                "Protomeks never take PSRs.");
    }

    /**
     * A "shaded" critical is a box shaded on the record sheet, implies pilot
     * damage when hit. Returns whether shaded.
     */
    public boolean shaded(int loc, int numHit) {
        switch (loc) {
            case LOC_HEAD:
            case LOC_LARM:
            case LOC_RARM:
                return (2 == numHit);
            case LOC_TORSO:
                return (0 < numHit);
            case LOC_MAINGUN:
            case LOC_NMISS:
            case LOC_LEG:
                return (3 == numHit);
        }
        return false;
    }

    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (isEngineHit()) {
            return 0;
        }
        int wmp = getOriginalWalkMP();
        int j;
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions()
                    .getMovementMods(this);
            if (weatherMod != 0) {
                wmp = Math.max(wmp + weatherMod, 0);
            }
        }
        // Gravity, Protos can't get faster
        if (gravity) {
            j = applyGravityEffectsOnMP(wmp);
        } else {
            j = wmp;
        }
        if (j < wmp) {
            wmp = j;
        }
        if (isGlider()) {
            // Torso crits reduce glider mp as jump
            int torsoCrits = getCritsHit(LOC_TORSO);
            switch (torsoCrits) {
                case 0:
                    break;
                case 1:
                    if (wmp > 0) {
                        wmp--;
                    }
                    break;
                case 2:
                    wmp /= 2;
                    break;
            }
            // Near misses damage the wings/flight systems, which reduce MP by one per hit.
            wmp = Math.max(0, wmp - wingHits);
        } else {
            int legCrits = getCritsHit(LOC_LEG);
            switch (legCrits) {
                case 0:
                    break;
                case 1:
                    wmp--;
                    break;
                case 2:
                    wmp = wmp / 2;
                    break;
                case 3:
                    wmp = 0;
                    break;
            }
        }
        return wmp;
    }

    @Override
    public String getRunMPasString() {
        if (hasMyomerBooster()) {
            return getRunMPwithoutMyomerBooster(true, false, false) + "(" + getRunMP() + ")";
        }
        return Integer.toString(getRunMP());
    }

    /**
     * Counts the # of crits taken by proto in the location. Needed in several
     * places, due to proto set criticals.
     */
    public int getCritsHit(int loc) {
        int count = 0;
        int numberOfCriticals = this.getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if (ccs.isDamaged() || ccs.isBreached()) {
                count++;
            }
        }
        return count;
    }

    public static int getInnerLocation(int location) {
        return LOC_TORSO;
    }

    /**
     * Add in any piloting skill mods
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        return roll;
    }
    
    /**
     * Returns the number of total critical slots in a location
     */
    @Override
    public int getNumberOfCriticals(int loc) {
        switch (loc) {
            case LOC_MAINGUN:
                return 0;
            case LOC_HEAD:
            case LOC_LARM:
            case LOC_RARM:
                return 2;
            case LOC_LEG:
            case LOC_TORSO:
                return 3;
            case LOC_BODY:
                // This is needed to keep everything ordered in the unit display system tab
                return 1;
        }
        return 0;
    }
    
    public static final TechAdvancement TA_STANDARD_PROTOMECH = new TechAdvancement(TECH_BASE_CLAN)
            .setClanAdvancement(3055, 3059, 3060).setClanApproximate(true, false, false)
            .setPrototypeFactions(F_CSJ).setProductionFactions(F_CSJ)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    public static final TechAdvancement TA_QUAD = new TechAdvancement(TECH_BASE_CLAN)
            .setClanAdvancement(3075, 3083, 3100).setClanApproximate(false, true, false)
            .setPrototypeFactions(F_CLAN).setProductionFactions(F_CCC)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    public static final TechAdvancement TA_ULTRA = new TechAdvancement(TECH_BASE_CLAN)
            .setClanAdvancement(3075, 3083, 3100).setClanApproximate(false, true, false)
            .setPrototypeFactions(F_CLAN).setProductionFactions(F_CCY)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    public static final TechAdvancement TA_GLIDER = new TechAdvancement(TECH_BASE_CLAN)
            .setClanAdvancement(3075, 3084, 3100).setClanApproximate(false, true, false)
            .setPrototypeFactions(F_CLAN).setProductionFactions(F_CSR)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_E, RATING_E)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    public static final TechAdvancement TA_INTERFACE_COCKPIT = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(3071, DATE_NONE, DATE_NONE, 3085).setISApproximate(true).setPrototypeFactions(F_WB)
            .setTechRating(RATING_E)
            .setAvailability(RATING_X, RATING_X, RATING_F, RATING_X)
            .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        if (isQuad) {
            return TA_QUAD;
        } else if (isGlider) {
            return TA_GLIDER;
        } else if (getWeightClass() == EntityWeightClass.WEIGHT_SUPER_HEAVY) {
            return TA_ULTRA;
        } else {
            return TA_STANDARD_PROTOMECH;
        }
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        if (interfaceCockpit) {
            ctl.addComponent(TA_INTERFACE_COCKPIT);
        }
    }

    /**
     * Override Entity#newRound() method.
     */
    @Override
    public void newRound(int roundNumber) {
        if (hasWorkingMisc(MiscType.F_ELECTRIC_DISCHARGE_ARMOR) && !edpCharged) {
            for (Mounted misc : getMisc()) {
                if (misc.getType().hasFlag(MiscType.F_ELECTRIC_DISCHARGE_ARMOR)
                        && misc.curMode().equals("charging")) {
                    if (edpChargeTurns == 6) {
                        setEDPCharged(true);
                        misc.setMode("not charging");
                        edpChargeTurns = 0;
                    } else {
                        edpChargeTurns++;
                    }
                }
            }
        }
        setSecondaryFacing(getFacing());
        
        grappledThisRound = false;
        
        super.newRound(roundNumber);

    } // End public void newRound()

    /**
     * This pmech's jumping MP modified for missing jump jets and gravity.
     */
    @Override
    public int getJumpMP(boolean gravity) {
        int jump = jumpMP;
        int torsoCrits = getCritsHit(LOC_TORSO);
        switch (torsoCrits) {
            case 0:
                break;
            case 1:
                if (jump > 0) {
                    jump--;
                }
                break;
            case 2:
                jump = jump / 2;
                break;
        }
        if (hasWorkingMisc(MiscType.F_PARTIAL_WING)) {
            int atmo = PlanetaryConditions.ATMO_STANDARD;
            if (game != null) {
                atmo = game.getPlanetaryConditions().getAtmosphere();
            }
            switch (atmo) {
                case PlanetaryConditions.ATMO_VHIGH:
                case PlanetaryConditions.ATMO_HIGH:
                    jump += 3;
                    break;
                case PlanetaryConditions.ATMO_STANDARD:
                case PlanetaryConditions.ATMO_THIN:
                    jump += 2;
                    break;
                case PlanetaryConditions.ATMO_TRACE:
                    jump += 1;
                    break;
            }
        }
        if (!gravity) {
            return jump;
        } else {
            if (applyGravityEffectsOnMP(jump) > jump) {
                return jump;
            }
            return applyGravityEffectsOnMP(jump);
        }
    }

    public int getJumpJets() {
        return jumpMP;
    }

    /**
     * Returns this mech's jumping MP, modified for missing and underwater jets.
     */
    @Override
    public int getJumpMPWithTerrain() {
        if (getPosition() == null) {
            return getJumpMP();
        }

        int waterLevel = game.getBoard().getHex(getPosition())
                .terrainLevel(Terrains.WATER);
        if ((waterLevel <= 0) || (getElevation() >= 0)) {
            return getJumpMP();
        }
        return 0;
    }

    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    /**
     * Returns the amount of heat that the entity can sink each turn. Pmechs
     * have no heat. //FIXME However, the number of heat sinks they have IS
     * important... For cost and validation purposes.
     */
    @Override
    public int getHeatCapacity(boolean radicalHeatSinks) {
        return DOES_NOT_TRACK_HEAT;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns the name of the type of movement used. This is pmech-specific.
     */
    @Override
    public String getMovementString(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_NONE:
                return "None";
            case MOVE_WALK:
            case MOVE_VTOL_WALK:
                return "Walked";
            case MOVE_VTOL_RUN:
            case MOVE_RUN:
                return "Ran";
            case MOVE_JUMP:
                return "Jumped";
            default:
                return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used. This is pmech-specific.
     */
    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_NONE:
                return "N";
            case MOVE_WALK:
            case MOVE_VTOL_WALK:
                return "W";
            case MOVE_RUN:
            case MOVE_VTOL_RUN:
                return "R";
            case MOVE_JUMP:
                return "J";
            default:
                return "?";
        }
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return !(getCritsHit(LOC_LEG) > 2) && !isBracing();
    }

    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    /**
     * Can this pmech torso twist in the given direction?
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            if (isQuad()) {
                return true;
            }
            return (rotate == 0) || (rotate == 1) || (rotate == -1)
                    || (rotate == -5);
        }
        return rotate == 0;
    }

    /**
     * Return the nearest valid direction to torso twist in
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        if (isValidSecondaryFacing(dir)) {
            return dir;
        }
        // otherwise, twist once in the appropriate direction
        final int rotate = (dir + (6 - getFacing())) % 6;
        return rotate >= 3 ? (getFacing() + 5) % 6 : (getFacing() + 1) % 6;
    }

    @Override
    public double getArmorWeight() {
        return RoundWeight.standard(EquipmentType.getProtomechArmorWeightPerPoint(getArmorType(LOC_TORSO))
                * getTotalOArmor(), this);
    }

    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    /**
     * get this ProtoMech's run MP without factoring in a possible myomer
     * booster
     *
     * @param gravity
     * @param ignoreheat
     * @return
     */
    public int getRunMPwithoutMyomerBooster(boolean gravity,
            boolean ignoreheat, boolean ignoremodulararmor) {
        return super.getRunMP(gravity, ignoreheat, ignoremodulararmor);
    }

    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        if (hasMyomerBooster()) {
            return (getWalkMP(gravity, ignoreheat, ignoremodulararmor) * 2);
        }
        return super.getRunMP(gravity, ignoreheat, ignoremodulararmor);
    }

    /**
     * does this protomech mount a myomer booster?
     *
     * @return
     */
    public boolean hasMyomerBooster() {
        for (Mounted mEquip : getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_MASC) && !mEquip.isInoperable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        // rear mounted?
        if (mounted.isRearMounted()) {
            return Compute.ARC_REAR;
        }
        // VGLs base arc on their facing
        if (mounted.getType().hasFlag(WeaponType.F_VGL)) {
            return Compute.firingArcFromVGLFacing(mounted.getFacing());
        }
        // front mounted
        switch (mounted.getLocation()) {
            case LOC_TORSO:
                return Compute.ARC_FORWARD;
            case LOC_RARM:
                return Compute.ARC_RIGHTARM;
            case LOC_LARM:
                return Compute.ARC_LEFTARM;
            case LOC_MAINGUN:
                return Compute.ARC_MAINGUN;
            default:
                return Compute.ARC_360;
        }
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc. If
     * false, assume it fires into the primary.
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        // for quads, only the main gun weapons can rotate
        if (isQuad()) {
            return getEquipment(weaponId).getLocation() == LOC_MAINGUN;
        }
        return true;
    }

    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, AimingMode.NONE, LosEffects.COVER_NONE);
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
                                   int cover) {
        int roll = -1;

        if ((aimedLocation != LOC_NONE) && aimingMode.isImmobile()) {
            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR, true);
            }

        }

        roll = Compute.d6(2);
        try {
            PrintWriter pw = PreferenceManager.getClientPreferences().getMekHitLocLog();

            if (pw != null) {
                pw.print(table);
                pw.print("\t");
                pw.print(side);
                pw.print("\t");
                pw.println(roll);
            }
        } catch (Throwable t) {
            LogManager.getLogger().error("", t);
        }

        switch (roll) {
            case 2:
                return new HitData(Protomech.LOC_MAINGUN);
            case 3:
            case 11:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    return new HitData(Protomech.LOC_LEG);
                }
                return new HitData(Protomech.LOC_NMISS);
            case 4:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    return new HitData(Protomech.LOC_LEG);
                }
                if (!isQuad()) {
                    return new HitData(Protomech.LOC_RARM);
                } else {
                    return new HitData(Protomech.LOC_LEG);
                }
            case 5:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    if (!isQuad()) {
                        return new HitData(Protomech.LOC_RARM);
                    } else {
                        return new HitData(Protomech.LOC_LEG);
                    }
                }
            case 9:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    if (!isQuad()) {
                        return new HitData(Protomech.LOC_LARM);
                    } else {
                        return new HitData(Protomech.LOC_LEG);
                    }
                }
                return new HitData(Protomech.LOC_LEG);
            case 6:
            case 7:
            case 8:
                return new HitData(Protomech.LOC_TORSO);
            case 10:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    return new HitData(Protomech.LOC_LEG);
                }
                if (!isQuad()) {
                    return new HitData(Protomech.LOC_LARM);
                } else {
                    return new HitData(Protomech.LOC_LEG);
                }
            case 12:
                return new HitData(Protomech.LOC_HEAD);

        }

        return null;
    }

    /**
     * Protos can't transfer crits.
     */
    @Override
    public boolean canTransferCriticals(int loc) {
        return false;
    }

    /**
     * Gets the location that excess damage transfers to
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        switch (hit.getLocation()) {
            case LOC_NMISS:
                return new HitData(LOC_NONE);
            case LOC_LARM:
            case LOC_LEG:
            case LOC_RARM:
            case LOC_HEAD:
            case LOC_MAINGUN:
                return new HitData(LOC_TORSO, hit.isRear(), hit.getEffect(),
                        hit.hitAimedLocation(), hit.getSpecCritMod(),
                        hit.getSpecCrit(), hit.isFromFront(),
                        hit.getGeneralDamageType(), hit.glancingMod());
            case LOC_TORSO:
            default:
                return new HitData(LOC_DESTROYED);
        }
    }

    /**
     * Gets the location that is destroyed recursively
     */
    @Override
    public int getDependentLocation(int loc) {

        return LOC_NONE;
    }
    
    @Override
    public int firstArmorIndex() {
        return LOC_HEAD;
    }

    /**
     * Sets the internal structure for the pmech.
     *
     * @param head
     *            head
     * @param torso
     *            center torso
     * @param arm
     *            right/left arm
     * @param legs
     *            right/left leg
     * @param mainGun
     *            main gun
     */
    public void setInternal(int head, int torso, int arm, int legs, int mainGun) {
        initializeInternal(IArmorState.ARMOR_NA, LOC_BODY);
        initializeInternal(head, LOC_HEAD);
        initializeInternal(torso, LOC_TORSO);
        initializeInternal(arm, LOC_RARM);
        initializeInternal(arm, LOC_LARM);
        initializeInternal(legs, LOC_LEG);
        initializeInternal(mainGun, LOC_MAINGUN);
    }

    /**
     * Set the internal structure to the appropriate value for the pmech's
     * weight class
     */
    @Override
    public void autoSetInternal() {
        int mainGunIS = hasMainGun() ? (getWeight() > 9 ? 2 : 1) : IArmorState.ARMOR_NA;
        switch ((int) weight) {
        // H, TSO, ARM, LEGS, MainGun
            case 2:
                setInternal(1, 2, isQuad() ? IArmorState.ARMOR_NA : 1,
                        isQuad() ? 4 : 2, mainGunIS);
                break;
            case 3:
                setInternal(1, 3, isQuad() ? IArmorState.ARMOR_NA : 1,
                        isQuad() ? 4 : 2, mainGunIS);
                break;
            case 4:
                setInternal(1, 4, isQuad() ? IArmorState.ARMOR_NA : 1,
                        isQuad() ? 5 : 3, mainGunIS);
                break;
            case 5:
                setInternal(1, 5, isQuad() ? IArmorState.ARMOR_NA : 1,
                        isQuad() ? 5 : 3, mainGunIS);
                break;
            case 6:
                setInternal(2, 6, isQuad() ? IArmorState.ARMOR_NA : 2,
                        isQuad() ? 8 : 4, mainGunIS);
                break;
            case 7:
                setInternal(2, 7, isQuad() ? IArmorState.ARMOR_NA : 2,
                        isQuad() ? 8 : 4, mainGunIS);
                break;
            case 8:
                setInternal(2, 8, isQuad() ? IArmorState.ARMOR_NA : 2,
                        isQuad() ? 9 : 5, mainGunIS);
                break;
            case 9:
                setInternal(2, 9, isQuad() ? IArmorState.ARMOR_NA : 2,
                        isQuad() ? 9 : 5, mainGunIS);
                break;
            case 10:
                setInternal(3, 10, isQuad() ? IArmorState.ARMOR_NA : 3,
                        isQuad() ? 12 : 6, mainGunIS);
                break;
            case 11:
                setInternal(3, 11, isQuad() ? IArmorState.ARMOR_NA : 3,
                        isQuad() ? 12 : 6, mainGunIS);
                break;
            case 12:
                setInternal(3, 12, isQuad() ? IArmorState.ARMOR_NA : 3,
                        isQuad() ? 13 : 7, mainGunIS);
                break;
            case 13:
                setInternal(3, 13, isQuad() ? IArmorState.ARMOR_NA : 3,
                        isQuad() ? 13 : 7, mainGunIS);
                break;
            case 14:
                setInternal(4, 14, isQuad() ? IArmorState.ARMOR_NA : 4,
                        isQuad() ? 14 : 8, mainGunIS);
                break;
            case 15:
                setInternal(4, 15, isQuad() ? IArmorState.ARMOR_NA : 4,
                        isQuad() ? 14 : 8, mainGunIS);
                break;
        }
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    @Override
    public Mounted addEquipment(EquipmentType etype, int loc)
            throws LocationFullException {
        return addEquipment(etype, loc, false, -1);
    }

    @Override
    public Mounted addEquipment(EquipmentType etype, int loc,
            boolean rearMounted) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, rearMounted, -1);
        return mounted;
    }

    @Override
    public Mounted addEquipment(EquipmentType etype, int loc,
            boolean rearMounted, int shots) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, rearMounted, shots);
        return mounted;

    }

    /**
     * Mounts the specified weapon in the specified location.
     */
    @Override
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted,
            int shots) throws LocationFullException {
        if (mounted.getType() instanceof AmmoType) {
            // Damn protomech ammo; nasty hack, should be cleaner
            if (-1 != shots) {
                mounted.setShotsLeft(shots);
                mounted.setOriginalShots(shots);
                mounted.setAmmoCapacity(shots * ((AmmoType) mounted.getType()).getKgPerShot() / 1000);
                super.addEquipment(mounted, loc, rearMounted);
                return;
            }
        }

        if (mounted.getType().isHittable() && (loc != LOC_BODY)) {
            int max = maxWeapons(loc);
            if (max == 0) {
                throw new LocationFullException("Weapon "
                        + mounted.getName() + " can't be mounted in "
                        + getLocationAbbr(loc));
            }
            // EDP armor reduces the number of torso slots by one.
            if ((loc == LOC_TORSO) && (getArmorType(loc) == EquipmentType.T_ARMOR_EDP)) {
                max--;
            }
            long current = getEquipment().stream().filter(m -> (m.getLocation() == loc)
                    && m.getType().isHittable()).count();
            if (current >= max) {
                throw new LocationFullException("Weapon "
                        + mounted.getName() + " exceeds maximum for "
                        + getLocationAbbr(loc));
            }
        }
        super.addEquipment(mounted, loc, rearMounted);
    }

    public int maxWeapons(int location) {
        switch (location) {
            case LOC_LARM:
            case LOC_RARM:
                return 1;
            case LOC_MAINGUN:
                if (m_bHasNoMainGun) {
                    return 0;
                } else if (isQuad()) {
                    return 2;
                } else {
                    return 1;
                }
            case LOC_TORSO:
                if (getWeight() < 10.0) {
                    return isQuad() ? 4 : 2;
                } else {
                    return isQuad() ? 6 : 3;
                }
            default:
                return 0;
        }
    }

    @Override
    protected int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return ProtoMekBVCalculator.calculateBV(this, ignoreSkill, calculationReport);
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7030);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(getCrew().getDescVector(true));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);

        if (isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if (killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if (killer != null) {
                r = new Report(7072, Report.PUBLIC);
                r.addDesc(killer);
            } else {
                r = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    @Override
    public int getMaxElevationChange() {
        return 1;
    }

    @Override
    public int getMaxElevationDown(int currElevation) {
        // Gliders have a maximum elevation of 12 over the surface terrain.
        if ((currElevation > 0)
                && (getMovementMode() == EntityMovementMode.WIGE)) {
            return 12;
        }
        return super.getMaxElevationDown(currElevation);
    }

    @Override
    public int getArmor(int loc, boolean rear) {
        if ((loc == LOC_BODY)
                || (loc == LOC_NMISS)) {
            return IArmorState.ARMOR_NA;
        }
        return super.getArmor(loc, rear);
    }

    @Override
    public int getInternal(int loc) {
        if ((loc == LOC_BODY)
                || (loc == LOC_NMISS)) {
            return IArmorState.ARMOR_NA;
        }
        return super.getInternal(loc);
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String getLocationAbbr(int loc) {
        if (loc == LOC_NMISS) {
            return "a near miss";
        }
        return super.getLocationAbbr(loc);
    }

    /**
     * * Not every Protomech has a main gun.
     */
    public boolean hasMainGun() {
        return !m_bHasNoMainGun;
    }

    /**
     * * Not every Protomech has a main gun.
     */
    public void setHasMainGun(boolean b) {
        m_bHasNoMainGun = !b;
    }

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
        if (m_bHasNoMainGun) {
            return NUM_PMECH_LOCATIONS - 1;
        }
        return NUM_PMECH_LOCATIONS;
    }

    @Override
    public int getBodyLocation() {
        return LOC_BODY;
    }

    /**
     * Protomechs have no piloting skill (set to 5 for BV purposes)
     */
    @Override
    public void setCrew(Crew p) {
        super.setCrew(p);
        if (null != p) {
            getCrew().setPiloting(5, 0);
        }
    }

    @Override
    public boolean canCharge() {
        // Protos can't Charge
        return false;
    }

    @Override
    public boolean canDFA() {
        // Protos can't DFA
        return false;
    }

    /**
     * @return The cost in C-Bills of the ProtoMech in question.
     */
    @Override
    public double getCost(boolean ignoreAmmo) {
        double retVal = 0;

        // Add the cockpit, a constant cost.
        if (weight >= 10) {
            retVal += 800000;
        } else {
            retVal += 500000;
        }

        // Add life support, a constant cost.
        retVal += 75000;

        // Sensor cost is based on tonnage.
        retVal += 2000 * weight;

        // Musculature cost is based on tonnage.
        retVal += 2000 * weight;

        // Internal Structure cost is based on tonnage.
        if (isGlider) {
            retVal += 600 * weight;
        } else if (isQuad) {
            retVal += 500 * weight;
        } else {
            retVal += 400 * weight;
        }

        // Arm actuators are based on tonnage.
        // Their cost is listed separately?
        retVal += 2 * 180 * weight;

        // Leg actuators are based on tonnage.
        retVal += 540 * weight;

        // Engine cost is based on tonnage and rating.
        if (hasEngine()) {
            retVal += (5000 * weight * getEngine().getRating()) / 75;
        }

        // Jump jet cost is based on tonnage and jump MP.
        retVal += weight * getJumpMP() * getJumpMP() * 200;

        // Heat sinks is constant per sink.
        // per the construction rules, we need enough sinks to sink all energy
        // weapon heat, so we just calculate the cost that way.
        int sinks = 0;
        for (Mounted mount : getWeaponList()) {
            if (mount.getType().hasFlag(WeaponType.F_ENERGY)) {
                WeaponType wtype = (WeaponType) mount.getType();
                sinks += wtype.getHeat();
            }
        }
        retVal += 2000 * sinks;

        // Armor is linear on the armor value of the Protomech
        retVal += getTotalArmor() * EquipmentType.getProtomechArmorCostPerPoint(getArmorType(firstArmorIndex()));

        // Add in equipment cost.
        retVal += getWeaponsAndEquipmentCost(ignoreAmmo);

        // Finally, apply the Final ProtoMech Cost Multiplier
        retVal *= getPriceMultiplier();

        return retVal;
    }

    @Override
    public double getPriceMultiplier() {
        return 1 + (weight / 100.0); // weight multiplier
    }

    @Override
    public boolean doomedInExtremeTemp() {
        return false;
    }

    @Override
    public boolean doomedInVacuum() {
        return false;
    }

    @Override
    public boolean doomedOnGround() {
        return false;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return true;
    }

    @Override
    public boolean hasActiveEiCockpit() {
        return (super.hasActiveEiCockpit() && (getCritsHit(LOC_HEAD) == 0));
    }

    @Override
    public boolean canAssaultDrop() {
        return true;
    }

    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        Hex hex = game.getBoard().getHex(c);
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if ((hex.containsTerrain(Terrains.PAVEMENT)
                    || hex.containsTerrain(Terrains.ROAD))
                    && (!hex.containsTerrain(Terrains.BUILDING)
                            && !hex.containsTerrain(Terrains.RUBBLE))) {
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

        return (hex.terrainLevel(Terrains.WOODS) > 2)
                || (hex.terrainLevel(Terrains.JUNGLE) > 2);
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }

    @Override
    public void setGrappled(int id, boolean attacker) {
        grappled_id = id;
        isGrappleAttacker = attacker;
    }

    @Override
    public boolean isGrappleAttacker() {
        return isGrappleAttacker;
    }

    @Override
    public int getGrappled() {
        return grappled_id;
    }
    
    @Override
    public boolean isGrappledThisRound() {
        return grappledThisRound;
    }
    
    @Override
    public void setGrappledThisRound(boolean grappled) {
        grappledThisRound = grappled;
    }

    @Override
    public boolean isEligibleForMovement() {
        // For normal grapples, neither unit can move
        // If the grapple is caused by a chain whip, then the attacker can move
        // (this breaks the grapple), TO pg 289
        if ((grappled_id != Entity.NONE)
                && (!isChainWhipGrappled() || !isGrappleAttacker())) {
            return false;
        }
        return super.isEligibleForMovement();
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#checkSkid(int, megamek.common.Hex, int,
     * megamek.common.MoveStep, int, int, megamek.common.Coords,
     * megamek.common.Coords, boolean, int)
     */
    @Override
    public PilotingRollData checkSkid(EntityMovementType moveType,
            Hex prevHex, EntityMovementType overallMoveType,
            MoveStep prevStep, MoveStep currStep, int prevFacing, int curFacing, Coords lastPos,
            Coords curPos, boolean isInfantry, int distance) {
        return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE,
                "ProtoMechs can't skid");
    }

    public PilotingRollData checkGliderLanding() {
        if (!isGlider) {
            return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE,
                    "Not a glider protomech.");
        }
        if (getCritsHit(LOC_LEG) > 2) {
            return new PilotingRollData(getId(), TargetRoll.AUTOMATIC_FAIL,
                    "Landing with destroyed legs.");
        }
        if (!getCrew().isActive()) {
            return new PilotingRollData(getId(), TargetRoll.AUTOMATIC_FAIL,
                    "Landing incapacitated pilot.");
        }
        if (getRunMP() < 4) {
            return new PilotingRollData(getId(), 8,
                    "Forced landing with insufficient thrust.");
        }
        return new PilotingRollData(getId(), 4, "Attempting to land");
    }

    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat,
            boolean ignoremodulararmor) {
        return getRunMP(gravity, ignoreheat, ignoremodulararmor);
    }

    @Override
    public void setAlphaStrikeMovement(Map<String,Integer> moves) {
        double walk = getWalkMP();
        if (hasMyomerBooster()) {
            walk *= 1.25;
        }
        int baseWalk = (int) Math.round(walk * 2);
        int baseJump = getJumpMP() * 2;
        if (baseJump > 0) {
            if (baseJump != baseWalk) {
                moves.put("", baseWalk);
            }
            moves.put("j", baseJump);
        } else {
            moves.put(getMovementModeAsBattleForceString(), baseWalk);
        }
    }
    
    @Override
    /*
     * Each ProtoMech has 1 Structure point
     */
    public int getBattleForceStructurePoints() {
        return 1;
    }

    @Override
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        for (Mounted m : getEquipment()) {
            if (!(m.getType() instanceof MiscType)) {
                continue;
            }
            if (m.getType().hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                if (getWeight() < 10) {
                    specialAbilities.put(BattleForceSPA.MCS, null);
                } else {
                    specialAbilities.put(BattleForceSPA.UCS, null);                    
                }
            }
        }
        specialAbilities.put(BattleForceSPA.SOA, null);
        if (getMovementMode().equals(EntityMovementMode.WIGE)) {
            specialAbilities.put(BattleForceSPA.GLD, null);
        }
    }
    
    @Override
    public int getEngineHits() {
        if (this.isEngineHit()) {
            return 1;
        }
        return 0;
    }
    
    public int getWingHits() {
        return wingHits;
    }
    
    public void setWingHits(int hits) {
        wingHits = hits;
    }

    public boolean isEDPCharged() {
        return hasWorkingMisc(MiscType.F_ELECTRIC_DISCHARGE_ARMOR)
                && edpCharged;
    }

    public void setEDPCharged(boolean charged) {
        edpCharged = charged;
    }

    public boolean isEDPCharging() {
        for (Mounted misc : getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_ELECTRIC_DISCHARGE_ARMOR)
                    && misc.curMode().equals("charging")) {
                return true;
            }
        }
        return false;
    }

    public boolean isQuad() {
        return isQuad;
    }

    public void setIsQuad(boolean isQuad) {
        this.isQuad = isQuad;
    }
    
    public boolean isGlider() {
        return isGlider;
    }
    
    public void setIsGlider(boolean isGlider) {
        this.isGlider = isGlider;
    }
    
    /**
     * WoB protomech interface allows it to be piloted by a quadruple amputee with a VDNI implant.
     * No effect on game play.
     * 
     * @return Whether the protomech is equipped with an Inner Sphere Protomech Interface.
     */
    public boolean hasInterfaceCockpit() {
        return interfaceCockpit;
    }
    
    /**
     * Sets whether the protomech has an Inner Sphere Protomech Interface. This will also determine
     * whether it is a mixed tech unit.
     * 
     * @param interfaceCockpit Whether the protomech has an IS interface
     */
    public void setInterfaceCockpit(boolean interfaceCockpit) {
        this.interfaceCockpit = interfaceCockpit;
        mixedTech = interfaceCockpit;
    }

    @Override
    public boolean isCrippled() {
        if ((getCrew() != null) && (getCrew().getHits() >= 4)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn())
            {
                System.out.println(getDisplayName()
                        + " CRIPPLED: Pilot has taken 4+ damage.");
            }
            return true;
        }

        for (Mounted weap : getWeaponList()) {
            if (!weap.isCrippled()) {
                return false;
            }
        }
        if (PreferenceManager.getClientPreferences().debugOutputOn())
        {
            System.out.println(getDisplayName()
                    + " CRIPPLED: has no more viable weapons.");
        }
        return true;
    }
    
    @Override
    public boolean isCrippled(boolean checkCrew) {
        return isCrippled();
    }

    @Override
    public boolean isDmgHeavy() {
        if (getArmorRemainingPercent() <= 0.25) {
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 3)) {
            return true;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.75;
    }

    @Override
    public boolean isDmgModerate() {
        if (getArmorRemainingPercent() <= 0.5) {
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 2)) {
            return true;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.5;
    }

    @Override
    public boolean isDmgLight() {
        if (getArmorRemainingPercent() <= 0.75) {
            return true;
        }

        if ((getCrew() != null) && (getCrew().getHits() == 1)) {
            return true;
        }

        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double) totalInoperable / totalWeapons) >= 0.25;
    }

    @Override
    public String getLocationDamage(int loc) {
        int hits = getCritsHit(loc);
        if (hits > 0) {
            String strHit = " critical hit";
            if (hits > 1) {
                strHit += "s";
            }
            return hits + strHit;
        }
        return "";
    }

    public boolean isEngineHit() {
        return engineHit;
    }

    public void setEngineHit(boolean b) {
        engineHit = b;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_PROTOMECH;
    }
    
    @Override
    public PilotingRollData checkLandingInHeavyWoods(
            EntityMovementType overallMoveType, Hex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE,
                         "Protomechs cannot fall");
        return roll;
    }
    
    /**
     * Based on the protomech's current damage status, return valid brace locations.
     */
    public List<Integer> getValidBraceLocations() {
        List<Integer> validLocations = new ArrayList<>();
        
        if (!isLocationBad(Protomech.LOC_MAINGUN)) {
            validLocations.add(Protomech.LOC_MAINGUN);
        }
        
        return validLocations;
    }
    
    /**
     * Protomechs can brace if not prone, crew conscious and have a main gun
     */
    @Override
    public boolean canBrace() {
        return !isProne() &&
                getCrew().isActive() &&
                !isLocationBad(Protomech.LOC_MAINGUN);
    }
    
    @Override
    public int getBraceMPCost() {
        return 0;
    }
}
