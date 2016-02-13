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

/**
 * Normally, reports are dealt with during report phases. When a report is sent
 * at an odd time though, an instance of this class is sent.
 */
public class GameReportEvent extends GameEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -986977282796844524L;
    private String report;

    /**
     * Create a new Report event.
     * 
     * @param source the Object that generated this report
     * @param s a String of the report
     */
    public GameReportEvent(Object source, String s) {
        super(source);
        this.report = s;
    }

    /**
     * Get the text of the report associated with this event.
     * 
     * @return a String of the report
     */
    public String getReport() {
        return this.report;
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
