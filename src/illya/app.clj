;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license

(ns illya.app
  (:gen-class)
  (:use compojure.core)
  (:require
   [illya.views :as views]
   [illya.storage :as storage]
   [ring.adapter.jetty-async :refer [run-jetty-async]]
   [compojure.route :as routes]
   [compojure.handler :as handler]
   [ring.util.response :as response]
   [clojure.string :as string]
   [spyscope.core :as spy]
   [hiccup.core :as hiccup :refer [h]]))

(defn add-headers [response-body & {:keys [status] :or {status 200}}]
  {:status 200
   :headers {"Content-Type" "text/html;charset=utf-8"}
   :body response-body})

(defn echo-handler [request]
  (add-headers (str request)))

(defn root-handler [request]
  (add-headers (views/render (views/index-template))))

(defn i404-handler [request]
  (add-headers (views/render (views/not-found-template))
               :status 404))

(defn thread-dispatching-middleware [handler]
  (fn [request]
    (let [{params :params} request]
      (try
        (if-let [thread-param (:thread params)]
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

(def board-handler
  (board-dispatching-middleware
   (fn [request]
     (let [{params :params} request
           board (:board params)]
       (add-headers
        (views/render
         (views/board-template
          (storage/threads board)
          {:board (name board)
           :post-url (str "/board/" (name board) "/thread/new")
           :post-header "[start a thread]"})))))))

(def thread-handler
  (board-dispatching-middleware
   (thread-dispatching-middleware
    (fn [request]
      (let [{params :params} request
            thread (:thread params)
            board (:board params)]
        (if-let [thread-model (storage/thread board thread)]
          (add-headers
           (views/render
            (views/thread-template
             thread-model
             {:post-url (str "/board/" (name board) "/thread/" thread "/new")
              :post-header "[post a reply]"})))
          (i404-handler request)))))))

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
  (POST "/board/:board/thread/new" [board] new-thread-handler)
  (POST "/board/:board/thread/:thread/new" [board thread] new-post-handler)
  (routes/resources "/static")
  (routes/not-found i404-handler))

(def app-handler
   (handler/site app-routes))

(defn app-init []
  (storage/init-storage))

(defn -main [& args]
  (app-init)
  (run-jetty-async #'app-handler {:join? true :port 3000}))

(comment
  (def jetty-instance (run-jetty-async #'app-handler {:join? false :port 3000}))
  (.stop jetty-instance))
