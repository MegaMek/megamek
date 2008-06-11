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

package megamek.client.ui.swing.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Polygon;
import java.util.Vector;

import javax.swing.JComponent;

import megamek.client.ui.swing.GUIPreferences;
import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.SmallCraft;

/**
 * Class which keeps set of all areas required to 
 * represent ASF unit in MechDsiplay.ArmorPanel class.
 */
public class SpheroidMapSet implements DisplayMapSet{

    private static final String IMAGE_DIR = "data/images/widgets";

    private JComponent comp;
    private PMSimplePolygonArea[] areas = new PMSimplePolygonArea[5];
    private PMSimpleLabel[] labels = new PMSimpleLabel[11];
    private PMValueLabel[] vLabels = new PMValueLabel[11];
    private Vector<BackGroundDrawer>  bgDrawers = new Vector<BackGroundDrawer>();
    private PMAreasGroup content = new PMAreasGroup();

    //private static final int INT_STR_OFFSET = 4;
    //Polygons for all areas
    private Polygon noseArmor = new Polygon( new int[]{0,20,80,100},
            new int[]{50,0,0,50}, 4);
    //front internal structure
    private Polygon Structure = new Polygon(new int[]{40,60,60,40},
            new int[]{50,50,150,150},4);
    //Left armor
    private Polygon leftWingArmor = new Polygon(new int[]{0,40,40,0},
            new int[]{50,50,150,150},4);  


    //Right armor
    private Polygon rightWingArmor = new Polygon(new int[]{60,100,100,60},
            new int[]{50,50,150,150}, 4);

    //Rear armor
    private Polygon aftArmor = new Polygon (new int[]{0,20,80,100},
            new int[]{150,200,200,150},4);

    private static final Font       FONT_LABEL = new Font("SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorSmallFontSize")); //$NON-NLS-1$
    private static final Font       FONT_VALUE = new Font("SansSerif", Font.PLAIN, GUIPreferences.getInstance().getInt("AdvancedMechDisplayArmorLargeFontSize")); //$NON-NLS-1$



    public SpheroidMapSet(JComponent c){
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
        Aero t = (Aero) e;
        int a = 1;
        int a0 = 1;
        //TODO: change this back to locations
        for(int i = 0; i < t.locations(); i++){
            a = t.getArmor(i);
            a0 = t.getOArmor(i);
            vLabels[i].setValue(t.getArmorString(i));
            WidgetUtils.setAreaColor(areas[i], vLabels[i], (double)a/(double)a0);

        }
        a = t.getSI();
        a0 = t.get0SI();
        vLabels[4].setValue(Integer.toString(t.getSI()));
        WidgetUtils.setAreaColor(areas[4], vLabels[4], (double)a/(double)a0);

        //now for the vitals
        vLabels[5].setValue(getCriticalHitTally(t.getAvionicsHits(),3));
        vLabels[6].setValue(getCriticalHitTally(t.getEngineHits(),t.getMaxEngineHits()));
        vLabels[7].setValue(getCriticalHitTally(t.getFCSHits(),3));
        vLabels[8].setValue(getCriticalHitTally(t.getSensorHits(),3));
        if(t instanceof SmallCraft) {
            //add in thrusters
            SmallCraft sc = (SmallCraft)t;
            vLabels[9].setValue(getCriticalHitTally(sc.getLeftThrustHits(),3));
            vLabels[10].setValue(getCriticalHitTally(sc.getRightThrustHits(),3));
        } else {
            vLabels[9].setValue("-");
            vLabels[10].setValue("-");
        }

    }

    private void setContent(){

        for(int i = 0; i < 4; i++){
            content.addArea(areas[i]);
            content.addArea(labels[i]);
            content.addArea(vLabels[i]);
        }
        content.addArea(areas[4]);
        content.addArea(labels[4]);
        content.addArea(vLabels[4]);

        content.addArea(labels[5]);
        content.addArea(vLabels[5]);
        content.addArea(labels[6]);
        content.addArea(vLabels[6]);
        content.addArea(labels[7]);
        content.addArea(vLabels[7]);
        content.addArea(labels[8]);
        content.addArea(vLabels[8]);
        content.addArea(labels[9]);
        content.addArea(vLabels[9]);
        content.addArea(labels[10]);
        content.addArea(vLabels[10]);
    }

    private void setAreas(){
        areas[Aero.LOC_NOSE] = new PMSimplePolygonArea(noseArmor);
        areas[Aero.LOC_RWING] = new PMSimplePolygonArea(rightWingArmor);
        areas[Aero.LOC_LWING] = new PMSimplePolygonArea(leftWingArmor);
        areas[Aero.LOC_AFT] = new PMSimplePolygonArea(aftArmor);
        areas[4] = new PMSimplePolygonArea(Structure);
    }

    private void setLabels(){
        FontMetrics fm = comp.getFontMetrics(FONT_LABEL);

        //Labels for Front view
        //Prefer to use message thingy but don't know how
        labels[Aero.LOC_NOSE] = WidgetUtils.createLabel("NOS", fm, Color.black,50,20); //$NON-NLS-1$
        //   labels[Aero.LOC_NOSE + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.FrontIS"), fm, Color.black,10,57); //$NON-NLS-1$
        labels[Aero.LOC_LWING] = WidgetUtils.createLabel("LWG", fm, Color.black,20,90); //$NON-NLS-1$
//      labels[Aero.LOC_LWING + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.LIS"), fm, Color.black,10,106); //$NON-NLS-1$
        labels[Aero.LOC_RWING] = WidgetUtils.createLabel("RWG", fm, Color.black,80,90); //$NON-NLS-1$
//      labels[Aero.LOC_RWING + INT_STR_OFFSET] = WidgetUtils.createLabel(Messages.getString("TankMapSet.RIS"), fm, Color.black,10,106); //$NON-NLS-1$
        labels[Aero.LOC_AFT] = WidgetUtils.createLabel("AFT", fm, Color.black,50,160); //$NON-NLS-1$
        labels[4] = WidgetUtils.createLabel("SI", fm, Color.black,50,120); //$NON-NLS-1$
        labels[5] = WidgetUtils.createLabel("Avionics:", fm, Color.white,10,210); //$NON-NLS-1$
        labels[6] = WidgetUtils.createLabel("Engine:", fm, Color.white,10,225); //$NON-NLS-1$
        labels[7] = WidgetUtils.createLabel("FCS:", fm, Color.white,10,240); //$NON-NLS-1$
        labels[8] = WidgetUtils.createLabel("Sensors:", fm, Color.white,10,255); //$NON-NLS-1$
        labels[9] = WidgetUtils.createLabel("L Thrust:", fm, Color.white,90,210); //$NON-NLS-1$
        labels[10] = WidgetUtils.createLabel("R Thrust:", fm, Color.white,90,225); //$NON-NLS-1$

        //Value labels for all parts of mek
        //front
        fm =  comp.getFontMetrics(FONT_VALUE);   
        vLabels[Aero.LOC_NOSE] = WidgetUtils.createValueLabel(50, 35, "", fm); //$NON-NLS-1$
        //   vLabels[Aero.LOC_NOSE + INT_STR_OFFSET] = WidgetUtils.createValueLabel(10, 58, "", fm); //$NON-NLS-1$
        vLabels[Aero.LOC_LWING] = WidgetUtils.createValueLabel(20, 105, "", fm); //$NON-NLS-1$
        //   vLabels[Aero.LOC_LWING + INT_STR_OFFSET] = WidgetUtils.createValueLabel(10, 100, "", fm); //$NON-NLS-1$
        vLabels[Aero.LOC_RWING] = WidgetUtils.createValueLabel(80, 105, "", fm); //$NON-NLS-1$
        //   vLabels[Aero.LOC_RWING + INT_STR_OFFSET] = WidgetUtils.createValueLabel(10, 100, "", fm); //$NON-NLS-1$
        vLabels[Aero.LOC_AFT] = WidgetUtils.createValueLabel(50, 175, "", fm); //$NON-NLS-1$
        vLabels[4] = WidgetUtils.createValueLabel(50, 135, "", fm); //$NON-NLS-1$
        vLabels[5] = WidgetUtils.createValueLabel(40, 210, "", fm); //$NON-NLS-1$
        vLabels[6] = WidgetUtils.createValueLabel(40, 225, "", fm); //$NON-NLS-1$
        vLabels[7] = WidgetUtils.createValueLabel(40, 240, "", fm); //$NON-NLS-1$
        vLabels[8] = WidgetUtils.createValueLabel(40, 255, "", fm); //$NON-NLS-1$
        vLabels[9] = WidgetUtils.createValueLabel(130, 210, "", fm); //$NON-NLS-1$
        vLabels[10] = WidgetUtils.createValueLabel(130, 225, "", fm); //$NON-NLS-1$
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

    private void translateAreas(){
        areas[Aero.LOC_NOSE].translate(0,0);
        //       areas[Aero.LOC_NOSE + INT_STR_OFFSET].translate(8,29);
        areas[Aero.LOC_LWING].translate(0,0);
        //      areas[Aero.LOC_LWING + INT_STR_OFFSET].translate(8,29);
        areas[Aero.LOC_RWING].translate(0,0);
        //    areas[Aero.LOC_RWING + INT_STR_OFFSET].translate(8,29);
        areas[Aero.LOC_AFT].translate(0, 0);
        areas[4].translate(0,0);
    }

    private String getCriticalHitTally(int tally, int max) {

        String marks = "";

        if(tally < 1) {
            return marks;
        }

        if(tally >= max) {
            marks = "Out";
            return marks;
        }

        while(tally > 0) {
            marks = marks + "X";
            tally--;
        }

        return marks;
    }

}