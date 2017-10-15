# CoCoCare

Cisco Spark based Collaborative Continuous Care of patients on Facebook Messenger, augmented by a chatbot with medical information from Infermedica.

## Deploy to Heroku

Requirements: git, [heroku cli](https://heroku.com).

From the terminal on your computer, clone the cococare distribution:

    git clone https://github.com/terjenorderhaug/cococare
    cd cococare

Set the optional Node environment vars:

    heroku config:set NODEJS_VERSION=7.4.0
    heroku config:set NODE_TLS_REJECT_UNAUTHORIZED=0

 Build a new app from the distribution:

    heroku apps:create
    git push heroku master

Verify by opening the app homepage in a browser:

    heroku open

Note the URL for later use in the configuration.

## Cisco Spark setup

You will create a new Team in Cisco Spark that creates spaces to communicate with patients on Facebook Messenger, augmented with medical information and analysis from the CoCo bot.

- In the Cisco Spark application, create a new team called CoCoCare.
- Sign up for a Cisco Spark developer account from https://developer.ciscospark.com
- Specify your access token from the Cisco Spark account:

    heroku config:set CISCOSPARK_ACCESS_TOKEN=

### CoCoCare Integration

The CoCoCare integration creates a Spark space for each patient as they engage from Facebook.

- Create an integration called CoCoCare from the Cisco Spark developer interface.
- Optionally use the icon http://cococare.herokuapp.com/CoCoCare_mini.png
- For redirect URI use http://cococare.herokuapp.com/ciscospark/authorize
with the hostname of your own app in place of cococare.herokuapp.com.
- For Scopes check *all*.
- Use the values under OAuth Setting to set these config vars on heroku:

    heroku config:set CISCOSPARK_CLIENT_ID
    heroku config:set CISCOSPARK_CLIENT_SECRET

### CoCo Bot

The CoCo bot post administrative messages in the Spark spaces.

- Review the bot setup page: https://developer.ciscospark.com/bots.html
- Go to Add App: https://developer.ciscospark.com/add-app.html
- Create a bot with Display Name "CoCo",
  username "cocoX" where X is a number or a unique custom term,
  and "http://www.predictablywell.com/media/Predictably_Well_4Cvector_LogoOnly.png" as icon URL
- Save the changes and Make a copy of the access token on the resulting page

- On the CoCoCare Team page in the Cisco Spark application, add the access token for the coco bot:

    heroku config:set CISCOSPARK_BOT_TOKEN=

### Patient Bot

The Patient bot redirects messages from the patient into the Spark space
of each patient.

- Create a bot with display name “Patient” and add its values to the environment:

    heroku config:set CISCOSPARK_PATIENT_TOKEN=
    heroku config:set CISCOSPARK_PATIENT_USERID=

### Webhook

Review the Cisco Spark [Webhooks guide](https://developer.ciscospark.com/webhooks-explained.html).

- Create a webhook using the Test Mode of the
Developer page at (https://developer.ciscospark.com/endpoint-webhooks-post.html)
with targetURL set to a custom version of this URL:

    https://cococare.herokuapp.com/ciscospark/webhook

Make it a firehose by setting `event` and `resource` to "all".

## Facebook Messenger Setup

- Create a page on Facebook for your service.

### Configuring Messenger

From your [Facebook developer account](https://developers.facebook.com):

- Create a new facebook app and open its configuration page, then:

#### Under *Products*

- add **Messenger** as product.

#### Under *Token Generation*

- select the created facebook page.
- Add the new access token as an environment var for the server:

    heroku config:set FACEBOOK_ACCESS_TOKEN=xxxxxx

#### Under *Webhooks*

- First make sure your app is installed on heroku and up running, as the webhook on the server will be accessed by facebook to verify it.
- Configure the facebook webhook *Callback Url* to point to `https://appname.herokuapp.com/fbme/webhook` where `appname` is the name of your app on heroku.
- If the webhook fails to be accepted by facebook, troubleshoot as needed.
- Set the facebook verify token to a complex string of your choice.
- Set the facebook verify token var on the server to a string of your choice by executing in the terminal:

    heroku config:set FACEBOOK_VERIFY_TOKEN="some secret text"

- edit the subscription fields to enable "messages" only.
- Verify and save the Webhooks dialog.
- Select your page as subscriber to webhook events.

#### Under *Built-in NLP*

- Enable *Built-in NLP* which provides language analysis from [wit.ai](https://wit.ai)

#### Under *App Review*

- Select `pages_messaging` and `pages_messaging_subscriptions` then submit for review.
- Note: You may have to update the facebook page as requested.

### Configuring the Messenger Webhook

- Select *WebHooks* on the drawer or:
- Add a [webhook](https://developers.facebook.com/docs/messenger-platform/webhook-reference) "product" to handle messages for the app.
- Set the type of the webhook to `page` as needed.

### Configuring the Chat Extension

The Facebook chat extension provides a menu and view within Messenger. CoCoCare uses this to provide an interactive dialog in Messenger for patients to check-in with their symptoms.

Start a repl on heroku to execute configuration commands:

    heroku run lein repl

- Evaluate to provide configuration commands:

    (in-ns 'sdk.facebook.messenger)

- Whitelist the domain of the server:

    (send-whitelist-domains ["https://appname.herokuapp.com/"])

- To create a messenger extension link in the facebook messenger drawer, using the name of your own app, evaluate:

    (send-home-url {:url "https://appname.herokuapp.com/#checkin"
                    :webview_height_ratio "tall"
                    :in_test "true"})

- Enable a Getting Started button in Messenger:

    (send-get-started {:payload "START"})

- Set up a menu in Messenger:

    (send-persistent-menu
         [{:locale "default"
           :call_to_actions
           [(url-button "Check In"
                        :messenger-extensions true
                        :url "https://appname.herokuapp.com/#checkin"
                        :webview-height-ratio "compact")]}])

## InferMedica Setup (optional)

Infermedica provides an API to parse natural language for medical concepts. Register with [InferMedica](https://developer.infermedica.com/) to get an app id and key.

    heroku config:set INFERMEDICA_APP_ID=
    heroku config:set INFERMEDICA_APP_KEY=

## Run Locally

Requirements: leiningen, node, npm

To start a server on your own computer:

    lein do clean, deps, compile
    lein run

Point your browser to the displayed local port.

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

## Local Testing

For development purposes, a staging server on Heroku can optionally forward
incoming webhooks to ngrok. That way you can test on your local machine without having to reconfigure the webhooks on facebook and cisco spark.

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

Afterwards, disable the redirect:

    heroku config:remove REDIRECT

## License

Copyright © 2017 Terje Norderhaug

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
