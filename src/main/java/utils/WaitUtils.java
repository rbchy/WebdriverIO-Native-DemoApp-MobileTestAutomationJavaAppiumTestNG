package utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class WaitUtils {

    /**
     * Waits until the element identified by {@code locator} is visible on screen.
     */
    public static WebElement waitForVisible(AndroidDriver driver, By locator, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits until the element identified by {@code locator} is clickable.
     */
    public static WebElement waitForClickable(AndroidDriver driver, By locator, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Retries finding an element up to {@code maxAttempts} times with a 500 ms gap.
     * Useful for elements that appear after animation or a network call.
     */
    public static WebElement retryFindElement(AndroidDriver driver, By locator, int maxAttempts) {
        int attempt = 0;
        while (attempt < maxAttempts) {
            try {
                return driver.findElement(locator);
            } catch (Exception e) {
                attempt++;
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException(
            "Element not found after " + maxAttempts + " retries: " + locator);
    }

    /**
     * Waits until the given text appears anywhere in the page source.
     */
    public static void waitForTextInSource(AndroidDriver driver, String text, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            if (driver.getPageSource().contains(text)) return;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        throw new RuntimeException("Text not found in page source: " + text);
    }
}
