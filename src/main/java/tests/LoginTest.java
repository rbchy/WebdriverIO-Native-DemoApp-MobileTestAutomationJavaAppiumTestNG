package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.HomePage;
import pages.LoginPage;
import utils.ExtentReportManager;

/**
 * Login screen tests.
 *
 * Valid credentials (as defined by the WDIO Demo App):
 *   email    : alice@example.com
 *   password : CorrectHorseBatteryStaple (any non-empty value works in this demo app)
 *
 * The app accepts any non-empty email and any non-empty password for a successful login.
 * It shows an error when either field is empty or credentials are in a wrong format.
 */
public class LoginTest extends BaseTest {

    private HomePage homePage;
    private LoginPage loginPage;

    @BeforeMethod
    public void navigateToLogin() {
        homePage  = new HomePage(driver);
        loginPage = new LoginPage(driver);
        homePage.goToLogin();
        Assert.assertTrue(loginPage.isLoginScreenDisplayed(),
            "Login screen should be displayed after tapping Login tab");
    }

    // ------------------------------------------------------------------ //
    //  TC-L01 : Valid login                                                //
    // ------------------------------------------------------------------ //
    @Test(priority = 1, description = "Valid credentials should log the user in")
    public void testValidLogin() {
        ExtentReportManager.createTest("TC-L01 : Valid Login");

        loginPage.login("alice@example.com", "Password1");

        Assert.assertTrue(loginPage.isLoginSuccessful(),
            "User should be logged in with valid credentials");
        ExtentReportManager.getTest().info("Login successful with valid credentials");
    }

    // ------------------------------------------------------------------ //
    //  TC-L02 : Empty email                                                //
    // ------------------------------------------------------------------ //
    @Test(priority = 2, description = "Empty email field should show an error")
    public void testEmptyEmailShowsError() {
        ExtentReportManager.createTest("TC-L02 : Empty Email");

        loginPage.enterPassword("Password1");
        loginPage.tapLogin();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
            "Error message should appear when email is empty");
        ExtentReportManager.getTest().info("Error displayed for empty email");
    }

    // ------------------------------------------------------------------ //
    //  TC-L03 : Empty password                                             //
    // ------------------------------------------------------------------ //
    @Test(priority = 3, description = "Empty password field should show an error")
    public void testEmptyPasswordShowsError() {
        ExtentReportManager.createTest("TC-L03 : Empty Password");

        loginPage.enterEmail("alice@example.com");
        loginPage.tapLogin();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
            "Error message should appear when password is empty");
        ExtentReportManager.getTest().info("Error displayed for empty password");
    }

    // ------------------------------------------------------------------ //
    //  TC-L04 : Invalid email format                                       //
    // ------------------------------------------------------------------ //
    @Test(priority = 4, description = "Invalid email format should show an error")
    public void testInvalidEmailFormat() {
        ExtentReportManager.createTest("TC-L04 : Invalid Email Format");

        loginPage.login("not-an-email", "Password1");

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
            "Error message should appear for invalid email format");
        ExtentReportManager.getTest().info("Error displayed for invalid email format");
    }

    // ------------------------------------------------------------------ //
    //  TC-L05 : Both fields empty                                          //
    // ------------------------------------------------------------------ //
    @Test(priority = 5, description = "Tapping Login with both fields empty should show an error")
    public void testBothFieldsEmpty() {
        ExtentReportManager.createTest("TC-L05 : Both Fields Empty");

        loginPage.tapLogin();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
            "Error message should appear when both fields are empty");
        ExtentReportManager.getTest().info("Error displayed when both fields are empty");
    }
}
