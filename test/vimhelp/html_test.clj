(ns vimhelp.html-test
  (:require
   [clojure.test :as t]
   [hiccup2.core :as hiccup]
   [vimhelp.html :as sut]))

(t/deftest replace-spaces-test
  (t/is (= (list "hi" (hiccup/raw "&nbsp;&nbsp;") "there"
                 (hiccup/raw "&nbsp;&nbsp;&nbsp;&nbsp;") "wow"
                 (hiccup/raw "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
                 "very" (hiccup/raw "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") "cool")
           (sut/replace-spaces "hi  there\twow\t\tvery \tcool"))))

(t/deftest render-tag-test
  (t/are [in out] (= out (sut/render* [:tag in] {}))
    "foo",   [:a.tag {:name "foo"} "foo"]
    "<foo>", [:a.tag {:name "%3Cfoo%3E"} "<foo>"]))

(t/deftest render-ref-test
  (t/are [in out] (= out (sut/render* [:ref in] {}))
    "foo",   [:a.ref {:class "missing-tag" :href "#foo"} "foo"]
    "<foo>", [:a.ref {:class "missing-tag" :href "#%3Cfoo%3E"} "<foo>"])

  (let [tags {"foo" "index.html"
              "<foo>" "bar.html"}]
    (t/are [in out] (= out (sut/render* [:ref in] {:tags tags}))
      "foo",   [:a.ref {:class nil :href "index.html#foo"} "foo"]
      "<foo>", [:a.ref {:class nil :href "bar.html#%3Cfoo%3E"} "<foo>"])))


(t/deftest render-section-header-test
  (t/is (= [:p.section-header
            [:a.section-link {:href "#foo"} "@"]
            [:span.section-title "title"]
            [:a.tag {:name "foo"} "foo"]]
           (sut/render* [:section-header "title" [:tag "foo"]] {})))

  (t/is (= [:p.section-header
            [:a.section-link {:href "#%3Cfoo%3E"} "@"]
            [:span.section-title "<title>"]
            [:a.tag {:name "%3Cfoo%3E"} "<foo>"]]
           (sut/render* [:section-header "<title>" [:tag "<foo>"]] {}))))
