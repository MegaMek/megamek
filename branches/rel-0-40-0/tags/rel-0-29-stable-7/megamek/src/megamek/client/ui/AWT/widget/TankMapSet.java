/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
import megamek.client.*;
import megamek.common.*;

/**
 * Class which keeps set of all areas required to 
 * represent Tank unit in MechDsiplay.ArmorPanel class.
 */



public class TankMapSet implements DisplayMapSet{
  
  private Component comp;
  private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[12];
    private PMSimpleLabel[] labels = new PMSimpleLabel[12];
    private PMValueLabel[] vLabels = new PMValueLabel[12];
    private Vector  bgDrawers = new Vector();
    private PMAreasGroup content = new PMAreasGroup();
  
  private final int INT_STR_OFFSET = 6;
  //Polygons for all areas
  private Polygon frontArmor = new Polygon( new int[]{0,19,109,128,105,92,37,23},
                                              new int[]{55,27,27,55,68,49,49,68}, 8);
        //front internal structure
    private Polygon frontIS = new Polygon(new int[]{67,67,105,92,37,23,61,61},
                                          new int[]{40,77,39,20,20,39,77,40},8);
       //Left armor
    private Polygon leftArmor = new Polygon(new int[]{0,0,23,23},
                                            new int[]{26,214,200,39},4);  
       
        //Left internal structure
    private Polygon leftIS = new Polygon(new int[]{61,23,23,23,40,33,33,46,54,58,61},
                                         new int[]{77,39,200,200,183,168,120,94,94,85,85},
                                         11);
       //Right armor
    private Polygon rightArmor = new Polygon(new int[]{128,105,105,128},
                                             new int[]{26,39,200,214}, 4);

       //Right internal structure
       
    private Polygon rightIS = new Polygon (new int[]{83,96,96,88,105,105,105,67,67,71,75},
                                           new int[]{94,120,168,183,200,200,39,77,85,85,94},
                                           11);  

      //Rear armor
    private Polygon rearArmor = new Polygon (new int[]{128,105,92,35,23,0,11,116},
                                             new int[]{214,200,220,220,200,214,239,239},8);
      //Rear internal structure
    private Polygon rearIS = new Polygon(new int[]{105,88,79,50,40,23,35,92},
                                         new int[]{200,183,202,202,183,200,220,220}, 8);
       //Turret armor
    private Polygon turretArmor = new Polygon(new int[]{64,74,89,89,39,39,54,64,64,64,
                                                        64,50,33,33,46,54,58,61,61,67,
                                                        67,71,75,83,96,96,88,79,64},
                                              new int[]{187,187,160,139,139,160,187,
                                                        187,202,187,202,202,168,120,
                                                        94,94,85,85,40,40,85,85,94,94,
                                                        120,168,183,202,202},29);
       //Turret internal structure
    private Polygon turretIS = new Polygon(new int[]{39,39,54,74,89,89},
                                           new int[]{139,160,187,187,160,139},6);

    
    private static final Font       FONT_LABEL = new Font("SansSerif", Font.PLAIN, 9);
    private static final Font       FONT_VALUE = new Font("SansSerif", Font.PLAIN, 12);
 
  
  
  public TankMapSet(Component c){
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
    
    public Vector getBackgroundDrawers(){
        return bgDrawers;
    }
    
    public void setEntity(Entity e){
      Tank t = (Tank) e;
      int a = 1;
        int a0 = 1;
      for(int i = 1; i < 6; i++){
           a = t.getArmor(i);
             a0 = t.getOArmor(i);
             vLabels[i].setValue(t.getArmorString(i));
             setAreaColor(areas[i], vLabels[i], (double)a/(double)a0);
      }
      for(int i = 7; i < 12; i++){
             a = t.getInternal(i-6);
             a0 = t.getOInternal(i-6);
             vLabels[i].setValue(t.getInternalString(i-6));
             setAreaColor(areas[i], vLabels[i], (double)a/(double)a0);
      }
      
    }
    
    private void setContent(){
      for(int i = 1; i < 6; i++){
        content.addArea(areas[i]);
        content.addArea(labels[i]);
        content.addArea(vLabels[i]);
      }
      for(int i = 1; i < 6; i++){
        content.addArea(areas[i + INT_STR_OFFSET]);
        content.addArea(labels[i + INT_STR_OFFSET]);
        content.addArea(vLabels[i + INT_STR_OFFSET]);
      }
    }
  
  private void setAreas(){
    areas[Tank.LOC_FRONT] = new PMSimplePolygonArea(frontArmor);
    areas[Tank.LOC_RIGHT] = new PMSimplePolygonArea(rightArmor);
    areas[Tank.LOC_LEFT] = new PMSimplePolygonArea(leftArmor);
    areas[Tank.LOC_REAR] = new PMSimplePolygonArea(rearArmor);
    areas[Tank.LOC_TURRET] = new PMSimplePolygonArea(turretArmor);
    areas[Tank.LOC_FRONT + INT_STR_OFFSET] = new PMSimplePolygonArea(frontIS);
    areas[Tank.LOC_RIGHT + INT_STR_OFFSET] = new PMSimplePolygonArea(rightIS);
    areas[Tank.LOC_LEFT + INT_STR_OFFSET] = new PMSimplePolygonArea(leftIS);
    areas[Tank.LOC_REAR + INT_STR_OFFSET] = new PMSimplePolygonArea(rearIS);
    areas[Tank.LOC_TURRET + INT_STR_OFFSET] = new PMSimplePolygonArea(turretIS);
  }
  
  private void setLabels(){
    FontMetrics fm = comp.getFontMetrics(FONT_LABEL);
    
    //Labels for Front view
    labels[Tank.LOC_FRONT] = createLabel("Front Armor", fm, Color.black,65,35);
    labels[Tank.LOC_FRONT + INT_STR_OFFSET] = createLabel("Front I.S.", fm, Color.black,63,57);
    labels[Tank.LOC_LEFT] = createLabel("LS", fm, Color.black,19,135);
    labels[Tank.LOC_LEFT + INT_STR_OFFSET] = createLabel("L. I.S.", fm, Color.black,49,106);
    labels[Tank.LOC_RIGHT] = createLabel("RS", fm, Color.black,124,135);
    labels[Tank.LOC_RIGHT + INT_STR_OFFSET] = createLabel("R. I.S.", fm, Color.black,95,106);
    labels[Tank.LOC_REAR] = createLabel("Rear Armor", fm, Color.black,65,257);
    labels[Tank.LOC_REAR + INT_STR_OFFSET] = createLabel("Rear I.S.", fm, Color.black,63,239);
    labels[Tank.LOC_TURRET] = createLabel("Turret Armor", fm, Color.black,73,145);
    labels[Tank.LOC_TURRET + INT_STR_OFFSET] = createLabel("Turret I.S.", fm, Color.black,73,173);
    
    //Value labels for all parts of mek
        //front
    fm =  comp.getFontMetrics(FONT_VALUE);   
    vLabels[Tank.LOC_FRONT] = createValueLabel(101, 37, "", fm);
    vLabels[Tank.LOC_FRONT + INT_STR_OFFSET] = createValueLabel(91, 58, "", fm);
    vLabels[Tank.LOC_LEFT] = createValueLabel(20, 150, "", fm);
    vLabels[Tank.LOC_LEFT + INT_STR_OFFSET] = createValueLabel(44, 121, "", fm);
    vLabels[Tank.LOC_RIGHT] = createValueLabel(125, 150, "", fm);
    vLabels[Tank.LOC_RIGHT + INT_STR_OFFSET] = createValueLabel(102, 121, "", fm);
    vLabels[Tank.LOC_REAR] = createValueLabel(99, 258, "", fm);
    vLabels[Tank.LOC_REAR + INT_STR_OFFSET] = createValueLabel(91, 241, "", fm);
    vLabels[Tank.LOC_TURRET] = createValueLabel(73, 159, "", fm);
    vLabels[Tank.LOC_TURRET + INT_STR_OFFSET] = createValueLabel(73, 193, "", fm);
    }
    
    private void setBackGround(){
        Image tile = comp.getToolkit().getImage("data/widgets/tile.gif");
        PMUtil.setImage(tile,comp);
        int b = BackGroundDrawer.TILING_BOTH;
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_TOP;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));                
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = comp.getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
                
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/tl_corner.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = comp.getToolkit().getImage("data/widgets/bl_corner.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/tr_corner.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = comp.getToolkit().getImage("data/widgets/br_corner.gif");
        PMUtil.setImage(tile,comp);
        bgDrawers.addElement(new BackGroundDrawer (tile,b));
    }
    
    private void translateAreas(){
      areas[Tank.LOC_FRONT].translate(8,0);
        areas[Tank.LOC_FRONT + INT_STR_OFFSET].translate(8,29);
        areas[Tank.LOC_LEFT].translate(8,29);
        areas[Tank.LOC_LEFT + INT_STR_OFFSET].translate(8,29);
        areas[Tank.LOC_RIGHT].translate(8,29);
        areas[Tank.LOC_RIGHT + INT_STR_OFFSET].translate(8,29);
        areas[Tank.LOC_REAR].translate(8, 29);
        areas[Tank.LOC_REAR + INT_STR_OFFSET].translate(8,29);
        areas[Tank.LOC_TURRET].translate(8,29);
        areas[Tank.LOC_TURRET + INT_STR_OFFSET].translate(8,29);
    }
    
    private PMSimpleLabel createLabel(String s, FontMetrics fm,Color color, int x, int y){
        PMSimpleLabel l = new PMSimpleLabel(s, fm, color);
        centerLabelAt(l,x,y);
        return l; 
    }
    
    private PMValueLabel createValueLabel(int x, int y, String v, FontMetrics fm){
        PMValueLabel l = new PMValueLabel(fm, Color.red);
        centerLabelAt(l, x, y);
        l.setValue(v);
        return l;
    }
    
    private void centerLabelAt(PMSimpleLabel l, int x, int y){
        if (l == null) return;
        Dimension d = l.getSize();
        l.moveTo( x - d.width/2, y + d.height/2); 
    }
    
    private void setAreaColor(PMSimplePolygonArea ha, PMValueLabel l, double percentRemaining) {
        if ( percentRemaining <= 0 ){
            ha.backColor = Color.darkGray.brighter();
            l.setColor(Color.blue);
            ha.highlightBorderColor = Color.green;          
        } else if ( percentRemaining <= .25 ){
            ha.backColor = Color.red.brighter();
            l.setColor(Color.blue);
            ha.highlightBorderColor = Color.green;          
        } else if ( percentRemaining <= .75 ){
            ha.backColor = Color.yellow;
            l.setColor(Color.blue);
            ha.highlightBorderColor = Color.green;
        } else {
            ha.backColor = Color.gray.brighter();
            l.setColor(Color.red);
            ha.highlightBorderColor = Color.red;
        }
    }
    
}
