(defproject evalid "0.1.0-SNAPSHOT"
  :description "Validate mailbox using SMTP"
  :dependencies [[org.clojure/clojure "1.7.0-alpha2"]
                 [nettyca "0.1.0-SNAPSHOT"]
                 [ch.qos.logback/logback-classic "1.1.2"]]
  :main evalid.core
  :aot [evalid.core])
