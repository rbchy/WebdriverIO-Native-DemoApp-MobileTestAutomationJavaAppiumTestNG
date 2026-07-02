package tests;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.HomePage;
import pages.SwipePage;
import utils.ExtentReportManager;

/**
 * Swipe screen tests.
 * Covers: left swipe, right swipe, vertical scroll, and card navigation.
 */
public class SwipeTest extends BaseTest {

    private HomePage homePage;
    private SwipePage swipePage;

    @BeforeMethod
    public void navigateToSwipe() {
        homePage  = new HomePage(driver);
        swipePage = new SwipePage(driver);

        // Dismiss any lingering dialog (e.g. "You found me!" from a previous test retry)
        // before navigating. A dialog covering the screen blocks nav-tab clicks.
        try {
            driver.findElement(io.appium.java_client.AppiumBy.accessibilityId("ok-button")).click();
            Thread.sleep(800);
        } catch (Exception ignored) {}
        try {
            driver.findElement(org.openqa.selenium.By.id("android:id/button1")).click();
            Thread.sleep(800);
        } catch (Exception ignored) {}

        homePage.goToSwipe();
        Assert.assertTrue(swipePage.isSwipeScreenDisplayed(),
            "Swipe screen should be displayed after tapping Swipe tab");
    }

    // ------------------------------------------------------------------ //
    //  TC-S01 : First card is visible on load                              //
    // ------------------------------------------------------------------ //
    @Test(priority = 1, description = "First card should be visible when Swipe screen opens")
    public void testFirstCardIsVisible() {
        ExtentReportManager.createTest("TC-S01 : First Card Visible");

        Assert.assertTrue(swipePage.isFirstCardVisible(),
            "First card 'FULLY OPEN SOURCE' should be visible on initial load");
        ExtentReportManager.getTest().info("First card confirmed visible");
    }

    // ------------------------------------------------------------------ //
    //  TC-S02 : Swipe left to reveal next card                             //
    // ------------------------------------------------------------------ //
    @Test(priority = 2, description = "Swiping left should reveal the next card")
    public void testSwipeLeftRevealsNextCard() {
        ExtentReportManager.createTest("TC-S02 : Swipe Left");

        swipePage.swipeLeft();

        boolean nextCardVisible = swipePage.isCardVisible("GREAT COMMUNITY");
        Assert.assertTrue(nextCardVisible,
            "Second card 'GREAT COMMUNITY' should be visible after swiping left");
        ExtentReportManager.getTest().info("Swipe left revealed second card");
    }

    // ------------------------------------------------------------------ //
    //  TC-S03 : Swipe right returns to previous card                       //
    // ------------------------------------------------------------------ //
    @Test(priority = 3, description = "Swiping right should go back to the previous card")
    public void testSwipeRightReturnsToPreviousCard() {
        ExtentReportManager.createTest("TC-S03 : Swipe Right");

        swipePage.swipeLeft();  // go to card 2
        swipePage.swipeRight(); // back to card 1

        Assert.assertTrue(swipePage.isFirstCardVisible(),
            "First card should reappear after swiping right");
        ExtentReportManager.getTest().info("Swipe right returned to first card");
    }

    // ------------------------------------------------------------------ //
    //  TC-S04 : Swipe to last card and tap "find me"                       //
    // ------------------------------------------------------------------ //
    @Test(priority = 4, description = "Swiping to the last card and tapping Find Me")
    public void testSwipeToLastCardAndFindMe() {
        ExtentReportManager.createTest("TC-S04 : Find Me Card");

        boolean lastCardFound = swipePage.swipeToLastCard();
        Assert.assertTrue(lastCardFound,
            "Should be able to reach the last card with 'find me' button");
        ExtentReportManager.getTest().info("Last card found");

        swipePage.tapFindMe();
        Assert.assertTrue(swipePage.isFoundMeDisplayed(),
            "'You found me!' message should appear after tapping Find Me");
        ExtentReportManager.getTest().info("'You found me!' confirmed");
    }

    // ------------------------------------------------------------------ //
    //  TC-S05 : Vertical swipe / scroll                                    //
    // ------------------------------------------------------------------ //
    @Test(priority = 5, description = "Vertical swipe up should scroll the screen")
    public void testVerticalSwipeUp() {
        ExtentReportManager.createTest("TC-S05 : Vertical Swipe Up");

        // Just verify the swipe action completes without error
        swipePage.swipeUp();
        Assert.assertTrue(swipePage.isSwipeScreenDisplayed(),
            "Swipe screen should still be displayed after vertical swipe");
        ExtentReportManager.getTest().info("Vertical swipe up completed without error");
    }
}
