/*
 * MegaMek - Copyright (C) 2020 - The MegaMek Team
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
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.enums.GamePhase;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class takes a time limit, which is to be set in Basic Options and counts down to zero
 * When zero is reached, the ready() method of the given {@link AbstractPhaseDisplay} is called
 * to end the users current turn.
 */
public class TurnTimer {
    private Timer timer;
    private JProgressBar progressBar;
    private ActionListener listener;
    private JLabel remaining;
    private JPanel display;
    private int timeLimit;
    private AbstractPhaseDisplay phaseDisplay;

    public TurnTimer(int limit, AbstractPhaseDisplay pD) {
        phaseDisplay = pD;
        // make it minutes here.
        timeLimit = limit * 60;

        display = new JPanel();
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, timeLimit);
        progressBar.setValue(timeLimit);
        progressBar.setForeground(Color.RED);
        remaining = new JLabel((timeLimit / 60) + ":" + (timeLimit % 60));
        phaseDisplay.getClientgui().getMenuBar().add(display);
        display.setLayout(new FlowLayout());
        display.add(remaining);
        display.add(progressBar);

        listener = new ActionListener() {
            int counter = timeLimit;
            @Override
            public void actionPerformed(ActionEvent ae) {
                counter--;
                int seconds = counter % 60;
                int minutes = counter / 60;
                remaining.setText(minutes + ":" + seconds);
                progressBar.setValue(counter);

                if (counter < 1) {
                    // get the NagForNoAction setting here
                    boolean nagSet = GUIPreferences.getInstance().getNagForNoAction();
                    // prevent the popup dialog from breaking time limit
                    GUIPreferences.getInstance().setNagForNoAction(false);
                    phaseDisplay.ready();
                    // reset NagForNoAction to the value it was before to preserve the user experience
                    // for use cases outside the timer
                    GUIPreferences.getInstance().setNagForNoAction(nagSet);
                    timer.stop();
                    display.setVisible(false);
                    phaseDisplay.getClientgui().getMenuBar().remove(display);
                }
            }
        };
    }

    public void startTimer() {
        SwingUtilities.invokeLater(() -> {
            timer = new Timer(1000, listener);
            phaseDisplay.getClientgui().getMenuBar().add(display);
            timer.start();
            display.setVisible(true);

        });
    }

    public void stopTimer() {
        display.setVisible(false);
        phaseDisplay.getClientgui().getMenuBar().remove(display);
        timer.stop();
    }

    public static TurnTimer init(AbstractPhaseDisplay phaseDisplay, Client client) {
        // check if there should be a turn timer running
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
        Option timer = (Option) client.getGame().getOptions().getOption(OptionsConstants.BASE_TURN_TIMER);
        // if timer is set to 0 in options, it is disabled so we only create one if a limit is set in options
        if (timer.intValue() > 0) {
            GamePhase phase = client.getGame().getPhase();

            // turn timer should only kick in on firing, targeting, movement and physical attack phase
            return phase == GamePhase.MOVEMENT || phase == GamePhase.FIRING || phase == GamePhase.PHYSICAL || phase == GamePhase.TARGETING;
        }
        return false;
    }
}
