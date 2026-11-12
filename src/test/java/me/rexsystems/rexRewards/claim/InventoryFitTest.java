package me.rexsystems.rexRewards.claim;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryFitTest {

    @Test
    void exactStackFitsInOneFreeSlot() {
        assertFalse(InventoryFit.shouldRollback(1, List.of(64)));
    }

    @Test
    void oneOverAStackNeedsASecondSlot() {
        assertTrue(InventoryFit.shouldRollback(1, List.of(65)));
        assertFalse(InventoryFit.shouldRollback(2, List.of(65)));
    }

    @Test
    void separateItemsEachTakeTheirOwnSlots() {
        assertFalse(InventoryFit.shouldRollback(2, List.of(64, 64)));
        assertTrue(InventoryFit.shouldRollback(2, List.of(64, 1, 1)));
        assertTrue(InventoryFit.shouldRollback(0, List.of(1)));
    }

    @Test
    void emptyAndNonPositiveAmountsDoNotForceARollback() {
        assertFalse(InventoryFit.shouldRollback(0, List.of()));
        assertFalse(InventoryFit.shouldRollback(0, List.of(0, -3)));
        assertFalse(InventoryFit.shouldRollback(-1, null));
    }

    @Test
    void stackLimitsBelowSixtyFourChangeTheDecision() {
        assertTrue(InventoryFit.shouldRollback(2, List.of(3), List.of(1)));
        assertFalse(InventoryFit.shouldRollback(3, List.of(3), List.of(1)));
        assertTrue(InventoryFit.shouldRollback(1, List.of(17), List.of(16)));
        assertFalse(InventoryFit.shouldRollback(2, List.of(17), List.of(16)));
    }
}
