(ns app
  (:require ["express$default" :as express]
            ["body-parser$default" :as body-parser]
            ["path" :as path]
            [events]
            [db]
            [promesa.core :as p]))

(events/watch-events)

(def socket "/run/guest-services/extension-backend.sock")

(def app (express))
(.use app (.json body-parser))
(.set app "view engine" "pug")
(.use app (.static express (.join path (.cwd js/process) "public")))
(.get app "/db" (fn [req resp] (p/then
                                (db/current-db)
                                (fn [db]
                                  (.send resp (clj->js db))))))
(.patch app "/db" (fn [req resp] (p/then
                                  (db/patch-db (js->clj (. req -body) :keywordize-keys true))
                                  (fn [db] (.send resp (clj->js db))))))

(.listen app socket (fn [] (println "Server running on " socket)))
