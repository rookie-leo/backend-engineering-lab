package com.rookie_leo.backend_engineering_lab.idempotency_uncertain_outcome.adapters.output.client.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProviderResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void readsProviderDateInDayMonthYearFormat() {
        var response = mapper.readValue(
                "{\"status\":\"SUCCESS\",\"createdAt\":\"26-09-2026\"}", PaymentProviderResponse.class);

        assertEquals("SUCCESS", response.status());
        assertEquals(LocalDate.of(2026, 9, 26), response.createdAt());
    }

    @Test
    void readsDeclineWithoutInventingProviderDate() {
        var response = mapper.readValue("{\"status\":\"DECLINED\"}", PaymentProviderResponse.class);

        assertEquals("DECLINED", response.status());
        assertNull(response.createdAt());
    }
}
