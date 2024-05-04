/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.boardview;

import megamek.client.Client;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.FiringSolution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FiringSolutionSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {

    private final Client client;
    private final Game game;

    // Cache the entity; thus, when firing solutions are turned on the sprites can easily be created
    private Entity currentEntity;

    public FiringSolutionSpriteHandler(BoardView boardView, Client client) {
        super(boardView);
        this.client = client;
        this.game = client.getGame();
    }

    public void showFiringSolutions(Entity entity) {
        clear();
        currentEntity = entity;
        if ((entity == null) || (entity.getId() == Entity.NONE) || !GUIP.getShowFiringSolutions()) {
            return;
        }

        // Determine which entities are spotted
        Set<Integer> spottedEntities = new HashSet<>();
        for (Entity spotter : game.getEntitiesVector()) {
            if (!spotter.isEnemyOf(entity) && spotter.isSpotting()) {
                spottedEntities.add(spotter.getSpotTargetId());
            }
        }

        // Calculate firing solutions
        Map<Integer, FiringSolution> solutions = new HashMap<>();
        for (Entity target : game.getEntitiesVector()) {
            if (shouldShowTarget(target, entity)) {
                ToHitData thd = WeaponAttackAction.toHit(game, entity.getId(), target);
                thd.setLocation(target.getPosition());
                thd.setRange(entity.getPosition().distance(target.getPosition()));
                solutions.put(target.getId(), new FiringSolution(thd, spottedEntities.contains(target.getId())));
            }
        }

        solutions.values().stream().map(sln -> new FiringSolutionSprite(boardView, sln)).forEach(currentSprites::add);
        boardView.addSprites(currentSprites);
    }

    protected boolean shouldShowTarget(Entity target, Entity ce) {
        boolean friendlyFire = client.getGame().getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
        boolean enemyTarget = target.getOwner().isEnemyOf(ce.getOwner());
        boolean friendlyFireOrEnemyTarget =  friendlyFire || enemyTarget;
        boolean NotEnemyTargetOrVisible = !enemyTarget
                || EntityVisibilityUtils.detectedOrHasVisual(client.getLocalPlayer(), client.getGame(), target);
        return (target.getId() != ce.getId())
                && friendlyFireOrEnemyTarget
                && NotEnemyTargetOrVisible
                && target.isTargetable();
    }

    @Override
    public void clear() {
        super.clear();
        currentEntity = null;
    }

    @Override
    public void initialize() {
        GUIP.addPreferenceChangeListener(this);
    }

    @Override
    public void dispose() {
        clear();
        GUIP.removePreferenceChangeListener(this);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        if (e.getName().equals(GUIPreferences.FIRING_SOLUTIONS)) {
            showFiringSolutions(currentEntity);
        }
    }
}
