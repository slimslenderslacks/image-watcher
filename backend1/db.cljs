(ns db
  (:require [clojure.edn :as edn]
            [promesa.core :as p]
            [nbb.core :refer [slurp]]
            ["fs$default" :as fs]))

(def db-file "/var/image-watcher-db/db.edn")

(defn current-db
  "fetch the current db fro a persistent volume mapped by docker-compose.yaml in backend"
  []
  (p/catch
   (p/then
    (slurp db-file)
    (fn [content] 
      (edn/read-string content)))
   (fn [error]
     (let [db {}]
       (.writeFile fs db-file (pr-str db) (fn [error] (println error)))
       db))))

(defn patch-db
  "patch the db by merging a new edn map"
  [m]
  (p/then
   (slurp db-file)
   (fn [content]
     (println "merge " m " with " content)
     (let [db (merge
               (edn/read-string content)
               m)]
       (.writeFile fs db-file (pr-str db) (fn [error] (println error)))
       db))))
