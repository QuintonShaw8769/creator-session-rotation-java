public final class CreatorSessionDemo {
    public static void main(String[] args) {
        SessionLedger ledger = new SessionLedger("refresh-a");
        boolean rotated = ledger.rotate("refresh-a", "refresh-b");
        boolean oldTokenRejected = !ledger.mayDeliver("session-42", "refresh-a");

        System.out.println("asset=course-7/lesson-3.zip processed=" + rotated);
        System.out.println("subscriber=student-42 status=updated");
        System.out.println("old-refresh-token=rejected " + oldTokenRejected);

        ledger.revoke("session-42");
        System.out.println("session=session-42 delivery=" + ledger.mayDeliver("session-42", "refresh-b"));
    }
}
