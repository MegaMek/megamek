/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.handlers;

import static megamek.common.weapons.ArtilleryHandlerHelper.clearMines;
import static megamek.common.weapons.ArtilleryHandlerHelper.findSpotter;
import static megamek.common.weapons.ArtilleryHandlerHelper.isForwardObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import megamek.common.units.Infantry;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.SpecialHexDisplay;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

public class CapitalLaserBayOrbitalBombardmentHandler extends BayWeaponHandler {

    private static final MMLogger LOGGER = MMLogger.create(CapitalLaserBayOrbitalBombardmentHandler.class);

    private boolean isReported = false;
    private final ArtilleryAttackAction attackAction;

    public CapitalLaserBayOrbitalBombardmentHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m)
          throws EntityLoadingException {
        super(t, w, g, m);
        attackAction = (ArtilleryAttackAction) w;
    }

    @Override
    public boolean cares(GamePhase phase) {
        return phase.isOffboard() || phase.isTargeting();
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> reports) {
        if (attackingEntity == null || target == null || weaponType == null || !game.hasBoardLocationOf(target)) {
            LOGGER.error("Attack info incomplete!");
            return false;
        }

        // This attack has just been declared; report it once
        if (!isReported) {
            addHeat();
            reportFiring(reports, attackAction);
            isReported = true;
        }

        if (phase.isTargeting()) {
            // I have no clue how/why this is used:
            setAnnouncedEntityFiring(false);
            return true;
        }

        // If at least one valid spotter, then get the benefits thereof.
        Optional<Entity> spotter = findSpotter(attackAction.getSpotterIds(), attackAction.getPlayerId(), game,
              target);

        if (spotter.isPresent()) {
            int modifier = (spotter.get().getCrew().getGunnery() - 4) / 2;
            modifier += isForwardObserver(spotter.get()) ? -1 : 0;
            // Comm implant bonus only applies to non-infantry spotters
            modifier += (!(spotter.get() instanceof Infantry) && spotter.get().hasAbility(OptionsConstants.MD_COMM_IMPLANT)) ? -1 : 0;
            toHit.addModifier(modifier, "Spotting modifier");
        }

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();
        // Set Margin of Success/Failure.
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));

        // If the shot hit the target hex, then all subsequent fire will hit the hex automatically.
        if (!bMissed) {
            attackingEntity.aTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, target.getPosition());
        } else if (spotter.isPresent()) {
            // If the shot missed, but was adjusted by a spotter, future shots are more likely to hit.
            // only add mods if it's not an automatic success
            int currentModifier = attackingEntity.aTracker.getModifier(weapon, target.getPosition());
            if (currentModifier != TargetRoll.AUTOMATIC_SUCCESS) {
                if (isForwardObserver(spotter.get())) {
                    attackingEntity.aTracker.setSpotterHasForwardObs(true);
                }
                // Comm implant bonus only applies to non-infantry spotters
                if (!(spotter.get() instanceof Infantry) &&
                      spotter.get().hasAbility(OptionsConstants.MD_COMM_IMPLANT)) {
                    attackingEntity.aTracker.setSpotterHasCommImplant(true);
                }
                attackingEntity.aTracker.setModifier(currentModifier - 1, target.getPosition());
            }
        }

        // Report weapon attack and its to-hit value.
        Report report = new Report(3120).indent().noNL().subject(subjectId).add(weaponType.getName());
        report.add(target.getDisplayName(), true);
        reports.addElement(report);

        if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
            reports.addElement(new Report(3135).subject(subjectId).add(toHit.getDesc()));
            return false;
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
            reports.addElement(new Report(3140).noNL().subject(subjectId).add(toHit.getDesc()));
        } else if (toHit.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            reports.addElement(new Report(3145).noNL().subject(subjectId).add(toHit.getDesc()));
        } else {
            // roll to hit
            reports.addElement(new Report(3150).noNL().subject(subjectId).add(toHit));
        }

        // dice have been rolled, thanks
        reports.addElement(new Report(3155).noNL().subject(subjectId).add(roll));

        // In the case of misses, we'll need to hit multiple hexes
        List<Coords> actualHits = new ArrayList<>();
        Board board = game.getBoard(target);

        if (!bMissed) {
            report = new Report(3203).subject(subjectId).add(numWeaponsHit).add(target.getPosition().getBoardNum());
            reports.addElement(report);
            String message = "Orbital Bombardment by %s hit here on round %d (this hex is now an auto-hit)"
                  .formatted(owner().getName(), game.getRoundCount());

            board.addSpecialHexDisplay(target.getPosition(),
                  new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_HIT, game.getRoundCount(), owner(), message));
            for (int i = 0; i < numWeaponsHit; i++) {
                actualHits.add(target.getPosition());
            }
        } else {
            // We're only going to display one missed shot hex on the board, at the intended target
            // Any drifted shots will be indicated at their end points
            String message = "Orbital Bombardment by %s missed here on round %d"
                  .formatted(owner().getName(), game.getRoundCount());
            board.addSpecialHexDisplay(target.getPosition(),
                  SpecialHexDisplay.createArtyMiss(owner(), game.getRoundCount(), message));

            while (numWeaponsHit > 0) {
                // Scatter individual weapons (not sure where this is applicable)
                // Scatter distance, see SO:AA, p.91; I decided to have Oblique Artilleryman (CO, p.78) not apply to
                // Capital Laser Weapons as they are not "artillery pieces"
                int scatterDistance = 2 * Math.abs(toHit.getMoS());
                Coords scatteredPosition = Compute.scatterDirectArty(target.getPosition(), scatterDistance);
                if (board.contains(scatteredPosition)) {
                    actualHits.add(scatteredPosition);
                    // misses and scatters to another hex
                    reports.addElement(new Report(3202).subject(subjectId)
                          .add("One")
                          .add(scatteredPosition.getBoardNum()));
                } else {
                    // misses and scatters off-board
                    reports.addElement(new Report(3200).subject(subjectId));
                }
                numWeaponsHit--;
            }

            // If we managed to land everything off the board, this handler is finished for good
            if (actualHits.isEmpty()) {
                return false;
            }
        }

        DamageFalloff falloff = new DamageFalloff();
        falloff.damage = calcAttackValue() * 10;
        falloff.falloff = calcAttackValue() * 2;
        falloff.radius = 4;
        falloff.clusterMunitionsFlag = false;

        for (Coords actualHit : actualHits) {
            clearMines(reports, actualHit, game, attackingEntity, gameManager);

            Vector<Integer> alreadyHit = new Vector<>();
            var blastShape = AreaEffectHelper.shapeBlast(null, actualHit, falloff, board.getHex(actualHit).getLevel(),
                  false, false, false, game, false);

            for (var entry : blastShape.keySet()) {
                alreadyHit = gameManager.artilleryDamageHex(entry.getValue(), board.getBoardId(), actualHit,
                      blastShape.get(entry), null, subjectId, attackingEntity, null, false, entry.getKey(),
                      board.getHex(actualHit).getLevel(), reports,
                      false, alreadyHit, false, falloff);
            }
        }
        return false;
    }

    private Player owner() {
        return game.getPlayer(attackAction.getPlayerId());
    }

    private void reportFiring(Vector<Report> reports, ArtilleryAttackAction aaa) {
        Report report = new Report(3121).indent().noNL().subject(subjectId);
        report.add(weaponType.getName());
        report.add(aaa.getTurnsTilHit());
        reports.addElement(report);
        Report.addNewline(reports);

        Player owner = game.getPlayer(aaa.getPlayerId());
        int landingRound = game.getRoundCount() + aaa.getTurnsTilHit();
        String message = "Orbital Bombardment incoming, landing this round, fired by " + owner.getName();
        SpecialHexDisplay incomingMarker = SpecialHexDisplay.createIncomingFire(owner, landingRound, message);
        game.getBoard(target).addSpecialHexDisplay(target.getPosition(), incomingMarker);
    }
}
