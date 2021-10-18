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
package megamek.client.ui.renderers;

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AbstractIconRenderer extends JPanel implements ListCellRenderer<AbstractIcon> {
    //region Variable Declarations
    private JLabel lblImage; // This JLabel displays the selectable image
    private JLabel lblText; // This JLabel displays the name of the selectable image
    private String toolTip = null; // The tooltip to be displayed
    //endregion Variable Declarations

    //region Constructors
    public AbstractIconRenderer() {
        setToolTip(null);
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public JLabel getLblImage() {
        return lblImage;
    }

    public void setLblImage(final JLabel lblImage) {
        this.lblImage = lblImage;
    }

    public JLabel getLblText() {
        return lblText;
    }

    public void setLblText(final JLabel lblText) {
        this.lblText = lblText;
    }

    @Override
    public String getToolTipText() {
        return toolTip;
    }

    public void setToolTip(final @Nullable String toolTip) {
        this.toolTip = toolTip;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initialize() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setName("AbstractIconRenderer");
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        setLblImage(new JLabel());
        getLblImage().setName("lblImage");
        add(getLblImage());

        setLblText(new JLabel());
        getLblText().setName("lblText");
        add(getLblText());
    }
    //endregion Initialization

    /**
     * Sets the image based on the passed category and name from the AbstractDirectory that the list
     * currently displays.
     */
    private void setImage(final AbstractIcon icon) {
        getLblImage().setIcon(icon.getImageIcon());
        setToolTip(String.format("<html><BODY>%s<br>%s</html>", icon.getCategory(), icon.getFilename()));
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends AbstractIcon> list,
                                                  final AbstractIcon value, final int index,
                                                  final boolean isSelected, final boolean cellHasFocus) {
        if (isSelected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            setBackground(UIManager.getColor("Table.background"));
            setForeground(UIManager.getColor("Table.foreground"));
        }
        setImage(value);
        getLblText().setText(value.getFilename());
        return this;
    }
}
