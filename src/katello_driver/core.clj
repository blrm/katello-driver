(ns katello-driver.core
  (:refer-clojure :exclude [fn])
  (:require
   [clj-webdriver.remote.server :as rs]
   [clj-webdriver.taxi :as taxi]
   [clj-http.client :as httpclient]
   [clojure.data.json :as json]
   [test.tree.script :refer :all]
   [test.tree]
   [test.tree.jenkins :as jenkins]
   [test.assert :as assert]
   [serializable.fn :refer :all]
   [test.tree.watcher :as watch]
   [sauce-api.jobs :as job]
   [sauce-api.utils :as util]
   ))

(def *sauce-cred* "username:sauce-api-token@ondemand.saucelabs.com")
(def *product-url* "servername/katello")
(def sauce-user "username")
(def api-token "token")

(defn timestamp [] (.. System (currentTimeMillis)))
;; Locators

(def log-out "div[ng-controller=\"MenuController\"] a[href*=\"logout\"]")
(def admin-menu  {:xpath "//span[.=\"Administer\"]"})
(def org-menu-item  {:tag :a, :class "dropdown-item-link", :href "/katello/organizations"})
(def new-org-link  {:tag :a :id "new" :text "+ New Organization"})
(def notif-org-create "div[class~=\"jnotify-notification-success\"] ul[class=\"organizations___create\"]")

;; Selenium Init Functions

(defn init-grid
  "Initializes the remote saucelabs grid"
  ([] (init-grid 80 *sauce-cred* {"browserName" "chrome"
                                  "platform" "LINUX"}))
  ([port cred spec]
      (let [[this-server this-driver]
            (rs/new-remote-session {:port port
                                    :host cred
                                    :existing true}
                                   {:capabilities spec})]
        (taxi/set-driver! this-driver)
        (taxi/set-finder! taxi/css-finder)
        (taxi/implicit-wait 3000))))

(defn init-browser
  "Initializes a local browser."
  []
  (taxi/set-driver! {:browser :firefox} *product-url*))

;; Katello REST API functions

(defn get-katello-version
  "Returns the katello version under test from the katello API"
  ([] (get-katello-version *product-url* "admin" "admin"))
  ([url user pass]
      (-> (httpclient/get (str url "api/version") {:basic-auth [user pass]
                                                   :insecure? true})
          (:body)
          (json/read-str)
          (get "version"))))

(defn get-katello-orgs
  "returns a list of all the available katello organizations (from the katello api)"
  ([] (get-katello-orgs *product-url* "admin" "admin"))
  ([url user pass]
     (-> (httpclient/get (str url "api/organizations") {:basic-auth [user pass]
                                                        :insecure? true})
         (:body)
         (json/read-str))))

;; Sauce convenience functions

(defn get-running-job
  "Returns the id of the most recent job created in sauce. This will need to be re-written to work with running jobs in parallel."
  ([] (get-running-job sauce-user api-token))
  ([username api-token]
     (job/get-all-ids username api-token {:limit 1})))

(defn on-pass
  "create a watcher that will call f when a test passes."
  [f]
  (watch/watch-on-pred (fn [test report]
                   (let [r (:report report)]
                     (and r (-> r :result (= :pass)))))
                 f))

(defn log-in
  "logs into katello"
  []
    (taxi/to *product-url*)
    (taxi/input-text "#username" "admin")
    (taxi/input-text "#password-input" "admin")
    (taxi/submit "#username"))

(defgroup demo-tests
  :test-setup init-grid
  :test-teardown taxi/quit

  (deftest "login test"
    (taxi/to *product-url*)
    (taxi/input-text "#username" "admin")
    (taxi/input-text "#password-input" "admin")
    (taxi/submit "#username")
    (assert (taxi/exists? log-out)))

  (deftest "create org"
    (log-in)
    (taxi/click (taxi/find-element admin-menu))
    (taxi/click (taxi/find-element org-menu-item))
    (taxi/click "#new")
    (taxi/input-text "#organization_name" (str "testorg-" (timestamp)))
    (taxi/input-text "#organization_description" "This is a demonstration org")
    (taxi/submit "#organization_name")
    (assert (taxi/present? notif-org-create))))
 
(defn run-tests []
  (test.tree/run-suite demo-tests {:watchers {:onpass (on-pass
                                                       (fn [t _]
                                                         (if (complement (contains? t :configuration))
                                                           (let [s-id (get (first (get-running-job)) "id")]
                                                             (job/update-id sauce-user api-token s-id {:name (:name t)
                                                                                                       :tags [(get-katello-version)]
                                                                                                       :passed true})))))
                                              :onfail (watch/on-fail
                                                       (fn [t e]
                                                         (if (complement (contains? t :configuration))
                                                           (let [s-id (get (first (get-running-job)) "id")]
                                                             (job/update-id sauce-user api-token s-id {:name (:name t)
                                                                                                       :tags [(get-katello-version)]
                                                                                                       :passed false
                                                                                                       :custom-data {"throwable" (pr-str (:throwable (:error (:report e))))
                                                                                                                     "stacktrace" (-> e :report :error :stack-trace java.util.Arrays/toString)}})))))}}))






































