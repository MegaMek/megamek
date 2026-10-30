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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Resolves the detonation of a nuclear weapon on a ground map: the crater, the blast, the terrain damage and the
 * secondary effects (IO:AE pp. 164-166).
 */
public class NuclearDetonationResolver {
    private static final MMLogger LOGGER = MMLogger.create(NuclearDetonationResolver.class);

    private final TWGameManager gameManager;
    private final Game game;

    /**
     * @param gameManager the game manager running the game the detonation happens in
     */
    public NuclearDetonationResolver(TWGameManager gameManager) {
        this.gameManager = gameManager;
        this.game = gameManager.getGame();
    }

    /**
     * Resolves a ground burst.
     *
     * @param position the Ground Zero hex
     * @param profile  the detonation's damage, radii and crater depth
     * @param vDesc    a vector that contains the output report
     *
     * @return the number of units the detonation destroyed
     */
    public int resolve(Coords position, NuclearBlastProfile profile, Vector<Report> vDesc) {
        long startNanos = System.nanoTime();
        Set<Integer> standingBefore = new LinkedHashSet<>();
        for (Entity entity : game.getEntitiesVector()) {
            if (!entity.isDestroyed() && !entity.isDoomed()) {
                standingBefore.add(entity.getId());
            }
        }
        // Every hex the detonation changes goes to the clients in one update at the end. A Santa Ana changes every
        // hex of a normal map, and one packet per hex made each client redraw the board thousands of times.
        Set<Coords> changedHexes = new LinkedHashSet<>();
        // Just in case.
        if (vDesc == null) {
            vDesc = new Vector<>();
        }

        // First, crater the terrain.
        // All terrain, units, buildings... EVERYTHING in here is just gone.
        // Got to love nukes.
        Report r = new Report(1215, Report.PUBLIC);

        r.indent();
        r.add(position.getBoardNum(), true);
        vDesc.add(r);

        int craterRadius = profile.craterRadius();
        LOGGER.info("[Nukes] Detonation at hex {}: impact {}, damage reduction {}/hex, blast radius {}, "
              + "secondary radius {}, crater depth {}, crater radius {}", position.getBoardNum(),
              profile.impactDamage(), profile.damageReduction(), profile.blastRadius(), profile.secondaryRadius(),
              profile.craterDepth(), craterRadius);
        NuclearTerrainEffects terrainEffects = new NuclearTerrainEffects(gameManager);
        terrainEffects.crater(position, profile, changedHexes);

        // This is technically part of cratering, but...
        // Now we destroy all the units inside the cratering range.
        for (Entity entity : game.getEntitiesVector()) {
            // loaded units and off board units don't have a position,
            // so we don't count 'em here
            if ((entity.getTransportId() != Entity.NONE) || (entity.getPosition() == null)) {
                continue;
            }

            // If it's too far away for this...
            if (position.distance(entity.getPosition()) >= craterRadius) {
                continue;
            }

            // If it's already destroyed...
            if (entity.isDestroyed()) {
                continue;
            }

            LOGGER.info("[Nukes] {} at hex {} is inside the crater ({} hexes from Ground Zero): destroyed",
                  entity.getDisplayName(), entity.getPosition().getBoardNum(), position.distance(entity.getPosition()));
            vDesc.addAll(gameManager.destroyEntity(entity, "nuclear explosion proximity", false, false));
            // Kill the crew
            entity.getCrew().setDoomed(true);
        }

        // Then, do actual blast damage.
        Vector<Report> tmpV = new Vector<>();
        Vector<Integer> blastedUnitsVec = new Vector<>();

        new NuclearBlastDamage(gameManager).apply(position, profile, tmpV, blastedUnitsVec);
        Report.indentAll(tmpV, 2);
        vDesc.addAll(tmpV);

        terrainEffects.clearGroundZero(position, profile, changedHexes);
        terrainEffects.damageTerrain(position, profile, changedHexes);
        new NuclearFires(gameManager).startFires(position, profile, changedHexes, vDesc);

        gameManager.sendChangedHexes(changedHexes, 0);

        // Lastly, do secondary effects.
        NuclearSecondaryEffects secondaryEffects = new NuclearSecondaryEffects(gameManager);
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.isDoomed() || entity.isDestroyed()) {
                continue;
            }
            // A carried unit rolls at its carrier's hex and height (HammerGS, 2026-10-26)
            Entity positioned = (entity.getTransportId() == Entity.NONE)
                  ? entity
                  : game.getEntity(entity.getTransportId());
            // Off-board units have no position
            if ((positioned == null) || (positioned.getPosition() == null)) {
                continue;
            }
            if (position.distance(positioned.getPosition()) > profile.secondaryRadius()) {
                continue;
            }
            if (!NuclearSecondaryEffects.isWithinSecondaryHeight(positioned, profile)) {
                continue;
            }
            secondaryEffects.apply(entity, position, vDesc);
        }

        LOGGER.info("[Nukes] Detonation at hex {} resolved in {} ms; {} changed hexes sent in one update",
              position.getBoardNum(), (System.nanoTime() - startNanos) / 1_000_000, changedHexes.size());

        // All right. We're done.
        r = new Report(1216, Report.PUBLIC);
        r.indent();
        r.newlines = 2;
        vDesc.add(r);

        // Nothing a nuclear weapon destroys can be salvaged (IO:AE p.168).
        int unitsDestroyed = 0;
        // A unit carried by a destroyed transport goes straight to the graveyard, so look up every unit that was
        // standing, not only the ones still in play
        for (int unitId : standingBefore) {
            Entity entity = game.getEntityFromAllSources(unitId);
            if ((entity == null) || !(entity.isDestroyed() || entity.isDoomed())) {
                continue;
            }
            entity.setSalvage(false);
            boolean inGraveyard = game.getEntity(unitId) == null;
            if (inGraveyard && (entity.getRemovalCondition() == IEntityRemovalConditions.REMOVE_SALVAGEABLE)) {
                entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_DEVASTATED);
            }
            LOGGER.info("[Nukes] {} destroyed by the detonation: unsalvageable{}", entity.getDisplayName(),
                  inGraveyard ? " (already in the graveyard)" : "");
            unitsDestroyed++;
        }
        LOGGER.info("[Nukes] {} unit(s) struck by the blast, {} destroyed and marked unsalvageable",
              blastedUnitsVec.size(), unitsDestroyed);
        return unitsDestroyed;
    }
}
