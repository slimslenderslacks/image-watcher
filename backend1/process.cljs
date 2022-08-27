(ns process
  (:require ["child_process" :as node-child-process]
            ["fs" :as fs]
            [clojure.string :as string]))

(defn tokenize [s] (string/split s #" "))

(defn process
  ([cmd] (process nil cmd nil))
  ([cmd opts] (if (map? cmd) ;; prev
                (process cmd opts nil)
                (process nil cmd opts)))
  ([prev cmd opts]
   (let [opts (merge {} opts)
         {:keys [:in]} opts
         in (or in (:out prev))
         cmd (if (and (string? cmd)
                      (not (.existsSync fs cmd)))
               (tokenize cmd)
               cmd)]
     (let [out (atom "")
           err (atom "")
           p (.spawn node-child-process (first cmd) (into-array (rest cmd)))]
       (.on (.-stderr p) "data" (fn [data] (swap! out str (str data))))
       (.on (.-stdout p) "data" (fn [data] (swap! out str (str data))))
       (.then
        (.all js/Promise #js [(js/Promise. (fn [accept _]
                                             (.on (.-stderr p) "end" (fn [] (accept true)))))
                              (js/Promise. (fn [accept _]
                                             (.on (.-stdout p) "end" (fn [] (accept true)))))
                              (js/Promise. (fn [accept _]
                                             (.on p "exit" (fn [exit _]
                                                             (accept {:exit exit
                                                                      :out @out
                                                                      :err @err})))))])
        (fn [data] (last data)))))))

