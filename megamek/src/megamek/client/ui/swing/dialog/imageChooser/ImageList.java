/* MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team  
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui.swing.dialog.imageChooser;

import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import megamek.common.util.fileUtils.DirectoryItem;

/**
 * A specialized JList to display a list of (ImageChoiceDialog.DirectoryItem)s
 * for the ImageChoiceDialog, e.g. camos or portraits. The images
 * will be displayed with horizontal wrap. This is best embedded
 * in a JScrollpane. 
 * Using any of the renderers in the package the images can be displayed
 * with or without the filename.
 */
public class ImageList extends JList<DirectoryItem> {

    private static final long serialVersionUID = -8060324139099113292L;

    /** The list model; a default model. */
    private DefaultListModel<DirectoryItem> itemModel;
    
    /**
     * A specialized JList to display a list of (ImageChoiceDialog.DirectoryItem)s
     * for the ImageChoiceDialog, e.g. camos or portraits. The images
     * will be displayed with horizontal wrap. This is best embedded
     * in a JScrollpane. 
     * Using any of the renderers in the package the images can be displayed
     * with or without the filename.
     */
    public ImageList(ListCellRenderer<DirectoryItem> renderer) {
        super(); 
        itemModel = new DefaultListModel<DirectoryItem>();
        setModel(itemModel);
        
        setOpaque(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayoutOrientation(JList.HORIZONTAL_WRAP);
        setFixedCellWidth(100);
        setFixedCellHeight(110);
        setVisibleRowCount(-1);
        setCellRenderer(renderer);
    }
    
    /** 
     * Updates the list to show (only) the given items.
     */
    public void updateImages(List<DirectoryItem> items) {
        itemModel.clear();
        // LGTM does not accept addAll
        for (DirectoryItem di: items) {
            itemModel.addElement(di);
        }
    }
}
