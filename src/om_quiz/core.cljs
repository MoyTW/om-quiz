(ns om-quiz.core
  (:require [om-quiz.questions :as q]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state (atom {:questions q/questions}))

(defn choice-view [choice owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/button nil choice)))))

(defn question-view [question-map owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div nil
             (dom/h3 nil (:question question-map))
             (om/build-all choice-view (:choices question-map))))))

(defn quiz-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h2 nil "Quiz Header")
               (apply dom/ul nil
                      (om/build-all question-view (:questions app)))))))

(om/root quiz-view app-state {:target (. js/document (getElementById "app"))})
