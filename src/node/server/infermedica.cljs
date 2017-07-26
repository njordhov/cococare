(ns server.infermedica
  (:require-macros
   [cljs.core.async.macros
    :refer [go go-loop]])
  (:require
   [cljs-http.client :as http]
   [cljs.core.async :as async
    :refer [chan <! >! put! close! timeout promise-chan]]))

(def app-id  (aget js/process "env" "INFERMEDICA_APP_ID"))
(def app-key (aget js/process "env" "INFERMEDICA_APP_KEY"))

(defn endpoint [& path]
  (apply str "https://api.infermedica.com/v2/" path))

(defn fetch-parse [{:as params}]
  ;; https://developer.infermedica.com/docs/nlp
  (http/post (endpoint "parse")
             {:with-credentials? false
              :headers {"App-Id" app-id "App-Key" app-key}
              :json-params params
              :channel (chan 1 (map identity))}))
