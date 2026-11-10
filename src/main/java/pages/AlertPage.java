package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import utils.WaitUtils;

/**
 * Native Android dialogs raised by the WDIO demo app.
 *
 * <ul>
 *   <li>Forms screen, <b>Active</b> button: "This button is" / "This button is active",
 *       with three buttons - ASK ME LATER (neutral), CANCEL (negative), OK (positive).</li>
 *   <li>Forms screen, <b>Inactive</b> button: disabled, no dialog.</li>
 *   <li>Login with valid input: "Success" / "You are logged in!" with OK.</li>
 * </ul>
 *
 * The previous version of this page searched every screen for "Alert / Confirm / Prompt"
 * buttons, which belong to a different demo app (ApiDemos) and do not exist here, so all
 * five alert tests could only ever be skipped.
 */
public class AlertPage {

    private final AndroidDriver driver;

    // Standard AlertDialog ids
    private static final By TITLE    = By.id("android:id/alertTitle");
    private static final By MESSAGE  = By.id("android:id/message");
    private static final By POSITIVE = By.id("android:id/button1"); // OK
    private static final By NEGATIVE = By.id("android:id/button2"); // CANCEL
    private static final By NEUTRAL  = By.id("android:id/button3"); // ASK ME LATER

    public static final String ACTIVE_BUTTON   = "button-Active";
    public static final String INACTIVE_BUTTON = "button-Inactive";

    public AlertPage(AndroidDriver driver) {
        this.driver = driver;
    }

    /** Scrolls the Forms screen until the button with this accessibility id is on screen, then taps it. */
    public void tapFormsButton(String accessibilityId) {
        WebElement btn;
        try {
            btn = driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true))"
                    + ".scrollIntoView(new UiSelector().description(\"" + accessibilityId + "\"))"));
        } catch (Exception notScrollable) {
            btn = WaitUtils.waitForVisible(driver, AppiumBy.accessibilityId(accessibilityId), 10);
        }
        btn.click();
    }

    public boolean isDialogShown(int seconds) {
        try {
            WaitUtils.waitForVisible(driver, POSITIVE, seconds);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Immediate check, for "no dialog must appear" assertions. */
    public boolean isDialogPresentNow() {
        return !driver.findElements(POSITIVE).isEmpty();
    }

    public String title() {
        return textOrEmpty(TITLE);
    }

    public String message() {
        return textOrEmpty(MESSAGE);
    }

    public String buttonText(String which) {
        By b = "ok".equals(which) ? POSITIVE : "cancel".equals(which) ? NEGATIVE : NEUTRAL;
        return textOrEmpty(b);
    }

    public void tapOk()         { tap(POSITIVE); }
    public void tapCancel()     { tap(NEGATIVE); }
    public void tapAskMeLater() { tap(NEUTRAL); }

    /** Waits until the dialog has gone. */
    public boolean isDialogGone(int seconds) {
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < end) {
            if (!isDialogPresentNow()) return true;
            sleep(300);
        }
        return !isDialogPresentNow();
    }

    private void tap(By button) {
        try {
            WaitUtils.waitForClickable(driver, button, 8).click();
        } catch (Exception ignored) {
            // no dialog open - nothing to close
        }
    }

    private String textOrEmpty(By by) {
        try {
            String t = WaitUtils.waitForVisible(driver, by, 5).getText();
            return t == null ? "" : t;
        } catch (Exception e) {
            return "";
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
