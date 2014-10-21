(ns spaghetti.devcards
  (:require
   [spaghetti.webaudio :as webaudio]
   [spaghetti.core :as spaghetti]
   [devcards.core :as dc :include-macros true]
   [devcards.system :refer [IMount IUnMount IConfig]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [clojure.string :as string]
   [sablono.core :as sab :refer [html] :include-macros true])
  (:require-macros
   [devcards.core :refer [defcard is are= are-not= format-code format-data mkdn-code mkdn-data]]))

(dc/start-devcard-ui!)

;; optional
(dc/start-figwheel-reloader!)

;; required ;)

(defcard intro
  (dc/markdown-card
    "# Spaghetti Patcher!

    Sound is a hard thing to grasp. Spaghetti aims to be a web audio patcher which allows you to do audio synthesis while maintaining overview of whats happening with the sound.
    "))

(defcard audio-node
  (dc/om-root-card spaghetti/audio-node {:type :OscillatorNode :node (.createOscillator webaudio/ctx)}))

(defcard node-types
  (dc/edn-card (keys webaudio/node-types)))

(def app-state (atom {:nodes
                      {:uuid {:id :uuid :x 500 :y 0 :type :AudioDestinationNode :node (.createGain webaudio/ctx)}
                       :uuid2 {:id :uuid2 :x 250 :y 0 :type :BiquadFilterNode :node (.createBiquadFilter webaudio/ctx)}
                       :uuid3 {:id :uuid3 :x 0 :y 0 :type :OscillatorNode :node (.createOscillator webaudio/ctx)}}
                      :wires (hash-set {:a :uuid3 :a-port :output :b :uuid2 :b-port :input} {:a :uuid2 :a-port :output :b :uuid :b-port :input})}))

(defcard live-app-state
  (dc/edn-card app-state))

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
