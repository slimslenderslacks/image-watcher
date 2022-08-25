(ns events
  (:require ["http$default" :as http]
            [clojure.pprint :refer [pprint]]))

(defn watch-events []
  (let [r (.request http
                    #js {:socketPath "/var/run/docker.sock" :path "/events"}
                    (fn [response]
                      (println "status" (. response -statusCode))
                      (.setEncoding response "utf8")
                      (.on response "data" (fn [data] (println "data: " data)))
                      (.on response "error" (fn [error] (println "error: " error)))))]
    (.end r)))

(defn containers []
  (let [r (.request http
                    #js {:socketPath "/var/run/docker.sock" :path "/containers/json"}
                    (fn [response]
                      (println "status" (. response -statusCode))
                      (.on response "data" (fn [data] 
                                             (->> (js->clj (.parse js/JSON (.toString data)) :keywordize-keys true)
                                                  (map #(select-keys % [:Id :Image :ImageID :Names :State]))
                                                  (pprint))))
                      (.on response "error" (fn [error] (println "error: " error)))))]
    (.end r)))

