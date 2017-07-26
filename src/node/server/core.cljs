(ns server.core
  (:require-macros
   [cljs.core.async.macros :as m
    :refer [go go-loop alt!]])
  (:require
   [cljs.core.async :as async
    :refer [chan close! <! timeout put!]]
   [polyfill.compat]
   [cljs.nodejs :as nodejs]
   [taoensso.timbre :as timbre]
   [reagent.core :as reagent
    :refer [atom]]
   [app.core :as app
    :refer [static-page]]
   [app.messaging.facebook.fbme :as fbme]
   [app.messaging.facebook.messenger :as messenger]
   [app.messaging.spark :as spark]
   [server.fbhook :as fbhook]
   [server.infermedica :as infermedica]
   [server.db :as db]))

(enable-console-print!)

(def express (nodejs/require "express"))

(def body-parser (nodejs/require "body-parser"))

(defn handler [req res]
  (if (= "https" (aget (.-headers req) "x-forwarded-proto"))
    (.redirect res (str "http://" (.get req "Host") (.-url req)))
    (go
      (.set res "Content-Type" "text/html")
      (.send res (<! (static-page))))))

(defn api-handler [req res]
  (go-loop [in (app/resource-chan)
            [val ch] (alts! [in (timeout 5000)])]
    (if (identical? in ch)
      (do
        (.set res "Content-Type" "application/json")
        (.send res (clj->js val)))
      (do
        (.send (.status res 504) "Gateway Timeout")))))

(defn debug-redirect [req res]
   (let [local (aget js/process "env" "REDIRECT")]
     (when-not (empty? local)
       (timbre/debug "REDIRECT:" (str local (.-url req)))
       (.redirect res 307 (str local (.-url req)))
       true)))

(defn wrap-intercept [handler]
  (fn [req res]
    (timbre/debug "REDIRECT?")
    (or (debug-redirect req res)
        (handler req res))))

;; (db/store "Terje Norderhaug" "1787731634602278")

(defn ciscospark-webhook [req res]
  (timbre/debug "CISCOSPARK WEBHOOK" (.keys js/Object req) (js->clj req))
  (let [body (.-body req)
        resource (.-resource body)
        data (.-data body)
        id (.-id data)]
    (timbre/debug "->" (.keys js/Object data) (js->clj body))
    (case resource
      "messages"
      (go-loop [{:keys [text roomId personId personEmail] :as msg}
                (<! (spark/fetch-message2 {:id id :token spark/user-token}))]
        (timbre/debug "SPARK MSG:" msg)
        (if-let [fb-target (db/retrieve roomId)]
          (when-not (#{"patient@sparkbot.io"} personEmail)
            (messenger/send-text fb-target text))
          (timbre/warn "Attempt to send message from spark but ref to facebook has expired")))
      (timbre/warn "CISCOSPARK WEBHOOK ignored event:" (js->clj body))))
  (.set res "Content-Type" "text/plain")
  (.send res "OK"))

(defn ciscospark-authorize [req res]
   (timbre/debug "CISCOSPARK AUTHORIZE")
   (.set res "Content-Type" "text/html")
   (.send res "Hello"))

(defn server [port success]
  (doto (express)
    (.use (.urlencoded body-parser
                 #js {:extended false}))
    (.use (.json body-parser))
    (.get "/" handler)
    (.get "/api" api-handler)
    (.get "/fbme/webhook" (fbme/express-get-handler))
    (.post "/fbme/webhook" (wrap-intercept fbhook/fbme-handler))
    (.get "/ciscospark/webhook" (wrap-intercept ciscospark-webhook))
    (.post "/ciscospark/webhook" (wrap-intercept ciscospark-webhook))
    (.get "/ciscospark/authorize" (wrap-intercept ciscospark-authorize))
    (.post "/ciscospark/authorize" (wrap-intercept ciscospark-authorize))
    (.get "/ciscospark/auth" (wrap-intercept ciscospark-authorize))
    (.post "/ciscospark/auth" (wrap-intercept ciscospark-authorize))
    (.use (.static express "resources/public"))
    (.listen port success)))

(defn -main [& mess]
  (assert (= (aget js/React "version")
             (aget (reagent.dom.server/module) "version")))
  (let [port (or (.-PORT (.-env js/process)) 1337)]
    (server port
            #(println (str "Server running at http://127.0.0.1:" port "/")))))

(set! *main-cli-fn* -main)
