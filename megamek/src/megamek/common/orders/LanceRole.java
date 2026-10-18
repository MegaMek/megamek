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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import megamek.common.OffBoardDirection;

/**
 * A lance's standing role, set up before the game or in it: a convoy that heads for its exit edge, following its route
 * on the way, and never breaks off to fight; or an escort that keeps its places round a convoy lance (HammerGS,
 * 2026-10-02). A role belongs to the lance's units and outlasts their routes: a new route, Stop or Clear leaves it.
 *
 * <p>In order text: {@code CONVOY:NORTH}, {@code CONVOY:NORTH:WAIT} for a convoy that waits at the end of its route
 * instead of leaving, or
 * {@code ESCORT:<convoy force id>:LEAD.LEFT.RIGHT:MEDIUM:IN_STEP:SCREEN:BRIEFLY:FOLLOW}.</p>
 *
 * <p>This is a plain class and not a record on purpose: the save format uses XStream, which cannot read records
 * without a custom converter.</p>
 */
public final class LanceRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 3150958027163329731L;

    /** What the lance is. */
    public enum Kind {
        /** Heads for its exit edge, following its route on the way, and never breaks off to fight. */
        CONVOY,
        /** Keeps its places round a convoy lance. */
        ESCORT
    }

    /** Where an escort keeps its units round the convoy, by the convoy's heading. */
    public enum Position {
        LEAD,
        LEFT,
        RIGHT,
        REAR
    }

    /** How far from the convoy an escort keeps. */
    public enum Distance {
        /** 2-3 hexes. */
        CLOSE(2, 3),
        /** 4-6 hexes. */
        MEDIUM(4, 6),
        /** 7-10 hexes. */
        FAR(7, 10);

        private final int nearest;
        private final int furthest;

        Distance(int nearest, int furthest) {
            this.nearest = nearest;
            this.furthest = furthest;
        }

        /** @return the nearest an escort keeps, in hexes */
        public int getNearest() {
            return nearest;
        }

        /** @return the furthest an escort keeps, in hexes */
        public int getFurthest() {
            return furthest;
        }
    }

    /** How an escort moves with the convoy. */
    public enum Movement {
        /** Holds its places round the convoy as it moves. */
        IN_STEP,
        /** Moves ahead to a covering spot, holds while the convoy passes, then moves up again. */
        BOUNDING
    }

    /** What an escort does when an enemy appears. */
    public enum Contact {
        /** Gets between the threat and the convoy. */
        SCREEN,
        /** Breaks off to fight, then returns to its places. */
        BREAK_AND_FIGHT,
        /** Stays in its places, firing at anything in range. */
        STAY
    }

    /** How far an escort may leave the convoy to fight. */
    public enum LeaveToFight {
        /** Not at all: it fires from its place. */
        NEVER,
        /** Up to {@link #BRIEF_CHASE_HEXES} hexes past its place, then back. */
        BRIEFLY,
        /** Until the attacker is gone, then back. */
        HUNT
    }

    /** What an escort does once its convoy has left the board or been destroyed. */
    public enum WhenConvoyGone {
        /** Leaves with it, or holds where it fell. */
        FOLLOW,
        /** Drops the role and acts as an ordinary lance. */
        BREAK_OFF
    }

    /** How far past its place an escort set to leave Briefly may chase. */
    public static final int BRIEF_CHASE_HEXES = 4;

    /** The text for no role at all. */
    public static final String NONE_TEXT = "NONE";

    private static final String SEPARATOR = ":";
    // positions are joined by dots: order text splits arguments on spaces and routes on hyphens
    private static final String POSITION_SEPARATOR = ".";
    private static final int ESCORT_PARTS = 8;
    private static final String WAIT_TEXT = "WAIT";

    private final Kind kind;
    private final OffBoardDirection exitEdge;
    private final int convoyForceId;
    private final EnumSet<Position> positions;
    private final Distance distance;
    private final Movement movement;
    private final Contact contact;
    private final LeaveToFight leaveToFight;
    private final WhenConvoyGone whenConvoyGone;
    // false - leaving the board - in a savegame made before a convoy could wait
    private final boolean waitsAtRouteEnd;

    private LanceRole(Kind kind, OffBoardDirection exitEdge, int convoyForceId, Set<Position> positions,
          Distance distance, Movement movement, Contact contact, LeaveToFight leaveToFight,
          WhenConvoyGone whenConvoyGone) {
        this.kind = Objects.requireNonNull(kind);
        this.exitEdge = Objects.requireNonNull(exitEdge);
        this.convoyForceId = convoyForceId;
        this.positions = positions.isEmpty() ? EnumSet.noneOf(Position.class) : EnumSet.copyOf(positions);
        this.distance = Objects.requireNonNull(distance);
        this.movement = Objects.requireNonNull(movement);
        this.contact = Objects.requireNonNull(contact);
        this.leaveToFight = Objects.requireNonNull(leaveToFight);
        this.whenConvoyGone = Objects.requireNonNull(whenConvoyGone);
        this.waitsAtRouteEnd = false;
    }

    private LanceRole(OffBoardDirection exitEdge, boolean waitsAtRouteEnd) {
        this.kind = Kind.CONVOY;
        this.exitEdge = Objects.requireNonNull(exitEdge);
        this.convoyForceId = -1;
        this.positions = EnumSet.noneOf(Position.class);
        this.distance = Distance.MEDIUM;
        this.movement = Movement.IN_STEP;
        this.contact = Contact.SCREEN;
        this.leaveToFight = LeaveToFight.BRIEFLY;
        this.whenConvoyGone = WhenConvoyGone.FOLLOW;
        this.waitsAtRouteEnd = waitsAtRouteEnd;
    }

    /**
     * @param exitEdge the edge the convoy leaves by
     *
     * @return the convoy role
     */
    public static LanceRole convoy(OffBoardDirection exitEdge) {
        return convoy(exitEdge, false);
    }

    /**
     * @param exitEdge        the edge the convoy leaves by
     * @param waitsAtRouteEnd {@code true} for a convoy that waits at the end of its route until given new orders,
     *                        {@code false} for one that then leaves the board by its edge (HammerGS, 2026-10-03)
     *
     * @return the convoy role
     */
    public static LanceRole convoy(OffBoardDirection exitEdge, boolean waitsAtRouteEnd) {
        if ((exitEdge == null) || (exitEdge == OffBoardDirection.NONE)) {
            throw new IllegalArgumentException("A convoy needs an exit edge");
        }
        return new LanceRole(exitEdge, waitsAtRouteEnd);
    }

    /**
     * @param convoyForceId  the convoy lance's force id
     * @param positions      where the escort keeps round the convoy; at least one
     * @param distance       how far from the convoy
     * @param movement       in step or bounding
     * @param contact        what it does when an enemy appears
     * @param leaveToFight   how far it may leave the convoy to fight
     * @param whenConvoyGone what it does once the convoy has gone
     *
     * @return the escort role
     */
    public static LanceRole escort(int convoyForceId, Set<Position> positions, Distance distance, Movement movement,
          Contact contact, LeaveToFight leaveToFight, WhenConvoyGone whenConvoyGone) {
        if (positions.isEmpty()) {
            throw new IllegalArgumentException("An escort needs at least one position");
        }
        return new LanceRole(Kind.ESCORT, OffBoardDirection.NONE, convoyForceId, positions, distance, movement,
              contact, leaveToFight, whenConvoyGone);
    }

    /**
     * @param convoyForceId the convoy lance's force id
     *
     * @return an escort with the usual settings: Lead and both flanks at Medium, in step, screening, chasing briefly,
     *       following the convoy
     */
    public static LanceRole defaultEscort(int convoyForceId) {
        return escort(convoyForceId, EnumSet.of(Position.LEAD, Position.LEFT, Position.RIGHT), Distance.MEDIUM,
              Movement.IN_STEP, Contact.SCREEN, LeaveToFight.BRIEFLY, WhenConvoyGone.FOLLOW);
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isConvoy() {
        return kind == Kind.CONVOY;
    }

    public boolean isEscort() {
        return kind == Kind.ESCORT;
    }

    /** @return the edge a convoy leaves by; {@link OffBoardDirection#NONE} for an escort */
    public OffBoardDirection getExitEdge() {
        return exitEdge;
    }

    /**
     * @return {@code true} for a convoy that waits at the end of its route, {@code false} for one that then leaves the
     *       board by its exit edge
     */
    public boolean isWaitingAtRouteEnd() {
        return waitsAtRouteEnd;
    }

    /** @return the force id of the convoy an escort guards; -1 for a convoy */
    public int getConvoyForceId() {
        return convoyForceId;
    }

    /** @return where an escort keeps round the convoy */
    public Set<Position> getPositions() {
        return EnumSet.copyOf(positions.isEmpty() ? EnumSet.noneOf(Position.class) : positions);
    }

    public Distance getDistance() {
        return distance;
    }

    public Movement getMovement() {
        return movement;
    }

    public Contact getContact() {
        return contact;
    }

    public LeaveToFight getLeaveToFight() {
        return leaveToFight;
    }

    public WhenConvoyGone getWhenConvoyGone() {
        return whenConvoyGone;
    }

    /**
     * @return the role as order text, e.g. {@code CONVOY:NORTH}
     */
    public String toCommandText() {
        if (isConvoy()) {
            return Kind.CONVOY + SEPARATOR + exitEdge.name() + (waitsAtRouteEnd ? SEPARATOR + WAIT_TEXT : "");
        }
        List<String> positionNames = new ArrayList<>();
        for (Position position : positions) {
            positionNames.add(position.name());
        }
        return String.join(SEPARATOR, Kind.ESCORT.name(), String.valueOf(convoyForceId),
              String.join(POSITION_SEPARATOR, positionNames), distance.name(), movement.name(), contact.name(),
              leaveToFight.name(), whenConvoyGone.name());
    }

    /**
     * @param text order text, as {@link #toCommandText()} writes it
     *
     * @return the role
     *
     * @throws IllegalArgumentException when the text is not a role
     */
    public static LanceRole parse(String text) {
        String[] parts = text.trim().toUpperCase(Locale.ROOT).split(SEPARATOR);
        try {
            Kind kind = Kind.valueOf(parts[0]);
            if (kind == Kind.CONVOY) {
                return convoy(OffBoardDirection.valueOf(parts[1]), (parts.length > 2) && WAIT_TEXT.equals(parts[2]));
            }
            if (parts.length != ESCORT_PARTS) {
                throw new IllegalArgumentException("Not an escort role: " + text);
            }
            EnumSet<Position> positions = EnumSet.noneOf(Position.class);
            for (String position : parts[2].split("\\.")) {
                positions.add(Position.valueOf(position));
            }
            return escort(Integer.parseInt(parts[1]), positions, Distance.valueOf(parts[3]),
                  Movement.valueOf(parts[4]), Contact.valueOf(parts[5]), LeaveToFight.valueOf(parts[6]),
                  WhenConvoyGone.valueOf(parts[7]));
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException missingPart) {
            throw new IllegalArgumentException("Not a lance role: " + text, missingPart);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return (other instanceof LanceRole otherRole) && (kind == otherRole.kind) && (exitEdge == otherRole.exitEdge)
              && (convoyForceId == otherRole.convoyForceId) && positions.equals(otherRole.positions)
              && (distance == otherRole.distance) && (movement == otherRole.movement)
              && (contact == otherRole.contact) && (leaveToFight == otherRole.leaveToFight)
              && (whenConvoyGone == otherRole.whenConvoyGone) && (waitsAtRouteEnd == otherRole.waitsAtRouteEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, exitEdge, convoyForceId, positions, distance, movement, contact, leaveToFight,
              whenConvoyGone, waitsAtRouteEnd);
    }

    @Override
    public String toString() {
        return toCommandText();
    }
}
