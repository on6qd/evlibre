package com.evlibre.server.adapter.persistence.inmemory;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Direction;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.FrameType;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.Lifecycle;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.LifecycleKind;
import com.evlibre.server.core.domain.shared.model.MessageTraceEntry.OcppFrame;
import com.evlibre.server.core.domain.shared.model.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryMessageTraceStoreTest {

    private static final TenantId TENANT_A = new TenantId("tenant-a");
    private static final TenantId TENANT_B = new TenantId("tenant-b");
    private static final ChargePointIdentity STATION_1 = new ChargePointIdentity("CP1");
    private static final ChargePointIdentity STATION_2 = new ChargePointIdentity("CP2");

    @Test
    void records_andReturnsInInsertionOrder() {
        InMemoryMessageTraceStore store = new InMemoryMessageTraceStore(10);

        store.record(TENANT_A, STATION_1, frame("BootNotification", "1"));
        store.record(TENANT_A, STATION_1, frame("StatusNotification", "2"));
        store.record(TENANT_A, STATION_1, lifecycle(LifecycleKind.DISCONNECTED, "1006"));

        List<MessageTraceEntry> recent = store.recent(TENANT_A, STATION_1);
        assertThat(recent).hasSize(3);
        assertThat(((OcppFrame) recent.get(0)).action()).isEqualTo("BootNotification");
        assertThat(((OcppFrame) recent.get(1)).action()).isEqualTo("StatusNotification");
        assertThat(((Lifecycle) recent.get(2)).kind()).isEqualTo(LifecycleKind.DISCONNECTED);
    }

    @Test
    void evictsOldestWhenAtCapacity() {
        InMemoryMessageTraceStore store = new InMemoryMessageTraceStore(3);

        store.record(TENANT_A, STATION_1, frame("A", "1"));
        store.record(TENANT_A, STATION_1, frame("B", "2"));
        store.record(TENANT_A, STATION_1, frame("C", "3"));
        store.record(TENANT_A, STATION_1, frame("D", "4"));

        List<MessageTraceEntry> recent = store.recent(TENANT_A, STATION_1);
        assertThat(recent).hasSize(3);
        assertThat(recent).extracting(e -> ((OcppFrame) e).action())
                .containsExactly("B", "C", "D");
    }

    @Test
    void isolatesTenants() {
        InMemoryMessageTraceStore store = new InMemoryMessageTraceStore();

        store.record(TENANT_A, STATION_1, frame("ForA", "1"));
        store.record(TENANT_B, STATION_1, frame("ForB", "2"));

        assertThat(store.recent(TENANT_A, STATION_1)).hasSize(1);
        assertThat(((OcppFrame) store.recent(TENANT_A, STATION_1).get(0)).action()).isEqualTo("ForA");
        assertThat(store.recent(TENANT_B, STATION_1)).hasSize(1);
        assertThat(((OcppFrame) store.recent(TENANT_B, STATION_1).get(0)).action()).isEqualTo("ForB");
    }

    @Test
    void isolatesStationsWithinSameTenant() {
        InMemoryMessageTraceStore store = new InMemoryMessageTraceStore();

        store.record(TENANT_A, STATION_1, frame("ForCP1", "1"));
        store.record(TENANT_A, STATION_2, frame("ForCP2", "2"));

        assertThat(store.recent(TENANT_A, STATION_1)).hasSize(1);
        assertThat(store.recent(TENANT_A, STATION_2)).hasSize(1);
    }

    @Test
    void recent_returnsEmptyForUnknownStation() {
        InMemoryMessageTraceStore store = new InMemoryMessageTraceStore();
        assertThat(store.recent(TENANT_A, STATION_1)).isEmpty();
    }

    @Test
    void recent_returnsImmutableSnapshot() {
        InMemoryMessageTraceStore store = new InMemoryMessageTraceStore();
        store.record(TENANT_A, STATION_1, frame("A", "1"));

        List<MessageTraceEntry> snapshot = store.recent(TENANT_A, STATION_1);
        store.record(TENANT_A, STATION_1, frame("B", "2"));

        assertThat(snapshot).hasSize(1);
        assertThat(store.recent(TENANT_A, STATION_1)).hasSize(2);
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new InMemoryMessageTraceStore(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InMemoryMessageTraceStore(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static OcppFrame frame(String action, String messageId) {
        return new OcppFrame(Instant.now(), Direction.IN, FrameType.CALL,
                action, messageId, "{}");
    }

    private static Lifecycle lifecycle(LifecycleKind kind, String detail) {
        return new Lifecycle(Instant.now(), kind, detail);
    }
}
