/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.autoresolve.acar.report;

public class DummyPhaseEndReporter implements IPhaseEndReporter {

    private final static DummyPhaseEndReporter INSTANCE = new DummyPhaseEndReporter();

    private DummyPhaseEndReporter() {
    }

    public static DummyPhaseEndReporter instance() {
        return INSTANCE;
    }

    @Override
    public void movementPhaseHeader() {
    }

    @Override
    public void firingPhaseHeader() {
    }

    @Override
    public void addSummary() {

    }

    @Override
    public void closeTheFile() {

    }
}
