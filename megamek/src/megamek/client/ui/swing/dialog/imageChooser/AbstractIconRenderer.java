/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.dialog.imageChooser;

import megamek.common.icons.AbstractIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AbstractIconRenderer extends JPanel implements ListCellRenderer<AbstractIcon> {
    /** Image file extensions, .jpg .jpeg .png .gif */
    protected static final String[] EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif"};

    /** This JLabel displays the selectable image */
    protected JLabel lblImage = new JLabel();

    /** This JLabel displays the name of the selectable image */
    protected JLabel lblText = new JLabel();

    /** The tooltip to be displayed */
    protected String tip = "";

    public AbstractIconRenderer() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(lblImage);
        add(lblText);
    }


    /**
     * Sets the image based on the passed category and name from
     * the DirectoryItems that the list currently displays.
     */
    private void setImage(AbstractIcon icon) {
        lblImage.setIcon(icon.getImageIcon());
        tip = "<HTML><BODY>" + icon.getCategory() + "<BR>" + icon.getFilename();
    }

    @Override
    public String getToolTipText() {
        return tip;
    }

    /** Sets the label text of the image, removing the file extension. */
    private void setText(String text) {
        // Remove the file extension
        for (String ext: EXTENSIONS) {
            if (text.toLowerCase().contains(ext)) {
                text = text.replace(ext, "");
                break;
            }
        }
        lblText.setText(text);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends AbstractIcon> list,
                                                  AbstractIcon value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            setBackground(UIManager.getColor("Table.background"));
            setForeground(UIManager.getColor("Table.foreground"));
        }
        setImage(value);
        setText(value.getFilename());

        return this;
    }
}
