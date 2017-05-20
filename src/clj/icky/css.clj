(ns icky.css
  (:require [garden.def :refer [defstyles]]))

(def bg1 "#394034")
(def bg2 "#473844")
(def bg3 "#272822")
(def monokai-yellow-d       "#BEB244")
(def monokai-yellow-l       "#FFF7A8")
(def monokai-orange-d       "#D47402")
(def monokai-orange-l       "#FFAC4A")
(def monokai-red-d          "#F70057")
(def monokai-red-l          "#FA518D")
(def monokai-magenta-d      "#FB35EA")
(def monokai-magenta-l      "#FE8CF4")
(def monokai-violet-d       "#945AFF")
(def monokai-violet-l       "#C9ACFF")
(def monokai-blue-d         "#40CAE4")
(def monokai-blue-l         "#92E7F7")
(def monokai-cyan-d         "#74DBCD")
(def monokai-cyan-l         "#D3FBF6")
(def monokai-green-d        "#86C30D")
(def monokai-green-l        "#BBEF53")
(def monokai-gray-d         "#35331D")
(def monokai-gray-l         "#7B7962")
;; Adaptive higher/lower contrast accented colors
(def monokai-foreground-hc  "#141414")
(def monokai-foreground-lc  "#171A0B")
;; High contrast colors
(def monokai-yellow-hc      "#FFFACE")
(def monokai-yellow-lc      "#9A8F21")
(def monokai-orange-hc      "#FFBE74")
(def monokai-orange-lc      "#A75B00")
(def monokai-red-hc         "#FEB0CC")
(def monokai-red-lc         "#F20055")
(def monokai-magenta-hc     "#FEC6F9")
(def monokai-magenta-lc     "#F309DF")
(def monokai-violet-hc      "#F0E7FF")
(def monokai-violet-lc      "#7830FC")
(def monokai-blue-hc        "#CAF5FD")
(def monokai-blue-lc        "#1DB4D0")
(def monokai-cyan-hc        "#D3FBF6")
(def monokai-cyan-lc        "#4BBEAE")
(def monokai-green-hc       "#CCF47C")
(def monokai-green-lc       "#679A01")

(def tango-butter-light     "#fce94f")
(def tango-butter-medium    "#edd400")
(def tango-butter-dark      "#c4a000")

(def tango-orange-light	    "#fcaf3e")
(def tango-orange-medium    "#f57900")
(def tango-orange-dark      "#ce5c00")

(def tango-chocolate-light  "#e9b96e")
(def tango-chocolate-medium "#c17d11")
(def tango-chocolate-dark   "#8f5902")

(def tango-chameleon-light  "#8ae234")
(def tango-chameleon-medium "#73d216")
(def tango-chameleon-dark   "#4e9a06")

(def tango-sky-blue-light   "#729fcf")
(def tango-sky-blue-medium  "#3465a4")
(def tango-sky-blue-dark    "#204a87")

(def tango-plum-light       "#ad7fa8")
(def tango-plum-medium      "#75507b")
(def tango-plum-dark        "#5c3566")

(def tango-scarlet-red-light "#ef2929")
(def tango-scarlet-red-medium "#cc0000")
(def tango-scarlet-red-dark   "#a40000")

(def tango-aluminium-light  "#eeeeec")
(def tango-aluminium-medium "#d3d7cf")
(def tango-aluminium-dark   "#babdb6")

(def tango-grey-light       "#888a85")
(def tango-grey-medium      "#555753")
(def tango-grey-dark        "#2e3436")

(defstyles screen

  [:input.smooth {:pointer-events "auto"
                  :background "none"
                  :border "none"
                  :box-sizing "border-box"
                  ;; :color "#fff"
                  :display "block"
                  :font-size "1.2em"
                  :height "36px"
                  :padding-right "10px"
                  :color tango-aluminium-light
                  ;;:position "absolute"
                  :width "100%"}]
  [:div.category {:padding "2px"
                  :border-color monokai-orange-d}]
  [:div.sub-category {:padding "2px"
                      :color monokai-blue-hc}]
  [:div.attribute {:padding "2px"
                   :color monokai-violet-hc}]
  [:div.attribute-add {:padding "2px"
                       :color monokai-violet-lc}]
  [:span.item {:padding "2px"}]
  ;; [:search-container {:background-color "DodgerBlue"}]
  [:div.cell-wrapper {:position "fixed" :padding "4px"}]
  ;;[:input {:color "black"}]
  [:div.cursor {:position "absolute"
                :top "8px"
                :right "-5px"
                :background "#000"
                :width "5px"
                :height "5px"
                :border "1px solid #fff"
                :border-right "0px"
                :border-bottom "0px"}]
  [:div.outer-cursor {:position "relative"}]
  [:div.attribute-container {:padding "2px"}]
  [:span {:padding "2px"}]
  [:div.solid {:color tango-sky-blue-dark}]
  [:body {:font "normal 14px Arial, sans-serif"
          ;;:color monokai-green-hc
          :color tango-aluminium-light
          :background-color bg3}]
  [:.ac-renderer  ^:prefix {:font "normal 13px Arial, sans-serif"
                            :position "absolute"
                            ;; :background "#fff"
                            :border "1px solid #666"
                            :max-height "500px"
                            :overflow "scroll"
                            ;; :box-shadow "2px 2px 2px rgba(102, 102, 102, .4)" ;

                                                                           ;;:width "300px";
                            }]
  [:.ac-renderer-container {:background-color bg3
                            :position "absolute"
                            :width "100%"}]
  [:.ac-row {:cursor "pointer"
             :padding ".4em"}]
  [:.ac-highlighted {:font-weight "bold" :background-color "#737429"}]
  [:.ac-highlighted-match {:color monokai-green-d :padding 0}]
  [:.ac-highlighted-nomatch {:padding 0}]

  [:.ac-active {:background-color "#b2b4bf"}]
  #_[:button {:color "DodgerBlue"}])
