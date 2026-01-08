/*
 * Copyright (c) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import megamek.MMConstants;
import megamek.client.bot.princess.Princess;
import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.client.generator.skillGenerators.ModifiedTotalWarfareSkillGenerator;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.tooltip.PilotToolTip;
import megamek.client.ui.tileset.TilesetManager;
import megamek.client.ui.util.UIUtil;
import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.TagInfo;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
import megamek.common.actions.ClubAttackAction;
import megamek.common.actions.DodgeAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.BoardDimensions;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.enums.VariableRangeTargetingMode;
import megamek.common.equipment.Flare;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.Minefield;
import megamek.common.equipment.Mounted;
import megamek.common.event.GameCFREvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.GameVictoryEvent;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.game.IGame;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.loaders.MapSettings;
import megamek.common.moves.MovePath;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.InvalidPacketDataException;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.turns.UnloadStrandedTurn;
import megamek.common.units.Crew;
import megamek.common.units.DemolitionCharge;
import megamek.common.units.Entity;
import megamek.common.units.EntitySelector;
import megamek.common.units.FighterSquadron;
import megamek.common.units.IBuilding;
import megamek.common.units.UnitLocation;
import megamek.common.util.C3Util;
import megamek.common.util.ImageUtil;
import megamek.common.util.SerializationHelper;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;
import megamek.server.SmokeCloud;

/**
 * This class is instantiated for each client and for each bot running on that client. non-local clients are not also
 * instantiated on the local server.
 */
public class Client extends AbstractClient {
    private final static MMLogger LOGGER = MMLogger.create(Client.class);

    /**
     * The game state object: this object is not ever replaced during a game, only updated. A reference can therefore be
     * cached by other objects.
     */
    protected final Game game = new Game();

    private Set<BoardDimensions> availableSizes = new TreeSet<>();
    private AbstractSkillGenerator skillGenerator;

    // FIXME: Should ideally be located elsewhere; the client should handle data, not gfx or UI-related stuff:
    private TilesetManager tilesetManager;

    public Client(String name, String host, int port) {
        super(name, host, port);
        setSkillGenerator(new ModifiedTotalWarfareSkillGenerator());
        try {
            tilesetManager = new TilesetManager(game);
        } catch (IOException e) {
            LOGGER.error(e, "Unknown Exception");
        }
    }

    public Game getGame() {
        return game;
    }

    public Entity getEntity(int id) {
        return game.getEntity(id);
    }

    /**
     * Returns an Enumeration of the entities that match the selection criteria.
     */
    public Iterator<Entity> getSelectedEntities(EntitySelector selector) {
        return game.getSelectedEntities(selector);
    }

    /**
     * Returns the number of first selectable entity
     */
    public int getFirstEntityNum() {
        return game.getFirstEntityNum(getMyTurn());
    }

    /**
     * Returns the number of the next selectable entity after the one given
     */
    public int getNextEntityNum(int entityId) {
        return game.getNextEntityNum(getMyTurn(), entityId);
    }

    /**
     * Returns the number of the previous selectable entity after the one given
     */
    public int getPrevEntityNum(int entityId) {
        return game.getPrevEntityNum(getMyTurn(), entityId);
    }

    /**
     * Returns the number of the first deployable entity
     */
    public int getFirstDeployableEntityNum() {
        return game.getFirstDeployableEntityNum(getMyTurn());
    }

    /**
     * Returns the number of the next deployable entity
     */
    public int getNextDeployableEntityNum(int entityId) {
        return game.getNextDeployableEntityNum(getMyTurn(), entityId);
    }

    /**
     * Shortcut to game.board
     */
    public Board getBoard() {
        return game.getBoard();
    }

    /**
     * Returns the board with the given boardId or null if the game does not have a board of that boardId. Shortcut to
     * game.getBoard(int).
     *
     * @param boardId The board's ID
     *
     * @return The board with the given ID
     *
     * @see IGame#getBoard(int)
     */
    @Nullable
    public Board getBoard(int boardId) {
        return game.getBoard(boardId);
    }

    /**
     * Returns an enumeration of the entities in game.entities
     */
    public List<Entity> getEntitiesVector() {
        return game.getEntitiesVector();
    }

    public MapSettings getMapSettings() {
        return game.getMapSettings();
    }

    /**
     * Changes the game phase, and the displays that go along with it.
     */
    @Override
    public void changePhase(GamePhase phase) {
        super.changePhase(phase);
        switch (phase) {
            case LOUNGE:
                tilesetManager.reset();
            case DEPLOYMENT:
            case TARGETING:
            case MOVEMENT:
            case PREMOVEMENT:
            case OFFBOARD:
            case PRE_FIRING:
            case FIRING:
            case PHYSICAL:
                memDump("entering phase " + phase);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean isMyTurn() {
        if (getGame().getPhase().isSimultaneous(getGame())) {
            return game.getTurnForPlayer(localPlayerNumber) != null;
        }
        return (game.getTurn() != null) && game.getTurn().isValid(localPlayerNumber, game);
    }

    public GameTurn getMyTurn() {
        if (getGame().getPhase().isSimultaneous(getGame())) {
            return game.getTurnForPlayer(localPlayerNumber);
        }
        return game.getTurn();
    }

    /**
     * Loads the turn list from the data in the packet
     */
    protected void receiveTurns(Packet packet) throws InvalidPacketDataException {
        game.setTurnVector(packet.getGameTurnList(0));
    }

    /**
     * Can I unload entities stranded on immobile transports?
     */
    public boolean canUnloadStranded() {
        return (game.getTurn() instanceof UnloadStrandedTurn)
              && game.getTurn().isValid(localPlayerNumber, game);
    }

    /**
     * Deploy an entity at the given coordinates, with the given facing, and starting with the given units already
     * loaded.
     *
     * @param id          the ID of the deployed entity
     * @param coords      the Coords where the entity should be deployed
     * @param nFacing     the direction the entity should face
     * @param loadedUnits a List of units that start the game being transported by the deployed entity.
     * @param assaultDrop true if deployment is an assault drop
     */
    public void deploy(int id, Coords coords, int boardId, int nFacing, int elevation, List<Entity> loadedUnits,
          boolean assaultDrop) {
        int packetCount = 7 + loadedUnits.size();
        int index = 0;
        Object[] data = new Object[packetCount];
        data[index++] = id;
        data[index++] = coords;
        data[index++] = boardId;
        data[index++] = nFacing;
        data[index++] = elevation;
        data[index++] = loadedUnits.size();
        data[index++] = assaultDrop;

        for (Entity ent : loadedUnits) {
            data[index++] = ent.getId();
        }

        send(new Packet(PacketCommand.ENTITY_DEPLOY, data));
        flushConn();
    }

    /**
     * For ground to air attacks, the ground unit targets the closest hex in the air units flight path. In the case of
     * several equidistant hexes, the attacker gets to choose. This method updates the server with the users choice.
     *
     * @param targetId   The target ID
     * @param attackerId The attacker Entity ID
     * @param pos        The selected hex
     */
    public void sendPlayerPickedPassThrough(Integer targetId, Integer attackerId, Coords pos) {
        send(new Packet(PacketCommand.ENTITY_GTA_HEX_SELECT, targetId, attackerId, pos));
    }

    /**
     * Send a weapon fire command to the server.
     */
    public void sendAttackData(int aen, Vector<EntityAction> attacks) {
        send(new Packet(PacketCommand.ENTITY_ATTACK, aen, attacks));
        flushConn();
    }

    /**
     * Send s done with prephase turn
     */
    public void sendPrePhaseData(int aen) {
        send(new Packet(PacketCommand.ENTITY_PREPHASE, aen));
        flushConn();
    }

    /**
     * Send the game options to the server
     */
    public void sendGameOptions(String password, Vector<IBasicOption> options) {
        send(new Packet(PacketCommand.SENDING_GAME_SETTINGS, password, options));
    }

    /**
     * Send the new map selection to the server
     */
    public void sendMapSettings(MapSettings settings) {
        send(new Packet(PacketCommand.SENDING_MAP_SETTINGS, settings));
    }

    /**
     * Send the new map dimensions to the server
     */
    public void sendMapDimensions(MapSettings settings) {
        send(new Packet(PacketCommand.SENDING_MAP_DIMENSIONS, settings));
    }

    /**
     * Send the planetary Conditions to the server
     */
    public void sendPlanetaryConditions(PlanetaryConditions conditions) {
        send(new Packet(PacketCommand.SENDING_PLANETARY_CONDITIONS, conditions));
    }

    /**
     * Sends a "reroll initiative" message to the server.
     */
    public void sendRerollInitiativeRequest() {
        send(new Packet(PacketCommand.REROLL_INITIATIVE));
    }

    /**
     * Reset round deployment packet
     */
    public void sendResetRoundDeployment() {
        send(new Packet(PacketCommand.RESET_ROUND_DEPLOYMENT));
    }

    public void sendEntityWeaponOrderUpdate(Entity entity) {
        if (entity.getWeaponSortOrder().isCustom()) {
            send(new Packet(PacketCommand.ENTITY_WORDER_UPDATE,
                  entity.getId(),
                  entity.getWeaponSortOrder(),
                  entity.getCustomWeaponOrder()));
        } else {
            send(new Packet(PacketCommand.ENTITY_WORDER_UPDATE, entity.getId(), entity.getWeaponSortOrder()));
        }
        entity.setWeaponOrderChanged(false);
    }

    /**
     * Sends an "add entity" packet that contains a collection of Entity objects.
     *
     * @param entities The collection of Entity objects to add. This should ideally be an {@link ArrayList<Entity>}, but
     *                 other kinds of {@link List} will be converted to an {@link ArrayList}.
     */
    public void sendAddEntity(List<Entity> entities) {
        // Trying to pass a non-ArrayList jams the receiving client and prevents it from ever receiving more packets.
        if (!(entities instanceof ArrayList<Entity>)) {
            entities = new ArrayList<>(entities);
        }

        for (Entity entity : entities) {
            checkDuplicateNamesDuringAdd(entity);
        }

        send(new Packet(PacketCommand.ENTITY_ADD, entities));
    }

    /**
     * Sends an "add squadron" packet
     */
    public void sendAddSquadron(FighterSquadron fighterSquadron, Collection<Integer> fighterIds) {
        checkDuplicateNamesDuringAdd(fighterSquadron);
        send(new Packet(PacketCommand.SQUADRON_ADD, fighterSquadron, fighterIds));
    }

    /**
     * Sends an "deploy minefields" packet
     */
    public void sendDeployMinefields(Vector<Minefield> minefields) {
        send(new Packet(PacketCommand.DEPLOY_MINEFIELDS, minefields));
    }

    /**
     * Sends an updated state of ground objects (i.e. cargo etc.)
     */
    public void sendDeployGroundObjects(Map<Coords, List<ICarryable>> groundObjects) {
        send(new Packet(PacketCommand.UPDATE_GROUND_OBJECTS, groundObjects));
    }

    /**
     * Sends a "set Artillery AutoHit Hexes" packet
     */
    public void sendArtyAutoHitHexes(List<BoardLocation> hexes) {
        send(new Packet(PacketCommand.SET_ARTILLERY_AUTO_HIT_HEXES, hexes));
    }

    /**
     * Sends an "update entity" packet
     */
    public void sendUpdateEntity(Entity entity) {
        send(new Packet(PacketCommand.ENTITY_UPDATE, entity));
    }

    /**
     * Sends a packet containing multiple entity updates. Should only be used in the lobby phase.
     */
    public void sendUpdateEntity(Collection<Entity> entities) {
        send(new Packet(PacketCommand.ENTITY_MULTI_UPDATE, entities));
    }

    /**
     * Sends a packet containing multiple entity updates. Should only be used in the lobby phase.
     */
    public void sendChangeOwner(Collection<Entity> entities, int newOwnerId) throws InvalidPacketDataException {
        send(new Packet(PacketCommand.ENTITY_ASSIGN, entities, newOwnerId));
    }

    /**
     * Sends an "update entity" packet
     */
    public void sendDeploymentUnload(Entity loader, Entity loaded) {
        send(new Packet(PacketCommand.ENTITY_DEPLOY_UNLOAD, loader.getId(), loaded.getId()));
    }

    /**
     * Sends a "delete entity" packet
     */
    public void sendDeleteEntity(int id) {
        List<Integer> ids = new ArrayList<>(1);
        ids.add(id);
        sendDeleteEntities(ids);
    }

    /** Sends an update to the server to delete the entities of the given ids. */
    public void sendDeleteEntities(List<Integer> ids) {
        checkDuplicateNamesDuringDelete(ids);
        send(new Packet(PacketCommand.ENTITY_REMOVE, ids));
    }

    /**
     * Sends a "load entity" packet
     */
    public void sendLoadEntity(int id, int loaderId, int bayNumber) {
        send(new Packet(PacketCommand.ENTITY_LOAD, id, loaderId, bayNumber));
    }

    /**
     * Sends a "tow entity" packet
     */
    public void sendTowEntity(int id, int tractorId) {
        send(new Packet(PacketCommand.ENTITY_TOW, id, tractorId));
    }

    public void sendExplodeBuilding(DemolitionCharge charge) {
        send(new Packet(PacketCommand.BLDG_EXPLODE, charge));
    }

    /**
     * Loads the entities from the data in the net command.
     */
    protected void receiveEntities(Packet packet) throws InvalidPacketDataException {
        List<Entity> newEntities = packet.getEntityList(0);
        List<Entity> newOutOfGame = packet.getEntityList(1);
        Forces forces = packet.getForces(2);

        // Replace the entities in the game.
        if (forces != null) {
            game.setForces(forces);
        }

        game.setEntitiesVector(newEntities);

        // CRITICAL FIX: Reconstruct C3 networks from UUIDs (matches server-side handling)
        // This is necessary for lobby-configured networks (Naval C3, Nova CEWS, C3i)
        for (Entity entity : newEntities) {
            if (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3() || entity.hasNovaCEWS()) {
                C3Util.wireC3(game, entity);
            }
        }

        // Diagnostic logging for Nova CEWS networks (enable DEBUG logging for C3 debugging)
        if (LOGGER.isDebugEnabled()) {
            for (Entity entity : newEntities) {
                if (entity.hasNovaCEWS()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < Entity.MAX_C3i_NODES; i++) {
                        sb.append(entity.getNC3NextUUIDAsString(i)).append(", ");
                    }
                    LOGGER.debug("[CLIENT] receiveEntities: Entity {} ({}), c3NetIdString: {}, NC3UUIDs: [{}]",
                        entity.getId(), entity.getShortName(), entity.getC3NetId(), sb.toString());
                }
            }
        }
        game.setOutOfGameEntitiesVector(newOutOfGame);
        for (Entity entity : newOutOfGame) {
            cacheImgTag(entity);
        }

        // cache the image data for the entities and set force for entities
        for (Entity entity : newEntities) {
            cacheImgTag(entity);
            entity.setForceId(game.getForces().getForceId(entity));
        }

        if (GUIPreferences.getInstance().getMiniReportShowSprites() &&
              game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) &&
              iconCache != null && !iconCache.containsKey(Report.HIDDEN_ENTITY_NUM)) {
            ImageUtil.createDoubleBlindHiddenImage(iconCache);
        }
    }

    /**
     * Receives a force-related update containing affected forces and affected entities
     */
    protected void receiveForceUpdate(Packet packet) throws InvalidPacketDataException {
        Collection<Force> forces = packet.getForceList(0);
        Collection<Entity> entities = packet.getEntityList(1);

        for (Force force : forces) {
            getGame().getForces().replace(force.getId(), force);
        }

        for (Entity entity : entities) {
            getGame().setEntity(entity.getId(), entity);
        }
    }

    /**
     * Receives a server packet commanding deletion of forces. Only valid in the lobby phase.
     */
    protected void receiveForcesDelete(Packet packet) throws InvalidPacketDataException {
        Collection<Integer> forceIds = packet.getIntList(0);
        Forces forces = game.getForces();

        // Gather the forces and entities to be deleted
        Set<Force> delForces = new HashSet<>();
        Set<Entity> delEntities = new HashSet<>();
        forceIds.stream().map(forces::getForce).forEach(delForces::add);
        for (Force delForce : delForces) {
            forces.getFullEntities(delForce).stream()
                  .filter(e -> e instanceof Entity)
                  .map(e -> (Entity) e)
                  .forEach(delEntities::add);
        }

        forces.deleteForces(delForces);

        for (Entity entity : delEntities) {
            game.removeEntity(entity.getId(), IEntityRemovalConditions.REMOVE_NEVER_JOINED);
        }
    }

    /**
     * Loads entity update data from the data in the net command.
     */
    protected void receiveEntityUpdate(Packet packet) throws InvalidPacketDataException {
        int entityIndex = packet.getIntValue(0);
        Entity entity = packet.getEntity(1);

        if (entity != null) {
            Vector<UnitLocation> movePath = packet.getUnitLocationVector(2);
            // Replace this entity in the game.
            getGame().setEntity(entityIndex, entity, movePath);
        }
    }

    /**
     * Update multiple entities from the server. Used only in the lobby phase.
     */
    protected void receiveEntitiesUpdate(Packet packet) throws InvalidPacketDataException {
        Collection<Entity> entities = packet.getEntityList(0);
        for (Entity entity : entities) {
            getGame().setEntity(entity.getId(), entity);
        }
    }

    protected void receiveEntityRemove(Packet packet) throws InvalidPacketDataException {
        List<Integer> entityIds = packet.getIntList(0);
        int condition = packet.getIntValue(1);
        List<Force> forces = packet.getForceList(2);
        // create a final image for the entity

        for (int id : entityIds) {
            cacheImgTag(game.getEntity(id));
        }

        for (Force force : forces) {
            game.getForces().replace(force.getId(), force);
        }

        // Move the unit to its final resting place.
        game.removeEntities(entityIds, condition);
    }

    protected void receiveEntityVisibilityIndicator(Packet packet) throws InvalidPacketDataException {
        Entity entity = game.getEntity(packet.getIntValue(0));

        if (entity != null) { // we may not have this entity due to double-blind
            entity.setEverSeenByEnemy(packet.getBooleanValue(1));
            entity.setVisibleToEnemy(packet.getBooleanValue(2));
            entity.setDetectedByEnemy(packet.getBooleanValue(3));
            entity.setWhoCanSee(packet.getPlayerVector(4));
            entity.setWhoCanDetect(packet.getPlayerVector(5));

            // this next call is only needed sometimes, but we'll just call it everytime
            game.processGameEvent(new GameEntityChangeEvent(this, entity));
        }
    }

    protected void receiveUpdateGroundObjects(Packet packet) throws InvalidPacketDataException {
        game.setGroundObjects(packet.getCoordsWithGroundObjectListMap(0));
        game.processGameEvent(new GameBoardChangeEvent(this));
    }

    protected void receiveDeployMinefields(Packet packet) throws InvalidPacketDataException {
        game.addMinefields(packet.getMinefieldVector(0));
    }

    protected void receiveSendingMinefields(Packet packet) throws InvalidPacketDataException {
        game.setMinefields(packet.getMinefieldVector(0));
    }

    protected void receiveIlluminatedHexes(Packet packet) throws InvalidPacketDataException {
        game.setIlluminatedPositions(packet.getCoordsHashSet(0));
    }

    protected void receiveRevealMinefield(Packet packet) throws InvalidPacketDataException {
        Minefield minefield = packet.getMinefield(0);

        if (minefield != null) {
            game.addMinefield(minefield);
        }
    }

    protected void receiveRemoveMinefield(Packet packet) throws InvalidPacketDataException {
        Minefield minefield = packet.getMinefield(0);

        if (minefield != null) {
            game.removeMinefield(minefield);
        }
    }

    protected void receiveUpdateMinefields(Packet packet) throws InvalidPacketDataException {
        // only update information if you know about the minefield
        Vector<Minefield> newMines = new Vector<>();
        for (Minefield mf : packet.getMinefieldVector(0)) {
            if (getLocalPlayer().containsMinefield(mf)) {
                newMines.add(mf);
            }
        }

        if (!newMines.isEmpty()) {
            game.resetMinefieldDensity(newMines);
        }
    }

    protected void receiveBuildingAdd(Packet packet) throws InvalidPacketDataException {
        for (IBuilding building : packet.getBuildingList(0)) {
            game.getBoard(building.getBoardId()).addBuildingToBoard(building);
        }
    }

    protected void receiveBuildingUpdate(Packet packet) throws InvalidPacketDataException {
        for (IBuilding building : packet.getBuildingList(0)) {
            game.getBoard(building.getBoardId()).updateBuilding(building);
        }
    }

    protected void receiveBuildingRemove(Packet packet) throws InvalidPacketDataException {
        for (IBuilding building : packet.getBuildingList(0)) {
            game.getBoard(building.getBoardId()).removeBuilding(building);
        }
    }

    protected void receiveBuildingCollapse(Packet packet) throws InvalidPacketDataException {
        int boardId = packet.getIntValue(1);
        game.getBoard(boardId).collapseBuilding(packet.getCoordsVector(0));
    }

    /**
     * Loads entity firing data from the data in the net command
     */
    protected void receiveAttack(Packet packet) throws InvalidPacketDataException {
        List<EntityAction> vector = packet.getEntityActionList(0);
        boolean isCharge = packet.getBooleanValue(1);
        boolean addAction = true;
        for (EntityAction entityAction : vector) {
            int entityId = entityAction.getEntityId();
            if ((entityAction instanceof TorsoTwistAction torsoTwistAction) && game.hasEntity(entityId)) {
                Entity entity = game.getEntity(entityId);
                if (entity != null) {
                    entity.setSecondaryFacing(torsoTwistAction.getFacing());
                }
            } else if ((entityAction instanceof FlipArmsAction flipArmsAction) && game.hasEntity(entityId)) {
                Entity entity = game.getEntity(entityId);
                if (entity != null) {
                    entity.setArmsFlipped(flipArmsAction.getIsFlipped());
                }
            } else if ((entityAction instanceof DodgeAction) && game.hasEntity(entityId)) {
                Entity entity = game.getEntity(entityId);
                if (entity != null) {
                    entity.dodging = true;
                    addAction = false;
                }
            } else if (entityAction instanceof ClubAttackAction clubAttackAction) {
                Mounted<?> club = clubAttackAction.getClub();
                club.restore();
            }

            if (addAction) {
                // track in the appropriate list
                if (!isCharge) {
                    game.addAction(entityAction);
                } else {
                    game.addCharge((AttackAction) entityAction);
                }
            }
        }
    }

    // Should be private?
    public String receiveReport(List<Report> reports) {
        if (reports == null) {
            return "[null report vector]";
        }

        StringBuilder report = new StringBuilder();
        for (Report r : reports) {
            report.append(r.text());
        }

        Set<Integer> setEntity = new HashSet<>();
        // find id stored in spans and extract it
        Pattern pEntity = Pattern.compile("<span id='(.*?)'></span>");
        Matcher mEntity = pEntity.matcher(report.toString());

        // add all instances to a hashset to prevent duplicates
        while (mEntity.find()) {
            String cleanedText = mEntity.group(1);
            if (!cleanedText.isBlank()) {
                try {
                    setEntity.add(Integer.parseInt(cleanedText));
                } catch (Exception ignored) {
                }
            }
        }

        String updatedReport = report.toString();
        // loop through the hashset of unique ids and replace the ids with img tags
        for (int i : setEntity) {
            String cachedImageTag = getCachedImgTag(i);
            if (cachedImageTag != null) {
                updatedReport = updatedReport.replace("<span id='" + i + "'></span>", cachedImageTag);
            }
        }

        Set<String> setCrew = new HashSet<>();
        // find id stored in spans and extract it
        Pattern pCrew = Pattern.compile("<span crew='(.*?)'></span>");
        Matcher mCrew = pCrew.matcher(report.toString());

        // add all instances to a hashset to prevent duplicates
        while (mCrew.find()) {
            String cleanedText = mCrew.group(1);
            if (!cleanedText.isBlank()) {
                setCrew.add(cleanedText);
            }
        }

        // loop through the hashset of unique ids and replace the ids with img tags
        for (String tmpCrew : setCrew) {
            String[] crewS = tmpCrew.split(":");
            int entityID = -1;
            int crewID = -1;

            try {
                entityID = Integer.parseInt(crewS[0]);
                crewID = Integer.parseInt(crewS[1]);
            } catch (Exception ignored) {
            }

            if (entityID != -1 && crewID != -1) {
                Entity e = game.getEntityFromAllSources(entityID);

                if (e != null) {
                    Crew crew = e.getCrew();

                    if (crew != null) {
                        // Adjust the portrait size to the GUI scale and number of pilots
                        float imgSize = UIUtil.scaleForGUI(PilotToolTip.PORTRAIT_BASE_SIZE);
                        imgSize /= 0.2f * (crew.getSlotCount() - 1) + 1;
                        Image portrait = crew.getPortrait(crewID).getBaseImage().getScaledInstance(-1, (int) imgSize,
                              Image.SCALE_SMOOTH);
                        // convert image to base64, add to the <img> tag and store in cache
                        BufferedImage bufferedImage = new BufferedImage(portrait.getWidth(null),
                              portrait.getHeight(null), BufferedImage.TYPE_INT_RGB);
                        bufferedImage.getGraphics().drawImage(portrait, 0, 0, null);
                        String base64Text = ImageUtil.base64TextEncodeImage(bufferedImage);
                        String img = "<img src='data:image/png;base64," + base64Text + "'>";
                        updatedReport = updatedReport.replace("<span crew='" + entityID + ":" + crewID + "'></span>",
                              img);
                    }
                }
            }
        }

        return updatedReport;
    }

    /**
     * returns the stored <img> tag for given unit id
     */
    private String getCachedImgTag(int id) {
        if (!GUIPreferences.getInstance().getMiniReportShowSprites()
              || (iconCache == null) || !iconCache.containsKey(id)) {
            return null;
        }
        return iconCache.get(id);
    }

    /**
     * Hashtable for storing img tags containing base64Text src.
     */
    protected void cacheImgTag(Entity entity) {
        if (entity == null) {
            return;
        }

        iconCache.remove(entity.getId());

        if (getTargetImage(entity) != null) {
            // convert image to base64, add to the <img> tag and store in cache
            BufferedImage image = ImageUtil.getScaledImage(getTargetImage(entity), 56, 48);
            String base64Text = ImageUtil.base64TextEncodeImage(image);
            String img = "<img src='data:image/png;base64," + base64Text + "'>";
            iconCache.put(entity.getId(), img);
        }
    }

    /**
     * Gets the current mek image
     */
    private Image getTargetImage(Entity e) {
        if (tilesetManager == null) {
            return null;
        } else if (e.isDestroyed()) {
            return tilesetManager.wreckMarkerFor(e, -1);
        } else {
            return tilesetManager.imageFor(e);
        }
    }

    /**
     * Saves server entity status data to a local file
     */
    private void saveEntityStatus(String sStatus) {
        try {
            String sLogDir = PreferenceManager.getClientPreferences().getLogDirectory();
            File logDir = new File(sLogDir);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            String fileName = "entitystatus.txt";
            if (PreferenceManager.getClientPreferences().stampFilenames()) {
                fileName = StringUtil.addDateTimeStamp(fileName);
            }
            FileWriter fw = new FileWriter(sLogDir + File.separator + fileName);
            fw.write(sStatus);
            fw.flush();
            fw.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "saveEntityStatus");
        }
    }

    /**
     * Send a Nova CEWS update packet
     */
    public void sendNovaChange(int id, String net) {
        send(new Packet(PacketCommand.ENTITY_NOVA_NETWORK_CHANGE, id, net));
    }

    /**
     * Send a Variable Range Targeting mode change packet (BMM pg. 86). Mode changes are applied at the start of the
     * next round.
     *
     * @param entityId the ID of the entity changing modes
     * @param mode     the new VariableRangeTargetingMode to apply next round
     */
    public void sendVariableRangeTargetingModeChange(int entityId, VariableRangeTargetingMode mode) {
        send(new Packet(PacketCommand.ENTITY_VARIABLE_RANGE_MODE_CHANGE, entityId, mode));
    }

    /**
     * Sends a unit abandonment announcement to the server. For Meks (TacOps:AR p.165): Must be prone and shutdown. For
     * Vehicles (TacOps): Can be abandoned anytime. The abandonment will execute during the End Phase of the following
     * turn.
     *
     * @param entityId the ID of the unit announcing abandonment
     */
    public void sendUnitAbandonmentAnnouncement(int entityId) {
        send(new Packet(PacketCommand.ENTITY_ABANDON_ANNOUNCE, entityId));
    }

    public void sendSpecialHexDisplayAppend(Coords c, int boardId, SpecialHexDisplay shd) {
        send(new Packet(PacketCommand.SPECIAL_HEX_DISPLAY_APPEND, c, boardId, shd));
    }

    public void sendSpecialHexDisplayDelete(Coords c, int boardId, SpecialHexDisplay shd) {
        send(new Packet(PacketCommand.SPECIAL_HEX_DISPLAY_DELETE, c, boardId, shd));
    }

    @Override
    protected boolean handleGameSpecificPacket(Packet packet) {
        try {
            switch (packet.command()) {
                case SERVER_GREETING:
                    if (this instanceof Princess) {
                        ((Princess) this).sendPrincessSettings();
                    }
                    break;
                case PRINCESS_SETTINGS:
                    game.setBotSettings(packet.getStringWIthBehaviorSettingsMap(0));
                    break;
                case ENTITY_UPDATE:
                    receiveEntityUpdate(packet);
                    break;
                case ENTITY_MULTI_UPDATE:
                    receiveEntitiesUpdate(packet);
                    break;
                case ENTITY_REMOVE:
                    receiveEntityRemove(packet);
                    break;
                case ENTITY_VISIBILITY_INDICATOR:
                    receiveEntityVisibilityIndicator(packet);
                    break;
                case FORCE_UPDATE:
                    receiveForceUpdate(packet);
                    break;
                case FORCE_DELETE:
                    receiveForcesDelete(packet);
                    break;
                case SENDING_MINEFIELDS:
                    receiveSendingMinefields(packet);
                    break;
                case SENDING_ILLUMINATED_HEXES:
                    receiveIlluminatedHexes(packet);
                    break;
                case CLEAR_ILLUMINATED_HEXES:
                    game.clearIlluminatedPositions();
                    break;
                case UPDATE_MINEFIELDS:
                    receiveUpdateMinefields(packet);
                    break;
                case DEPLOY_MINEFIELDS:
                    receiveDeployMinefields(packet);
                    break;
                case REVEAL_MINEFIELD:
                    receiveRevealMinefield(packet);
                    break;
                case REMOVE_MINEFIELD:
                    receiveRemoveMinefield(packet);
                    break;
                case UPDATE_GROUND_OBJECTS:
                    receiveUpdateGroundObjects(packet);
                    break;
                case ADD_SMOKE_CLOUD:
                    SmokeCloud cloud = packet.getSmokeCloud(0);

                    if (cloud != null) {
                        game.addSmokeCloud(cloud);
                    }

                    break;
                case CHANGE_HEX:
                    Coords hexCoords = packet.getCoords(0);
                    int boardID = packet.getIntValue(1);
                    Hex targetHex = packet.getHex(2);

                    if (hexCoords != null) {
                        game.getBoard(boardID).setHex(hexCoords, targetHex);
                    }

                    break;
                case CHANGE_HEXES:
                    var changedHexes = packet.getBoardLocationHexMap(0);
                    game.getBoards().values().forEach(board -> board.setHexes(changedHexes));
                    break;
                case BLDG_ADD:
                    receiveBuildingAdd(packet);
                    break;
                case BLDG_REMOVE:
                    receiveBuildingRemove(packet);
                    break;
                case BLDG_UPDATE:
                    receiveBuildingUpdate(packet);
                    break;
                case BLDG_COLLAPSE:
                    receiveBuildingCollapse(packet);
                    break;
                case SENDING_TURNS:
                    receiveTurns(packet);
                    break;
                case SENDING_ENTITIES:
                    receiveEntities(packet);
                    break;
                case SENDING_REPORTS:
                case SENDING_REPORTS_TACTICAL_GENIUS:
                    phaseReport = receiveReport(packet.getReportList(0));
                    if (keepGameLog()) {
                        if ((log == null) && (game.getRoundCount() == 1)) {
                            initGameLog();
                        }
                        if (log != null) {
                            log.appendRaw(phaseReport);
                        }
                    }
                    game.addReports(packet.getReportList(0));
                    roundReport = receiveReport(game.getReports(game.getRoundCount()));
                    if (packet.command().isSendingReportsTacticalGenius()) {
                        game.processGameEvent(new GameReportEvent(this, roundReport));
                    }
                    break;
                case SENDING_REPORTS_SPECIAL:
                    game.processGameEvent(new GameReportEvent(this, receiveReport(packet.getReportList(0))));
                    break;
                case SENDING_REPORTS_ALL:
                    var allReports = packet.getReportListOfList(0);
                    game.setAllReports(allReports);
                    if (keepGameLog()) {
                        // Re-write gamelog.txt from scratch
                        initGameLog();
                        if (log != null) {
                            for (List<Report> allReport : allReports) {
                                log.appendRaw(receiveReport(allReport));
                            }
                        }
                    }
                    roundReport = receiveReport(game.getReports(game.getRoundCount()));
                    // We don't really have a copy of the phase report at
                    // this point, so I guess we'll just use the round report
                    // until the next phase actually completes.
                    phaseReport = roundReport;
                    break;
                case ENTITY_ATTACK:
                    receiveAttack(packet);
                    break;
                case TURN:
                    changeTurnIndex(packet.getIntValue(0), packet.getIntValue(1));
                    break;
                case SENDING_GAME_SETTINGS:
                    GameOptions options = packet.getGameOptions(0);

                    if (options != null) {
                        game.setOptions(options);
                    }

                    break;
                case SENDING_MAP_SETTINGS:
                    MapSettings mapSettings = packet.getMapSettings(0);

                    if (mapSettings != null) {
                        game.setMapSettings(mapSettings);
                        GameSettingsChangeEvent gameSettingsChangeEvent = new GameSettingsChangeEvent(this);
                        gameSettingsChangeEvent.setMapSettingsOnlyChange(true);
                        game.processGameEvent(gameSettingsChangeEvent);
                    }
                    break;
                case SENDING_PLANETARY_CONDITIONS:
                    PlanetaryConditions planetaryConditions = packet.getPlanetaryConditions(0);

                    if (planetaryConditions != null) {
                        game.setPlanetaryConditions(planetaryConditions);
                        game.processGameEvent(new GameSettingsChangeEvent(this));
                    }
                    break;
                case SENDING_TAG_INFO:
                    Vector<TagInfo> tagInfoVector = packet.getTagInfoVector(0);
                    for (TagInfo tagInfo : tagInfoVector) {
                        game.addTagInfo(tagInfo);
                    }
                    break;
                case RESET_TAG_INFO:
                    game.resetTagInfo();
                    break;
                case END_OF_GAME:
                    String sEntityStatus = packet.getStringValue(0);
                    game.end(packet.getIntValue(1), packet.getIntValue(2));
                    // save victory report
                    saveEntityStatus(sEntityStatus);
                    break;
                case SENDING_ARTILLERY_ATTACKS:
                    Vector<ArtilleryAttackAction> artilleryAttackActions = packet.getArtilleryAttackAction(0);
                    game.setArtilleryVector(artilleryAttackActions);
                    break;
                case SENDING_FLARES:
                    Vector<Flare> flareVector = packet.getFlareVector(0);
                    game.setFlares(flareVector);
                    break;
                case SEND_SAVEGAME:
                    String sFinalFile = packet.getStringValue(0);
                    String sLocalPath = packet.getStringValue(2);
                    String localFile = sLocalPath + File.separator + sFinalFile;
                    File sDir = new File(sLocalPath);
                    if (!sDir.exists()) {
                        try {
                            if (!sDir.mkdir()) {
                                LOGGER.error("Failed to create savegames directory.");
                                return true;
                            }
                        } catch (Exception ex) {
                            LOGGER.error(ex, "Unable to create savegames directory.");
                        } finally {
                            setAwaitingSave(false);
                        }
                    }

                    try (OutputStream os = new FileOutputStream(localFile);
                          BufferedOutputStream bos = new BufferedOutputStream(os)) {
                        List<Integer> data = packet.getIntList(1);
                        for (Integer integer : data) {
                            bos.write(integer);
                        }
                        bos.flush();
                    } catch (Exception ex) {
                        LOGGER.error(ex, "Unable to save file {}", sFinalFile);
                    }
                    setAwaitingSave(false);
                    break;
                case LOAD_SAVEGAME:
                    String loadFile = packet.getStringValue(0);
                    try {
                        sendLoadGame(new File(MMConstants.SAVEGAME_DIR, loadFile));
                    } catch (Exception ex) {
                        LOGGER.error(ex, "Unable to load savegame file: {}", loadFile);
                    }
                    break;
                case SENDING_SPECIAL_HEX_DISPLAY:
                    var shdTable = packet.getCoordsWithSpecialHexDisplayCollectionMap(0);
                    var boardId = packet.getIntValue(1);
                    game.getBoard(boardId).setSpecialHexDisplayTable(shdTable);
                    game.processGameEvent(new GameBoardChangeEvent(this));
                    break;
                case SENDING_AVAILABLE_MAP_SIZES:
                    availableSizes = packet.getBoardDimensionsSet(0);
                    game.processGameEvent(new GameSettingsChangeEvent(this));
                    break;
                case ENTITY_NOVA_NETWORK_CHANGE:
                    receiveEntityNovaNetworkModeChange(packet);
                    break;
                case CLIENT_FEEDBACK_REQUEST:
                    final PacketCommand cfrType = packet.getPacketCommand(0);
                    if (cfrType != null) {
                        GameCFREvent cfrEvt = new GameCFREvent(this, cfrType);
                        switch (cfrType) {
                            case CFR_DOMINO_EFFECT:
                                cfrEvt.setEntityId(packet.getIntValue(1));
                                break;
                            case CFR_AMS_ASSIGN:
                                cfrEvt.setEntityId(packet.getIntValue(1));
                                cfrEvt.setAmsEquipNum(packet.getIntValue(2));
                                cfrEvt.setWAAs(packet.getWeaponAttackActionList(3));
                                break;
                            case CFR_APDS_ASSIGN:
                                cfrEvt.setEntityId(packet.getIntValue(1));
                                cfrEvt.setApdsDistances(packet.getIntList(2));
                                cfrEvt.setWAAs(packet.getWeaponAttackActionList(3));
                                break;
                            case CFR_HIDDEN_PBS:
                                cfrEvt.setEntityId(packet.getIntValue(1));
                                cfrEvt.setTargetId(packet.getIntValue(2));
                                break;
                            case CFR_TELEGUIDED_TARGET:
                                cfrEvt.setTeleguidedMissileTargets(packet.getIntList(1));
                                cfrEvt.setTmToHitValues(packet.getIntList(2));
                                break;
                            case CFR_TAG_TARGET:
                                cfrEvt.setTAGTargets(packet.getIntList(1));
                                cfrEvt.setTAGTargetTypes(packet.getIntList(2));
                                break;
                            default:
                                break;
                        }
                        game.processGameEvent(cfrEvt);
                    }
                    break;
                case GAME_VICTORY_EVENT:
                    GameVictoryEvent gve = new GameVictoryEvent(this, game);
                    game.processGameEvent(gve);
                    break;
                default:
                    return false;
            }
            return true;

        } catch (InvalidPacketDataException e) {
            LOGGER.error("Invalid packet data:", e);
            return false;
        }
    }

    /**
     * receive and process an entity nova network mode change packet
     *
     * @param packet The received packet
     */
    private void receiveEntityNovaNetworkModeChange(Packet packet) {
        try {
            int entityId = packet.getIntValue(0);
            String networkID = packet.getStringValue(1);
            Entity entity = game.getEntity(entityId);
            if (entity != null) {
                entity.setNewRoundNovaNetworkString(networkID);
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to process Entity Nova Network mode change");
        }
    }

    public void sendDominoCFRResponse(MovePath mp) {
        send(new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST, PacketCommand.CFR_DOMINO_EFFECT, mp));
    }

    public void sendAMSAssignCFRResponse(Integer waaIndex) {
        send(new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST, PacketCommand.CFR_AMS_ASSIGN, waaIndex));
    }

    public void sendAPDSAssignCFRResponse(Integer waaIndex) {
        send(new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST, PacketCommand.CFR_APDS_ASSIGN, waaIndex));
    }

    public void sendHiddenPBSCFRResponse(Vector<EntityAction> attacks) {
        send(new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST, PacketCommand.CFR_HIDDEN_PBS, attacks));
    }

    public void sendTelemissileTargetCFRResponse(int index) {
        send(new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST, PacketCommand.CFR_TELEGUIDED_TARGET, index));
    }

    public void sendTAGTargetCFRResponse(int index) {
        send(new Packet(PacketCommand.CLIENT_FEEDBACK_REQUEST, PacketCommand.CFR_TAG_TARGET, index));
    }

    public Set<BoardDimensions> getAvailableMapSizes() {
        return availableSizes;
    }

    /**
     * If we remove an entity, we may need to update the duplicate identifier.
     *
     * @param ids The Entity IDs to check
     */
    private void checkDuplicateNamesDuringDelete(List<Integer> ids) {
        final List<Entity> updatedEntities = new ArrayList<>();
        synchronized (unitNameTracker) {
            for (int id : ids) {
                Entity removedEntity = game.getEntity(id);
                if (removedEntity == null) {
                    continue;
                }

                unitNameTracker.remove(removedEntity, updatedEntities::add);
            }
        }

        // Send updates for any entity which had its name updated
        for (Entity e : updatedEntities) {
            sendUpdateEntity(e);
        }
    }

    /** Sends the given forces to the server to be made top-level forces. */
    public void sendForceParent(Collection<Force> forceList, int newParentId) {
        send(new Packet(PacketCommand.FORCE_PARENT, forceList, newParentId));
    }

    /**
     * Sends a packet containing multiple entity updates. Should only be used in the lobby phase.
     */
    public void sendChangeTeam(Vector<Player> players, int newTeamId) {
        send(new Packet(PacketCommand.PLAYER_TEAM_CHANGE, players, newTeamId));
    }

    /**
     * Sends an "Update force" packet
     */
    public void sendUpdateForce(Collection<Force> changedForces, Collection<Entity> changedEntities) {
        send(new Packet(PacketCommand.FORCE_UPDATE, changedForces, changedEntities));
    }

    /**
     * Sends an "Update force" packet
     */
    public void sendUpdateForce(Collection<Force> changedForces) {
        send(new Packet(PacketCommand.FORCE_UPDATE, changedForces, new ArrayList<>()));
    }

    /**
     * Sends a packet instructing the server to add the given entities to the given force. The server will handle this;
     * the client does not have to implement the change.
     */
    public void sendAddEntitiesToForce(Collection<Entity> entities, int forceId) {
        send(new Packet(PacketCommand.FORCE_ADD_ENTITY, entities, forceId));
    }

    /**
     * Sends a packet instructing the server to add the given entities to the given force. The server will handle this;
     * the client does not have to implement the change.
     */
    public void sendAssignForceFull(Collection<Force> forceList, int newOwnerId) {
        send(new Packet(PacketCommand.FORCE_ASSIGN_FULL, forceList, newOwnerId));
    }

    /**
     * Sends a packet to the Server requesting to delete the given forces.
     */
    public void sendDeleteForces(List<Force> toDelete) {
        send(new Packet(PacketCommand.FORCE_DELETE, toDelete.stream()
              .mapToInt(Force::getId)
              .boxed()
              .collect(Collectors.toList())));
    }

    /**
     * Sends an "Add force" packet
     */
    public void sendAddForce(Force force, Collection<Entity> entities) {
        send(new Packet(PacketCommand.FORCE_ADD, force, entities));
    }

    /**
     * Sends an "update custom initiative" packet
     */
    public void sendCustomInit(Player player) {
        send(new Packet(PacketCommand.CUSTOM_INITIATIVE, player));
    }

    public AbstractSkillGenerator getSkillGenerator() {
        return skillGenerator;
    }

    public void setSkillGenerator(final AbstractSkillGenerator skillGenerator) {
        this.skillGenerator = skillGenerator;
    }

    /**
     * Send command to unload stranded entities to the server
     */
    public void sendUnloadStranded(int... entityIds) {
        send(new Packet(PacketCommand.UNLOAD_STRANDED, entityIds));
    }

    /**
     * Change whose turn it is.
     */
    protected void changeTurnIndex(int index, int prevPlayerId) {
        game.setTurnIndex(index, prevPlayerId);
    }

    /**
     * Send mode-change data to the server
     */
    public void sendModeChange(int nEntity, int nEquip, int nMode) {
        send(new Packet(PacketCommand.ENTITY_MODE_CHANGE, nEntity, nEquip, nMode));
    }

    /**
     * Send mount-facing-change data to the server
     */
    public void sendMountFacingChange(int nEntity, int nEquip, int nFacing) {
        send(new Packet(PacketCommand.ENTITY_MOUNTED_FACING_CHANGE, nEntity, nEquip, nFacing));
    }

    /**
     * Send called shot change data to the server
     */
    public void sendCalledShotChange(int nEntity, int nEquip) {
        send(new Packet(PacketCommand.ENTITY_CALLED_SHOT_CHANGE, nEntity, nEquip));
    }

    /**
     * Send system mode-change data to the server
     */
    public void sendSystemModeChange(int nEntity, int nSystem, int nMode) {
        send(new Packet(PacketCommand.ENTITY_SYSTEM_MODE_CHANGE, nEntity, nSystem, nMode));
    }

    /**
     * Send mode-change data to the server
     */
    public void sendAmmoChange(int nEntity, int nWeapon, int nAmmo, int reason) {
        send(new Packet(PacketCommand.ENTITY_AMMO_CHANGE, nEntity, nWeapon, nAmmo, reason));
    }

    /**
     * Send sensor-change data to the server
     */
    public void sendSensorChange(int nEntity, int nSensor) {
        send(new Packet(PacketCommand.ENTITY_SENSOR_CHANGE, nEntity, nSensor));
    }

    /**
     * Send sinks-change data to the server
     */
    public void sendSinksChange(int nEntity, int activeSinks) {
        send(new Packet(PacketCommand.ENTITY_SINKS_CHANGE, nEntity, activeSinks));
    }

    /**
     * Send activate hidden data to the server
     */
    public void sendActivateHidden(int nEntity, GamePhase phase) {
        send(new Packet(PacketCommand.ENTITY_ACTIVATE_HIDDEN, nEntity, phase));
    }

    /**
     * Send movement data for the given entity to the server.
     */
    public void moveEntity(int id, MovePath md) {
        send(new Packet(PacketCommand.ENTITY_MOVE, id, md));
    }

    /**
     * sends a load game file to the server
     */
    public void sendLoadGame(File f) {
        try (InputStream is = new FileInputStream(f)) {
            InputStream gzi;

            if (f.getName().toLowerCase().endsWith(".gz")) {
                gzi = new GZIPInputStream(is);
            } else {
                gzi = is;
            }

            game.reset();
            send(new Packet(PacketCommand.LOAD_GAME, SerializationHelper.getLoadSaveGameXStream().fromXML(gzi)));
        } catch (Exception ex) {
            String message = String.format("Can't find the local savegame %s", f);
            LOGGER.error(ex, message);
        }
    }
}
