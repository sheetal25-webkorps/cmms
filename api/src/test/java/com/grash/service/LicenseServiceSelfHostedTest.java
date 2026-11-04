package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.license.LicensingState;
import com.grash.repository.KeygenRequestTrackerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LicenseServiceSelfHostedTest {

    private final KeygenRequestTrackerRepository trackerRepository = mock(KeygenRequestTrackerRepository.class);
    private final LicenseService licenseService = new LicenseService(new ObjectMapper(), trackerRepository);

    @Test
    void withoutCommercialKey_selfHostedDeploymentHasEveryEntitlement() {
        LicensingState state = licenseService.getLicensingState();

        assertTrue(state.isValid());
        assertFalse(state.isHasLicense());
        Arrays.stream(LicenseEntitlement.values())
                .forEach(entitlement -> assertTrue(licenseService.hasEntitlement(entitlement), entitlement.name()));
        // No call to the external license server is made
        verifyNoInteractions(trackerRepository);
    }

    @Test
    void withCommercialKey_keyIsStillValidated() {
        ReflectionTestUtils.setField(licenseService, "licenseKey", "COMMERCIAL-KEY");
        ReflectionTestUtils.setField(licenseService, "keygenAccountId", "account");
        // Daily request budget exhausted: validation is attempted (not bypassed) and reports invalid.
        com.grash.model.KeygenRequestTracker exhausted = new com.grash.model.KeygenRequestTracker();
        exhausted.setRequestCount(1000);
        when(trackerRepository.findByRequestDate(any())).thenReturn(java.util.Optional.of(exhausted));

        LicensingState state = licenseService.getLicensingState();

        assertTrue(state.isHasLicense());
        assertFalse(state.isValid());
    }
}
