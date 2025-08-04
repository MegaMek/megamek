/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.util;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import megamek.client.IClient;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.panels.phaseDisplay.AbstractPhaseDisplay;
import megamek.common.enums.GamePhase;
import megamek.common.options.OptionsConstants;

/**
 * This class takes a time limit, which is to be set in Basic Options and counts down to zero When zero is reached, the
 * ready() method of the given {@link AbstractPhaseDisplay} is called to end the users current turn.
 */
public class TurnTimer {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final JProgressBar progressBar;
    private final ActionListener listener;
    private final JLabel remaining;
    private final JPanel display;
    private final int timeLimit;
    private final AbstractPhaseDisplay phaseDisplay;
    private final boolean allowExtension;

    private Timer timer;
    private boolean extendTimer;
    private boolean expired;

    public TurnTimer(int limit, AbstractPhaseDisplay pD, IClient client) {
        phaseDisplay = pD;
        // linit in seconds.
        timeLimit = limit;
        extendTimer = false;
        expired = false;

        display = new JPanel();
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, timeLimit);
        progressBar.setValue(timeLimit);
        progressBar.setForeground(GUIP.getCautionColor());
        int seconds = timeLimit % 60;
        int minutes = timeLimit / 60;
        remaining = new JLabel(String.format("%s:%02d", minutes, seconds));
        phaseDisplay.getClientgui().turnTimerComponent().add(display);
        display.setLayout(new FlowLayout());
        display.add(remaining);
        display.add(progressBar);

        var options = client.getGame().getOptions();
        allowExtension = options.getOption(OptionsConstants.BASE_TURN_TIMER_ALLOW_EXTENSION).booleanValue();

        listener = new ActionListener() {
            int counter = timeLimit;

            @Override
            public void actionPerformed(ActionEvent ae) {
                counter--;
                int seconds = counter % 60;
                int minutes = counter / 60;
                String extended = (extendTimer && allowExtension) ? "\u2B50" : "";
                String text = String.format("%s:%02d %s", minutes, seconds, extended);
                remaining.setText(text);
                Color c = counter >= 10 ? GUIP.getCautionColor() : GUIP.getWarningColor();
                progressBar.setForeground(c);
                progressBar.setValue(counter);

                if (counter < 2 && extendTimer && allowExtension) {
                    timer.restart();
                    counter = timeLimit;
                    extendTimer = false;
                    client.sendChat("Turn Timer extended");
                } else if (counter < 1) {
                    // prevent the popup dialog from breaking time limit
                    expired = true;
                    phaseDisplay.ready();
                    timer.stop();
                    display.setVisible(false);
                    phaseDisplay.getClientgui().turnTimerComponent().remove(display);
                }
            }
        };
    }

    public void startTimer() {
        SwingUtilities.invokeLater(() -> {
            timer = new Timer(1000, listener);
            phaseDisplay.getClientgui().turnTimerComponent().add(display);
            timer.start();
            display.setVisible(true);
        });
    }

    public void stopTimer() {
        display.setVisible(false);

        if (phaseDisplay.getClientgui().turnTimerComponent() != null) {
            phaseDisplay.getClientgui().turnTimerComponent().remove(display);
        }

        if (timer != null) {
            timer.stop();
        }
    }

    public void setExtendTimer() {
        extendTimer = true;
    }

    public boolean isTimerExpired() {
        return expired;
    }

    public static TurnTimer init(AbstractPhaseDisplay phaseDisplay, IClient client) {
        // check if there should be a turn timer running
        var options = client.getGame().getOptions();
        GamePhase phase = client.getGame().getPhase();

        int timerLimit = 0;

        switch (phase) {
            case TARGETING:
            case SET_ARTILLERY_AUTOHIT_HEXES:
            case DEPLOY_MINEFIELDS:
                timerLimit = options.getOption(OptionsConstants.BASE_TURN_TIMER_TARGETING).intValue();
                break;
            case MOVEMENT:
                timerLimit = options.getOption(OptionsConstants.BASE_TURN_TIMER_MOVEMENT).intValue();
                break;
            case FIRING:
                timerLimit = options.getOption(OptionsConstants.BASE_TURN_TIMER_FIRING).intValue();
                break;
            case PHYSICAL:
                timerLimit = options.getOption(OptionsConstants.BASE_TURN_TIMER_PHYSICAL).intValue();
                break;
            default:
        }

        if (timerLimit > 0) {
            TurnTimer tt = new TurnTimer(timerLimit, phaseDisplay, client);
            tt.startTimer();
            return tt;
        }

        return null;
    }
}
