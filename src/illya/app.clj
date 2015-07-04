;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license

(ns illya.app
  (:gen-class)
  (:use compojure.core)
  (:require
   [illya.views :as views]
   [illya.storage :as storage]
   [illya.config :as configuration]
   [illya.sessions :as sessions]
   [ring.adapter.jetty-async :refer [run-jetty-async]]
   [compojure.route :as routes]
   [compojure.handler :as handler]
   [ring.util.response :as response]
   [clojure.string :as string]
   [spyscope.core :as spy]
   [hiccup.core :as hiccup :refer [h]]))

(defn control-present? [{params :params}]
  (if-let [control-key (:control params)]
    (= control-key (configuration/property "control.key"))
    false))

(defn check-credentials [{params :params}]
  (and (= (:user params) (configuration/property "control.user"))
       (= (:pass params) (configuration/property "control.pass"))))

(defn add-headers [response-body & {:keys [status] :or {status 200}}]
  {:status 200
   :headers {"Content-Type" "text/html;charset=utf-8"}
   :body response-body})

(defn add-cookies [cookies response-body]
  (assoc response-body :cookies cookies))

(defn echo-handler [request]
  (add-headers (str request)))

(defn root-handler [request]
  (add-headers (views/render (views/index-template))))

(defn authorization-handler [request]
  (fn [request]
    (let [session-id (java.util.UUID/randomUUID)]
      (add-cookies {"session-id" {:value session-id :max-age 3600}}
                   (do
                     (if (and (control-present? request) (check-credentials request))
                       (let [ip (:remote-addr request)]
                         (sessions/add-session {:id session-id :ip ip})))
                     (root-handler request))))))

(defn ask-authorization? [{cookies :cookies :as request}]
  (nil? (cookies "session-id")))

(defn has-permissions? [{cookies :cookies :as request}]
  (if-let [session-id-string (:value (cookies "session-id"))]
    (try
      (let [session-id (java.util.UUID/fromString session-id-string)]
        (if-let [session (sessions/get-session session-id)]
          (= (:remote-addr request) (:ip session))
          false))
      (catch IllegalArgumentException e
        false))
    false))

(defn i404-handler [request]
  (add-headers (views/render (views/not-found-template))
               :status 404))

(defn thread-dispatching-middleware [handler]
  (fn [request]
    (let [{params :params} request]
      (try
        (if-let [thread-param #spy/t (:thread params)]
          (let [tn (Integer/parseInt thread-param)]
            (handler (assoc-in request [:params :thread] tn)))
          (i404-handler request))
        (catch NumberFormatException e
          (i404-handler request))))))

(defn board-dispatching-middleware [handler]
  (fn [request]
    (let [{params :params} request
          board-param (:board params)]
      (if-let [bk ({"a" :a "c" :c} board-param)]
        (handler (assoc-in request [:params :board] bk))
        (i404-handler request)))))


(defn control-middleware [handler]
  (fn [{params :params :as request}]
    (if (control-present? request)
      (if (ask-authorization? request)
        (add-headers (views/render (views/authorization-template {:key (:control params)})))
        (if (has-permissions? request)
          (handler (assoc request :control {:permitted true :key (:control params)}))
          (handler request)))
      (handler request))))

(defn board-request-dispatcher [request board]
  (add-headers
   (views/render
    (views/board-template
     (storage/threads board)
     {:control (:control request)
      :board (name board)
      :post-url (str "/board/" (name board) "/thread/new")
      :post-header "[start a thread]"}))))

(def board-handler
  (control-middleware
   (board-dispatching-middleware
    (fn [request]
      (let [{params :params} request
            board (:board params)]
        (board-request-dispatcher request board))))))

(defn thread-request-dispatcher [request board thread]
  (if-let [thread-model (storage/thread board thread)]
    (add-headers
     (views/render
      (views/thread-template
       thread-model
       {:control (:control request)
        :thread thread
        :board board
        :post-url (str "/board/" (name board) "/thread/" thread "/new")
        :post-header "[post a reply]"})))
    (i404-handler request)))

(def thread-handler
  (control-middleware
   (board-dispatching-middleware
    (thread-dispatching-middleware
     (fn [request]
       (let [{params :params} request
             thread (:thread params)
             board (:board params)]
         (thread-request-dispatcher request board thread)))))))

(defn text->html [text]
  (-> (hiccup/h text)
      (string/replace #"(?im)^(.+)\s*$" "$1<br/>")
      (string/replace #"\s" "&nbsp;")))

(def new-thread-handler
  (board-dispatching-middleware
   (fn [request]
     (let [{params :params} request
           board (:board params)
           title (:title params)
           text (:text params)]
       (storage/post-thread board {:title (hiccup/h title) :contents (text->html text)})
       (response/redirect-after-post (str "/board/" (name board)))))))

(def new-post-handler
  (board-dispatching-middleware
   (thread-dispatching-middleware
    (fn [request]
      (let [{params :params} request
            board (:board params)
            thread (:thread params)
            title (:title params)
            text (:text params)]
        (storage/post-reply board thread {:title (hiccup/h title) :contents (text->html text)})
        (response/redirect-after-post (str "/board/" (name board) "/thread/" thread)))))))

(def hide-post-handler
  (control-middleware
   (board-dispatching-middleware
    (fn [{params :params :as request}]
      (try
        (if-let [number-param (:number params)]
          (let [number (Integer/parseInt number-param)
                board (:board params)
                redirect-param (:redirect params)
                control (:control request)]
            (if (:permitted control)
              (storage/hide-post board number))
            (if redirect-param
              (let [redirect (Integer/parseInt redirect-param)]
                (thread-request-dispatcher request board redirect))
              (board-request-dispatcher request board)))
          (i404-handler request))
        (catch NumberFormatException e
          (i404-handler request)))))))

(def faq-handler
  (fn [request]
    (add-headers
     (views/render
      (views/faq-template)))))

(defroutes app-routes
  (GET "/" [] root-handler)
  (GET "/faq" [] faq-handler)
  (GET "/board/:board" [board] board-handler)
  (GET "/board/:board/:page" [board page] board-handler)
  (GET "/board/:board/thread/:thread" [board thread] thread-handler)
  (GET "/board/:board/control/hide/:number" [board number] hide-post-handler)
  (POST "/auth" [] authorization-handler)
  (POST "/board/:board/thread/new" [board] new-thread-handler)
  (POST "/board/:board/thread/:thread/new" [board thread] new-post-handler)
  (routes/resources "/static")
  (routes/not-found i404-handler))

(def app-handler
   (handler/site app-routes))

(defn app-init []
  (sessions/init-session-store)
  (configuration/load-configuration)
  (storage/init-storage))

(defn -main [& args]
  (app-init)
  (run-jetty-async #'app-handler {:join? true :port 3000}))

(comment
  (def jetty-instance (run-jetty-async #'app-handler {:join? false :port 3000}))
  (.stop jetty-instance))
