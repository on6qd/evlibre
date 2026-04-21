package com.evlibre.server.adapter.webui.handlers;

import com.evlibre.server.core.domain.shared.model.TenantId;
import com.evlibre.server.core.domain.shared.ports.outbound.TenantRepositoryPort;
import io.vertx.ext.web.RoutingContext;

import java.util.function.BiConsumer;

public class TenantContextExtractor {

    private final TenantRepositoryPort tenantRepository;

    public TenantContextExtractor(TenantRepositoryPort tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public void extractAndValidate(RoutingContext ctx, BiConsumer<RoutingContext, TenantId> handler) {
        String tenantIdStr = ctx.pathParam("tenantId");

        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            ctx.response()
                    .setStatusCode(400)
                    .end("Missing tenant ID in URL");
            return;
        }

        TenantId tenantId = new TenantId(tenantIdStr);
        if (tenantRepository.findByTenantId(tenantId).isEmpty()) {
            ctx.response()
                    .setStatusCode(403)
                    .end("Unknown tenant: " + tenantIdStr);
            return;
        }

        handler.accept(ctx, tenantId);
    }
}
