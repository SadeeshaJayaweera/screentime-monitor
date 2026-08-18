package com.screentime.restriction;

import java.time.Instant;

/**
 * Model representing a screen time extension request.
 */
public class ExtensionRequest {

    private int extensionMinutes;
    private String reason;
    private boolean approved;
    private Instant requestedAt;

    public ExtensionRequest() {
        this.requestedAt = Instant.now();
    }

    public ExtensionRequest(int extensionMinutes, String reason, boolean approved) {
        this.extensionMinutes = extensionMinutes;
        this.reason = reason;
        this.approved = approved;
        this.requestedAt = Instant.now();
    }

    public int getExtensionMinutes() {
        return extensionMinutes;
    }

    public void setExtensionMinutes(int extensionMinutes) {
        this.extensionMinutes = extensionMinutes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    @Override
    public String toString() {
        return "ExtensionRequest{" +
                "extensionMinutes=" + extensionMinutes +
                ", reason='" + reason + '\'' +
                ", approved=" + approved +
                ", requestedAt=" + requestedAt +
                '}';
    }
}
