(ns backend.events
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [backend.events :as events]
            [clojure.pprint :refer [pprint]]
            [unixsocket-http.core :as uhttp])
  (:import [java.io InputStreamReader BufferedInputStream BufferedReader PipedOutputStream PipedInputStream InputStream]))

(def socket-file-name "/var/run/docker.sock")

(defn docker-events [ch]
  (let [client (uhttp/client (format "unix://%s" socket-file-name))
        response (uhttp/get client "/events" {:as :stream})]
    (pprint response)
    (async/go
      (async/<!
       (async/thread
         (with-open [rdr (BufferedReader. (InputStreamReader. (:body response)))]
           (loop []
             (when-let [line (.readLine rdr)]
               (async/go (async/>! ch line))
               (recur))))))
      (async/close! ch))))

(comment
  (let [ch (async/chan)]
    (async/go-loop []
      (when-let [v (async/<! ch)]
        (println v)
        (recur)))
    (docker-events ch)))
