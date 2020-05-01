(ns vimhelp.parser
  (:require
   [clojure.string :as str]))

(declare parse-line)

(defn replace-by
  [s re f]
  (let [re (re-pattern (str "(.*)" re "(.*)"))]
    (if-let [[[_ head body tail]] (re-seq re s)]
      ^::matched [head (f body) tail]
      [s])))

(defn parse-texts
  [texts re f]
  (let [res (map #(if (vector? %) [%] (replace-by % re f)) texts)
        matched (some (comp ::matched meta) res)]
    (with-meta (mapcat identity res) {::matched matched})))

(defmacro def-inline-parser
  [name re f]
  `(defn ~name [texts#]
     (loop [ls# texts#]
       (let [res# (parse-texts ls# ~re ~f)]
         (if-not (::matched (meta res#))
           res#
           (recur res#))))))

(def-inline-parser parse-tag #"\*([^*]+)\*" #(vector :tag %))
(def-inline-parser parse-ref #"\|([^|]+)\|" #(vector :ref %))
(def-inline-parser parse-command #"\`([^`]+)\`" #(vector :command %))
(def-inline-parser parse-const #"(\{[^{}]+\})" #(vector :constant %))
(def-inline-parser parse-url #"(https?://[^ \r\n]+)" #(vector :url %))

(defn parse-header
  [[first-text :as texts]]
  (if-let [[[_ header]] (and (= 1 (count texts))
                             (string? first-text)
                             (re-seq #"^(.+)~$" first-text))]
    [[:header header]]
    texts))

(defn parse-divider
  [[first-text :as texts]]
  (if (and (= 1 (count texts))
           (string? first-text)
           (re-seq #"^(={3,}|-{3,})$" first-text))
    [[:divider first-text]]
    texts))

(defn parse-section-header
  [[first-text :as texts]]
  (if-let [[[_ section-title section-tag]] (and (= 1 (count texts))
                                                (string? first-text)
                                                (re-seq #"^([A-Z. -]+)(\*[^*]+\*)$" first-text))]
    [[:section-header section-title (second (parse-tag [section-tag]))]]
    texts))

(defn parse-text-line*
  [line]
  (->> [line]
       parse-section-header parse-divider parse-header
       parse-command parse-tag parse-ref parse-const parse-url
       (cons :text)
       vec))

(defn- parse-text-line
  [state line]
  (cond
    (= ">" line)
    [(assoc state :mode :example :examples [])]

    (str/ends-with? line " >")
    [(assoc state :mode :example :examples [])
     (parse-text-line* (subs line 0 (- (count line) 2)))]

    :else
    [nil (parse-text-line* line)]))

(defn- parse-example-line
  [state line]
  (cond
    ;; Explicit end of example
    (= "<" line)
    [(assoc state :mode :text :examples nil)
     [:example (str/join "\n" (:examples state))]]

    ;; Inside example
    (or (= "" line)
        (re-seq #"^\s+" line))
    [(update state :examples conj line) nil]

    ;; Implicit end of example
    :else
    (let [new-state (assoc state :mode :text :examples nil)
          [new-state' new-line] (parse-line new-state line)]
      [(merge new-state new-state')
       (list [:example (str/join "\n" (:examples state))]
             new-line)])))

(defn- parse-line
  [state line]
  (case (:mode state)
    :text (parse-text-line state line)
    :example (parse-example-line state line)
    (throw (ex-info "Invalid line" {:state state :line line}))))

(defn parse
  [reader]
  (:result
   (reduce (fn [{:keys [state] :as m} line]
             (let [[new-state new-line] (parse-line state line)]
               (cond-> m
                 new-state (assoc :state new-state)
                 new-line (update :result #(if (list? new-line)
                                             (vec (concat % new-line))
                                             (conj % new-line))))))
           {:state {:mode :text} :result []}
           (line-seq reader))))
