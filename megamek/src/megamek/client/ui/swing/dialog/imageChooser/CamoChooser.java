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

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.util.fileUtils.DirectoryItems;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CamoChooser extends AbstractIconChooser {
    //region Variable Declarations
    private boolean canHaveIndividualCamouflage;
    //endregion Variable Declarations

    //region Constructors
    public CamoChooser(@Nullable AbstractIcon camouflage, boolean canHaveIndividualCamouflage) {
        super(new CamoChooserTree(), camouflage);
        setCanHaveIndividualCamouflage(canHaveIndividualCamouflage);
    }
    //endregion Constructors

    //region Getters/Setters
    public boolean canHaveIndividualCamouflage() {
        return canHaveIndividualCamouflage;
    }

    public void setCanHaveIndividualCamouflage(boolean canHaveIndividualCamouflage) {
        this.canHaveIndividualCamouflage = canHaveIndividualCamouflage;
    }
    //endregion Getters/Setters

    @Override
    protected DirectoryItems getDirectory() {
        return MMStaticDirectoryManager.getCamouflage();
    }

    @Override
    protected AbstractIcon createIcon(String category, String filename) {
        return new Camouflage(category, filename);
    }

    @Override
    protected List<AbstractIcon> getItems(String category) {
        List<AbstractIcon> result = new ArrayList<>();

        if (category.startsWith(Camouflage.COLOUR_CAMOUFLAGE)) {
            // This section is a list of all colour camouflages supported
            for (PlayerColour colour : PlayerColour.values()) {
                result.add(createIcon(Camouflage.COLOUR_CAMOUFLAGE, colour.name()));
            }
        } else {
            // In any other camouflage section, the camos of the selected category are
            // presented. When the includeSubDirs flag is true, all categories
            // below the selected one are also presented.
            if (includeSubDirs) {
                for (Iterator<String> catNames = getDirectory().getCategoryNames(); catNames.hasNext(); ) {
                    String tcat = catNames.next();
                    if (tcat.startsWith(category)) {
                        addCategoryItems(tcat, result);
                    }
                }
            } else {
                addCategoryItems(category, result);
            }
        }
        return result;
    }

    /** Reloads the camo directory from disk. */
    @Override
    protected void refreshDirectory() {
        MMStaticDirectoryManager.refreshCamouflageDirectory();
        refreshDirectory(new CamoChooserTree());
    }

    @Override
    protected void refreshDirectory(JTree newTree) {
        super.refreshDirectory(newTree);
    }
}
