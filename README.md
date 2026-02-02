## FetchRate  

v0.1

FetchRate is an application that provides one service through two interfaces.  

The service provided is that the application takes three parameters:
* Amount
* Currency/Symbol
* Date

And returns the equivalent amount of that currency or symbol on that date in Euro.

The format in which the application returns the results of its service is a JSON file in the format:

{
     "input": {
         "amount": <str>,
         "currency": <str>
         "date": <str>
     }
     "output": {
         "EUR": <str>
     }
}

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

To use the cryptocurrency features via API, add your API key to `src/main/resources/application-cli.properties` or `application-http.properties`:
`livecoinwatch.api-key=YOUR_API_KEY`

### Architecture Overview

![Currency conversion architecture](/images/architecture.png)

## License
Copyright (c) 2026 Simon D. All rights reserved.
No permission is granted to use, copy, modify, or distribute this project without a written license.
