/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.boardeditor;

import megamek.MMConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Specialized field for the BoardEditor that supports MouseWheel changes.
 *
 * @author Simon
 */
class EditorTextField extends JTextField {
    private int minValue = Integer.MIN_VALUE;
    private int maxValue = Integer.MAX_VALUE;

    /**
     * Creates an EditorTextField based on JTextField. This is a specialized field for the BoardEditor that supports
     * MouseWheel changes.
     *
     * @param text    the initial text
     * @param columns as in JTextField
     *
     * @see JTextField#JTextField(String, int)
     */
    EditorTextField(String text, int columns) {
        super(text, columns);
        // Automatically select all text when clicking the text field
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                selectAll();
            }
        });
        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                incValue();
            } else {
                decValue();
            }
        });
        setMargin(new Insets(1, 1, 1, 1));
        setHorizontalAlignment(JTextField.CENTER);
        setFont(new Font(MMConstants.FONT_SANS_SERIF, Font.BOLD, 20));
        setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Creates an EditorTextField based on JTextField. This is a specialized field for the BoardEditor that supports
     * MouseWheel changes.
     *
     * @param text    the initial text
     * @param columns as in JTextField
     * @param minimum a minimum value that the EditorTextField will generally adhere to when its own methods are used to
     *                change its value.
     *
     * @author Simon/Juliez
     * @see JTextField#JTextField(String, int)
     */
    public EditorTextField(String text, int columns, int minimum) {
        this(text, columns);
        minValue = minimum;
    }

    /**
     * Creates an EditorTextField based on JTextField. This is a specialized field for the BoardEditor that supports
     * MouseWheel changes.
     *
     * @param text    the initial text
     * @param columns as in JTextField
     * @param minimum a minimum value that the EditorTextField will generally adhere to when its own methods are used to
     *                change its value.
     * @param maximum a maximum value that the EditorTextField will generally adhere to when its own methods are used to
     *                change its value.
     *
     * @author Simon/Juliez
     * @see JTextField#JTextField(String, int)
     */
    public EditorTextField(String text, int columns, int minimum, int maximum) {
        this(text, columns);
        minValue = minimum;
        maxValue = maximum;
    }

    /**
     * Increases the EditorTextField's number by one, if a number is present.
     */
    public void incValue() {
        int newValue = getNumber() + 1;
        setNumber(newValue);
    }

    /**
     * Lowers the EditorTextField's number by one, if a number is present and if that number is higher than the minimum
     * value.
     */
    public void decValue() {
        setNumber(getNumber() - 1);
    }

    /**
     * Sets the text to <code>newValue</code>. If <code>newValue</code> is lower than the EditorTextField's minimum
     * value, the minimum value will be set instead.
     *
     * @param newValue the value to be set
     */
    public void setNumber(int newValue) {
        int value = Math.max(newValue, minValue);
        value = Math.min(value, maxValue);
        setText(Integer.toString(value));
    }

    /**
     * Returns the text in the EditorTextField's as an int. Returns 0 when no parsable number (only letters) are
     * present.
     */
    public int getNumber() {
        try {
            return Integer.parseInt(getText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
