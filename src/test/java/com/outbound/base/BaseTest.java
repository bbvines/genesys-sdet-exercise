package com.outbound.base;

import com.outbound.config.ConfigManager;
import com.outbound.factory.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;

/**
 * Template Method Pattern — BaseTest
 *
 * Defines the test lifecycle skeleton (setUp → test → tearDown).
 * Subclasses inherit a ready-to-use driver and wait without knowing
 * HOW the driver was created or which browser is running.
 *
 * Driver creation is delegated to DriverFactory (Factory Pattern).
 * Timeout values are read from ConfigManager (Singleton Pattern).
 * This class has zero hardcoded values.
 */
public class BaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final ConfigManager config = ConfigManager.getInstance();

    @BeforeMethod
    public void setUp() {
        log.info("Starting browser: {}", System.getProperty("browser", "chrome"));
        driver = DriverFactory.getDriver();
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(config.getInt("page.load.timeout", 60)));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
        wait = new WebDriverWait(driver,
                Duration.ofSeconds(config.getInt("explicit.wait.timeout", 30)));
        log.debug("WebDriver initialised successfully");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        log.info("Quitting browser");
        DriverFactory.quitDriver();
    }
}
