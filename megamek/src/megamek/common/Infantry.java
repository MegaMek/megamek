/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import megamek.MMConstants;
import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.annotations.Nullable;
import megamek.common.cost.InfantryCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.MiscMounted;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.planetaryconditions.Wind;
import megamek.common.verifier.TestInfantry;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.apache.logging.log4j.LogManager;

import java.text.NumberFormat;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * This class represents Conventional Infantry (with BattleArmor as a subclass).
 * The lowest of the low, the ground pounders, the city rats, the PBI (Poor Bloody Infantry).
 *
 * Note that BattleArmor extends Infantry!
 *
 * @author Suvarov454@sourceforge.net (James A. Damour)
 */
public class Infantry extends Entity {
    private static final long serialVersionUID = -8706716079307721282L;

    // Infantry Specializations
    public static int BRIDGE_ENGINEERS  = 1;
    public static int DEMO_ENGINEERS    = 1 << 1;
    public static int FIRE_ENGINEERS    = 1 << 2;
    public static int MINE_ENGINEERS    = 1 << 3;
    public static int SENSOR_ENGINEERS  = 1 << 4;
    public static int TRENCH_ENGINEERS  = 1 << 5;
    public static int MARINES           = 1 << 6;
    public static int MOUNTAIN_TROOPS   = 1 << 7;
    public static int PARAMEDICS        = 1 << 8;
    public static int PARATROOPS        = 1 << 9;
    public static int TAG_TROOPS        = 1 << 10;
    public static int XCT               = 1 << 11;
    public static int SCUBA             = 1 << 12;
    public static int NUM_SPECIALIZATIONS = 13;
    public static int COMBAT_ENGINEERS = BRIDGE_ENGINEERS | DEMO_ENGINEERS | FIRE_ENGINEERS | MINE_ENGINEERS
            | SENSOR_ENGINEERS | TRENCH_ENGINEERS;

    protected int squadCount = 1;
    private int squadSize = 1;
    protected int originalTrooperCount;

    /** The number of troopers alive in this platoon at the beginning of the phase, before taking damage. */
    private int troopersShooting;

    /** The number of troopers alive in this platoon, including any damage sustained in the current phase. */
    private int activeTroopers;

    // Information on primary and secondary weapons. This must be kept separate from the equipment array
    // because they are not fired as separate weapons
    private InfantryWeapon primaryWeapon;
    private String primaryName;
    private InfantryWeapon secondaryWeapon;
    private String secondName;
    private int secondaryWeaponsPerSquad = 0;

    // Armor
    private double damageDivisor = 1.0;
    private boolean encumbering = false;
    private boolean spaceSuit = false;
    private boolean dest = false;
    private boolean sneak_camo = false;
    private boolean sneak_ir = false;
    private boolean sneak_ecm = false;

    /** The active specializations of this platoon. */
    private int infSpecs = 0;

    /**
     * For mechanized VTOL infantry, stores whether the platoon are microlite troops,
     * which need to enter a hex every turn to remain in flight.
     */
    private boolean isMicrolite = false;

    public static final int LOC_INFANTRY = 0;
    public static final int LOC_FIELD_GUNS = 1;

    // Infantry only have critical slots for field gun ammo
    private static final int[] NUM_OF_SLOTS = { 20, 20 };
    private static final String[] LOCATION_ABBRS = { "TPRS", "FGUN" };
    private static final String[] LOCATION_NAMES = { "Troopers", "Field Guns" };

    public int turnsLayingExplosives = -1;

    public static final int DUG_IN_NONE = 0;
    public static final int DUG_IN_WORKING = 1; // no protection, can't attack
    public static final int DUG_IN_COMPLETE = 2; // protected, restricted arc
    public static final int DUG_IN_FORTIFYING1 = 3; // no protection, can't attack
    public static final int DUG_IN_FORTIFYING2 = 4; // no protection, can't attack
    public static final int DUG_IN_FORTIFYING3 = 5; // no protection, can't attack
    private int dugIn = DUG_IN_NONE;

    private boolean isTakingCover = false;
    private boolean canCallSupport = true;
    private boolean isCallingSupport = false;
    private InfantryMount mount = null;

    /** The maximum number of troopers in an infantry platoon. */
    public static final int INF_PLT_MAX_MEN = 30;

    // Anti-Mek attacks
    public static final String LEG_ATTACK = "LegAttack";
    public static final String SWARM_MEK = "SwarmMek";
    public static final String SWARM_WEAPON_MEK = "SwarmWeaponMek";
    public static final String STOP_SWARM = "StopSwarm";

    public static final int ANTI_MECH_SKILL_NO_GEAR = 8;

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
        return 2;
    }

    /** Generate a new, blank, infantry platoon. Hopefully, we'll be loaded from somewhere. */
    public Infantry() {
        super();
        // Create a "dead" leg rifle platoon.
        originalTrooperCount = 0;
        troopersShooting = 0;
        activeTroopers = 0;
        setMovementMode(EntityMovementMode.INF_LEG);
        setOriginalWalkMP(1);
    }

    @Override
    public int getUnitType() {
        return UnitType.INFANTRY;
    }

    @Override
    public CrewType defaultCrewType() {
        return CrewType.INFANTRY_CREW;
    }

    public TechAdvancement getMotiveTechAdvancement() {
        return getMotiveTechAdvancement(mount == null ? getMovementMode() : EntityMovementMode.NONE);
    }

    /**
     * Generates the {@link TechAdvancement} for the unit's motive type. A value of EntityMovementMode.NONE indicates
     * Beast-mounted infantry.
     *
     * @param movementMode An infantry movement mode.
     * @return The Tech Advancement data for the movement mode.
     */
    public static TechAdvancement getMotiveTechAdvancement(EntityMovementMode movementMode) {
        TechAdvancement techAdvancement = new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
        switch (movementMode) {
            case INF_MOTORIZED:
                techAdvancement.setTechRating(RATING_B)
                    .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                    .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case INF_JUMP:
                techAdvancement.setAdvancement(DATE_ES, DATE_ES, DATE_ES)
                    .setTechRating(RATING_D).setAvailability(RATING_B, RATING_B, RATING_B, RATING_B)
                    .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case INF_UMU:
                techAdvancement.setAdvancement(DATE_PS, DATE_PS).setTechRating(RATING_B)
                    .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                    .setStaticTechLevel(SimpleTechLevel.ADVANCED);
                break;
            case WHEELED:
                techAdvancement.setTechRating(RATING_A)
                    .setAvailability(RATING_A, RATING_B, RATING_A, RATING_A)
                    .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case TRACKED:
                techAdvancement.setTechRating(RATING_B)
                    .setAvailability(RATING_B, RATING_C, RATING_B, RATING_B)
                    .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case HOVER:
                techAdvancement.setTechRating(RATING_C)
                    .setAvailability(RATING_A, RATING_B, RATING_A, RATING_B)
                    .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            case VTOL:
                techAdvancement.setAdvancement(DATE_ES, DATE_ES).setTechRating(RATING_C)
                    .setAvailability(RATING_C, RATING_D, RATING_D, RATING_C)
                    .setStaticTechLevel(SimpleTechLevel.ADVANCED);
                break;
            case SUBMARINE:
                techAdvancement.setAdvancement(DATE_PS, DATE_PS).setTechRating(RATING_C)
                    .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                    .setStaticTechLevel(SimpleTechLevel.ADVANCED);
                break;
            case NONE:
                // Beast-mounted
                techAdvancement.setAdvancement(DATE_PS, DATE_PS).setTechRating(RATING_A)
                        .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                        .setStaticTechLevel(SimpleTechLevel.ADVANCED);
                break;
            case INF_LEG:
            default:
                techAdvancement.setTechRating(RATING_A)
                    .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                    .setStaticTechLevel(SimpleTechLevel.STANDARD);
                break;
            }
        return techAdvancement;
    }

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    public static TechAdvancement getCombatEngineerTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_C)
                .setAvailability(RATING_A, RATING_B, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getMarineTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_C)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getMountainTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_B)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getParatrooperTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_B)
                .setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getParamedicTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_PS, DATE_PS, DATE_PS).setTechRating(RATING_B)
                .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    public static TechAdvancement getTAGTroopsTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setISAdvancement(2585, 2600, DATE_NONE, 2535, 3037)
                .setClanAdvancement(2585, 2600)
                .setApproximate(true, false, false, false, false).setTechRating(RATING_E)
                .setPrototypeFactions(F_TH).setProductionFactions(F_TH).setReintroductionFactions(F_FS)
                .setAvailability(RATING_F, RATING_X, RATING_E, RATING_E)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        ctl.addComponent(getMotiveTechAdvancement());
        if (hasSpecialization(COMBAT_ENGINEERS)) {
            ctl.addComponent(Infantry.getCombatEngineerTA());
        }
        if (hasSpecialization(MARINES)) {
            ctl.addComponent(Infantry.getMarineTA());
        }
        if (hasSpecialization(MOUNTAIN_TROOPS)) {
            ctl.addComponent(Infantry.getMountainTA());
        }
        if (hasSpecialization(PARATROOPS)) {
            ctl.addComponent(Infantry.getParatrooperTA());
        }
        if (hasSpecialization(PARAMEDICS)) {
            ctl.addComponent(Infantry.getParamedicTA());
        }
        if (hasSpecialization(TAG_TROOPS)) {
            ctl.addComponent(Infantry.getTAGTroopsTA());
        }
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return !(hasActiveFieldArtillery() || getAlreadyTwisted());
    }

    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return true;
    }

    @Override
    public int clipSecondaryFacing(int dir) {
        return dir;
    }

    /** Creates a local platoon for Urban Guerrilla. */
    public void createLocalSupport() {
        if (Compute.isInUrbanEnvironment(game, getPosition())) {
            setIsCallingSupport(true);
            canCallSupport = false;
        }
    }

    public void setIsCallingSupport(boolean b) {
        isCallingSupport = b;
    }

    public boolean getIsCallingSupport() {
        return isCallingSupport;
    }

    @Override
    public int getWalkMP(MPCalculationSetting mpCalculationSetting) {
        int mp = getOriginalWalkMP();
        // Beast mounted infantry depends entirely on the creature
        if (mount == null) {
            // Encumbering armor (TacOps, pg. 318)
            if (encumbering) {
                mp = Math.max(mp - 1, 1);
            }
            if ((getSecondaryWeaponsPerSquad() > 1)
                    && !hasAbility(OptionsConstants.MD_TSM_IMPLANT)
                    && !hasAbility(OptionsConstants.MD_DERMAL_ARMOR)
                    && (null != secondaryWeapon) && secondaryWeapon.hasFlag(WeaponType.F_INF_SUPPORT)
                    && !getMovementMode().isTracked()
                    && !getMovementMode().isJumpInfantry()) {
                mp = Math.max(mp - 1, 0);
            }
            //  PL-MASC IntOps p.84
            if ((null != getCrew())
                    && hasAbility(OptionsConstants.MD_PL_MASC)
                    && getMovementMode().isLegInfantry()
                    && isConventionalInfantry()) {
                mp += 1;
            }

            if ((null != getCrew()) && hasAbility(OptionsConstants.INFANTRY_FOOT_CAV)
                    && getMovementMode().isJumpOrLegInfantry()) {
                mp += 1;
            }

            if (hasActiveFieldArtillery()) {
                mp = Math.min(mp, 1);
            }
        }

        if (!mpCalculationSetting.ignoreWeather && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            int weatherMod = conditions.getMovementMods(this);
            mp = Math.max(mp + weatherMod, 0);

            if (conditions.getWeather().isGustingRain()
                    && getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_RAIN)) {
                if ((mp !=0) || getMovementMode().isMotorizedInfantry()) {
                    mp += 1;
                }
            }

            if (getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_SNOW)) {
                if (conditions.getWeather().isSnowFlurriesOrIceStorm()
                        && (getOriginalWalkMP() != 0)) {
                    mp += 1;
                }
            }

            if (getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)
                    && conditions.getWeather().isClear()) {
                if (conditions.getWind().isModerateGale()) {
                    mp += 1;
                }

                if (conditions.getWind().isStrongGale()
                        && ((mp != 0) || getMovementMode().isMotorizedInfantry())) {
                    mp += 1;
                }
            }
        }

        if (!mpCalculationSetting.ignoreGravity) {
            mp = applyGravityEffectsOnMP(mp);
        }

        return mp;
    }

    @Override
    public int getRunMP(MPCalculationSetting mpCalculationSetting) {
        int walkMP = getWalkMP(mpCalculationSetting);
        if (!mpCalculationSetting.ignoreOptionalRules && (game != null)
                && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_FAST_INFANTRY_MOVE)) {
            return (walkMP > 0) ? walkMP + 1 : walkMP + 2;
        } else {
            return walkMP;
        }
    }

    @Override
    public int getJumpMP(MPCalculationSetting mpCalculationSetting) {
        int mp = hasUMU() ? 0 : getOriginalJumpMP();
        if (mount == null) {
            if ((getSecondaryWeaponsPerSquad() > 1)
                    && !hasAbility(OptionsConstants.MD_TSM_IMPLANT)
                    && !hasAbility(OptionsConstants.MD_DERMAL_ARMOR)
                    && !getMovementMode().isSubmarine()
                    && (null != secondaryWeapon) && secondaryWeapon.hasFlag(WeaponType.F_INF_SUPPORT)) {
                mp = Math.max(mp - 1, 0);
            } else if (movementMode.isVTOL() && getSecondaryWeaponsPerSquad() > 0) {
                mp = Math.max(mp - 1, 0);
            }
        }

        if (!mpCalculationSetting.ignoreGravity) {
            mp = applyGravityEffectsOnMP(mp);
        }

        if (!mpCalculationSetting.ignoreWeather && (null != game)) {
            PlanetaryConditions conditions = game.getPlanetaryConditions();
            if (conditions.getWind().isStrongerThan(Wind.MOD_GALE)) {
                return 0;
            } else if (conditions.getWind().isModerateGale()
                    && !getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_WIND)) {
                mp--;
            }

            if (getCrew().getOptions().stringOption(OptionsConstants.MISC_ENV_SPECIALIST).equals(Crew.ENVSPC_SNOW)) {
                if (conditions.getWeather().isSnowFlurries()
                        && conditions.getWeather().isIceStorm()) {
                    mp += 1;
                }
            }
        }
        return Math.max(mp, 0);
    }

    @Override
    public boolean hasUMU() {
        return getMovementMode().isUMUInfantry() || getMovementMode().isSubmarine();
    }

    @Override
    public int getActiveUMUCount() {
        return getAllUMUCount();
    }

    @Override
    public int getAllUMUCount() {
        return hasUMU() ? jumpMP : 0;
    }

    @Override
    public int height() {
        return mount == null ? 0 : mount.getSize().height;
    }

    @Override
    public boolean antiTSMVulnerable() {
        if (!hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            return false;
        }
        EquipmentType armorKit = getArmorKit();
        return (armorKit == null)
                || !armorKit.hasSubType(MiscType.S_SPACE_SUIT | MiscType.S_XCT_VACUUM | MiscType.S_TOXIC_ATMO);
    }

    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        // Coords off the board aren't legal
        if (!game.getBoard().contains(c)) {
            return true;
        }
        Hex hex = game.getBoard().getHex(c);
        // Taharqa: waiting to hear back from Welshie but I am going to assume that units pulling artillery
        // should be treated as wheeled rather than motorized because otherwise mechanized units face fewer
        // terrain restrictions when pulling field artillery

        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }
        if (hex.containsTerrain(Terrains.MAGMA)) {
            return true;
        }
        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        // Additional restrictions for hidden units
        if (isHidden()) {
            // Can't deploy in paved hexes
            if ((hex.containsTerrain(Terrains.PAVEMENT) || hex.containsTerrain(Terrains.ROAD))
                    && (!hex.containsTerrain(Terrains.BUILDING) && !hex.containsTerrain(Terrains.RUBBLE))) {
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

        if (hex.containsTerrain(Terrains.MAGMA)) {
            return true;
        }

        if (getMovementMode().isWheeled()) {
            if (hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.ROUGH)
                    || hex.containsTerrain(Terrains.RUBBLE)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || (hex.terrainLevel(Terrains.SNOW) > 1)
                    || (hex.terrainLevel(Terrains.GEYSER) == 2)) {
                return true;
            }
        }

        if (getMovementMode().isTracked()) {
            if ((hex.terrainLevel(Terrains.WOODS) > 1)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || (hex.terrainLevel(Terrains.ROUGH) > 1)
                    || (hex.terrainLevel(Terrains.RUBBLE) > 5)) {
                return true;
            }
        }

        if (getMovementMode().isHover()) {
            if (hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || (hex.terrainLevel(Terrains.ROUGH) > 1)
                    || (hex.terrainLevel(Terrains.RUBBLE) > 5)) {
                return true;
            }
        }

        if ((hex.terrainLevel(Terrains.WATER) <= 0) && getMovementMode().isSubmarine()) {
            return (mount == null) || (mount.getSecondaryGroundMP() == 0);
        }

        if ((hex.terrainLevel(Terrains.WATER) > 0) && !hex.containsTerrain(Terrains.ICE)) {
            if (mount == null) {
                return !getMovementMode().isHover() && !getMovementMode().isUMUInfantry()
                        && !getMovementMode().isSubmarine() && !getMovementMode().isVTOL();
            } else {
                return hex.terrainLevel(Terrains.WATER) > mount.getMaxWaterDepth();
            }
        }
        return false;
    }

    @Override
    public boolean isElevationValid(int assumedElevation, Hex hex) {
        if (mount != null) {
            // Mounted infantry can enter water hexes if the mount allows it
            if (hex.containsTerrain(Terrains.WATER) && (hex.terrainLevel(Terrains.WATER) <= mount.getMaxWaterDepth())) {
                return true;
            }
            // Aquatic mounts may be able to move onto land
            if (!hex.containsTerrain(Terrains.WATER) && movementMode.isSubmarine()) {
                return mount.getSecondaryGroundMP() > 0;
            }
        }
        return super.isElevationValid(assumedElevation, hex);
    }

        @Override
    public String getMovementString(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_NONE:
                return "None";
            case MOVE_WALK:
            case MOVE_RUN:
                switch (getMovementMode()) {
                    case INF_LEG:
                        return mount == null ? "Walked" : "Rode";
                    case INF_MOTORIZED:
                        return "Biked";
                    case HOVER:
                    case TRACKED:
                    case WHEELED:
                        return "Drove";
                    case INF_JUMP:
                    default:
                        return "Unknown!";
                }
            case MOVE_VTOL_WALK:
            case MOVE_VTOL_RUN:
                return "Flew";
            case MOVE_JUMP:
                return "Jumped";
            default:
                return "Unknown!";
        }
    }

    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_NONE:
                return "N";
            case MOVE_WALK:
                return "W";
            case MOVE_RUN:
                switch (getMovementMode()) {
                    case INF_LEG:
                        return "R";
                    case INF_MOTORIZED:
                        return "B";
                    case HOVER:
                    case TRACKED:
                    case WHEELED:
                        return "D";
                    default:
                        return "?";
                }
            case MOVE_JUMP:
                return "J";
            default:
                return "?";
        }
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode, int cover) {
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_INFANTRY);
    }

    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(Entity.LOC_DESTROYED);
    }

    @Override
    public int getDependentLocation(int loc) {
        return Entity.LOC_NONE;
    }

    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    @Override
    public int getInternal(int loc) {
        if (!isConventionalInfantry()) {
            return super.getInternal(loc);
        }
        if (loc != LOC_INFANTRY) {
            return 0;
        }
        return (activeTroopers > 0 ? activeTroopers : IArmorState.ARMOR_DESTROYED);
    }

    @Override
    public int getOInternal(int loc) {
        return isConventionalInfantry() ? originalTrooperCount : super.getOInternal(loc);
    }

    @Override
    public void setInternal(int val, int loc) {
        super.setInternal(val, loc);
        if (loc == LOC_INFANTRY) {
            activeTroopers = Math.max(val, 0);
            damageFieldWeapons();
        }
    }

    /**
     * Returns the percent of the troopers remaining in the platoon.
     */
    @Override
    public double getInternalRemainingPercent() {
        if (isConventionalInfantry()) {
            return (double) Math.max(activeTroopers, 0) / originalTrooperCount;
        } else {
            return super.getInternalRemainingPercent();
        }
    }

    /**
     * Initializes the number of troopers in the platoon. Sets the original and
     * starting point of the platoon to the same number.
     */
    @Override
    public void initializeInternal(int val, int loc) {
        if (loc == LOC_INFANTRY) {
            originalTrooperCount = val;
            troopersShooting = val;
        }
        super.initializeInternal(val, loc);
    }

    @Override
    public void autoSetInternal() {
        initializeInternal(squadSize * squadCount, LOC_INFANTRY);
    }

    @Override
    public int getWeaponArc(int wn) {
        Mounted weapon = getEquipment(wn);
        // Infantry can fire all around themselves. But field guns are set up to a vehicular turret facing
        if (isFieldWeapon(weapon)) {
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                return Compute.ARC_TURRET;
            }
            return Compute.ARC_FORWARD;
        }
        // According to TacOps rules, dug-in units no longer have to declare a facing
        return Compute.ARC_360;
    }

    @Override
    public boolean isSecondaryArcWeapon(int wn) {
        return isFieldWeapon((getEquipment(wn))) && !hasActiveFieldArtillery();
    }

    @Override
    public int getHeatCapacity(boolean radicalHeatSinks) {
        return DOES_NOT_TRACK_HEAT;
    }

    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    public boolean hasHittableCriticals(int loc) {
        return false;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

        Report r = new Report(7025, Report.PUBLIC);
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7041, Report.PUBLIC);
        r.add(getCrew().getGunnery());
        vDesc.addElement(r);

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
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        return prd;
    }

    @Override
    public int getMaxElevationChange() {
        return hasSpecialization(MOUNTAIN_TROOPS) ? 3 : 1;
    }

    @Override
    public void applyDamage() {
        super.applyDamage();
        troopersShooting = activeTroopers;
    }

    /**
     * Marks field guns and artillery as hit according to their crew requirement when losing troopers. Having
     * them destroyed requires a later call of {@link #applyDamage()}. This method does not restore FG/FA when
     * damage is removed from the unit.
     * Only affects Conventional Infantry, not BattleArmor (can be called safely on BA).
     */
    protected void damageFieldWeapons() {
        int totalCrewNeeded = 0;
        for (Mounted weapon : activeFieldWeapons()) {
            totalCrewNeeded += requiredCrewForFieldWeapon((WeaponType) weapon.getType());
            if (totalCrewNeeded > activeTroopers) {
                weapon.setHit(true);
            }
        }
    }

    /**
     * Destroys and restores field guns and artillery according to their crew requirements. This method is intended
     * for any place where damage can be assigned and removed without cost (lobby). Only affects Conventional Infantry,
     * not BattleArmor (can be called safely on BA).
     */
    public void damageOrRestoreFieldWeapons() {
        int totalCrewNeeded = 0;
        for (Mounted weapon : originalFieldWeapons()) {
            totalCrewNeeded += requiredCrewForFieldWeapon((WeaponType) weapon.getType());
            weapon.setHit(totalCrewNeeded > activeTroopers);
            weapon.setDestroyed(totalCrewNeeded > activeTroopers);
        }
    }

    /**
     * @return The crew required to operate the given field gun or field artillery weapon, TO:AUE p.123.
     * The rules are silent on rounding for the weight of artillery, therefore adopting that of field guns.
     */
    public int requiredCrewForFieldWeapon(WeaponType weaponType) {
        int roundedWeight = (int) Math.ceil(weaponType.getTonnage(this));
        return weaponType.hasFlag(WeaponType.F_ARTILLERY) ? roundedWeight : Math.max(2, roundedWeight);
    }

    /** @return Active field guns and artillery of this infantry (= not including destroyed ones). Empty on BA. */
    public List<Mounted> activeFieldWeapons() {
        return originalFieldWeapons().stream().filter(e -> !e.isDestroyed()).collect(toList());
    }

    /** @return All field guns and artillery of this infantry including destroyed ones. Empty on BA. */
    public List<Mounted> originalFieldWeapons() {
        return getEquipment().stream().filter(this::isFieldWeapon).collect(toList());
    }

    /** @return True when the given Mounted is a Field Gun or Artillery. On BA, always returns false. */
    protected boolean isFieldWeapon(Mounted equipment) {
        return (equipment.getType() instanceof WeaponType) && (equipment.getLocation() == LOC_FIELD_GUNS);
    }

    /** @return The number of troopers in the platoon before damage of the current phase is applied. */
    public int getShootingStrength() {
        return troopersShooting;
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
    public PilotingRollData checkBogDown(MoveStep step, EntityMovementType moveType, Hex curHex, Coords lastPos,
                                         Coords curPos, int lastElev, boolean isPavementStep) {
        return checkBogDown(step, curHex, lastPos, curPos, isPavementStep);
    }

    public PilotingRollData checkBogDown(MoveStep step, Hex curHex,
            Coords lastPos, Coords curPos, boolean isPavementStep) {
        PilotingRollData roll = new PilotingRollData(getId(), 4, "entering boggy terrain");
        int bgMod = curHex.getBogDownModifier(getMovementMode(), false);
        final boolean onBridge = (curHex.terrainLevel(Terrains.BRIDGE) > 0)
                && (getElevation() == curHex.terrainLevel(Terrains.BRIDGE_ELEV));
        if (!lastPos.equals(curPos) && (bgMod != TargetRoll.AUTOMATIC_SUCCESS)
                && (step.getMovementType(false) != EntityMovementType.MOVE_JUMP)
                && (getMovementMode() != EntityMovementMode.HOVER)
                && (getMovementMode() != EntityMovementMode.VTOL)
                && (getMovementMode() != EntityMovementMode.WIGE)
                && (step.getElevation() == 0) && !isPavementStep && !onBridge) {
            roll.append(new PilotingRollData(getId(), bgMod, "avoid bogging down"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Not entering bog-down terrain, or jumping/hovering over such terrain");
        }
        return roll;
    }

    public boolean getCanCallSupport() {
        return canCallSupport;
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return InfantryCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        double priceMultiplier = 1.0;

        // Anti-Mek Trained Multiplier
        if (hasAntiMekGear()) {
            priceMultiplier *= 5.0;
        }

        // Motive type costs
        switch (getMovementMode()) {
            case INF_UMU:
                priceMultiplier *= getAllUMUCount() > 1 ? 2.5 : 2;
                break;
            case INF_LEG:
                priceMultiplier *= 1.0;
                break;
            case INF_MOTORIZED:
                priceMultiplier *= 1.6;
                break;
            case INF_JUMP:
                priceMultiplier *= 2.6;
                break;
            case HOVER:
            case WHEELED:
            case TRACKED:
            case SUBMARINE: // No cost given in TacOps, using basic mechanized cost for now
                priceMultiplier *= 3.2;
                break;
            case VTOL:
                priceMultiplier *= hasMicrolite() ? 4 : 4.5;
                break;
            default:
                break;
        }

        // Specialization costs
        if (hasSpecialization(COMBAT_ENGINEERS)) {
            priceMultiplier *= 5;
        }
        if (hasSpecialization(MARINES)) {
            priceMultiplier *= 3;
        }
        if (hasSpecialization(MOUNTAIN_TROOPS)) {
            priceMultiplier *= 2;
        }
        if (hasSpecialization(PARATROOPS)) {
            priceMultiplier *= 3;
        }
        if (hasSpecialization(XCT)) {
            priceMultiplier *= 5;
        }
        // TODO : paramedics cost an addition x0.375 per paramedic
        return priceMultiplier;
    }

    /**
     * The alternate cost here is used by MekHQ to create costs that reflect just the cost of
     * equipment. The motive costs here are based on the costs associated with an auto-rifle
     * platoon.
     */
    @Override
    public double getAlternateCost() {
        double cost = 0;
        if (null != primaryWeapon) {
            cost += primaryWeapon.getCost(this, false, -1) * (squadSize - secondaryWeaponsPerSquad);
        }
        if (null != secondaryWeapon) {
            cost += secondaryWeapon.getCost(this, false, -1) * secondaryWeaponsPerSquad;
        }
        cost = cost / squadSize;

        EquipmentType armor = getArmorKit();
        if (armor != null) {
            cost += armor.getCost(this, false, LOC_INFANTRY);
        }

        // Add in motive type costs
        switch (getMovementMode()) {
            case INF_UMU:
                cost += 17888;
                if (getAllUMUCount() > 1) {
                    cost += 17888 * 0.5;
                }
                break;
            case INF_MOTORIZED:
                cost += 17888 * 0.6;
                break;
            case INF_JUMP:
                cost += 17888 * 1.6;
                break;
            case HOVER:
            case WHEELED:
            case TRACKED:
            case SUBMARINE: // FIXME: there is no cost shown for mech. scuba in tac ops
                cost += 17888 * 2.2;
                break;
            case VTOL:
                cost += 17888 * (hasMicrolite() ? 3 : 3.5);
                break;
            default:
                break;
        }
        cost *= originalTrooperCount;
        // add in field gun costs
        cost += originalFieldWeapons().stream().mapToDouble(m -> m.getType().getCost(this, false, -1)).sum();
        return cost;
    }

    @Override
    public boolean doomedInExtremeTemp() {
        // If there is no game object, count any temperature protection.
        if (getArmorKit() != null) {
            if (getArmorKit().hasSubType(MiscType.S_XCT_VACUUM)) {
                return false;
            } else if (getArmorKit().hasSubType(MiscType.S_COLD_WEATHER)
                    && ((game == null) || game.getPlanetaryConditions().getTemperature() < -30)) {
                return false;
            } else if (getArmorKit().hasSubType(MiscType.S_HOT_WEATHER)
                    && ((game == null) || game.getPlanetaryConditions().getTemperature() > 50)) {
                return false;
            } else {
                return true;
            }
        }
        if (hasSpaceSuit() || isMechanized()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean doomedInVacuum() {
        return getMovementMode().isVTOL() || !hasSpaceSuit();
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
    public boolean canAssaultDrop() {
        return game.getOptions().booleanOption(OptionsConstants.ADVANCED_PARATROOPERS);
    }

    @Override
    public boolean isEligibleFor(GamePhase phase) {
        if ((turnsLayingExplosives > 0) && !phase.isPhysical()) {
            return false;
        } else if ((dugIn != DUG_IN_COMPLETE) && (dugIn != DUG_IN_NONE)) {
            return false;
        } else {
            return super.isEligibleFor(phase);
        }
    }

    @Override
    public boolean isEligibleForFiring() {
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_FAST_INFANTRY_MOVE)
                && (moved == EntityMovementType.MOVE_RUN)) {
            return false;
        }
        return super.isEligibleForFiring();
    }

    @Override
    public void newRound(int roundNumber) {
        if (turnsLayingExplosives >= 0) {
            turnsLayingExplosives++;
            if (!Compute.isInBuilding(game, this)) {
                turnsLayingExplosives = -1; // give up if no longer in a building
            }
        }

        if ((dugIn != DUG_IN_COMPLETE) && (dugIn != DUG_IN_NONE)) {
            dugIn++;
            if (dugIn > DUG_IN_FORTIFYING3) {
                dugIn = DUG_IN_NONE;
            }
        }

        setTakingCover(false);
        super.newRound(roundNumber);
    }

    public void setDugIn(int i) {
        dugIn = i;
    }

    public int getDugIn() {
        return dugIn;
    }

    @Override
    public boolean isNuclearHardened() {
        return false;
    }

    /**
     * This function is called when loading a unit into a transport. This is
     * overridden to ensure infantry are no longer considered dug in when they
     * are being transported.
     *
     * @param transportID The entity ID of the transporter
     */
    @Override
    public void setTransportId(int transportID) {
        super.setTransportId(transportID);
        setDugIn(DUG_IN_NONE);
    }

    /**
     * Returns the anti-mek skill for this unit.  Since Infantry don't have
     * piloting the crew's piloting skill is treated as the anti-mek skill.
     * This is largely just a convenience method for setting the Crew's piloting
     * skill.
     * @return The Anti-Mek skill
     */
    @SuppressWarnings("unused") // used in MHQ
    public int getAntiMekSkill() {
        return (getCrew() == null) ? (hasAntiMekGear() ? 5 : ANTI_MECH_SKILL_NO_GEAR) : getCrew().getPiloting();
    }

    /**
     * Returns true if this unit carries anti-mek gear. Only with this gear can it improve
     * its anti-mek skill below 8.
     *
     * @return True when this infantry carries anti-mek gear
     */
    public boolean hasAntiMekGear() {
        return hasWorkingMisc(EquipmentTypeLookup.ANTI_MEK_GEAR);
    }

    public boolean isMechanized() {
        return (getMovementMode().isTrackedWheeledOrHover()) || (getMovementMode().isSubmarine())
                || (getMovementMode().isVTOL());
    }

    public boolean isXCT() {
        return hasSpecialization(XCT);
    }

    @Override
    public int getTotalCommGearTons() {
        return 0;
    }

    public EquipmentType getArmorKit() {
        return getMisc().stream()
                .filter(m -> m.getType().hasFlag(MiscType.F_ARMOR_KIT))
                .findFirst().map(Mounted::getType).orElse(null);
    }

    public void setArmorKit(EquipmentType armorKit) {
        removeArmorKits();
        if ((armorKit != null) && armorKit.hasFlag(MiscType.F_ARMOR_KIT)) {
            try {
                addEquipment(armorKit, LOC_INFANTRY);
            } catch (LocationFullException ex) {
                LogManager.getLogger().error("", ex);
            }
            encumbering = (armorKit.getSubType() & MiscType.S_ENCUMBERING) != 0;
            spaceSuit = (armorKit.getSubType() & MiscType.S_SPACE_SUIT) != 0;
            dest = (armorKit.getSubType() & MiscType.S_DEST) != 0;
            sneak_camo = (armorKit.getSubType() & MiscType.S_SNEAK_CAMO) != 0;
            sneak_ir = (armorKit.getSubType() & MiscType.S_SNEAK_IR) != 0;
            sneak_ecm = (armorKit.getSubType() & MiscType.S_SNEAK_ECM) != 0;
        }
        calcDamageDivisor();
    }

    private void removeArmorKits() {
        List<MiscMounted> toRemove = getEquipment().stream()
                .filter(m -> m instanceof MiscMounted)
                .map(m -> (MiscMounted) m)
                .filter(m -> m.getType().hasFlag(MiscType.F_ARMOR_KIT))
                .collect(toList());

        getEquipment().removeAll(toRemove);
        getMisc().removeAll(toRemove);

        for (CriticalSlot slot : getCriticalSlots(Infantry.LOC_INFANTRY)) {
            if ((slot != null) && (slot.getMount() instanceof MiscMounted)
                    && toRemove.contains((MiscMounted) slot.getMount())) {
                removeCriticals(Infantry.LOC_INFANTRY, slot);
            }
        }
    }

    public double calcDamageDivisor() {
        double divisor = 1.0;
        EquipmentType armorKit = getArmorKit();
        if (armorKit != null) {
            divisor = ((MiscType) armorKit).getDamageDivisor();
        }
        // TSM implant reduces divisor to 0.5 if no other armor is worn
        if ((divisor == 1.0) && hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            divisor = 0.5;
        }
        // Dermal armor adds one to the divisor, cumulative with armor kit and TSM implant
        if (hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
            divisor += 1.0;
        }
        if (mount != null) {
            divisor *= mount.getDamageDivisor();
        }
        return divisor;
    }

    public double getArmorDamageDivisor() {
        return damageDivisor;
    }

    public void setArmorDamageDivisor(double d) {
        damageDivisor = d;
    }

    public boolean isArmorEncumbering() {
        return encumbering;
    }

    public void setArmorEncumbering(boolean b) {
        encumbering = b;
    }

    public void setCanCallSupport(boolean b) {
        canCallSupport =b;
    }

    public boolean hasSpaceSuit() {
        return spaceSuit;
    }

    public void setSpaceSuit(boolean b) {
        spaceSuit = b;
    }

    public boolean hasDEST() {
        return dest;
    }

    public void setDEST(boolean b) {
        dest = b;
    }

    public boolean hasSpecialization() {
        return infSpecs != 0;
    }

    public boolean hasSpecialization(int spec) {
        return (infSpecs & spec) > 0;
    }

    public int getSpecializations() {
        return infSpecs;
    }

    public void setSpecializations(int spec) {
        // Equipment for Trench/Fieldworks Engineers
        if ((spec & TRENCH_ENGINEERS) > 0 && (infSpecs & TRENCH_ENGINEERS) == 0) {
            // Add vibro shovels
            try {
                EquipmentType shovels = EquipmentType.get(EquipmentTypeLookup.VIBRO_SHOVEL);
                addEquipment(shovels, Infantry.LOC_INFANTRY);
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        } else if ((spec & TRENCH_ENGINEERS) == 0 && (infSpecs & TRENCH_ENGINEERS) > 0) {
            // Need to remove vibro shovels
            List<Mounted> eqToRemove = new ArrayList<>();
            for (Mounted eq : getEquipment()) {
                if (eq.getType().hasFlag(MiscType.F_TOOLS) && eq.getType().hasSubType(MiscType.S_VIBROSHOVEL)) {
                    eqToRemove.add(eq);
                }
            }
            getEquipment().removeAll(eqToRemove);
            getMisc().removeAll(eqToRemove);
        }
        // Equipment for Demolition Engineers
        if ((spec & DEMO_ENGINEERS) > 0 && (infSpecs & DEMO_ENGINEERS) == 0) {
            // Add demolition charge
            try {
                EquipmentType charge = EquipmentType.get(EquipmentTypeLookup.DEMOLITION_CHARGE);
                addEquipment(charge, Infantry.LOC_INFANTRY);
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        } else if ((spec & DEMO_ENGINEERS) == 0 && (infSpecs & DEMO_ENGINEERS) > 0) {
            // Need to remove vibro shovels
            List<Mounted> eqToRemove = new ArrayList<>();
            for (Mounted eq : getEquipment()) {
                if (eq.getType().hasFlag(MiscType.F_TOOLS) && eq.getType().hasSubType(MiscType.S_DEMOLITION_CHARGE)) {
                    eqToRemove.add(eq);
                }
            }
            getEquipment().removeAll(eqToRemove);
            getMisc().removeAll(eqToRemove);
        }
        infSpecs = spec;
    }

    public static String getSpecializationName(int spec) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < NUM_SPECIALIZATIONS; i++) {
            int currSpec = 1 << i;
            if ((spec & currSpec) < 1) {
                continue;
            }
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(Messages.getString("Infantry.specialization" + i));
        }
        return name.toString();
    }

    public static String getSpecializationTooltip(int spec) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < NUM_SPECIALIZATIONS; i++) {
            int currSpec = 1 << i;
            if ((spec & currSpec) < 1) {
                continue;
            }
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(Messages.getString("Infantry.specializationTip" + i));
        }
        return name.toString();
    }

    public boolean hasSneakCamo() {
        return sneak_camo;
    }

    public void setSneakCamo(boolean b) {
        sneak_camo = b;
    }

    public boolean hasSneakIR() {
        return sneak_ir;
    }

    public void setSneakIR(boolean b) {
        sneak_ir = b;
    }

    public boolean hasSneakECM() {
        return sneak_ecm;
    }

    public void setSneakECM(boolean b) {
        sneak_ecm = b;
    }

    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // Note: infantry are immune to stealth, but not camoflage or mimetic armor
        if ((sneak_ir || dest) && !(ae instanceof Infantry)) {
            switch (range) {
                case RangeType.RANGE_MINIMUM:
                case RangeType.RANGE_SHORT:
                case RangeType.RANGE_MEDIUM:
                    result = new TargetRoll(+1, "Sneak, IR/DEST suit");
                    break;
                case RangeType.RANGE_LONG:
                case RangeType.RANGE_EXTREME:
                case RangeType.RANGE_LOS:
                    result = new TargetRoll(+2, "Sneak, IR/DEST suit");
                    break;
                case RangeType.RANGE_OUT:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown range constant: " + range);
            }
        }

        // Simple camo modifier is on top of the movement modifier
        // This can also be in addition to IR/DEST stealth mods!
        if (sneak_camo && (delta_distance < 3)) {
            int mod = Math.max(3 - delta_distance, 0);
            if (result == null) {
                result = new TargetRoll(mod, "Sneak, Camo");
            } else {
                result.append(new TargetRoll(mod, "Sneak, Camo"));
            }
        }

        if (dest && (delta_distance == 0)) {
            if (result == null) {
                result = new TargetRoll(1, "DEST suit");
            } else {
                result.append(new TargetRoll(1, "DEST Suit"));
            }
        }

        if (result == null) {
            result = new TargetRoll(0, "no sneak mods");
        }

        return result;
    }

    /** @return True if this infantry has any type of stealth system. */
    public boolean isStealthy() {
       return dest || sneak_camo || sneak_ir || sneak_ecm;
    }

    public boolean hasMicrolite() {
        return isMicrolite;
    }

    public void setMicrolite(boolean microlite) {
        this.isMicrolite = microlite;
    }

    public void setMount(InfantryMount mount) {
        this.mount = mount;
        if (mount != null) {
            setMovementMode(mount.getMovementMode());
            if (mount.getMovementMode().isLegInfantry()) {
                setOriginalWalkMP(mount.getMP());
            } else {
                setOriginalWalkMP(mount.getSecondaryGroundMP());
                setOriginalJumpMP(mount.getMP());
            }
            setArmorDamageDivisor(mount.getDamageDivisor());
        }
        calcDamageDivisor();
    }

    public @Nullable InfantryMount getMount() {
        return mount;
    }

    /**
     * Used to check for standard or motorized SCUBA infantry, which have a maximum depth of 2.
     * @return true if this is a conventional infantry unit with non-mechanized SCUBA specialization
     */
    public boolean isNonMechSCUBA() {
        return isConventionalInfantry() && getMovementMode().isUMUInfantry();
    }

    public void setPrimaryWeapon(InfantryWeapon w) {
        primaryWeapon = w;
        primaryName = w.getName();
    }

    public InfantryWeapon getPrimaryWeapon() {
        return primaryWeapon;
    }

    public void setSecondaryWeapon(InfantryWeapon w) {
        secondaryWeapon = w;
        if (null == w) {
            secondName = null;
        } else {
            secondName = w.getName();
        }
    }

    public InfantryWeapon getSecondaryWeapon() {
        return secondaryWeapon;
    }

    public void setSquadSize(int size) {
        squadSize = size;
    }

    public int getSquadSize() {
        return squadSize;
    }

    public void setSquadCount(int n) {
        squadCount = n;
    }

    public int getSquadCount() {
        return squadCount;
    }

    public void setSecondaryWeaponsPerSquad(int n) {
        secondaryWeaponsPerSquad = n;
    }

    public int getSecondaryWeaponsPerSquad() {
        return secondaryWeaponsPerSquad;
    }

    public double getDamagePerTrooper() {
        if (null == primaryWeapon) {
            return 0;
        }

        // per 09/2021 errata, primary infantry weapon damage caps out at 0.6
        double adjustedDamage = Math.min(MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP, primaryWeapon.getInfantryDamage());
        double damage = adjustedDamage * (squadSize - secondaryWeaponsPerSquad);
        if (null != secondaryWeapon) {
            damage += secondaryWeapon.getInfantryDamage() * secondaryWeaponsPerSquad;
        }
        return damage / squadSize;
    }

    public boolean primaryWeaponDamageCapped() {
        return getPrimaryWeaponDamage() > MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP;
    }

    public double getPrimaryWeaponDamage() {
        return (primaryWeapon != null) ? primaryWeapon.getInfantryDamage() : 0;
    }

    public boolean isSquad() {
        return squadCount == 1;
    }

    @Override
    public boolean isEligibleForPavementBonus() {
        if ((game != null)
                && game.getOptions().booleanOption(OptionsConstants.ADVANCED_TACOPS_INF_PAVE_BONUS)) {
            return movementMode == EntityMovementMode.TRACKED || movementMode == EntityMovementMode.WHEELED || movementMode == EntityMovementMode.INF_MOTORIZED || movementMode == EntityMovementMode.HOVER;
        } else {
            return false;
        }
    }

    @Override
    public void setMovementMode(EntityMovementMode movementMode) {
        super.setMovementMode(movementMode);
        //movement mode will determine base mp
        if (isConventionalInfantry()) {
            setOriginalJumpMP(0);
            switch (getMovementMode()) {
                case INF_MOTORIZED:
                case TRACKED:
                    setOriginalWalkMP(3);
                    break;
                case HOVER:
                    setOriginalWalkMP(5);
                    break;
                case WHEELED:
                    setOriginalWalkMP(4);
                    break;
                case SUBMARINE:
                    setOriginalJumpMP(3);
                    setOriginalWalkMP(0);
                    setSpecializations(getSpecializations() | SCUBA);
                    break;
                case VTOL:
                    if (hasMicrolite()) {
                        setOriginalJumpMP(6);
                    } else {
                        setOriginalJumpMP(5);
                    }
                    setOriginalWalkMP(1);
                    break;
                case INF_UMU:
                    setOriginalJumpMP(1);
                    setOriginalWalkMP(1);
                    setSpecializations(getSpecializations() | SCUBA);
                    break;
                case INF_JUMP:
                    // fall through to get the original Walk MP is deliberate
                    setOriginalJumpMP(3);
                case INF_LEG:
                    setOriginalWalkMP(1);
                    break;
                default:
                    setOriginalWalkMP(1);
            }
            addTechComponent(getMotiveTechAdvancement());
        }
    }

    /**
     * Standard and motorized SCUBA only differ in base movement, so they both use
     * INF_UMU. If the motion_type contains the string "motorized", the movement is set here instead.
     */
    public void setMotorizedScuba() {
        setMovementMode(EntityMovementMode.INF_UMU);
        setOriginalJumpMP(2);
    }

    @Override
    public String getMovementModeAsString() {
        if (isConventionalInfantry() && (mount == null)) {
            if (getMovementMode().isVTOL()) {
                return hasMicrolite() ? "Microlite" : "Microcopter";
            }
            if (getMovementMode().isUMUInfantry()) {
                return getOriginalJumpMP() > 1 ? "Motorized SCUBA" : "SCUBA";
            }
        }
        return super.getMovementModeAsString();
    }

    /**
     * @return True for all infantry that are allowed AM attacks. Mechanized infantry and infantry units with
     * encumbering armor or field guns are not allowed to make AM attacks, while all other infantry are.
     * Note that a conventional infantry unit without Anti-Mek gear (15 kg per trooper) can still make AM attacks
     * but has a fixed 8 AM skill rating.
     */
    public boolean canMakeAntiMekAttacks() {
        return !isMechanized() && !isArmorEncumbering() && !hasActiveFieldWeapon();
    }

    @Override
    public double getWeight() {
        return TestInfantry.getWeight(this);
    }

    public String getArmorDesc() {
        StringBuilder sArmor = new StringBuilder();
        sArmor.append(calcDamageDivisor());
        if (isArmorEncumbering()) {
            sArmor.append("E");
        }

        if (hasSpaceSuit()) {
            sArmor.append(" (Spacesuit) ");
        }

        if (hasDEST()) {
            sArmor.append(" (DEST) ");
        }

        if (hasSneakCamo() || (getCrew() != null && hasAbility(OptionsConstants.MD_DERMAL_CAMO_ARMOR))) {
            sArmor.append(" (Camo) ");
        }

        if (hasSneakIR()) {
            sArmor.append(" (IR) ");
        }

        if (hasSneakECM()) {
            sArmor.append(" (ECM) ");
        }

        return sArmor.toString();
    }

    @Override
    public void restore() {
        super.restore();

        if (null != primaryName) {
            primaryWeapon = (InfantryWeapon) EquipmentType.get(primaryName);
        }

        if (null != secondName) {
            secondaryWeapon = (InfantryWeapon) EquipmentType.get(secondName);
        }
    }

    /** @return True if this infantry has a field artillery weapon that is not destroyed. */
    public boolean hasActiveFieldArtillery() {
        return activeFieldWeapons().stream().anyMatch(gun -> gun.getType().hasFlag(WeaponType.F_ARTILLERY));
    }

    /**
     * Infantry don't use MP to change facing, and don't do PSRs, so just don't let them use maneuvering ace
     * otherwise, their movement gets screwed up
     */
    @Override
    public boolean isUsingManAce() {
        return false;
    }

    @Override
    public int getEngineHits() {
        return 0;
    }

    @Override
    public String getLocationDamage(int loc) {
        return "";
    }

    @Override
    public boolean isCrippled() {
        double activeTroopPercent = (double) getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY);
        if (activeTroopPercent < 0.25) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Only "
                    + NumberFormat.getPercentInstance().format(activeTroopPercent) + " troops remaining.");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        return isCrippled();
    }

    @Override
    public boolean isDmgHeavy() {
        return (((double) getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY)) < 0.5);
    }

    @Override
    public boolean isDmgModerate() {
        return (((double) getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY)) < 0.75);
    }

    @Override
    public boolean isDmgLight() {
        return (((double) getInternal(LOC_INFANTRY) / getOInternal(LOC_INFANTRY)) < 0.9);
    }

    /** @return True if this infantry has any field gun or artillery (destroyed or not). */
    public boolean hasFieldWeapon() {
        return !originalFieldWeapons().isEmpty();
    }

    /** @return True if this infantry has any working (not destroyed) field gun or artillery. */
    public boolean hasActiveFieldWeapon() {
        return !activeFieldWeapons().isEmpty();
    }

    @Override
    public boolean hasEngine() {
        return false;
    }

    @Override
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted) throws LocationFullException {
        super.addEquipment(mounted, loc, rearMounted);
        // Add equipment slots for ammo switching of field guns and field artillery
        addCritical(loc, new CriticalSlot(mounted));
    }

    @Override
    public boolean isConventionalInfantry() {
        return true;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_INFANTRY;
    }

    @Override
    public PilotingRollData checkLandingInHeavyWoods(EntityMovementType overallMoveType, Hex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE, "Infantry cannot fall");
        return roll;
    }

    /**
     * Determines if there is valid cover for an infantry unit to utilize the
     * Using Non-Infantry as Cover rules (TO pg 108).
     * @param game The current {@link Game}
     * @param pos The hex coords
     * @param elevation The elevation (flying or in building)
     * @return True when this infantry has valid conver
     */
    public static boolean hasValidCover(Game game, Coords pos, int elevation) {
        // Can't do anything if we don't have a position
        // If elevation > 0, we're either flying, or in a building
        // In either case, we shouldn't be allowed to take cover
        if ((pos == null) || (elevation > 0)) {
            return false;
        }
        boolean hasMovedEntity = false;
        // First, look for ground untis in the same hex that have already moved
        for (Entity e : game.getEntitiesVector(pos)) {
            if (e.isDone() && !(e instanceof Infantry) && (e.getElevation() == elevation)) {
                hasMovedEntity = true;
                break;
            }
        }
        // If we didn't find anything, check for wrecks
        // The rules don't explicitly cover this, but it makes sense
        if (!hasMovedEntity) {
            Enumeration<Entity> wrecks = game.getWreckedEntities();
            while (wrecks.hasMoreElements()) {
                Entity e = wrecks.nextElement();
                if (pos.equals(e.getPosition()) && !(e instanceof Infantry)) {
                    hasMovedEntity = true;
                }
            }
        }
        return hasMovedEntity;
    }

    public boolean isTakingCover() {
        return isTakingCover;
    }

    public void setTakingCover(boolean isTakingCover) {
        this.isTakingCover = isTakingCover;
    }

    @Override
    protected boolean hasViableWeapons() {
        return !isCrippled();
    }

    @Override
    public int getSpriteDrawPriority() {
        return 1;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(3.586 + 0.336*Math.log(getWeight())));
    }
}