/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.server;

import megamek.client.ui.swing.lobby.LobbyActions;
import megamek.common.Entity;
import megamek.common.ForceAssignable;
import megamek.common.Game;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import org.apache.logging.log4j.LogManager;
import megamek.server.gameManager.*;

import java.util.*;

import static java.util.stream.Collectors.toList;

class ServerLobbyHelper {
    /**
     * Returns true when the given new force (that is not part of the given game's forces)
     * can be integrated into game's forces without error; i.e.:
     * if the force's parent exists or it is top-level, 
     * if it has no entities and no subforces,
     * if the client sending it is the owner
     */
    static boolean isNewForceValid(Game game, Force force) {
        if ((!force.isTopLevel() && !game.getForces().contains(force.getParentId()))
                || (force.getChildCount() != 0)) {
            return false;
        }
        return true;
    }

    /** 
     * Writes a "Unit XX has been customized" message to the chat. The message
     * is adapted to blind drop conditions. 
     */
    static String entityUpdateMessage(Entity entity, Game game) {
        StringBuilder result = new StringBuilder();
        if (game.getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP)) {
            result.append("A Unit of ");
            result.append(entity.getOwner().getName());
            result.append(" has been customized.");

        } else if (game.getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP)) {
            result.append("Unit ");
            if (!entity.getExternalIdAsString().equals("-1")) {
                result.append('[').append(entity.getExternalIdAsString()).append("] ");
            }
            result.append(entity.getId());
            result.append('(').append(entity.getOwner().getName()).append(')');
            result.append(" has been customized.");

        } else {
            result.append("Unit ");
            result.append(entity.getDisplayName());
            result.append(" has been customized.");
        }
        return result.toString();
    }
    
    /** 
     * Disembarks and offloads the given entities (removing it from transports
     * and removing transported units from it). 
     * Returns a set of entities that received changes. The set may be empty, but not null.
     * <P>NOTE: This is a simplified unload that is only valid in the lobby!
     */
    static HashSet<Entity> lobbyUnload(Game game, Collection<Entity> entities) {
        HashSet<Entity> result = new HashSet<>();
        for (Entity entity: entities) {
            result.addAll(lobbyDisembark(game, entity));
            for (Entity carriedUnit: entity.getLoadedUnits()) {
                result.addAll(lobbyDisembark(game, carriedUnit));
            } 
        }
        return result;
    }
    
    /** 
     * Disembarks and offloads the given entity (removing it from transports
     * and removing transported units from it) unless the transport or carried unit
     * is also part of the given entities.
     * Returns a set of entities that received changes. The set may be empty, but not null.
     * <P>NOTE: This is a simplified unload that is only valid in the lobby!
     */
    static HashSet<Entity> lobbyUnloadOthers(Game game, Collection<Entity> entities) {
        HashSet<Entity> result = new HashSet<>();
        for (Entity entity: entities) {
            result.addAll(lobbyDisembarkOthers(game, entity, entities));
            for (Entity carriedUnit: entity.getLoadedUnits()) {
                result.addAll(lobbyDisembarkOthers(game, carriedUnit, entities));
            } 
        }
        return result;
    }
    
    /** 
     * Have the given entity disembark if it is carried by another unit.
     * Returns a set of entities that were modified. The set is empty if
     * the entity was not loaded to a carrier. 
     * <P>NOTE: This is a simplified disembark that is only valid in the lobby!
     */
    private static HashSet<Entity> lobbyDisembark(Game game, Entity entity) {
        return lobbyDisembarkOthers(game, entity, new ArrayList<>());
    }
    
    /** 
     * Have the given entity disembark if it is carried by another unit.
     * Returns a set of entities that were modified. The set is empty if
     * the entity was not loaded to a carrier. 
     * <P>NOTE: This is a simplified disembark that is only valid in the lobby!
     */
    private static HashSet<Entity> lobbyDisembarkOthers(Game game, Entity entity, Collection<Entity> entities) {
        HashSet<Entity> result = new HashSet<>();
        if (entity.getTransportId() != Entity.NONE) {
            Entity carrier = game.getEntity(entity.getTransportId());
            if (carrier != null && !entities.contains(carrier)) {
                carrier.unload(entity);
                result.add(entity);
                result.add(carrier);
                entity.setTransportId(Entity.NONE);
            }
        }
        return result;
    }
    
    /** 
     * Have the given entity disembark if it is carried by an enemy unit.
     * <P>NOTE: This is a simplified disembark that is only valid in the lobby!
     */
    private static void lobbyDisembarkEnemy(Game game, Entity entity) {
        if (entity.getTransportId() != Entity.NONE) {
            Entity carrier = game.getEntity(entity.getTransportId());
            if (carrier == null) {
                entity.setTransportId(Entity.NONE);
            } else if (carrier.getOwner().isEnemyOf(entity.getOwner())) {
                carrier.unload(entity);
                entity.setTransportId(Entity.NONE);
            }
        }
    }
    
    /** 
     * Performs a disconnect from C3 networks for the given entities. 
     * This is a simplified version that is only valid in the lobby.
     * Returns a set of entities that received changes.
     */
    static HashSet<Entity> performC3Disconnect(Game game, Collection<Entity> entities) {
        HashSet<Entity> result = new HashSet<>();
        // Disconnect the entity from networks
        for (Entity entity: entities) {
            if (entity.hasNhC3()) {
                entity.setC3NetIdSelf();
                result.add(entity);
            } else if (entity.hasAnyC3System()) {
                entity.setC3Master(null, true);
                result.add(entity);
            }
        }
        // Also disconnect all units connected *to* that entity
        for (Entity entity: game.getEntitiesVector()) {
            if (entities.contains(entity.getC3Master())) {
                entity.setC3Master(null, true);
                result.add(entity);
            }
        }
        return result;
    }
    
    /**
     * Creates a packet for an update for the given entities. 
     * Only valid in the lobby.
     */
    static Packet createMultiEntityPacket(Collection<Entity> entities) {
        return new Packet(PacketCommand.ENTITY_MULTIUPDATE, entities);
    }
    
    /**
     * Creates a packet detailing a force delete.
     * Only valid in the lobby.
     */
    static Packet createForcesDeletePacket(Collection<Integer> forces) {
        return new Packet(PacketCommand.FORCE_DELETE, forces);
    }

    /** 
     * Handles a force parent packet, attaching the sent forces to a new parent or 
     * making the sent forces top-level. 
     * This method is intended for use in the lobby!
     */
    static void receiveForceParent(Packet c, int connId, Game game, GameManager gameManager) {
        @SuppressWarnings("unchecked")
        var forceList = (Collection<Force>) c.getObject(0);
        int newParentId = (int) c.getObject(1);
        
        var forces = game.getForces();
        var changedForces = new HashSet<Force>();
        
        if (newParentId == Force.NO_FORCE) {
            forceList.stream().forEach(f -> changedForces.addAll(forces.promoteForce(forces.getForce(f.getId()))));
        } else {
            if (!forces.contains(newParentId)) {
                LogManager.getLogger().warn("Tried to attach forces to non-existing parent force ID " + newParentId);
                return;
            }
            Force newParent = forces.getForce(newParentId);
            forceList.stream().forEach(f -> changedForces.addAll(forces.attachForce(forces.getForce(f.getId()), newParent)));
        }
        
        if (!changedForces.isEmpty()) {
            gameManager.send(createForceUpdatePacket(changedForces));
        }
    }
    
    /** 
     * Handles a force assign full packet, changing the owner of forces and everything in them.
     * This method is intended for use in the lobby!
     */
    static void receiveEntitiesAssign(Packet c, int connId, Game game, GameManager gameManager) {
        @SuppressWarnings("unchecked")
        var entityList = (Collection<Entity>) c.getObject(0);
        int newOwnerId = (int) c.getObject(1);
        Player newOwner = game.getPlayer(newOwnerId);

        if (entityList.isEmpty() || newOwner == null) {
            return;
        }

        // Get the local (server) entities
        var serverEntities =  new HashSet<Entity>();
        entityList.stream().map(e -> game.getEntity(e.getId())).forEach(serverEntities::add);
        for (Entity entity: serverEntities) {
            entity.setOwner(newOwner);
        }
        game.getForces().correct();
        correctLoading(game);
        correctC3Connections(game);
        gameManager.send(gameManager.createFullEntitiesPacket());
    }
    
    /** 
     * Handles a force assign full packet, changing the owner of forces and everything in them.
     * This method is intended for use in the lobby!
     */
    static void receiveForceAssignFull(Packet c, int connId, Game game, GameManager gameManager) {
        @SuppressWarnings("unchecked")
        var forceList = (Collection<Force>) c.getObject(0);
        int newOwnerId = (int) c.getObject(1);
        Player newOwner = game.getPlayer(newOwnerId);

        if (forceList.isEmpty() || newOwner == null) {
            return;
        }
        
        var forces = game.getForces();
        // Get the local (server) forces
        var serverForces = new HashSet<Force>();
        forceList.stream().map(f -> forces.getForce(f.getId())).forEach(serverForces::add);
        // Remove redundant forces (subforces of others in the list)
        Set<Force> allSubForces = new HashSet<>();
        serverForces.stream().map(forces::getFullSubForces).forEach(allSubForces::addAll);
        serverForces.removeIf(allSubForces::contains);

        for (Force force: serverForces) {
            Collection<Entity> entities = ForceAssignable.filterToEntityList(forces.getFullEntities(force));
            forces.assignFullForces(force, newOwner);
            for (Entity entity: entities) {
                entity.setOwner(newOwner);
            }
        }
        forces.correct();
        correctLoading(game);
        correctC3Connections(game);
        gameManager.send(gameManager.createFullEntitiesPacket());
    }
    
    /** 
     * Handles a force update packet, forwarding a client-side change that 
     * only affects forces, not entities:
     * - rename
     * - move subforce/entity up/down (this does not change the entity, only the force)
     * - owner change of only the force (not the entities, only within a team) 
     * This method is intended for use in the lobby!
     */
    static void receiveForceUpdate(Packet c, int connId, Game game, GameManager gameManager) {
        @SuppressWarnings("unchecked")
        var forceList = (Collection<Force>) c.getObject(0);
        
        // Check if the updated Forces are valid
        Forces forcesClone = game.getForces().clone();
        for (Force force: forceList) {
            forcesClone.replace(force.getId(), force);
        }
        if (forcesClone.isValid()) {
            game.setForces(forcesClone);
            gameManager.send(createForceUpdatePacket(forceList));
        } else {
            LogManager.getLogger().warn("Invalid forces update received.");
            gameManager.send(gameManager.createFullEntitiesPacket());
        }
    }
    
    /** 
     * Handles a team change, updating units and forces as necessary.
     * This method is intended for use in the lobby!
     */
    static void receiveLobbyTeamChange(Packet c, int connId, Game game, GameManager gameManager) {
        @SuppressWarnings("unchecked")
        var players = (Collection<Player>) c.getObject(0);
        var newTeam = (int) c.getObject(1);
        
        // Collect server-side player objects
        Set<Player> serverPlayers = new HashSet<>();
        players.stream().map(p -> game.getPlayer(p.getId())).forEach(serverPlayers::add);
        
        // Check parameters and if there's an actual change to a player
        serverPlayers.removeIf(p -> p == null || p.getTeam() == newTeam);
        if (serverPlayers.isEmpty() || newTeam < 0 || newTeam > 5) {
            return;
        }
        
        // First, change all teams, then correct all connections (load, C3, force)
        for (Player player : serverPlayers) {
            player.setTeam(newTeam);
        }
        Forces forces = game.getForces();
        forces.correct();
        correctLoading(game);
        correctC3Connections(game);
        
        gameManager.send(gameManager.createFullEntitiesPacket());
        for (Player player: serverPlayers) {
            gameManager.transmitPlayerUpdate(player);
        }
    }

    /** 
     * For all game units, disembarks from carriers and offloads carried units
     * if they are enemies.  
     * <P>NOTE: This is a simplified unload that is only valid in the lobby!
     */
    static void correctLoading(Game game) {
        for (Entity entity: game.getEntitiesVector()) {
            lobbyDisembarkEnemy(game, entity);
            for (Entity carriedUnit: entity.getLoadedUnits()) {
                lobbyDisembarkEnemy(game, carriedUnit);
            } 
        }
    }

    /** 
     * For all game units, disconnects from enemy C3 masters / networks
     * <P>NOTE: This is intended for use in the lobby phase!
     */
    static void correctC3Connections(Game game) {
        for (Entity entity: game.getEntitiesVector()) {
            if (entity.hasNhC3()) {
                String net = entity.getC3NetId();
                int id = Entity.NONE;
                try {
                    id = Integer.parseInt(net.substring(net.indexOf(".") + 1));
                    if (game.getEntity(id).getOwner().isEnemyOf(entity.getOwner())) {
                        entity.setC3NetIdSelf();
                    }
                } catch (Exception ignored) {
                }
            } else if (entity.hasAnyC3System()) {
                if ((entity.getC3Master() != null)
                        && entity.getC3Master().getOwner().isEnemyOf(entity.getOwner())) {
                    entity.setC3Master(null, true);
                }
            }
        }
    }

    /** 
     * Handles an add entity to force / remove from force packet, attaching the 
     * sent entities to a force or removing them from any force. 
     * This method is intended for use in the lobby!
     */
    static void receiveAddEntititesToForce(Packet c, int connId, Game game, GameManager gameManager) {
        @SuppressWarnings("unchecked")
        var entityList = (Collection<Entity>) c.getObject(0);
        var forceId = (int) c.getObject(1);
        // Get the local (server) entities
        List<Entity> entities = entityList.stream().map(e -> game.getEntity(e.getId())).collect(toList());
        var forces = game.getForces();

        var changedEntities = new HashSet<Entity>();
        var changedForces = new HashSet<Force>();
        // Check if the entities are teammembers of the force, unless the entities are removed from forces
        if (forceId != Force.NO_FORCE) {
            var forceOwner = forces.getOwner(forceId);
            if (entities.stream().anyMatch(e -> e.getOwner().isEnemyOf(forceOwner))) {
                LogManager.getLogger().warn("Tried to add entities to an enemy force.");
                return;
            }
            for (Entity entity: entities) {
                List<Force> result = forces.addEntity(entity, forceId);
                if (!result.isEmpty()) {
                    changedForces.addAll(result);
                    changedEntities.add(entity);
                }
            }
        } else {
            changedForces.addAll(game.getForces().removeEntityFromForces(entities));
            changedEntities.addAll(entities);
        }
        gameManager.send(createForceUpdatePacket(changedForces, changedEntities));
    }
    
    /**
     * Adds a force with the info from the client. Only valid during the lobby phase.
     */
    static void receiveForceAdd(Packet c, int connId, Game game, GameManager gameManager) {
        var force = (Force) c.getObject(0);
        @SuppressWarnings("unchecked")
        var entities = (Collection<Entity>) c.getObject(1);

        int newId;
        if (force.isTopLevel()) {
            newId = game.getForces().addTopLevelForce(force, game.getPlayer(force.getOwnerId()));
        } else {
            Force parent = game.getForces().getForce(force.getParentId()); 
            newId = game.getForces().addSubForce(force, parent);
        }
        for (var entity: entities) {
            game.getForces().addEntity(game.getEntity(entity.getId()), newId);
        }
        gameManager.send(gameManager.createFullEntitiesPacket());
    }

    /**
     * Creates a packet detailing a force update. Force updates must contain all
     * affected forces and all affected entities.
     */
    static Packet createForceUpdatePacket(Collection<Force> changedForces) {
        return createForceUpdatePacket(changedForces, new ArrayList<>());
    }

    /**
     * Creates a packet detailing a force update. Force updates must contain all
     * affected forces and all affected entities. Entities are only affected if their
     * forceId changed.
     */
    static Packet createForceUpdatePacket(Collection<Force> changedForces, Collection<Entity> entities) {
        return new Packet(PacketCommand.FORCE_UPDATE, changedForces, entities);
    }

    /**
     * A force is editable to the sender of a command if any forces in its force chain
     * (this includes the force itself) is owned by the sender. This allows editing 
     * forces of other players if they are a subforce of one's own force.
     * See also LobbyActions.isEditable(Force)
     */
    static boolean isEditable(Force force, Game game, Player sender) {
        List<Force> chain = game.getForces().forceChain(force);
        return chain.stream().map(f -> game.getForces().getOwner(f)).anyMatch(p -> p.equals(sender));
    }
}
