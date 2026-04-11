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
                    <title>Error %d - evlibre</title>
                    <script src="https://cdn.tailwindcss.com"></script>
                </head>
                <body class="bg-gray-50">
                    <div class="min-h-screen flex items-center justify-center">
                        <div class="max-w-md w-full bg-white shadow-lg rounded-lg p-8">
                            <div class="text-center">
                                <h1 class="text-6xl font-bold text-red-600 mb-4">%d</h1>
                                <p class="text-xl text-gray-700 mb-6">%s</p>
                                <a href="/" class="inline-block px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                                    Go to Home
                                </a>
                            </div>
                        </div>
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
