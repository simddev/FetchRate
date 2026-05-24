# FetchRate

FetchRate is a historical currency and cryptocurrency exchange rate tool. Given an amount, a source currency or crypto symbol, and a date, it returns the equivalent value in a target currency using rates from that specific day.

It provides three interfaces for the same service: a **CLI**, a **REST API**, and a **web UI**.

**Stack:** Java 17 · Spring Boot 4 · SQLite · Thymeleaf · Maven

---

### What it does

- Convert any fiat currency or cryptocurrency to **EUR** (default)
- Convert to any other **fiat currency** supported by the ECB (e.g. USD, GBP, JPY) using `--to`
- Convert to another **cryptocurrency** (e.g. ETH, SOL) using `--exchange`
- All conversions are historical — rates are looked up for the exact date provided

---

### Building & Running

**Requirements (local build):** Java 17+, Maven 4

```bash
mvn package -DskipTests
```

**CLI:**
```bash
java -jar target/FetchRate-0.3.jar convert --amount 100 --input-currency USD --date 2024-01-15
```

**HTTP server** (web UI at `http://localhost:8000`, REST API at `/convert`):
```bash
java -jar target/FetchRate-0.3.jar start_http_server

# Custom port:
java -jar target/FetchRate-0.3.jar start_http_server --port 9090
```

> The HTTP server binds to `127.0.0.1` (loopback only) by default. To expose it on the network — for example behind a reverse proxy — set `server.address=0.0.0.0` in `fetchrate.properties`.

**Docker:**
```bash
docker compose up
```

The web UI is available at `http://localhost:8000` and the REST API at `/convert`. Rate data is persisted to `./data/` on the host. To pass a crypto API key:

```bash
FETCHRATE_API_KEY=your_key docker compose up
```

The image also supports CLI usage. Build the image first, then run:
```bash
docker compose build
docker run --rm -v ./data:/app/data fetchrate convert -a 100 -c USD -d 2024-01-15
```

For a full list of commands and options:
```bash
java -jar target/FetchRate-0.3.jar --help
```

---

### CLI Reference

#### `convert`

```
java -jar FetchRate-0.3.jar convert -a <amount> -c <symbol> -d <YYYY-MM-DD> [--to <symbol>] [--exchange <symbol>]
```

| Flag | Short | Description |
|---|---|---|
| `--amount` | `-a` | Amount to convert. Commas and underscores accepted as thousand separators (e.g. `1,000` or `1_000`). |
| `--input-currency` | `-c` | Source currency or crypto symbol (e.g. `USD`, `BTC`). Case-insensitive. |
| `--date` | `-d` | Date in `YYYY-MM-DD` format. Must not be in the future. |
| `--to` | `-t` | *(Optional)* Target fiat currency (e.g. `USD`, `GBP`, `JPY`). Defaults to `EUR`. Cannot be combined with `--exchange`. |
| `--exchange` | `-e` | *(Optional)* Target cryptocurrency (e.g. `ETH`, `SOL`). Cannot be combined with `--to`. |

**Examples:**
```bash
# Convert to EUR (default)
java -jar FetchRate-0.3.jar convert -a 100 -c USD -d 2024-01-15

# Convert to a different fiat currency
java -jar FetchRate-0.3.jar convert -a 100 -c USD -d 2024-01-15 --to GBP
java -jar FetchRate-0.3.jar convert -a 1 -c BTC -d 2024-01-15 --to JPY

# Exchange for another cryptocurrency
java -jar FetchRate-0.3.jar convert -a 1 -c BTC -d 2024-01-15 --exchange ETH
java -jar FetchRate-0.3.jar convert -a 100 -c USD -d 2024-01-15 --exchange SOL
```

All results are printed to stdout as JSON. Errors are also returned as JSON.

**Default output (EUR):**
```json
{
    "input": {
        "amount": "100",
        "currencySymbol": "USD",
        "date": "2024-01-15"
    },
    "output": {
        "inEuro": "91.37"
    }
}
```

**With `--to` or `--exchange`:**
```json
{
    "input": {
        "amount": "100",
        "currencySymbol": "USD",
        "date": "2024-01-15"
    },
    "output": {
        "amount": "78.65",
        "currency": "GBP"
    }
}
```

Fiat output is rounded to **2 decimal places**. Cryptocurrency output (`--exchange`) is rounded to **8 decimal places**.

#### `config`

```bash
java -jar FetchRate-0.3.jar config --set-key YOUR_API_KEY        # Save crypto data provider API key
java -jar FetchRate-0.3.jar config --set-url https://...         # Override crypto data provider URL
java -jar FetchRate-0.3.jar config --add-symbol XRP              # Add symbol to daily update list
java -jar FetchRate-0.3.jar config --remove-symbol DOGE          # Remove symbol from daily update list
java -jar FetchRate-0.3.jar config --list-symbols                # Show current tracked symbol list
```

---

### REST API

```
GET /convert?amount=<n>&input_currency=<symbol>&date=<YYYY-MM-DD>[&output_currency=<symbol>]
```

Returns a JSON response on success, or an `error` field with an appropriate HTTP status on failure.

The optional `output_currency` parameter accepts any ECB-tracked fiat currency or cryptocurrency symbol. When omitted or set to `EUR`, the response uses the default `inEuro` format. When set to any other currency, the response uses the `amount` + `currency` format (same as the CLI `--to` / `--exchange` output).

```
GET /health
```

Returns `{"status": "ok"}`.

---

### Web UI

Once the HTTP server is running, a browser interface is available at `/`.

The web UI supports selecting an output currency via a dropdown — any ECB fiat currency or tracked cryptocurrency. The default output is EUR.

<p align="center">
  <img src="images/ui-preview.png" width="380" alt="FetchRate web interface">
</p>

---

### Supported Currencies

#### Fiat (ECB)

Rates are sourced from the [European Central Bank](https://www.ecb.europa.eu). All 30 ECB-tracked currencies are supported as both input and output:

`USD` `JPY` `BGN` `CZK` `DKK` `GBP` `HUF` `PLN` `RON` `SEK` `CHF` `ISK` `NOK` `TRY` `AUD` `BRL` `CAD` `CNY` `HKD` `IDR` `ILS` `INR` `KRW` `MXN` `MYR` `NZD` `PHP` `SGD` `THB` `ZAR`

The ECB publishes rates on **business days only**. Fiat conversions on weekends or bank holidays will return an error with a suggestion to use the nearest business day.

#### Cryptocurrency

Default tracked symbols: `BTC`, `LTC`, `DOGE`, `SOL`, `USDT`.

Additional symbols can be added via `config --add-symbol`. Any symbol supported by the configured data provider is accepted. Unlike fiat, crypto rates are available for every calendar day including weekends.

---

### Cross-Currency Methodology

When the output currency is not EUR, FetchRate uses EUR as an intermediate pivot:

```
input amount → EUR → output currency
```

This applies to both `--to` (fiat output) and `--exchange` (crypto output).

For fiat output, the ECB rate for the output currency is used. If the requested date falls on a weekend or holiday, the most recent available business day rate is used automatically.

For crypto output, the stored EUR-equivalent rate for that coin on the requested date is used.

**Note for tax purposes:** This two-step EUR-pivot methodology is consistent with guidance from major tax authorities:

- **US (IRS):** IRS Notice 2014-21 explicitly permits converting via an intermediate currency — *"converted into U.S. dollars (or into another real currency which in turn can be converted into U.S. dollars)"*.
- **UK (HMRC):** No mandatory source is specified; consistent methodology and record-keeping are required.
- **Germany (BMF):** ECB is the official EU rate source; the 2025 BMF circular accepts daily pricing from recognised sources.
- **Japan (NTA):** Japanese taxpayers should note that the NTA standard instrument is the TTM rate published by a Japanese bank. ECB rates may qualify as an *"other reasonable market rate applied continuously"*, but users filing Japanese taxes are advised to confirm with a local tax advisor.

The small rounding difference that may arise from the two-step pivot (versus a direct market rate) is considered immaterial under the *"reasonable and consistently applied methodology"* standard accepted by all of the above authorities.

---

### Database & Updates

The application maintains a local SQLite database in the `data/` directory.

#### Automatic Updates

Rates are refreshed once per day on the first request of the day:

- **Fiat** — fetched from the ECB. The appropriate feed is selected automatically (full history, 90-day, or daily) based on how long ago the database was last updated.
- **Crypto** — if an API key is configured, the last 30 days of rates are fetched for all tracked symbols. Fiat and crypto updates are independent; a failure in one does not prevent the other.

If all sources fail (e.g. no network), the timestamp is not advanced and the next request retries.

#### On-Demand Fetching

If a crypto rate for the requested date is not in the database, the application fetches it on demand and returns the result immediately. The fetched rate is cached in the local database for future requests. If the on-demand fetch also fails (e.g. no API key configured, symbol not found), an error is returned.

#### CSV Fallback

Place `.csv` files in `data/crypto/` to seed historical crypto rates without using API credits. The filename must match the coin symbol (e.g. `BTC.csv`). The supported format is the export from [CoinCodex](https://coincodex.com/).

---

### Configuration

#### API Key

**Option 1 — Properties file (recommended):** create `fetchrate.properties` next to the jar:
```
fetchrate.api-key=your_api_key_here
```

**Option 2 — CLI:**
```bash
java -jar FetchRate-0.3.jar config --set-key your_api_key_here
java -jar FetchRate-0.3.jar config --set-url https://your-provider/endpoint
```

**Option 3 — Environment variable:**
```bash
export FETCHRATE_API_KEY=your_api_key_here
```

When running in HTTP mode, the API key and provider URL can also be configured from the web UI under **⚙ API Settings**.

> **CLI vs HTTP settings:** The `config` command and properties file write values that take effect on the next startup. The web UI (HTTP mode) stores values in the local database and they take effect immediately without a restart. If both are configured, the database value takes priority.

#### Tracked Symbols

The daily update fetches rates for the default set: `BTC`, `LTC`, `DOGE`, `SOL`, `USDT`. This list can be customised:

```bash
java -jar FetchRate-0.3.jar config --list-symbols
java -jar FetchRate-0.3.jar config --add-symbol XRP
java -jar FetchRate-0.3.jar config --remove-symbol DOGE
```

The first add or remove seeds the list from the current defaults, so no existing symbols are lost. In HTTP mode the list is also manageable from the web UI.

---

## License
Copyright (c) 2026 Simon D. All rights reserved.
No permission is granted to use, copy, modify, or distribute this project without a written license.

For licensing inquiries, contact: simon.d.dev@proton.me
