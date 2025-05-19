/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import megamek.server.totalwarfare.TWGameManager;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public class CapitalLaserBayWeaponOrbitalBombardmentHandler extends BayWeaponHandler {

    private static final MMLogger LOGGER = MMLogger.create(CapitalLaserBayWeaponOrbitalBombardmentHandler.class);

    private boolean isReported = false;
    private final ArtilleryAttackAction attackAction;

    public CapitalLaserBayWeaponOrbitalBombardmentHandler(ToHitData t, WeaponAttackAction w, Game g, TWGameManager m) {
        super(t, w, g, m);
        attackAction = (ArtilleryAttackAction) w;
    }

    @Override
    public boolean cares(GamePhase phase) {
        return phase.isOffboard() || phase.isTargeting();
    }

    @Override
    public boolean handle(GamePhase phase, Vector<Report> reports) {
        if (!cares(phase)) {
            return true;
        } else if (ae == null || target == null || wtype == null || !game.hasBoardLocationOf(target)) {
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
            // I have no clue how this is used:
            setAnnouncedEntityFiring(false);
            return true;
        }

        // If at least one valid spotter, then get the benefits thereof.
        Optional<Entity> bestSpotter = findSpotter(attackAction.getSpotterIds(), attackAction.getPlayerId());
        if (bestSpotter.isPresent()) {
            int modifier = (bestSpotter.get().getCrew().getGunnery() - 4) / 2;
            modifier += isForwardObserver(bestSpotter.get()) ? -1 : 0;
            toHit.addModifier(modifier, "Spotting modifier");
        }

        // do we hit?
        bMissed = roll.getIntValue() < toHit.getValue();
        // Set Margin of Success/Failure.
        toHit.setMoS(roll.getIntValue() - Math.max(2, toHit.getValue()));

        // If the shot hit the target hex, then all subsequent fire will hit the hex automatically.
        if (!bMissed) {
            ae.aTracker.setModifier(TargetRoll.AUTOMATIC_SUCCESS, target.getPosition());
        } else if (bestSpotter.isPresent()) {
            // If the shot missed, but was adjusted by a spotter, future shots are more likely to hit.
            // Note: Because artillery fire is adjusted on a per-unit basis, this can result in
            // a unit firing multiple artillery weapons at the same hex getting this bonus more
            // than once per turn. Since the Artillery Modifiers Table on TacOps p. 180 lists a
            // -1 per shot (not salvo!) previously fired at the target hex, this would in fact
            // appear to be correct.

            // only add mods if it's not an automatic success
            int currentModifier = ae.aTracker.getModifier(weapon, target.getPosition());
            if (currentModifier != TargetRoll.AUTOMATIC_SUCCESS) {
                if (isForwardObserver(bestSpotter.get())) {
                    ae.aTracker.setSpotterHasForwardObs(true);
                }
                ae.aTracker.setModifier(currentModifier - 1, target.getPosition());
            }
        }

        // Report weapon attack and its to-hit value.
        Report report = new Report(3120).indent().noNL().subject(subjectId).add(wtype.getName());
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
            report = new Report(3203).subject(subjectId).add(nweaponsHit).add(target.getPosition().getBoardNum());
            reports.addElement(report);
            String artyMsg = "Artillery hit here on round " + game.getRoundCount()
                  + ", fired by " + game.getPlayer(attackAction.getPlayerId()).getName()
                  + " (this hex is now an auto-hit)";
            board.addSpecialHexDisplay(target.getPosition(),
                  new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_HIT,
                        game.getRoundCount(), game.getPlayer(attackAction.getPlayerId()), artyMsg));
            for (int i = 0; i < nweaponsHit; i++) {
                actualHits.add(target.getPosition());
            }
        } else {
            int moF = toHit.getMoS();
            if (ae.hasAbility(OptionsConstants.GUNNERY_OBLIQUE_ARTILLERY)) {
                // getMoS returns a negative MoF
                // simple math is better so lets make it positive
                if ((-moF - 2) < 1) {
                    moF = 0;
                } else {
                    moF += 2;
                }
            }
            // We're only going to display one missed shot hex on the board, at the intended target
            // Any drifted shots will be indicated at their end points
            String artyMsg = "Orbital Bombardment missed here on round "
                  + game.getRoundCount() + ", by "
                  + game.getPlayer(attackAction.getPlayerId()).getName();
            board.addSpecialHexDisplay(target.getPosition(),
                  new SpecialHexDisplay(SpecialHexDisplay.Type.ARTILLERY_MISS, game.getRoundCount(),
                        game.getPlayer(attackAction.getPlayerId()), artyMsg));

            // Typically, nweaponsHit is exactly 1
            while (nweaponsHit > 0) {
                // We'll generate a new report and scatter for each weapon fired
                Coords scatteredPosition = Compute.scatterDirectArty(target.getPosition(), moF);
                if (board.contains(scatteredPosition)) {
                    actualHits.add(scatteredPosition);
                    // misses and scatters to another hex
                    reports.addElement(new Report(3202).subject(subjectId).add("One").add(scatteredPosition.getBoardNum()));
                } else {
                    // misses and scatters off-board
                    reports.addElement(new Report(3200).subject(subjectId));
                }
                nweaponsHit--;
            }

            // If we managed to land everything off the board, stop
            if (actualHits.isEmpty()) {
                return !bMissed;
            }
        }

        AreaEffectHelper.DamageFalloff falloff = new AreaEffectHelper.DamageFalloff();
        falloff.damage = calcAttackValue() * 10;
        falloff.falloff = calcAttackValue() * 2;
        falloff.radius = 4;
        falloff.clusterMunitionsFlag = false;

        for (Coords actualHit : actualHits) {
            clearMines(reports, actualHit);
            gameManager.artilleryDamageArea(actualHit, board.getBoardId(), null,
                  ae.getId(), ae, falloff, false, board.getHex(actualHit).getLevel(), reports, false);
        }
        return false;
    }

    private void clearMines(Vector<Report> reports, Coords coords) {
        if (game.containsMinefield(coords)) {
            Enumeration<Minefield> minefields = game.getMinefields(coords).elements();
            ArrayList<Minefield> mfRemoved = new ArrayList<>();
            while (minefields.hasMoreElements()) {
                Minefield mf = minefields.nextElement();
                if (gameManager.clearMinefield(mf, ae, 10, reports)) {
                    mfRemoved.add(mf);
                }
            }
            for (Minefield mf : mfRemoved) {
                gameManager.removeMinefield(mf);
            }
        }
    }

    private void reportFiring(Vector<Report> reports, ArtilleryAttackAction aaa) {
        Report report = new Report(3121).indent().noNL().subject(subjectId);
        report.add(wtype.getName());
        report.add(aaa.getTurnsTilHit());
        reports.addElement(report);
        Report.addNewline(reports);

        Player owner = game.getPlayer(aaa.getPlayerId());
        int landingRound = game.getRoundCount() + aaa.getTurnsTilHit();
        String message = "Orbital Bombardment incoming, landing this round, fired by " + owner.getName();
        SpecialHexDisplay incomingMarker = SpecialHexDisplay.createIncomingFire(owner, landingRound, message);
        game.getBoard(target).addSpecialHexDisplay(target.getPosition(), incomingMarker);
    }

    private Optional<Entity> findSpotter(List<Integer> spottersBefore, int playerId) {
        Entity bestSpotter = null;

        // Are there any valid spotters?
        if (null != spottersBefore) {
            // fetch possible spotters now
            Iterator<Entity> spottersAfter = game.getSelectedEntities(entity -> {
                Integer id = entity.getId();
                return (playerId == entity.getOwnerId())
                      && spottersBefore.contains(id)
                      && !LosEffects.calculateLOS(game, entity, target, true).isBlocked()
                      && entity.isActive()
                      // airborne aeros can't spot for arty
                      && !(entity.isAero() && entity.isAirborne())
                      && !entity.isINarcedWith(INarcPod.HAYWIRE);
            });

            // Out of any valid spotters, pick the best.
            while (spottersAfter.hasNext()) {
                Entity spotter = spottersAfter.next();
                if (bestSpotter == null) {
                    bestSpotter = spotter;
                } else if (isForwardObserver(spotter) && !isForwardObserver(bestSpotter)) {
                    bestSpotter = spotter;
                } else if (spotter.getCrew().getGunnery() < bestSpotter.getCrew().getGunnery()
                      && !isForwardObserver(bestSpotter)) {
                    bestSpotter = spotter;
                } else if (isForwardObserver(bestSpotter) && isForwardObserver(spotter)) {
                    if (spotter.getCrew().getGunnery() < bestSpotter.getCrew().getGunnery()) {
                        bestSpotter = spotter;
                    }
                }
            }
        }
        return Optional.ofNullable(bestSpotter);
    }

    /**
     * @param entity The unit in question
     * @return True when the given unit has the Forward Observer ability
     * @see OptionsConstants#MISC_FORWARD_OBSERVER
     */
    public static boolean isForwardObserver(Entity entity) {
        return entity.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER);
    }
}
