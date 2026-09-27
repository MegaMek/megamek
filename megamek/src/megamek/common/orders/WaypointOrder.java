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
package megamek.common.orders;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;

/**
 * What a unit does at one waypoint of its route: the way it faces on arrival, whether and how long it holds there
 * before moving on, the formation for the leg ending there and, at the end of the route, whether it then leaves the
 * board. A waypoint with no hold is passed through; its facing applies only if the unit ends a turn on it.
 *
 * <p>A hold is either a fixed delay - hold two turns, whatever happens - or a wait for the formation to assemble,
 * moving on as soon as every unit is in its slot, after at most so many turns.</p>
 *
 * <p>Examples in a route order: {@code 1706/NE/2} - arrive at 1706, face northeast, hold two full turns after the
 * arrival turn, then move on; {@code 1623/U3} - wait at 1623 until the formation has assembled, three turns at most;
 * {@code 1802/EXIT} - the end of the route, then leave the board by the edge nearest 1802.</p>
 *
 * <p>This is a plain class and not a record on purpose: the save format uses XStream, which cannot read records
 * without a custom converter.</p>
 */
public final class WaypointOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 6203851840318724119L;

    /** A waypoint the unit passes through, facing as the bot decides. */
    public static final WaypointOrder PASS_THROUGH = new WaypointOrder(UnitOrders.FACING_AUTO, 0);

    /** The letter codes for the facings 0-5, as a route order writes them. */
    private static final List<String> FACING_CODES = List.of("N", "NE", "SE", "S", "SW", "NW");

    /** The code for a facing left to the bot. */
    private static final String AUTO_CODE = "A";

    /** The code before the most turns of a wait for the formation to assemble, e.g. {@code U3}. */
    private static final String ASSEMBLE_CODE = "U";

    /** The code for leaving the board at the end of the route. */
    private static final String EXIT_CODE = "EXIT";

    /**
     * How a unit leaves a waypoint.
     */
    public enum HoldMode {
        /** Straight on, without stopping. */
        PASS,
        /** After holding the set number of turns, whatever happens. */
        HOLD,
        /** As soon as its formation has assembled, or after the set number of turns at most. */
        ASSEMBLE
    }

    private final int facing;
    private final int holdTurns;
    // PASS, HOLD or ASSEMBLE; null in a save made before holds had modes, read from the hold turns
    private final HoldMode holdMode;
    // true for a last waypoint the unit leaves the board from; false in a save made before exits
    private final boolean exitBoard;
    // the formation for the leg ending here, or null to keep the units' own; null in a save made before legs had one
    private final WaypointFormation formation;
    // the formation the units re-form into here before moving on, or null to change shape on the way; null in a save
    // made before formations could change at a waypoint
    private final WaypointFormation arrivalFormation;

    /**
     * @param facing    the facing 0-5 on arrival, or {@link UnitOrders#FACING_AUTO}
     * @param holdTurns the full turns to hold after arriving; 0 passes through
     */
    public WaypointOrder(int facing, int holdTurns) {
        this(facing, holdTurns, null);
    }

    /**
     * @param facing    the facing 0-5 on arrival, or {@link UnitOrders#FACING_AUTO}
     * @param holdTurns the full turns to hold after arriving; 0 passes through
     * @param formation the formation for the leg ending at this waypoint, or {@code null} to keep the units' own
     */
    public WaypointOrder(int facing, int holdTurns, @Nullable WaypointFormation formation) {
        this(facing, (holdTurns > 0) ? HoldMode.HOLD : HoldMode.PASS, holdTurns, formation, false);
    }

    /**
     * @param facing    the facing 0-5 on arrival, or {@link UnitOrders#FACING_AUTO}
     * @param holdMode  how the unit leaves the waypoint
     * @param holdTurns the full turns to hold after arriving: the delay for a hold, the most for a wait to assemble
     * @param formation the formation for the leg ending at this waypoint, or {@code null} to keep the units' own
     * @param exitBoard {@code true} to leave the board, by the edge nearest this waypoint, once it is reached at the
     *                  end of the route
     */
    public WaypointOrder(int facing, HoldMode holdMode, int holdTurns, @Nullable WaypointFormation formation,
          boolean exitBoard) {
        this(facing, holdMode, holdTurns, formation, exitBoard, null);
    }

    /**
     * @param facing           the facing 0-5 on arrival, or {@link UnitOrders#FACING_AUTO}
     * @param holdMode         pass through, hold the turns, or hold until the formation has assembled
     * @param holdTurns        the turns to hold, or at most to wait for the formation; 0 passes through
     * @param formation        the formation for the leg ending here, or {@code null} to keep the units' own
     * @param exitBoard        {@code true} to leave the board by the nearest edge from the last waypoint
     * @param arrivalFormation the formation to re-form into here before moving on, or {@code null} to take the
     *                         leg's formation on the way
     */
    public WaypointOrder(int facing, HoldMode holdMode, int holdTurns, @Nullable WaypointFormation formation,
          boolean exitBoard, @Nullable WaypointFormation arrivalFormation) {
        if ((facing != UnitOrders.FACING_AUTO) && ((facing < 0) || (facing >= FACING_CODES.size()))) {
            throw new IllegalArgumentException("Facing must be 0-5 or FACING_AUTO, was " + facing);
        }
        if (holdTurns < 0) {
            throw new IllegalArgumentException("Hold turns may not be negative, was " + holdTurns);
        }
        this.facing = facing;
        this.holdMode = Objects.requireNonNull(holdMode);
        this.holdTurns = (holdMode == HoldMode.PASS) ? 0 : holdTurns;
        this.formation = formation;
        this.exitBoard = exitBoard;
        this.arrivalFormation = arrivalFormation;
    }

    /**
     * @param newFacing the facing 0-5 on arrival, or {@link UnitOrders#FACING_AUTO}
     *
     * @return this waypoint's settings with the facing changed and everything else kept
     */
    public WaypointOrder withFacing(int newFacing) {
        return new WaypointOrder(newFacing, getHoldMode(), holdTurns, formation, exitBoard, arrivalFormation);
    }

    /**
     * @param turns the turns to hold; 0 passes through
     *
     * @return this waypoint's settings holding a fixed number of turns, everything else kept
     */
    public WaypointOrder withHoldTurns(int turns) {
        return new WaypointOrder(facing, (turns > 0) ? HoldMode.HOLD : HoldMode.PASS, turns, formation, exitBoard,
              arrivalFormation);
    }

    /**
     * @return the formation the units re-form into at this waypoint before moving on, or {@code null} when they take
     *       the leg's formation on the way to it
     */
    public @Nullable WaypointFormation getArrivalFormation() {
        return arrivalFormation;
    }

    /**
     * @return how the unit leaves this waypoint
     */
    public HoldMode getHoldMode() {
        if (holdMode != null) {
            return holdMode;
        }
        return (holdTurns > 0) ? HoldMode.HOLD : HoldMode.PASS;
    }

    /**
     * @return {@code true} if the unit waits here for its formation to assemble, rather than for a fixed delay
     */
    public boolean isAssemble() {
        return (getHoldMode() == HoldMode.ASSEMBLE) && (holdTurns > 0);
    }

    /**
     * @return {@code true} if the unit leaves the board, by the edge nearest this waypoint, once it reaches it at the
     *       end of its route
     */
    public boolean isExitBoard() {
        return exitBoard;
    }

    /**
     * @return the formation for the leg ending at this waypoint, or {@code null} to keep the units' own formation
     */
    public @Nullable WaypointFormation getFormation() {
        return formation;
    }

    /**
     * @return the facing 0-5 on arrival, or {@link UnitOrders#FACING_AUTO}
     */
    public int getFacing() {
        return facing;
    }

    /**
     * @return the full turns to hold after arriving; 0 passes through
     */
    public int getHoldTurns() {
        return holdTurns;
    }

    /**
     * @return {@code true} if the unit stops and holds at this waypoint
     */
    public boolean isHold() {
        return (getHoldMode() != HoldMode.PASS) && (holdTurns > 0);
    }

    /**
     * @return the waypoint's settings as a route order writes them after the hex, e.g. {@code /NE/2}; empty for a
     *       waypoint passed through with the bot choosing the facing
     */
    public String toCommandSuffix() {
        StringBuilder suffix = new StringBuilder();
        if (facing != UnitOrders.FACING_AUTO) {
            suffix.append('/').append(FACING_CODES.get(facing));
        }
        if (isAssemble()) {
            suffix.append('/').append(ASSEMBLE_CODE).append(holdTurns);
        } else if (isHold()) {
            suffix.append('/').append(holdTurns);
        }
        if (formation != null) {
            suffix.append('/').append(formation.toCommandText());
        }
        if (arrivalFormation != null) {
            suffix.append('/').append(arrivalFormation.toArrivalCommandText());
        }
        if (exitBoard) {
            suffix.append('/').append(EXIT_CODE);
        }
        return suffix.toString();
    }

    /**
     * @return the digits as a number of turns
     *
     * @throws IllegalArgumentException when they are not a number, such as {@code 2X} or one too big to hold
     */
    private static int parseTurns(String digits, String segment) {
        int turns = MathUtility.parseInt(digits, -1);
        if (turns < 0) {
            throw new IllegalArgumentException("Not a number of turns: " + segment);
        }
        return turns;
    }

    /**
     * Reads the settings a route order writes after a hex: letters are a facing (N, NE, SE, S, SW, NW, or A for the
     * bot's choice), digits are the turns to hold, {@code U} and digits a wait for the formation to assemble with the
     * most turns to wait, {@code EXIT} leaves the board at the end of the route, and a part starting {@code F:} is the
     * formation for the leg ending here, in any order.
     *
     * @param segments the parts after the hex, e.g. {@code ["NE", "2"]}; none for a plain waypoint
     *
     * @return the waypoint's settings
     *
     * @throws IllegalArgumentException when a part is neither a facing nor a number of turns
     */
    public static WaypointOrder parse(List<String> segments) {
        int parsedFacing = UnitOrders.FACING_AUTO;
        int parsedHold = 0;
        HoldMode parsedMode = HoldMode.PASS;
        boolean parsedExit = false;
        WaypointFormation parsedFormation = null;
        WaypointFormation parsedArrival = null;
        for (String segment : segments) {
            String code = segment.trim().toUpperCase(Locale.ROOT);
            if (code.isEmpty()) {
                continue;
            }
            if (WaypointFormation.isArrivalCommandText(code)) {
                parsedArrival = WaypointFormation.parse(code);
            } else if (WaypointFormation.isCommandText(code)) {
                parsedFormation = WaypointFormation.parse(code);
            } else if (code.equals(EXIT_CODE)) {
                parsedExit = true;
            } else if (code.startsWith(ASSEMBLE_CODE) && (code.length() > 1)
                  && Character.isDigit(code.charAt(1))) {
                parsedHold = parseTurns(code.substring(1), segment);
                parsedMode = HoldMode.ASSEMBLE;
            } else if (Character.isDigit(code.charAt(0))) {
                parsedHold = parseTurns(code, segment);
                parsedMode = HoldMode.HOLD;
            } else if (code.equals(AUTO_CODE)) {
                parsedFacing = UnitOrders.FACING_AUTO;
            } else if (FACING_CODES.contains(code)) {
                parsedFacing = FACING_CODES.indexOf(code);
            } else {
                throw new IllegalArgumentException("Not a facing or a number of turns: " + segment);
            }
        }
        if (parsedHold == 0) {
            parsedMode = HoldMode.PASS;
        }
        return new WaypointOrder(parsedFacing, parsedMode, parsedHold, parsedFormation, parsedExit, parsedArrival);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return (other instanceof WaypointOrder otherOrder) && (facing == otherOrder.facing)
              && (holdTurns == otherOrder.holdTurns) && (getHoldMode() == otherOrder.getHoldMode())
              && (exitBoard == otherOrder.exitBoard) && Objects.equals(formation, otherOrder.formation)
              && Objects.equals(arrivalFormation, otherOrder.arrivalFormation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(facing, holdTurns, getHoldMode(), exitBoard, formation, arrivalFormation);
    }

    @Override
    public String toString() {
        String suffix = toCommandSuffix();
        return suffix.isEmpty() ? "-" : suffix.substring(1);
    }
}
