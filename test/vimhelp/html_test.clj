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
           (#'sut/replace-spaces "hi  there\twow\t\tvery \tcool"))))

(t/deftest render-text-test
  (t/is (= [:p {:class "text"} ["foo"] ["bar"]]
           (sut/render* [:text "foo" "bar"] {}))))

(t/deftest render-tag-test
  (t/are [in out] (= out (sut/render* [:tag in] {}))
    "foo",   [:a {:class "tag" :name "foo"} "foo"]
    "<foo>", [:a {:class "tag" :name "%3Cfoo%3E"} "<foo>"]))

(t/deftest render-ref-test
  (t/are [in out] (= out (sut/render* [:ref in] {}))
    "foo",   [:a {:class "ref missing-tag" :href "#foo"} "foo"]
    "<foo>", [:a {:class "ref missing-tag" :href "#%3Cfoo%3E"} "<foo>"])

  (let [tags {"foo" "index.html"
              "<foo>" "bar.html"}]
    (t/are [in out] (= out (sut/render* [:ref in] {:tags tags}))
      "foo",   [:a {:class "ref" :href "index.html#foo"} "foo"]
      "<foo>", [:a {:class "ref" :href "bar.html#%3Cfoo%3E"} "<foo>"])))

(t/deftest render-section-header-test
  (t/is (= [:p {:class "section-header"}
            [:a {:class "section-link" :href "#foo"} "@"]
            [:span {:class "section-title"} "title"]
            [:a {:class "tag" :name "foo"} "foo"]]
           (sut/render* [:section-header "title" [:tag "foo"]] {})))

  (t/is (= [:p {:class "section-header"}
            [:a {:class "section-link" :href "#%3Cfoo%3E"} "@"]
            [:span {:class "section-title"} "<title>"]
            [:a {:class "tag" :name "%3Cfoo%3E"} "<foo>"]]
           (sut/render* [:section-header "<title>" [:tag "<foo>"]] {}))))

(t/deftest render-with-extra-classes-test
  (t/is (= [:a {:class "tag hello world" :name "foo"} "foo"]
           (sut/render* [:tag "foo"] {:extra-classes {:tag "hello world"}}))))
