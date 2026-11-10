package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AlertPage;
import pages.FormPage;
import pages.HomePage;
import pages.LoginPage;
import utils.ExtentReportManager;

/**
 * Native Android dialogs in the WDIO demo app (Forms "Active" / "Inactive" buttons, login success).
 */
public class AlertTest extends BaseTest {

    private HomePage homePage;
    private AlertPage alertPage;

    @BeforeMethod
    public void openForms() {
        homePage  = new HomePage(driver);
        alertPage = new AlertPage(driver);
        alertPage.tapOk(); // close a dialog left open by a failed test, if any
        homePage.goToForms();
        Assert.assertTrue(new FormPage(driver).isFormScreenDisplayed(), "Forms screen should open");
    }

    private void openActiveDialog() {
        alertPage.tapFormsButton(AlertPage.ACTIVE_BUTTON);
        Assert.assertTrue(alertPage.isDialogShown(10), "Tapping Active should open a dialog");
    }

    @Test(priority = 1, description = "Active button opens a dialog with the expected title, message and buttons")
    public void testActiveButtonShowsDialog() {
        ExtentReportManager.createTest("TC-A01 : Active button - dialog content");
        openActiveDialog();
        Assert.assertEquals(alertPage.title(), "This button is");
        Assert.assertEquals(alertPage.message(), "This button is active");
        Assert.assertEquals(alertPage.buttonText("ok").toUpperCase(), "OK");
        Assert.assertEquals(alertPage.buttonText("cancel").toUpperCase(), "CANCEL");
        Assert.assertEquals(alertPage.buttonText("neutral").toUpperCase(), "ASK ME LATER");
        alertPage.tapOk();
        ExtentReportManager.getTest().info("Dialog: " + alertPage.title() + " / " + alertPage.message());
    }

    @Test(priority = 2, description = "OK closes the dialog")
    public void testDialogOkCloses() {
        ExtentReportManager.createTest("TC-A02 : Dialog - OK");
        openActiveDialog();
        alertPage.tapOk();
        Assert.assertTrue(alertPage.isDialogGone(5), "Dialog should close after OK");
    }

    @Test(priority = 3, description = "CANCEL closes the dialog")
    public void testDialogCancelCloses() {
        ExtentReportManager.createTest("TC-A03 : Dialog - Cancel");
        openActiveDialog();
        alertPage.tapCancel();
        Assert.assertTrue(alertPage.isDialogGone(5), "Dialog should close after CANCEL");
    }

    @Test(priority = 4, description = "ASK ME LATER closes the dialog")
    public void testDialogAskMeLaterCloses() {
        ExtentReportManager.createTest("TC-A04 : Dialog - Ask me later");
        openActiveDialog();
        alertPage.tapAskMeLater();
        Assert.assertTrue(alertPage.isDialogGone(5), "Dialog should close after ASK ME LATER");
    }

    @Test(priority = 5, description = "Inactive button is disabled and opens no dialog")
    public void testInactiveButtonShowsNoDialog() {
        ExtentReportManager.createTest("TC-A05 : Inactive button - no dialog");
        alertPage.tapFormsButton(AlertPage.INACTIVE_BUTTON);
        Assert.assertFalse(alertPage.isDialogShown(3), "Inactive button must not open a dialog");
    }

    @Test(priority = 6, description = "Valid login shows the Success dialog, and OK closes it")
    public void testLoginSuccessDialog() {
        ExtentReportManager.createTest("TC-A06 : Login success dialog");
        homePage.goToLogin();
        LoginPage login = new LoginPage(driver);
        Assert.assertTrue(login.isLoginScreenDisplayed());
        login.login("alice@example.com", "Password1");
        Assert.assertTrue(alertPage.isDialogShown(10), "Success dialog expected");
        Assert.assertEquals(alertPage.title(), "Success");
        Assert.assertEquals(alertPage.message(), "You are logged in!");
        alertPage.tapOk();
        Assert.assertTrue(alertPage.isDialogGone(5));
    }
}
