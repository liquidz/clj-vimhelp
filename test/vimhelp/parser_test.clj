(ns vimhelp.parser-test
  (:require [clojure.test :as t]
            [vimhelp.parser :as sut])
  (:import [java.io BufferedReader StringReader]))

(defn- parse [s]
  (with-open [r (BufferedReader. (StringReader. s))]
    (sut/parse r)))

(t/deftest parse-text-test
  (t/testing "single line text"
    (t/is (= [[:text "foo"]] (parse "foo"))))

  (t/testing "multiple line texts"
    (t/is (= [[:text "foo"] [:text "bar"]] (parse "foo\nbar"))))

  (t/testing "texts with tag"
    (t/are [in out] (= (parse in) out)
      "*foo*",          [[:text "" [:tag "foo"] ""]]
      "*foo* bar",      [[:text "" [:tag "foo"] " bar"]]
      "*foo* bar\nbaz", [[:text "" [:tag "foo"] " bar"] [:text "baz"]]
      "foo *bar* baz",  [[:text "foo " [:tag "bar"] " baz"]]
      "foo *bar baz",   [[:text "foo *bar baz"]]
      "foo *bar\nbaz*", [[:text "foo *bar"] [:text "baz*"]]))

  (t/testing "texts with ref"
    (t/are [in out] (= (parse in) out)
      "foo |bar| baz",  [[:text "foo " [:ref "bar"] " baz"]]
      "foo |bar baz",   [[:text "foo |bar baz"]]
      "foo |bar\nbaz|", [[:text "foo |bar"] [:text "baz|"]]))

  (t/testing "texts with inline command"
    (t/are [in out] (= (parse in) out)
      "foo `bar` baz",  [[:text "foo " [:command "bar"] " baz"]]
      "foo `bar baz",   [[:text "foo `bar baz"]]
      "foo `bar\nbaz`", [[:text "foo `bar"] [:text "baz`"]]))

  (t/testing "texts with constant"
    (t/are [in out] (= (parse in) out)
      "foo {bar} baz",     [[:text "foo " [:constant "{bar}"] " baz"]]
      "foo {bar baz",      [[:text "foo {bar baz"]]
      "foo {bar\nbaz}",    [[:text "foo {bar"] [:text "baz}"]]
      "foo {{bar}} baz",   [[:text "foo {" [:constant "{bar}"] "} baz"]]
      "foo `{{bar}}` baz", [[:text "foo " [:command "{{bar}}"] " baz"]]))

  (t/testing "texts with URL"
    (t/are [in out] (= (parse in) out)
      "https://example.com",         [[:text "" [:url "https://example.com"] ""]]
      "foo https://example.com",     [[:text "foo " [:url "https://example.com"] ""]]
      "foo https://example.com bar", [[:text "foo " [:url "https://example.com"] " bar"]])))

(t/deftest parse-example-test
  (t/testing "starts from line head"
    (t/testing "ends with '<'"
      (t/are [in out] (= (parse in) out)
        ">\n foo\n bar\n<",        [[:example " foo\n bar"]]
        ">\n foo\n bar\n<\nbaz",   [[:example " foo\n bar"] [:text "baz"]]
        "foo\n>\n bar\n<\nbaz",    [[:text "foo"] [:example " bar"] [:text "baz"]]
        ">\n\tfoo\n\tbar\n<",      [[:example "\tfoo\n\tbar"]]
        ">\n\tfoo\n\tbar\n<\nbaz", [[:example "\tfoo\n\tbar"] [:text "baz"]]
        ">\n<",                    [[:example ""] ]
        "foo\n>\n<\nbar",          [[:text "foo"] [:example ""] [:text "bar"]]))

    (t/testing "ends with some texts"
      (t/are [in out] (= (parse in) out)
        ">\n foo\nbar",         [[:example " foo"] [:text "bar"]]
        ">\n foo\n bar\nbaz",   [[:example " foo\n bar"] [:text "baz"]]
        "foo\n>\n bar\nbaz",    [[:text "foo"] [:example " bar"] [:text "baz"]]
        ">\n\tfoo\nbar",        [[:example "\tfoo"] [:text "bar"]]
        ">\n\tfoo\n\tbar\nbaz", [[:example "\tfoo\n\tbar"] [:text "baz"]]
        ">\nfoo",               [[:example ""] [:text "foo"]]
        "foo\n>\nbar",          [[:text "foo"] [:example ""] [:text "bar"]])))

  (t/testing "start from line tail"
    (t/testing "ends with '<'"
      (t/are [in out] (= (parse in) out)
        "foo >\n bar\n<",       [[:text "foo"] [:example " bar"]]
        "foo >\n bar\n baz\n<", [[:text "foo"] [:example " bar\n baz"]]
        "foo >\n bar\n<\nbaz",  [[:text "foo"] [:example " bar"] [:text "baz"]]
        "foo >\n\tbar\n<",      [[:text "foo"] [:example "\tbar"]]
        "foo >\n<",             [[:text "foo"] [:example ""]]
        "foo >\n<\nbar",        [[:text "foo"] [:example ""] [:text "bar"]]))
    (t/testing "ends with some texts"
      (t/are [in out] (= (parse in) out)
        "foo >\n bar\nbaz",         [[:text "foo"] [:example " bar"] [:text "baz"]]
        "foo >\n bar\n baz\neot",   [[:text "foo"] [:example " bar\n baz"] [:text "eot"]]
        "foo >\n\tbar\nbaz",        [[:text "foo"] [:example "\tbar"] [:text "baz"]]
        "foo >\n\tbar\n\tbaz\neot", [[:text "foo"] [:example "\tbar\n\tbaz"] [:text "eot"]]
        "foo >\nbar",               [[:text "foo"] [:example ""] [:text "bar"]])))

  (t/testing "mismatched"
    (t/are [in out] (= (parse in) out)
        ">\n foo",      []
        "foo >\n barz", [[:text "foo"]])))
