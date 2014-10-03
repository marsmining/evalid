(ns evalid.core
  (:require [clojure.pprint :refer [pprint]]
            [nettyca.core :as nc]
            [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan timeout go go-loop alts!
                                        <! >! close!] :as async])
  (:gen-class))

(defmacro lwrite [c s n]
  `(do (log/info "smtp: writ:" ~s)
       (alts! [[~c (str ~s "\r\n")] (timeout ~n)])))

(defmacro lread [c n]
  `(let [[v# _#] (alts! [~c (timeout ~n)])]
     (log/info "smtp: read:" v#)))

(defn smtp-client [e]
  (fn [r w c]
    (go (let [

              _ (lread r 5000)
              _ (lwrite w "helo" 5000)
              _ (lread r 5000)
              _ (lwrite w "mail from: <foo@bar.com>" 5000)
              _ (lread r 5000)
              _ (lwrite w (format "rcpt to: %s" e) 5000)
              _ (lread r 15000)

              ]
          (log/info "smtp: done:" nil)
          (close! r) (close! w) (close! c)))))

(defn verify [{:keys [e f l u d m]}]
  (nc/start m 25 (smtp-client e) :client))

(defn grab []
  (read-string (slurp "out.edn")))

(defn -main [& args]
  (let [g (grab)]
    (println (count g))
    (verify (last g))))

(comment

  (def g (grab))
  (pprint g)
  (pprint (last g))

  (verify (last g))

  )
