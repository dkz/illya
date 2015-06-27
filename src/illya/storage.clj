;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license.

(ns illya.storage
  (:require
   [illya.config :as configuration]
   [illya.orientdb.schema :as schema]
   [illya.orientdb.migration :as migration]
   [illya.orientdb.storage :as ostorage]
   [illya.orientdb.mapper :as mapper]
   [illya.orientdb.dsl :as ograph])
  (:import
   [com.tinkerpop.blueprints.impls.orient OrientGraph OrientGraphFactory]))

(defprotocol Storage
  (thread [this board-key number])
  (threads [this board-key])
  (post-thread [this board-key post])
  (post-reply [this board-key thread-number post]))

(def message-fetch-plan
  (list (mapper/property "title" :title)
        (mapper/property "number" :number)
        (mapper/property "contents" :contents)
        (mapper/property "created-date" :created-date)
        (mapper/property "is-hidden" :is-hidden)))

(def thread-overview-plan
  (list (mapper/property "message" :message message-fetch-plan)))

(def thread-details-plan
  (list (mapper/property "message" :message message-fetch-plan)
        (mapper/incoming-edges :PostedTo :replies message-fetch-plan)))

(defrecord OStorage []
  Storage
  (thread [_ board-key number]
          (ostorage/transaction-load-thread board-key number thread-details-plan))
  (threads [_ board-key]
    (ostorage/transaction-load-threads board-key thread-overview-plan))
  (post-thread [_ board-key post]
    (ostorage/transaction-post-thread board-key post))
  (post-reply [_ board-key number post]
    (ostorage/transaction-post-reply board-key number post)))

(def storage (atom :undefined))

(defn init-storage []
  (ograph/set-graph-factory!
   (doto (new OrientGraphFactory
              (configuration/property "orient-db.url")
              (configuration/property "orient-db.user")
              (configuration/property "orient-db.pass")) (.setupPool 1 10)))
  (comment (.close ograph/*orient-graph-factory*))
  (migration/migrate-db schema/configuration)
  (reset! storage (new OStorage)))

(defn get-storage []
  ;;@storage)
  (new OStorage)) ;; Required for code hot-deploying.

