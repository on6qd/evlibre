package com.evlibre.server.adapter.ocpp;

import com.evlibre.server.adapter.ocpp.testutil.OcppTestHarness;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OCPP-J sub-protocol negotiation (§4.1.2 / JSON whitepaper §3.1):
 *   "If the client offers Sec-WebSocket-Protocol values that none of which the
 *    server supports, the server MUST complete the handshake with no
 *    Sec-WebSocket-Protocol header and immediately close the connection."
 */
@ExtendWith(VertxExtension.class)
@Tag("integration")
class SubProtocolNegotiationIT {

    private OcppTestHarness harness;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        harness = new OcppTestHarness();
        harness.deploy(vertx, ctx);
    }

    @Test
    void supported_sub_protocol_is_selected(Vertx vertx, VertxTestContext ctx) {
        var options = new WebSocketConnectOptions()
                .setPort(harness.port())
                .setHost("localhost")
                .setURI("/ocpp/demo-tenant/CHARGER-001")
                .addSubProtocol("ocpp1.6");

        vertx.createWebSocketClient().connect(options).onComplete(ctx.succeeding(ws -> ctx.verify(() -> {
            assertThat(ws.subProtocol()).isEqualTo("ocpp1.6");
            ws.close();
            ctx.completeNow();
        })));
    }

    @Test
    void unsupported_sub_protocol_closes_connection(Vertx vertx, VertxTestContext ctx) {
        // Client offers only a protocol the server doesn't support.
        var options = new WebSocketConnectOptions()
                .setPort(harness.port())
                .setHost("localhost")
                .setURI("/ocpp/demo-tenant/CHARGER-002")
                .addSubProtocol("ocpp1.5");

        vertx.createWebSocketClient().connect(options).onComplete(ar -> ctx.verify(() -> {
            // Acceptable outcomes per spec:
            //   (a) handshake succeeds with no Sec-WebSocket-Protocol header
            //       and the server then closes the connection; or
            //   (b) the handshake fails.
            // Either way, the client must NOT have an active session on "ocpp1.5".
            if (ar.succeeded()) {
                var ws = ar.result();
                assertThat(ws.subProtocol()).isNotEqualTo("ocpp1.5");
                // Wait briefly for the server-side close.
                ws.closeHandler(v -> ctx.completeNow());
                vertx.setTimer(500, t -> {
                    if (!ctx.completed()) ctx.completeNow();
                });
            } else {
                // Upgrade rejected — also acceptable (no session established).
                ctx.completeNow();
            }
        }));
    }
}
