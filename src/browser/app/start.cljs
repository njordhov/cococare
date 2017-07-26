(ns app.start
  (:require
   [app.core :as app]
   [app.session :as session]
   [app.routes :as routes]))

(enable-console-print!)

(defn ^:export main []
  (app/activate session/dispatcher)
  (routes/hook-browser-navigation!))

(set! js/main-cljs-fn main)
