package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class ExtentReportManager {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();

    public static ExtentReports getReportInstance() {
        if (extent == null) {
            String reportPath = System.getProperty("user.dir") + "/test-output/ExtentReport.html";
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
            sparkReporter.config().setReportName("WDIO Demo App – Appium TestNG Report");
            sparkReporter.config().setDocumentTitle("WDIODemoApp Test Results");

            extent = new ExtentReports();
            extent.attachReporter(sparkReporter);
            extent.setSystemInfo("Application", "WDIO Native Demo App v2.2.0");
            extent.setSystemInfo("App Package", "com.wdiodemoapp");
            extent.setSystemInfo("Platform", "Android");
            extent.setSystemInfo("Automation", "Appium + UiAutomator2 + TestNG");
        }
        return extent;
    }

    public static ExtentTest createTest(String testName) {
        ExtentTest test = getReportInstance().createTest(testName);
        testThread.set(test);
        return test;
    }

    public static ExtentTest getTest() {
        return testThread.get();
    }
}
