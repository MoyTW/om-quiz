(ns om-quiz.core
  (:use compojure.core
        ring.middleware.reload
        ring.middleware.stacktrace
        ring.middleware.params
        )
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))

(defroutes main-routes
  (route/resources "/res")
  (route/not-found "ACK 404"))

(def app
  (-> (handler/site main-routes)
      (wrap-params)
      (wrap-reload)
      (wrap-stacktrace)
      ))
