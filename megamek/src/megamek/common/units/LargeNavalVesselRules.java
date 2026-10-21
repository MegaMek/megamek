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

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.moves.MobileStructureLinkage;

/** Printed naval templates and their physical contact with mobile structures (TO:AUE pp.24,41,57,215). */
public final class LargeNavalVesselRules {
    private LargeNavalVesselRules() { }

    public static boolean applies(Entity unit) {
        return unit instanceof LargeSupportTank
              && (unit.getMovementMode().isNaval() || unit.getMovementMode().isSubmarine());
    }

    /** The table's 1/3/5/7/9 values describe the central spine; C–E also have the printed side hexes. */
    public static int template(Entity unit) {
        double tons = unit.getWeight();
        return tons <= 500 ? 0 : tons <= 6000 ? 1 : tons <= 12000 ? 2 : tons <= 30000 ? 3 : 4;
    }

    public static int aboveSurface(Entity unit) { return new int[] { 0, 1, 1, 2, 3 }[template(unit)]; }
    public static int belowSurface(Entity unit) { return new int[] { 1, 1, 2, 2, 3 }[template(unit)]; }

    public static List<Coords> footprint(Entity unit, Coords origin, int facing) {
        if (origin == null) { return List.of(); }
        if (!applies(unit)) { return List.of(origin); }
        int half = template(unit);
        var relative = new ArrayList<CubeCoords>();
        for (int row = -half; row <= half; row++) { relative.add(new CubeCoords(0, row, -row)); }
        int sideLength = half < 2 ? 0 : half == 2 ? 2 : half * 2;
        for (int row = -sideLength / 2; row < sideLength / 2; row++) {
            relative.add(new CubeCoords(1, row, -1 - row));
            relative.add(new CubeCoords(-1, row + 1, -row));
        }
        return relative.stream().map(hex -> origin.toCube().add(MobileStructureLinkage.rotate(hex, facing)).toOffset()).toList();
    }

    public static List<Coords> footprint(Entity unit) {
        return footprint(unit, unit.getPosition(), unit.getFacing());
    }

    /** Newly occupied hull volume only: moving on a roof must not be mistaken for a ram. */
    public static List<MobileStructure> obstacles(Entity unit, Coords from, int fromFacing, int fromElevation,
          Coords to, int facing, int elevation) {
        if (!applies(unit) || unit.getGame() == null || from == null || to == null) { return List.of(); }
        var board = unit.getGame().getBoard(unit);
        return unit.getGame().getEntitiesVector().stream().filter(other -> other instanceof MobileStructure mobile
              && mobile.getBoardId() == unit.getBoardId() && !mobile.isDestroyed() && !mobile.isDoomed()
              && MobileStructureLinkage.group(mobile).stream().flatMap(module -> module.getCoordsList().stream()).filter(board::contains)
                    .allMatch(coords -> board.getHex(coords).containsTerrain(Terrains.WATER))
              && !contactHexes(unit, mobile, from, fromFacing, fromElevation, to, facing, elevation).isEmpty())
              .map(MobileStructure.class::cast).toList();
    }

    /** Per-hex heights and new hull contact drive both obstacle discovery and applied charge damage. */
    public static List<Coords> contactHexes(Entity unit, MobileStructure mobile, Coords from, int fromFacing,
          int fromElevation, Coords to, int facing, int elevation) {
        var board = unit.getGame().getBoard(unit);
        var before = footprint(unit, from, fromFacing);
        boolean upward = from.equals(to) && elevation > fromElevation;
        int pivotLevel = board.getHex(to) == null ? board.getHex(from).getLevel() : board.getHex(to).getLevel();
        return footprint(unit, to, facing).stream().filter(coords -> mobile.isIn(coords) && board.contains(coords)
              && (upward || !before.contains(coords))
              && pivotLevel + elevation - belowSurface(unit)
                    < board.getHex(coords).getLevel() + BuildingElevation.roof(mobile, coords)
              && pivotLevel + elevation + aboveSurface(unit)
                    >= board.getHex(coords).getLevel() + BuildingElevation.base(mobile, coords)).toList();
    }

    public static boolean collisionAttempt(Entity unit, Coords from, int fromFacing, int fromElevation,
          Coords to, int facing, int elevation) {
        if (obstacles(unit, from, fromFacing, fromElevation, to, facing, elevation).isEmpty()) { return false; }
        var board = unit.getGame().getBoard(unit);
        return footprint(unit, to, facing).stream().allMatch(coords -> board.contains(coords)
              && board.getHex(coords).containsTerrain(Terrains.WATER)
              && !board.getHex(coords).containsTerrain(Terrains.IMPASSABLE));
    }
}
