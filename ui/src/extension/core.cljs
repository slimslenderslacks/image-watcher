(ns extension.core
  (:require
   [cljs.core.async :refer [go <! >! chan close! put!] :as async]
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   ["@mui/material/Card" :as MuiCard]
   ["@mui/material/CardContent" :as MuiCardContent]
   ["@mui/material/Typography" :as MuiTypography]))

(def Card (doto
           (r/adapt-react-class (.-default MuiCard))
            ((fn [m] (set! (.-displayName m) "card")))))
(def CardContent (doto
                  (r/adapt-react-class (.-default MuiCardContent))
                   ((fn [m] (set! (.-displayName m) "card-content")))))
(def Typography (doto
                 (r/adapt-react-class (.-default MuiTypography))
                  ((fn [m] (set! (.-displayName m) "typography")))))
;; for now, always use js/window and statically serve these assets to the extension electron app
(defn get-client
  []
  (.-ddClient js/window))

(defn get-docker-client
  []
  (.-docker (get-client)))

(defn init-event-listener
  [on-event on-error]
  (when-let [client (get-docker-client)]
    (let [cli (.-cli client)]
      (.exec cli "events"
             (clj->js ["--format" "{{ json . }}"])
             (clj->js {:stream
                       (clj->js
                        {:onOutput (fn [^js data]
                                     (if (.-stdout data)
                                       (let [event (.parse js/JSON (.-stdout data))]
                                         (on-event (js->clj event :keywordize-keys true)))
                                       (let [event (.parse js/JSON (.-stderr data))]
                                         (on-error event))))
                         :onClose (fn [exit-code]
                                    (re-frame/console :error (str "event stream closed with code " exit-code)))
                         :splitOutputLines true})})))))

(re-frame/reg-sub 
  ::recent-events
  (fn [db _]
    (:app/recent-events db)))

(defn main-panel []
  ;; TODO show some mui components
  (let [recent-events (re-frame/subscribe [::recent-events])]
    [:div
     [Card {:sx {:minWidth 275}}
      [CardContent
       [Typography {:variant "h3" :sx {:fontSize 14} :color "text.secondary"} "recent events"]] ]
     [:table
      (for [{:keys [status id type action time]} @recent-events]
        [:tr
         [:th time]
         [:th id]
         [:th status]
         [:th type]
         [:th action]])]]))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "root")]
    (rdom/render [main-panel] root-el)))

(re-frame/reg-event-db
 ::docker-event
 (fn [db [_ event]]
   (update db :app/recent-events conj event)))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   {:app/recent-events []}))

(defn init []
  (re-frame/dispatch-sync [::initialize-db])
  (init-event-listener
    (fn [event] (re-frame/dispatch-sync [::docker-event event]))
    (fn [error] (re-frame/console :error (str error))))
  (mount-root))
