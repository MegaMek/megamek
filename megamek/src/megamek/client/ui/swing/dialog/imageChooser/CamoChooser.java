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
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Camouflage;
import megamek.common.util.fileUtils.DirectoryItems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CamoChooser extends AbstractIconChooser {
    //region Variable Declarations
    /** True when an individual camo is being selected for an entity. */
    private boolean individualCamo = false;

    /** When an individual camo is being selected, the player's camo is displayed as a reset option. */
    private AbstractIcon entityOwnerCamo;
    //endregion Variable Declarations

    //region Constructors
    public CamoChooser() {
        this(null);
    }

    public CamoChooser(AbstractIcon icon) {
        super(new CamoChooserTree(), icon);
    }
    //endregion Constructors

    //region Getters/Setters
    public boolean isIndividualCamo() {
        return individualCamo;
    }

    public void setIndividualCamo(boolean individualCamo) {
        this.individualCamo = individualCamo;
    }

    public AbstractIcon getEntityOwnerCamo() {
        return entityOwnerCamo;
    }

    public void setEntityOwnerCamo(AbstractIcon entityOwnerCamo) {
        this.entityOwnerCamo = entityOwnerCamo;
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

        // If a player camo is being selected, the items presented in the
        // NO_CAMO section are the available player colors.
        // If an individual camo is being selected, then the only item presented
        // in the NO_CAMO section is the owner's camo. This can be chosen
        // to remove the individual camo.
        if (category.startsWith(Camouflage.NO_CAMOUFLAGE)) {
            if (individualCamo) {
                result.add(entityOwnerCamo);
            } else {
                for (String color: PlayerColors.COLOR_NAMES) {
                    result.add(createIcon(Camouflage.NO_CAMOUFLAGE, color));
                }
            }
            return result;
        }

        // In any other camo section, the camos of the selected category are
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
        return result;
    }

    /** Reloads the camo directory from disk. */
    @Override
    protected void refreshDirectory() {
        MMStaticDirectoryManager.refreshCamouflageDirectory();
        refreshDirectory(new CamoChooserTree());
    }
}
