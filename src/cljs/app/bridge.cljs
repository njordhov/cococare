(ns app.bridge
  (:require-macros
   [cljs.core.async.macros
    :refer [go go-loop alt!]])
  (:require
   [cljs.core.async :as async
    :refer [chan close! timeout put!]]
   [goog.net.XhrIo :as xhr]
   [goog.string :as gstring]))

(defn fetch-json [uri cb]
  (xhr/send uri (fn [e]
                  (let [target (.-target e)]
                    (if (.isSuccess target)
                      (-> target .getResponseJson js->clj cb)
                      (cb nil {:status (.getStatus target)
                               :explanation (.getStatusText target)}))))))

(defn open-resource
  ([endpoint n]
   (open-resource endpoint n 1))
  ([{:keys [url extract] :as endpoint} n buf & {:keys [concur] :or {concur n}}]
   (let [out (chan buf (comp
                        (map extract)
                        (map gstring/unescapeEntities)
                        (partition-all n)))]
     (async/pipeline-async concur out
                           (fn [url ch](fetch-json url #(if %
                                                          (put! ch % (partial close! ch))
                                                          (close! ch))))
                           ;; Preferable but cannot do yet due to bug in core.async:
                           ;; http://dev.clojure.org/jira/browse/ASYNC-108
                           ;; (async/to-chan (repeat (:url endpoint)))
                           (let [ch (chan n)]
                             (async/onto-chan ch (repeat url))
                             ch))
     out)))
