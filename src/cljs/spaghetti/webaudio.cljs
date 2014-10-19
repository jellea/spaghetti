(ns spaghetti.webaudio)

(defonce node-types {:OscillatorNode {:create-fn ".createOscillator" :io [{:n :type :type :choices :choices ["sine" "triangle" "sawtooth" "square"]
                                                                           :default "sine"} {:n :frequency :type :number :default 440} {:n :detune :type :number :default 0}]}
                     :MidiNode {:create-fn "js/WebMIDIAPIWrapper." :io [{:n :channel :type :number :default 1} {:n :type :type :choices :choices ["noteon" "noteoff" "poly aftertouch"] :default "noteon"}]}
                     :GainNode {:create-fn ".createGain" :io [:input {:n :gain :type :number :default 1}]}
                     :DelayNode {:create-fn ".createDelay" :io [:input :delayTime]}
                     :AudioBufferSourceNode {:create-fn ".createBuffer" :io [:playbackRate :loop :loopStart :loopEnd :buffer]}
                     :PannerNode {:create-fn ".createPanner" :io [:input :panningModel :distanceModel :refDistance :maxDistance :rolloffFactor :coneInnerAngle :coneOuterAngle :coneOuterGain]}
                     :ADSRNode {:create-fn "js/ADSR." :io [:start :stop {:n :attack :type :number :default 0} {:n :decay :type :number :default 0} {:n :sustain :type :number :default 1} {:n :release :type :number :default 0}]}
                     :ConvolverNode {:create-fn ".createConvolver" :io [:input :buffer :normalize]}
                     :DynamicsCompressorNode {:create-fn ".createDynamicsCompressor" :io [:input :threshold :knee :ratio :reduction :attack :release]}
                     :BiquadFilterNode {:create-fn ".createBiquadFilter" :io [:input {:n :type :type :choices :choices ["lowpass" "highpass" "bandpass" "lowshelf" "highshelf" "peaking" "notch" "allpass"] :default "lowpass"} {:n :frequency :type :number :default 350} {:n :Q :type :number :default 1} {:n :detune :type :number :default 0} {:n :gain :type :number :default 0}]}
                     :WaveShaperNode {:create-fn ".createWaveShaper" :io [:input :curve :oversample]}
                     :AudioDestinationNode {:create-fn ".log js/console" :io [:input]}
                     :ChannelSplitterNode {:create-fn ".createChannelSplitter" :io [:input :output]}
                     :ChannelMergerNode {:create-fn ".createChannelMerger" :io [:input1 :input2]}})
