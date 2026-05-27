package com.outbound.tests.ui;

import com.outbound.base.BaseTest;
import com.outbound.pages.ApiExplorerPage;
import io.qameta.allure.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Test Scenario 1 — UI: Navigate the Developer Center
 *
 * Validates the end-to-end browsing journey from the Genesys Cloud Developer
 * Center home page through to an Outbound API endpoint detail view.
 */
@Epic("Genesys Cloud Developer Center")
@Feature("UI Navigation")
public class DeveloperCenterUITest extends BaseTest {

    private ApiExplorerPage apiExplorerPage;

    @BeforeMethod
    public void initPage() {
        apiExplorerPage = new ApiExplorerPage(driver, wait);
    }

    /**
     * Step 1: Developer Center home page loads with expected title and URL.
     */
    @Test(description = "Test 1: Developer Center home page loads successfully")
    public void homePageLoads() {
        apiExplorerPage.loadHomePage();

        String title = apiExplorerPage.getPageTitle();
        String url   = apiExplorerPage.getCurrentUrl();
        assertTrue(title.toLowerCase().contains("genesys"),
                "Page title should mention Genesys, got: " + title);
        assertTrue(url.contains("developer.genesys.cloud"),
                "URL should be on developer.genesys.cloud, got: " + url);
    }

    /**
     * Step 2: API Explorer page loads and shows a list of API categories/resources.
     */
    @Test(description = "Test 2: API Explorer loads with visible API category list",
          dependsOnMethods = "homePageLoads")
    public void apiExplorerLoadsWithCategories() {
        apiExplorerPage.loadHomePage();
        apiExplorerPage.loadApiExplorerDirectly();

        String url = apiExplorerPage.getCurrentUrl();
        assertTrue(url.contains("api-explorer"),
                "URL should contain 'api-explorer', got: " + url);

        assertTrue(apiExplorerPage.apiCategoryListIsVisible(),
                "API Explorer should display at least one API category/resource tag");
    }

    /**
     * Step 3: The Outbound API section is visible with at least one endpoint.
     */
    @Test(description = "Test 3: Outbound API section is displayed with available endpoints",
          dependsOnMethods = "apiExplorerLoadsWithCategories")
    public void outboundApiSectionIsDisplayed() {
        apiExplorerPage.loadApiExplorerDirectly();
        apiExplorerPage.clickOutboundCategory();

        assertTrue(apiExplorerPage.outboundEndpointsAreVisible(),
                "Outbound category should expose at least one endpoint after being clicked");
    }

    /**
     * Step 4 & 5: Selecting an Outbound endpoint shows method, path, parameters, and response schema.
     */
    @Test(description = "Test 4 & 5: Outbound endpoint detail view shows HTTP method, path, parameters, and response schema",
          dependsOnMethods = "outboundApiSectionIsDisplayed")
    public void outboundEndpointDetailIsComplete() {
        apiExplorerPage.loadApiExplorerDirectly();
        apiExplorerPage.clickOutboundCategory();
        apiExplorerPage.clickOutboundCampaignsEndpoint();

        // The detail panel must surface the HTTP method
        String method = apiExplorerPage.getEndpointHttpMethod();
        assertFalse(method.isEmpty(),
                "Endpoint detail should display an HTTP method (GET/POST/PUT/DELETE)");

        // Must be specifically GET — the assignment targets GET /api/v2/outbound/campaigns
        assertTrue(method.contains("GET"),
                "HTTP method should be GET for the outbound/campaigns endpoint, got: " + method);

        // Path must reference specifically /outbound/campaigns
        String path = apiExplorerPage.getEndpointPath();
        assertTrue(path.contains("/outbound/campaigns"),
                "Endpoint path should contain '/outbound/campaigns', got: " + path);

        // Parameters section or description must be present
        assertTrue(apiExplorerPage.endpointHasParameterOrDescription(),
                "Endpoint detail should include at least one parameter or description field");

        // Response schema block must be present
        assertTrue(apiExplorerPage.endpointHasResponseSchema(),
                "Endpoint detail should include a response schema or example");
    }
}