/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.models;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;

/**
 * The FileNameComboBoxModel class is a ComboBoxModel that uses the file name as the value. It receives a list or array
 * of files and instead of showing the stringfied version of the file it shows only the value of {@link File#getName()}
 * method. It has a helper function to get the selected file on its own if necessary.
 *
 * @author Luana Coppio
 */
public class FileNameComboBoxModel implements ComboBoxModel<String> {
    private final List<File> files;
    private File selectedFile;

    public FileNameComboBoxModel(List<File> files) {
        this.files = files;
        if (!files.isEmpty()) {
            selectedFile = files.get(0);
        }
    }

    public FileNameComboBoxModel(File[] files) {
        this.files = new ArrayList<>();
        if (files != null) {
            this.files.addAll(Arrays.asList(files));
            this.selectedFile = this.files.get(0);
        }
    }

    @Override
    public void setSelectedItem(Object anItem) {
        for (File file : files) {
            if (file.getName().equals(anItem)) {
                selectedFile = file;
                break;
            }
        }
    }

    @Override
    public Object getSelectedItem() {
        return selectedFile != null ? selectedFile.getName() : null;
    }

    @Override
    public int getSize() {
        return files.size();
    }

    /**
     * Returns the file name of the file at the given index.
     *
     * @param index the requested index
     *
     * @return the file name of the file at the given index, or {@code null} if the index is out of bounds or there is
     *       no element at the given index.
     */
    @Override
    public String getElementAt(int index) {
        return Optional.ofNullable(files.get(index))
              .map(File::getName)
              .orElse(null);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        // No-op
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        // No-op
    }

    public File getSelectedFile() {
        return selectedFile;
    }
}
