package megamek.server.Applier;

import megamek.common.*;
import megamek.server.Server;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

public class DropShipApplier {
    public static void applyDropShipLandingDamage(Server server, Coords centralPos, Entity killer) {
        // first cycle through hexes to figure out final elevation
        Hex centralHex = server.getGame().getBoard().getHex(centralPos);
        if (null == centralHex) {
            // shouldn't happen
            return;
        }
        int finalElev = centralHex.getLevel();
        if (!centralHex.containsTerrain(Terrains.PAVEMENT)
            && !centralHex.containsTerrain(Terrains.ROAD)) {
            finalElev--;
        }
        Vector<Coords> positions = new Vector<>();
        positions.add(centralPos);
        for (int i = 0; i < 6; i++) {
            Coords pos = centralPos.translated(i);
            Hex hex = server.getGame().getBoard().getHex(pos);
            if (null == hex) {
                continue;
            }
            if (hex.getLevel() < finalElev) {
                finalElev = hex.getLevel();
            }
            positions.add(pos);
        }
        // ok now cycle through hexes and make all changes
        for (Coords pos : positions) {
            Hex hex = server.getGame().getBoard().getHex(pos);
            hex.setLevel(finalElev);
            // get rid of woods and replace with rough
            if (hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE)) {
                hex.removeTerrain(Terrains.WOODS);
                hex.removeTerrain(Terrains.JUNGLE);
                hex.removeTerrain(Terrains.FOLIAGE_ELEV);
                hex.addTerrain(new Terrain(Terrains.ROUGH, 1));
            }
            server.sendChangedHex(pos);
        }

        applyDropShipProximityDamage(server, centralPos, killer);
    }

    public static void applyDropShipProximityDamage(Server server, Coords centralPos, Entity killer) {
        applyDropShipProximityDamage(server, centralPos, false, 0, killer);
    }

    /**
     * apply damage to units and buildings within a certain radius of a landing
     * or lifting off DropShip
     *
     * @param server
     * @param centralPos - the Coords for the central position of the DropShip
     */
    public static void applyDropShipProximityDamage(Server server, Coords centralPos, boolean rearArc, int facing, Entity killer) {

        Vector<Integer> alreadyHit = new Vector<>();

        // anything in the central hex or adjacent hexes is destroyed
        Hashtable<Coords, Vector<Entity>> positionMap = server.getGame().getPositionMap();
        for (Entity en : server.getGame().getEntitiesVector(centralPos)) {
            if (!en.isAirborne()) {
                server.addReport(server.destroyEntity(en, "DropShip proximity damage", false,
                                        false));
                alreadyHit.add(en.getId());
            }
        }
        Building bldg = server.getGame().getBoard().getBuildingAt(centralPos);
        if (null != bldg) {
            server.collapseBuilding(bldg, positionMap, centralPos, server.getvPhaseReport());
        }
        for (int i = 0; i < 6; i++) {
            Coords pos = centralPos.translated(i);
            for (Entity en : server.getGame().getEntitiesVector(pos)) {
                if (!en.isAirborne()) {
                    server.addReport(server.destroyEntity(en, "DropShip proximity damage",
                                            false, false));
                }
                alreadyHit.add(en.getId());
            }
            bldg = server.getGame().getBoard().getBuildingAt(pos);
            if (null != bldg) {
                server.collapseBuilding(bldg, positionMap, pos, server.getvPhaseReport());
            }
        }

        // Report r;
        // ok now I need to look at the damage rings - start at 2 and go to 7
        for (int i = 2; i < 8; i++) {
            int damageDice = (8 - i) * 2;
            List<Coords> ring = centralPos.allAtDistance(i);
            for (Coords pos : ring) {
                if (rearArc && !Compute.isInArc(centralPos, facing, pos, Compute.ARC_AFT)) {
                    continue;
                }

                alreadyHit = server.artilleryDamageHex(pos, centralPos, damageDice, null, killer.getId(),
                        killer, null, false, 0, server.getvPhaseReport(), false,
                        alreadyHit, true);
            }
        }
        server.destroyDoomedEntities(alreadyHit);
    }
}
