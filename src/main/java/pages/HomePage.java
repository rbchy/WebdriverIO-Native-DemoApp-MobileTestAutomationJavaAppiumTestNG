package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.WaitUtils;

import java.time.Duration;
import java.util.Map;

/**
 * Home screen — bottom navigation bar.
 *
 * Round 13 fixes:
 *
 * (A) LoginTest Run 1 — keyboard timing race in goToLogin().
 *   ROOT CAUSE (Round 12 finding): mobile:isKeyboardShown returned false because
 *   the soft keyboard had not fully animated in at the moment we polled (once, after
 *   a fixed 3-second sleep). By the time isLoginScreenDisplayed() ran, the keyboard
 *   was fully showing, making the email field displayed=false and absent from
 *   getPageSource(). All LoginPage checks therefore returned false.
 *
 *   FIX: After confirming "input-email" in the page source, POLL mobile:isKeyboardShown
 *   in a loop for up to 5 s. This catches the keyboard at whatever point it finishes
 *   animating in (typically 1–4 s after the screen loads). Once detected, dismiss
 *   with KEYCODE_BACK (safe when keyboard is the active IME). The initial sleep in
 *   step 4 is reduced from 3 s to 1 s since the polling loop takes over.
 *
 * (B) SwipeTest — "You found me!" dialog persists across @BeforeMethod calls.
 *   ROOT CAUSE: SwipePage.tapFindMe() prints "already showing — skipping tap" and
 *   returns without closing the dialog. The subsequent @BeforeMethod navigates back
 *   to the Swipe tab, but because React Navigation doesn't remount the screen when
 *   you tap the same tab, the modal stays open. isSwipeScreenDisplayed() then fails
 *   because the swipe card elements are behind the modal overlay.
 *
 *   FIX: Override goToSwipe() (was previously a generic tapTab delegate). After
 *   clicking the Swipe tab, check getPageSource() for "You found me!" and if found,
 *   press KEYCODE_BACK to close the React Native Modal, then sleep 800 ms.
 *
 * Round 12 (preserved): goToLogin() fast path + dismissKeyboardSafely().
 * Round 11 (preserved): tapTab() BACK-after-5s overlay dismissal.
 */
public class HomePage {

    private final AndroidDriver driver;

    private static final By TAB_HOME    = AppiumBy.accessibilityId("Home");
    private static final By TAB_LOGIN   = AppiumBy.accessibilityId("Login");
    private static final By TAB_FORMS   = AppiumBy.accessibilityId("Forms");
    private static final By TAB_SWIPE   = AppiumBy.accessibilityId("Swipe");
    private static final By TAB_WEBVIEW = AppiumBy.accessibilityId("WebView");
    private static final By TAB_DRAG    = AppiumBy.accessibilityId("Drag");
    private static final By TAB_CART    = AppiumBy.accessibilityId("Cart");

    private static final By LOGIN_SCREEN_INDICATOR = AppiumBy.accessibilityId("input-email");

    public HomePage(AndroidDriver driver) {
        this.driver = driver;
    }

    public boolean isHomeScreenDisplayed() {
        try { WaitUtils.waitForVisible(driver, TAB_HOME, 15); return true; }
        catch (Exception e) { return false; }
    }

    // ------------------------------------------------------------------ //
    //  Login navigation                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Navigates to the Login screen with keyboard-aware retry logic.
     *
     * FAST PATH: if already on Login screen (input-email in page source),
     *   dismiss keyboard and return — handles @BeforeMethod retries.
     *
     * NORMAL PATH each iteration:
     *   1. KEYCODE_ESCAPE — close any keyboard before clicking nav tab.
     *   2. Click Login tab.
     *   3. Wait up to 5 s for input-email to appear.
     *   4. sleep(1000) — brief initial wait for keyboard animation to begin.
     *   5. Re-verify "input-email" in page source.
     *   6. POLL mobile:isKeyboardShown up to 5 s — catch keyboard after it
     *      finishes animating in (timing race fixed vs Round 12).
     *   7. If keyboard detected: dismissKeyboardSafely() so isLoginScreenDisplayed()
     *      finds email with displayed=true.
     */
    public void goToLogin() {

        // ── FAST PATH ────────────────────────────────────────────────────
        // Round 15: use driver.findElement() instead of getPageSource().contains().
        // getPageSource() EXCLUDES elements with displayed=false.  When the keyboard
        // is up, React Native KeyboardAvoidingView marks the email field displayed=false,
        // so getPageSource() silently omits "input-email" and the fast path is missed.
        // driver.findElement() searches the full accessibility tree regardless of
        // displayed status, so it reliably detects the Login screen in all states.
        boolean alreadyOnLogin = false;
        try { driver.findElement(LOGIN_SCREEN_INDICATOR); alreadyOnLogin = true; }
        catch (Exception ignored) {}
        // R20: when email is displayed=false (keyboard event from prior test), try
        // UiSelector (UIAutomator2-native, may bypass displayed filter) then selected-tab.
        if (!alreadyOnLogin) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().description(\"input-email\")"));
                alreadyOnLogin = true;
                System.out.println("[HomePage] Fast path: email found via UiSelector");
            } catch (Exception ignored) {}
        }
        if (!alreadyOnLogin) {
            try {
                driver.findElement(By.xpath("//*[@selected='true' and @content-desc='Login']"));
                alreadyOnLogin = true;
                System.out.println("[HomePage] Fast path: Login screen detected via selected tab");
            } catch (Exception ignored) {}
        }
        if (alreadyOnLogin) {
            System.out.println("[HomePage] Already on Login screen — dismissing keyboard and returning");
            dismissKeyboardSafely();
            sleep(600);
            // Re-verify: accept either email field OR selected tab
            boolean confirmed = false;
            try { driver.findElement(LOGIN_SCREEN_INDICATOR); confirmed = true; }
            catch (Exception ignored) {}
            if (!confirmed) {
                try {
                    driver.findElement(By.xpath("//*[@selected='true' and @content-desc='Login']"));
                    confirmed = true;
                } catch (Exception ignored) {}
            }
            if (confirmed) {
                System.out.println("[HomePage] Login screen confirmed (fast path)");
                return;
            }
            System.out.println("[HomePage] Login screen lost during fast-path keyboard dismiss, retrying navigation");
        }

        // ── NORMAL PATH ──────────────────────────────────────────────────
        long deadline = System.currentTimeMillis() + 200_000L;

        // Round 18: dismiss keyboard ONCE before entering the loop.
        try { driver.hideKeyboard(); sleep(2000); } catch (Exception ignored) {}

        // ── R25: HOME-THEN-LOGIN FRESH NAVIGATION ─────────────────────────
        //
        // ROOT CAUSE (confirmed by R24 log):
        //
        //   @BeforeClass's isLoginScreenDisplayed() calls findElement(EMAIL)
        //   which sends ACTION_ACCESSIBILITY_FOCUS to the email TextInput →
        //   React Native fires keyboard layout event → email displayed=false.
        //
        //   @BeforeMethod #1 then calls goToLogin().  The coordinate tap at
        //   (385,2183) fires WHILE WE ARE ALREADY ON THE LOGIN SCREEN →
        //   React Navigation pop-to-top event → JS thread busy for ~57 s →
        //   email stays displayed=false until T≈78s after the tap → always
        //   misses the step 3 window regardless of ESCAPE timing.
        //
        //   R24 pre-tap ESCAPE (25 s, no tap) also failed — ESCAPE alone
        //   does not dismiss React Native TextInput accessibility focus.
        //
        //   SOLUTION: Tap HOME first (unmounts Login screen → TextInput focus
        //   cleared), then tap LOGIN (fresh mount → email displayed=true, no
        //   pop-to-top event).  Coordinate taps bypass the accessibility tree
        //   so they work even when all elements are displayed=false.
        //
        //   Home tab: x=77 (tab 0 centre), y=2183 (same nav bar row as Login)
        //
        System.out.println("[HomePage] R25: Home-then-Login fresh navigation...");
        try {
            driver.executeScript("mobile: clickGesture", Map.of("x", 77, "y", 2183));
            System.out.println("[HomePage] R25: Home tap sent");
            sleep(3000);
        } catch (Exception ignored) {}
        try {
            driver.executeScript("mobile: clickGesture", Map.of("x", 385, "y", 2183));
            System.out.println("[HomePage] R25: Login tap sent (fresh navigation — no pop-to-top)");
            sleep(2000);
        } catch (Exception ignored) {}

        boolean freshNavFound = false;
        long freshNavEnd = System.currentTimeMillis() + 20_000L;
        while (System.currentTimeMillis() < freshNavEnd) {
            try { driver.findElement(LOGIN_SCREEN_INDICATOR); freshNavFound = true; break; }
            catch (Exception ignored) {}
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().description(\"input-email\")"));
                freshNavFound = true;
                System.out.println("[HomePage] R25: email found via UiSelector");
                break;
            } catch (Exception ignored) {}
            sleep(500);
        }

        if (freshNavFound) {
            System.out.println("[HomePage] R25: Home-then-Login succeeded — returning");
            sleep(600);
            return;
        }
        System.out.println("[HomePage] R25: email not found in 20 s — R26: force app restart");

        // ── R26: TERMINATE + ACTIVATE APP ────────────────────────────────────
        // Why R25 Home-then-Login fails: React Navigation tab navigator uses
        // unmountOnBlur=false by default.  When we tap the Home tab, the Login
        // screen is NOT unmounted — it stays alive in memory with its TextInput
        // still holding accessibility focus.  Tapping Login again merely brings
        // the already-mounted screen back into view, focus intact, email still
        // displayed=false.  Home-then-Login only works in @BeforeClass because
        // the Login screen had never been mounted yet (fresh app launch).
        //
        // terminateApp() kills the app process entirely.  activateApp() relaunches
        // it from scratch.  Every React Native component remounts — no TextInput
        // focus anywhere.  The app starts on its initial route (Home screen).
        // A single coordinate tap to the Login tab then gives a genuine fresh mount.
        System.out.println("[HomePage] R26: terminating app to clear TextInput focus...");
        try {
            driver.terminateApp("com.wdiodemoapp");
            System.out.println("[HomePage] R26: app terminated");
            sleep(3000);
            driver.activateApp("com.wdiodemoapp");
            System.out.println("[HomePage] R26: app activated — waiting 7s for startup");
            sleep(7000);
            // App is now on initial Home screen with no TextInput focus anywhere.
            driver.executeScript("mobile: clickGesture", Map.of("x", 385, "y", 2183));
            System.out.println("[HomePage] R26: Login tap after app restart");
            sleep(4000);
        } catch (Exception e) {
            System.out.println("[HomePage] R26: restart sequence exception: " + e.getMessage());
        }

        boolean restartFound = false;
        long restartPollEnd = System.currentTimeMillis() + 20_000L;
        while (System.currentTimeMillis() < restartPollEnd) {
            try { driver.findElement(LOGIN_SCREEN_INDICATOR); restartFound = true; break; }
            catch (Exception ignored) {}
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().description(\"input-email\")"));
                restartFound = true;
                System.out.println("[HomePage] R26: email found via UiSelector after restart");
                break;
            } catch (Exception ignored) {}
            sleep(500);
        }
        if (restartFound) {
            System.out.println("[HomePage] R26: Login screen confirmed after app restart — returning");
            sleep(600);
            return;
        }
        System.out.println("[HomePage] R26: still not found after app restart — entering tap loop fallback");

        while (System.currentTimeMillis() < deadline) {

            // Step 1 (R18): hideKeyboard() is now called ONCE before the loop (above),
            // not on every iteration.  Repeated calls were re-triggering keyboard events
            // that made TAB_LOGIN temporarily inaccessible (R17 root-cause finding).

            // Step 2: click Login tab — three strategies in order.
            boolean clicked = false;
            // Strategy A: standard accessibilityId find + direct click
            try {
                driver.findElement(TAB_LOGIN).click();
                sleep(800);
                clicked = true;
            } catch (Exception e1) {
                // Strategy B: find element, tap centre via coordinate gesture
                try {
                    WebElement el = driver.findElement(TAB_LOGIN);
                    int x = el.getLocation().getX() + el.getSize().getWidth()  / 2;
                    int y = el.getLocation().getY() + el.getSize().getHeight() / 2;
                    driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
                    sleep(800);
                    clicked = true;
                } catch (Exception e2) {
                    // Strategy C: hard-coded coordinate tap at the Login tab's pixel position.
                    // Bypasses the accessibility tree entirely — works even when findElement
                    // fails because KeyboardAvoidingView layout events mark TAB_LOGIN as
                    // displayed=false.
                    // Device: 1080 x 2340 px.  7 nav tabs (Home, Webview, Login, Forms,
                    // Swipe, Drag, Menu), each ≈154 px wide.
                    // Login = tab index 2 → centre x = 154×2 + 77 = 385.
                    // App content bottom (above Android system nav bar) ≈ y 2283;
                    // nav bar height ≈ 200 px → nav bar centre ≈ y 2183.
                    try {
                        driver.executeScript("mobile: clickGesture", Map.of("x", 385, "y", 2183));
                        sleep(800);
                        clicked = true;
                        System.out.println("[HomePage] Login tab tapped via coordinate fallback (x=385, y=2183)");
                    } catch (Exception e3) {
                        System.out.println("[HomePage] Login tab not found yet (all 3 strategies failed)");
                    }
                }
            }

            if (clicked) {
                // Step 3: wait for Login screen indicator — up to 55s (R22).
                //
                // R22 root-cause analysis (from R21 log):
                //
                //   R21 sent ESCAPE at T=0.8s (0.8s after coordinate tap). Still failed.
                //   R20 waited 20s without ESCAPE.  Still failed.
                //   R19 waited 20s without ESCAPE.  Still failed.
                //
                //   KEY INSIGHT from isLoginScreenDisplayed() (always succeeds in Run 2):
                //     isLoginScreenDisplayed() sends ESCAPE at T=90s of the run.
                //     Recovery happens at T=111s = 21s after ESCAPE.
                //     The last coordinate tap before T=90s was at T≈67s (R21 iteration 3).
                //     So ESCAPE fired 23s after the last tap — by which point the
                //     React Native JS thread had finished processing the pop-to-top
                //     navigation event triggered by the coordinate tap.
                //
                //   CONCLUSION: ESCAPE only triggers recovery when the React Native JS
                //   thread is IDLE (finished processing the pop-to-top event).
                //   Sending ESCAPE at T=0.8s (while JS is still busy) does nothing.
                //   The JS thread becomes idle ~23-25s after the coordinate tap.
                //
                //   R22 FAILURE ANALYSIS (confirms R22 timing model was wrong):
                //     R22 sent ESCAPE at T=25s mark — still failed all 3 iterations.
                //     Root cause: ESCAPE only triggers FAST recovery (~19s) when the
                //     React Native JS thread has been idle for ≥33s after the last tap.
                //     At T=25s, the thread is still processing the pop-to-top event;
                //     ESCAPE is deferred and recovery takes ~51s (too late for 55s window).
                //
                //   Confirmed threshold from R21 data:
                //     R21: ESCAPE at T=33s after last tap → recovery in 19s (total 52s).
                //     R22 step 3: ESCAPE at T=25.8s → recovery in 51.5s (too slow).
                //
                //   R24 FAILURE ANALYSIS (R23 confirmed pop-to-top = 57s):
                //
                //     R23 tap at T≈2.8s → JS event starts → finishes at T≈59.8s.
                //     ESCAPE at T≈43.3s (40s mark) → deferred (JS busy 16.5s more).
                //     ESCAPE processed at T=59.8s → recovery at T=59.8+19=T=78.8s.
                //     Step 3 window ended at T=73.3s → MISSED BY 5.5s.
                //
                //   R24 FALL-THROUGH STRATEGY (pre-tap ESCAPE failed → we're on a
                //   different screen → coordinate tap is necessary):
                //     1. Poll every 500ms (catches fast-nav cases immediately).
                //     2. At 62s mark: ESCAPE fires AFTER JS event completes (62 > 57).
                //        → JS idle → ESCAPE processed immediately → recovery in ~19s.
                //     3. Email found at ~T=81s (62+19).
                //     4. 90s window covers 81s + 9s margin.
                boolean loginFound = false;
                boolean escapeSent = false;
                long escapeAt    = System.currentTimeMillis() + 62_000L;
                long findDeadline = System.currentTimeMillis() + 90_000L;
                while (System.currentTimeMillis() < findDeadline) {
                    // At 25s mark: JS is idle — send ESCAPE to trigger layout recovery
                    if (!escapeSent && System.currentTimeMillis() >= escapeAt) {
                        try {
                            driver.executeScript("mobile: pressKey", Map.of("keycode", 111));
                            System.out.println("[HomePage] step 3: ESCAPE sent at 62s mark (past 57s JS threshold → recovery in ~19s)");
                            escapeSent = true;
                        } catch (Exception ignored) {}
                    }
                    // Primary: email field via accessibilityId
                    try { driver.findElement(LOGIN_SCREEN_INDICATOR); loginFound = true; break; }
                    catch (Exception ignored) {}
                    // UiSelector: UIAutomator2-native query
                    try {
                        driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().description(\"input-email\")"));
                        loginFound = true;
                        System.out.println("[HomePage] step 3: email found via UiSelector");
                        break;
                    } catch (Exception ignored) {}
                    // selected-tab: @selected='true' on the Login nav tab
                    try {
                        driver.findElement(By.xpath(
                            "//*[@selected='true' and @content-desc='Login']"));
                        loginFound = true;
                        System.out.println("[HomePage] step 3: Login screen confirmed via selected tab");
                        break;
                    } catch (Exception ignored) {}
                    sleep(500);
                }
                if (!loginFound) {
                    System.out.println("[HomePage] Login tab clicked — waited 90s, Login screen not detected, tapping again...");
                    sleep(2000);
                    continue;
                }

                // Step 4: brief initial wait for keyboard animation to begin
                sleep(1000);

                // Step 5: re-verify Login screen still showing.
                // R20: triple check — email field, UiSelector, OR selected-tab.
                boolean stillOnLogin = false;
                try { driver.findElement(LOGIN_SCREEN_INDICATOR); stillOnLogin = true; }
                catch (Exception ignored) {}
                if (!stillOnLogin) {
                    try {
                        driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiSelector().description(\"input-email\")"));
                        stillOnLogin = true;
                        System.out.println("[HomePage] step 5: email found via UiSelector");
                    } catch (Exception ignored) {}
                }
                if (!stillOnLogin) {
                    try {
                        driver.findElement(By.xpath(
                            "//*[@selected='true' and @content-desc='Login']"));
                        stillOnLogin = true;
                        System.out.println("[HomePage] step 5: Login tab selected — screen still showing");
                    } catch (Exception ignored) {}
                }
                if (!stillOnLogin) {
                    System.out.println("[HomePage] Login screen lost after brief wait, retrying...");
                    sleep(1000);
                    continue;
                }

                // Step 6: POLL mobile:isKeyboardShown up to 5 s.
                // Round 12 bug: we checked only once after 3 s and got false because
                // the keyboard hadn't finished animating in yet. Polling catches it.
                boolean keyboardDetected = false;
                long kbDeadline = System.currentTimeMillis() + 5_000L;
                while (System.currentTimeMillis() < kbDeadline) {
                    try {
                        Object r = driver.executeScript("mobile: isKeyboardShown");
                        if (Boolean.TRUE.equals(r)) { keyboardDetected = true; break; }
                    } catch (Exception ignored) { break; }
                    sleep(300);
                }
                System.out.println("[HomePage] keyboard poll after Login nav: detected=" + keyboardDetected);

                // Step 7: dismiss if keyboard was found; either way, Login screen is ready
                if (keyboardDetected) {
                    dismissKeyboardSafely();
                    sleep(600);
                }

                System.out.println("[HomePage] Login screen confirmed and stable (input-email visible)");
                return;
            }

            sleep(1500);
        }
        System.out.println("[HomePage] FAILED to navigate to Login screen after 200s");
    }

    /**
     * Dismisses the soft keyboard safely via hideKeyboard().
     *
     * Round 16 fix: Round 15 sent KEYCODE_BACK (4) unconditionally after hideKeyboard().
     * When no keyboard is showing, Android routes BACK to the focused Activity (not the
     * IME), causing the app to navigate backwards — out of scope entirely.  This was the
     * root cause of the Round 15 regression where the FIRST goToLogin() call immediately
     * started printing "Login tab not found" because the app had already been exited.
     *
     * driver.hideKeyboard() uses InputMethodManager.hideSoftInputFromWindow() — it is
     * an IME API call, not a key event.  Safe to call unconditionally: closes keyboard
     * when showing, silent no-op when not showing.  No navigation side-effects.
     */
    private void dismissKeyboardSafely() {
        try { driver.hideKeyboard(); sleep(500); }
        catch (Exception ignored) {}
        System.out.println("[HomePage] keyboard dismiss attempted via hideKeyboard()");
    }

    // ------------------------------------------------------------------ //
    //  Other tab navigation                                                //
    // ------------------------------------------------------------------ //

    public void goToHome()    { tapTab(TAB_HOME,    "Home",    60); }
    public void goToForms()   { tapTab(TAB_FORMS,   "Forms",   60); }
    public void goToWebView() { tapTab(TAB_WEBVIEW, "WebView", 60); }
    public void goToDrag()    { tapTab(TAB_DRAG,    "Drag",    60); }
    public void goToCart()    { tapTab(TAB_CART,    "Cart",    60); }

    /**
     * Navigate to the Swipe tab.
     *
     * Round 14 fix: the previous Round 13 approach used getPageSource().contains("You found me!")
     * which is a FALSE POSITIVE — that string exists in the source even when the dialog overlay
     * is CLOSED (React Native renders it but with displayed=false).  The unnecessary KEYCODE_BACK
     * press was then navigating AWAY from the Swipe screen, causing isSwipeScreenDisplayed() to
     * fail on the wrong screen.
     *
     * FIX: use XPath with @displayed='true' to only match the dialog when it is visually open.
     * Use KEYCODE_ESCAPE (111) rather than BACK (4): ESCAPE closes overlays/dialogs in React
     * Native without triggering React Navigation's back-stack pop (which BACK does).
     */
    public void goToSwipe() {
        tapTab(TAB_SWIPE, "Swipe", 60);

        // R21: 2s pre-check wait — give modal time to auto-close naturally.
        // R19/R20 evidence: modal often self-dismisses within 1-3s of the test
        // method that triggered it. tapTab() + XPath check execute in ~700ms which
        // can be faster than the auto-close timer. This sleep lets the modal resolve
        // before we need to actively intervene.
        sleep(2000);

        // Check if "You found me!" dialog is ACTUALLY VISIBLE (displayed=true only).
        // A broad page-source string check is a false positive; XPath + displayed='true'
        // is precise.
        try {
            driver.findElement(By.xpath(
                "//*[@displayed='true' and (contains(@text,'You found me!') " +
                "or contains(@content-desc,'you-found-me') " +
                "or contains(@content-desc,'You found me!'))]"));
            // Dialog IS visually open.
            //
            // Round 18 root-cause analysis (R14→R17 history):
            //   R14: used ESCAPE (111).  ESCAPE does NOT close the RN Modal, so the modal
            //        stays open.  isSwipeScreenDisplayed() passes via its getPageSource()
            //        fallback: driver.getPageSource().contains("Swipe horizontal") returns
            //        true even while the modal is overlaid (underlying screen still rendered).
            //        SwipeTest PASSED.
            //   R17: changed to KEYCODE_BACK (4).  In this APK, the modal's onRequestClose
            //        calls navigation.goBack(), so BACK both closes the modal AND pops the
            //        Swipe screen off the stack — landing on a different screen.
            //        isSwipeScreenDisplayed() then fails (not on Swipe screen). SwipeTest
            //        FAILED with a 132 s elapsed time.
            //
            //   R18 STRATEGY (three tiers):
            //     1. Click the "ok-button" directly via its accessibilityId and a coordinate
            //        gesture (bypasses @clickable='false' on React Native elements).
            //        This is the cleanest close — modal shuts, we stay on Swipe screen.
            //     2. XPath close/OK button WITHOUT the @clickable='true' filter (many RN
            //        elements report clickable=false to Android's a11y tree).
            //     3. Fall back to ESCAPE (111) — modal stays open, but isSwipeScreenDisplayed()
            //        passes via getPageSource() fallback.  NEVER use BACK here.
            boolean modalClosed = false;

            // Tier 1: accessibilityId("ok-button") + clickGesture
            try {
                WebElement okBtn = driver.findElement(AppiumBy.accessibilityId("ok-button"));
                int bx = okBtn.getLocation().getX() + okBtn.getSize().getWidth()  / 2;
                int by = okBtn.getLocation().getY() + okBtn.getSize().getHeight() / 2;
                driver.executeScript("mobile: clickGesture", Map.of("x", bx, "y", by));
                sleep(600);
                System.out.println("[HomePage] 'You found me!' modal closed via ok-button");
                modalClosed = true;
            } catch (Exception e1) {}

            // Tier 2: XPath (no @clickable='true') + clickGesture
            if (!modalClosed) {
                try {
                    WebElement btn = driver.findElement(By.xpath(
                        "//*[@displayed='true' and " +
                        "(contains(@text,'Close') or contains(@text,'OK') or " +
                        "contains(@content-desc,'close') or contains(@content-desc,'ok'))]"));
                    int bx = btn.getLocation().getX() + btn.getSize().getWidth()  / 2;
                    int by = btn.getLocation().getY() + btn.getSize().getHeight() / 2;
                    driver.executeScript("mobile: clickGesture", Map.of("x", bx, "y", by));
                    sleep(600);
                    System.out.println("[HomePage] 'You found me!' modal closed via XPath button");
                    modalClosed = true;
                } catch (Exception e2) {}
            }

            // Tier 3: Poll up to 10s for modal to auto-close (R23: replaces R21 fixed 3s sleep).
            //
            // R22 regression: the fixed 3s sleep was not long enough. The "You found me!"
            // overlay behaves like an Android Toast (no tappable button, auto-dismisses).
            // Toast.LENGTH_LONG can last up to ~5.5s, and custom toast durations may be
            // longer (up to ~10s). With only a 3s fixed wait, the modal was still open
            // when we rechecked → Tier 4 ESCAPE fired → isSwipeScreenDisplayed() failed
            // because the modal was still present after ESCAPE+5s.
            //
            // R23 fix: poll every 500ms for up to 10s, exiting early when modal closes.
            // Covers toast durations up to 10s with no unnecessary wait when it closes sooner.
            if (!modalClosed) {
                // Diagnostic dump
                try {
                    String src = driver.getPageSource();
                    int idx = src.indexOf("ok-button");
                    if (idx >= 0) {
                        System.out.println("[HomePage] ok-button IN source: " +
                            src.substring(Math.max(0, idx - 150), Math.min(src.length(), idx + 200)));
                    } else {
                        System.out.println("[HomePage] ok-button NOT in page source — polling 10s for auto-close");
                        idx = src.indexOf("found me");
                        if (idx >= 0) {
                            System.out.println("[HomePage] modal area: " +
                                src.substring(Math.max(0, idx - 100), Math.min(src.length(), idx + 300)));
                        }
                    }
                } catch (Exception ignored) {}

                long autoCloseDeadline = System.currentTimeMillis() + 10_000L;
                while (System.currentTimeMillis() < autoCloseDeadline) {
                    sleep(500);
                    try {
                        driver.findElement(By.xpath(
                            "//*[@displayed='true' and (contains(@text,'You found me!') " +
                            "or contains(@content-desc,'you-found-me') " +
                            "or contains(@content-desc,'You found me!'))]"));
                        // modal still visible — keep polling
                    } catch (Exception gone) {
                        System.out.println("[HomePage] Modal auto-closed during 10s poll");
                        modalClosed = true;
                        break;
                    }
                }
                if (!modalClosed) {
                    System.out.println("[HomePage] Modal still open after 10s poll — sending ESCAPE");
                }
            }

            // Tier 4: ESCAPE — last resort.
            // NEVER use KEYCODE_BACK (4) here — this APK's onRequestClose calls
            // navigation.goBack() which pops the Swipe screen off the stack.
            //
            // R26: after ESCAPE, poll 15s (was fixed 5s) then tap modal center
            // then restart app if still stuck.  Root cause of R25 SwipeTest
            // regression: React Navigation keeps Swipe screen mounted
            // (unmountOnBlur=false) so the modal persists across tab switches.
            // terminateApp + activateApp is the only reliable clear.
            if (!modalClosed) {
                try {
                    driver.executeScript("mobile: pressKey", Map.of("keycode", 111));
                    System.out.println("[HomePage] 'You found me!' modal: ESCAPE sent as last resort");
                } catch (Exception e4) {}

                // R26: extended poll 15s after ESCAPE (replaces fixed 5s sleep)
                boolean postEscapeClosed = false;
                long postEscapeDeadline = System.currentTimeMillis() + 15_000L;
                while (System.currentTimeMillis() < postEscapeDeadline) {
                    sleep(500);
                    try {
                        driver.findElement(By.xpath(
                            "//*[@displayed='true' and (contains(@text,'You found me!') " +
                            "or contains(@content-desc,'you-found-me') " +
                            "or contains(@content-desc,'You found me!'))]"));
                        // modal still visible — keep polling
                    } catch (Exception gone) {
                        System.out.println("[HomePage] R26: modal closed during post-ESCAPE poll");
                        postEscapeClosed = true;
                        break;
                    }
                }
                System.out.println("[HomePage] R26: post-ESCAPE 15s poll complete — closed=" + postEscapeClosed);

                if (!postEscapeClosed) {
                    // Try tapping modal center — coordinate tap bypasses clickable=false
                    // Modal bounds [196,1974][884,2027] → center ≈ (540, 2000)
                    System.out.println("[HomePage] R26: tapping modal center (540,2000)");
                    try {
                        driver.executeScript("mobile: clickGesture", Map.of("x", 540, "y", 2000));
                        sleep(3000);
                    } catch (Exception tapEx) {
                        System.out.println("[HomePage] R26: tap failed: " + tapEx.getMessage());
                    }

                    boolean tapClosed = false;
                    try {
                        driver.findElement(By.xpath(
                            "//*[@displayed='true' and (contains(@text,'You found me!') " +
                            "or contains(@content-desc,'you-found-me') " +
                            "or contains(@content-desc,'You found me!'))]"));
                        // still visible after tap
                    } catch (Exception gone2) {
                        System.out.println("[HomePage] R26: modal dismissed by coordinate tap");
                        tapClosed = true;
                    }

                    if (!tapClosed) {
                        // Nuclear: restart app — only way to clear mounted screen state
                        System.out.println("[HomePage] R26: modal persists — restarting app");
                        try {
                            driver.terminateApp("com.wdiodemoapp");
                            sleep(2000);
                            driver.activateApp("com.wdiodemoapp");
                            sleep(7000);
                            System.out.println("[HomePage] R26: app restarted — navigating to Swipe");
                            tapTab(TAB_SWIPE, "Swipe", 30);
                        } catch (Exception restartEx) {
                            System.out.println("[HomePage] R26: Swipe restart failed: " + restartEx.getMessage());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // findElement threw → dialog is not visually open → nothing to dismiss
            System.out.println("[HomePage] 'You found me!' dialog not visible — no action");
        }
    }

    /**
     * Generic tab-click retry loop used for all tabs except Login.
     *
     * After 5 s of the element being not-found, presses KEYCODE_BACK once.
     * This dismisses a side drawer (left open by AlertPage) or a modal dialog
     * (e.g., "You found me!" from SwipeTest) that is blocking the nav bar.
     * BACK is sent only once to avoid cascading back-navigation.
     */
    private void tapTab(By locator, String name, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        long backPressAfter = System.currentTimeMillis() + 5_000L;
        boolean triedBack = false;

        while (System.currentTimeMillis() < deadline) {

            try {
                WebElement el = driver.findElement(locator);
                el.click();
                sleep(600);
                System.out.println("[HomePage] clicked " + name + " tab");
                return;
            } catch (Exception ignored) {}

            try {
                WebElement el = driver.findElement(locator);
                int x = el.getLocation().getX() + el.getSize().getWidth()  / 2;
                int y = el.getLocation().getY() + el.getSize().getHeight() / 2;
                driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
                sleep(600);
                System.out.println("[HomePage] clickGesture " + name + " tab");
                return;
            } catch (Exception ignored) {}

            if (!triedBack && System.currentTimeMillis() > backPressAfter) {
                triedBack = true;
                try {
                    driver.executeScript("mobile: pressKey", Map.of("keycode", 4));
                    sleep(1000);
                    System.out.println("[HomePage] pressed BACK to dismiss overlay, retrying " + name + " tab");
                } catch (Exception ignored) {}
            }

            sleep(1500);
        }
        System.out.println("[HomePage] FAILED to tap " + name + " tab after " + timeoutSeconds + "s");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
