(ns spaghetti.webaudio)

(defonce ctx (js/AudioContext.))
(defonce midi (js/WebMIDIAPIWrapper. true))

(defonce node-types {:OscillatorNode {:mount-fn (fn [node] (.start node)) :create-fn #(.createOscillator ctx) :io [{:n :type :type :choices :choices ["sine" "triangle" "sawtooth" "square"]
                                                                           :default "sine"} {:n :frequency :type :number :default 440} {:n :detune :type :number :default 0}]}
                     :MidiNode {:create-fn #(js/WebMIDIAPIWrapper.) :io [{:n :channel :type :number :default 1} {:n :type :type :choices :choices ["noteon" "noteoff" "poly aftertouch"] :default "noteon"}]}
                     :GainNode {:create-fn #(.createGain ctx) :io [:input {:n :gain :type :number :default 1}]}
                     :DelayNode {:create-fn #(.createDelay ctx) :io [:input :delayTime]}
                     :AudioBufferSourceNode {:create-fn #(.createBuffer ctx) :io [:playbackRate :loop :loopStart :loopEnd :buffer]}
                     :PannerNode {:create-fn #(.createPanner ctx) :io [:input :panningModel :distanceModel :refDistance :maxDistance :rolloffFactor :coneInnerAngle :coneOuterAngle :coneOuterGain]}
                     :ADSRNode {:create-fn #(js/ADSR.) :io [:start :stop {:n :attack :type :number :default 0} {:n :decay :type :number :default 0} {:n :sustain :type :number :default 1} {:n :release :type :number :default 0}]}
                     :ConvolverNode {:create-fn #(.createConvolver ctx) :io [:input :buffer :normalize]}
                     :DynamicsCompressorNode {:create-fn #(.createDynamicsCompressor ctx) :io [:input :threshold :knee :ratio :reduction :attack :release]}
                     :BiquadFilterNode {:create-fn #(.createBiquadFilter ctx) :io [:input {:n :type :type :choices :choices ["lowpass" "highpass" "bandpass" "lowshelf" "highshelf" "peaking" "notch" "allpass"] :default "lowpass"} {:n :frequency :type :number :default 350} {:n :Q :type :number :default 1} {:n :detune :type :number :default 0} {:n :gain :type :number :default 0}]}
                     :WaveShaperNode {:create-fn #(.createWaveShaper ctx) :io [:input :curve :oversample]}
                     :AudioDestinationNode {:mount-fn (fn [node] (.connect node (.-destination ctx))) :create-fn #(.createGain ctx) :io [:input]}
                     :ChannelSplitterNode {:create-fn #(.createChannelSplitter ctx) :io [:input :output]}
                     :ChannelMergerNode {:create-fn #(.createChannelMerger ctx) :io [:input1 :input2]}})
