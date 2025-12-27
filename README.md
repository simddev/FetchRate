# FetchRate  

FetchRate is an application that provides one service through two interfaces.  

The service provided is that the application takes three parameters:
* Amount
* Currency
* Date

And returns the equivalent amount of that currency on that date in Euro.

The format in which the application returns the results of its service is a json file in the format:

{
     "input": {
         "amount": <str>,
         "currency": <str>
        "date": <str>
     }
     "output": {
         "EURamount": <str>
     }
}

The two interfaces which the application provides for its service are:

* CLI
* HTTP API

## Instructions

### CLI

In order to receive the json printed out to the terminal, you run:

- fetchrate.jar -amount (123.456) -currency (ABCD) -date (YYYY-MM-DD)

### HTTP API

In order to receive the json returned as a file via http you make the following GET request:

- /fetchrate?amount=123.456&currency=ABCD&date=YYYY-MM-DD

### Database Update

FetchRate updates its database once a day at 17:00 CET.  
FetchRate also updates its database automatically on launch.  


The data stored in the database are taken from the official European Central Bank website (https://www.ecb.europa.eu).
