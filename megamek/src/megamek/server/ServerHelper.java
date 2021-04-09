package megamek.server;

import java.io.File;
import java.util.*;
import megamek.MegaMek;
import megamek.client.ui.swing.lobby.LobbyActions;
import megamek.common.*;
import megamek.common.force.*;
import megamek.common.net.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.util.StringUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.weapons.other.TSEMPWeapon;

import static java.util.stream.Collectors.*;

/**
 * This class contains computations carried out by the Server class.
 * Methods put in here should be static and self-contained. 
 * @author NickAragua
 *
 */
public class ServerHelper {
    /**
     * Determines if the given entity is an infantry unit in the given hex is "in the open" 
     * (and thus subject to double damage from attacks)
     * @param te Target entity.
     * @param te_hex Hex where target entity is located.
     * @param game Game being played.
     * @param isPlatoon Whether the target unit is a platoon.
     * @param ammoExplosion Whether we're considering a "big boom" ammo explosion from tacops.
     * @param ignoreInfantryDoubleDamage Whether we should ignore double damage to infantry.
     * @return Whether the infantry unit can be considered to be "in the open"
     */
    public static boolean infantryInOpen(Entity te, IHex te_hex, IGame game, 
            boolean isPlatoon, boolean ammoExplosion, boolean ignoreInfantryDoubleDamage) {
        
        if (isPlatoon && !te.isDestroyed() && !te.isDoomed() && !ignoreInfantryDoubleDamage
                && (((Infantry) te).getDugIn() != Infantry.DUG_IN_COMPLETE)) {
        	
        	if(te_hex == null) {
        		te_hex = game.getBoard().getHex(te.getPosition());
        	}
        	
            if ((te_hex != null) && !te_hex.containsTerrain(Terrains.WOODS) && !te_hex.containsTerrain(Terrains.JUNGLE)
                    && !te_hex.containsTerrain(Terrains.ROUGH) && !te_hex.containsTerrain(Terrains.RUBBLE)
                    && !te_hex.containsTerrain(Terrains.SWAMP) && !te_hex.containsTerrain(Terrains.BUILDING)
                    && !te_hex.containsTerrain(Terrains.FUEL_TANK) && !te_hex.containsTerrain(Terrains.FORTIFIED)
                    && (!te.hasAbility(OptionsConstants.INFANTRY_URBAN_GUERRILLA))
                    && (!te_hex.containsTerrain(Terrains.PAVEMENT) || !te_hex.containsTerrain(Terrains.ROAD))
                    && !ammoExplosion) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Worker function that handles heat as applied to aerospace fighter
     */
    public static void resolveAeroHeat(IGame game, Entity entity, Vector<Report> vPhaseReport, Vector<Report> rhsReports, 
            int radicalHSBonus, int hotDogMod, Server s) {
        Report r;
        
        // If this aero is part of a squadron, we will deal with its
        // heat with the fighter squadron
        if (game.getEntity(entity.getTransportId()) instanceof FighterSquadron) {
            return;
        }

        // should we even bother?
        if (entity.isDestroyed() || entity.isDoomed()
            || entity.getCrew().isDoomed()
            || entity.getCrew().isDead()) {
            return;
        }

        // engine hits add a lot of heat, provided the engine is on
        entity.heatBuildup += entity.getEngineCritHeat();

        // If an Aero had an active Stealth suite, add 10 heat.
        if (entity.isStealthOn()) {
            entity.heatBuildup += 10;
            r = new Report(5015);
            r.subject = entity.getId();
            vPhaseReport.add(r);
        }

        // Combat computers help manage heat
        if (entity.hasQuirk(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER)) {
            int reduce = Math.min(entity.heatBuildup, 4);
            r = new Report(5026);
            r.subject = entity.getId();
            r.add(reduce);
            vPhaseReport.add(r);
            entity.heatBuildup -= reduce;
        }

        // Add heat from external sources to the heat buildup
        int max_ext_heat = game.getOptions().intOption(
                OptionsConstants.ADVCOMBAT_MAX_EXTERNAL_HEAT); // Check Game Options
        if (max_ext_heat < 0) {
            max_ext_heat = 15; // standard value specified in TW p.159
        }
        entity.heatBuildup += Math.min(max_ext_heat, entity.heatFromExternal);
        entity.heatFromExternal = 0;
        // remove heat we cooled down
        entity.heatBuildup -= Math.min(9, entity.coolFromExternal);
        entity.coolFromExternal = 0;

        // add the heat we've built up so far.
        entity.heat += entity.heatBuildup;

        // how much heat can we sink?
        int tosink = entity.getHeatCapacityWithWater() + radicalHSBonus;

        // should we use a coolant pod?
        int safeHeat = entity.hasInfernoAmmo() ? 9 : 13;
        int possibleSinkage = ((Aero) entity).getHeatSinks()
                - entity.getCoolantFailureAmount();
        for (Mounted m : entity.getEquipment()) {
            if (m.getType() instanceof AmmoType) {
                AmmoType at = (AmmoType) m.getType();
                if ((at.getAmmoType() == AmmoType.T_COOLANT_POD) && m.isAmmoUsable()) {
                    EquipmentMode mode = m.curMode();
                    if (mode.equals("dump")) {
                        r = new Report(5260);
                        r.subject = entity.getId();
                        vPhaseReport.add(r);
                        m.setShotsLeft(0);
                        tosink += possibleSinkage;
                        break;
                    }
                    if (mode.equals("safe") && ((entity.heat - tosink) > safeHeat)) {
                        r = new Report(5265);
                        r.subject = entity.getId();
                        vPhaseReport.add(r);
                        m.setShotsLeft(0);
                        tosink += possibleSinkage;
                        break;
                    }
                    if (mode.equals("efficient")
                            && ((entity.heat - tosink) >= possibleSinkage)) {
                        r = new Report(5270);
                        r.subject = entity.getId();
                        vPhaseReport.add(r);
                        m.setShotsLeft(0);
                        tosink += possibleSinkage;
                        break;
                    }
                }
            }
        }

        tosink = Math.min(tosink, entity.heat);
        entity.heat -= tosink;
        r = new Report(5035);
        r.subject = entity.getId();
        r.addDesc(entity);
        r.add(entity.heatBuildup);
        r.add(tosink);
        r.add(entity.heat);
        vPhaseReport.add(r);
        entity.heatBuildup = 0;
        vPhaseReport.addAll(rhsReports);

        // add in the effects of heat

        if ((entity instanceof Dropship) || (entity instanceof Jumpship)) {
            // only check for a possible control roll
            if (entity.heat > 0) {
                int bonus = (int) Math.ceil(entity.heat / 100.0);
                game.addControlRoll(new PilotingRollData(
                        entity.getId(), bonus, "used too much heat"));
                entity.heat = 0;
            }
            return;
        }

        // Capital fighters can overheat and require control rolls
        if (entity.isCapitalFighter() && (entity.heat > 0)) {
            int penalty = (int) Math.ceil(entity.heat / 15.0);
            game.addControlRoll(new PilotingRollData(entity.getId(),
                    penalty, "used too much heat"));
        }

        // Like other large craft, the rest of these rules don't apply
        // to capital fighters
        if (entity.isCapitalFighter()) {
            return;
        }

        int autoShutDownHeat = 30;
        boolean mtHeat = game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_HEAT);
        if (mtHeat) {
            autoShutDownHeat = 50;
        }

        // heat effects: start up
        if ((entity.heat < autoShutDownHeat) && entity.isShutDown()) {
            // only start up if not shut down by taser or a TSEMP
            if ((entity.getTaserShutdownRounds() == 0)
                    && (entity.getTsempEffect() != TSEMPWeapon.TSEMP_EFFECT_SHUTDOWN)) {
                if ((entity.heat < 14) && !entity.isManualShutdown()) {
                    // automatically starts up again
                    entity.setShutDown(false);
                    r = new Report(5045);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    vPhaseReport.add(r);
                } else if (!entity.isManualShutdown()) {
                    // If the pilot is KO and we need to roll, auto-fail.
                    if (!entity.getCrew().isActive()) {
                        r = new Report(5049);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                    } else {
                        // roll for startup
                        int startup = (4 + (((entity.heat - 14) / 4) * 2)) - hotDogMod;
                        if (mtHeat) {
                            startup -= 5;
                            switch (entity.getCrew().getPiloting()) {
                                case 0:
                                case 1:
                                    startup -= 2;
                                    break;
                                case 2:
                                case 3:
                                    startup -= 1;
                                    break;
                                case 6:
                                case 7:
                                    startup += 1;
                                    break;
                            }
                        }
                        int startupRoll = entity.getCrew().rollPilotingSkill();
                        r = new Report(5050);
                        r.subject = entity.getId();
                        r.addDesc(entity);
                        r.add(startup);
                        r.add(startupRoll);
                        if (startupRoll >= startup) {
                            // start 'er back up
                            entity.setShutDown(false);
                            r.choose(true);
                        } else {
                            r.choose(false);
                        }
                    }
                    vPhaseReport.add(r);
                }
            } else {
                // if we're shutdown by a BA taser, we might activate
                // again
                if (entity.isBATaserShutdown()) {
                    int roll = Compute.d6(2);
                    if (roll >= 8) {
                        entity.setTaserShutdownRounds(0);
                        if (!(game.getOptions().booleanOption(
                                OptionsConstants.RPG_MANUAL_SHUTDOWN)
                                && entity.isManualShutdown())) {
                            entity.setShutDown(false);
                        }
                        entity.setBATaserShutdown(false);
                    }
                }
            }
        }
        // heat effects: shutdown!
        else if ((entity.heat >= 14) && !entity.isShutDown()) {
            if (entity.heat >= autoShutDownHeat) {
                r = new Report(5055);
                r.subject = entity.getId();
                r.addDesc(entity);
                vPhaseReport.add(r);
                // okay, now mark shut down
                entity.setShutDown(true);
            } else {
                // Again, pilot KO means shutdown is automatic.
                if (!entity.getCrew().isActive()) {
                    r = new Report(5056);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    vPhaseReport.add(r);
                    entity.setShutDown(true);
                } else {
                    int shutdown = (4 + (((entity.heat - 14) / 4) * 2)) - hotDogMod;
                    if (mtHeat) {
                        shutdown -= 5;
                        switch (entity.getCrew().getPiloting()) {
                            case 0:
                            case 1:
                                shutdown -= 2;
                                break;
                            case 2:
                            case 3:
                                shutdown -= 1;
                                break;
                            case 6:
                            case 7:
                                shutdown += 1;
                                break;
                        }
                    }
                    int shutdownRoll = Compute.d6(2);
                    r = new Report(5060);
                    r.subject = entity.getId();
                    r.addDesc(entity);
                    r.add(shutdown);
                    r.add(shutdownRoll);
                    if (shutdownRoll >= shutdown) {
                        // avoided
                        r.choose(true);
                        vPhaseReport.add(r);
                    } else {
                        // shutting down...
                        r.choose(false);
                        vPhaseReport.add(r);
                        // okay, now mark shut down
                        entity.setShutDown(true);
                    }
                }
            }
        }

        s.checkRandomAeroMovement(entity, hotDogMod);

        // heat effects: ammo explosion!
        if (entity.heat >= 19) {
            int boom = (4 + (entity.heat >= 23 ? 2 : 0) + (entity.heat >= 28 ? 2 : 0))
                    - hotDogMod;
            if (mtHeat) {
                boom += (entity.heat >= 35 ? 2 : 0)
                        + (entity.heat >= 40 ? 2 : 0)
                        + (entity.heat >= 45 ? 2 : 0);
                // Last line is a crutch; 45 heat should be no roll
                // but automatic explosion.
            }
            r = new Report(5065);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(boom);
            
            int roll = Compute.d6(2);
            r.add(roll);
            
            if (roll >= boom) {
                // no ammo explosion
                r.choose(true);
                vPhaseReport.add(r);
            } else {
                // boom!
                r.choose(false);
                vPhaseReport.add(r);
                vPhaseReport.addAll(s.explodeAmmoFromHeat(entity));
            }
        }

        // heat effects: pilot damage
        if (entity.heat >= 21) {
            int ouch = (6 + (entity.heat >= 27 ? 3 : 0)) - hotDogMod;
            int ouchRoll = Compute.d6(2);
            r = new Report(5075);
            r.subject = entity.getId();
            r.addDesc(entity);
            r.add(ouch);
            r.add(ouchRoll);
            if (ouchRoll >= ouch) {
                // pilot is ok
                r.choose(true);
                vPhaseReport.add(r);
            } else {
                // pilot is hurting
                r.choose(false);
                vPhaseReport.add(r);
                vPhaseReport.addAll(s.damageCrew(entity, 1));
            }
        }

        // The pilot may have just expired.
        if ((entity.getCrew().isDead() || entity.getCrew().isDoomed())
                && !entity.getCrew().isEjected()) {
            r = new Report(5080);
            r.subject = entity.getId();
            r.addDesc(entity);
            vPhaseReport.add(r);
            vPhaseReport.addAll(s.destroyEntity(entity, "pilot death", true));
        }
    }
    
    /**
     * Helper function that causes an entity to sink to the bottom of the water
     * hex it's currently in.
     */
    public static void sinkToBottom(Entity entity) {
        if((entity == null) || !entity.getGame().getBoard().contains(entity.getPosition())) {
            return;
        }
        
        IHex fallHex = entity.getGame().getBoard().getHex(entity.getPosition());
        int waterDepth = 0;
        
        // we're going hull down, we still sink to the bottom if appropriate
        if (fallHex.containsTerrain(Terrains.WATER)) {
            boolean hexHasBridge = fallHex.containsTerrain(Terrains.BRIDGE_CF);
            boolean entityOnTopOfBridge = hexHasBridge && (entity.getElevation() == fallHex.ceiling());
            
            if (!entityOnTopOfBridge) {
                // *Only* use this if there actually is water in the hex, otherwise
                // we get ITerrain.LEVEL_NONE, i.e. Integer.minValue...
                waterDepth = fallHex.terrainLevel(Terrains.WATER);
                entity.setElevation(-waterDepth);
            }
        }
    }
    
    public static void checkAndApplyMagmaCrust(IHex hex, int elevation, Entity entity, Coords curPos,
            boolean jumpLanding, Vector<Report> vPhaseReport, Server server) {
        
        if ((hex.terrainLevel(Terrains.MAGMA) == 1) && (elevation == 0) && (entity.getMovementMode() != EntityMovementMode.HOVER)) {
            int reportID = jumpLanding ? 2396 : 2395;
            
            int roll = Compute.d6();
            Report r = new Report(reportID);
            r.addDesc(entity);
            r.add(roll);
            r.subject = entity.getId();
            vPhaseReport.add(r);
            
            int rollTarget = jumpLanding ? 4 : 6;
            
            if (roll >= rollTarget) {
                hex.removeTerrain(Terrains.MAGMA);
                hex.addTerrain(Terrains.getTerrainFactory().createTerrain(Terrains.MAGMA, 2));
                server.sendChangedHex(curPos);
                for (Entity en : entity.getGame().getEntitiesVector(curPos)) {
                    server.doMagmaDamage(en, false);
                }
            }
        }
    }
    
    /**
     * Returns a list of path names of available boards of the size set in the given
     * mapSettings. The path names are minus the '.board' extension and relative to
     * the boards data directory.
     */
    static ArrayList<String> scanForBoards(MapSettings mapSettings) {
        BoardDimensions boardSize = mapSettings.getBoardSize();
        ArrayList<String> result = new ArrayList<>();
        
        // Scan the Megamek boards directory
        File boardDir = Configuration.boardsDir();
        scanForBoardsInDir(boardDir, "", boardSize, result);
        
        // Scan the userData directory
        boardDir = new File(Configuration.userdataDir(), Configuration.boardsDir().toString());
        if (boardDir.isDirectory()) {
            scanForBoardsInDir(boardDir, "", boardSize, result);
        }
        
        result.sort(StringUtil.stringComparator());
        return result;
    }
    
    /**
     * Scans the given boardDir directory for map boards of the given size and
     * returns them by adding them to the given boards list. Removes the .board extension.
     */
    static List<String> scanForBoardsInDir(final File boardDir, final String basePath,
                                            final BoardDimensions dimensions, List<String> boards) {
        if (boardDir == null) {
            throw new IllegalArgumentException("must provide searchDir");
        } else if (basePath == null) {
            throw new IllegalArgumentException("must provide basePath");
        } else if (dimensions == null) {
            throw new IllegalArgumentException("must provide dimensions");
        } else if (boards == null) {
            throw new IllegalArgumentException("must provide boards");
        }

        String[] fileList = boardDir.list();
        if (fileList != null) {
            for (String filename : fileList) {
                File filePath = new MegaMekFile(boardDir, filename).getFile();
                if (filePath.isDirectory()) {
                    scanForBoardsInDir(filePath, basePath + File.separator + filename, dimensions, boards);
                } else {
                    if (filename.endsWith(".board")) {
                        if (Board.boardIsSize(filePath, dimensions)) {
                            boards.add(basePath + File.separator + filename.substring(0, filename.lastIndexOf(".")));
                        }
                    }
                }
            }
        }
        return boards;
    }


    /**
     * Returns true when the given new force (that is not part of the given game's forces)
     * can be integrated into game's forces without error; i.e.:
     * if the force's parent exists or it is top-level, 
     * if it has no entities and no subforces,
     * if the client sending it is the owner
     */
    static boolean isNewForceValid(IGame game, Force force) {
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
    static String entityUpdateMessage(Entity entity, IGame game) {
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
    static HashSet<Entity> lobbyUnload(IGame game, Collection<Entity> entities) { 
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
    static HashSet<Entity> lobbyUnloadOthers(IGame game, Collection<Entity> entities) { 
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
    private static HashSet<Entity> lobbyDisembark(IGame game, Entity entity) {
        return lobbyDisembarkOthers(game, entity, new ArrayList<>());
    }
    
    /** 
     * Have the given entity disembark if it is carried by another unit.
     * Returns a set of entities that were modified. The set is empty if
     * the entity was not loaded to a carrier. 
     * <P>NOTE: This is a simplified disembark that is only valid in the lobby!
     */
    private static HashSet<Entity> lobbyDisembarkOthers(IGame game, Entity entity, Collection<Entity> entities) {
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
    private static void lobbyDisembarkEnemy(IGame game, Entity entity) {
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
    static HashSet<Entity> performC3Disconnect(IGame game, Collection<Entity> entities) {
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
        return new Packet(Packet.COMMAND_ENTITY_MULTIUPDATE, entities);
    }
    
    /**
     * Creates a packet detailing a force delete.
     * Only valid in the lobby.
     */
    static Packet createForcesDeletePacket(Collection<Integer> forces) {
        return new Packet(Packet.COMMAND_FORCE_DELETE, forces);
    }

    /** 
     * Handles a force parent packet, attaching the sent forces to a new parent or 
     * making the sent forces top-level. 
     * This method is intended for use in the lobby!
     */
    static void receiveForceParent(Packet c, int connId, IGame game, Server server) {
        @SuppressWarnings("unchecked")
        var forceList = (Collection<Force>) c.getObject(0);
        int newParentId = (int) c.getObject(1);
        
        var forces = game.getForces();
        var changedForces = new HashSet<Force>();
        
        if (newParentId == Force.NO_FORCE) {
            forceList.stream().forEach(f -> changedForces.addAll(forces.promoteForce(forces.getForce(f.getId()))));
        } else {
            if (!forces.contains(newParentId)) {
                MegaMek.getLogger().warning("Tried to attach forces to non-existing parent force ID " + newParentId);
                return;
            }
            Force newParent = forces.getForce(newParentId);
            forceList.stream().forEach(f -> changedForces.addAll(forces.attachForce(forces.getForce(f.getId()), newParent)));
        }
        
        if (!changedForces.isEmpty()) {
            server.send(createForceUpdatePacket(changedForces));
        }
    }
    
    /** 
     * Handles a force assign full packet, changing the owner of forces and everything in them.
     * This method is intended for use in the lobby!
     */
    static void receiveEntitiesAssign(Packet c, int connId, IGame game, Server server) {
        @SuppressWarnings("unchecked")
        var entityList = (Collection<Entity>) c.getObject(0);
        int newOwnerId = (int) c.getObject(1);
        IPlayer newOwner = game.getPlayer(newOwnerId);

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
        server.send(server.createFullEntitiesPacket());
    }
    
    /** 
     * Handles a force assign full packet, changing the owner of forces and everything in them.
     * This method is intended for use in the lobby!
     */
    static void receiveForceAssignFull(Packet c, int connId, IGame game, Server server) {
        @SuppressWarnings("unchecked")
        var forceList = (Collection<Force>) c.getObject(0);
        int newOwnerId = (int) c.getObject(1);
        IPlayer newOwner = game.getPlayer(newOwnerId);

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
            Collection<Entity> entities = forces.getFullEntities(force);
            forces.assignFullForces(force, newOwner);
            for (Entity entity: entities) {
                entity.setOwner(newOwner);
            }
        }
        forces.correct();
        correctLoading(game);
        correctC3Connections(game);
        server.send(server.createFullEntitiesPacket());
    }
    
    /** 
     * Handles a force update packet, forwarding a client-side change that 
     * only affects forces, not entities:
     * - rename
     * - move subforce/entity up/down (this does not change the entitiy, only the force)
     * - owner change of only the force (not the entities, only within a team) 
     * This method is intended for use in the lobby!
     */
    static void receiveForceUpdate(Packet c, int connId, IGame game, Server server) {
        @SuppressWarnings("unchecked")
        var forceList = (Collection<Force>) c.getObject(0);
        
        // Check if the updated Forces are valid
        Forces forcesClone = game.getForces().clone();
        for (Force force: forceList) {
            forcesClone.replace(force.getId(), force);
        }
        if (forcesClone.isValid()) {
            game.setForces(forcesClone);
            server.send(createForceUpdatePacket(forceList));
        } else {
            MegaMek.getLogger().warning("Invalid forces update received.");
            server.send(server.createFullEntitiesPacket());
        }
    }
    
    /** 
     * Handles a team change, updating units and forces as necessary.
     * This method is intended for use in the lobby!
     */
    static void receiveLobbyTeamChange(Packet c, int connId, IGame game, Server server) {
        @SuppressWarnings("unchecked")
        var players = (Collection<IPlayer>) c.getObject(0);
        var newTeam = (int) c.getObject(1);
        
        // Collect server-side player objects
        var serverPlayers = new HashSet<IPlayer>();
        players.stream().map(p -> game.getPlayer(p.getId())).forEach(serverPlayers::add);
        
        // Check parameters and if there's an actual change to a player
        serverPlayers.removeIf(p -> p == null || p.getTeam() == newTeam);
        if (serverPlayers.isEmpty() || newTeam < 0 || newTeam > 5) {
            return;
        }
        
        // First, change all teams, then correct all connections (load, C3, force)
        for (IPlayer player: serverPlayers) {
            player.setTeam(newTeam);
        }
        Forces forces = game.getForces();
        forces.correct();
        correctLoading(game);
        correctC3Connections(game);
        
        server.send(server.createFullEntitiesPacket());
        for (IPlayer player: serverPlayers) {
            server.send(server.createPlayerUpdatePacket(player.getId()));
        }
    }

    /** 
     * For all game units, disembarks from carriers and offloads carried units
     * if they are enemies.  
     * <P>NOTE: This is a simplified unload that is only valid in the lobby!
     */
    static void correctLoading(IGame game) {
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
    static void correctC3Connections(IGame game) {
        for (Entity entity: game.getEntitiesVector()) {
            if (entity.hasNhC3()) {
                String net = entity.getC3NetId();
                int id = Entity.NONE;
                try {
                    id = Integer.parseInt(net.substring(net.indexOf(".") + 1));
                    if (game.getEntity(id).getOwner().isEnemyOf(entity.getOwner())) {
                        entity.setC3NetIdSelf();
                    }
                } catch (Exception e) {
                }
            } else if (entity.hasAnyC3System()) {
                if (entity.getC3Master() != null 
                        && entity.getC3Master().getOwner().isEnemyOf(entity.getOwner()))
                entity.setC3Master(null, true);
            }
        }
    }

    /** 
     * Handles an add entity to force / remove from force packet, attaching the 
     * sent entities to a force or removing them from any force. 
     * This method is intended for use in the lobby!
     */
    static void receiveAddEntititesToForce(Packet c, int connId, IGame game, Server server) {
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
                MegaMek.getLogger().warning("Tried to add entities to an enemy force.");
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
        server.send(createForceUpdatePacket(changedForces, changedEntities));
    }
    
    /**
     * Adds a force with the info from the client. Only valid during the lobby phase.
     */
    static void receiveForceAdd(Packet c, int connId, IGame game, Server server) {
        var force = (Force) c.getObject(0);
        @SuppressWarnings("unchecked")
        var entities = (Collection<Entity>) c.getObject(1);

        int newId;
        if (force.isTopLevel()) {
            newId = game.getForces().addTopLevelForce(force.getName(), game.getPlayer(force.getOwnerId()));
        } else {
            Force parent = game.getForces().getForce(force.getParentId()); 
            newId = game.getForces().addSubForce(force.getName(), parent);
        }
        for (var entity: entities) {
            game.getForces().addEntity(game.getEntity(entity.getId()), newId);
        }
        server.send(server.createFullEntitiesPacket());
    }
    
    /**
     * Creates a packet detailing a force update. Force updates must contain all
     * affected forces and all affected entities. Entities are only affected if their
     * forceId changed.
     */
    static Packet createForceUpdatePacket(Collection<Force> changedForces, Collection<Entity> entities) {
        final Object[] data = new Object[2];
        data[0] = changedForces;
        data[1] = entities;
        return new Packet(Packet.COMMAND_FORCE_UPDATE, data);
    }
    
    /**
     * Creates a packet detailing a force update. Force updates must contain all
     * affected forces and all affected entities.
     */
    static Packet createForceUpdatePacket(Collection<Force> changedForces) {
        final Object[] data = new Object[2];
        data[0] = changedForces;
        data[1] = new ArrayList<Entity>();
        return new Packet(Packet.COMMAND_FORCE_UPDATE, data);
    }
    
    /**
     * A force is editable to the sender of a command if any forces in its force chain
     * (this includes the force itself) is owned by the sender. This allows editing 
     * forces of other players if they are a subforce of one's own force.
     * @see LobbyActions#isEditable(Force)
     */
    static boolean isEditable(Force force, IGame game, IPlayer sender) {
        List<Force> chain = game.getForces().forceChain(force);
        return chain.stream().map(f -> game.getForces().getOwner(f)).anyMatch(p -> p.equals(sender));
    }
  
}
