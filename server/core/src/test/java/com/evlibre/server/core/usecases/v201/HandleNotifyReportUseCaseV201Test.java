package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.AttributeType;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.Mutability;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedVariable;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.devicemodel.VariableAttribute;
import com.evlibre.server.core.domain.v201.ports.outbound.DeviceModelRepositoryPort;
import com.evlibre.server.core.domain.v201.ports.outbound.NotifyReportCompletionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandleNotifyReportUseCaseV201Test {

    private final TenantId tenant = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CS-201");

    private RecordingRepo repo;
    private RecordingCompletion completion;
    private HandleNotifyReportUseCaseV201 useCase;

    @BeforeEach
    void setUp() {
        repo = new RecordingRepo();
        completion = new RecordingCompletion();
        useCase = new HandleNotifyReportUseCaseV201(repo, completion);
    }

    @Test
    void single_frame_with_tbc_false_commits_and_signals_immediately() {
        List<ReportedVariable> frame = List.of(report("SecurityCtrlr", "BasicAuthPassword"));

        useCase.handleFrame(tenant, station, 1, 0, false, frame);

        assertThat(repo.upsertCalls).hasSize(1);
        assertThat(repo.upsertCalls.get(0)).hasSize(1);
        assertThat(repo.upsertCalls.get(0).get(0).variable().name()).isEqualTo("BasicAuthPassword");
        assertThat(completion.events).hasSize(1);
        assertThat(completion.events.get(0).requestId).isEqualTo(1);
    }

    @Test
    void multi_frame_sequence_buffers_until_tbc_false_then_commits_atomically() {
        useCase.handleFrame(tenant, station, 42, 0, true, List.of(report("A", "x")));
        useCase.handleFrame(tenant, station, 42, 1, true, List.of(report("B", "y")));

        // Nothing persisted or signalled yet.
        assertThat(repo.upsertCalls).isEmpty();
        assertThat(completion.events).isEmpty();

        useCase.handleFrame(tenant, station, 42, 2, false, List.of(report("C", "z")));

        // Single upsert containing all three frames' reports.
        assertThat(repo.upsertCalls).hasSize(1);
        List<String> variables = repo.upsertCalls.get(0).stream()
                .map(r -> r.variable().name())
                .toList();
        assertThat(variables).containsExactly("x", "y", "z");

        assertThat(completion.events).hasSize(1);
        assertThat(completion.events.get(0).requestId).isEqualTo(42);
    }

    @Test
    void concurrent_requests_on_same_station_are_kept_separate() {
        useCase.handleFrame(tenant, station, 10, 0, true, List.of(report("A", "x")));
        useCase.handleFrame(tenant, station, 20, 0, true, List.of(report("B", "y")));

        useCase.handleFrame(tenant, station, 10, 1, false, List.of(report("A2", "x2")));

        // Only requestId=10 was committed; requestId=20 is still buffered.
        assertThat(repo.upsertCalls).hasSize(1);
        List<String> vars = repo.upsertCalls.get(0).stream().map(r -> r.variable().name()).toList();
        assertThat(vars).containsExactly("x", "x2");
        assertThat(completion.events).extracting(e -> e.requestId).containsExactly(10);

        useCase.handleFrame(tenant, station, 20, 1, false, List.of(report("B2", "y2")));

        assertThat(repo.upsertCalls).hasSize(2);
        vars = repo.upsertCalls.get(1).stream().map(r -> r.variable().name()).toList();
        assertThat(vars).containsExactly("y", "y2");
        assertThat(completion.events).extracting(e -> e.requestId).containsExactly(10, 20);
    }

    @Test
    void empty_final_frame_after_buffered_frames_still_commits_buffered_state() {
        useCase.handleFrame(tenant, station, 5, 0, true, List.of(report("A", "x")));
        useCase.handleFrame(tenant, station, 5, 1, false, List.of());

        assertThat(repo.upsertCalls).hasSize(1);
        assertThat(repo.upsertCalls.get(0)).hasSize(1);
        assertThat(repo.upsertCalls.get(0).get(0).variable().name()).isEqualTo("x");
        assertThat(completion.events).extracting(e -> e.requestId).containsExactly(5);
    }

    @Test
    void empty_single_frame_fires_completion_without_upsert() {
        useCase.handleFrame(tenant, station, 99, 0, false, List.of());

        assertThat(repo.upsertCalls).isEmpty();
        assertThat(completion.events).extracting(e -> e.requestId).containsExactly(99);
    }

    @Test
    void second_report_after_first_completes_uses_fresh_buffer() {
        useCase.handleFrame(tenant, station, 1, 0, true, List.of(report("A", "x")));
        useCase.handleFrame(tenant, station, 1, 1, false, List.of(report("B", "y")));
        useCase.handleFrame(tenant, station, 2, 0, false, List.of(report("C", "z")));

        assertThat(repo.upsertCalls).hasSize(2);
        assertThat(repo.upsertCalls.get(0)).extracting(r -> r.variable().name()).containsExactly("x", "y");
        assertThat(repo.upsertCalls.get(1)).extracting(r -> r.variable().name()).containsExactly("z");
        assertThat(completion.events).extracting(e -> e.requestId).containsExactly(1, 2);
    }

    private static ReportedVariable report(String componentName, String variableName) {
        return new ReportedVariable(
                new Component(componentName, null, null),
                new Variable(variableName, null),
                List.of(new VariableAttribute(AttributeType.ACTUAL, "v",
                        Mutability.READ_WRITE, false, false)),
                null);
    }

    private static final class RecordingRepo implements DeviceModelRepositoryPort {
        final List<List<ReportedVariable>> upsertCalls = new ArrayList<>();

        @Override
        public void upsert(TenantId tenantId, ChargePointIdentity stationIdentity,
                           List<ReportedVariable> reports) {
            upsertCalls.add(List.copyOf(reports));
        }

        @Override
        public List<ReportedVariable> findAll(TenantId tenantId, ChargePointIdentity stationIdentity) {
            return List.of();
        }
    }

    private static final class RecordingCompletion implements NotifyReportCompletionPort {
        record Event(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {}
        final List<Event> events = new ArrayList<>();

        @Override
        public void onReportComplete(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {
            events.add(new Event(tenantId, stationIdentity, requestId));
        }
    }
}
