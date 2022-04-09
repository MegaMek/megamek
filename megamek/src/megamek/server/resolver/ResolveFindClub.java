package megamek.server.resolver;

import megamek.common.*;
import megamek.server.Server;

public class ResolveFindClub {
    public static void resolveFindClub(Server server, Entity entity) {
        EquipmentType clubType = null;

        entity.setFindingClub(true);

        // Get the entity's current hex.
        Coords coords = entity.getPosition();
        Hex curHex = server.getGame().getBoard().getHex(coords);

        Report r;

        // Is there a blown off arm in the hex?
        if (curHex.terrainLevel(Terrains.ARMS) > 0) {
            clubType = EquipmentType.get(EquipmentTypeLookup.LIMB_CLUB);
            curHex.addTerrain(new Terrain(Terrains.ARMS, curHex.terrainLevel(Terrains.ARMS) - 1));
            server.sendChangedHex(entity.getPosition());
            r = new Report(3035);
            r.subject = entity.getId();
            r.addDesc(entity);
            server.addReport(r);
        }
        // Is there a blown off leg in the hex?
        else if (curHex.terrainLevel(Terrains.LEGS) > 0) {
            clubType = EquipmentType.get(EquipmentTypeLookup.LIMB_CLUB);
            curHex.addTerrain(new Terrain(Terrains.LEGS, curHex.terrainLevel(Terrains.LEGS) - 1));
            server.sendChangedHex(entity.getPosition());
            r = new Report(3040);
            r.subject = entity.getId();
            r.addDesc(entity);
            server.addReport(r);
        }

        // Is there the rubble of a medium, heavy,
        // or hardened building in the hex?
        else if (Building.LIGHT < curHex.terrainLevel(Terrains.RUBBLE)) {

            // Finding a club is not guaranteed. The chances are
            // based on the type of building that produced the
            // rubble.
            boolean found = false;
            int roll = Compute.d6(2);
            switch (curHex.terrainLevel(Terrains.RUBBLE)) {
                case Building.MEDIUM:
                    if (roll >= 7) {
                        found = true;
                    }
                    break;
                case Building.HEAVY:
                    if (roll >= 6) {
                        found = true;
                    }
                    break;
                case Building.HARDENED:
                    if (roll >= 5) {
                        found = true;
                    }
                    break;
                case Building.WALL:
                    if (roll >= 13) {
                        found = true;
                    }
                    break;
                default:
                    // we must be in ultra
                    if (roll >= 4) {
                        found = true;
                    }
            }

            // Let the player know if they found a club.
            if (found) {
                clubType = EquipmentType.get(EquipmentTypeLookup.GIRDER_CLUB);
                r = new Report(3045);
            } else {
                // Sorry, no club for you.
                r = new Report(3050);
            }
            r.subject = entity.getId();
            r.addDesc(entity);
            server.addReport(r);
        }

        // Are there woods in the hex?
        else if (curHex.containsTerrain(Terrains.WOODS)
                 || curHex.containsTerrain(Terrains.JUNGLE)) {
            clubType = EquipmentType.get(EquipmentTypeLookup.TREE_CLUB);
            r = new Report(3055);
            r.subject = entity.getId();
            r.addDesc(entity);
            server.addReport(r);
        }

        // add the club
        try {
            if (clubType != null) {
                entity.addEquipment(clubType, Entity.LOC_NONE);
            }
        } catch (LocationFullException ex) {
            // unlikely...
            r = new Report(3060);
            r.subject = entity.getId();
            r.addDesc(entity);
            server.addReport(r);
        }
    }
}
