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

package megamek.common.turns;

import java.io.Serial;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.IntStream;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.game.IGame;
import megamek.common.units.Entity;

/**
 * A type of game turn that indicates that one or more players should be given the opportunity to unload entities that
 * are stranded on immobile transports. Each player declares which stranded units they will unload at the beginning of
 * the movement phase, without being told what stranded units their opponent(s) are unloading.
 * <p>
 * According to Randall Bills, the "minimum move" rule allow stranded units to dismount at the start of the turn [there
 * used to be an ancient link to a forum post here].
 */
public class UnloadStrandedTurn extends GameTurn {
    @Serial
    private static final long serialVersionUID = 2403095752478007872L;
    private final int[] entityIds;

    /**
     * Any player that owns an entity whose ID is in the past array should be given a chance to unload it.
     *
     * @param ids the array of <code>int</code> IDs of stranded entities. This value must not be
     *            <code>null</code> or empty.
     *
     * @throws IllegalArgumentException if a <code>null</code> or empty value is passed for ids.
     */
    public UnloadStrandedTurn(int... ids) {
        super(Player.PLAYER_NONE);

        // Validate input.
        if (ids == null) {
            throw new IllegalArgumentException("the passed array of ids is null");
        } else if (ids.length == 0) {
            throw new IllegalArgumentException("the passed array of ids is empty");
        }

        // Create a copy of the array to prevent any post-call shenanigans.
        entityIds = new int[ids.length];
        System.arraycopy(ids, 0, entityIds, 0, ids.length);
    }

    /**
     * Any player that owns an entity in the past enumeration should be given a chance to unload it.
     *
     * @param entities the <code>Enumeration</code> of stranded entities. This value must not be
     *                 <code>null</code> or empty.
     *
     * @throws IllegalArgumentException if a <code>null</code> or empty value is passed for entities.
     */
    public UnloadStrandedTurn(Iterator<Entity> entities) {
        super(Player.PLAYER_NONE);

        // Validate input.
        if (entities == null) {
            throw new IllegalArgumentException("the passed enumeration of entities is null");
        } else if (!entities.hasNext()) {
            throw new IllegalArgumentException("the passed enumeration of entities is empty");
        }

        // Get the first entity.
        Entity entity = entities.next();

        // Do we need to get more entities?
        if (entities.hasNext()) {
            // It's a bit of a hack, but get the Game from the first/ entity, and create a
            // temporary array that can hold the IDs of every entity in the game.
            int[] ids = new int[entity.getGame().getNoOfEntities()];
            int length = 0;

            // Store the first entity's ID.
            ids[length++] = entity.getId();

            // Walk the list of remaining stranded entities.
            while (entities.hasNext()) {
                ids[length++] = entities.next().getId();
            }

            // Create an array that just holds the stranded entity ids.
            entityIds = new int[length];
            System.arraycopy(ids, 0, entityIds, 0, length);
        } else {
            // There was only one stranded entity.
            entityIds = new int[1];
            entityIds[0] = entity.getId();
        }
    }

    public int[] getEntityIds() {
        return entityIds;
    }

    /**
     * Determine if the given entity is a valid one to use for this turn.
     *
     * @param entity the <code>Entity</code> being tested for the move.
     * @param game   The {@link Game} the entity belongs to
     *
     * @return <code>true</code> if the entity can be moved.
     */
    @Override
    public boolean isValidEntity(final @Nullable Entity entity, final Game game) {
        // Null entities don't need to be checked.
        if (entity == null) {
            return false;
        }

        // Check if any entity in the array is valid.
        final int entityId = entity.getId();
        return IntStream.range(0, entityIds.length).anyMatch(index -> entityId == entityIds[index]);
    }

    /**
     * @return true if the player and entity are both valid.
     */
    @Override
    public boolean isValid(final int playerId, final @Nullable Entity entity, final Game game) {
        return isValidEntity(entity, game) && (entity.getOwnerId() == playerId);
    }

    /**
     * @return true if the player is valid.
     */
    @Override
    public boolean isValid(int playerId, IGame game) {
        Game actualGame = (Game) game;
        return IntStream.range(0, entityIds.length)
              .anyMatch(index -> (actualGame.getEntity(entityIds[index]) != null)
                    && (playerId == Objects.requireNonNull(actualGame.getEntity(entityIds[index]))
                    .getOwnerId()));
    }

    @Override
    public String toString() {
        return super.toString() + ", Entity IDs: [" + Arrays.toString(entityIds) + ']';
    }
}
