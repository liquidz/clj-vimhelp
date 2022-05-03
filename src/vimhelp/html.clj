(ns vimhelp.html
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [hiccup2.core :as hiccup]
   [hiccup.page :as page])
  (:import
   java.net.URLEncoder))

(defn- url-encode
  [s]
  (URLEncoder/encode s "UTF-8"))

(defn replace-spaces
  [s]
  (->> s
       (partition-by #(case % \space 0 \tab 0 1))
       (map (fn [char-seq]
              (if (#{\space \tab} (first char-seq))
                (->> char-seq
                     (map #(case %
                             \space "&nbsp;"
                             \tab "&nbsp;&nbsp;&nbsp;&nbsp;"))
                     (apply str)
                     hiccup/raw)
                (apply str char-seq))))))

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
  (->> (if (= [""] elements) [" "] elements)
       (map #(if (vector? %)
               (render* % opts)
               (replace-spaces %)))
       (cons :p)
       vec))

(defmethod render* :tag
  [[_ tag-name] _]
  [:a.tag {:name (url-encode tag-name)} tag-name])

(defmethod render* :ref
  [[_ ref-name] {:keys [tags]}]
  [:a.ref {:class (when-not (contains? tags ref-name) "missing-tag")
           :href (str (get tags ref-name) "#" (url-encode ref-name))}
   ref-name])

(defmethod render* :constant
  [[_ constant-name] _]
  [:span.constant constant-name])

(defmethod render* :header
  [[_ header-text] _]
  [:span.header header-text])

(defmethod render* :command
  [[_ command] _]
  [:code.command command])

(defmethod render* :example
  [[_ example] _]
  [:pre.example [:code example]])

(defmethod render* :url
  [[_ url] _]
  [:a.url {:href url} url])

(defmethod render* :divider
  [[_ text] _]
  [:span.divider text])

(defmethod render* :section-header
  [[_ title tag] opts]
  (let [[_ tag-name] tag]
    [:p.section-header
     [:a.section-link {:href (str "#" (url-encode tag-name))} "@"]
     (into [:span.section-title] (replace-spaces title))
     (render* tag opts)]))

(defn render
  ([parsed-data] (render parsed-data {}))
  ([parsed-data {:keys [title style copyright path blob index] :as opts}]
   (page/html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:title title]
     (for [href (:css opts)]
       [:link {:rel "stylesheet" :href href}])
     (when style
       [:style {:type "text/css"} style])]

    [:body
     [:header
      [:h1.title title]

      (when (:show-navigation opts)
        [:nav.files
         [:input#current-file {:type "checkbox"}]
         [:label {:for "current-file"} (.getName (io/file path))]

         [:ul
          (for [path (sort #(cond
                              (and index (str/ends-with? %1 index)) -1
                              (and index (str/ends-with? %2 index)) 1
                              :else (compare %1 %2))
                           (:paths opts))]
            [:li {:class (when (= path (:path opts)) "active")}
             [:a {:href (html-file-name index path)}
              (.getName (io/file path))]])]])

      (when (and path blob)
        [:p.edit-link
         [:a {:href (str (str/replace blob #"/$" "") "/" (.getName (io/file path)))}
          "Edit this page"]])]

     [:div {:class (:wrapper opts)}
      (map #(render* % opts) parsed-data)]

     [:footer
      (when copyright [:p.copyright copyright])
      [:p.vimhelp
       "Built by " [:a {:href "https://github.com/liquidz/clj-vimhelp"} "clj-vimhelp"]
       " ver " (:version opts)]]])))
