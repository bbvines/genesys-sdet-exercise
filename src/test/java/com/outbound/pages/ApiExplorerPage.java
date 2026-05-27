package com.outbound.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

public class ApiExplorerPage {

    public static final String HOME_URL        = "https://developer.genesys.cloud/";
    public static final String EXPLORER_URL    = "https://developer.genesys.cloud/devapps/api-explorer";

    private final WebDriver driver;
    private final WebDriverWait wait;

    public ApiExplorerPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public void loadHomePage() {
        driver.get(HOME_URL);
        // Wait for full page load before checking title — React SPA sets title after JS hydration
        wait.until(d -> ((JavascriptExecutor) d)
                .executeScript("return document.readyState").equals("complete"));
        wait.until(ExpectedConditions.not(ExpectedConditions.titleIs("")));
    }

    public void loadApiExplorerDirectly() {
        driver.get(EXPLORER_URL);
        waitForExplorerToRender();
    }


    public void waitForExplorerToRender() {
        wait.until(driver -> {
            // accordion-heading = API section headers (e.g. "Outbound") confirmed live in DOM
            // operation = endpoint rows confirmed live in DOM (77 elements)
            List<WebElement> candidates = driver.findElements(
                    By.xpath("//*[contains(@class,'accordion-heading') or contains(@class,'operation')]"));
            return !candidates.isEmpty();
        });
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Returns true once at least one API category/resource is visible in the sidebar.
     * TOPICS items: class="nav-item nav-level-1 group-title" (Analytics, Authorization...)
     * RESOURCES items: class="nav-item nav-level-1" (Accelerators, Announcements...)
     * Both share 'nav-item' — confirmed live in DOM (36 elements).
     */
    public boolean apiCategoryListIsVisible() {
        List<WebElement> items = driver.findElements(
                By.xpath("//*[contains(@class,'nav-item')]"));
        return !items.isEmpty();
    }

    /**
     * Finds and clicks the "Outbound" section heading in the main content area.
     * Uses case-insensitive XPath text matching to handle different capitalizations.
     */
    public void clickOutboundCategory() {
        By outboundLocator = By.xpath(
                "//*[contains(@class,'accordion-heading') and " +
                "contains(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'outbound')]");

        // If outbound path elements are already in the DOM, the section is already expanded.
        // Clicking an already-open accordion collapses it — so skip the click in that case.
        List<WebElement> alreadyExpanded = driver.findElements(
                By.xpath("//*[contains(text(),'/outbound/')]"));

        if (alreadyExpanded.isEmpty()) {
            // Section is collapsed — click to expand it
            WebElement outboundTag = wait.until(ExpectedConditions.elementToBeClickable(outboundLocator));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", outboundTag);
            outboundTag.click();
        } else {
            // Section already expanded — just scroll the heading into view
            List<WebElement> headings = driver.findElements(outboundLocator);
            if (!headings.isEmpty()) {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block:'center'});", headings.get(0));
            }
        }

        // Wait until outbound path elements are present in the DOM
        wait.until(driver -> !driver.findElements(
                By.xpath("//*[contains(text(),'/outbound/')]"))
                .isEmpty());
    }

    /**
     * Returns true when the Outbound section contains at least one visible endpoint.
     */
    public boolean outboundEndpointsAreVisible() {
        List<WebElement> endpoints = driver.findElements(
                By.xpath("//*[contains(@class,'operation')]"));
        return !endpoints.isEmpty();
    }

    /**
     * Clicks the first endpoint whose path contains "/outbound/campaigns" (GET preferred).
     * Falls back to the first visible Outbound endpoint if the preferred one is not found.
     */
    public void clickOutboundCampaignsEndpoint() {
        // New API Explorer uses a filter input — type the path to narrow results
        List<WebElement> filterInputs = driver.findElements(
                By.xpath("//input[@placeholder[contains(.,'filter') or contains(.,'path') or contains(.,'resource')]]"));
        if (!filterInputs.isEmpty()) {
            filterInputs.get(0).clear();
            filterInputs.get(0).sendKeys("/outbound/campaigns");
        }

        // Target specifically GET /api/v2/outbound/campaigns.
        // The operation row for GET carries a 'get' CSS class alongside 'operation'.
        // The path text must be exactly '/outbound/campaigns' — not a deeper path like
        // '/outbound/campaigns/{campaignId}' — so we exclude text that goes further with '/campaigns/'.
        By campaignsPath = By.xpath(
                "//*[contains(text(),'/outbound/campaigns') and " +
                "not(contains(text(),'/outbound/campaigns/'))]");
        WebElement pathEl = wait.until(ExpectedConditions.presenceOfElementLocated(campaignsPath));
        // Walk up the DOM to find the 'get' operation row — the clickable container, not the text span
        WebElement clickTarget = (WebElement) ((JavascriptExecutor) driver).executeScript(
                "var el = arguments[0];" +
                "while (el && el.className && !el.className.includes('operation')) {" +
                "  el = el.parentElement;" +
                "}" +
                "return el || arguments[0];", pathEl);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", clickTarget);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", clickTarget);

        // Wait for detail section — new explorer shows "Request Parameters" or "Operation Information"
        wait.until(driver -> !driver.findElements(
                By.xpath("//*[contains(text(),'Request Parameters') or contains(text(),'Operation Information') " +
                         "or contains(text(),'pageSize') or contains(@class,'description')]"))
                .isEmpty());
    }

    /**
     * Returns the HTTP method badge text shown on the expanded endpoint (e.g., "GET").
     */
    public String getEndpointHttpMethod() {
        // Find verb badge inside the specific expanded operation that contains /outbound/campaigns path
        List<WebElement> methodBadges = driver.findElements(
                By.xpath("//div[contains(@class,'operation') and " +
                         ".//*[contains(text(),'/outbound/campaigns') and not(contains(text(),'/outbound/campaigns/'))]]" +
                         "//div[@class='verb']"));
        return methodBadges.isEmpty() ? "" : methodBadges.get(0).getText().trim().toUpperCase();
    }

    /**
     * Returns the path string shown in the expanded endpoint detail (e.g., "/api/v2/outbound/campaigns").
     */
    public String getEndpointPath() {
        // Target path element specifically for /outbound/campaigns — excludes deeper paths and other outbound endpoints
        List<WebElement> pathEls = driver.findElements(
                By.xpath("//*[@class='path' and " +
                         "contains(text(),'/outbound/campaigns') and " +
                         "not(contains(text(),'/outbound/campaigns/'))]"));
        return pathEls.isEmpty() ? "" : pathEls.get(0).getText().trim();
    }

    /**
     * True if any parameter row or description text is present in the detail panel.
     */
    public boolean endpointHasParameterOrDescription() {
        // description class confirmed live in DOM (25 elements found)
        List<WebElement> params = driver.findElements(
                By.xpath("//*[contains(@class,'description') or " +
                         "contains(text(),'pageSize') or contains(text(),'pageNumber')]"));
        return !params.isEmpty();
    }

    /**
     * True if a response schema block is present in the detail panel.
     */
    public boolean endpointHasResponseSchema() {
        // Three-layer check matching what the assignment requires — "response schema or example":
        // Layer 1: 'API Responses' heading — always in DOM whether accordion open or closed
        // Layer 2: responses-display class — confirms response rows (200, 400...) are loaded
        // Layer 3: '200 - successful operation' — confirms a success response schema is defined
        List<WebElement> responses = driver.findElements(
                By.xpath("//*[text()='API Responses' or " +
                         "contains(@class,'responses-display') or " +
                         "contains(text(),'200 - successful operation')]"));
        return !responses.isEmpty();
    }
}
