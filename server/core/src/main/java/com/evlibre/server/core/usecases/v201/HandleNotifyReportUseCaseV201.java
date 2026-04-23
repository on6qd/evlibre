package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.devicemodel.ReportedVariable;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyReportPort;
import com.evlibre.server.core.domain.v201.ports.outbound.DeviceModelRepositoryPort;
import com.evlibre.server.core.domain.v201.ports.outbound.NotifyReportCompletionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates NotifyReport frames per {@code (tenantId, stationIdentity, requestId)}
 * so multi-frame reports land in the repository atomically and a completion event
 * fires exactly once.
 *
 * <p>A frame with {@code tbc=false} is the last one: accumulated reports (including
 * this frame's) are upserted, the completion port is notified, and per-request
 * state is cleared. A frame with {@code tbc=true} is buffered.
 *
 * <p>Frames with an empty {@code reports} list are permitted mid-sequence and are
 * tracked (they count toward seqNo progression) but contribute no reports.
 */
public class HandleNotifyReportUseCaseV201 implements HandleNotifyReportPort {

    private static final Logger log = LoggerFactory.getLogger(HandleNotifyReportUseCaseV201.class);

    private final DeviceModelRepositoryPort deviceModelRepository;
    private final NotifyReportCompletionPort completionPort;

    private final Map<RequestKey, List<ReportedVariable>> buffers = new ConcurrentHashMap<>();

    public HandleNotifyReportUseCaseV201(DeviceModelRepositoryPort deviceModelRepository,
                                         NotifyReportCompletionPort completionPort) {
        this.deviceModelRepository = Objects.requireNonNull(deviceModelRepository);
        this.completionPort = Objects.requireNonNull(completionPort);
    }

    @Override
    public void handleFrame(TenantId tenantId,
                            ChargePointIdentity stationIdentity,
                            int requestId,
                            int seqNo,
                            boolean tbc,
                            List<ReportedVariable> reports) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);
        Objects.requireNonNull(reports);

        RequestKey key = new RequestKey(tenantId, stationIdentity, requestId);

        if (tbc) {
            buffers.merge(key, new ArrayList<>(reports), (existing, incoming) -> {
                existing.addAll(incoming);
                return existing;
            });
            log.debug("Buffered NotifyReport frame from {} (requestId={}, seqNo={}, reports={})",
                    stationIdentity.value(), requestId, seqNo, reports.size());
            return;
        }

        // Final frame: combine buffered frames with this one, upsert atomically, signal completion.
        List<ReportedVariable> buffered = buffers.remove(key);
        List<ReportedVariable> combined = (buffered != null)
                ? buffered
                : new ArrayList<>(reports.size());
        combined.addAll(reports);

        if (!combined.isEmpty()) {
            deviceModelRepository.upsert(tenantId, stationIdentity, combined);
        }
        completionPort.onReportComplete(tenantId, stationIdentity, requestId);

        log.info("NotifyReport complete from {} (requestId={}, frames totalled, reports={})",
                stationIdentity.value(), requestId, combined.size());
    }

    private record RequestKey(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {}
}
