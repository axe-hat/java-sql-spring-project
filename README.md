# SQL Webhook Challenge

[![CI](https://github.com/axe-hat/java-sql-spring-project/actions/workflows/ci.yml/badge.svg)](https://github.com/axe-hat/java-sql-spring-project/actions/workflows/ci.yml)

A small Spring Boot application that completes a webhook-based coding challenge on
startup: it registers with a hiring API, receives a one-time webhook URL and a
bearer token, picks the SQL problem assigned to the candidate, and submits the
answer — retrying either call if the API is briefly unavailable.

There is no controller and no database. The whole thing runs once as an
`ApplicationRunner`, does its work over HTTP, logs the outcome, and exits.

## Flow

```
start
  │
  ▼
POST /generateWebhook/JAVA   { name, regNo, email }
  │        ← { webhook, accessToken }        (retried on 5xx / network error)
  ▼
pick query by regNo parity   odd → question 1, even → question 2
  │
  ▼
POST <webhook>   header: Authorization: <accessToken>   body: { finalQuery }
  │   (retried on 5xx / network error)
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
    RestClientConfig           shared RestClient.Builder with connect/read timeouts
  model/
    WebhookRequest             { name, regNo, email }          (record)
    WebhookResponse            { webhook, accessToken }         (record)
    SolutionRequest            { finalQuery }                   (record)
  client/
    HiringApiClient            typed RestClient wrapper; @Retryable register + submit
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
  environment variables; if any is missing, the email is malformed or the
  registration number has no digit, the app refuses to start rather than failing
  part way through a network call.
- **Only transient failures are retried.** Both calls back off and retry
  (`spring-retry`) on a 5xx response or a network / timeout error. A 4xx (bad
  request, rejected token) can't succeed on a second try, so it fails at once. A
  `@Recover` turns the final failure into a `ChallengeException` that names the
  HTTP status, and the process exits non-zero.
- **Every call has a timeout.** Connect and read timeouts
  (`challenge.http.*`) stop a hung endpoint from stalling the run.
- **The SQL is tested for real.** Both queries run in the test suite against the
  challenge schema in H2 (MySQL mode) with a fixture built around each question's
  edge cases.
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
| `challenge.retry.max-attempts` | — | attempts per call, 5xx / network errors only (default 4) |
| `challenge.retry.backoff-ms` | — | delay between attempts (default 1000) |
| `challenge.http.connect-timeout` | — | connect timeout (default `10s`) |
| `challenge.http.read-timeout` | — | read timeout (default `30s`) |
| `challenge.autorun` | — | set `false` to load the app without running |

## Build and run

Requires JDK 21. Maven is not needed: the included wrapper downloads the pinned
version on first use.

```bash
./mvnw clean package          # Windows: mvnw.cmd clean package
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
./mvnw verify
```

CI runs the same command on every push and pull request
([`.github/workflows/ci.yml`](.github/workflows/ci.yml)); Dependabot keeps the
Maven dependencies and GitHub Actions current.

The suite covers:

- **`SqlQueriesExecutionTest`** — runs both submitted queries against the challenge
  schema in H2 (MySQL mode): payments on the 1st are excluded (including one at
  00:00:01), colleagues sharing a birthday are not counted as younger, employees
  with no younger colleague still appear with 0, and the column order is exact.
- **`SqlSolutionProviderTest`** — trailing-digit extraction (including junk input)
  and that odd/even registration numbers select the right query.
- **`HiringApiClientRetryTest`** — through the real retry proxy: 5xx and network
  errors are retried then succeed, 4xx is not retried, retries stop at the limit,
  an incomplete registration fails at once.
- **`HiringApiClientTest`** — request/response mapping against a
  `MockRestServiceServer`, including the `Authorization` header and JSON body.
- **`ChallengePropertiesValidationTest`** — startup is refused for a missing or
  invalid email, a registration number without digits, or zero retry attempts.
- **`ChallengeServiceTest`** / **`ChallengeRunnerTest`** — orchestration and the
  runner's failure propagation (mocked collaborators).
- **`SqlChallengeApplicationTests`** — the context wires up with autorun disabled.

## Notes

- The queries target the schema described by the challenge (`EMPLOYEE`,
  `DEPARTMENT`, `PAYMENTS`) and use MySQL functions (`TIMESTAMPDIFF`, `CURDATE`).
  They are submitted as text; the tests execute them in H2's MySQL mode.
- The webhook URL and token are issued per-run by the API, so there is nothing
  secret to store.
