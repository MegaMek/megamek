/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client;

import megamek.MMConstants;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.commands.*;
import megamek.client.generator.RandomUnitGenerator;
import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.client.generator.skillGenerators.ModifiedTotalWarfareSkillGenerator;
import megamek.client.ui.IClientCommandHandler;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.ImageUtil;
import megamek.common.util.SerializationHelper;
import megamek.common.util.StringUtil;
import megamek.server.SmokeCloud;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * This class is instantiated for each client and for each bot running on that
 * client. non-local clients are not also instantiated on the local server.
 */
public class Client extends AbstractClient implements IClientCommandHandler {

    /**
     * The game state object: this object is not ever replaced during a game, only updated. A
     * reference can therefore be cached by other objects.
     */
    protected final Game game = new Game();

    // Hashtable for storing image tags containing base64Text src
    private Hashtable<Integer, String> imgCache;
    private BoardView bv;
    private Coords currentHex;
    private Set<BoardDimensions> availableSizes = new TreeSet<>();
    private Vector<Coords> artilleryAutoHitHexes = null;
    private AbstractSkillGenerator skillGenerator;

    public Client(String name, String host, int port) {
        super(name, host, port);
        setSkillGenerator(new ModifiedTotalWarfareSkillGenerator());
        registerCommand(new HelpCommand(this));
        registerCommand(new MoveCommand(this));
        registerCommand(new RulerCommand(this));
        registerCommand(new ShowEntityCommand(this));
        registerCommand(new FireCommand(this));
        registerCommand(new DeployCommand(this));
        registerCommand(new AddBotCommand(this));
        registerCommand(new AssignNovaNetworkCommand(this));
        registerCommand(new SitrepCommand(this));
        registerCommand(new LookCommand(this));
        registerCommand(new ChatCommand(this));
        registerCommand(new DoneCommand(this));
        ShowTileCommand tileCommand = new ShowTileCommand(this);
        registerCommand(tileCommand);
        for (String direction : ShowTileCommand.directions) {
            clientCommands.put(direction.toLowerCase(), tileCommand);
        }
    }

    @Override
    public IGame getIGame() {
        return game;
    }

    public Game getGame() {
        return game;
    }


    /**
     * Get hexes designated for automatic artillery hits.
     */
    public Vector<Coords> getArtilleryAutoHit() {
        return artilleryAutoHitHexes;
    }


    public Entity getEntity(int id) {
        return game.getEntity(id);
    }

    /**
     * Returns an Enumeration of the entities that match the
     * selection criteria.
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
     * Returns an enumeration of the entities in game.entities
     */
    public List<Entity> getEntitiesVector() {
        return game.getEntitiesVector();
    }

    public MapSettings getMapSettings() {
        return game.getMapSettings();
    }

    public void setBoardView(BoardView bv) {
        this.bv = bv;
    }

    /**
     * Changes the game phase, and the displays that go along with it.
     */
    public void changePhase(GamePhase phase) {
        game.setPhase(phase);
        // Handle phase-specific items.
        switch (phase) {
            case STARTING_SCENARIO:
            case EXCHANGE:
                sendDone(true);
                break;
            case DEPLOYMENT:
                // free some memory that's only needed in lounge
                MechFileParser.dispose();
                // We must do this last, as the name and unit generators can create
                // a new instance if they are running
                MechSummaryCache.dispose();
                memDump("entering deployment phase");
                break;
            case TARGETING:
                memDump("entering targeting phase");
                break;
            case MOVEMENT:
                memDump("entering movement phase");
                break;
            case PREMOVEMENT:
                memDump("entering premovement phase");
                break;
            case OFFBOARD:
                memDump("entering offboard phase");
                break;
            case PREFIRING:
                memDump("entering prefiring phase");
                break;
            case FIRING:
                memDump("entering firing phase");
                break;
            case PHYSICAL:
                memDump("entering physical phase");
                break;
            case LOUNGE:
                MechSummaryCache.getInstance().addListener(RandomUnitGenerator::getInstance);
                if (MechSummaryCache.getInstance().isInitialized()) {
                    RandomUnitGenerator.getInstance();
                }
                synchronized (unitNameTracker) {
                    unitNameTracker.clear(); // reset this
                }
                break;
            default:
                break;
        }
    }

    /**
     * is it my turn?
     */
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
    @SuppressWarnings("unchecked")
    protected void receiveTurns(Packet packet) {
        game.setTurnVector((List<GameTurn>) packet.getObject(0));
    }

    /**
     * Can I unload entities stranded on immobile transports?
     */
    public boolean canUnloadStranded() {
        return (game.getTurn() instanceof GameTurn.UnloadStrandedTurn)
                && game.getTurn().isValid(localPlayerNumber, game);
    }

    /**
     * Maintain backwards compatibility.
     *
     * @param id
     *            - the <code>int</code> ID of the deployed entity
     * @param c
     *            - the <code>Coords</code> where the entity should be deployed
     * @param nFacing
     *            - the <code>int</code> direction the entity should face
     */
    public void deploy(int id, Coords c, int nFacing, int elevation) {
        this.deploy(id, c, nFacing, elevation, new Vector<>(), false);
    }

    /**
     * Deploy an entity at the given coordinates, with the given facing, and
     * starting with the given units already loaded.
     *
     * @param id
     *            - the <code>int</code> ID of the deployed entity
     * @param c
     *            - the <code>Coords</code> where the entity should be deployed
     * @param nFacing
     *            - the <code>int</code> direction the entity should face
     * @param loadedUnits
     *            - a <code>List</code> of units that start the game being
     *            transported byt the deployed entity.
     * @param assaultDrop
     *            - true if deployment is an assault drop
     */
    public void deploy(int id, Coords c, int nFacing, int elevation, List<Entity> loadedUnits, boolean assaultDrop) {
        int packetCount = 6 + loadedUnits.size();
        int index = 0;
        Object[] data = new Object[packetCount];
        data[index++] = id;
        data[index++] = c;
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
     * For ground to air attacks, the ground unit targets the closest hex in the
     * air units flight path. In the case of several equidistant hexes, the
     * attacker gets to choose. This method updates the server with the users
     * choice.
     *
     * @param targetId The target ID
     * @param attackerId The attacker Entity ID
     * @param pos The selected hex
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
    public void sendPrephaseData(int aen) {
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
            send(new Packet(PacketCommand.ENTITY_WORDER_UPDATE, entity.getId(),
                    entity.getWeaponSortOrder(), entity.getCustomWeaponOrder()));
        } else {
            send(new Packet(PacketCommand.ENTITY_WORDER_UPDATE, entity.getId(),
                    entity.getWeaponSortOrder()));
        }
        entity.setWeapOrderChanged(false);
    }

    /**
     * Sends an "add entity" packet with only one Entity.
     *
     * @param entity
     *            The Entity to add.
     */
    public void sendAddEntity(Entity entity) {
        ArrayList<Entity> entities = new ArrayList<>(1);
        entities.add(entity);
        sendAddEntity(entities);
    }

    /**
     * Sends an "add entity" packet that contains a collection of Entity
     * objects.
     *
     * @param entities
     *            The collection of Entity objects to add.
     */
    public void sendAddEntity(List<Entity> entities) {
        for (Entity entity : entities) {
            checkDuplicateNamesDuringAdd(entity);
        }
        send(new Packet(PacketCommand.ENTITY_ADD, entities));
    }

    /**
     * Sends an "add squadron" packet
     */
    public void sendAddSquadron(FighterSquadron fs, Collection<Integer> fighterIds) {
        checkDuplicateNamesDuringAdd(fs);
        send(new Packet(PacketCommand.SQUADRON_ADD, fs, fighterIds));
    }

    /**
     * Sends an "deploy minefields" packet
     */
    public void sendDeployMinefields(Vector<Minefield> minefields) {
        send(new Packet(PacketCommand.DEPLOY_MINEFIELDS, minefields));
    }

    /**
     * Sends a "set Artillery Autohit Hexes" packet
     */
    public void sendArtyAutoHitHexes(Vector<Coords> hexes) {
        artilleryAutoHitHexes = hexes; // save for minimap use
        send(new Packet(PacketCommand.SET_ARTILLERY_AUTOHIT_HEXES, hexes));
    }

    /**
     * Sends an "update entity" packet
     */
    public void sendUpdateEntity(Entity entity) {
        send(new Packet(PacketCommand.ENTITY_UPDATE, entity));
    }

    /**
     * Sends a packet containing multiple entity updates. Should only be used
     * in the lobby phase.
     */
    public void sendUpdateEntity(Collection<Entity> entities) {
        send(new Packet(PacketCommand.ENTITY_MULTIUPDATE, entities));
    }

    /**
     * Sends a packet containing multiple entity updates. Should only be used
     * in the lobby phase.
     */
    public void sendChangeOwner(Collection<Entity> entities, int newOwnerId) {
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

    public void sendExplodeBuilding(Building.DemolitionCharge charge) {
        send(new Packet(PacketCommand.BLDG_EXPLODE, charge));
    }

    /**
     * Loads the board from the data in the net command.
     */
    protected void receiveBoard(Packet c) {
        Board newBoard = (Board) c.getObject(0);
        game.setBoard(newBoard);
    }

    /**
     * Loads the entities from the data in the net command.
     */
    @SuppressWarnings("unchecked")
    protected void receiveEntities(Packet c) {
        List<Entity> newEntities = (List<Entity>) c.getObject(0);
        List<Entity> newOutOfGame = (List<Entity>) c.getObject(1);
        Forces forces = (Forces) c.getObject(2);
        // Replace the entities in the game.
        if (forces != null) {
            game.setForces(forces);
        }
        game.setEntitiesVector(newEntities);
        if (newOutOfGame != null) {
            game.setOutOfGameEntitiesVector(newOutOfGame);
            for (Entity e: newOutOfGame) {
                cacheImgTag(e);
            }
        }
        // cache the image data for the entities and set force for entities
        for (Entity e: newEntities) {
            cacheImgTag(e);
            e.setForceId(game.getForces().getForceId(e));
        }

        if (GUIPreferences.getInstance().getMiniReportShowSprites() &&
                game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND) &&
                imgCache != null && !imgCache.containsKey(Report.HIDDEN_ENTITY_NUM)) {
            ImageUtil.createDoubleBlindHiddenImage(imgCache);
        }
    }

    /**
     * Receives a force-related update containing affected forces and affected entities
     */
    @SuppressWarnings("unchecked")
    protected void receiveForceUpdate(Packet c) {
        Collection<Force> forces = (Collection<Force>) c.getObject(0);
        Collection<Entity> entities = (Collection<Entity>) c.getObject(1);
        for (Force force : forces) {
            getGame().getForces().replace(force.getId(), force);
        }

        for (Entity entity : entities) {
            getGame().setEntity(entity.getId(), entity);
        }
    }

    /** Receives a server packet commanding deletion of forces. Only valid in the lobby phase. */
    protected void receiveForcesDelete(Packet c) {
        @SuppressWarnings("unchecked")
        Collection<Integer> forceIds = (Collection<Integer>) c.getObject(0);
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
    @SuppressWarnings("unchecked")
    protected void receiveEntityUpdate(Packet c) {
        int eindex = c.getIntValue(0);
        Entity entity = (Entity) c.getObject(1);
        Vector<UnitLocation> movePath = (Vector<UnitLocation>) c.getObject(2);
        // Replace this entity in the game.
        getGame().setEntity(eindex, entity, movePath);
    }

    /**
     * Update multiple entities from the server. Used only in the lobby phase.
     */
    @SuppressWarnings("unchecked")
    protected void receiveEntitiesUpdate(Packet c) {
        Collection<Entity> entities = (Collection<Entity>) c.getObject(0);
        for (Entity entity: entities) {
            getGame().setEntity(entity.getId(), entity);
        }
    }

    protected void receiveEntityRemove(Packet packet) {
        @SuppressWarnings("unchecked")
        List<Integer> entityIds = (List<Integer>) packet.getObject(0);
        int condition = packet.getIntValue(1);
        @SuppressWarnings("unchecked")
        List<Force> forces = (List<Force>) packet.getObject(2);
        // create a final image for the entity
        for (int id: entityIds) {
            cacheImgTag(game.getEntity(id));
        }
        for (Force force: forces) {
            game.getForces().replace(force.getId(), force);
        }
        // Move the unit to its final resting place.
        game.removeEntities(entityIds, condition);
    }

    @SuppressWarnings("unchecked")
    protected void receiveEntityVisibilityIndicator(Packet packet) {
        Entity e = game.getEntity(packet.getIntValue(0));
        if (e != null) { // we may not have this entity due to double blind
            e.setEverSeenByEnemy(packet.getBooleanValue(1));
            e.setVisibleToEnemy(packet.getBooleanValue(2));
            e.setDetectedByEnemy(packet.getBooleanValue(3));
            e.setWhoCanSee((Vector<Player>) packet.getObject(4));
            e.setWhoCanDetect((Vector<Player>) packet.getObject(5));
            // this next call is only needed sometimes, but we'll just
            // call it everytime
            game.processGameEvent(new GameEntityChangeEvent(this, e));
        }
    }

    @SuppressWarnings("unchecked")
    protected void receiveDeployMinefields(Packet packet) {
        game.addMinefields((Vector<Minefield>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveSendingMinefields(Packet packet) {
        game.setMinefields((Vector<Minefield>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveIlluminatedHexes(Packet p) {
        game.setIlluminatedPositions((HashSet<Coords>) p.getObject(0));
    }

    protected void receiveRevealMinefield(Packet packet) {
        game.addMinefield((Minefield) packet.getObject(0));
    }

    protected void receiveRemoveMinefield(Packet packet) {
        game.removeMinefield((Minefield) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveUpdateMinefields(Packet packet) {
        // only update information if you know about the minefield
        Vector<Minefield> newMines = new Vector<>();
        for (Minefield mf : (Vector<Minefield>) packet.getObject(0)) {
            if (getLocalPlayer().containsMinefield(mf)) {
                newMines.add(mf);
            }
        }

        if (!newMines.isEmpty()) {
            game.resetMinefieldDensity(newMines);
        }
    }

    @SuppressWarnings("unchecked")
    protected void receiveBuildingUpdate(Packet packet) {
        game.getBoard().updateBuildings((Vector<Building>) packet.getObject(0));
    }

    @SuppressWarnings("unchecked")
    protected void receiveBuildingCollapse(Packet packet) {
        game.getBoard().collapseBuilding((Vector<Coords>) packet.getObject(0));
    }

    /**
     * Loads entity firing data from the data in the net command
     */
    @SuppressWarnings("unchecked")
    protected void receiveAttack(Packet c) {
        List<EntityAction> vector = (List<EntityAction>) c.getObject(0);
        int charge = c.getIntValue(1);
        boolean addAction = true;
        for (EntityAction ea : vector) {
            int entityId = ea.getEntityId();
            if ((ea instanceof TorsoTwistAction) && game.hasEntity(entityId)) {
                TorsoTwistAction tta = (TorsoTwistAction) ea;
                Entity entity = game.getEntity(entityId);
                entity.setSecondaryFacing(tta.getFacing());
            } else if ((ea instanceof FlipArmsAction) && game.hasEntity(entityId)) {
                FlipArmsAction faa = (FlipArmsAction) ea;
                Entity entity = game.getEntity(entityId);
                entity.setArmsFlipped(faa.getIsFlipped());
            } else if ((ea instanceof DodgeAction) && game.hasEntity(entityId)) {
                Entity entity = game.getEntity(entityId);
                entity.dodging = true;
                addAction = false;
            } else if (ea instanceof AttackAction) {
                // The equipment type of a club needs to be restored.
                if (ea instanceof ClubAttackAction) {
                    ClubAttackAction caa = (ClubAttackAction) ea;
                    Mounted club = caa.getClub();
                    club.restore();
                }
            }

            if (addAction) {
                // track in the appropriate list
                if (charge == 0) {
                    game.addAction(ea);
                } else if (charge == 1) {
                    game.addCharge((AttackAction) ea);
                }
            }
        }
    }


    // Should be private?
    public String receiveReport(Vector<Report> v) {
        if (v == null) {
            return "[null report vector]";
        }

        StringBuffer report = new StringBuffer();
        for (Report r : v) {
            report.append(r.getText());
        }

        Set<Integer> setEntity = new HashSet<>();
        //find id stored in spans and extract it
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
            if (getCachedImgTag(i) != null) {
                updatedReport = updatedReport.replace("<span id='" + i + "'></span>", getCachedImgTag(i));
            }
        }

        Set<String> setCrew = new HashSet<>();
        //find id stored in spans and extract it
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
                        float imgSize = UIUtil.scaleForGUI(PilotToolTip.PORTRAIT_BASESIZE);
                        imgSize /= 0.2f * (crew.getSlotCount() - 1) + 1;
                        Image portrait = crew.getPortrait(crewID).getBaseImage().getScaledInstance(-1, (int) imgSize, Image.SCALE_SMOOTH);
                        // convert image to base64, add to the <img> tag and store in cache
                        BufferedImage bufferedImage = new BufferedImage(portrait.getWidth(null), portrait.getHeight(null), BufferedImage.TYPE_INT_RGB);
                        bufferedImage.getGraphics().drawImage(portrait, 0, 0, null);
                        String base64Text = ImageUtil.base64TextEncodeImage(bufferedImage);
                        String img = "<img src='data:image/png;base64," + base64Text + "'>";
                        updatedReport = updatedReport.replace("<span crew='" + entityID + ":" + crewID + "'></span>", img);
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
                || (imgCache == null) || !imgCache.containsKey(id)) {
            return null;
        }
        return imgCache.get(id);
    }

    /**
     * Hashtable for storing img tags containing base64Text src.
     */
    protected void cacheImgTag(Entity entity) {
        if (entity == null) {
            return;
        }

        // remove images that should be refreshed
        if (imgCache == null) {
            imgCache = new Hashtable<>();
        } else {
            imgCache.remove(entity.getId());
        }

        if (getTargetImage(entity) != null) {
            // convert image to base64, add to the <img> tag and store in cache
            BufferedImage image = ImageUtil.getScaledImage(getTargetImage(entity), 56, 48);
            String base64Text = ImageUtil.base64TextEncodeImage(image);
            String img = "<img src='data:image/png;base64," + base64Text + "'>";
            imgCache.put(entity.getId(), img);
        }
    }

    /**
     * Gets the current mech image
     */
    private Image getTargetImage(Entity e) {
        if (bv == null) {
            return null;
        } else if (e.isDestroyed()) {
            return bv.getTilesetManager().wreckMarkerFor(e, -1);
        } else {
            return bv.getTilesetManager().imageFor(e);
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
            LogManager.getLogger().error("", ex);
        }
    }

    /**
     * Send a Nova CEWS update packet
     */
    public void sendNovaChange(int id, String net) {
        send(new Packet(PacketCommand.ENTITY_NOVA_NETWORK_CHANGE, id, net));
    }

    public void sendSpecialHexDisplayAppend(Coords c, SpecialHexDisplay shd) {
        send(new Packet(PacketCommand.SPECIAL_HEX_DISPLAY_APPEND, c, shd));
    }

    public void sendSpecialHexDisplayDelete(Coords c, SpecialHexDisplay shd) {
        send(new Packet(PacketCommand.SPECIAL_HEX_DISPLAY_DELETE, c, shd));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean handleGameSpecificPacket(Packet packet) throws Exception {
        if (packet == null) {
            LogManager.getLogger().error("Client: got null packet");
            return false;
        }

        switch (packet.getCommand()) {
            case SERVER_GREETING:
                connected = true;
                send(new Packet(PacketCommand.CLIENT_NAME, name, isBot()));
                if (this instanceof Princess) {
                    ((Princess) this).sendPrincessSettings();
                }
                break;
            case SERVER_CORRECT_NAME:
                correctName(packet);
                break;
            case PRINCESS_SETTINGS:
                game.setBotSettings((Map<String, BehaviorSettings>) packet.getObject(0));
                break;
            case ENTITY_UPDATE:
                receiveEntityUpdate(packet);
                break;
            case ENTITY_MULTIUPDATE:
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
            case SENDING_ILLUM_HEXES:
                receiveIlluminatedHexes(packet);
                break;
            case CLEAR_ILLUM_HEXES:
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
            case ADD_SMOKE_CLOUD:
                SmokeCloud cloud = (SmokeCloud) packet.getObject(0);
                game.addSmokeCloud(cloud);
                break;
            case CHANGE_HEX:
                game.getBoard().setHex((Coords) packet.getObject(0), (Hex) packet.getObject(1));
                break;
            case CHANGE_HEXES:
                List<Coords> coords = new ArrayList<>((Set<Coords>) packet.getObject(0));
                List<Hex> hexes = new ArrayList<>((Set<Hex>) packet.getObject(1));
                game.getBoard().setHexes(coords, hexes);
                break;
            case BLDG_UPDATE:
                receiveBuildingUpdate(packet);
                break;
            case BLDG_COLLAPSE:
                receiveBuildingCollapse(packet);
                break;
            case PHASE_CHANGE:
                changePhase((GamePhase) packet.getObject(0));
                break;
            case ROUND_UPDATE:
                game.setRoundCount(packet.getIntValue(0));
                break;
            case SENDING_TURNS:
                receiveTurns(packet);
                break;
            case SENDING_BOARD:
                receiveBoard(packet);
                break;
            case SENDING_ENTITIES:
                receiveEntities(packet);
                break;
            case SENDING_REPORTS:
            case SENDING_REPORTS_TACTICAL_GENIUS:
                phaseReport = receiveReport((Vector<Report>) packet.getObject(0));
                if (keepGameLog()) {
                    if ((log == null) && (game.getRoundCount() == 1)) {
                        initGameLog();
                    }
                    if (log != null) {
                        log.append(phaseReport);
                    }
                }
                game.addReports((Vector<Report>) packet.getObject(0));
                roundReport = receiveReport(game.getReports(game.getRoundCount()));
                if (packet.getCommand().isSendingReportsTacticalGenius()) {
                    game.processGameEvent(new GameReportEvent(this, roundReport));
                }
                break;
            case SENDING_REPORTS_SPECIAL:
                game.processGameEvent(new GameReportEvent(this,
                        receiveReport((Vector<Report>) packet.getObject(0))));
                break;
            case SENDING_REPORTS_ALL:
                Vector<Vector<Report>> allReports = (Vector<Vector<Report>>) packet.getObject(0);
                game.setAllReports(allReports);
                if (keepGameLog()) {
                    // Re-write gamelog.txt from scratch
                    initGameLog();
                    if (log != null) {
                        for (int i = 0; i < allReports.size(); i++) {
                            log.append(receiveReport(allReports.elementAt(i)));
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
                game.setOptions((GameOptions) packet.getObject(0));
                break;
            case SENDING_MAP_SETTINGS:
                MapSettings mapSettings = (MapSettings) packet.getObject(0);
                game.setMapSettings(mapSettings);
                GameSettingsChangeEvent evt = new GameSettingsChangeEvent(this);
                evt.setMapSettingsOnlyChange(true);
                game.processGameEvent(evt);
                break;
            case SENDING_PLANETARY_CONDITIONS:
                game.setPlanetaryConditions((PlanetaryConditions) packet.getObject(0));
                game.processGameEvent(new GameSettingsChangeEvent(this));
                break;
            case SENDING_TAG_INFO:
                Vector<TagInfo> vti = (Vector<TagInfo>) packet.getObject(0);
                for (TagInfo ti : vti) {
                    game.addTagInfo(ti);
                }
                break;
            case RESET_TAG_INFO:
                game.resetTagInfo();
                break;
            case END_OF_GAME:
                String sEntityStatus = (String) packet.getObject(0);
                game.end(packet.getIntValue(1), packet.getIntValue(2));
                // save victory report
                saveEntityStatus(sEntityStatus);
                break;
            case SENDING_ARTILLERY_ATTACKS:
                Vector<ArtilleryAttackAction> v = (Vector<ArtilleryAttackAction>) packet.getObject(0);
                game.setArtilleryVector(v);
                break;
            case SENDING_FLARES:
                Vector<Flare> v2 = (Vector<Flare>) packet.getObject(0);
                game.setFlares(v2);
                break;
            case SEND_SAVEGAME:
                String sFinalFile = (String) packet.getObject(0);
                String sLocalPath = (String) packet.getObject(2);
                String localFile = sLocalPath + File.separator + sFinalFile;
                File sDir = new File(sLocalPath);
                if (!sDir.exists()) {
                    try {
                        if (!sDir.mkdir()) {
                            LogManager.getLogger().error("Failed to create savegames directory.");
                            return true;
                        }
                    } catch (Exception ex) {
                        LogManager.getLogger().error("Unable to create savegames directory.", ex);
                    }
                }

                try (OutputStream os = new FileOutputStream(localFile);
                     BufferedOutputStream bos = new BufferedOutputStream(os)) {
                    List<Integer> data = (List<Integer>) packet.getObject(1);
                    for (Integer d : data) {
                        bos.write(d);
                    }
                    bos.flush();
                } catch (Exception ex) {
                    LogManager.getLogger().error("Unable to save file " + sFinalFile, ex);
                }
                break;
            case LOAD_SAVEGAME:
                String loadFile = (String) packet.getObject(0);
                try {
                    sendLoadGame(new File(MMConstants.SAVEGAME_DIR, loadFile));
                } catch (Exception ex) {
                    LogManager.getLogger().error("Unable to load savegame file: " + loadFile, ex);
                }
                break;
            case SENDING_SPECIAL_HEX_DISPLAY:
                game.getBoard().setSpecialHexDisplayTable(
                        (Hashtable<Coords, Collection<SpecialHexDisplay>>) packet.getObject(0));
                game.processGameEvent(new GameBoardChangeEvent(this));
                break;
            case SENDING_AVAILABLE_MAP_SIZES:
                availableSizes = (Set<BoardDimensions>) packet.getObject(0);
                game.processGameEvent(new GameSettingsChangeEvent(this));
                break;
            case ENTITY_NOVA_NETWORK_CHANGE:
                receiveEntityNovaNetworkModeChange(packet);
                break;
            case CLIENT_FEEDBACK_REQUEST:
                final PacketCommand cfrType = (PacketCommand) packet.getData()[0];
                GameCFREvent cfrEvt = new GameCFREvent(this, cfrType);
                switch (cfrType) {
                    case CFR_DOMINO_EFFECT:
                        cfrEvt.setEntityId((int) packet.getData()[1]);
                        break;
                    case CFR_AMS_ASSIGN:
                        cfrEvt.setEntityId((int) packet.getData()[1]);
                        cfrEvt.setAmsEquipNum((int) packet.getData()[2]);
                        cfrEvt.setWAAs((List<WeaponAttackAction>) packet.getData()[3]);
                        break;
                    case CFR_APDS_ASSIGN:
                        cfrEvt.setEntityId((int) packet.getData()[1]);
                        cfrEvt.setApdsDists((List<Integer>) packet.getData()[2]);
                        cfrEvt.setWAAs((List<WeaponAttackAction>) packet.getData()[3]);
                        break;
                    case CFR_HIDDEN_PBS:
                        cfrEvt.setEntityId((int) packet.getObject(1));
                        cfrEvt.setTargetId((int) packet.getObject(2));
                        break;
                    case CFR_TELEGUIDED_TARGET:
                        cfrEvt.setTeleguidedMissileTargets((List<Integer>) packet.getObject(1));
                        cfrEvt.setTmToHitValues((List<Integer>) packet.getObject(2));
                        break;
                    case CFR_TAG_TARGET:
                        cfrEvt.setTAGTargets((List<Integer>) packet.getObject(1));
                        cfrEvt.setTAGTargetTypes((List<Integer>) packet.getObject(2));
                        break;
                    default:
                        break;
                }
                game.processGameEvent(cfrEvt);
                break;
            case GAME_VICTORY_EVENT:
                GameVictoryEvent gve = new GameVictoryEvent(this, game);
                game.processGameEvent(gve);
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * receive and process an entity nova network mode change packet
     *
     * @param c The received packet
     */
    private void receiveEntityNovaNetworkModeChange(Packet c) {
        try {
            int entityId = c.getIntValue(0);
            String networkID = c.getObject(1).toString();
            Entity e = game.getEntity(entityId);
            if (e != null) {
                e.setNewRoundNovaNetworkString(networkID);
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to process Entity Nova Network mode change", ex);
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
     * Sends a packet containing multiple entity updates. Should only be used
     * in the lobby phase.
     */
    public void sendChangeTeam(Collection<Player> players, int newTeamId) {
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
     * Sends a packet instructing the server to add the given entities to the given force.
     * The server will handle this; the client does not have to implement the change.
     */
    public void sendAddEntitiesToForce(Collection<Entity> entities, int forceId) {
        send(new Packet(PacketCommand.FORCE_ADD_ENTITY, entities, forceId));
    }

    /**
     * Sends a packet instructing the server to add the given entities to the given force.
     * The server will handle this; the client does not have to implement the change.
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
        send(new Packet(PacketCommand.ENTITY_MODECHANGE, nEntity, nEquip, nMode));
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
        send(new Packet(PacketCommand.ENTITY_CALLEDSHOTCHANGE, nEntity, nEquip));
    }

    /**
     * Send system mode-change data to the server
     */
    public void sendSystemModeChange(int nEntity, int nSystem, int nMode) {
        send(new Packet(PacketCommand.ENTITY_SYSTEMMODECHANGE, nEntity, nSystem, nMode));
    }

    /**
     * Send mode-change data to the server
     */
    public void sendAmmoChange(int nEntity, int nWeapon, int nAmmo, int reason) {
        send(new Packet(PacketCommand.ENTITY_AMMOCHANGE, nEntity, nWeapon, nAmmo, reason));
    }

    /**
     * Send sensor-change data to the server
     */
    public void sendSensorChange(int nEntity, int nSensor) {
        send(new Packet(PacketCommand.ENTITY_SENSORCHANGE, nEntity, nSensor));
    }

    /**
     * Send sinks-change data to the server
     */
    public void sendSinksChange(int nEntity, int activeSinks) {
        send(new Packet(PacketCommand.ENTITY_SINKSCHANGE, nEntity, activeSinks));
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
            LogManager.getLogger().error("Can't find the local savegame " + f, ex);
        }
    }

    /**
     * Return the Current Hex, used by client commands for the visually impaired
     * @return the current Hex
     */
    public Coords getCurrentHex() {
        return currentHex;
    }

    /**
     * Set the Current Hex, used by client commands for the visually impaired
     */
    public void setCurrentHex(Hex hex) {
        if (hex != null) {
            currentHex = hex.getCoords();
        }
    }

    public void setCurrentHex(Coords hex) {
        currentHex = hex;
    }
}