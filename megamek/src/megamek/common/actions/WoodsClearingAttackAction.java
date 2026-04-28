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
import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import megamek.common.units.Terrains;

/**
 * Represents a unit using a chainsaw or dual saw to clear woods from a hex.
 *
 * <p>Per TM pp.241-243, a chainsaw or dual saw takes 2 turns to reduce a wooded hex
 * one level (heavy to light, light to rough). Two units clearing the same hex reduce this to 1 turn. While clearing,
 * the unit must remain in the hex or an adjacent hex, and weapon attacks are penalized as though moving at
 * running/flank speed.</p>
 *
 * <p>This action is declared during the physical phase.</p>
 */
public class WoodsClearingAttackAction extends AbstractAttackAction {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int sawEquipmentId;
    private final Coords targetCoords;
    private final int targetBoardId;

    /**
     * Creates a new woods clearing action.
     *
     * @param entityId       the ID of the entity performing the clearing
     * @param targetType     the target type (should be Targetable.TYPE_HEX_CLEAR)
     * @param targetId       the target ID
     * @param sawEquipmentId the equipment number of the saw being used
     * @param targetCoords   the coordinates of the hex being cleared
     * @param targetBoardId  the board ID of the hex being cleared
     */
    public WoodsClearingAttackAction(int entityId, int targetType, int targetId,
          int sawEquipmentId, Coords targetCoords, int targetBoardId) {
        super(entityId, targetType, targetId);
        this.sawEquipmentId = sawEquipmentId;
        this.targetCoords = targetCoords;
        this.targetBoardId = targetBoardId;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public int getSawEquipmentId() {
        return sawEquipmentId;
    }

    public Coords getTargetCoords() {
        return targetCoords;
    }

    public int getTargetBoardId() {
        return targetBoardId;
    }

    /**
     * Woods clearing is automatic (no roll needed) but may be impossible.
     *
     * @param game the current game
     *
     * @return AUTOMATIC_SUCCESS if clearing is valid, IMPOSSIBLE otherwise
     */
    public ToHitData toHit(Game game) {
        Entity ae = getEntity(game);
        Targetable target = getTarget(game);

        if (ae == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker not found");
        }
        if (target == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target not found");
        }

        ToHitData validation = canClearWoods(game, ae, target.getPosition(), targetBoardId);
        if (validation != null) {
            return validation;
        }

        return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, "clearing woods with saw");
    }

    /**
     * Checks if the given entity can clear woods at the target hex.
     *
     * @param game         the current game
     * @param entity       the entity attempting to clear
     * @param targetCoords the hex coordinates to clear
     *
     * @return a ToHitData with IMPOSSIBLE if clearing is not valid, or null if it is valid
     */
    public static ToHitData canClearWoods(Game game, Entity entity, Coords targetCoords, int boardId) {
        if (entity == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Entity is null");
        }
        if (targetCoords == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target hex is null");
        }

        // Entity must have a working chainsaw or dual saw
        if (!hasWorkingSaw(entity)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "No working chainsaw or dual saw");
        }

        // Entity must be in or adjacent to the target hex
        Coords entityPos = entity.getPosition();
        if (entityPos == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Entity has no position");
        }
        int distance = entityPos.distance(targetCoords);
        if (distance > 1) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target hex is not adjacent");
        }

        // Target hex must have woods or jungle
        Hex targetHex = game.getBoard(boardId).getHex(targetCoords);
        if (targetHex == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target hex not found");
        }
        if (!targetHex.containsTerrain(Terrains.WOODS) && !targetHex.containsTerrain(Terrains.JUNGLE)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target hex has no woods or jungle");
        }

        // Entity must not be prone
        if (entity.isProne()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot clear woods while prone");
        }

        // Entity must not be immobile
        if (entity.isImmobile()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot clear woods while immobile");
        }

        // Target hex must be in the saw's attack arc (only matters for adjacent hexes)
        if (distance == 1 && !isInSawArc(entity, targetCoords)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target hex is not in the saw's attack arc");
        }

        return null;
    }

    /**
     * Checks if the entity has a working chainsaw or dual saw.
     *
     * @param entity the entity to check
     *
     * @return true if the entity has a functional saw
     */
    public static boolean hasWorkingSaw(Entity entity) {
        return entity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_CHAINSAW)
              || entity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_DUAL_SAW);
    }

    /**
     * Checks if a target hex is within the attack arc of at least one of the entity's working saws. The arc is
     * determined by the saw's mounting location, following the same logic as club attacks.
     *
     * @param entity       the entity with the saw
     * @param targetCoords the target hex coordinates
     *
     * @return true if the target is in the arc of at least one working saw
     */
    public static boolean isInSawArc(Entity entity, Coords targetCoords) {
        for (MiscMounted misc : entity.getMisc()) {
            if (!misc.isReady()) {
                continue;
            }
            if (!misc.getType().hasFlag(MiscType.F_CLUB)) {
                continue;
            }
            if (!misc.getType().hasFlag(MiscTypeFlag.S_CHAINSAW)
                  && !misc.getType().hasFlag(MiscTypeFlag.S_DUAL_SAW)) {
                continue;
            }

            // Determine the arc based on mounting location (same logic as ClubAttackAction)
            int sawArc;
            int location = misc.getLocation();
            if (location == Mek.LOC_LEFT_ARM) {
                sawArc = Compute.ARC_LEFT_ARM;
            } else if (location == Mek.LOC_RIGHT_ARM) {
                sawArc = Compute.ARC_RIGHT_ARM;
            } else if (misc.isRearMounted()) {
                sawArc = Compute.ARC_REAR;
            } else {
                sawArc = Compute.ARC_FORWARD;
            }

            if (ComputeArc.isInArc(entity.getPosition(), entity.getSecondaryFacing(),
                  targetCoords, sawArc)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toSummaryString(final Game game) {
        return Messages.getString("BoardView1.WoodsClearingAction");
    }
}
