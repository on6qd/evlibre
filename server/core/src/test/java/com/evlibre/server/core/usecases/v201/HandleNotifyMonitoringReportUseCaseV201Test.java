package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.Component;
import com.evlibre.server.core.domain.v201.devicemodel.MonitorType;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedMonitoring;
import com.evlibre.server.core.domain.v201.devicemodel.Variable;
import com.evlibre.server.core.domain.v201.devicemodel.VariableMonitor;
import com.evlibre.server.core.domain.v201.ports.outbound.MonitorRepositoryPort;
import com.evlibre.server.core.domain.v201.ports.outbound.NotifyMonitoringReportCompletionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandleNotifyMonitoringReportUseCaseV201Test {

    private final TenantId tenant = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CS-201");

    private RecordingRepo repo;
    private RecordingCompletion completion;
    private HandleNotifyMonitoringReportUseCaseV201 useCase;

    @BeforeEach
    void setUp() {
        repo = new RecordingRepo();
        completion = new RecordingCompletion();
        useCase = new HandleNotifyMonitoringReportUseCaseV201(repo, completion);
    }

    @Test
    void single_frame_with_tbc_false_commits_and_signals_immediately() {
        List<ReportedMonitoring> frame = List.of(report("EVSE", "Power", monitor(1)));

        useCase.handleFrame(tenant, station, 1, 0, false, frame);

        assertThat(repo.upsertCalls).hasSize(1);
        assertThat(repo.upsertCalls.get(0)).hasSize(1);
        assertThat(repo.upsertCalls.get(0).get(0).monitors()).hasSize(1);
        assertThat(completion.events).hasSize(1);
        assertThat(completion.events.get(0).requestId).isEqualTo(1);
    }

    @Test
    void multi_frame_sequence_buffers_until_tbc_false_then_commits_atomically() {
        useCase.handleFrame(tenant, station, 42, 0, true, List.of(report("A", "x", monitor(1))));
        useCase.handleFrame(tenant, station, 42, 1, true, List.of(report("B", "y", monitor(2))));

        assertThat(repo.upsertCalls).isEmpty();
        assertThat(completion.events).isEmpty();

        useCase.handleFrame(tenant, station, 42, 2, false, List.of(report("C", "z", monitor(3))));

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
        useCase.handleFrame(tenant, station, 10, 0, true, List.of(report("A", "x", monitor(1))));
        useCase.handleFrame(tenant, station, 20, 0, true, List.of(report("B", "y", monitor(2))));

        useCase.handleFrame(tenant, station, 10, 1, false, List.of(report("A2", "x2", monitor(3))));

        assertThat(repo.upsertCalls).hasSize(1);
        List<String> vars = repo.upsertCalls.get(0).stream().map(r -> r.variable().name()).toList();
        assertThat(vars).containsExactly("x", "x2");
        assertThat(completion.events).extracting(e -> e.requestId).containsExactly(10);

        useCase.handleFrame(tenant, station, 20, 1, false, List.of(report("B2", "y2", monitor(4))));

        assertThat(repo.upsertCalls).hasSize(2);
        vars = repo.upsertCalls.get(1).stream().map(r -> r.variable().name()).toList();
        assertThat(vars).containsExactly("y", "y2");
        assertThat(completion.events).extracting(e -> e.requestId).containsExactly(10, 20);
    }

    @Test
    void empty_final_frame_after_buffered_frames_still_commits_buffered_state() {
        useCase.handleFrame(tenant, station, 5, 0, true, List.of(report("A", "x", monitor(1))));
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
    void reported_monitoring_with_zero_monitors_is_rejected_at_construction() {
        assertThatThrownBy(() -> new ReportedMonitoring(
                new Component("C", null, null), new Variable("V", null), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    private static ReportedMonitoring report(String componentName, String variableName,
                                              VariableMonitor... monitors) {
        return new ReportedMonitoring(
                new Component(componentName, null, null),
                new Variable(variableName, null),
                List.of(monitors));
    }

    private static VariableMonitor monitor(int id) {
        return new VariableMonitor(id, false, 10.0, MonitorType.UPPER_THRESHOLD, 5);
    }

    private static final class RecordingRepo implements MonitorRepositoryPort {
        final List<List<ReportedMonitoring>> upsertCalls = new ArrayList<>();

        @Override
        public void upsert(TenantId tenantId, ChargePointIdentity stationIdentity,
                           List<ReportedMonitoring> reports) {
            upsertCalls.add(List.copyOf(reports));
        }

        @Override
        public List<ReportedMonitoring> findAll(TenantId tenantId, ChargePointIdentity stationIdentity) {
            return List.of();
        }
    }

    private static final class RecordingCompletion implements NotifyMonitoringReportCompletionPort {
        record Event(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {}
        final List<Event> events = new ArrayList<>();

        @Override
        public void onMonitoringReportComplete(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {
            events.add(new Event(tenantId, stationIdentity, requestId));
        }
    }
}
