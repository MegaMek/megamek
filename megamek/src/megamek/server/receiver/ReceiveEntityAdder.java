package megamek.server.receiver;

import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.net.Packet;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.*;
import megamek.server.Processor.MovementProcessor;
import megamek.server.Server;
import megamek.server.ServerBoardHelper;
import megamek.server.ServerHelper;
import megamek.server.ServerLobbyHelper;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.ceil;

public class ReceiveEntityAdder {
    /**
     * Checks if an entity added by the client is valid and if so, adds it to
     * the list
     *
     * @param server
     * @param c the packet to be processed
     * @param connIndex the id for connection that received the packet.
     */
    public static void receiveEntityAdd(Server server, Packet c, int connIndex) {
        @SuppressWarnings("unchecked")
        final List<Entity> entities = (List<Entity>) c.getObject(0);
        List<Integer> entityIds = new ArrayList<>(entities.size());
        // Map client-received to server-given IDs:
        Map<Integer, Integer> idMap = new HashMap<>();
        // Map MUL force ids to real Server-given force ids;
        Map<Integer, Integer> forceMapping = new HashMap<>();

        // Need to use a new ArrayLiut to prevent a concurrent modification exception when removing
        // illegal entities
        for (final Entity entity : new ArrayList<>(entities)) {
            // Verify the entity's design
            if (MovementProcessor.entityVerifier == null) {
                MovementProcessor.entityVerifier = EntityVerifier.getInstance(new MegaMekFile(
                        Configuration.unitsDir(), EntityVerifier.CONFIG_FILENAME).getFile());
            }

            // Create a TestEntity instance for supported unit types
            TestEntity testEntity = null;
            entity.restore();
            if (entity instanceof Mech) {
                testEntity = new TestMech((Mech) entity, MovementProcessor.entityVerifier.mechOption, null);
            } else if ((entity.getEntityType() == Entity.ETYPE_TANK)
                       && (entity.getEntityType() != Entity.ETYPE_GUN_EMPLACEMENT)) {
                if (entity.isSupportVehicle()) {
                    testEntity = new TestSupportVehicle(entity, MovementProcessor.entityVerifier.tankOption, null);
                } else {
                    testEntity = new TestTank((Tank) entity, MovementProcessor.entityVerifier.tankOption, null);
                }
            } else if ((entity.getEntityType() == Entity.ETYPE_AERO)
                       && (entity.getEntityType() != Entity.ETYPE_DROPSHIP)
                       && (entity.getEntityType() != Entity.ETYPE_SMALL_CRAFT)
                       && (entity.getEntityType() != Entity.ETYPE_FIGHTER_SQUADRON)
                       && (entity.getEntityType() != Entity.ETYPE_JUMPSHIP)
                       && (entity.getEntityType() != Entity.ETYPE_SPACE_STATION)) {
                testEntity = new TestAero((Aero) entity, MovementProcessor.entityVerifier.aeroOption, null);
            } else if (entity instanceof BattleArmor) {
                testEntity = new TestBattleArmor((BattleArmor) entity, MovementProcessor.entityVerifier.baOption, null);
            }

            if (testEntity != null) {
                StringBuffer sb = new StringBuffer();
                if (testEntity.correctEntity(sb, TechConstants.getGameTechLevel(server.getGame(), entity.isClan()))) {
                    entity.setDesignValid(true);
                } else {
                    LogManager.getLogger().error(sb.toString());
                    if (server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_ALLOW_ILLEGAL_UNITS)) {
                        entity.setDesignValid(false);
                    } else {
                        Player cheater = server.getGame().getPlayer(connIndex);
                        server.sendServerChat(String.format(
                                "Player %s attempted to add an illegal unit design (%s), the unit was rejected.",
                                cheater.getName(), entity.getShortNameRaw()));
                        entities.remove(entity);
                        continue;
                    }
                }
            }

            // If we're adding a ProtoMech, calculate it's unit number.
            if (entity instanceof Protomech) {
                // How many ProtoMechs does the player already have?
                int numPlayerProtos = server.getGame().getSelectedEntityCount(new EntitySelector() {
                    private final int ownerId = entity.getOwnerId();

                    @Override
                    public boolean accept(Entity entity) {
                        return (entity instanceof Protomech) && (ownerId == entity.getOwnerId());
                    }
                });

                // According to page 54 of the BMRr, ProtoMechs must be
                // deployed in full Points of five, unless circumstances have
                // reduced the number to less than that.
                entity.setUnitNumber((short) (numPlayerProtos / 5));
            }

            // Only assign an entity ID when the client hasn't.
            if (Entity.NONE == entity.getId()) {
                entity.setId(server.getFreeEntityId());
            }

            int clientSideId = entity.getId();
            server.getGame().addEntity(entity);

            // Remember which received ID corresponds to which actual ID
            idMap.put(clientSideId, entity.getId());

            // Now we relink C3/NC3/C3i to our guys! Yes, this is hackish... but, we
            // do what we must. Its just too bad we have to loop over the entire entities array..
            if (entity.hasC3() || entity.hasC3i() || entity.hasNavalC3()) {
                boolean C3iSet = false;

                for (Entity e : server.getGame().getEntitiesVector()) {

                    // C3 Checks
                    if (entity.hasC3()) {
                        if ((entity.getC3MasterIsUUIDAsString() != null)
                                && entity.getC3MasterIsUUIDAsString().equals(e.getC3UUIDAsString())) {
                            entity.setC3Master(e, false);
                            entity.setC3MasterIsUUIDAsString(null);
                        } else if ((e.getC3MasterIsUUIDAsString() != null)
                                && e.getC3MasterIsUUIDAsString().equals(entity.getC3UUIDAsString())) {
                            e.setC3Master(entity, false);
                            e.setC3MasterIsUUIDAsString(null);
                            // Taharqa: we need to update the other entity for
                            // the
                            // client
                            // or it won't show up right. I am not sure if I
                            // like
                            // the idea of updating other entities in this
                            // method,
                            // but it
                            // will work for now.
                            if (!entities.contains(e)) {
                                server.entityUpdate(e.getId());
                            }
                        }
                    }

                    // C3i Checks
                    if (entity.hasC3i() && !C3iSet) {
                        entity.setC3NetIdSelf();
                        int pos = 0;
                        while (pos < Entity.MAX_C3i_NODES) {
                            // We've found a network, join it.
                            if ((entity.getC3iNextUUIDAsString(pos) != null)
                                    && (e.getC3UUIDAsString() != null)
                                    && entity.getC3iNextUUIDAsString(pos)
                                    .equals(e.getC3UUIDAsString())) {
                                entity.setC3NetId(e);
                                C3iSet = true;
                                break;
                            }

                            pos++;
                        }
                    }

                    // NC3 Checks
                    if (entity.hasNavalC3() && !C3iSet) {
                        entity.setC3NetIdSelf();
                        int pos = 0;
                        while (pos < Entity.MAX_C3i_NODES) {
                            // We've found a network, join it.
                            if ((entity.getNC3NextUUIDAsString(pos) != null)
                                    && (e.getC3UUIDAsString() != null)
                                    && entity.getNC3NextUUIDAsString(pos)
                                    .equals(e.getC3UUIDAsString())) {
                                entity.setC3NetId(e);
                                C3iSet = true;
                                break;
                            }

                            pos++;
                        }
                    }
                }
            }
            // Give the unit a spotlight, if it has the spotlight quirk
            entity.setExternalSearchlight(entity.hasExternalSearchlight()
                    || entity.hasQuirk(OptionsConstants.QUIRK_POS_SEARCHLIGHT));
            entityIds.add(entity.getId());

            if (server.getGame().getPhase() != GamePhase.LOUNGE) {
                entity.getOwner().changeInitialEntityCount(1);
                entity.getOwner().changeInitialBV(entity.calculateBattleValue());
            }

            // Restore forces from MULs or other external sources from the forceString, if any
            if (entity.getForceString().length() > 0) {
                List<Force> forceList = Forces.parseForceString(entity);
                int realId = Force.NO_FORCE;
                boolean topLevel = true;

                for (Force force: forceList) {
                    if (!forceMapping.containsKey(force.getId())) {
                        if (topLevel) {
                            realId = server.getGame().getForces().addTopLevelForce(force, entity.getOwner());
                        } else {
                            Force parent = server.getGame().getForces().getForce(realId);
                            realId = server.getGame().getForces().addSubForce(force, parent);
                        }
                        forceMapping.put(force.getId(), realId);
                    } else {
                        realId = forceMapping.get(force.getId());
                    }
                    topLevel = false;
                }
                entity.setForceString("");
                server.getGame().getForces().addEntity(entity, realId);
            }
        }

        // Cycle through the entities again and update any carried units
        // and carrier units to use the correct server-given IDs.
        // Typically necessary when loading a MUL containing transported units.

        // First, deal with units loaded into bays. These are saved for the carrier
        // in MULs and must be restored exactly to recreate the bay loading.
        Set<Entity> transportCorrected = new HashSet<>();
        for (final Entity carrier : entities) {
            for (int carriedId : carrier.getBayLoadedUnitIds()) {
                // First, see if a bay loaded unit can be found and unloaded,
                // because it might be the wrong unit
                Entity carried = server.getGame().getEntity(carriedId);
                if (carried == null) {
                    continue;
                }
                int bay = carrier.getBay(carried).getBayNumber();
                carrier.unload(carried);
                // Now, load the correct unit if there is one
                if (idMap.containsKey(carriedId)) {
                    Entity newCarried = server.getGame().getEntity(idMap.get(carriedId));
                    if (carrier.canLoad(newCarried, false)) {
                        carrier.load(newCarried, false, bay);
                        newCarried.setTransportId(carrier.getId());
                        // Remember that the carried unit should not be treated again below
                        transportCorrected.add(newCarried);
                    }
                }
            }
        }

        // Now restore the transport settings from the entities' transporter IDs
        // With anything other than bays, MULs only show the carrier, not the carried units
        for (final Entity entity : entities) {
            // Don't correct those that are already corrected
            if (transportCorrected.contains(entity)) {
                continue;
            }
            // Get the original (client side) ID of the transporter
            int origTrsp = entity.getTransportId();
            // Only act if the unit thinks it is transported
            if (origTrsp != Entity.NONE) {
                // If the transporter is among the new units, go on with loading
                if (idMap.containsKey(origTrsp)) {
                    // The wrong transporter doesn't know of anything and does not need an update
                    Entity carrier = server.getGame().getEntity(idMap.get(origTrsp));
                    if (carrier.canLoad(entity, false)) {
                        // The correct transporter must be told it's carrying something and
                        // the carried unit must be told where it is embarked
                        carrier.load(entity, false);
                        entity.setTransportId(idMap.get(origTrsp));
                    } else {
                        // This seems to be an invalid carrier; update the entity accordingly
                        entity.setTransportId(Entity.NONE);
                    }
                } else {
                    // this transporter does not exist; update the entity accordingly
                    entity.setTransportId(Entity.NONE);
                }
            }
        }

        // Set the "loaded keepers" which is apparently used for deployment unloading to
        // differentiate between units loaded in the lobby and other carried units
        // When entering a game from the lobby, this list is generated again, but not when
        // the added entities are loaded during a game. When getting loaded units from a MUL,
        // act as if they were loaded in the lobby.
        for (final Entity entity : entities) {
            if (!entity.getLoadedUnits().isEmpty()) {
                Vector<Integer> v = entity.getLoadedUnits().stream().map(Entity::getId).collect(Collectors.toCollection(Vector::new));
                entity.setLoadedKeepers(v);
            }
        }

        List<Integer> changedForces = new ArrayList<>(forceMapping.values());

        server.send(server.createAddEntityPacket(entityIds, changedForces));
    }

}
