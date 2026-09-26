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
import java.util.Locale;
import java.util.Objects;

import megamek.common.annotations.Nullable;

/**
 * The formation a group of units travels in on one leg of its route: the leg that ends at the waypoint this is set on.
 * It carries everything a formation needs except the leader and each unit's slot, which stay with the units'
 * {@link FormationOrder}. A leg with no shape is travelled out of formation, each unit on its own.
 *
 * <p>Example: {@code 1706/NE/2/F:WEDGE:2:WALK:BREAK:T} in a route order - travel to 1706 as a Wedge, two hexes
 * apart, walking, breaking on contact and keeping together; there face northeast and hold two turns.</p>
 *
 * <p>This is a plain class and not a record on purpose: the save format uses XStream, which cannot read records
 * without a custom converter.</p>
 */
public final class WaypointFormation implements Serializable {

    @Serial
    private static final long serialVersionUID = -4821930554419873206L;

    /** A leg travelled out of formation, each unit on its own. */
    public static final WaypointFormation NONE = new WaypointFormation(null, FormationOrder.DEFAULT_SPACING,
          FormationPace.WALK, ContactRule.BREAK, false);

    private static final String PREFIX = "F";
    private static final String NO_SHAPE = "NONE";
    private static final String SEPARATOR = ":";
    private static final String KEEP_TOGETHER = "T";
    private static final String APART = "A";
    private static final int PART_COUNT = 6;

    // null for a leg travelled out of formation (MM @Nullable is not applicable to fields)
    private final FormationShape shape;
    private final int spacing;
    private final FormationPace pace;
    private final ContactRule contactRule;
    private final boolean keepTogether;

    /**
     * @param shape        the shape, or {@code null} to travel out of formation
     * @param spacing      hexes between neighbouring slots
     * @param pace         how the units move
     * @param contactRule  what the formation does on contact
     * @param keepTogether {@code true} to move as a block
     */
    public WaypointFormation(@Nullable FormationShape shape, int spacing, FormationPace pace, ContactRule contactRule,
          boolean keepTogether) {
        if ((spacing < FormationOrder.MINIMUM_SPACING) || (spacing > FormationOrder.MAXIMUM_SPACING)) {
            throw new IllegalArgumentException("Spacing must be " + FormationOrder.MINIMUM_SPACING + "-"
                  + FormationOrder.MAXIMUM_SPACING + ", was " + spacing);
        }
        this.shape = shape;
        this.spacing = spacing;
        this.pace = Objects.requireNonNull(pace);
        this.contactRule = Objects.requireNonNull(contactRule);
        this.keepTogether = keepTogether;
    }

    /**
     * @return {@code true} for a leg travelled out of formation
     */
    public boolean isNone() {
        return shape == null;
    }

    /**
     * @return the shape, or {@code null} for a leg travelled out of formation
     */
    public @Nullable FormationShape getShape() {
        return shape;
    }

    public int getSpacing() {
        return spacing;
    }

    public FormationPace getPace() {
        return pace;
    }

    public ContactRule getContactRule() {
        return contactRule;
    }

    public boolean isKeepTogether() {
        return keepTogether;
    }

    /**
     * @param base the units' formation order, which carries the leader and each unit's slot
     *
     * @return the formation for this leg with the base's leader and slot; only call for a leg with a shape
     */
    public FormationOrder applyTo(FormationOrder base) {
        return new FormationOrder(Objects.requireNonNull(shape), base.getLeaderId(), spacing, base.getSlot(), pace,
              contactRule, keepTogether);
    }

    /**
     * @return this leg's formation as a route order writes it, e.g. {@code F:WEDGE:2:WALK:BREAK:T}, or
     *       {@code F:NONE} for a leg out of formation
     */
    public String toCommandText() {
        if (isNone()) {
            return PREFIX + SEPARATOR + NO_SHAPE;
        }
        return String.join(SEPARATOR, PREFIX, shape.name(), String.valueOf(spacing), pace.name(), contactRule.name(),
              keepTogether ? KEEP_TOGETHER : APART);
    }

    /**
     * @param text a route order's part
     *
     * @return {@code true} if the part is a leg's formation, starting {@code F:}
     */
    public static boolean isCommandText(String text) {
        return text.toUpperCase(Locale.ROOT).startsWith(PREFIX + SEPARATOR);
    }

    /**
     * Reads a leg's formation as {@link #toCommandText()} writes it.
     *
     * @param text the part, e.g. {@code F:WEDGE:2:WALK:BREAK:T}
     *
     * @return the leg's formation
     *
     * @throws IllegalArgumentException when the text is not a formation
     */
    public static WaypointFormation parse(String text) {
        String[] parts = text.trim().toUpperCase(Locale.ROOT).split(SEPARATOR);
        if ((parts.length == 2) && parts[1].equals(NO_SHAPE)) {
            return NONE;
        }
        if ((parts.length != PART_COUNT) || !parts[0].equals(PREFIX)) {
            throw new IllegalArgumentException("Not a formation, such as F:WEDGE:2:WALK:BREAK:T: " + text);
        }
        return new WaypointFormation(FormationShape.valueOf(parts[1]), Integer.parseInt(parts[2]),
              FormationPace.valueOf(parts[3]), ContactRule.valueOf(parts[4]), parts[5].equals(KEEP_TOGETHER));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return (other instanceof WaypointFormation otherFormation) && (shape == otherFormation.shape)
              && (spacing == otherFormation.spacing) && (pace == otherFormation.pace)
              && (contactRule == otherFormation.contactRule) && (keepTogether == otherFormation.keepTogether);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shape, spacing, pace, contactRule, keepTogether);
    }

    @Override
    public String toString() {
        return toCommandText();
    }
}
