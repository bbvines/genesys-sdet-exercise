/**
 * EnvironmentConfig.groovy — Scenario 3: Environment Configuration Utility
 *
 * Approach:
 *   getRegion     — closure backed by an immutable whitelist Map. Explicit allowlist means
 *                   unknown environments fail fast with a clear error rather than silently
 *                   returning a wrong value.
 *
 *   getTestGroups — checks whether the environment starts with "prod" to cover all prod
 *                   variants (prod, prod_euc1, prod_usw2 …) without enumerating each one.
 *                   Prod gets P1 only (critical path, minimal prod risk); non-prod gets
 *                   the full wildcard suite for broader coverage.
 *
 * Run: groovy scripts/EnvironmentConfig.groovy
 */

// ---------------------------------------------------------------------------
// Region mapping
// ---------------------------------------------------------------------------

final Map<String, String> REGION_MAP = [
    "dev"       : "dev",
    "test"      : "test",
    "prod"      : "prod",
    "prod_euc1" : "prod-euc1",
    "prod_usw2" : "prod-usw2"
].asImmutable()

def getRegion = { String environment ->
    if (!REGION_MAP.containsKey(environment)) {
        throw new IllegalArgumentException(
            "Unrecognized environment: '${environment}'. Valid values: ${REGION_MAP.keySet().sort()}"
        )
    }
    return REGION_MAP[environment]
}

def getTestGroups = { String environment ->
    if (!REGION_MAP.containsKey(environment)) {
        throw new IllegalArgumentException(
            "Unrecognized environment: '${environment}'. Valid values: ${REGION_MAP.keySet().sort()}"
        )
    }
    return environment.startsWith("prod") ? "outbound.oms.p1" : "outbound.oms.*"
}

// ---------------------------------------------------------------------------
// Inline tests
// ---------------------------------------------------------------------------

int passed = 0
int failed = 0

def assert_eq = { String label, String actual, String expected ->
    if (actual == expected) {
        println "[PASS] ${label}: '${actual}'"
        passed++
    } else {
        println "[FAIL] ${label}: expected '${expected}', got '${actual}'"
        failed++
    }
}

def assert_throws = { String label, Closure block ->
    try {
        block()
        println "[FAIL] ${label}: expected IllegalArgumentException but none was thrown"
        failed++
    } catch (IllegalArgumentException e) {
        println "[PASS] ${label}: threw IllegalArgumentException"
        passed++
    }
}

println ""
println "==================================================="
println "  Scenario 3 — Environment Configuration Utility  "
println "==================================================="
println ""

// getRegion — assertions required by the spec
assert_eq("getRegion('prod_usw2')", getRegion("prod_usw2"), "prod-usw2")
assert_eq("getRegion('dev')",       getRegion("dev"),       "dev")
assert_eq("getRegion('test')",      getRegion("test"),      "test")
assert_eq("getRegion('prod')",      getRegion("prod"),      "prod")
assert_eq("getRegion('prod_euc1')", getRegion("prod_euc1"), "prod-euc1")

// Invalid environment must throw
assert_throws("getRegion('staging') throws") { getRegion("staging") }
assert_throws("getRegion('') throws")        { getRegion("") }

// getTestGroups — assertions required by the spec
assert_eq("getTestGroups('test')",      getTestGroups("test"),      "outbound.oms.*")
assert_eq("getTestGroups('dev')",       getTestGroups("dev"),       "outbound.oms.*")
assert_eq("getTestGroups('prod_euc1')", getTestGroups("prod_euc1"), "outbound.oms.p1")
assert_eq("getTestGroups('prod_usw2')", getTestGroups("prod_usw2"), "outbound.oms.p1")
assert_eq("getTestGroups('prod')",      getTestGroups("prod"),      "outbound.oms.p1")

// Invalid environment must throw
assert_throws("getTestGroups('unknown') throws") { getTestGroups("unknown") }

println ""
println "---------------------------------------------------"
println "  Results: ${passed} passed, ${failed} failed out of ${passed + failed} tests"
println "---------------------------------------------------"
println ""

if (failed > 0) {
    System.exit(1)
}
