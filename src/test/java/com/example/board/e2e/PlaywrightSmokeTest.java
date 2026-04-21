package com.example.board.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlaywrightSmokeTest {

    @LocalServerPort
    int port;

    static Playwright playwright;
    static Browser browser;

    @BeforeAll
    static void launchBrowser() {
        // channel("chrome") = system-installed Google Chrome. 번들 Chromium 다운로드를
        // 우회하기 위해 build.gradle.kts 의 test 태스크에서
        // PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 을 세팅함.
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setChannel("chrome"));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    void h2ConsoleLoadsInChromium() {
        try (BrowserContext context = browser.newContext();
             Page page = context.newPage()) {
            page.navigate("http://localhost:" + port + "/h2-console");
            assertThat(page.title()).containsIgnoringCase("h2 console");
        }
    }
}
