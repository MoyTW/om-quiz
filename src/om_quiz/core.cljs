(ns om-quiz.core
  (:require [om-quiz.questions :as questions]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:text questions/questions}))

(defn question-view [question owner]
  (reify
    om/IRender
    (render [_]
      (dom/li nil (:question question)))))

(defn quiz-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h2 nil "Blah!")
               (apply dom/ul nil
                      (om/build-all question-view (:text app)))))))

(om/root quiz-view app-state {:target (. js/document (getElementById "app"))})
