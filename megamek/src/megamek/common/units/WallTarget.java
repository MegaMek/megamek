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

package megamek.common.units;

import java.io.Serial;
import java.util.Map;

import megamek.common.HexTarget;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.game.Game;

/** A specific hexside, resolved from either adjoining hex without losing its identity over the network. */
public final class WallTarget extends BuildingTarget {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Coords anchor;
    private final int side;
    private final int boardId;
    private final int height;
    private final int absoluteBase;
    private final String name;
    private Coords viewedPosition;
    private int viewedElevation;
    private final int adjacentElevation;
    private final int anchorElevation;

    public WallTarget(WallRules.Segment segment) {
        anchor = segment.hex();
        side = segment.side();
        boardId = segment.building().getBoardId();
        height = Math.max(0, segment.height() - 1);
        absoluteBase = segment.baseAltitude();
        name = segment.building().getShortName() + " " + anchor.getBoardNum() + "/" + new String[] { "N", "NE", "SE", "S", "SW", "NW" }[side];
        var board = segment.building().getGame().getBoard(boardId);
        anchorElevation = board.getHex(anchor) == null ? 0 : board.getHex(anchor).getLevel();
        adjacentElevation = board.getHex(anchor.translated(side)) == null ? anchorElevation
              : board.getHex(anchor.translated(side)).getLevel();
        viewedPosition = anchor;
        viewedElevation = absoluteBase - anchorElevation;
    }

    public static WallTarget fromId(Game game, int type, int id) {
        BoardLocation location = HexTarget.idToLocation(id);
        return WallRules.at(game, location.boardId(), location.coords(), type - TYPE_WALL_N).stream()
              .findFirst().map(WallTarget::new).orElse(null);
    }

    public WallTarget viewFrom(Coords observer) {
        viewedPosition = observer != null && observer.distance(anchor.translated(side)) < observer.distance(anchor)
              ? anchor.translated(side) : anchor;
        viewedElevation = absoluteBase - (viewedPosition.equals(anchor) ? anchorElevation : adjacentElevation);
        return this;
    }

    public WallRules.Segment segment(Game game) {
        return WallRules.at(game, boardId, anchor, side).stream().findFirst().orElse(null);
    }

    public Coords anchor() { return anchor; }
    public int side() { return side; }
    @Override public int getTargetType() { return TYPE_WALL_N + side; }
    @Override public int getId() { return HexTarget.locationToId(BoardLocation.of(anchor, boardId)); }
    @Override public int getBoardId() { return boardId; }
    @Override public Coords getPosition() { return viewedPosition; }
    @Override public int getHeight() { return height; }
    @Override public int getElevation() { return viewedElevation; }
    @Override public String getDisplayName() { return name; }
    @Override public String generalName() { return name; }
    @Override public Map<Integer, Coords> getSecondaryPositions() { return Map.of(0, anchor, 1, anchor.translated(side)); }
}
