(ns om-quiz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om-quiz.questions :as q]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

;; Structure of app:
;; {:questions {java.lang.Integer {:question java.lang.String
;;                                 :answer java.lang.String
;;                                 :guess java.lang.String
;;                                 :choices [java.lang.String]
;;                                 :score java.lang.Integer}}
;;  :current-question java.lang.Integer}
(def app-state (atom {:questions q/questions
                      :num-questions (count q/questions)
                      :num-asked 0
                      :num-to-ask 2
                      :current-question (quot (count q/questions) 2)
                      :answers []
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

(defn score [app]
  (let [correct (filter #(= (:guess %) (:answer %)) (:answers app))]
    {:num-correct (count correct)
     :score (reduce + (map :score correct))}))

(defn break-at [n coll]
  [(take n coll) (drop (inc n) coll)])

;; The mapping should go as follows:
;; Count=0 -> Special Case
;; Count=1 -> 0
;; Count=2 -> 1
;; Count=3 -> 1
;; Count=4 -> 2
;; Count=5 -> 2
;; Count=6 -> 3
;; Count=7 -> 3

(defn next-correct-with-removal [current-index above]
  (dec (if (empty? above)
         current-index
         (+ current-index (inc (quot (count above) 2))))))

(defn next-incorrect-with-removal [current-index below]
  (max 0 (quot (count below) 2)))

(defn process-answer [app]
  (let [[below above] (break-at (:current-question @app) (:questions @app))
        question (get (:questions @app) (:current-question @app))
        correct? (= (:guess question) (:answer question))
        current-index (:current-question @app)
        next-index (if correct?
                     (next-correct-with-removal current-index above)
                     (next-incorrect-with-removal current-index below))]
    ;; TODO: Hahaha four state-changing statements in a row!
    (do (om/transact! app [:num-asked] inc)
        (om/update! app [:current-question] next-index)
        (om/update! app [:questions] (vec (concat below above)))
        (om/transact! app [:answers] #(conj % question)))))

(defn submit-button [app owner]
  (let [new-num-asked (inc (:num-asked @app))
        guess (-> @app :questions (get (:current-question @app)) :guess)]
    (if (nil? guess)
      (js/alert "The guess is nil! Write something to handle this properly.")
      (process-answer app))))

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
      (if (= (:num-asked app) (:num-to-ask app))
        (let [score-result (score app)]
          (dom/div nil
                   (dom/h1 nil "Congratulations!")
                   (dom/h2 nil (str "You answered " (:num-correct score-result) " out of " (:num-to-ask app)))
                   (dom/h3 nil (str "Your score was " (:score score-result)))))
        (dom/div nil
                 (dom/h2 #js {:ref "a"} "Quiz Header")
                 (dom/div #js {:ref "b"}
                          (om/build question-view
                                    (get (:questions app) (:current-question app))
                                    {:init-state {:c c}}))
                 (dom/button #js {:onClick (fn [e] (submit-button app owner))}
                             "Submit!"))))))

(om/root quiz-view app-state {:target (. js/document (getElementById "app"))})
