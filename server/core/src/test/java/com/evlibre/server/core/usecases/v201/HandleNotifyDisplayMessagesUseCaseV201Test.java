package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.displaymessage.MessageInfo;
import com.evlibre.server.core.domain.v201.displaymessage.MessagePriority;
import com.evlibre.server.core.domain.v201.model.MessageContent;
import com.evlibre.server.core.domain.v201.model.MessageFormat;
import com.evlibre.server.core.domain.v201.ports.outbound.DisplayMessagesSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandleNotifyDisplayMessagesUseCaseV201Test {

    private final TenantId tenant = new TenantId("demo-tenant");
    private final ChargePointIdentity station = new ChargePointIdentity("CS-201");

    private RecordingSink sink;
    private HandleNotifyDisplayMessagesUseCaseV201 useCase;

    @BeforeEach
    void setUp() {
        sink = new RecordingSink();
        useCase = new HandleNotifyDisplayMessagesUseCaseV201(sink);
    }

    @Test
    void single_frame_tbc_false_fires_sink_immediately() {
        useCase.handleFrame(tenant, station, 1, false, List.of(msg(1, "hello")));

        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).messages).hasSize(1);
        assertThat(sink.events.get(0).messages.get(0).id()).isEqualTo(1);
    }

    @Test
    void multi_frame_sequence_concatenates_and_fires_once() {
        useCase.handleFrame(tenant, station, 42, true, List.of(msg(1, "a")));
        useCase.handleFrame(tenant, station, 42, true, List.of(msg(2, "b")));
        assertThat(sink.events).isEmpty();

        useCase.handleFrame(tenant, station, 42, false, List.of(msg(3, "c")));

        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).messages).extracting(MessageInfo::id)
                .containsExactly(1, 2, 3);
        assertThat(sink.events.get(0).requestId).isEqualTo(42);
    }

    @Test
    void concurrent_requests_on_same_station_are_kept_separate() {
        useCase.handleFrame(tenant, station, 10, true, List.of(msg(1, "A")));
        useCase.handleFrame(tenant, station, 20, true, List.of(msg(10, "B")));

        useCase.handleFrame(tenant, station, 10, false, List.of(msg(2, "A2")));
        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).requestId).isEqualTo(10);
        assertThat(sink.events.get(0).messages).extracting(MessageInfo::id).containsExactly(1, 2);

        useCase.handleFrame(tenant, station, 20, false, List.of(msg(11, "B2")));
        assertThat(sink.events).hasSize(2);
        assertThat(sink.events.get(1).requestId).isEqualTo(20);
        assertThat(sink.events.get(1).messages).extracting(MessageInfo::id).containsExactly(10, 11);
    }

    @Test
    void empty_final_frame_after_buffered_content_fires_buffered_state() {
        useCase.handleFrame(tenant, station, 5, true, List.of(msg(1, "a")));
        useCase.handleFrame(tenant, station, 5, false, List.of());

        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).messages).extracting(MessageInfo::id).containsExactly(1);
    }

    @Test
    void empty_single_frame_fires_empty_list() {
        useCase.handleFrame(tenant, station, 99, false, List.of());

        assertThat(sink.events).hasSize(1);
        assertThat(sink.events.get(0).messages).isEmpty();
        assertThat(sink.events.get(0).requestId).isEqualTo(99);
    }

    private static MessageInfo msg(int id, String content) {
        return MessageInfo.of(id, MessagePriority.NORMAL_CYCLE,
                MessageContent.of(MessageFormat.UTF8, content));
    }

    private static final class RecordingSink implements DisplayMessagesSink {
        record Event(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId, List<MessageInfo> messages) {}
        final List<Event> events = new ArrayList<>();

        @Override
        public void onDisplayMessages(TenantId tenantId, ChargePointIdentity stationIdentity,
                                       int requestId, List<MessageInfo> messages) {
            events.add(new Event(tenantId, stationIdentity, requestId, messages));
        }
    }
}
