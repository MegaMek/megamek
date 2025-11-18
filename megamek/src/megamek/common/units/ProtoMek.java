/*
 * Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.io.PrintWriter;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.*;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.cost.ProtoMekCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.interfaces.ITechnology;
import megamek.common.moves.MoveStep;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.preference.PreferenceManager;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.util.RoundWeight;
import megamek.logging.MMLogger;

/**
 * ProtoMeks. Level 2 Clan equipment.
 */
public class ProtoMek extends Entity {
    @Serial
    private static final long serialVersionUID = -1376410042751538158L;
    private static final MMLogger LOGGER = MMLogger.create(ProtoMek.class);

    public static final int NUM_PROTOMEK_LOCATIONS = 7;

    private static final String[] LOCATION_NAMES = { "Body", "Head", "Torso", "Right Arm", "Left Arm", "Legs",
                                                     "Main Gun" };
    private static final String[] LOCATION_ABBREVIATIONS = { "BD", "HD", "T", "RA", "LA", "L", "MG" };

    // Crew damage caused so far by crits to this location.
    // Needed for location destruction pilot damage.
    private final int[] pilotDamageTaken = { 0, 0, 0, 0, 0, 0, 0 };

    /**
     * Not every ProtoMek has a main gun. N.B. Regardless of the value set here, the variable is initialized to false
     * until after the Entity is initialized, which is too late to allow main gun armor, hence the convoluted reverse
     * logic.
     */
    private boolean hasNoMainGun;

    public static final int LOC_BODY = 0;
    public static final int LOC_HEAD = 1;
    public static final int LOC_TORSO = 2;
    public static final int LOC_RIGHT_ARM = 3;
    public static final int LOC_LEFT_ARM = 4;
    public static final int LOC_LEG = 5;
    public static final int LOC_MAIN_GUN = 6;

    // Near miss repairs.
    public static final int LOC_NEAR_MISS = 7;

    // "Systems". These represent protoMek critical hits; which remain constant regardless of proto. doesn't matter
    // what gets hit in a proto section, just the number of times it's been critted so just have the right number of
    // these systems and it works.
    public static final int SYSTEM_ARM_CRIT = 0;
    public static final int SYSTEM_LEG_CRIT = 1;
    public static final int SYSTEM_HEAD_CRIT = 2;
    public static final int SYSTEM_TORSO_CRIT = 3;
    public static final int SYSTEM_TORSO_WEAPON_A = 4;
    public static final int SYSTEM_TORSO_WEAPON_B = 5;
    public static final int SYSTEM_TORSO_WEAPON_C = 6;
    public static final int SYSTEM_TORSO_WEAPON_D = 7;
    public static final int SYSTEM_TORSO_WEAPON_E = 8;
    public static final int SYSTEM_TORSO_WEAPON_F = 9;

    private static final int[] NUM_OF_SLOTS = { 0, 2, 3, 2, 2, 3, 0 };

    public static final int[] POSSIBLE_PILOT_DAMAGE = { 0, 1, 3, 1, 1, 1, 0 };

    public static final String[] SYSTEM_NAMES = { "Arm", "Leg", "Head", "Torso" };

    /**
     * Contains a mapping of locations which are blocked when carrying cargo in the "key" location
     */
    public static final Map<Integer, List<Integer>> BLOCKED_FIRING_LOCATIONS;

    static {
        BLOCKED_FIRING_LOCATIONS = new HashMap<>();
        BLOCKED_FIRING_LOCATIONS.put(LOC_LEFT_ARM, new ArrayList<>());
        BLOCKED_FIRING_LOCATIONS.get(LOC_LEFT_ARM).add(LOC_LEFT_ARM);
        BLOCKED_FIRING_LOCATIONS.get(LOC_LEFT_ARM).add(LOC_TORSO);

        BLOCKED_FIRING_LOCATIONS.put(LOC_RIGHT_ARM, new ArrayList<>());
        BLOCKED_FIRING_LOCATIONS.get(LOC_RIGHT_ARM).add(LOC_RIGHT_ARM);
        BLOCKED_FIRING_LOCATIONS.get(LOC_RIGHT_ARM).add(LOC_TORSO);
    }

    // For grapple attacks
    private int grappled_id = Entity.NONE;
    private boolean isGrappleAttacker = false;
    private boolean grappledThisRound = false;

    private boolean edpCharged = true;
    private int edpChargeTurns = 0;

    // jump types
    public static final int JUMP_UNKNOWN = -1;
    public static final int JUMP_NONE = 0;
    public static final int JUMP_STANDARD = 1;
    public static final int JUMP_IMPROVED = 2;

    private int jumpType = JUMP_UNKNOWN;

    private boolean isQuad = false;
    private boolean isGlider = false;
    private boolean interfaceCockpit = false;
    private int wingHits = 0;

    // for MHQ
    private boolean engineHit = false;

    /**
     * Construct a new, blank, pMek.
     */
    public ProtoMek() {
        super();
        setCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_HEAD_CRIT));
        setCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_HEAD_CRIT));
        setCritical(LOC_RIGHT_ARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ARM_CRIT));
        setCritical(LOC_RIGHT_ARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ARM_CRIT));
        setCritical(LOC_LEFT_ARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ARM_CRIT));
        setCritical(LOC_LEFT_ARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ARM_CRIT));
        setCritical(LOC_TORSO, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TORSO_CRIT));
        setCritical(LOC_TORSO, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TORSO_CRIT));
        setCritical(LOC_TORSO, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_TORSO_CRIT));
        setCritical(LOC_LEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LEG_CRIT));
        setCritical(LOC_LEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LEG_CRIT));
        setCritical(LOC_LEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LEG_CRIT));
        hasNoMainGun = true;
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
     * Returns # of pilot damage points taken due to crits to the location so far.
     */
    public int getPilotDamageTaken(int loc) {
        return pilotDamageTaken[loc];
    }

    /**
     * Get the weapon in the given torso location (if any).
     *
     * @param torsoNum - a <code>int</code> that corresponds to SYSTEM_TORSO_WEAPON_A thorough SYSTEM_TORSO_WEAPON_F
     *
     * @return the <code>Mounted</code> weapon at the needed location. This value will be <code>null</code> if no weapon
     *       is in the indicated location.
     */
    public Mounted<?> getTorsoWeapon(int torsoNum) {
        int index = torsoNum - SYSTEM_TORSO_WEAPON_A;
        // There are some non-weapons that take up weapon critical slots
        List<Mounted<?>> torsoEquipment = getEquipment().stream()
              .filter(m -> (m.getLocation() == LOC_TORSO) && m.getType().isHittable())
              .toList();
        if (index < torsoEquipment.size()) {
            return torsoEquipment.get(index);
        } else {
            return null;
        }
    }

    /**
     * Tells the ProtoMek to note pilot damage taken from crit damage to the location
     */
    public void setPilotDamageTaken(int loc, int damage) {
        pilotDamageTaken[loc] = damage;
    }

    /**
     * ProtoMeks don't take piloting skill rolls.
     */
    // TODO: this is no longer true in TacOps. ProtoMeks sometimes make PSRs using
    // their gunnery skill
    @Override
    public PilotingRollData getBasePilotingRoll() {
        return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE, "ProtoMeks never take PSRs.");
    }

    /**
     * A "shaded" critical is a box shaded on the record sheet, implies pilot damage when hit. Returns whether shaded.
     */
    public boolean shaded(int loc, int numHit) {
        return switch (loc) {
            case LOC_HEAD, LOC_LEFT_ARM, LOC_RIGHT_ARM -> (2 == numHit);
            case LOC_TORSO -> (0 < numHit);
            case LOC_MAIN_GUN, LOC_NEAR_MISS, LOC_LEG -> (3 == numHit);
            default -> false;
        };
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        if (isEngineHit()) {
            return 0;
        }
        int mp = getOriginalWalkMP();

        if (!mpCalculationSetting.ignoreWeather() && (game != null)) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);
        }

        // Gravity, ProtoMeks can't get faster
        if (!mpCalculationSetting.ignoreGravity()) {
            mp = Math.min(mp, applyGravityEffectsOnMP(mp));
        }

        if (isGlider()) {
            // Torso crits reduce glider mp as jump
            int torsoCrits = getCritsHit(ProtoMek.LOC_TORSO);
            if (torsoCrits == 1) {
                mp--;
            } else if (torsoCrits == 2) {
                mp /= 2;
            }
            // Near misses damage the wings/flight systems, which reduce MP by one per hit.
            mp -= getWingHits();
        } else {
            int legCrits = getCritsHit(ProtoMek.LOC_LEG);
            if (legCrits == 1) {
                mp--;
            } else if (legCrits == 2) {
                mp /= 2;
            } else if (legCrits == 3) {
                mp = 0;
            }
        }

        return Math.max(mp, 0);
    }

    @Override
    public String getRunMPasString(boolean gameState) {
        if (hasMyomerBooster()) {
            return getRunMP(MPCalculationSetting.NO_MYOMER_BOOSTER) + "(" + getRunMP() + ")";
        } else {
            return Integer.toString(getRunMP());
        }
    }

    /**
     * Counts the # of crits taken by proto in the location. Needed in several places, due to proto set criticalSlots.
     */
    public int getCritsHit(int loc) {
        int count = 0;
        int numberOfCriticalSlots = this.getNumberOfCriticalSlots(loc);
        for (int i = 0; i < numberOfCriticalSlots; i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if (ccs.isDamaged() || ccs.isBreached()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        return roll;
    }

    @Override
    public int getNumberOfCriticalSlots(int loc) {
        return switch (loc) {
            case LOC_HEAD, LOC_LEFT_ARM, LOC_RIGHT_ARM -> 2;
            case LOC_LEG, LOC_TORSO -> 3;
            case LOC_BODY ->
                // This is needed to keep everything ordered in the unit display system tab
                  1;
            default -> 0;
        };
    }

    public static final TechAdvancement TA_STANDARD_PROTOMEK = new TechAdvancement(TechBase.CLAN).setClanAdvancement(
                3055,
                3059,
                3060)
          .setClanApproximate(true, false, false)
          .setPrototypeFactions(Faction.CSJ)
          .setProductionFactions(Faction.CSJ)
          .setTechRating(TechRating.F)
          .setAvailability(AvailabilityValue.X,
                AvailabilityValue.X,
                AvailabilityValue.E,
                AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    public static final TechAdvancement TA_QUAD = new TechAdvancement(TechBase.CLAN).setClanAdvancement(3075,
                3083,
                3100)
          .setClanApproximate(false, true, false)
          .setPrototypeFactions(Faction.CLAN)
          .setProductionFactions(Faction.CCC)
          .setTechRating(TechRating.F)
          .setAvailability(AvailabilityValue.X,
                AvailabilityValue.X,
                AvailabilityValue.E,
                AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    public static final TechAdvancement TA_ULTRA = new TechAdvancement(TechBase.CLAN).setClanAdvancement(
                3075,
                3083,
                3100)
          .setClanApproximate(false, true, false)
          .setPrototypeFactions(Faction.CLAN)
          .setProductionFactions(Faction.CCY)
          .setTechRating(TechRating.F)
          .setAvailability(AvailabilityValue.X,
                AvailabilityValue.X,
                AvailabilityValue.D,
                AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    public static final TechAdvancement TA_GLIDER = new TechAdvancement(TechBase.CLAN).setClanAdvancement(
                3075,
                3084,
                3100)
          .setClanApproximate(false, true, false)
          .setPrototypeFactions(Faction.CLAN)
          .setProductionFactions(Faction.CSR)
          .setTechRating(TechRating.F)
          .setAvailability(AvailabilityValue.X,
                AvailabilityValue.X,
                AvailabilityValue.E,
                AvailabilityValue.E)
          .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    public static final TechAdvancement TA_INTERFACE_COCKPIT = new TechAdvancement(TechBase.IS).setISAdvancement(
                3071,
                ITechnology.DATE_NONE,
                ITechnology.DATE_NONE,
                3085)
          .setISApproximate(true)
          .setPrototypeFactions(Faction.WB)
          .setTechRating(TechRating.E)
          .setAvailability(AvailabilityValue.X,
                AvailabilityValue.X,
                AvailabilityValue.F,
                AvailabilityValue.X)
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
            return TA_STANDARD_PROTOMEK;
        }
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        if (interfaceCockpit) {
            ctl.addComponent(TA_INTERFACE_COCKPIT);
        }
    }

    @Override
    public void newRound(int roundNumber) {
        if (hasWorkingMisc(MiscType.F_ELECTRIC_DISCHARGE_ARMOR) && !edpCharged) {
            for (Mounted<?> misc : getMisc()) {
                if (misc.getType().hasFlag(MiscType.F_ELECTRIC_DISCHARGE_ARMOR) && misc.curMode().equals("charging")) {
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
    }

    @Override
    public int getJumpMP(MPCalculationSetting mpCalculationSetting) {
        if (mpCalculationSetting.ignoreSubmergedJumpJets() && isUnderwater()) {
            return 0;
        }
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
        if (!mpCalculationSetting.ignoreWeather() && hasWorkingMisc(MiscType.F_PARTIAL_WING)) {
            Atmosphere atmosphere = Atmosphere.STANDARD;
            if (game != null) {
                atmosphere = game.getPlanetaryConditions().getAtmosphere();
            }
            switch (atmosphere) {
                case VERY_HIGH:
                case HIGH:
                    jump += 3;
                    break;
                case STANDARD:
                case THIN:
                    jump += 2;
                    break;
                case TRACE:
                    jump += 1;
                    break;
                default:
            }
        }

        return mpCalculationSetting.ignoreGravity() ? jump : Math.min(applyGravityEffectsOnMP(jump), jump);
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public String getMovementString(EntityMovementType movementType) {
        return switch (movementType) {
            case MOVE_NONE -> "None";
            case MOVE_WALK, MOVE_VTOL_WALK -> "Walked";
            case MOVE_VTOL_RUN, MOVE_RUN -> "Ran";
            case MOVE_JUMP -> "Jumped";
            default -> "Unknown!";
        };
    }

    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
        return switch (movementType) {
            case MOVE_NONE -> "N";
            case MOVE_WALK, MOVE_VTOL_WALK -> "W";
            case MOVE_RUN, MOVE_VTOL_RUN -> "R";
            case MOVE_JUMP -> "J";
            default -> "?";
        };
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return !((getCritsHit(LOC_LEG) > 2) || isBracing() || getAlreadyTwisted());
    }

    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            if (isQuad()) {
                return true;
            }
            return (rotate == 0) || (rotate == 1) || (rotate == -1) || (rotate == -5);
        }
        return rotate == 0;
    }

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
        return RoundWeight.standard(ArmorType.forEntity(this).getWeightPerPoint() * getTotalOArmor(), this);
    }

    @Override
    public boolean canPickupGroundObject() {
        return (!isLocationBad(ProtoMek.LOC_LEFT_ARM) && (getCarriedObject(ProtoMek.LOC_LEFT_ARM) == null)) ||
              (!isLocationBad(ProtoMek.LOC_RIGHT_ARM) && (getCarriedObject(ProtoMek.LOC_RIGHT_ARM) == null)) ||
              super.canPickupGroundObject();
    }

    @Override
    public double maxGroundObjectTonnage() {
        double percentage = 0.0;

        if (!isLocationBad(ProtoMek.LOC_LEFT_ARM) && (getCarriedObject(ProtoMek.LOC_LEFT_ARM) == null)) {
            percentage += 0.05;
        }
        if (!isLocationBad(ProtoMek.LOC_RIGHT_ARM) && (getCarriedObject(ProtoMek.LOC_RIGHT_ARM) == null)) {
            percentage += 0.05;
        }

        double heavyLifterMultiplier = hasAbility(OptionsConstants.PILOT_HVY_LIFTER) ? 1.5 : 1.0;

        return getWeight() * percentage * heavyLifterMultiplier;
    }

    @Override
    public List<Integer> getDefaultPickupLocations() {
        List<Integer> result = new ArrayList<>();

        if ((getCarriedObject(ProtoMek.LOC_LEFT_ARM) == null) && !isLocationBad(ProtoMek.LOC_LEFT_ARM)) {
            result.add(ProtoMek.LOC_LEFT_ARM);
        }
        if ((getCarriedObject(ProtoMek.LOC_RIGHT_ARM) == null) && !isLocationBad(ProtoMek.LOC_RIGHT_ARM)) {
            result.add(ProtoMek.LOC_RIGHT_ARM);
        }

        return result;
    }

    @Override
    public List<Integer> getValidHalfWeightPickupLocations(ICarryable cargo) {
        List<Integer> result = new ArrayList<>();

        // if we can pick the object up according to "one-handed pick up rules" in TacOps
        if (cargo.getTonnage() <= (getWeight() / 20)) {
            if ((getCarriedObject(ProtoMek.LOC_LEFT_ARM) == null) && !isLocationBad(ProtoMek.LOC_LEFT_ARM)) {
                result.add(ProtoMek.LOC_LEFT_ARM);
            }

            if ((getCarriedObject(ProtoMek.LOC_RIGHT_ARM) == null) && !isLocationBad(ProtoMek.LOC_RIGHT_ARM)) {
                result.add(ProtoMek.LOC_RIGHT_ARM);
            }
        }

        return result;
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        if (!mpCalculationSetting.ignoreMyomerBooster() && hasMyomerBooster()) {
            return (getWalkMP(mpCalculationSetting) * 2);
        } else {
            return super.getRunMP(mpCalculationSetting);
        }
    }

    /**
     * @return True if this ProtoMek mounts an operable Myomer Booster.
     *
     * @see Mounted#isOperable()
     */
    public boolean hasMyomerBooster() {
        return getMisc().stream().filter(Mounted::isOperable).anyMatch(m -> m.getType().hasFlag(MiscType.F_MASC));
    }

    @Override
    public int getWeaponArc(int weaponNumber) {
        final Mounted<?> mounted = getEquipment(weaponNumber);
        if (mounted.isRearMounted()) {
            return Compute.ARC_REAR;
        } else if (mounted.getType().hasFlag(WeaponType.F_VGL)) {
            // VGLs base arc on their facing
            return Compute.firingArcFromVGLFacing(mounted.getFacing());
        } else {
            // front mounted
            return switch (mounted.getLocation()) {
                case LOC_TORSO -> Compute.ARC_FORWARD;
                case LOC_RIGHT_ARM -> Compute.ARC_RIGHT_ARM;
                case LOC_LEFT_ARM -> Compute.ARC_LEFT_ARM;
                case LOC_MAIN_GUN -> Compute.ARC_MAIN_GUN;
                default -> Compute.ARC_360;
            };
        }
    }

    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        // for quads, only the main gun weapons can rotate
        return !isQuad() || getEquipment(weaponId).getLocation() == LOC_MAIN_GUN;
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE, AimingMode.NONE, LosEffects.COVER_NONE);
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        int roll;

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
            LOGGER.error("", t);
        }

        switch (roll) {
            case 2:
                return new HitData(ProtoMek.LOC_MAIN_GUN);
            case 3:
            case 11:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    return new HitData(ProtoMek.LOC_LEG);
                }
                return new HitData(ProtoMek.LOC_NEAR_MISS);
            case 4:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    return new HitData(ProtoMek.LOC_LEG);
                }
                if (!isQuad()) {
                    return new HitData(ProtoMek.LOC_RIGHT_ARM);
                } else {
                    return new HitData(ProtoMek.LOC_LEG);
                }
            case 5:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    if (!isQuad()) {
                        return new HitData(ProtoMek.LOC_RIGHT_ARM);
                    } else {
                        return new HitData(ProtoMek.LOC_LEG);
                    }
                }
            case 9:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    if (!isQuad()) {
                        return new HitData(ProtoMek.LOC_LEFT_ARM);
                    } else {
                        return new HitData(ProtoMek.LOC_LEG);
                    }
                }
                return new HitData(ProtoMek.LOC_LEG);
            case 6:
            case 7:
            case 8:
                return new HitData(ProtoMek.LOC_TORSO);
            case 10:
                if (table == ToHitData.HIT_SPECIAL_PROTO) {
                    return new HitData(ProtoMek.LOC_LEG);
                }
                if (!isQuad()) {
                    return new HitData(ProtoMek.LOC_LEFT_ARM);
                } else {
                    return new HitData(ProtoMek.LOC_LEG);
                }
            case 12:
                return new HitData(ProtoMek.LOC_HEAD);
        }
        return null;
    }

    @Override
    public boolean canTransferCriticalSlots(int loc) {
        return false;
    }

    @Override
    public HitData getTransferLocation(HitData hit) {
        return switch (hit.getLocation()) {
            case LOC_NEAR_MISS -> new HitData(LOC_NONE);
            case LOC_LEFT_ARM, LOC_LEG, LOC_RIGHT_ARM, LOC_HEAD, LOC_MAIN_GUN -> new HitData(LOC_TORSO,
                  hit.isRear(),
                  hit.getEffect(),
                  hit.hitAimedLocation(),
                  hit.getSpecCritMod(),
                  hit.getSpecCrit(),
                  hit.isFromFront(),
                  hit.getGeneralDamageType(),
                  hit.glancingMod());
            default -> new HitData(LOC_DESTROYED);
        };
    }

    @Override
    public int firstArmorIndex() {
        return LOC_HEAD;
    }

    /**
     * Sets the internal structure for the pMek.
     *
     * @param head    head
     * @param torso   center torso
     * @param arm     right/left arm
     * @param legs    right/left leg
     * @param mainGun main gun
     */
    public void setInternal(int head, int torso, int arm, int legs, int mainGun) {
        initializeInternal(IArmorState.ARMOR_NA, LOC_BODY);
        initializeInternal(head, LOC_HEAD);
        initializeInternal(torso, LOC_TORSO);
        initializeInternal(arm, LOC_RIGHT_ARM);
        initializeInternal(arm, LOC_LEFT_ARM);
        initializeInternal(legs, LOC_LEG);
        initializeInternal(mainGun, LOC_MAIN_GUN);
    }

    @Override
    public void autoSetInternal() {
        int mainGunIS = hasMainGun() ? (getWeight() > 9 ? 2 : 1) : IArmorState.ARMOR_NA;
        int headIS = headStructure(weight);
        int armIS = isQuad() ? IArmorState.ARMOR_NA : headIS;
        setInternal(headIS, (int) weight, armIS, legStructure(weight), mainGunIS);
    }

    private int headStructure(double weight) {
        return switch ((int) weight) {
            case 2, 3, 4, 5 -> 1;
            case 6, 7, 8, 9 -> 2;
            case 10, 11, 12, 13 -> 3;
            default -> 4;
        };
    }

    private int legStructure(double weight) {
        if (isQuad) {
            return switch ((int) weight) {
                case 2, 3 -> 4;
                case 4, 5 -> 5;
                case 6, 7 -> 8;
                case 8, 9 -> 9;
                case 10, 11 -> 12;
                case 12, 13 -> 13;
                default -> 14;
            };
        } else {
            return switch ((int) weight) {
                case 2, 3 -> 2;
                case 4, 5 -> 3;
                case 6, 7 -> 4;
                case 8, 9 -> 5;
                case 10, 11 -> 6;
                case 12, 13 -> 7;
                default -> 8;
            };
        }
    }

    @Override
    public Mounted<?> addEquipment(EquipmentType equipmentType, int loc) throws LocationFullException {
        return addEquipment(equipmentType, loc, false, -1);
    }

    @Override
    public Mounted<?> addEquipment(EquipmentType equipmentType, int loc, boolean rearMounted)
          throws LocationFullException {
        Mounted<?> mounted = Mounted.createMounted(this, equipmentType);
        addEquipment(mounted, loc, rearMounted, -1);
        return mounted;
    }

    @Override
    public Mounted<?> addEquipment(EquipmentType etype, int loc, boolean rearMounted, int shots)
          throws LocationFullException {
        Mounted<?> mounted = Mounted.createMounted(this, etype);
        addEquipment(mounted, loc, rearMounted, shots);
        return mounted;

    }

    @Override
    protected void addEquipment(Mounted<?> mounted, int loc, boolean rearMounted, int shots)
          throws LocationFullException {
        if (mounted instanceof AmmoMounted) {
            // Damn protoMek ammo; nasty hack, should be cleaner
            if (-1 != shots) {
                mounted.setShotsLeft(shots);
                mounted.setOriginalShots(shots);
                ((AmmoMounted) mounted).setAmmoCapacity(shots * ((AmmoMounted) mounted).getType().getKgPerShot() /
                      1000);
                super.addEquipment(mounted, loc, rearMounted);
                return;
            }
        }

        if (mounted.getType().isHittable() && (loc != LOC_BODY)) {
            int max = maxWeapons(loc);
            if (max == 0) {
                throw new LocationFullException("Weapon " +
                      mounted.getName() +
                      " can't be mounted in " +
                      getLocationAbbr(loc));
            }
            // EDP armor reduces the number of torso slots by one.
            if ((loc == LOC_TORSO) && (getArmorType(loc) == EquipmentType.T_ARMOR_EDP)) {
                max--;
            }
            long current = getEquipment().stream()
                  .filter(m -> (m.getLocation() == loc) && m.getType().isHittable())
                  .count();
            if (current >= max) {
                throw new LocationFullException("Weapon " +
                      mounted.getName() +
                      " exceeds maximum for " +
                      getLocationAbbr(loc));
            }
        }
        super.addEquipment(mounted, loc, rearMounted);
    }

    public int maxWeapons(int location) {
        switch (location) {
            case LOC_LEFT_ARM:
            case LOC_RIGHT_ARM:
                return 1;
            case LOC_MAIN_GUN:
                if (hasNoMainGun) {
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
        if ((currElevation > 0) && (getMovementMode() == EntityMovementMode.WIGE)) {
            return 12;
        }
        return super.getMaxElevationDown(currElevation);
    }

    @Override
    public int getArmor(int loc, boolean rear) {
        if ((loc == LOC_BODY) || (loc == LOC_NEAR_MISS)) {
            return IArmorState.ARMOR_NA;
        }
        return super.getArmor(loc, rear);
    }

    @Override
    public int getInternal(int loc) {
        if ((loc == LOC_BODY) || (loc == LOC_NEAR_MISS)) {
            return IArmorState.ARMOR_NA;
        }
        return super.getInternal(loc);
    }

    @Override
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
    }

    @Override
    public String getLocationAbbr(int loc) {
        return (loc == LOC_NEAR_MISS) ? "a near miss" : super.getLocationAbbr(loc);
    }

    /**
     * Not every ProtoMek has a main gun.
     */
    public boolean hasMainGun() {
        return !hasNoMainGun;
    }

    /**
     * Not every ProtoMek has a main gun.
     */
    public void setHasMainGun(boolean b) {
        hasNoMainGun = !b;
    }

    @Override
    public int locations() {
        return hasNoMainGun ? NUM_PROTOMEK_LOCATIONS - 1 : NUM_PROTOMEK_LOCATIONS;
    }

    @Override
    public int getBodyLocation() {
        return LOC_BODY;
    }

    @Override
    public void setCrew(Crew p) {
        super.setCrew(p);
        if (null != p) {
            getCrew().setPiloting(5, 0);
        }
    }

    @Override
    public boolean canCharge() {
        return false;
    }

    @Override
    public boolean canDFA() {
        return false;
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return ProtoMekCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        return 1 + (weight / 100.0);
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
    public boolean isLocationProhibited(Coords c, int testBoardId, int currElevation) {
        if (!game.hasBoardLocation(c, testBoardId)) {
            return true;
        }

        Hex hex = game.getHex(c, testBoardId);
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if ((hex.containsTerrain(Terrains.PAVEMENT) || hex.containsTerrain(Terrains.ROAD)) &&
                  (!hex.containsTerrain(Terrains.BUILDING) && !hex.containsTerrain(Terrains.RUBBLE))) {
                return true;
            }
            // Can't deploy on a bridge
            if ((hex.terrainLevel(Terrains.BRIDGE_ELEV) == currElevation) && hex.containsTerrain(Terrains.BRIDGE)) {
                return true;
            }
            // Can't deploy on the surface of water
            if (hex.containsTerrain(Terrains.WATER) && (currElevation == 0)) {
                return true;
            }
        }

        return (hex.terrainLevel(Terrains.WOODS) > 2) || (hex.terrainLevel(Terrains.JUNGLE) > 2);
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
        // If the grapple is caused by a chain whip, then the attacker can move (this breaks the grapple), TO pg 289
        if ((grappled_id != Entity.NONE) && (!isChainWhipGrappled() || !isGrappleAttacker())) {
            return false;
        }
        return super.isEligibleForMovement();
    }

    @Override
    public int getTotalCommGearTons() {
        return 0;
    }

    @Override
    public PilotingRollData checkSkid(EntityMovementType moveType, Hex prevHex, EntityMovementType overallMoveType,
          MoveStep prevStep, MoveStep currStep, int prevFacing, int curFacing, Coords lastPos, Coords curPos,
          boolean isInfantry, int distance) {
        return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE, "ProtoMeks can't skid");
    }

    public PilotingRollData checkGliderLanding() {
        if (!isGlider) {
            return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE, "Not a glider protoMek.");
        } else if (getCritsHit(LOC_LEG) > 2) {
            return new PilotingRollData(getId(), TargetRoll.AUTOMATIC_FAIL, "Landing with destroyed legs.");
        } else if (!getCrew().isActive()) {
            return new PilotingRollData(getId(), TargetRoll.AUTOMATIC_FAIL, "Landing incapacitated pilot.");
        } else if (getRunMP() < 4) {
            return new PilotingRollData(getId(), 8, "Forced landing with insufficient thrust.");
        } else {
            return new PilotingRollData(getId(), 4, "Attempting to land");
        }
    }

    @Override
    public int getEngineHits() {
        return isEngineHit() ? 1 : 0;
    }

    public int getWingHits() {
        return wingHits;
    }

    public void setWingHits(int hits) {
        wingHits = hits;
    }

    public boolean isEDPCharged() {
        return hasWorkingMisc(MiscType.F_ELECTRIC_DISCHARGE_ARMOR) && edpCharged;
    }

    public void setEDPCharged(boolean charged) {
        edpCharged = charged;
    }

    public boolean isEDPCharging() {
        return getMisc().stream()
              .filter(m -> m.getType().hasFlag(MiscType.F_ELECTRIC_DISCHARGE_ARMOR))
              .anyMatch(m -> m.curMode().equals("charging"));
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
     * WoB protoMek interface allows it to be piloted by a quadruple amputee with a VDNI implant. No effect on game
     * play.
     *
     * @return Whether the protoMek is equipped with an Inner Sphere ProtoMek Interface.
     */
    public boolean hasInterfaceCockpit() {
        return interfaceCockpit;
    }

    /**
     * Sets whether the protoMek has an Inner Sphere ProtoMek Interface. This will also determine whether it is a mixed
     * tech unit.
     *
     * @param interfaceCockpit Whether the protoMek has an IS interface
     */
    public void setInterfaceCockpit(boolean interfaceCockpit) {
        this.interfaceCockpit = interfaceCockpit;
        mixedTech = interfaceCockpit;
    }

    @Override
    public boolean isCrippled() {
        if ((getCrew() != null) && (getCrew().getHits() >= 4)) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                LOGGER.debug("{} CRIPPLED: Pilot has taken 4+ damage.", getDisplayName());
            }
            return true;
        }

        for (Mounted<?> mountedWeapon : getWeaponList()) {
            if (!mountedWeapon.isCrippled()) {
                return false;
            }
        }

        if (PreferenceManager.getClientPreferences().debugOutputOn()) {
            LOGGER.debug("{} CRIPPLED: has no more viable weapons.", getDisplayName());
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
        for (Mounted<?> mountedWeapon : getTotalWeaponList()) {
            if (mountedWeapon.isCrippled()) {
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
        for (Mounted<?> weapon : getTotalWeaponList()) {
            if (weapon.isCrippled()) {
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
        for (Mounted<?> weapon : getTotalWeaponList()) {
            if (weapon.isCrippled()) {
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
        return Entity.ETYPE_PROTOMEK;
    }

    @Override
    public PilotingRollData checkLandingInHeavyWoods(EntityMovementType overallMoveType, Hex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE, "ProtoMeks cannot fall");
        return roll;
    }

    @Override
    public List<Integer> getValidBraceLocations() {
        List<Integer> validLocations = new ArrayList<>();

        if (!isLocationBad(ProtoMek.LOC_MAIN_GUN)) {
            validLocations.add(ProtoMek.LOC_MAIN_GUN);
        }

        return validLocations;
    }

    @Override
    public boolean canBrace() {
        return !isProne() && getCrew().isActive() && !isLocationBad(ProtoMek.LOC_MAIN_GUN);
    }

    @Override
    public int getBraceMPCost() {
        return 0;
    }

    @Override
    public boolean isProtoMek() {
        return true;
    }

    @Override
    public int getJumpType() {
        jumpType = JUMP_NONE;
        for (Mounted<?> m : miscList) {
            if (m.getType().hasFlag(MiscType.F_JUMP_JET)) {
                if (m.getType().hasSubType(MiscType.S_IMPROVED)) {
                    jumpType = JUMP_IMPROVED;
                } else {
                    jumpType = JUMP_STANDARD;
                }
            }

        }
        return jumpType;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(3.385 + 1.093 * Math.log(getWeight())));
    }

    @Override
    public int slotNumber(Mounted<?> mounted) {
        int location = mounted.getLocation();
        if (location != Entity.LOC_NONE) {
            int slot = 0;
            for (Mounted<?> equipment : getEquipment()) {
                if (equipment.getLocation() == location) {
                    if (equipment == mounted) {
                        return slot;
                    } else {
                        slot++;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    protected Mounted<?> getEquipmentForWeaponQuirk(QuirkEntry quirkEntry) {
        int location = getLocationFromAbbr(quirkEntry.location());
        int slot = quirkEntry.slot();
        for (Mounted<?> equipment : getEquipment()) {
            if (equipment.getLocation() == location) {
                if (slot == 0) {
                    return equipment;
                } else {
                    slot--;
                }
            }
        }
        return null;
    }

    @Override
    protected Map<Integer, List<Integer>> getBlockedFiringLocations() {
        return BLOCKED_FIRING_LOCATIONS;
    }

    @Override
    public boolean hasPatchworkArmor() {
        return false;
    }

    @Override
    public int getRecoveryTime() {
        return 20;
    }
}
