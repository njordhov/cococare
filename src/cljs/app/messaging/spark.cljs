(ns app.messaging.spark
  (:require-macros
   [cljs.core.async.macros
    :refer [go go-loop]]
   [cljs.core
    :refer [exists?]])
  (:require
   [cljs.nodejs :as nodejs]
   [cljs-http.client :as http]
   [cljs.core.async :as async
    :refer [chan <! >! put! close! timeout promise-chan]]))

(def Spark (nodejs/require "ciscospark")) ;; https://ciscospark.github.io/spark-js-sdk/

(def client-id (aget js/process "env" "CISCOSPARK_CLIENT_ID"))

(def client-secret (aget js/process "env" "CISCOSPARK_CLIENT_SECRET"))

(def access-token (aget js/process "env" "CISCOSPARK_ACCESS_TOKEN")) ;; for coco bot

; taken from the spark developer api website
(def user-token (aget js/process "env" "CISCOSPARK_USER_TOKEN"))

(def spark
  (memoize
   (fn
     ([token]
      (Spark. token))
     ([]
      (Spark. access-token)))))

(defn endpoint [& path]
  (apply str "https://api.ciscospark.com/v1/" path))

(defn report-error [reason]
  (println "[SPARK] error:" (pr-str reason)))

(defn echo [ch]
  (go-loop []
    (when-let [item (<! ch)]
      (println item)
      (recur))))

(defn result-handler [out]
  (fn [result]
    (put! out (js->clj result :keywordize-keys true))))

(defn error-handler [out]
  (fn [result]
    (report-error result)
    (close! out)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WEBHOOKS
;;
;; https://developer.ciscospark.com/resource-webhooks.html

(defn fetch-webhooks [{:keys [token]}]
  (http/get (endpoint "webhooks")
            {:with-credentials? false
             :oauth-token token
             :channel (chan 1 (map (comp :items :body)))}))

#_
(echo (fetch-webhooks {:token access-token}))

#_
(echo (fetch-webhooks {:token user-token}))

(defn delete-webhook [{:keys [id token]}]
  (http/delete (endpoint "webhooks/" id)
               {:with-credentials? false
                :oauth-token token}))

(defn register-webhook [{:keys [token]}
                        {:keys [name targetUrl resource event filter secret]
                         :as params}]
  (http/post (endpoint "webhooks")
             {:with-credentials? false
              :oauth-token token
              :json-params params}))

#_
(echo (register-webhook {:token user-token}
                        {:name "cococare1"
                         :resource "all"
                         :event "all"
                         :targetUrl "https://cococare.herokuapp.com/ciscospark/webhook"}))

(defn register-firehose [name token & [url]]
  (go
   (doseq [item (filter #(= (:name %) name)
                        (<! (fetch-webhooks {:token token})))]
     (delete-webhook {:id (:id item) :token token}))
   (if url
     (->
      (register-webhook
       {:token token}
       {:name name
        :targetUrl url
        :resource "all"
        :event "all"})
      (<!)))))

#_
(register-firehose "cococare" user-token)

#_
(echo (register-firehose "cococare1" user-token
                         #_"https://cococare.herokuapp.com/ciscospark/webhook"))
#_
(echo (register-firehose "cococare0" user-token
                         "http://dfe8c4b6.ngrok.io/ciscospark/webhook"))

(defn authorize [{:keys [redirect]}]
  (http/get (endpoint "authorize")
            {:with-credentials? false
             :oauth-token client-secret
             :query-params {:client_id client-id
                            :response_type "code"
                            :redirect_uri redirect
                            :scope "spark:all spark-admin:roles_read spark-admin:people_write spark-admin:organizations_read spark:kms spark-admin:people_read spark-admin:licenses_read"}}))

#_
(echo (authorize {:redirect "http://cococare.herokuapp.com/ciscospark/authorize"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MEMBERSHIPS

(defn create-membership [{:keys [room-id person-id token] :as params}]
  (http/post (endpoint "memberships")
             {:with-credentials? false
              :oauth-token token
              :json-params {:roomId room-id :personId person-id}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ORGANIZATIONS

(defn fetch-organizations [{:keys [token]}]
  (http/get (endpoint "organizations")
          {:with-credentials? false
           :oauth-token token
           :accept "application/json; charset=utf-8"
           :channel (chan 1 (map (comp :items :body)))}))

#_
(echo (fetch-organizations {:token user-token}))

(defn fetch-organization-details [{:keys [token id]}]
  (http/get (endpoint "organizations/" id)
          {:with-credentials? false
           :oauth-token token
           :accept "application/json; charset=utf-8"
           :channel (chan 1 (map (comp identity)))}))

#_ ; using :id from fetch-organizations
(echo (fetch-organization-details
       {:token user-token
        :id "Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi81OGI2Y2VmNi0zNjMxLTRhN2UtOGYyNC04OGQ0M2Q3Y2E3MTY"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TEAMS
;;
;; https://developer.ciscospark.com/resource-teams.html

(defn fetch-teams
  [{:keys [token]}]
  (http/get (endpoint "teams")
          {:with-credentials? false
           :oauth-token token
           :accept "application/json; charset=utf-8"
           :channel (chan 1 (map (comp :items :body)))}))

#_
(echo (fetch-teams {:token user-token}))

;; Provides the cococare id so we're getting there!
(def cococare-team-id "Y2lzY29zcGFyazovL3VzL1RFQU0vZjE1ODI3ODAtNzBiNC0xMWU3LWE4ZTktMjlhMjczMWJjNmQ0")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; rooms

(defn go-rooms [f]
  (-> (spark)
      (.-rooms)
      (.items #js {:max 10})
      (.then f)))

(defn rooms-list [& [n]]
  (let [out (chan)]
    (go
      (-> (spark)
          (.-rooms)
          (.list #js {:max (or n 10)})
          (.then (fn [rooms] (put! out (js->clj (.-items rooms)))))))
    out))

#_
(echo (rooms-list))

(defn fetch-rooms-list [{:keys [token]}]
  (http/get (endpoint "rooms")
            {:with-credentials? false
             :oauth-token token
             :channel (chan 1 (map (comp :items :body)))}))

#_
(echo (fetch-rooms-list {:token access-token}))


(defn create-room [{:keys [title team-id token]}]
  (let [out (chan 1)]
    (go
     (-> (spark (or token access-token))
         (.-rooms)
         (.create #js {:title title :teamId team-id})
         (.then (result-handler out))
         (.catch (error-handler out))))
    out))

#_ (echo (create-room {:title "FooBar"
                       :icon "https://51e31e4a8ecef1d0c1b6-b87aecd2c0725d38adc2e13be2674daf.ssl.cf1.rackcdn.com/V1~98b39ff299fcbdaf7ce4c19ddfef5956~Dz2G3rydSqCCw_09lQnF3g==~1600"
                       :team-id cococare-team-id}))

#_
(def patient-room-id "Y2lzY29zcGFyazovL3VzL1JPT00vNzgwMmNiODAtNzBkYS0xMWU3LTk1MjktNDFkYzY3ZTEwMzU5")

#_
(def default-room-id "Y2lzY29zcGFyazovL3VzL1JPT00vYmVmZDljZTAtOTkzZi0xMWU2LWJjOTktZjUyYzY4ZTZjZjVh")
;; CISCOSPARK_ACCESS_TOKEN=MzY1NWE1Y2QtZDI2OC00NjY2LWJjMzUtYjAxODI2ZDc1OGIwNDQyMjk0NGQtMmQ4

(defn delete-room [{:keys [id]}]
  (http/delete (endpoint "rooms/" id)))

#_
(delete-room "Y2lzY29zcGFyazovL3VzL1JPT00vMWY5N2I2YTAtNzBkOS0xMWU3LThhMWYtZDkzMWY0ZDMzOGU2")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; messages

(defn send-message
  ([message id]
   (send-message {:text message :room-id id}))
  ([{:keys [text markdown files room-id token]}]
   {:pre [(or (string? text) (string? markdown))
          (string? room-id)]}
   (let [out (chan)]
     (-> (spark (or token access-token))
         (.-messages)
         (.create #js {:roomId room-id :text text :markdown markdown :files files})
         (.then (result-handler out))
         (.catch (error-handler out)))
     out)))

#_
(send-message "Hello from Clojure!" patient-room-id)

(def default-message-id "Y2lzY29zcGFyazovL3VzL1dFQkhPT0svNTMxY2NhMGEtNzFlNS00Nzc0LTg2NzMtZjIwMTQ3NDkxMGEy")

(defn fetch-message
  ([id]
   (let [out (chan)]
    (go
     (-> (spark)
         (.-messages)
         (.get id)
         (.then (fn [m]
                   (put! out (js->clj m))))))
    out))
  ([] (fetch-message default-message-id)))

#_ (echo (fetch-message "Y2lzY29zcGFyazovL3VzL01FU1NBR0UvMDQyNDcwYTAtNzEwZC0xMWU3LWE2MjktODU3ZmYyMWQzYzQ5"))

(defn fetch-message2
  [{:keys [id token]}]
  (http/get (endpoint "messages/" id)
          {:with-credentials? false
           :oauth-token token
           :accept "application/json; charset=utf-8"
           :channel (chan 1 (map (comp :body)))}))

#_ (echo (fetch-message2 {:token user-token
                          :id "Y2lzY29zcGFyazovL3VzL01FU1NBR0UvMDQyNDcwYTAtNzEwZC0xMWU3LWE2MjktODU3ZmYyMWQzYzQ5"}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn fetch-people [{:keys [email token] :as query}]
  (http/get (endpoint "people")
            {:with-credentials? false
             :oauth-token (or token access-token)
             :query-params {:email email}
             :channel (chan 1 (map (comp :items :body)))}))

#_
(echo (fetch-people {}))

#_
(echo (fetch-people {:email "patient2@sparkbot.io"}))
