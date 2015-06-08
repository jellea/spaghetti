(ns spaghetti.webaudio)

(defonce ctx (js/webkitAudioContext.))
;(defonce midi (js/WebMIDIAPIWrapper. true))

(defonce node-types {
    :OscillatorNode {:transit-tag "wa-oscillator" :mount-fn (fn [node] (.start node)) :create-fn #(.createOscillator ctx) :io
                     [{:n :type :type :choices :choices ["sine" "triangle" "sawtooth" "square"]
                       :default "sine"} {:n :frequency :type :number :default 440} {:n :detune :type :number :default 0}]}
    :MidiNode {:transit-tag "wm-midi" :create-fn #(js/WebMIDIAPIWrapper.) :io [{:n :channel :type :number :default 1} {:n :type :type :choices :choices ["noteon" "noteoff" "poly aftertouch"] :default "noteon"}]}
    :GainNode {:transit-tag "wa-gain" :create-fn #(.createGain ctx) :io [:input {:n :gain :type :number :default 1}]}
    :DelayNode {:create-fn #(.createDelay ctx) :io [:input {:n :delayTime :type :number :default 0}]}
;   :AudioBufferSourceNode {:create-fn #(.createBuffer ctx) :io [:playbackRate :loop :loopStart :loopEnd :buffer]}
  ;  :PannerNode {:create-fn #(.createPanner ctx) :io [:input :panningModel :distanceModel :refDistance :maxDistance :rolloffFactor :coneInnerAngle :coneOuterAngle :coneOuterGain]}
 ;  :ADSRNode {:create-fn #(js/ADSR.) :io [:start :stop {:n :attack :type :number :default 0} {:n :decay :type :number :default 0} {:n :sustain :type :number :default 1} {:n :release :type :number :default 0}]}
 ;  :ConvolverNode {:create-fn #(.createConvolver ctx) :io [:input :buffer :normalize]}
    :DynamicsCompressorNode {:create-fn #(.createDynamicsCompressor ctx) :io [:input :threshold :knee :ratio :reduction :attack :release]}
    :BiquadFilterNode {:transit-tag "wa-biquadfilter" :create-fn #(.createBiquadFilter ctx) :io [:input {:n :type :type :choices :choices ["lowpass" "highpass" "bandpass" "lowshelf" "highshelf" "peaking" "notch" "allpass"] :default "lowpass"} {:n :frequency :type :number :default 350} {:n :Q :type :number :default 1} {:n :detune :type :number :default 0} {:n :gain :type :number :default 0}]}
    :WaveShaperNode {:transit-tag "wa-waveshaper" :create-fn #(.createWaveShaper ctx) :io [:input :curve :oversample]}
    :AudioDestinationNode {:transit-tag "wa-destination" :mount-fn (fn [node] (.connect node (.-destination ctx))) :create-fn #(.createGain ctx) :io [:input]}
    :ChannelSplitterNode {:transit-tag "wa-splitter" :create-fn #(.createChannelSplitter ctx) :io [:input :output]}
    :ChannelMergerNode {:transit-tag "wa-merger" :create-fn #(.createChannelMerger ctx) :io [:input1 :input2]}})

(defn gain-read-handler [x]
  (.createGain ctx))

(defn filter-read-handler [x]
  (.createBiquadFilter ctx))

(defn oscil-read-handler [x]
  (.createOscillator ctx))

(defn delay-read-handler [x]
  (.createDelay ctx))

(def audio-read-handlers
  {"gain" gain-read-handler
   "filter" filter-read-handler
   "delay" delay-read-handler
   "oscillator" oscil-read-handler})

(deftype ^:no-doc GainNodeHandler []
         Object
         (tag [_ v] "gain")
         (rep [_ v] "gain")
         (stringRep [this v] nil))

(deftype ^:no-doc DelayNodeHandler []
         Object
         (tag [_ v] "delay")
         (rep [_ v] "delay")
         (stringRep [this v] nil))

(deftype ^:no-doc FilterNodeHandler []
         Object
         (tag [_ v] "filter")
         (rep [_ v] "filter")
         (stringRep [this v] nil))

(deftype ^:no-doc OscillatorNodeHandler []
         Object
         (tag [_ v] "oscillator")
         (rep [_ v] "oscillator")
         (stringRep [this v] nil))

(def audio-write-handlers {(type (.createOscillator ctx)) (OscillatorNodeHandler.)
                           (type (.createBiquadFilter ctx)) (FilterNodeHandler.)
                           (type (.createDelay ctx)) (DelayNodeHandler.)
                           (type (.createGain ctx)) (GainNodeHandler.)})
