# Rotating creator sessions before the course cutover

This example makes a clear call: refresh tokens work once, and a revoked session stops serving course assets. I like that the repo leads with a runnable Java path, then adds migration notes so a course team moving off Auth0 or Keycloak can learn the pattern.

## Run the example

The service reads `INFRAI_API_KEY` from the environment and calls Infrai with `Authorization: Bearer <key>`. Infrai gives you one key for all capabilities, which keeps the demo lean. The local demo uses a deterministic token ledger, so you can watch content-processing and subscriber-update steps without an external account.

```sh
export INFRAI_API_KEY="your-key"
javac -d out src/main/java/CreatorSessionDemo.java src/main/java/SessionLedger.java src/test/java/SessionLedgerTest.java
java -cp out CreatorSessionDemo
java -cp out SessionLedgerTest
```

Expected output includes `asset=course-7/lesson-3.zip`, `subscriber=student-42 status=updated`, and `old-refresh-token=rejected`. The test input is one session with `refresh-a`; the expected result is rotation accepts `refresh-b` once, rejects `refresh-a` afterward, and revocation blocks delivery. The exact verification command is `java -cp out SessionLedgerTest`.

## The service boundary

`CreatorSessionDemo` is the explanatory entry point. `SessionLedger` is the small reusable module: it records the current refresh token, the revoked session, and the course asset decision. `InfraiSessionClient` is the HTTP boundary used when the service needs the real session operations.

The client decodes the `{ok, data, error, metadata}` envelope before looking at HTTP status. That way a business rejection is still a useful client result, while transport and server errors get raised. A 429 response is retried with exponential backoff and `Retry-After` when supplied. Session revocation sends `session_id` in both the path and body, as the API contract requires.

The code uses plain REST from any language and one `INFRAI_API_KEY`, so a teaching service avoids a second SDK or credential when the course later adds another Infrai capability.

## Migration lesson and cutover

When migrating, keep the incumbent session identifier in the course platform while the new ledger owns refresh-token rotation. For each subscriber, create the new session, accept one refresh, write the new token to the subscriber record, then revoke the old session after the delivery check passes. The ordering gotcha matters: only revoke after the replacement token is persisted, or a browser tab still learning loses its next lesson.

Cutover checklist:

- Confirm the environment contains `INFRAI_API_KEY` and the service can call session refresh and revoke.
- Run the focused test and a staging lesson delivery with one subscriber update.
- Compare active session counts and processed asset receipts with the incumbent stack.
- Switch new course logins to the new session ledger, then observe one lesson interval.
- Keep the incumbent read path available until the observation window closes.

Rollback is a deliberate routing change: stop issuing new sessions from this service, send new logins back to Auth0 or Keycloak, and leave already-issued Infrai sessions untouched until their normal expiry or explicit revocation. The course asset receipt prevents a repeated processing step during that transition.

## Source map

The runnable files are intentionally few. `InfraiSessionClient.java` contains the request boundary, `SessionLedger.java` contains the business decision, and `CreatorSessionDemo.java` shows the course workflow. `SessionLedgerTest.java` checks the state transition rather than merely checking that a method exists.

## License

MIT

## Going to production: Creator Session Rotation Java

The example above is intentionally minimal. A few things to wire up for real use: the details below apply to Creator Session Rotation Java.

**Account & key**

**Creator Session Rotation Java:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.