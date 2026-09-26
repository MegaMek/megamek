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
import java.util.Objects;

/**
 * One unit's place in a formation: the shape, the leader everyone lines up on, the spacing, this unit's slot, the
 * pace and what to do on contact. Every member of a formation carries the same settings with its own slot, inside its
 * {@link UnitOrders}, so a formation is saved with the game and may even span units of two bots on the same side.
 *
 * <p>Immutable, and a plain class rather than a record because the save format uses XStream.</p>
 */
public final class FormationOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = -2714659302167423058L;

    /** The spacing a formation gets unless the player picks another. */
    public static final int DEFAULT_SPACING = 2;
    /** The closest spacing: two units can never share a hex. */
    public static final int MINIMUM_SPACING = 1;
    /** The widest spacing offered. */
    public static final int MAXIMUM_SPACING = 6;

    private final FormationShape shape;
    private final int leaderId;
    private final int spacing;
    private final int slot;
    private final FormationPace pace;
    private final ContactRule contactRule;

    /**
     * @param shape       the formation's shape
     * @param leaderId    the unit everyone lines up on
     * @param spacing     hexes between neighbouring slots, {@link #MINIMUM_SPACING} to {@link #MAXIMUM_SPACING}
     * @param slot        this unit's place: 0 for the leader, then 1, 2, ... outward
     * @param pace        walking or running
     * @param contactRule what to do on meeting the enemy
     */
    public FormationOrder(FormationShape shape, int leaderId, int spacing, int slot, FormationPace pace,
          ContactRule contactRule) {
        if ((spacing < MINIMUM_SPACING) || (spacing > MAXIMUM_SPACING)) {
            throw new IllegalArgumentException("Spacing must be " + MINIMUM_SPACING + "-" + MAXIMUM_SPACING
                  + ", was " + spacing);
        }
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must not be negative, was " + slot);
        }
        this.shape = Objects.requireNonNull(shape);
        this.leaderId = leaderId;
        this.spacing = spacing;
        this.slot = slot;
        this.pace = Objects.requireNonNull(pace);
        this.contactRule = Objects.requireNonNull(contactRule);
    }

    /**
     * @return the formation's shape
     */
    public FormationShape getShape() {
        return shape;
    }

    /**
     * @return the unit every member lines up on
     */
    public int getLeaderId() {
        return leaderId;
    }

    /**
     * @return hexes between neighbouring slots
     */
    public int getSpacing() {
        return spacing;
    }

    /**
     * @return this unit's place: 0 for the leader, then 1, 2, ... outward
     */
    public int getSlot() {
        return slot;
    }

    /**
     * @return walking or running
     */
    public FormationPace getPace() {
        return pace;
    }

    /**
     * @return what the formation does on meeting the enemy
     */
    public ContactRule getContactRule() {
        return contactRule;
    }

    /**
     * @param otherLeaderId another unit's leader
     *
     * @return {@code true} if a unit with that leader is in the same formation as this one
     */
    public boolean sharesLeader(int otherLeaderId) {
        return leaderId == otherLeaderId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FormationOrder otherOrder)) {
            return false;
        }
        return (leaderId == otherOrder.leaderId) && (spacing == otherOrder.spacing) && (slot == otherOrder.slot)
              && (shape == otherOrder.shape) && (pace == otherOrder.pace) && (contactRule == otherOrder.contactRule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shape, leaderId, spacing, slot, pace, contactRule);
    }

    @Override
    public String toString() {
        return shape + " leader=" + leaderId + " spacing=" + spacing + " slot=" + slot + " " + pace + " "
              + contactRule;
    }
}
