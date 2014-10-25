(ns spaghetti.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [goog.events :as events]
            [cognitect.transit :as t]
            [spaghetti.uuid :refer [make-uuid]]
            [spaghetti.webaudio :as webaudio :refer [node-types ctx midi]]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :as async :refer [<! >! chan put! sliding-buffer close!]]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(defonce app-state (atom {:wiring false
                          :menu {:x 0 :y 0 :visible false}
                          :nodes {:out {:x (- (.-innerWidth js/window) 380)
                                        :y (- (.-innerHeight js/window) 220)
                                        :id :out :type :AudioDestinationNode :node (.createGain ctx)}}
                          :wires (hash-set)}))

(defonce wiring (atom {:a nil :b nil}))

(def r (t/reader :json {:handlers webaudio/audio-read-handlers}))
(def w (t/writer :json {:handlers webaudio/audio-write-handlers}))

(defn focus-input [e app owner]
  (if (or (.-ctrlKey e) (.-metaKey e))
    (let [node (om/get-node owner)]
      (set! (.-value node) (t/write w @app))
      (.select node))))

(defn paste-state [app owner]
  (let [input-data (.-value (om/get-node owner))]
    (prn (t/read r input-data))
    ; wanted:
    #_(om/update! app (t/read r input-data))))

(defcomponent clipboard [app owner]
  (did-mount [_]
             (.listen goog/events js/document "keydown" #(focus-input % app owner)))
  (render [_]
          (html [:input {:type "textarea" :style {:display "none"} :value "" :onPaste #(paste-state app owner)}])))

(defn add-node
  [app n x y]
  (let [id (str (make-uuid))]
    (om/transact! app [:nodes] #(assoc % id {:id id :type n :node ((get-in webaudio/node-types [n :create-fn])) :x x :y y}))))

(defn toggle-menu [{:keys [x y cursor]}]
  (om/update! cursor :menu {:x x :y y :visible (not (:visible (:menu @cursor)))}))

(defcomponent contextmenu [{{x :x y :y visible :visible} :menu :as app} owner]
  (render [_]
          (html
             [:ul.contextmenu {:style {:display (if visible "block" "none") :transform (str "translate(" x "px," y "px)")}}
              (for [n (keys webaudio/node-types)]
                [:li {:onClick #(do (add-node app n (- x 150) (- y 100))
                                    (toggle-menu {:x x :y y :cursor app}))} (str "+ "(name n))])])))

(defn start-wiring [{:keys [ab portid cursor]}]
  (swap! wiring assoc ab portid))

(add-watch wiring :b #(let [wires @wiring]
                        (if (and (not (nil? (:a wires)))
                                 (not (nil? (:b wires))))
                          (do
                            (swap! app-state (fn [w] (update-in w [:wires] (fn [o] (conj o {:a (:a wires) :b (:b wires)})))))
                            (reset! wiring {:a nil :b nil})))))

(defn break-wire [cursor wire]
  (om/transact! cursor :wires #(filter (fn [w] (not (identical? w wire))) %)))

(defn calc-wire [{:keys [a b]} nodes]
  (let [node-a (get-in nodes [a])
        node-b (get-in nodes [b])
        x1     (+ (:x node-a) 300)
        y1     (+ (:y node-a) 165)
        x2     (+ (:x node-b) 100)
        y2     (+ (:y node-b) 65)]
    {:x1 x1 :y1 y1 :x2 x2 :y2 y2}))

(defcomponent wire [{:keys [wire nodes]} owner {:keys [app]}]
  (will-unmount [_]
    ; this currently breaks the scope, because you can only disconnect everything from the output
    (.disconnect (:node (get-in nodes [(:a wire)])) 0))
  (did-mount [_]
    (.connect (:node (get-in nodes [(:a wire)])) (:node (get-in nodes [(:b wire)]))))
  (render[_]
         (let [{:keys [x1 y1 x2 y2]} (calc-wire wire nodes)]
           (html [:path {:onClick #(break-wire app wire)
                         :d (str "M" x1 "," y1 " Q" (/ (+ x1 x2) 2) "," (+ (max y1 y2) 40) " " x2 "," y2)
                         :fill "none" :stroke "#444" :stroke-width 1}]))))

(defcomponent wire-canvas [app owner]
  (render [_]
          (html [:svg.maincanvas {:onClick #(toggle-menu {:x (.-clientX %) :y (.-clientY %) :cursor app})}
                 (for [w (:wires app)]
                   (om/build wire {:wire w :nodes (:nodes app)} {:opts {:app app}}))])))

(defcomponent port [app owner {:keys [parentid]}]
  (init-state [_]
              {:selected false
               :value (:default app)})
  (render-state [_ {:keys [selected value]}]
    (html
     (cond
      (= app :output) [:div.io.output {:onClick #(start-wiring {:ab :a :portid parentid})} "output"]
      (= (:type app) :choices) [:div.io.choise.input {:onClick #(start-wiring {:ab :b :portid parentid})} (str (name (:n app)))
                                [:span.value value]]
      (= (:type app) :number) [:div.io.number.input (str (name (:n app)))
                               [:span.value value]]
      :default [:div.io.input {:onClick #(start-wiring {:ab :b :portid parentid})} (name app)]))))

(defn handle-drag-event [cursor owner evt-type e]
  (when (= evt-type :down)
    (om/set-state! owner :mouse {:offset-top (.-offsetY e)
                                 :offset-left (.-offsetX e)
                                 :pressed true}))
  (when (= evt-type :up)
    (om/set-state! owner [:mouse :pressed] false))
  (when (and (= evt-type :move) (om/get-state owner [:mouse :pressed]))
    (let [{:keys [offset-top offset-left]} (om/get-state owner :mouse)
          x (- (.-clientX e) 200)
          y (- (.-clientY e) 120)]
      (om/transact! cursor #(merge % {:y y :x x})))))

(defcomponent audio-node [{:keys [x y type io node id] :as app} owner]
  (init-state [_]
      {:mouse-chan (chan (sliding-buffer 1))
       :mouse {:offset-top 0 :offset-left 0 :pressed false}})
  (will-mount [_]
      (let [mouse-chan (om/get-state owner :mouse-chan)]
        (go-loop []
          (let [[evt-type e] (<! mouse-chan)]
            (handle-drag-event app owner evt-type e))
          (recur))))
  (did-mount [_]
      (let [htmlelem   (om/get-node owner "draggable")
            mouse-chan (om/get-state owner :mouse-chan)
            mount-fn   (get-in node-types [type :mount-fn])
            scope      (js/WavyJones ctx htmlelem)]
        (if mount-fn
          (mount-fn node))
        (.connect node scope)
        (events/listen htmlelem "mousemove" #(put! mouse-chan [:move %]))
        (events/listen htmlelem "mousedown" #(put! mouse-chan [:down %]))
        (events/listen htmlelem "mouseup" #(put! mouse-chan [:up %]))))
  (render-state [_ _]
    (html
     [:div.node {:class (name type) :style {:transform (str "translate(" x "px," y "px)")}}
      [:h2 (name type)]
      ; give number to port
        (om/build-all port (take 6 (vec (get-in webaudio/node-types [type :io]))) {:opts {:parentid id}})
        (om/build port :output {:opts {:parentid id}})
        [:svg.canvas {:ref "draggable"}]])))

(defcomponent viewport [app owner]
  (will-mount [_])
  (render [_]
          (html
           [:div.patcher
            (om/build clipboard app {})
            (om/build contextmenu app {})
            (om/build-all audio-node (vals (:nodes app)) {:key :id})
            (om/build wire-canvas app {})])))

(defn main []
  (om/root
    viewport
    app-state
    {:target (. js/document (getElementById "app"))}))
