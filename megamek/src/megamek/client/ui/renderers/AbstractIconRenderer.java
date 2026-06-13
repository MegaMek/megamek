/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.renderers;

import java.awt.Component;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;

/**
 * This renders the AbstractIcon for the current Cell of the rendered list
 */
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
     * Sets the image based on the passed AbstractIcon
     *
     * @param icon the icon to render as the image
     */
    private void setImage(final AbstractIcon icon) {
        getLblImage().setIcon(icon.getImageIcon(80));
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
