(defproject todo-split "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[cider/cider-nrepl "0.15.1"]
                 [clj-oauth "1.5.4"]
                 [clj-time "0.14.2"]
                 [cljs-ajax "0.7.3"]
                 [com.google.guava/guava "20.0"]
                 [com.novemberain/monger "3.1.0" :exclusions [com.google.guava/guava]]
                 [com.rpl/specter "1.1.0"]
                 [compojure "1.6.0"]
                 [cprop "0.1.11"]
                 [funcool/struct "1.2.0"]
                 [luminus-immutant "0.2.4"]
                 [luminus-nrepl "0.1.4"]
                 [luminus/ring-ttl-session "0.3.2"]
                 [markdown-clj "1.0.2"]
                 [metosin/compojure-api "1.1.11"]
                 [metosin/muuntaja "0.5.0"]
                 [metosin/ring-http-response "0.9.0"]
                 [mount "0.1.12"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238" :scope "provided"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.reader "1.2.2"]
                 [org.webjars.bower/tether "1.4.3"]
                 [org.webjars/bootstrap "4.0.0"]
                 [org.webjars/font-awesome "5.0.6"]
                 [re-frame "0.10.5"]
                 [day8.re-frame/undo "0.3.2"]
                 [akiroz.re-frame/storage "0.1.2"]
                 [reagent "0.7.0"]
                 [refactor-nrepl "2.4.0-SNAPSHOT"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [secretary "1.2.3"]
                 [selmer "1.11.7"]
                 [kee-frame "0.2.1"]]

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot todo-split.core

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-immutant "2.1.0"]]
  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :figwheel
  {:http-server-root "public"
   :nrepl-port 7002
   :css-dirs ["resources/public/css"]
   :init todo-split.core/-main
   :nrepl-middleware
   [cemerick.piggieback/wrap-cljs-repl
    refactor-nrepl.middleware/wrap-refactor
    cider.nrepl/cider-middleware]}

  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
     :figwheel {:on-jsload "todo-split.core/start-kf!"}
     :compiler
     {:main "todo-split.app"
      :asset-path "/js/out"
      :output-to "target/cljsbuild/public/js/app.js"
      :output-dir "target/cljsbuild/public/js/out"
      :source-map true
      :optimizations :none
      :pretty-print true
      :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true
                        "day8.re_frame.tracing.trace_enabled_QMARK_" true}
      :aot-cache true
      :preloads [day8.re-frame-10x.preload]}}
    :test
    {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
     :compiler
     {:output-to "target/cljsbuild/public/js/test.js"
      :output-dir "target/cljsbuild/public/js/test-out"
      :asset-path "/js/test-out"
      :main "todo-split.doo-runner"
      :source-map true
      :optimizations :none
      :aot-cache true
      :pretty-print true}}
    :min
    {:source-paths ["src/cljc" "src/cljs" "env/prod/cljs"]
     :compiler
     {:output-dir "target/cljsbuild/public/js"
      :output-to "target/cljsbuild/public/js/app.js"
      :source-map "target/cljsbuild/public/js/app.js.map"
      :optimizations :advanced
      :aot-cache true
      :pretty-print false
      :closure-warnings
      {:externs-validation :off :non-standard-jsdoc :off}
      :externs ["react/externs/react.js"]}}}}

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
             :dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]
             :aot :all
             :uberjar-name "todo-split.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-server" "-Dconf=dev-config.edn"]
                  :dependencies [[binaryage/devtools "0.9.9"]
                                 [com.cemerick/piggieback "0.2.2"]
                                 [day8.re-frame/re-frame-10x "0.3.3"]
                                 [day8.re-frame/tracing "0.5.1"]
                                 [doo "0.1.8"]
                                 [figwheel-sidecar "0.5.16-SNAPSHOT"]
                                 [pjstadig/humane-test-output "0.8.3"]
                                 [prone "1.5.0"]
                                 [ring/ring-devel "1.6.3"]
                                 [ring/ring-mock "0.3.2"]
                                 [org.clojure/test.check "0.9.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.19.0"]
                                 [lein-doo "0.1.8"]
                                 [lein-figwheel "0.5.16-SNAPSHOT"]
                                 [org.clojure/clojurescript "1.10.238"]]
                  :doo {:build "test"}
                  :source-paths ["env/dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-server" "-Dconf=test-config.edn"]
                  :resource-paths ["env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})
