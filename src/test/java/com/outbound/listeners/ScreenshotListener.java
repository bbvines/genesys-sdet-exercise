package com.outbound.listeners;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.IRetryAnalyzer;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TestNG listener that captures a full-page screenshot whenever a UI test fails
 * or is skipped due to a dependency failure.
 *
 * Registered in testng.xml so it applies to the entire suite automatically.
 * Screenshots land in target/screenshots/<timestamp>_<testName>.png — these
 * are picked up by CI artifact collectors (GitHub Actions, Jenkins) and linked
 * from the test report for instant visual debugging.
 *
 * Design note: the listener reaches into the test class via reflection to find
 * a field named "driver". This keeps BaseTest free of listener-specific code
 * while still giving the listener access to the live WebDriver instance.
 */
public class ScreenshotListener implements ITestListener {

    private static final String SCREENSHOT_DIR = "target/screenshots";
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public void onTestFailure(ITestResult result) {
        // Only capture screenshot on the FINAL failure — not on intermediate retry attempts
        IRetryAnalyzer retryAnalyzer = result.getMethod().getRetryAnalyzer(result);
        if (retryAnalyzer instanceof RetryAnalyzer ra && ra.hasRetriesRemaining()) {
            System.out.printf("[ScreenshotListener] Skipping screenshot — '%s' will be retried%n",
                    result.getName());
            return;
        }
        captureScreenshot(result, "FAILED");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        // Skipped usually means a dependency failed — capture state for diagnosis
        captureScreenshot(result, "SKIPPED");
    }

    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}


    private void captureScreenshot(ITestResult result, String status) {
        WebDriver driver = extractDriver(result);
        if (driver == null) return;

        try {
            Path dir = Paths.get(SCREENSHOT_DIR);
            Files.createDirectories(dir);

            String filename = String.format("%s_%s_%s.png",
                    LocalDateTime.now().format(TIMESTAMP),
                    status,
                    sanitise(result.getName()));

            Path dest = dir.resolve(filename);
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(dest, bytes);

            System.err.printf("[ScreenshotListener] %s → %s%n", status, dest.toAbsolutePath());

            Throwable t = result.getThrowable();
            if (t != null) {
                System.err.printf("[ScreenshotListener] Failure cause: %s%n", stackTrace(t));
            }

        } catch (IOException e) {
            System.err.println("[ScreenshotListener] Could not save screenshot: " + e.getMessage());
        }
    }

    /**
     * Walks up the test-instance class hierarchy looking for a field named "driver".
     * Returns null if the test class does not extend BaseTest (e.g. SwaggerApiTest).
     */
    private WebDriver extractDriver(ITestResult result) {
        Object instance = result.getInstance();
        Class<?> clazz = instance.getClass();

        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField("driver");
                field.setAccessible(true);
                Object value = field.get(instance);
                if (value instanceof WebDriver) {
                    return (WebDriver) value;
                }
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                System.err.println("[ScreenshotListener] Cannot access driver field: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    private String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
