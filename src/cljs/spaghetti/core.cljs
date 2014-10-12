(ns spaghetti.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [goog.events :as events]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :as async :refer [<! chan put! sliding-buffer close!]]
            [sablono.core :as html :refer-macros [html]]))

(defonce ctx (js/webkitAudioContext.))

(defonce node-types {:OscillatorNode {:create-fn #(.createOscillator ctx) :io [{:n :type :choises ["sine" "triangle" "square"]} :frequency :detune]}
                     :GainNode {:create-fn #(.createGain ctx) :io [:input :gain]}
                     :DelayNode {:create-fn #(.createDelay ctx) :io [:input :delayTime]}
                     :AudioBufferSourceNode {:create-fn #(.createBuffer ctx) :io [:playbackRate :loop :loopStart :loopEnd :buffer]}
                     :PannerNode {:create-fn #(.createPanner ctx) :io [:input :panningModel :distanceModel :refDistance :maxDistance :rolloffFactor :coneInnerAngle :coneOuterAngle :coneOuterGain]}
                     :ConvolverNode {:create-fn #(.createConvolver ctx) :io [:input :buffer :normalize]}
                     :DynamicsCompressorNode {:create-fn #(.createDynamicsCompressor ctx) :io [:input :threshold :knee :ratio :reduction :attack :release]}
                     :BiquadFilterNode {:create-fn #(.createBiquadFilter ctx) :io [:input :type :frequency :Q :detune :gain]}
                     :WaveShaperNode {:create-fn #(.createWaveShaper ctx) :io [:input :curve :oversample]}
                     :AudioDestinationNode {:create-fn #(.log js/console "I want to sleep") :io [:input]}
                     :ChannelSplitterNode {:create-fn #(.createChannelSplitter ctx) :io [:input]}
                     :ChannelMergerNode {:create-fn #(.createChannelMerger ctx) :io [:input]}})

(defonce app-state (atom {:wiring false
                          :nodes [{:type :OscillatorNode :node (.createOscillator ctx)} {:type :BiquadFilterNode :node (.createBiquadFilter ctx)} {:type :AudioDestinationNode :node ctx}]
                          :wires [{:x1 300 :y1 165 :x2 400 :y2 65} {:x1 600 :y1 165 :x2 700 :y2 65}]}))

(defn add-node [app n]
  (om/transact! app #(conj % {:type n :node (.createOscillator ctx)})))

(defcomponent contextmenu [{:keys [menu] :as app} owner]
  (render-state [_ _]
            (html
             [:ul.contextmenu {:style {:transform "translate(0px,20px)"}}
              (for [n (keys node-types)]
                [:li {:onClick #(add-node app n)} (str "+ "(name n))])])))

(defcomponent viewport [app owner]
  (will-mount [_])
  (render [_]
          (html
           [:div
            (om/build contextmenu (:nodes app) {})
            (om/build-all audio-node (:nodes app) {})
            (om/build wire-canvas app {})])))

(defcomponent wire [{:keys [x1 x2 y1 y2]} owner]
  (render-state [_ _]
                (html [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke "#444" :stroke-width 1}])))

(defcomponent wire-canvas [app owner]
  (render [_]
          (html [:svg.maincanvas
                 (om/build-all wire (:wires app) {})])))

(defn start-wiring [])
(defn make-wire [])
(defn break-wire [])

(defcomponent port [app owner]
  (init-state [_]
              {:selected false})
  (render-state [_ {:keys [selected]}]
    (html
     (cond
      (= app :output) [:div.io.output "output"]
      (some? (:choises app)) [:div.io.input (str (name (:n app)))]
      :default [:div.io.input (name app)]))))

(defcomponent audio-node [{:keys [type io node]} owner]
  (did-mount [_]
    (let [div        (om/get-node owner)
          scope      (js/WavyJones ctx (.querySelector div ".canvas"))]
        (if (= type :OscillatorNode)
          (do
            (.start node 0)
            (.connect node (.-destination ctx))))
        (.connect node scope)))
  (render-state [_ _]
    (html
         [:div.node {:class (name type)}
          [:h2 (name type)]
          (om/build-all port (take 6 (vec (get-in node-types [type :io]))) {})
          (om/build port :output {})
          [:svg.canvas]])))

(defn main []
  (om/root
    viewport
    app-state
    {:target (. js/document (getElementById "app"))}))
