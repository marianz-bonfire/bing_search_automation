<p align="center">
  <a href="https://github.com/marianz-bonfire/bing_search_automation">
    <img height="120" src="https://raw.githubusercontent.com/marianz-bonfire/tarsier_assets/master/package-assets/bing_search_automation/logo.png">
  </a>
  <h1 align="center">Bing Search Automation</h1>
</p>

<p align="center">
  <a href="https://github.com/marianz-bonfire/bing_search_automation">
    <img src="https://img.shields.io/static/v1?label=github.com&message=repo&labelColor=333940&logo=github">
  </a>
  <a href="https://tarsier-marianz.blogspot.com">
    <img src="https://img.shields.io/static/v1?label=website&message=tarsier-marianz&labelColor=135d34&logo=blogger&logoColor=white&color=fd3a13">
  </a>
</p>


Automate search input in the Bing Android app using an `AccessibilityService` from Flutter. Send queries from your Flutter app via a `MethodChannel`, and simulate user interaction in the Bing app: focus search field, enter text, and submit

<img src="https://raw.githubusercontent.com/marianz-bonfire/tarsier_assets/master/package-assets/bing_search_automation/demo.gif">

### Automated Bing search interaction using:

-   🤖 Android AccessibilityService (Android 8+)
-   🖥 Windows Desktop Automation Support
-   🧠 Smart UI detection (RecyclerView / WebView aware)
-   ⏱ Adaptive waiting (no fixed delay issues)

This project is designed for controlled automation, testing, and
research purposes.


## 📦 Features

#### ✅ Android Support

-   AccessibilityService-based automation
-   Smart search box detection
-   Reliable text input (ACTION_SET_TEXT compatible with Android 8+)
-   Suggestion click fallback
-   Search button fallback
-   Smart result detection (RecyclerView / WebView / Tabs)
-   Automatic retry system
-   Randomized delay between searches (human-like timing)

#### ✅ Windows Support

-   Bing automation via browser control
-   Compatible with:
    -   Edge
    -   Chrome
    -   WebView2
-   DOM-based search triggering
-   Result page detection
-   Headless or visible mode support


# 🖥Architecture

#### Android Flow

1.  Click Home Search Box
2.  Wait for EditText
3.  Focus + Clear text
4.  Enter query using ACTION_SET_TEXT
5.  Trigger search:
    -   Click suggestion
    -   Click Search button
6.  Wait for results (smart polling)
7.  Random delay (10--20 seconds)
8.  Repeat

#### Windows Flow

1.  Open Bing
2.  Inject search query into input field
3.  Trigger submit (Enter or button click)
4.  Wait for result container
5.  Extract or click first result
6.  Delay
7.  Repeat


### 📱 Android Requirements

-   Android 8 (API 26) or higher
-   Accessibility permission enabled
-   Foreground service recommended
-   Internet connection

### Required Permissions

``` xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

Accessibility service must be enabled manually:

- Settings → Accessibility → Your Service → Enable

## 📱Screenshots


<table>
  <thead>
    <tr>
      <th align="center">Splash</th>
      <th align="center">Home</th>
      <th align="center">Settings</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td align="center">
        <img src="https://raw.githubusercontent.com/marianz-bonfire/tarsier_assets/master/package-assets/bing_search_automation/splash.png" width="200" />
      </td>
      <td align="center">
        <img src="https://raw.githubusercontent.com/marianz-bonfire/tarsier_assets/master/package-assets/bing_search_automation/home.png" width="200" />
      </td>
      <td align="center">
        <img src="https://raw.githubusercontent.com/marianz-bonfire/tarsier_assets/master/package-assets/bing_search_automation/settings.png" width="200" />
      </td>
    </tr>
  </tbody>
</table>

## 🎖️ License
This project is licensed under the [MIT License](https://mit-license.org/). See the LICENSE file for details.
## 🐞 Contributing
Contributions are welcome! Please submit a pull request or file an issue for any bugs or feature requests
on [GitHub](https://github.com/marianz-bonfire/bing_search_automation).



