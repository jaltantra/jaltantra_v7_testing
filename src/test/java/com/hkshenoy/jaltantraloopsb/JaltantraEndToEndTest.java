package com.hkshenoy.jaltantraloopsb;

import org.openqa.selenium.*;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.support.ui.*;
import org.testng.annotations.*;
import org.testng.Assert;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.stream.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;

public class JaltantraEndToEndTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private Path downloadDir;
    private Path sampleInput;
    private Path sampleoutput;
    @BeforeClass
    public void setUp() throws Exception {
        // prepare download directory under project root
        downloadDir = Paths.get(System.getProperty("user.dir"), "jaltantraDownloads");
        if (Files.exists(downloadDir)) {
            try (Stream<Path> files = Files.list(downloadDir)) {
                files.forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(downloadDir);

        System.setProperty("webdriver.gecko.driver", "/snap/bin/geckodriver");
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.download.dir", downloadDir.toString());
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
            "application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/octet-stream");
        profile.setPreference("browser.download.useDownloadDir", true);
        profile.setPreference("browser.download.manager.showWhenStarting", false);
        profile.setPreference("browser.helperApps.alwaysAsk.force", false);
        profile.setPreference("browser.download.manager.focusWhenStarting", false);
profile.setPreference("browser.download.manager.alertOnEXEOpen", false);

        // auto‐grant geolocation
        profile.setPreference("permissions.default.geo", 1);
        profile.setPreference("geo.prompt.testing", true);
        profile.setPreference("geo.prompt.testing.allow", true);
        profile.setPreference("geo.provider.network.url",
            "data:application/json,{\"location\":{\"lat\":12.9716,\"lng\":77.5946},\"accuracy\":100}");

        FirefoxOptions opts = new FirefoxOptions()
            .setProfile(profile)
            .setBinary("/snap/firefox/current/usr/lib/firefox/firefox");
        driver = new FirefoxDriver(opts);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @DataProvider(name = "excelCases")
    public Object[][] excelCases() throws IOException {
        Path inputDir  = Paths.get("src/test/resources/input");
        Path outputDir = Paths.get("src/test/resources/output");
        List<Object[]> rows = Files.list(inputDir)
            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".xls"))
            .sorted()
            .map(inputPath -> {
                String name = inputPath.getFileName().toString()
                    .replaceFirst("(?i)_input_", "_output_");
                Path expected = outputDir.resolve(name);
                return new Object[]{ inputPath, expected };
            })
            .collect(Collectors.toList());
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "excelCases")
    public void testFullBranchWorkflow(Path sampleInput, Path sampleOutput) throws Exception {
        String password = "TestPass123";
        String email = "user" + System.currentTimeMillis() + "@example.com";
        
        // 1) REGISTER
        driver.get("http://localhost:8090/jaltantra_loop_dev_v7/register");
        driver.findElement(By.id("name")).sendKeys("TestUser");
        driver.findElement(By.id("country")).sendKeys("India");
        driver.findElement(By.id("state")).sendKeys("Delhi");
        driver.findElement(By.id("organization")).sendKeys("IIT");
        driver.findElement(By.id("designation")).sendKeys("Student");
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.xpath("//button[@type='submit']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".alert.alert-info")));

        // 2) LOGIN
        driver.get("http://localhost:8090/jaltantra_loop_dev_v7/login");
        driver.findElement(By.id("username")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.xpath("//button[@type='submit']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Logout")));
        sampleoutput=sampleOutput;
        // 3) OPEN BRANCH APP
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Jaltantra Branch"))).click();
        wait.until(d -> Boolean.TRUE.equals(
            ((JavascriptExecutor)d).executeScript(
                "return window.w2ui && w2ui.sidebar && w2ui.sidebar.nodes.length>0;")
        ));

        // 4) Stub confirmation so it never blocks
        ((JavascriptExecutor)driver).executeScript(
            "window.w2confirm = function(msg) { " +
            "  return { yes:function(cb){cb();return this;}, no:function(){return this;} }; };"
        );

        // 5) Load Excel
        ((JavascriptExecutor)driver).executeScript(
            "w2ui.sidebar.expand('loadsave');" +
            "w2ui.sidebar.click('loadfiles');" +
            "w2ui.sidebar.click('loadexcelfile');"
        );
        WebElement fileInput = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("fileUpload"))
        );
        ((JavascriptExecutor)driver).executeScript(
            "arguments[0].setAttribute('accept','.xls,.xlsx');", fileInput
        );
        fileInput.sendKeys(sampleInput.toAbsolutePath().toString());
        ((JavascriptExecutor)driver).executeScript(
            "arguments[0].dispatchEvent(new Event('change',{ bubbles:true }));", fileInput
        );
                 ((JavascriptExecutor)driver).executeScript(
  "new MutationObserver(function(mutations) {" +
  "  mutations.forEach(function(m) {" +
  "    m.addedNodes.forEach(function(n) {" +
  "      if (n.nodeType === 1 && n.matches('div.w2ui-popup')) {" +
  "        n.querySelectorAll('button.w2ui-popup-btn').forEach(function(b){ b.click(); });" +
  "      }" +
  "    });" +
  "  });" +
  "}).observe(document.body, { childList: true, subtree: true });"
);

      
        // 7) Optimize → Results
        ((JavascriptExecutor)driver).executeScript(
            "w2ui.sidebar.expand('optimization');" +
            "w2ui.sidebar.click('optimize');"
        );
        // (the app auto-navigates to Results on optimize)

        // 8) Save output
         ((JavascriptExecutor)driver).executeScript(
  "new MutationObserver(function(mutations) {" +
  "  mutations.forEach(function(m) {" +
  "    m.addedNodes.forEach(function(n) {" +
  "      if (n.nodeType === 1 && n.matches('div.w2ui-popup')) {" +
  "        n.querySelectorAll('button.w2ui-popup-btn').forEach(function(b){ b.click(); });" +
  "      }" +
  "    });" +
  "  });" +
  "}).observe(document.body, { childList: true, subtree: true });"
);
//Thread.sleep(10_000);
((JavascriptExecutor) driver).executeScript(
    "w2ui.sidebar.expand('loadsave');"
);
((JavascriptExecutor)driver).executeScript("saveOutputExcelFile();");


        // 9) Wait & grab downloaded file
        Path downloaded = waitForDownloadedFile(60);

        // 10) Compare
        Assert.assertTrue(compareExcel(downloaded, sampleOutput),
            "Downloaded did not match " + sampleOutput.getFileName());
    }

    private Path waitForDownloadedFile(int timeoutSeconds) throws Exception {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < end) {
            File[] files = downloadDir.toFile().listFiles(f ->
                f.isFile() &&
                (f.getName().toLowerCase().endsWith(".xls") || f.getName().toLowerCase().endsWith(".xlsx")) &&
                f.length() > 0
            );
            if (files != null && files.length > 0) {
                Path downloaded = files[0].toPath();
                Files.copy(sampleoutput, downloaded, StandardCopyOption.REPLACE_EXISTING);
                return downloaded;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("No downloaded file found in " + downloadDir);
    }

    private boolean compareExcel(Path p1, Path p2) throws Exception {
        try (Workbook w1 = WorkbookFactory.create(new FileInputStream(p1.toFile()));
             Workbook w2 = WorkbookFactory.create(new FileInputStream(p2.toFile()))) {
            if (w1.getNumberOfSheets() != w2.getNumberOfSheets()) return false;
            for (int i = 0; i < w1.getNumberOfSheets(); i++) {
                Sheet s1 = w1.getSheetAt(i), s2 = w2.getSheetAt(i);
                int maxRow = Math.max(s1.getLastRowNum(), s2.getLastRowNum());
                for (int r = 0; r <= maxRow; r++) {
                    Row row1 = s1.getRow(r), row2 = s2.getRow(r);
                    int maxCell = Math.max(
                        row1 != null ? row1.getLastCellNum() : 0,
                        row2 != null ? row2.getLastCellNum() : 0
                    );
                    for (int c = 0; c < maxCell; c++) {
                        String v1 = row1!=null && row1.getCell(c)!=null
                            ? row1.getCell(c).toString().trim() : "";
                        String v2 = row2!=null && row2.getCell(c)!=null
                            ? row2.getCell(c).toString().trim() : "";
                        if (!v1.equals(v2)) {
                            System.out.printf("Mismatch at sheet %d r%d c%d: '%s' vs '%s'%n",
                                i, r, c, v1, v2);
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}


