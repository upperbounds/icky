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
         [0 1] [2 4]))

  ;; no overlap, reorder
  (is (= (sort-position [2 4] [0 1])
         [[0 1] [2 4]]))

  ;; no overlap, no reorder
  (is (= (sort-position [0 1] [2 4])
         [[0 1] [2 4]]))

  ;; widen at the start
  (is (= (sort-position [0 3] [2 4])
         [[0 4]]))

  ;; widen at the end
  (is (= (sort-position [0 4] [3 5])
         [[0 5]]))

  ;; widen at the start and reorder
  (is (= (sort-position [2 4] [0 3])
         [[0 4]]))

  ;; widen at the and reorder
  (is (= (sort-position [3 5] [0 4])
         [[0 5]])))
