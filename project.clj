(defproject spaghetti "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [ring "1.3.1"]
                 [compojure "1.2.0"]
                 [enlive "1.1.5"]
                 [om "0.7.3"]
                 [figwheel "0.1.4-SNAPSHOT"]
                 [environ "1.0.0"]
                 [com.cemerick/piggieback "0.1.3"]
                 [weasel "0.4.3-SNAPSHOT"]
                 [leiningen "2.5.0"]
                 [http-kit "2.1.19"]
                 [com.cognitect/transit-cljs "0.8.188"]
;                 [devcards "0.1.2-SNAPSHOT"]
                 [sablono "0.2.22"]
                 [prismatic/om-tools "0.3.3"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-environ "1.0.0"]]

  :min-lein-version "2.5.0"

  :uberjar-name "spaghetti.jar"

  :cljsbuild {:builds {:app {:source-paths ["src/cljs/spaghetti"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :source-map    "resources/public/js/out.js.map"
                                        :optimizations :none
                                        :preamble      ["react/react.min.js" "public/js/adsr/index.js"
                                                        "public/js/WebMIDIAPIWrapper/js/WebMIDIAPIWrapper.js"
                                                        "public/js/hammerjs/hammer.min.js"
                                                        "public/js/wavy-jones/wavy-jones.js"]
                                        :externs       ["react/externs/react.js" "public/js/adsr/adsr.externs.js"
                                                        "public/js/WebMIDIAPIWrapper/WebMIDIAPIWrapper.externs.js"
                                                        "public/js/hammerjs/hammerjs.externs.js"
                                                        "public/js/wavy-jonewavy-jones.externs.js"]
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns spaghetti.server
                                  :timeout 120000
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :plugins [[lein-figwheel "0.1.4-SNAPSHOT"]]
                   :figwheel {:http-server-root "public"
                              :port 3449
                              :css-dirs ["resources/public/css"]}
                   :env {:is-dev true}
                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]}}}}

             :uberjar {:hooks [leiningen.cljsbuild]
                       :env {:production true}
                       :omit-source true
                       :aot :all
                       :cljsbuild {:builds {:app
                                            {:source-paths ["env/prod/cljs"]
                                             :compiler
                                             {:optimizations :advanced
                                              :pretty-print false
                                              }}}}}})
