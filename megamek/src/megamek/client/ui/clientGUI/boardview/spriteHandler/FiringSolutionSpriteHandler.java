/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.clientGUI.boardview.spriteHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import megamek.client.Client;
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FiringSolutionSprite;
import megamek.common.units.Entity;
import megamek.common.units.EntityVisibilityUtils;
import megamek.common.game.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.util.FiringSolution;

public class FiringSolutionSpriteHandler extends BoardViewSpriteHandler implements IPreferenceChangeListener {

    private final Client client;
    private final Game game;

    // Cache the entity; thus, when firing solutions are turned on the sprites can easily be created
    private Entity currentEntity;

    public FiringSolutionSpriteHandler(AbstractClientGUI clientGUI, Client client) {
        super(clientGUI);
        this.client = client;
        this.game = client.getGame();
    }

    public void showFiringSolutions(Entity entity) {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
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

        IBoardView boardView = clientGUI.getBoardView(entity);
        solutions.values().stream()
              .map(sln -> new FiringSolutionSprite((BoardView) boardView, sln))
              .forEach(currentSprites::add);
        boardView.addSprites(currentSprites);
    }

    protected boolean shouldShowTarget(Entity target, Entity ce) {
        boolean friendlyFire = client.getGame().getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
        boolean enemyTarget = target.getOwner().isEnemyOf(ce.getOwner());
        boolean friendlyFireOrEnemyTarget = friendlyFire || enemyTarget;
        boolean NotEnemyTargetOrVisible = !enemyTarget
              || EntityVisibilityUtils.detectedOrHasVisual(client.getLocalPlayer(), client.getGame(), target);
        return (target.getId() != ce.getId())
              && friendlyFireOrEnemyTarget
              && NotEnemyTargetOrVisible
              && game.onTheSameBoard(ce, target)
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
