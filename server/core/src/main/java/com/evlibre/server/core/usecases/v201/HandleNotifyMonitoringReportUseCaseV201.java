package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedMonitoring;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyMonitoringReportPort;
import com.evlibre.server.core.domain.v201.ports.outbound.MonitorRepositoryPort;
import com.evlibre.server.core.domain.v201.ports.outbound.NotifyMonitoringReportCompletionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates {@code NotifyMonitoringReport} frames per
 * {@code (tenantId, stationIdentity, requestId)} so multi-frame reports land
 * in the repository atomically and a completion event fires exactly once.
 *
 * <p>A frame with {@code tbc=false} is the last one: accumulated reports
 * (including this frame's) are upserted, the completion port is notified,
 * and per-request state is cleared. A frame with {@code tbc=true} is
 * buffered.
 *
 * <p>Frames with an empty {@code reports} list are permitted mid-sequence
 * and are tracked (they count toward seqNo progression) but contribute no
 * reports. Mirrors {@code HandleNotifyReportUseCaseV201}.
 */
public class HandleNotifyMonitoringReportUseCaseV201 implements HandleNotifyMonitoringReportPort {

    private static final Logger log = LoggerFactory.getLogger(HandleNotifyMonitoringReportUseCaseV201.class);

    private final MonitorRepositoryPort monitorRepository;
    private final NotifyMonitoringReportCompletionPort completionPort;

    private final Map<RequestKey, List<ReportedMonitoring>> buffers = new ConcurrentHashMap<>();

    public HandleNotifyMonitoringReportUseCaseV201(MonitorRepositoryPort monitorRepository,
                                                    NotifyMonitoringReportCompletionPort completionPort) {
        this.monitorRepository = Objects.requireNonNull(monitorRepository);
        this.completionPort = Objects.requireNonNull(completionPort);
    }

    @Override
    public void handleFrame(TenantId tenantId,
                            ChargePointIdentity stationIdentity,
                            int requestId,
                            int seqNo,
                            boolean tbc,
                            List<ReportedMonitoring> reports) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(reports);

        RequestKey key = new RequestKey(tenantId, stationIdentity, requestId);

        if (tbc) {
            buffers.merge(key, new ArrayList<>(reports), (existing, incoming) -> {
                existing.addAll(incoming);
                return existing;
            });
            log.debug("Buffered NotifyMonitoringReport frame from {} (requestId={}, seqNo={}, reports={})",
                    stationIdentity.value(), requestId, seqNo, reports.size());
            return;
        }

        List<ReportedMonitoring> buffered = buffers.remove(key);
        List<ReportedMonitoring> combined = (buffered != null)
                ? buffered
                : new ArrayList<>(reports.size());
        combined.addAll(reports);

        if (!combined.isEmpty()) {
            monitorRepository.upsert(tenantId, stationIdentity, combined);
        }
        completionPort.onMonitoringReportComplete(tenantId, stationIdentity, requestId);

        log.info("NotifyMonitoringReport complete from {} (requestId={}, reports={})",
                stationIdentity.value(), requestId, combined.size());
    }

    private record RequestKey(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {}
}
