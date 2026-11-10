package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import utils.WaitUtils;

import java.util.List;
import java.util.Map;

/**
 * Login screen — WDIO Native Demo App.
 *
 * ROOT CAUSE of "Cannot find login-btn":
 *   After typing, the soft keyboard covers the bottom of the screen. On some
 *   React Native KeyboardAvoidingView configurations the login button is pushed
 *   completely off-screen and the UiAutomator2 accessibility tree drops it.
 *   AppiumBy.accessibilityId("login-btn") therefore returns no match.
 *
 * FIX — 6-level fallback strategy in findLoginButton():
 *   1. Dismiss keyboard (hideKeyboard + back-key via KEYCODE_111).
 *   2. Scroll the page down to push login button into view.
 *   3. Try accessibilityId("login-btn") — official testID.
 *   4. Try text variants: "LOGIN", "Log In", etc.
 *   5. Try any clickable ViewGroup in the bottom 40 % of the screen.
 *   6. Coordinate-tap at 65 % screen height (button's typical Y position).
 *
 * Additionally: the login() method first tries Keys.ENTER on the
 * password field (submits the form if onSubmitEditing is wired up) and
 * falls back to tapLogin() if the form was not submitted.
 */
public class LoginPage {

    private final AndroidDriver driver;

    private static final By EMAIL_INPUT    = AppiumBy.accessibilityId("input-email");
    private static final By PASSWORD_INPUT = AppiumBy.accessibilityId("input-password");
    private static final By LOGIN_BUTTON   = AppiumBy.accessibilityId("login-btn");
    private static final By ERROR_MESSAGE  = AppiumBy.accessibilityId("generic-error-message");

    public LoginPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // ------------------------------------------------------------------ //
    //  Screen identity                                                     //
    // ------------------------------------------------------------------ //

    public boolean isLoginScreenDisplayed() {
        // Round 14 root-cause fix:
        //
        // CONFIRMED: mobile:isKeyboardShown ALWAYS returns false on this emulator
        // (hardware keyboard mode). React Native's KeyboardAvoidingView still fires
        // keyboard events when a hardware keyboard is active, causing the email field
        // to be marked displayed=false by UiAutomator2.  getPageSource() omits
        // elements with displayed=false, so the page-source check also fails.
        // waitForVisible uses ExpectedConditions.visibilityOf which also requires
        // displayed=true — so all previous strategies miss the element.
        //
        // FIX: driver.findElement() in UiAutomator2 searches the FULL accessibility
        // tree and finds elements regardless of displayed status.  We use a fast
        // retry loop (20 × 500 ms = 10 s total) that calls findElement for both
        // EMAIL_INPUT and PASSWORD_INPUT.  The first successful find returns true
        // immediately.  This works whether the keyboard is up or not.
        //
        // Keyboard-dismiss attempt is kept as a best-effort beforehand; it no longer
        // gates the result.
        try {
            Object r = driver.executeScript("mobile: isKeyboardShown");
            if (Boolean.TRUE.equals(r)) {
                driver.executeScript("mobile: pressKey", Map.of("keycode", 4));
                Thread.sleep(800);
                System.out.println("[LoginPage] keyboard dismissed (isKeyboardShown=true)");
            } else {
                // Hardware keyboard: isKeyboardShown is false but layout may still be
                // reflowed. Try ESCAPE just in case (closes soft KB without back-nav).
                driver.executeScript("mobile: pressKey", Map.of("keycode", 111));
                Thread.sleep(500);
            }
        } catch (Exception ignored) {}

        // Primary: findElement retry (works regardless of displayed status).
        for (int i = 0; i < 20; i++) {
            try {
                driver.findElement(EMAIL_INPUT);
                System.out.println("[LoginPage] EMAIL_INPUT found via findElement (attempt " + (i+1) + ")");
                return true;
            } catch (Exception ignored) {}
            try {
                driver.findElement(PASSWORD_INPUT);
                System.out.println("[LoginPage] PASSWORD_INPUT found via findElement (attempt " + (i+1) + ")");
                return true;
            } catch (Exception ignored) {}
            sleep(500);
        }

        // Fallback: waitForVisible in case displayed becomes true (e.g., keyboard closed)
        try { WaitUtils.waitForVisible(driver, EMAIL_INPUT, 5); return true; }
        catch (Exception ignored) {}
        try { WaitUtils.waitForVisible(driver, PASSWORD_INPUT, 3); return true; }
        catch (Exception ignored) {}

        // R20 fallbacks: when keyboard events keep email/password in displayed=false,
        // all findElement and waitForVisible checks above fail.
        //
        // Fallback A: UiSelector — UIAutomator2-native query that may bypass the
        // displayed=false filter (unlike Appium's accessibilityId/XPath strategies).
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                "new UiSelector().description(\"input-email\")"));
            System.out.println("[LoginPage] Login screen confirmed via UiSelector (email)");
            return true;
        } catch (Exception ignored) {}
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                "new UiSelector().description(\"input-password\")"));
            System.out.println("[LoginPage] Login screen confirmed via UiSelector (password)");
            return true;
        } catch (Exception ignored) {}

        // Fallback B: selected Login tab.
        // (R19 showed this also has displayed=false during keyboard layout passes;
        // kept as final safety net in case UiSelector also fails.)
        try {
            driver.findElement(By.xpath(
                "//*[@selected='true' and @content-desc='Login']"));
            System.out.println("[LoginPage] Login screen confirmed via selected Login tab");
            return true;
        } catch (Exception ignored) {}

        System.out.println("[LoginPage] isLoginScreenDisplayed() could not detect Login screen");
        return false;
    }

    // ------------------------------------------------------------------ //
    //  Input actions                                                       //
    // ------------------------------------------------------------------ //

    public void enterEmail(String email) {
        WebElement input = WaitUtils.waitForVisible(driver, EMAIL_INPUT, 15);
        input.clear();
        input.sendKeys(email);
    }

    public void enterPassword(String password) {
        WebElement input = WaitUtils.waitForVisible(driver, PASSWORD_INPUT, 15);
        input.clear();
        input.sendKeys(password);
    }

    // ------------------------------------------------------------------ //
    //  Login button — multi-strategy                                       //
    // ------------------------------------------------------------------ //

    /**
     * Taps the login button using up to 6 fallback strategies.
     */
    public void tapLogin() {
        // R29: CONFIRMED ROOT CAUSE — tap password TextInput first to shift focus.
        //
        // R28 debug revealed the exact issue:
        //   "[LoginPage] R28: login-btn NOT found after blur tap — KAV may still be active"
        //   "[LoginPage] btn coords: x=540 y=1573"
        //
        //   Login button IS at y=1573 in the accessibility tree (found by UiSelector).
        //   But tapping y=1573 does nothing because:
        //
        //   After enterEmail(), email TextInput has focus → React Native fires
        //   keyboardDidShow → KAV treats the bottom ~900px (y≈1440–2340) as keyboard
        //   zone → React Native's touch handler ONLY processes events within y=0–1440.
        //   The login button at y=1573 is OUTSIDE this touch area → clickGesture at
        //   y=1573 reaches the physical screen but React Native ignores it.
        //   Blank-area taps at y=1700 also fail (y=1700 > 1440 → also outside).
        //
        //   PROOF from testEmptyEmailShowsError (which PASSES):
        //   After enterPassword(), password TextInput has focus (y≈700, inside the
        //   touch area). KAV adjusts relative to password position → login button
        //   at y=1573 IS accessible because KAV's offset is smaller (password is
        //   already near the bottom of the visible form, so less upward shift needed).
        //   dismissKeyboard + blank-area tap → login-btn found → form submitted → ✓
        //
        // R29 FIX: tap password TextInput at start of tapLogin() to shift focus
        //   email.onFocus → password.onFocus
        //   (email.onBlur fires → password becomes the focused field → KAV re-evaluates
        //   with password as focal point → login button at y=1573 IS accessible)
        //   Then dismissKeyboard + blank-area tap deactivate KAV fully → login-btn
        //   strategy 1 finds it → clickGesture succeeds → empty password error shown.
        //
        //   Safe for all tests:
        //   - testValidLogin / testInvalidEmailFormat: enterEmail + enterPassword already
        //     left password focused; tapping it again is a no-op (re-focuses same field)
        //   - testEmptyEmailShowsError: password already focused; same behaviour
        //   - testBothFieldsEmpty: no field focused; tapping password focuses it (fine,
        //     password is still empty when form is submitted)
        try {
            WebElement pwd = driver.findElement(PASSWORD_INPUT);
            int px = pwd.getLocation().getX() + pwd.getSize().getWidth()  / 2;
            int py = pwd.getLocation().getY() + pwd.getSize().getHeight() / 2;
            driver.executeScript("mobile: clickGesture", Map.of("x", px, "y", py));
            System.out.println("[LoginPage] R29: tapped password field at (" + px + "," + py + ") — focus shifted from email");
            sleep(600);
        } catch (Exception e) {
            System.out.println("[LoginPage] R29: password field tap failed: " + e.getMessage() + " — trying coordinate fallback");
            try {
                // Approximate y for password TextInput on this 2340px screen
                driver.executeScript("mobile: clickGesture", Map.of("x", 540, "y", 700));
                sleep(600);
            } catch (Exception ignored) {}
        }

        dismissKeyboard();

        // Blank-area tap: now that password has focus (at y≈700), y=1700 is in the
        // React Native touch area (KAV with password-focus has smaller offset than
        // email-focus). Fires password.onBlur() → KAV fully deactivates → login-btn
        // at y=1573 returned to displayed=true and within touch area.
        try {
            driver.executeScript("mobile: clickGesture", Map.of("x", 540, "y", 1700));
            sleep(1500);
        } catch (Exception ignored) {}

        // Debug: confirm login-btn is now accessible
        try {
            driver.findElement(LOGIN_BUTTON);
            System.out.println("[LoginPage] R29: login-btn accessible after password-focus + blur sequence ✓");
        } catch (Exception e) {
            System.out.println("[LoginPage] R29: login-btn still not found — falling through to strategies");
        }

        WebElement btn = findLoginButton();

        if (btn != null) {
            try {
                int x = btn.getLocation().getX() + btn.getSize().getWidth()  / 2;
                int y = btn.getLocation().getY() + btn.getSize().getHeight() / 2;
                System.out.println("[LoginPage] btn coords: x=" + x + " y=" + y);
                if (x > 0 && y > 200 && y < 1800) {
                    driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
                    return;
                }
                System.out.println("[LoginPage] btn coords out of form area — trying direct click");
            } catch (Exception e) {
                System.out.println("[LoginPage] clickGesture failed: " + e.getMessage());
            }
            try { btn.click(); return; }
            catch (Exception e) { System.out.println("[LoginPage] direct click failed: " + e.getMessage()); }
        }

        // Strategy C: Keys.ENTER on password field
        try { driver.findElement(PASSWORD_INPUT).sendKeys(Keys.ENTER); return; }
        catch (Exception ignored) {}

        // Strategy D: fixed coordinate at login button position (y=1573 confirmed from R28)
        try {
            Dimension size = driver.manage().window().getSize();
            int x = size.getWidth() / 2;
            int y = (int)(size.getHeight() * 0.67); // y≈1568 — matches R28 observed y=1573
            System.out.println("[LoginPage] Strategy D: coordinate tap at (" + x + "," + y + ")");
            driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
        } catch (Exception e) {
            System.out.println("[LoginPage] coordinate tap failed: " + e.getMessage());
        }
    }

    /**
     * Dismisses the soft keyboard through multiple mechanisms.
     */
    private void dismissKeyboard() {
        try { driver.hideKeyboard(); } catch (Exception ignored) {}
        sleep(500);
        // Explicitly press KEYCODE_ESCAPE (111) which closes the keyboard on Android
        // without navigating back in the app.
        try {
            driver.executeScript("mobile: pressKey", Map.of("keycode", 111));
        } catch (Exception ignored) {}
        sleep(1500); // give UI time to re-layout
    }

    /**
     * Finds the login button with 5 locator strategies.
     * Dumps a page-source excerpt on first failure so we can see the real IDs.
     *
     * R28 changes:
     *   - Removed scrollGesture: the login form is not a scrollable container.
     *     Scrolling shifted element Y positions, making clickGesture miss even when
     *     the element was found. It also risked re-triggering KAV focus events.
     *   - Strategy 1: added 500ms retry (layout may still be settling after blur tap).
     *   - Strategy 3: changed to exact @content-desc='login-btn' match. The old partial
     *     match also matched the nav bar "Login" tab (content-desc="Login"), which
     *     was then returned and clicked as a no-op (already on Login screen).
     *   - Strategy 4: bounded to Y 20%–55% of screen. The old lower-bound-only check
     *     (threshold = 60% = y≈1404) matched nav bar tabs at y≈2183, causing the
     *     Login tab to be returned instead of the actual login button.
     */
    private WebElement findLoginButton() {
        // 1. accessibilityId "login-btn" (official testID) — should succeed when KAV inactive
        try { return driver.findElement(LOGIN_BUTTON); }
        catch (Exception ignored) {}

        // Retry once: KAV layout settling can take up to ~500ms after onBlur
        sleep(500);
        try { return driver.findElement(LOGIN_BUTTON); }
        catch (Exception ignored) {}

        // 2. Text variants used in different app versions
        for (String text : new String[]{"LOGIN", "Login", "Log In", "SIGN IN", "Sign In", "SUBMIT"}) {
            try {
                return driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + text + "\")"));
            } catch (Exception ignored) {}
        }

        // 3. XPath EXACT content-desc match (avoids matching nav bar "Login" tab)
        try {
            return driver.findElement(By.xpath("//*[@content-desc='login-btn']"));
        } catch (Exception ignored) {}

        // 4. Any clickable element in the FORM area (Y: 20%–55% of screen height).
        // R28: bounded above and below to exclude:
        //   - status bar / header (y < 20%)
        //   - nav bar tabs at y≈2183 (y > 55%) which are clickable and were
        //     previously returned as false positives by the old lower-bound-only check.
        try {
            Dimension size = driver.manage().window().getSize();
            int loY = (int)(size.getHeight() * 0.20); // ~468 on 2340px screen
            int hiY = (int)(size.getHeight() * 0.55); // ~1287 on 2340px screen
            List<WebElement> elements = driver.findElements(By.xpath("//*[@clickable='true']"));
            for (WebElement el : elements) {
                int ey = el.getLocation().getY();
                if (ey > loY && ey < hiY) {
                    System.out.println("[LoginPage] Heuristic button found at y=" + ey
                        + " text=" + el.getText()
                        + " desc=" + el.getAttribute("content-desc"));
                    return el;
                }
            }
        } catch (Exception ignored) {}

        // 5. UiScrollable fallback
        try {
            return driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true).instance(0))" +
                ".scrollIntoView(new UiSelector().description(\"login-btn\").instance(0))"));
        } catch (Exception ignored) {}

        // Debug dump — helps identify the real element IDs
        try {
            String src = driver.getPageSource();
            int end = Math.min(src.length(), 4000);
            System.out.println("[LoginPage] DUMP (first 4000 chars):\n" + src.substring(0, end));
        } catch (Exception ignored) {}

        System.out.println("[LoginPage] Cannot find login button with any strategy!");
        return null;
    }

    // ------------------------------------------------------------------ //
    //  High-level login                                                    //
    // ------------------------------------------------------------------ //

    public LoginPage login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        tapLogin();
        return this;
    }

    // ------------------------------------------------------------------ //
    //  Assertions                                                          //
    // ------------------------------------------------------------------ //

    /** Validation texts the WDIO demo app shows under the fields. */
    public static final String EMAIL_ERROR    = "Please enter a valid email address";
    public static final String PASSWORD_ERROR = "Please enter at least 8 characters";

    /**
     * True when the given validation message is on screen.
     * (The old check also accepted any page source containing "error" or "Invalid",
     * which made a negative test pass without the app showing any message, and it
     * did not know the password message at all, so TC-L03 always failed.)
     */
    public boolean isValidationMessageShown(String message) {
        try {
            WaitUtils.waitForVisible(driver, AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + message + "\")"), 6);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isEmailErrorShown()    { return isValidationMessageShown(EMAIL_ERROR); }
    public boolean isPasswordErrorShown() { return isValidationMessageShown(PASSWORD_ERROR); }

    /** Immediate check (no wait) for "this message must NOT be shown" assertions. */
    public boolean isValidationMessagePresentNow(String message) {
        return !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().text(\"" + message + "\")")).isEmpty();
    }

    public boolean isErrorMessageDisplayed() {
        return isEmailErrorShown() || isPasswordErrorShown();
    }

    public String getErrorMessageText() {
        try { return driver.findElement(ERROR_MESSAGE).getText(); }
        catch (Exception e) { return ""; }
    }

    public boolean isLoginSuccessful() {
        try { WaitUtils.waitForTextInSource(driver, "You are logged in", 10); return true; }
        catch (Exception e) { return false; }
    }

    /** Closes the "Success / You are logged in!" dialog so the next test starts clean. */
    public void dismissSuccessDialog() {
        new AlertPage(driver).tapOk();
    }

    public void clearEmailField() {
        WaitUtils.waitForVisible(driver, EMAIL_INPUT, 10).clear();
    }

    public void clearPasswordField() {
        WaitUtils.waitForVisible(driver, PASSWORD_INPUT, 10).clear();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
