/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.util;

import megamek.common.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * A text field for integer values that can specify a minimum and maximum value. Attempting to release
 * focus with an illegal value will set the value to the minimum or maximum as appropriate rather than
 * allowing the focus to be released.
 *
 * @author Neoancient
 */
public class IntRangeTextField extends JTextField {

    private Integer minimum = null;
    private Integer maximum = null;

    public IntRangeTextField() {
        super();
        setInputVerifier(inputVerifier);
        if (getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) getDocument()).setDocumentFilter(docFilter);
        }
    }

    public IntRangeTextField(int columns) {
        this();
        setColumns(columns);
    }

    /**
     * @return The minimum legal value
     */
    public @Nullable Integer getMinimum() {
        return minimum;
    }

    /**
     * Sets the minimum value for the field.
     * @param min the minimum value
     */
    public void setMinimum(@Nullable Integer min) {
        minimum = min;
    }

    /**
     * @return The maximum legal value
     */
    public @Nullable Integer getMaximum() {
        return maximum;
    }

    /**
     * Sets the maximum legal value
     * @param max the maximum value
     */
    public void setMaximum(@Nullable Integer max) {
        maximum = max;
    }

    private final InputVerifier inputVerifier = new InputVerifier() {
        @Override
        public boolean verify(JComponent input) {
            try {
                return (((minimum == null) || (getIntVal() >= minimum))
                        && ((maximum == null) || (getIntVal() <= maximum)));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        @Override
        public boolean shouldYieldFocus(JComponent input, JComponent target) {
            if (!verify(input)) {
                int val = getIntVal();
                if ((minimum != null) && (val < minimum)) {
                    setIntVal(minimum);
                } else if ((maximum != null) && (val > maximum)) {
                    setIntVal(maximum);
                }
            }
            return true;
        }
    };

    private final DocumentFilter docFilter = new DocumentFilter() {

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string.chars().allMatch(this::isCharValid)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text.chars().allMatch(this::isCharValid)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        // Allow digits and, if the minimum is below zero or not set, a minus sign
        private boolean isCharValid(int chr) {
            return Character.isDigit(chr) || ((chr == '-') && ((minimum == null) || (minimum < 0)));
        }

    };

    /**
     * Parses the text as an {@code int}.
     * @return The {@code int} value of the text, or zero if the text is not a valid int value
     */
    public int getIntVal() {
        return getIntVal(0);
    }

    /**
     * Parses the text as an {@code int}.
     * @param defaultVal The value to return if the text cannot be parsed as an int
     * @return The {@code int} value of the text, or the indicated default if the text is not a valid int value
     */
    public int getIntVal(int defaultVal) {
        try {
            return Integer.parseInt(getText());
        } catch (NumberFormatException ignored) {
            return defaultVal;
        }
    }

    /**
     * Sets the text to a string representation of the provided value
     * @param val the provided value
     */
    public void setIntVal(int val) {
        setText(String.valueOf(val));
    }
}