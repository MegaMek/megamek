/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.generator;

import static java.util.Map.entry;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.IntStream;

import org.apache.commons.collections4.IteratorUtils;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ui.swing.dialog.AbstractUnitSelectorDialog;
import megamek.common.*;
import megamek.common.containers.MunitionTree;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.ArmorType;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

/**
 * Notes: check out
 * - RATGenerator.java
 * - ForceDescriptor.java
 * for era-based search examples
 */

public class TeamLoadOutGenerator {
    private final static MMLogger logger = MMLogger.create(TeamLoadOutGenerator.class);

    // region Constants
    // XML file containing flat list of weights; if not found, defaults used. If
    // found, defaults overridden.
    public static final String LOAD_OUT_SETTINGS_PATH = "mmconf" + File.separator + "munitionLoadOutSettings.xml";
    public static Properties weightProperties = new Properties();
    static {
        try (InputStream is = new FileInputStream(LOAD_OUT_SETTINGS_PATH)) {
            weightProperties.loadFromXML(is);
        } catch (Exception e) {
            logger.warn(e, "Munition weight properties could not be loaded!  Using defaults...");
            logger.debug(e, LOAD_OUT_SETTINGS_PATH + " was not loaded: ");
        }
    }

    public static float UNSET_FILL_RATIO = Float.NEGATIVE_INFINITY;

    public static final ArrayList<String> AP_MUNITIONS = new ArrayList<>(List.of(
            "Armor-Piercing", "Tandem-Charge"));

    public static final ArrayList<String> FLAK_MUNITIONS = new ArrayList<>(List.of(
            "ADA", "Cluster", "Flak", "AAAMissile Ammo", "LAAMissile Ammo"));

    public static final ArrayList<String> ACCURATE_MUNITIONS = new ArrayList<>(List.of(
            "Precision"));

    public static final ArrayList<String> HIGH_POWER_MUNITIONS = new ArrayList<>(List.of(
            "Tandem-Charge", "Fuel-Air", "HE", "Dead-Fire", "Davy Crockett-M",
            "ASMissile Ammo", "FABombLarge Ammo", "FABombSmall Ammo", "AlamoMissile Ammo"));

    public static final ArrayList<String> ANTI_INF_MUNITIONS = new ArrayList<>(List.of(
            "Inferno", "Fragmentation", "Flechette", "Fuel-Air", "Anti-personnel", "Acid",
            "FABombSmall Ammo", "ClusterBomb", "HEBomb"));

    public static final ArrayList<String> ANTI_BA_MUNITIONS = new ArrayList<>(List.of(
            "Inferno", "Fuel-Air", "Tandem-Charge", "Acid", "FABombSmall Ammo", "HEBomb"));

    public static final ArrayList<String> HEAT_MUNITIONS = new ArrayList<>(List.of(
            "Inferno", "Incendiary", "InfernoBomb"));

    public static final ArrayList<String> ILLUMINATION_MUNITIONS = new ArrayList<>(List.of(
            "Illumination", "Tracer", "Inferno", "Incendiary", "Flare", "InfernoBomb"));

    public static final ArrayList<String> UTILITY_MUNITIONS = new ArrayList<>(List.of(
            "Illumination", "Smoke", "Mine Clearance", "Anti-TSM", "Laser Inhibiting",
            "Thunder", "FASCAM", "Thunder-Active", "Thunder-Augmented", "Thunder-Vibrabomb",
            "Thunder-Inferno", "Flare", "ThunderBomb", "TAGBomb", "TorpedoBomb", "ASEWMissile Ammo"));

    // Guided munitions come in two main flavors
    public static final ArrayList<String> GUIDED_MUNITIONS = new ArrayList<>(List.of(
            "Semi-Guided", "Narc-capable", "Homing", "Copperhead", "LGBomb", "ArrowIVHomingMissile Ammo"));
    public static final ArrayList<String> TAG_GUIDED_MUNITIONS = new ArrayList<>(List.of(
            "Semi-Guided", "Homing", "Copperhead", "LGBomb", "ArrowIVHomingMissile Ammo"));
    public static final ArrayList<String> NARC_GUIDED_MUNITIONS = new ArrayList<>(List.of("Narc-capable"));

    // TODO Anti-Radiation Missiles See IO pg 62 (TO 368)
    public static final ArrayList<String> SEEKING_MUNITIONS = new ArrayList<>(List.of(
            "Heat-Seeking", "Listen-Kill", "Swarm", "Swarm-I"));

    public static final ArrayList<String> AMMO_REDUCING_MUNITIONS = new ArrayList<>(List.of(
            "Acid", "Laser Inhibiting", "Follow The Leader", "Heat-Seeking", "Tandem-Charge",
            "Thunder-Active", "Thunder-Augmented", "Thunder-Vibrabomb", "Thunder-Inferno",
            "AAAMissile Ammo", "ASMissile Ammo", "ASWEMissile Ammo", "ArrowIVMissile Ammo",
            "AlamoMissile Ammo", "Precision", "Armor-Piercing"));

    public static final ArrayList<String> TYPE_LIST = new ArrayList<String>(List.of(
            "LRM", "SRM", "AC", "ATM", "Arrow IV", "Artillery", "Artillery Cannon",
            "Mek Mortar", "Narc", "Bomb"));

    public static final Map<String, List<String>> TYPE_MAP = Map.ofEntries(
            entry("LRM", MunitionTree.LRM_MUNITION_NAMES),
            entry("SRM", MunitionTree.SRM_MUNITION_NAMES),
            entry("AC", MunitionTree.AC_MUNITION_NAMES),
            entry("ATM", MunitionTree.ATM_MUNITION_NAMES),
            entry("Arrow IV", MunitionTree.ARROW_MUNITION_NAMES),
            entry("Artillery", MunitionTree.ARTILLERY_MUNITION_NAMES),
            entry("Artillery Cannon", MunitionTree.MEK_MORTAR_MUNITION_NAMES),
            entry("Mek Mortar", MunitionTree.MEK_MORTAR_MUNITION_NAMES),
            entry("Narc", MunitionTree.NARC_MUNITION_NAMES),
            entry("Bomb", MunitionTree.BOMB_MUNITION_NAMES));

    // subregion Bombs
    // bomb types assignable to aerospace units on ground maps
    private static final int[] validBotBombs = {
            BombType.B_HE,
            BombType.B_CLUSTER,
            BombType.B_RL,
            BombType.B_INFERNO,
            BombType.B_THUNDER,
            BombType.B_FAE_SMALL,
            BombType.B_FAE_LARGE,
            BombType.B_LG,
            BombType.B_ARROW,
            BombType.B_HOMING,
            BombType.B_TAG
    };

    private static final int[] validBotAABombs = {
            BombType.B_RL,
            BombType.B_LAA,
            BombType.B_AAA
    };

    /**
     * External ordnance types that rely on TAG
     */
    private static final Collection<Integer> GUIDED_ORDNANCE = new HashSet<>(
            Arrays.asList(BombType.B_LG, BombType.B_HOMING));

    /**
     * Relative weight distribution of various external ordnance choices for
     * non-pirate forces
     */
    private static final Map<String, Integer> bombMapGroundSpread = Map.ofEntries(
            Map.entry("Normal", castPropertyInt("bombMapGroundSpreadNormal", 6)),
            Map.entry("Anti-Mek", castPropertyInt("bombMapGroundSpreadAnti-Mek", 3)),
            Map.entry("Anti-conventional", castPropertyInt("bombMapGroundSpreadAnti-conventional", 2)),
            Map.entry("Standoff", castPropertyInt("bombMapGroundSpreadStandoff", 1)),
            Map.entry("Strike", castPropertyInt("bombMapGroundSpreadStrike", 2)));

    /**
     * Relative weight distribution of various external ordnance choices for pirate
     * forces
     */
    private static final Map<String, Integer> bombMapPirateGroundSpread = Map.ofEntries(
            Map.entry("Normal", castPropertyInt("bombMapPirateGroundSpreadNormal", 7)),
            Map.entry("Firestorm", castPropertyInt("bombMapPirateGroundSpreadFirestorm", 3)));

    /**
     * Relative weight distribution of general purpose external ordnance choices
     */
    private static final Map<Integer, Integer> normalBombLoad = Map.ofEntries(
            Map.entry(BombType.B_HE, castPropertyInt("normalBombLoad_HE", 40)),
            Map.entry(BombType.B_LG, castPropertyInt("normalBombLoad_LG", 5)),
            Map.entry(BombType.B_CLUSTER, castPropertyInt("normalBombLoad_CLUSTER", 30)),
            Map.entry(BombType.B_INFERNO, castPropertyInt("normalBombLoad_INFERNO", 15)),
            Map.entry(BombType.B_THUNDER, castPropertyInt("normalBombLoad_THUNDER", 10)));

    /**
     * Relative weight distribution of external ordnance choices for use against
     * Meks
     */
    private static final Map<Integer, Integer> antiMekBombLoad = Map.ofEntries(
            Map.entry(BombType.B_HE, castPropertyInt("antiMekBombLoad_HE", 55)),
            Map.entry(BombType.B_LG, castPropertyInt("antiMekBombLoad_LG", 15)),
            Map.entry(BombType.B_INFERNO, castPropertyInt("antiMekBombLoad_INFERNO", 10)),
            Map.entry(BombType.B_THUNDER, castPropertyInt("antiMekBombLoad_THUNDER", 10)),
            Map.entry(BombType.B_HOMING, castPropertyInt("antiMekBombLoad_HOMING", 10)));

    /**
     * Relative weight distribution of external ordnance choices for use against
     * ground vehicles
     * and infantry
     */
    private static final Map<Integer, Integer> antiConvBombLoad = Map.ofEntries(
            Map.entry(BombType.B_CLUSTER, castPropertyInt("antiConvBombLoad_CLUSTER", 50)),
            Map.entry(BombType.B_INFERNO, castPropertyInt("antiConvBombLoad_INFERNO", 40)),
            Map.entry(BombType.B_THUNDER, castPropertyInt("antiConvBombLoad_THUNDER", 8)),
            Map.entry(BombType.B_FAE_SMALL, castPropertyInt("antiConvBombLoad_FAE_SMALL", 2)));

    /**
     * Relative weight distribution of external ordnance choices for providing
     * artillery support
     */
    private static final Map<Integer, Integer> standoffBombLoad = Map.ofEntries(
            Map.entry(BombType.B_ARROW, castPropertyInt("standoffBombLoad_ARROW", 40)),
            Map.entry(BombType.B_HOMING, castPropertyInt("standoffBombLoad_HOMING", 60)));

    /**
     * Relative weight distribution of external ordnance choices for attacking
     * static targets
     */
    private static final Map<Integer, Integer> strikeBombLoad = Map.ofEntries(
            Map.entry(BombType.B_LG, castPropertyInt("strikeBombLoad_LG", 45)),
            Map.entry(BombType.B_HOMING, castPropertyInt("strikeBombLoad_HOMING", 25)),
            Map.entry(BombType.B_HE, castPropertyInt("strikeBombLoad_HE", 30)));

    /**
     * Relative weight distribution of external ordnance choices for low tech
     * forces. Also used as
     * a default/fall-back selection.
     */
    private static final Map<Integer, Integer> lowTechBombLoad = Map.ofEntries(
            Map.entry(BombType.B_HE, castPropertyInt("lowTechBombLoad_HE", 35)),
            Map.entry(BombType.B_RL, castPropertyInt("lowTechBombLoad_RL", 65)));

    /**
     * Relative weight distribution of external ordnance choices for pirates. Low
     * tech, high chaos
     * factor.
     */
    private static final Map<Integer, Integer> pirateBombLoad = Map.ofEntries(
            Map.entry(BombType.B_HE, castPropertyInt("pirateBombLoad_HE", 7)),
            Map.entry(BombType.B_RL, castPropertyInt("pirateBombLoad_RL", 45)),
            Map.entry(BombType.B_INFERNO, castPropertyInt("pirateBombLoad_INFERNO", 35)),
            Map.entry(BombType.B_CLUSTER, castPropertyInt("pirateBombLoad_CLUSTER", 5)),
            Map.entry(BombType.B_FAE_SMALL, castPropertyInt("pirateBombLoad_FAE_SMALL", 6)),
            Map.entry(BombType.B_FAE_LARGE, castPropertyInt("pirateBombLoad_FAE_LARGE", 2)));

    /**
     * External ordnance choices for pirates to set things on fire
     */
    private static final Map<Integer, Integer> pirateFirestormBombLoad = Map.ofEntries(
            Map.entry(BombType.B_INFERNO, castPropertyInt("pirateFirestormBombLoad_INFERNO", 60)),
            Map.entry(BombType.B_FAE_SMALL, castPropertyInt("pirateFirestormBombLoad_FAE_SMALL", 30)),
            Map.entry(BombType.B_FAE_LARGE, castPropertyInt("pirateFirestormBombLoad_FAE_LARGE", 10)));

    /**
     * External ordnance choices for air-to-air combat
     */
    private static final Map<Integer, Integer> antiAirBombLoad = Map.ofEntries(
            Map.entry(BombType.B_RL, castPropertyInt("antiAirBombLoad_RL", 40)),
            Map.entry(BombType.B_LAA, castPropertyInt("antiAirBombLoad_LAA", 40)),
            Map.entry(BombType.B_AAA, castPropertyInt("antiAirBombLoad_AAA", 15)),
            Map.entry(BombType.B_AS, castPropertyInt("antiAirBombLoad_AS", 4)),
            Map.entry(BombType.B_ASEW, castPropertyInt("antiAirBombLoad_ASEW", 1)));

    /**
     * External ordnance choices for attacking DropShips and other large craft
     */
    private static final Map<Integer, Integer> antiShipBombLoad = Map.ofEntries(
            Map.entry(BombType.B_AAA, castPropertyInt("antiShipBombLoad_AAA", 50)),
            Map.entry(BombType.B_AS, castPropertyInt("antiShipBombLoad_AS", 35)),
            Map.entry(BombType.B_ASEW, castPropertyInt("antiShipBombLoad_ASEW", 15)));

    /**
     * External ordnance choices for pirate air-to-air combat. Selects fewer high
     * tech choices than
     * the standard load out.
     */
    private static final Map<Integer, Integer> pirateAirBombLoad = Map.ofEntries(
            Map.entry(BombType.B_RL, castPropertyInt("pirateAntiBombLoad_RL", 60)),
            Map.entry(BombType.B_LAA, castPropertyInt("pirateAntiBombLoad_LAA", 30)),
            Map.entry(BombType.B_AAA, castPropertyInt("pirateAntiBombLoad_AAA", 10)));

    // endsubregion Bombs
    // endregion Constants

    private static Game game;

    protected GameOptions gameOptions = null;
    protected int allowedYear = AbstractUnitSelectorDialog.ALLOWED_YEAR_ANY;
    protected int gameTechLevel = TechConstants.T_SIMPLE_INTRO;
    protected SimpleTechLevel legalLevel;
    protected boolean eraBasedTechLevel = false;
    protected boolean advAeroRules = false;
    protected boolean showExtinct = false;
    protected boolean trueRandom = false;
    protected String defaultBotMunitionsFile = null;

    public TeamLoadOutGenerator(Game ownerGame) {
        game = ownerGame;
        updateOptionValues(game.getOptions());
    }

    public TeamLoadOutGenerator(Game ownerGame, String defaultSettings) {
        this(ownerGame);
        this.defaultBotMunitionsFile = defaultSettings;
    }

    public void updateOptionValues() {
        updateOptionValues(game.getOptions());
    }

    public void updateOptionValues(GameOptions gameOpts) {
        gameOptions = gameOpts;
        allowedYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        gameTechLevel = TechConstants.getSimpleLevel(gameOptions.stringOption(OptionsConstants.ALLOWED_TECHLEVEL));
        legalLevel = SimpleTechLevel.getGameTechLevel(game);
        eraBasedTechLevel = gameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED);
        advAeroRules = gameOptions.booleanOption(OptionsConstants.ADVAERORULES_AERO_ARTILLERY_MUNITIONS);
        showExtinct = gameOptions.booleanOption((OptionsConstants.ALLOWED_SHOW_EXTINCT));
    }

    /**
     * Calculates legality of ammo types given a faction, tech base (IS/CL), mixed
     * tech, and the instance's
     * already-set year, tech level, and option for showing extinct equipment.
     *
     * @param aType     the AmmoType of the munition under consideration. q.v.
     * @param faction   MM-style faction code, per factions.xml and FactionRecord
     *                  keys
     * @param techBase  either 'IS' or 'CL', used for clan boolean check.
     * @param mixedTech makes munitions checks more lenient by allowing faction to
     *                  access both IS and CL tech bases.
     * @return boolean true if legal for combination of inputs, false otherwise.
     *         Determines if an AmmoType is loaded.
     */
    public boolean checkLegality(AmmoType aType, String faction, String techBase, boolean mixedTech) {
        boolean legal = false;
        boolean clan = techBase.equals("CL");

        // Check if tech exists at all (or is explicitly allowed despite being extinct)
        // and whether it is available at the current tech level.
        legal = aType.isAvailableIn(allowedYear, showExtinct)
                && aType.isLegal(allowedYear, legalLevel, clan, mixedTech, showExtinct);

        if (eraBasedTechLevel) {
            // Check if tech is available to this specific faction with the current year and
            // tech base.
            boolean eraBasedLegal = aType.isAvailableIn(allowedYear, clan, ITechnology.getCodeFromMMAbbr(faction));
            if (mixedTech) {
                eraBasedLegal |= aType.isAvailableIn(allowedYear, !clan, ITechnology.getCodeFromMMAbbr(faction));
            }
            legal &= eraBasedLegal;
        }

        // Nukes are not allowed... unless they are!
        legal &= (!aType.hasFlag(AmmoType.F_NUCLEAR)
                || gameOptions.booleanOption(OptionsConstants.ADVAERORULES_AT2_NUKES));

        return legal;
    }

    /**
     * Use values from the Properties file defined in TeamLoadOutGenerator class if
     * available; else use provided default
     *
     * @param field    Field name in property file
     * @param defValue Default value to use
     * @return Double read value or default
     */
    public static Double castPropertyDouble(String field, Double defValue) {
        try {
            return Double.parseDouble(TeamLoadOutGenerator.weightProperties.getProperty(field));
        } catch (Exception ignored) {
            return defValue;
        }
    }

    public static int castPropertyInt(String field, int defValue) {
        try {
            return Integer.parseInt(TeamLoadOutGenerator.weightProperties.getProperty(field));
        } catch (Exception ignored) {
            return defValue;
        }
    }

    public void setTrueRandom(boolean value) {
        trueRandom = value;
    }

    public boolean getTrueRandom() {
        return trueRandom;
    }

    // region Check for various unit types, armor types, etc.
    private static long checkForBombers(ArrayList<Entity> el) {
        return el.stream().filter(Targetable::isBomber).count();
    }

    private static long checkForFliers(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.isAero() || e.hasETypeFlag(Entity.ETYPE_VTOL)).count();
    }

    private static long checkForInfantry(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.isInfantry() && !e.isBattleArmor()).count();
    }

    private static long checkForBattleArmor(ArrayList<Entity> el) {
        return el.stream().filter(Entity::isBattleArmor).count();
    }

    private static long checkForVehicles(ArrayList<Entity> el) {
        return el.stream().filter(BTObject::isVehicle).count();
    }

    private static long checkForMeks(ArrayList<Entity> el) {
        return el.stream().filter(BTObject::isMek).count();
    }

    /**
     * Quick and dirty energy boat calc; useful for selecting Laser-Inhibiting Arrow
     * and heat-based weapons
     *
     * @param el
     * @return
     */
    private static long checkForEnergyBoats(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.tracksHeat() && e.getAmmo().isEmpty()).count();
    }

    /**
     * "Missile Boat" defined here as any unit with half or more weapons dedicated
     * to missiles
     * (This could probably be traded for a weight- or role-based check)
     *
     * @param el
     * @return
     */
    private static long checkForMissileBoats(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getRole().isAnyOf(UnitRole.MISSILE_BOAT) || e.getWeaponList().stream().filter(
                        w -> w.getName().toLowerCase().contains("lrm") ||
                                w.getName().toLowerCase().contains("srm") ||
                                w.getName().toLowerCase().contains("atm") ||
                                w.getName().toLowerCase().contains("mml") ||
                                w.getName().toLowerCase().contains("arrow") ||
                                w.getName().toLowerCase().contains("thunder"))
                        .count() >= e.getWeaponList().size())
                .count();
    }

    private static long checkForTAG(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.hasTAG()).count();
    }

    private static long checkForNARC(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getAmmo().stream().anyMatch(
                        a -> ((AmmoType) a.getType()).getAmmoType() == AmmoType.T_NARC))
                .count();
    }

    private static long checkForAdvancedArmor(ArrayList<Entity> el) {
        // Most units have a location 0
        return el.stream().filter(
                e -> e.getArmorType(0) == ArmorType.T_ARMOR_HARDENED ||
                        e.getArmorType(0) == ArmorType.T_ARMOR_BALLISTIC_REINFORCED ||
                        e.getArmorType(0) == ArmorType.T_ARMOR_REACTIVE ||
                        e.getArmorType(0) == ArmorType.T_ARMOR_BA_REACTIVE ||
                        e.getArmorType(0) == ArmorType.T_ARMOR_FERRO_LAMELLOR)
                .count();
    }

    private static long checkForReflectiveArmor(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getArmorType(0) == ArmorType.T_ARMOR_REFLECTIVE ||
                        e.getArmorType(0) == ArmorType.T_ARMOR_BA_REFLECTIVE)
                .count();
    }

    private static long checkForFireproofArmor(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getArmorType(0) == ArmorType.T_ARMOR_BA_FIRE_RESIST).count();
    }

    private static long checkForFastMovers(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.getOriginalWalkMP() > 5).count();
    }

    private static long checkForOffBoard(ArrayList<Entity> el) {
        return el.stream().filter(
                e -> e.shouldOffBoardDeploy(e.getDeployRound())).count();
    }

    private static long checkForECM(ArrayList<Entity> el) {
        return el.stream().filter(
                Entity::hasECM).count();
    }

    private static long checkForTSM(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.isMek() && ((Mek) e).hasTSM(false)).count();
    }
    // endregion Check for various unit types, armor types, etc.

    // region generateParameters
    public ReconfigurationParameters generateParameters(Team t) {
        ArrayList<Entity> ownTeamEntities = (ArrayList<Entity>) IteratorUtils.toList(game.getTeamEntities(t));
        return generateParameters(game, gameOptions, ownTeamEntities, t.getFaction(), t);
    }

    public ReconfigurationParameters generateParameters(ArrayList<Entity> ownEntities, String ownFaction, Team t) {
        return generateParameters(game, gameOptions, ownEntities, ownFaction, t);
    }

    /**
     * Create the parameters that will determine how to configure ammo loadouts for
     * this team
     *
     * @param g
     * @param gOpts
     * @param ownEntities
     * @param friendlyFaction
     * @param team
     * @return ReconfigurationParameters with information about enemy and friendly
     *         forces
     */
    public static ReconfigurationParameters generateParameters(
            Game g,
            GameOptions gOpts,
            ArrayList<Entity> ownEntities,
            String friendlyFaction,
            Team team) {
        if (ownEntities.isEmpty()) {
            // Nothing to generate
            return new ReconfigurationParameters();
        }
        ArrayList<Entity> etEntities = new ArrayList<Entity>();
        ArrayList<String> enemyFactions = new ArrayList<>();
        for (Team et : g.getTeams()) {
            if (!et.isEnemyOf(team)) {
                continue;
            }
            enemyFactions.add(et.getFaction());
            etEntities.addAll((ArrayList<Entity>) IteratorUtils.toList(g.getTeamEntities(et)));
        }

        return generateParameters(
                g, gOpts, ownEntities, friendlyFaction, etEntities, enemyFactions, ForceDescriptor.RATING_5, 1.0f);
    }

    public static ReconfigurationParameters generateParameters(
            Game g,
            GameOptions gOpts,
            ArrayList<Entity> ownEntities,
            String friendlyFaction,
            ArrayList<Entity> enemyEntities,
            ArrayList<String> enemyFactions,
            int rating,
            float fillRatio) {

        boolean blind = gOpts.booleanOption(OptionsConstants.BASE_BLIND_DROP)
                || gOpts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
        boolean darkEnvironment = g.getPlanetaryConditions().getLight().isDuskOrFullMoonOrMoonlessOrPitchBack();
        boolean groundMap = (g.getMapSettings().getMedium() == MapSettings.MEDIUM_GROUND);
        boolean spaceEnvironment = (g.getMapSettings().getMedium() == MapSettings.MEDIUM_SPACE);

        if (blind) {
            enemyEntities.clear();
        }

        return generateParameters(
                ownEntities,
                enemyEntities,
                friendlyFaction,
                enemyFactions,
                blind,
                darkEnvironment,
                groundMap,
                spaceEnvironment,
                rating,
                fillRatio);
    }

    public static ReconfigurationParameters generateParameters(
            ArrayList<Entity> ownTeamEntities,
            ArrayList<Entity> etEntities,
            String friendlyFaction,
            ArrayList<String> enemyFactions,
            boolean blind,
            boolean darkEnvironment,
            boolean groundMap,
            boolean spaceEnvironment,
            int rating,
            float fillRatio) {
        ReconfigurationParameters reconfigurationParameters = new ReconfigurationParameters();

        // Set own faction and quality rating (default to generic IS if faction is not
        // provided)
        reconfigurationParameters.friendlyFaction = (friendlyFaction == null) ? "IS" : friendlyFaction;
        reconfigurationParameters.friendlyQuality = rating;

        // Fill desired bin fill ratio / percentage (as float)
        reconfigurationParameters.binFillPercent = fillRatio;

        // Get our own side's numbers for comparison
        reconfigurationParameters.friendlyCount = ownTeamEntities.size();

        // Estimate enemy count for ammo count purposes; may include observers. The fog
        // of war!
        reconfigurationParameters.enemyCount = etEntities.size();

        // Record if ground map
        reconfigurationParameters.groundMap = groundMap;
        // Record if space-based environment
        reconfigurationParameters.spaceEnvironment = spaceEnvironment;

        // If our team can see other teams...
        if (!blind) {
            reconfigurationParameters.enemiesVisible = true;
            reconfigurationParameters.enemyFactions.addAll(enemyFactions);
            reconfigurationParameters.enemyFliers += checkForFliers(etEntities);
            reconfigurationParameters.enemyBombers += checkForBombers(etEntities);
            reconfigurationParameters.enemyInfantry += checkForInfantry(etEntities);
            reconfigurationParameters.enemyBattleArmor += checkForBattleArmor(etEntities);
            reconfigurationParameters.enemyVehicles += checkForVehicles(etEntities);
            reconfigurationParameters.enemyMeks += checkForMeks(etEntities);
            reconfigurationParameters.enemyEnergyBoats += checkForEnergyBoats(etEntities);
            // Enemy Missile Boats might be good to know for Retro Streak weighting
            reconfigurationParameters.enemyMissileBoats += checkForMissileBoats(etEntities);
            reconfigurationParameters.enemyAdvancedArmorCount += checkForAdvancedArmor(etEntities);
            reconfigurationParameters.enemyReflectiveArmorCount += checkForReflectiveArmor(etEntities);
            reconfigurationParameters.enemyFireproofArmorCount += checkForFireproofArmor(etEntities);
            reconfigurationParameters.enemyFastMovers += checkForFastMovers(etEntities);
            reconfigurationParameters.enemyOffBoard = checkForOffBoard(etEntities);
            reconfigurationParameters.enemyECMCount = checkForECM(etEntities);
            reconfigurationParameters.enemyTSMCount = checkForTSM(etEntities);
        } else {
            // Assume we know _nothing_ about enemies if Double Blind is on.
            reconfigurationParameters.enemiesVisible = false;
        }

        // Friendly force info
        reconfigurationParameters.friendlyEnergyBoats = checkForEnergyBoats(ownTeamEntities);
        reconfigurationParameters.friendlyMissileBoats = checkForMissileBoats(ownTeamEntities);
        reconfigurationParameters.friendlyTAGs = checkForTAG(ownTeamEntities);
        reconfigurationParameters.friendlyNARCs = checkForNARC(ownTeamEntities);
        reconfigurationParameters.friendlyOffBoard = checkForOffBoard(ownTeamEntities);
        reconfigurationParameters.friendlyECMCount = checkForECM(ownTeamEntities);
        reconfigurationParameters.friendlyInfantry = checkForInfantry(ownTeamEntities);
        reconfigurationParameters.friendlyBattleArmor = checkForBattleArmor(ownTeamEntities);

        // General parameters
        reconfigurationParameters.darkEnvironment = darkEnvironment;

        return reconfigurationParameters;
    }
    // endregion generateParameters

    // region Imperative mutators
    private static void setACImperatives(Entity e, MunitionTree mt,
            ReconfigurationParameters reconfigurationParameters) {
        applyACCaselessImperative(e, mt, reconfigurationParameters);
    }

    private static int getACWeaponCount(Entity e, String size) {
        // Only applies to non-LB-X AC weapons
        return (int) e.getWeaponList().stream()
                .filter(
                        w -> w.getName().toLowerCase()
                                .contains("ac")
                                && !w.getName().toLowerCase()
                                        .contains("lb")
                                && w.getName()
                                        .contains(size))
                .count();
    }

    private static int getACAmmoCount(Entity e, String size) {
        // Only applies to non-LB-X AC weapons
        return (int) e.getAmmo().stream()
                .filter(
                        w -> w.getName().toLowerCase()
                                .contains("ac")
                                && !w.getName().toLowerCase()
                                        .contains("lb")
                                && w.getName()
                                        .contains(size))
                .count();
    }

    private static boolean applyACCaselessImperative(Entity e, MunitionTree mt, ReconfigurationParameters rp) {
        // TODO: remove this block when implementing new anti-ground Aero errata
        // Ignore Aero's, which can't use most alt munitions.
        if (e.isAero()) {
            return false;
        }

        boolean swapped = false;
        Map<String, Double> caliberToRatioMap = Map.of(
                "2", 0.25, // if 1 ton or less per 4 AC/2 barrels,
                "5", 0.5, // if 1 ton or less per 2 AC/5 barrels,
                "10", 1.0, // if 1 ton or less per AC/10 or AC/20 barrel
                "20", 1.0 // Replace existing imperatives with Caseless only.
        );

        // Iterate over any possible Autocannons and update their ammo imperatives if
        // count of bins/barrel
        // is at or below the relevant ratio.
        for (String caliber : caliberToRatioMap.keySet()) {
            int barrelCount = getACWeaponCount(e, caliber);
            int binCount = getACAmmoCount(e, caliber);

            if (barrelCount == 0) {
                // Nothing to do
                continue;
            } else if (((double) binCount) / barrelCount <= caliberToRatioMap.get(caliber)) {
                // Replace any existing imperatives with Caseless as default
                mt.insertImperative(
                        e.getFullChassis(),
                        e.getModel(),
                        "any",
                        "AC/" + caliber,
                        "Caseless");
                swapped = true;
            }
        }

        return swapped;
    }

    private static boolean insertArtemisImperatives(Entity e, MunitionTree mt, String ammoClass) {
        boolean artemis = !(e.getMiscEquipment(MiscType.F_ARTEMIS).isEmpty()
                && e.getMiscEquipment(MiscType.F_ARTEMIS_V).isEmpty());

        if (artemis) {
            for (AmmoMounted bin : e.getAmmo()) {
                if (bin.getName().toUpperCase().contains(ammoClass)) {
                    String binType = bin.getType().getBaseName();
                    mt.insertImperative(e.getFullChassis(), e.getModel(), "any", binType,
                            "Artemis-capable");
                }
            }
            return true;
        }
        return false;
    }

    // Set Artemis LRM carriers to use Artemis LRMs
    private static boolean setLRMImperatives(Entity e, MunitionTree mt, ReconfigurationParameters rp) {
        return insertArtemisImperatives(e, mt, "LRM");
    }

    private static boolean setSRMImperatives(Entity e, MunitionTree mt, ReconfigurationParameters rp) {
        return insertArtemisImperatives(e, mt, "SRM");
    }

    private static boolean setMMLImperatives(Entity e, MunitionTree mt, ReconfigurationParameters rp) {
        return insertArtemisImperatives(e, mt, "MML");
    }
    // region Imperative mutators

    // region generateMunitionTree
    public MunitionTree generateMunitionTree(ReconfigurationParameters rp, Team t) {
        ArrayList<Entity> ownTeamEntities = (ArrayList<Entity>) IteratorUtils.toList(game.getTeamEntities(t));
        return generateMunitionTree(rp, ownTeamEntities, "");
    }

    public static MunitionTree generateMunitionTree(ReconfigurationParameters rp, ArrayList<Entity> entities,
            String defaultSettingsFile) {
        // Based on various requirements from rp, set weights for some ammo types over
        // others
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        return generateMunitionTree(rp, entities, defaultSettingsFile, mwc);
    }

    /**
     * Generate the list of desired ammo load-outs for this team.
     * TODO: implement generateDetailedMunitionTree with more complex breakdowns per
     * unit type
     * NOTE: if sub-classing this generator, should only need to override this
     * method.
     *
     * @param reconfigurationParameters
     * @param defaultSettingsFile
     * @return generated MunitionTree with imperatives for each weapon type
     */
    public static MunitionTree generateMunitionTree(ReconfigurationParameters reconfigurationParameters,
            ArrayList<Entity> ownTeamEntities,
            String defaultSettingsFile, MunitionWeightCollection mwc) {

        // Either create a new tree or, if a defaults file is provided, load that as a
        // base config
        MunitionTree mt = (defaultSettingsFile == null | defaultSettingsFile.isBlank()) ? new MunitionTree()
                : new MunitionTree(defaultSettingsFile);

        // Modify weights for parameters
        if (reconfigurationParameters.darkEnvironment) {
            // Bump munitions that light stuff up
            mwc.increaseIlluminationMunitions();
        } else {
            // decrease weights
            mwc.decreaseIlluminationMunitions();
        }

        // Adjust weights for enemy force composition
        if (reconfigurationParameters.enemiesVisible) {
            // Drop weight of shot-reducing ammo unless this team significantly outnumbers
            // the enemy
            if (!(reconfigurationParameters.friendlyCount >= reconfigurationParameters.enemyCount
                    * castPropertyDouble("mtReducingAmmoReduceIfUnderFactor", 2.0))) {
                // Skip munitions that reduce the number of rounds because we need to shoot a
                // lot!
                mwc.decreaseAmmoReducingMunitions();
            } else if (reconfigurationParameters.friendlyCount >= reconfigurationParameters.enemyCount
                    * castPropertyDouble("mtReducingAmmoIncreaseIfOverFactor", 3.0)) {
                mwc.increaseAmmoReducingMunitions();
            }

            // Flak: bump for any bombers, or fliers > 1/4th of enemy force
            if (reconfigurationParameters.enemyBombers > castPropertyDouble("mtFlakMinBombersExceedThreshold", 0.0)) {
                mwc.increaseFlakMunitions();
            }
            if (reconfigurationParameters.enemyFliers >= reconfigurationParameters.enemyCount
                    / castPropertyDouble("mtFlakEnemyFliersFractionDivisor", 4.0)) {
                mwc.increaseFlakMunitions();
            }
            // Decrease if no bombers or fliers at all
            if (reconfigurationParameters.enemyBombers == 0 && reconfigurationParameters.enemyFliers == 0) {
                mwc.decreaseFlakMunitions();
            }

            // Enemy fast movers make more precise ammo attractive
            if (reconfigurationParameters.enemyFastMovers >= reconfigurationParameters.enemyCount
                    / castPropertyDouble("mtPrecisionAmmoFastEnemyFractionDivisor", 4.0)) {
                mwc.increaseAccurateMunitions();
            }

            // AP munitions are hard-countered by hardened, reactive, etc. armor
            if (reconfigurationParameters.enemyAdvancedArmorCount > castPropertyDouble(
                    "mtHPAmmoAdvArmorEnemiesExceedThreshold", 0.0)
                    && reconfigurationParameters.enemyAdvancedArmorCount > reconfigurationParameters.enemyReflectiveArmorCount) {
                mwc.decreaseAPMunitions();
                mwc.increaseHighPowerMunitions();
            } else if (reconfigurationParameters.enemyReflectiveArmorCount > reconfigurationParameters.enemyAdvancedArmorCount) {
                // But AP munitions really hurt Reflective!
                mwc.increaseAPMunitions();
            }

            // Heat-based weapons kill infantry dead, also vehicles
            // But anti-infantry weapons are generally inferior without infantry targets
            if (reconfigurationParameters.enemyFireproofArmorCount < reconfigurationParameters.enemyCount
                    / castPropertyDouble("mtFireproofMaxEnemyFractionDivisor", 4.0)) {
                if (reconfigurationParameters.enemyInfantry >= reconfigurationParameters.enemyCount
                        / castPropertyDouble("mtInfantryEnemyExceedsFractionDivisor", 4.0)) {
                    mwc.increaseHeatMunitions();
                    mwc.increaseAntiInfMunitions();
                } else {
                    mwc.decreaseAntiInfMunitions();
                }
                if (reconfigurationParameters.enemyVehicles >= reconfigurationParameters.enemyCount
                        / castPropertyDouble("mtVeeEnemyExceedsFractionDivisor", 4.0)) {
                    mwc.increaseHeatMunitions();
                }
                // BAs are proof against some dedicated Anti-Infantry weapons but not
                // heat-generating rounds
                if (reconfigurationParameters.enemyBattleArmor > reconfigurationParameters.enemyCount
                        / castPropertyDouble("mtBAEnemyExceedsFractionDivisor", 4.0)) {
                    mwc.increaseHeatMunitions();
                    mwc.increaseAntiBAMunitions();
                }
            } else if (reconfigurationParameters.enemyFireproofArmorCount >= reconfigurationParameters.enemyCount
                    / castPropertyDouble("mtFireproofMaxEnemyFractionDivisor", 4.0)) {
                if (reconfigurationParameters.enemyInfantry >= reconfigurationParameters.enemyCount
                        / castPropertyDouble("mtInfantryEnemyExceedsFractionDivisor", 4.0)) {
                    mwc.increaseAntiInfMunitions();
                }
                if (reconfigurationParameters.enemyBattleArmor > reconfigurationParameters.enemyCount
                        / castPropertyDouble("mtBAEnemyExceedsFractionDivisor", 4.0)) {
                    mwc.increaseAntiBAMunitions();
                }
                mwc.decreaseHeatMunitions();
            }

            // Energy boats run hot; increase heat munitions and heat-seeking specifically
            if (reconfigurationParameters.enemyEnergyBoats > reconfigurationParameters.enemyCount
                    / castPropertyDouble("mtEnergyBoatEnemyFractionDivisor", 4.0)) {
                mwc.increaseHeatMunitions();
                mwc.increaseHeatMunitions();
                mwc.increaseMunitions(new ArrayList<>(List.of("Heat-Seeking")));
            }

            // Counter EMC by swapping Seeking in for Guided
            if (reconfigurationParameters.enemyECMCount > castPropertyDouble("mtSeekingAmmoEnemyECMExceedThreshold",
                    1.0)) {
                mwc.decreaseGuidedMunitions();
                mwc.increaseSeekingMunitions();
            }
            if (reconfigurationParameters.enemyTSMCount > castPropertyDouble("mtSeekingAmmoEnemyTSMExceedThreshold",
                    1.0)) {
                // Seeking
                mwc.increaseSeekingMunitions();
            }
            if (reconfigurationParameters.enemyECMCount == 0.0 && reconfigurationParameters.enemyTSMCount == 0.0
                    && reconfigurationParameters.enemyEnergyBoats == 0.0) {
                // Seeking munitions are generally situational
                mwc.decreaseSeekingMunitions();
            }
        }

        // Section: Friendly capabilities

        // Guided munitions are worth exponentially more with guidance and supporting
        // missile units
        if (reconfigurationParameters.friendlyTAGs >= castPropertyDouble("mtGuidedAmmoFriendlyTAGThreshold", 1.0)
                || reconfigurationParameters.friendlyNARCs >= castPropertyDouble("mtGuidedAmmoFriendlyNARCThreshold",
                        1.0)) {

            // And worth even more with more guidance around
            if (reconfigurationParameters.friendlyMissileBoats >= reconfigurationParameters.friendlyCount /
                    castPropertyDouble("mtGuidedAmmoFriendlyMissileBoatFractionDivisor", 3.0)) {
                for (int i = 0; i < reconfigurationParameters.friendlyMissileBoats; i++) {
                    mwc.increaseGuidedMunitions();
                }
            }

            // Increase the relevant types depending on the present guidance systems and
            // their counts
            for (int i = 0; i < reconfigurationParameters.friendlyTAGs; i++) {
                mwc.increaseTagGuidedMunitions();
            }
            for (int i = 0; i < reconfigurationParameters.friendlyNARCs; i++) {
                mwc.increaseNARCGuidedMunitions();
            }

            // TAG-guided rounds may have _some_ use, but not as much as base rounds,
            // without TAG support
            if (reconfigurationParameters.friendlyTAGs == 0) {
                mwc.decreaseTagGuidedMunitions();
            }
            // Narc-capable are just not worth it without NARC support
            if (reconfigurationParameters.friendlyNARCs == 0) {
                mwc.zeroMunitionsWeight(new ArrayList<>(List.of("Narc-capable")));
            }
        } else {
            // Expensive waste without guidance
            mwc.zeroMunitionsWeight(GUIDED_MUNITIONS);
        }

        // Downgrade utility munitions unless there are multiple units that could use
        // them; off-board arty
        // in particular
        if (reconfigurationParameters.friendlyOffBoard > castPropertyDouble("mtUtilityAmmoOffBoardUnitsThreshold",
                2.0)) {
            // Only increase utility rounds if we have more off-board units that the other
            // guys
            if (reconfigurationParameters.enemyOffBoard < reconfigurationParameters.friendlyOffBoard /
                    castPropertyDouble("mtUtilityAmmoFriendlyVsEnemyFractionDivisor", 1.0)) {
                mwc.increaseArtilleryUtilityMunitions();
            }
        } else {
            // Reduce utility munition chances if we've only got a lance or so of arty
            mwc.decreaseUtilityMunitions();
        }

        // Just for LOLs: when FS fights CC in 3028 ~ 3050, set Anti-TSM weight to 15.0
        if ((reconfigurationParameters.friendlyFaction != null && reconfigurationParameters.enemyFactions != null)
                && reconfigurationParameters.friendlyFaction.equals("FS")
                && reconfigurationParameters.enemyFactions.contains("CC")
                && (3028 <= reconfigurationParameters.allowedYear && reconfigurationParameters.allowedYear <= 3050)) {
            ArrayList<String> tsmOnly = new ArrayList<String>(List.of("Anti-TSM"));
            mwc.increaseMunitions(tsmOnly);
            mwc.increaseMunitions(tsmOnly);
            mwc.increaseMunitions(tsmOnly);
        }

        // Set nukes to lowest possible weight if user has set the to unusable /for
        // this team/
        // This is a separate mechanism from the legality check.
        if (reconfigurationParameters.nukesBannedForMe) {
            mwc.zeroMunitionsWeight(new ArrayList<>(List.of("Davy Crockett-M", "AlamoMissile Ammo")));
        }

        // L-K Missiles are essentially useless after 3042
        // TODO: add more precise faction and year checks here so Wolf's Dragoons can
        // have them before everybody
        // else.
        if (reconfigurationParameters.allowedYear < 3028 || reconfigurationParameters.allowedYear > 3042) {
            mwc.zeroMunitionsWeight(new ArrayList<>(List.of("Listen-Kill")));
        }

        // The main event!
        // Convert MWC to MunitionsTree for loading
        applyWeightsToMunitionTree(mwc, mt);

        // Handle individual cases like Artemis LRMs, AC/20s with limited ammo, etc.
        for (Entity e : ownTeamEntities) {
            // Set certain imperatives based on weapon types, due to low ammo count / low
            // utility
            setACImperatives(e, mt, reconfigurationParameters);
            setLRMImperatives(e, mt, reconfigurationParameters);
            setSRMImperatives(e, mt, reconfigurationParameters);
            setMMLImperatives(e, mt, reconfigurationParameters);
        }

        return mt;
    }

    /**
     * Turn a selection of the computed munition weights into imperatives to load in
     * the MunitionTree
     *
     * @param mt
     * @param mwc
     * @return
     */
    public static MunitionTree applyWeightsToMunitionTree(MunitionWeightCollection mwc, MunitionTree mt) {
        // Iterate over every entry in the set of top-weighted munitions for each
        // category
        HashMap<String, List<String>> topWeights = mwc.getTopN(
                castPropertyInt("mtTopMunitionsSubsetCount", 8));

        for (Map.Entry<String, List<String>> e : topWeights.entrySet()) {
            StringBuilder sb = new StringBuilder();
            int size = e.getValue().size();
            for (int i = 0; i < size; i++) {
                String[] fields = e.getValue().get(i).split("=");
                // Add the current munition
                sb.append(fields[0]);
                if (i < size - 1) {
                    sb.append(":");
                }
            }

            mt.insertImperative("any", "any", "any", e.getKey(), sb.toString());
        }
        return mt;
    }
    // endregion generateMunitionTree

    // region reconfigureEntities
    /**
     * Wrapper to streamline bot team configuration using standardized defaults
     */
    public void reconfigureBotTeamWithDefaults(Team team, String faction) {
        // Load in some hard-coded defaults now before calculating more.
        reconfigureTeam(team, faction, defaultBotMunitionsFile);
    }

    /**
     * Wrapper to load a file of preset munition imperatives
     *
     * @param team
     * @param faction
     * @param adfFile
     */
    public void reconfigureTeam(Team team, String faction, String adfFile) {
        ReconfigurationParameters reconfigurationParameters = generateParameters(team);
        reconfigurationParameters.allowedYear = allowedYear;

        ArrayList<Entity> updateEntities = (ArrayList<Entity>) IteratorUtils.toList(
                game.getTeamEntities(team));

        MunitionTree mt = generateMunitionTree(reconfigurationParameters, updateEntities, adfFile);
        reconfigureEntities(updateEntities, faction, mt, reconfigurationParameters);
    }

    /**
     * More generic reconfiguration function that acts on sets of units, not teams
     *
     * @param entities ArrayList of entities, including ground and air units
     * @param faction  String code for entities' main faction
     * @param mt       MunitionTree defining all applicable load out imperatives
     */
    public void reconfigureEntities(ArrayList<Entity> entities, String faction, MunitionTree mt,
            ReconfigurationParameters reconfigurationParameters) {
        // For Pirate forces, assume fewer rounds per bin at lower quality levels,
        // minimum 20%
        // If fill ratio is already set, leave it.
        if (reconfigurationParameters.binFillPercent == UNSET_FILL_RATIO) {
            if (reconfigurationParameters.isPirate) {
                reconfigurationParameters.binFillPercent = (float) (Math.min(
                        castPropertyDouble("pirateMaxAllowedBinFillRatio", 1.0),
                        Math.max(
                                castPropertyDouble("pirateMinAllowedBinFillRatio", 0.2),
                                Math.random() / castPropertyDouble("pirateRandomRangeDivisor", 4.0)
                                        + (reconfigurationParameters.friendlyQuality
                                                / castPropertyDouble("pirateQualityDivisor", 8.0)))));
            } else {
                // If we get this far without setting the ratio, but are not pirates, reset to
                // fill
                reconfigurationParameters.binFillPercent = 1.0f;
            }
        }

        ArrayList<Entity> aeroSpaceUnits = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.isAero()) {
                // TODO: Will be used when A2G attack errata are implemented
                aeroSpaceUnits.add(entity);
            } else {
                reconfigureEntity(entity, mt, faction, reconfigurationParameters.binFillPercent);
            }
        }

        populateAeroBombs(
                entities,
                this.allowedYear,
                reconfigurationParameters.groundMap
                        || reconfigurationParameters.enemyCount > reconfigurationParameters.enemyFliers,
                reconfigurationParameters.friendlyQuality,
                reconfigurationParameters.isPirate);
    }

    /**
     * Configure Bot Team with all munitions randomized
     *
     * @param team
     * @param faction
     */
    public void randomizeBotTeamConfiguration(Team team, String faction) {
        ReconfigurationParameters rp = generateParameters(team);
        ArrayList<Entity> updateEntities = (ArrayList<Entity>) IteratorUtils.toList(
                game.getTeamEntities(team));
        reconfigureEntities(updateEntities, faction, generateRandomizedMT(), rp);
    }

    public static MunitionTree generateRandomizedMT() {
        MunitionTree mt = new MunitionTree();
        for (String typeName : TYPE_LIST) {
            mt.insertImperative("any", "any", "any", typeName, "Random");
        }
        return mt;
    }
    // endregion reconfigureEntities

    // region reconfigureEntity

    /**
     * Wrapper that assumes full bins, mostly for testing
     *
     * @param e
     * @param mt
     * @param faction
     */
    public void reconfigureEntity(Entity e, MunitionTree mt, String faction) {
        reconfigureEntity(e, mt, faction, 1.0f);
    }

    /**
     * Method to apply a MunitionTree to a specific unit.
     * Main application logic
     *
     * @param entity
     * @param mt
     * @param faction
     * @param binFillRatio float setting the max fill rate for all bins in this
     *                     entity (mostly for Pirates)
     */
    public void reconfigureEntity(Entity entity, MunitionTree mt, String faction, float binFillRatio) {
        // Create map of bin counts in unit by type
        HashMap<String, List<AmmoMounted>> binLists = new HashMap<>();

        // Populate map with _valid_, _available_ ammo
        for (AmmoMounted ammoBin : entity.getAmmo()) {
            AmmoType aType = ammoBin.getType();
            String sName = ("".equals(aType.getBaseName())) ? ammoBin.getType().getShortName() : aType.getBaseName();

            // Store the actual bins under their types
            if (!binLists.containsKey(sName)) {
                binLists.put(sName, new ArrayList<>());
            }
            binLists.get(sName).add(ammoBin);
        }

        // Iterate over each type and fill it with the requested ammos (as much as
        // possible)
        for (String binName : binLists.keySet()) {
            iterativelyLoadAmmo(entity, mt, binLists.get(binName), binName, faction);
        }

        // Apply requested fill ratio to all final bin loadouts (between max fill and 0)
        clampAmmoShots(entity, binFillRatio);
    }

    /**
     * Applies specified ammo fill ratio to all bins
     *
     * @param entity
     * @param binFillRatio
     */
    protected void clampAmmoShots(Entity entity, float binFillRatio) {
        if (binFillRatio < 1.0f) {
            for (Mounted<AmmoType> ammo : entity.getAmmo()) {
                int maxShots = ammo.getType().getShots();
                ammo.setShotsLeft(Math.min(maxShots, (int) Math.max(0, Math.ceil(binFillRatio * maxShots))));
            }
        }
    }
    // endregion reconfigureEntity

    // region reconfigureAero

    /**
     * TODO: implement in 0.50.1 with other new errata changes
     * This method should mirror reconfigureEntity but with more restrictions based
     * on the types of alternate
     * munitions allowed by Aerospace rules.
     *
     * @param e
     * @param mt
     * @param faction
     */
    public void reconfigureAero(Entity e, MunitionTree mt, String faction) {

    }
    // endregion reconfigureAero

    // region iterativelyLoadAmmo
    private void iterativelyLoadAmmo(
            Entity e, MunitionTree mt, List<AmmoMounted> binList, String binName, String faction) {
        String techBase = (e.isClan()) ? "CL" : "IS";
        iterativelyLoadAmmo(e, mt, binList, binName, techBase, faction);
    }

    /**
     * Manage loading ammo bins for a given type.
     * Type can be designated by size (LRM-5) or generic (AC)
     * Logic:
     * Iterate over list of priorities and fill the first as many times as
     * requested.
     * Repeat for 2nd..Nth ammo types
     * If more bins remain than desired types are specified, fill the remainder with
     * the top priority type
     * If more desired types remain than there are bins, oh well.
     * If a requested ammo type is not available in the specified time frame or
     * faction, skip it.
     *
     * @param e        Entity to load
     * @param mt       MunitionTree, stores required munitions in desired loading
     *                 order
     * @param binList  List of actual mounted ammo bins matching this type
     * @param binName  String bin type we are loading now
     * @param techBase "CL" or "IS"
     * @param faction  Faction to outfit for, used in ammo validity checks (uses MM,
     *                 not IO, faction codes)
     */
    private void iterativelyLoadAmmo(
            Entity e, MunitionTree mt, List<AmmoMounted> binList, String binName, String techBase, String faction) {
        // Copy counts that we will update, otherwise mt entry gets edited permanently.
        HashMap<String, Integer> counts = new HashMap<String, Integer>(
                mt.getCountsOfAmmosForKey(e.getFullChassis(), e.getModel(), e.getCrew().getName(0), binName));
        List<String> priorities = mt.getPriorityList(e.getFullChassis(), e.getModel(), e.getCrew().getName(0), binName);
        // Track default type for filling in unfilled bins
        AmmoType defaultType = null;
        int defaultIdx = 0;

        // If the imperative is to use Random for every bin, we need a different Random
        // for each bin
        if (priorities.size() == 1 && priorities.get(0).contains("Random")) {
            priorities = new ArrayList<>(Collections.nCopies(binList.size(), "Random"));
        }

        for (int i = 0; i < priorities.size() && !binList.isEmpty(); i++) {
            // binName is the weapon to which the bin connects: LRM-15, AC20, SRM, etc.
            // binType is the munition type loaded in currently
            // If all required bins are filled, revert to defaultType
            // If "Random", choose a random ammo type. Availability will be checked later.
            // If not trueRandom, only select from munitions that deal damage

            boolean random = priorities.get(i).contains("Random");
            String binType = (random) ? getRandomBin(binName, trueRandom) : priorities.get(i);
            Mounted<AmmoType> bin = binList.get(0);
            AmmoType desired = null;

            // Load matching AmmoType
            if (binType.toLowerCase().contains("standard")) {
                desired = (AmmoType) EquipmentType.get(techBase + " " + binName + " " + "Ammo");
                if (desired == null) {
                    // Some ammo, like AC/XX ammo, is named funny
                    desired = (AmmoType) EquipmentType.get(techBase + " Ammo " + binName);
                }
            } else {
                // Get available munitions
                Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(((AmmoType) bin.getType()).getAmmoType());
                if (vAllTypes == null) {
                    continue;
                }

                // Make sure the desired munition type is available and valid
                desired = vAllTypes.stream()
                        .filter(m -> m.getInternalName().startsWith(techBase) && m.getBaseName().contains(binName)
                                && m.getName().contains(binType))
                        .filter(d -> checkLegality(d, faction, techBase, e.isMixedTech()))
                        .findFirst().orElse(null);
            }

            if (desired == null) {
                // Couldn't find a bin, move on to the next priority.
                // Update default idx if we're currently setting the default
                defaultIdx = (i == defaultIdx) ? defaultIdx + 1 : defaultIdx;
                continue;
            }

            // Add one of the current binType to counts so we get a new random type every
            // bin
            if (random) {
                counts.put(binType, 1);
            }

            // Store default AmmoType
            if (i == defaultIdx) {
                defaultType = desired;
            }

            // Continue filling with this munition type until count is fulfilled or there
            // are no more bins
            while (counts.getOrDefault(binType, 0) > 0
                    && !binList.isEmpty()) {
                try {
                    // fill one ammo bin with the requested ammo type

                    if (!((AmmoType) bin.getType()).equalsAmmoTypeOnly(desired)) {
                        // can't use this ammo if not
                        logger.debug("Unable to load bin " + bin.getName() + " with " + desired.getName());
                        // Unset default bin if ammo was not loadable
                        if (i == defaultIdx) {
                            defaultType = null;
                            defaultIdx += 1;
                        }
                        break;
                    }
                    // Apply ammo change
                    binList.get(0).changeAmmoType(desired);

                    // Decrement count and remove bin from list
                    counts.put(binType, counts.get(binType) - 1);
                    binList.remove(0);

                } catch (Exception ex) {
                    logger.debug("Error loading ammo bin!", ex);
                    break;
                }
            }
        }

        if (!(defaultType == null || binList.isEmpty())) {
            for (AmmoMounted bin : binList) {
                bin.changeAmmoType(defaultType);
            }
        }
    }

    /**
     * Select a random munition type that is a valid damaging ammo (for "random") or
     * truly random valid ammo
     * (for true random) for the bin type. IE "flechette" is
     *
     * @param binName
     * @param trueRandom
     * @return
     */
    private static String getRandomBin(String binName, boolean trueRandom) {
        String result = "";
        for (String typeName : TYPE_LIST) {
            if ((trueRandom || !UTILITY_MUNITIONS.contains(typeName)) &&
                    (binName.toLowerCase().contains(typeName.toLowerCase())
                            || typeName.toLowerCase().contains(binName.toLowerCase()))) {
                List<String> tList = TYPE_MAP.get(typeName);
                result = tList.get(new Random().nextInt(tList.size()));
                break;
            }
        }
        return result;
    }
    // endregion iterativelyLoadAmmo

    // region aero / bombs
    /**
     * Helper function to load bombs onto a random portion of units that can carry
     * them
     *
     * @param entityList       The list of entities to process
     * @param hasGroundTargets true to select air-to-ground ordnance, false for
     *                         air-to-air only
     * @param quality          IUnitRating enum for force quality (A/A* through F)
     * @param isPirate         true to use specific pirate ordnance loadouts
     */
    public static void populateAeroBombs(List<Entity> entityList,
            int year,
            boolean hasGroundTargets,
            int quality,
            boolean isPirate) {

        // Get all valid bombers, and sort unarmed ones to the front
        // Ignore VTOLs for now, as they suffer extra penalties for mounting bomb
        // munitions
        List<Entity> bomberList = new ArrayList<>();
        for (Entity curEntity : entityList) {
            if (curEntity.isBomber() && !curEntity.isVehicle()) {
                if (!curEntity.getIndividualWeaponList().isEmpty()) {
                    bomberList.add(curEntity);
                } else {
                    bomberList.add(0, curEntity);
                }
            }
        }

        if (bomberList.isEmpty()) {
            return;
        }

        // Some bombers may not be loaded; calculate percentage of total to equip
        int maxBombers = Math.min(
                (int) Math.ceil(((castPropertyInt("percentBombersToEquipMin", 40)
                        + Compute.randomInt(castPropertyInt("percentBombersToEquipRange", 60))) / 100.0)
                        * bomberList.size()),
                bomberList.size());
        int numBombers = 0;

        Map<Integer, int[]> bombsByCarrier = new HashMap<>();

        boolean forceHasGuided = false;
        for (int i = 0; i < bomberList.size(); i++) {
            int minThrust;
            int maxLoad;

            int[] generatedBombs;
            bombsByCarrier.put(i, new int[BombType.B_NUM]);

            // Only generate loadouts up to the maximum number, use empty load out for the
            // rest
            if (numBombers >= maxBombers) {
                continue;
            }

            Entity curBomber = bomberList.get(i);
            boolean isUnarmed = curBomber.getIndividualWeaponList().isEmpty();

            // Some fighters on ground attack may be flying air cover rather than strictly
            // air-to-ground
            boolean isCAP = !hasGroundTargets ||
                    (Compute.d6() <= castPropertyInt("fightersLoadForCAPRollTargetThreshold", 1));

            // Set minimum thrust values, with lower minimums for unarmed and ground attack,
            // and use remaining thrust to limit hard points
            if (isCAP) {
                minThrust = isUnarmed ? castPropertyInt("fighterCAPMinUnarmedSafeThrustValue", 2)
                        : ((int) Math.ceil(curBomber.getWalkMP() /
                                castPropertyDouble("fighterCAPMinArmedSafeThrustFractionDivisor", 2.0)));
            } else {
                minThrust = isUnarmed ? castPropertyInt("bomberMinUnarmedSafeThrustValue", 2)
                        : castPropertyInt("bomberMinArmedSafeThrustValue", 3);
            }
            maxLoad = Math.min((int) Math.floor(
                    curBomber.getWeight() / castPropertyDouble("maxBomberLoadFactorDivisor", 5.0)),
                    (curBomber.getWalkMP() - minThrust) * castPropertyInt("maxBomberLoadThrustDiffFactor", 5));

            // Get a random percentage (default 40 ~ 90) of the maximum bomb load for armed
            // entities
            if (!isUnarmed) {
                maxLoad = (int) Math.ceil(
                        (castPropertyInt("maxPercentBomberLoadToEquipMin", 50) +
                                Compute.randomInt(castPropertyInt("maxPercentBomberLoadToEquipRange", 40))) * maxLoad
                                / 100.0);
            }

            if (maxLoad == 0) {
                continue;
            }

            // Generate bomb load
            generatedBombs = generateExternalOrdnance(
                    maxLoad,
                    isCAP,
                    isPirate,
                    quality,
                    year);
            // Whoops, go yell at the ordnance technician
            if (Arrays.stream(generatedBombs).sum() == 0) {
                continue;
            }

            // Set a flag to indicate at least one of the bombers is carrying guided
            // ordnance
            forceHasGuided = forceHasGuided || hasGuidedOrdnance(generatedBombs);

            // Store the bomb selections as we might need to add in TAG later
            bombsByCarrier.put(i, generatedBombs);

            // Do not increment bomber count for unarmed entities
            if (!isUnarmed) {
                numBombers++;
            }

        }

        loadBombsOntoBombers(bomberList, bombsByCarrier, forceHasGuided);
    }

    private static void loadBombsOntoBombers(List<Entity> bomberList, Map<Integer, int[]> bombsByCarrier,
            boolean forceHasGuided) {
        // Load ordnance onto units. If there is guided ordnance present then randomly
        // add some TAG
        // pods to those without the guided ordnance.
        int tagCount = Math.min(bomberList.size(), Compute.randomInt(
                castPropertyInt("bombersToAddTagMaxCount", 3)));
        for (int i = 0; i < bomberList.size(); i++) {
            Entity curBomber = bomberList.get(i);

            int[] generatedBombs = bombsByCarrier.get(i);

            // Don't combine guided ordnance with external TAG
            if (forceHasGuided && tagCount > 0) {
                int maxLoadForTagger = Math.min((int) Math.floor(
                        curBomber.getWeight() / castPropertyDouble("maxBomberLoadFactorDivisor", 5.0)),
                        (curBomber.getWalkMP() - 2) * castPropertyInt("maxBomberLoadThrustDiffFactor", 5));
                if (addExternalTAG(generatedBombs, true, maxLoadForTagger)) {
                    tagCount--;
                }
            }

            // Load the provided ordnance onto the unit
            if (generatedBombs != null && Arrays.stream(generatedBombs).sum() > 0) {
                ((IBomber) curBomber).setBombChoices(generatedBombs);
            }
        }
    }

    /**
     * Randomly generate a set of external ordnance up to the number of indicated
     * bomb units. Lower
     * rated forces are more likely to get simpler types (HE and rockets).
     * Because TAG is only useful as one-per-fighter, it should be handled
     * elsewhere.
     *
     * @param bombUnits how many bomb units to generate, some types count as more
     *                  than one unit so
     *                  returned counts may be lower than this but never higher
     * @param airOnly   true to only select air-to-air ordnance
     * @param isPirate  true if force is pirate, specific low-tech/high chaos
     *                  selections
     * @param quality   force rating to work with
     * @param year      current year, for tech filter
     * @return array of integers, with each element being a bomb count using
     *         BombUnit
     *         enums as the lookup e.g. [BombUnit.HE] will get the number of HE
     *         bombs.
     */
    private static int[] generateExternalOrdnance(int bombUnits,
            boolean airOnly,
            boolean isPirate,
            int quality,
            int year) {

        int[] bombLoad = new int[BombType.B_NUM];

        if (bombUnits <= 0) {
            return bombLoad;
        }

        // Get a random predefined load out
        double countWeight = 0.0;
        double completeWeight = 0.0;
        double randomThreshold = 0.0;

        // Use weighted random generation for air-to-ground loadouts. Use simple random
        // selection
        // for air-to-air.
        Map<Integer, Integer> bombMap;
        if (!airOnly) {
            bombMap = lowTechBombLoad;

            // Randomly select a load out using the weighted map of names. Pirates use a
            // separate
            // map with different loadouts.
            Map<String, Integer> loadOutMap;
            List<String> mapNames = new ArrayList<>();
            List<Integer> mapWeights = new ArrayList<>();
            if (!isPirate) {
                loadOutMap = bombMapGroundSpread;
            } else {
                loadOutMap = bombMapPirateGroundSpread;
            }
            for (String curName : loadOutMap.keySet()) {
                mapNames.add(curName);
                mapWeights.add(loadOutMap.get(curName));
            }

            // Weighted random selection
            completeWeight = mapWeights.stream().mapToInt(curWeight -> curWeight).asDoubleStream().sum();
            randomThreshold = (Compute.randomInt(castPropertyInt("bomberRandomThresholdMaxPercent", 100))
                    / 100.0) * completeWeight;
            for (int i = 0; i < mapNames.size(); i++) {
                countWeight += Math.max(mapWeights.get(i), 1.0);
                if (countWeight >= randomThreshold) {

                    if (!isPirate) {
                        switch (mapNames.get(i)) {
                            case "Normal":
                                bombMap = normalBombLoad;
                                break;
                            case "Anti-Mek":
                                bombMap = antiMekBombLoad;
                                break;
                            case "Anti-conventional":
                                bombMap = antiConvBombLoad;
                                break;
                            case "Standoff":
                                bombMap = standoffBombLoad;
                                break;
                            case "Strike":
                                bombMap = strikeBombLoad;
                                break;
                            default:
                                break;
                        }
                    } else {
                        switch (mapNames.get(i)) {
                            case "Normal":
                                bombMap = pirateBombLoad;
                                break;
                            case "Firestorm":
                                bombMap = pirateFirestormBombLoad;
                                break;
                            default:
                                break;
                        }
                    }

                    break;
                }
            }

        } else {

            // Air-to-air loadouts are more limited, just use explicit random selection
            if (!isPirate) {
                if (Compute.randomInt(castPropertyInt("fighterCAPRandomPercentageRange", 100)) > castPropertyInt(
                        "fighterCAPAntiShipLoadOutRandomPercentageMax", 20)) {
                    bombMap = antiAirBombLoad;
                } else {
                    bombMap = antiShipBombLoad;
                }
            } else {
                bombMap = pirateAirBombLoad;
            }

        }

        // Slight hack to account for difficulties with isAvailableIn() with certain
        // bombs
        boolean guidedAndArrowAvailable = ((year >= 2600) && (year <= 2835)) || (year > 3044);

        // Generate a working map with all the unavailable ordnance replaced with
        // rockets or HE
        Map<Integer, Integer> workingBombMap = new HashMap<>();
        for (int curBombType : bombMap.keySet()) {
            String typeName = BombType.getBombInternalName(curBombType);
            if (curBombType == BombType.B_RL ||
                    curBombType == BombType.B_HE ||
                    (curBombType != BombType.B_LG &&
                            curBombType != BombType.B_ARROW &&
                            curBombType != BombType.B_HOMING &&
                            BombType.get(typeName).isAvailableIn(year, false))
                    ||
                    ((curBombType == BombType.B_LG ||
                            curBombType == BombType.B_ARROW ||
                            curBombType == BombType.B_HOMING) &&
                            guidedAndArrowAvailable)) {

                if (workingBombMap.containsKey(curBombType)) {
                    workingBombMap.put(curBombType, bombMap.get(curBombType) +
                            workingBombMap.get(curBombType));
                } else {
                    workingBombMap.put(curBombType, bombMap.get(curBombType));
                }

            } else {
                int replacementBomb = airOnly ? BombType.B_RL
                        : Compute.randomInt(castPropertyInt("bombReplacementIntRange", 2)) <= castPropertyInt(
                                "bombReplacementRLThreshold", 0)
                                        ? BombType.B_RL
                                        : BombType.B_HE;
                if (workingBombMap.containsKey(replacementBomb)) {
                    workingBombMap.put(replacementBomb, bombMap.get(curBombType) +
                            workingBombMap.get(replacementBomb));
                } else {
                    workingBombMap.put(replacementBomb, bombMap.get(curBombType));
                }

            }
        }

        // Generate enough bombs to meet the desired count

        int selectedBombType = -1;
        int loopSafety = 0;

        List<Integer> ordnanceIDs = new ArrayList<>();
        List<Integer> ordnanceRandomWeights = new ArrayList<>();
        for (int curID : workingBombMap.keySet()) {
            ordnanceIDs.add(curID);
            ordnanceRandomWeights.add(workingBombMap.get(curID));
        }
        completeWeight = ordnanceRandomWeights.stream().mapToInt(curWeight -> Math.max(curWeight, 1)).asDoubleStream()
                .sum();

        for (int curLoad = 0; curLoad < bombUnits && loopSafety < castPropertyInt("maxBombApplicationLoopCount", 10);) {

            // Randomly get the ordnance type
            randomThreshold = (Compute.randomInt(
                    castPropertyInt("maxBombOrdnanceWeightPercentThreshold", 100)) / 100.0) * completeWeight;
            countWeight = 0.0;
            for (int i = 0; i < ordnanceIDs.size(); i++) {
                countWeight += Math.max(ordnanceRandomWeights.get(i), 1.0);
                if (countWeight >= randomThreshold) {
                    selectedBombType = ordnanceIDs.get(i);
                    break;
                }
            }

            // If the selected ordnance doesn't exceed the provided limit increment the
            // counter,
            // otherwise skip it and keep trying with some safeties to prevent infinite
            // loops.
            if (selectedBombType >= 0 &&
                    curLoad + BombType.getBombCost(selectedBombType) <= bombUnits) {
                bombLoad[selectedBombType]++;
                curLoad += BombType.getBombCost(selectedBombType);
            } else {
                loopSafety++;
            }
        }

        // Oops, nothing left - rocket launchers are always popular
        if (Arrays.stream(bombLoad).sum() == 0) {
            bombLoad[BombType.B_RL] = bombUnits;
            return bombLoad;
        }

        // Randomly replace advanced ordnance with rockets or HE, depending on force
        // rating and
        // air-air/ground preference

        List<Integer> advancedOrdnance = Arrays.asList(
                BombType.B_LG,
                BombType.B_ARROW,
                BombType.B_HOMING,
                BombType.B_LAA,
                BombType.B_AAA,
                BombType.B_THUNDER,
                BombType.B_FAE_SMALL,
                BombType.B_FAE_LARGE,
                BombType.B_AS,
                BombType.B_ASEW);

        switch (quality) {
            case ForceDescriptor.RATING_5:
            case ForceDescriptor.RATING_4:
                randomThreshold = castPropertyInt("bombRandomReplaceRating4PlusThreshold", 5);
                break;
            case ForceDescriptor.RATING_3:
                randomThreshold = castPropertyInt("bombRandomReplaceRating3PlusThreshold", 10);
                break;
            case ForceDescriptor.RATING_2:
                randomThreshold = castPropertyInt("bombRandomReplaceRating2PlusThreshold", 25);
                break;
            case ForceDescriptor.RATING_1:
                randomThreshold = castPropertyInt("bombRandomReplaceRating1PlusThreshold", 40);
                break;
            case ForceDescriptor.RATING_0:
                randomThreshold = castPropertyInt("bombRandomReplaceRating0PlusThreshold", 80);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized rating value: " + quality);
        }

        for (int curBomb : advancedOrdnance) {
            int loadCount = bombLoad[curBomb];

            for (int i = 0; i < loadCount; i++) {
                if (Compute.randomInt(100) < randomThreshold) {
                    if (airOnly) {
                        bombLoad[BombType.B_RL]++;
                    } else {
                        bombLoad[Compute.randomInt(
                                castPropertyInt("bombReplacementIntRange", 2)) <= castPropertyInt(
                                        "bombReplacementRLThreshold", 0)
                                                ? BombType.B_RL
                                                : BombType.B_HE]++;
                    }
                    bombLoad[curBomb]--;
                }
            }
        }
        return bombLoad;
    }

    /**
     * Checks to see if a bomb load contains ordnance that relies on TAG guidance,
     * such as laser/TAG
     * guided bombs and homing Arrow IV
     *
     * @param bombLoad array of size BombType.B_NUM, suitable for setting bombs on
     *                 IBomber entities
     * @return true if guided ordnance is carried
     */
    private static boolean hasGuidedOrdnance(int[] bombLoad) {
        if (bombLoad.length < Collections.max(GUIDED_ORDNANCE)) {
            throw new IllegalArgumentException("Invalid array LENGTH for bombLoad parameter.");
        }

        for (int curHomingBomb : GUIDED_ORDNANCE) {
            if (bombLoad[curHomingBomb] > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Updates a bomb load to include an external TAG system. If this exceeds the
     * provided
     * maximum load (in bomb units i.e. Arrow IV counts as multiple units), then one
     * of the basic
     * one-slot types is removed and the TAG system is added in its place.
     *
     * @param bombLoad   array of size BombType.B_NUM, suitable for setting bombs on
     *                   IBomber
     *                   entities
     * @param skipGuided true to only select external TAG for units without guided
     *                   ordnance
     * @param maxLoad    Maximum external ordnance load in total bomb units (NOT
     *                   bomb count)
     * @return true, if TAG was added, false otherwise
     */
    private static boolean addExternalTAG(int[] bombLoad, boolean skipGuided, int maxLoad) {
        if (bombLoad.length < BombType.B_NUM) {
            throw new IllegalArgumentException("Invalid array length for bombLoad parameter.");
        }

        if (!skipGuided || !hasGuidedOrdnance(bombLoad)) {

            // If there's enough room, add it
            int totalLoad = IntStream.range(0, bombLoad.length)
                    .map(i -> BombType.getBombCost(i) * Math.max(bombLoad[i], 0)).sum();
            if (totalLoad < maxLoad) {
                bombLoad[BombType.B_TAG]++;
                return true;
            } else if (totalLoad == maxLoad) {

                List<Integer> replaceableTypes = Arrays.asList(
                        BombType.B_RL,
                        BombType.B_HE,
                        BombType.B_INFERNO,
                        BombType.B_CLUSTER);
                for (int i = 0; i < replaceableTypes.size(); i++) {
                    if (bombLoad[i] > 0) {
                        bombLoad[i]--;
                        bombLoad[BombType.B_TAG]++;
                        return true;
                    }
                }

            } else {
                // Already overloaded, don't bother
                return false;
            }

            bombLoad[BombType.B_TAG]++;
            return true;
        }

        return false;
    }

    // endregion aero / bombs
}

// region MunitionWeightCollection
class MunitionWeightCollection {
    private HashMap<String, Double> lrmWeights;
    private HashMap<String, Double> srmWeights;
    private HashMap<String, Double> acWeights;
    private HashMap<String, Double> atmWeights;
    private HashMap<String, Double> arrowWeights;
    private HashMap<String, Double> artyWeights;
    private HashMap<String, Double> artyCannonWeights;
    private HashMap<String, Double> mekMortarWeights;
    private HashMap<String, Double> narcWeights;
    private HashMap<String, Double> bombWeights;
    private Map<String, HashMap<String, Double>> mapTypeToWeights;

    MunitionWeightCollection() {
        resetWeights();
    }

    public void resetWeights() {
        // Initialize weights for all the weapon types using known munition names
        lrmWeights = initializeMissileWeaponWeights(MunitionTree.LRM_MUNITION_NAMES);
        srmWeights = initializeMissileWeaponWeights(MunitionTree.SRM_MUNITION_NAMES);
        acWeights = initializeWeaponWeights(MunitionTree.AC_MUNITION_NAMES);
        // ATMs are treated differently
        atmWeights = initializeATMWeights(MunitionTree.ATM_MUNITION_NAMES);
        arrowWeights = initializeWeaponWeights(MunitionTree.ARROW_MUNITION_NAMES);
        artyWeights = initializeWeaponWeights(MunitionTree.ARTILLERY_MUNITION_NAMES);
        artyCannonWeights = initializeWeaponWeights(MunitionTree.ARTILLERY_CANNON_MUNITION_NAMES);
        mekMortarWeights = initializeWeaponWeights(MunitionTree.MEK_MORTAR_MUNITION_NAMES);
        narcWeights = initializeWeaponWeights(MunitionTree.NARC_MUNITION_NAMES);
        bombWeights = initializeWeaponWeights(MunitionTree.BOMB_MUNITION_NAMES);

        mapTypeToWeights = new HashMap<>(Map.ofEntries(
                entry("LRM", lrmWeights),
                entry("SRM", srmWeights),
                entry("AC", acWeights),
                entry("ATM", atmWeights),
                entry("Arrow IV", arrowWeights),
                entry("Artillery", artyWeights),
                entry("Artillery Cannon", artyCannonWeights),
                entry("Mek Mortar", mekMortarWeights),
                entry("Narc", narcWeights),
                entry("Bomb", bombWeights)));
    }

    /**
     * Use values from the Properties file defined in TeamLoadOutGenerator class if
     * available; else use provided default
     *
     * @param field    Field name in property file
     * @param defValue Default value to use
     * @return Double read value or default
     */
    private static Double getPropDouble(String field, Double defValue) {
        return TeamLoadOutGenerator.castPropertyDouble(field, defValue);
    }

    // Section: initializing weights
    private static HashMap<String, Double> initializeWeaponWeights(List<String> wepAL) {
        HashMap<String, Double> weights = new HashMap<String, Double>();
        for (String name : wepAL) {
            weights.put(name, getPropDouble("defaultWeaponWeight", 1.0));
        }
        // Every weight list should have a Standard set as weight 2.0
        weights.put("Standard", getPropDouble("defaultStandardMunitionWeight", 2.0));
        return weights;
    }

    private static HashMap<String, Double> initializeMissileWeaponWeights(List<String> wepAL) {
        HashMap<String, Double> weights = new HashMap<String, Double>();
        for (String name : wepAL) {
            weights.put(name, getPropDouble("defaultWeaponWeight", 1.0));
        }
        // Every missile weight list should have a Standard set as weight 2.0
        weights.put("Standard", getPropDouble("defaultMissileStandardMunitionWeight", 2.0));
        // Dead-Fire should be even higher to start
        weights.put("Dead-Fire", getPropDouble("defaultDeadFireMunitionWeight", 3.0));
        // Artemis should be zeroed; Artemis-equipped launchers will be handled
        // separately
        weights.put("Artemis-capable", getPropDouble("defaultArtemisCapableMunitionWeight", 0.0));
        return weights;
    }

    private static HashMap<String, Double> initializeATMWeights(List<String> wepAL) {
        HashMap<String, Double> weights = new HashMap<String, Double>();
        for (String name : wepAL) {
            weights.put(name, getPropDouble("defaultATMMunitionWeight", 2.0));
        }
        // ATM Standard ammo is weighted lower due to overlap with HE and ER
        weights.put("Standard", getPropDouble("defaultATMStandardWeight", 1.0));
        return weights;
    }

    // Increase/Decrease functions. Increase is 2x + 1, decrease is 0.5x, so items
    // voted up and down multiple times should still exceed items never voted up
    // _or_ down.
    public void increaseMunitions(List<String> munitions) {
        mapTypeToWeights.entrySet().forEach(
                e -> modifyMatchingWeights(
                        e.getValue(),
                        munitions,
                        getPropDouble("increaseWeightFactor", 2.0),
                        getPropDouble("increaseWeightIncrement", 1.0)));
    }

    public void decreaseMunitions(List<String> munitions) {
        mapTypeToWeights.entrySet().forEach(
                e -> modifyMatchingWeights(
                        e.getValue(),
                        munitions,
                        getPropDouble("decreaseWeightFactor", 0.5),
                        getPropDouble("decreaseWeightDecrement", 0.0)));
    }

    public void zeroMunitionsWeight(List<String> munitions) {
        mapTypeToWeights.entrySet().forEach(
                e -> modifyMatchingWeights(
                        e.getValue(), munitions, 0.0, 0.0));
    }

    public void increaseAPMunitions() {
        increaseMunitions(TeamLoadOutGenerator.AP_MUNITIONS);
    }

    public void decreaseAPMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.AP_MUNITIONS);
    }

    public void increaseFlakMunitions() {
        increaseMunitions(TeamLoadOutGenerator.FLAK_MUNITIONS);
    }

    public void decreaseFlakMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.FLAK_MUNITIONS);
    }

    public void increaseAccurateMunitions() {
        increaseMunitions(TeamLoadOutGenerator.ACCURATE_MUNITIONS);
    }

    public void decreaseAccurateMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.ACCURATE_MUNITIONS);
    }

    public void increaseAntiInfMunitions() {
        increaseMunitions(TeamLoadOutGenerator.ANTI_INF_MUNITIONS);
    }

    public void decreaseAntiInfMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.ANTI_INF_MUNITIONS);
    }

    public void increaseAntiBAMunitions() {
        increaseMunitions(TeamLoadOutGenerator.ANTI_BA_MUNITIONS);
    }

    public void decreaseAntiBAMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.ANTI_BA_MUNITIONS);
    }

    public void increaseHeatMunitions() {
        increaseMunitions(TeamLoadOutGenerator.HEAT_MUNITIONS);
    }

    public void decreaseHeatMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.HEAT_MUNITIONS);
    }

    public void increaseIlluminationMunitions() {
        increaseMunitions(TeamLoadOutGenerator.ILLUMINATION_MUNITIONS);
    }

    public void decreaseIlluminationMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.ILLUMINATION_MUNITIONS);
    }

    public void increaseUtilityMunitions() {
        increaseMunitions(TeamLoadOutGenerator.UTILITY_MUNITIONS);
    }

    public void decreaseUtilityMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.UTILITY_MUNITIONS);
    }

    public void increaseArtilleryUtilityMunitions() {
        modifyMatchingWeights(
                mapTypeToWeights.get("Artillery"),
                TeamLoadOutGenerator.UTILITY_MUNITIONS,
                getPropDouble("increaseWeightFactor", 2.0),
                getPropDouble("increaseWeightDecrement", 1.0));
    }

    public void decreaseArtilleryUtilityMunitions() {
        modifyMatchingWeights(
                mapTypeToWeights.get("Artillery"),
                TeamLoadOutGenerator.UTILITY_MUNITIONS,
                getPropDouble("decreaseWeightFactor", 0.5),
                getPropDouble("decreaseWeightDecrement", 0.0));
    }

    public void increaseGuidedMunitions() {
        increaseMunitions(TeamLoadOutGenerator.GUIDED_MUNITIONS);
    }

    public void increaseTagGuidedMunitions() {
        increaseMunitions(TeamLoadOutGenerator.TAG_GUIDED_MUNITIONS);
    }

    public void increaseNARCGuidedMunitions() {
        increaseMunitions(TeamLoadOutGenerator.NARC_GUIDED_MUNITIONS);
    }

    public void decreaseGuidedMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.GUIDED_MUNITIONS);
    }

    public void decreaseTagGuidedMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.TAG_GUIDED_MUNITIONS);
    }

    public void decreaseNARCGuidedMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.NARC_GUIDED_MUNITIONS);
    }

    public void increaseAmmoReducingMunitions() {
        increaseMunitions(TeamLoadOutGenerator.AMMO_REDUCING_MUNITIONS);
    }

    public void decreaseAmmoReducingMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.AMMO_REDUCING_MUNITIONS);
    }

    public void increaseSeekingMunitions() {
        increaseMunitions(TeamLoadOutGenerator.SEEKING_MUNITIONS);
    }

    public void decreaseSeekingMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.SEEKING_MUNITIONS);
    }

    public void increaseHighPowerMunitions() {
        increaseMunitions(TeamLoadOutGenerator.HIGH_POWER_MUNITIONS);
    }

    public void decreaseHighPowerMunitions() {
        decreaseMunitions(TeamLoadOutGenerator.HIGH_POWER_MUNITIONS);
    }

    /**
     * Update all matching types in a category by multiplying by a factor and adding
     * an increment
     * (1.0, 0.0) = no change; (2.0, 0.0) = double, (0.5, 0.0) = halve,
     * (1.0, 1.0) = increment by 1, (1.0, -1.0) = decrement by 1, etc.
     *
     * @param current
     * @param types
     * @param factor
     * @param increment
     */
    private static void modifyMatchingWeights(HashMap<String, Double> current,
            List<String> types, double factor,
            double increment) {
        for (String key : types) {
            if (current.containsKey(key)) {
                current.put(key, current.get(key) * factor + increment);
            }
        }
    }

    public List<String> getMunitionTypesInWeightOrder(Map<String, Double> weightMap) {
        ArrayList<String> orderedTypes = new ArrayList<>();
        weightMap.entrySet().stream()
                .sorted((E1, E2) -> E2.getValue().compareTo(E1.getValue()))
                .forEach(k -> orderedTypes.add(String.valueOf(k)));
        // Make Standard the first entry if tied with other highest-weight munitions
        for (String munitionString : orderedTypes) {
            if (munitionString.contains("Standard") && !orderedTypes.get(0).contains("Standard")) {
                int idx = orderedTypes.indexOf(munitionString);
                if (weightMap.get("Standard") == Double.parseDouble(orderedTypes.get(0).split("=")[1])) {
                    Collections.swap(orderedTypes, idx, 0);
                    break;
                }
            }
        }
        return orderedTypes;
    }

    public HashMap<String, List<String>> getTopN(int count) {
        HashMap<String, List<String>> topMunitionsMap = new HashMap<>();
        for (String key : TeamLoadOutGenerator.TYPE_MAP.keySet()) {
            List<String> orderedList = getMunitionTypesInWeightOrder(mapTypeToWeights.get(key));
            topMunitionsMap.put(key, (orderedList.size() >= count) ? orderedList.subList(0, count) : orderedList);
        }
        return topMunitionsMap;
    }

    public HashMap<String, Double> getLrmWeights() {
        return lrmWeights;
    }

    public HashMap<String, Double> getSrmWeights() {
        return srmWeights;
    }

    public HashMap<String, Double> getAcWeights() {
        return acWeights;
    }

    public HashMap<String, Double> getAtmWeights() {
        return atmWeights;
    }

    public HashMap<String, Double> getArrowWeights() {
        return arrowWeights;
    }

    public HashMap<String, Double> getArtyWeights() {
        return artyWeights;
    }

    public HashMap<String, Double> getBombWeights() {
        return bombWeights;
    }

    public HashMap<String, Double> getArtyCannonWeights() {
        return artyCannonWeights;
    }

    public HashMap<String, Double> getMekMortarWeights() {
        return mekMortarWeights;
    }
}
// endregion MunitionWeightCollection
