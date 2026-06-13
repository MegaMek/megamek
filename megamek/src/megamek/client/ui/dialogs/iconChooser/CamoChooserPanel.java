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
package megamek.client.ui.dialogs.iconChooser;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;

import megamek.client.ui.panels.abstractPanels.abstractIconChooserPanel;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.trees.CamoChooserTree;
import megamek.client.ui.util.PlayerColour;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.util.fileUtils.AbstractDirectory;

/**
 * CamoChooser is an implementation of AbstractIconChooser that is used to select a Camouflage from the Camouflage
 * Directory. This implementation allows for the inclusion of the Colour Camouflage category, which is not included as
 * part of the base AbstractDirectory.
 *
 * @see abstractIconChooserPanel
 */
public class CamoChooserPanel extends abstractIconChooserPanel {
    //region Variable Declarations
    private boolean canHaveIndividualCamouflage;
    //endregion Variable Declarations

    //region Constructors
    public CamoChooserPanel(final JFrame frame, final @Nullable AbstractIcon camouflage,
          final boolean canHaveIndividualCamouflage) {
        super(frame, "CamouflageChooser", new CamoChooserTree(), camouflage);
        setCanHaveIndividualCamouflage(canHaveIndividualCamouflage);
    }
    //endregion Constructors

    //region Getters/Setters
    public boolean canHaveIndividualCamouflage() {
        return canHaveIndividualCamouflage;
    }

    public void setCanHaveIndividualCamouflage(final boolean canHaveIndividualCamouflage) {
        this.canHaveIndividualCamouflage = canHaveIndividualCamouflage;
    }
    //endregion Getters/Setters

    @Override
    protected @Nullable AbstractDirectory getDirectory() {
        return MMStaticDirectoryManager.getCamouflage();
    }

    @Override
    protected Camouflage createIcon(String category, final String filename) {
        return new Camouflage(category, filename);
    }

    @Override
    public @Nullable Camouflage getSelectedItem() {
        final AbstractIcon icon = super.getSelectedItem();
        return (icon instanceof Camouflage) ? (Camouflage) icon : null;
    }

    /**
     * This override handles Colour Camouflage, as they are not stored as part of the directory tree but instead are an
     * isolated system currently tied to PlayerColours.
     * <p>
     * Called at start and when a new category is selected in the directory tree. Assumes that the root of the path
     * (AbstractIcon.ROOT_CATEGORY) is passed as ""!
     *
     * @param category the category to get the items for, in TreePath format
     *
     * @return a list of items that should be shown for the category
     */
    @Override
    protected List<AbstractIcon> getIcons(final String category) {
        if (category.startsWith(Camouflage.COLOUR_CAMOUFLAGE)) {
            final List<AbstractIcon> icons = new ArrayList<>();
            // This section is a list of all colour camouflages supported
            for (PlayerColour colour : PlayerColour.values()) {
                icons.add(createIcon(Camouflage.COLOUR_CAMOUFLAGE, colour.name()));
            }
            return icons;
        } else {
            return super.getIcons(category);
        }
    }

    @Override
    public void refreshDirectory() {
        MMStaticDirectoryManager.refreshCamouflageDirectory();
        refreshDirectory(new CamoChooserTree());
    }
}
