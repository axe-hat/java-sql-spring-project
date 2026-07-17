# SQL Webhook Challenge

A small Spring Boot application that completes a webhook-based coding challenge on
startup: it registers with a hiring API, receives a one-time webhook URL and a
bearer token, picks the SQL problem assigned to the candidate, and submits the
answer — retrying the submission if the endpoint is briefly unavailable.

There is no controller and no database. The whole thing runs once as an
`ApplicationRunner`, does its work over HTTP, logs the outcome, and exits.

## Flow

```
start
  │
  ▼
POST /generateWebhook/JAVA   { name, regNo, email }
  │        ← { webhook, accessToken }
  ▼
pick query by regNo parity   odd → question 1, even → question 2
  │
  ▼
POST <webhook>   header: Authorization: <accessToken>   body: { finalQuery }
  │   (retries with backoff on transient failure)
  ▼
log response, exit
```

The registration number decides the problem: an **odd** final two digits gets the
"highest payment not made on the 1st of the month" query, an **even** value gets
the "younger employees per department" query. The two statements live in
[`src/main/resources/queries`](src/main/resources/queries) so they can be edited
without recompiling.

## Design

```
com.hiring.sqlchallenge
  SqlChallengeApplication      launches Spring, enables retry + config binding
  config/
    ChallengeProperties        binds & validates challenge.* (fails fast if unset)
    RestClientConfig           shared RestClient.Builder
  model/
    WebhookRequest             { name, regNo, email }          (record)
    WebhookResponse            { webhook, accessToken }         (record)
    SolutionRequest            { finalQuery }                   (record)
  client/
    HiringApiClient            typed RestClient wrapper; @Retryable submit
  service/
    SqlSolutionProvider        regNo parity → query from resources
    ChallengeService           register → solve → submit
  runner/
    ChallengeRunner            fires the flow (gated by challenge.autorun)
  exception/
    ChallengeException         thrown when the flow can't complete
```

Design notes:

- **Configuration is validated at startup.** Candidate details bind from
  environment variables; if any is missing the app refuses to start rather than
  failing part way through a network call.
- **The submit step retries.** It's the flaky part of the flow, so it backs off
  and retries (`spring-retry`), and a `@Recover` turns a final failure into a
  clear `ChallengeException`.
- **No web server.** `spring.main.web-application-type=none` — this is a one-shot
  job, not a service.
- **Records + a thin client** keep the HTTP surface typed and easy to mock.

## Configuration

Everything lives under `challenge.*` in
[`application.yml`](src/main/resources/application.yml). Candidate identity is
supplied at runtime and never committed:

| property | env var | notes |
|----------|---------|-------|
| `challenge.candidate.name` | `CANDIDATE_NAME` | required |
| `challenge.candidate.reg-no` | `CANDIDATE_REG_NO` | required; last two digits pick the query |
| `challenge.candidate.email` | `CANDIDATE_EMAIL` | required, must be a valid email |
| `challenge.api.base-url` | — | hiring API base URL |
| `challenge.api.generate-path` | — | registration path |
| `challenge.retry.max-attempts` | — | submit attempts (default 4) |
| `challenge.retry.backoff-ms` | — | delay between attempts (default 1000) |
| `challenge.autorun` | — | set `false` to load the app without running |

## Build and run

Requires JDK 21.

```bash
mvn clean package
```

Run it, supplying your details as environment variables:

```bash
CANDIDATE_NAME="Your Name" \
CANDIDATE_REG_NO="YOUR_REG_NO" \
CANDIDATE_EMAIL="you@example.com" \
java -jar target/sql-webhook-challenge-1.0.0.jar
```

To boot the application without making any live calls (useful for a smoke test):

```bash
java -jar target/sql-webhook-challenge-1.0.0.jar --challenge.autorun=false
```

## Tests

```bash
mvn test
```

The suite covers:

- **`SqlSolutionProviderTest`** — trailing-digit extraction (including junk input)
  and that odd/even registration numbers select the right query.
- **`ChallengeServiceTest`** — the orchestration wires registration → query
  selection → submit correctly (mocked client).
- **`HiringApiClientTest`** — request/response mapping against a
  `MockRestServiceServer`, including the `Authorization` header and JSON body.
- **`SqlChallengeApplicationTests`** — the context wires up with autorun disabled.

## Notes

- The queries target the schema described by the challenge (`EMPLOYEE`,
  `DEPARTMENT`, `PAYMENTS`); they are submitted as text and not executed locally.
- The webhook URL and token are issued per-run by the API, so there is nothing
  secret to store.
