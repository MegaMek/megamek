package megamek.server.receiver;

import megamek.common.Entity;
import megamek.common.EntitySelector;
import megamek.common.IEntityRemovalConditions;
import megamek.common.Protomech;
import megamek.common.enums.GamePhase;
import megamek.common.force.Force;
import megamek.common.net.Packet;
import megamek.server.Server;
import megamek.server.ServerHelper;
import megamek.server.ServerLobbyHelper;

import java.util.*;

import static java.lang.Math.ceil;

public class ReceiveEntityDeleter {
    /**
     * Deletes an entity owned by a certain player from the list
     */
    public static void receiveEntityDelete(Server server, Packet c, int connIndex) {
        @SuppressWarnings("unchecked") List<Integer> ids = (List<Integer>) c.getObject(0);

        Set<Entity> delEntities = new HashSet<>();
        ids.stream().map(id -> server.getGame().getEntity(id)).forEach(delEntities::add);

        // Unload units and disconnect any C3 networks
        Set<Entity> updateCandidates = new HashSet<>();
        updateCandidates.addAll(ServerLobbyHelper.lobbyUnload(server.getGame(), delEntities));
        updateCandidates.addAll(ServerLobbyHelper.performC3Disconnect(server.getGame(), delEntities));

        // Units that get deleted must not receive updates
        updateCandidates.removeIf(delEntities::contains);
        if (!updateCandidates.isEmpty()) {
            server.send(ServerLobbyHelper.createMultiEntityPacket(updateCandidates));
        }

        ArrayList<Force> affectedForces = new ArrayList<>();
        for (Integer entityId : ids) {
            final Entity entity = server.getGame().getEntity(entityId);

            // Players can delete units of their teammates
            if ((entity != null) && (!entity.getOwner().isEnemyOf(server.getPlayer(connIndex)))) {

                affectedForces.addAll(server.getGame().getForces().removeEntityFromForces(entity));

                // If we're deleting a ProtoMech, recalculate unit numbers.
                if (entity instanceof Protomech) {

                    // How many ProtoMechs does the player have (include this one)?
                    int numPlayerProtos = server.getGame().getSelectedEntityCount(new EntitySelector() {
                        private final int ownerId = entity.getOwnerId();

                        @Override
                        public boolean accept(Entity entity) {
                            return (entity instanceof Protomech) && (ownerId == entity.getOwnerId());
                        }
                    });

                    // According to page 54 of the BMRr, ProtoMechs must be
                    // deployed in full Points of five, unless "losses" have
                    // reduced the number to less than that.
                    final char oldMax = (char) (ceil(numPlayerProtos / 5.0) - 1);
                    char newMax = (char) (ceil((numPlayerProtos - 1) / 5.0) - 1);
                    short deletedUnitNum = entity.getUnitNumber();

                    // Do we have to update a ProtoMech from the last unit?
                    if ((oldMax != deletedUnitNum) && (oldMax != newMax)) {

                        // Yup. Find a ProtoMech from the last unit, and
                        // set it's unit number to the deleted entity.
                        Iterator<Entity> lastUnit =
                                server.getGame().getSelectedEntities(new EntitySelector() {
                                    private final int ownerId = entity.getOwnerId();

                                    private final char lastUnitNum = oldMax;

                                    @Override
                                    public boolean accept(Entity entity) {
                                        return (entity instanceof Protomech)
                                                && (ownerId == entity.getOwnerId())
                                                && (lastUnitNum == entity.getUnitNumber());
                                    }
                                });
                        Entity lastUnitMember = lastUnit.next();
                        lastUnitMember.setUnitNumber(deletedUnitNum);
                        server.entityUpdate(lastUnitMember.getId());
                    } // End update-unit-number
                } // End added-ProtoMech

                if (server.getGame().getPhase() != GamePhase.DEPLOYMENT) {
                    // if a unit is removed during deployment just keep going
                    // without adjusting the turn vector.
                    server.getGame().removeTurnFor(entity);
                    server.getGame().removeEntity(entityId, IEntityRemovalConditions.REMOVE_NEVER_JOINED);
                }

                if (!server.getGame().getPhase().isLounge()) {
                    ServerHelper.clearBloodStalkers(server.getGame(), entityId, server);
                }
            }
        }

        // during deployment this absolutely must be called before game.removeEntity(), otherwise the game hangs
        // when a unit is removed. Cause unknown.
        server.send(server.createRemoveEntityPacket(ids, affectedForces, IEntityRemovalConditions.REMOVE_NEVER_JOINED));

        // Prevents deployment hanging. Only do this during deployment.
        if (server.getGame().getPhase() == GamePhase.DEPLOYMENT) {
            for (Integer entityId : ids) {
                final Entity entity = server.getGame().getEntity(entityId);
                server.getGame().removeEntity(entityId, IEntityRemovalConditions.REMOVE_NEVER_JOINED);
                server.endCurrentTurn(entity);
            }
        }
    }
}
