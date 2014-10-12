adsr
===

Attack, decay, sustain, release envelope for automating Web Audio API AudioParams.

## Install

```bash
$ npm install adsr
```

## API

```js
var ADSR = require('adsr')
```

### ADSR(audioContext)

Returns an ADSR ModulatorNode instance.

### node.attack (get/set)

Attack time in seconds.

### node.decay (get/set)

Decay time in seconds.

### node.sustain (get/set)

Decimal representing what multiple of initial value to hold at in sustain portion of envelope.

### node.release (get/set)

Release time in seconds.

### node.value (get/set)

The target value of the attack portion of envelope.

### node.startValue (get/set)

The start value which will ramp to `node.value` over time specified by `node.attack`. Defaults to 0.

### node.endValue (get/set)

The final value which will be ramped to over time specified by `node.release`. Defaults to 0.

### node.connect(destinationAudioParam)

Connect the modulator to the desired destination audio param.

### node.disconnect()

Disconnect from any target AudioParams and reset to `node.value`.

### node.start(at)

Trigger the attack-decay-sustain portion of the envelope at the specified time relative to audioContext.currentTime.

### node.stop(at, isTarget)

Specify the time to start the release portion of the envelope. Or if `isTarget === true`, the time the release portion should complete by.

Returns the time that the release portion will complete by (this can be used to decide when to stop the source AudioNode)

## Example

```js
var audioContext = new webkitAudioContext()
var oscillator = audioContext.createOscillator()
var gain = audioContext.createGain()

oscillator.connect(gain)
gain.connect(audioContext.destination)

var envelopeModulator = ADSR(audioContext)
envelopeModulator.connect(gain.gain)

envelopeModulator.attack = 0.01 // seconds
envelopeModulator.decay = 0.4 // seconds
envelopeModulator.sustain = 0.6 // multiply gain.gain.value
envelopeModulator.release = 0.4 // seconds

envelopeModulator.start(audioContext.currentTime)
oscillator.start(audioContext.currentTime)

var stopAt = envelopeModulator.stop(audioContext.currentTime + 1)
oscillator.stop(stopAt)
```
