/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur
 * (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.client.ui.swing.util;


import megamek.client.Client;
import megamek.client.ui.swing.AbstractPhaseDisplay;
import megamek.common.IGame;
import megamek.common.options.Option;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class takes a time limit, which is to be set in Basic Options and counts down to zero
 * When zero is reached, the ready() method of the given {@link AbstractPhaseDisplay} is called
 * to end the users current turn.
 *
 */
public class TurnTimer {

    private Timer timer;
    private JProgressBar progressBar;
    private ActionListener listener;
    private JLabel remaining;
    private JDialog display;
    private int timeLimit ;


    public TurnTimer(int limit, AbstractPhaseDisplay phaseDisplay) {

        // make it minutes here.
        timeLimit = limit*60;

        display = new JDialog();
        progressBar = new JProgressBar(JProgressBar.VERTICAL, 0, timeLimit);
        progressBar.setValue(timeLimit);


        listener = new ActionListener() {
            int counter = timeLimit;
            public void actionPerformed(ActionEvent ae) {
                counter--;
                int seconds =counter % 60;
                int minutes = counter/60;
                remaining.setText(minutes+":"+seconds);
                progressBar.setValue(counter);
                if (counter<1) {
                    phaseDisplay.ready();
                    timer.stop();
                    display.setVisible(false);
                    display.dispose();
                }
            }
        };

    }

    public void startTimer() {

        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                remaining = new JLabel(timeLimit+" sec");
                timer = new Timer(1000, listener);
                timer.start();
                display.setLayout(new BorderLayout());
                display.getContentPane().add(remaining, BorderLayout.NORTH);
                display.getContentPane().add(progressBar, BorderLayout.CENTER);
                display.pack();
                display.setAlwaysOnTop(true);
                display.setModal(false);
                //display.setUndecorated(true);
                display.setVisible(true);

            }
        });
    }
    public void stopTimer() {
        display.setVisible(false);
        display.dispose();
        timer.stop();
    }

    public static TurnTimer init(AbstractPhaseDisplay phaseDisplay, Client client){

        //check if there should be a turn timer running
        if (timerShouldStart(client)) {
            Option timer = (Option) client.getGame().getOptions().getOption("turn_timer");
            TurnTimer tt = new TurnTimer(timer.intValue(), phaseDisplay);
            tt.startTimer();
            return tt;
        }
        return null;
    }

    /**
     * Checks if a turn time limit is set in options
     * limit is only imposed on movement, firing
     */
    //TODO: add timer to physical and targeting phase currently it is only in movement and fire
    private static boolean timerShouldStart(Client client) {
        // check if there is a timer set
        Option timer = (Option) client.getGame().getOptions().getOption("turn_timer");
        // if timer is set to 0 in options, it is disabled so we only create one if a limit is set in options
        if (timer.intValue() > 0 ) {
            IGame.Phase phase = client.getGame().getPhase();

            // turn timer should only kick in on firing, targeting, movement and physical attack phase
            return phase == IGame.Phase.PHASE_MOVEMENT || phase == IGame.Phase.PHASE_FIRING || phase == IGame.Phase.PHASE_PHYSICAL || phase == IGame.Phase.PHASE_TARGETING;
        }
        return false;
    }
}
