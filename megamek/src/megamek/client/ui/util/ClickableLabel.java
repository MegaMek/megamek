/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JLabel;

/**
 * This is a specialized label that acts as a replacement for a button. It underlines its text when hovered like a
 * hyperlink and so indicates that it is clickable. Note that the label always uses HTML to underline its text
 * internally and so does not need an additional HTML tag to show HTML content. This can be used in circumstances where
 * the button's function is less important or a fallback and the label is used to call less attention to it than a
 * button would. Another use is when a text info label should have the button functionality only as a secondary
 * function. The label calls a callback method when it is clicked.
 */
public class ClickableLabel extends JLabel implements MouseListener {

    private static final String HOVERED_PREFIX = "<HTML><U>";
    private static final String NON_HOVERED_PREFIX = "<HTML>";
    private boolean isHovered = false;
    private String baseText = "";
    private final Consumer<MouseEvent> clickCallback;

    /**
     * Creates a new clickable label. The given callback method is called when the label is mouse-clicked.
     *
     * @param clickCallback The method to call when the label is clicked
     */
    public ClickableLabel(Consumer<MouseEvent> clickCallback) {
        this.clickCallback = Objects.requireNonNull(clickCallback);
        addMouseListener(this);
    }

    @Override
    public void setText(String text) {
        baseText = text;
        updateText();
    }

    private void updateText() {
        super.setText((isHovered ? HOVERED_PREFIX : NON_HOVERED_PREFIX) + baseText);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        clickCallback.accept(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        isHovered = true;
        updateText();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        isHovered = false;
        updateText();
    }
}
