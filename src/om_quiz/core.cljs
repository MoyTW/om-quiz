(ns om-quiz.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om-quiz.questions :as q]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

(enable-console-print!)

;; Nested sub-structures of app:
;; {:questions [{:question java.lang.String
;;               :answer java.lang.String
;;               :guess java.lang.String
;;               :choices [java.lang.String]
;;               :score java.lang.Integer}]
;;  :answers [{:question java.lang.String
;;             :answer java.lang.String
;;             :guess java.lang.String
;;             :choices [java.lang.String]
;;             :score java.lang.Integer}]}
(def app-state (atom {:questions q/questions
                      :num-questions (count q/questions)
                      :num-asked 0
                      :num-to-ask 3
                      :current-question (quot (count q/questions) 2)
                      :answers []}))

;; TODO: Make the radio buttons clear properly!
(defn choice-view [choice owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [c]}]
      (dom/div nil
               (dom/input #js {:name owner
                               :type "button"
                               :onClick (fn [e] (put! c choice))})
               (dom/label nil (str " <- " choice))))))

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

(defn score [app]
  ;; TODO: filter & remove same can be one!
  (let [correct (filter #(= (:guess %) (:answer %)) (:answers app))
        incorrect (remove #(= (:guess %) (:answer %)) (:answers app))]
    {:num-correct (count correct)
     :score (reduce + (map :score correct))
     :incorrect incorrect}))

(defn break-at [n coll]
  [(take n coll) (drop (inc n) coll)])

(defn next-correct-with-removal [current-index above]
  (dec (if (empty? above)
         current-index
         (+ current-index (inc (quot (count above) 2))))))

(defn next-incorrect-with-removal [current-index below]
  (max 0 (quot (count below) 2)))

;; To find the max score, we just iterate through the questions
;; simulating correct answers until we've reached num-to-ask.
(defn find-max-score [all-questions current-index num-to-ask]
  (loop [num-asked 0
         questions all-questions
         score 0
         current-index current-index]
    (if (= num-asked num-to-ask)
      score
      (let [[below above] (break-at current-index questions)]
        (recur (inc num-asked)
               (vec (concat below above))
               (+ score (:score (get questions current-index)))
               (next-correct-with-removal current-index above))))))

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

(defn incorrect-view [incorrect owner]
  (reify
    om/IRender
    (render [this]
      (dom/li nil
              (dom/div nil "Question: " (:question incorrect))
              (dom/div nil "Your Guess: " (:guess incorrect))
              (dom/div nil "Answer: " (:answer incorrect))))))

(defn finish-page [app]
  (let [score-result (score app)]
    (dom/div nil
             (dom/h1 nil "Congratulations!")
             (dom/h2 nil (str "You answered " (:num-correct score-result)
                              " out of " (:num-to-ask app)))
             (dom/h3 nil (str "Your score was " (:score score-result)))
             (dom/h3 nil (str "Max score is: " (find-max-score q/questions
                                                               (quot (count q/questions) 2)
                                                               (:num-to-ask app))))
             (dom/h3 nil "Missed questions:")
             (apply dom/ul nil (om/build-all incorrect-view (:incorrect score-result))))))

(defn quiz-page [app owner c]
  (dom/div nil
           (dom/h2 nil "Quiz Header")
           (dom/div nil
                    (om/build question-view
                              (get (:questions app)
                                   (:current-question app))
                              {:init-state {:c c}}))
           (dom/button #js {:onClick (fn [e] (submit-button app owner))}
                       "Submit!")))

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
        (finish-page app)
        (quiz-page app owner c)))))

(om/root quiz-view app-state {:target (. js/document (getElementById "app"))})
