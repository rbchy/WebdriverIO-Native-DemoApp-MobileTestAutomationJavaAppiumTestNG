<div align="center">

# 📱 WDIO Native Demo App — Mobile Test Automation (Java · Appium · TestNG)

### Android UI tests for WebdriverIO's official React Native demo app: Page Objects, Appium 2 + UiAutomator2, ExtentReports, and a real Android 13 emulator in GitHub Actions

[![Typing SVG](https://readme-typing-svg.demolab.com/?font=Fira+Code&size=18&pause=1500&color=8B5CF6&center=true&vCenter=true&width=900&repeat=true&lines=Java+17+%C2%B7+Appium+2+%C2%B7+UiAutomator2+%C2%B7+TestNG;Login+%C2%B7+Forms+%C2%B7+Swipe+%C2%B7+Native+dialogs;21+%2F+21+green+on+an+Android+13+emulator+in+CI)](https://git.io/typing-svg)

<!-- ============ STATUS BAR ============ -->
[![CI](https://github.com/rbchy/WebdriverIO-Native-DemoApp-MobileTestAutomationJavaAppiumTestNG/actions/workflows/ci.yml/badge.svg)](https://github.com/rbchy/WebdriverIO-Native-DemoApp-MobileTestAutomationJavaAppiumTestNG/actions/workflows/ci.yml)
![Tests](https://img.shields.io/badge/Tests-21%20%2F%2021%20passed-22c55e?style=for-the-badge&logo=checkmarx&logoColor=white)
![Device](https://img.shields.io/badge/Device-Android%2013%20emulator-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=for-the-badge)

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Appium](https://img.shields.io/badge/Appium-2%20%C2%B7%20java--client%209.2-662D91?style=for-the-badge&logo=appium&logoColor=white)
![Selenium](https://img.shields.io/badge/Selenium-4.19-43B02A?style=for-the-badge&logo=selenium&logoColor=white)
![TestNG](https://img.shields.io/badge/TestNG-7.10-FF7F00?style=for-the-badge)
![ExtentReports](https://img.shields.io/badge/ExtentReports-5.1-0ea5e9?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

![Last Commit](https://img.shields.io/github/last-commit/rbchy/WebdriverIO-Native-DemoApp-MobileTestAutomationJavaAppiumTestNG?style=flat-square&color=8b5cf6&label=Last%20commit)
![Repo Size](https://img.shields.io/github/repo-size/rbchy/WebdriverIO-Native-DemoApp-MobileTestAutomationJavaAppiumTestNG?style=flat-square&color=155e75&label=Repo%20size)

</div>

---

## 📌 At a Glance

<table>
<tr>
<td width="50%" valign="top">

**🙋 What this is**

A Page Object framework that tests the Android build of the **[WDIO Native Demo App v2.2.0](https://github.com/webdriverio/native-demo-app)**, the React Native app the WebdriverIO team publishes for test automation practice. It covers:

- login validation, success and error messages
- form widgets: text mirror, switch and its label, dropdown
- swipe gestures on the card carousel
- the app's **native Android dialogs**

The whole suite runs **on every push**. GitHub Actions boots an **Android 13 emulator** (KVM), starts **Appium 2 + UiAutomator2**, installs the APK and runs all 21 tests. The ExtentReport, failure screenshots, UI-hierarchy dumps and the Appium log are uploaded as artifacts.

</td>
<td width="50%" valign="top">

**📍 Quick facts**

| | |
|---|---|
| 🤖 **App** | WDIO Native Demo App 2.2.0 (React Native) |
| 📱 **Device** | Android 13 emulator (API 33, x86_64) |
| 🧪 **Tests** | 21: Login 5 · Forms 5 · Swipe 5 · Dialogs 6 |
| ✅ **CI** | 21 / 21 · suite ≈ 7.5 min |
| 🧱 **Pattern** | Page Object Model, one driver per class |
| 📊 **Reports** | ExtentReports · screenshot + UI XML on failure |
| 🔁 **Runs** | push · pull request · manual |

</td>
</tr>
</table>

---

## ✅ Latest Results (GitHub Actions, Android 13 emulator)

| Feature | Tests | Result | Time in tests |
|---|:--:|:--:|--:|
| 🔐 Login | 5 | ✅ 5 / 5 | 45 s |
| 📝 Forms | 5 | ✅ 5 / 5 | 13 s |
| 👆 Swipe | 5 | ✅ 5 / 5 | 92 s |
| 🔔 Native dialogs | 6 | ✅ 6 / 6 | 100 s |
| **Total** | **21** | **✅ 21 / 21** | **≈ 7.5 min with app start-up** |

```mermaid
pie showData title 21 tests by feature
    "Login" : 5
    "Forms" : 5
    "Swipe" : 5
    "Native dialogs" : 6
```

```mermaid
xychart-beta
    title "Seconds spent in each feature's tests (CI run)"
    x-axis ["Login", "Forms", "Swipe", "Dialogs"]
    y-axis "Seconds" 0 --> 110
    bar [45, 13, 92, 100]
```

> Two tests take most of the time: *swipe to the hidden logo* (~86 s) and *login success dialog* (~81 s). Both go through long fallback searches written for a slower local emulator. Tightening them is on the roadmap.

### Before → after this update

```mermaid
xychart-beta
    title "Test outcomes: last local run (July) vs CI now"
    x-axis ["July passed", "July failed", "July skipped", "Now passed", "Now failed", "Now skipped"]
    y-axis "Tests" 0 --> 22
    bar [15, 1, 5, 21, 0, 0]
```

| | Last local run (July) | Now, in CI |
|---|:--:|:--:|
| Passed | 15 | **21** |
| Failed | 1 (TC-L03) | **0** |
| Skipped | 5 (every alert test) | **0** |
| Tests that could pass without checking anything | 5 (see Maintenance Log) | **0** |

---

## 🏗️ Architecture

```mermaid
flowchart LR
    X[testng.xml<br/>4 test groups] --> T[Test classes<br/>Login · Form · Swipe · Alert]
    T --> B[BaseTest<br/>UiAutomator2Options<br/>waits for React Native hydration]
    T --> P[Page Objects<br/>HomePage · LoginPage · FormPage<br/>SwipePage · AlertPage]
    P --> W[WaitUtils<br/>explicit waits]
    B --> A[[Appium 2 server<br/>:4723]]
    P --> A
    A --> U[UiAutomator2 driver]
    U --> D[(Android 13 emulator<br/>com.wdiodemoapp)]
    T -. on failure .-> L[ScreenshotListener<br/>PNG + UI XML]
    T --> R[ExtentReportManager<br/>test-output/ExtentReport.html]
    L --> R
```

### 🔄 What happens in one test class

```mermaid
sequenceDiagram
    participant TNG as TestNG
    participant BT as BaseTest
    participant AP as Appium + UiAutomator2
    participant APP as Demo app
    TNG->>BT: @BeforeClass setUp(deviceName, platformVersion)
    BT->>AP: is the server up? (GET /status)
    BT->>AP: new AndroidDriver(UiAutomator2Options, APK)
    AP->>APP: install + launch .MainActivity
    BT->>APP: wait for "Home", then "Login" tab (JS bridge hydrated)
    loop each @Test
        TNG->>APP: @BeforeMethod: open the screen, reset its state
        TNG->>APP: actions through the Page Object
        TNG->>APP: assert the exact text / state on screen
        alt failure
            TNG->>APP: screenshot + page source saved
        end
    end
    TNG->>BT: @AfterClass(alwaysRun) quit driver
    TNG->>BT: @AfterSuite(alwaysRun) write ExtentReport
```

### 🚦 CI pipeline

```mermaid
flowchart LR
    P([push · PR · manual]) --> C[Compile<br/>mvn test-compile]
    C --> E[Enable KVM]
    E --> I[Appium 2 +<br/>UiAutomator2 3.10]
    I --> K[Download APK<br/>scripts/get-apk.sh]
    K --> V[Boot Android 13 AVD<br/>cached snapshot]
    V --> S[mvn test<br/>21 tests]
    S --> R[Artifacts: ExtentReport,<br/>screenshots, UI XML,<br/>surefire, appium.log]
```

---

## 🧪 Test Cases

```mermaid
mindmap
  root((WDIO demo app))
    Login
      Valid login shows success
      Empty email message
      Empty password message
      Invalid email format
      Both fields empty
    Forms
      Typed text is mirrored
      Switch toggles
      Dropdown shows the pick
      Switch label follows state
      Alphanumeric input
    Swipe
      First card visible
      Swipe left
      Swipe right
      Find the hidden logo
      Vertical swipe
    Native dialogs
      Active button dialog content
      OK closes
      CANCEL closes
      ASK ME LATER closes
      Inactive button no dialog
      Login success dialog
```

### 🔐 Login

| ID | Test case | Asserts |
|---|---|---|
| TC-L01 | Valid credentials | "You are logged in!" appears, no email error shown; the dialog is closed afterwards |
| TC-L02 | Empty email | Shows *"Please enter a valid email address"*, and no password error |
| TC-L03 | Empty password | Shows *"Please enter at least 8 characters"*, and no email error |
| TC-L04 | Invalid email format | Shows *"Please enter a valid email address"* |
| TC-L05 | Both fields empty | Shows both messages |

### 📝 Forms

| ID | Test case | Asserts |
|---|---|---|
| TC-F01 | Text input | The *input-text-result* mirror equals the typed text **exactly** |
| TC-F02 | Switch | `checked` flips, and flips back on a second tap |
| TC-F03 | Dropdown | The field shows *"Appium is awesome"*, then *"This app is awesome"* after a second pick |
| TC-F04 | Switch label | *"Click to turn the switch ON/OFF"* follows the state |
| TC-F05 | Alphanumeric input | The mirror equals `Test123` |

### 👆 Swipe

| ID | Test case | Asserts |
|---|---|---|
| TC-S01 | First card | *FULLY OPEN SOURCE* is visible |
| TC-S02 | Swipe left | *GREAT COMMUNITY* is visible |
| TC-S03 | Swipe right | Back to the first card |
| TC-S04 | Hidden logo | Scrolls to *find-me*, taps it, and the "You found me" confirmation appears |
| TC-S05 | Vertical swipe | The gesture completes and the screen is still shown (smoke only) |

### 🔔 Native dialogs

The old tests looked for Alert / Confirm / Prompt buttons. Those belong to a different demo app (ApiDemos), so the tests could never run. They were replaced with the dialogs this app really has.

```mermaid
stateDiagram-v2
    direction LR
    [*] --> Forms
    Forms --> Dialog : tap Active
    Forms --> Forms : tap Inactive (disabled, no dialog)
    Dialog --> Forms : OK
    Dialog --> Forms : CANCEL
    Dialog --> Forms : ASK ME LATER
    state Dialog {
        [*] --> Shown
        Shown : "This button is"
        Shown : "This button is active"
    }
```

| ID | Test case | Asserts |
|---|---|---|
| TC-A01 | Active button | Title *"This button is"*, message *"This button is active"*, buttons OK / CANCEL / ASK ME LATER |
| TC-A02 | OK | The dialog closes |
| TC-A03 | CANCEL | The dialog closes |
| TC-A04 | ASK ME LATER | The dialog closes |
| TC-A05 | Inactive button | No dialog appears |
| TC-A06 | Login success | Title *"Success"*, message *"You are logged in!"*, and OK closes it |

---

## ▶️ Running Locally

**You need:** JDK 17, Maven 3.6+, Node 18+ with Appium 2, and the Android SDK with an emulator or a device.

```bash
# 1. Appium 2 + a driver line that supports it
npm install -g appium@2
appium driver install uiautomator2@3.10.0

# 2. The app under test (downloaded next to pom.xml; not committed)
./scripts/get-apk.sh

# 3. Start an emulator (API 33 recommended) and Appium
emulator -avd <your_avd> &
appium &

# 4. Run
mvn clean test
open test-output/ExtentReport.html      # report; failures in test-output/screenshots/
```

The device is set in `testng.xml` (`deviceName = emulator-5554`, `platformVersion = 13`). Change both to match your device.

---

## 📁 Project Structure

```text
WebdriverIO-Native-DemoApp-MobileTestAutomationJavaAppiumTestNG/
├── .github/workflows/ci.yml          # compile → Android 13 emulator + Appium → 21 tests → artifacts
├── scripts/get-apk.sh                # downloads android.wdio.native.app.v2.2.0.apk
├── src/main/java/
│   ├── base/BaseTest.java            # driver per class, hydration wait, alwaysRun teardown
│   ├── pages/                        # HomePage · LoginPage · FormPage · SwipePage · AlertPage
│   ├── tests/                        # LoginTest · FormTest · SwipeTest · AlertTest
│   ├── listeners/ScreenshotListener  # PNG + UI XML on failure, ExtentReports logging
│   └── utils/                        # WaitUtils · ExtentReportManager
├── testng.xml
└── pom.xml
```

```mermaid
pie showData title Framework code by layer (2,765 lines)
    "Page Objects" : 2007
    "Tests" : 459
    "BaseTest" : 122
    "Utils" : 96
    "Listener" : 81
```

---

## 🧹 Maintenance Log

The README was checked against the code, and the suite was run on a real emulator in CI. These issues were found and fixed:

| Issue found | Fix |
|---|---|
| ❌ **No CI**, and the suite had never run anywhere but one laptop | Added GitHub Actions with an Android 13 emulator, Appium 2 and UiAutomator2, plus artifacts |
| 🔁 **All 5 alert tests could only be skipped.** They searched for Alert / Confirm / Prompt buttons from ApiDemos, which this app does not have. | Replaced with 6 tests of the app's real native dialogs |
| 🎯 **Wrong locators, hidden by fallbacks.** The app uses `input-text-result` (code had `text-input-result`) and `button-LOGIN` (code had `login-btn`). TC-F01 / F05 passed only because they read the text back from the input itself, and every login tap went through slow fallbacks. | Fixed the locators. Tests now compare the mirror text exactly. |
| 🙈 **Negative login tests could not fail.** "Error shown" meant the page source contained `error` or `Invalid`. The password message ("Please enter at least 8 characters") was not recognised at all, which is why TC-L03 failed. | Each test asserts the exact validation message it expects, and that the other message is absent |
| 🧹 **Tests leaked state into each other.** The login fields kept the previous test's text, so *empty email* actually submitted an email and logged in. | Each login test starts with empty fields |
| 🔽 **Two checks were vacuous.** *Dropdown* passed if anything on the page changed, and *Slider* passed by only checking that the Forms screen was open (v2.2.0 has no slider). | The dropdown value is now asserted. The slider test was replaced by the switch-label test. |
| 📊 **The ExtentReport was lost** whenever a class's set-up was skipped, because `@AfterSuite` was skipped too. A second failure of a test also lost its screenshot (the file already existed). | `alwaysRun = true`, screenshots overwrite, and the UI hierarchy XML is saved next to each screenshot |
| 📝 **The README claimed features that are not in the code:** Allure, `AllureScreenshotListener`, `rerunFailingTestsCount = 4`, an APK in the repo, "100 % pass" | Rewritten to match the code and the CI results |
| 📦 `target/` and old failure screenshots were committed | Removed, and added a `.gitignore` |

---

## 📈 Roadmap

- [ ] Cut the two slow paths (hidden-logo scroll, login after navigation) from ~80 s to a few seconds
- [ ] Make TC-S05 assert what the vertical swipe reveals
- [ ] Allure reporting next to ExtentReports
- [ ] Drag-and-drop puzzle, WebView and biometrics screens
- [ ] Parallel runs on two emulators (API 30 and 34) with a TestNG data provider
- [ ] iOS run on a macOS runner with XCUITest

---

## 🔗 Related Projects

| Project | Stack |
|---|---|
| 📱 [Appium Automation – API Demos](https://github.com/rbchy/AppiumAutomation_ApiDemos) | Appium · Android · CI |
| 🏔️ [NorthFace × Automation Exercise BDD](https://github.com/rbchy/NorthFace_AutomationExercise-BDD-Test-Automation) | Selenium · Cucumber · RestAssured · CI |
| 🎭 [QaBrains Automation – Playwright TypeScript](https://github.com/rbchy/QaBrainsAutomationTypeScriptPlaywright) | Playwright · TypeScript · CI |
| 🌲 [QaBrains Automation – Cypress](https://github.com/rbchy/QaBrainsAutomationCypressJavaScript) | Cypress · JavaScript · CI |

<sub>All projects: [github.com/rbchy](https://github.com/rbchy)</sub>

---

## ⚠️ Disclaimer

This is a portfolio / training project. The WDIO Native Demo App belongs to the [WebdriverIO](https://webdriver.io/) project. It is downloaded from their releases at test time and is not redistributed here.

---

## 👤 Author

<div align="center">

**Ranajit Baran Chowdhury (RB Chowdhury)**
QA Automation Engineer / SDET · West Norriton, Pennsylvania, USA

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/rbchy/)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/rbchy)
[![Website](https://img.shields.io/badge/Website-rbchy.github.io%2Frbchy-0d9488?style=for-the-badge&logo=githubpages&logoColor=white)](https://rbchy.github.io/rbchy/)
[![Portfolio](https://img.shields.io/badge/Portfolio-Website-blueviolet?style=for-the-badge&logo=wix&logoColor=white)](https://rbc6543.wixsite.com/rbc-portfolio)
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:chyranajit@gmail.com)

<sub>MIT licensed — see [LICENSE](LICENSE).</sub>

⭐ *If this project helps you, a star on the repo is appreciated.*

</div>
