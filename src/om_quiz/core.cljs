(ns om-quiz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om-quiz.questions :as q]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

;; Structure of app:
;; {:questions [{:question java.lang.String
;;               :answer java.lang.String
;;               :guess java.lang.String
;;               :choices [java.lang.String]}]
;;  :current-question java.lang.Integer}
(def app-state (atom {:questions q/questions
                      :current-question 1}))

(defn choice-view [choice owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [c]}]
      (dom/div nil
               (dom/input #js {:name owner :type "radio" :onClick (fn [e] (put! c choice))}
                           choice)))))

(defn question-view [question owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [c]}]
      (dom/div nil
               (dom/h3 nil (:question question))
               (dom/h4 nil (str "Your Answer: ") (:guess question))
               (apply dom/form nil 
                      (om/build-all choice-view
                                    (:choices question)
                                    {:init-state {:c c}}))))))

(defn questions-views [question-map owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [c]}]
      (dom/div nil
               (dom/h3 nil (:question (second question-map)))
               (dom/h4 nil (str "Your Answer: ") (:guess question-map))
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
                (om/transact! app
                              [:questions 0]
                              (fn [qs] (assoc qs :guess choice))))
              (recur)))))
    om/IRenderState
    (render-state [this {:keys [c]}]
      (dom/div nil
               (dom/h2 nil "Quiz Header")
               (dom/ul nil
                       (om/build question-view
                                 (get (:questions app) (:current-question app))
                                 {:init-state {:c c}}))
               (dom/button #js {:onClick (fn [e] (js/alert (str #_(:questions @app) [(get (:questions @app) 0)])))}
                           "Submit!")))))

(om/root quiz-view app-state {:target (. js/document (getElementById "app"))})
