package com.outbound.factory;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Factory Pattern + Singleton (ThreadLocal) Pattern
 *
 * Factory Pattern:
 *   The factory decides WHICH WebDriver to create based on the "browser"
 *   system property. Tests never call "new ChromeDriver()" directly — they
 *   ask the factory. Adding Edge or Safari support means changing one class,
 *   not every test.
 *
 * Singleton via ThreadLocal:
 *   One WebDriver instance per thread. This makes the factory safe for
 *   parallel test execution — each thread gets its own isolated browser
 *   session. Without ThreadLocal, parallel tests would share one driver
 *   and stomp on each other.
 *
 * Usage:
 *   WebDriver driver = DriverFactory.getDriver();   // get or create
 *   DriverFactory.quitDriver();                     // quit and clear
 *
 * Supported values for -Dbrowser=chrome|firefox (default: chrome)
 */
public class DriverFactory {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverFactory() {}

    public static WebDriver getDriver() {
        if (driverThreadLocal.get() == null) {
            String browser = System.getProperty("browser", "chrome").toLowerCase();
            WebDriver driver = switch (browser) {
                case "firefox" -> createFirefoxDriver();
                case "chrome"  -> createChromeDriver();
                default -> throw new IllegalArgumentException(
                        "Unsupported browser: '" + browser + "'. Use -Dbrowser=chrome|firefox");
            };
            driverThreadLocal.set(driver);
        }
        return driverThreadLocal.get();
    }

    /**
     * Quits the driver and removes it from the ThreadLocal store.
     * Must be called in @AfterMethod to prevent browser leaks between tests.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }

    private static WebDriver createChromeDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        applyHeadlessFlag(options);
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--remote-allow-origins=*"
        );
        return new ChromeDriver(options);
    }

    private static WebDriver createFirefoxDriver() {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        if (isHeadless()) {
            options.addArguments("--headless");
        }
        return new FirefoxDriver(options);
    }

    private static void applyHeadlessFlag(ChromeOptions options) {
        if (isHeadless()) {
            options.addArguments("--headless=new");
        }
    }

    private static boolean isHeadless() {
        return Boolean.parseBoolean(System.getProperty("headless", "false"));
    }
}
