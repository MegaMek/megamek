/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.client.ui.swing.tooltip.EntityActionLog;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.options.OptionsConstants;
import megamek.common.util.FiringSolution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AttackPhaseDisplay extends ActionPhaseDisplay {
    // client list of attacks user has input
    protected EntityActionLog attacks;

    protected AttackPhaseDisplay(ClientGUI cg) {
        super(cg);
        attacks = new EntityActionLog(clientgui.getClient().getGame());
    }

    /**
     * called by updateDonePanel to populate the label text of the Done button. Usually wraps a call to Messages.getString(...Fire)
     * but can be extended to add more details.
     * @return text for label
     */
    abstract protected String getDoneButtonLabel();

    /**
     * called by updateDonePanel to populate the label text of the NoAction button. Usually wraps a call to Messages.getString(...Skip)
     * but can be extended to add more details.
     * @return text for label
     */
    abstract protected String getSkipTurnButtonLabel();


    @Override
    protected void updateDonePanel()
    {
        if (attacks.isEmpty() || (attacks.size() == 1 && attacks.firstElement() instanceof TorsoTwistAction)){
            // a torso twist alone should not trigger Done button
            updateDonePanelButtons(getDoneButtonLabel(), getSkipTurnButtonLabel(), false, null);
        } else {
            updateDonePanelButtons(getDoneButtonLabel(), getSkipTurnButtonLabel(), true, attacks.getDescriptions());
        }
    }

    protected void removeAttack(Object o)
    {
        attacks.remove(o);
        updateDonePanel();
    }

    /** removes all elements from the local temporary attack list */
    protected void removeAllAttacks()
    {
        attacks.clear();
        updateDonePanel();
    }

    /** add an attack at the given index to the local temporary attack list */
    protected void addAttack(int index, EntityAction entityAction)
    {
        attacks.add(index, entityAction);
        updateDonePanel();
    }

    /** add an attack to the end of the local temporary attack list */
    protected void addAttack(EntityAction entityAction)
    {
        attacks.add(entityAction);
        updateDonePanel();
    }

    public void setFiringSolutions(Entity ce) {
        // If no Entity is selected, exit
        if (ce.getId() == Entity.NONE) {
            return;
        }

        Game game = clientgui.getClient().getGame();
        Player localPlayer = clientgui.getClient().getLocalPlayer();
        if (!GUIP.getFiringSolutions()) {
            return;
        }

        // Determine which entities are spotted
        Set<Integer> spottedEntities = new HashSet<>();
        for (Entity target : game.getEntitiesVector()) {
            if (!target.isEnemyOf(ce) && target.isSpotting()) {
                spottedEntities.add(target.getSpotTargetId());
            }
        }

        // Calculate firing solutions
        Map<Integer, FiringSolution> fs = new HashMap<>();
        for (Entity target : game.getEntitiesVector()) {
            if (shouldShowTarget(game, localPlayer, target, ce)) {
                ToHitData thd = WeaponAttackAction.toHit(game, ce.getId(), target);
                thd.setLocation(target.getPosition());
                thd.setRange(ce.getPosition().distance(target.getPosition()));
                fs.put(target.getId(), new FiringSolution(thd, spottedEntities.contains(target.getId())));
            }
        }
        clientgui.getBoardView().setFiringSolutions(ce, fs);
    }

    protected boolean shouldShowTarget(Game game, Player localPlayer, Entity target, Entity ce) {
        boolean friendlyFire = game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
        boolean enemyTarget = target.getOwner().isEnemyOf(ce.getOwner());
        boolean friendlyFireOrEnemyTarget =  friendlyFire || enemyTarget;
        boolean NotEnemyTargetOrVisible = !enemyTarget
                || EntityVisibilityUtils.detectedOrHasVisual(localPlayer, game, target);
        return (target.getId() != ce.getId())
                && friendlyFireOrEnemyTarget
                && NotEnemyTargetOrVisible
                && target.isTargetable();
    }
}
