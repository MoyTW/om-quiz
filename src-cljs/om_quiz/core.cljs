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

(defn clean-state []
  {:questions q/questions
   :num-questions (count q/questions)
   :num-asked 0
   :num-to-ask 3
   :current-page :landing
   :answers []})

;; TODO: Make current-page not a horribly type-fluctuating hybrid that
;; is sometimes a keyword and sometimes an integer
(def app-state (atom (clean-state)))

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
  (let [[below above] (break-at (:current-page @app) (:questions @app))
        question (get (:questions @app) (:current-page @app))
        correct? (= (:guess question) (:answer question))
        current-index (:current-page @app)
        next-page
        (if (= (:num-to-ask @app) (inc (:num-asked @app)))
          :finish
          (if correct?
            (next-correct-with-removal current-index above)
            (next-incorrect-with-removal current-index below)))]
    ;; TODO: Hahaha four state-changing statements in a row!
    (do (om/transact! app [:num-asked] inc)
        (om/update! app [:current-page] next-page)
        (om/update! app [:questions] (vec (concat below above)))
        (om/transact! app [:answers] #(conj % question)))))

(defn submit-button [app owner]
  (let [new-num-asked (inc (:num-asked @app))
        guess (-> @app :questions (get (:current-page @app)) :guess)]
    (if (nil? guess)
      (js/alert "The guess is nil! Write something to handle this properly.")
      (process-answer app))))

;; TODO: Ugly code.
(defn answer-view [answer owner]
  (reify
    om/IRender
    (render [this]
      (if (= (:guess answer) (:answer answer))
        (let [st #js {:backgroundColor "#32CD32"}]
          (dom/li #js {:style st}
                  (dom/div nil "Question: " (:question answer))
                  (dom/div nil "Points Value: " (:score answer))
                  (dom/div nil "Answer: " (:answer answer))))
        (let [st #js {:backgroundColor "#FFA07A"}]
          (dom/li #js {:style st}
                  (dom/div nil "Question: " (:question answer))
                  (dom/div nil "Points Value: " (:score answer))
                  (dom/div nil "Your Guess: " (:guess answer))
                  (dom/div nil "Answer: " (:answer answer))))))))

(defn restart-quiz [app]
  (om/update! app (clean-state)))

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
             (dom/button #js {:onClick #(restart-quiz app)} "RESTART THE QUIZ!")
             (dom/h3 nil "Score Card:")
             (apply dom/ol nil (om/build-all answer-view (:answers app))))))

(defn quiz-page [app owner c]
  (dom/div nil
           (dom/h2 nil (str "Quiz Header - Question " (inc (:num-asked app)) " of " (:num-to-ask app)))
           (dom/div nil
                    (om/build question-view
                              (get (:questions app)
                                   (:current-page app))
                              {:init-state {:c c}}))
           (dom/button #js {:onClick (fn [e] (submit-button app owner))}
                       "Submit!")))

(defn start-quiz [app owner]
  (om/update! app [:current-page] (quot (:num-questions @app) 2)))

(defn landing-page [app owner]
  (dom/div nil
           (dom/h1 nil "Here is an explanation of the quiz.")
           (dom/div nil "You should point out that this quiz will make
 the questions harder if you get them and make them easier if you miss
 them. To the best of its ability, of course.")
           (dom/div nil "WordsWordsWords")
           (dom/button #js {:onClick (fn [e] (start-quiz app owner))}
                       "Go!")))

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
                              [:questions (:current-page @app)]
                              (fn [qs] (assoc qs :guess choice))))
              (recur)))))
    om/IRenderState
    (render-state [this {:keys [c]}]
      (let [cp (:current-page app)]
        (cond
         (= cp :landing)
         (landing-page app owner)

         (= cp :finish)
         (finish-page app)

         (= (type cp) (type 1))
         (quiz-page app owner c))))))

(om/root quiz-view app-state {:target (. js/document (getElementById "app"))})
