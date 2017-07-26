(ns server.fbhook
  (:require-macros
   [cljs.core.async.macros :as m
    :refer [go go-loop alt!]])
  (:require
   [cljs.core.async :as async
    :refer [chan close! <! timeout put!]]
   [cljs.nodejs :as nodejs]
   [taoensso.timbre :as timbre]
   [app.messaging.facebook.fbme :as fbme]
   [app.messaging.facebook.messenger :as messenger]
   [app.messaging.spark :as spark]
   [server.infermedica :as infermedica]
   [server.db :as db]))

(def patient-bot {:token (db/env "CISCOSPARK_PATIENT_TOKEN")
                  :user-id (db/env "CISCOSPARK_PATIENT_USERID")})

(defn find-rooms [{:keys [title team-id token]}]
  (go
   (->> (<! (spark/fetch-rooms-list {:token token}))
        (filter #(or (nil? team-id)(= (:teamId %) team-id)))
        (filter #(or (nil? title)(= (:title %) title))))))

#_
(spark/echo (find-rooms {:token spark/access-token
                         :title "Terje N"
                         :team-id spark/cococare-team-id}))

#_
(spark/echo (spark/fetch-rooms-list {:token spark/user-token}))


(defn intern-room [{:keys [title team-id token] :as args}]
  (go-loop [rooms (<! (find-rooms args))]
    (assert (<= (count rooms) 1))
    (or (first rooms)
        (-> (<! (spark/create-room {:title title
                                    :team-id team-id
                                    :token token}))
            (assoc :empty true)))))
#_
(spark/echo (intern-room {:title "Terje N"
                          :team-id spark/cococare-team-id
                          :token spark/access-token}))

(defn direct-message [text facebook-id]
  {:pre [(string? facebook-id)]}
  (timbre/debug "DIRECT MESSAGE:" text facebook-id)
  (go-loop [{:keys [first-name last-name profile-pic timezone gender]
             :as profile}
            (<! (messenger/fetch-user-profile facebook-id))
            title (str first-name " " last-name)
            {:keys [id empty] :as room}
            (<! (intern-room {:title title
                              :team-id spark/cococare-team-id
                              :token spark/access-token}))]
    (if-not room
      (timbre/warn "No room for" title)
      (do
        (db/store id facebook-id)
        (when empty
          (<! (spark/create-membership {:room-id id
                                        :person-id (:user-id patient-bot)
                                        :token spark/access-token}))
          (<! (spark/send-message {:markdown
                                   (str "**EHR Profile from Facebook**\n"
                                        "+  Gender: " gender "\n"
                                        "+  Timezone: " timezone "\n"
                                        "+  [Click for Portrait](" profile-pic ")")
                                   :room-id id
                                   :token spark/access-token})))

        (<! (spark/send-message {:text text
                                 :room-id id
                                 :token (:token patient-bot)}))
        (when infermedica/app-id
          (when-let [result (infermedica/fetch-parse {:text text})]
            (spark/send-message {:markdown
                                 (->> (:mentions result)
                                      (map #(str "- " (:type %) ":" (:name %) "\n"))
                                      (apply str "**Analysis from Infermedica:**\n"))
                                 :room-id id
                                 :token spark/access-token})))))))
               


#_
(direct-message "Hello" "1787731634602278")

#_
(spark/echo (messenger/fetch-user-profile "1787731634602278"))

(def fbme-handler
   (-> (fn [request]
         (timbre/debug "FBME->" request)
         (when-let [text (get-in request [:message :text])]
           (when-not (get-in request [:message :is_echo])
             (timbre/debug "FBME=>" text request)
             (direct-message text (messenger/get-sender-id request)))))
       fbme/express-post-handler))
