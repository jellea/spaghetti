(ns cards.core
  (:require
   [spaghetti.webaudio :as webaudio]
   [spaghetti.core :as spaghetti]
   [devcards.core :as dc :include-macros true]
   [devcards.system :refer [IMount IUnMount IConfig]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [om-tools.core :refer-macros [defcomponent]]
   [devcards.util.edn-renderer :as edn-rend]
   [clojure.string :as string]
   [sablono.core :as html :refer-macros [html]])
  (:require-macros
   [devcards.core :refer [defcard is are= are-not= format-code format-data mkdn-code mkdn-data]]))

(dc/start-devcard-ui!)

(defn open-close [x]
  (cond
   (map? x)     {:class-name "map" :open "{" :close "}"}
   (set? x)     {:class-name "set" :open "#{" :close "}"}
   (vector? x)  {:class-name "vector" :open "[" :close "]"}
   (seq? x)     {:class-name "seq" :open "(" :close ")"}))

(defn type? [x]
  (cond
    (number? x)  "number"
    (keyword? x) "keyword"
    (symbol? x)  "symbol"
    (string? x)  "string"))

(defn literal? [x]
  (and (not (seq? x))
       (not (coll? x))))

(def app-state (atom {:nodes
                      {:uuid {:id :uuid :x 500 :y 0 :type :AudioDestinationNode :node (.createGain webaudio/ctx)}
                       :uuid2 {:id :uuid2 :x 250 :y 0 :type :BiquadFilterNode :node (.createBiquadFilter webaudio/ctx)}
                       :uuid3 {:id :uuid3 :x 0 :y 0 :type :OscillatorNode :node (.createOscillator webaudio/ctx)}}
                      :wires (hash-set {:a :uuid3 :a-port :output :b :uuid2 :b-port :input} {:a :uuid2 :a-port :output :b :uuid :b-port :input})}))

(defcomponent literal [app owner {:keys [extra-type]}]
  (render [_]
   (html [:span {:onClick #(om/update! app inc) :class (str (type? app) " " extra-type )} (str app)])))

(defcomponent collection [app owner]
  (render [_]
          (let [{:keys [class-name open close]} (open-close app)]
            (html [:span.collection {:class class-name}
                   [:span.opener open]
                   [:span.contents
                    #_(if (map? app)
                      (for [x app]
                        (parse-edn (first x))
                        (parse-edn (second x)))u
                      (for [x app]
                       (parse-edn x)))]
                   [:span.closer close]]))))

(defn parse-edn [e]
  (if (literal? e)
    (om/build literal e {})
    (om/build collection e {})))

(defn treeroot [e]
   (html [:div.rendered-edn
          (parse-edn e)]))

(defn edn-editor-card [clj-data]
  "A card that renders EDN."
  (if (satisfies? IAtom clj-data)
    (dc/om-root-card #(om/component (treeroot %)) clj-data)
    (dc/markdown-card "please feed me an Atom :)")))

;; optional
(dc/start-figwheel-reloader!)


(defcard live-app-state
  (edn-editor-card app-state))

(defcard intro
  (dc/markdown-card
    "# Spaghetti Patcher!

    Sound is a hard thing to grasp. Spaghetti aims to be a web audio patcher which allows you to do audio synthesis while maintaining overview of whats happening with the sound.
    "))

(defcard audio-node
  (dc/om-root-card spaghetti/audio-node {:type :OscillatorNode :node (.createOscillator webaudio/ctx)}))

(defcard node-types
  (dc/edn-card (keys webaudio/node-types)))


(defcard patch-node
  (dc/om-root-card spaghetti/viewport app-state))

(defcard todo
  (dc/markdown-card "## Todo
   * Parameter controls
   * WebMidi
   * Better dnd
* More (custom) displays, like: FTT, ADSR
   * Sharing of patches
   * Beziered wires w physics
   "))
