function ADSR(audioContext){

  if (!(this instanceof ADSR)){
    return new ADSR(audioContext)
  }

  this._targets = []
  this.context = audioContext
  this.attack = 0
  this.decay = 0
  this.value = 1
  this.sustain = 1
  this.release = 0
  this.startValue = 0
  this.endValue = 0
}

ADSR.prototype = {

  constructor: ADSR,

  start: function(at){
    this._decayFrom = this._decayFrom = at+this.attack
    this._startedAt = at
    var targets = this._targets
    for (var i=0;i<targets.length;i++){
      var target = targets[i]
      var sustain = this.value * this.sustain

      target.cancelScheduledValues(at)

      if (this.attack){
        target.setValueAtTime(this.startValue, at)
        target.linearRampToValueAtTime(this.value, at + this.attack)
      } else {
        target.setValueAtTime(this.value, at)
      }

      if (this.decay){
        target.setTargetAtTime(sustain, this._decayFrom, getTimeConstant(this.decay))
      }
    }
  },
  stop: function(at, isTarget){
    if (isTarget){
      at = at - this.release
    }
    var endTime = at + this.release
    if (this.release){
      var targets = this._targets
      for (var i=0;i<targets.length;i++){
        var target = targets[i]

        target.cancelScheduledValues(at)

        // truncate attack (required as linearRamp is removed by cancelScheduledValues)
        if (this.attack && at < this._decayFrom){
          var valueAtTime = getValue(this.startValue, this.value, this._startedAt, this._decayFrom, at)
          target.linearRampToValueAtTime(valueAtTime, at)
        }


        target.setTargetAtTime(this.endValue, at, getTimeConstant(this.release))
      }
    }
    return endTime
  },
  connect: function(param){
    if (!~this._targets.indexOf(param)){
      this._targets.push(param)
    }
  },
  disconnect: function(){
    var targets = this._targets
    for (var i=0;i<targets.length;i++){
      var target = targets[i]
      target.cancelScheduledValues(this.context.currentTime)
      target.setValueAtTime(this.value, this.context.currentTime)
    }
    targets.length = 0
  }
}

function getTimeConstant(time){
  return Math.log(time+1)/Math.log(100)
}

function getValue(start, end, fromTime, toTime, at){
  var difference = end - start
  var time = toTime - fromTime
  var truncateTime = at - fromTime
  var phase = truncateTime / time
  return start + phase * difference
}
