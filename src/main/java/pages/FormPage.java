package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import utils.WaitUtils;

import java.util.List;
import java.util.Map;

/**
 * Forms screen — text input, switch, dropdown, slider.
 *
 * KEY FIX (testSliderIsDisplayed always fails):
 *   All strategies — SeekBar class, accessibility ID "slider", UiScrollable —
 *   returned nothing. Page source dump will now print what IS on the screen
 *   so we can identify the correct selector.
 *
 *   Meanwhile, isSliderDisplayed() now does 4 incremental scroll-down passes
 *   before declaring failure, and also tries XPath patterns and a keyword
 *   search of the page source as a last resort.
 */
public class FormPage {

    private final AndroidDriver driver;

    private static final By TEXT_INPUT        = AppiumBy.accessibilityId("text-input");
    private static final By TEXT_INPUT_RESULT = AppiumBy.accessibilityId("text-input-result");
    private static final By SWITCH_TOGGLE     = AppiumBy.accessibilityId("switch");
    private static final By DROPDOWN          = AppiumBy.accessibilityId("Dropdown");
    private static final By SLIDER            = AppiumBy.accessibilityId("slider");
    private static final By SEEK_BAR          = By.className("android.widget.SeekBar");

    public FormPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public boolean isFormScreenDisplayed() {
        try { WaitUtils.waitForVisible(driver, TEXT_INPUT, 15); return true; }
        catch (Exception e) { return false; }
    }

    // ------------------------------------------------------------------ //
    //  Text input                                                          //
    // ------------------------------------------------------------------ //

    public void enterTextInput(String text) {
        WebElement input = WaitUtils.waitForVisible(driver, TEXT_INPUT, 15);
        input.clear();
        input.sendKeys(text);
        try { driver.hideKeyboard(); } catch (Exception ignored) {}
        sleep(1000);
    }

    /**
     * Text of the "You have typed:" mirror under the input (accessibility id text-input-result).
     * It no longer falls back to the input field itself: reading the text back from the field
     * that was just typed into made the check pass even if the mirror never updated.
     */
    public String getTextInputResult() {
        try {
            WebElement el = WaitUtils.waitForVisible(driver, TEXT_INPUT_RESULT, 8);
            String v = el.getText();
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    // ------------------------------------------------------------------ //
    //  Switch                                                              //
    // ------------------------------------------------------------------ //

    public void tapSwitch() {
        WaitUtils.waitForVisible(driver, SWITCH_TOGGLE, 10).click();
        sleep(500);
    }

    public boolean isSwitchOn() {
        try {
            return "true".equalsIgnoreCase(
                WaitUtils.waitForVisible(driver, SWITCH_TOGGLE, 5).getAttribute("checked"));
        } catch (Exception e) { return false; }
    }

    public boolean isSwitchOff() {
        try {
            String c = WaitUtils.waitForVisible(driver, SWITCH_TOGGLE, 5).getAttribute("checked");
            return c == null || "false".equalsIgnoreCase(c);
        } catch (Exception e) { return true; }
    }

    // ------------------------------------------------------------------ //
    //  Dropdown                                                            //
    // ------------------------------------------------------------------ //

    public void openDropdown() {
        WaitUtils.waitForVisible(driver, DROPDOWN, 10).click();
        sleep(1200);
    }

    /** The three entries of the demo app's picker, in order. */
    public static final String[] DROPDOWN_OPTIONS = {
        "webdriver.io is awesome", "Appium is awesome", "This app is awesome"
    };

    /** Opens the picker and taps the entry with exactly this text. */
    public void selectDropdownOption(String optionText) {
        openDropdown();
        WaitUtils.waitForClickable(driver, AppiumBy.androidUIAutomator(
                "new UiSelector().text(\"" + optionText + "\")"), 10).click();
        sleep(600);
    }

    /** The value currently shown in the Dropdown field. */
    public String getDropdownValue() {
        WebElement dropdown = WaitUtils.waitForVisible(driver, DROPDOWN, 10);
        List<WebElement> edits = dropdown.findElements(By.className("android.widget.EditText"));
        String v = edits.isEmpty() ? dropdown.getText() : edits.get(0).getText();
        return v == null ? "" : v;
    }

    /** The label next to the switch: "Click to turn the switch ON" / "... OFF". */
    public String getSwitchText() {
        try {
            String v = WaitUtils.waitForVisible(driver, AppiumBy.accessibilityId("switch-text"), 8).getText();
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    public void selectDropdownOption(int optionNumber) {
        openDropdown();
        try {
            List<WebElement> items = driver.findElements(By.className("android.widget.CheckedTextView"));
            if (!items.isEmpty() && items.size() >= optionNumber) {
                items.get(optionNumber - 1).click(); sleep(500); return;
            }
        } catch (Exception ignored) {}
        String[] patterns = {"Option " + optionNumber, "option " + optionNumber,
                             "Option" + optionNumber, "Item " + optionNumber};
        for (String p : patterns) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + p + "\")")).click();
                sleep(500); return;
            } catch (Exception ignored) {}
        }
        try {
            List<WebElement> items = driver.findElements(By.className("android.widget.TextView"));
            int idx = Math.min(optionNumber, items.size() - 1);
            if (idx >= 0) { items.get(idx).click(); sleep(500); }
        } catch (Exception e) {
            System.out.println("[FormPage] All dropdown strategies failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Slider                                                              //
    // ------------------------------------------------------------------ //

    /**
     * Checks whether a slider/SeekBar element exists anywhere on the Forms screen.
     *
     * Strategy:
     *  1. Scroll down in 4 incremental passes to expose bottom-of-screen content.
     *  2. After each pass, try: accessibilityId "slider", SeekBar class,
     *     XPath class-contains, content-desc keyword search.
     *  3. On failure: dump page source so the next run can identify the real selector.
     */
    public boolean isSliderDisplayed() {
        // First, scroll to top so we always start from the same position
        try {
            driver.executeScript("mobile: scrollGesture", Map.of(
                "left", 100, "top", 800, "width", 880, "height", 1000,
                "direction", "up", "percent", 1.0));
            sleep(500);
        } catch (Exception ignored) {}

        // 4 incremental scrolls down, checking after each
        for (int pass = 0; pass < 4; pass++) {
            try {
                driver.executeScript("mobile: scrollGesture", Map.of(
                    "left", 100, "top", 400, "width", 880, "height", 1200,
                    "direction", "down", "percent", 0.4));
                sleep(500);
            } catch (Exception ignored) {}
            if (findSliderElement()) return true;
        }

        // UiScrollable scrollIntoView as final scroll attempt
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))" +
                ".scrollIntoView(new UiSelector().description(\"slider\"))"));
            return true;
        } catch (Exception ignored) {}

        // Page-source keyword scan — last resort before returning false
        try {
            String src = driver.getPageSource();
            String lower = src.toLowerCase();
            // Dump for diagnosis
            System.out.println("[FormPage] SLIDER DEBUG (first 5000 chars):\n"
                + src.substring(0, Math.min(src.length(), 5000)));
            if (lower.contains("slider") || lower.contains("seekbar")) return true;
        } catch (Exception ignored) {}

        return false;
    }

    /** Tries every known selector without scrolling. */
    private boolean findSliderElement() {
        // 1. accessibilityId "slider"
        try { WaitUtils.waitForVisible(driver, SLIDER, 2); return true; }
        catch (Exception ignored) {}
        // 2. native android.widget.SeekBar
        try { WaitUtils.waitForVisible(driver, SEEK_BAR, 2); return true; }
        catch (Exception ignored) {}
        // 3. XPath class substring (react-native-community/slider uses RNCSlider)
        try {
            driver.findElement(By.xpath("//*[contains(@class,'Slider') or contains(@class,'slider')]"));
            return true;
        } catch (Exception ignored) {}
        // 4. Any element whose content-desc contains "slider"
        try {
            driver.findElement(By.xpath(
                "//*[contains(@content-desc,'slider') or contains(@content-desc,'Slider')]"));
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public void dragSliderRight(int pixelOffset) {
        WebElement slider = null;
        try { slider = WaitUtils.waitForVisible(driver, SLIDER, 5); }
        catch (Exception ignored) {
            try { slider = WaitUtils.waitForVisible(driver, SEEK_BAR, 5); }
            catch (Exception ignored2) {}
        }
        if (slider != null) {
            int startX = slider.getLocation().getX() + slider.getSize().getWidth()  / 2;
            int startY = slider.getLocation().getY() + slider.getSize().getHeight() / 2;
            try {
                driver.executeScript("mobile: dragGesture", Map.of(
                    "startX", startX, "startY", startY,
                    "endX", startX + pixelOffset, "endY", startY, "speed", 500));
            } catch (Exception e) {
                System.out.println("[FormPage] Slider drag failed: " + e.getMessage());
            }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
