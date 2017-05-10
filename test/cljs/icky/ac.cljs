(ns cljs.icky.ac
  (:require  [cljs.test :include-macros true]
             [icky.complete :as ic]))

(deftest "test highligher"
  (is (= 1 1))
  (is (= 2 4)))

(def-test "test position sorter"
  (is (= (ic/sort-positon [2 4] [0 1])
         [[0 1] [2 4]]))

  (is (= (ic/sort-position [0 1] [2 4])
         [0 1] [2 4])))
