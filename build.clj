(ns build
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as deploy]))

(def ^:private version (str/trim (slurp "resources/version")))
(def ^:private lib 'com.github.liquidz/clj-vimhelp)
(def ^:private class-dir "target/classes")
(def ^:private jar-file "target/clj-vimhelp.jar")
(def ^:private uberjar-file "target/clj-vimhelp-standalone.jar")
(def ^:private scm
  {:connection "scm:git:git://github.com/liquidz/clj-vimhelp.git"
   :developerConnection "scm:git:ssh://git@github.com/liquidz/clj-vimhelp.git"
   :url "https://github.com/liquidz/clj-vimhelp"
   :tag version})

(defn pom
  [_]
  (let [basis (b/create-basis)]
    (b/write-pom {:basis basis
                  :class-dir class-dir
                  :lib lib
                  :version version
                  :src-dirs (:paths basis)
                  :scm scm})))

(defn jar
  [arg]
  (let [basis (b/create-basis)]
    (pom arg)
    (b/copy-dir {:src-dirs (:paths basis)
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})))

(defn uberjar
  [_]
  (let [basis (b/create-basis)]
    (b/copy-dir {:src-dirs (:paths basis)
                 :target-dir class-dir})
    (b/compile-clj {:basis basis
                    :src-dirs ["src"]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uberjar-file
             :basis basis
             :main 'vimhelp.core})))

(defn install
  [arg]
  (jar arg)
  (deploy/deploy {:artifact jar-file
                  :installer :local
                  :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))

(defn tag
  [_]
  (sh/sh "git" "tag" version)
  (sh/sh "git" "push" "origin" version))

(defn deploy
  [arg]
  (assert (and (System/getenv "CLOJARS_USERNAME")
               (System/getenv "CLOJARS_PASSWORD")))
  (jar arg)
  (deploy/deploy {:artifact jar-file
                  :installer :remote
                  :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))
