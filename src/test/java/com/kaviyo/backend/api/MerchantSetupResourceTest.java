package com.kaviyo.backend.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantSetupResourceTest {
    @Test
    void acceptsCompleteProfile() {
        assertNull(MerchantSetupResource.validationError(
                "Trail Co.", "DTC", "https://trail.example", "We sell daypacks.",
                "https://trail.example/returns", "Help shoppers find our packs", "weekly"));
    }

    @Test
    void rejectsBlankBusinessButAllowsMissingLegacyGoal() {
        assertEquals("Business name is required.", MerchantSetupResource.validationError(
                "  ", null, null, null, null, "A goal", "weekly"));
        assertNull(MerchantSetupResource.validationError(
                "Trail Co.", null, null, null, null, null, null));
    }

    @Test
    void validatesStructuredGoals() {
        var valid = java.util.List.of(
                new MerchantSetupResource.GoalInput("MENTION_RATE", new java.math.BigDecimal("60"), "weekly"),
                new MerchantSetupResource.GoalInput("FINDINGS_FIXED", new java.math.BigDecimal("5"), "monthly"));
        assertNull(MerchantSetupResource.validateGoals(valid));
        assertEquals("Set at least one goal.", MerchantSetupResource.validateGoals(java.util.List.of()));
        assertTrue(MerchantSetupResource.validateGoals(java.util.List.of(
                new MerchantSetupResource.GoalInput("NOPE", new java.math.BigDecimal("1"), "weekly"))
        ).contains("known metric"));
        assertTrue(MerchantSetupResource.validateGoals(java.util.List.of(
                new MerchantSetupResource.GoalInput("MENTION_RATE", new java.math.BigDecimal("101"), "weekly"))
        ).contains("between 1 and 100"));
        assertTrue(MerchantSetupResource.validateGoals(java.util.List.of(
                new MerchantSetupResource.GoalInput("FINDINGS_FIXED", new java.math.BigDecimal("2.5"), "weekly"))
        ).contains("whole number"));
        assertEquals("Set each metric once per period.", MerchantSetupResource.validateGoals(java.util.List.of(
                new MerchantSetupResource.GoalInput("MENTION_RATE", new java.math.BigDecimal("60"), "weekly"),
                new MerchantSetupResource.GoalInput("MENTION_RATE", new java.math.BigDecimal("80"), "weekly"))));
        assertTrue(MerchantSetupResource.validateGoals(java.util.List.of(
                new MerchantSetupResource.GoalInput(null, new java.math.BigDecimal("60"), "weekly"))
        ).contains("known metric"));
    }

    @Test
    void rejectsBadCadenceAndOversizeFields() {
        assertEquals("Cadence must be daily, weekly, or monthly.", MerchantSetupResource.validationError(
                "Trail Co.", null, null, null, null, "A goal", "yearly"));
        assertTrue(MerchantSetupResource.validationError(
                "x".repeat(101), null, null, null, null, "A goal", "weekly").contains("100"));
        assertTrue(MerchantSetupResource.validationError(
                "Trail Co.", "y".repeat(41), null, null, null, "A goal", "weekly").contains("40"));
        assertTrue(MerchantSetupResource.validationError(
                "Trail Co.", null, null, null, null, "z".repeat(601), "weekly").contains("600"));
    }
}
