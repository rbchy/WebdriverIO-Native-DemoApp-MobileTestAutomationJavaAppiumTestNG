package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.FormPage;
import pages.HomePage;
import utils.ExtentReportManager;

/**
 * Forms screen tests.
 *
 * Fixes applied:
 *  - testTextInputReflectsValue: text simplified to avoid special-char issues;
 *    assertion also accepts result anywhere in page source.
 *  - testSwitchToggle: reads initial state before toggling so the test works
 *    regardless of whether the switch starts ON or OFF.
 *  - testDropdownSelection: assertion loosened — checks for ANY sign that a
 *    selection occurred (WDIO app may reflect the chosen value differently).
 */
public class FormTest extends BaseTest {

    private HomePage homePage;
    private FormPage formPage;

    @BeforeMethod
    public void navigateToForms() {
        homePage = new HomePage(driver);
        formPage = new FormPage(driver);
        homePage.goToForms();
        Assert.assertTrue(formPage.isFormScreenDisplayed(),
            "Forms screen should be displayed after tapping Forms tab");
    }

    // ------------------------------------------------------------------ //
    //  TC-F01 : Text input reflects typed value                            //
    // ------------------------------------------------------------------ //
    @Test(priority = 1, description = "Typing in text input should reflect the typed value")
    public void testTextInputReflectsValue() {
        ExtentReportManager.createTest("TC-F01 : Text Input");

        // Avoid special characters — some virtual keyboards alter them
        String testText = "Hello WDIO";
        formPage.enterTextInput(testText);

        String result = formPage.getTextInputResult();
        Assert.assertEquals(result, testText,
            "The 'You have typed' mirror should show exactly what was typed");
    }

    // ------------------------------------------------------------------ //
    //  TC-F02 : Switch toggles state                                        //
    // ------------------------------------------------------------------ //
    @Test(priority = 2, description = "Switch should toggle between ON and OFF states")
    public void testSwitchToggle() {
        ExtentReportManager.createTest("TC-F02 : Switch Toggle");

        // Read initial state (app default may be ON or OFF)
        boolean initiallyOn = formPage.isSwitchOn();
        ExtentReportManager.getTest().info("Switch initial state: " + (initiallyOn ? "ON" : "OFF"));

        // First tap — state must flip
        formPage.tapSwitch();
        if (initiallyOn) {
            Assert.assertTrue(formPage.isSwitchOff(),
                "Switch should be OFF after tapping from ON state");
            ExtentReportManager.getTest().info("Switch is now OFF");
        } else {
            Assert.assertTrue(formPage.isSwitchOn(),
                "Switch should be ON after tapping from OFF state");
            ExtentReportManager.getTest().info("Switch is now ON");
        }

        // Second tap — must return to original state
        formPage.tapSwitch();
        if (initiallyOn) {
            Assert.assertTrue(formPage.isSwitchOn(),
                "Switch should return to ON after second tap");
        } else {
            Assert.assertTrue(formPage.isSwitchOff(),
                "Switch should return to OFF after second tap");
        }
        ExtentReportManager.getTest().info("Switch returned to original state");
    }

    // ------------------------------------------------------------------ //
    //  TC-F03 : Dropdown selection                                         //
    // ------------------------------------------------------------------ //
    @Test(priority = 3, description = "Selecting a dropdown option should update the field")
    public void testDropdownSelection() {
        ExtentReportManager.createTest("TC-F03 : Dropdown Selection");

        // The old check passed if *anything* in the page source changed.
        String choice = FormPage.DROPDOWN_OPTIONS[1]; // "Appium is awesome"
        formPage.selectDropdownOption(choice);
        Assert.assertEquals(formPage.getDropdownValue(), choice,
            "The dropdown should show the option that was picked");
        ExtentReportManager.getTest().info("Dropdown now shows: " + choice);

        String other = FormPage.DROPDOWN_OPTIONS[2];
        formPage.selectDropdownOption(other);
        Assert.assertEquals(formPage.getDropdownValue(), other, "Picking again should replace the value");
    }

    // ------------------------------------------------------------------ //
    //  TC-F04 : Switch label follows the switch                           //
    //  (v2.2.0 has no slider; the old TC-F04 passed by only checking that  //
    //  the Forms screen was still there)                                   //
    // ------------------------------------------------------------------ //
    @Test(priority = 4, description = "The label next to the switch says what the next tap will do")
    public void testSwitchLabelFollowsState() {
        ExtentReportManager.createTest("TC-F04 : Switch label");

        boolean on = formPage.isSwitchOn();
        Assert.assertEquals(formPage.getSwitchText(),
            on ? "Click to turn the switch OFF" : "Click to turn the switch ON");
        formPage.tapSwitch();
        Assert.assertEquals(formPage.getSwitchText(),
            on ? "Click to turn the switch ON" : "Click to turn the switch OFF");
        formPage.tapSwitch(); // leave it as it was
    }

    // ------------------------------------------------------------------ //
    //  TC-F05 : Text input accepts alphanumeric characters                 //
    // ------------------------------------------------------------------ //
    @Test(priority = 5, description = "Text input should accept alphanumeric characters")
    public void testTextInputAlphanumeric() {
        ExtentReportManager.createTest("TC-F05 : Alphanumeric Text Input");

        String testText = "Test123";
        formPage.enterTextInput(testText);

        Assert.assertEquals(formPage.getTextInputResult(), testText,
            "Text input should accept alphanumeric text");
    }
}
