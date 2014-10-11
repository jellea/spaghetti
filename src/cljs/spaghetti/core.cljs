(ns spaghetti.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [goog.events :as events]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :as html :refer-macros [html]]))

(defonce ctx (js/webkitAudioContext.))

(defonce app-state (atom {:text "Hello Chestnut!"
                          :nodes [{:type :context :node ctx} {:type :oscillator :node (.createOscillator ctx)} {:type :gain :node (.createGain ctx)}]
                          :wires []}))

(defn add-node [app n]
  (om/transact! app #(conj % {:type :oscillator :node (.createOscillator ctx)})))

(defcomponent contextmenu [app owner]
  (render [_]
          (html
           [:ul.contextmenu
            (for [n ["oscillator" "gain" "context" "filter" "delay" "biquad-filter" "convolver" "waveshaper"]]
              [:li {:onClick #(add-node app n)} (str "+ " n)])])))

(defcomponent viewport [app owner]
  (will-mount [_])
  (render [_]
          (html
           [:div
            (om/build contextmenu (:nodes app) {})
            (om/build-all nodes (:nodes app) {})
            (om/build wire-canvas app {})
            ])))

(defcomponent wire-canvas [app owner]
  (did-mount [this]
             (.addEventListener (om/get-node owner) "click" #(prn "jo!")))
  (render [_]
          (html [:svg.maincanvas])))

(defcomponent nodes [app owner]
  (render [_]
    (html
     (cond
      (= (:type app) :oscillator) (om/build oscillator-node app {})
      (= (:type app) :context) (om/build context-node app {})
      (= (:type app) :gain) (om/build gain-node app {})))))

(defcomponent context-node [app owner]
  (render-state [_ state]
                (html [:div.node.context
                       [:h2 "context"]
                       [:div.io.input "input"]
                       [:div.canvas]])))

(defcomponent gain-node [app owner]
  (init-state [_]
              {})
  (did-mount [_]
              (.connect (:node app) (.-destination ctx)))
  (render-state [_ state]
                (html [:div.node.gain
                       [:h2 "gain-node"]
                       [:div.io.input "input"]
                       [:div.io.input "gain"]
                       [:div.io.output "output"]
                       [:div.canvas]])))

(defcomponent oscillator-node [app owner]
  (init-state [_]
              {:scope (.createAnalyser ctx)})
  (did-mount [_]
             (.start (:node app) 0)
             (.connect (:node app) (.-destination ctx))
             (.connect (:node app) (:scope (om/get-state owner))))
  (render-state [_ state]
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
