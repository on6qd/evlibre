package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.outbound.CustomerInformationSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandleNotifyCustomerInformationUseCaseV201Test {

    private final TenantId tenant = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CS-201");

    private RecordingSink sink;
    private HandleNotifyCustomerInformationUseCaseV201 useCase;

    @BeforeEach
    void setUp() {
        sink = new RecordingSink();
        useCase = new HandleNotifyCustomerInformationUseCaseV201(sink);
    }

    @Test
    void single_frame_with_tbc_false_fires_sink_immediately() {
        useCase.handleFrame(tenant, station, 1, 0, false, "full data");

        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).data).isEqualTo("full data");
    }

    @Test
    void multi_frame_sequence_concatenates_and_fires_once() {
        useCase.handleFrame(tenant, station, 42, 0, true, "Hello, ");
        useCase.handleFrame(tenant, station, 42, 1, true, "world. ");
        assertThat(sink.events).isEmpty();

        useCase.handleFrame(tenant, station, 42, 2, false, "Final chunk.");

        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).data).isEqualTo("Hello, world. Final chunk.");
        assertThat(sink.events.get(0).requestId).isEqualTo(42);
    }

    @Test
    void concurrent_requests_on_same_station_are_kept_separate() {
        useCase.handleFrame(tenant, station, 10, 0, true, "A-0");
        useCase.handleFrame(tenant, station, 20, 0, true, "B-0");

        useCase.handleFrame(tenant, station, 10, 1, false, "A-1");
        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).data).isEqualTo("A-0A-1");
        assertThat(sink.events.get(0).requestId).isEqualTo(10);

        useCase.handleFrame(tenant, station, 20, 1, false, "B-1");
        assertThat(sink.events).hasSize(2);
        assertThat(sink.events.get(1).data).isEqualTo("B-0B-1");
        assertThat(sink.events.get(1).requestId).isEqualTo(20);
    }

    @Test
    void empty_final_frame_fires_with_buffered_content() {
        useCase.handleFrame(tenant, station, 5, 0, true, "partial");
        useCase.handleFrame(tenant, station, 5, 1, false, "");

        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).data).isEqualTo("partial");
    }

    @Test
    void empty_single_frame_fires_empty_string_so_no_data_is_distinguishable() {
        useCase.handleFrame(tenant, station, 99, 0, false, "");

        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).data).isEmpty();
        assertThat(sink.events.get(0).requestId).isEqualTo(99);
    }

    @Test
    void second_request_after_first_completes_uses_fresh_buffer() {
        useCase.handleFrame(tenant, station, 1, 0, true, "A");
        useCase.handleFrame(tenant, station, 1, 1, false, "B");
        useCase.handleFrame(tenant, station, 2, 0, false, "C");

        assertThat(sink.events).hasSize(2);
        assertThat(sink.events.get(0).data).isEqualTo("AB");
        assertThat(sink.events.get(1).data).isEqualTo("C");
    }

    private static final class RecordingSink implements CustomerInformationSink {
        record Event(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId, String data) {}
        final List<Event> events = new ArrayList<>();

        @Override
        public void onCustomerInformation(TenantId tenantId, ChargePointIdentity stationIdentity,
                                           int requestId, String data) {
            events.add(new Event(tenantId, stationIdentity, requestId, data));
        }
    }
}
