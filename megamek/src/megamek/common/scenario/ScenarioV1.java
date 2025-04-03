/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.common.scenario;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.client.generator.RandomGenderGenerator;
import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.enums.Gender;
import megamek.common.equipment.AmmoMounted;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.Atmosphere;
import megamek.common.planetaryconditions.BlowingSand;
import megamek.common.planetaryconditions.EMI;
import megamek.common.planetaryconditions.Fog;
import megamek.common.planetaryconditions.Light;
import megamek.common.planetaryconditions.Weather;
import megamek.common.planetaryconditions.Wind;
import megamek.common.planetaryconditions.WindDirection;
import megamek.common.util.BoardUtilities;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.server.IGameManager;
import megamek.server.totalwarfare.TWGameManager;

/**
 * This class holds all scenario info loaded from a scenario (.mms) file. It is a map of constants given in
 * {@link ScenarioLoader} to a list of data for that constant.
 */
public class ScenarioV1 extends HashMap<String, Collection<String>> implements Scenario {
    private static final MMLogger LOGGER = MMLogger.create(ScenarioV1.class);

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
    private static final String PARAM_SINGLE_PLAYER = "SinglePlayer";

    private static final String PARAM_PLANET_CONDITIONS_FIXED = "FixedPlanetaryConditions";

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
    private static final String PARAM_CRITICAL_HIT = "CriticalHit";
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

    private static final String PARAM_PLANET_CONDITIONS_TEMP = "PlanetaryConditionsTemperature";
    private static final String PARAM_PLANET_CONDITIONS_GRAV = "PlanetaryConditionsGravity";
    private static final String PARAM_PLANET_CONDITIONS_LIGHT = "PlanetaryConditionsLight";
    private static final String PARAM_PLANET_CONDITIONS_WEATHER = "PlanetaryConditionsWeather";
    private static final String PARAM_PLANET_CONDITIONS_WIND = "PlanetaryConditionsWind";
    private static final String PARAM_PLANET_CONDITIONS_WIND_DIRECTION = "PlanetaryConditionsWindDir";
    private static final String PARAM_PLANET_CONDITIONS_ATMOSPHERE = "PlanetaryConditionsAtmosphere";
    private static final String PARAM_PLANET_CONDITIONS_FOG = "PlanetaryConditionsFog";
    private static final String PARAM_PLANET_CONDITIONS_WIND_SHIFTING_STRENGTH = "PlanetaryConditionsWindShiftingStr";
    private static final String PARAM_PLANET_CONDITIONS_WIND_MIN = "PlanetaryConditionsWindMin";
    private static final String PARAM_PLANET_CONDITIONS_WIND_MAX = "PlanetaryConditionsWindMax";
    private static final String PARAM_PLANET_CONDITIONS_WIND_SHIFTING_DIRECTION = "PlanetaryConditionsWindShiftingDir";
    private static final String PARAM_PLANET_CONDITIONS_BLOWING_SAND = "PlanetaryConditionsBlowingSand";
    private static final String PARAM_PLANET_CONDITIONS_EMI = "PlanetaryConditionsEMI";
    private static final String PARAM_PLANET_CONDITIONS_TERRAIN_CHANGES = "PlanetaryConditionsAllowTerrainChanges";

    private File file;

    private final List<DamagePlan> damagePlans = new ArrayList<>();
    // Used to store Critical Hits
    private final List<CriticalHitPlan> criticalHitPlans = new ArrayList<>();
    // Used to set ammo Spec Amounts
    private final List<SetAmmoPlan> ammoPlans = new ArrayList<>();

    /** When true, the Game Options Dialog is skipped. */
    private boolean fixedGameOptions = false;

    /** When true, the Planetary Conditions Dialog is skipped. */
    private boolean fixedPlanetCond;

    /**
     * When true, the Player assignment/camo Dialog and the host dialog are skipped. The first faction (player) is
     * assumed to be the local player and the rest are assumed to be Princess.
     */
    private boolean singlePlayer;

    ScenarioV1(File file) throws IOException {
        this.file = file;
        load();
    }

    @Override
    public ScenarioV1 clone() {
        try {
            ScenarioV1 scenarioV1 = (ScenarioV1) super.clone();
            scenarioV1.file = file;
            load();
            scenarioV1.damagePlans.clear();
            scenarioV1.damagePlans.addAll(damagePlans);
            scenarioV1.criticalHitPlans.clear();
            scenarioV1.criticalHitPlans.addAll(criticalHitPlans);
            scenarioV1.ammoPlans.clear();
            scenarioV1.ammoPlans.addAll(ammoPlans);
            scenarioV1.fixedGameOptions = fixedGameOptions;
            scenarioV1.fixedPlanetCond = fixedPlanetCond;
            return scenarioV1;
        } catch (IOException e) {
            LOGGER.error(e, "Unable to clone a ScenarioV1 object: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public String getName() {
        return getString(NAME);
    }

    @Override
    public String getDescription() {
        return getString(DESCRIPTION);
    }

    @Override
    public String getFileName() {
        return file.toString();
    }

    public void load() throws IOException {
        for (String line : readLines(file, s -> s.trim().startsWith("#"))) {
            if (!line.contains("=")) {
                LOGGER.error("Missing = in scenario line: {}", line);
            } else {
                String[] elements = line.split(SEPARATOR_PROPERTY, 2);
                String keyword = elements[0].trim();
                put(keyword, elements[1].trim());
            }
        }
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
        StringBuilder stringBuilder = new StringBuilder();
        for (String value : values) {
            if (firstElement) {
                firstElement = false;
            } else {
                stringBuilder.append(separator);
            }
            stringBuilder.append(value);
        }
        return stringBuilder.toString();
    }

    /**
     * @return the number of values for this key in the file
     */
    public int getNumValues(String key) {
        Collection<String> values = get(key);
        return (values == null) ? 0 : values.size();
    }

    public int parseInt(String key, int defaultValue) {
        String value = getString(key);
        return MathUtility.parseInt(value, defaultValue);
    }

    public float parseFloat(String key, float defaultValue) {
        String value = getString(key);
        return MathUtility.parseFloat(value, defaultValue);
    }

    /**
     * Parses a boolean value. When the key is not present, returns the given defaultValue. When the key is present,
     * interprets "true" and "on" and "1" as true and everything else as false.
     */
    private boolean parseBoolean(String key, boolean defaultValue) {
        if (containsKey(key)) {
            return getString(key).equalsIgnoreCase("true") ||
                         getString(key).equalsIgnoreCase("on") ||
                         getString(key).equalsIgnoreCase("1");
        }

        return defaultValue;
    }

    @Override
    public Collection<String> get(Object key) {
        return super.get(key);
    }

    @Override
    public IGame createGame() throws ScenarioLoaderException {
        LOGGER.info("Loading scenario from {}", file);
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
                ++entityId;
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
        fixedGameOptions = parseBoolean(PARAM_GAME_OPTIONS_FIXED, false);

        // set wind
        parsePlanetaryConditions(game, this);
        game.getPlanetaryConditions().determineWind();
        fixedPlanetCond = parseBoolean(PARAM_PLANET_CONDITIONS_FIXED, false);
        singlePlayer = parseBoolean(PARAM_SINGLE_PLAYER, false);

        // Set up the teams (for initiative)
        game.setupTeams();
        game.setPhase(GamePhase.STARTING_SCENARIO);
        game.setupDeployment();
        game.setExternalGameId(parseExternalGameId(this));
        game.setVictoryContext(new HashMap<>());
        game.createVictoryConditions();
        return game;
    }

    // TODO : legal/valid ammo type handling and game options, since they are set at this point
    private AmmoType getValidAmmoType(Game game, Mounted<?> mounted, String ammoString) {
        final Entity entity = mounted.getEntity();
        final int year = game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        final EquipmentType currentAmmoType = mounted.getType();
        final Mounted<?> currentWeapon = mounted.getLinkedBy();
        final EquipmentType currentWeaponType = (null != currentWeapon) ? currentWeapon.getType() : null;
        final EquipmentType equipmentType = EquipmentType.get(ammoString);

        if (!(equipmentType instanceof AmmoType newAmmoType)) {
            LOGGER.error("Ammo type '{}' not found or not an AmmoType", ammoString);
            return null;
        }

        if (!newAmmoType.isLegal(year,
              SimpleTechLevel.getGameTechLevel(game),
              entity.isClan(),
              entity.isMixedTech(),
              game.getOptions().booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT))) {
            LOGGER.warn("Ammo {} (TL {}) is not legal for year {} (TL {})",
                  newAmmoType.getName(),
                  newAmmoType.getTechLevel(year),
                  year,
                  TechConstants.getGameTechLevel(game, entity.isClan()));
            return null;
        } else if (entity.isClan() &&
                         !game.getOptions().booleanOption(OptionsConstants.ALLOWED_CLAN_IGNORE_EQ_LIMITS)) {
            // Check for clan weapon restrictions
            if (newAmmoType.notAllowedByClanRules()) {
                LOGGER.warn("Ammo type {} not allowed by Clan rules", newAmmoType.getName());
                return null;
            }

            // Construct EnumSet with all the relevant
            final EnumSet<AmmoType.Munitions> munitionTypes = newAmmoType.getMunitionType();
            munitionTypes.add(AmmoType.Munitions.M_INCENDIARY_LRM);
        }

        if (AmmoType.canDeliverMinefield(newAmmoType) &&
                  !game.getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            LOGGER.warn("Minefield-creating ammo type {} forbidden by game rules", newAmmoType.getName());
            return null;
        }

        int weaponAmmoType = (currentWeaponType instanceof WeaponType weaponType) ? weaponType.getAmmoType() : 0;
        if ((newAmmoType.getRackSize() == ((AmmoType) currentAmmoType).getRackSize()) &&
                  (newAmmoType.hasFlag(AmmoType.F_BATTLEARMOR) == currentAmmoType.hasFlag(AmmoType.F_BATTLEARMOR)) &&
                  (newAmmoType.hasFlag(AmmoType.F_ENCUMBERING) == currentAmmoType.hasFlag(AmmoType.F_ENCUMBERING)) &&
                  (newAmmoType.getTonnage(entity) == currentAmmoType.getTonnage(entity)) &&
                  (newAmmoType.getAmmoType() == weaponAmmoType)) {
            return newAmmoType;
        } else {
            return null;
        }
    }

    /**
     * The damage procedures are built into a server object, so we delay dealing the random damage until a server is
     * made available to us.
     */
    @Override
    public void applyDamage(IGameManager iGameManager) {
        TWGameManager twGameManager = (TWGameManager) iGameManager;
        for (DamagePlan damagePlan : damagePlans) {
            LOGGER.debug("Applying damage to {}", damagePlan.entity.getShortName());
            for (int y = 0; y < damagePlan.nBlocks; ++y) {
                HitData hit = damagePlan.entity.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
                LOGGER.debug("[s.damageEntity(dp.entity, hit, 5)]");
                twGameManager.damageEntity(damagePlan.entity, hit, 5);
            }

            // Apply Spec Damage
            for (SpecDam specDamage : damagePlan.specificDamage) {
                if (damagePlan.entity.locations() <= specDamage.loc) {
                    // location is valid
                    LOGGER.error("\tInvalid location specified {}", specDamage.loc);
                } else {
                    // Infantry only take damage to "internal"
                    if (specDamage.internal || damagePlan.entity.isConventionalInfantry()) {
                        if (damagePlan.entity.getOInternal(specDamage.loc) > specDamage.setArmorTo) {
                            damagePlan.entity.setInternal(specDamage.setArmorTo, specDamage.loc);
                            LOGGER.debug("\tSet armor value for (internal {}) to {}",
                                  damagePlan.entity.getLocationName(specDamage.loc),
                                  specDamage.setArmorTo);
                            if (specDamage.setArmorTo == 0) {
                                LOGGER.debug("\tSection destroyed {}",
                                      damagePlan.entity.getLocationName(specDamage.loc));
                                damagePlan.entity.destroyLocation(specDamage.loc);
                            }
                        }
                    } else {
                        if (specDamage.rear && damagePlan.entity.hasRearArmor(specDamage.loc)) {
                            if (damagePlan.entity.getOArmor(specDamage.loc, true) > specDamage.setArmorTo) {
                                LOGGER.debug("\tSet armor value for (rear {}) to {}",
                                      damagePlan.entity.getLocationName(specDamage.loc),
                                      specDamage.setArmorTo);
                                damagePlan.entity.setArmor(specDamage.setArmorTo, specDamage.loc, true);
                            }
                        } else {
                            if (damagePlan.entity.getOArmor(specDamage.loc, false) > specDamage.setArmorTo) {
                                LOGGER.debug("\tSet armor value for ({}) to {}",
                                      damagePlan.entity.getLocationName(specDamage.loc),
                                      specDamage.setArmorTo);

                                // Battle Armor Handled Differently
                                // If armor set to Zero kill the Armor sport which represents one member of the squad
                                if (damagePlan.entity instanceof BattleArmor) {
                                    if (specDamage.setArmorTo == 0) {
                                        damagePlan.entity.setArmor(IArmorState.ARMOR_DOOMED, specDamage.loc, false);
                                        damagePlan.entity.setInternal(IArmorState.ARMOR_DOOMED, specDamage.loc);
                                    } else {
                                        // For some reason setting armor to 1 will result in 2 armor points left on
                                        // the GUI. Don't know why but adjust here!
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

        // Loop through Critical Hits
        for (CriticalHitPlan criticalHitPlan : criticalHitPlans) {
            LOGGER.debug("Applying critical hits to {}", criticalHitPlan.entity.getShortName());
            for (CriticalHit criticalHit : criticalHitPlan.criticalHits) {
                // Apply a critical hit to the indicated slot.
                if (criticalHitPlan.entity.locations() <= criticalHit.loc) {
                    LOGGER.error("Invalid location specified {}", criticalHit.loc);
                } else {
                    // Make sure that we have critical spot to hit
                    if ((criticalHitPlan.entity instanceof Mek) || (criticalHitPlan.entity instanceof ProtoMek)) {
                        // Is this a torso weapon slot?
                        CriticalSlot criticalSlot = null;
                        if ((criticalHitPlan.entity instanceof ProtoMek) &&
                                  (ProtoMek.LOC_TORSO == criticalHit.loc) &&
                                  ((ProtoMek.SYSTEM_TORSO_WEAPON_A == criticalHit.slot) ||
                                         (ProtoMek.SYSTEM_TORSO_WEAPON_B == criticalHit.slot))) {
                            criticalSlot = new CriticalSlot(CriticalSlot.TYPE_SYSTEM, criticalHit.slot);
                        }
                        // Is this a valid slot number?
                        else if ((criticalHit.slot < 0) ||
                                       (criticalHit.slot >
                                              criticalHitPlan.entity.getNumberOfCriticals(criticalHit.loc))) {
                            LOGGER.error("{} - invalid slot specified (Slot < 0 OR Slot > Number of Critical Slots) " +
                                               "{}: {}",
                                  criticalHitPlan.entity.getShortName(),
                                  criticalHit.loc,
                                  (criticalHit.slot + 1));
                        } else {
                            // Get the slot from the entity.
                            criticalSlot = criticalHitPlan.entity.getCritical(criticalHit.loc, criticalHit.slot);
                        }

                        // Ignore invalid, non-hittable, and damaged slots.
                        if ((null == criticalSlot) || !criticalSlot.isHittable()) {
                            LOGGER.error("{} - slot not hittable {}: {}",
                                  criticalHitPlan.entity.getShortName(),
                                  criticalHit.loc,
                                  (criticalHit.slot + 1));
                        } else {
                            LOGGER.debug("[s.applyCriticalHit(criticalHitPlan.entity, ch.loc, criticalSlot, false)]");
                            twGameManager.applyCriticalHit(criticalHitPlan.entity,
                                  criticalHit.loc,
                                  criticalSlot,
                                  false,
                                  0,
                                  false);
                        }
                    }
                    // Handle Tanks differently.
                    else if (criticalHitPlan.entity instanceof Tank) {
                        if ((criticalHit.slot < 0) || (criticalHit.slot >= 6)) {
                            LOGGER.error("{} - invalid slot specified ( Tank Slot < 0 OR Slot >= 6) {}: {}",
                                  criticalHitPlan.entity.getShortName(),
                                  criticalHit.loc,
                                  (criticalHit.slot + 1));
                        } else {
                            CriticalSlot cs = new CriticalSlot(CriticalSlot.TYPE_SYSTEM, criticalHit.slot + 1);
                            LOGGER.debug("[s.applyCriticalHit(criticalHitPlan.entity, ch.loc, cs, false)]");
                            twGameManager.applyCriticalHit(criticalHitPlan.entity, Entity.NONE, cs, false, 0, false);
                        }
                    }
                }
            }
        }

        // Loop through Set Ammo To
        for (SetAmmoPlan setAmmoPlan : ammoPlans) {
            LOGGER.debug("Applying ammo adjustment to {}", setAmmoPlan.entity.getShortName());
            for (SetAmmoType setAmmoType : setAmmoPlan.ammoSetType) {
                // Limit to `Meks for now (needs to be extended later)
                if (setAmmoPlan.entity instanceof Mek) {
                    if (setAmmoType.slot < setAmmoPlan.entity.getNumberOfCriticals(setAmmoType.loc)) {
                        CriticalSlot criticalSlot = setAmmoPlan.entity.getCritical(setAmmoType.loc, setAmmoType.slot);
                        if (criticalSlot != null) {
                            AmmoMounted ammo = (AmmoMounted) setAmmoPlan.entity.getCritical(setAmmoType.loc,
                                  setAmmoType.slot).getMount();
                            if (ammo == null) {
                                LOGGER.error("{} - invalid slot specified (setAmmoPlan) {}: {}",
                                      setAmmoPlan.entity.getShortName(),
                                      setAmmoType.loc,
                                      setAmmoType.slot + 1);
                            } else {
                                AmmoType newAmmoType = getValidAmmoType(twGameManager.getGame(),
                                      ammo,
                                      setAmmoType.type);
                                if (newAmmoType != null) {
                                    ammo.changeAmmoType(newAmmoType);
                                } else {
                                    LOGGER.warn("Illegal ammo type '{}' for unit {}, slot {}",
                                          setAmmoType.type,
                                          setAmmoPlan.entity.getDisplayName(),
                                          ammo.getName());
                                }
                            }
                        }
                    }
                }
            }

            for (SetAmmoTo setAmmoTo : setAmmoPlan.ammoSetTo) {
                // Only can be done against Meks
                if (setAmmoPlan.entity instanceof Mek) {
                    if (setAmmoTo.slot < setAmmoPlan.entity.getNumberOfCriticals(setAmmoTo.loc)) {
                        // Get the piece of equipment and check to make sure it is an ammo item then set its amount!
                        CriticalSlot criticalSlot = setAmmoPlan.entity.getCritical(setAmmoTo.loc, setAmmoTo.slot);
                        if (criticalSlot != null) {
                            Mounted<?> ammo = setAmmoPlan.entity.getCritical(setAmmoTo.loc, setAmmoTo.slot).getMount();
                            if (ammo == null) {
                                LOGGER.error("{} - invalid slot specified (SetAmmoTo) {}: {}",
                                      setAmmoPlan.entity.getShortName(),
                                      setAmmoTo.loc,
                                      setAmmoTo.slot + 1);
                            } else if (ammo.getType() instanceof AmmoType) {
                                // Also make sure we don't exceed the max allowed
                                ammo.setShotsLeft(Math.min(setAmmoTo.setAmmoTo, ammo.getBaseShotsLeft()));
                            }
                        }
                    }
                }
            }
        }
    }

    private void parsePlanetaryConditions(Game game, ScenarioV1 scenarioV1) {
        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_TEMP)) {
            game.getPlanetaryConditions().setTemperature(parseInt(PARAM_PLANET_CONDITIONS_TEMP, 0));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_GRAV)) {
            game.getPlanetaryConditions().setGravity(parseFloat(PARAM_PLANET_CONDITIONS_GRAV, 0.0f));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_FOG)) {
            game.getPlanetaryConditions().setFog(Fog.getFog(parseInt(PARAM_PLANET_CONDITIONS_FOG, 0)));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_ATMOSPHERE)) {
            game.getPlanetaryConditions()
                  .setAtmosphere(Atmosphere.getAtmosphere(parseInt(PARAM_PLANET_CONDITIONS_ATMOSPHERE, 0)));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_LIGHT)) {
            game.getPlanetaryConditions().setLight(Light.getLight(parseInt(PARAM_PLANET_CONDITIONS_LIGHT, 0)));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_WEATHER)) {
            game.getPlanetaryConditions().setWeather(Weather.getWeather(parseInt(PARAM_PLANET_CONDITIONS_WEATHER, 0)));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_WIND)) {
            game.getPlanetaryConditions().setWind(Wind.getWind(parseInt(PARAM_PLANET_CONDITIONS_WIND, 0)));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_WIND_DIRECTION)) {
            game.getPlanetaryConditions()
                  .setWindDirection(WindDirection.getWindDirection(parseInt(PARAM_PLANET_CONDITIONS_WIND_DIRECTION,
                        0)));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_WIND_SHIFTING_DIRECTION)) {
            game.getPlanetaryConditions()
                  .setShiftingWindDirection(parseBoolean(PARAM_PLANET_CONDITIONS_WIND_SHIFTING_DIRECTION, false));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_WIND_SHIFTING_STRENGTH)) {
            game.getPlanetaryConditions()
                  .setShiftingWindStrength(parseBoolean(PARAM_PLANET_CONDITIONS_WIND_SHIFTING_STRENGTH, false));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_WIND_MIN)) {
            game.getPlanetaryConditions().setWindMin(Wind.getWind(parseInt(PARAM_PLANET_CONDITIONS_WIND_MIN, 0)));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_WIND_MAX)) {
            game.getPlanetaryConditions().setWindMax(Wind.getWind(parseInt(PARAM_PLANET_CONDITIONS_WIND_MAX, 0)));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_EMI)) {
            EMI emi = parseBoolean(PARAM_PLANET_CONDITIONS_EMI, false) ? EMI.EMI : EMI.EMI_NONE;
            game.getPlanetaryConditions().setEMI(emi);
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_TERRAIN_CHANGES)) {
            game.getPlanetaryConditions()
                  .setTerrainAffected(parseBoolean(PARAM_PLANET_CONDITIONS_TERRAIN_CHANGES, true));
        }

        if (scenarioV1.containsKey(PARAM_PLANET_CONDITIONS_BLOWING_SAND)) {
            BlowingSand blowingSand = parseBoolean(PARAM_PLANET_CONDITIONS_BLOWING_SAND, false) ?
                                            BlowingSand.BLOWING_SAND :
                                            BlowingSand.BLOWING_SAND_NONE;
            game.getPlanetaryConditions().setBlowingSand(blowingSand);
        }
    }

    private Collection<Entity> buildFactionEntities(ScenarioV1 scenarioV1, Player player)
          throws ScenarioLoaderException {
        String faction = player.getName();
        Pattern unitPattern = Pattern.compile(String.format("^Unit_\\Q%s\\E_[^_]+$", faction));
        Pattern unitDataPattern = Pattern.compile(String.format("^(Unit_\\Q%s\\E_[^_]+)_([A-Z][^_]+)$", faction));

        Map<String, Entity> entities = new HashMap<>();

        // Gather all defined units
        for (String key : scenarioV1.keySet()) {
            if (unitPattern.matcher(key).matches() && (scenarioV1.getNumValues(key) > 0)) {
                if (scenarioV1.getNumValues(key) > 1) {
                    LOGGER.error("Scenario loading: Unit declaration {} found {} times",
                          key,
                          scenarioV1.getNumValues(key));
                    throw new ScenarioLoaderException("ScenarioLoaderException.multipleUnitDeclarations", key);
                }
                entities.put(key, parseEntityLine(scenarioV1.getString(key)));
            }
        }

        // Add other information
        for (String key : scenarioV1.keySet()) {
            Matcher dataMatcher = unitDataPattern.matcher(key);
            if (dataMatcher.matches()) {
                String unitKey = dataMatcher.group(1);
                if (!entities.containsKey(unitKey)) {
                    LOGGER.warn("Scenario loading: Data for undeclared unit encountered, ignoring: {}", key);
                    continue;
                }
                Entity entity = entities.get(unitKey);
                switch (dataMatcher.group(2)) {
                    case PARAM_DAMAGE:
                        for (String value : scenarioV1.get(key)) {
                            damagePlans.add(new DamagePlan(entity, MathUtility.parseInt(value, 0)));
                        }
                        break;
                    case PARAM_SPECIFIC_DAMAGE:
                        DamagePlan damagePlan = new DamagePlan(entity);
                        for (String value : scenarioV1.getString(key).split(SEPARATOR_COMMA, -1)) {
                            damagePlan.addSpecificDamage(value);
                        }
                        damagePlans.add(damagePlan);
                        break;
                    case PARAM_CRITICAL_HIT:
                        CriticalHitPlan criticalHitPlan = new CriticalHitPlan(entity);
                        for (String value : scenarioV1.getString(key).split(SEPARATOR_COMMA, -1)) {
                            criticalHitPlan.addCriticalHits(value);
                        }
                        criticalHitPlans.add(criticalHitPlan);
                        break;
                    case PARAM_AMMO_AMOUNT:
                        SetAmmoPlan setAmmoPlanAmount = new SetAmmoPlan(entity);
                        for (String value : scenarioV1.getString(key).split(SEPARATOR_COMMA, -1)) {
                            setAmmoPlanAmount.addSetAmmoTo(value);
                        }
                        ammoPlans.add(setAmmoPlanAmount);
                        break;
                    case PARAM_AMMO_TYPE:
                        SetAmmoPlan setAmmoPlanType = new SetAmmoPlan(entity);
                        for (String val : scenarioV1.getString(key).split(SEPARATOR_COMMA, -1)) {
                            setAmmoPlanType.addSetAmmoType(val);
                        }
                        ammoPlans.add(setAmmoPlanType);
                        break;
                    case PARAM_PILOT_HITS:
                        int hits = parseInt(scenarioV1.getString(key), 0);
                        entity.getCrew().setHits(Math.min(hits, 5), 0);
                        break;
                    case PARAM_EXTERNAL_ID:
                        entity.setExternalIdAsString(scenarioV1.getString(key));
                        break;
                    case PARAM_ADVANTAGES:
                        parseAdvantages(entity, scenarioV1.getString(key, SEPARATOR_SPACE));
                        break;
                    case PARAM_AUTO_EJECT:
                        parseAutoEject(entity, scenarioV1.getString(key));
                        break;
                    case PARAM_COMMANDER:
                        parseCommander(entity, scenarioV1.getString(key));
                        break;
                    case PARAM_DEPLOYMENT_ROUND:
                        int round = parseInt(scenarioV1.getString(key), 0);
                        if (round > 0) {
                            LOGGER.debug("{} will be deployed before round {}", entity.getDisplayName(), round);
                            entity.setDeployRound(round);
                            entity.setDeployed(false);
                            entity.setNeverDeployed(false);
                            entity.setPosition(null);
                        }
                        break;
                    case PARAM_CAMO:
                        final Camouflage camouflage = parseCamouflage(scenarioV1.getString(key));
                        if (!camouflage.isDefault()) {
                            entity.setCamouflage(camouflage);
                        }
                        break;
                    case PARAM_ALTITUDE:
                        int altitude = Math.min(parseInt(scenarioV1.getString(key), 0), 10);
                        if (entity.isAero()) {
                            entity.setAltitude(altitude);
                            if (altitude <= 0) {
                                ((IAero) entity).land();
                            }
                        } else {
                            LOGGER.warn("Altitude setting for a non-aerospace unit {}; ignoring",
                                  entity.getShortName());
                        }
                        break;
                    default:
                        LOGGER.error("Scenario loading: Unknown unit data key {}", key);
                }
            }
        }

        return entities.values();
    }

    private Entity parseEntityLine(String line) throws ScenarioLoaderException {
        try {
            String[] parts = line.split(SEPARATOR_COMMA, -1);
            int i;
            MekSummary mekSummary = MekSummaryCache.getInstance().getMek(parts[0]);

            if (mekSummary == null) {
                throw new ScenarioLoaderException("ScenarioLoaderException.missingRequiredEntity", parts[0]);
            }

            LOGGER.debug("Loading {}", mekSummary.getName());
            Entity entity = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();

            // The following section is used to determine if part 4 of the string includes gender or not the regex is
            // used to match a number that might be negative. As the direction is never a number, if a number is
            // found it must be gendered. The `i` value must be included to ensure that the correct indexes are used for
            // the direction calculation below.
            if ((parts.length > 4) && parts[4].matches("-?\\d+")) {
                entity.setCrew(new Crew(entity.getCrew().getCrewType(),
                      parts[1],
                      1,
                      Integer.parseInt(parts[2]),
                      Integer.parseInt(parts[3]),
                      Gender.parseFromString(parts[4]),
                      Boolean.parseBoolean(parts[5]),
                      null));
                i = 6; // direction will be part 6, as the scenario has the gender of its pilots
                // included
            } else {
                entity.setCrew(new Crew(entity.getCrew().getCrewType(),
                      parts[1],
                      1,
                      Integer.parseInt(parts[2]),
                      Integer.parseInt(parts[3]),
                      RandomGenderGenerator.generate(),
                      entity.isClan(),
                      null));
                i = 4; // direction will be part 4, as the scenario does not contain gender
            }

            // This uses the `i` value to ensure it is calculated correctly
            if (parts.length >= 7) {
                String direction = parts[i++].toUpperCase(Locale.ROOT); // grab value at i, then increment
                switch (direction) {
                    case "N":
                        entity.setFacing(0);
                        break;
                    case "NW":
                        entity.setFacing(5);
                        break;
                    case "SW":
                        entity.setFacing(4);
                        break;
                    case "S":
                        entity.setFacing(3);
                        break;
                    case "SE":
                        entity.setFacing(2);
                        break;
                    case "NE":
                        entity.setFacing(1);
                        break;
                    default:
                        break;
                }
                int x = Integer.parseInt(parts[i++]) - 1;
                int y = Integer.parseInt(parts[i]) - 1;
                entity.setPosition(new Coords(x, y));
                entity.setDeployed(true);
            }
            return entity;
        } catch (NumberFormatException | IndexOutOfBoundsException | EntityLoadingException ex) {
            LOGGER.error("", ex);
            throw new ScenarioLoaderException("ScenarioLoaderException.unparsableEntityLine", line);
        }
    }

    private void parseAdvantages(Entity entity, String adv) {
        String[] advantages = adv.split(SEPARATOR_SPACE, -1);

        for (String curAdv : advantages) {
            String[] advantageData = curAdv.split(SEPARATOR_COLON, -1);
            IOption option = entity.getCrew().getOptions().getOption(advantageData[0]);
            if (option == null) {
                LOGGER.error("Ignoring invalid pilot advantage '{}'", curAdv);
            } else {
                LOGGER.debug("Adding pilot advantage '{}' to {}", curAdv, entity.getDisplayName());
                if (advantageData.length > 1) {
                    option.setValue(advantageData[1]);
                } else {
                    option.setValue(true);
                }
            }
        }
    }

    private void parseAutoEject(Entity entity, String eject) {
        if (entity instanceof Mek mek) {
            mek.setAutoEject(Boolean.parseBoolean(eject));
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
            LOGGER.error("Attempted to parse illegal camouflage parameter array of size {} from {}",
                  camouflageParameters.length,
                  camouflage);
            return new Camouflage();
        }
    }

    private String getFactionParam(String faction, String param) {
        return param + SEPARATOR_UNDERSCORE + faction;
    }

    private Collection<Player> createPlayers(ScenarioV1 scenarioV1) throws ScenarioLoaderException {
        String sFactions = scenarioV1.getString(PARAM_FACTIONS);
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

            String loc = scenarioV1.getString(getFactionParam(faction, PARAM_LOCATION));

            if (loc == null) {
                loc = "Any";
            }

            int dir = Math.max(findIndex(IStartingPositions.START_LOCATION_NAMES, loc), 0);
            player.setStartingPos(dir);

            final Camouflage camouflage = parseCamouflage(scenarioV1.getString(getFactionParam(faction, PARAM_CAMO)));
            if (!camouflage.isDefault()) {
                player.setCamouflage(camouflage);
            }

            String team = scenarioV1.getString(getFactionParam(faction, PARAM_TEAM));
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

            String minefields = scenarioV1.getString(getFactionParam(faction, PARAM_MINEFIELDS));
            if ((minefields != null) && !minefields.isEmpty()) {
                String[] mines = minefields.split(SEPARATOR_COMMA, -1);
                if (mines.length >= 3) {
                    try {
                        int minesConventional = MathUtility.parseInt(mines[0], 0);
                        int minesCommand = MathUtility.parseInt(mines[1], 0);
                        int minesVibra = MathUtility.parseInt(mines[2], 0);
                        player.setNbrMFConventional(minesConventional);
                        player.setNbrMFCommand(minesCommand);
                        player.setNbrMFVibra(minesVibra);
                    } catch (NumberFormatException numberFormatException) {
                        LOGGER.error("Format error with minefields string '{}' for {}", minefields, faction);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Load board files and create the megaboard.
     */
    private Board createBoard(ScenarioV1 scenarioV1) throws ScenarioLoaderException {
        int mapWidth = 16, mapHeight = 17;
        if (scenarioV1.getString(PARAM_MAP_WIDTH) == null) {
            LOGGER.info("No map width specified; using {}", mapWidth);
        } else {
            mapWidth = parseInt(PARAM_MAP_WIDTH, mapWidth);
        }

        if (scenarioV1.getString(PARAM_MAP_HEIGHT) == null) {
            LOGGER.info("No map height specified; using {}", mapHeight);
        } else {
            mapHeight = scenarioV1.parseInt(PARAM_MAP_HEIGHT, mapHeight);
        }

        int nWidth = 1, nHeight = 1;
        if (scenarioV1.getString(PARAM_BOARD_WIDTH) == null) {
            LOGGER.info("No board width specified; using {}", nWidth);
        } else {
            nWidth = parseInt(PARAM_BOARD_WIDTH, nWidth);
        }

        if (scenarioV1.getString(PARAM_BOARD_HEIGHT) == null) {
            LOGGER.info("No board height specified; using {}", nHeight);
        } else {
            nHeight = parseInt(PARAM_BOARD_HEIGHT, nHeight);
        }

        LOGGER.debug("Map sheets are {} by {} hexes.", mapWidth, mapHeight);
        LOGGER.debug("Constructing {} by {} board.", nWidth, nHeight);
        int constructionFactor = 0;
        if (scenarioV1.getString(PARAM_BRIDGE_CF) == null) {
            LOGGER.debug("No CF for bridges defined. Using map file defaults.");
        } else {
            constructionFactor = parseInt(PARAM_BRIDGE_CF, constructionFactor);
            LOGGER.debug("Overriding map-defined bridge CFs with {}", constructionFactor);
        }
        // load available boards
        // basically copied from Server.java. Should get moved somewhere neutral
        List<String> boards = new ArrayList<>();

        // Find subdirectories given in the scenario file
        List<String> allDirs = new LinkedList<>();
        // "" entry stands for the boards base directory
        allDirs.add("");

        if (scenarioV1.getString(PARAM_MAP_DIRECTORIES) != null) {
            allDirs = Arrays.asList(scenarioV1.getString(PARAM_MAP_DIRECTORIES).split(SEPARATOR_COMMA, -1));
        }

        for (String dir : allDirs) {
            File curDir = new File(Configuration.boardsDir(), dir);
            if (curDir.exists()) {
                for (String file : Objects.requireNonNull(curDir.list())) {
                    if (file.toLowerCase(Locale.ROOT).endsWith(FILE_SUFFIX_BOARD)) {
                        boards.add(dir + "/" + file.substring(0, file.length() - FILE_SUFFIX_BOARD.length()));
                    }
                }
            }
        }

        Board[] ba = new Board[nWidth * nHeight];
        Queue<String> maps = new LinkedList<>(Arrays.asList(scenarioV1.getString(PARAM_MAPS)
                                                                  .split(SEPARATOR_COMMA, -1)));
        List<Boolean> rotateBoard = new ArrayList<>();
        for (int x = 0; x < nWidth; x++) {
            for (int y = 0; y < nHeight; y++) {
                int n = (y * nWidth) + x;
                String board = MAP_RANDOM;

                if (!maps.isEmpty()) {
                    board = maps.poll();
                }

                LOGGER.debug("({}, {}) {}", x, y, board);

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

                if (constructionFactor > 0) {
                    ba[n].setBridgeCF(constructionFactor);
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
    private int parseExternalGameId(ScenarioV1 scenarioV1) {
        String sExternalId = scenarioV1.getString(PARAM_GAME_EXTERNAL_ID);
        int ExternalGameId = 0;

        if (sExternalId != null) {
            ExternalGameId = MathUtility.parseInt(sExternalId, 0);
        }

        return ExternalGameId;
    }

    @Override
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
     * This is used specify the critical hit location
     */
    private static class CriticalHit {
        public int loc;
        public int slot;

        public CriticalHit(int l, int s) {
            loc = l;
            slot = s;
        }
    }

    /**
     * This class is used to store the critical hit plan for an entity it is loaded from the scenario file. It contains
     * a vector of {@link CriticalHit}.
     */
    private static class CriticalHitPlan {
        public Entity entity;
        List<CriticalHit> criticalHits = new ArrayList<>();

        public CriticalHitPlan(Entity e) {
            entity = e;
        }

        public void addCriticalHits(String s) {
            int ewSpot = s.indexOf(':');
            int loc = MathUtility.parseInt(s.substring(0, ewSpot), 0);
            int slot = MathUtility.parseInt(s.substring(ewSpot + 1), 0);

            criticalHits.add(new CriticalHit(loc, slot - 1));
        }
    }

    /**
     * This is used to store the armor to change ammo at a given location
     */
    private record SetAmmoTo(int loc, int slot, int setAmmoTo) {
    }

    private record SetAmmoType(int loc, int slot, String type) {
    }

    /**
     * This class is used to store the ammo Adjustments it is loaded from the scenario file. It contains a vector of
     * SetAmmoTo.
     */
    private static class SetAmmoPlan {
        public final Entity entity;
        public final List<SetAmmoTo> ammoSetTo = new ArrayList<>();
        public final List<SetAmmoType> ammoSetType = new ArrayList<>();

        public SetAmmoPlan(Entity e) {
            entity = e;
        }

        /**
         * Converts 2:1-34 to Location 2 Slot 1 set Ammo to 34
         */
        public void addSetAmmoTo(String string) {
            int equipmentSlot = string.indexOf(':');
            int ammoSpot = string.indexOf('-');

            if (string.isEmpty() || (equipmentSlot < 1) || (ammoSpot < equipmentSlot)) {
                return;
            }

            int loc = MathUtility.parseInt(string.substring(0, equipmentSlot), 0);
            int slot = MathUtility.parseInt(string.substring(equipmentSlot + 1, ammoSpot), 0);
            int setTo = MathUtility.parseInt(string.substring(ammoSpot + 1), 0);

            ammoSetTo.add(new SetAmmoTo(loc, slot - 1, setTo));
        }

        public void addSetAmmoType(String s) {
            int equipmentSlot = s.indexOf(':');
            int ammoTypeSpot = s.indexOf('-');

            if (s.isEmpty() || (equipmentSlot < 1) || (ammoTypeSpot < equipmentSlot)) {
                return;
            }

            int loc = MathUtility.parseInt(s.substring(0, equipmentSlot), 0);
            int slot = MathUtility.parseInt(s.substring(equipmentSlot + 1, ammoTypeSpot), 0);

            ammoSetType.add(new SetAmmoType(loc, slot - 1, s.substring(ammoTypeSpot + 1)));
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
     * This class is used to store the damage plan for an entity it is loaded from the scenario file. It contains a
     * vector of SpecDam.
     */
    private static class DamagePlan {
        public final Entity entity;
        public final int nBlocks;
        public final List<SpecDam> specificDamage = new ArrayList<>();

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
            int loc = MathUtility.parseInt(s.substring(1, ewSpot), 0);
            int setTo = MathUtility.parseInt(s.substring(ewSpot + 1), 0);
            boolean rear = (s.charAt(0) == 'R');
            boolean internal = (s.charAt(0) == 'I');

            specificDamage.add(new SpecDam(loc, setTo, rear, internal));
        }
    }

}
