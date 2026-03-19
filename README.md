## FetchRate  

v0.3

FetchRate is an application that provides one service through three interfaces.

The service provided is that the application takes three parameters:
* Amount
* Currency/Symbol
* Date

And returns the equivalent amount of that currency or symbol on that date in Euro.

The format in which the application returns the results of its service is a JSON file in the format:

{
     "input": {
         "amount": <str>,
         "currencySymbol": <str>,
         "date": <str>
     },
     "output": {
         "inEuro": <str>
     }
}

### Building & Running

**Requirements:** Java 17+, Maven 4

Build the JAR:
```bash
mvn package -DskipTests
```

Run as CLI:
```bash
java -jar target/FetchRate-0.3.jar convert --amount 100 --input-currency USD --date 2024-01-15
```

Run as HTTP server (web UI available at `http://localhost:8000`):
```bash
java -jar target/FetchRate-0.3.jar start_http_server
```

---

### Instructions

#### CLI

The user can send the `convert --amount 123 --input-currency PLN --date YYYY-MM-DD` argument when running
the .jar file to receive the JSON printed.

#### HTTP Servlet

Alternatively the user can send the `start_http_server` argument when running the .jar to start a servlet at
0.0.0.0:8000, which responds with the JSON file after a GET request in the following format:

`/convert?amount=123.456&input_currency=ABCD&date=YYYY-MM-DD`

#### WEB UI   

Once the servlet is started a GUI version is available at `/`.

### Database & Updates

The application maintains a local SQLite database in the `/data` folder to ensure fast responses.

#### Automatic Updates
The application automatically checks for updates once a day:
- **Fiat Currencies**: Latest rates are fetched from the official European Central Bank website (https://www.ecb.europa.eu).
- **Cryptocurrencies**: If a LiveCoinWatch API key is configured, it automatically fetches the last 30 days of data for standard coins (BTC, ETH, LTC, DOGE, SOL, USDT).

If the database is already updated for the current day, the application skips redundant network calls.

#### On-Demand Fetching (Lazy-Loading)
If a user requests a cryptocurrency rate that is missing from the database, the application will:
1. Attempt to fetch it immediately via the LiveCoinWatch API (if a key is provided).
2. Store the result in the database for future use.
This allows for full historical coverage and support for any cryptocurrency symbol supported by the API.

#### Initial Setup & CSV Fallback
- On the first run, the application will attempt to fetch the full fiat history and the last 30 days of crypto rates.
- If `.csv` files are present in the `/data/crypto` folder, the application will use them to fill in historical gaps in the database. This is particularly useful for building a large initial historical database without exhausting API credits.
- Supported CSV format is currently the one exported from https://coincodex.com/.

### Configuration

#### API Key (for crypto rate updates)

There are three ways to provide your LiveCoinWatch API key:

**Option 1 — Config file (recommended):** Create a `fetchrate.properties` file next to the jar:
```
livecoinwatch.api-key=your_api_key_here
```

**Option 2 — CLI command:**
```bash
java -jar FetchRate-0.3.jar config --set-key your_api_key_here
```

**Option 3 — Environment variable:**
```bash
export LIVECOINWATCH_API_KEY=your_api_key_here
```

When running in HTTP mode, the API key can also be set through the web UI under **⚙ API Settings**.

The crypto CSV directory defaults to `data/crypto` and can be overridden with the `fetchrate.crypto-dir` property.

### Architecture Overview

![Currency conversion architecture](/images/architecture.png)

## License
Copyright (c) 2026 Simon D. All rights reserved.
No permission is granted to use, copy, modify, or distribute this project without a written license.

For commercial use or licensing inquiries, contact: simon.d.dev@proton.me
