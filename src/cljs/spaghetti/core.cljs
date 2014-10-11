(ns spaghetti.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [sablono.core :as html :refer-macros [html]]))

(defonce app-state (atom {:text "Hello Chestnut!"}))

(defcomponent viewport [app owner]
  (render [_]
          (html
           [:div
            (om/build oscillator-node app {})
            (om/build context-node app {})
            (om/build gain-node app {})])))

(defcomponent context-node [app owner]
  (init-state [_]
    {#_(webkitAudioContext. js/window)})
  (render-state [_ state]
                (html [:div.node.context
                       [:h2 "context"
                        ]
                       [:div.input "input"]
                       [:div.canvas]])))

(defcomponent gain-node [app owner]
  (render-state [_ state]
                (html [:div.node.gain
                       [:h2 "gain-node"]
                       [:div.input "input"]
                       [:div.input "gain"]
                       [:div.output "output"]
                       [:div.canvas]])))

(defcomponent oscillator-node [app owner]
  (will-mount [_]
   )
  (render [_]
    (html
     [:div.node.oscil
      [:h2 "oscillator"]
      [:div.input "start"]
      [:div.input "frequency"]
      [:div.output "output"]
      [:div.canvas]])))

(defn main []
  (om/root
    viewport
    app-state
    {:target (. js/document (getElementById "app"))}))
