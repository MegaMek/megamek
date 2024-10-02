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
package megamek.common.scenario;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.*;
import megamek.common.alphaStrike.ASGame;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.enums.GamePhase;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.hexarea.HexArea;
import megamek.common.icons.Camouflage;
import megamek.common.icons.FileCamouflage;
import megamek.common.jacksonadapters.*;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.logging.MMLogger;
import megamek.server.IGameManager;
import megamek.server.scriptedevent.GameEndTriggeredEvent;

public class ScenarioV2 implements Scenario {
    private static final MMLogger logger = MMLogger.create(ScenarioV2.class);

    private static final String DEPLOY = "deploy";
    private static final String DEPLOY_EDGE = "edge";
    private static final String DEPLOY_OFFSET = "offset";
    private static final String DEPLOY_WIDTH = "width";
    private static final String MAP = "map";
    private static final String MAPS = "maps";
    private static final String UNITS = "units";
    private static final String OPTIONS = "options";
    private static final String OBJECTS = "objects";
    private static final String MESSAGES = "messages";
    private static final String END = "end";
    private static final String TRIGGER = "trigger";
    private static final String VICTORY = "victory";
    private static final String AREA = "area";

    private final JsonNode node;
    private final File scenariofile;

    private final List<HexArea> deploymentAreas = new ArrayList<>();

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    ScenarioV2(File scenariofile) throws IOException {
        this.scenariofile = scenariofile;
        node = yamlMapper.readTree(scenariofile);
    }

    @Override
    public String getName() {
        return node.get(NAME).textValue();
    }

    @Override
    public String getDescription() {
        return node.get(DESCRIPTION).textValue();
    }

    @Override
    public String getFileName() {
        return scenariofile.toString();
    }

    @Override
    public String getPlanet() {
        return node.has(PLANET) ? node.get(PLANET).textValue() : "";
    }

    @Override
    public GameType getGameType() {
        return node.has(GAMETYPE) ? GameType.valueOf(node.get(GAMETYPE).textValue()) : GameType.TW;
    }

    @Override
    public boolean isSinglePlayer() {
        return !node.has(PARAM_SINGLEPLAYER) || node.get(PARAM_SINGLEPLAYER).booleanValue();
    }

    @Override
    public boolean hasFixedGameOptions() {
        return !node.has(PARAM_GAME_OPTIONS_FIXED) || node.get(PARAM_GAME_OPTIONS_FIXED).booleanValue();
    }

    @Override
    public boolean hasFixedPlanetaryConditions() {
        return !node.has(PARAM_PLANETCOND_FIXED) || node.get(PARAM_PLANETCOND_FIXED).booleanValue();
    }

    @Override
    public IGame createGame() throws IOException, ScenarioLoaderException {
        logger.info("Loading scenario from {}", scenariofile);
        IGame game = selectGameType();
        game.setPhase(GamePhase.STARTING_SCENARIO);
        parseOptions(game);
        parsePlayers(game);
        parseMessages(game);
        parseGameEndEvents(game);

        game.setupTeams();

        game.setBoard(0, createBoard());
        int zone = 1000;
        for (HexArea hexArea : deploymentAreas) {
            game.getBoard().addDeploymentZone(zone++, hexArea);
        }
        if ((game instanceof PlanetaryConditionsUsing)) {
            parsePlanetaryConditions((PlanetaryConditionsUsing) game);
        }

        if (game instanceof Game twGame) {
            twGame.setupDeployment();
            if (node.has(PARAM_GAME_EXTERNAL_ID)) {
                twGame.setExternalGameId(node.get(PARAM_GAME_EXTERNAL_ID).intValue());
            }
            twGame.setVictoryContext(new HashMap<>());
            twGame.createVictoryConditions();
        } else if (game instanceof SBFGame) {
            validateSBFGame((SBFGame) game);
        }

        // TODO: check the game for inconsistencies such as units outside board
        // coordinates
        return game;
    }

    private void parsePlanetaryConditions(PlanetaryConditionsUsing plGame) throws JsonProcessingException {
        if (node.has(MMS_PLANETCOND)) {
            PlanetaryConditions conditions = yamlMapper.treeToValue(node.get(MMS_PLANETCOND),
                    PlanetaryConditions.class);
            conditions.determineWind();
            plGame.setPlanetaryConditions(conditions);
        }
    }

    private void parseGameEndEvents(IGame game) {
        if (node.has(END)) {
            node.get(END).iterator().forEachRemaining(n -> parseGameEndEvent(game, n));
        }
    }

    private void parseGameEndEvent(IGame game, JsonNode node) {
        game.addScriptedEvent(new GameEndTriggeredEvent(TriggerDeserializer.parseNode(node.get(TRIGGER))));

    }

    private void parseMessages(IGame game) {
        if (node.has(MESSAGES)) {
            node.get(MESSAGES).iterator().forEachRemaining(n -> parseMessage(game, n));
        }
    }

    private void parseMessage(IGame game, JsonNode node) {
        game.addScriptedEvent(MessageDeserializer.parse(node, scenarioDirectory()));
    }

    private void parsePlayers(IGame game) throws ScenarioLoaderException, IOException {
        List<Player> players = readPlayers(game);
        for (Player player : players) {
            game.addPlayer(player.getId(), player);
        }
    }

    private void parseOptions(IGame game) {
        game.getOptions().initialize();
        if (node.has(PARAM_GAME_OPTIONS_FILE)) {
            File optionsFile = new File(scenariofile.getParentFile(), node.get(PARAM_GAME_OPTIONS_FILE).textValue());
            game.getOptions().loadOptions(optionsFile, true);
        } else {
            game.getOptions().loadOptions();
        }
        if (node.has(OPTIONS)) {
            JsonNode optionsNode = node.get(OPTIONS);
            if (optionsNode.isArray()) {
                optionsNode.iterator().forEachRemaining(n -> game.getOptions().getOption(n.textValue()).setValue(true));
            } else if (optionsNode.isTextual()) {
                game.getOptions().getOption(optionsNode.textValue()).setValue(true);
            }
        }
    }

    private void parseDeployment(JsonNode playerNode, Player player) {
        String edge = "Any";
        if (playerNode.has(DEPLOY)) {
            if (!playerNode.get(DEPLOY).isContainerNode()) {
                edge = playerNode.get(DEPLOY).textValue();
            } else if (playerNode.get(DEPLOY).has(AREA)) {
                deploymentAreas.add(HexAreaDeserializer.parseShape(playerNode.get(DEPLOY).get(AREA)));
                player.setStartingPos(1000 + deploymentAreas.size() - 1);
                return;
            } else {
                JsonNode deployNode = playerNode.get(DEPLOY);
                if (deployNode.has(DEPLOY_EDGE)) {
                    edge = deployNode.get(DEPLOY_EDGE).textValue();
                }
                if (deployNode.has(DEPLOY_OFFSET)) {
                    player.setStartOffset(deployNode.get(DEPLOY_OFFSET).intValue());
                }
                if (deployNode.has(DEPLOY_WIDTH)) {
                    player.setStartWidth(deployNode.get(DEPLOY_WIDTH).intValue());
                }
            }
        }
        int dir = Math.max(findIndex(IStartingPositions.START_LOCATION_NAMES, edge), 0);
        player.setStartingPos(dir);
    }

    private IGame selectGameType() {
        return switch (getGameType()) {
            case AS -> new ASGame();
            case SBF -> new SBFGame();
            default -> new Game();
        };
    }

    @Override
    public void applyDamage(IGameManager gameManager) {
        // TODO
    }

    private List<Player> readPlayers(IGame game) throws ScenarioLoaderException, IOException {
        if (!node.has(PARAM_FACTIONS) || !node.get(PARAM_FACTIONS).isArray()) {
            throw new ScenarioLoaderException("ScenarioLoaderException.missingFactions");
        }
        List<Player> result = new ArrayList<>();
        int playerId = 0;
        int teamId = 0;
        final PlayerColour[] colours = PlayerColour.values();

        for (Iterator<JsonNode> it = node.get(PARAM_FACTIONS).elements(); it.hasNext();) {
            JsonNode playerNode = it.next();
            MMUReader.requireFields("Player", playerNode, NAME);

            Player player = new Player(playerId, playerNode.get(NAME).textValue());
            result.add(player);
            player.setColour(colours[playerId % colours.length]);
            playerId++;

            // scenario players start out as ghosts to be logged into
            player.setGhost(true);

            parseDeployment(playerNode, player);
            parseVictories(game, playerNode);

            if (playerNode.has(PARAM_CAMO)) {
                String camoPath = playerNode.get(PARAM_CAMO).textValue();
                File file = new File(scenarioDirectory(), camoPath);
                if (file.exists()) {
                    player.setCamouflage(new FileCamouflage(file));
                } else {
                    player.setCamouflage(new Camouflage(new File(camoPath)));
                }
            }

            teamId = playerNode.has(PARAM_TEAM) ? playerNode.get(PARAM_TEAM).intValue() : teamId + 1;
            player.setTeam(Math.min(teamId, Player.TEAM_NAMES.length - 1));

            // The flee area
            if (playerNode.has(EntityDeserializer.FLEE_AREA)) {
                JsonNode fleeNode = playerNode.get(EntityDeserializer.FLEE_AREA);
                // allow using or omitting "area:"
                JsonNode areaNode = fleeNode.has(AREA) ? fleeNode.get(AREA) : fleeNode;
                player.setFleeZone(HexAreaDeserializer.parseShape(areaNode));
            }

            // TODO minefields

            // Carryables
            if (playerNode.has(OBJECTS) && (game instanceof AbstractGame)) {
                JsonNode carryablesNode = playerNode.get(OBJECTS);
                List<CarryableDeserializer.CarryableInfo> carryables = new MMUReader(scenariofile)
                        .read(carryablesNode, CarryableDeserializer.CarryableInfo.class).stream()
                        .filter(o -> o instanceof CarryableDeserializer.CarryableInfo)
                        .map(o -> (CarryableDeserializer.CarryableInfo) o)
                        .toList();
                for (CarryableDeserializer.CarryableInfo carryableInfo : carryables) {
                    if (carryableInfo.position() == null) {
                        player.getGroundObjectsToPlace().add(carryableInfo.carryable());
                    } else {
                        ((AbstractGame) game).placeGroundObject(carryableInfo.position(), carryableInfo.carryable());
                    }
                }
            }

            if (playerNode.has(UNITS)) {
                JsonNode unitsNode = playerNode.get(UNITS);
                if (game instanceof Game twGame) {
                    List<Entity> units = new MMUReader(scenariofile).read(unitsNode, Entity.class).stream()
                            .filter(o -> o instanceof Entity)
                            .map(o -> (Entity) o)
                            .collect(Collectors.toList());
                    int entityId = Math.max(smallestFreeUnitID(units), game.getNextEntityId());
                    Map<Integer, Integer> forceMapping = new HashMap<>();
                    for (Entity unit : units) {
                        unit.setOwner(player);
                        if (unit.getId() == Entity.NONE) {
                            unit.setId(entityId);
                            entityId++;
                        }
                        twGame.addEntity(unit);
                        // Grounded DropShips don't set secondary positions unless they're part of a
                        // game and can verify
                        // they're not on a space map.
                        if (unit.isLargeCraft() && !unit.isAirborne()) {
                            unit.setAltitude(0);
                        }
                        // Map parsed force strings to real Forces
                        if (!unit.getForceString().isBlank()) {
                            List<Force> forceList = Forces.parseForceString(unit);
                            int realId = Force.NO_FORCE;
                            boolean topLevel = true;

                            for (Force force : forceList) {
                                if (!forceMapping.containsKey(force.getId())) {
                                    if (topLevel) {
                                        realId = game.getForces().addTopLevelForce(force, unit.getOwner());
                                    } else {
                                        Force parent = game.getForces().getForce(realId);
                                        realId = game.getForces().addSubForce(force, parent);
                                    }
                                    forceMapping.put(force.getId(), realId);
                                } else {
                                    realId = forceMapping.get(force.getId());
                                }
                                topLevel = false;
                            }
                            unit.setForceString("");
                            game.getForces().addEntity(unit, realId);
                        }
                    }
                } else if (game instanceof SBFGame) {
                    List<InGameObject> units = new MMUReader(scenariofile).read(unitsNode).stream()
                            .filter(o -> o instanceof InGameObject)
                            .map(o -> (InGameObject) o)
                            .collect(Collectors.toList());
                    int entityId = Math.max(smallestFreeUnitID(units), game.getNextEntityId());
                    for (InGameObject unit : units) {
                        if (unit.getId() == Entity.NONE) {
                            unit.setId(entityId);
                            entityId++;
                        }
                        unit.setOwnerId(player.getId());
                        ((SBFGame) game).addUnit(unit);
                    }
                }
            }
            // TODO: look at unit individual camo and see if it's a file in the scenario
            // directory; the entity parsers
            // cannot handle this as they don't know it's a scenario
        }

        return result;
    }

    private void parseVictories(IGame game, JsonNode playerNode) {
        if (playerNode.has(VICTORY)) {
            playerNode.get(VICTORY).iterator().forEachRemaining(n -> parseVictory(game, n));
        }
    }

    private void parseVictory(IGame game, JsonNode node) {
        game.addScriptedEvent(VictoryDeserializer.parse(node));
    }

    private int smallestFreeUnitID(List<? extends InGameObject> units) {
        return units.stream().mapToInt(InGameObject::getId).max().orElse(0) + 1;
    }

    private Board createBoard() throws ScenarioLoaderException {
        if (!node.has(MAP) && !node.has(MAPS)) {
            throw new ScenarioLoaderException("ScenarioLoaderException.missingMap");
        }
        JsonNode mapNode = node.get(MAP);
        if (mapNode == null) {
            mapNode = node.get(MAPS);
        }

        // TODO: currently, the first parsed board is used
        return BoardDeserializer.parse(mapNode, scenarioDirectory()).get(0);
    }

    private File scenarioDirectory() {
        return scenariofile.getParentFile();
    }

    private void validateSBFGame(SBFGame game) {
        // Exactly one COM formation per team
        Map<Integer, Long> comCountsByTeam = game.getActiveFormations().stream()
                .filter(f -> f.hasSUA(BattleForceSUA.COM))
                .collect(Collectors.groupingBy(f -> game.getPlayer(f.getOwnerId()).getTeam(), Collectors.counting()));
        for (Team team : game.getTeams()) {
            if (!comCountsByTeam.containsKey(team.getId()) || comCountsByTeam.get(team.getId()) != 1) {
                throw new IllegalArgumentException("Each team must have one formation with the COM ability");
            }
        }
    }
}
