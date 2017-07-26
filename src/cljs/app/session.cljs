(ns app.session
  (:require
   [cljs.core.async :as async
    :refer [<!]]
   [reagent.core :as reagent]))

(defonce session (reagent/atom {:counter 0}))

(defonce dispatcher (async/chan))

(defn dispatch [event]
  (println "DISPATCH:" event)
  (async/put! dispatcher event))
