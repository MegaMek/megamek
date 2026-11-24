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
package megamek.client.ui.clientGUI;

import java.awt.Color;

import jakarta.annotation.Nullable;
import megamek.common.Player;
import megamek.common.game.IGame;

/**
 * Decides which colour a unit is drawn in on the minimap and in its board label border.
 *
 * <p>With the team coloring setting on, units are drawn from the viewer's point of view: own units, allied
 * units and enemy units each get a fixed colour. That only makes sense for a viewer who is on a side. An
 * observer, a Game Master who owns no units, or a view with no local player is on no side, so every unit would
 * come out as an enemy; such a viewer gets each owner's own lobby colour instead. With the setting off, every
 * viewer gets the owner's lobby colour.</p>
 */
public final class UnitOwnerColors {

    /**
     * The three colours used when team coloring applies.
     *
     * @param ownColor   the colour for the viewer's own units
     * @param allyColor  the colour for units of the viewer's teammates
     * @param enemyColor the colour for everyone else's units
     */
    public record TeamColors(Color ownColor, Color allyColor, Color enemyColor) {

        /**
         * @return the own, ally and enemy colours currently set in the client settings
         */
        public static TeamColors fromPreferences() {
            GUIPreferences preferences = GUIPreferences.getInstance();
            return new TeamColors(preferences.getMyUnitColor(),
                  preferences.getAllyUnitColor(),
                  preferences.getEnemyUnitColor());
        }
    }

    private UnitOwnerColors() {}

    /**
     * Returns the colour for a unit owned by {@code owner}, using the team coloring setting and the own, ally
     * and enemy colours from the client settings.
     *
     * @param owner       the player who owns the unit
     * @param localPlayer the player viewing the board, or {@code null} when there is none (e.g. the board
     *                    editor)
     * @param game        the game being shown, used to tell whether a Game Master owns any units
     *
     * @return the colour to draw the unit in
     */
    public static Color colorFor(Player owner, @Nullable Player localPlayer, IGame game) {
        boolean isTeamColoringApplicable = GUIPreferences.getInstance().getTeamColoring()
              && isViewerOnASide(localPlayer, game);
        return colorFor(owner, localPlayer, isTeamColoringApplicable, TeamColors.fromPreferences());
    }

    /**
     * Returns the colour for a unit owned by {@code owner}.
     *
     * @param owner                    the player who owns the unit
     * @param localPlayer              the player viewing the board, or {@code null} when there is none
     * @param isTeamColoringApplicable {@code true} when the team coloring setting is on and the viewer is on a
     *                                 side (see {@link #isViewerOnASide(Player, IGame)})
     * @param teamColors               the own, ally and enemy colours used when team coloring applies
     *
     * @return the owner's lobby colour when team coloring does not apply, otherwise the own, ally or enemy
     *       colour from {@code teamColors}
     */
    public static Color colorFor(Player owner, @Nullable Player localPlayer, boolean isTeamColoringApplicable,
          TeamColors teamColors) {
        if (!isTeamColoringApplicable || (localPlayer == null)) {
            return owner.getDisplayColour().getColour(false);
        }
        if (owner.equals(localPlayer)) {
            return teamColors.ownColor();
        }
        if (owner.getTeam() == localPlayer.getTeam()) {
            return teamColors.allyColor();
        }
        return teamColors.enemyColor();
    }

    /**
     * Returns whether the viewer is playing a side, so that own, ally and enemy colours make sense to them. An
     * observer is not, and neither is a Game Master who owns no units: the server never marks a Game Master as
     * an observer, so without this check a refereeing Game Master would see every unit as an enemy.
     *
     * @param localPlayer the player viewing the board, or {@code null} when there is none
     * @param game        the game being shown
     *
     * @return {@code true} if the viewer is on a side, {@code false} for no viewer, an observer, or a Game
     *       Master who owns no units
     */
    public static boolean isViewerOnASide(@Nullable Player localPlayer, IGame game) {
        if ((localPlayer == null) || localPlayer.isObserver()) {
            return false;
        }
        boolean isRefereeingGameMaster = localPlayer.isGameMaster() && (game.getEntitiesOwnedBy(localPlayer) == 0);
        return !isRefereeingGameMaster;
    }
}
