package com.nbfc.stresssimulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.shared.LocalServiceClient;
import com.nbfc.shared.PortfolioData.Snapshot;
import com.nbfc.shared.StressData.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ScenarioServiceTest {

    private static final String USER_ID =
            "00000000-0000-0000-0000-000000000042";

    private static final String ACCESS_DATA_URL =
            "http://127.0.0.1:1";

    @Test
    void missingInputsPersistFailedRunAndIdempotencyReturnsThatRun() {
        var repository = mock(Scenarios.class);

        Map<String, ScenarioRecord> stored = new HashMap<>();

        when(repository.findByBankIdAndCreatedByAndRequestKey(
                anyString(),
                anyString(),
                anyString()
        )).thenAnswer(invocation ->
                Optional.ofNullable(
                        stored.get(invocation.getArgument(2))
                )
        );

        when(repository.saveAndFlush(any()))
                .thenAnswer(invocation -> {
                    ScenarioRecord record = invocation.getArgument(0);
                    stored.put(record.requestKey, record);
                    return record;
                });

        var service = new ScenarioService(
                repository,
                new ObjectMapper().findAndRegisterModules(),
                ACCESS_DATA_URL
        );

        var request = new Request(
                null,
                "SYN-NBFC-001",
                null,
                null,
                null,
                "60",
                "30",
                "Synthetic test",
                "request-1"
        );

        var first = service.execute(request, USER_ID);

        assertThat(first.status()).isEqualTo("FAILED");
        assertThat(first.result()).isNull();
        assertThat(first.failureReason()).contains("Select saved");

        var retry = service.execute(request, USER_ID);

        assertThat(retry.id()).isEqualTo(first.id());

        verify(repository, times(1)).saveAndFlush(any());

        var changedRequest = new Request(
                null,
                "SYN-NBFC-001",
                null,
                null,
                null,
                "70",
                "30",
                "Synthetic test",
                "request-1"
        );

        assertThatThrownBy(
                () -> service.execute(changedRequest, USER_ID)
        ).isInstanceOfSatisfying(
                ResponseStatusException.class,
                exception -> assertThat(
                        exception.getStatusCode().value()
                ).isEqualTo(409)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void accessFailureDoesNotSaveFailedScenario(int status) {
        var repository = mock(Scenarios.class);

        when(repository.findByBankIdAndCreatedByAndRequestKey(
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(Optional.empty());

        String id = "00000000-0000-0000-0000-000000000001";

        var service = new ScenarioService(
                repository,
                new ObjectMapper().findAndRegisterModules(),
                ACCESS_DATA_URL
        );

        var request = new Request(
                id,
                "SYN-NBFC-001",
                id,
                id,
                id,
                "60",
                "30",
                "Synthetic authentication test",
                "auth-" + status
        );

        var denied = new ResponseStatusException(
                HttpStatus.valueOf(status),
                "Access denied."
        );

        try (var client = mockStatic(LocalServiceClient.class)) {
            client.when(() -> LocalServiceClient.get(
                    ACCESS_DATA_URL,
                    "/api/portfolios/" + id,
                    Snapshot.class
            )).thenThrow(denied);

            assertThatThrownBy(
                    () -> service.execute(request, USER_ID)
            ).isSameAs(denied);

            verify(repository, never()).saveAndFlush(any());
        }
    }
}