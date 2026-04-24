package com.evlibre.server.core.usecases.v201;

import com.evlibre.common.model.ChargePointIdentity;
import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.v201.ports.inbound.HandleNotifyCustomerInformationPort;
import com.evlibre.server.core.domain.v201.ports.outbound.CustomerInformationSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregates {@code NotifyCustomerInformation} frames per
 * {@code (tenantId, stationIdentity, requestId)} so the chunked {@code data}
 * strings land in the subscriber as a single concatenated payload exactly
 * once.
 *
 * <p>Unlike NotifyReport / NotifyMonitoringReport the shape here is a free
 * string per frame, so the buffer is a {@link StringBuilder} keyed per
 * request. Frame order is driven by {@code seqNo} at the station; the spec
 * guarantees frames arrive in order (it is the station's job to emit them
 * sequentially over the session), so we simply append as they come.
 */
public class HandleNotifyCustomerInformationUseCaseV201 implements HandleNotifyCustomerInformationPort {

    private static final Logger log = LoggerFactory.getLogger(HandleNotifyCustomerInformationUseCaseV201.class);

    private final CustomerInformationSink sink;

    private final Map<RequestKey, StringBuilder> buffers = new ConcurrentHashMap<>();

    public HandleNotifyCustomerInformationUseCaseV201(CustomerInformationSink sink) {
        this.sink = Objects.requireNonNull(sink);
    }

    @Override
    public void handleFrame(TenantId tenantId,
                            ChargePointIdentity stationIdentity,
                            int requestId,
                            int seqNo,
                            boolean tbc,
                            String data) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(stationIdentity);

        RequestKey key = new RequestKey(tenantId, stationIdentity, requestId);

        if (tbc) {
            buffers.compute(key, (k, existing) -> {
                StringBuilder buf = (existing != null) ? existing : new StringBuilder();
                if (data != null) buf.append(data);
                return buf;
            });
            log.debug("Buffered NotifyCustomerInformation frame from {} (requestId={}, seqNo={}, chars={})",
                    stationIdentity.value(), requestId, seqNo, data != null ? data.length() : 0);
            return;
        }

        StringBuilder buffered = buffers.remove(key);
        StringBuilder combined = (buffered != null) ? buffered : new StringBuilder();
        if (data != null) combined.append(data);

        String fullData = combined.toString();
        sink.onCustomerInformation(tenantId, stationIdentity, requestId, fullData);

        log.info("NotifyCustomerInformation complete from {} (requestId={}, totalChars={})",
                stationIdentity.value(), requestId, fullData.length());
    }

    private record RequestKey(TenantId tenantId, ChargePointIdentity stationIdentity, int requestId) {}
}
