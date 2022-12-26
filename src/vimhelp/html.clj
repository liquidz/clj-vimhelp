(ns vimhelp.html
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [hiccup.util :as h.util])
  (:import
   java.net.URLEncoder))

(defn- url?
  [s]
  (or (str/starts-with? s "http")
      (str/starts-with? s "//")))

(defn- class-names
  [opts class-name]
  (if-let [klass (get-in opts [:class class-name])]
    (str (name class-name) " " klass)
    (name class-name)))

(defn- url-encode
  [^String s]
  (URLEncoder/encode s "UTF-8"))

(defn- replace-spaces
  [s]
  (let [spaces #{\space \tab}]
    (->> s
         (partition-by #(contains? spaces %))
         (map (fn [char-seq]
                (if (contains? spaces (first char-seq))
                  (->> char-seq
                       (map #(case %
                               \space "&nbsp;"
                               \tab "&nbsp;&nbsp;&nbsp;&nbsp;"))
                       (apply str)
                       h.util/raw-string)
                  (apply str char-seq)))))))

(defn html-file-name
  [index path]
  (let [file-name (.getName (io/file path))]
    (if (= index file-name)
      "index.html"
      (str/replace file-name #"\.[^.]+$" ".html"))))

(defmulti render* (fn [line _] (first line)))
(defmethod render* :default [x _] x)

(defmethod render* :text
  [[_ & elements] opts]
  (->> (if (= [""] elements)
         [" "]
         elements)
       (map #(if (vector? %)
               (render* % opts)
               (replace-spaces %)))
       (into [:p {:class (class-names opts :text)}])))

(defmethod render* :tag
  [[_ tag-name] opts]
  [:a {:class (class-names opts :tag)
       :name (url-encode tag-name)}
   tag-name])

(defmethod render* :ref
  [[_ ref-name] {:as opts :keys [tags]}]
  [:a {:class (str (class-names opts :ref)
                   (when-not (contains? tags ref-name)
                     (str " " (class-names opts :missing-tag))))
       :href (str (get tags ref-name) "#" (url-encode ref-name))}
   ref-name])

(defmethod render* :constant
  [[_ constant-name] opts]
  [:span {:class (class-names opts :constant)}
   constant-name])

(defmethod render* :heading
  [[_ heading-text] opts]
  [:span {:class (class-names opts :heading)}
   heading-text])

(defmethod render* :command
  [[_ command] opts]
  [:code {:class (class-names opts :command)}
   command])

(defmethod render* :example
  [[_ example] opts]
  [:pre {:class (class-names opts :example)}
   [:code {:class (class-names opts :example-code)} example]])

(defmethod render* :url
  [[_ url] opts]
  [:a {:class (class-names opts :url) :href url}
   url])

(defmethod render* :divider
  [[_ text] opts]
  [:span {:class (class-names opts :divider)}
   text])

(defmethod render* :section-heading
  [[_ title tag] opts]
  (let [[_ tag-name] tag]
    [:p {:class (class-names opts :section-heading)}
     [:a {:class (class-names opts :section-link) :href (str "#" (url-encode tag-name))}
      "@"]
     (into [:span {:class (class-names opts :section-title)}]
           (replace-spaces title))
     (render* tag opts)]))


(defn render
  ([parsed-data] (render parsed-data {}))
  ([parsed-data {:as opts :keys [title copyright path blob index]}]
   (str "<!DOCTYPE html>"
        (hiccup/html
         [:html
          [:head
           [:meta {:charset "UTF-8"}]
           [:title title]
           (for [content (:css opts)]
             (if (url? content)
               [:link {:rel "stylesheet" :href content}]
               [:style {:type "text/css"} (h.util/raw-string content)]))
           (for [content (:script opts)]
             (if (url? content)
               [:script {:src content}]
               [:script (h.util/raw-string content)]))]
          [:body
           [:div {:class (class-names opts :wrapper)}
            [:header {:class (class-names opts :header)}
             [:h1 {:class (class-names opts :title)}
              title]

             (when (:show-navigation opts)
               [:nav {:class (class-names opts :files)}
                [:input#current-file {:type "checkbox"}]
                [:label {:for "current-file"} (.getName (io/file path))]

                [:ul
                 (for [path (sort #(cond
                                     (and index (str/ends-with? %1 index)) -1
                                     (and index (str/ends-with? %2 index)) 1
                                     :else (compare %1 %2))
                                  (:paths opts))]
                   [:li {:class (when (= path (:path opts))
                                  (class-names opts :active))}
                    [:a {:href (html-file-name index path)}
                     (.getName (io/file path))]])]])

             (when (and path blob)
               [:p {:class (class-names opts :edit-link)}
                [:a {:href (str (str/replace blob #"/$" "") "/" (.getName (io/file path)))}
                 "Edit this page"]])]

            [:div {:class (class-names opts :main)}
             (map #(render* % opts) parsed-data)]

            [:footer {:class (class-names opts :footer)}
             (when copyright [:p {:class (class-names opts :copyright)} copyright])
             [:p {:class (class-names opts :vimhelp)}
              "Built by " [:a {:href "https://github.com/liquidz/clj-vimhelp"} "clj-vimhelp"]
              " ver " (:version opts)]]]]]))))
