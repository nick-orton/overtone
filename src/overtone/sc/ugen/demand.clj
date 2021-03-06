(ns overtone.sc.ugen.demand
  (:use (overtone.sc.ugen common)))

(def specs
     [
      {:name "Demand",
       :args [{:name "trig" :doc "Can be any signal. A trigger happens when the signal changes from non-positive to positive."}
              {:name "reset" :doc "Resets the list of ugens when triggered."}
              {:name "demandUGens", :mode :append-sequence-set-num-outs :doc "list of demand rate ugens"}],
       :check (same-rate-as-first-input)
       :auto-rate true
       :doc "On every trigger it demands the next value from each of the demand ugens passed as args.  Used to pull values from the other demand rate ugens.

By design, a reset trigger only resets the demand ugens; it does not reset the value at Demand's output. Demand continues to hold its value until the next value is demanded, at which point its output value will be the first expected item in the list."}



      {:name "Duty",
       :args [{:name "dur", :default 1.0 :doc "time values. Can be a demand ugen or any signal. The next level is acquired after duration.
"}
              {:name "reset", :default 0.0 :doc "trigger or reset time values. Resets the list of ugens and the duration ugen when triggered. The reset input may also be a demand ugen, providing a stream of reset times."}
              {:name "action", :default :none :map DONE-ACTIONS}
              {:name "level", :default 1.0 :doc "demand ugen providing the output values."}
]
       :check (fn [rate num-outs [dur reset & _] spec]
                (if (and (dr? dur)
                         (not (or (dr? reset)
                                  (ir? reset)
                                  (rate-of? reset rate))))
                  "TODO write error string. and understad why this is an error"))
       :doc "Expects demand ugen args for dur and level.  Uses successive dur values to determine how long to wait before emitting each level value.

A value is demanded each ugen in the list and output according to a stream of duration values.
The unit generators in the list should be 'demand' rate.

When there is a trigger at the reset input, the demand rate ugens in the list and the duration are reset. The reset input may also be a demand ugen, providing a stream of reset times."}

      {:name "TDuty" :extends "Duty"
       :args [{:name "dur", :default 1.0 :doc "time values. Can be a demand ugen or any signal. The next trigger value is acquired after the duration provided by the last time value."}
              {:name "reset", :default 0.0 :doc "trigger or reset time values. Resets the list of ugens and the duration ugen when triggered. The reset input may also be a demand ugen, providing a stream of reset times."}
              {:name "action", :default 0 :map DONE-ACTIONS}
              {:name "level", :default 1.0 :doc "demand ugen providing the output values."}
              {:name "gapFirst", :default 0}]
       :doc "A value is demanded each ugen in the list and output  as a trigger according to a stream of duration values. The unit generators in the list should be 'demand' rate.

When there is a trigger at the reset input, the demand rate ugens in the list and the duration are reset.The reset input may also be a demand ugen, providing a stream of reset times."}

      {:name "DemandEnvGen",
       :args [{:name "level" :doc "demand ugen (or other ugen) returning level values"}
              {:name "dur" :doc "demand ugen (or other ugen) returning time values"}
              {:name "shape", :default 1 :doc "demand ugen (or other ugen) returning shape number - the number given is the shape number"}
              {:name "curve", :default 0 :doc "demand ugen (or other ugen) returning curve values -  if shape is 5, this is the curve factor. Thee possible values are: 0 - flat segments, 1 - linear segments, the default, 2 - natural exponential growth and decay. In this case, the levels must all be nonzero and the have the same sign, 3 - sinusoidal S shaped segments, 4 - sinusoidal segments shaped like the sides of a Welch window, a Float - a curvature value for all segments, an Array of Floats - curvature values for each segments."
               }
              {:name "gate", :default 1.0 :doc "control rate gate if gate is x >= 1, the ugen runs, if gate is 0 > x > 1, the ugen is released at the next level (doneAction), if gate is x <= 0, the ugen is sampled end held"}
              {:name "reset", :default 1.0 :doc "if reset crosses from nonpositive to positive, the ugen is reset at the next level. If it is > 1, it is reset immediately."}
              {:name "levelScale", :default 1.0 :doc "demand ugen returning level scaling values"}
              {:name "levelBias", :default 0.0 :doc "demand ugen returning level offset values"}
              {:name "timeScale", :default 1.0 :doc "demand ugen returning time scaling values"}
              {:name "action", :default :none :map DONE-ACTIONS}]
;;       :init (fn [rate [l d s c gate reset ls lb ts da] spec]
;;               (if (or (ar? gate) (ar? reset))
;;                 [l d s c (as-ar gate) (as-ar reset) ls lb ts da]))
       :doc "Plays back break point envelope contours (levels, times, shapes) given by demand ugens. The next values are called when the next node is reached."}

      ;; DUGen : UGen {
      ;;  init { arg ... argInputs;
      ;;    super.init(*argInputs);
      ;;    this.forceAudioRateInputsIntoUGenGraph;
      ;;  }
      ;;  forceAudioRateInputsIntoUGenGraph {
      ;;      inputs.do { |in| if(in.rate == \audio) { in <! 0 } }; }
      ;;
      ;;    // some n-ary op special cases
      ;;
      ;;    linlin { arg inMin, inMax, outMin, outMax, clip=\minmax;
      ;;        ^((this.prune(inMin, inMax, clip)-inMin)/(inMax-inMin) * (outMax-outMin) + outMin);
      ;;  }
      ;;
      ;;  linexp { arg inMin, inMax, outMin, outMax, clip=\minmax;
      ;;    ^(pow(outMax/outMin, (this-inMin)/(inMax-inMin)) * outMin)
      ;;      .prune(inMin, inMax, clip);
      ;;  }
      ;;
      ;;  explin { arg inMin, inMax, outMin, outMax, clip=\minmax;
      ;;    ^(log(this.prune(inMin, inMax, clip)/inMin))
      ;;      / (log(inMax/inMin)) * (outMax-outMin) + outMin
      ;;  }
      ;;
      ;;  expexp { arg inMin, inMax, outMin, outMax, clip=\minmax;
      ;;    ^pow(outMax/outMin, log(this.prune(inMin, inMax, clip/inMin) / log(inMax/inMin)) * outMin)
      ;;  }
      ;; }
      ;; TODO understand forceAudioRateInputsIntoUGenGraph
      ;; which is implemented in the pseudo-ugen DUGen parent of below

      {:name "Dseries",
       :args [{:name "length", :default INFINITE, :doc "number of values to create"}
              {:name "start", :default 1, :doc "start value"}
              {:name "step", :default 1, :doc "step value"}]
       :rates #{:dr}
       :doc "Generate a series of incrementing values on demand."}

      {:name "Dgeom",
       :args [{:name "length", :default INFINITE, :doc "doc number of values to create"}
              {:name "start", :default 1, :doc "start value"}
              {:name "grow", :default 2, :doc "value by which to grow ( x = x[-1] * grow )"}
]
       :rates #{:dr}
       :doc "Generate a geometric sequence on demand. The arguments can be a number or any other ugen"}

      {:name "Dbufrd",
       :args [{:name "bufnum", :default 0.0, :doc "buffer number to read from"}
              {:name "phase", :default 0.0, :doc "index into the buffer"}
              {:name "loop", :default 1.0, :doc "when phase exceeds number of frames in buffer, loops when set to 1"}],
       :rates #{:dr}
       :doc "Read values from a buffer on demand, using phase (index) value that is also pulled on demand. All inputs can be either demand ugen or any other ugen."}

      {:name "Dbufwr",
       :args [{:name "bufnum", :default 0, :doc "buffer number to read from (single channel buffer)"}
              {:name "phase", :default 0.0, :doc "index into the buffer"}
              {:name "input", :default 0.0 :doc "single channel input"}
              {:name "loop", :default 1.0, :doc "when phase exceeds number of frames in buffer, loops when set to 1"}],
       :rates #{:dr}
       :doc "Write a demand sequence into a buffer. All inputs can be either demand ugen or any other ugen."}

      {:name "Dseq",
       :args [{:name "list", :mode :append-sequence, :array true, :doc "array of values or other ugens"}
              {:name "num-repeats", :default 1, :doc "number of repeats"}],
       :rates #{:dr}
       :doc "Demand rate sequence generator. Outputs a sequence of values, possibly repeating multiple times. Use INF as a repeat val to create an endless loop."}

      {:name "Dser"
       :args [{:name "list", :mode :append-sequence, :array true :doc "array of values or other ugens"}
              {:name "count", :default 1 :doc "number of values to return"}],
       :doc "Demand rate sequence generator. Generates a sequence of values Like dseq, except outputs only count total values, rather than repeating."
       :rates #{:dr}}

      {:name "Dshuf" :extends "Dseq"
       :doc "Demand rate random sequence generator. Shuffle a sequence once and then output it one or more times."}

      {:name "Drand" :extends "Dseq"
       :doc "Demand rate random sequence generator. Generate a random ordering of an input sequence."}

      {:name "Dxrand" :extends "Dseq"
       :doc "Demand rate random sequence generator. Generate a random ordering of the given sequence without repeating any element until all elements have been returned."}

      {:name "Dswitch1",
       :args [{:name "list", :mode :append-sequence, :array true :doc "array of values or other ugens"}
              {:name "index" :doc "which of the inputs to return"}],
       :rates #{:dr}
       :doc "A demand rate switch that can be used to select one of multiple demand rate inputs."}

      {:name "Dswitch" :extends "Dswitch1"
       :doc "A demand rate switch. In difference to Dswitch1, Dswitch embeds all items of an input demand ugen first before looking up the next index."}

      {:name "Dwhite",
       :args [
              {:name "length", :default INFINITE :doc "number of values to create"}
              {:name "lo", :default 0.0 :doc "minimum value"}
              {:name "hi", :default 1.0 :doc "maximum value"}
]
       :rates #{:dr}
       :doc "Generate a sequence of random values in the continuous range between lo and hi."}

      {:name "Diwhite" :extends "Dwhite"
       :doc "Generates a sequence of random integer values between lo and hi. The arguments can be a number or any other ugen"}

      {:name "Dbrown",
       :args [{:name "length", :default INFINITE :doc "number of values to create"}
              {:name "lo", :default 0.0 :doc "minimum value"}
              {:name "hi", :default 1.0 :doc "maximum value"}
              {:name "step", :default 0.01 :doc "maximum step for each new value"}
]
       :rates #{:dr}
       :doc "Generates a sequence of random values in the continuous range betweeen lo and hi, not exceeding step from one value to the next. The arguments can be a number or any other ugen"}

      {:name "Dibrown" :extends "Dbrown"
       :doc "Generates a sequence of random integer values betweeen lo and hi, not exceeding step from one value to the next. The arguments can be a number or any other ugen"}

      {:name "Dstutter",
       :args [{:name "num-repeats" :doc "number of repeats (can be a demand ugen)"}
              {:name "in" :doc "input ugen"}],
       :rates #{:dr}
       :doc "Replicates input values n times on demand.  Both inputs can be demand rate ugens."}

      {:name "Donce",
       :args [{:name "in"}],
       :rates #{:dr}}

      {:name "Dpoll",
       :args [{:name "in" :doc "demand ugen to poll values from"}
              {:name "trig-id" :default -1
               :doc "if greater than 0, a '/tr' message is sent back to the client (similar to SendTrig)"}
              {:name "label" :doc "a label string" :default "dpoll-val" :mode :append-string}
              {:name "run", :default 1.0 :doc "activation switch 0 or 1 (can be a demand ugen)"}
],
       :rates #{:dr}
       :doc "Print the value of an input demand ugen. The print-out is in the form: label: value block offset: offset.

WARNING: Printing values from the Server in intensive for the CPU. Poll should be used for debugging purposes.
"}
      ])
