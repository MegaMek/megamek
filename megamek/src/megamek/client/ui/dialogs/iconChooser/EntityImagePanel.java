/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.iconChooser;

import java.awt.Image;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.tileset.EntityImage;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;

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
        if ((entity == null) || (MMStaticDirectoryManager.getMekTileset() == null)
              || !(camouflage instanceof Camouflage)) {
            getImageLabel().setIcon(null);
            return;
        }

        final Image base = MMStaticDirectoryManager.getMekTileset().imageFor(entity);
        getImageLabel().setIcon(new ImageIcon(EntityImage.createLobbyIcon(base, (Camouflage) camouflage, entity)
              .loadPreviewImage(false)));
    }
}
