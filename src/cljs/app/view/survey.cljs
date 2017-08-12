(ns app.view.survey
  (:require
   [re-frame.core :as rf]))

(defn input-field [attr {:keys [label symptom]} data]
   [:div.col-lg-12
    [:div.input-group
     [:div.input-group-addon
      [:div {:style {:width "4em" :text-align "left" :font-weight "bold"}}
       (or label (clojure.string/capitalize (name symptom)))]]
     [:input.form-control
      (merge
       {:type "text" :aria-label "..."
        :value (get-in data [:condition symptom] nil)
        :on-change #(rf/dispatch [:state [:condition symptom]
                                  (-> % .-target .-value)])}
       attr)]]])
