package com.outbound.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failing test up to MAX_RETRY times before marking it as failed.
 *
 * Applied to UI tests only (see @Test(retryAnalyzer = RetryAnalyzer.class) in
 * DeveloperCenterUITest). API tests are deterministic and should not be retried
 * — a failing API assertion is a real failure, not transient flakiness.
 *
 * Why: browser-based tests are inherently flaky due to network latency,
 * animation timing, and SPA hydration delays. A single retry distinguishes
 * genuine failures from one-off timing issues without masking real bugs.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRY = 2;
    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (attempt < MAX_RETRY) {
            attempt++;
            System.out.printf("[RetryAnalyzer] Retrying '%s' (attempt %d of %d)%n",
                    result.getName(), attempt, MAX_RETRY);
            return true;
        }
        return false;
    }

    /**
     * Returns true if there are still retries remaining.
     * Used by ScreenshotListener to skip screenshots on intermediate failures
     * and only capture on the final failure.
     */
    public boolean hasRetriesRemaining() {
        return attempt < MAX_RETRY;
    }
}
