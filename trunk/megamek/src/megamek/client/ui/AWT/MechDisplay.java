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
    public Button                movBut, armBut, weaBut, criBut, extBut;
    
    public Panel                displayP;
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
        movBut = new Button("Movement");
        movBut.addActionListener(this);
        armBut = new Button("Armor");
        armBut.addActionListener(this);
        weaBut = new Button("Weapons");
        weaBut.addActionListener(this);
        criBut = new Button("Systems");
        criBut.addActionListener(this);
        extBut = new Button("Extras");
        extBut.addActionListener(this);
        
        displayP = new Panel(new CardLayout());
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
        c.insets = new Insets(1, 1, 1, 1);
        
        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(movBut, c);
        
        addBag(armBut, c);
        
        addBag(weaBut, c);
        
        addBag(criBut, c);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(extBut, c);
        
        c.weightx = 1.0;    c.weighty = 1.0;
        addBag(displayP, c);
        
        ((CardLayout)displayP.getLayout()).show(displayP, "movement");
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
        if(e.getActionCommand().equalsIgnoreCase("extras")) {
            showPanel("extras");
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
        super();
        
        GridBagLayout gridbag;
        GridBagConstraints c;
            
        mechTypeL = new Label("Loc0st LCT-1L", Label.CENTER);

        // status stuff
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
            
        // movement stuff
        mpL = new Label("Movement:", Label.RIGHT);
        curMoveL = new Label("Currently:", Label.RIGHT);
        heatL = new Label("Heat:", Label.RIGHT);

        mpR = new Label("8/12/0", Label.LEFT);
        curMoveR = new Label("No Movement", Label.LEFT);
        heatR = new Label("2 (10 capacity)", Label.LEFT);
            
        statusP = new Panel();
        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        statusP.setLayout(gridbag);
        
        c.insets = new Insets(1, 1, 1, 1);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        
        c.gridwidth = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(statusL, c);
        statusP.add(statusL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(statusR, c);
        statusP.add(statusR);

        c.gridwidth = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(playerL, c);
        statusP.add(playerL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(playerR, c);
        statusP.add(playerR);
        
        c.gridwidth = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(teamL, c);
        statusP.add(teamL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(teamR, c);
        statusP.add(teamR);

        c.gridwidth = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(weightL, c);
        statusP.add(weightL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(weightR, c);
        statusP.add(weightR);

        c.gridwidth = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(pilotL, c);
        statusP.add(pilotL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(pilotR, c);
        statusP.add(pilotR);
        
        c.gridwidth = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(mpL, c);
        statusP.add(mpL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(mpR, c);
        statusP.add(mpR);
        
        c.gridwidth = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(curMoveL, c);
        statusP.add(curMoveL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(curMoveR, c);
        statusP.add(curMoveR);
        
        c.gridwidth = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(heatL, c);
        statusP.add(heatL);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(heatR, c);
        statusP.add(heatR);

        // layout main panel
        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        setLayout(gridbag);
        
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(mechTypeL, c);
        add(mechTypeL);
        
        c.weightx = 1.0;    c.weighty = 1.0;
        gridbag.setConstraints(statusP, c);
        add(statusP);
    }
        
    /**
     * updates fields for the specified mech
     */
    public void displayMech(Entity en) {
        this.mechTypeL.setText(en.getShortName());

        this.statusR.setText(en.isProne() ? "prone" : "normal");
        this.playerR.setText(en.getOwner().getName());
        this.teamR.setText(en.getOwner().getTeam() == 0 ? "No Team" : "Team " + en.getOwner().getTeam());
        this.weightR.setText(Integer.toString((int)en.getWeight()));
        this.pilotR.setText(en.crew.getDesc());

        StringBuffer mp = new StringBuffer();
        mp.append(en.getWalkMP());
        mp.append('/');
        mp.append(en.getRunMP());
        mp.append('/');
        mp.append(en.getJumpMPWithTerrain());
        if (en.mpUsed > 0) {
            mp.append(" (");
            mp.append(en.mpUsed);
            mp.append(" used)");
        }
        this.mpR.setText(mp.toString());
        this.curMoveR.setText(en.getMovementString(en.moved) + (en.moved == en.MOVE_NONE ? "" : " " + en.delta_distance));
        
        int heatCap = en.getHeatCapacity();
        int heatCapWater = en.getHeatCapacityWithWater();
        String heatCapacityStr = Integer.toString(heatCap);
        
        if ( heatCap < heatCapWater ) {
          heatCapacityStr = heatCap + " [" + heatCapWater + "]";
        }
        
        this.heatR.setText(Integer.toString(en.heat) + " (" + heatCapacityStr + " capacity)");

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
            this.internalL[i].setText(en.getInternalString(i));
            this.armorL[i].setText(en.getArmorString(i) + (en.hasRearArmor(i) ? " (" + en.getArmorString(i, true) + ")" : ""));
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
    public Choice m_chAmmo;
    public Panel displayP, rangeP, targetP, buttonP;
        
    public Label wAmmo, wNameL, wHeatL, wDamL, wMinL, wShortL,
        wMedL, wLongL;
    public Label wNameR, wHeatR, wDamR, wMinR, wShortR,
        wMedR, wLongR;
    public Label currentHeatBuildupL, currentHeatBuildupR;
        
    public Label wTargetL, wRangeL, wToHitL;
    public Label wTargetR, wRangeR, wToHitR;
        
    public TextArea toHitText;
        
    public Button ammoB;
        
    // I need to keep a pointer to the weapon list of the
    // currently selected mech.
    private Vector weapons;
    private Vector vAmmo;
    private Entity entity;
    private Client client;
        
    public WeaponPanel(Client client) {        
        super(new GridBagLayout());
        
        this.client = client;
            
        // weapon list
        weaponList = new java.awt.List(4, false);
        weaponList.addItemListener(this);
        
        // ammo choice panel
        wAmmo = new Label("Ammo", Label.LEFT);
        m_chAmmo = new Choice();
        m_chAmmo.addItemListener(this);
        
        Panel ammoP = new Panel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = 1;
        c.weightx = 0.0;    c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        ((GridBagLayout)ammoP.getLayout()).setConstraints(wAmmo, c);
        ammoP.add(wAmmo);
        
        c.gridwidth = 3;
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        ((GridBagLayout)ammoP.getLayout()).setConstraints(m_chAmmo, c);
        ammoP.add(m_chAmmo);
            
        currentHeatBuildupL = new Label("Heat Buildup");
        currentHeatBuildupR = new Label("--");
        Panel currentHeatP = new Panel(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = 1;
        c.weightx = 0.0;    c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        ((GridBagLayout)currentHeatP.getLayout()).setConstraints(currentHeatBuildupL, c);
        currentHeatP.add(currentHeatBuildupL);
        
        c.gridwidth = 3;
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        ((GridBagLayout)currentHeatP.getLayout()).setConstraints(currentHeatBuildupR, c);
        currentHeatP.add(currentHeatBuildupR);

            
        // weapon display panel
        wNameL = new Label("Name", Label.LEFT);
        wHeatL = new Label("Heat", Label.CENTER);
        wDamL = new Label("Damage", Label.CENTER);
        wNameR = new Label("", Label.LEFT);
        wHeatR = new Label("--", Label.CENTER);
        wDamR = new Label("--", Label.CENTER);
            
        displayP = new Panel(new GridBagLayout());
            
        c = new GridBagConstraints();
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
        
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ((GridBagLayout)getLayout()).setConstraints(weaponList, c);
        add(weaponList);
        
        c.weightx = 1.0;    c.weighty = 0.0;
        ((GridBagLayout)getLayout()).setConstraints(ammoP, c);
        add(ammoP);
        
        c.weightx = 1.0;    c.weighty = 0.0;
        ((GridBagLayout)getLayout()).setConstraints(currentHeatP, c);
        add(currentHeatP);

        c.weightx = 1.0;    c.weighty = 0.0;
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
                    if (mounted.getLinked() == mountedAmmo) {
                        nCur = i;
                    }
                    i++;
                }
            }
            for (int x = 0, n = vAmmo.size(); x < n; x++) {
                m_chAmmo.add(formatAmmo((Mounted)vAmmo.elementAt(x)));
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
        }
    }
        
}

/**
 * This class shows the critical hits and systems for a mech
 */
class SystemPanel 
    extends Panel
    implements ItemListener, ActionListener
{
    private static Object SYSTEM = new Object();
    
    public Label locLabel;
    public Label slotLabel;
    public java.awt.List slotList;
    public java.awt.List locList;
    
    private Vector vEquipment = new Vector(16);
    
    public Choice m_chMode;
    public Button m_bDumpAmmo;
    public Label modeLabel;
    private Client client;
    
    Entity en;
    
    public SystemPanel(Client client) {
        super();
        
        this.client = client;
        
        locLabel = new Label("Location", Label.CENTER);
        slotLabel = new Label("Slot", Label.CENTER);
        
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
        modeLabel = new Label("Mode", Label.CENTER);
        modeLabel.setEnabled(false);
        
        // layout choice panel
        Panel p = new Panel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        p.setLayout(gridbag);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 0.0;    c.weighty = 0.0;
        c.gridwidth = 1;    c.gridheight = 1;
        gridbag.setConstraints(modeLabel, c);
        p.add(modeLabel);
        
        c.gridx = 1;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(m_chMode, c);
        p.add(m_chMode);
        
        c.gridx = 0;        c.gridy = 1;
        gridbag.setConstraints(m_bDumpAmmo, c);
        p.add(m_bDumpAmmo);
        
        // layout main panel
        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        setLayout(gridbag);
        
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        
        c.weightx = 0.5;    c.weighty = 0.0;
        c.gridwidth = 1;
        gridbag.setConstraints(locLabel, c);
        add(locLabel);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(slotLabel, c);
        add(slotLabel);
        
        c.weightx = 0.5;    c.weighty = 1.0;
        c.gridwidth = 1;
        c.gridheight = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(locList, c);
        add(locList);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 2;
        c.gridy = 1;
        c.gridx = 1;
        c.weightx = 1.0;
        c.weighty = 0.8;
        gridbag.setConstraints(slotList, c);
        add(slotList);
        
        c.gridy = 4;
        c.weighty = 0.2;
        c.gridheight = 1;
        gridbag.setConstraints(p, c);
        add(p);
    }
    
    public int getSelectedLocation() {
        return locList.getSelectedIndex();
    }
    
    public Mounted getSelectedEquipment() {
        int n = slotList.getSelectedIndex();
        if (n == -1) {
            return null;
        }
        Object o = vEquipment.elementAt(n);
        if (o == SYSTEM) {
            return null;
        }
        return (Mounted)o;
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
        vEquipment = new Vector(16);
        for (int i = 0; i < en.getNumberOfCriticals(loc); i++) {
            final CriticalSlot cs = en.getCritical(loc, i);
            StringBuffer sb = new StringBuffer(32);
            if(cs == null) {
                sb.append("---");
            } else {
                switch(cs.getType()) {
                case CriticalSlot.TYPE_SYSTEM :
                    sb.append(cs.isDestroyed() ? "*" : "").append(Mech.systemNames[cs.getIndex()]);
                    vEquipment.addElement(SYSTEM);
                    break;
                case CriticalSlot.TYPE_EQUIPMENT :
                    Mounted m = en.getEquipment(cs.getIndex());
                    sb.append(cs.isDestroyed() ? "*" : "").append(m.getDesc());
                    if (m.getType().hasModes()) {
                        sb.append(" (").append(m.curMode()).append(")");
                    }
                    vEquipment.addElement(m);
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
                m_chMode.setEnabled(true);
                modeLabel.setEnabled(true);
                m_chMode.removeAll();
                String[] saModes = m.getType().getModes();
                for (int x = 0; x < saModes.length; x++) {
                    m_chMode.add(saModes[x]);
                }
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
}


/**
 * This class shows information about a unit that doesn't belong elsewhere.
 */
class ExtraPanel 
    extends Panel
{
    private static Object SYSTEM = new Object();
    
    public Label    unusedL, carrysL;
    public TextArea unusedR, carrysR;
    public Label narcLabel;
    public java.awt.List narcList;

    private Client client;
    
    public ExtraPanel(Client client) {
        super();
        
        this.client = client;
        
        narcLabel = new Label("NARCed By:", Label.CENTER);
        
        narcList = new List(3, false);

        // transport stuff
        unusedL = new Label( "Unused Space:", Label.CENTER );
        unusedR = new TextArea("", 2, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        unusedR.setEditable(false);
        carrysL = new Label( "Carrying:", Label.CENTER );
        carrysR = new TextArea("", 4, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        carrysR.setEditable(false);

        // layout choice panel
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        gridbag = new GridBagLayout();
        c = new GridBagConstraints();
        setLayout(gridbag);
        
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1.0;

        c.weighty = 0.0;
        gridbag.setConstraints(narcLabel, c);
        add(narcLabel);
        
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
        
        c.weighty = 1.0;
        gridbag.setConstraints(carrysR, c);
        add(carrysR);

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

