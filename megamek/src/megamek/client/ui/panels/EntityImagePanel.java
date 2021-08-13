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
package megamek.client.ui.panels;

import megamek.client.ui.swing.tileset.EntityImage;
import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

import javax.swing.*;
import java.awt.*;

/**
 * The EntityImagePanel displays the Entity's Image using the provided camouflage.
 */
public class EntityImagePanel extends JPanel {
    //region Variable Declarations
    private JLabel imageLabel;
    //endregion Variable Declarations

    //region Constructors
    public EntityImagePanel(final @Nullable Entity entity, final AbstractIcon camouflage) {
        super();
        initialize();
        updateDisplayedEntity(entity, camouflage);
    }
    //endregion Constructors

    //region Getters/Setters
    public JLabel getImageLabel() {
        return imageLabel;
    }

    public void setImageLabel(final JLabel imageLabel) {
        this.imageLabel = imageLabel;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initialize() {
        // Create Panel Components
        setImageLabel(new JLabel());

        // Layout the UI
        setName("entityImagePanel");
        final GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getImageLabel())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(getImageLabel())
        );
    }
    //endregion Initialization

    /**
     * This updates the panel's currently displayed entity
     *
     * @param entity     the entity to update to, or null if the label is to be reset.
     * @param camouflage the camouflage to display
     */
    public void updateDisplayedEntity(final @Nullable Entity entity, final AbstractIcon camouflage) {
        if ((entity == null) || (MMStaticDirectoryManager.getMechTileset() == null)
                || !(camouflage instanceof Camouflage)) {
            getImageLabel().setIcon(null);
            return;
        }

        final Image base = MMStaticDirectoryManager.getMechTileset().imageFor(entity);
        getImageLabel().setIcon(new ImageIcon(new EntityImage(base, (Camouflage) camouflage, this, entity)
                .loadPreviewImage()));
    }
}
