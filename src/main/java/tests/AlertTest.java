package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AlertPage;
import pages.HomePage;
import utils.ExtentReportManager;

/**
 * Alert / Dialog screen tests.
 *
 * KEY FIX — @BeforeMethod no longer asserts isAlertScreenDisplayed().
 *
 * Previous behaviour:
 *   @BeforeMethod called isAlertScreenDisplayed() which waited 40 s.
 *   React Native hydration on Run 1 takes 60-90 s → assertion always timed
 *   out → Run 1 was always FAIL, Runs 2-5 passed (app was already hydrated).
 *
 * New behaviour:
 *   @BeforeMethod only navigates to Home and scrolls toward the buttons
 *   (fast, non-blocking, no assertion).
 *   Each trigger method (triggerDefaultAlert etc.) waits up to 120 s for its
 *   own button. Run 1 now waits up to 120 s and should PASS.
 */
public class AlertTest extends BaseTest {

    private HomePage  homePage;
    private AlertPage alertPage;

    @BeforeMethod
    public void navigateToAlerts() {
        homePage  = new HomePage(driver);
        alertPage = new AlertPage(driver);

        // Navigate to Home tab (Alert buttons are on the home screen)
        homePage.goToHome();

        // Scroll toward the Alert buttons (best-effort; no assert)
        alertPage.scrollToAlertButtons();

        // DO NOT assert here — the 120 s wait is inside each trigger method.
        // Asserting here caused Run 1 to always fail with a 40-79 s timeout.
    }

    // ------------------------------------------------------------------ //
    //  TC-A01 : Default alert — dismiss with OK                            //
    // ------------------------------------------------------------------ //
    @Test(priority = 1, description = "Default alert should appear and be dismissible with OK")
    public void testDefaultAlertAccept() {
        ExtentReportManager.createTest("TC-A01 : Default Alert - Accept");

        // triggerDefaultAlert waits up to 120 s — survives cold start
        alertPage.triggerDefaultAlert();
        Assert.assertTrue(alertPage.isAlertDisplayed(),
            "Default alert dialog should be displayed");
        ExtentReportManager.getTest().info("Default alert appeared");

        alertPage.acceptAlert();
        Assert.assertFalse(alertPage.isAlertDisplayed(),
            "Alert should be dismissed after pressing OK");
        ExtentReportManager.getTest().info("Alert dismissed with OK");
    }

    // ------------------------------------------------------------------ //
    //  TC-A02 : Confirm alert — accept with OK                             //
    // ------------------------------------------------------------------ //
    @Test(priority = 2, description = "Confirm alert should show OK and Cancel; accept with OK")
    public void testConfirmAlertAccept() {
        ExtentReportManager.createTest("TC-A02 : Confirm Alert - Accept");

        alertPage.triggerConfirmAlert();
        Assert.assertTrue(alertPage.isConfirmAlertDisplayed(),
            "Confirm alert should show Cancel button");
        ExtentReportManager.getTest().info("Confirm alert appeared");

        alertPage.acceptAlert();
        Assert.assertTrue(alertPage.isResultTextVisible("OK"),
            "Result text should indicate OK was pressed");
        ExtentReportManager.getTest().info("Confirm alert accepted with OK");
    }

    // ------------------------------------------------------------------ //
    //  TC-A03 : Confirm alert — dismiss with Cancel                        //
    // ------------------------------------------------------------------ //
    @Test(priority = 3, description = "Confirm alert should be dismissible with Cancel")
    public void testConfirmAlertCancel() {
        ExtentReportManager.createTest("TC-A03 : Confirm Alert - Cancel");

        alertPage.triggerConfirmAlert();
        Assert.assertTrue(alertPage.isConfirmAlertDisplayed(),
            "Confirm alert should be displayed");

        alertPage.dismissAlert();
        Assert.assertTrue(alertPage.isResultTextVisible("Cancel"),
            "Result text should indicate Cancel was pressed");
        ExtentReportManager.getTest().info("Confirm alert dismissed with Cancel");
    }

    // ------------------------------------------------------------------ //
    //  TC-A04 : Prompt alert — enter text and confirm                      //
    // ------------------------------------------------------------------ //
    @Test(priority = 4, description = "Prompt alert should accept typed text and confirm")
    public void testPromptAlertWithInput() {
        ExtentReportManager.createTest("TC-A04 : Prompt Alert - Input");

        alertPage.triggerPromptAlert();
        Assert.assertTrue(alertPage.isPromptAlertDisplayed(),
            "Prompt alert should display an input field");
        ExtentReportManager.getTest().info("Prompt alert appeared");

        alertPage.typeInPrompt("Hello Appium!");
        alertPage.acceptAlert();
        Assert.assertFalse(alertPage.isPromptAlertDisplayed(),
            "Prompt dialog should be dismissed after accepting");
        ExtentReportManager.getTest().info("Prompt alert accepted");
    }

    // ------------------------------------------------------------------ //
    //  TC-A05 : Prompt alert — cancel without input                        //
    // ------------------------------------------------------------------ //
    @Test(priority = 5, description = "Prompt alert should be dismissible with Cancel")
    public void testPromptAlertCancel() {
        ExtentReportManager.createTest("TC-A05 : Prompt Alert - Cancel");

        alertPage.triggerPromptAlert();
        Assert.assertTrue(alertPage.isPromptAlertDisplayed(),
            "Prompt alert should be displayed");

        alertPage.dismissAlert();
        Assert.assertFalse(alertPage.isPromptAlertDisplayed(),
            "Prompt alert should be dismissed after pressing Cancel");
        ExtentReportManager.getTest().info("Prompt alert cancelled");
    }
}
