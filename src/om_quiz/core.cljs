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
                      :num-questions (count q/questions)
                      :num-asked 0
                      :num-to-ask 3 ;; TODO: Change!
                      :current-question 0 ;; TODO: Change!
                      }))

;; TODO: Make the radio buttons clear properly!
(defn choice-view [choice owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [c]}]
      (dom/div nil
               (dom/input #js {:name owner :type "button" :onClick (fn [e] (put! c choice))})
               (dom/label nil (str " <- " choice))))))

(defn question-view [question owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [c]}]
      (dom/div nil
               (dom/h3 #js {:ref "q"} (:question question))
               (dom/h4 nil (str "Your Answer: ") (:guess question))
               (apply dom/form nil
                      (om/build-all choice-view
                                    (:choices question)
                                    {:init-state {:c c}}))))))

(defn submit-button [app owner]
  (let [new-num-asked (inc (:num-asked @app))
        guess (-> @app :questions (get (:current-question @app)) :guess)]
    (cond
     (nil? guess)
     (js/alert "The guess is nil!")

     (= new-num-asked (:num-to-ask @app))
     (js/alert "We're done here.")

     :else
     (do (om/transact! app [:num-asked] inc)
         (om/transact! app [:current-question] inc)))))

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
                              [:questions (:current-question @app)]
                              (fn [qs] (assoc qs :guess choice))))
              (recur)))))
    om/IRenderState
    (render-state [this {:keys [c]}]
      (dom/div nil
               (dom/h2 #js {:ref "a"} "Quiz Header")
               (dom/div #js {:ref "b"}
                        (om/build question-view
                                  (get (:questions app) (:current-question app))
                                  {:init-state {:c c}}))
               (dom/button #js {:onClick (fn [e] (submit-button app owner))}
                           "Submit!")))))

(om/root quiz-view app-state {:target (. js/document (getElementById "app"))})
