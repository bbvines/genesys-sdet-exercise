# Genesys Cloud SDET Take-Home Exercise

Automated test suite covering UI, API, and scripting scenarios against the
[Genesys Cloud Developer Center](https://developer.genesys.cloud/).

---

## Quick Start

**No Genesys account needed. No environment setup beyond Java + Maven.**

```bash
# Clone and run all tests (headless Chrome, ~3 min)
git clone <repo-url>
cd genesys-sdet-exercise
mvn test -Dheadless=true -B --no-transfer-progress

# Run Groovy script (Scenario 3)
groovy scripts/EnvironmentConfig.groovy

# View results in Allure
mvn allure:serve
```

**Or run everything in Docker (no local Java/Maven needed):**
```bash
docker build -t genesys-sdet-exercise .
docker run --rm genesys-sdet-exercise
```

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 17 |
| Test framework | TestNG 7.9 |
| Browser automation | Selenium 4 + ChromeDriver |
| Driver management | WebDriverManager 5 (auto-downloads ChromeDriver) |
| HTTP client | Java 11 built-in `java.net.http.HttpClient` |
| JSON parsing | Jackson Databind 2.17 |
| Build tool | Maven 3.9 |
| Scripting (Scenario 3) | Groovy |
| Containerisation | Docker (multi-stage build) |
| Code coverage | JaCoCo 0.8.12 (XML + HTML reports) |
| Dependency security | OWASP Dependency-Check 12.1.3 |
| Static analysis | PMD 3.22 + SonarCloud (sonar-maven-plugin 3.11) |
| Test reporting | Allure 2.29 |

---

## Project Structure

```
genesys-sdet-exercise/
├── src/test/java/com/outbound/
│   ├── base/
│   │   └── BaseTest.java                   # WebDriver setup / teardown
│   ├── config/
│   │   ├── ConfigManager.java              # Reads system properties & env vars
│   │   └── SwaggerSpecCache.java           # Downloads & caches Swagger JSON once
│   ├── factory/
│   │   └── DriverFactory.java              # Creates Chrome / Firefox WebDriver
│   ├── listeners/
│   │   ├── AnnotationTransformer.java      # Wires RetryAnalyzer onto every test
│   │   ├── RetryAnalyzer.java              # Retries flaky tests up to N times
│   │   └── ScreenshotListener.java         # Captures screenshot on test failure
│   ├── pages/
│   │   └── ApiExplorerPage.java            # Page Object for the API Explorer SPA
│   └── tests/
│       ├── ui/
│       │   └── DeveloperCenterUITest.java  # Scenario 1 – UI tests
│       └── api/
│           └── SwaggerApiTest.java         # Scenario 2 – API tests
├── scripts/
│   └── EnvironmentConfig.groovy            # Scenario 3 – Groovy config utility
├── .github/workflows/ci.yml                # GitHub Actions CI pipeline (5 jobs)
├── owasp-suppressions.xml                  # OWASP false-positive suppressions
├── sonar-project.properties                # SonarScanner CLI config
├── pom.xml
├── testng.xml
├── Dockerfile
└── README.md
```

---

## Prerequisites (local run)

| Tool | Version |
|---|---|
| Java JDK | 17+ |
| Maven | 3.9+ |
| Google Chrome | latest stable |
| Groovy (Scenario 3 only) | 4.x |

WebDriverManager automatically downloads the ChromeDriver binary that matches
your installed Chrome version — no manual ChromeDriver setup needed.

---

## Running the Tests Locally

### Scenario 1 & 2 — UI + API tests

```bash
cd genesys-sdet-exercise
mvn test -Dheadless=true -B --no-transfer-progress
```

Expected output:
```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.outbound.tests.api.SwaggerApiTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0

Running com.outbound.tests.ui.DeveloperCenterUITest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0

Results:
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

### Scenario 3 — Groovy script

```bash
groovy scripts/EnvironmentConfig.groovy
```

Expected output:
```
[PASS] getRegion('prod_usw2'): 'prod-usw2'
[PASS] getRegion('dev'): 'dev'
...
13 tests: 13 passed, 0 failed
```

### View Allure report after any test run

```bash
mvn allure:serve
```

---

## Running in Docker

### Build the image

```bash
docker build -t genesys-sdet-exercise .
```

### Run all tests (default)

```bash
docker run --rm genesys-sdet-exercise
```

The Dockerfile uses a multi-stage build — Maven dependencies are pre-fetched
in stage 1 so rebuilds after source changes skip the slow download layer.
Chrome stable is installed in the final image.
WebDriverManager downloads the matching ChromeDriver at runtime
(requires outbound internet access from the container).

---

## CI/CD Pipeline (GitHub Actions)

Five jobs run automatically on every push/PR to `main`:

| Job | What it does |
|---|---|
| **TestNG Suite** | Runs Scenario 1 (UI) + Scenario 2 (API) with headless Chrome; then runs Scenario 3 (Groovy) |
| **OWASP Dependency-Check** | Scans all dependencies against NVD CVE database |
| **PMD** | Static analysis — detects unused vars, empty catch blocks, complex methods |
| **SonarCloud** | Full static analysis + JaCoCo coverage upload (runs after tests) |
| **Docker** | Builds the Docker image and runs the test suite inside it |

Artifacts uploaded on every run: Surefire reports, JaCoCo coverage report, screenshots (on failure), OWASP report.

---

## All Commands & Report Locations

### Run Everything Locally (Single Command)

> **Local use only** — tokens are passed as environment variables, never hardcoded.
> In CI, GitHub Actions reads these automatically from repository secrets.

```bash
mvn test pmd:check dependency-check:check sonar:sonar \
  -Dheadless=true \
  -DnvdApiKey=$NVD_API_KEY \
  -Dsonar.token=$SONAR_TOKEN \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.organization=<your-org> && \
groovy scripts/EnvironmentConfig.groovy
```

This runs all 3 scenarios + all tools in one go:
1. `mvn test` → Scenario 1 (UI) + Scenario 2 (API) + JaCoCo coverage
2. `pmd:check` → PMD static analysis
3. `dependency-check:check` → OWASP CVE scan
4. `sonar:sonar` → SonarCloud analysis
5. `groovy scripts/EnvironmentConfig.groovy` → Scenario 3 (only runs if Maven succeeds)

> **Note:** For local runs, export your tokens first:
> ```bash
> export NVD_API_KEY=your-nvd-key
> export SONAR_TOKEN=your-sonar-token
> ```

---

### In CI (GitHub Actions) — Order is Automatic

In GitHub Actions the execution order is defined in `.github/workflows/ci.yml`:

| Order | Job | What runs |
|-------|-----|-----------|
| 1st (parallel) | **TestNG Suite** | Scenario 1 + Scenario 2 + Scenario 3 + JaCoCo |
| 1st (parallel) | **OWASP** | Dependency CVE scan |
| 1st (parallel) | **PMD** | Static analysis |
| 1st (parallel) | **Docker** | Build image + run tests in container |
| 2nd (after TestNG) | **SonarCloud** | Uploads coverage + analysis |

No manual command needed — push to `main` and GitHub Actions runs everything automatically.

---

### Individual Commands

| Tool | Command | Report Location |
|------|---------|----------------|
| **TestNG (tests only)** | `mvn test -Dheadless=true` | `target/surefire-reports/` |
| **JaCoCo (coverage)** | `mvn test` (runs automatically) | `target/site/jacoco/index.html` |
| **PMD (static analysis)** | `mvn pmd:pmd` | `target/site/pmd.html` |
| **OWASP (security scan)** | `mvn dependency-check:check` | `target/dependency-check-report/dependency-check-report.html` |
| **SonarCloud** | `mvn sonar:sonar -Dsonar.token=$SONAR_TOKEN` | `https://sonarcloud.io/dashboard?id=genesys-sdet-exercise` |
| **Groovy (Scenario 3)** | `groovy scripts/EnvironmentConfig.groovy` | Printed to console |
| **Allure report** | `mvn allure:serve` | Opens in browser automatically |

**Open all local reports at once:**
```bash
open target/site/jacoco/index.html
open target/dependency-check-report/dependency-check-report.html
open target/site/pmd.html
```

---

## Security & Static Analysis

### OWASP Dependency-Check

Scans every declared dependency against the NVD CVE database. Fails the build
if any dependency carries a CVSS score ≥ 8 (HIGH or CRITICAL).

```bash
# One-off scan — produces target/dependency-check-report/dependency-check-report.html
mvn dependency-check:check

# Runs automatically as part of the full lifecycle
mvn verify
```

For CI pipelines, obtain a free NVD API key from
https://nvd.nist.gov/developers/request-an-api-key and pass it as:

```bash
mvn verify -DnvdApiKey=$NVD_API_KEY
```

False positives (CVEs that affect a code path this project never exercises)
are suppressed in `owasp-suppressions.xml` with a mandatory `<notes>` field
explaining *why* the suppression is safe.

### SonarQube / SonarCloud static analysis

Detects bugs, code smells, security hotspots, and duplication in the Java
sources and the Groovy script.

**Local SonarQube (Docker):**
```bash
# Start a local SonarQube instance
docker run -d --name sonarqube -p 9000:9000 sonarqube:community

# Run analysis (default credentials admin/admin on first run)
mvn sonar:sonar -Dsonar.token=<your-token>
```

**SonarCloud (CI-friendly, no server needed):**
```bash
mvn sonar:sonar \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.organization=<your-org> \
  -Dsonar.token=$SONAR_TOKEN
```

**How to get your `SONAR_TOKEN`:**
1. Log in at https://sonarcloud.io
2. Go to **My Account** → **Security**
3. Generate a new token → copy it

**For local runs** — pass it directly on the command line:
```bash
mvn sonar:sonar \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.organization=<your-org> \
  -Dsonar.token=<paste-token-here>
```

**For GitHub Actions CI** — store it as a secret (never hardcode tokens in code):
1. Go to your GitHub repo → **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add the following secrets:

| Secret Name | Value |
|---|---|
| `SONAR_TOKEN` | Your SonarCloud token |
| `SONAR_ORGANIZATION` | Your SonarCloud org key (e.g. `bbvines`) |
| `NVD_API_KEY` | Free key from https://nvd.nist.gov/developers/request-an-api-key |

The CI pipeline reads these automatically via `${{ secrets.SONAR_TOKEN }}`.

The `sonar-project.properties` file in the repo root covers teams that prefer
the standalone SonarScanner CLI over the Maven plugin.

> **Note on Coverage:** This is a test-only project (no `src/main/java`).
> JaCoCo measures coverage of infrastructure classes (`DriverFactory`,
> `ScreenshotListener`, `RetryAnalyzer`) which contain Firefox branches and
> exception-handling paths that are intentionally not triggered during a
> passing test run. `sonar.qualitygate.wait=false` is set so CI is not
> blocked, while full metrics remain visible on the SonarCloud dashboard.

---

## Design Decisions & Trade-offs

### Page Object Model
`ApiExplorerPage` encapsulates all locator logic so test methods read like
specifications. If the DOM changes, only the page object needs updating.

### Resilient locators
The API Explorer is a React SPA with no stable `id` or `data-testid`
attributes. Locators combine class-name substrings and XPath text matching
to survive minor DOM restructuring, at the cost of being slightly more
verbose than simple CSS selectors.

### `@BeforeClass` for the Swagger fetch
Fetching the ~20 MB Swagger spec once per class avoids hammering the
Genesys API endpoint and keeps the test suite fast. All five API test
methods share the single parsed `JsonNode`.

### API tests run before UI tests
`testng.xml` orders API tests first. They are faster and have no browser
dependency, so any spec-level failures surface immediately without
waiting for a browser to spin up.

### Groovy whitelist map
`getRegion()` uses an explicit allow-list `Map` rather than string
manipulation. This makes valid environments self-documenting and causes
any unrecognised key to fail automatically — no extra validation code needed.

### Multi-stage Dockerfile
The `deps` stage pre-fetches Maven dependencies, so iterative rebuilds
after source changes skip the slow dependency download layer.
