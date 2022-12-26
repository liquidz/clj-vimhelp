(ns vimhelp.core-test
  (:require
   [clojure.test :as t]
   [clojure.tools.cli :as cli]
   [vimhelp.core :as sut]))

(t/deftest cli-options-test
  (t/testing "css"
    (t/is (= {:css ["foo" "bar"]}
             (-> ["-c" "foo" "--css" "bar"]
                 (cli/parse-opts  sut/cli-options)
                 (:options)
                 (select-keys [:css])))))

  (t/testing "script"
    (t/is (= {:script ["foo" "bar"]}
             (-> ["-s" "foo" "--script" "bar"]
                 (cli/parse-opts  sut/cli-options)
                 (:options)
                 (select-keys [:script])))))

  (t/testing "class"
    (t/is (= {:class {:foo "bar baz"
                      :bar "baz"}}
             (-> ["-k" "foo=bar"
                  "--class" "foo=baz"
                  "--class" "bar=baz"]
                 (cli/parse-opts  sut/cli-options)
                 (:options)
                 (select-keys [:class]))))))
