package listeners;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.ExtentReportManager;
import com.aventstack.extentreports.Status;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ScreenshotListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        try {
            AndroidDriver driver = extractDriver(result);
            if (driver != null) {
                File srcFile = driver.getScreenshotAs(OutputType.FILE);
                String folder = System.getProperty("user.dir") + "/test-output/screenshots/";
                new File(folder).mkdirs();
                String fileName = result.getMethod().getMethodName() + "_FAILED.png";
                Files.copy(srcFile.toPath(), Paths.get(folder + fileName));
                System.out.println("Screenshot saved: " + folder + fileName);

                if (ExtentReportManager.getTest() != null) {
                    ExtentReportManager.getTest().log(Status.FAIL,
                        "Screenshot: test-output/screenshots/" + fileName);
                }
            }
        } catch (Exception e) {
            System.out.println("Could not capture screenshot: " + e.getMessage());
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (ExtentReportManager.getTest() != null) {
            ExtentReportManager.getTest().log(Status.PASS, "Test passed: " + result.getName());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (ExtentReportManager.getTest() != null) {
            ExtentReportManager.getTest().log(Status.SKIP, "Test skipped: " + result.getName());
        }
    }

    // ------------------------------------------------------------------ //
    //  Helper: pull the AndroidDriver from the test instance via reflection
    // ------------------------------------------------------------------ //
    private AndroidDriver extractDriver(ITestResult result) {
        try {
            Object instance = result.getInstance();
            Class<?> clazz = instance.getClass();
            // Walk up the hierarchy to find the 'driver' field in BaseTest
            while (clazz != null) {
                try {
                    Field f = clazz.getDeclaredField("driver");
                    f.setAccessible(true);
                    return (AndroidDriver) f.get(instance);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            System.out.println("Could not extract driver: " + e.getMessage());
        }
        return null;
    }
}
