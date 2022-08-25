(ns backend.handler
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes GET PATCH]]
            [compojure.route :as route]
            [clojure.core.async :as async]
            [ring.adapter.jetty :refer [run-jetty]]
            [backend.db :as db]
            [backend.events :as events]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [taoensso.timbre :as timbre]
            [ring.core.protocols :refer [StreamableResponseBody]])
  (:import [org.eclipse.jetty.unixsocket UnixSocketConnector]))

;; extend a core async channel using a ring core protocol that knows
;; how to handler a response body that is a channel
(extend-type clojure.core.async.impl.channels.ManyToManyChannel
  StreamableResponseBody
  (write-body-to-stream [channel _ output-stream]
    (async/go (with-open [writer (io/writer output-stream)]
                (async/loop []
                  (when-let [msg (async/<! channel)]
                    (doto writer (.write msg) (.flush))
                    (recur)))))))

(defroutes app
  ;; use a volume to store session state in a "db"
  (->
   (GET "/db" _ {:status 200 :body (db/current-db)})
   (wrap-json-response))
  ;; update session state for next time the plugin starts
  (->
   (PATCH "/db" req {:status 200 :body (db/patch-db (:body req))})
   (wrap-json-response)
   (wrap-json-body))
  ;; stream events continuously
  (GET "/stream-events" _ (fn [req res _]
                            (let [ch (async/chan)]
                              (res {:status 200 :headers {} :body ch})
                              (events/docker-events ch))))
  (route/not-found "<h1>not found</h1>"))

(defn -main
  [& {:keys [join?]}]
  (try
    (run-jetty #'app {:join? (if (false? join?) false true)
                      :port 3000
                      :configurator (fn [server]
                                      (.addConnector server (doto
                                                             (UnixSocketConnector. server)
                                                              (.setAcceptQueueSize 128)
                                                              (.setUnixSocket "/var/run/blah.sock")))
                                      server)
                      :async? true})
    (catch Throwable t
      (timbre/errorf t "failed to start"))))

(comment
  (def server (-main :join? false))
  (.stop server))
