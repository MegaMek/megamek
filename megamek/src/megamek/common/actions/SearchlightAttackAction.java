/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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


package megamek.common.actions;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.common.ComputeArc;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.LosEffects;
import megamek.common.Report;
import megamek.common.Tank;
import megamek.common.Targetable;

/**
 * Used for aiming a searchlight at a target.
 */
public class SearchlightAttackAction extends AbstractAttackAction {

    /**
     *
     */
    private static final long serialVersionUID = 6699459935811592434L;

    // default to attacking an entity
    public SearchlightAttackAction(int entityId, int targetId) {
        super(entityId, targetId);
    }

    public SearchlightAttackAction(int entityId, int targetType, int targetId) {
        super(entityId, targetType, targetId);
    }

    public boolean isPossible(Game game) {
        return SearchlightAttackAction.isPossible(game,
              getEntityId(),
              game.getTarget(getTargetType(), getTargetId()),
              this);
    }

    public static boolean isPossible(Game game, int attackerId, Targetable target, SearchlightAttackAction exempt) {
        final Entity attacker = game.getEntity(attackerId);

        // can't light up if either you or the target don't exist, or you don't have your light on
        if ((attacker == null) || !attacker.isUsingSearchlight() || (target == null)) {
            return false;
        }

        // can't light up if you're stunned
        if ((attacker instanceof Tank) && (((Tank) attacker).getStunnedTurns() > 0)) {
            return false;
        }

        // can't searchlight if target is outside of the front firing arc
        if (!ComputeArc.isInArc(attacker.getPosition(),
              attacker.getSecondaryFacing(),
              target,
              attacker.getForwardArc())) {
            return false;
        }

        // can't light up more than once per round
        for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
            EntityAction action = actions.nextElement();
            if (action instanceof SearchlightAttackAction) {
                SearchlightAttackAction act = (SearchlightAttackAction) action;
                if (act == exempt) {
                    break; // 1st in list is OK
                }
                if (act.getEntityId() == attackerId) {
                    return false; // can only declare searchlight once!
                }
            }
        }

        // per TacOps, integrated searchlights have max range of 30 hexes
        // hand-held ones have a max range of 10 hexes, but are not implemented
        if (attacker.getPosition().distance(target.getPosition()) > 30) {
            return false;
        }

        // can't light up if out of LOS. Most expensive calculation, so keep it last
        return LosEffects.calculateLOS(game, attacker, target).canSee();
    }

    /**
     * illuminate an entity and all entities that are between us and the hex
     */
    public Vector<Report> resolveAction(Game game) {
        Vector<Report> reports = new Vector<>();
        Report r;
        if (!isPossible(game)) {
            r = new Report(3445);
            r.subject = getEntityId();
            r.newlines = 1;
            reports.addElement(r);
            return reports;
        }
        final Entity attacker = getEntity(game);
        final Coords apos = attacker.getPosition();
        final Targetable target = getTarget(game);
        final Coords tpos = target.getPosition();

        if (attacker.usedSearchlight()) {
            r = new Report(3450);
            r.subject = getEntityId();
            r.add(attacker.getDisplayName());
            r.newlines = 1;
            reports.addElement(r);
            return reports;
        }
        attacker.setUsedSearchlight(true);

        ArrayList<Coords> in = Coords.intervening(apos, tpos); // nb includes
        // attacker &
        // target
        for (Coords c : in) {
            for (Entity en : game.getEntitiesVector(c)) {
                LosEffects los = LosEffects.calculateLOS(game, attacker, en);
                if (los.canSee()) {
                    en.setIlluminated(true);
                    r = new Report(3455);
                    r.subject = getEntityId();
                    r.newlines = 1;
                    r.add(en.getDisplayName());
                    r.add(attacker.getDisplayName());
                    reports.addElement(r);
                }
            }
        }
        return reports;
    }

    /**
     * Updates the supplied Game's list of hexes illuminated.
     *
     * @param game The {@link Game} to update
     *
     * @return True if new hexes were added, else false.
     */
    public boolean setHexesIlluminated(Game game) {
        boolean hexesAdded = false;

        final Entity attacker = getEntity(game);
        final Coords apos = attacker.getPosition();
        final Targetable target = getTarget(game);
        final Coords tpos = target.getPosition();

        ArrayList<Coords> intervening = Coords.intervening(apos, tpos);
        for (Coords c : intervening) {
            if (game.getBoard().contains(c)) {
                hexesAdded |= game.addIlluminatedPosition(c);
            }
        }
        return hexesAdded;
    }

    public boolean willIlluminate(Game game, Entity who) {
        if (!isPossible(game)) {
            return false;
        }
        final Entity attacker = getEntity(game);
        final Coords apos = attacker.getPosition();
        final Targetable target = getTarget(game);
        final Coords tpos = target.getPosition();

        ArrayList<Coords> in = Coords.intervening(apos, tpos); // nb includes
        // attacker &
        // target
        for (Coords c : in) {
            for (Entity en : game.getEntitiesVector(c)) {
                LosEffects los = LosEffects.calculateLOS(game, attacker, en);
                if (los.canSee() && en.equals(who)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toSummaryString(final Game game) {
        Entity target = game.getEntity(this.getTargetId());
        return Messages.getString("BoardView1.SearchlightAttackAction") + ((target != null) ?
              ' ' + target.getShortName() :
              "");
    }
}
