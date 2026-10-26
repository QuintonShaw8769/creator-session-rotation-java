public final class SessionLedgerTest {
    public static void main(String[] args) {
        SessionLedger ledger = new SessionLedger("refresh-a");
        require(ledger.rotate("refresh-a", "refresh-b"), "first rotation should be accepted");
        require(!ledger.rotate("refresh-a", "refresh-c"), "old token should be rejected");
        require(ledger.mayDeliver("session-42", "refresh-b"), "current token should deliver");
        ledger.revoke("session-42");
        require(!ledger.mayDeliver("session-42", "refresh-b"), "revoked session should not deliver");
        System.out.println("SessionLedgerTest passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
