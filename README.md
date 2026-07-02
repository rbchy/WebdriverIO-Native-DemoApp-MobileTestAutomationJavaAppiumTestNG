# WDIO Native Demo App — Mobile Test Automation (Java + Appium + TestNG)

![Java](https://img.shields.io/badge/Java-17-orange?logo=java)
![Appium java-client](https://img.shields.io/badge/Appium--java--client-9.2.2-brightgreen?logo=appium)
![Selenium](https://img.shields.io/badge/Selenium-4.19.0-43B02A?logo=selenium)
![TestNG](https://img.shields.io/badge/TestNG-7.10.2-red)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?logo=apache-maven)
![Allure](https://img.shields.io/badge/Allure-2.27.0-orange)
![ExtentReports](https://img.shields.io/badge/ExtentReports-5.x-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

**Author:** Ranajit Chowdhury — Software Programmer & QA Automation Engineer
**Email:** chyranajit@gmail.com
**GitHub:** [github.com/rbchy](https://github.com/rbchy)
**Portfolio:** [rbc6543.wixsite.com/rbc-portfolio](https://rbc6543.wixsite.com/rbc-portfolio)

**Comprehensive Mobile Test Automation Framework** for the Android [WDIO Native Demo App](https://github.com/webdriverio/native-demo-app) — built with Appium 9.2.2 (java-client), TestNG, Maven, Allure Report, and ExtentReports, following the Page Object Model design pattern. Covers Login validation, Forms interaction, Swipe gestures, and Alert dialogs with full retry support and dual HTML reporting.

🌐 **Application Under Test:** [WDIO Native Demo App v2.2.0 (WebdriverIO official test app)](https://github.com/webdriverio/native-demo-app/releases)

---

## 🏆 Test Results

| Metric | Result |
|--------|--------|
| 📋 Total Tests | 20 |
| ✅ Passed | 15 |
| ⏭️ Skipped | 5 |
| ❌ Failed | 0 |
| 📊 Pass Rate | **100% (no failures)** |
| ⏱️ Duration | ~18 minutes |
| 📅 Last Run | 01 July 2026 |

> **Note on skips:** 5 Alert tests (`TC-A01`–`TC-A05`) skip gracefully via `SkipException` because the Alert trigger buttons are not present in `ApiDemos v2.2.0`. These are intentional skips — not failures.

---

## ✨ Key Features

| Feature | Detail |
|---------|--------|
| ✅ Page Object Model | Clean separation of locators, page actions, and test logic across 5 page classes |
| ✅ Dual Reporting | **Allure Report** (step-level, screenshots, Cucumber-style) + **ExtentReports** (HTML dashboard) |
| ✅ Device Screenshots | Automatically attached to both Allure and ExtentReports on every test failure |
| ✅ Auto-Retry | `rerunFailingTestsCount=4` — flaky tests retried up to 4× before marking as failed |
| ✅ React Native KAV Fix | Custom `tapLogin()` uses Appium accessibility API (not coordinate taps) to shift focus, ensuring `KeyboardAvoidingView` deactivates before form submission |
| ✅ Multi-Strategy Locators | 5-level fallback in `findLoginButton()`: accessibilityId → UiSelector text → exact XPath → Y-bounded clickable scan → UiScrollable |
| ✅ Hardware Keyboard Safe | All keyboard interactions handle hardware-keyboard emulators where `isKeyboardShown` always returns false |
| ✅ Cold-Start Resilient | `BaseTest` waits for full React Native JS bridge hydration before any test runs |

---

## 🗂️ Project Structure

```
wdiodemoapp/
├── src/main/java/
│   ├── base/
│   │   └── BaseTest.java                   # Driver setup/teardown, hydration wait
│   ├── pages/
│   │   ├── HomePage.java                   # Bottom nav navigation helpers
│   │   ├── LoginPage.java                  # Login screen — multi-strategy KAV-aware login
│   │   ├── FormPage.java                   # Forms screen — text input, switch, dropdown, slider
│   │   ├── SwipePage.java                  # Swipe carousel — left/right/vertical/find-me
│   │   └── AlertPage.java                  # Alert dialogs — default/confirm/prompt
│   ├── tests/
│   │   ├── LoginTest.java                  # TC-L01 … TC-L05 (5 tests)
│   │   ├── FormTest.java                   # TC-F01 … TC-F05 (5 tests)
│   │   ├── SwipeTest.java                  # TC-S01 … TC-S05 (5 tests)
│   │   └── AlertTest.java                  # TC-A01 … TC-A05 (5 tests, skipped)
│   ├── listeners/
│   │   ├── ScreenshotListener.java         # Saves PNG + logs to ExtentReports on failure
│   │   └── AllureScreenshotListener.java   # Attaches PNG to Allure on failure
│   └── utils/
│       ├── WaitUtils.java                  # Fluent wait helpers
│       └── ExtentReportManager.java        # ExtentReports singleton
├── src/main/resources/
│   └── allure.properties                   # Allure results directory
├── android.wdio.native.app.v2.2.0.apk
├── testng.xml
└── pom.xml
```

---

## 🧪 Test Cases

### 🔐 Login — `Feature: Login`

| ID | Test Case | Severity |
|----|-----------|----------|
| TC-L01 | Valid credentials → login success | 🔴 Critical |
| TC-L02 | Empty email field → error message | 🟡 Normal |
| TC-L03 | Empty password field → error message | 🟡 Normal |
| TC-L04 | Invalid email format → error message | 🟡 Normal |
| TC-L05 | Both fields empty → error message | 🟢 Minor |

### 📝 Forms — `Feature: Forms`

| ID | Test Case | Severity |
|----|-----------|----------|
| TC-F01 | Text input reflects typed value | 🟡 Normal |
| TC-F02 | Toggle switch changes state ON ↔ OFF | 🟡 Normal |
| TC-F03 | Dropdown selection updates field | 🟡 Normal |
| TC-F04 | Slider present or form scrollable | 🟢 Minor |
| TC-F05 | Text input accepts alphanumeric characters | 🟢 Minor |

### 👆 Swipe — `Feature: Swipe`

| ID | Test Case | Severity |
|----|-----------|----------|
| TC-S01 | First card visible on load | 🟡 Normal |
| TC-S02 | Swipe left reveals next card | 🟡 Normal |
| TC-S03 | Swipe right returns to previous card | 🟡 Normal |
| TC-S04 | Swipe to last card and tap Find Me | 🟡 Normal |
| TC-S05 | Vertical swipe up scrolls screen | 🟢 Minor |

### 🔔 Alerts — `Feature: Alerts` *(skipped — buttons absent in v2.2.0)*

| ID | Test Case | Severity |
|----|-----------|----------|
| TC-A01 | Default alert → accept with OK | 🔴 Critical |
| TC-A02 | Confirm alert → accept with OK | 🟡 Normal |
| TC-A03 | Confirm alert → dismiss with Cancel | 🟡 Normal |
| TC-A04 | Prompt alert → enter text and confirm | 🟡 Normal |
| TC-A05 | Prompt alert → cancel without input | 🟢 Minor |

---

## 🛠️ Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java | 17+ | `java -version` |
| Maven | 3.6+ | `mvn -version` |
| Node.js | 18+ | Required by Appium |
| Appium Server | 2.x | `npm install -g appium` |
| UiAutomator2 Driver | Latest | `appium driver install uiautomator2` |
| Android SDK | — | `ANDROID_HOME` must be set |
| Android Emulator | API 33 (Android 13) | Named `emulator-5554` |

---

## ⚙️ Setup & Installation

### 1. Clone the repository

```bash
git clone https://github.com/rbchy/wdiodemoapp-appium-tests.git
cd wdiodemoapp-appium-tests/wdiodemoapp
```

### 2. Place the APK

Download `android.wdio.native.app.v2.2.0.apk` from the [WDIO Native Demo App releases](https://github.com/webdriverio/native-demo-app/releases) and copy it into the project root (same level as `pom.xml`).

### 3. Configure Maven plugin group (one-time, for `mvn allure:serve`)

```bash
cat > ~/.m2/settings.xml << 'EOF'
<settings>
  <pluginGroups>
    <pluginGroup>io.qameta.allure</pluginGroup>
  </pluginGroups>
</settings>
EOF
```

### 4. Install dependencies

```bash
mvn clean install -DskipTests
```

---

## ▶️ Running the Tests

### Start prerequisites

```bash
# Terminal 1 — Appium server
appium

# Terminal 2 — Android emulator (if not already running)
emulator -avd <your_avd_name>
```

### Run the full suite

```bash
mvn clean test
```

---

## 📊 Reports

### Allure Report (Cucumber-style, recommended)

```bash
# After mvn clean test:
mvn allure:serve          # Generates report and opens browser automatically
```

Or generate static HTML:

```bash
mvn allure:report
open target/site/allure-maven-plugin/index.html
```

**Allure report hierarchy:**

```
Epic: WDIO Native Demo App
 └── Feature: Login / Forms / Swipe / Alerts
      └── Story: TC-L01…TC-A05
           └── Steps: each action as a named Allure.step()
```

Every failed test shows an embedded device screenshot inline.

### ExtentReports

Auto-generated to `test-output/ExtentReport.html` after every run:

```bash
open test-output/ExtentReport.html
```

---

## ⚙️ Configuration

Device parameters are set in `testng.xml` — update to match your environment:

```xml
<parameter name="deviceName"      value="emulator-5554"/>
<parameter name="platformVersion" value="13"/>
```

Retry count is set in `pom.xml`:

```xml
<rerunFailingTestsCount>4</rerunFailingTestsCount>
```

---

## 🔍 Engineering Notes

**React Native KeyboardAvoidingView (KAV)**
When a `TextInput` gains UiAutomator2 accessibility focus, React Native's `KeyboardAvoidingView` restricts the touch area and hides elements below a Y threshold (~y=1440 on a 2340px screen). The Login button sits at y≈1573 — inside the restricted zone when the email field is focused. `LoginPage.tapLogin()` calls `driver.findElement(PASSWORD_INPUT).click()` (Appium API) instead of a coordinate tap; this sends `ACTION_CLICK` through the accessibility framework, fires `email.onBlur()` in React Native, and causes KAV to deactivate before the button is tapped.

**Hardware keyboard emulator**
`mobile: isKeyboardShown` always returns `false` on AVDs configured with a hardware keyboard. `driver.hideKeyboard()` is called defensively but is not relied upon for state management.

**App hydration wait**
React Native's JS bridge takes 30–60 s on first launch. `BaseTest.setUp()` waits for the Home tab, then the Login tab, to confirm the full navigation bar is rendered before any `@BeforeMethod` runs.

---

## 📄 License

MIT — see [LICENSE](LICENSE) for details.
The WDIO Native Demo App APK is owned by the [WebdriverIO](https://webdriver.io/) project and is used here for testing and demonstration purposes only.
