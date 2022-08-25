(ns extension.json)

(defn ->obj [s]
  (js->clj (.parse js/JSON s) :keywordize-keys true))
