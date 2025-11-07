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

package megamek.common.scenario;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.client.ui.util.PlayerColour;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.alphaStrike.ASGame;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.AbstractGame;
import megamek.common.game.Game;
import megamek.common.game.GameType;
import megamek.common.game.IGame;
import megamek.common.game.InGameObject;
import megamek.common.hexArea.HexArea;
import megamek.common.icons.Camouflage;
import megamek.common.icons.FileCamouflage;
import megamek.common.interfaces.IStartingPositions;
import megamek.common.interfaces.PlanetaryConditionsUsing;
import megamek.common.jacksonAdapters.BoardDeserializer;
import megamek.common.jacksonAdapters.BotParser;
import megamek.common.jacksonAdapters.EntityDeserializer;
import megamek.common.jacksonAdapters.GeneralEventDeserializer;
import megamek.common.jacksonAdapters.HexAreaDeserializer;
import megamek.common.jacksonAdapters.MMUReader;
import megamek.common.jacksonAdapters.MessageDeserializer;
import megamek.common.jacksonAdapters.TriggerDeserializer;
import megamek.common.jacksonAdapters.VictoryDeserializer;
import megamek.common.jacksonAdapters.dtos.GroundObjectInfo;
import megamek.common.options.GameOptions;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import megamek.common.util.C3Util;
import megamek.logging.MMLogger;
import megamek.server.IGameManager;
import megamek.server.scriptedEvents.GameEndTriggeredEvent;

public class ScenarioV2 implements Scenario {
    private static final MMLogger logger = MMLogger.create(ScenarioV2.class);

    private static final String OPTIONS_FILE = "file";
    private static final String OPTIONS_ON = "on";
    private static final String OPTIONS_OFF = "off";
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
    private static final String BOT = "bot";
    private static final String EVENTS = "events";

    private final JsonNode node;
    private final File scenariofile;
    private final Map<String, BotParser.BotInfo> botInfo = new HashMap<>();

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
        return !node.has(PARAM_PLANET_CONDITIONS_FIXED) || node.get(PARAM_PLANET_CONDITIONS_FIXED).booleanValue();
    }

    @Override
    public BotParser.BotInfo getBotInfo(String playerName) {
        return botInfo.get(playerName);
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
        parseGeneralEvents(game);
        parseGameVictories(game, node);

        game.setupTeams();

        game.receiveBoards(new HashMap<>());

        int id = 0;
        for (Board board : parseBoards()) {
            int boardId = board.getBoardId();
            // when the scenario does not give an ID, it will be 0; then assign an ID
            // default to 0 instead of -1 to keep compatibility with single-board!
            if (boardId == 0) {
                boardId = id;
                id++;
                board.setBoardId(boardId);
            }
            game.setBoard(boardId, board);
        }
        // post-process embedded boards and deployment areas
        for (Board board : game.getBoards().values()) {
            for (Coords coord : board.getEmbeddedBoardHexes()) {
                Board embeddedBoard = game.getBoard(board.getEmbeddedBoardAt(coord));
                embeddedBoard.setEnclosingBoard(board.getBoardId());
            }
            int zone = 1000;
            for (HexArea hexArea : deploymentAreas) {
                if (hexArea.matchesBoardId(board)) {
                    board.addDeploymentZone(zone++, hexArea);
                }
            }
        }
        if ((game instanceof PlanetaryConditionsUsing)) {
            parsePlanetaryConditions((PlanetaryConditionsUsing) game);
        }

        if (game instanceof Game twGame) {
            List<C3ScenarioParser.ParsedC3Info> networks = C3ScenarioParser.parse(node);
            for (C3ScenarioParser.ParsedC3Info network : networks) {
                List<Entity> units = network.participants.stream().map(twGame::getEntity).toList();
                try {
                    if (network.masterId == Entity.NONE) {
                        C3Util.joinNh(twGame, units, units.get(0).getId(), false);
                    } else {
                        boolean connectMM = units.stream().anyMatch(Entity::hasC3M);
                        if (connectMM) {
                            Entity master = Objects.requireNonNull(twGame.getEntity(network.masterId));
                            C3Util.setCompanyMaster(List.of(master));
                        }
                        C3Util.connect(twGame, new ArrayList<>(units), network.masterId, false);
                    }
                } catch (Exception e) {
                    throw new ScenarioLoaderException("Faulty C3 network definition: " + network);
                }
            }

            List<TransportsScenarioParser.ParsedTransportsInfo> transportsInfos = TransportsScenarioParser.parse(node);
            for (TransportsScenarioParser.ParsedTransportsInfo transport : transportsInfos) {
                try {
                    Entity carrier = twGame.getEntity(transport.carrierId);
                    List<Entity> units = transport.carriedUnits.stream().map(twGame::getEntity).toList();
                    for (Entity unit : units) {
                        carrier.load(unit);
                        unit.setTransportId(transport.carrierId);
                    }
                } catch (Exception e) {
                    throw new ScenarioLoaderException("Faulty transports definition: " + transport);
                }
            }

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
        if (node.has(MMS_PLANET_CONDITIONS)) {
            PlanetaryConditions conditions = yamlMapper.treeToValue(node.get(MMS_PLANET_CONDITIONS),
                  PlanetaryConditions.class);
            conditions.determineWind();
            plGame.setPlanetaryConditions(conditions);
        }
    }

    private void parseGeneralEvents(IGame game) {
        if (node.has(EVENTS)) {
            node.get(EVENTS).iterator().forEachRemaining(n -> {
                try {
                    parseGeneralEvent(game, n);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void parseGeneralEvent(IGame game, JsonNode node) throws JsonProcessingException {
        game.addScriptedEvent(GeneralEventDeserializer.parse(node, scenarioDirectory()));
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
        var gameOptions = ((GameOptions) game.getOptions());
        gameOptions.initialize();
        if (node.has(OPTIONS)) {
            JsonNode optionsNode = node.get(OPTIONS);
            if (optionsNode.has(OPTIONS_FILE)) {
                File optionsFile = new File(scenariofile.getParentFile(), optionsNode.get(OPTIONS_FILE).textValue());
                gameOptions.loadOptions(optionsFile, true);
            } else {
                gameOptions.loadOptions();
            }

            if (optionsNode.has(OPTIONS_ON)) {
                JsonNode onNode = optionsNode.get(OPTIONS_ON);
                onNode.iterator().forEachRemaining(n -> game.getOptions().getOption(n.textValue()).setValue(true));
            }
            if (optionsNode.has(OPTIONS_OFF)) {
                JsonNode offNode = optionsNode.get(OPTIONS_OFF);
                offNode.iterator().forEachRemaining(n -> game.getOptions().getOption(n.textValue()).setValue(false));
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
            throw new ScenarioLoaderException("The scenario does not contain any factions (players)!");
        }
        List<Player> result = new ArrayList<>();
        int playerId = 0;
        int teamId = 0;
        final PlayerColour[] colours = PlayerColour.values();

        for (Iterator<JsonNode> it = node.get(PARAM_FACTIONS).elements(); it.hasNext(); ) {
            JsonNode playerNode = it.next();
            MMUReader.requireFields("Player", playerNode, NAME);

            Player player = new Player(playerId, playerNode.get(NAME).textValue());
            result.add(player);
            player.setColour(colours[playerId % colours.length]);
            playerId++;

            // scenario players start out as ghosts to be logged into
            player.setGhost(true);

            parseDeployment(playerNode, player);
            parsePlayerVictories(game, playerNode, player.getName());

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

            // Bot type
            if (playerNode.has(BOT)) {
                botInfo.put(player.getName(), BotParser.parse(playerNode.get(BOT)));
            }

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
                List<GroundObjectInfo> carryables = new MMUReader(scenariofile)
                      .read(carryablesNode, GroundObjectInfo.class).stream()
                      .filter(o -> o instanceof GroundObjectInfo)
                      .map(o -> (GroundObjectInfo) o)
                      .toList();
                for (GroundObjectInfo groundObjectInfo : carryables) {
                    if (groundObjectInfo.position() == null) {
                        player.getGroundObjectsToPlace().add(groundObjectInfo.groundObject());
                    } else {
                        ((AbstractGame) game).placeGroundObject(groundObjectInfo.position(), groundObjectInfo.groundObject());
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
                        if (unit instanceof IBomber bomber) {
                            bomber.applyBombs();
                        }
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

    private void parseGameVictories(IGame game, JsonNode playerNode) {
        if (playerNode.has(VICTORY)) {
            playerNode.get(VICTORY).iterator().forEachRemaining(n -> parseGameVictory(game, n));
        }
    }

    private void parseGameVictory(IGame game, JsonNode node) {
        game.addScriptedEvent(VictoryDeserializer.parse(node));
    }


    private void parsePlayerVictories(IGame game, JsonNode playerNode, String playerName) {
        if (playerNode.has(VICTORY)) {
            playerNode.get(VICTORY).iterator().forEachRemaining(n -> parsePlayerVictory(game, n, playerName));
        }
    }

    private void parsePlayerVictory(IGame game, JsonNode node, String playerName) {
        game.addScriptedEvent(VictoryDeserializer.parse(node, playerName));
    }

    private int smallestFreeUnitID(List<? extends InGameObject> units) {
        return units.stream().mapToInt(InGameObject::getId).max().orElse(0) + 1;
    }

    private List<Board> parseBoards() throws ScenarioLoaderException {
        if (!node.has(MAP) && !node.has(MAPS)) {
            throw new ScenarioLoaderException("The scenario does not declare any game map!");
        }
        JsonNode mapNode = node.get(MAP);
        if (mapNode == null) {
            mapNode = node.get(MAPS);
        }

        return BoardDeserializer.parse(mapNode, scenarioDirectory());
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
