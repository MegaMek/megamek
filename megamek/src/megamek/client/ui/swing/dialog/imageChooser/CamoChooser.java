/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import java.awt.Window;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.tileset.CamoManager;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.IPlayer;
import megamek.common.util.fileUtils.DirectoryItem;

/**
 * This dialog allows players to select the camo pattern (or color) used by
 * their units during the game. It automatically fills itself with all the color
 * choices in IPlayer and all the camo patterns in the
 * {@link Configuration#camoDir()} directory tree.
 * Should be shown by using showDialog(IPlayer) or showDialog(Entity). These
 * methods return either JOptionPane.OK_OPTION or .CANCEL_OPTION.
 * 
 * @see AbstractImageChooser
 */
public class CamoChooser extends AbstractImageChooser {
    
    private static final long serialVersionUID = -8060324139099113292L;

    /** When true, camos from all subdirectories of the current selection are shown. */
    private boolean includeSubDirs = true;
    
    /** True when an individual camo is being selected for an entity. */
    private boolean individualCamo = false;
    
    /** When an individual camo is being selected, the player's camo is displayed as a reset option. */
    private DirectoryItem entityOwnerCamo;
    
    /** Creates a dialog that allows players to choose a camo pattern. */
    public CamoChooser(Window parent) {
        super(parent, Messages.getString("CamoChoiceDialog.select_camo_pattern"), 
                new CamoRenderer(), new CamoChooserTree());
        showSearch(true);
    }
    
    /** 
     * Show the camo choice dialog and pre-select the camo or color
     * of the given player. The dialog will allow choosing camos
     * and colors. Also refreshes the camos from disk.
     */
    public int showDialog(IPlayer player) {
        refreshCamos();
        individualCamo = false;
        setPlayer(player);
        return showDialog();
    }

    /** 
     * Show the camo choice dialog and pre-select the camo or color
     * of the given entity. The dialog will allow choosing camos
     * and the single color of the player owning the entity.
     * Also refreshes the camos from disk.
     */
    public int showDialog(Entity entity) {
        refreshCamos();
        individualCamo = true;
        setEntity(entity);
        return showDialog();
    }
    
    /** Reloads the camo directory from disk. */
    private void refreshCamos() {
        CamoManager.refreshDirectory();
        refreshDirectory(new CamoChooserTree());
    }
    
    @Override
    protected ArrayList<DirectoryItem> getItems(String category) {
        
        ArrayList<DirectoryItem> result = new ArrayList<>();
        
        // If a player camo is being selected, the items presented in the 
        // NO_CAMO section are the available player colors. 
        // If an individual camo is being selected, then the only item presented 
        // in the NO_CAMO section is the owner's camo. This can be chosen 
        // to remove the individual camo. 
        if (category.startsWith(IPlayer.NO_CAMO)) {
            if (individualCamo) {
                result.add(entityOwnerCamo);
            } else {
                for (String color: IPlayer.colorNames) {
                    result.add(new DirectoryItem(IPlayer.NO_CAMO, color));
                }
            } 
            return result;
        }
        
        // In any other camo section, the camos of the selected category are
        // presented. When the includeSubDirs flag is true, all categories
        // below the selected one are also presented.
        if (includeSubDirs) {
            for (Iterator<String> catNames = CamoManager.getCamos().getCategoryNames(); catNames.hasNext(); ) {
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
    
    @Override
    protected ArrayList<DirectoryItem> getSearchedItems(String searched) {
        
        // For a category that contains the search string, all its items
        // are added to the list. Additionally, all items that contain 
        // the search string are added.

        ArrayList<DirectoryItem> result = new ArrayList<>();
        String lowerSearched = searched.toLowerCase();
        
        for (Iterator<String> catNames = CamoManager.getCamos().getCategoryNames(); catNames.hasNext(); ) {
            String tcat = catNames.next(); 
            if (tcat.toLowerCase().contains(lowerSearched)) {
                addCategoryItems(tcat, result);
                continue;
            }
            for (Iterator<String> itemNames = CamoManager.getCamos().getItemNames(tcat); itemNames.hasNext(); ) {
                String item = itemNames.next();
                if (item.toLowerCase().contains(lowerSearched)) {
                    result.add(new DirectoryItem(tcat, item));
                }
            }
        }

        return result;
    }

    /** 
     * Adds the camos of the given category to the given items ArrayList.
     * Assumes that the root of the path (IPlayer.ROOT_CAMO) is passed as ""! 
     * */
    private void addCategoryItems(String category, List<DirectoryItem> items) {
        for (Iterator<String> camoNames = CamoManager.getCamos().getItemNames(category); camoNames.hasNext(); ) {
            items.add(new DirectoryItem(category, camoNames.next()));
        }
    }

    /** Preselects the Tree and the Images with the given player's camo. */ 
    private void setPlayer(IPlayer player) {
        String category = player.getCamoCategory();
        String filename;
        if (category.equals(IPlayer.NO_CAMO)) {
            filename = IPlayer.colorNames[player.getColorIndex()];
        } else {
            filename = player.getCamoFileName();
        }
        
        setSelection(category, filename);
    }

    /** 
     * Preselects the Tree and the Images with the given entity's camo
     * or the owner's, if the entity has no individual camo. Also stores
     * the owner's camo to present a "revert to no individual" camo option.
     */
    private void setEntity(Entity entity) {
        // Store the owner's camo to display as the only "No Camo" option
        // This may be a color
        String item = entity.getOwner().getCamoFileName();
        if (entity.getOwner().getCamoCategory().equals(IPlayer.NO_CAMO)) {
            item = IPlayer.colorNames[entity.getOwner().getColorIndex()];
        }
        entityOwnerCamo = new DirectoryItem(entity.getOwner().getCamoCategory(), item);

        // Set the camo category and filename to the entity's if it has one,
        // otherwise to the corresponding player's camo category
        if (entity.getCamoCategory() == null) {
            setPlayer(entity.getOwner());
        } else {
            String category = entity.getCamoCategory();
            String filename = entity.getCamoFileName();
            setSelection(category, filename);
        }
    }
        
}