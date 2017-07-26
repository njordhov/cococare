(defproject cljsnode "0.1.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.562"]
                 [org.clojure/core.async "0.3.443"]
                 [reagent "0.7.0"]
                 [secretary "1.2.3"]
                 [kioo "0.5.0-SNAPSHOT" :exclusions [cljsjs/react]]
                 [com.taoensso/timbre "4.10.0"]
                 [cljs-http "0.1.43"]
                 [camel-snake-kebab "0.4.0"]]

  :npm {:dependencies [[express "4.14.1"]
                       [express-ws "2.0.0"]
                       [ws "0.8.0"]
                       [body-parser "1.14.1"]
                       [xmlhttprequest "1.8.0"]
                       [xmldom "0.1.27"]
                       [source-map-support "*"]
                       [react "15.4.2"]
                       [react-dom "15.4.2"]
                       [ciscospark "1.4.0"]]
        :root :root}

  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-pprint "1.1.2"]
            [lein-npm "0.6.2"]]

  :min-lein-version "2.5.3"

  :hooks [leiningen.cljsbuild]

  :aliases {"test" ["with-profile" "test" "doo" "node" "server" "once"]}

  :main "main.js"

  :source-paths ["src/cljs"]

  :clean-targets ^{:protect false} [[:cljsbuild :builds :server :compiler :output-to]
                                    [:cljsbuild :builds :app :compiler :output-dir]
                                    "node_modules"
                                    :target-path :compile-path]

  :cljsbuild {:builds
              {:app
               {:source-paths ["src/browser" "src/cljs"]
                :compiler {:output-to "resources/public/js/out/app.js"
                           :output-dir "resources/public/js/out"
                           :asset-path "js/out"
                           :main app.start
                           :foreign-libs []
                           :infer-externs true
                           :optimizations :none}}
               :server
               {:source-paths ["src/node" "src/cljs"]
                :compiler {:target :nodejs
                           :output-to "main.js"
                           :output-dir "target"
                           :main server.core
                           :optimizations :none}}}}


  :profiles {:dev
             {:plugins
              [[lein-figwheel "0.5.10"] ;; was "0.5.4-7"
               [lein-doo "0.1.7"]]
              :dependencies
              [[figwheel-sidecar "0.5.10"]
               [org.clojure/tools.namespace "0.2.11"]
               [org.clojure/tools.nrepl "0.2.12"]
               [com.cemerick/piggieback "0.2.1"]
               [proto-repl "0.3.1"]
               [proto-repl-charts "0.3.2"]]
              :figwheel {:http-server-root "public"
                         :css-dirs ["resources/public/css"]
                         :server-logfile "logs/figwheel.log"
                         :builds-to-start [:app :server]
                         :nrepl-middleware [clojure.tools.nrepl.middleware.session/add-stdin
                                            cemerick.piggieback/wrap-cljs-repl]
                         :server-port 3453 ;; just use default 3449 instead
                         :load-all-builds false}
              :cljsbuild
              {:builds
               {:app
                {:compiler
                 {:pretty-print true
                  :source-map true}
                 :figwheel true}
                :server
                {:compiler
                 {:pretty-print true
                  :source-map true}
                 :figwheel {:heads-up-display false}}}}
              :npm {:dependencies [[ws "*"]]}}

             :test {:cljsbuild
                    {:builds
                     {:server
                      {:source-paths ["test"]
                       :compiler {:main runners.doo
                                  :optimizations :none
                                  :output-to "target/test/server.js"
                                  :output-dir "target/test"}}}}}

             :gen {:prep-tasks ^:replace []}

             :production
             {:env {:production true}
              :prep-tasks
               ;; separate builds to avoid leaking pubnub shims in advanced compilation
               ;; until fixed in cljsbuild. Without it app.js will contain cljs.nodejs
              ["compile"
               ["with-profile" "-dev,+gen,+production" "cljsbuild" "once" "server"]
               ["with-profile" "-dev,+gen,+production" "cljsbuild" "once" "app"]]
              :cljsbuild
              {:builds
               {:server
                {:compiler {}};:optimizations :simple
                            ;; likely optional unless :simple
                            ;:foreign-libs [{:file "src/node/polyfill/simple.js"
                            ;                :provides ["polyfill.simple"]
                            ;:pretty-print false}}
                :app
                {:compiler {:output-dir "target/app/out"
                            :optimizations :advanced
                            :pretty-print false}}}}}})
