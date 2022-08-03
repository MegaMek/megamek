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
import megamek.common.battlevalue.InfantryBVCalculator;
import megamek.common.cost.InfantryCostCalculator;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.infantry.InfantryWeapon;
import org.apache.logging.log4j.LogManager;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents the lowest of the low, the ground pounders, the city
 * rats, the PBI (Poor Bloody Infantry). <p> PLEASE NOTE!!! This class just
 * represents unarmored infantry platoons as described by CitiTech (c) 1986.
 * I've never seen the rules for powered armor, "anti-mech" troops, or
 * Immortals.
 *
 * PLEASE NOTE!!! My programming style is to put constants first in tests so the
 * compiler catches my "= for ==" errors.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour)
 */
public class Infantry extends Entity {
    private static final long serialVersionUID = -8706716079307721282L;

    /**
     * Infantry Specializations
     */
    public static int BRIDGE_ENGINEERS  = 1 << 0;
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
    public static int COMBAT_ENGINEERS = BRIDGE_ENGINEERS | DEMO_ENGINEERS
            | FIRE_ENGINEERS | MINE_ENGINEERS | SENSOR_ENGINEERS
            | TRENCH_ENGINEERS;
    
    /**
     * squad size and number
     */
    protected int squadn = 1;
    private int squadsize = 1;

    /**
     * The number of men originally in this platoon.
     */
    protected int menStarting = 0;

    /**
     * The number of men alive in this platoon at the beginning of the phase,
     * before it begins to take damage.
     */
    private int menShooting = 0;

    /**
     * The number of men left alive in this platoon.
     */
    private int men = 0;

    /**
     * Information on primary and secondary weapons
     * This must be kept separate from the equipment array
     * because they are not fired as separate weapons
     */
    private transient InfantryWeapon primaryW;
    private String primaryName;
    private transient InfantryWeapon secondW;
    private String secondName;
    private int secondn = 0;


    /**
     * Infantry armor
     */
    private double damageDivisor = 1.0;
    private boolean encumbering = false;
    private boolean spaceSuit = false;
    private boolean dest = false;
    private boolean sneak_camo = false;
    private boolean sneak_ir = false;
    private boolean sneak_ecm = false;

    /**
     * Stores which infantry specializations are active.
     */
    private int infSpecs = 0;

    /**
     * For mechanized VTOL infantry, stores whether the platoon are microlite troops,
     * which need to enter a hex every turn to remain in flight.
     */
    private boolean microlite = false;

    /**
     * The location for infantry equipment.
     */
    public static final int LOC_INFANTRY = 0;
    public static final int LOC_FIELD_GUNS = 1;

    /**
     * Infantry only have critical slots for field gun ammo
     */
    private static final int[] NUM_OF_SLOTS = { 20, 20 };
    private static final String[] LOCATION_ABBRS = { "MEN", "FGUN" };
    private static final String[] LOCATION_NAMES = { "Men", "Field Guns"};

    public int turnsLayingExplosives = -1;

    public static final int DUG_IN_NONE = 0;
    public static final int DUG_IN_WORKING = 1; // no protection, can't attack
    public static final int DUG_IN_COMPLETE = 2; // protected, restricted arc
    public static final int DUG_IN_FORTIFYING1 = 3; // no protection, can't
    // attack
    public static final int DUG_IN_FORTIFYING2 = 4; // no protection, can't
    // attack
    private int dugIn = DUG_IN_NONE;

    private boolean isTakingCover = false;
    private boolean canCallSupport = true;
    private boolean isCallingSupport = false;

    // Public and Protected constants, constructors, and methods.

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int INF_PLT_MAX_MEN = 30;

    /**
     * The internal names of the anti-Mek attacks.
     */
    public static final String LEG_ATTACK = "LegAttack";
    public static final String SWARM_MEK = "SwarmMek";
    public static final String SWARM_WEAPON_MEK = "SwarmWeaponMek";
    public static final String STOP_SWARM = "StopSwarm";

    public static final int ANTI_MECH_SKILL_UNTRAINED = 8;
    public static final int ANTI_MECH_SKILL_FOOT = 5;
    public static final int ANTI_MECH_SKILL_JUMP = 6;

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns the number of locations in this platoon
     */
    @Override
    public int locations() {
        return 2;
    }

    /**
     * Generate a new, blank, infantry platoon. Hopefully, we'll be loaded from
     * somewhere.
     */
    public Infantry() {
        // Instantiate the superclass.
        super();
        // Create a "dead" leg rifle platoon.
        menStarting = 0;
        menShooting = 0;
        men = 0;
        setMovementMode(EntityMovementMode.INF_LEG);
        // Determine the number of MPs.
        setOriginalWalkMP(1);
    }

    @Override
    public int getUnitType() {
        return UnitType.INFANTRY;
    }

    @Override
    public CrewType defaultCrewType() {
        return CrewType.CREW;
    }

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

    public static TechAdvancement getAntiMekTA() {
        return new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(2456, 2460, 2500).setApproximate(true, false, false)
                .setPrototypeFactions(F_LC).setProductionFactions(F_LC)
                .setTechRating(RATING_D)
                .setAvailability(RATING_D, RATING_D, RATING_D, RATING_D)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    protected void addSystemTechAdvancement(CompositeTechLevel ctl) {
        super.addSystemTechAdvancement(ctl);
        ctl.addComponent(Infantry.getMotiveTechAdvancement(movementMode));
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
        if (isAntiMekTrained()) {
            ctl.addComponent(Infantry.getAntiMekTA());
        }
    }

    /**
     * Infantry can face freely (except when dug in)
     */
    @Override
    public boolean canChangeSecondaryFacing() {
        return !hasActiveFieldArtillery();
    }

    /**
     * Infantry can face freely
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return true;
    }

    /**
     * Infantry can face freely
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        return dir;
    }

    /**
     * Create local platoon for Urban Guerrilla
     */
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

    /**
     * return this infantry's walk mp, adjusted for planetary conditions
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        int mp = getOriginalWalkMP();
        // encumbering armor reduces MP by 1 to a minimum of one (TacOps, pg. 318)
        if (encumbering) {
            mp = Math.max(mp - 1, 1);
        }
        if ((getSecondaryN() > 1)
                && !hasAbility(OptionsConstants.MD_TSM_IMPLANT)
                && !hasAbility(OptionsConstants.MD_DERMAL_ARMOR)
                && (null != secondW) && secondW.hasFlag(WeaponType.F_INF_SUPPORT)
                && (getMovementMode() != EntityMovementMode.TRACKED)
                && (getMovementMode() != EntityMovementMode.INF_JUMP)) {
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
                && ((getMovementMode() == EntityMovementMode.INF_LEG)
                        || (getMovementMode() == EntityMovementMode.INF_JUMP))) {
            mp += 1;
        }
        if (hasActiveFieldArtillery()) {
            //mp of 1 at the most
            mp = Math.min(mp, 1);
        }
        if (null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if (weatherMod != 0) {
                mp = Math.max(mp + weatherMod, 0);
            }
        }
        if (gravity) {
            mp = applyGravityEffectsOnMP(mp);
        }
        return mp;
    }

    /**
     * Return this Infantry's run MP, which is identical to its walk MP
     */
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if ( (game != null)
                && game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_FAST_INFANTRY_MOVE)) {
            if (getWalkMP(gravity, ignoreheat, ignoremodulararmor) > 0) {
                return getWalkMP(gravity, ignoreheat, ignoremodulararmor) + 1;
            }
            return getWalkMP(gravity, ignoreheat, ignoremodulararmor) + 2;
        }
        return getWalkMP(gravity, ignoreheat, ignoremodulararmor);
    }

    /**
     * Infantry don't have MASC
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        return getRunMP(gravity, ignoreheat, ignoremodulararmor);
    }


    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getJumpMP(boolean)
     */
    @Override
    public int getJumpMP(boolean gravity) {
        int mp = 0;
        if (getMovementMode() != EntityMovementMode.INF_UMU
                && getMovementMode() != EntityMovementMode.SUBMARINE) {
            mp = getOriginalJumpMP();
        }
        if ((getSecondaryN() > 1)
                && !hasAbility(OptionsConstants.MD_TSM_IMPLANT)
                && !hasAbility(OptionsConstants.MD_DERMAL_ARMOR)
                && (getMovementMode() != EntityMovementMode.SUBMARINE)
                && (null != secondW) && secondW.hasFlag(WeaponType.F_INF_SUPPORT)) {
            mp = Math.max(mp - 1, 0);
        } else if (movementMode.equals(EntityMovementMode.VTOL) && getSecondaryN() > 0) {
            mp = Math.max(mp - 1, 0);
        }
        if (gravity) {
            mp = applyGravityEffectsOnMP(mp);
        }
        int windP = 0;
        if (null != game) {
            int windCond = game.getPlanetaryConditions().getWindStrength();
            if (windCond == PlanetaryConditions.WI_MOD_GALE) {
                windP++;
            }
            if (windCond >= PlanetaryConditions.WI_STRONG_GALE) {
                return 0;
            }
        }
        mp = Math.max(mp - windP, 0);
        return mp;
    }

    @Override
    public boolean hasUMU() {
        return getMovementMode().equals(EntityMovementMode.INF_UMU)
                || getMovementMode().equals(EntityMovementMode.SUBMARINE);
    }

    @Override
    public int getActiveUMUCount() {
        return getAllUMUCount();
    }

    @Override
    public int getAllUMUCount() {
        if (hasUMU()) {
            return jumpMP;
        } else {
            return 0;
        }
    }

    @Override
    public boolean antiTSMVulnerable() {
        if (!hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            return false;
        }
        EquipmentType armorKit = getArmorKit();
        return (armorKit == null)
                || !armorKit.hasSubType(MiscType.S_SPACE_SUIT | MiscType.S_XCT_VACUUM
                    | MiscType.S_TOXIC_ATMO);
    }

    /**
     * Infantry can not enter water unless they have UMU mp or hover.
     */
    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        // Coords off the board aren't legal
        if (!game.getBoard().contains(c)) {
            return true;
        }
        Hex hex = game.getBoard().getHex(c);
        // Taharqa: waiting to hear back from Welshie but I am going to assume
        // that units pulling artillery
        // should be treated as wheeled rather than motorized because otherwise
        // mechanized units face fewer
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

        if (hex.containsTerrain(Terrains.MAGMA)) {
            return true;
        }

        if (getMovementMode() == EntityMovementMode.WHEELED) {
            if (hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.ROUGH)
                    || hex.containsTerrain(Terrains.RUBBLE)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || (hex.terrainLevel(Terrains.SNOW) > 1)
                    || (hex.terrainLevel(Terrains.GEYSER) == 2)) {
                return true;
            }
        }

        if (getMovementMode() == EntityMovementMode.TRACKED) {
            if ((hex.terrainLevel(Terrains.WOODS) > 1)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || (hex.terrainLevel(Terrains.ROUGH) > 1)
                    || (hex.terrainLevel(Terrains.RUBBLE) > 5)) {
                return true;
            }
        }

        if (getMovementMode() == EntityMovementMode.HOVER) {
            if (hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.JUNGLE)
                    || (hex.terrainLevel(Terrains.ROUGH) > 1)
                    || (hex.terrainLevel(Terrains.RUBBLE) > 5)) {
                return true;
            }
        }

        if (hex.terrainLevel(Terrains.WATER) <= 0
                && getMovementMode() == EntityMovementMode.SUBMARINE) {
            return true;
        }

        if ((hex.terrainLevel(Terrains.WATER) > 0)
                && !hex.containsTerrain(Terrains.ICE)) {
            if ((getMovementMode() == EntityMovementMode.HOVER)
                    || (getMovementMode() == EntityMovementMode.INF_UMU)
                    || (getMovementMode() == EntityMovementMode.SUBMARINE)
                    || (getMovementMode() == EntityMovementMode.VTOL)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the name of the type of movement used. This is Infantry-specific.
     */
    @Override
    public String getMovementString(EntityMovementType mtype) {
        switch (mtype) {
            case MOVE_NONE:
                return "None";
            case MOVE_WALK:
            case MOVE_RUN:
                switch (getMovementMode()) {
                    case INF_LEG:
                        return "Walked";
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

    /**
     * Returns the abbreviation of the type of movement used. This is
     * Infantry-specific.
     */
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

    /**
     * Infantry only have one hit location.
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
                                   int cover) {
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_INFANTRY);
    }

    /**
     * Infantry only have one hit location.
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(Entity.LOC_DESTROYED);
    }

    /**
     * Gets the location that is destroyed recursively.
     */
    @Override
    public int getDependentLocation(int loc) {
        return Entity.LOC_NONE;
    }

    /**
     * Infantry have no rear armor.
     */
    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    /**
     * Infantry platoons do wierd and wacky things with armor and internals, but
     * not all Infantry objects are platoons.
     *
     * @see megamek.common.BattleArmor#isPlatoon()
     */
    protected boolean isPlatoon() {
        return true;
    }

    /**
     * Returns the number of men left in the platoon, or
     * IArmorState.ARMOR_DESTROYED.
     */
    @Override
    public int getInternal(int loc) {
        if (!isPlatoon()) {
            return super.getInternal(loc);
        }
        if (loc != LOC_INFANTRY) {
            return 0;
        }
        return (men > 0 ? men : IArmorState.ARMOR_DESTROYED);
    }

    /**
     * Returns the number of men originally the platoon.
     */
    @Override
    public int getOInternal(int loc) {
        if (!isPlatoon()) {
            return super.getOInternal(loc);
        }
        return menStarting;
    }

    /**
     * Sets the amount of men remaining in the platoon.
     */
    @Override
    public void setInternal(int val, int loc) {
        super.setInternal(val, loc);
        if (loc == LOC_INFANTRY) {
            men = val;
        }
    }

    /**
     * Returns the percent of the men remaining in the platoon.
     */
    @Override
    public double getInternalRemainingPercent() {
        if (!isPlatoon()) {
            return super.getInternalRemainingPercent();
        }
        int menTotal = men > 0 ? men : 0; // Handle "DESTROYED"
        return ((double) menTotal / menStarting);
    }

    /**
     * Initializes the number of men in the platoon. Sets the original and
     * starting point of the platoon to the same number.
     */
    @Override
    public void initializeInternal(int val, int loc) {
        menStarting = val;
        menShooting = val;
        super.initializeInternal(val, loc);
    }

    /**
     * Set the men in the platoon based on squad size and number
     */
    @Override
    public void autoSetInternal() {
        //TODO: put checks here on size
        initializeInternal(squadsize*squadn, LOC_INFANTRY);
    }

    /**
     * Infantry can fire all around themselves. But field guns are set up to a
     * vehicular turret facing
     */
    @Override
    public int getWeaponArc(int wn) {
        Mounted mounted = getEquipment(wn);
        if (mounted.getLocation() == LOC_FIELD_GUNS) {
            if (game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_VEHICLE_ARCS)) {
                return Compute.ARC_TURRET;
            }
            return Compute.ARC_FORWARD;
        }
        //This is interesting, according to TacOps rules, Dug in units no longer
        //have to declare a facing
        return Compute.ARC_360;
    }

    /**
     * Infantry can fire all around themselves. But field guns act like turret
     * mounted on a tank
     */
    @Override
    public boolean isSecondaryArcWeapon(int wn) {
        if ((getEquipment(wn).getLocation() == LOC_FIELD_GUNS) && !hasActiveFieldArtillery()) {
            return true;
        }
        return false;
    }

    /**
     * Infantry build no heat.
     */
    @Override
    public int getHeatCapacity(boolean radicalHeatSinks) {
        return DOES_NOT_TRACK_HEAT;
    }

    /**
     * Infantry build no heat.
     */
    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    /**
     * Infantry build no heat.
     */
    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    /**
     * Infantry have no critical slots.
     */
    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Infantry criticals can't be hit.
     */
    public boolean hasHittableCriticals(int loc) {
        return false;
    }

    @Override
    protected int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return InfantryBVCalculator.calculateBV(this, ignoreSkill, calculationReport);
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7041);
        r.type = Report.PUBLIC;
        r.add(getCrew().getGunnery());
        r.newlines = 0;
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

    /**
     * Infantry don't need piloting rolls.
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        return prd;
    }

    /**
     * Infantry can only change 1 elevation level at a time unless Mountain Inf
     * which is 3.
     */
    @Override
    public int getMaxElevationChange() {
        if (hasSpecialization(MOUNTAIN_TROOPS)) {
            return 3;
        }
        return 1;
    }

    /**
     * Update the platoon to reflect damages taken in this phase.
     */
    @Override
    public void applyDamage() {
        super.applyDamage();
        menShooting = men;
    }

    // The methods below aren't in the Entity interface.

    /**
     * Get the number of men in the platoon (before damage is applied).
     */
    public int getShootingStrength() {
        return menShooting;
    }

    @Override
    public boolean canCharge() {
        // Infantry can't Charge
        return false;
    }

    @Override
    public boolean canDFA() {
        // Infantry can't DFA
        return false;
    }

    /**
     * Checks if the entity is moving into a swamp. If so, returns the target
     * roll for the piloting skill check. now includes the level 3 terains which
     * can bog down
     */
    @Override
    public PilotingRollData checkBogDown(MoveStep step,
                                         EntityMovementType moveType, Hex curHex, Coords lastPos,
                                         Coords curPos, int lastElev, boolean isPavementStep) {
        return checkBogDown(step, curHex, lastPos, curPos, isPavementStep);
    }

    public PilotingRollData checkBogDown(MoveStep step, Hex curHex,
            Coords lastPos, Coords curPos, boolean isPavementStep) {
        PilotingRollData roll = new PilotingRollData(getId(), 4,
                "entering boggy terrain");
        int bgMod = curHex.getBogDownModifier(getMovementMode(), false);
        final boolean onBridge = (curHex.terrainLevel(Terrains.BRIDGE) > 0)
                && (getElevation() == curHex.terrainLevel(Terrains.BRIDGE_ELEV));
        if (!lastPos.equals(curPos) && (bgMod != TargetRoll.AUTOMATIC_SUCCESS)
                && (step.getMovementType(false) != EntityMovementType.MOVE_JUMP)
                && (getMovementMode() != EntityMovementMode.HOVER)
                && (getMovementMode() != EntityMovementMode.VTOL)
                && (getMovementMode() != EntityMovementMode.WIGE)
                && (step.getElevation() == 0) && !isPavementStep && !onBridge) {
            roll.append(new PilotingRollData(getId(), bgMod,
                    "avoid bogging down"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE,
                    "Check false: Not entering bog-down terrain, "
                            + "or jumping/hovering over such terrain");
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
        if (isAntiMekTrained()) {
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
                priceMultiplier *= 3.2;
                break;
            case WHEELED:
                priceMultiplier *= 3.2;
                break;
            case TRACKED:
                priceMultiplier *= 3.2;
                break;
            case VTOL:
                priceMultiplier *= hasMicrolite() ? 4 : 4.5;
                break;
            case SUBMARINE:
                // No cost given in TacOps, using basic mechanized cost for now
                priceMultiplier *= 3.2;
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
        if (null != primaryW) {
            cost += primaryW.getCost(this, false, -1) * (squadsize - secondn);
        }
        if (null != secondW) {
            cost += secondW.getCost(this, false, -1) * secondn;
        }
        cost = cost / squadsize;

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
            case INF_LEG:
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
        cost *= menStarting;
        // add in field gun costs
        for (Mounted mounted : getEquipment()) {
            if (mounted.getLocation() == LOC_FIELD_GUNS) {
                cost += mounted.getType().getCost(this, false, -1);
            }
        }
        return cost;
    }

    @Override
    public boolean doomedInExtremeTemp() {
        if (getArmorKit() != null) {
            if (getArmorKit().hasSubType(MiscType.S_XCT_VACUUM)) {
                return false;
            } else if (getArmorKit().hasSubType(MiscType.S_COLD_WEATHER) && (game.getPlanetaryConditions().getTemperature() < -30)) {
                return false;
            } else if (getArmorKit().hasSubType(MiscType.S_HOT_WEATHER) && (game.getPlanetaryConditions().getTemperature() > 50)) {
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
        if (getMovementMode() == EntityMovementMode.VTOL) {
            return true;
        } else {
            return !hasSpaceSuit();
        }
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
        if ((turnsLayingExplosives > 0) && (phase != GamePhase.PHYSICAL)) {
            return false;
        }
        if ((dugIn != DUG_IN_COMPLETE) && (dugIn != DUG_IN_NONE)) {
            return false;
        }
        return super.isEligibleFor(phase);
    }

    @Override
    public boolean isEligibleForFiring() {
        if (game.getOptions().booleanOption(OptionsConstants.ADVGRNDMOV_TACOPS_FAST_INFANTRY_MOVE)) {
            if (moved == EntityMovementType.MOVE_RUN) {
                return false;
            }
        }
        return super.isEligibleForFiring();
    }

    @Override
    public void newRound(int roundNumber) {
        if (turnsLayingExplosives >= 0) {
            turnsLayingExplosives++;
            if (!(Compute.isInBuilding(game, this))) {
                turnsLayingExplosives = -1; // give up if no longer in a building
            }
        }
        if ((dugIn != DUG_IN_COMPLETE) && (dugIn != DUG_IN_NONE)) {
            dugIn++;
            if (dugIn > DUG_IN_FORTIFYING2) {
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
     * @param transportID
     */
    public void setTransportID(int transportID) {
        super.setTransportId(transportID);

        setDugIn(DUG_IN_NONE);
    }

    /**
     * Convenience method for setting the anti-mek skill of the unit based on
     * whether or not they have anti-mek training.  If the input is false, the
     * anti-mek skill is set to the default untrained value, otherwise it's
     * set to the default value based on motive type.
     *
     * @param amTraining
     */
    public void setAntiMekSkill(boolean amTraining) {
        if (getCrew() == null) {
            return;
        }
        if (amTraining) {
            if ((getMovementMode() == EntityMovementMode.INF_MOTORIZED)
                    || getMovementMode() == EntityMovementMode.INF_JUMP) {
                getCrew().setPiloting(ANTI_MECH_SKILL_JUMP, 0);
            } else {
                getCrew().setPiloting(ANTI_MECH_SKILL_FOOT, 0);
            }
        } else {
            getCrew().setPiloting(ANTI_MECH_SKILL_UNTRAINED, 0);
        }
    }

    /**
     * Set the anti-mek skill for this unit.  Since Infantry don't have piloting
     * the crew's piloting skill is treated as the anti-mek skill.  This is
     * largely just a convenience method for setting the Crew's piloting skill.
     * @param amSkill
     */
    public void setAntiMekSkill(int amSkill) {
        if (getCrew() == null) {
            return;
        }
        getCrew().setPiloting(amSkill, 0);
    }

    /**
     * Returns the anti-mek skill for this unit.  Since Infantry don't have
     * piloting the crew's piloting skill is treated as the anti-mek skill.
     * This is largely just a convenience method for setting the Crew's piloting
     * skill.
     * @return
     */
    public int getAntiMekSkill() {
        if (getCrew() == null) {
            return ANTI_MECH_SKILL_UNTRAINED;
        } else {
            return getCrew().getPiloting();
        }
    }

    /**
     * Returns true if this unit has anti-mek training.  According to TM pg 155,
     * any unit that has less than 8 anti-mek skill is assumed to have anti-mek
     * training.  This implies that the unit carries the requisite equipment for
     * properly performing anti-mek attacks (and the weight and cost that goes
     * along with that).
     * @return
     */
    public boolean isAntiMekTrained() {
        // Anything below the antimech skill default is considered to be AM
        // trained.  See TM pg 155
        return getAntiMekSkill() < ANTI_MECH_SKILL_UNTRAINED;
    }

    public boolean isMechanized() {
        return (getMovementMode() == EntityMovementMode.WHEELED) ||
                (getMovementMode() == EntityMovementMode.HOVER) ||
                (getMovementMode() == EntityMovementMode.TRACKED) ||
                (getMovementMode() == EntityMovementMode.SUBMARINE) ||
                (getMovementMode() == EntityMovementMode.VTOL);
    }

    public boolean isXCT() {
        return hasSpecialization(XCT);
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 0;
    }

    public EquipmentType getArmorKit() {
        Optional<Mounted> kit = getEquipment().stream()
                .filter(m -> m.getType().hasFlag(MiscType.F_ARMOR_KIT))
                .findFirst();
        if (kit.isPresent()) {
            return kit.get().getType();
        } else {
            return null;
        }
    }

    public void setArmorKit(EquipmentType armorKit) {
        List<Mounted> toRemove = getEquipment().stream()
                .filter(m -> m.getType().hasFlag(MiscType.F_ARMOR_KIT))
                .collect(Collectors.toList());
        getEquipment().removeAll(toRemove);
        getMisc().removeAll(toRemove);
        if (armorKit != null && armorKit.hasFlag(MiscType.F_ARMOR_KIT)) {
            try {
                addEquipment(armorKit, LOC_INFANTRY);
            } catch (LocationFullException ex) {
                LogManager.getLogger().error("", ex);
            }
            damageDivisor = ((MiscType) armorKit).getDamageDivisor();
            encumbering = (armorKit.getSubType() & MiscType.S_ENCUMBERING) != 0;
            spaceSuit = (armorKit.getSubType() & MiscType.S_SPACE_SUIT) != 0;
            dest = (armorKit.getSubType() & MiscType.S_DEST) != 0;
            sneak_camo = (armorKit.getSubType() & MiscType.S_SNEAK_CAMO) != 0;
            sneak_ir = (armorKit.getSubType() & MiscType.S_SNEAK_IR) != 0;
            sneak_ecm = (armorKit.getSubType() & MiscType.S_SNEAK_ECM) != 0;
        }
    }

    public double calcDamageDivisor() {
        double divisor = damageDivisor;
        // TSM implant reduces divisor to 0.5 if no other armor is worn
        if ((divisor == 1.0) && hasAbility(OptionsConstants.MD_TSM_IMPLANT)) {
            divisor = 0.5;
        }
        // Dermal armor adds one to the divisor, cumulative with armor kit and TSM implant
        if (hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
            divisor += 1.0;
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
            // Need to add vibro shovels
            try {
                EquipmentType shovels = EquipmentType.get(EquipmentTypeLookup.VIBRO_SHOVEL);
                addEquipment(shovels, Infantry.LOC_INFANTRY);
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        } else if ((spec & TRENCH_ENGINEERS) == 0
                && (infSpecs & TRENCH_ENGINEERS) > 0) {
            // Need to remove vibro shovels
            List<Mounted> eqToRemove = new ArrayList<>();
            for (Mounted eq : getEquipment()) {
                if (eq.getType().hasFlag(MiscType.F_TOOLS)
                        && eq.getType().hasSubType(MiscType.S_VIBROSHOVEL)) {
                    eqToRemove.add(eq);
                }
            }
            getEquipment().removeAll(eqToRemove);
            getMisc().removeAll(eqToRemove);
        }
        // Equipment for Demolition Engineers
        if ((spec & DEMO_ENGINEERS) > 0 && (infSpecs & DEMO_ENGINEERS) == 0) {
            // Need to add vibro shovels
            try {
                EquipmentType shovels = EquipmentType.get(EquipmentTypeLookup.DEMOLITION_CHARGE);
                addEquipment(shovels, Infantry.LOC_INFANTRY);
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        } else if ((spec & DEMO_ENGINEERS) == 0
                && (infSpecs & DEMO_ENGINEERS) > 0) {
            // Need to remove vibro shovels
            List<Mounted> eqToRemove = new ArrayList<>();
            for (Mounted eq : getEquipment()) {
                if (eq.getType().hasFlag(MiscType.F_TOOLS)
                        && eq.getType()
                                .hasSubType(MiscType.S_DEMOLITION_CHARGE)) {
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

    /**
     * Determine the stealth modifier for firing at this unit from the given
     * range. If the value supplied for <code>range</code> is not one of the
     * <code>Entity</code> class range constants, an
     * <code>IllegalArgumentException</code> will be thrown. <p> Sub-classes
     * are encouraged to override this method.
     *
     * @param range - an <code>int</code> value that must match one of the
     *            <code>Compute</code> class range constants.
     * @param ae - the entity making the attack.
     * @return a <code>TargetRoll</code> value that contains the stealth
     *         modifier for the given range.
     */
    @Override
    public TargetRoll getStealthModifier(int range, Entity ae) {
        TargetRoll result = null;

        // Note: infantry are immune to stealth, but not camoflage
        // or mimetic armor

        if ((sneak_ir || dest)
                && !(ae instanceof Infantry)) {
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
                    throw new IllegalArgumentException(
                            "Unknown range constant: " + range);
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

        // Return the result.
        return result;
    } // End public TargetRoll getStealthModifier( char )

    /**
     * Determines if the infantry has any type of stealth system.
     *
     * @return
     */
    public boolean isStealthy() {
       return  dest || sneak_camo || sneak_ir || sneak_ecm;
    }

    public boolean hasMicrolite() {
        return microlite;
    }

    public void setMicrolite(boolean microlite) {
        this.microlite = microlite;
    }

    /**
     * Used to check for standard or motorized SCUBA infantry, which have a maximum
     * depth of 2.
     * @return true if this is a conventional infantry unit with non-mechanized SCUBA specialization
     */
    public boolean isNonMechSCUBA() {
        return isConventionalInfantry() && (getMovementMode() == EntityMovementMode.INF_UMU);
    }

    public void setPrimaryWeapon(InfantryWeapon w) {
        primaryW = w;
        primaryName = w.getName();
    }

    public InfantryWeapon getPrimaryWeapon() {
        return primaryW;
    }

    public void setSecondaryWeapon(InfantryWeapon w) {
        secondW = w;
        if (null == w) {
            secondName = null;
        } else {
            secondName = w.getName();
        }
    }

    public InfantryWeapon getSecondaryWeapon() {
        return secondW;
    }

    public void setSquadSize(int size) {
        squadsize = size;
    }

    public int getSquadSize() {
        return squadsize;
    }

    public void setSquadN(int n) {
        squadn = n;
    }

    public int getSquadN() {
        return squadn;
    }

    public void setSecondaryN(int n) {
        secondn = n;
    }

    public int getSecondaryN() {
        return secondn;
    }

    public double getDamagePerTrooper() {
        if (null == primaryW) {
            return 0;
        }

        // per 09/2021 errata, primary infantry weapon damage caps out at 0.6
        double adjustedDamage = Math.min(MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP, primaryW.getInfantryDamage());
        double damage = adjustedDamage * (squadsize - secondn);
        if (null != secondW) {
            damage += secondW.getInfantryDamage() * secondn;
        }
        return damage/squadsize;
    }

    public boolean primaryWeaponDamageCapped() {
        return getPrimaryWeaponDamage() > MMConstants.INFANTRY_PRIMARY_WEAPON_DAMAGE_CAP;
    }

    public double getPrimaryWeaponDamage() {
        if (null == primaryW) {
            return 0;
        }

        return primaryW.getInfantryDamage();
    }

    public boolean isSquad() {
        return (squadn == 1);
    }

    /**
     * Set the movement type of the entity
     */
    @Override
    public void setMovementMode(EntityMovementMode movementMode) {
        super.setMovementMode(movementMode);
        //movement mode will determine base mp
        if (isConventionalInfantry()) {
            setOriginalJumpMP(0);
            switch (getMovementMode()) {
                case INF_MOTORIZED:
                    setOriginalWalkMP(3);
                    break;
                case HOVER:
                    setOriginalWalkMP(5);
                    break;
                case TRACKED:
                    setOriginalWalkMP(3);
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
            addTechComponent(Infantry.getMotiveTechAdvancement(movementMode));
        }
    }

    /**
     * Standard and motorized SCUBA only differ in base movement, so they both use
     * INF_UMU. If the motion_type contains the string "motorized",
     * the movement is set here instead.
     */
    public void setMotorizedScuba() {
        setMovementMode(EntityMovementMode.INF_UMU);
        setOriginalJumpMP(2);
    }

    @Override
    public String getMovementModeAsString() {
        if (!hasETypeFlag(Entity.ETYPE_BATTLEARMOR)) {
            if (getMovementMode().equals(EntityMovementMode.VTOL)) {
                return hasMicrolite() ? "Microlite" : "Microcopter";
            }
            if (getMovementMode() == EntityMovementMode.INF_UMU) {
                return getOriginalJumpMP() > 1 ? "Motorized SCUBA" : "SCUBA";
            }
        }
        return super.getMovementModeAsString();
    }

    /**
     * @return True for all infantry that are allowed AM attacks. Mechanized infantry and infantry units with
     * encumbering armor are not allowed to make AM attacks, while all other infantry are.
     * Note that a conventional infantry unit without Anti-Mek gear (15 kg per trooper) can still make AM attacks
     * but has a fixed 8 AM skill rating.
     */
    public boolean canMakeAntiMekAttacks() {
        return !isMechanized() && !isArmorEncumbering();
    }

    @Override
    public double getWeight() {
        double mult;
        switch (getMovementMode()) {
            case INF_MOTORIZED:
                mult = 0.195;
                break;
            case HOVER:
            case TRACKED:
            case WHEELED:
                mult = 1.0;
                break;
            case VTOL:
                mult = (hasMicrolite() ? 1.4 : 1.9);
                break;
            case INF_JUMP:
                mult = 0.165;
                break;
            case INF_UMU:
                if (getActiveUMUCount() > 1) {
                    mult = 0.295; //motorized + 0.1 for motorized scuba
                } else {
                    mult = 0.135; //foot + 0.05 for scuba
                }
                break;
            case SUBMARINE:
                mult = 0.9;
                break;
            case INF_LEG:
            default:
                mult = 0.085;
        }

        if (hasSpecialization(COMBAT_ENGINEERS)) {
            mult += 0.1;
        }

        if (hasSpecialization(PARATROOPS)) {
            mult += 0.05;
        }

        if (hasSpecialization(PARAMEDICS)) {
            mult += 0.05;
        }

        if (isAntiMekTrained()) {
            mult +=.015;
        }

        double ton = men * mult;
        
        // add in field gun weight
        for (Mounted mounted : getEquipment()) {
            if (mounted.getLocation() == LOC_FIELD_GUNS) {
                ton += mounted.getTonnage();
            }
        }

        return RoundWeight.nearestHalfTon(ton);
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

        if (hasSneakCamo() ||
                (getCrew() != null
                    && hasAbility(OptionsConstants.MD_DERMAL_CAMO_ARMOR))) {
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

    /**
     * Restores the entity after serialization
     */
    @Override
    public void restore() {
        super.restore();

        if (null != primaryName) {
            primaryW = (InfantryWeapon) EquipmentType.get(primaryName);
        }

        if (null != secondName) {
            secondW = (InfantryWeapon) EquipmentType.get(secondName);
        }
    }

    public boolean hasActiveFieldArtillery() {
        boolean hasArtillery = false;
        double smallestGun = 100.0;
        for (Mounted wpn : getWeaponList()) {
            if (wpn.getLocation() != LOC_FIELD_GUNS) {
                continue;
            }

            if (wpn.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                hasArtillery = true;
                if (wpn.getTonnage() < smallestGun) {
                    smallestGun = wpn.getTonnage();
                }
            }
        }

        //you must have enough men to fire at least the smallest piece
        return hasArtillery && (getShootingStrength() >= smallestGun);

    }

    /**
     * Infantry don't use MP to change facing, and don't
     * do PSRs, so just don't let them use maneuvering ace
     * otherwise, their movement gets screwed up
     */
    @Override
    public boolean isUsingManAce() {
        return false;
    }

    @Override
    public void setAlphaStrikeMovement(Map<String,Integer> moves) {
        moves.put(getMovementModeAsBattleForceString(),
                Math.max(getWalkMP(), getJumpMP()) * 2);
    }

    @Override
    public int getBattleForceSize() {
        //The tables are on page 356 of StartOps
        return 1;
    }

    @Override
    public int getBattleForceArmorPoints() {
        // Infantry armor points is # of men / 15
        return (int) Math.ceil(getInternal(0)/15.0);
    }

    @Override
    /**
     * Each squad has 1 structure point
     */
    public int getBattleForceStructurePoints() {
        return 1;
    }

    @Override
    public int getNumBattleForceWeaponsLocations() {
        if (hasFieldGun()) {
            return 2;
        }
        return 1;
    }

    @Override
    public double getBattleForceLocationMultiplier(int index, int location, boolean rearMounted) {
        if (index == location) {
            return 1.0;
        }
        return 0;
    }

    @Override
    public String getBattleForceLocationName(int index) {
        if (index == 0) {
            return "";
        }
        return LOCATION_ABBRS[index];
    }

    @Override
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        specialAbilities.put(BattleForceSPA.CAR, (int) Math.ceil(getWeight()));
        if (getMovementMode().equals(EntityMovementMode.INF_UMU)) {
            specialAbilities.put(BattleForceSPA.UMU, null);
        }
        if (hasSpecialization(FIRE_ENGINEERS)) {
            specialAbilities.put(BattleForceSPA.FF, null);
        }
        if (hasSpecialization(MINE_ENGINEERS)) {
            specialAbilities.put(BattleForceSPA.MSW, null);
        }
        if (hasSpecialization(MOUNTAIN_TROOPS)) {
            specialAbilities.put(BattleForceSPA.MTN, null);
        }
        if (hasSpecialization(PARATROOPS)) {
            specialAbilities.put(BattleForceSPA.PARA, null);
        }
        if (hasSpecialization(SCUBA)) {
            specialAbilities.put(BattleForceSPA.UMU, null);
        }
        if (hasSpecialization(TRENCH_ENGINEERS)) {
            specialAbilities.put(BattleForceSPA.TRN, null);
        }
        if (hasAbility("tsm_implant")) {
            specialAbilities.put(BattleForceSPA.TSI, null);
        }
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

    public boolean hasFieldGun() {
        for (Mounted m : getWeaponList()) {
            if (m.getLocation() == Infantry.LOC_FIELD_GUNS) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasEngine() {
        return false;
    }

    /**
     * Mounts the specified equipment in the specified location.
     */
    @Override
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted) throws LocationFullException {
        // Implement parent's behavior.
        super.addEquipment(mounted, loc, rearMounted);

        //we do need to equipment slots for ammo switching of field guns and field artillery
        // Add the piece equipment to our slots.
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
    public PilotingRollData checkLandingInHeavyWoods(
            EntityMovementType overallMoveType, Hex curHex) {
        PilotingRollData roll = getBasePilotingRoll(overallMoveType);
        roll.addModifier(TargetRoll.CHECK_FALSE,
                         "Infantry cannot fall");
        return roll;
    }

    /**
     * Determines if there is valid cover for an infantry unit to utilize the
     * Using Non-Infantry as Cover rules (TO pg 108).
     * @param game The current {@link Game}
     * @param pos
     * @param elevation
     * @return
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
            if (e.isDone() && !(e instanceof Infantry)
                    && (e.getElevation() == elevation)) {
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
                if (pos.equals(e.getPosition())
                        && !(e instanceof Infantry)) {
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

    /**
     * Used to determine the draw priority of different Entity subclasses.
     * This allows different unit types to always be draw above/below other
     * types.
     *
     * @return
     */
    @Override
    public int getSpriteDrawPriority() {
        return 1;
    }
} // End class Infantry
