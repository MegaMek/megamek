/*
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

package megamek.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;
/*Targeting Phase Display.  Breaks naming convention because
TargetingDisplay is too easy to confuse with something else*/
public class TargetingPhaseDisplay
    extends StatusBarPhaseDisplay
    implements BoardListener, GameListener, ActionListener, DoneButtoned,
               KeyListener, ItemListener, BoardViewListener, Distractable
{
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // Action command names
    public static final String FIRE_FIRE       = "fireFire";
    public static final String FIRE_FLIP_ARMS  = "fireFlipArms";
    public static final String FIRE_NEXT       = "fireNext";
    public static final String FIRE_SKIP       = "fireSkip";
    public static final String FIRE_TWIST      = "fireTwist";
    public static final String FIRE_CANCEL     = "fireCancel";
    public static final String FIRE_REPORT     = "fireReport";

    // parent game
    public ClientGUI clientgui;
    private Client client;

    // buttons
    private Container        panButtons;

    private Button            butFire;
    private Button            butTwist;
    private Button            butSkip;
    private Button            butFlipArms;
//    private Button            butReport;
    private Button            butSpace;
    private Button            butNext;
    private Button            butDone;

    private int               buttonLayout;

    // let's keep track of what we're shooting and at what, too
    private int                cen = Entity.NONE;        // current entity number
    private Targetable         target;        // target

    // shots we have so far.
    private Vector attacks;

    // is the shift key held?
    private boolean            shiftheld;
    private boolean            twisting;

    private final int           phase;


    /**
     * Creates and lays out a new targeting phase display
     * for the specified client.
     */
    public TargetingPhaseDisplay(ClientGUI clientgui, boolean offboard) {
        this.clientgui = clientgui;
        this.client = clientgui.getClient();
        this.phase = offboard?Game.PHASE_OFFBOARD:Game.PHASE_TARGETING;
        shiftheld = false;

        // fire
        attacks = new Vector();

        setupStatusBar("Waiting to begin Targeting phase...");

        butFire = new Button("Fire");
        butFire.addActionListener(this);
        butFire.setActionCommand(FIRE_FIRE);
        butFire.setEnabled(false);

        butSkip = new Button("Skip");
        butSkip.addActionListener(this);
        butSkip.setActionCommand(FIRE_SKIP);
        butSkip.setEnabled(false);

        butTwist = new Button("Twist");
        butTwist.addActionListener(this);
        butTwist.setActionCommand(FIRE_TWIST);
        butTwist.setEnabled(false);

        butFlipArms = new Button("Flip Arms");
        butFlipArms.addActionListener(this);
        butFlipArms.setActionCommand(FIRE_FLIP_ARMS);
        butFlipArms.setEnabled(false);

        butSpace = new Button(".");
        butSpace.setEnabled(false);

        butDone = new Button("Done");
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        butNext = new Button(" Next Unit ");
        butNext.addActionListener(this);
        butNext.setActionCommand(FIRE_NEXT);
        butNext.setEnabled(false);

        // layout button grid
        panButtons = new Panel();
        buttonLayout = 0;
        setupButtonPanel();

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
//         c.gridwidth = GridBagConstraints.REMAINDER;
//         addBag(clientgui.bv, gridbag, c);

//         c.weightx = 1.0;    c.weighty = 0;
//         c.gridwidth = 1;
//         addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(panButtons, gridbag, c);

        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panStatus, gridbag, c);

    }

    /**
     * Have the panel register itself as a listener wherever it's needed.
     * <p/>
     * According to http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html
     * it is a major bad no-no to perform these registrations before the
     * constructor finishes, so this function has to be called after the
     * panel is created.  Please note, this restriction only applies to
     * listeners for objects that aren't on the panel itself.
     */
    public void initializeListeners() {

        this.client.addGameListener(this);
        this.client.game.board.addBoardListener(this);

        this.clientgui.bv.addKeyListener( this );
        addKeyListener(this);

        // mech display.
        this.clientgui.mechD.wPan.weaponList.addItemListener(this);
        this.clientgui.mechD.wPan.weaponList.addKeyListener(this);
    }

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(0, 8));

        switch (buttonLayout) {
        case 0 :
            panButtons.add(butNext);
            panButtons.add(butFire);
            panButtons.add(butSkip);
            panButtons.add(butFlipArms);
            panButtons.add(butTwist);
         // panButtons.add(butDone);
            break;

        }

        validate();
    }
    /**
     * Selects an entity, by number, for movement.
     */
    public void selectEntity(int en) {
        // clear any previously considered attacks
        if (en != cen) {
            clearAttacks();
            refreshAll();
        }

        if (client.game.getEntity(en) != null) {
                if (ce() != null) {
                        ce().setSelected(false);
                }
                this.cen = en;
                ce().setSelected(true);

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if ( null == ce().getPosition() ) {

                            // Walk through the list of entities for this player.
                            for ( int nextId = client.getNextEntityNum(en);
                                    nextId != en;
                                nextId = client.getNextEntityNum(nextId) ) {

                                if (null != client.game.getEntity(nextId).getPosition()) {
                                              this.cen = nextId;
                                              break;
                                }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if ( null == ce().getPosition() ) {
                    System.err.println
                        ("FiringDisplay: could not find an on-board entity: " +
                         en);
                    return;
                }

            } // End ce()-not-on-board

            target(null);
            client.game.board.highlight(ce().getPosition());
            client.game.board.select(null);
            client.game.board.cursor(null);

            refreshAll();

            if (!clientgui.bv.isMovingUnits()) {
                clientgui.bv.centerOnHex(ce().getPosition());
            }

            // Update the menu bar.
            clientgui.getMenuBar().setEntity( ce() );

            // 2003-12-29, nemchenk -- only twist if crew conscious
            setTwistEnabled(ce().canChangeSecondaryFacing() && ce().getCrew().isActive());
            setFlipArmsEnabled(ce().canFlipArms());
        } else {
            System.err.println("FiringDisplay: tried to select non-existant entity: " + en);
        }
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        target = null;

        selectEntity(client.getFirstEntityNum());

        if (!clientgui.bv.isMovingUnits()) {
            clientgui.setDisplayVisible(true);
        }

        // There's special processing for triggering AP Pods.
        if ( client.game.getTurn() instanceof GameTurn.TriggerAPPodTurn &&
             null != ce() ) {
            disableButtons();
            TriggerAPPodDialog dialog = new TriggerAPPodDialog
                ( clientgui.getFrame(), ce() );
            dialog.show();
            attacks.removeAllElements();
            Enumeration actions = dialog.getActions();
            while ( actions.hasMoreElements() ) {
                attacks.addElement( actions.nextElement() );
            }
            ready();
        } else {
            setNextEnabled(true);
            butDone.setEnabled(true);
            client.game.board.select(null);
        }
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = client.game.getNextEntity( client.game.getTurnIndex() );
        if ( phase == client.game.getPhase()
             && null != next
             && null != ce()
             && next.getOwnerId() != ce().getOwnerId() ) {
            clientgui.setDisplayVisible(false);
        };
        cen = Entity.NONE;
        target(null);
        client.game.board.select(null);
        client.game.board.highlight(null);
        client.game.board.cursor(null);
        clientgui.bv.clearMovementData();
        disableButtons();

    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setFireEnabled(false);
        setSkipEnabled(false);
        setTwistEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setFlipArmsEnabled(false);
    }





    /**
     * Called when the current entity is done firing.  Send out our attack
     * queue to the server.
     */
    private void ready() {
        if (attacks.isEmpty() && Settings.nagForNoAction) {
            // comfirm this action
            String title = "Don't fire?";
            String body = "This unit has not fired any weapons.\n\n" +
                "Are you really done?\n";
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if ( !response.getShowAgain() ) {
                Settings.nagForNoAction = false;
                Settings.save();
            }
            if ( !response.getAnswer() ) {
                return;
            }
        }

        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // send out attacks
        client.sendAttackData(cen, attacks);

        // clear queue
        attacks.removeAllElements();

        // Clear the menu bar.
        clientgui.getMenuBar().setEntity( null );

        // close aimed shot display, if any

    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack
     * queue.
     */
    private void fire() {
        // get the sepected weaponnum
        int weaponNum = clientgui.mechD.wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if (ce() == null || target == null || mounted == null
        || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException("current fire parameters are invalid");
        }

        WeaponAttackAction waa = new WeaponAttackAction(cen, target.getTargetType(),
                target.getTargetId(), weaponNum);




        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        client.game.addAction(waa);
        clientgui.bv.addAttack(waa);
        clientgui.bv.repaint(100);
        clientgui.minimap.drawMap();

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = ce().getNextWeapon(weaponNum);

        // check; if there are no ready weapons, you're done.
        if (nextWeapon == -1 && Settings.autoEndFiring) {
            ready();
            return;
        }

        // otherwise, display firing info for the next weapon
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(nextWeapon);
        updateTarget();

    }

    /**
     * Skips to the next weapon
     */
    private void nextWeapon() {
        int nextWeapon = ce().getNextWeapon(clientgui.mechD.wPan.getSelectedWeaponNum());
        // if there's no next weapon, forget about it
        if(nextWeapon == -1) {
            return;
        }
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(nextWeapon);
        updateTarget();
    }



    /**
     * Removes all current fire
     */
    private void clearAttacks() {
        // We may not have an entity selected yet (race condition).
        if (ce() == null) {
            return;
        }

        // remove attacks, set weapons available again
        Enumeration i = attacks.elements();
        while ( i.hasMoreElements() ) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction)o;
                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            }
        }
        attacks.removeAllElements();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // restore any other movement to default
        ce().setSecondaryFacing(ce().getFacing());
        ce().setArmsFlipped(false);

    }

    /**
     * Removes temp attacks from the game and board
     */
    private void removeTempAttacks() {
        // remove temporary attacks from game & board
        client.game.removeActionsFor(cen);
        clientgui.bv.removeAttacksFor(cen);
        clientgui.bv.repaint(100);

    }

    /**
     * Refeshes all displays.
     */
    private void refreshAll() {
        if (ce() == null) {
            return;
        }
        clientgui.bv.redrawEntity(ce());
        clientgui.mechD.displayEntity(ce());
        clientgui.mechD.showPanel("weapons");
        clientgui.mechD.wPan.selectWeapon(ce().getFirstWeapon());
        updateTarget();
    }

    /**
     * Targets something
     */
    void target(Targetable t) {
        this.target = t;
        updateTarget();

    }

    /**
     * Targets something
     */
    protected void updateTarget() {
        setFireEnabled(false);

        // make sure we're showing the current entity in the mech display
        if (ce() != null && !ce().equals(clientgui.mechD.getCurrentEntity())) {
            clientgui.mechD.displayEntity(ce());
        }

        // update target panel
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        if (target != null && weaponId != -1) {
            ToHitData toHit;

              toHit = Compute.toHitWeapon(client.game, cen, target, weaponId, Mech.LOC_NONE, 0);
              clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName());

            clientgui.mechD.wPan.wRangeR.setText("" + ce().getPosition().distance(target.getPosition()));
            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                clientgui.mechD.wPan.wToHitR.setText("Already fired");
                setFireEnabled(false);
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                clientgui.mechD.wPan.wToHitR.setText("Auto-firing weapon");
                setFireEnabled(false);
            } else if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(false);
            } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(true);
            } else {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString() + " (" + Compute.oddsAbove(toHit.getValue()) + "%)");
                setFireEnabled(true);
            }
            clientgui.mechD.wPan.toHitText.setText(toHit.getDesc());
            setSkipEnabled(true);
        } else {
            clientgui.mechD.wPan.wTargetR.setText("---");
            clientgui.mechD.wPan.wRangeR.setText("---");
            clientgui.mechD.wPan.wToHitR.setText("---");
            clientgui.mechD.wPan.toHitText.setText("");
        }
    }

    /**
     * Torso twist in the proper direction.
     */
    private void torsoTwist(Coords target) {
        int direction = ce().getFacing();

        if ( null != target )
          direction = ce().clipSecondaryFacing(ce().getPosition().direction(target));

        if (direction != ce().getSecondaryFacing()) {
            clearAttacks();
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        }
    }

    /**
     * Returns the current entity.
     */
    private Entity ce() {
        return client.game.getEntity(cen);
    }

    //
    // BoardListener
    //
    public void boardHexMoused(BoardEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        // ignore buttons other than 1
        if (!client.isMyTurn() || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }
    // control pressed means a line of sight check.
    // added ALT_MASK by kenn
    if ((b.getModifiers() & InputEvent.CTRL_MASK) != 0 || (b.getModifiers() & InputEvent.ALT_MASK) != 0) {
      return;
    }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & MouseEvent.SHIFT_MASK) != 0)) {
            shiftheld = (b.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
        }

        if (b.getType() == BoardEvent.BOARD_HEX_DRAGGED) {
            if (shiftheld || twisting) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            }
            client.game.board.cursor(b.getCoords());
        } else if (b.getType() == BoardEvent.BOARD_HEX_CLICKED) {
            twisting = false;
            client.game.board.select(b.getCoords());
        }
    }

    public void boardHexSelected(BoardEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.isMyTurn() && b.getCoords() != null && ce() != null && !b.getCoords().equals(ce().getPosition())) {
            boolean friendlyFire = client.game.getOptions().booleanOption("friendly_fire");
            if (shiftheld) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            } else if (friendlyFire && client.game.getFirstEntity(b.getCoords()) != null) {
                target(client.game.getFirstEntity(b.getCoords()));
            } else if ( client.game.getFirstEnemyEntity(b.getCoords(), ce()) != null) {
                target(client.game.getFirstEnemyEntity(b.getCoords(), ce()));
            }
        }
    }

    //
    // GameListener
    //
    public void gameTurnChange(GameEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if(client.game.getPhase() == phase) {
            endMyTurn();

            if(client.isMyTurn()) {
                beginMyTurn();
                setStatusBarText("It's your turn to fire.");
            } else {
                setStatusBarText("It's " + ev.getPlayer().getName() + "'s turn to fire.");
            }
        }
    }
    public void gamePhaseChange(GameEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if(client.isMyTurn() && client.game.getPhase() != phase) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if(client.game.getPhase() ==  phase) {
            setStatusBarText("Waiting to begin Firing phase...");
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if ( statusBarActionPerformed(ev, client) )
          return;

        if (!client.isMyTurn()) {
            return;
        }

        if (ev.getSource() == butDone) {
            ready();
        } else if (ev.getActionCommand().equals(FIRE_REPORT)) {
            new MiniReportDisplay(clientgui.frame, client.eotr).show();
            return;
        } else if (ev.getActionCommand().equalsIgnoreCase("viewGameOptions")) {
            // Make sure the game options dialog is not editable.
            if ( clientgui.getGameOptionsDialog().isEditable() ) {
                clientgui.getGameOptionsDialog().setEditable( false );
            }
            // Display the game options dialog.
            clientgui.getGameOptionsDialog().update(client.game.getOptions());
            clientgui.getGameOptionsDialog().show();
        } else if (ev.getActionCommand().equals(FIRE_FIRE)) {
            fire();
        } else if (ev.getActionCommand().equals(FIRE_SKIP)) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(FIRE_TWIST)) {
            twisting = true;
        } else if (ev.getActionCommand().equals(FIRE_NEXT)) {
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(FIRE_FLIP_ARMS)) {
            updateFlipArms(!ce().getArmsFlipped());
        } else if (ev.getActionCommand().equals(FIRE_CANCEL)) {
            clearAttacks();
            client.game.board.select(null);
            client.game.board.cursor(null);
            refreshAll();
        }
    }

    private void updateFlipArms(boolean armsFlipped) {
        if (armsFlipped == ce().getArmsFlipped()) {
            return;
        }

      twisting = false;

      torsoTwist(null);

      clearAttacks();
      ce().setArmsFlipped(armsFlipped);
      attacks.addElement(new FlipArmsAction(cen, armsFlipped));
      updateTarget();
      refreshAll();
    }

        private void setFireEnabled(boolean enabled) {
                butFire.setEnabled(enabled);
        clientgui.getMenuBar().setFireFireEnabled(enabled);
        }
        private void setTwistEnabled(boolean enabled) {
                butTwist.setEnabled(enabled);
        clientgui.getMenuBar().setFireTwistEnabled(enabled);
        }
        private void setSkipEnabled(boolean enabled) {
                butSkip.setEnabled(enabled);
        clientgui.getMenuBar().setFireSkipEnabled(enabled);
        }
        private void setFlipArmsEnabled(boolean enabled) {
                butFlipArms.setEnabled(enabled);
        clientgui.getMenuBar().setFireFlipArmsEnabled(enabled);
        }
        private void setNextEnabled(boolean enabled) {
                butNext.setEnabled(enabled);
        clientgui.getMenuBar().setFireNextEnabled(enabled);
        }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearAttacks();
            client.game.board.select(null);
            client.game.board.cursor(null);
            refreshAll();
        }
        if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.isControlDown()) {
            if (client.isMyTurn()) {
                //
            }
        }
        if (ev.getKeyCode() == KeyEvent.VK_SHIFT && !shiftheld) {
            shiftheld = true;
            if (client.isMyTurn() && client.game.board.lastCursor != null) {
                updateFlipArms(false);
                torsoTwist(client.game.board.lastCursor);
            }
        }
/*        if (ev.getKeyCode() == KeyEvent.VK_M) {
            changeMode();
        }
*/  }
    public void keyReleased(KeyEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (ev.getKeyCode() == KeyEvent.VK_SHIFT && shiftheld) {
            shiftheld = false;
        }
    }
    public void keyTyped(KeyEvent ev) {
    }

    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if(ev.getItemSelectable() == clientgui.mechD.wPan.weaponList) {
            // update target data in weapon display
            updateTarget();
        }
    }

    // board view listener
    public void finishedMovingUnits(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.isMyTurn() && ce() != null) {
            clientgui.setDisplayVisible(true);
            clientgui.bv.centerOnHex(ce().getPosition());
        }
    }

    public void selectUnit(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

            Entity e = client.game.getEntity(b.getEntityId());
            if (client.isMyTurn()) {
            if ( client.game.getTurn().isValidEntity(e,client.game) ) {
                selectEntity(e.getId());
            }
            } else {
            clientgui.setDisplayVisible(true);
            clientgui.mechD.displayEntity(e);
            if (e.isDeployed()) {
                    clientgui.bv.centerOnHex(e.getPosition());
            }
            }
    }


    /**
     * Determine if the listener is currently distracted.
     *
     * @return  <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents() {
        return this.distracted.isIgnoringEvents();
    }

    /**
     * Specify if the listener should be distracted.
     *
     * @param   distract <code>true</code> if the listener should ignore events
     *          <code>false</code> if the listener should pay attention again.
     *          Events that occured while the listener was distracted NOT
     *          going to be processed.
     */
    public void setIgnoringEvents( boolean distracted ) {
        this.distracted.setIgnoringEvents( distracted );
    }

    /**
     * Retrieve the "Done" button of this object.
     *
     * @return  the <code>java.awt.Button</code> that activates this
     *          object's "Done" action.
     */
    public Button getDoneButton() {
        return butDone;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        client.removeGameListener(this);
        client.game.board.removeBoardListener(this);
        clientgui.mechD.wPan.weaponList.removeItemListener(this);
    }

}
