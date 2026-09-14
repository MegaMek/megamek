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
package megamek.common.actions;

import java.io.Serial;

import megamek.client.ui.Messages;
import megamek.common.HexTarget;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * A unit's order to scan something with its sensors this turn: a hex, a building hex or another unit. Declared in
 * the Firing phase beside the attacks and resolved in the End Phase, where the scan is rolled and the result
 * reported - a banked reading when there was an objective to read, "nothing of interest" when there was not. One
 * per unit per turn; a later order replaces an earlier one.
 */
public class ScanAction extends AbstractEntityAction {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int targetType;
    private final int targetId;
    private final Coords targetPosition;
    private final int boardId;

    /**
     * An order to scan a unit.
     *
     * @param entityId the scanning unit
     * @param targetId the unit to scan
     */
    public ScanAction(int entityId, int targetId) {
        super(entityId);
        this.targetType = Targetable.TYPE_ENTITY;
        this.targetId = targetId;
        this.targetPosition = null;
        this.boardId = 0;
    }

    /**
     * An order to scan a hex, or the building standing in it.
     *
     * @param entityId       the scanning unit
     * @param targetPosition the hex to scan
     * @param boardId        the board the hex is on
     */
    public ScanAction(int entityId, Coords targetPosition, int boardId) {
        super(entityId);
        this.targetType = Targetable.TYPE_HEX_CLEAR;
        this.targetId = Entity.NONE;
        this.targetPosition = targetPosition;
        this.boardId = boardId;
    }

    /** @return {@code true} when the order names a unit rather than a hex */
    public boolean isUnitTarget() {
        return targetType == Targetable.TYPE_ENTITY;
    }

    /** @return the scanned unit's id, or {@link Entity#NONE} for a hex */
    public int getTargetId() {
        return targetId;
    }

    /** @return the scanned hex, or {@code null} for a unit */
    public @Nullable Coords getTargetPosition() {
        return targetPosition;
    }

    /** @return the board the scanned hex is on; meaningless for a unit target */
    public int getBoardId() {
        return boardId;
    }

    /**
     * @param game the game to look the target up in
     *
     * @return the order's target as something the line-of-sight and rules code can work with, or {@code null} when a
     *       unit target no longer exists
     */
    public @Nullable Targetable resolveTarget(Game game) {
        if (isUnitTarget()) {
            return game.getEntity(targetId);
        }
        return new HexTarget(targetPosition, boardId, Targetable.TYPE_HEX_CLEAR);
    }

    /**
     * @param game the game to look the target up in
     *
     * @return the hex the order points at: the unit's current hex, or the hex itself; {@code null} when a unit
     *       target no longer exists or has no position
     */
    public @Nullable Coords resolveTargetPosition(Game game) {
        Targetable target = resolveTarget(game);
        return (target == null) ? null : target.getPosition();
    }

    @Override
    public String toSummaryString(final Game game) {
        if (isUnitTarget()) {
            Entity target = game.getEntity(targetId);
            return Messages.getString("BoardView1.ScanAction", (target != null) ? target.getShortName() : "");
        }
        return Messages.getString("BoardView1.ScanAction", (targetPosition != null) ? targetPosition.getBoardNum() : "");
    }
}
