/*
 * Copyright (c) 2022, 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.scenario;

import megamek.client.generator.RandomGenderGenerator;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.enums.Gender;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.*;
import megamek.common.util.BoardUtilities;
import megamek.common.util.StringUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.server.GameManager;
import megamek.server.IGameManager;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class holds all scenario info loaded from a scenario (.mms) file. It is a map of constants given in
 * {@link ScenarioLoader} to a list of data for that constant.
 */
public class ScenarioV1 extends HashMap<String, Collection<String>> implements Scenario {

    private static final String SEPARATOR_PROPERTY = "=";
    private static final String SEPARATOR_COMMA = ",";
    private static final String SEPARATOR_SPACE = " ";
    private static final String SEPARATOR_COLON = ":";
    private static final String SEPARATOR_UNDERSCORE = "_";

    private static final String NAME = "Name";
    private static final String DESCRIPTION = "Description";

    private static final String PARAM_GAME_OPTIONS_FILE = "GameOptionsFile";
    private static final String PARAM_GAME_OPTIONS_FIXED = "FixedGameOptions";
    private static final String PARAM_GAME_EXTERNAL_ID = "ExternalId";
    private static final String PARAM_FACTIONS = "Factions";
    private static final String PARAM_SINGLEPLAYER = "SinglePlayer";

    private static final String PARAM_PLANETCOND_FIXED = "FixedPlanetaryConditions";

    private static final String PARAM_MAP_WIDTH = "MapWidth";
    private static final String PARAM_MAP_HEIGHT = "MapHeight";
    private static final String MAP_RANDOM = "RANDOM";
    private static final String PARAM_BOARD_WIDTH = "BoardWidth";
    private static final String PARAM_BOARD_HEIGHT = "BoardHeight";
    private static final String PARAM_BRIDGE_CF = "BridgeCF";
    private static final String PARAM_MAPS = "Maps";
    private static final String PARAM_MAP_DIRECTORIES = "RandomDirs";

    private static final String PARAM_TEAM = "team";
    private static final String PARAM_LOCATION = "Location";
    private static final String PARAM_MINEFIELDS = "Minefields";
    private static final String PARAM_DAMAGE = "Damage";
    private static final String PARAM_SPECIFIC_DAMAGE = "DamageSpecific";
    private static final String PARAM_CRITICAL_HIT = "CritHit";
    private static final String PARAM_AMMO_AMOUNT = "SetAmmoTo";
    private static final String PARAM_AMMO_TYPE = "SetAmmoType";
    private static final String PARAM_PILOT_HITS = "PilotHits";
    private static final String PARAM_EXTERNAL_ID = "ExternalID";
    private static final String PARAM_ADVANTAGES = "Advantages";
    private static final String PARAM_AUTO_EJECT = "AutoEject";
    private static final String PARAM_COMMANDER = "Commander";
    private static final String PARAM_DEPLOYMENT_ROUND = "DeploymentRound";
    private static final String PARAM_CAMO = "camo";
    private static final String PARAM_ALTITUDE = "altitude";

    private static final String PARAM_PLANETCOND_TEMP = "PlanetaryConditionsTemperature";
    private static final String PARAM_PLANETCOND_GRAV = "PlanetaryConditionsGravity";
    private static final String PARAM_PLANETCOND_LIGHT = "PlanetaryConditionsLight";
    private static final String PARAM_PLANETCOND_WEATHER = "PlanetaryConditionsWeather";
    private static final String PARAM_PLANETCOND_WIND = "PlanetaryConditionsWind";
    private static final String PARAM_PLANETCOND_WINDDIR = "PlanetaryConditionsWindDir";
    private static final String PARAM_PLANETCOND_ATMOS = "PlanetaryConditionsAtmosphere";
    private static final String PARAM_PLANETCOND_FOG = "PlanetaryConditionsFog";
    private static final String PARAM_PLANETCOND_WINDSHIFTINGSTR = "PlanetaryConditionsWindShiftingStr";
    private static final String PARAM_PLANETCOND_WINDMIN = "PlanetaryConditionsWindMin";
    private static final String PARAM_PLANETCOND_WINDMAX = "PlanetaryConditionsWindMax";
    private static final String PARAM_PLANETCOND_WINDSHIFTINGDIR = "PlanetaryConditionsWindShiftingDir";
    private static final String PARAM_PLANETCOND_BLOWINGSAND = "PlanetaryConditionsBlowingSand";
    private static final String PARAM_PLANETCOND_EMI = "PlanetaryConditionsEMI";
    private static final String PARAM_PLANETCOND_TERRAINCHANGES = "PlanetaryConditionsAllowTerrainChanges";

    private final File file;

    private final List<DamagePlan> damagePlans = new ArrayList<>();
    // Used to store Crit Hits
    private final List<CritHitPlan> critHitPlans = new ArrayList<>();
    // Used to set ammo Spec Ammounts
    private final List<SetAmmoPlan> ammoPlans = new ArrayList<>();

    /** When true, the Game Options Dialog is skipped. */
    private boolean fixedGameOptions = false;

    /** When true, the Planetary Conditions Dialog is skipped. */
    private boolean fixedPlanetCond;

    /**
     * When true, the Player assignment/camo Dialog and the host dialog are skipped.
     * The first faction (player) is assumed to be the local player and the rest
     * are assumed to be Princess.
     */
    private boolean singlePlayer;

    ScenarioV1(File file) throws IOException {
        this.file = file;
        load();
    }

    /**
     * @return The name of the scenario; keyword {@link ScenarioLoader#NAME}
     */
    public String getName() {
        return getString(NAME);
    }

    /**
     * @return The description of the scenario; keyword {@link ScenarioLoader#DESCRIPTION}
     */
    public String getDescription() {
        return getString(DESCRIPTION);
    }

    /**
     * @return The filename including directories of the scenario
     */
    public String getFileName() {
        return file.toString();
    }

    public void load() throws IOException {
        for (String line : readLines(file, s -> s.trim().startsWith("#"))) {
            if (!line.contains("=")) {
                LogManager.getLogger().error("Missing = in scenario line: " + line);
            } else {
                String[] elements = line.split(SEPARATOR_PROPERTY, 2);
                String keyword = elements[0].trim();
                put(keyword, elements[1].trim());
            }
        }
//        ScenarioV1 props = new ScenarioV1();
//        put("filename", List.of(file.toString()));
//        try (FileInputStream fis = new FileInputStream(file);
//             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
//             BufferedReader br = new BufferedReader(isr)) {
//            String line;
//            int lineNum = 0;
//            while ((line = br.readLine()) != null) {
//                lineNum++;
//                line = line.trim();
//                if (line.startsWith(COMMENT_MARK) || line.isBlank()) {
//                    continue;
//                } else if (!line.contains(SEPARATOR_PROPERTY)) {
//                    LogManager.getLogger().error(String.format("Equality sign in scenario file %s on line %d missing; ignoring",
//                            file, lineNum));
//                    continue;
//                }
//                String[] elements = line.split(SEPARATOR_PROPERTY, -1);
//                if (elements.length > 2) {
//                    LogManager.getLogger().error(String.format("Multiple equality signs in scenario file %s on line %d; ignoring",
//                            file, lineNum));
//                    continue;
//                }
//                put(elements[0].trim(), elements[1].trim());
//            }
//        } catch (IOException e) {
//            LogManager.getLogger().error("", e);
////            throw new ScenarioLoaderException("exceptionReadingFile", file.toString());
//        }
////        return props;
    }

    public static List<String> readLines(File file, Predicate<String> isCommentLine) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath());
        lines.removeIf(String::isBlank);
        lines.removeIf(isCommentLine);
        return lines;
    }

    public void put(String key, String value) {
        Collection<String> values = get(key);
        if (values == null) {
            values = new ArrayList<>();
            put(key, values);
        }
        values.add(value);
    }

    public String getString(String key) {
        return getString(key, SEPARATOR_COMMA);
    }

    public String getString(String key, String separator) {
        Collection<String> values = get(key);
        if ((values == null) || values.isEmpty()) {
            return null;
        }

        boolean firstElement = true;
        StringBuilder sb = new StringBuilder();
        for (String val : values) {
            if (firstElement) {
                firstElement = false;
            } else {
                sb.append(separator);
            }
            sb.append(val);
        }
        return sb.toString();
    }

    /**
     * @return the number of values for this key in the file
     */
    public int getNumValues(String key) {
        Collection<String> values = get(key);
        return (values == null) ? 0 : values.size();
    }

    @Override
    public Collection<String> get(Object key) {
        return super.get(key);
    }

    public IGame createGame() throws ScenarioLoaderException {
        LogManager.getLogger().info("Loading scenario from " + file);
        Game game = new Game();
        game.setBoardDirect(createBoard(this));

        // build the faction players
        Collection<Player> players = createPlayers(this);
        for (Player player : players) {
            game.addPlayer(player.getId(), player);
        }

        // build the entities
        int entityId = 0;
        for (Player player : players) {
            Collection<Entity> entities = buildFactionEntities(this, player);
            for (Entity entity : entities) {
                entity.setOwner(player);
                entity.setId(entityId);
                ++ entityId;
                game.addEntity(entity);
                // Grounded DropShips don't set secondary positions unless they're part of a game and can verify
                // they're not on a space map.
                if (entity.isLargeCraft() && !entity.isAirborne()) {
                    entity.setAltitude(0);
                }
            }
        }
        // game's ready
        game.getOptions().initialize();
        String optionFile = getString(PARAM_GAME_OPTIONS_FILE);
        if (optionFile == null) {
            game.getOptions().loadOptions();
        } else {
            game.getOptions().loadOptions(new MegaMekFile(file.getParentFile(), optionFile).getFile(), true);
        }
        fixedGameOptions = parseBoolean(this, PARAM_GAME_OPTIONS_FIXED, false);

        // set wind
        parsePlanetaryConditions(game, this);
        game.getPlanetaryConditions().determineWind();
        fixedPlanetCond = parseBoolean(this, PARAM_PLANETCOND_FIXED, false);
        singlePlayer = parseBoolean(this, PARAM_SINGLEPLAYER, false);

        // Set up the teams (for initiative)
        game.setupTeams();
        game.setPhase(GamePhase.STARTING_SCENARIO);
        game.setupRoundDeployment();
        game.setExternalGameId(parseExternalGameId(this));
        game.setVictoryContext(new HashMap<>());
        game.createVictoryConditions();
        return game;
    }

    /**
     * Parses a boolean value. When the key is not present, returns the given
     * defaultValue. When the key is present, interprets "true" and "on"  and "1"
     * as true and everything else as false.
     */
    private boolean parseBoolean(String key, boolean defaultValue) {
        boolean result = defaultValue;
        if (containsKey(key)) {
            result = getString(key).equalsIgnoreCase("true")
                    || getString(key).equalsIgnoreCase("on")
                    || getString(key).equalsIgnoreCase("1");
        }
        return result;
    }

    // TODO : legal/valid ammo type handling and game options, since they are set at this point
    private AmmoType getValidAmmoType(Game game, Mounted mounted, String ammoString) {
        final Entity e = mounted.getEntity();
        final int year = game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        final EquipmentType currentAmmoType = mounted.getType();
        final Mounted currentWeapon = mounted.getLinkedBy();
        final EquipmentType currentWeaponType = (null != currentWeapon) ? currentWeapon.getType() : null;
        final EquipmentType newAmmoType = EquipmentType.get(ammoString);
        if (newAmmoType == null) {
            LogManager.getLogger().error(String.format("Ammo type '%s' not found", ammoString));
            return null;
        } else if (!(newAmmoType instanceof AmmoType)) {
            LogManager.getLogger().error(String.format("Equipment %s is not an ammo type", newAmmoType.getName()));
            return null;
        } else if (!newAmmoType.isLegal(year, SimpleTechLevel.getGameTechLevel(game), e.isClan(),
                e.isMixedTech(), game.getOptions().booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT))) {
            LogManager.getLogger().warn(String.format("Ammo %s (TL %d) is not legal for year %d (TL %d)",
                    newAmmoType.getName(), newAmmoType.getTechLevel(year), year,
                    TechConstants.getGameTechLevel(game, e.isClan())));
            return null;
        } else if (e.isClan() && !game.getOptions().booleanOption(OptionsConstants.ALLOWED_CLAN_IGNORE_EQ_LIMITS)) {
            // Check for clan weapon restrictions
            // Construct EnumSet with all the relevant
            final EnumSet<AmmoType.Munitions> muniType = ((AmmoType) newAmmoType).getMunitionType();
            muniType.add(AmmoType.Munitions.M_INCENDIARY_LRM);
            if ((muniType.contains(AmmoType.Munitions.M_SEMIGUIDED) || (muniType.contains(AmmoType.Munitions.M_SWARM_I))
                    || (muniType.contains(AmmoType.Munitions.M_THUNDER_AUGMENTED)) || (muniType.contains(AmmoType.Munitions.M_THUNDER_INFERNO))
                    || (muniType.contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB)) || (muniType.contains(AmmoType.Munitions.M_THUNDER_ACTIVE))
                    || (muniType.contains(AmmoType.Munitions.M_INFERNO_IV)) || (muniType.contains(AmmoType.Munitions.M_VIBRABOMB_IV))
                    || (muniType.contains(AmmoType.Munitions.M_LISTEN_KILL)) || (muniType.contains(AmmoType.Munitions.M_ANTI_TSM))
                    || (muniType.contains(AmmoType.Munitions.M_DEAD_FIRE)) || (muniType.contains(AmmoType.Munitions.M_MINE_CLEARANCE)))) {
                LogManager.getLogger().warn(String.format("Ammo type %s not allowed by Clan rules", newAmmoType.getName()));
                return null;
            }
        }

        if (AmmoType.canDeliverMinefield((AmmoType) newAmmoType)
                && !game.getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            LogManager.getLogger().warn(String.format("Minefield-creating ammo type %s forbidden by game rules",
                    newAmmoType.getName()));
            return null;
        }

        int weaponAmmoType = (currentWeaponType instanceof WeaponType) ? ((WeaponType) currentWeaponType).getAmmoType() : 0;
        if ((((AmmoType) newAmmoType).getRackSize() == ((AmmoType) currentAmmoType).getRackSize())
                && (newAmmoType.hasFlag(AmmoType.F_BATTLEARMOR) == currentAmmoType.hasFlag(AmmoType.F_BATTLEARMOR))
                && (newAmmoType.hasFlag(AmmoType.F_ENCUMBERING) == currentAmmoType.hasFlag(AmmoType.F_ENCUMBERING))
                && (newAmmoType.getTonnage(e) == currentAmmoType.getTonnage(e))
                && (((AmmoType) newAmmoType).getAmmoType() == weaponAmmoType)) {
            return (AmmoType) newAmmoType;
        } else {
            return null;
        }
    }

    /**
     * The damage procedures are built into a server object, so we delay dealing
     * the random damage until a server is made available to us.
     */
    @Override
    public void applyDamage(IGameManager iGameManager) {
        GameManager m = (GameManager) iGameManager;
        for (DamagePlan damagePlan : damagePlans) {
            LogManager.getLogger().debug(String.format("Applying damage to %s", damagePlan.entity.getShortName()));
            for (int y = 0; y < damagePlan.nBlocks; ++ y) {
                HitData hit = damagePlan.entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                LogManager.getLogger().debug("[s.damageEntity(dp.entity, hit, 5)]");
                m.damageEntity(damagePlan.entity, hit, 5);
            }

            // Apply Spec Damage
            for (SpecDam specDamage : damagePlan.specificDammage) {
                if (damagePlan.entity.locations() <= specDamage.loc) {
                    // location is valid
                    LogManager.getLogger().error(String.format("\tInvalid location specified %d", specDamage.loc));
                } else {
                    // Infantry only take damage to "internal"
                    if (specDamage.internal || damagePlan.entity.isConventionalInfantry()) {
                        if (damagePlan.entity.getOInternal(specDamage.loc) > specDamage.setArmorTo) {
                            damagePlan.entity.setInternal(specDamage.setArmorTo, specDamage.loc);
                            LogManager.getLogger().debug(String.format("\tSet armor value for (internal %s) to %d",
                                    damagePlan.entity.getLocationName(specDamage.loc), specDamage.setArmorTo));
                            if (specDamage.setArmorTo == 0) {
                                LogManager.getLogger().debug(String.format("\tSection destroyed %s",
                                        damagePlan.entity.getLocationName(specDamage.loc)));
                                damagePlan.entity.destroyLocation(specDamage.loc);
                            }
                        }
                    } else {
                        if (specDamage.rear && damagePlan.entity.hasRearArmor(specDamage.loc)) {
                            if (damagePlan.entity.getOArmor(specDamage.loc, true) > specDamage.setArmorTo) {
                                LogManager.getLogger().debug(String.format("\tSet armor value for (rear %s) to %d",
                                        damagePlan.entity.getLocationName(specDamage.loc), specDamage.setArmorTo));
                                damagePlan.entity.setArmor(specDamage.setArmorTo, specDamage.loc, true);
                            }
                        } else {
                            if (damagePlan.entity.getOArmor(specDamage.loc, false) > specDamage.setArmorTo) {
                                LogManager.getLogger().debug(String.format("\tSet armor value for (%s) to %d",
                                        damagePlan.entity.getLocationName(specDamage.loc), specDamage.setArmorTo));

                                // Battle Armor Handled Differently
                                // If armor set to Zero kill the Armor sport
                                // which represents one member of the squad
                                if (damagePlan.entity instanceof BattleArmor) {
                                    if (specDamage.setArmorTo == 0) {
                                        damagePlan.entity.setArmor(IArmorState.ARMOR_DOOMED, specDamage.loc, false);
                                        damagePlan.entity.setInternal(IArmorState.ARMOR_DOOMED, specDamage.loc);
                                    } else {
                                        // For some reason setting armor to 1 will result in 2 armor points
                                        // left on the GUI. Dont know why but adjust here!
                                        damagePlan.entity.setArmor(specDamage.setArmorTo - 1, specDamage.loc);
                                    }
                                } else {
                                    damagePlan.entity.setArmor(specDamage.setArmorTo, specDamage.loc);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Loop through Crit Hits
        for (CritHitPlan chp : critHitPlans) {
            LogManager.getLogger().debug("Applying critical hits to " + chp.entity.getShortName());
            for (CritHit critHit : chp.critHits) {
                // Apply a critical hit to the indicated slot.
                if (chp.entity.locations() <= critHit.loc) {
                    LogManager.getLogger().error("Invalid location specified " + critHit.loc);
                } else {
                    // Make sure that we have crit spot to hit
                    if ((chp.entity instanceof Mech) || (chp.entity instanceof Protomech)) {
                        // Is this a torso weapon slot?
                        CriticalSlot cs = null;
                        if ((chp.entity instanceof Protomech)
                                && (Protomech.LOC_TORSO == critHit.loc)
                                && ((Protomech.SYSTEM_TORSO_WEAPON_A == critHit.slot) || (Protomech.SYSTEM_TORSO_WEAPON_B == critHit.slot))) {
                            cs = new CriticalSlot(CriticalSlot.TYPE_SYSTEM, critHit.slot);
                        }
                        // Is this a valid slot number?
                        else if ((critHit.slot < 0)
                                || (critHit.slot > chp.entity.getNumberOfCriticals(critHit.loc))) {
                            LogManager.getLogger().error(String.format("%s - invalid slot specified %d: %d",
                                    chp.entity.getShortName(), critHit.loc, (critHit.slot + 1)));
                        }
                        // Get the slot from the entity.
                        else {
                            cs = chp.entity.getCritical(critHit.loc, critHit.slot);
                        }

                        // Ignore invalid, unhittable, and damaged slots.
                        if ((null == cs) || !cs.isHittable()) {
                            LogManager.getLogger().error(String.format("%s - slot not hittable %d: %d",
                                    chp.entity.getShortName(), critHit.loc, (critHit.slot + 1)));
                        } else {
                            LogManager.getLogger().debug("[s.applyCriticalHit(chp.entity, ch.loc, cs, false)]");
                            m.applyCriticalHit(chp.entity, critHit.loc, cs, false, 0, false);
                        }
                    }
                    // Handle Tanks differently.
                    else if (chp.entity instanceof Tank) {
                        if ((critHit.slot < 0) || (critHit.slot >= 6)) {
                            LogManager.getLogger().error(String.format("%s - invalid slot specified %d: %d",
                                    chp.entity.getShortName(), critHit.loc, (critHit.slot + 1)));
                        } else {
                            CriticalSlot cs = new CriticalSlot(CriticalSlot.TYPE_SYSTEM, critHit.slot + 1);
                            LogManager.getLogger().debug("[s.applyCriticalHit(chp.entity, ch.loc, cs, false)]");
                            m.applyCriticalHit(chp.entity, Entity.NONE, cs, false, 0, false);
                        }
                    }
                }
            }
        }

        // Loop throught Set Ammo To
        for (SetAmmoPlan sap : ammoPlans) {
            LogManager.getLogger().debug("Applying ammo adjustment to " + sap.entity.getShortName());
            for (SetAmmoType sa : sap.ammoSetType) {
                // Limit to 'Mechs for now (needs to be extended later)
                if (sap.entity instanceof Mech) {
                    if (sa.slot < sap.entity.getNumberOfCriticals(sa.loc)) {
                        CriticalSlot cs = sap.entity.getCritical(sa.loc, sa.slot);
                        if (cs != null) {
                            Mounted ammo = sap.entity.getCritical(sa.loc, sa.slot).getMount();
                            if (ammo == null) {
                                LogManager.getLogger().error(String.format("%s - invalid slot specified %d: %d",
                                        sap.entity.getShortName(), sa.loc, sa.slot + 1));
                            } else if (ammo.getType() instanceof AmmoType) {
                                AmmoType newAmmoType = getValidAmmoType(m.getGame(), ammo, sa.type);
                                if (newAmmoType != null) {
                                    ammo.changeAmmoType(newAmmoType);
                                } else {
                                    LogManager.getLogger().warn(String.format("Illegal ammo type '%s' for unit %s, slot %s",
                                            sa.type, sap.entity.getDisplayName(), ammo.getName()));
                                }
                            }
                        }
                    }
                }
            }

            for (SetAmmoTo sa : sap.ammoSetTo) {
                // Only can be done against Mechs
                if (sap.entity instanceof Mech) {
                    if (sa.slot < sap.entity.getNumberOfCriticals(sa.loc)) {
                        // Get the piece of equipment and check to make sure it
                        // is a ammo item then set its amount!
                        CriticalSlot cs = sap.entity.getCritical(sa.loc, sa.slot);
                        if (cs != null) {
                            Mounted ammo = sap.entity.getCritical(sa.loc, sa.slot).getMount();
                            if (ammo == null) {
                                LogManager.getLogger().error(String.format("%s - invalid slot specified %d: %d",
                                        sap.entity.getShortName(), sa.loc, sa.slot + 1));
                            } else if (ammo.getType() instanceof AmmoType) {
                                // Also make sure we dont exceed the max allowed
                                ammo.setShotsLeft(Math.min(sa.setAmmoTo, ammo.getBaseShotsLeft()));
                            }
                        }
                    }
                }
            }
        }
    }

    private void parsePlanetaryConditions(Game g, ScenarioV1 p) {
        if (p.containsKey(PARAM_PLANETCOND_TEMP)) {
            g.getPlanetaryConditions().setTemperature(Integer.parseInt(p.getString(PARAM_PLANETCOND_TEMP)));
        }

        if (p.containsKey(PARAM_PLANETCOND_GRAV)) {
            g.getPlanetaryConditions().setGravity(Float.parseFloat(p.getString(PARAM_PLANETCOND_GRAV)));
        }

        if (p.containsKey(PARAM_PLANETCOND_FOG)) {
            g.getPlanetaryConditions().setFog(Fog.getFog(StringUtil.toInt(p.getString(PARAM_PLANETCOND_FOG), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_ATMOS)) {
            g.getPlanetaryConditions().setAtmosphere(Atmosphere.getAtmosphere(StringUtil.toInt(p.getString(PARAM_PLANETCOND_ATMOS),0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_LIGHT)) {
            g.getPlanetaryConditions().setLight(Light.getLight(StringUtil.toInt(p.getString(PARAM_PLANETCOND_LIGHT), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WEATHER)) {
            g.getPlanetaryConditions().setWeather(Weather.getWeather(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WEATHER), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WIND)) {
            g.getPlanetaryConditions().setWind(Wind.getWind(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WIND),0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDDIR)) {
            g.getPlanetaryConditions().setWindDirection(WindDirection.getWindDirection(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WINDDIR),0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDSHIFTINGDIR)) {
            g.getPlanetaryConditions().setShiftingWindDirection(parseBoolean(p, PARAM_PLANETCOND_WINDSHIFTINGDIR, false));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDSHIFTINGSTR)) {
            g.getPlanetaryConditions().setShiftingWindStrength(parseBoolean(p, PARAM_PLANETCOND_WINDSHIFTINGSTR, false));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDMIN)) {
            g.getPlanetaryConditions().setWindMin(Wind.getWind(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WINDMIN), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_WINDMAX)) {
            g.getPlanetaryConditions().setWindMax(Wind.getWind(StringUtil.toInt(p.getString(PARAM_PLANETCOND_WINDMAX), 0)));
        }

        if (p.containsKey(PARAM_PLANETCOND_EMI)) {
            EMI emi = parseBoolean(p, PARAM_PLANETCOND_EMI, false) ? EMI.EMI : EMI.EMI_NONE;
            g.getPlanetaryConditions().setEMI(emi);
        }

        if (p.containsKey(PARAM_PLANETCOND_TERRAINCHANGES)) {
            g.getPlanetaryConditions().setTerrainAffected(parseBoolean(p, PARAM_PLANETCOND_TERRAINCHANGES, true));
        }

        if (p.containsKey(PARAM_PLANETCOND_BLOWINGSAND)) {
            BlowingSand blowingSand = parseBoolean(p, PARAM_PLANETCOND_BLOWINGSAND, false) ? BlowingSand.BLOWING_SAND : BlowingSand.BLOWING_SAND_NONE;
            g.getPlanetaryConditions().setBlowingSand(blowingSand);
        }
    }

    private Collection<Entity> buildFactionEntities(ScenarioV1 p, Player player) throws ScenarioLoaderException {
        String faction = player.getName();
        Pattern unitPattern = Pattern.compile(String.format("^Unit_\\Q%s\\E_[^_]+$", faction));
        Pattern unitDataPattern = Pattern.compile(String.format("^(Unit_\\Q%s\\E_[^_]+)_([A-Z][^_]+)$", faction));

        Map<String, Entity> entities = new HashMap<>();

        // Gather all defined units
        for (String key : p.keySet()) {
            if (unitPattern.matcher(key).matches() && (p.getNumValues(key) > 0)) {
                if (p.getNumValues(key) > 1) {
                    LogManager.getLogger().error(String.format("Scenario loading: Unit declaration %s found %d times",
                            key, p.getNumValues(key)));
                    throw new ScenarioLoaderException("ScenarioLoaderException.multipleUnitDeclarations", key);
                }
                entities.put(key, parseEntityLine(p.getString(key)));
            }
        }

        // Add other information
        for (String key: p.keySet()) {
            Matcher dataMatcher = unitDataPattern.matcher(key);
            if (dataMatcher.matches()) {
                String unitKey = dataMatcher.group(1);
                if (!entities.containsKey(unitKey)) {
                    LogManager.getLogger().warn("Scenario loading: Data for undeclared unit encountered, ignoring: " + key);
                    continue;
                }
                Entity e = entities.get(unitKey);
                switch (dataMatcher.group(2)) {
                    case PARAM_DAMAGE:
                        for (String val : p.get(key)) {
                            damagePlans.add(new DamagePlan(e, Integer.parseInt(val)));
                        }
                        break;
                    case PARAM_SPECIFIC_DAMAGE:
                        DamagePlan dp = new DamagePlan(e);
                        for (String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            dp.addSpecificDamage(val);
                        }
                        damagePlans.add(dp);
                        break;
                    case PARAM_CRITICAL_HIT:
                        CritHitPlan chp = new CritHitPlan(e);
                        for (String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            chp.addCritHit(val);
                        }
                        critHitPlans.add(chp);
                        break;
                    case PARAM_AMMO_AMOUNT:
                        SetAmmoPlan amountSap = new SetAmmoPlan(e);
                        for (String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            amountSap.addSetAmmoTo(val);
                        }
                        ammoPlans.add(amountSap);
                        break;
                    case PARAM_AMMO_TYPE:
                        SetAmmoPlan typeSap = new SetAmmoPlan(e);
                        for (String val : p.getString(key).split(SEPARATOR_COMMA, -1)) {
                            typeSap.addSetAmmoType(val);
                        }
                        ammoPlans.add(typeSap);
                        break;
                    case PARAM_PILOT_HITS:
                        int hits = Integer.parseInt(p.getString(key));
                        e.getCrew().setHits(Math.min(hits, 5), 0);
                        break;
                    case PARAM_EXTERNAL_ID:
                        e.setExternalIdAsString(p.getString(key));
                        break;
                    case PARAM_ADVANTAGES:
                        parseAdvantages(e, p.getString(key, SEPARATOR_SPACE));
                        break;
                    case PARAM_AUTO_EJECT:
                        parseAutoEject(e, p.getString(key));
                        break;
                    case PARAM_COMMANDER:
                        parseCommander(e, p.getString(key));
                        break;
                    case PARAM_DEPLOYMENT_ROUND:
                        int round = Integer.parseInt(p.getString(key));
                        if (round > 0) {
                            LogManager.getLogger().debug(String.format("%s will be deployed before round %d",
                                    e.getDisplayName(), round));
                            e.setDeployRound(round);
                            e.setDeployed(false);
                            e.setNeverDeployed(false);
                            e.setPosition(null);
                        }
                        break;
                    case PARAM_CAMO:
                        final Camouflage camouflage = parseCamouflage(p.getString(key));
                        if (!camouflage.isDefault()) {
                            e.setCamouflage(camouflage);
                        }
                        break;
                    case PARAM_ALTITUDE:
                        int altitude = Math.min(Integer.parseInt(p.getString(key)), 10);
                        if (e.isAero()) {
                            e.setAltitude(altitude);
                            if (altitude <= 0) {
                                ((IAero) e).land();
                            }
                        } else {
                            LogManager.getLogger().warn(String.format("Altitude setting for a non-aerospace unit %s; ignoring",
                                    e.getShortName()));
                        }
                        break;
                    default:
                        LogManager.getLogger().error("Scenario loading: Unknown unit data key " + key);
                }
            }
        }

        return entities.values();
    }

    private Entity parseEntityLine(String s) throws ScenarioLoaderException {
        try {
            String[] parts = s.split(SEPARATOR_COMMA, -1);
            int i;
            MechSummary ms = MechSummaryCache.getInstance().getMech(parts[0]);
            if (ms == null) {
                throw new ScenarioLoaderException("ScenarioLoaderException.missingRequiredEntity", parts[0]);
            }
            LogManager.getLogger().debug(String.format("Loading %s", ms.getName()));
            Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();

            // The following section is used to determine if part 4 of the string includes gender or not
            // The regex is used to match a number that might be negative. As the direction is never
            // a number, if a number is found it must be gender.
            // The i value must be included to ensure that the correct indexes are used for the
            // direction calculation below.
            if ((parts.length > 4) && parts[4].matches("-?\\d+")) {
                e.setCrew(new Crew(e.getCrew().getCrewType(), parts[1], 1,
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                        Gender.parseFromString(parts[4]), Boolean.parseBoolean(parts[5]), null));
                i = 6; // direction will be part 6, as the scenario has the gender of its pilots included
            } else {
                e.setCrew(new Crew(e.getCrew().getCrewType(), parts[1], 1,
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                        RandomGenderGenerator.generate(), e.isClan(), null));
                i = 4; // direction will be part 4, as the scenario does not contain gender
            }

            // This uses the i value to ensure it is calculated correctly
            if (parts.length >= 7) {
                String direction = parts[i++].toUpperCase(Locale.ROOT); // grab value at i, then increment
                switch (direction) {
                    case "N":
                        e.setFacing(0);
                        break;
                    case "NW":
                        e.setFacing(5);
                        break;
                    case "SW":
                        e.setFacing(4);
                        break;
                    case "S":
                        e.setFacing(3);
                        break;
                    case "SE":
                        e.setFacing(2);
                        break;
                    case "NE":
                        e.setFacing(1);
                        break;
                    default:
                        break;
                }
                int x = Integer.parseInt(parts[i++]) - 1;
                int y = Integer.parseInt(parts[i]) - 1;
                e.setPosition(new Coords(x, y));
                e.setDeployed(true);
            }
            return e;
        } catch (NumberFormatException | IndexOutOfBoundsException | EntityLoadingException ex) {
            LogManager.getLogger().error("", ex);
            throw new ScenarioLoaderException("ScenarioLoaderException.unparsableEntityLine", s);
        }
    }

    private void parseAdvantages(Entity entity, String adv) {
        String[] advantages = adv.split(SEPARATOR_SPACE, -1);

        for (String curAdv : advantages) {
            String[] advantageData = curAdv.split(SEPARATOR_COLON, -1);
            IOption option = entity.getCrew().getOptions().getOption(advantageData[0]);
            if (option == null) {
                LogManager.getLogger().error(String.format("Ignoring invalid pilot advantage '%s'", curAdv));
            } else {
                LogManager.getLogger().debug(String.format("Adding pilot advantage '%s' to %s",
                        curAdv, entity.getDisplayName()));
                if (advantageData.length > 1) {
                    option.setValue(advantageData[1]);
                } else {
                    option.setValue(true);
                }
            }
        }
    }

    private void parseAutoEject(Entity entity, String eject) {
        if (entity instanceof Mech) {
            Mech mech = (Mech) entity;
            mech.setAutoEject(Boolean.parseBoolean(eject));
        }
    }

    private void parseCommander(Entity entity, String commander) {
        entity.setCommander(Boolean.parseBoolean(commander));
    }

    private Camouflage parseCamouflage(final @Nullable String camouflage) {
        if ((camouflage == null) || camouflage.isBlank()) {
            return new Camouflage();
        }
        final String[] camouflageParameters = camouflage.split(SEPARATOR_COMMA, -1);
        if (camouflageParameters.length == 2) {
            return new Camouflage(camouflageParameters[0], camouflageParameters[1]);
        } else {
            LogManager.getLogger().error("Attempted to parse illegal camouflage parameter array of size " + camouflageParameters.length + " from " + camouflage);
            return new Camouflage();
        }
    }

    private String getFactionParam(String faction, String param) {
        return param + SEPARATOR_UNDERSCORE + faction;
    }

    private Collection<Player> createPlayers(ScenarioV1 p) throws ScenarioLoaderException {
        String sFactions = p.getString(PARAM_FACTIONS);
        if ((sFactions == null) || sFactions.isEmpty()) {
            throw new ScenarioLoaderException("ScenarioLoaderException.missingFactions");
        }
        String[] factions = sFactions.split(SEPARATOR_COMMA, -1);
        List<Player> result = new ArrayList<>(factions.length);

        int playerId = 0;
        int teamId = 0;
        for (String faction : factions) {
            Player player = new Player(playerId, faction);
            result.add(player);
            playerId++;

            // scenario players start out as ghosts to be logged into
            player.setGhost(true);

            String loc = p.getString(getFactionParam(faction, PARAM_LOCATION));
            if (loc == null) {
                loc = "Any";
            }
            int dir = Math.max(findIndex(IStartingPositions.START_LOCATION_NAMES, loc), 0);
            player.setStartingPos(dir);

            final Camouflage camouflage = parseCamouflage(p.getString(getFactionParam(faction, PARAM_CAMO)));
            if (!camouflage.isDefault()) {
                player.setCamouflage(camouflage);
            }

            String team = p.getString(getFactionParam(faction, PARAM_TEAM));
            if ((team != null) && !team.isEmpty()) {
                try {
                    teamId = Integer.parseInt(team);
                } catch (NumberFormatException ignored) {
                    teamId++;
                }
            } else {
                teamId++;
            }
            player.setTeam(Math.min(teamId, Player.TEAM_NAMES.length - 1));

            String minefields = p.getString(getFactionParam(faction, PARAM_MINEFIELDS));
            if ((minefields != null) && !minefields.isEmpty()) {
                String[] mines = minefields.split(SEPARATOR_COMMA, -1);
                if (mines.length >= 3) {
                    try {
                        int minesConventional = Integer.parseInt(mines[0]);
                        int minesCommand = Integer.parseInt(mines[1]);
                        int minesVibra = Integer.parseInt(mines[2]);
                        player.setNbrMFConventional(minesConventional);
                        player.setNbrMFCommand(minesCommand);
                        player.setNbrMFVibra(minesVibra);
                    } catch (NumberFormatException nfex) {
                        LogManager.getLogger().error(String.format("Format error with minefields string '%s' for %s",
                                minefields, faction));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Load board files and create the megaboard.
     */
    private Board createBoard(ScenarioV1 p) throws ScenarioLoaderException {
        int mapWidth = 16, mapHeight = 17;
        if (p.getString(PARAM_MAP_WIDTH) == null) {
            LogManager.getLogger().info("No map width specified; using " + mapWidth);
        } else {
            mapWidth = Integer.parseInt(p.getString(PARAM_MAP_WIDTH));
        }

        if (p.getString(PARAM_MAP_HEIGHT) == null) {
            LogManager.getLogger().info("No map height specified; using " + mapHeight);
        } else {
            mapHeight = Integer.parseInt(p.getString(PARAM_MAP_HEIGHT));
        }

        int nWidth = 1, nHeight = 1;
        if (p.getString(PARAM_BOARD_WIDTH) == null) {
            LogManager.getLogger().info("No board width specified; using " + nWidth);
        } else {
            nWidth = Integer.parseInt(p.getString(PARAM_BOARD_WIDTH));
        }

        if (p.getString(PARAM_BOARD_HEIGHT) == null) {
            LogManager.getLogger().info("No board height specified; using " + nHeight);
        } else {
            nHeight = Integer.parseInt(p.getString(PARAM_BOARD_HEIGHT));
        }

        LogManager.getLogger().debug(String.format("Mapsheets are %d by %d hexes.", mapWidth, mapHeight));
        LogManager.getLogger().debug(String.format("Constructing %d by %d board.", nWidth, nHeight));
        int cf = 0;
        if (p.getString(PARAM_BRIDGE_CF) == null) {
            LogManager.getLogger().debug("No CF for bridges defined. Using map file defaults.");
        } else {
            cf = Integer.parseInt(p.getString(PARAM_BRIDGE_CF));
            LogManager.getLogger().debug("Overriding map-defined bridge CFs with " + cf);
        }
        // load available boards
        // basically copied from Server.java. Should get moved somewhere neutral
        List<String> boards = new ArrayList<>();

        // Find subdirectories given in the scenario file
        List<String> allDirs = new LinkedList<>();
        // "" entry stands for the boards base directory
        allDirs.add("");

        if (p.getString(PARAM_MAP_DIRECTORIES) != null) {
            allDirs = Arrays.asList(p.getString(PARAM_MAP_DIRECTORIES)
                    .split(SEPARATOR_COMMA, -1));
        }

        for (String dir: allDirs) {
            File curDir = new File(Configuration.boardsDir(), dir);
            if (curDir.exists()) {
                for (String file : curDir.list()) {
                    if (file.toLowerCase(Locale.ROOT).endsWith(FILE_SUFFIX_BOARD)) {
                        boards.add(dir+"/"+file.substring(0, file.length() - FILE_SUFFIX_BOARD.length()));
                    }
                }
            }
        }

        Board[] ba = new Board[nWidth * nHeight];
        Queue<String> maps = new LinkedList<>(
                Arrays.asList(p.getString(PARAM_MAPS).split(SEPARATOR_COMMA, -1)));
        List<Boolean> rotateBoard = new ArrayList<>();
        for (int x = 0; x < nWidth; x++) {
            for (int y = 0; y < nHeight; y++) {
                int n = (y * nWidth) + x;
                String board = MAP_RANDOM;
                if (!maps.isEmpty()) {
                    board = maps.poll();
                }
                LogManager.getLogger().debug(String.format("(%d,%d) %s", x, y, board));

                boolean isRotated = false;
                if (board.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                    isRotated = true;
                    board = board.substring(Board.BOARD_REQUEST_ROTATION.length());
                }

                String sBoardFile;
                if (board.equals(MAP_RANDOM)) {
                    sBoardFile = (boards.get(Compute.randomInt(boards.size()))) + FILE_SUFFIX_BOARD;
                } else {
                    sBoardFile = board + FILE_SUFFIX_BOARD;
                }
                File fBoard = new MegaMekFile(Configuration.boardsDir(), sBoardFile).getFile();
                if (!fBoard.exists()) {
                    throw new ScenarioLoaderException("ScenarioLoaderException.nonexistentBoard", board);
                }
                ba[n] = new Board();
                ba[n].load(new MegaMekFile(Configuration.boardsDir(), sBoardFile).getFile());
                if (cf > 0) {
                    ba[n].setBridgeCF(cf);
                }
                BoardUtilities.flip(ba[n], isRotated, isRotated);
                rotateBoard.add(isRotated);
            }
        }

        // if only one board just return it.
        if (ba.length == 1) {
            return ba[0];
        }
        // construct the big board
        return BoardUtilities.combine(mapWidth, mapHeight, nWidth, nHeight, ba, rotateBoard, MapSettings.MEDIUM_GROUND);
    }

    /**
     * Parses out the external game id from the scenario file
     */
    private int parseExternalGameId(ScenarioV1 p) {
        String sExternalId = p.getString(PARAM_GAME_EXTERNAL_ID);
        int ExternalGameId = 0;
        if (sExternalId != null) {
            ExternalGameId = Integer.parseInt(sExternalId);
        }
        return ExternalGameId;
    }

    public boolean hasFixedGameOptions() {
        return fixedGameOptions;
    }


    @Override
    public boolean hasFixedPlanetaryConditions() {
        return fixedPlanetCond;
    }

    @Override
    public boolean isSinglePlayer() {
        return singlePlayer;
    }

    /**
     * Parses a boolean value. When the key is not present, returns the given
     * defaultValue. When the key is present, interprets "true" and "on"  and "1"
     * as true and everything else as false.
     */
    private boolean parseBoolean(ScenarioV1 p, String key, boolean defaultValue) {
        boolean result = defaultValue;
        if (p.containsKey(key)) {
            if (p.getString(key).equalsIgnoreCase("true")
                    || p.getString(key).equalsIgnoreCase("on")
                    || p.getString(key).equalsIgnoreCase("1")) {
                result = true;
            } else {
                result = false;
            }
        }
        return result;
    }

    /**
     * This is used specify the critical hit location
     */
    private static class CritHit {
        public int loc;
        public int slot;

        public CritHit(int l, int s) {
            loc = l;
            slot = s;
        }
    }

    /**
     * This class is used to store the critical hit plan for a entity it is
     * loaded from the scenario file. It contains a vector of CritHit.
     */
    private class CritHitPlan {
        public Entity entity;
        List<CritHit> critHits = new ArrayList<>();

        public CritHitPlan(Entity e) {
            entity = e;
        }

        public void addCritHit(String s) {
            int ewSpot = s.indexOf(':');
            int loc = Integer.parseInt(s.substring(0, ewSpot));
            int slot = Integer.parseInt(s.substring(ewSpot + 1));

            critHits.add(new CritHit(loc, slot - 1));
        }
    }

    /**
     * This is used to store the armor to change ammo at a given location
     */
    private static class SetAmmoTo {
        public final int loc;
        public final int slot;
        public final int setAmmoTo;

        public SetAmmoTo(int loc, int slot, int setAmmoTo) {
            this.loc = loc;
            this.slot = slot;
            this.setAmmoTo = setAmmoTo;
        }
    }

    private static class SetAmmoType {
        public final int loc;
        public final int slot;
        public final String type;

        public SetAmmoType(int loc, int slot, String type) {
            this.loc = loc;
            this.slot = slot;
            this.type = type;
        }
    }

    /**
     * This class is used to store the ammo Adjustments it is loaded from the
     * scenario file. It contains a vector of SetAmmoTo.
     */
    private class SetAmmoPlan {
        public final Entity entity;
        public final List<SetAmmoTo> ammoSetTo = new ArrayList<>();
        public final List<SetAmmoType> ammoSetType = new ArrayList<>();

        public SetAmmoPlan(Entity e) {
            entity = e;
        }

        /**
         * Converts 2:1-34 to Location 2 Slot 1 set Ammo to 34
         */
        public void addSetAmmoTo(String s) {
            int ewSpot = s.indexOf(':');
            int amSpot = s.indexOf('-');
            if (s.isEmpty() || (ewSpot < 1) || (amSpot < ewSpot)) {
                return;
            }
            int loc = Integer.parseInt(s.substring(0, ewSpot));
            int slot = Integer.parseInt(s.substring(ewSpot + 1, amSpot));
            int setTo = Integer.parseInt(s.substring(amSpot + 1));

            ammoSetTo.add(new SetAmmoTo(loc, slot - 1, setTo));
        }

        public void addSetAmmoType(String s) {
            int ewSpot = s.indexOf(':');
            int atSpot = s.indexOf('-');
            if (s.isEmpty() || (ewSpot < 1) || (atSpot < ewSpot)) {
                return;
            }
            int loc = Integer.parseInt(s.substring(0, ewSpot));
            int slot = Integer.parseInt(s.substring(ewSpot + 1, atSpot));

            ammoSetType.add(new SetAmmoType(loc, slot - 1, s.substring(atSpot + 1)));
        }
    }

    /**
     * This is used specify the one damage location
     */
    private static class SpecDam {
        public final int loc;
        public final int setArmorTo;
        public final boolean rear;
        public final boolean internal;

        public SpecDam(int Location, int SetArmorTo, boolean RearHit, boolean Internal) {
            loc = Location;
            setArmorTo = SetArmorTo;
            rear = RearHit;
            internal = Internal;
        }
    }

    /**
     * This class is used to store the damage plan for a entity it is loaded
     * from the scenario file. It contains a vector of SpecDam.
     */
    private class DamagePlan {
        public final Entity entity;
        public final int nBlocks;
        public final List<SpecDam> specificDammage = new ArrayList<>();

        public DamagePlan(Entity e, int n) {
            entity = e;
            nBlocks = n;
        }

        public DamagePlan(Entity e) {
            entity = e;
            nBlocks = 0;
        }

        /**
         * Converts N2:1 to Nornam hit to location 2 set armor to 1!
         */
        public void addSpecificDamage(String s) {
            int ewSpot = s.indexOf(':');
            if (s.isEmpty() || (ewSpot < 1)) {
                return;
            }
            int loc = Integer.parseInt(s.substring(1, ewSpot));
            int setTo = Integer.parseInt(s.substring(ewSpot + 1));
            boolean rear = (s.charAt(0) == 'R');
            boolean internal = (s.charAt(0) == 'I');

            specificDammage.add(new SpecDam(loc, setTo, rear, internal));
        }
    }

}