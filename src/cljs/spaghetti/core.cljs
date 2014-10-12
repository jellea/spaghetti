(ns spaghetti.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [goog.events :as events]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :as async :refer [<! >! chan put! sliding-buffer close!]]
            [sablono.core :as html :refer-macros [html]]))

(defonce ctx (js/AudioContext.))
(defonce midi (js/WebMIDIAPIWrapper. true))

(defonce midi-chan (chan (sliding-buffer 50)))

(defn midi2freq [x]
  (* 440.0 (Math/pow 2.0 (/ (- x 69.0) 12.0))))

(defonce oscil (.createOscillator ctx))
(defonce filt (.createBiquadFilter ctx))
(defonce gainnode (.createGain ctx))

(set! (.-setMidiInputSelect midi)
      (fn []
        (let [inputs (-> midi .-devices .-inputs)
              seaboard (first (filter #(= (.-name %) "Seaboard") inputs))]
          (set! (.-onmidimessage seaboard) #(let [msg (.parseMIDIMessage midi %)
                                                  e   (.-event msg)]
                                              (if (= (.-subType msg) "noteOn")
                                                (set! (.-value (.-frequency oscil)) (midi2freq (.-noteNumber e))))
                                              (if (= (.-subType msg) "channelAftertouch")
                                                (let [amount (* (/ 1 127) (.-amount e))]
                                                     (set! (.-value (.-gain gainnode)) amount)))
                                              (if (= (.-subType msg) "pitchBend")
                                                (.log js/console (.-value e))
                                                  (set! (.-value (.-frequency filt)) (.-value e))
                                                  )
                                                  )
                                             ))))

(.initMidi midi nil)

(.connect oscil filt)
(.connect filt gainnode)
(.connect gainnode (.-destination ctx))

(go-loop
    (let [msg (<! midi-chan)]
      (.log js/console msg)
      ))

(defonce node-types {:OscillatorNode
                       {:create-fn #(.createOscillator ctx) :io [{:n :type :type :choices :choices ["sine" "triangle" "sawtooth" "square"]
                                                                                :default "sine"} {:n :frequency :type :number :default 440} {:n :detune :type :number :default 0}]}
                     :MidiNode {:create-fn #(js/WebMIDIAPIWrapper. nil) :io [{:n :channel :type :number :default 1} {:n :type :type :choices :choices ["noteon" "noteoff" "poly aftertouch"] :default "noteon"}]}
                     :GainNode {:create-fn #(.createGain ctx) :io [:input {:n :gain :type :number :default 1}]}
                     :DelayNode {:create-fn #(.createDelay ctx) :io [:input :delayTime]}
                     :AudioBufferSourceNode {:create-fn #(.createBuffer ctx) :io [:playbackRate :loop :loopStart :loopEnd :buffer]}
                     :PannerNode {:create-fn #(.createPanner ctx) :io [:input :panningModel :distanceModel :refDistance :maxDistance :rolloffFactor :coneInnerAngle :coneOuterAngle :coneOuterGain]}
                     :ADSRNode {:create-fn #(js/ADSR. ctx) :io [:start :stop {:n :attack :type :number :default 0} {:n :decay :type :number :default 0} {:n :sustain :type :number :default 1} {:n :release :type :number :default 0}]}
                     :ConvolverNode {:create-fn #(.createConvolver ctx) :io [:input :buffer :normalize]}
                     :DynamicsCompressorNode {:create-fn #(.createDynamicsCompressor ctx) :io [:input :threshold :knee :ratio :reduction :attack :release]}
                     :BiquadFilterNode {:create-fn #(.createBiquadFilter ctx) :io [:input {:n :type :type :choices :choices ["lowpass" "highpass" "bandpass" "lowshelf" "highshelf" "peaking" "notch" "allpass"] :default "lowpass"} {:n :frequency :type :number :default 350} {:n :Q :type :number :default 1} {:n :detune :type :number :default 0} {:n :gain :type :number :default 0}]}
                     :WaveShaperNode {:create-fn #(.createWaveShaper ctx) :io [:input :curve :oversample]}
                     :AudioDestinationNode {:create-fn #(.log js/console "I want to sleep") :io [:input]}
                     :ChannelSplitterNode {:create-fn #(.createChannelSplitter ctx) :io [:input]}
                     :ChannelMergerNode {:create-fn #(.createChannelMerger ctx) :io [:input]}})


(defonce app-state (atom {:wiring false
                          :nodes [{:type :MidiNode :node midi :x 0 :y 20 :vals {} :id :midi1}
                                  {:type :OscillatorNode :x 330 :y 30 :node oscil :id :oscil1}
                                  {:type :BiquadFilterNode :x 640 :y 25 :node filt :id :filter}
                                  {:type :MidiNode :node midi :x 10 :y 200 :vals {} :id :midi2}
                                  {:type :MidiNode :node midi :x 0 :y 380 :vals {} :id :midi3}
                                  {:type :ADSRNode :x 350 :y 250 :node (js/ADSR. ctx) :id :adsr}
                                  {:type :GainNode :x 650 :y 220 :node gainnode :id :gain}
                                  {:type :AudioDestinationNode :x 660 :y 400 :node ctx :id :out}]
                          :wires [

                                  {:x1 300 :y1 185 :x2 430 :y2 115}
                                  {:x1 310 :y1 365 :x2 450 :y2 315}
                                  {:x1 310 :y1 365 :x2 450 :y2 335}
                                  {:x1 300 :y1 550 :x2 740 :y2 110}
                                  {:x1 630 :y1 195 :x2 740 :y2 90}
                                  {:x1 940 :y1 190 :x2 750 :y2 287}
                                  {:x1 650 :y1 417 :x2 750 :y2 305}
                                  {:x1 950 :y1 385 :x2 760 :y2 465}
                                  ]}))

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
            (om/build-all audio-node (:nodes app) {:key :id})
            (om/build wire-canvas app {})])))

(defn calc-wire [{:keys [in out]}]
  {:x1 300 :y1 165 :x2 400 :y2 85})

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
              {:selected false
               :value (:default app)})
  (render-state [_ {:keys [selected value]}]
    (html
     (cond
      (= app :output) [:div.io.output "output"]
      (= (:type app) :choices) [:div.io.choise.input (str (name (:n app)))
                                [:span.value value]]
      (= (:type app) :number) [:div.io.number.input (str (name (:n app)))
                               [:span.value value]]
      :default [:div.io.input (name app)]))))

(defcomponent audio-node [{:keys [type io node id] :as app} owner]
  (init-state [_]
    {:x (:x app)
     :y (:y app)})
  (did-mount [_]
    (let [div   (om/get-node owner)
          scope (js/WavyJones ctx (.querySelector div ".canvas"))]
        (cond (= type :OscillatorNode)
          (do
            (.start node 0)
            (.connect node scope))
          (= type :BiquadFilterNode)
            (do (.connect node scope))
          (= type :AudioDestinationNode)
            (do (.connect gainnode scope))
          (= type :GainNode)
          (do
            (set! (.-value (.-gain node)) 0.3)
            (.connect node scope))
          )

        ))
  (render-state [_ {:keys [x y]}]
    (html
     (let []
       [:div.node {:class (name type) :style {:transform (str "translate(" x "px," y "px)")}}
        [:h2 (name type)]
        (om/build-all port (take 6 (vec (get-in node-types [type :io]))) {:opts {:parentid id}})
        (om/build port :output {:opts {:parentid id}})
        [:svg.canvas]]))))

(defn main []
  (om/root
    viewport
    app-state
    {:target (. js/document (getElementById "app"))}))
