(defproject katello-driver "0.1.0-SNAPSHOT"
  :description "A sauce-driven katello automation demo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.4"]
                 [clj-webdriver "0.6.0"]
                 [test.tree.jenkins "0.10.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
                 [test.tree "0.10.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
                 [slingshot "0.10.3"]
                 [com.redhat.qe/test.assert "1.0.0-SNAPSHOT"]
                 [org.clojars.firesofmay/sauce-api "0.1.0-SNAPSHOT"]
                 [com.saucelabs/sauce_testng "1.0.13"]]
  :repositories [["saucelabs-repository" {:url "https://repository-saucelabs.forge.cloudbees.com/release"
                                          :snapshots true
                                          :releases true}]])
