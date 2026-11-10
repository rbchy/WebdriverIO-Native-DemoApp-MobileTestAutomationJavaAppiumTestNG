package base;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import utils.ExtentReportManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

/**
 * BaseTest — creates one AndroidDriver per test class.
 *
 * KEY FIX (LoginTest Run-1 failure):
 *   After React Native app launch, the bottom nav bar renders in two passes:
 *   1. The Home tab label appears first (used by previous wait).
 *   2. All other tab labels (Login, Forms, Swipe…) become accessible only
 *      after the React Native JS bridge finishes hydration (~30-60 s).
 *
 *   The old code only waited for "Home" and returned immediately.
 *   goToLogin() then tried AppiumBy.accessibilityId("Login") which wasn't
 *   in the accessibility tree yet → NoSuchElement → Run 1 always failed.
 *
 *   Fix: after "Home" is visible, also wait up to 60 s for "Login" tab.
 *   This ensures the full nav bar is ready before any @BeforeMethod runs.
 */
public class BaseTest {

    protected AndroidDriver driver;
    protected String deviceName;
    protected String platformVersion;

    private static final String APPIUM_URL = "http://127.0.0.1:4723";

    private void assertAppiumServerIsRunning() {
        try {
            HttpURLConnection conn = (HttpURLConnection)
                    new URL(APPIUM_URL + "/status").openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200)
                throw new IllegalStateException("Appium HTTP " + conn.getResponseCode());
        } catch (IOException e) {
            throw new IllegalStateException(
                "\n\n==============================================\n" +
                " APPIUM SERVER IS NOT RUNNING — start it: appium\n" +
                "==============================================\n", e);
        }
    }

    @Parameters({"deviceName", "platformVersion"})
    @BeforeClass
    public void setUp(String deviceName, String platformVersion) throws Exception {
        this.deviceName      = deviceName;
        this.platformVersion = platformVersion;

        assertAppiumServerIsRunning();

        UiAutomator2Options options = new UiAutomator2Options();
        options.setDeviceName(deviceName);
        options.setPlatformVersion(platformVersion);
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setApp(System.getProperty("user.dir") + "/android.wdio.native.app.v2.2.0.apk");
        options.setAppPackage("com.wdiodemoapp");
        options.setAppActivity(".MainActivity");
        options.setNewCommandTimeout(Duration.ofSeconds(120));
        options.setUiautomator2ServerLaunchTimeout(Duration.ofSeconds(60));
        options.setUiautomator2ServerInstallTimeout(Duration.ofSeconds(60));
        options.setNoReset(false);
        options.setFullReset(false);

        driver = new AndroidDriver(new URL(APPIUM_URL), options);

        // ── Step 1: give the JS bundle time to start loading ──
        Thread.sleep(5000);

        // ── Step 2: wait for Home tab (app is running) ──
        try {
            new WebDriverWait(driver, Duration.ofSeconds(45))
                .until(ExpectedConditions.visibilityOfElementLocated(
                    AppiumBy.accessibilityId("Home")));
            System.out.println("[BaseTest] Home tab visible");
        } catch (Exception e) {
            System.out.println("[BaseTest] Home tab not visible in 45 s: " + e.getMessage());
        }

        // ── Step 3: wait for Login tab (confirms full nav bar is hydrated) ──
        // React Native renders other tabs slightly after Home. Without this wait,
        // goToLogin() fires before the Login tab is in the accessibility tree.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(60))
                .until(ExpectedConditions.visibilityOfElementLocated(
                    AppiumBy.accessibilityId("Login")));
            System.out.println("[BaseTest] Login tab visible — nav bar ready");
        } catch (Exception e) {
            System.out.println("[BaseTest] Login tab not visible in 60 s, proceeding: "
                + e.getMessage());
        }
    }

    // alwaysRun: when a class's set-up is skipped, TestNG would otherwise skip these too -
    // the driver session leaked and @AfterSuite never wrote the ExtentReport.
    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    @AfterSuite(alwaysRun = true)
    public void flushReport() {
        ExtentReportManager.getReportInstance().flush();
    }
}
