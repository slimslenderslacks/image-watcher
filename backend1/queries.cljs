(ns queries
  (:require [process :refer [process]]
            [clojure.edn :as edn]
            [promesa.core :as p]
            [clojure.string :as s]
            [clojure.pprint :refer [pprint]]
            ["http$default" :as http]
            [goog.string :refer [format]]
            [nbb.core :refer [slurp]] 
            [db]
            [docker]))

(defn query [team datalog args callback]
  (p/then
   (db/current-db)
   (fn [db]
     (let [token (:token db)
           cmd (->> args
                    (concat ["q" "--team" team "--query" (pr-str datalog) "--token" token])
                    (into []))]
       (docker/run-container "slimslenderslacks/bb_scripts:latest" cmd callback)))))

(defn ->args [ks m]
  (->> ks
       (map #(% m))
       (into [])))

(defn run-query [req resp]
  (p/then
   (slurp (format "datalog/%s.edn" (.. req -params -name)))
   (fn [datalog]
     (let [edn (edn/read-string datalog)]
       (query
        (.. req -query -team)
        edn
        (->args (-> edn meta :args) (js->clj (. req -query) :keywordize-keys true))
        (fn [data] (.send resp (if data 
                                 (.toString data)
                                 "empty"))))))))

(comment
  (run-query #js {:params #js {:name "layers-by-digest"}
                  :query #js {:team "AQ1K5FIKA"
                              :digest "sha256:7cbf46aeb98b3b757a10952651dbbcb8ad60714c33d76473e6c00d6bf73c438a"}}
             #js {:send (fn [s] (println "send " s))})
  (p/then
   (slurp "/Users/slim/slimslenderslacks/image-watcher/backend1/datalog/layers-by-digest.edn")
   (fn [datalog]
     (query "AQ1K5FIKA" (edn/read-string datalog) ["sha256:7cbf46aeb98b3b757a10952651dbbcb8ad60714c33d76473e6c00d6bf73c438a"] (fn [data] (println "data: " data)))))
  (p/then
   (slurp "/Users/slim/atmhq/bb_scripts/datalog/all-repos.edn")
   (fn [datalog]
     (query "AQ1K5FIKA" (edn/read-string datalog) {} (fn [data] (println "data: " data)))))
  (p/then (slurp "/Users/slim/.atomist/bb.jwt")
          (fn [content] (db/patch-db {:token content})))
  (p/then (db/current-db) #(println %)))
