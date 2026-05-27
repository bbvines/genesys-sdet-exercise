# ---------------------------------------------------------------------------
# Stage 1: dependency cache
# Pre-download all Maven dependencies so the test image doesn't need to
# re-fetch them on every rebuild — only changes to pom.xml bust this layer.
#
# NOTE: --platform linux/amd64 is required because Google Chrome ships
# amd64-only binaries. On Apple Silicon (arm64) Macs, Docker uses QEMU
# emulation for this image — expect a slower build locally, but CI on
# GitHub Actions (ubuntu-latest = x86_64) runs natively at full speed.
# ---------------------------------------------------------------------------
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS deps

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# ---------------------------------------------------------------------------
# Stage 2: final image
# Installs Google Chrome, copies source, runs TestNG suite.
# ---------------------------------------------------------------------------
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17

# Install Google Chrome stable via direct .deb download.
# wget is already present in the base image; this single layer also pulls
# in all Chrome runtime dependencies via apt's dependency resolver.
RUN apt-get update -qq && \
    wget -q -O /tmp/google-chrome.deb \
        https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt-get install -y -qq --no-install-recommends /tmp/google-chrome.deb && \
    rm /tmp/google-chrome.deb && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Reuse the pre-fetched local Maven repo from the deps stage
COPY --from=deps /root/.m2 /root/.m2

# Copy project files
COPY pom.xml .
COPY testng.xml .
COPY owasp-suppressions.xml .
COPY src ./src
COPY scripts ./scripts

# MAVEN_OPTS: extra heap for the ~20 MB Swagger spec parse
ENV MAVEN_OPTS="-Xmx512m"

# Install Groovy for Scenario 3
RUN apt-get update -qq && \
    apt-get install -y -qq --no-install-recommends groovy && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# WebDriverManager detects the installed google-chrome version and downloads
# the matching ChromeDriver automatically (requires outbound internet access).
# Runs Scenario 1 (UI) + Scenario 2 (API) via Maven, then Scenario 3 (Groovy).
CMD mvn test -B --no-transfer-progress -Dheadless=true -Dsurefire.useFile=false && \
    groovy scripts/EnvironmentConfig.groovy
