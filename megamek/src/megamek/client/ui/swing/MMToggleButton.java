/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.MMConstants;

import javax.swing.*;
import java.awt.*;

/** 
 * A JToggleButton that shows a check mark and cross mark to make its 
 * selection status clearer. 
 * 
 * @author Simon (Juliez)
 */
public class MMToggleButton extends JToggleButton {
    
    private static final long serialVersionUID = 3726089213745832469L;
    
    private static final String CHECK = "#90FF90>\u2713 ";
    private static final String CROSS = "#FF9090>\u2717 ";
    private static final String INTRO = "<HTML><NOBR><FONT COLOR=";
    private static final String CLOSE = "</FONT>";
    private static final int MARK_LENGTH = CHECK.length() + INTRO.length() + CLOSE.length();
    
    public MMToggleButton(String text) {
        super();
        setText(text);
        // The standard UI font doesn't show unicode characters (on Win10)
        setFont(new Font(MMConstants.FONT_DIALOG, Font.PLAIN, getFont().getSize()));
        addActionListener(event -> setText(getText()));
    }
    
    @Override
    public void setText(String text) {
        if (text.length() > MARK_LENGTH && text.startsWith(INTRO)) {
            text = text.substring(MARK_LENGTH);
        }
        if (isSelected()) {
            text = INTRO + CHECK + CLOSE + text;
        } else {
            text = INTRO + CROSS + CLOSE + text;
        }
        super.setText(text);
    }
    
    @Override
    public void setSelected(boolean b) {
        super.setSelected(b);
        setText(getText());
    }
    
}