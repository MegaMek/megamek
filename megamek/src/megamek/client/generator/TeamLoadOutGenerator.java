/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.generator;

import static java.util.Map.entry;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ui.dialogs.unitSelectorDialogs.AbstractUnitSelectorDialog;
import megamek.common.SimpleTechLevel;
import megamek.common.Team;
import megamek.common.TechConstants;
import megamek.common.compute.Compute;
import megamek.common.containers.MunitionTree;
import megamek.common.enums.Faction;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.BombType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.game.Game;
import megamek.common.loaders.MapSettings;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BTObject;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.UnitRole;
import megamek.logging.MMLogger;

/**
 * Notes: check out - RATGenerator.java - ForceDescriptor.java for era-based search examples
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
            logger.debug(e, "{} was not loaded: ", LOAD_OUT_SETTINGS_PATH);
        }
    }

    public static float UNSET_FILL_RATIO = Float.NEGATIVE_INFINITY;

    public static final ArrayList<String> AP_MUNITIONS = new ArrayList<>(List.of("Armor-Piercing", "Tandem-Charge"));

    public static final ArrayList<String> FLAK_MUNITIONS = new ArrayList<>(List.of("ADA",
          "Cluster",
          "Flak",
          "AAAMissile Ammo",
          "LAAMissile Ammo"));

    public static final ArrayList<String> ACCURATE_MUNITIONS = new ArrayList<>(List.of("Precision"));

    public static final ArrayList<String> HIGH_POWER_MUNITIONS = new ArrayList<>(List.of("Tandem-Charge",
          "Fuel-Air",
          "HE",
          "Dead-Fire",
          "Davy Crockett-M",
          "ASMissile Ammo",
          "FABombLarge Ammo",
          "FABombSmall Ammo",
          "AlamoMissile Ammo"));

    public static final ArrayList<String> ANTI_INF_MUNITIONS = new ArrayList<>(List.of("Inferno",
          "Fragmentation",
          "Flechette",
          "Fuel-Air",
          "Anti-personnel",
          "Acid",
          "FABombSmall Ammo",
          "ClusterBomb",
          "HEBomb"));

    public static final ArrayList<String> ANTI_BA_MUNITIONS = new ArrayList<>(List.of("Inferno",
          "Fuel-Air",
          "Tandem-Charge",
          "Acid",
          "FABombSmall Ammo",
          "HEBomb"));

    public static final ArrayList<String> HEAT_MUNITIONS = new ArrayList<>(List.of("Inferno",
          "Incendiary",
          "InfernoBomb"));

    public static final ArrayList<String> ILLUMINATION_MUNITIONS = new ArrayList<>(List.of("Illumination",
          "Tracer",
          "Inferno",
          "Incendiary",
          "Flare",
          "InfernoBomb"));

    public static final ArrayList<String> UTILITY_MUNITIONS = new ArrayList<>(List.of("Illumination",
          "Smoke",
          "Mine Clearance",
          "Anti-TSM",
          "Laser Inhibiting",
          "Thunder",
          "FASCAM",
          "Thunder-Active",
          "Thunder-Augmented",
          "Thunder-Vibrabomb",
          "Thunder-Inferno",
          "Flare",
          "ThunderBomb",
          "TAGBomb",
          "TorpedoBomb",
          "ASEWMissile Ammo"));

    // Guided munitions come in two main flavors
    public static final ArrayList<String> GUIDED_MUNITIONS = new ArrayList<>(List.of("Semi-Guided",
          "Narc-capable",
          "Homing",
          "Copperhead",
          "LGBomb",
          "ArrowIVHomingMissile Ammo"));
    public static final ArrayList<String> TAG_GUIDED_MUNITIONS = new ArrayList<>(List.of("Semi-Guided",
          "Homing",
          "Copperhead",
          "LGBomb",
          "ArrowIVHomingMissile Ammo"));
    public static final ArrayList<String> NARC_GUIDED_MUNITIONS = new ArrayList<>(List.of("Narc-capable"));

    // TODO Anti-Radiation Missiles See IO pg 62 (TO 368)
    public static final ArrayList<String> SEEKING_MUNITIONS = new ArrayList<>(List.of("Heat-Seeking",
          "Listen-Kill",
          "Swarm",
          "Swarm-I",
          "Anti-Radiation"));

    public static final ArrayList<String> AMMO_REDUCING_MUNITIONS = new ArrayList<>(List.of("Acid",
          "Laser Inhibiting",
          "Follow The Leader",
          "Heat-Seeking",
          "Tandem-Charge",
          "Thunder-Active",
          "Thunder-Augmented",
          "Thunder-Vibrabomb",
          "Thunder-Inferno",
          "AAAMissile Ammo",
          "ASMissile Ammo",
          "ASWEMissile Ammo",
          "ArrowIVMissile Ammo",
          "AlamoMissile Ammo",
          "Precision",
          "Armor-Piercing"));

    public static final ArrayList<String> TYPE_LIST = new ArrayList<>(List.of("LRM",
          "SRM",
          "AC",
          "ATM",
          "Arrow IV",
          "Artillery",
          "Artillery Cannon",
          "Mek Mortar",
          "Narc",
          "Bomb"));

    public static final Map<String, List<String>> TYPE_MAP = Map.ofEntries(entry("LRM",
                MunitionTree.LRM_MUNITION_NAMES),
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
    private static final Set<BombTypeEnum> validBotBombs = Set.of(BombTypeEnum.HE,
          BombTypeEnum.CLUSTER,
          BombTypeEnum.RL,
          BombTypeEnum.INFERNO,
          BombTypeEnum.THUNDER,
          BombTypeEnum.FAE_SMALL,
          BombTypeEnum.FAE_LARGE,
          BombTypeEnum.LG,
          BombTypeEnum.ARROW,
          BombTypeEnum.HOMING,
          BombTypeEnum.TAG);

    private static final Set<BombTypeEnum> validBotAABombs = Set.of(BombTypeEnum.RL,
          BombTypeEnum.LAA,
          BombTypeEnum.AAA);


    /**
     * Relative weight distribution of various external ordnance choices for non-pirate forces
     */
    private static final Map<String, Integer> bombMapGroundSpread = Map.ofEntries(Map.entry("Normal",
                castPropertyInt("bombMapGroundSpreadNormal", 6)),
          Map.entry("Anti-Mek", castPropertyInt("bombMapGroundSpreadAnti-Mek", 3)),
          Map.entry("Anti-conventional", castPropertyInt("bombMapGroundSpreadAnti-conventional", 2)),
          Map.entry("Standoff", castPropertyInt("bombMapGroundSpreadStandoff", 1)),
          Map.entry("Strike", castPropertyInt("bombMapGroundSpreadStrike", 2)));

    /**
     * Relative weight distribution of various external ordnance choices for pirate forces
     */
    private static final Map<String, Integer> bombMapPirateGroundSpread = Map.ofEntries(Map.entry("Normal",
                castPropertyInt("bombMapPirateGroundSpreadNormal", 7)),
          Map.entry("Firestorm", castPropertyInt("bombMapPirateGroundSpreadFirestorm", 3)));

    /**
     * Relative weight distribution of general purpose external ordnance choices
     */
    private static final BombLoadout normalBombLoad = new BombLoadout() {{
        put(BombTypeEnum.HE, castPropertyInt("normalBombLoad_HE", 40));
        put(BombTypeEnum.LG, castPropertyInt("normalBombLoad_LG", 5));
        put(BombTypeEnum.CLUSTER, castPropertyInt("normalBombLoad_CLUSTER", 30));
        put(BombTypeEnum.INFERNO, castPropertyInt("normalBombLoad_INFERNO", 15));
        put(BombTypeEnum.THUNDER, castPropertyInt("normalBombLoad_THUNDER", 10));
    }};

    /**
     * Relative weight distribution of external ordnance choices for use against Meks
     */
    private static final BombLoadout antiMekBombLoad = new BombLoadout() {{
        put(BombTypeEnum.HE, castPropertyInt("antiMekBombLoad_HE", 55));
        put(BombTypeEnum.LG, castPropertyInt("antiMekBombLoad_LG", 15));
        put(BombTypeEnum.INFERNO, castPropertyInt("antiMekBombLoad_INFERNO", 10));
        put(BombTypeEnum.THUNDER, castPropertyInt("antiMekBombLoad_THUNDER", 10));
        put(BombTypeEnum.HOMING, castPropertyInt("antiMekBombLoad_HOMING", 10));
    }};

    /**
     * Relative weight distribution of external ordnance choices for use against ground vehicles and infantry
     */
    private static final BombLoadout antiConvBombLoad = new BombLoadout() {{
        put(BombTypeEnum.CLUSTER, castPropertyInt("antiConvBombLoad_CLUSTER", 50));
        put(BombTypeEnum.INFERNO, castPropertyInt("antiConvBombLoad_INFERNO", 40));
        put(BombTypeEnum.THUNDER, castPropertyInt("antiConvBombLoad_THUNDER", 8));
        put(BombTypeEnum.FAE_SMALL, castPropertyInt("antiConvBombLoad_FAE_SMALL", 2));
    }};

    /**
     * Relative weight distribution of external ordnance choices for providing artillery support
     */
    private static final BombLoadout standoffBombLoad = new BombLoadout() {{
        put(BombTypeEnum.ARROW, castPropertyInt("standoffBombLoad_ARROW", 40));
        put(BombTypeEnum.HOMING, castPropertyInt("standoffBombLoad_HOMING", 60));
    }};

    /**
     * Relative weight distribution of external ordnance choices for attacking static targets
     */
    private static final BombLoadout strikeBombLoad = new BombLoadout() {{
        put(BombTypeEnum.LG, castPropertyInt("strikeBombLoad_LG", 45));
        put(BombTypeEnum.HOMING, castPropertyInt("strikeBombLoad_HOMING", 25));
        put(BombTypeEnum.HE, castPropertyInt("strikeBombLoad_HE", 30));
    }};

    /**
     * Relative weight distribution of external ordnance choices for low tech forces. Also used as a default/fall-back
     * selection.
     */
    private static final BombLoadout lowTechBombLoad = new BombLoadout() {{
        put(BombTypeEnum.HE, castPropertyInt("lowTechBombLoad_HE", 35));
        put(BombTypeEnum.RL, castPropertyInt("lowTechBombLoad_RL", 65));
    }};

    /**
     * Relative weight distribution of external ordnance choices for pirates. Low tech, high chaos factor.
     */
    private static final BombLoadout pirateBombLoad = new BombLoadout() {{
        put(BombTypeEnum.HE, castPropertyInt("pirateBombLoad_HE", 7));
        put(BombTypeEnum.RL, castPropertyInt("pirateBombLoad_RL", 45));
        put(BombTypeEnum.INFERNO, castPropertyInt("pirateBombLoad_INFERNO", 35));
        put(BombTypeEnum.CLUSTER, castPropertyInt("pirateBombLoad_CLUSTER", 5));
        put(BombTypeEnum.FAE_SMALL, castPropertyInt("pirateBombLoad_FAE_SMALL", 6));
        put(BombTypeEnum.FAE_LARGE, castPropertyInt("pirateBombLoad_FAE_LARGE", 2));
    }};
    /**
     * External ordnance choices for pirates to set things on fire
     */
    private static final BombLoadout pirateFirestormBombLoad = new BombLoadout() {{
        put(BombTypeEnum.INFERNO, castPropertyInt("pirateFirestormBombLoad_INFERNO", 60));
        put(BombTypeEnum.FAE_SMALL, castPropertyInt("pirateFirestormBombLoad_FAE_SMALL", 30));
        put(BombTypeEnum.FAE_LARGE, castPropertyInt("pirateFirestormBombLoad_FAE_LARGE", 10));
    }};

    /**
     * External ordnance choices for air-to-air combat
     */
    private static final BombLoadout antiAirBombLoad = new BombLoadout() {{
        put(BombTypeEnum.RL, castPropertyInt("antiAirBombLoad_RL", 40));
        put(BombTypeEnum.LAA, castPropertyInt("antiAirBombLoad_LAA", 40));
        put(BombTypeEnum.AAA, castPropertyInt("antiAirBombLoad_AAA", 15));
        put(BombTypeEnum.AS, castPropertyInt("antiAirBombLoad_AS", 4));
        put(BombTypeEnum.ASEW, castPropertyInt("antiAirBombLoad_ASEW", 1));
    }};

    /**
     * External ordnance choices for attacking DropShips and other large craft
     */
    private static final BombLoadout antiShipBombLoad = new BombLoadout() {{
        put(BombTypeEnum.AAA, castPropertyInt("antiShipBombLoad_AAA", 50));
        put(BombTypeEnum.AS, castPropertyInt("antiShipBombLoad_AS", 35));
        put(BombTypeEnum.ASEW, castPropertyInt("antiShipBombLoad_ASEW", 15));
    }};

    /**
     * External ordnance choices for pirate air-to-air combat. Selects fewer high-tech choices than the standard load
     * out.
     */
    private static final BombLoadout pirateAirBombLoad = new BombLoadout() {{
        put(BombTypeEnum.RL, castPropertyInt("pirateAntiBombLoad_RL", 60));
        put(BombTypeEnum.LAA, castPropertyInt("pirateAntiBombLoad_LAA", 30));
        put(BombTypeEnum.AAA, castPropertyInt("pirateAntiBombLoad_AAA", 10));
    }};

    // end subregion Bombs
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

    public void updateOptionValues() {
        updateOptionValues(game.getOptions());
    }

    public void updateOptionValues(GameOptions gameOpts) {
        gameOptions = gameOpts;
        allowedYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        gameTechLevel = TechConstants.getSimpleLevel(gameOptions.stringOption(OptionsConstants.ALLOWED_TECH_LEVEL));
        legalLevel = SimpleTechLevel.getGameTechLevel(game);
        eraBasedTechLevel = gameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED);
        advAeroRules = gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AERO_ARTILLERY_MUNITIONS);
        showExtinct = gameOptions.booleanOption((OptionsConstants.ALLOWED_SHOW_EXTINCT));
    }

    /**
     * Calculates legality of ammo types given a faction, tech base (IS/CL), mixed tech, and the instance's already-set
     * year, tech level, and option for showing extinct equipment.
     *
     * @param aType     the AmmoType of the munition under consideration. q.v.
     * @param faction   MM-style faction code, per factions.xml and FactionRecord keys
     * @param techBase  either 'IS' or 'CL', used for clan boolean check.
     * @param mixedTech makes munitions checks more lenient by allowing faction to access both IS and CL tech bases.
     *
     * @return boolean true if legal for combination of inputs, false otherwise. Determines if an AmmoType is loaded.
     */
    public boolean checkLegality(AmmoType aType, String faction, String techBase, boolean mixedTech) {
        boolean clan = techBase.equals("CL");

        // Null-type is illegal!
        if (null == aType) {
            return false;
        }

        // Check if tech exists at all (or is explicitly allowed despite being extinct) and whether it is available
        // at the current tech level.
        boolean legal = aType.isAvailableIn(allowedYear, showExtinct) &&
              aType.isLegal(allowedYear, legalLevel, clan, mixedTech, showExtinct);

        if (eraBasedTechLevel) {
            // Check if tech is available to this specific faction with the current year and tech base.
            final Faction factionEntry = Faction.fromMMAbbr(faction);
            boolean eraBasedLegal = aType.isAvailableIn(allowedYear, clan, factionEntry);
            if (mixedTech) {
                eraBasedLegal |= aType.isAvailableIn(allowedYear, !clan, factionEntry);
            }
            legal &= eraBasedLegal;
        }

        // Nukes are not allowed... unless they are!
        legal &= (!aType.hasFlag(AmmoType.F_NUCLEAR) ||
              gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_AT2_NUKES));

        return legal;
    }

    /**
     * Use values from the Properties file defined in TeamLoadOutGenerator class if available; else use provided
     * default
     *
     * @param field    Field name in property file
     * @param defValue Default value to use
     *
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
     * Quick and dirty energy boat calc; useful for selecting Laser-Inhibiting Arrow and heat-based weapons
     *
     * @param el {@link Entity} List
     *
     * @return Number of Energy Boats
     */
    private static long checkForEnergyBoats(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.tracksHeat() && e.getAmmo().isEmpty()).count();
    }

    /**
     * "Missile Boat" defined here as any unit with half or more weapons dedicated to missiles (This could probably be
     * traded for a weight- or role-based check)
     *
     * @param el {@link Entity} List
     *
     * @return Number of Missile Boats
     */
    private static long checkForMissileBoats(ArrayList<Entity> el) {
        return el.stream()
              .filter(e -> e.getRole().isAnyOf(UnitRole.MISSILE_BOAT) ||
                    e.getWeaponList()
                          .stream()
                          .filter(w -> w.getName().toLowerCase().contains("lrm") ||
                                w.getName().toLowerCase().contains("srm") ||
                                w.getName().toLowerCase().contains("atm") ||
                                w.getName().toLowerCase().contains("mml") ||
                                w.getName().toLowerCase().contains("arrow") ||
                                w.getName().toLowerCase().contains("thunder"))
                          .count() >= e.getWeaponList().size())
              .count();
    }

    private static long checkForTAG(ArrayList<Entity> el) {
        return el.stream().filter(Entity::hasTAG).count();
    }

    private static long checkForNARC(ArrayList<Entity> el) {
        return el.stream()
              .filter(e -> e.getAmmo().stream().anyMatch(a -> a.getType().getAmmoType() == AmmoType.AmmoTypeEnum.NARC))
              .count();
    }

    private static long checkForAdvancedArmor(ArrayList<Entity> el) {
        // Most units have a location 0
        return el.stream()
              .filter(e -> e.getArmorType(0) == ArmorType.T_ARMOR_HARDENED ||
                    e.getArmorType(0) == ArmorType.T_ARMOR_BALLISTIC_REINFORCED ||
                    e.getArmorType(0) == ArmorType.T_ARMOR_REACTIVE ||
                    e.getArmorType(0) == ArmorType.T_ARMOR_BA_REACTIVE ||
                    e.getArmorType(0) == ArmorType.T_ARMOR_FERRO_LAMELLOR)
              .count();
    }

    private static long checkForReflectiveArmor(ArrayList<Entity> el) {
        return el.stream()
              .filter(e -> e.getArmorType(0) == ArmorType.T_ARMOR_REFLECTIVE ||
                    e.getArmorType(0) == ArmorType.T_ARMOR_BA_REFLECTIVE)
              .count();
    }

    private static long checkForFireproofArmor(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.getArmorType(0) == ArmorType.T_ARMOR_BA_FIRE_RESIST).count();
    }

    private static long checkForFastMovers(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.getOriginalWalkMP() > 5).count();
    }

    private static long checkForOffBoard(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.shouldOffBoardDeploy(e.getDeployRound())).count();
    }

    private static long checkForECM(ArrayList<Entity> el) {
        return el.stream().filter(Entity::hasECM).count();
    }

    private static long checkForTSM(ArrayList<Entity> el) {
        return el.stream().filter(e -> e.isMek() && ((Mek) e).hasTSM(false)).count();
    }
    // endregion Check for various unit types, armor types, etc.

    // region generateParameters
    public ReconfigurationParameters generateParameters(Team team) {
        Iterator<Entity> entityIterator = game.getTeamEntities(team);
        ArrayList<Entity> ownTeamEntities = new ArrayList<>();
        entityIterator.forEachRemaining(ownTeamEntities::add);

        return generateParameters(game, gameOptions, ownTeamEntities, team.getFaction(), team);
    }

    public ReconfigurationParameters generateParameters(ArrayList<Entity> ownEntities, String ownFaction, Team t) {
        return generateParameters(game, gameOptions, ownEntities, ownFaction, t);
    }

    /**
     * Create the parameters that will determine how to configure ammo loadouts for this team
     *
     * @param g               {@link Game} Object
     * @param gOpts           {@link GameOptions} Object
     * @param ownEntities     {@link Entity} List of own entities
     * @param friendlyFaction Friendly Faction Name
     * @param team            {@link Team} Object
     *
     * @return ReconfigurationParameters with information about enemy and friendly forces
     */
    public static ReconfigurationParameters generateParameters(Game g, GameOptions gOpts, ArrayList<Entity> ownEntities,
          String friendlyFaction, Team team) {
        if (ownEntities.isEmpty()) {
            // Nothing to generate
            return new ReconfigurationParameters();
        }
        ArrayList<Entity> etEntities = new ArrayList<>();
        ArrayList<String> enemyFactions = new ArrayList<>();

        for (Team et : g.getTeams()) {
            if (!et.isEnemyOf(team)) {
                continue;
            }
            enemyFactions.add(et.getFaction());
            Iterator<Entity> entityIterator = g.getTeamEntities(et);
            entityIterator.forEachRemaining(etEntities::add);
        }

        return generateParameters(g,
              gOpts,
              ownEntities,
              friendlyFaction,
              etEntities,
              enemyFactions,
              ForceDescriptor.RATING_5,
              1.0f);
    }

    public static ReconfigurationParameters generateParameters(Game g, GameOptions gOpts, ArrayList<Entity> ownEntities,
          String friendlyFaction, ArrayList<Entity> enemyEntities, ArrayList<String> enemyFactions, int rating,
          float fillRatio) {

        boolean blind = gOpts.booleanOption(OptionsConstants.BASE_BLIND_DROP) ||
              gOpts.booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
        boolean darkEnvironment = g.getPlanetaryConditions().getLight().isDuskOrFullMoonOrMoonlessOrPitchBack();
        boolean groundMap = (g.getMapSettings().getMedium() == MapSettings.MEDIUM_GROUND);
        boolean spaceEnvironment = (g.getMapSettings().getMedium() == MapSettings.MEDIUM_SPACE);

        if (blind) {
            enemyEntities.clear();
        }

        return generateParameters(ownEntities,
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

    public static ReconfigurationParameters generateParameters(ArrayList<Entity> ownTeamEntities,
          ArrayList<Entity> etEntities, String friendlyFaction, ArrayList<String> enemyFactions, boolean blind,
          boolean darkEnvironment, boolean groundMap, boolean spaceEnvironment, int rating, float fillRatio) {
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
        return (int) e.getWeaponList()
              .stream()
              .filter(w -> w.getName().toLowerCase().contains("ac") &&
                    !w.getName().toLowerCase().contains("lb") &&
                    w.getName().contains(size))
              .count();
    }

    private static int getACAmmoCount(Entity e, String size) {
        // Only applies to non-LB-X AC weapons
        return (int) e.getAmmo()
              .stream()
              .filter(w -> w.getName().toLowerCase().contains("ac") &&
                    !w.getName().toLowerCase().contains("lb") &&
                    w.getName().contains(size))
              .count();
    }

    private static void applyACCaselessImperative(Entity entity, MunitionTree munitionTree,
          ReconfigurationParameters reconfigurationParameters) {
        // TODO: remove this block when implementing new anti-ground Aero errata
        // Ignore Aero's, which can't use most alt munitions.
        if (entity.isAero()) {
            return;
        }

        Map<String, Double> caliberToRatioMap = Map.of("2", 0.25, // if 1 ton or fewer per 4 AC/2 barrels,
              "5", 0.5, // if 1 ton or fewer per 2 AC/5 barrels,
              "10", 1.0, // if 1 ton or fewer per AC/10 or AC/20 barrel
              "20", 1.0 // Replace existing imperatives with Caseless only.
        );

        // Iterate over any possible AutoCannons and update their ammo imperatives if count of bins/barrel is at or
        // below the relevant ratio.
        for (String caliber : caliberToRatioMap.keySet()) {
            int barrelCount = getACWeaponCount(entity, caliber);
            int binCount = getACAmmoCount(entity, caliber);

            if (barrelCount != 0) {
                if (((double) binCount) / barrelCount <= caliberToRatioMap.get(caliber)) {
                    // Replace any existing imperatives with Caseless as default
                    munitionTree.insertImperative(entity.getFullChassis(),
                          entity.getModel(),
                          "any",
                          "AC/" + caliber,
                          "Caseless");
                }
            }
        }
    }

    private static void insertArtemisImperatives(Entity e, MunitionTree mt, String ammoClass) {
        boolean artemis = !(e.getMiscEquipment(MiscType.F_ARTEMIS).isEmpty() &&
              e.getMiscEquipment(MiscType.F_ARTEMIS_V).isEmpty());

        if (artemis) {
            for (AmmoMounted bin : e.getAmmo()) {
                if (bin.getName().toUpperCase().contains(ammoClass)) {
                    String binType = bin.getType().getBaseName();
                    mt.insertImperative(e.getFullChassis(), e.getModel(), "any", binType, "Artemis-capable");
                }
            }
        }
    }

    // Set Artemis LRM carriers to use Artemis LRMs
    private static void setLRMImperatives(Entity entity, MunitionTree munitionTree,
          ReconfigurationParameters reconfigurationParameters) {
        insertArtemisImperatives(entity, munitionTree, "LRM");
    }

    private static void setSRMImperatives(Entity entity, MunitionTree munitionTree,
          ReconfigurationParameters reconfigurationParameters) {
        insertArtemisImperatives(entity, munitionTree, "SRM");
    }

    private static void setMMLImperatives(Entity entity, MunitionTree munitionTree,
          ReconfigurationParameters reconfigurationParameters) {
        insertArtemisImperatives(entity, munitionTree, "MML");
    }
    // region Imperative mutators

    // region generateMunitionTree

    public static MunitionTree generateMunitionTree(ReconfigurationParameters reconfigurationParameters,
          ArrayList<Entity> entities,
          String defaultSettingsFile) {
        // Based on various requirements from reconfigurationParameters, set weights for some ammo types over
        // others
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        return generateMunitionTree(reconfigurationParameters, entities, defaultSettingsFile, mwc);
    }

    /**
     * Generate the list of desired ammo load-outs for this team.
     * TODO: implement generateDetailedMunitionTree with more complex breakdowns per unit type
     * NOTE: if sub-classing this generator, should only need to override this
     * method.
     *
     * @param reconfigurationParameters {@link ReconfigurationParameters} Object
     * @param defaultSettingsFile       File name to settings file.
     *
     * @return generated MunitionTree with imperatives for each weapon type
     */
    public static MunitionTree generateMunitionTree(ReconfigurationParameters reconfigurationParameters,
          ArrayList<Entity> ownTeamEntities, String defaultSettingsFile,
          MunitionWeightCollection munitionWeightCollection) {

        // Either create a new tree or, if a defaults file is provided, load that as a base config
        MunitionTree mt = (defaultSettingsFile == null || defaultSettingsFile.isBlank()) ?
              new MunitionTree() :
              new MunitionTree(defaultSettingsFile);

        // Modify weights for parameters
        if (reconfigurationParameters.darkEnvironment) {
            // Bump munitions that light stuff up
            munitionWeightCollection.increaseIlluminationMunitions();
        } else {
            // decrease weights
            munitionWeightCollection.decreaseIlluminationMunitions();
        }

        // Adjust weights for enemy force composition
        if (reconfigurationParameters.enemiesVisible) {
            // Drop weight of shot-reducing ammo unless this team significantly outnumbers the enemy
            if (!(reconfigurationParameters.friendlyCount >=
                  reconfigurationParameters.enemyCount *
                        castPropertyDouble("mtReducingAmmoReduceIfUnderFactor", 2.0))) {
                // Skip munitions that reduce the number of rounds because we need to shoot a lot!
                munitionWeightCollection.decreaseAmmoReducingMunitions();
            } else if (reconfigurationParameters.friendlyCount >=
                  reconfigurationParameters.enemyCount *
                        castPropertyDouble("mtReducingAmmoIncreaseIfOverFactor", 3.0)) {
                munitionWeightCollection.increaseAmmoReducingMunitions();
            }

            // Flak: bump for any bombers, or fliers > 1/4th of enemy force
            if (reconfigurationParameters.enemyBombers > castPropertyDouble("mtFlakMinBombersExceedThreshold", 0.0)) {
                munitionWeightCollection.increaseFlakMunitions();
            }
            if (reconfigurationParameters.enemyFliers >=
                  reconfigurationParameters.enemyCount /
                        castPropertyDouble("mtFlakEnemyFliersFractionDivisor", 4.0)) {
                munitionWeightCollection.increaseFlakMunitions();
            }
            // Decrease if no bombers or fliers at all
            if (reconfigurationParameters.enemyBombers == 0 && reconfigurationParameters.enemyFliers == 0) {
                munitionWeightCollection.decreaseFlakMunitions();
            }

            // Enemy fast movers make more precise ammo attractive
            if (reconfigurationParameters.enemyFastMovers >=
                  reconfigurationParameters.enemyCount /
                        castPropertyDouble("mtPrecisionAmmoFastEnemyFractionDivisor", 4.0)) {
                munitionWeightCollection.increaseAccurateMunitions();
            }

            // AP munitions are hard-countered by hardened, reactive, etc. armor
            if (reconfigurationParameters.enemyAdvancedArmorCount >
                  castPropertyDouble("mtHPAmmoAdvArmorEnemiesExceedThreshold", 0.0) &&
                  reconfigurationParameters.enemyAdvancedArmorCount >
                        reconfigurationParameters.enemyReflectiveArmorCount) {
                munitionWeightCollection.decreaseAPMunitions();
                munitionWeightCollection.increaseHighPowerMunitions();
            } else if (reconfigurationParameters.enemyReflectiveArmorCount >
                  reconfigurationParameters.enemyAdvancedArmorCount) {
                // But AP munitions really hurt Reflective!
                munitionWeightCollection.increaseAPMunitions();
            }

            // Heat-based weapons kill infantry dead, also vehicles but anti-infantry weapons are generally inferior
            // without infantry targets
            if (reconfigurationParameters.enemyFireproofArmorCount <
                  reconfigurationParameters.enemyCount /
                        castPropertyDouble("mtFireproofMaxEnemyFractionDivisor", 4.0)) {
                if (reconfigurationParameters.enemyInfantry >=
                      reconfigurationParameters.enemyCount /
                            castPropertyDouble("mtInfantryEnemyExceedsFractionDivisor", 4.0)) {
                    munitionWeightCollection.increaseHeatMunitions();
                    munitionWeightCollection.increaseAntiInfMunitions();
                } else {
                    munitionWeightCollection.decreaseAntiInfMunitions();
                }
                if (reconfigurationParameters.enemyVehicles >=
                      reconfigurationParameters.enemyCount /
                            castPropertyDouble("mtVeeEnemyExceedsFractionDivisor", 4.0)) {
                    munitionWeightCollection.increaseHeatMunitions();
                }
                // BAs are proof against some dedicated Anti-Infantry weapons but not heat-generating rounds
                if (reconfigurationParameters.enemyBattleArmor >
                      reconfigurationParameters.enemyCount /
                            castPropertyDouble("mtBAEnemyExceedsFractionDivisor", 4.0)) {
                    munitionWeightCollection.increaseHeatMunitions();
                    munitionWeightCollection.increaseAntiBAMunitions();
                }
            } else if (reconfigurationParameters.enemyFireproofArmorCount >=
                  reconfigurationParameters.enemyCount /
                        castPropertyDouble("mtFireproofMaxEnemyFractionDivisor", 4.0)) {
                if (reconfigurationParameters.enemyInfantry >=
                      reconfigurationParameters.enemyCount /
                            castPropertyDouble("mtInfantryEnemyExceedsFractionDivisor", 4.0)) {
                    munitionWeightCollection.increaseAntiInfMunitions();
                }
                if (reconfigurationParameters.enemyBattleArmor >
                      reconfigurationParameters.enemyCount /
                            castPropertyDouble("mtBAEnemyExceedsFractionDivisor", 4.0)) {
                    munitionWeightCollection.increaseAntiBAMunitions();
                }
                munitionWeightCollection.decreaseHeatMunitions();
            }

            // Energy boats run hot; increase heat munitions and heat-seeking specifically
            if (reconfigurationParameters.enemyEnergyBoats >
                  reconfigurationParameters.enemyCount /
                        castPropertyDouble("mtEnergyBoatEnemyFractionDivisor", 4.0)) {
                munitionWeightCollection.increaseHeatMunitions();
                munitionWeightCollection.increaseMunitions(new ArrayList<>(List.of("Heat-Seeking")));
            }

            // Counter EMC by swapping Seeking in for Guided
            if (reconfigurationParameters.enemyECMCount >
                  castPropertyDouble("mtSeekingAmmoEnemyECMExceedThreshold", 1.0)) {
                munitionWeightCollection.decreaseGuidedMunitions();
                munitionWeightCollection.increaseSeekingMunitions();

                for (int i=0; i<Compute.log2(Math.max(2, (int) reconfigurationParameters.enemyECMCount)); i++) {
                    munitionWeightCollection.increaseMunitions(new ArrayList<>(List.of("Anti-Radiation")));
                }
            }
            if (reconfigurationParameters.enemyTSMCount >
                  castPropertyDouble("mtSeekingAmmoEnemyTSMExceedThreshold", 1.0)) {
                // Seeking
                munitionWeightCollection.increaseSeekingMunitions();
            }
            if (reconfigurationParameters.enemyECMCount == 0.0 &&
                  reconfigurationParameters.enemyTSMCount == 0.0 &&
                  reconfigurationParameters.enemyEnergyBoats == 0.0) {
                // Seeking munitions are generally situational
                munitionWeightCollection.decreaseSeekingMunitions();
            }
        }

        // Section: Friendly capabilities

        // Guided munitions are worth exponentially more with guidance and supporting missile units
        if (reconfigurationParameters.friendlyTAGs >= castPropertyDouble("mtGuidedAmmoFriendlyTAGThreshold", 1.0) ||
              reconfigurationParameters.friendlyNARCs >=
                    castPropertyDouble("mtGuidedAmmoFriendlyNARCThreshold", 1.0)) {

            // And worth even more with more guidance around
            if (reconfigurationParameters.friendlyMissileBoats >=
                  reconfigurationParameters.friendlyCount /
                        castPropertyDouble("mtGuidedAmmoFriendlyMissileBoatFractionDivisor", 3.0)) {
                for (long i = 0; i < reconfigurationParameters.friendlyMissileBoats; i++) {
                    munitionWeightCollection.increaseGuidedMunitions();
                }
            }

            // Increase the relevant types depending on the present guidance systems and their counts
            for (long i = 0; i < reconfigurationParameters.friendlyTAGs; i++) {
                munitionWeightCollection.increaseTagGuidedMunitions();
            }
            for (long i = 0; i < reconfigurationParameters.friendlyNARCs; i++) {
                munitionWeightCollection.increaseNARCGuidedMunitions();
                munitionWeightCollection.increaseMunitions(new ArrayList<>(List.of("Anti-Radiation")));
            }

            // TAG-guided rounds may have _some_ use, but not as much as base rounds, without TAG support
            if (reconfigurationParameters.friendlyTAGs == 0) {
                munitionWeightCollection.decreaseTagGuidedMunitions();
            }
            // Narc-capable are just not worth it without NARC support
            if (reconfigurationParameters.friendlyNARCs == 0) {
                munitionWeightCollection.zeroMunitionsWeight(new ArrayList<>(List.of("Narc-capable")));
            }
        } else {
            // Expensive waste without guidance
            munitionWeightCollection.zeroMunitionsWeight(GUIDED_MUNITIONS);
        }

        // Downgrade utility munitions unless there are multiple units that could use them; off-board arty in particular
        if (reconfigurationParameters.friendlyOffBoard >
              castPropertyDouble("mtUtilityAmmoOffBoardUnitsThreshold", 2.0)) {
            // Only increase utility rounds if we have more off-board units that the other guys
            if (reconfigurationParameters.enemyOffBoard <
                  reconfigurationParameters.friendlyOffBoard /
                        castPropertyDouble("mtUtilityAmmoFriendlyVsEnemyFractionDivisor", 1.0)) {
                munitionWeightCollection.increaseArtilleryUtilityMunitions();
            }
        } else {
            // Reduce utility munition chances if we've only got a lance or so of arty
            munitionWeightCollection.decreaseUtilityMunitions();
        }

        // Just for LOLs: when FS fights CC in 3028 ~ 3050, set Anti-TSM weight to 15.0
        if ((reconfigurationParameters.friendlyFaction != null && reconfigurationParameters.enemyFactions != null) &&
              reconfigurationParameters.friendlyFaction.equals("FS") &&
              reconfigurationParameters.enemyFactions.contains("CC") &&
              (3028 <= reconfigurationParameters.allowedYear && reconfigurationParameters.allowedYear <= 3050)) {
            ArrayList<String> tsmOnly = new ArrayList<>(List.of("Anti-TSM"));
            munitionWeightCollection.increaseMunitions(tsmOnly);
            munitionWeightCollection.increaseMunitions(tsmOnly);
            munitionWeightCollection.increaseMunitions(tsmOnly);
        }

        // Set nukes to the lowest possible weight if user has set them to unusable /for this team/ This is a separate
        // mechanism from the legality check.
        if (reconfigurationParameters.nukesBannedForMe) {
            munitionWeightCollection.zeroMunitionsWeight(new ArrayList<>(List.of("Davy Crockett-M",
                  "AlamoMissile Ammo")));
        }

        // L-K Missiles are essentially useless after 3042
        // TODO: add more precise faction and year checks here so Wolf's Dragoons can have them before everybody else.
        if (reconfigurationParameters.allowedYear < 3028 || reconfigurationParameters.allowedYear > 3042) {
            munitionWeightCollection.zeroMunitionsWeight(new ArrayList<>(List.of("Listen-Kill")));
        }

        // The main event!
        // Convert MWC to MunitionsTree for loading
        applyWeightsToMunitionTree(munitionWeightCollection, mt);

        // Handle individual cases like Artemis LRMs, AC/20s with limited ammo, etc.
        for (Entity e : ownTeamEntities) {
            // Set certain imperatives based on weapon types, due to low ammo count / low utility
            setACImperatives(e, mt, reconfigurationParameters);
            setLRMImperatives(e, mt, reconfigurationParameters);
            setSRMImperatives(e, mt, reconfigurationParameters);
            setMMLImperatives(e, mt, reconfigurationParameters);
        }

        return mt;
    }

    /**
     * Turn a selection of the computed munition weights into imperatives to load in the MunitionTree
     *
     * @param mt  {@link MunitionTree} Object
     * @param mwc {@link MunitionWeightCollection} Object
     */
    public static void applyWeightsToMunitionTree(MunitionWeightCollection mwc, MunitionTree mt) {
        // Iterate over every entry in the set of top-weighted munitions for each category
        HashMap<String, List<String>> topWeights = mwc.getTopN(castPropertyInt("mtTopMunitionsSubsetCount", 8));

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
    }
    // endregion generateMunitionTree

    // region reconfigureEntities

    /**
     * Wrapper to load a file of preset munition imperatives
     *
     * @param team    {@link Team} Object
     * @param faction Related Faction
     * @param adfFile Munitions File.
     */
    public void reconfigureTeam(Team team, String faction, String adfFile) {
        ReconfigurationParameters reconfigurationParameters = generateParameters(team);
        reconfigurationParameters.allowedYear = allowedYear;

        Iterator<Entity> entityIterator = game.getTeamEntities(team);
        ArrayList<Entity> updateEntities = new ArrayList<>();
        entityIterator.forEachRemaining(updateEntities::add);

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
        // For Pirate forces, assume fewer rounds per bin at lower quality levels, minimum 20%. If fill ratio is
        // already set, leave it.
        if (reconfigurationParameters.binFillPercent == UNSET_FILL_RATIO) {
            if (reconfigurationParameters.isPirate) {
                reconfigurationParameters.binFillPercent = (float) (Math.min(castPropertyDouble(
                            "pirateMaxAllowedBinFillRatio",
                            1.0),
                      Math.max(castPropertyDouble("pirateMinAllowedBinFillRatio", 0.2),
                            Math.random() / castPropertyDouble("pirateRandomRangeDivisor", 4.0) +
                                  (reconfigurationParameters.friendlyQuality /
                                        castPropertyDouble("pirateQualityDivisor", 8.0)))));
            } else {
                // If we get this far without setting the ratio, but are not pirates, reset to fill
                reconfigurationParameters.binFillPercent = 1.0f;
            }
        }

        // Commented out until A2G Attack Errata are implemented. No need to allocate memory for something not in use.
        // ArrayList<Entity> aeroSpaceUnits = new ArrayList<>();
        for (Entity entity : entities) {
            if (!entity.isAero()) {
                // TODO: Will be used when A2G attack errata are implemented
                //                aeroSpaceUnits.add(entity);
                //           } else {
                reconfigureEntity(entity, mt, faction, reconfigurationParameters.binFillPercent);
            }
        }

        populateAeroBombs(entities,
              this.allowedYear,
              reconfigurationParameters.groundMap ||
                    reconfigurationParameters.enemyCount > reconfigurationParameters.enemyFliers,
              reconfigurationParameters.friendlyQuality,
              reconfigurationParameters.isPirate,
              faction);
    }

    /**
     * Configure Bot Team with all munitions randomized
     *
     * @param team    {@link Team} Object
     * @param faction Related Faction to team
     */
    public void randomizeBotTeamConfiguration(Team team, String faction) {
        ReconfigurationParameters rp = generateParameters(team);
        Iterator<Entity> entityIterator = game.getTeamEntities(team);
        ArrayList<Entity> updateEntities = new ArrayList<>();
        entityIterator.forEachRemaining(updateEntities::add);

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
     * @param e       {@link Entity} with Ammo Bins
     * @param mt      {@link MunitionTree} Ammo used
     * @param faction Related Faction
     */
    public void reconfigureEntity(Entity e, MunitionTree mt, String faction) {
        reconfigureEntity(e, mt, faction, 1.0f);
    }

    /**
     * Method to apply a MunitionTree to a specific unit. Main application logic
     *
     * @param entity       {@link Entity} with Ammo Bins
     * @param mt           {@link MunitionTree} Ammo used
     * @param faction      Related Faction
     * @param binFillRatio float setting the max fill rate for all bins in this entity (mostly for Pirates)
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

        // Iterate over each type and fill it with the requested ammo (as much as possible)
        for (String binName : binLists.keySet()) {
            iterativelyLoadAmmo(entity, mt, binLists.get(binName), binName, faction);
        }

        // Apply requested fill ratio to all final bin loadouts (between max fill and 0)
        clampAmmoShots(entity, binFillRatio);
    }

    /**
     * Applies specified ammo fill ratio to all bins
     *
     * @param entity       {@link Entity} Unit tow work with.
     * @param binFillRatio How full to make bins.
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
     * This method should mirror reconfigureEntity but with more restrictions based on the types of alternate
     * munitions allowed by Aerospace rules.
     *
     * @param entity       {@link Entity} Unit to work with.
     * @param munitionTree {@link MunitionTree} Ammo Tree
     * @param faction      Related Faction
     */
    public void reconfigureAero(Entity entity, MunitionTree munitionTree, String faction) {

    }
    // endregion reconfigureAero

    // region iterativelyLoadAmmo
    private void iterativelyLoadAmmo(Entity entity, MunitionTree munitionTree, List<AmmoMounted> binList,
          String binName,
          String faction) {
        String techBase = (entity.isClan()) ? "CL" : "IS";
        iterativelyLoadAmmo(entity, munitionTree, binList, binName, techBase, faction);
    }

    /**
     * Manage loading ammo bins for a given type. Type can be designated by size (LRM-5) or generic (AC) Logic: Iterate
     * over list of priorities and fill the first as many times as requested. Repeat for 2nd...Nth ammo types If more
     * bins remain than desired types are specified, fill the remainder with the top priority type If more desired types
     * remain than there are bins, oh well. If a requested ammo type is not available in the specified time frame or
     * faction, skip it.
     *
     * @param e        Entity to load
     * @param mt       MunitionTree, stores required munitions in desired loading order
     * @param binList  List of actual mounted ammo bins matching this type
     * @param binName  String bin type we are loading now
     * @param techBase "CL" or "IS"
     * @param faction  Faction to outfit for, used in ammo validity checks (uses MM, not IO, faction codes)
     */
    private void iterativelyLoadAmmo(Entity e, MunitionTree mt, List<AmmoMounted> binList, String binName,
          String techBase, String faction) {
        // Copy counts that we will update, otherwise mt entry gets edited permanently.
        HashMap<String, Integer> counts = new HashMap<>(mt.getCountsOfAmmunitionForKey(e.getFullChassis(),
              e.getModel(),
              e.getCrew().getName(0),
              binName));
        List<String> priorities = mt.getPriorityList(e.getFullChassis(), e.getModel(), e.getCrew().getName(0), binName);
        // Track default type for filling in unfilled bins
        AmmoType defaultType = null;
        int defaultIdx = 0;

        // If the imperative is to use Random for every bin, we need a different Random for each bin
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
            AmmoType desired;

            // Load matching AmmoType
            if (binType.toLowerCase().contains("standard")) {
                desired = (AmmoType) EquipmentType.get(techBase + " " + binName + " " + "Ammo");
                if (desired == null) {
                    // Some ammo, like AC/XX ammo, is named funny
                    desired = (AmmoType) EquipmentType.get(techBase + " Ammo " + binName);
                }
            } else {
                // Get available munitions
                Vector<AmmoType> vAllTypes = AmmoType.getMunitionsFor(bin.getType().getAmmoType());
                if (vAllTypes == null) {
                    continue;
                }

                // Make sure the desired munition type is available and valid
                desired = vAllTypes.stream()
                      .filter(m -> m.getInternalName().startsWith(techBase) &&
                            m.getBaseName().contains(binName) &&
                            m.getName().contains(binType))
                      .filter(d -> checkLegality(d, faction, techBase, e.isMixedTech()))
                      .findFirst()
                      .orElse(null);
            }

            if (desired == null) {
                // Couldn't find a bin, move on to the next priority.
                // Update default idx if we're currently setting the default
                defaultIdx = (i == defaultIdx) ? defaultIdx + 1 : defaultIdx;
                continue;
            }

            // Add one of the current binType to counts so we get a new random type every bin
            if (random) {
                counts.put(binType, 1);
            }

            // Store default AmmoType
            if (i == defaultIdx) {
                defaultType = desired;
            }

            // Continue filling with this munition type until count is fulfilled or there are no more bins
            while (counts.getOrDefault(binType, 0) > 0 && !binList.isEmpty()) {
                try {
                    // fill one ammo bin with the requested ammo type

                    if (!bin.getType().equalsAmmoTypeOnly(desired)) {
                        // can't use this ammo if not
                        logger.debug("Unable to load bin {} with {}", bin.getName(), desired.getName());
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
     * Select a random munition type that is a valid damaging ammo (for "random") or truly random valid ammo (for true
     * random) for the bin type. IE "fléchette" is
     *
     * @param binName    Name of the Bin
     * @param trueRandom If it needs to be a random type of ammo.
     *
     * @return A random munition type.
     */
    private static String getRandomBin(String binName, boolean trueRandom) {
        String result = "";
        for (String typeName : TYPE_LIST) {
            if ((trueRandom || !UTILITY_MUNITIONS.contains(typeName)) &&
                  (binName.toLowerCase().contains(typeName.toLowerCase()) ||
                        typeName.toLowerCase().contains(binName.toLowerCase()))) {
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
     * Helper function to load bombs onto a random portion of units that can carry them
     *
     * @param entityList       The list of entities to process
     * @param hasGroundTargets true to select air-to-ground ordnance, false for air-to-air only
     * @param quality          IUnitRating enum for force quality (A/A* through F)
     * @param isPirate         true to use specific pirate ordnance loadouts
     */
    public void populateAeroBombs(List<Entity> entityList, int year, boolean hasGroundTargets, int quality,
          boolean isPirate, String faction) {

        // Get all valid bombers, and sort unarmed ones to the front. Ignore VTOLs for now, as they suffer extra
        // penalties for mounting bomb munitions
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
        int maxBombers = Math.min((int) Math.ceil(((castPropertyInt("percentBombersToEquipMin", 40) +
              Compute.randomInt(castPropertyInt("percentBombersToEquipRange",
                    60))) / 100.0) * bomberList.size()), bomberList.size());
        int numBombers = 0;

        Map<Integer, BombLoadout> bombsByCarrier = new HashMap<>();

        boolean forceHasGuided = false;
        for (int i = 0; i < bomberList.size(); i++) {
            int minThrust;
            int maxLoad;

            BombLoadout generatedBombs;
            bombsByCarrier.put(i, new BombLoadout());

            // Only generate loadouts up to the maximum number, use empty load out for the rest
            if (numBombers >= maxBombers) {
                continue;
            }

            Entity curBomber = bomberList.get(i);
            boolean isUnarmed = curBomber.getIndividualWeaponList().isEmpty();
            String techBase = (curBomber.isClan()) ? "CL" : "IS";
            boolean mixedTech = curBomber.isMixedTech();

            // Some fighters on ground attack may be flying air cover rather than strictly air-to-ground
            boolean isCAP = !hasGroundTargets ||
                  (Compute.d6() <= castPropertyInt("fightersLoadForCAPRollTargetThreshold", 1));

            // Set minimum thrust values, with lower minimums for unarmed and ground attack, and use remaining thrust
            // to limit hard points
            if (isCAP) {
                minThrust = isUnarmed ?
                      castPropertyInt("fighterCAPMinUnarmedSafeThrustValue", 2) :
                      ((int) Math.ceil(curBomber.getWalkMP() /
                            castPropertyDouble(
                                  "fighterCAPMinArmedSafeThrustFractionDivisor",
                                  2.0)));
            } else {
                minThrust = isUnarmed ?
                      castPropertyInt("bomberMinUnarmedSafeThrustValue", 2) :
                      castPropertyInt("bomberMinArmedSafeThrustValue", 3);
            }
            maxLoad = Math.min((int) Math.floor(curBomber.getWeight() /
                        castPropertyDouble("maxBomberLoadFactorDivisor", 5.0)),
                  (curBomber.getWalkMP() - minThrust) * castPropertyInt("maxBomberLoadThrustDiffFactor", 5));

            // Get a random percentage (default 40 ~ 90) of the maximum bomb load for armed entities
            if (!isUnarmed) {
                maxLoad = (int) Math.ceil((castPropertyInt("maxPercentBomberLoadToEquipMin", 50) +
                      Compute.randomInt(castPropertyInt("maxPercentBomberLoadToEquipRange",
                            40))) * maxLoad / 100.0);
            }

            if (maxLoad == 0) {
                continue;
            }

            // Generate bomb load
            generatedBombs = generateExternalOrdnance(maxLoad,
                  isCAP,
                  isPirate,
                  quality,
                  year,
                  faction,
                  techBase,
                  mixedTech);
            // Whoops, go yell at the ordnance technician
            if (generatedBombs.getTotalBombs() <= 0) {
                continue;
            }

            // Set a flag to indicate at least one of the bombers is carrying guided ordnance
            forceHasGuided = forceHasGuided || generatedBombs.hasGuidedOrdnance();

            // Store the bomb selections as we might need to add in TAG later
            bombsByCarrier.put(i, generatedBombs);

            // Do not increment bomber count for unarmed entities
            if (!isUnarmed) {
                numBombers++;
            }

        }

        loadBombsOntoBombers(bomberList, bombsByCarrier, forceHasGuided);
    }

    private static void loadBombsOntoBombers(List<Entity> bomberList, Map<Integer, BombLoadout> bombsByCarrier,
          boolean forceHasGuided) {
        // Load ordnance onto units. If there is guided ordnance present then randomly add some TAG pods to those
        // without the guided ordnance.
        int tagCount = Math.min(bomberList.size(), Compute.randomInt(castPropertyInt("bombersToAddTagMaxCount", 3)));
        for (int i = 0; i < bomberList.size(); i++) {
            Entity curBomber = bomberList.get(i);

            BombLoadout generatedBombs = bombsByCarrier.get(i);

            // Don't combine guided ordnance with external TAG
            if (forceHasGuided && tagCount > 0) {
                int maxLoadForTagger = Math.min((int) Math.floor(curBomber.getWeight() /
                            castPropertyDouble("maxBomberLoadFactorDivisor",
                                  5.0)),
                      (curBomber.getWalkMP() - 2) * castPropertyInt("maxBomberLoadThrustDiffFactor", 5));
                if (addExternalTAG(generatedBombs, true, maxLoadForTagger)) {
                    tagCount--;
                }
            }

            // Load the provided ordnance onto the unit
            if (generatedBombs.getTotalBombs() > 0) {
                ((IBomber) curBomber).setBombChoices(generatedBombs);
            }
        }
    }

    /**
     * Randomly generate a set of external ordnance up to the number of indicated bomb units. Lower rated forces are
     * more likely to get simpler types (HE and rockets). Because TAG is only useful as one-per-fighter, it should be
     * handled elsewhere.
     *
     * @param bombUnits how many bomb units to generate, some types count as more than one unit so returned counts may
     *                  be lower than this but never higher
     * @param airOnly   true to only select air-to-air ordnance
     * @param isPirate  true if force is pirate, specific low-tech/high chaos selections
     * @param quality   force rating to work with
     * @param year      current year, for tech filter
     *
     * @return {@link BombLoadout} of integers, with each element being a bomb count using BombUnit enums as the lookup
     *       e.g. [BombUnit.HE] will get the number of HE bombs.
     */
    public BombLoadout generateExternalOrdnance(int bombUnits, boolean airOnly, boolean isPirate, int quality, int year,
          String faction, String techBase, boolean mixedTech) {

        BombLoadout bombLoad = new BombLoadout();

        if (bombUnits <= 0) {
            return bombLoad;
        }

        // Get a random predefined load out
        double countWeight = 0.0;
        double completeWeight;
        double randomThreshold;

        // Use weighted random generation for air-to-ground loadouts. Use simple random selection for air-to-air.
        BombLoadout bombMap;
        if (!airOnly) {
            bombMap = lowTechBombLoad;

            // Randomly select a load out using the weighted map of names. Pirates use a separate map with different
            // loadouts.
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
            randomThreshold = (Compute.randomInt(castPropertyInt("bomberRandomThresholdMaxPercent", 100)) / 100.0) *
                  completeWeight;
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
                if (Compute.randomInt(castPropertyInt("fighterCAPRandomPercentageRange", 100)) >
                      castPropertyInt("fighterCAPAntiShipLoadOutRandomPercentageMax", 20)) {
                    bombMap = antiAirBombLoad;
                } else {
                    bombMap = antiShipBombLoad;
                }
            } else {
                bombMap = pirateAirBombLoad;
            }

        }

        // Slight hack to account for difficulties with isAvailableIn() with certain bombs
        boolean guidedAndArrowAvailable = ((year >= 2600) && (year <= 2835)) || (year > 3044);

        // Generate a working map with all the unavailable ordnance replaced with rockets or HE
        BombLoadout workingBombMap = new BombLoadout();
        for (BombTypeEnum curBombType : bombMap.keySet()) {
            // Make sure the bomb type is even legal for the current scenario
            if (!checkLegality(BombType.createBombByType(curBombType), faction, techBase, mixedTech)) {
                continue;
            }

            String typeName = curBombType.getInternalName();
            if (curBombType == BombTypeEnum.RL ||
                  curBombType == BombTypeEnum.HE ||
                  (curBombType != BombTypeEnum.LG &&
                        curBombType != BombTypeEnum.ARROW &&
                        curBombType != BombTypeEnum.HOMING &&
                        BombType.get(typeName).isAvailableIn(year, false)) ||
                  ((curBombType == BombTypeEnum.LG ||
                        curBombType == BombTypeEnum.ARROW ||
                        curBombType == BombTypeEnum.HOMING) && guidedAndArrowAvailable)) {

                if (workingBombMap.containsKey(curBombType)) {
                    workingBombMap.put(curBombType, bombMap.get(curBombType) + workingBombMap.get(curBombType));
                } else {
                    workingBombMap.put(curBombType, bombMap.get(curBombType));
                }

            } else {
                BombTypeEnum replacementBomb = airOnly ?
                      BombTypeEnum.RL :
                      Compute.randomInt(castPropertyInt("bombReplacementIntRange", 2)) <=
                            castPropertyInt("bombReplacementRLThreshold", 0) ?
                            BombTypeEnum.RL :
                            BombTypeEnum.HE;
                if (workingBombMap.containsKey(replacementBomb)) {
                    workingBombMap.put(replacementBomb, bombMap.get(curBombType) + workingBombMap.get(replacementBomb));
                } else {
                    workingBombMap.put(replacementBomb, bombMap.get(curBombType));
                }

            }
        }

        // Generate enough bombs to meet the desired count

        BombTypeEnum selectedBombType = BombTypeEnum.NONE;
        int loopSafety = 0;

        List<BombTypeEnum> ordnanceIDs = new ArrayList<>();
        List<Integer> ordnanceRandomWeights = new ArrayList<>();
        for (BombTypeEnum curID : workingBombMap.keySet()) {
            ordnanceIDs.add(curID);
            ordnanceRandomWeights.add(workingBombMap.get(curID));
        }
        completeWeight = ordnanceRandomWeights.stream()
              .mapToInt(curWeight -> Math.max(curWeight, 1))
              .asDoubleStream()
              .sum();

        if (!ordnanceIDs.isEmpty() && !ordnanceRandomWeights.isEmpty() && completeWeight != 0) {
            for (int curLoad = 0;
                  curLoad < bombUnits && loopSafety < castPropertyInt("maxBombApplicationLoopCount", 10); ) {

                // Randomly get the ordnance type
                randomThreshold = (Compute.randomInt(castPropertyInt("maxBombOrdnanceWeightPercentThreshold", 100)) /
                      100.0) * completeWeight;
                countWeight = 0.0;
                for (int i = 0; i < ordnanceIDs.size(); i++) {
                    countWeight += Math.max(ordnanceRandomWeights.get(i), 1.0);
                    if (countWeight >= randomThreshold) {
                        selectedBombType = ordnanceIDs.get(i);
                        break;
                    }
                }

                // If the selected ordnance doesn't exceed the provided limit increment the counter, otherwise skip
                // it and keep trying with some safeties to prevent infinite loops.
                if ((selectedBombType != BombTypeEnum.NONE) && (curLoad + selectedBombType.getCost() <= bombUnits)) {
                    bombLoad.addBombs(selectedBombType, 1);
                    curLoad += selectedBombType.getCost();
                } else {
                    loopSafety++;
                }
            }
        }

        // Oops, nothing left - rocket launchers are always popular
        if (bombLoad.getTotalBombs() == 0) {
            // Rocket Launchers are a good option after CI era
            if (checkLegality(BombType.createBombByType(BombTypeEnum.RL), faction, techBase, mixedTech)) {
                bombLoad.put(BombTypeEnum.RL, bombUnits);
                return bombLoad;
            }
            // Otherwise, Prototype Rocket Launchers are almost always in style.
            if (checkLegality(BombType.createBombByType(BombTypeEnum.RLP), faction, techBase, mixedTech)) {
                bombLoad.put(BombTypeEnum.RLP, bombUnits);
                return bombLoad;
            }
        }

        // Randomly replace advanced ordnance with rockets or HE, depending on force rating and air-air/ground
        // preference

        List<BombTypeEnum> advancedOrdnance = List.of(BombTypeEnum.LG,
              BombTypeEnum.ARROW,
              BombTypeEnum.HOMING,
              BombTypeEnum.LAA,
              BombTypeEnum.AAA,
              BombTypeEnum.THUNDER,
              BombTypeEnum.FAE_SMALL,
              BombTypeEnum.FAE_LARGE,
              BombTypeEnum.AS,
              BombTypeEnum.ASEW);

        randomThreshold = switch (quality) {
            case ForceDescriptor.RATING_5, ForceDescriptor.RATING_4 ->
                  castPropertyInt("bombRandomReplaceRating4PlusThreshold", 5);
            case ForceDescriptor.RATING_3 -> castPropertyInt("bombRandomReplaceRating3PlusThreshold", 10);
            case ForceDescriptor.RATING_2 -> castPropertyInt("bombRandomReplaceRating2PlusThreshold", 25);
            case ForceDescriptor.RATING_1 -> castPropertyInt("bombRandomReplaceRating1PlusThreshold", 40);
            case ForceDescriptor.RATING_0 -> castPropertyInt("bombRandomReplaceRating0PlusThreshold", 80);
            default -> throw new IllegalArgumentException("Unrecognized rating value: " + quality);
        };

        for (BombTypeEnum curBomb : advancedOrdnance) {
            int loadCount = bombLoad.getCount(curBomb);

            for (int i = 0; i < loadCount; i++) {
                if (Compute.randomInt(100) < randomThreshold) {
                    if (airOnly) {
                        bombLoad.addBombs(BombTypeEnum.RL, 1);
                    } else {
                        BombTypeEnum replacementBomb = Compute.randomInt(castPropertyInt("bombReplacementIntRange", 2))
                              <=
                              castPropertyInt("bombReplacementRLThreshold", 0) ?
                              BombTypeEnum.RL :
                              BombTypeEnum.HE;
                        bombLoad.addBombs(replacementBomb, 1);
                    }
                    bombLoad.addBombs(curBomb, -1);
                }
            }
        }
        return bombLoad;
    }

    /**
     * Updates a bomb load to include an external TAG system. If this exceeds the provided maximum load (in bomb units
     * i.e. Arrow IV counts as multiple units), then one of the basic one-slot types is removed and the TAG system is
     * added in its place.
     *
     * @param bombLoad   a BombLoadout to update with TAG
     * @param skipGuided true to only select external TAG for units without guided ordnance
     * @param maxLoad    Maximum external ordnance load in total bomb units (NOT bomb count)
     *
     * @return true, if TAG was added, false otherwise
     */
    private static boolean addExternalTAG(BombLoadout bombLoad, boolean skipGuided, int maxLoad) {
        if (!skipGuided || !bombLoad.hasGuidedOrdnance()) {
            int totalLoad = bombLoad.getTotalBombCost();
            if (totalLoad < maxLoad) {
                bombLoad.addBombs(BombTypeEnum.TAG, 1);
                return true;
            } else if (totalLoad == maxLoad) {
                Set<BombTypeEnum> replaceableTypes = Set.of(
                      BombTypeEnum.RL,
                      BombTypeEnum.HE,
                      BombTypeEnum.INFERNO,
                      BombTypeEnum.CLUSTER
                );
                for (BombTypeEnum curType : replaceableTypes) {
                    if (bombLoad.getCount(curType) > 0) {
                        bombLoad.addBombs(curType, -1);
                        bombLoad.addBombs(BombTypeEnum.TAG, 1);
                        return true;
                    }
                }

            } else {
                // Already overloaded, don't bother
                return false;
            }
            bombLoad.addBombs(BombTypeEnum.TAG, 1);
            return true;
        }

        return false;
    }

    // endregion aero / bombs
}

// endregion MunitionWeightCollection
