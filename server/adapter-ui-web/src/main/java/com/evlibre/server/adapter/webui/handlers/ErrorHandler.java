package com.evlibre.server.adapter.webui.handlers;

import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    public void handleError(RoutingContext ctx) {
        Throwable failure = ctx.failure();
        int statusCode = ctx.statusCode();
        if (statusCode == -1) {
            statusCode = 500;
        }

        log.error("Error handling request to {}: {}", ctx.request().path(),
                failure != null ? failure.getMessage() : "Unknown error", failure);

        String message = statusCode == 500
                ? "Internal Server Error"
                : (failure != null ? failure.getMessage() : "Error");

        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "text/html; charset=UTF-8")
                .end(buildErrorPage(statusCode, message));
    }

    public void handle404(RoutingContext ctx) {
        String path = ctx.request().path();
        log.warn("404 Not Found: {}", path);

        ctx.response()
                .setStatusCode(404)
                .putHeader("Content-Type", "text/html; charset=UTF-8")
                .end(buildErrorPage(404, "Page not found: " + path));
    }

    private String buildErrorPage(int statusCode, String message) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Error %d // evlibre</title>
                    <style>
                        body {
                            font-family: 'Courier New', monospace;
                            background: #0a0a0a;
                            color: #ff4444;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                        }
                        .error-box {
                            border: 1px solid #882222;
                            padding: 32px 48px;
                            text-align: center;
                        }
                        h1 { font-size: 48px; margin-bottom: 12px; }
                        p { color: #666; font-size: 12px; margin-bottom: 20px; }
                        a {
                            color: #00ff88;
                            border: 1px solid #333;
                            padding: 8px 20px;
                            text-decoration: none;
                            font-size: 11px;
                            text-transform: uppercase;
                            letter-spacing: 1px;
                        }
                        a:hover { border-color: #00ff88; }
                    </style>
                </head>
                <body>
                    <div class="error-box">
                        <h1>%d</h1>
                        <p>%s</p>
                        <a href="/">&lt; home</a>
                    </div>
                </body>
                </html>
                """.formatted(statusCode, statusCode, escapeHtml(message));
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
