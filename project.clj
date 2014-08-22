(defproject elixir "1.0.0"
  :dependencies [[org.clojure/clojurescript "0.0-2202"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [sablono "0.2.15"]
                 [prismatic/dommy "0.1.2"]
                 [om "0.5.3"]]
  :main app.ns
  :description "Elixir reborn from the ashes in Clojurescript"
  :jvm-opts ^:replace ["-Xmx1024m" "-server"]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src"]
     :compiler
     {:output-to "scripts/main.js"
      :output-dir "scripts/out"
      :optimizations :none
      :source-map true}}
    {:id "release"
     :source-paths ["src"]
     :compiler
     {:output-to "scripts/main.js"
      :optimizations :advanced
      :pretty-print false
      :preamble ["react/react.min.js"]
      :externs ["react/externs/react.js"]}}]})
