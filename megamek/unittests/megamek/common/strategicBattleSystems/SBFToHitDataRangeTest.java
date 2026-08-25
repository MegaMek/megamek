/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 */

package megamek.common.strategicBattleSystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.stream.Stream;

import megamek.common.alphaStrike.ASRange;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SBFToHitDataRangeTest {

    @ParameterizedTest
    @MethodSource("effectiveRanges")
    void resolvesEffectiveRange(int distance, ASRange submittedRange, ASRange expectedRange) {
        SBFFormation attacker = formation(new Coords(1, 1), 0);
        SBFFormation target = formation(new Coords(1 + distance, 1), 0);

        assertEquals(Optional.of(expectedRange), SBFToHitData.effectiveRange(attacker, target, submittedRange));
    }

    private static Stream<Arguments> effectiveRanges() {
        return Stream.of(
              Arguments.of(0, ASRange.SHORT, ASRange.SHORT),
              Arguments.of(0, ASRange.MEDIUM, ASRange.MEDIUM),
              Arguments.of(0, ASRange.LONG, ASRange.LONG),
              Arguments.of(1, ASRange.SHORT, ASRange.LONG),
              Arguments.of(1, ASRange.HORIZON, ASRange.LONG),
              Arguments.of(2, ASRange.SHORT, ASRange.EXTREME));
    }

    @Test
    void rejectsIllegalSameHexRange() {
        SBFFormation attacker = formation(new Coords(1, 1), 0);
        SBFFormation target = formation(new Coords(1, 1), 0);

        assertTrue(SBFToHitData.effectiveRange(attacker, target, ASRange.EXTREME).isEmpty());
    }

    @Test
    void rejectsOutOfRangeAndDifferentBoard() {
        SBFFormation attacker = formation(new Coords(1, 1), 0);
        SBFFormation distantTarget = formation(new Coords(5, 1), 0);
        SBFFormation otherBoardTarget = formation(new Coords(1, 1), 1);

        assertTrue(SBFToHitData.effectiveRange(attacker, distantTarget, ASRange.LONG).isEmpty());
        assertTrue(SBFToHitData.effectiveRange(attacker, otherBoardTarget, ASRange.LONG).isEmpty());
    }

    @Test
    void rejectsMissingPositions() {
        SBFFormation attacker = new SBFFormation();
        SBFFormation target = formation(new Coords(1, 1), 0);

        assertTrue(SBFToHitData.effectiveRange(attacker, target, ASRange.LONG).isEmpty());
        assertTrue(SBFToHitData.effectiveRange(target, attacker, ASRange.LONG).isEmpty());
    }

    private static SBFFormation formation(Coords coords, int boardId) {
        SBFFormation formation = new SBFFormation();
        formation.setPosition(BoardLocation.of(coords, boardId));
        return formation;
    }
}
