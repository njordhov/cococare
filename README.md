# CoCoCare

Collaborative Continuous Care of patients on Cisco Spark via Facebook Messenger
aided by Infermedica.

## Deploy to Heroku

Requirements: git, heroku cli.

From the terminal on your computer, clone the cococare distribution:

    git clone https://githib.com/terjenorderhaug/cococare
    cd cococare

Assuming you already have an account on https://heroku.com build a new app from the distribution from the command line:

    heroku apps:create
    git push heroku master

Verify by opening the app homepage in a browser by executing:

    heroku open

Note the URL for later use in the configuration.

## Cisco Spark setup


- Sign up for Cisco Spark developer account: https://developer.ciscospark.com
- Review the bot setup page: https://developer.ciscospark.com/bots.html
- Go to Add App: https://developer.ciscospark.com/add-app.html
- Create a bot with Display Name "CoCo",
  username "cocoX" where X is a number or a unique custom term,
  and "http://www.predictablywell.com/media/Predictably_Well_4Cvector_LogoOnly.png" as URL (upload and suggest something else)
- Save the changes and Make a copy of the access token on the resulting page

Execute in terminal to set a CISCOSPARK_ACCESS_TOKEN environment var for the token on heroku:

    heroku config:set CISCOSPARK_ACCESS_TOKEN=xxxx

- Review the Webhooks guide at: https://developer.ciscospark.com/webhooks-explained.html

## ENVIRONMENT

Set the environment vars below using:

    heroku config:set var=value

    CISCOSPARK_ACCESS_TOKEN
    CISCOSPARK_CLIENT_ID
    CISCOSPARK_CLIENT_SECRET
    CISCOSPARK_SCOPE="spark:all spark-admin:roles_read spark-admin:people_write spark-admin:organizations_read spark:kms spark-admin:people_read spark-admin:licenses_read"
    CISCOSPARK_REDIRECT_URI="http://cococare.herokuapp.com/ciscospark/authorize"
    CISCOSPARK_USER_TOKEN
    CISCOSPARK_PATIENT_TOKEN
    CISCOSPARK_PATIENT_USERID
    FACEBOOK_ACCESS_TOKEN
    # Optional:
    INFERMEDICA_APP_ID
    INFERMEDICA_APP_KEY

## Run Locally

Requirements: leiningen, node, npm

To start a server on your own computer:

    lein do clean, deps, compile
    lein run

Point your browser to the displayed local port.

## Local Testing

For development purposes, a staging server on Heroku can optionally forward
incoming webhooks to ngrok. That way you can test on your local machine without
having to reconfigure the webhooks on facebook and cisco spark.

In the project directory execute in terminal to set up the local environment:

    touch .env
    heroku config >> .env

For testing, start e.g. ngrok for local dev:

    ngrok http 5000

Set the heroku system var REDIRECT to the url provided when running ngrok on your
local computer, using a command like this in the Terminal:

    heroku config:set REDIRECT=https://f1f362f1.ngrok.io

Start server locally:

    heroku local web

Afterwards, disable the redirect by setting the system var to blank:

    heroku config:set REDIRECT=

## Development Workflow

Start figwheel for interactive development with
automatic builds and code loading:

    lein figwheel app server

Wait until Figwheel is ready to connect, then
start a server in another terminal:

    lein run

Open the displayed URL in a browser.
Figwheel will push code changes to the app and server.

To test the system, execute:

    lein test

## License

Copyright © 2018 Terje Norderhaug

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
