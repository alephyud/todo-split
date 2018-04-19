(ns todo-split.core
  (:require [todo-split.handler :as handler]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [todo-split.config :refer [env]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [refactor-nrepl.middleware]
            [mount.core :as mount]
            [schema.core :as s])
  (:gen-class))

(def nrepl-handler
  "A development NREPL handler with standard CIDER middleware and
  refactor-nrepl middleware."
  (apply clojure.tools.nrepl.server/default-handler
         (cons #'refactor-nrepl.middleware/wrap-refactor
               (map resolve cider.nrepl/cider-middleware))))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
    (-> env
        (assoc  :handler #'handler/app)
        (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime)))))
        (update :port #(or (-> env :options :port) %))))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when-let [nrepl-port (env :nrepl-port)]
    (repl/start {:port nrepl-port :handler nrepl-handler}))
  :stop
  (when repl-server
    (repl/stop repl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& args]
  (start-app args))
