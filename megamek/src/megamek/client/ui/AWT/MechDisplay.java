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

import megamek.common.*;

/**
 * Displays the info for a mech.  This is also a sort
 * of interface for special movement and firing actions.
 */
public class MechDisplay extends Panel 
    implements ActionListener
{
    // buttons & gizmos for top level
    public Button                movBut, armBut, weaBut, criBut;
    
    public Panel                displayP;
    public MovementPanel        mPan;
    public ArmorPanel            aPan;
    public WeaponPanel            wPan;
    public SystemPanel            sPan;
    
    /**
     * Creates and lays out a new mech display.
     */
    public MechDisplay() {
        super(new GridBagLayout());
        
        movBut = new Button("Movement");
        movBut.addActionListener(this);
        armBut = new Button("Armor");
        armBut.addActionListener(this);
        weaBut = new Button("Weapons");
        weaBut.addActionListener(this);
        criBut = new Button("Systems");
        criBut.addActionListener(this);
        
        displayP = new Panel(new CardLayout());
        mPan = new MovementPanel();
        displayP.add("movement", mPan);
        aPan = new ArmorPanel();
        displayP.add("armor", aPan);
        wPan = new WeaponPanel();
        displayP.add("weapons", wPan);
        sPan = new SystemPanel();
        displayP.add("systems", sPan);
        
        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        
        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(movBut, c);
        
        addBag(armBut, c);
        
        addBag(weaBut, c);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(criBut, c);
        
        c.weightx = 1.0;    c.weighty = 1.0;
        addBag(displayP, c);
        
        ((CardLayout)displayP.getLayout()).show(displayP, "movement");
    }
    
    public void addBag(Component comp, GridBagConstraints c) {
        ((GridBagLayout)getLayout()).setConstraints(comp, c);
        add(comp);
    }
    
    /**
     * Displays the specified mech in the panel.
     */
    public void displayMech(Entity en) {
        mPan.displayMech(en);
        aPan.displayMech(en);
        wPan.displayMech(en);
        sPan.displayMech(en);
    }
    
    /**
     * Changes to the specified panel.
     */
    public void showPanel(String s) {
        ((CardLayout)displayP.getLayout()).show(displayP, s);
    }
    
    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equalsIgnoreCase("movement")) {
            showPanel("movement");
        }
        if(e.getActionCommand().equalsIgnoreCase("armor")) {
            showPanel("armor");
        }
        if(e.getActionCommand().equalsIgnoreCase("weapons")) {
            showPanel("weapons");
        }
        if(e.getActionCommand().equalsIgnoreCase("systems")) {
            showPanel("systems");
        }
    }
    
}
/**
 * The movement panel contains all the buttons, readouts
 * and gizmos relating to moving around on the 
 * battlefield.
 */
class MovementPanel 
    extends Panel 
{
    public Panel    statusP, terrainP, moveP;
    public Label    mechTypeL;
    public Label    statusL, statusR, playerL, playerR, teamL, teamR, weightL, weightR, pilotL, pilotR;
    public Label    mpL, mpR, curMoveL, curMoveR, heatL, heatR;
        
    public MovementPanel() {
        super(new GridBagLayout());
            
        mechTypeL = new Label("LCT-1L Loc0st", Label.CENTER);
            
        // status panel
        statusL = new Label("Status:", Label.RIGHT);
        playerL = new Label("Player:", Label.RIGHT);
        teamL = new Label("Team:", Label.RIGHT);
        weightL = new Label("Weight:", Label.RIGHT);
        pilotL = new Label("Pilot:", Label.RIGHT);
            
        statusR = new Label("?", Label.LEFT);
        playerR = new Label("?", Label.LEFT);
        teamR = new Label("?", Label.LEFT);
        weightR = new Label("?", Label.LEFT);
        pilotR = new Label("?", Label.LEFT);
            
        statusP = new Panel(new GridLayout(5, 2));
        statusP.add(statusL);
        statusP.add(statusR);
        statusP.add(playerL);
        statusP.add(playerR);
        statusP.add(teamL);
        statusP.add(teamR);
        statusP.add(weightL);
        statusP.add(weightR);
        statusP.add(pilotL);
        statusP.add(pilotR);
            
        // terrain panel
        terrainP = new Panel();
            
        // movement panel
        mpL = new Label("Movement:", Label.RIGHT);
        curMoveL = new Label("Currently:", Label.RIGHT);
        heatL = new Label("Heat:", Label.RIGHT);
            
        mpR = new Label("8/12/0", Label.LEFT);
        curMoveR = new Label("No Movement", Label.LEFT);
        heatR = new Label("2 (10 capacity)", Label.LEFT);
            
        moveP = new Panel(new GridLayout(3, 2));
        moveP.add(mpL);
        moveP.add(mpR);
        moveP.add(curMoveL);
        moveP.add(curMoveR);
        moveP.add(heatL);
        moveP.add(heatR);
            
        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout)getLayout()).setConstraints(mechTypeL, c);
        add(mechTypeL);
        
        //c.gridwidth = 1;
        ((GridBagLayout)getLayout()).setConstraints(statusP, c);
        add(statusP);
        
        /*
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout)getLayout()).setConstraints(terrainP, c);
        add(terrainP);
        */
        
        ((GridBagLayout)getLayout()).setConstraints(moveP, c);
        add(moveP);
    }
        
    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        this.mechTypeL.setText(en.getModel() + " " + en.getName());

        this.statusR.setText("not yet");
        this.playerR.setText(en.getOwner().getName());
        this.teamR.setText("not yet");
        this.weightR.setText(Integer.toString((int)en.getWeight()));
        this.pilotR.setText(en.crew.getDesc());

        this.mpR.setText(Integer.toString(en.getWalkMP()) + "/" + Integer.toString(en.getRunMP()) + "/" + Integer.toString(en.getJumpMP()));
        this.curMoveR.setText(en.getMovementString(en.moved) + (en.moved == en.MOVE_NONE ? "" : " " + en.delta_distance));
        this.heatR.setText(Integer.toString(en.heat) + " (" + en.getHeatCapacity() + " capacity)");
        
        validate();
    }
}
    
/**
 * This panel contains the armor readout display.
 */
class ArmorPanel
    extends Panel 
{
    public Label    locHL, internalHL, armorHL;
    public Label[]    locL, internalL, armorL;
        
    public ArmorPanel() {
        super(new GridBagLayout());
            
        locHL = new Label("Location", Label.CENTER);
        internalHL = new Label("Internal", Label.CENTER);
        armorHL = new Label("Armor", Label.CENTER);
            
    }
        
    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        this.removeAll();
        
        locL = new Label[en.locations()];
        internalL = new Label[en.locations()];
        armorL = new Label[en.locations()];
            
        // initialize
        for(int i = 0; i < en.locations(); i++) {
            locL[i] = new Label("Center Torso Rear", Label.LEFT);
            internalL[i] = new Label("99", Label.CENTER);
            armorL[i] = new Label("999", Label.CENTER);
        }

        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);
        
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 1;
        ((GridBagLayout)getLayout()).setConstraints(locHL, c);
        add(locHL);
        
        ((GridBagLayout)getLayout()).setConstraints(armorHL, c);
        add(armorHL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout)getLayout()).setConstraints(internalHL, c);
        add(internalHL);
            
        for(int i = 0; i < en.locations(); i++) {
            c.gridwidth = 1;
            ((GridBagLayout)getLayout()).setConstraints(locL[i], c);
            add(locL[i]);
        
            ((GridBagLayout)getLayout()).setConstraints(armorL[i], c);
            add(armorL[i]);
        
            c.gridwidth = GridBagConstraints.REMAINDER;
            ((GridBagLayout)getLayout()).setConstraints(internalL[i], c);
            add(internalL[i]);
        }
        
        // update armor panel
        for(int i = 0; i < en.locations(); i++) {
            this.locL[i].setText(en.getLocationName(i));
            if (en.isRearLocation(i)) {
                this.internalL[i].setText("N/A");
            } else {
                this.internalL[i].setText(en.getInternalString(i));
            }

            this.armorL[i].setText(en.getArmorString(i));
        }
        
        this.validate();
    }
}
    
/**
 * This class contains the all the gizmos for firing the
 * mech's weapons.
 */
class WeaponPanel 
    extends Panel 
    implements ItemListener
{
    public java.awt.List weaponList;
    public Panel displayP, rangeP, targetP, buttonP;
        
    public Label wNameL, wHeatL, wDamL, wMinL, wShortL,
        wMedL, wLongL;
    public Label wNameR, wHeatR, wDamR, wMinR, wShortR,
        wMedR, wLongR;
        
    public Label wTargetL, wRangeL, wToHitL;
    public Label wTargetR, wRangeR, wToHitR;
        
    public TextArea toHitText;
        
    public Button ammoB;
        
    // I need to keep a pointer to the weapon list of the
    // currently selected mech.
    private Vector weapons;
        
    public WeaponPanel() {
        super(new GridBagLayout());
            
        // weapon list
        weaponList = new java.awt.List(4, false);
        weaponList.addItemListener(this);
            
        // weapon display panel
        wNameL = new Label("Name", Label.LEFT);
        wHeatL = new Label("Heat", Label.CENTER);
        wDamL = new Label("Damage", Label.CENTER);
        wNameR = new Label("", Label.LEFT);
        wHeatR = new Label("--", Label.CENTER);
        wDamR = new Label("--", Label.CENTER);
            
        displayP = new Panel(new GridBagLayout());
            
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        
        c.gridwidth = 1;            
        c.weightx = 1.0;    c.weighty = 0.0;
        ((GridBagLayout)displayP.getLayout()).setConstraints(wNameL, c);
        displayP.add(wNameL);
        
        c.weightx = 0.0;    c.weighty = 0.0;
        ((GridBagLayout)displayP.getLayout()).setConstraints(wHeatL, c);
        displayP.add(wHeatL);
            
        c.gridwidth = GridBagConstraints.REMAINDER;            
        ((GridBagLayout)displayP.getLayout()).setConstraints(wDamL, c);
        displayP.add(wDamL);
        
        c.gridwidth = 1;            
        c.weightx = 1.0;    c.weighty = 0.0;
        ((GridBagLayout)displayP.getLayout()).setConstraints(wNameR, c);
        displayP.add(wNameR);
        
        c.weightx = 0.0;    c.weighty = 0.0;
        ((GridBagLayout)displayP.getLayout()).setConstraints(wHeatR, c);
        displayP.add(wHeatR);
            
        c.gridwidth = GridBagConstraints.REMAINDER;            
        ((GridBagLayout)displayP.getLayout()).setConstraints(wDamR, c);
        displayP.add(wDamR);
        
        // range panel
        wMinL = new Label("Min", Label.CENTER);
        wShortL = new Label("Short", Label.CENTER);
        wMedL = new Label("Med", Label.CENTER);
        wLongL = new Label("Long", Label.CENTER);
        wMinR = new Label("---", Label.CENTER);
        wShortR = new Label("---", Label.CENTER);
        wMedR = new Label("---", Label.CENTER);
        wLongR = new Label("---", Label.CENTER);
            
        rangeP = new Panel(new GridLayout(2, 4));
        rangeP.add(wMinL);
        rangeP.add(wShortL);
        rangeP.add(wMedL);
        rangeP.add(wLongL);
        rangeP.add(wMinR);
        rangeP.add(wShortR);
        rangeP.add(wMedR);
        rangeP.add(wLongR);
            
        // target panel
        wTargetL = new Label("Target:");
        wRangeL = new Label("Range:");
        wToHitL = new Label("To Hit:");
            
        wTargetR = new Label("---");
        wRangeR = new Label("---");
        wToHitR = new Label("---");
            
        targetP = new Panel(new GridLayout(3, 2));
        targetP.add(wTargetL);
        targetP.add(wTargetR);
        targetP.add(wRangeL);
        targetP.add(wRangeR);
        targetP.add(wToHitL);
        targetP.add(wToHitR);
            
        // to-hit text
        toHitText = new TextArea("", 4, 20, TextArea.SCROLLBARS_VERTICAL_ONLY);
        toHitText.setEditable(false);
            
        // button panel
        ammoB = new Button("Change Ammo");
        ammoB.setEnabled(false);
            
        buttonP = new Panel();
        buttonP.add(ammoB);
            
            
        // layout main panel
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout)getLayout()).setConstraints(weaponList, c);
        add(weaponList);
        
        ((GridBagLayout)getLayout()).setConstraints(displayP, c);
        add(displayP);
        
        ((GridBagLayout)getLayout()).setConstraints(rangeP, c);
        add(rangeP);
        
        c.weightx = 1.0;    c.weighty = 0.0;
        ((GridBagLayout)getLayout()).setConstraints(targetP, c);
        add(targetP);
                
        c.weightx = 1.0;    c.weighty = 1.0;
        ((GridBagLayout)getLayout()).setConstraints(toHitText, c);
        add(toHitText);
                
        /*
        c.weightx = 1.0;    c.weighty = 0.0;
        ((GridBagLayout)getLayout()).setConstraints(buttonP, c);
        add(buttonP);
        */
    }
        
    /**
     * updates fields for the specified mech
     * 
     * fix the ammo when it's added
     */
    public void displayMech(Entity en) {
        // update pointer to weapons
        weapons = en.weapons;
            
        // update weapon list
        weaponList.removeAll();
        for(int i = 0; i < en.weapons.size(); i++) {
            MountedWeapon w = en.getWeapon(i);
            String wn = i + " : " + (w.isReady() ? "" : "*" ) 
                        + w.getType().getName() 
                        + " [" + en.getLocationAbbr(w.getLocation()) + "]";
            if (w.getType().getAmmoType() != Ammo.TYPE_NA) {
                if (w.getAmmoFeed() == null || w.getAmmoFeed().shots == 0) {
                    wn += " (empty)";
                } else {
                    wn += " (" + w.getAmmoFeed().shots + ")";
                }
            }
            weaponList.add(wn);
        }
    }
  
  /**
   * Selects the weapon at the specified index in the list
   */
  public void selectWeapon(int wn) {
    weaponList.select(wn);
    displaySelected();
  }
        
    /**
     * displays the selected item from the list in the weapon
     * display panel.
     */
    public void displaySelected() {
        // short circuit if not selected
        if(weaponList.getSelectedIndex() == -1) {
            wNameR.setText("");
            wHeatR.setText("--");
            wDamR.setText("--");
            wMinR.setText("---");
            wShortR.setText("---");
            wMedR.setText("---");
            wLongR.setText("---");
            return;
        }
        Weapon w = ((MountedWeapon)weapons.elementAt(weaponList.getSelectedIndex())).getType();
        // update weapon display
        wNameR.setText(w.getName());
        wHeatR.setText(w.getHeat() + "");
        if(w.getDamage() != Weapon.DAMAGE_MISSILE) {
            wDamR.setText(w.getDamage() + "");
        } else {
            wDamR.setText("missile");
        }
            
        // update range
        if(w.getMinimumRange() > 0) {
            wMinR.setText(Integer.toString(w.getMinimumRange()));
        } else {
            wMinR.setText("---");
        }
        if(w.getShortRange() > 1) {
            wShortR.setText("1 - " + w.getShortRange());
        } else {
            wShortR.setText("" + w.getShortRange());
        }
        if(w.getMediumRange() - w.getShortRange() > 1) {
            wMedR.setText((w.getShortRange() + 1) + " - " + w.getMediumRange());
        } else {
            wMedR.setText("" + w.getMediumRange());
        }
        if(w.getLongRange() - w.getMediumRange() > 1) {
            wLongR.setText((w.getMediumRange() + 1) + " - " + w.getLongRange());
        } else {
            wLongR.setText("" + w.getLongRange());
        }
    }
        
    // 
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {
        if(ev.getItemSelectable() == weaponList) {
            displaySelected();
        }
    }
        
}

/**
 * This class shows the critical hits and systems for a mech
 */
class SystemPanel 
    extends Panel
    implements ItemListener 
{
    public Label    locLabel, slotLabel;
    public java.awt.List        locList, slotList;
    
    Entity en;
    
    public SystemPanel() {
        super(new GridBagLayout());
        
        locLabel = new Label("Location", Label.CENTER);
        slotLabel = new Label("Slot", Label.CENTER);
        
        locList = new List(8, false);
        locList.addItemListener(this);
        slotList = new List(12, false);
        slotList.setEnabled(false);
        
        // layout main panel
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = 1;
        ((GridBagLayout)getLayout()).setConstraints(locLabel, c);
        add(locLabel);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout)getLayout()).setConstraints(slotLabel, c);
        add(slotLabel);
        
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 1;
        ((GridBagLayout)getLayout()).setConstraints(locList, c);
        add(locList);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout)getLayout()).setConstraints(slotList, c);
        add(slotList);
    }

    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        this.en = en;
        
        locList.removeAll();
        for(int i = 0; i < en.locations(); i++) {
            if(en.getNumberOfCriticals(i) > 0) {
                locList.add(Mech.locationNames[i], i);
            }
        }
        locList.select(0);
        displaySlots();
    }
    
    public void displaySlots() {
        int loc = locList.getSelectedIndex();
        if(loc > Mech.LOC_LT) {
            loc += 3;
        }
        slotList.removeAll();
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            if(cs == null) {
                slotList.add("---");
            } else {
                switch(cs.getType()) {
                case CriticalSlot.TYPE_SYSTEM :
                    slotList.add((cs.isDestroyed() ? "*" : "") + Mech.systemNames[cs.getIndex()]);
                    break;
                case CriticalSlot.TYPE_WEAPON :
                    slotList.add((cs.isDestroyed() ? "*" : "") + en.getWeapon(cs.getIndex()).getType().getName());
                    break;
                case CriticalSlot.TYPE_AMMO :
                    slotList.add((cs.isDestroyed() ? "*" : "") + en.getAmmo(cs.getIndex()).getName());
                    break;
                }
            }
        }
    }

    // 
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {
        if(ev.getItemSelectable() == locList) {
            displaySlots();
        }
    }
}

