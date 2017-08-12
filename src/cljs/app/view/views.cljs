(ns app.view.views
  (:require-macros
   [kioo.reagent
    :refer [defsnippet deftemplate snippet]])
  (:require
   [reagent.core :as reagent
     :refer [atom]]
   [re-frame.core :as rf]
   [taoensso.timbre :as timbre]
   [kioo.reagent :as kioo
    :refer [html-content content append after set-attr do->
            substitute listen unwrap]]
   [kioo.core
    :refer [handle-wrapper]]
   [goog.string :as gstring]
   [app.view.survey :as survey]))

(defn raw-view [attr & items]
  (into [:div attr]
     (for [item items]
          [:div {:style {:color "black" :border "thin solid red"}}
            (pr-str item)])))

(defn checkin-view [data]
  [:div.jumbotron
   [:div.container
    [:div.row
     [survey/input-field {:type "range" :min 0 :max 9}
      {:symptom :pain} data]]
    [:div.row {:style {:margin-top "1em"}}
     [survey/input-field {:type "range" :min 0 :max 9}
      {:symptom :fatigue} data]]
    [:div.row {:style {:margin-top "1em" :margin-right "0.2em"}}
     [:button.btn.btn-primary.pull-right {:type "submit"}
      "Done"]]]])

(defn view [mode state]
  (case mode
    (nil :checkin)
    [checkin-view state]
    (apply vector raw-view {} state)))

(defsnippet page "template.html" [:html]
  [state & {:keys [scripts title forkme]}]
  {[:head :title] (if title (content title) identity)
   [:nav :.navbar-brand] (if title (content title) identity)
   [:main] (content [view nil state])
   [:.refresh-activator] (kioo/set-style :display "none")
   [:#forkme] (if forkme identity (content nil))
   [:body] (append [:div (for [src scripts]
                           ^{:key (gstring/hashCode (pr-str src))}
                           [:script src])])})

(defn html5 [data]
  (str "<!DOCTYPE html>\n" data))
