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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;

import megamek.client.util.widget.*;
import megamek.common.*;

/**
 * Displays the info for a mech.  This is also a sort
 * of interface for special movement and firing actions.
 */
public class MechDisplay extends BufferedPanel 


{
    // buttons & gizmos for top level
    
    MechPanelTabStrip tabStrip;
    
    
    public BufferedPanel        displayP;
    public MovementPanel        mPan;
    public ArmorPanel           aPan;
    public WeaponPanel          wPan;
    public SystemPanel          sPan;
    public ExtraPanel           ePan;
    private Client              client;
    
    private Entity              currentlyDisplaying = null;
    
    /**
     * Creates and lays out a new mech display.
     */
    public MechDisplay(Client client) {
        super(new GridBagLayout());
        
        this.client = client;
        
        tabStrip = new MechPanelTabStrip(this);
        
        displayP = new BufferedPanel(new CardLayout());
        mPan = new MovementPanel();
        displayP.add("movement", mPan);
        aPan = new ArmorPanel();
        displayP.add("armor", aPan);
        wPan = new WeaponPanel(client);
        displayP.add("weapons", wPan);
        sPan = new SystemPanel(client);
        displayP.add("systems", sPan);
        ePan = new ExtraPanel(client);
        displayP.add("extras", ePan);
        
        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 1, 0, 1);
        
        c.weightx = 1.0;    c.weighty = 0.0;
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(tabStrip, c);
        c.insets = new Insets(0, 1, 1, 1);
        c.weighty = 1.0;
        addBag(displayP, c);
        
        ((CardLayout)displayP.getLayout()).show(displayP, "movement");
        //tabStrip.setTab(0);
    }
    
    public void addBag(Component comp, GridBagConstraints c) {
        ((GridBagLayout)getLayout()).setConstraints(comp, c);
        add(comp);
    }
    
    /**
     * @deprecated use displayEntity instead
     */
    public void displayMech(Entity en) {
        displayEntity(en);
    }
    
    /**
     * Displays the specified entity in the panel.
     */
    public void displayEntity(Entity en) {
        this.currentlyDisplaying = en;

        mPan.displayMech(en);
        aPan.displayMech(en);
        wPan.displayMech(en);
        sPan.displayMech(en);
        ePan.displayMech(en);
    }
    
    /**
     * Returns the entity we'return currently displaying
     */
    
    public Entity getCurrentEntity() {
        return currentlyDisplaying;
    }
    
    
    /**
     * Changes to the specified panel.
     */
    public void showPanel(String s) {
        ((CardLayout)displayP.getLayout()).show(displayP, s);
        if (s == "movement") {
	       tabStrip.setTab(0);
	    } else if (s == "armor") {
	    	tabStrip.setTab(1);
	    } else if (s == "weapons"){
	    	tabStrip.setTab(3);
	    } else if (s == "systems") {
	    	tabStrip.setTab(2);
	    } else if (s == "extras") {
	    	tabStrip.setTab(4);
	    }
    }
    
}
/**
 * The movement panel contains all the buttons, readouts
 * and gizmos relating to moving around on the 
 * battlefield.
 */
class MovementPanel extends PicMap{

    private GeneralInfoMapSet gi;

    private int minTopMargin = 8;
    private int minLeftMargin = 8;
    
    
    
    public void addNotify(){
        super.addNotify();
        gi = new GeneralInfoMapSet(this);
        addElement(gi.getContentGroup());
        Vector v = gi.getBackgroundDrawers();
        Enumeration enum = v.elements();
        while(enum.hasMoreElements()){
        	addBgDrawer( (BackGroundDrawer) enum.nextElement());
        }
        onResize();
        update();
    }
    
    public void onResize(){
    	int w = getSize().width;
    	Rectangle r = getContentBounds();
    	int dx = (int) Math.round((w - r.width)/2);
    	if (dx < minLeftMargin) dx = minLeftMargin;
    	int dy = minTopMargin;
      	if( r != null) setContentMargins(dx, dy, dx, dy);
    }
        
    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
       gi.setEntity(en);
       onResize();
       update();
    }     


    
}
    
/**
 * This panel contains the armor readout display.
 */
class ArmorPanel  extends PicMap 
{
    private TankMapSet tank;
    private MechMapSet mech;
    private InfantryMapSet infantry;
    private BattleArmorMapSet battleArmor;
    private int minTopMargin = 0;
    private int minLeftMargin = 0;
    private int minBottomMargin = 0;
    private int minRightMargin = 0;
    
    private static final int minTankTopMargin = 8;
    private static final int minTankLeftMargin = 8;
    private static final int minMechTopMargin = 18;
    private static final int minMechLeftMargin = 7;
    private static final int minMechBottomMargin = 0;
    private static final int minMechRightMargin = 0;
    private static final int minInfTopMargin = 8;
    private static final int minInfLeftMargin = 8;
    
    
    public void addNotify(){
        super.addNotify();
        tank = new TankMapSet(this);
        mech = new MechMapSet(this);
        infantry = new InfantryMapSet(this);
        battleArmor = new BattleArmorMapSet(this);
    }
    
    public void onResize(){
    	Rectangle r = getContentBounds();
    	if( r == null) return;
    	int w = (int) Math.round((getSize().width - r.width)/2);
    	int h = (int) Math.round((getSize().height - r.height)/2);
    	int dx = (w < minLeftMargin) ? minLeftMargin : w;
    	int dy = (h < minTopMargin) ? minTopMargin : h;
      	setContentMargins(dx, dy, minRightMargin, minBottomMargin);
    }
        
    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
    	DisplayMapSet ams = (DisplayMapSet) mech;
    	removeAll();
        if(en instanceof Mech){
        	ams = (DisplayMapSet) mech;
         	minLeftMargin = minMechLeftMargin;
        	minTopMargin = minMechTopMargin;
            minBottomMargin = minMechBottomMargin;
            minRightMargin = minMechRightMargin;
        } else if (en instanceof Tank){
        	ams = (DisplayMapSet) tank;
        	minLeftMargin = minTankLeftMargin;
        	minTopMargin = minTankTopMargin;
        	minBottomMargin = minTankTopMargin;
            minRightMargin = minTankLeftMargin;
        } else if (	en instanceof BattleArmor){
        	ams = (DisplayMapSet) battleArmor;
            minLeftMargin = minInfLeftMargin;
        	minTopMargin = minInfTopMargin;
        	minBottomMargin = minInfTopMargin;
            minRightMargin = minInfLeftMargin;
        	       	
        } else if (en instanceof Infantry){
        	ams = (DisplayMapSet)infantry;
            minLeftMargin = minInfLeftMargin;
        	minTopMargin = minInfTopMargin;
        	minBottomMargin = minInfTopMargin;
            minRightMargin = minInfLeftMargin;        	
        	         	
        }
        ams.setEntity(en); 
        this.addElement(ams.getContentGroup());
        Vector v = ams.getBackgroundDrawers();
        Enumeration enum = v.elements();
        while(enum.hasMoreElements()){
        	addBgDrawer( (BackGroundDrawer) enum.nextElement());
        }
        onResize();
        update();
    }
}
    
    
/**
 * This class contains the all the gizmos for firing the
 * mech's weapons.
 */
class WeaponPanel extends BufferedPanel 
    implements ItemListener
{
    public java.awt.List weaponList;
    public Choice m_chAmmo;
        
    public TransparentLabel wAmmo, wNameL, wHeatL, wDamL, wMinL, wShortL, wMedL, wLongL;
    public TransparentLabel wNameR, wHeatR, wDamR, wMinR, wShortR,wMedR, wLongR;
    public TransparentLabel currentHeatBuildupL, currentHeatBuildupR;
        
    public TransparentLabel wTargetL, wRangeL, wToHitL;
    public TransparentLabel wTargetR, wRangeR, wToHitR;
        
    public TextArea toHitText;
        
    // I need to keep a pointer to the weapon list of the
    // currently selected mech.
    private Vector weapons;
    private Vector vAmmo;
    private Entity entity;
    private Client client;
    
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, 10);
        
    public WeaponPanel(Client client) {        
        super(new GridBagLayout());
        
        this.client = client;
         
        FontMetrics fm = getFontMetrics(FONT_VALUE);
        
        Color clr = Color.white;

        // weapon list
        weaponList = new java.awt.List(4, false);
        weaponList.addItemListener(this);
        
        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        
        //adding Weapon List
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 9);
        c.weightx = 0.0;    c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout)getLayout()).setConstraints(weaponList, c);
        add(weaponList);
        
        //adding Ammo choice + label
        
        wAmmo = new TransparentLabel("Ammo", fm, clr, TransparentLabel.LEFT);
        m_chAmmo = new Choice();
        m_chAmmo.addItemListener(this);
        
        c.insets = new Insets(1, 9, 1, 1);
        
        c.gridwidth = 1;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        ((GridBagLayout)getLayout()).setConstraints(wAmmo, c);
        add(wAmmo);
        
        c.insets = new Insets(1, 1, 1, 9);        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        ((GridBagLayout)getLayout()).setConstraints(m_chAmmo, c);
        add(m_chAmmo);
        
        //Adding Heat Buildup
            
        currentHeatBuildupL = new TransparentLabel("Heat Buildup: ", fm, clr, TransparentLabel.RIGHT);
        currentHeatBuildupR = new TransparentLabel("--", fm, clr, TransparentLabel.LEFT);
        
        c.insets = new Insets(2, 9, 2, 1);
        c.gridwidth = 2; c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        ((GridBagLayout)getLayout()).setConstraints(currentHeatBuildupL, c);
        add(currentHeatBuildupL);
        
        c.insets = new Insets(2, 1, 2, 9);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 2;
        c.anchor = GridBagConstraints.WEST;
        //c.fill = GridBagConstraints.HORIZONTAL;
        ((GridBagLayout)getLayout()).setConstraints(currentHeatBuildupR, c);
        add(currentHeatBuildupR);

            
        //Adding weapon display labels
        wNameL = new TransparentLabel("Name", fm, clr, TransparentLabel.CENTER);
        wHeatL = new TransparentLabel("Heat", fm, clr, TransparentLabel.CENTER);
        wDamL =  new TransparentLabel("Damage", fm, clr, TransparentLabel.CENTER);
        wNameR = new TransparentLabel("", fm, clr, TransparentLabel.CENTER);
        wHeatR = new TransparentLabel("--", fm, clr, TransparentLabel.CENTER);
        wDamR =  new TransparentLabel("--", fm, clr, TransparentLabel.CENTER);
        
        
         c.anchor = GridBagConstraints.CENTER;    
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 9, 1, 1);
        c.gridwidth = 2; c.gridx = 0;            
        ((GridBagLayout)getLayout()).setConstraints(wNameL, c);
        add(wNameL);
        
        
        c.insets = new Insets(2, 1, 1, 1);
        c.gridwidth = 1; c.gridx = 2; 
        ((GridBagLayout)getLayout()).setConstraints(wHeatL, c);
        add(wHeatL);
        
        c.insets = new Insets(2, 1, 1, 9);    
        c.gridwidth = GridBagConstraints.REMAINDER; 
        c.gridx = 3;            
        ((GridBagLayout)getLayout()).setConstraints(wDamL, c);
        add(wDamL);
        
        c.insets = new Insets(1, 9, 2, 1);
        c.gridwidth = 2;
        c.gridx = 0; c.gridy = 4;            
        ((GridBagLayout)getLayout()).setConstraints(wNameR, c);
        add(wNameR);
        
        c.gridwidth = 1;
        c.gridx = 2; 
        ((GridBagLayout) getLayout()).setConstraints(wHeatR, c);
        add(wHeatR);
        
        c.insets = new Insets(1, 1, 2, 9);
        c.gridx = 3;    
        c.gridwidth = GridBagConstraints.REMAINDER;            
        ((GridBagLayout) getLayout()).setConstraints(wDamR, c);
        add(wDamR);
        
        
        // Adding range labels
        wMinL = new TransparentLabel("Min", fm, clr, TransparentLabel.CENTER);
        wShortL = new TransparentLabel("Short", fm, clr, TransparentLabel.CENTER);
        wMedL = new TransparentLabel("Med", fm, clr, TransparentLabel.CENTER);
        wLongL = new TransparentLabel("Long", fm, clr, TransparentLabel.CENTER);
        wMinR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wShortR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wMedR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wLongR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        
        c.weightx = 1.0;
        c.insets = new Insets(2, 9, 1, 1);
        c.gridx = 0; c.gridy = 5; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wMinL, c);
        add(wMinL);
        
        c.insets = new Insets(2, 1, 1, 1);
        c.gridx = 1; c.gridy = 5;
        ((GridBagLayout) getLayout()).setConstraints(wShortL, c);
        add(wShortL);
        
        c.gridx = 2; c.gridy = 5;
        ((GridBagLayout) getLayout()).setConstraints(wMedL, c);
        add(wMedL);
        
        c.insets = new Insets(2, 1, 1, 9);
        c.gridx = 3; c.gridy = 5; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wLongL, c);
        add(wLongL);
        //---------------- 
        
         c.insets = new Insets(1, 9, 2, 1);
        c.gridx = 0; c.gridy = 6; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wMinR, c);
        add(wMinR);
        
        c.insets = new Insets(1, 1, 2, 1);
        c.gridx = 1; c.gridy = 6;
        ((GridBagLayout) getLayout()).setConstraints(wShortR, c);
        add(wShortR);
        
        c.gridx = 2; c.gridy = 6;
        ((GridBagLayout) getLayout()).setConstraints(wMedR, c);
        add(wMedR);
        
        c.insets = new Insets(1, 1, 2, 9);
        c.gridx = 3; c.gridy = 6; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wLongR, c);
        add(wLongR);        
        
            
        // target panel
        wTargetL = new TransparentLabel("Target:", fm, clr, TransparentLabel.CENTER);
        wRangeL = new TransparentLabel("Range:", fm, clr, TransparentLabel.CENTER);
        wToHitL = new TransparentLabel("To Hit:", fm, clr, TransparentLabel.CENTER);
            
        wTargetR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wRangeR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        wToHitR = new TransparentLabel("---", fm, clr, TransparentLabel.CENTER);
        
        c.weightx = 0.0;
        c.insets = new Insets(2, 9, 1, 1);    
        c.gridx = 0; c.gridy = 7; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wTargetL, c);
        add(wTargetL);

        c.insets = new Insets(2, 1, 1, 9);
        c.gridx = 1; c.gridy = 7; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wTargetR, c);
        add(wTargetR);

        c.insets = new Insets(1, 9, 1, 1);
        c.gridx = 0; c.gridy = 8; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wRangeL, c);
        add(wRangeL);

        c.insets = new Insets(1, 1, 1, 9);
        c.gridx = 1; c.gridy = 8; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wRangeR, c);
        add(wRangeR);

        c.insets = new Insets(1, 9, 1, 1);
        c.gridx = 0; c.gridy = 9; c.gridwidth = 1;
        ((GridBagLayout) getLayout()).setConstraints(wToHitL, c);
        add(wToHitL);

        c.insets = new Insets(1, 1, 1, 9);
        c.gridx = 1; c.gridy = 9; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(wToHitR, c);
        add(wToHitR);

        // to-hit text
        toHitText = new TextArea("", 2, 20, TextArea.SCROLLBARS_VERTICAL_ONLY);
        toHitText.setEditable(false);
        
        c.insets = new Insets(1, 9, 15, 9);
        c.gridx = 0; c.gridy = 10; c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout) getLayout()).setConstraints(toHitText, c);
        add(toHitText);
 
        
        setBackGround();
        
        
    }
    
    private void setBackGround(){
        Image tile = getToolkit().getImage("data/widgets/tile.gif");
        PMUtil.setImage(tile, (Component) this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));                
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
                
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/tl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/bl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/tr_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/br_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
         
    }

        
    /**
     * updates fields for the specified mech
     * 
     * fix the ammo when it's added
     */
    public void displayMech(Entity en) {
        // update pointer to weapons
        this.weapons = en.getWeaponList();
        this.entity = en;
            
        int currentHeatBuildup = en.heat // heat from last round
            + en.getEngineCritHeat() // heat engine crits will add
            + en.heatBuildup; // heat we're building up this round
        if ( en instanceof Mech && en.infernos.isStillBurning() ) { // hit with inferno ammo
            currentHeatBuildup += en.infernos.getHeat();
        }
        if ( en instanceof Mech && en.isStealthActive() ) {
            currentHeatBuildup += 10; // active stealth heat
        }
        
        // update weapon list
        weaponList.removeAll();
        m_chAmmo.removeAll();
        m_chAmmo.setEnabled(false);
        
        for(int i = 0; i < weapons.size(); i++) {
            Mounted mounted = (Mounted)weapons.elementAt(i);
            WeaponType wtype = (WeaponType)mounted.getType();
            String wn = mounted.getDesc() 
                        + " [" + en.getLocationAbbr(mounted.getLocation()) + "]";
            // determine shots left & total shots left
            if (wtype.getAmmoType() != AmmoType.T_NA) {
                int shotsLeft = 0;
                if (mounted.getLinked() != null && !mounted.getLinked().isDumping()) {
                    shotsLeft = mounted.getLinked().getShotsLeft();
                }
                
                EquipmentType typeUsed = mounted.getLinked() == null ? null : mounted.getLinked().getType();
                int totalShotsLeft = entity.getTotalAmmoOfType(typeUsed);
                
                wn += " (" + shotsLeft + "/" + totalShotsLeft + ")";
            }

            // Fire Mode - lots of things have variable modes
            if (wtype.hasModes()) {
                wn += " " + mounted.curMode();
            }    
            weaponList.add(wn);
            if (mounted.isUsedThisRound() && client.game.phase == Game.PHASE_FIRING) {
                // add heat from weapons fire to heat tracker
                currentHeatBuildup += wtype.getHeat() * mounted.howManyShots();
            }
        }
        
        // This code block copied from the MovementPanel class,
        //  bad coding practice (duplicate code).
        int heatCap = en.getHeatCapacity();
        int heatCapWater = en.getHeatCapacityWithWater();
        String heatCapacityStr = Integer.toString(heatCap);
        
        if ( heatCap < heatCapWater ) {
          heatCapacityStr = heatCap + " [" + heatCapWater + "]";
        }
        // end duplicate block

        String heatText = Integer.toString(currentHeatBuildup);
        if (currentHeatBuildup > en.getHeatCapacityWithWater()) {
            heatText += "*"; // overheat indication
        }
        this.currentHeatBuildupR.setText(heatText + " (" + heatCapacityStr + ")");
    }
  
    /**
     * Selects the weapon at the specified index in the list
     */
    public void selectWeapon(int wn) {
        if (wn == -1) {
            weaponList.select(-1);
            return;
        }        
        int index = weapons.indexOf(entity.getEquipment(wn));
        weaponList.select(index);
        displaySelected();
    }
        
    /**
     * Selects the weapon at the specified index in the list
     */
    public int getSelectedWeaponNum() {
        int selected = weaponList.getSelectedIndex();
        if (selected == -1) {
            return -1;
        }
        return entity.getEquipmentNum((Mounted)weapons.elementAt(selected));
    }
        
    /**
     * displays the selected item from the list in the weapon
     * display panel.
     */
    public void displaySelected() {
        // short circuit if not selected
        if(weaponList.getSelectedIndex() == -1) {
            m_chAmmo.removeAll();
            m_chAmmo.setEnabled(false);
            wNameR.setText("");
            wHeatR.setText("--");
            wDamR.setText("--");
            wMinR.setText("---");
            wShortR.setText("---");
            wMedR.setText("---");
            wLongR.setText("---");
            return;
        }
        Mounted mounted = (Mounted)weapons.elementAt(weaponList.getSelectedIndex());
        WeaponType wtype = (WeaponType)mounted.getType();
        // update weapon display
        wNameR.setText(mounted.getDesc());
        wHeatR.setText(wtype.getHeat() + "");
        if(wtype.getDamage() == WeaponType.DAMAGE_MISSILE) {
            wDamR.setText("Missile");
        } else if(wtype.getDamage() == WeaponType.DAMAGE_VARIABLE) {
            wDamR.setText("Variable");
        } else if(wtype.getDamage() == WeaponType.DAMAGE_SPECIAL) {
            wDamR.setText("Special");
        } else {
            wDamR.setText(new Integer(wtype.getDamage()).toString());
        }
            
        // update range
        if(wtype.getMinimumRange() > 0) {
            wMinR.setText(Integer.toString(wtype.getMinimumRange()));
        } else {
            wMinR.setText("---");
        }
        if(wtype.getShortRange() > 1) {
            wShortR.setText("1 - " + wtype.getShortRange());
        } else {
            wShortR.setText("" + wtype.getShortRange());
        }
        if(wtype.getMediumRange() - wtype.getShortRange() > 1) {
            wMedR.setText((wtype.getShortRange() + 1) + " - " + wtype.getMediumRange());
        } else {
            wMedR.setText("" + wtype.getMediumRange());
        }
        if(wtype.getLongRange() - wtype.getMediumRange() > 1) {
            wLongR.setText((wtype.getMediumRange() + 1) + " - " + wtype.getLongRange());
        } else {
            wLongR.setText("" + wtype.getLongRange());
        }

        // Update the range display to account for the weapon's loaded ammo.
        if ( null != mounted.getLinked() ) {
            AmmoType atype = (AmmoType) mounted.getLinked().getType();
            updateRangeDisplayForAmmo( atype );
        }

        // update ammo selector
        boolean bOwner = (client.getLocalPlayer() == entity.getOwner());
        m_chAmmo.removeAll();
        if (wtype.getAmmoType() == AmmoType.T_NA || !bOwner) {
            m_chAmmo.setEnabled(false);
        }
        else {
            m_chAmmo.setEnabled(true);
            vAmmo = new Vector();
            int nCur = -1;
            int i = 0;
            for (Enumeration j = entity.getAmmo(); j.hasMoreElements();) {
                Mounted mountedAmmo = (Mounted)j.nextElement();
                AmmoType atype = (AmmoType)mountedAmmo.getType();
                if (mountedAmmo.isDestroyed() || mountedAmmo.getShotsLeft() <= 0 || mountedAmmo.isDumping()) {
                    continue;
                }
                if (atype.getAmmoType() == wtype.getAmmoType() && atype.getRackSize() == wtype.getRackSize()) {
                    vAmmo.addElement(mountedAmmo);
                    m_chAmmo.add( formatAmmo(mountedAmmo) );
                    if (mounted.getLinked() == mountedAmmo) {
                        nCur = i;
                    }
                    i++;
                }
            }
            if (nCur == -1) {
                m_chAmmo.setEnabled(false);
            }
            else {
                m_chAmmo.select(nCur);
            }
        }
    }
    
    private String formatAmmo(Mounted m)
    {
        StringBuffer sb = new StringBuffer(64);
        int ammoIndex = m.getDesc().indexOf("Ammo");
        sb.append("[").append(entity.getLocationAbbr(m.getLocation())).append("] ");
        if (ammoIndex == -1) {
            sb.append(m.getDesc());
        } else {
            sb.append(m.getDesc().substring(0, ammoIndex));
            sb.append(m.getDesc().substring(ammoIndex + 4));
        }
        return sb.toString();
    }

    /**
     * Update the range display for the selected ammo.
     *
     * @param   atype - the <code>AmmoType</code> of the weapon's loaded ammo.
     */
    private void updateRangeDisplayForAmmo( AmmoType atype ) {

        // Only override the display for the various ATM ammos
        if(AmmoType.T_ATM == atype.getAmmoType())
        {
            if(atype.getMunitionType() == AmmoType.M_EXTENDED_RANGE)
            {
                wMinR.setText("4");
                wShortR.setText("1 - 9");
                wMedR.setText("10 - 18");
                wLongR.setText("19 - 27");
            }
            else if(atype.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE)
            {
                wMinR.setText("---");
                wShortR.setText("1 - 3");
                wMedR.setText("4 - 6");
                wLongR.setText("7 - 9");
            }
            else
            {
                wMinR.setText("4");
                wShortR.setText("1 - 5");
                wMedR.setText("6 - 10");
                wLongR.setText("11 - 15");
            }
        } // End weapon-is-ATM

    } // End private void updateRangeDisplayForAmmo( AmmoType )

    // 
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {
        if(ev.getItemSelectable() == weaponList) {
            displaySelected();
        }
        else if (ev.getItemSelectable() == m_chAmmo) {
            int n = weaponList.getSelectedIndex();
            if (n == -1) {
                return;
            }
            Mounted mWeap = (Mounted)weapons.elementAt(n);
            Mounted mAmmo = (Mounted)vAmmo.elementAt(m_chAmmo.getSelectedIndex());
            entity.loadWeapon(mWeap, mAmmo);
            // Update the range display to account for the weapon's loaded ammo.
            AmmoType atype = (AmmoType) mAmmo.getType();
            updateRangeDisplayForAmmo( atype );

            // When in the Firing Phase, update the targeting information.
            // TODO: make this an accessor function instead of a member access.
            if ( this.client.curPanel instanceof FiringDisplay ) {
                ( (FiringDisplay) this.client.curPanel ).updateTarget();
            }

            // Alert the server of the update.
            this.client.sendAmmoChange( entity.getId(),
                                        entity.getEquipmentNum(mWeap),
                                        entity.getEquipmentNum(mAmmo) );
        }
    }
        
}

/**
 * This class shows the critical hits and systems for a mech
 */
class SystemPanel 
    extends BufferedPanel
    implements ItemListener, ActionListener
{
    private static Object SYSTEM = new Object();
    
    private TransparentLabel locLabel, slotLabel, modeLabel;
    public java.awt.List slotList;
    public java.awt.List locList;
    
    public Choice m_chMode;
    public Button m_bDumpAmmo;
    //public Label modeLabel;
    private Client client;
    
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, 12);
    
    Entity en;
    
    public SystemPanel(Client client) {
        super();
        
        FontMetrics fm = getFontMetrics(FONT_VALUE);
        
        this.client = client;
        locLabel = new TransparentLabel("Location", fm, Color.white,TransparentLabel.CENTER);
        slotLabel = new TransparentLabel("Slot", fm, Color.white,TransparentLabel.CENTER);
        
        locList = new List(8, false);
        locList.addItemListener(this);
        slotList = new List(12, false);
        slotList.addItemListener(this);
        //slotList.setEnabled(false);
        
        m_chMode = new Choice();
        m_chMode.add("   ");
        m_chMode.setEnabled(false);
        m_chMode.addItemListener(this);
        m_bDumpAmmo = new Button("Dump");
        m_bDumpAmmo.setEnabled(false);
        m_bDumpAmmo.setActionCommand("dump");
        m_bDumpAmmo.addActionListener(this);
        modeLabel = new TransparentLabel("Mode", fm, Color.white,TransparentLabel.RIGHT);
        //modeLabel.setEnabled(false);
        
        
        // layout main panel
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 1);
        c.gridy = 0; c.gridx = 0;
        c.weightx = 0.5;    c.weighty = 0.0;
        c.gridwidth = 1; c.gridheight = 1;
        gridbag.setConstraints(locLabel, c);
        add(locLabel);
        
        c.weightx = 0.0;
        c.gridy = 0; c.gridx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(15, 1, 1, 9);
        gridbag.setConstraints(slotLabel, c);
        add(slotLabel);
        
        c.weightx = 0.5;
        //c.weighty = 1.0;
        c.gridy = 1; c.gridx = 0;
        c.gridwidth = 1;
        c.insets = new Insets(1, 9, 15, 1);
        c.gridheight = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(locList, c);
        add(locList);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 9);
        gridbag.setConstraints(slotList, c);
        add(slotList);
        
        c.gridwidth = 1;
        c.gridy = 2;
        c.gridx = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(modeLabel, c);
        c.insets = new Insets(1, 1, 1, 1);
        add(modeLabel);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy = 2;
        c.gridx = 2;
        c.insets = new Insets(1, 1, 1, 9);
        gridbag.setConstraints(m_chMode, c);
        add(m_chMode);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridy = 3;
        c.gridx = 1;
        c.insets = new Insets(4, 4, 15, 9);
        gridbag.setConstraints(m_bDumpAmmo, c);
        add(m_bDumpAmmo);
        
        
        setBackGround();
    }
    
    public int getSelectedLocation() {
        return locList.getSelectedIndex();
    }
    
    public Mounted getSelectedEquipment() {
        int loc = locList.getSelectedIndex();
        int slot = slotList.getSelectedIndex();
        if (loc == -1 || slot == -1) {
            return null;
        }
        final CriticalSlot cs = en.getCritical(loc, slot);
        if (null == cs) {
            return null;
        }
        if (cs.getType() == CriticalSlot.TYPE_SYSTEM) {
            return null;
        }
        return en.getEquipment (cs.getIndex());
    }

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        this.en = en;
        
        locList.removeAll();
        for(int i = 0; i < en.locations(); i++) {
            if(en.getNumberOfCriticals(i) > 0) {
                locList.add(en.getLocationName(i), i);
            }
        }
        locList.select(0);
        displaySlots();
    }
    
    public void displaySlots() {
        int loc = locList.getSelectedIndex();
        slotList.removeAll();
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            StringBuffer sb = new StringBuffer(32);
            if(cs == null) {
                sb.append("---");
            } else {
                switch(cs.getType()) {
                case CriticalSlot.TYPE_SYSTEM :
                    sb.append(cs.isDestroyed() ? "*" : "").append(Mech.systemNames[cs.getIndex()]);
                    break;
                case CriticalSlot.TYPE_EQUIPMENT :
                    Mounted m = en.getEquipment(cs.getIndex());
                    sb.append(cs.isDestroyed() ? "*" : "").append(m.getDesc());
                    if (m.getType().hasModes()) {
                        sb.append(" (").append(m.curMode()).append(")");
                    }
                    break;
                }
            }
            slotList.add(sb.toString());
        }
    }

    // 
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {
        if(ev.getItemSelectable() == locList) {
            displaySlots();
        }
        else if (ev.getItemSelectable() == slotList) {
            m_bDumpAmmo.setEnabled(false);
            m_chMode.setEnabled(false);
            modeLabel.setEnabled(false);
            Mounted m = getSelectedEquipment();
          
            boolean bOwner = (client.getLocalPlayer() == en.getOwner());
            if (m != null && bOwner && m.getType() instanceof AmmoType 
                    && m.getShotsLeft() > 0 && !m.isDumping()) {
                m_bDumpAmmo.setEnabled(true);
            }
            else if (m != null && bOwner && m.getType().hasModes()) {
                if (!m.isDestroyed()) {
                    m_chMode.setEnabled(true);
                }
                modeLabel.setEnabled(true);
                m_chMode.removeAll();
                String[] saModes = m.getType().getModes();
                for (int x = 0; x < saModes.length; x++) {
                    m_chMode.add(saModes[x]);
                }
                m_chMode.select(m.curMode());
            }
        }
        else if (ev.getItemSelectable() == m_chMode) {
            Mounted m = getSelectedEquipment();
            if (m != null && m.getType().hasModes()) {
                int nMode = m.setMode(m_chMode.getSelectedItem());
            
            
                // send the event to the server
                client.sendModeChange(en.getId(), en.getEquipmentNum(m), nMode);
                    
                // notify the player
                if (m.getType().hasInstantModeSwitch()) {
                    client.cb.systemMessage("Switched " + m.getName() + " to " + m.curMode());
                }
                else {
                    client.cb.systemMessage(m.getName() + " will switch to " + m.pendingMode() + 
                            " at end of turn.");
                }
                displaySlots();
            }
        }
    }
    
    // ActionListener
    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getActionCommand().equals("dump")) {
            Mounted m = getSelectedEquipment();
            boolean bOwner = (client.getLocalPlayer() == en.getOwner());
            if (m == null || !bOwner || !(m.getType() instanceof AmmoType) || 
                        m.getShotsLeft() <= 0) {
                return;
            }
            
            boolean bDumping;
            
            if (m.isPendingDump()) {
                bDumping = false;
                client.cb.systemMessage(m.getName() + " WON'T be dumped next turn.");
            }
            else {
                bDumping = true;
                client.cb.systemMessage(m.getName() + " will be dumped next turn.");
            }
            m.setPendingDump(bDumping);
            client.sendModeChange(en.getId(), en.getEquipmentNum(m), bDumping ? 1 : 0);
        }
    }
    
    private void setBackGround(){
        Image tile = getToolkit().getImage("data/widgets/tile.gif");
        PMUtil.setImage(tile, (Component) this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));                
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
                
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/tl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/bl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/tr_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/br_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
         
    }

}


/**
 * This class shows information about a unit that doesn't belong elsewhere.
 */
class ExtraPanel 
    extends BufferedPanel
{
    private TransparentLabel  narcLabel, unusedL, carrysL;
    //public Label    unusedL, carrysL;
    public TextArea unusedR, carrysR;
    public java.awt.List narcList;
    private static final Font FONT_VALUE = new Font("SansSerif", Font.PLAIN, 12);
    
    private Client client;
    
    public ExtraPanel(Client client) {
        super();
        
        this.client = client;
        
        FontMetrics fm = getFontMetrics(FONT_VALUE);
        
        narcLabel = new TransparentLabel("NARCed By:", fm, Color.white,TransparentLabel.CENTER);
        
        narcList = new List(3, false);

        // transport stuff
        //unusedL = new Label( "Unused Space:", Label.CENTER );
                
        unusedL = new TransparentLabel("Unused Space:", fm, Color.white,TransparentLabel.CENTER);
        unusedR = new TextArea("", 2, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        unusedR.setEditable(false);
        carrysL = new TransparentLabel( "Carrying:", fm, Color.white,TransparentLabel.CENTER);
        carrysR = new TextArea("", 4, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        carrysR.setEditable(false);

        // layout choice panel
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        setLayout(gridbag);
        
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(15, 9, 1, 9);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1.0;


        c.weighty = 0.0;
        gridbag.setConstraints(narcLabel, c);
        add(narcLabel);
        
        
        c.insets = new Insets(1, 9, 1, 9);
        c.weighty = 1.0;
        gridbag.setConstraints(narcList, c);
        add(narcList);
        
        c.weighty = 0.0;
        gridbag.setConstraints(unusedL, c);
        add(unusedL);
        
        c.weighty = 1.0;
        gridbag.setConstraints(unusedR, c);
        add(unusedR);

        c.weighty = 0.0;
        gridbag.setConstraints(carrysL, c);
        add(carrysL);
        
        c.insets = new Insets(1, 9, 18, 9);
        c.weighty = 1.0;
        gridbag.setConstraints(carrysR, c);
        add(carrysR);
        
        setBackGround();
        

    }
    
    
    
   private void setBackGround(){
        Image tile = getToolkit().getImage("data/widgets/tile.gif");
        PMUtil.setImage(tile, (Component) this);
        int b = BackGroundDrawer.TILING_BOTH;
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_TOP;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));                
        
            b = BackGroundDrawer.TILING_HORIZONTAL | 
                BackGroundDrawer.VALIGN_BOTTOM;
        tile = getToolkit().getImage("data/widgets/h_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.TILING_VERTICAL | 
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/v_line.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
                
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/tl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_LEFT;
        tile = getToolkit().getImage("data/widgets/bl_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_TOP |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/tr_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
        
            b = BackGroundDrawer.NO_TILING | 
                BackGroundDrawer.VALIGN_BOTTOM |
                BackGroundDrawer.HALIGN_RIGHT;
        tile = getToolkit().getImage("data/widgets/br_corner.gif");
        PMUtil.setImage(tile, (Component) this);
        addBgDrawer(new BackGroundDrawer (tile,b));
         
    }
    

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {

        // Walk through the list of teams.  There
        // can't be more teams than players.
        Enumeration loop = client.game.getPlayers();
        while ( loop.hasMoreElements() ) {
            Player player = (Player) loop.nextElement();
            int team = player.getTeam();
            if ( !player.equals(client.getLocalPlayer()) &&
                 en.isNarcedBy( team ) ) {
                StringBuffer buff = new StringBuffer( player.getName() );
                buff.append( " [" )
                    .append( Player.teamNames[team] )
                    .append( "]" );
                narcList.add( buff.toString() );
            }
        }

        // transport values
        String unused = en.getUnusedString();
        if ( unused.equals("") ) unused = "None";
        this.unusedR.setText( unused );
        Enumeration iter = en.getLoadedUnits().elements();
        carrysR.setText( null );
        while ( iter.hasMoreElements() ) {
            carrysR.append( ((Entity)iter.nextElement()).getShortName() );
            if ( iter.hasMoreElements() ) {
                carrysR.append( "\n" );
            }
        }

    } // End public void displayMech( Entity )

} // End class ExtraPanel extends Panel
