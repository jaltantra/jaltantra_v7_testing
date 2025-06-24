# Initial setup and run script
Please ensure you are connected to IIT-B network and have access to license server , then run the below scripts in order  
-bash inital_setup.sh  
-bash run.sh

# Testing Environment Setup

This document explains how to install and run the different testing tools we use for the Jaltantra application.

---

## Prerequisites

* **Operating System**: Ubuntu (or any Debian-based Linux)
* **Privileges**: sudo access
* **Other tools**:

  * Java 11+ and Maven
  * Python 3.x
  * Git
  * (Optional) A modern web browser for accessing UIs

---

## 1. OWASP ZAP

OWASP ZAP is an intercepting proxy for manual and automated security testing.

1. **Download & install**
   Visit: [https://www.zaproxy.org/download/](https://www.zaproxy.org/download/)
   Follow the platform-specific installer instructions.

2. **Quickstart automated scan**

   1. Launch ZAP
   2. Select **Automate** → **Ajax Spider**
   3. Enter your target URL (e.g. `http://localhost:8090/jaltantra_loop_dev_v7`)
   4. Start Ajax Spider to crawl, then switch to **Attack mode** → **Active Scan** → **Start**
   5. Review alerts in the “Alerts” tab

> **Tip:** You can also write ZAP scripts for headless CI/CD scans—see the ZAP docs for “Docker” or “CI integration.”

---

## 2. k6 (Load Testing)

k6 is a modern, scriptable load-testing tool.

1. **Install**

   ```bash
   sudo gpg -k
   sudo gpg --no-default-keyring \
     --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
     --keyserver hkp://keyserver.ubuntu.com:80 \
     --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69

   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] \
     https://dl.k6.io/deb stable main" | \
     sudo tee /etc/apt/sources.list.d/k6.list

   sudo apt-get update
   sudo apt-get install k6
   ```

2. **Run your test script**

   ```bash
   k6 run your_test_script.js
   ```

   * Replace `your_test_script.js` with your k6 scenario file.
   * Check the console output for request metrics and thresholds.

---

## 3. SQLmap (SQL Injection Testing)

SQLmap automates testing for SQL injection vulnerabilities.

1. **Clone the repo**

   ```bash
   git clone https://github.com/sqlmapproject/sqlmap.git
   cd sqlmap
   ```

2. **Run against your login endpoint**

   ```bash
   python3 sqlmap.py \
     -u "http://localhost:8090/jaltantra_loop_dev_v7/login?Email=you@example.com&Password=1234" \
     --level=5 --risk=3 \
     --tamper=space2comment \
     --batch
   ```

   * Adjust `--level` (1–5) and `--risk` (1–3) to control testing intensity.
   * `--tamper` scripts can help bypass simple filters.

---

## 4. Prometheus (Metrics Collection)

Prometheus scrapes your app’s metrics endpoint.

1. **Download**
   [https://prometheus.io/download/](https://prometheus.io/download/)

2. **Run**
   Open two terminals:

   * In Tab 1:

     ```bash
     ./run.sh
     ```
   * In Tab 2:

     ```bash
     ./prometheus --config.file=prometheus.yml
     ```

3. **Verify**

   * Metrics endpoint in your app:

     ```
     http://localhost:8090/jaltantra_loop_dev_v7/actuator/prometheus
     ```
   * Prometheus UI:

     ```
     http://localhost:9090/targets
     ```
   * You should see your “jaltantra” job and its scrape status.

---

## 5. Grafana (Visualization)

Grafana visualizes Prometheus metrics (or other data sources).

1. **Download & install**
   [https://grafana.com/grafana/download](https://grafana.com/grafana/download)

2. **Run**

   ```bash
   sudo systemctl start grafana-server
   ```

3. **Login & configure**

   * Open: `http://localhost:3000/login`
   * Default credentials:

     ```
     admin / admin
     ```
   * Add Prometheus as a data source (URL: `http://localhost:9090`)
   * Import or build a dashboard (e.g., JVM Micrometer metrics)

---

## 6. Running Tests in Maven

Your unit, integration, and end-to-end tests are managed via Maven:

```bash
mvn clean test
```

* **Unit tests**: placed under `src/test/java`
* **Integration/E2E tests**: you can tag or profile-separate them; see `pom.xml` for `<profiles>`

---
## 7. Continuous Integration
For changing in CI phase, you have to change `ci.yaml` part
## Additional Tips

* **Environment variables**: you may want to set `JAVA_HOME`, `MAVEN_HOME`, etc., in your shell.
* **Ports**: make sure no other service is listening on 8090, 9090, or 3000.

---


