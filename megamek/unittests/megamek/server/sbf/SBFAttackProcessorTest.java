/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 of the License, or (at your option) any later version,
 * as published by the Free Software Foundation.
 */

package megamek.server.sbf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.Player;
import megamek.common.actions.EntityAction;
import megamek.common.actions.sbf.SBFAttackAction;
import megamek.common.actions.sbf.SBFStandardUnitAttack;
import megamek.common.alphaStrike.ASRange;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.options.SBFRuleOptions;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFToHitData;
import megamek.common.strategicBattleSystems.SBFUnit;
import megamek.common.strategicBattleSystems.SBFVisibilityStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests validation and authoritative mutation of SBF attack submissions. */
class SBFAttackProcessorTest {

    private static final int OWNER_ID = 1;
    private static final int ENEMY_ID = 2;
    private static final int FORMATION_ID = 17;
    private static final int TARGET_ID = 20;

    private SBFGame game;
    private SBFGameManager gameManager;
    private SBFAttackProcessor processor;
    private SBFFormation formation;
    private SBFFormation target;

    @BeforeEach
    void setUp() {
        game = new SBFGame();
        game.setPhase(GamePhase.FIRING);
        game.addPlayer(OWNER_ID, new Player(OWNER_ID, "Owner"));
        game.addPlayer(ENEMY_ID, new Player(ENEMY_ID, "Enemy"));
        formation = formation(FORMATION_ID, OWNER_ID, new Coords(1, 1), 2);
        target = formation(TARGET_ID, ENEMY_ID, new Coords(2, 1), 1);
        game.addUnit(formation);
        game.addUnit(target);

        gameManager = mock(SBFGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        processor = new SBFAttackProcessor(gameManager);
    }

    @Test
    void validAttackIsRebuiltWithAuthoritativeRangeAndEndsTurn() {
        SBFStandardUnitAttack submitted = attack(0, target, ASRange.SHORT);

        assertTrue(processor.processAttacks(List.of(submitted), formation));

        SBFStandardUnitAttack published = (SBFStandardUnitAttack) game.getActionsVector().getFirst();
        assertNotSame(submitted, published);
        assertEquals(ASRange.LONG, published.getRange());
        assertTrue(formation.isDone());
        verify(gameManager).sendUnitUpdate(formation);
        verify(gameManager).sendPendingActions();
        verify(gameManager).endCurrentTurn(formation);
    }

    @Test
    void emptySubmissionEndsFiringTurnWithoutActions() {
        assertTrue(processor.processAttacks(List.of(), formation));
        assertTrue(game.getActionsVector().isEmpty());
        assertTrue(formation.isDone());
    }

    @Test
    void wrongPhaseRejectsWithoutMutation() {
        game.setPhase(GamePhase.TARGETING);
        assertRejected(List.of(attack(0, target, ASRange.LONG)));
    }

    @Test
    void sprintedFormationCannotAttack() {
        formation.setSprintedThisTurn(true);

        assertRejected(List.of(attack(0, target, ASRange.LONG)));
    }

    @Test
    void attacksAgainstSprintingTargetReceiveToHitBonus() {
        SBFStandardUnitAttack submitted = attack(0, target, ASRange.LONG);
        int normalTargetNumber = SBFToHitData.compileToHit(game, submitted).getValue();

        target.setSprintedThisTurn(true);

        SBFToHitData sprintedTargetToHit = SBFToHitData.compileToHit(game, submitted);
        assertEquals(normalTargetNumber - 1, sprintedTargetToHit.getValue());
        assertTrue(sprintedTargetToHit.getDesc().contains("target sprinted"));
    }

    @Test
    void duplicateUnitInSubmissionRejectsAtomically() {
        assertRejected(List.of(attack(0, target, ASRange.LONG), attack(0, target, ASRange.LONG)));
    }

    @Test
    void duplicateUnitAgainstPendingAttackRejects() {
        SBFStandardUnitAttack existing = attack(0, target, ASRange.LONG);
        game.addAction(existing);

        assertFalse(processor.processAttacks(List.of(attack(0, target, ASRange.LONG)), formation));
        assertEquals(List.of(existing), game.getActionsVector());
        assertFalse(formation.isDone());
        verifyNoPublication();
    }

    @Test
    void differentUnitsMayAttackSameTarget() {
        assertTrue(processor.processAttacks(List.of(attack(0, target, ASRange.LONG),
              attack(1, target, ASRange.LONG)), formation));
        assertEquals(2, game.getActionsVector().size());
    }

    @Test
    void friendlyTargetRejects() {
        target.setOwnerId(OWNER_ID);
        assertRejected(List.of(attack(0, target, ASRange.LONG)));
    }

    @Test
    void hiddenTargetRejectsWithReconEnabled() {
        game.getOptions().getOption(SBFRuleOptions.BASE_RECON).setValue(true);
        game.visibilityHelper().setVisibility(OWNER_ID, TARGET_ID, SBFVisibilityStatus.PARTIAL_SCAN);
        assertRejected(List.of(attack(0, target, ASRange.LONG)));
    }

    @Test
    void visibleTargetSucceedsWithReconEnabled() {
        game.getOptions().getOption(SBFRuleOptions.BASE_RECON).setValue(true);
        game.visibilityHelper().setVisible(OWNER_ID, TARGET_ID);
        assertTrue(processor.processAttacks(List.of(attack(0, target, ASRange.LONG)), formation));
    }

    @Test
    void sameHexExtremeRangeRejects() {
        target.setPosition(formation.getPosition());
        assertRejected(List.of(attack(0, target, ASRange.EXTREME)));
    }

    @Test
    void outOfRangeTargetRejects() {
        target.setPosition(BoardLocation.of(new Coords(5, 1), 0));
        assertRejected(List.of(attack(0, target, ASRange.LONG)));
    }

    @Test
    void unsupportedAttackTypeRejects() {
        SBFAttackAction unsupported = mock(SBFAttackAction.class);
        when(unsupported.getEntityId()).thenReturn(FORMATION_ID);
        when(unsupported.getTargetId()).thenReturn(TARGET_ID);
        assertRejected(List.of(unsupported));
    }

    private SBFFormation formation(int id, int ownerId, Coords coords, int unitCount) {
        SBFFormation result = new SBFFormation();
        result.setId(id);
        result.setOwnerId(ownerId);
        result.setPosition(BoardLocation.of(coords, 0));
        for (int i = 0; i < unitCount; i++) {
            result.addUnit(new SBFUnit());
        }
        return result;
    }

    private SBFStandardUnitAttack attack(int unitNumber, SBFFormation attackTarget, ASRange range) {
        return new SBFStandardUnitAttack(FORMATION_ID, unitNumber, attackTarget.getId(), range);
    }

    private void assertRejected(List<EntityAction> actions) {
        assertFalse(processor.processAttacks(actions, formation));
        assertTrue(game.getActionsVector().isEmpty());
        assertFalse(formation.isDone());
        verifyNoPublication();
    }

    private void verifyNoPublication() {
        verify(gameManager, never()).sendUnitUpdate(any());
        verify(gameManager, never()).sendPendingActions();
        verify(gameManager, never()).endCurrentTurn(any());
    }
}
