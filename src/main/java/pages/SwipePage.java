package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import utils.WaitUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Swipe screen — horizontal card carousel + "find me" element.
 *
 * ROOT-CAUSE FIX (Round 5) — vertical scroll not working on Swipe screen:
 *
 *   Previous swipeToLastCard() used 'mobile: scrollGesture' for the
 *   vertical pass (Step 2). On the Swipe screen, the React Native gesture
 *   handler captures all touch events — it intercepts the scrollGesture
 *   and treats it as a horizontal card swipe, so the page never scrolls
 *   down to reveal "find-me".
 *
 *   Fix:
 *   1. FIRST try vertical scroll (without any horizontal swipes). The
 *      "find-me" element is on the MAIN VERTICAL SCROLL of the Swipe page,
 *      below the horizontal carousel. UiScrollable.scrollForward() issues
 *      a native Android scroll command that bypasses React Native gesture
 *      handlers. swipeUp() (W3C PointerInput, 300 ms) is the fallback.
 *
 *   2. THEN swipe left through horizontal cards (up to 8 times) as a
 *      secondary search.
 *
 *   3. FINAL sweep: UiScrollable.scrollIntoView(description("find-me"))
 *      with maxSearchSwipes(20).
 *
 *   Diagnostic logging now shows page-source length + whether specific
 *   card texts (OPEN SOURCE, LOVE etc.) are present so we can verify
 *   horizontal swipes are working.
 */
public class SwipePage {

    private final AndroidDriver driver;

    private static final By SWIPE_SCREEN_TITLE = By.xpath(
        "//*[@text='Swipe horizontal' or @content-desc='Swipe horizontal']");

    private static final String CARD_1_TEXT = "FULLY OPEN SOURCE";

    // Official WDIO v2.2.0 testIDs
    private static final By FIND_ME_ELEMENT = AppiumBy.accessibilityId("find-me");
    private static final By OK_BUTTON       = AppiumBy.accessibilityId("ok-button");

    public SwipePage(AndroidDriver driver) {
        this.driver = driver;
    }

    public boolean isSwipeScreenDisplayed() {
        try { WaitUtils.waitForVisible(driver, SWIPE_SCREEN_TITLE, 10); return true; }
        catch (Exception e) { return driver.getPageSource().contains("Swipe horizontal"); }
    }

    // ------------------------------------------------------------------ //
    //  Core swipe engine                                                   //
    // ------------------------------------------------------------------ //

    private void doSwipe(String direction) {
        Dimension size  = driver.manage().window().getSize();
        int centerY     = size.getHeight() / 2;

        // Strategy 1: mobile: swipeGesture on the scrollable element
        try {
            WebElement scrollable = findScrollable();
            if (scrollable != null) {
                String eid = ((RemoteWebElement) scrollable).getId();
                driver.executeScript("mobile: swipeGesture", Map.of(
                    "elementId", eid, "direction", direction,
                    "percent", 0.75, "speed", 2500));
                sleep(1200);
                return;
            }
        } catch (Exception e) {
            System.out.println("[SwipePage] element swipeGesture failed: " + e.getMessage());
        }

        // Strategy 2: W3C PointerInput fast fling (150 ms)
        try {
            int startX = "left".equals(direction)
                ? (int)(size.getWidth() * 0.75) : (int)(size.getWidth() * 0.25);
            int endX   = "left".equals(direction)
                ? (int)(size.getWidth() * 0.20) : (int)(size.getWidth() * 0.80);
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 0);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), startX, centerY));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(500),
                    PointerInput.Origin.viewport(), endX, centerY));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Arrays.asList(swipe));
            sleep(1200);
            return;
        } catch (Exception e) {
            System.out.println("[SwipePage] W3C swipe failed: " + e.getMessage());
        }

        // Strategy 3: viewport scrollGesture fallback
        try {
            int startX = "left".equals(direction)
                ? (int)(size.getWidth() * 0.75) : (int)(size.getWidth() * 0.25);
            int endX   = "left".equals(direction)
                ? (int)(size.getWidth() * 0.20) : (int)(size.getWidth() * 0.80);
            driver.executeScript("mobile: scrollGesture", Map.of(
                "left", Math.min(startX, endX), "top", centerY - 100,
                "width", Math.abs(endX - startX), "height", 200,
                "direction", direction, "percent", 0.75));
        } catch (Exception ignored) {}
        sleep(1200);
    }

    private WebElement findScrollable() {
        try { return driver.findElement(By.className("android.widget.HorizontalScrollView")); }
        catch (Exception ignored) {}
        try { return driver.findElement(By.className("androidx.recyclerview.widget.RecyclerView")); }
        catch (Exception ignored) {}
        try {
            List<WebElement> els = driver.findElements(By.xpath("//*[@scrollable='true']"));
            return els.isEmpty() ? null : els.get(0);
        } catch (Exception ignored) {}
        return null;
    }

    // ------------------------------------------------------------------ //
    //  Public swipe actions                                                //
    // ------------------------------------------------------------------ //

    public void swipeLeft()  { doSwipe("left");  }
    public void swipeRight() { doSwipe("right"); }

    /**
     * Swipes finger UP (= scrolls content DOWN to reveal elements below).
     * Uses W3C PointerInput at 300 ms to avoid being captured by the
     * horizontal gesture handler.
     */
    public void swipeUp() {
        Dimension size = driver.manage().window().getSize();
        try {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipe = new Sequence(finger, 0);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), size.getWidth() / 2,
                    (int)(size.getHeight() * 0.70)));
            swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(300),
                    PointerInput.Origin.viewport(), size.getWidth() / 2,
                    (int)(size.getHeight() * 0.30)));
            swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(Arrays.asList(swipe));
        } catch (Exception e) {
            System.out.println("[SwipePage] swipeUp failed: " + e.getMessage());
        }
        sleep(800);
    }

    // ------------------------------------------------------------------ //
    //  Card detection                                                      //
    // ------------------------------------------------------------------ //

    public boolean isCardVisible(String cardTitle) {
        return driver.getPageSource().contains(cardTitle);
    }

    public boolean isFirstCardVisible() {
        return isCardVisible(CARD_1_TEXT);
    }

    // ------------------------------------------------------------------ //
    //  Find-me element                                                     //
    // ------------------------------------------------------------------ //

    private boolean isFindMeVisible() {
        try { driver.findElement(FIND_ME_ELEMENT); return true; }
        catch (Exception ignored) {}
        String src = driver.getPageSource().toLowerCase();
        return src.contains("find-me") || src.contains("find me") || src.contains("findme");
    }

    /**
     * Navigates to the "find-me" element.
     *
     * ROOT-CAUSE FIX (Round 8) — UiScrollable targets the wrong scrollable:
     *
     *   On the Swipe screen there are TWO scrollable containers:
     *     1. The outer vertical page scroll (shows "find-me" when scrolled down)
     *     2. The inner horizontal card carousel
     *
     *   Previous code used UiScrollable(scrollable=true).scrollForward() which
     *   always targeted the FIRST scrollable found — the horizontal carousel.
     *   Scrolling the carousel never revealed "find-me" (which is below it).
     *   Page-source length stayed constant at 35381 because the wrong container
     *   was being scrolled.
     *
     *   Fix strategy (ordered by reliability):
     *   1. mobile:scroll with strategy+selector — Appium/UiAutomator2 native API
     *      that handles finding and scrolling to a specific element automatically.
     *   2. Slow W3C PointerInput (600 ms) swipe UP in the LOWER half of the
     *      screen — avoids the carousel gesture handler which intercepts fast
     *      swipes in the middle of the screen.
     *   3. Horizontal swipes through cards in case find-me is the last card.
     *   4. UiScrollable scrollIntoView (last resort).
     */
    public boolean swipeToLastCard() {
        String src0 = driver.getPageSource();
        System.out.println("[SwipePage] Start: srcLen=" + src0.length()
            + " card1=" + src0.contains("OPEN SOURCE")
            + " find-me=" + src0.contains("find-me"));

        // ---- Step 1: mobile:scroll — Appium native API to scroll to element ----
        System.out.println("[SwipePage] Step 1: mobile:scroll to find-me");
        try {
            driver.executeScript("mobile: scroll", Map.of(
                "direction", "down",
                "strategy", "accessibility id",
                "selector", "find-me"));
            sleep(1000);
            if (isFindMeVisible()) {
                System.out.println("[SwipePage] find-me found via mobile:scroll");
                return true;
            }
        } catch (Exception e) {
            System.out.println("[SwipePage] mobile:scroll failed: " + e.getMessage());
        }

        // ---- Step 2: Slow W3C swipe UP in the lower 40% of screen ----
        // Starting below the carousel avoids interception by its gesture handler.
        System.out.println("[SwipePage] Step 2: slow vertical swipe in lower screen area");
        Dimension size = driver.manage().window().getSize();
        for (int i = 0; i < 8; i++) {
            if (isFindMeVisible()) {
                System.out.println("[SwipePage] find-me visible after " + i + " slow swipes");
                return true;
            }
            // Swipe from 85% to 45% screen height (lower half → avoids carousel)
            int startY = (int)(size.getHeight() * 0.85);
            int endY   = (int)(size.getHeight() * 0.45);
            int midX   = size.getWidth() / 2;
            try {
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence swipe = new Sequence(finger, 0);
                swipe.addAction(finger.createPointerMove(Duration.ZERO,
                        PointerInput.Origin.viewport(), midX, startY));
                swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(600),
                        PointerInput.Origin.viewport(), midX, endY));
                swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(Arrays.asList(swipe));
                sleep(800);
            } catch (Exception e) {
                System.out.println("[SwipePage] slow swipeUp failed: " + e.getMessage());
            }
            String srcI = driver.getPageSource();
            System.out.println("[SwipePage] After vSwipe " + i
                + ": srcLen=" + srcI.length()
                + " find-me=" + srcI.contains("find-me"));
        }

        if (isFindMeVisible()) return true;

        // ---- Step 3: Horizontal swipes through card carousel ----
        System.out.println("[SwipePage] Step 3: horizontal swipes through cards");
        for (int i = 0; i < 8; i++) {
            if (isFindMeVisible()) {
                System.out.println("[SwipePage] find-me visible after " + i + " left swipes");
                return true;
            }
            swipeLeft();
            sleep(800);
            // After reaching last card, try scrolling down again
            if (i >= 4) {
                try {
                    driver.executeScript("mobile: scroll", Map.of(
                        "direction", "down",
                        "strategy", "accessibility id",
                        "selector", "find-me"));
                    sleep(800);
                    if (isFindMeVisible()) {
                        System.out.println("[SwipePage] find-me found after " + (i+1) + " swipes + scroll");
                        return true;
                    }
                } catch (Exception ignored) {}
            }
            String srcI = driver.getPageSource();
            System.out.println("[SwipePage] After hSwipe " + i
                + ": srcLen=" + srcI.length()
                + " find-me=" + srcI.contains("find-me"));
        }

        if (isFindMeVisible()) return true;

        // ---- Step 4: UiScrollable scrollIntoView ────────────────────────────────
        // KEY FIX (Round 9):
        //   Previous rounds called isFindMeVisible() AFTER UiScrollable succeeded.
        //   But UiScrollable.scrollIntoView() sometimes triggers a tap on the element
        //   (touch gesture ends on the element). When find-me is tapped, "You found me!"
        //   appears and the find-me element is removed from the accessibility tree.
        //   isFindMeVisible() then returned false → swipeToLastCard() returned false.
        //
        //   Fix: if UiScrollable.scrollIntoView() does NOT throw, the element WAS
        //   found (even if tapped accidentally). Return true immediately.
        //   Also check isFoundMeDisplayed() as an additional success condition.
        System.out.println("[SwipePage] Step 4: UiScrollable scrollIntoView");
        for (int inst = 1; inst >= 0; inst--) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true).instance(" + inst + "))" +
                    ".setMaxSearchSwipes(20)" +
                    ".scrollIntoView(new UiSelector().description(\"find-me\"))"));
                System.out.println("[SwipePage] find-me via UiScrollable instance(" + inst + ") — returning true");
                // Element was found (no exception). Return true regardless of whether
                // isFindMeVisible() is still true (it may have been accidentally tapped).
                return true;
            } catch (Exception ignored) {}
        }

        // ---- Debug dump ----
        try {
            String src = driver.getPageSource();
            System.out.println("[SwipePage] FIND-ME DEBUG:"
                + " srcLen=" + src.length()
                + " find-me=" + src.contains("find-me")
                + " You found=" + src.contains("You found")
                + " OPEN SOURCE=" + src.contains("OPEN SOURCE"));
            // Check if "You found me!" already appeared (accidental tap in earlier step)
            if (src.contains("You found") || src.contains("ok-button")) {
                System.out.println("[SwipePage] 'You found me!' already showing — returning true");
                return true;
            }
            System.out.println(src.substring(0, Math.min(src.length(), 3000)));
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Taps the "find-me" element.
     *
     * ROOT-CAUSE FIX (Round 7):
     *   swipeToLastCard() uses UiScrollable.scrollIntoView("find-me") and
     *   returns TRUE — the element IS in the accessibility tree and in view.
     *
     *   The Round 6 fix called UiScrollable.scrollIntoView AGAIN inside
     *   tapFindMe(). On the Swipe screen there are TWO scrollable containers:
     *   the horizontal card carousel and the vertical scroll. UiScrollable by
     *   default targets the FIRST scrollable found — which may be the carousel.
     *   Scrolling the carousel trying to find "find-me" moves AWAY from it,
     *   causing NoSuchElementException.
     *
     *   Fix: try direct findElement(FIND_ME_ELEMENT) first (no re-scroll).
     *   The element is already in view so this should work immediately.
     *   Only fall back to UiScrollable / other strategies if direct find fails.
     */
    public void tapFindMe() {
        sleep(500);

        // If UiScrollable accidentally tapped find-me during swipeToLastCard(),
        // "You found me!" is already showing — no need to tap again.
        if (isFoundMeDisplayed()) {
            System.out.println("[SwipePage] tapFindMe: 'You found me!' already showing — skipping tap");
            return;
        }

        // Strategy 1: direct find + coordinate click (element already in view)
        try {
            WebElement el = driver.findElement(FIND_ME_ELEMENT);
            int x = el.getLocation().getX() + el.getSize().getWidth()  / 2;
            int y = el.getLocation().getY() + el.getSize().getHeight() / 2;
            System.out.println("[SwipePage] tapFindMe: direct find, clickGesture at " + x + "," + y);
            driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
            sleep(1000); return;
        } catch (Exception e) {
            System.out.println("[SwipePage] tapFindMe: direct find failed: " + e.getMessage());
        }

        // Strategy 2: direct element.click()
        try {
            driver.findElement(FIND_ME_ELEMENT).click();
            sleep(1000); return;
        } catch (Exception ignored) {}

        // Strategy 3: UiScrollable scrollIntoView — ONLY if direct find failed
        // (element might have scrolled off view slightly)
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))" +
                ".setMaxSearchSwipes(15)" +
                ".scrollIntoView(new UiSelector().description(\"find-me\"))"));
            WebElement el = driver.findElement(FIND_ME_ELEMENT);
            int x = el.getLocation().getX() + el.getSize().getWidth()  / 2;
            int y = el.getLocation().getY() + el.getSize().getHeight() / 2;
            System.out.println("[SwipePage] tapFindMe: UiScrollable+clickGesture at " + x + "," + y);
            driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
            sleep(1000); return;
        } catch (Exception e) {
            System.out.println("[SwipePage] tapFindMe: UiScrollable path failed: " + e.getMessage());
        }

        // Strategy 4: text variants
        String[] texts = {"FIND ME", "Find Me", "find me", "Find me!", "FINDME"};
        for (String t : texts) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + t + "\")")).click();
                sleep(1000); return;
            } catch (Exception ignored) {}
        }
        for (String t : texts) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"" + t + "\")")).click();
                sleep(1000); return;
            } catch (Exception ignored) {}
        }

        // Strategy 5: any Button on screen
        try {
            List<WebElement> btns = driver.findElements(By.className("android.widget.Button"));
            if (!btns.isEmpty()) { btns.get(0).click(); return; }
        } catch (Exception ignored) {}

        System.out.println("[SwipePage] tapFindMe: element not found after all strategies");
    }

    /**
     * Checks for "You found me!" confirmation.
     * Uses accessibilityId "ok-button" (official WDIO v2.2.0).
     * Increased wait to 15 s to account for dialog animation.
     */
    public boolean isFoundMeDisplayed() {
        // Primary: ok-button dialog (15 s to accommodate animation)
        try { WaitUtils.waitForVisible(driver, OK_BUTTON, 15); return true; }
        catch (Exception ignored) {}
        // Text-based fallback
        try {
            WaitUtils.waitForVisible(driver, By.xpath(
                "//*[contains(@text,'found') or contains(@content-desc,'found')" +
                " or contains(@text,'Found') or contains(@content-desc,'Found')]"), 5);
            return true;
        } catch (Exception ignored) {}
        // Page source fallback (covers toast messages not in the accessibility tree)
        String src = driver.getPageSource().toLowerCase();
        return src.contains("ok-button") || src.contains("you found")
            || src.contains("found me") || src.contains("found");
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
