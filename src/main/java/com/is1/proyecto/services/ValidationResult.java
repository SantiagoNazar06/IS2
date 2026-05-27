package com.is1.proyecto.services;

import java.util.List;

public class ValidationResult {

    private final boolean allowed;
    private final String reason;
    private final String message;
    private final List<MissingPrerequisite> missingPrerequisites;

    public ValidationResult(boolean allowed, String reason, String message,
                            List<MissingPrerequisite> missingPrerequisites) {
        this.allowed = allowed;
        this.reason = reason;
        this.message = message;
        this.missingPrerequisites = missingPrerequisites;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, null, null, null);
    }

    public static ValidationResult fail(String reason, String message, List<MissingPrerequisite> missing) {
        return new ValidationResult(false, reason, message, missing);
    }

    public boolean isAllowed() { return allowed; }
    public String getReason() { return reason; }
    public String getMessage() { return message; }
    public List<MissingPrerequisite> getMissingPrerequisites() { return missingPrerequisites; }

    public static class MissingPrerequisite {
        public final long id;
        public final String name;

        public MissingPrerequisite(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
