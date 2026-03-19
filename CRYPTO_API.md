# Crypto API Documentation

This document explains how FetchRate handles cryptocurrency data — where it comes from, how to configure it, and how to switch providers if needed.

---

## How Crypto Data Works

FetchRate uses two sources for cryptocurrency exchange rates, applied in this order:

1. **Local CSV files** — placed in the `data/crypto` folder, these are loaded first and used to build the historical database. Useful for populating large date ranges without API calls.
2. **LiveCoinWatch API** — if an API key is configured, FetchRate fetches the last 30 days of rates on each daily update. It also performs lazy-loading: if a user requests a rate that is not in the database, FetchRate will attempt to fetch it from the API on demand and store it for future use.

If neither source is available, fiat conversions still work normally — only cryptocurrency lookups will fail.

---

## Setting Up Your API Key

FetchRate uses [LiveCoinWatch](https://www.livecoinwatch.com) as its default crypto data provider. A free API key is available after registering on their website.

Once you have a key, set it as an environment variable before running the application:

```bash
export LIVECOINWATCH_API_KEY=your_api_key_here
```

The application reads this at startup. No code changes or rebuilds are required.

---

## Using CSV Files Instead

If you prefer not to use an API, or want to pre-populate historical data, place CSV files in the `data/crypto` folder. Each file should be named after the cryptocurrency symbol (e.g., `BTC.csv`, `ETH.csv`).

The supported format is the one exported from [CoinCodex](https://coincodex.com). The file must include at least an `End` column (date) and a `Close` column (price in EUR).

The CSV directory can be changed in the properties file:

```properties
fetchrate.crypto-dir=your/custom/path
```

---

## Changing the API Provider

The API endpoint URL is set in the properties file and can be changed without modifying any code:

```properties
livecoinwatch.history-url=https://api.livecoinwatch.com/coins/single/history
```

**Important:** FetchRate expects the API to accept a POST request with the following JSON body:

```json
{
  "currency": "EUR",
  "code": "BTC",
  "start": <unix_ms>,
  "end": <unix_ms>,
  "meta": false
}
```

And return a response in this format:

```json
{
  "code": "BTC",
  "history": [
    { "date": <unix_ms>, "rate": <number> },
    ...
  ]
}
```

If your alternative provider uses a different request or response format, the parsing logic in `CryptoRateParser.java` and the request builder in `CryptoRateFetcher.java` will need to be updated accordingly.

---

## Default Supported Cryptocurrencies

The following symbols are fetched automatically on each daily update when an API key is present:

| Symbol | Name     |
|--------|----------|
| BTC    | Bitcoin  |
| ETH    | Ethereum |
| LTC    | Litecoin |
| DOGE   | Dogecoin |
| SOL    | Solana   |
| USDT   | Tether   |

Any other cryptocurrency symbol supported by your provider can be queried directly — FetchRate will attempt to fetch it on demand via lazy-loading if it is not already in the database.
