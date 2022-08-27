(ns docker
  (:require ["http$default" :as http]
            [goog.string :refer [format]]))

(defn delete-container [id]
  (let [request (.request http
                          #js {:method "DELETE"
                               :socketPath "/var/run/docker.sock"
                               :path (format "/containers/%s" id)}
                          (fn [response]
                            (when (not (= 204 (.-statusCode response)))
                              (println "container delete error" (.-statusCode response)))))]
    (.end request)))

(defn attach-container [id callback]
  (let [request (.request http
                          #js {:path (format "/containers/%s/attach?stream=true&stdout=true" id)
                               :method "POST"
                               :socketPath "/var/run/docker.sock"}
                          (fn [response]
                            (if (not (= 200 (.-statusCode response)))
                              (println "container attach error" (.-statusCode response))
                              (try
                                (.on response "data" callback)
                                (.on response "close" (fn [] (delete-container id)))
                                (catch Error ex (println "error" ex))))))]
    (.end request)))

(defn start-container [id callback]
  (let [request (.request
                 http
                 #js {:method "POST"
                      :socketPath "/var/run/docker.sock"
                      :path (format "/containers/%s/start" id)}
                 (fn [response]
                   (if (not (= 204 (.-statusCode response)))
                     (println "container start error " (.-statusCode response))
                     (attach-container id callback))))]
    (.end request)))

(defn run-container [image cmd callback]
  (let [payload (js/JSON.stringify #js {:Image image
                                        :Tty true
                                        :Cmd (into-array cmd)})
        r (.request
           http
           #js {:method "POST"
                :socketPath "/var/run/docker.sock"
                :path "/containers/create"
                :headers #js {"Content-Type" "application/json"
                              "Content-Length" (count payload)}}
           (fn [response]
             (try
               (if (not (= 201 (.-statusCode response)))
                 (println "container create error" (.-statusCode response))
                 (.on response "data" (fn [data]
                                        (let [id (.-Id (.parse js/JSON data))]
                                          (start-container id callback)))))
               (catch Error e (println "error " e)))))]
    (.write r payload)
    (.end r)))

