/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.event;

import java.util.Collections;
import java.util.List;

import megamek.common.Game;
import megamek.common.Report;


/**
 * Normally, reports are dealt with during report phases. When a report is sent
 * at an odd time though, an instance of this class is sent.
 */
public class GameReportEvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -986977282796844524L;

    private int round;
    private List<Report> reports;
    private boolean isImportant;

    /**
     * Emitted when new game reports are received from the server.
     *
     * @param source game instance that emitted the event
     * @param round the round this report is associated with
     * @param reports new reports
     * @param isImportant determines if the reports are high-priority
     *                    or standard priority
     */
    public GameReportEvent(Game source,
                           int round,
                           List<Report> reports,
                           boolean isImportant) {
        super(source);
        this.round = round;
        this.reports = Collections.unmodifiableList(reports);
        this.isImportant = isImportant;
    }

    /**
     * Returns the round associated with the event's report.
     *
     * @return a round number
     */
    public int getRound() {
        return this.round;
    }

    /**
     * Returns the added reports
     *
     * @return added reports
     */
    public List<Report> getReports() {
        return this.reports;
    }

    /**
     * Determines if these are high or standard priority reports.
     *
     * @return true if high priority, false if standard
     */
    public boolean isImportant() {
        return this.isImportant;
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameReport(this);
    }

    @Override
    public String getEventName() {
        return "Game Report";
    }
}
