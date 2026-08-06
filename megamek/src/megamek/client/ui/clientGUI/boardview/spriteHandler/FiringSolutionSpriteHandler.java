/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import megamek.client.Client;
import megamek.client.ui.clientGUI.AbstractClientGUI;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.clientGUI.boardview.sprite.FiringSolutionSprite;
import megamek.common.HexTarget;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType.Munitions;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingTarget;
import megamek.common.units.Entity;
import megamek.common.units.EntityVisibilityUtils;
import megamek.common.units.Targetable;
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

    public void showFiringSolutions(Entity entity, Optional<WeaponMounted> weapon, Optional<AmmoMounted> ammo) {
        clear();
        if (clientGUI.boardViews().isEmpty()) {
            return;
        }
        currentEntity = entity;
        if ((entity == null)
              || (entity.getId() == Entity.NONE)
              || (!GUIP.getShowFiringSolutions())
              || (entity.getPosition() == null)) {
            return;
        }
        boolean narcCapableAmmo =
              (ammo.isPresent() &&
                    (EnumSet.of(Munitions.M_NARC_CAPABLE, Munitions.M_ARAD).containsAll(
                          ammo.get().getType().getMunitionType())));

        // Determine which entities are spotted / Narc'ed
        HashMap<Entity, Entity> spottedEntities = new HashMap<>();
        for (Entity spotter : game.getEntitiesVector()) {
            // Targets spotted by other entities should be "visible" in this view
            if (!spotter.isEnemyOf(entity) && spotter.isSpotting()) {
                spottedEntities.put(game.getEntity(spotter.getSpotTargetId()), spotter);
            }

            // We consider entities with attached Narc pods to be "spotting" themselves (see toHit code)
            // They should also be "visible" as viable targets, if the attacker has compatible ammo.
            if (spotter.hasAnyTypeNarcPodsAttached() && narcCapableAmmo) {
                int teamId = currentEntity.getOwner().getTeam();
                if (spotter.isNarcedBy(teamId) || spotter.isINarcedBy(teamId)) {
                    spottedEntities.put(spotter, spotter);
                }
            }
        }

        // Calculate firing solutions, including weapon/ammo information
        Map<Integer, FiringSolution> solutions = new HashMap<>();
        for (Entity target : game.getEntitiesVector()) {
            if (shouldShowTarget(target, entity)) {
                Optional<Entity> spotter = Optional.ofNullable(spottedEntities.get(target));
                ToHitData thd = WeaponAttackAction.toHit(game, entity.getId(), weapon, ammo,
                      spotter, target);
                thd.setLocation(target.getPosition());
                thd.setRange(entity.getPosition().distance(target.getPosition()));
                solutions.put(target.getId(), new FiringSolution(thd, spotter.isPresent()));
            } else if (target instanceof AbstractBuildingEntity buildingEntity
                  && game.onTheSameBoard(entity, buildingEntity)
                  && (client.getGame().getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE)
                  || target.getOwner().isEnemyOf(entity.getOwner()))) {
                for (Coords buildingHex : buildingEntity.getCoordsList()) {
                    BuildingTarget buildingTarget = new BuildingTarget(buildingHex,
                          game.getBoard(buildingEntity.getBoardId()), Targetable.TYPE_BUILDING);
                    Optional<Entity> spotter = game.getEntitiesVector().stream()
                          .filter(s -> !s.isEnemyOf(entity) && s.isSpotting()
                                && s.getSpotTargetId() == buildingTarget.getId())
                          .findFirst();
                    ToHitData thd = WeaponAttackAction.toHit(game, entity.getId(), weapon, ammo,
                          spotter, buildingTarget);
                    thd.setLocation(buildingHex);
                    thd.setRange(entity.getPosition().distance(buildingHex));
                    solutions.put(Objects.hash(buildingEntity.getId(), buildingHex),
                          new FiringSolution(thd, spotter.isPresent()));
                }
            }
        }

        // Spotted hexes
        for (Entity spotter : game.getEntitiesVector()) {
            if (!spotter.isEnemyOf(entity) && spotter.isSpotting()
                  && game.getEntityFromAllSources(spotter.getSpotTargetId()) == null) {
                BoardLocation spotLocation = HexTarget.idToLocation(spotter.getSpotTargetId());
                Board board = game.getBoard(spotLocation.boardId());
                if (board == null || !board.contains(spotLocation.coords())
                      || board.getBuildingAt(spotLocation.coords()) != null) {
                    continue;
                }
                HexTarget hexTarget = new HexTarget(spotLocation, Targetable.TYPE_HEX_TAG);
                if (!game.onTheSameBoard(entity, hexTarget)) {
                    continue;
                }
                ToHitData thd = WeaponAttackAction.toHit(game, entity.getId(), weapon, ammo,
                      Optional.of(spotter), hexTarget);
                thd.setLocation(spotLocation.coords());
                thd.setRange(entity.getPosition().distance(spotLocation.coords()));
                solutions.put(hexTarget.getId(), new FiringSolution(thd, true));
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
            if (clientGUI instanceof ClientGUI nonAbstract) {
                showFiringSolutions(currentEntity, nonAbstract.getDisplayedWeapon(), nonAbstract.getDisplayedAmmo());
            } else {
                showFiringSolutions(currentEntity, Optional.empty(), Optional.empty());
            }
        }
    }
}
