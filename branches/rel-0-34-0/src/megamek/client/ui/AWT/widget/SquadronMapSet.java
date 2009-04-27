/**
 * MegaMek - Copyright (C) 2000,2001,2002,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT.widget;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.FighterSquadron;
import megamek.common.Entity;

/**
 * Class which keeps set of all areas required to 
 * represent Capital Fighter unit in MechDsiplay.ArmorPanel class.
 */
public class SquadronMapSet implements DisplayMapSet{

    private static final String IMAGE_DIR = "data/images/widgets";
    
    private Component comp;
//  Images that shows how much armor left.
    private Image[] armorImage = new Image[FighterSquadron.MAX_SIZE];
    // Set of areas to show fighter armor left
    private PMPicArea[] armorArea = new PMPicArea[FighterSquadron.MAX_SIZE];
    // Set of labels to show fighter armor left
    //images and areas for each crit tally
    private Image[] avCritImage = new Image[FighterSquadron.MAX_SIZE];
    private PMPicArea[] avCritArea = new PMPicArea[FighterSquadron.MAX_SIZE];
    private Image[] engineCritImage = new Image[FighterSquadron.MAX_SIZE];
    private PMPicArea[] engineCritArea = new PMPicArea[FighterSquadron.MAX_SIZE];
    private Image[] fcsCritImage = new Image[FighterSquadron.MAX_SIZE];
    private PMPicArea[] fcsCritArea = new PMPicArea[FighterSquadron.MAX_SIZE];
    private Image[] sensorCritImage = new Image[FighterSquadron.MAX_SIZE];
    private PMPicArea[] sensorCritArea = new PMPicArea[FighterSquadron.MAX_SIZE];
    private Image[] pilotCritImage = new Image[FighterSquadron.MAX_SIZE];
    private PMPicArea[] pilotCritArea = new PMPicArea[FighterSquadron.MAX_SIZE];
    private PMSimpleLabel[] nameLabel = new PMSimpleLabel[FighterSquadron.MAX_SIZE];
    private PMValueLabel[] armorVLabel = new PMValueLabel[FighterSquadron.MAX_SIZE];
    private PMSimpleLabel[] avCritLabel = new PMSimpleLabel[FighterSquadron.MAX_SIZE];
    private PMSimpleLabel[] engineCritLabel = new PMSimpleLabel[FighterSquadron.MAX_SIZE];
    private PMSimpleLabel[] fcsCritLabel = new PMSimpleLabel[FighterSquadron.MAX_SIZE];
    private PMSimpleLabel[] sensorCritLabel = new PMSimpleLabel[FighterSquadron.MAX_SIZE];
    private PMSimpleLabel[] pilotCritLabel = new PMSimpleLabel[FighterSquadron.MAX_SIZE];
    private Vector<BackGroundDrawer>  bgDrawers = new Vector<BackGroundDrawer>();
    private PMAreasGroup content = new PMAreasGroup();

    private int stepY = 11;
    private int squareSize = 7;
    private int armorRows = 6;
    private int armorCols = 8;
    
    private static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 9); //$NON-NLS-1$
 
    public SquadronMapSet(Component c){
        comp = c;
        setAreas();
        setLabels();
        setBackGround();
        translateAreas();
        setContent();
    }

    public void setRest(){
    }

    public PMAreasGroup getContentGroup(){
        return content;
    }

    public Vector<BackGroundDrawer> getBackgroundDrawers(){
        return bgDrawers;
    }

    public void setEntity(Entity e){
        FighterSquadron fs = (FighterSquadron) e;
        
        for(int i = 0; i < fs.getN0Fighters(); i++) {
            Aero fighter = fs.getFighter(i);
            int armor = fighter.getCapArmor();
            int armorO = fighter.getCap0Armor();
            
            drawArmorImage(armorImage[i], armor, armorO);
            armorVLabel[i].setValue(Integer.toString(armor));
            drawCrits(avCritImage[i], fighter.getAvionicsHits());
            drawCrits(engineCritImage[i], fighter.getEngineHits());
            drawCrits(fcsCritImage[i], fighter.getFCSHits());
            drawCrits(sensorCritImage[i], fighter.getSensorHits());
            drawCrits(pilotCritImage[i], fighter.getCrew().getHits());
            
            nameLabel[i].setString(fighter.getDisplayName());
            
            armorArea[i].setVisible(true);
            armorVLabel[i].setVisible(true);
            avCritArea[i].setVisible(true);
            engineCritArea[i].setVisible(true);
            fcsCritArea[i].setVisible(true);
            sensorCritArea[i].setVisible(true);
            pilotCritArea[i].setVisible(true);
            nameLabel[i].setVisible(true);
            avCritLabel[i].setVisible(true);
            engineCritLabel[i].setVisible(true);
            fcsCritLabel[i].setVisible(true);
            sensorCritLabel[i].setVisible(true);
            pilotCritLabel[i].setVisible(true);
        }
        
        for(int j = fs.getN0Fighters(); j < FighterSquadron.MAX_SIZE; j++) {
            armorArea[j].setVisible(false);
            armorVLabel[j].setVisible(false);
            avCritArea[j].setVisible(false);
            engineCritArea[j].setVisible(false);
            fcsCritArea[j].setVisible(false);
            sensorCritArea[j].setVisible(false);
            pilotCritArea[j].setVisible(false);
            nameLabel[j].setVisible(false);
            avCritLabel[j].setVisible(false);
            engineCritLabel[j].setVisible(false);
            fcsCritLabel[j].setVisible(false);
            sensorCritLabel[j].setVisible(false);
            pilotCritLabel[j].setVisible(false);
        }
        
    }

    private void setContent(){
        
        for(int i = 0; i < FighterSquadron.MAX_SIZE; i++) {
            content.addArea(nameLabel[i]);
            content.addArea(armorArea[i]);
            content.addArea(armorVLabel[i]);
            content.addArea(avCritLabel[i]);
            content.addArea(engineCritLabel[i]);
            content.addArea(fcsCritLabel[i]);
            content.addArea(sensorCritLabel[i]);
            content.addArea(pilotCritLabel[i]);     
            content.addArea(avCritArea[i]);
            content.addArea(engineCritArea[i]);
            content.addArea(fcsCritArea[i]);
            content.addArea(sensorCritArea[i]);
            content.addArea(pilotCritArea[i]);
        }
    }

    private void setAreas(){
        for(int i = 0; i < FighterSquadron.MAX_SIZE; i++) {
            armorImage[i] = comp.createImage(armorCols*(squareSize+1), armorRows*(squareSize+1));
            armorArea[i] = new PMPicArea(armorImage[i]);
            
            avCritImage[i] = comp.createImage(3*(squareSize+1), squareSize+1);
            avCritArea[i] = new PMPicArea(avCritImage[i]);
            engineCritImage[i] = comp.createImage(3*(squareSize+1), squareSize+1);
            engineCritArea[i] = new PMPicArea(engineCritImage[i]);
            fcsCritImage[i] = comp.createImage(3*(squareSize+1), squareSize+1);
            fcsCritArea[i] = new PMPicArea(fcsCritImage[i]);
            sensorCritImage[i] = comp.createImage(3*(squareSize+1), squareSize+1);
            sensorCritArea[i] = new PMPicArea(sensorCritImage[i]);
            pilotCritImage[i] = comp.createImage(6*(squareSize+1), squareSize+1);
            pilotCritArea[i] = new PMPicArea(pilotCritImage[i]);
        }
    }

    private void setLabels(){
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);
        for(int i = 0; i < FighterSquadron.MAX_SIZE; i++) {
            nameLabel[i] = new PMSimpleLabel("Unknown", fm, Color.white); //$NON-NLS-1$
            armorVLabel[i] = new PMValueLabel(fm, Color.red.brighter());    
            avCritLabel[i] = new PMSimpleLabel("Avionics:", fm, Color.white); //$NON-NLS-1$
            engineCritLabel[i] = new PMSimpleLabel("Engine:", fm, Color.white); //$NON-NLS-1$
            fcsCritLabel[i] = new PMSimpleLabel("FCS:", fm, Color.white); //$NON-NLS-1$
            sensorCritLabel[i] = new PMSimpleLabel("Sensors:", fm, Color.white); //$NON-NLS-1$
            pilotCritLabel[i] = new PMSimpleLabel("Pilot hits:", fm, Color.white); //$NON-NLS-1$
        }
    }

    private void setBackGround(){
        Image tile = comp.getToolkit().getImage(IMAGE_DIR+"/tile.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer (tile,b));

        b = BackGroundDrawer.TILING_HORIZONTAL | 
        BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));                

        b = BackGroundDrawer.TILING_HORIZONTAL | 
        BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/h_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));

        b = BackGroundDrawer.TILING_VERTICAL | 
        BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));

        b = BackGroundDrawer.TILING_VERTICAL | 
        BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/v_line.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));


        b = BackGroundDrawer.NO_TILING | 
        BackGroundDrawer.VALIGN_TOP |
        BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/tl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));

        b = BackGroundDrawer.NO_TILING | 
        BackGroundDrawer.VALIGN_BOTTOM |
        BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/bl_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));

        b = BackGroundDrawer.NO_TILING | 
        BackGroundDrawer.VALIGN_TOP |
        BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/tr_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));

        b = BackGroundDrawer.NO_TILING | 
        BackGroundDrawer.VALIGN_BOTTOM |
        BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage(IMAGE_DIR+"/br_corner.gif"); //$NON-NLS-1$
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
    }
    
    private void translateAreas() { 
        //get size of each fighter block
        int blockSize = 6*stepY;
        for(int i = 0; i < FighterSquadron.MAX_SIZE; i++) {
            nameLabel[i].translate(0, blockSize*i);
            armorArea[i].translate(0, squareSize+blockSize*i);
            armorVLabel[i].translate((armorCols*(squareSize+1))/2, blockSize*i+squareSize+(armorRows*(squareSize+1))/2);
            
            avCritLabel[i].translate(5+armorCols*(squareSize+1), stepY+blockSize*i);
            engineCritLabel[i].translate(5+armorCols*(squareSize+1), 2*stepY+blockSize*i);
            fcsCritLabel[i].translate(5+armorCols*(squareSize+1), 3*stepY+blockSize*i);
            sensorCritLabel[i].translate(5+armorCols*(squareSize+1), 4*stepY+blockSize*i);
            pilotCritLabel[i].translate(5+armorCols*(squareSize+1), 5*stepY+blockSize*i);
            
            avCritArea[i].translate(10+pilotCritLabel[0].width+armorCols*(squareSize+1), stepY-(squareSize+1)+blockSize*i);
            engineCritArea[i].translate(10+pilotCritLabel[0].width+armorCols*(squareSize+1), 2*stepY-(squareSize+1)+blockSize*i);
            fcsCritArea[i].translate(10+pilotCritLabel[0].width+armorCols*(squareSize+1), 3*stepY-(squareSize+1)+blockSize*i);
            sensorCritArea[i].translate(10+pilotCritLabel[0].width+armorCols*(squareSize+1), 4*stepY-(squareSize+1)+blockSize*i);
            pilotCritArea[i].translate(10+pilotCritLabel[0].width+armorCols*(squareSize+1), 5*stepY-(squareSize+1)+blockSize*i);
        }
    }

    private void drawCrits(Image im, int crits) {
        int w = im.getWidth(null);
        int h = im.getHeight(null);
        Graphics g = im.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < crits; i++) {
            g.setColor(Color.red.darker());
            g.fillRect(i*(squareSize+1), 0, squareSize, squareSize);
        }
    }
    
//  Redraws armor images
    private void drawArmorImage(Image im, int a, int initial) {
        int w = im.getWidth(null);
        int h = im.getHeight(null);
        Graphics g = im.getGraphics();
        g.setColor(Color.gray);
        g.fillRect(0, 0, w, h);
        //first fill up the initial armor area with black
        for (int i = 0; i < initial; i++) {
            //6 across and 8 down
            int row = i / armorRows;
            int column = i - row * armorRows;
            g.setColor(Color.black);
            g.fillRect(row*(squareSize+1),column*(squareSize+1), (squareSize+1), (squareSize+1));
        }
        for (int i = 0; i < a; i++) {
            int row = i / armorRows;
            int column = i - row * armorRows;
            g.setColor(Color.green.darker());
            g.fillRect(row*(squareSize+1),column*(squareSize+1), squareSize, squareSize);
        }
    }

}
