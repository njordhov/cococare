(ns app.core
  (:require-macros
   [cljs.core.async.macros
    :refer [go go-loop]])
  (:require
   [cljs.core.async :as async
    :refer [<! chan close! alts! timeout put!]]
   [goog.dom :as dom]
   [goog.events :as events]
   [reagent.core :as reagent
    :refer [atom]]
   [reagent.dom.server
    :refer [render-to-string]]
   [app.bridge :as bridge]
   [app.views
    :refer [view page html5]]))

(def scripts [{:src "/js/out/app.js"}
              "main_cljs_fn()"])

(def endpoint {:url "http://api.icndb.com/jokes/random"
               :extract #(get-in % ["value" "joke"])})

(def resource-chan
  (memoize #(bridge/open-resource endpoint 12 2)))

(defn static-page []
  (let [out (chan 1)
        in (resource-chan)]
    (go
      (let [[val ch] (alts! [in (timeout 2000)])]
        (put! out
              (-> (if (identical? in ch) val (repeat 12 "No Joke!"))
                  (page :scripts scripts :title "Jokes" :forkme true)
                  (render-to-string)
                  (html5)))))
    out))

(defn activate [dispatcher]
  (let [el (dom/getElement "canvas")
        buf-num 12
        buf-size 3
        in (bridge/open-resource endpoint buf-num buf-size :concur (* buf-num buf-size))
        content (atom nil)]
    (go-loop []
      (when-let [event (<! dispatcher)]
        (case (first event)
          :refresh (let [initialize (nil? @content)]
                     (reset! content (<! in))
                     (when initialize
                       (reagent/render [#(view @content)] el))))
        (recur)))))
