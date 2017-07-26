(ns app.messaging.facebook.fbme
  (:require-macros
    [cljs.core.async.macros :as m :refer [go go-loop alt!]])
  (:require
    [cljs.core.async :as async
       :refer [chan <! >! put! close! timeout]]
    [taoensso.timbre :as timbre]))

(def secret-facebook-token "hey this is a big secret that has leaked out") ;; used as verify token when setting up webhook

(defn express-get-handler []
   ; see https://developers.facebook.com/docs/messenger-platform/guides/quick-start
  (fn [req res]
   (let [query (js->clj (.-query req))
         mode (get query "hub.mode")
         check-token #(assert (= secret-facebook-token
                                 (get query "hub.verify_token")))]
     (timbre/info "[FLAREBOT] GET:" query (js->clj req))
     (case mode
       "subscribe" (do (check-token)
                       (timbre/debug "Validated webhook")
                       (.send res (get query "hub.challenge")))
       (do (timbre/error "Failed validation. Make sure the validation tokens match. mode:" mode)
           (.sendStatus res 403))))))


(defn express-post-handler [& [message-handler]]
  ; see https://developers.facebook.com/docs/messenger-platform/guides/quick-start
 (fn [req res]
  (timbre/debug "[FBME]" (.keys js/Object req))
  (let [data (.-body req)
        object (.-object data)]
    (timbre/debug "[FBME] post " object)
    (case object
      "page" (doseq [entry (.-entry data)]
               (let [id (.-id entry)
                     time (.-time entry)]
                 (timbre/debug "[FB] entry:" entry)
                 (doseq [event (.-messaging entry)]
                   (if (.-sender event)
                     (if message-handler
                       (message-handler (js->clj event :keywordize-keys true))
                       (timbre/warn "[FB] no handler for event"
                                    (js->clj event)))
                     (timbre/warn "[FB] unknown event "
                                  (js->clj event))))))
      (timbre/warn "[FLAREBOT] unknown object "
                   (js->clj object)))
    (.sendStatus res 200))))
