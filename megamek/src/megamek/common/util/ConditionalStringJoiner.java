/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.util;

import java.util.function.Supplier;

/**
 * A utility class for conditionally joining strings with a delimiter.
 * @author Luana Coppio
 */
public class ConditionalStringJoiner {
    private static final String DEFAULT_DELIMITER = ", ";
    private static final int DEFAULT_CAPACITY = 64;
    private final StringBuilder stringBuilder;
    private final String delimiter;
    private boolean isFirst;

    /**
     * Creates a ConditionalStringJoiner.
     */
    public ConditionalStringJoiner() {
        this(DEFAULT_DELIMITER, DEFAULT_CAPACITY);
    }

    /**
     * Creates a ConditionalStringJoiner with the specified delimiter.
     * @param delimiter the delimiter to use when joining strings
     */
    public ConditionalStringJoiner(String delimiter) {
        this(delimiter, DEFAULT_CAPACITY);
    }

    /**
     * Creates a ConditionalStringJoiner with the specified initial capacity.
     * @param capacity the initial capacity of the ConditionalStringJoiner
     */
    public ConditionalStringJoiner(int capacity) {
        this(DEFAULT_DELIMITER, capacity);
    }

    /**
     * Creates a ConditionalStringJoiner with the specified delimiter and initial capacity.
     * @param delimiter the delimiter to use when joining strings
     * @param capacity the initial capacity of the ConditionalStringJoiner
     */
    public ConditionalStringJoiner(String delimiter, int capacity) {
        this.stringBuilder = new StringBuilder(capacity);
        this.delimiter = delimiter;
        this.isFirst = true;
    }

    /**
     * Adds a string to the joiner if the condition is true. Uses a supplier to get the string value, this way it is
     * not evaluated unless the condition is true.
     * <pre>
     *     {@code
     *     ConditionalStringJoiner conditionalStringJoiner = new ConditionalStringJoiner();
     *     // This will only add the string if speed > 5
     *     conditionalStringJoiner.add(speed > 5,
     *         () -> I18n.getFormattedText("common.speedWarning", calculateDamage())
     *     );
     *     // partial string if speed > 5
     *     //  >> "Risk of taking 75 damage if you fail PSR"
     *
     *     // This will add the string if isCriticalHit is true
     *     conditionalStringJoiner.add(isCriticalHit, () -> I18n.getText("common.criticalHit"));
     *     String result = conditionalStringJoiner.toString();
     *
     *     // The result will be a string like this:
     *     //  >> "Risk of taking 75 damage if you fail PSR, this unit has a critical hit"
     *     }
     * </pre>
     * @param condition the condition to check
     * @param messageSupplier the supplier that provides the string to add if the condition is true
     * @return this instance for method chaining
     */
    public ConditionalStringJoiner add(boolean condition, Supplier<String> messageSupplier) {
        if (!condition || (messageSupplier == null)) {
            return this;
        }
        return this.add(messageSupplier.get());
    }

    /**
     * Adds a string to the joiner if the condition is true.
     * <pre>
     *     {@code
     *     ConditionalStringJoiner conditionalStringJoiner = new ConditionalStringJoiner();
     *     // This will only add the string if speed > 5
     *     conditionalStringJoiner.add(speed > 5, I18n.getFormattedText("common.speedWarning", calculateDamage()));
     *     // partial string if speed > 5
     *     //  >> "Risk of taking 75 damage if you fail PSR"
     *
     *     // This will add the string if isCriticalHit is true
     *     conditionalStringJoiner.add(isCriticalHit, I18n.getText("common.criticalHit"));
     *     String result = conditionalStringJoiner.toString();
     *
     *     // The result will be a string like this:
     *     //  >> "Risk of taking 75 damage if you fail PSR, this unit has a critical hit"
     *     }
     * </pre>
     * @param condition the condition to check
     * @param message the string to add if the condition is true
     * @return this instance for method chaining
     */
    public ConditionalStringJoiner add(boolean condition, String message) {
        if (!condition) {
            return this;
        }
        return this.add(message);
    }

    /**
     * Adds a string to the joiner. No condition is checked.
     * <pre>
     *     {@code
     *     ConditionalStringJoiner conditionalStringJoiner = new ConditionalStringJoiner();
     *     conditionalStringJoiner.add(I18n.getFormattedText("common.speedWarning", calculateDamage()));
     *     //  >> "Risk of taking 75 damage if you fail PSR"
     *
     *     conditionalStringJoiner.add(I18n.getText("common.criticalHit"));
     *     String result = conditionalStringJoiner.toString();
     *
     *     // The result will be a string like this:
     *     //  >> "Risk of taking 75 damage if you fail PSR, this unit has a critical hit"
     *     }
     * </pre>
     * @param message the string to add
     * @return this instance for method chaining
     */
    public ConditionalStringJoiner add(String message) {
        if (message == null || message.isEmpty()) {
            return this;
        }

        if (!isFirst) {
            stringBuilder.append(delimiter);
        }
        stringBuilder.append(message);
        isFirst = false;

        return this;
    }

    @Override
    public String toString() {
        return stringBuilder.toString();
    }

}
