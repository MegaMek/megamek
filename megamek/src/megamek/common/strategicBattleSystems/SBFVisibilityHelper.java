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

package megamek.common.strategicBattleSystems;

import static megamek.common.strategicBattleSystems.SBFVisibilityStatus.INVISIBLE;
import static megamek.common.strategicBattleSystems.SBFVisibilityStatus.VISIBLE;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import megamek.common.game.InGameObject;
import megamek.common.Player;
import megamek.common.interfaces.ServerOnly;
import megamek.common.Team;
import megamek.common.options.SBFRuleOptions;
import megamek.logging.MMLogger;

//TODO: use for hidden formations?

/**
 * This class holds visibility information for an SBF game. In SBF games, visibility is not instantaneous but checked
 * and updated in the Detection and Recon phase, see IO:BF p.194. Note that this class checks the game options by itself
 * and will always indicate that formations are visible when detection and recon is not used.
 * <p>
 * Note that while this class is used by SBFGame (as it needs to be saved with the game) it should only be used by the
 * server (SBFGameManager). The Clients must accept what they get from the server as a result of testing visibility.
 */
public final class SBFVisibilityHelper implements SBFRuleOptionsUser {
    private static final MMLogger logger = MMLogger.create(SBFVisibilityHelper.class);

    /**
     * This map holds the visibility status. It maps a player ID to an inner map having pairs of formation ID and
     * visibility status. The default value if no entry can be found is INVISIBLE when double blind rules are in use
     * (Detection and Recon, IO:BF p.195), VISIBLE otherwise.
     */
    private final Map<Integer, Map<Integer, SBFVisibilityStatus>> visibilityMap = new HashMap<>();

    private final SBFGame game;

    public SBFVisibilityHelper(SBFGame game) {
        this.game = game;
    }

    /**
     * Returns true when the given formation is fully visible to the given player, depending on the game's options. When
     * double blind rules are not used, this is always true. Otherwise it depends on previous detection. Players always
     * see their own units.
     *
     * @param viewingPlayer the player that is doing the looking
     * @param formationID   the formation in question
     *
     * @return True when the formation is fully visible, false otherwise
     */
    @ServerOnly
    public boolean isVisible(int viewingPlayer, int formationID) {
        return getVisibility(viewingPlayer, formationID) == VISIBLE;
    }

    /**
     * Returns the visibility status for the given formation to the given player, depending on the game's options. When
     * double blind rules are not used, the status is always {@link SBFVisibilityStatus#VISIBLE}. Otherwise it depends
     * on previous detection. Players always see their own units.
     *
     * @param viewingPlayer the player that is doing the looking
     * @param formationID   the formation in question
     *
     * @return The {@link SBFVisibilityStatus}
     *
     * @see SBFVisibilityStatus
     * @see #isVisible(int, int)
     */
    @ServerOnly
    public SBFVisibilityStatus getVisibility(int viewingPlayer, int formationID) {
        if (!usesDoubleBlind()) {
            return VISIBLE;
        } else {
            return getBestVisibilityFor(getPlayersToCheck(viewingPlayer), formationID);
        }
    }

    /**
     * Sets the given formation as fully visible to the given player.
     * <p>
     * Note: that the ID values are not checked; when they don't map to anything, no error will happen and they will
     * have no effect.
     *
     * @param viewingPlayer The player
     * @param formationID   The formation to make visible
     */
    @ServerOnly
    public void setVisible(int viewingPlayer, int formationID) {
        setVisibility(viewingPlayer, formationID, VISIBLE);
    }

    /**
     * Sets the given formation as invisible to the given player.
     * <p>
     * Note: that the ID values are not checked; when they don't map to anything, no error will happen and they will
     * have no effect.
     *
     * @param viewingPlayer The player
     * @param formationID   The formation to make invisible
     */
    @ServerOnly
    public void setInvisible(int viewingPlayer, int formationID) {
        Map<Integer, SBFVisibilityStatus> playerMap = visibilityMap.get(viewingPlayer);
        if (playerMap != null) {
            playerMap.remove(formationID);
        }
    }

    /**
     * Sets the given {@link SBFVisibilityStatus} for the given formation as viewed by the given player.
     * <p>
     * Note: that the ID values are not checked; when they don't map to anything, no error will happen and they will
     * have no effect.
     *
     * @param viewingPlayer    The player
     * @param formationID      The formation
     * @param visibilityStatus The new visibility status
     *
     * @see #setVisible(int, int)
     * @see #setInvisible(int, int)
     */
    @ServerOnly
    public void setVisibility(int viewingPlayer, int formationID, SBFVisibilityStatus visibilityStatus) {
        if (!usesDoubleBlind()) {
            logger.warn("Setting visibility in a non-double blind game.");
        }
        if (visibilityStatus == INVISIBLE) {
            setInvisible(viewingPlayer, formationID);
        } else {
            Map<Integer, SBFVisibilityStatus> playerMap = visibilityMap.computeIfAbsent(viewingPlayer,
                  k -> new HashMap<>());
            playerMap.put(formationID, visibilityStatus);
        }
    }

    // region PRIVATE

    private SBFVisibilityStatus getBestVisibilityFor(Collection<Player> players, int formationID) {
        return players.stream()
              .map(Player::getId)
              .map(id -> getVisibilityImpl(id, formationID))
              .max(Comparator.comparing(Enum::ordinal))
              .orElse(INVISIBLE);
    }

    @Override
    public SBFRuleOptions getOptions() {
        return game.getOptions();
    }

    /**
     * Returns the players to test visibility on; this is the viewing player alone unless the rule that teams share
     * their vision is used, when it is all players of the viewing player's team. The list can be empty when the viewing
     * player id does not match a player.
     *
     * @param viewingPlayer The viewing player's id
     *
     * @return A list of players who share their vision
     */
    private Set<Player> getPlayersToCheck(int viewingPlayer) {
        Set<Player> playersToCheck = new HashSet<>();
        Player player = game.getPlayer(viewingPlayer);
        if (player == null) {
            logger.error("Player {} does not exist.", viewingPlayer);
        } else {
            playersToCheck.add(player);
            if (usesTeamVision()) {
                Team team = game.getTeamForPlayer(player);
                if (team == null) {
                    logger.error("Player {} is not on a team.", viewingPlayer);
                } else {
                    playersToCheck.addAll(team.players());
                }
            }
        }
        return playersToCheck;
    }

    private SBFVisibilityStatus getVisibilityImpl(int viewingPlayer, int formationID) {
        if (isOwner(viewingPlayer, formationID)) {
            return VISIBLE;
        } else {
            Map<Integer, SBFVisibilityStatus> playerMap = visibilityMap.get(viewingPlayer);
            if (playerMap == null) {
                return INVISIBLE;
            } else {
                return playerMap.getOrDefault(formationID, INVISIBLE);
            }
        }
    }

    private boolean isOwner(int viewingPlayer, int formationID) {
        Optional<InGameObject> unit = game.getInGameObject(formationID);
        return unit.isPresent() && (unit.get().getOwnerId() == viewingPlayer) && (viewingPlayer != Player.PLAYER_NONE);
    }

    // endregion
}
