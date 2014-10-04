(ns evalid.core
  (:require [nettyca.core :as nc]
            [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan timeout go go-loop alts!
                                        <! >! close!] :as async])
  (:gen-class))

(defmacro lwrite [c s n]
  `(do (log/info "smtp: writ:" ~s)
       (alts! [[~c (str ~s "\r\n")] (timeout ~n)])))

(defmacro lread [c n]
  `(let [[v# _#] (alts! [~c (timeout ~n)])]
     (log/info "smtp: read:" v#)
     v#))

(defn smtp-client [e]
  (fn [r w c]
    (go (let [_ (lread r 5000)
              _ (lwrite w "helo" 5000)
              _ (lread r 5000)
              _ (lwrite w "mail from: <foo@bar.com>" 5000)
              _ (lread r 5000)
              _ (lwrite w (format "rcpt to: %s" e) 5000)
              rez (lread r 15000)]
          (log/info "smtp: done:" e "->" rez)
          (close! r) (close! w) (close! c)))))

(defn verify [{:keys [e f l u d m]}]
  (nc/start m 25 (smtp-client e) :client))

(def verify-and-wait
  (comp async/<!! :go-chan verify))

(defn grab []
  (read-string (slurp "out.edn")))

(defn -main [& args]
  (let [xs (grab)]
    (log/info "working on records:" (count xs))
    (doseq [x xs]
      (verify-and-wait x))))

(comment

  (def gs (grab))
  (last gs)

  (verify (last gs))
  (verify-and-wait (last gs))

  )
