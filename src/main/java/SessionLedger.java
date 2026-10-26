import java.util.HashSet;
import java.util.Set;

public final class SessionLedger {
    private String currentRefreshToken;
    private final Set<String> revokedSessions = new HashSet<>();

    public SessionLedger(String initialRefreshToken) {
        this.currentRefreshToken = initialRefreshToken;
    }

    public boolean rotate(String presentedRefreshToken, String replacementRefreshToken) {
        if (!presentedRefreshToken.equals(currentRefreshToken) || replacementRefreshToken.isBlank()) {
            return false;
        }
        currentRefreshToken = replacementRefreshToken;
        return true;
    }

    public void revoke(String sessionId) {
        revokedSessions.add(sessionId);
    }

    public boolean mayDeliver(String sessionId, String refreshToken) {
        return !revokedSessions.contains(sessionId) && currentRefreshToken.equals(refreshToken);
    }
}
