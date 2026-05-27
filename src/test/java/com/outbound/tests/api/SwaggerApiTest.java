package com.outbound.tests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.outbound.config.SwaggerSpecCache;
import io.qameta.allure.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Test Scenario 2 — API: Validate the Public API Schema
 *
 * Fetches the Genesys Cloud OpenAPI 2.0 (Swagger) specification and runs
 * structural validations against the Outbound API paths and schema definitions.
 *
 * The spec is fetched once per class (expensive network call) and reused
 * across all test methods via @BeforeClass.
 */
@Epic("Genesys Cloud API")
@Feature("Swagger Validation")
public class SwaggerApiTest {

    private static final String OUTBOUND_CAMPAIGNS_PATH = "/api/v2/outbound/messagingcampaigns";
    private static final String SCHEMA_DEF_NAME         = "MessagingCampaign";
    private static final long   SWAGGER_RESPONSE_SLA_MS = 15_000;

    private JsonNode swaggerRoot;
    private int      httpStatusCode;
    private long     responseTimeMs;

    @BeforeClass(alwaysRun = true)
    @Step("Load Swagger specification (shared cache — downloads once per JVM)")
    public void fetchSwaggerSpec() {
        long start = System.currentTimeMillis();
        swaggerRoot    = SwaggerSpecCache.getRoot();
        httpStatusCode = SwaggerSpecCache.getHttpStatus();
        responseTimeMs = System.currentTimeMillis() - start;
    }

    /**
     * Step 1: Swagger endpoint returns 200 OK with a parseable JSON body.
     */
    @Test(description = "Test 1: Swagger spec returns 200 OK with valid JSON")
    public void swaggerSpecReturns200WithValidJson() {
        assertEquals(httpStatusCode, 200,
                "Expected HTTP 200 from " + SwaggerSpecCache.SWAGGER_URL + ", got: " + httpStatusCode);
        assertNotNull(swaggerRoot,
                "Response body should be valid JSON and parseable");
        assertTrue(swaggerRoot.has("paths"),
                "Swagger root must contain a 'paths'");
        assertTrue(swaggerRoot.has("definitions"),
                "Swagger root must contain a 'definitions'");

        // Non-functional: response time SLA — even a 20MB payload must arrive within limit
        assertTrue(responseTimeMs < SWAGGER_RESPONSE_SLA_MS,
                "Swagger spec took " + responseTimeMs + "ms to load — exceeds SLA of "
                + SWAGGER_RESPONSE_SLA_MS + "ms. Possible: network degradation or server overload.");
        System.out.printf("[SLA] Swagger spec loaded in %dms (limit: %dms)%n",
                responseTimeMs, SWAGGER_RESPONSE_SLA_MS);
    }

    /**
     * Step 2: At least 5 paths contain "/outbound/" — verifies the Outbound API is well-represented.
     */
    @Test(description = "Test 2: Swagger spec contains at least 5 outbound paths")
    public void swaggerContainsAtLeastFiveOutboundPaths() {
        JsonNode paths = swaggerRoot.get("paths");
        assertNotNull(paths, "'paths' node must not be null");

        List<String> outboundPaths = new ArrayList<>();
        paths.fieldNames().forEachRemaining(path -> {
            if (path.contains("/outbound/")) {
                outboundPaths.add(path);
            }
        });

        assertTrue(outboundPaths.size() >= 5,
                "Expected at least 5 outbound paths, found: " + outboundPaths.size()
                + ". Paths found: " + outboundPaths);
    }

    /**
     * Step 3: /api/v2/outbound/messagingcampaigns defines a GET operation
     *         with a 200 response that references a schema.
     */
    @Test(description = "Test 3: GET /outbound/messagingcampaigns has 200 response with schema")
    public void messagingCampaignsGetHas200ResponseWithSchema() {
        JsonNode pathNode = swaggerRoot.path("paths").path(OUTBOUND_CAMPAIGNS_PATH);
        assertFalse(pathNode.isMissingNode(),
                "Path '" + OUTBOUND_CAMPAIGNS_PATH + "' must exist in the spec");

        JsonNode getOperation = pathNode.path("get");
        assertFalse(getOperation.isMissingNode(),
                "A GET operation must be defined on " + OUTBOUND_CAMPAIGNS_PATH);

        JsonNode response200 = getOperation.path("responses").path("200");
        assertFalse(response200.isMissingNode(),
                "GET " + OUTBOUND_CAMPAIGNS_PATH + " must define a 200 response");

        // The 200 response must reference a schema (either inline or via $ref)
        boolean hasSchema = response200.has("schema");
        assertTrue(hasSchema,
                "The 200 response for GET " + OUTBOUND_CAMPAIGNS_PATH
                + " must include a 'schema' field referencing a definition");

        // The schema must actually point somewhere (either $ref or type)
        JsonNode schema = response200.get("schema");
        boolean hasRef  = schema.has("$ref");
        boolean hasType = schema.has("type");
        assertTrue(hasRef || hasType,
                "Response schema must contain a '$ref' or 'type' field");
    }

    /**
     * Step 4: /api/v2/outbound/messagingcampaigns also defines a POST operation
     *         with a request body (parameters of type 'body').
     */
    @Test(description = "Test 4: POST /outbound/messagingcampaigns has request body schema")
    public void messagingCampaignsPostHasRequestBodySchema() {
        JsonNode pathNode = swaggerRoot.path("paths").path(OUTBOUND_CAMPAIGNS_PATH);
        assertFalse(pathNode.isMissingNode(),
                "Path '" + OUTBOUND_CAMPAIGNS_PATH + "' must exist in the spec");

        JsonNode postOperation = pathNode.path("post");
        assertFalse(postOperation.isMissingNode(),
                "A POST operation must be defined on " + OUTBOUND_CAMPAIGNS_PATH);

        // In Swagger 2.0 the request body is a parameter with "in": "body"
        JsonNode parameters = postOperation.path("parameters");
        assertFalse(parameters.isMissingNode() || !parameters.isArray(),
                "POST " + OUTBOUND_CAMPAIGNS_PATH + " must define a parameters array");

        boolean hasBodyParam = false;
        for (JsonNode param : parameters) {
            if ("body".equals(param.path("in").asText())) {
                hasBodyParam = true;
                // The body parameter must reference or inline a schema
                JsonNode schema = param.path("schema");
                assertFalse(schema.isMissingNode(),
                        "Body parameter on POST " + OUTBOUND_CAMPAIGNS_PATH
                        + " must include a 'schema' field");
                boolean hasRef  = schema.has("$ref");
                boolean hasType = schema.has("type");
                assertTrue(hasRef || hasType,
                        "Body parameter schema must contain a '$ref' or 'type'");
                break;
            }
        }

        assertTrue(hasBodyParam,
                "POST " + OUTBOUND_CAMPAIGNS_PATH
                + " must have a body-type parameter (in: body)");
    }

    /**
     * Step 5: The MessagingCampaign schema definition contains business-meaningful properties.
     *
     * Validates that core fields — "name", "contactList", and "messagesPerMinute" — exist
     * in the schema. These are the properties you'd configure when setting up an outbound
     * messaging campaign, so their presence confirms the spec is complete and not truncated.
     */
    @Test(description = "Test 5: MessagingCampaign schema has expected properties")
    public void messagingCampaignSchemaHasExpectedProperties() {
        JsonNode definitions = swaggerRoot.path("definitions");
        assertFalse(definitions.isMissingNode(),
                "Swagger spec must contain a 'definitions' section");

        JsonNode schema = definitions.path(SCHEMA_DEF_NAME);
        assertFalse(schema.isMissingNode(),
                "Definition '" + SCHEMA_DEF_NAME + "' must exist in the spec");

        JsonNode properties = schema.path("properties");
        assertFalse(properties.isMissingNode(),
                SCHEMA_DEF_NAME + " must define a 'properties' object");

        assertTrue(properties.has("name"),
                SCHEMA_DEF_NAME + " properties must include 'name'");
        assertTrue(properties.has("contactList"),
                SCHEMA_DEF_NAME + " properties must include 'contactList'");
        assertTrue(properties.has("messagesPerMinute"),
                SCHEMA_DEF_NAME + " properties must include 'messagesPerMinute'");
    }
}
