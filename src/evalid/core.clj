(ns evalid.core
  (:require [nettyca.core :as nc]
            [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan timeout go go-loop alts!
                                        <! >! close!] :as async])
  (:gen-class))

(defmacro lwrite [c s n]
  `(do (log/info "smtp: writ:" ~s)
       (let [[v# c#] (alts! [[~c (str ~s "\r\n")] (timeout ~n)])]
         (identical? c# ~c))))

(defmacro lread [c n]
  `(let [[v# c#] (alts! [~c (timeout ~n)])]
     (log/info "smtp: read:" v#)
     v#))

(defn rvalid? [s] (and s (.startsWith s "2")))

(defn smtp-client [rcpt domain]
  (fn [r w c]
    (go (let [r0 (lread r 5000)
              w0 (when (rvalid? r0)
                   (lwrite w (format "helo %s" domain) 5000))
              r1 (when w0 (lread r 5000))
              w1 (when (rvalid? r1)
                   (lwrite w "mail from: <foo@bar.com>" 5000))
              r2 (when w1 (lread r 5000))
              w2 (when (rvalid? r2)
                   (lwrite w (format "rcpt to: <%s>" rcpt) 5000))
              r3 (when w2 (lread r 15000))
              rz (first (drop-while nil? [r3 r2 r1 r0]))]
          (log/info "smtp: done:" rcpt "->" rz)
          (close! r) (close! w) (close! c)))))

(defn verify [{:keys [e f l u d m]} domain]
  (nc/start m 25 (smtp-client e domain) :client))

(def verify-and-wait
  (comp async/<!! :go-chan verify))

(defn grab []
  (read-string (slurp "out.edn")))

(defn -main [& args]
  (let [xs (grab)]
    (log/info "working on records:" (count xs))
    (doseq [x xs]
      (verify-and-wait x (first args)))))

(comment

  (def gs (grab))
  (last gs)

  (verify (last gs) "bar.com")
  (verify-and-wait (last gs) "bar.com")

  )
