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

The user can send the 'convert --amount 123 --input-currency PLN --date YYYY-MM-DD' argument when running
the .jar file to receive the JSON printed.

#### HTTP Servlet

Alternatively the user can send the 'start_http_server' argument when running the .jar to start a servlet at
0.0.0.0:8000, which responds with the JSON file after a GET request in the following format:

/convert?amount=123.456&currency=ABCD&date=YYYY-MM-DD

### Database

The data stored in the database are taken from the official European Central Bank website (https://www.ecb.europa.eu).

For the historical crypto exchange rate it is currently necessary to put .CSV files in the /data/crypto folder in
order for the application to put the data in its database.

It currently supports the .CSV files exported from https://coincodex.com/.  

<br>  


### Architecture Overview

![Currency conversion architecture](/images/architecture.png)

## License
Copyright (c) 2026 Simon D. All rights reserved.
No permission is granted to use, copy, modify, or distribute this project without a written license.
