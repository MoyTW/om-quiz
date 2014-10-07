(ns om-quiz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om-quiz.questions :as q]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

(def app-state (atom {:questions q/questions}))

(defn choice-view [choice owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [c] :as state}]
      (dom/div nil
               (dom/input #js {:name owner :type "radio" :onClick (fn [e] (put! c choice))}
                           choice)))))

(defn question-view [question-map owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [c] :as state}]
      (dom/div nil
               (dom/h3 nil (:question question-map))
               (apply dom/form nil 
                      (om/build-all choice-view
                                    (:choices question-map)
                                    {:init-state {:c c}}))))))

(defn quiz-view [app owner]
  (reify
    om/IInitState
    (init-state [_] {:c (chan)})
    om/IWillMount
    (will-mount [_]
      (let [click-channel (om/get-state owner :c)]
        (go (loop []
              (let [choice (<! click-channel)]
                  (js/alert choice))
              (recur)))))
    om/IRenderState
    (render-state [this {:keys [c]}]
      (dom/div nil
               (dom/h2 nil "Quiz Header")
               (apply dom/ul nil
                      (om/build-all question-view (:questions app) {:init-state {:c c}}))
               (dom/button #js {:onClick (fn [e] (js/alert "What!?"))} "Submit!")))))

(om/root quiz-view app-state {:target (. js/document (getElementById "app"))})
