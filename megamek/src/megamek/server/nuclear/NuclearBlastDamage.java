/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.server.nuclear;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.MekWarrior;
import megamek.common.units.ProtoMek;
import megamek.common.weapons.DamageType;
import megamek.common.weapons.handlers.AreaEffectHelper;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Deals a nuclear detonation's blast damage to the buildings, terrain and units around Ground Zero (IO:AE p.165).
 */
class NuclearBlastDamage {
    /** Nuclear blast damage is applied in 5-point clusters (IO:AE p.165). */
    static final int BLAST_CLUSTER_SIZE = 5;
    private static final int BOARD_ID = 0;

    private final TWGameManager gameManager;
    private final Game game;

    NuclearBlastDamage(TWGameManager gameManager) {
        this.gameManager = gameManager;
        this.game = gameManager.getGame();
    }

    /**
     * Applies the blast.
     *
     * @param groundZero   the Ground Zero hex
     * @param damages      blast damage by distance from Ground Zero, index 0 being Ground Zero
     * @param vDesc        the report the results are added to
     * @param blastedUnits filled with the ids of the units the blast struck
     */
    void apply(Coords groundZero, int[] damages, Vector<Report> vDesc, Vector<Integer> blastedUnits) {
        damageBuildings(groundZero, damages, vDesc);
        damageTerrain(groundZero, damages, vDesc);

        List<Entity> carriedUnits = new ArrayList<>();
        for (Entity entity : game.inGameTWEntities()) {
            if (entity.isDestroyed() || !entity.isDeployed()) {
                continue;
            }
            if ((entity instanceof MekWarrior mekWarrior) && !mekWarrior.hasLanded()) {
                // Still in the air after ejecting
                continue;
            }
            Coords entityPosition = entity.getPosition();
            if (entityPosition == null) {
                Entity transport = game.getEntity(entity.getTransportId());
                if ((transport != null) && !transport.isAirborne()) {
                    carriedUnits.add(entity);
                }
                continue;
            }
            // An Advanced Building entity is the building at its hexes and was already damaged as a building
            if (entity instanceof AbstractBuildingEntity) {
                continue;
            }
            int range = groundZero.distance(entityPosition);
            if (range >= damages.length) {
                continue;
            }
            if (entity.isAirborne()) {
                continue;
            }
            if (range == 0) {
                vDesc.addAll(gameManager.destroyEntity(entity, "explosion proximity", false, false));
                blastedUnits.add(entity.getId());
                entity.getCrew().setDoomed(true);
                continue;
            }
            if (isSheltered(entity, entityPosition, entity.relHeight(), groundZero, vDesc)) {
                continue;
            }
            blastedUnits.add(entity.getId());
            AreaEffectHelper.applyExplosionClusterDamageToEntity(entity, damages[range], BLAST_CLUSTER_SIZE,
                  groundZero, vDesc, gameManager);
            Report.addNewline(vDesc);
        }

        for (Entity entity : carriedUnits) {
            damageCarriedUnit(entity, groundZero, damages, vDesc, blastedUnits);
        }
    }

    private void damageBuildings(Coords groundZero, int[] damages, Vector<Report> vDesc) {
        Enumeration<IBuilding> buildings = game.getBoard(BOARD_ID).getBuildings();
        while (buildings.hasMoreElements()) {
            IBuilding building = buildings.nextElement();
            Enumeration<Coords> buildingHexes = building.getCoords();
            while (buildingHexes.hasMoreElements()) {
                Coords coords = buildingHexes.nextElement();
                int distance = groundZero.distance(coords);
                if (distance < damages.length) {
                    Vector<Report> buildingReport = gameManager.damageBuilding(building, damages[distance], coords);
                    for (Report report : buildingReport) {
                        report.type = Report.PUBLIC;
                    }
                    vDesc.addAll(buildingReport);
                }
            }
        }
    }

    private void damageTerrain(Coords groundZero, int[] damages, Vector<Report> vDesc) {
        for (int distance = 0; distance < damages.length; distance++) {
            for (Coords coords : groundZero.allAtDistance(distance)) {
                Hex hex = game.getHex(coords, BOARD_ID);
                if ((hex != null) && hex.hasTerrainFactor()) {
                    Report report = new Report(3384);
                    report.indent(2);
                    report.type = Report.PUBLIC;
                    report.add(coords.getBoardNum());
                    report.add(damages[distance]);
                    vDesc.add(report);
                }
                Vector<Report> reports = gameManager.tryClearHex(coords, damages[distance], Entity.NONE);
                for (Report report : reports) {
                    report.indent(3);
                }
                vDesc.addAll(reports);
            }
        }
    }

    /**
     * A unit carried outside its transport, or whose transport is gone, takes the blast at the transport's hex.
     */
    private void damageCarriedUnit(Entity entity, Coords groundZero, int[] damages, Vector<Report> vDesc,
          Vector<Integer> blastedUnits) {
        Entity transporter = game.getEntity(entity.getTransportId());
        if ((transporter != null) && !transporter.getExternalUnits().contains(entity)) {
            return;
        }
        Coords entityPosition = (transporter == null) ? entity.getPosition() : transporter.getPosition();
        int range = groundZero.distance(entityPosition);
        if (range >= damages.length) {
            return;
        }
        int height = (transporter == null) ? entity.relHeight() : transporter.relHeight();
        if (isSheltered(entity, entityPosition, height, groundZero, vDesc)) {
            return;
        }
        blastedUnits.add(entity.getId());
        int damage = damages[range];
        Report report = new Report(6175);
        report.subject = entity.getId();
        report.indent(2);
        report.addDesc(entity);
        report.add(damage);
        vDesc.addElement(report);
        while (damage > 0) {
            int cluster = Math.min(BLAST_CLUSTER_SIZE, damage);
            int table = (entity instanceof ProtoMek) ? ToHitData.HIT_SPECIAL_PROTO : ToHitData.HIT_NORMAL;
            HitData hit = entity.rollHitLocation(table, ToHitData.SIDE_FRONT);
            vDesc.addAll(gameManager.damageEntity(entity, hit, cluster, false, DamageType.IGNORE_PASSENGER, false,
                  true));
            damage -= cluster;
        }
        Report.addNewline(vDesc);
    }

    /**
     * The shelter roll: a unit behind a taller hex toward Ground Zero escapes the blast on 9+ on 2d6. Reports either
     * outcome.
     *
     * @return {@code true} if the unit escapes the blast
     */
    private boolean isSheltered(Entity entity, Coords entityPosition, int height, Coords groundZero,
          Vector<Report> vDesc) {
        if (!gameManager.canShelter(entityPosition, BOARD_ID, groundZero, height)) {
            return false;
        }
        boolean sheltered = Compute.d6(2) >= 9;
        Report report = new Report(sheltered ? 6545 : 6546);
        report.addDesc(entity);
        report.subject = entity.getId();
        vDesc.addElement(report);
        return sheltered;
    }
}
