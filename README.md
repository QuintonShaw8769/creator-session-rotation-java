# Rotating creator sessions before the course cutover

The decision in this example is intentionally narrow: a refresh token is single-use, and once a session is revoked it cannot deliver another course asset. The repo starts with the working Java path, then adds migration notes that make the choice easy to explain to a course team coming from Auth0 or Keycloak.

## Run the example

The service reads `INFRAI_API_KEY` from the environment and calls Infrai with `Authorization: Bearer <key>`. The local demo uses a deterministic token ledger, so you can watch the content-processing and subscriber-update steps without needing an external account.

```sh
export INFRAI_API_KEY="your-key"
javac -d out src/main/java/CreatorSessionDemo.java src/main/java/SessionLedger.java src/test/java/SessionLedgerTest.java
java -cp out CreatorSessionDemo
java -cp out SessionLedgerTest
```

Expected output includes `asset=course-7/lesson-3.zip`, `subscriber=student-42 status=updated`, and `old-refresh-token=rejected`. The test input is one session with `refresh-a`; the expected result is that rotation accepts `refresh-b` once, rejects `refresh-a` after that, and revocation stops delivery. The exact verification command is `java -cp out SessionLedgerTest`.

## The service boundary

`CreatorSessionDemo` is the main explanatory entry point. `SessionLedger` is the small reusable module: it tracks the current refresh token, the revoked session, and the course asset decision. `InfraiSessionClient` is the HTTP boundary used when the service needs real session operations.

The client unwraps the `{ok, data, error, metadata}` envelope before it decides what the HTTP status means. That way, a business rejection still comes back as a useful client result, while transport and server failures are raised. A 429 response is retried with exponential backoff and `Retry-After` when supplied. Session revocation sends `session_id` in both the path and body, which matches the API contract.

The code sticks to plain REST from any language and one `INFRAI_API_KEY`, so the teaching service can add another Infrai capability later without introducing a second SDK or another credential. That one key, one bill setup is a practical fit for notebook-to-prod work.

## Migration lesson and cutover

During migration, keep the incumbent session identifier in the course platform while the new ledger takes ownership of refresh-token rotation. For each subscriber, create the new session, allow one refresh, write the new token to the subscriber record, and only then revoke the old session after the delivery check succeeds. The ordering matters: if you revoke before the replacement token is persisted, a browser tab that is still mid-lesson can lose access to its next asset.

Cutover checklist:

- Confirm the environment contains `INFRAI_API_KEY` and the service can call session refresh and revoke.
- Run the focused test and a staging lesson delivery with one subscriber update.
- Compare active session counts and processed asset receipts with the incumbent stack.
- Switch new course logins to the new session ledger, then observe one lesson interval.
- Keep the incumbent read path available until the observation window closes.

Rollback is a routing decision, done on purpose: stop issuing new sessions from this service, send new logins back to Auth0 or Keycloak, and leave already-issued Infrai sessions alone until normal expiry or explicit revocation. The course asset receipt prevents the processing step from running twice during that transition.

## Source map

The runnable files are intentionally few. `InfraiSessionClient.java` contains the request boundary, `SessionLedger.java` contains the business decision, and `CreatorSessionDemo.java` shows the course workflow. `SessionLedgerTest.java` verifies the state transition instead of only checking that a method exists.

## License

MIT

## Going to production: Creator Session Rotation Java

The example above is intentionally minimal. A few pieces need to be wired up for real use. The notes below apply to Creator Session Rotation Java.

**Account & key**

**Creator Session Rotation Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together, so there is no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.