(ns vimhelp.html-test
  (:require [clojure.test :as t]
            [vimhelp.html :as sut]))

(t/deftest render-tag-test
  (t/are [in out] (= out (sut/render* [:tag in] {}))
    "foo",   [:a.tag {:name "foo"} "foo"]
    "<foo>", [:a.tag {:name "%3Cfoo%3E"} "&lt;foo&gt;"]))

(t/deftest render-ref-test
  (t/are [in out] (= out (sut/render* [:ref in] {}))
    "foo",   [:a.ref {:class "missing-tag" :href "#foo"} "foo"]
    "<foo>", [:a.ref {:class "missing-tag" :href "#%3Cfoo%3E"} "&lt;foo&gt;"])

  (let [tags {"foo" "index.html"
              "<foo>" "bar.html"}]
    (t/are [in out] (= out (sut/render* [:ref in] {:tags tags}))
      "foo",   [:a.ref {:class nil :href "index.html#foo"} "foo"]
      "<foo>", [:a.ref {:class nil :href "bar.html#%3Cfoo%3E"} "&lt;foo&gt;"])))


(t/deftest render-section-header-test
  (t/is (= [:p.section-header
            [:a.section-link {:href "#foo"} "@"]
            [:span.section-title "title"]
            [:a.tag {:name "foo"} "foo"]]
           (sut/render* [:section-header "title" [:tag "foo"]] {})))

  (t/is (= [:p.section-header
            [:a.section-link {:href "#%3Cfoo%3E"} "@"]
            [:span.section-title "&lt;title&gt;"]
            [:a.tag {:name "%3Cfoo%3E"} "&lt;foo&gt;"]]
           (sut/render* [:section-header "<title>" [:tag "<foo>"]] {}))))
