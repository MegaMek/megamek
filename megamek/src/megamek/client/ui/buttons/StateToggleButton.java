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
package megamek.client.ui.buttons;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JToggleButton;

import megamek.common.annotations.Nullable;

/**
 * A {@link JToggleButton} that works as an on/off switch and always tells the reader where things stand: it names
 * the state it is in while untouched ("ON" or "OFF"), and the change it will make once it has been toggled away
 * from its initial state ("Turning ON" or "Turning OFF") - so a reader can tell a thing that is on from a thing
 * that is about to be turned on. Each state can also carry its own text color, such as green for on and red for
 * off. The button is sized up front to the widest of its texts, so a row it sits in does not shift when it is
 * clicked.
 *
 * @see MMToggleButton MMToggleButton - the fixed-text sibling, which marks its selection state with a check or
 *       cross in front of an unchanging label
 */
public class StateToggleButton extends JToggleButton {

    private final String selectedText;
    private final String unselectedText;
    private final String turningSelectedText;
    private final String turningUnselectedText;
    private final Color selectedColor;
    private final Color unselectedColor;
    /** The state the switched thing is actually in, which toggling the button does not change until applied. */
    private final boolean initialSelected;

    /**
     * A button without transition texts: it always names the state its selection stands on.
     *
     * @param selectedText   the text shown while the button is selected, naming that state (such as "ON")
     * @param unselectedText the text shown while the button is not selected (such as "OFF")
     * @param selected       the initial state
     */
    public StateToggleButton(String selectedText, String unselectedText, boolean selected) {
        this(selectedText, unselectedText, selectedText, unselectedText, selected, null, null);
    }

    /**
     * @param selectedText          the text shown selected and unchanged from the initial state (such as "ON")
     * @param unselectedText        the text shown unselected and unchanged (such as "OFF")
     * @param turningSelectedText   the text shown once toggled to selected, while the switched thing is still in
     *                              its unselected state (such as "Turning ON")
     * @param turningUnselectedText the text shown once toggled to unselected (such as "Turning OFF")
     * @param selected              the initial state
     * @param selectedColor         the text color of the selected texts, or {@code null} for the default color;
     *                              pass a theme-aware color such as {@code UIUtil.uiLightGreen()} so both UI
     *                              themes stay readable
     * @param unselectedColor       the text color of the unselected texts, or {@code null} for the default color
     */
    public StateToggleButton(String selectedText, String unselectedText, String turningSelectedText,
          String turningUnselectedText, boolean selected,
          @Nullable Color selectedColor, @Nullable Color unselectedColor) {
        this.selectedText = selectedText;
        this.unselectedText = unselectedText;
        this.turningSelectedText = turningSelectedText;
        this.turningUnselectedText = turningUnselectedText;
        this.selectedColor = selectedColor;
        this.unselectedColor = unselectedColor;
        this.initialSelected = selected;
        setSelected(selected);
        lockSizeToWidestText();
        showState();
        addItemListener(event -> showState());
    }

    /**
     * Sets the text and text color of the current state: the state's own name while the button stands on its
     * initial state, the transition name once it has been toggled away from it.
     */
    private void showState() {
        boolean isTurning = isSelected() != initialSelected;
        if (isSelected()) {
            setText(isTurning ? turningSelectedText : selectedText);
        } else {
            setText(isTurning ? turningUnselectedText : unselectedText);
        }
        Color stateColor = isSelected() ? selectedColor : unselectedColor;
        if (stateColor != null) {
            setForeground(stateColor);
        }
    }

    /**
     * Fixes the button's preferred size to fit the widest of its state texts, so that toggling it does not resize
     * it. The size is derived from the current font, so it follows the GUI scale.
     */
    private void lockSizeToWidestText() {
        int widest = 0;
        int tallest = 0;
        for (String stateText : new String[] { selectedText, unselectedText, turningSelectedText,
              turningUnselectedText }) {
            setText(stateText);
            Dimension stateSize = getPreferredSize();
            widest = Math.max(widest, stateSize.width);
            tallest = Math.max(tallest, stateSize.height);
        }
        setPreferredSize(new Dimension(widest, tallest));
    }
}
