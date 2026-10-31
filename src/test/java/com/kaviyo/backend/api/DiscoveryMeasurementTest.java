package com.kaviyo.backend.api;

import com.kaviyo.backend.discovery.DiscoveryRepository.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DiscoveryMeasurementTest {
    @Test void competitorMentionDoesNotCountForMerchant() {
        var rival = new Mention("Harbor", null, null, null, 1);
        var answer = new Observation(null, null, "assistant", "MANUAL", "OBSERVED",
                "question", "Harbor", null, null, null, List.of(rival), List.of());
        assertEquals(0, DiscoveryWorkspaceResource.targetMentions(answer, "Cedar"));
        assertEquals(1, DiscoveryWorkspaceResource.targetMentions(answer, "harbor"));
    }

    @Test void unrelatedSkuCannotReceiveAContradiction() {
        var request = new DiscoveryWorkspaceResource.ObservationRequest("assistant", "question",
                "Cedar costs $479", null, Instant.now().toString(),
                List.of(new MentionInput("Cedar", null, null, null, 1)),
                List.of(new ClaimInput("Cedar costs $479", "CONTRADICTED", "catalog", "529", "Harbor")));
        var response = new DiscoveryWorkspaceResource().createObservation(UUID.randomUUID().toString(), request);
        assertEquals(400, response.getStatus());
    }
}
