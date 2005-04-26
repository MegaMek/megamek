/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.util.widget;

import java.awt.*;
import java.util.*;

import megamek.client.GUIPreferences;
import megamek.client.Messages;
import megamek.common.*;

/**
 * Class which keeps set of all areas required to 
 * represent Protomech unit in MechDisplay.ArmorPanel class.
 */
public class ProtomechMapSet implements DisplayMapSet{

    // Boring list of labels.
    private PMValueLabel[] sectionLabels =
        new PMValueLabel[Protomech.NUM_PMECH_LOCATIONS];
    private PMValueLabel[] armorLabels =
        new PMValueLabel[Protomech.NUM_PMECH_LOCATIONS];
    private PMValueLabel[] internalLabels =
        new PMValueLabel[Protomech.NUM_PMECH_LOCATIONS];
    /* BEGIN killme block **
    //Picture with figure
	private Image battleArmorImage;
	//Images that shows how much armor + 1 internal damage left.
	private Image[] armorImage = new Image[5];
	//Set of areas to show BA figures
	private PMPicArea[] unitAreas =  new PMPicArea[5];
	//Set of areas to show BA armor left
	private PMPicArea[] armorAreas = new PMPicArea[5];
	//Set of labels to show BA armor left
	private PMValueLabel[]    armorLabels = new PMValueLabel[5];
    **  END  killme block */

    //Reference to Component (required for Image handling)
    private Component comp;
    //Content group which will be sent to PicMap component
    private PMAreasGroup content = new PMAreasGroup();
    //Set of Backgrpund drawers which will be sent to PicMap component
    private Vector    bgDrawers = new Vector();
	
    private int stepY = 53;
	
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, GUIPreferences.getInstance().getMechDisplayArmorLargeFontSize()); //$NON-NLS-1$
	
    /**
     * This constructor have to be called anly from addNotify() method
     */
    public ProtomechMapSet( Component c){
        comp = c;
        setAreas();
        setBackGround();
    }

    /*
    ** Set the armor diagram on the mapset.
    */
    private void setAreas() {
        FontMetrics fm = comp.getFontMetrics(FONT_VALUE);
		
        for(int i = 0; i < Protomech.NUM_PMECH_LOCATIONS; i++){
            int shiftY = i * stepY;

            sectionLabels[i] = new PMValueLabel(fm, Color.white.brighter());
            sectionLabels[i]. moveTo(0, shiftY + 24);
            content.addArea((PMElement) sectionLabels[i]);

            armorLabels[i] = new PMValueLabel(fm, Color.yellow.brighter());
            armorLabels[i]. moveTo(80, shiftY + 24);
            content.addArea((PMElement) armorLabels[i]);

            internalLabels[i] = new PMValueLabel(fm, Color.red.brighter());
            internalLabels[i]. moveTo(160, shiftY + 24);
            content.addArea((PMElement) internalLabels[i]);
        }
    }
	
    public PMAreasGroup getContentGroup(){
        return content;
    }
    
    public Vector getBackgroundDrawers(){
    	return bgDrawers;
    }

    /**
     * Show the diagram for the given Protomech.
     *
     * @param   entity - the <code>Entity</code> to be displayed.
     *          This should be a <code>Protomech</code> unit.
     */
    public void setEntity(Entity entity){
    	Protomech proto = (Protomech) entity;
    	int armor = 0;
    	int internal =0;
        int loc = proto.locations();

        // Not all Protomechs have a Main Gun.
        if ( Protomech.NUM_PMECH_LOCATIONS == loc ){
            sectionLabels[Protomech.NUM_PMECH_LOCATIONS - 1].setVisible(true);
            armorLabels[Protomech.NUM_PMECH_LOCATIONS - 1].setVisible(true);
            internalLabels[Protomech.NUM_PMECH_LOCATIONS - 1].setVisible(true);
        } else{
            sectionLabels[Protomech.NUM_PMECH_LOCATIONS - 1].setVisible(false);
            armorLabels[Protomech.NUM_PMECH_LOCATIONS - 1].setVisible(false);
            internalLabels[Protomech.NUM_PMECH_LOCATIONS - 1].setVisible(false);
        }

        // Set the armor and internal labels for each of the sections.
        for ( int i = 0; i < loc ; i++ ) {

            // Handle ARMOR_NA, ARMOR_DOOMED< and ARMOR_DESTROYED.
            armor = proto.getArmor(i, false);
            if ( armor < 0 ) {
                armor = 0;
            }
       	    internal = proto.getInternal(i);
            if ( internal < 0 ) {
                internal = 0;
            }

            // Now set the labels.
            sectionLabels[i].setValue( proto.getLocationName(i) );
            armorLabels[i].setValue( Integer.toString(armor) );
       	    if ( armor + internal == 0 ) {
       	    	internalLabels[i].setValue( Messages.getString("ProtomechMapSet.Destroyed") ); //$NON-NLS-1$
       	    } else {
       	    	internalLabels[i].setValue( Integer.toString(internal) );
       	    }

        } // Handle the next location.

    }

    /*
    ** Sets the background on the mapset.
    */
    private void setBackGround() {
        Image tile = comp.getToolkit().getImage("data/widgets/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
        b = BackGroundDrawer.TILING_HORIZONTAL | 
            BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));                
        
        b = BackGroundDrawer.TILING_HORIZONTAL | 
            BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
        b = BackGroundDrawer.TILING_VERTICAL | 
            BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
        b = BackGroundDrawer.TILING_VERTICAL | 
            BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
                
        
        b = BackGroundDrawer.NO_TILING | 
            BackGroundDrawer.VALIGN_TOP |
            BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
        b = BackGroundDrawer.NO_TILING | 
            BackGroundDrawer.VALIGN_BOTTOM |
            BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
        b = BackGroundDrawer.NO_TILING | 
            BackGroundDrawer.VALIGN_TOP |
            BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
        b = BackGroundDrawer.NO_TILING | 
            BackGroundDrawer.VALIGN_BOTTOM |
            BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile, comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
         
    }

}
