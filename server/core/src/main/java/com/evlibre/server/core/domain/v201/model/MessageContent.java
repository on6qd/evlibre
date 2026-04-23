package com.evlibre.server.core.domain.v201.model;

import java.util.Objects;

/**
 * OCPP 2.0.1 {@code MessageContentType}. The {@code language} field is an
 * RFC-5646 tag (e.g. {@code en}, {@code nl-BE}); {@code content} is bounded
 * to 512 chars by the spec but enforcement is left to the wire schema.
 */
public record MessageContent(MessageFormat format, String language, String content) {

    public MessageContent {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(content, "content");
    }

    public static MessageContent of(MessageFormat format, String content) {
        return new MessageContent(format, null, content);
    }
}
