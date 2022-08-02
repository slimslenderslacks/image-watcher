(ns backend.db
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

;; use a local file to store session state for the extension
(def db-file "/var/image-watcher-db/db.edn")

(defn current-db 
  "fetch the current db from a persistent volume mapped by docker-compose.yaml in backend"
  []
  (let [f (io/file db-file)]
    (if (.exists f)
      (edn/read-string (slurp f))
      (let [db {}]
        (spit f (pr-str db)) db))))

(defn patch-db 
  "patch the db by merging a new edn map"
  [m]
  (let [db (merge
            (read-string (slurp db-file))
            m)]
    (spit (io/file db-file) (pr-str db)) 
    db))
