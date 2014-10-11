(ns spaghetti.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [goog.events :as events]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :as async :refer [<! chan put! sliding-buffer close!]]
            [sablono.core :as html :refer-macros [html]]))

(defonce ctx (js/webkitAudioContext.))

(defonce node-types {:OscillatorNode [:type
                                      :frequency
                                      :detune]
                     :GainNode [:gain]
                     :DelayNode [:delayTime]
                     :PannerNode [
                                  :panningModel
                                  :distanceModel
                                  :refDistance
                                  :maxDistance
                                  :rolloffFactor
                                  :coneInnerAngle
                                  :coneOuterAngle
                                  :coneOuterGain
                                  ]
                     :ConvolverNode [
                                     :buffer
                                     :normalize]
                     :DynamicsCompressorNode [ :threshold
                                              :knee
                                              :ratio
                                              :reduction
                                              :attack
                                              :release]
                     :BiquadFilterNode [:type
                                        :frequency
                                        :Q
                                        :detune
                                        :gain]
                     :WaveShaperNode [:curve
                                      :oversample]
                     :AnalyserNode [:fftSize
                                    :minDecibels
                                    :maxDecibels
                                    :smoothingTimeConstant
                                    :frequencyBinCount]
                     :AudioDestinationNode []
                     :ChannelSplitterNode []
                     :ChannelMergerNode []
                     :MediaElementAudioSourceNode []
                     :MediaStreamAudioSourceNode []
                     :MediaStreamAudioDestinationNode [:stream]})

(defonce app-state (atom {:menu {:x 0 :y 0 :visible false}
                          :text "Hello Chestnut!"
                          :nodes [{:type :oscillator :node (.createOscillator ctx)} {:type :filter :node (.createBiquadFilter ctx)} {:type :context :node ctx}]
                          :wires [{:x1 300 :y1 153 :x2 400 :y2 70} {:x1 600 :y1 153 :x2 700 :y2 70}]}))

(defn add-node [app n]
  (om/transact! app #(conj % {:type :oscillator :node (.createOscillator ctx)})))

(defcomponent contextmenu [{:keys [menu] :as app} owner]
  (render-state [_ _]
            (html
             [:ul.contextmenu {:style {:transform "translate(0px,20px)"}}
              (for [n (keys node-types)]
                [:li {:onClick #(add-node app n)} (str "+ " (name n))])])))

(defcomponent viewport [app owner]
  (will-mount [_])
  (render [_]
          (html
           [:div
            (om/build contextmenu (:nodes app) {})
            (om/build-all nodes (:nodes app) {})
            (om/build wire-canvas app {})
            ])))

(defn toggle-contextmenu [e app]
  (.log js/console @app)
  (om/transact! app #(assoc % :menu {:x (.-clientX e) :y (.-clientY e) :visible true})))

(defcomponent wire [{:keys [x1 x2 y1 y2]} owner]
  (render-state [_ _]
                (html [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke "#444" :stroke-width 1}]))
  )
(defcomponent wire-canvas [app owner]
  (did-mount [this]
             (.addEventListener (om/get-node owner) "click" #(prn "jo!")))
  (render [_]
          (html [:svg.maincanvas {:onClick #(toggle-contextmenu % app)}
                 (om/build-all wire (:wires app) {})
          ])))

(defcomponent nodes [app owner]
  (render [_]
    (html
     (cond
      (= (:type app) :oscillator) (om/build oscillator-node app {})
      (= (:type app) :filter) (om/build filter-node app {})
      (= (:type app) :context) (om/build context-node app {})
      (= (:type app) :gain) (om/build gain-node app {})))))

(defcomponent context-node [app owner]
  (did-mount [_]
             (let [node  (om/get-node owner)
                   scope (js/WavyJones ctx (.querySelector node ".canvas"))]
               (.connect node scope)))
  (render-state [_ state]
                (html [:div.node.context
                       [:h2 "context"]
                       [:div.io.input "input"]
                       [:svg.canvas]])))

(defcomponent filter-node [app owner]
  (did-mount [_]
              (.connect (:node app) (.-destination ctx)))
  (render-state [_ state]
                (html [:div.node.gain
                       [:h2 "gain"]
                       [:div.io.input "input"]
                       [:div.io.input "gain"]
                       [:div.io.output "output"]
                       [:svg.canvas]])))

(defcomponent gain-node [app owner]
  (init-state [_]
              {})
  (did-mount [_]
              (.connect (:node app) (.-destination ctx)))
  (render-state [_ state]
                (html [:div.node.gain
                       [:h2 "gain"]
                       [:div.io.input "input"]
                       [:div.io.input "gain"]
                       [:div.io.output "output"]
                       [:svg.canvas]])))

(defcomponent oscillator-node [app owner]
  (did-mount [this]
      (let [node       (om/get-node owner)
            scope      (js/WavyJones ctx (.querySelector node ".canvas"))]
        (.start (:node app) 0)
        (.connect (:node app) scope)
        (.connect scope (.-destination ctx))))
  (render-state [_ _]
    (html
         [:div.node.oscil
          [:h2 "oscillator"]
          [:div.io.input "start"]
          [:div.io.input "frequency"]
          [:div.io.output "output"]
          [:svg.canvas]])))

(defn main []
  (om/root
    viewport
    app-state
    {:target (. js/document (getElementById "app"))}))
