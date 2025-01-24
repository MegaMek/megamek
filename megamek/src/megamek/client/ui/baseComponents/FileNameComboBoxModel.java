/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.ui.baseComponents;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The FileNameComboBoxModel class is a ComboBoxModel that uses the file name as the value.
 * It receives a list or array of files and instead of showing the stringfied version of the file
 * it shows only the value of {@link File#getName()} method.
 * It has a helper function to get the selected file on its own if necessary.
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

    @Override
    public String getElementAt(int index) {
        return files.get(index).getName();
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
