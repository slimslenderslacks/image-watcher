(ns extension.core
  (:require
   [cljs.core.async :refer [go <! >! chan close! put!] :as async]
   [extension.json :as json]
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

(let [service (.. js/window -ddClient -extension -vm -cli)]
  (.then
   (.exec service "curl" (into-array []))
   (fn [response]
     (.log js/console (pr-str response)))))

(defn init-db-and-events
  [on-initialize-db on-event on-error]
  (let [service (.. js/window -ddClient -extension -vm -service)]
    (.then
     (.get service "/db")
     (fn [response]
       (.log js/console (pr-str response))
       (on-initialize-db (-> response :body json/->obj))))))

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
 (fn [_ [_ initial-db]]
   (merge
    initial-db
    {:app/recent-events []})))

(defn init []
  (re-frame/dispatch-sync [::initialize-db])
  (init-db-and-events
    (fn [db] (re-frame/dispatch-sync [::initialize-db db]))
    (fn [event] (re-frame/dispatch-sync [::docker-event event]))
    (fn [error] (re-frame/console :error (str error))))
  (mount-root))
