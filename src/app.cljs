(ns elixir.app
  (:require [dommy.core :as dommy]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]])
  (:use-macros [dommy.macros :only [sel1]]))

(def app-state
  (atom {:shapes
         [{:id :test :type :rect :x 20 :y 30 :width 200 :height 100}
          {:id :wut :type :rect :x 300 :y 30 :width 200 :height 100}
          {:id :another :type :rect :x 300 :y 300 :width 200 :height 100}]
         :tools
         [:select :rect :ellipse :polygon]
         :active-shape :wut
         :active-tool :select}))

(defn create-drag-event [delta-x delta-y]
  (js/CustomEvent. "mousedrag"
                   (clj->js {:detail {:deltaX delta-x :deltaY delta-y}
                             :bubbles true
                             :cancelable true})))

(defn add-drag-capabilities [element]
  (let [origin-x (atom 0)
        origin-y (atom 0)
        set-origin
        (fn [event]
          (reset! origin-x (.-screenX event))
          (reset! origin-y (.-screenY event)))
        dispatch-drag-event
        (fn [event]
          (let [delta-x (- (.-screenX event) @origin-x)
                delta-y (- (.-screenY event) @origin-y)]
            (.dispatchEvent element (create-drag-event delta-x delta-y))))]
    (dommy/listen! element :mousedown
                   (fn [event]
                     (set-origin event)
                     (dommy/listen! js/window :mousemove dispatch-drag-event)))
    (dommy/listen! js/window :mouseup
                   (fn [event]
                     (dommy/unlisten! js/window :mousemove dispatch-drag-event)))))

(defn rect [{:keys [id x y width height] :as shape} owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [element (om/get-node owner)
            shape-x (atom 0)
            shape-y (atom 0)
            select-shape
            (fn [_]
              (reset! shape-x (:x @shape))
              (reset! shape-y (:y @shape))
              (swap! app-state #(assoc % :active-shape id)))
            whatever-shape
            (fn [event]
              (let [data (.-detail event)
                    delta-x (.-deltaX data)
                    delta-y (.-deltaY data)]
                (om/update! shape :x (+ @shape-x delta-x))
                (om/update! shape :y (+ @shape-y delta-y))))]
        (add-drag-capabilities element)
        (dommy/listen! element :mousedown select-shape)
        (dommy/listen! element :mousedrag whatever-shape)))
    om/IRender
    (render [_]
      (html
       [:rect {:x x :y y :width width :height height}]))))

(defn canvas [app owner]
  (reify
    om/IRender
    (render [_]
      (html [:g (om/build-all rect (:shapes app))]))))
;[:g (om/build-all rect (:shapes app))]

(defn overview [{:keys [type]} owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        [:h1 "Elixir"]]))))

(defn handle-change [event shape key]
  (om/update! shape key (.parseInt js/Number (.. event -target -value) 10)))

(defn metrics [{:keys [x y width height] :as shape} owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:section
        [:h2 "Metrics"]
        [:fieldset
         [:label.half.brief
          [:span "W"]
          [:input {:placeholder "Width"
                   :on-change #(handle-change % shape :width)
                   :value width :type "text"}]]
         [:label.half.brief
          [:span "H"]
          [:input {:placeholder "Height"
                   :on-change #(handle-change % shape :height)
                   :value height :type "text"}]]
         [:label.half.brief
          [:span "X"]
          [:input {:placeholder "X-axis"
                   :on-change #(handle-change % shape :x)
                   :value x :type "text"}]]
         [:label.half.brief
          [:span "Y"]
          [:input {:placeholder "Y-axis"
                   :on-change #(handle-change % shape :y)
                   :value y :type "text"}]]]]))))

(defn get-selected [app]
  (let [selected-id (:active-shape app)
        shapes (:shapes app)
        filtered (filter #(= (get % :id) selected-id) shapes)]
    (first filtered)))

(defn tool [tool-name owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:button (merge {:id (str (name tool-name) "Tool")
                        :on-click (fn [_]
                                    (swap! app-state
                                           #(assoc % :active-tool tool-name))
                                    (.log js/console (clj->js @app-state)))} 
                       (if (= tool-name (:active-tool @app-state))
                         {:class "selected"}))
        ]))))

(defn toolbar [app owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div
        (let [active-shape (get-selected app)]
          [:section.panel
           (om/build overview active-shape)
           (om/build metrics active-shape)])
        [:section.panel#toolbar
         [:h1 "Tools"]
         (om/build-all tool (:tools app))]]))))

(om/root toolbar app-state {:target (sel1 :#sidebar)})
(om/root canvas app-state {:target (sel1 :#elements)})

