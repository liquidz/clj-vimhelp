(ns vimhelp.html
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]))

(defn- replace-spaces [s]
  (-> s
      (str/replace " " "&nbsp;")
      (str/replace "\t" "&nbsp;&nbsp;&nbsp;&nbsp;")))

(defn html-file-name [path]
  (str/replace (.getName (io/file path))
               #"\.[^.]+$" ".html"))

(defmulti render* (fn [line _] (first line)))
(defmethod render* :default [x _] x)

(defmethod render* :text
  [[_ & elements] opts]
  (->> (if (= [""] elements) [" "] elements)
       (map #(if (vector? %)
               (render* % opts)
               (-> % hiccup/h replace-spaces)))
       (cons :p)
       vec))

(defmethod render* :tag
  [[_ tag-name] _]
  [:a.tag {:name tag-name} (hiccup/h tag-name)])

(defmethod render* :ref
  [[_ ref-name] {:keys [tags]}]
  [:a.ref {:class (when-not (contains? tags ref-name) "missing-tag")
           :href (str (get tags ref-name) "#" ref-name)}
   (hiccup/h ref-name)])

(defmethod render* :constant
  [[_ constant-name] _]
  [:span.constant (hiccup/h constant-name)])

(defmethod render* :header
  [[_ header-text] _]
  [:span.header (hiccup/h header-text)])

(defmethod render* :command
  [[_ command] _]
  [:code.command (hiccup/h command)])

(defmethod render* :example
  [[_ example] _]
  [:pre.example [:code (hiccup/h example)]])

(defmethod render* :url
  [[_ url] _]
  [:a.url {:href url} (hiccup/h url)])

(defmethod render* :divider
  [[_ text] _]
  [:span.divider text])

(defmethod render* :section-header
  [[_ title tag] opts]
  (let [[_ tag-name] tag]
    [:p.section-header
     [:a.section-link {:href (str "#" tag-name)} "@"]
     [:span.section-title (-> title hiccup/h replace-spaces)]
     (render* tag opts)]))

(defn render
  ([parsed-data] (render parsed-data {}))
  ([parsed-data {:keys [title css wrapper style copyright version] :as opts}]
   (page/html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:title title]
     (for [href css]
       [:link {:rel "stylesheet" :href href}])
     (when style
       [:style {:type "text/css"} style])]

    [:body
     [:div {:class wrapper}
      (map #(render* % opts) parsed-data)]

     [:footer
      (when copyright [:p.copyright copyright])
      [:p.vimhelp
       "Built by " [:a {:href "https://github.com/liquidz/clj-vimhelp"} "clj-vimhelp"]
       " ver " version]]])))
