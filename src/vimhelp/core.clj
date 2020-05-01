(ns vimhelp.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [clojure.walk :as walk]
   [vimhelp.html :as h]
   [vimhelp.parser :as p]))

(def version
  (-> "version" io/resource slurp str/trim))

(def separator
  (System/getProperty "file.separator"))

(defn- extract-tags
  [parsed-data]
  (let [tags (atom #{})]
    (walk/postwalk
     #(do (when (and (vector? %) (= :tag (first %)))
            (swap! tags conj (second %)))
          %)
     parsed-data)
    @tags))

(def cli-options
  [["-c" "--css URL" "CSS URL"
    :default [] :assoc-fn (fn [m k v] (update m k conj v))]
   ["-t" "--title TITLE" "Help title" :default "no title"]
   ["-s" "--style STYLE" "CSS style rules"]
   ["-o" "--output OUTPUT" "Output directory"]
   ["-w" "--wrapper WRAPPER" "Body wrapper div class"
    :default "container"]
   [nil "--copyright COPYRIGHT" "Copyright text"]
   [nil "--blob BLOB" "Blob URL"]
   [nil "--index INDEX" "Index file name"]
   ["-v" "--verbose"]
   ["-h" "--help"]])

(defn -main
  [& args]
  (let [{:keys [arguments options summary errors]} (cli/parse-opts args cli-options)
        {:keys [help verbose output index]} options
        log #(when verbose (println %))
        html-file-name (partial h/html-file-name index)]
    (cond
      errors (doseq [e errors] (println e))
      help (println (str "Usage:\n" summary))
      (empty? arguments) (println "You must specify files to parse.")
      :else (let [path-parsed-data-pairs (map (fn [path]
                                                (log (format "Parsing: %s" path))
                                                (with-open [r (io/reader path)]
                                                  [path (p/parse r)]))
                                              arguments)
                  tags (reduce (fn [res [path parsed-data]]
                                 (->> (extract-tags parsed-data)
                                      (map #(vector % (html-file-name path)))
                                      (into {})
                                      (merge res)))
                               {} path-parsed-data-pairs)
                  opts (assoc options
                              :version version
                              :tags tags
                              :paths arguments
                              :show-navigation (> (count arguments) 1))]
              (doseq [[path parsed-data] path-parsed-data-pairs]
                (let [output-path (cond->> (html-file-name path)
                                    output (str output separator))
                      opts (assoc opts :path path)]
                  (log (format "Rendering: %s" output-path))
                  (spit output-path (h/render parsed-data opts))))))))
