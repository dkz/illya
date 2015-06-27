;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license.

(ns illya.orientdb.storage
  (:require
   [spyscope.core :as spy]
   [cats.core :as cats]
   [illya.orientdb.dsl :as ograph]
   [illya.orientdb.mapper :as mapper]))

(defn m-board-vertex [board-key]
  (cats/mlet
   [boards (ograph/query! "select from MessageBoard where name = :name"
                          {"name" (name board-key)})]
   (cats/return (first boards))))

(defn transaction-post-thread [board-key {contents :contents title :title}]
  (let [created-date (new java.util.Date)]
    (ograph/do-transaction
     (cats/mlet
      [board (m-board-vertex board-key)
       counter (ograph/property board "message-counter")
       message (ograph/create-vertex!
                :Message
                {"created-date" created-date
                 "is-hidden" false
                 "number" (+ 1 counter)
                 "title" title
                 "contents" contents})
       thread (ograph/create-vertex!
               :MessageThread
               {"message" message
                "board" board
                "updated-date" created-date})]
      (cats/>>
       (ograph/set-properties! board {"message-counter" (+ 1 counter)})
       (ograph/create-edge! :PostedOn thread board)
       (cats/return ()))))))

(defn m-thread-vertex [board-key thread-number]
  (cats/mlet
   [threads (ograph/query!
             "select from MessageThread where board.name = :name and message.number = :number and message.is-hidden = false"
             {"name" (name board-key)
              "number" thread-number})]
   (cats/return (first threads))))

(defn transaction-post-reply [board-key thread-number {contents :contents title :title}]
  (let [created-date (new java.util.Date)]
    (ograph/do-transaction
     (cats/mlet
      [board (m-board-vertex board-key)
       counter (ograph/property board "message-counter")
       thread (m-thread-vertex board-key thread-number)
       message (ograph/create-vertex!
                :Message
                {"created-date" created-date
                 "is-hidden" false
                 "number" (+ 1 counter)
                 "title" title
                 "contents" contents})]
      (cats/>>
       (ograph/set-properties! board {"message-counter" (+ 1 counter)})
       (ograph/set-properties! thread {"updated-date" created-date})
       (ograph/create-edge! :PostedTo message thread)
       (cats/return ()))))))

(defn filter-hidden-messages [thread]
  (assoc thread :replies (doall (filter (comp not :is-hidden) (:replies thread)))))

(defn transaction-load-threads [board-key fetch-plan]
  (ograph/do-transaction
   (cats/mlet
    [threads (ograph/query!
              "select from MessageThread where board.name = :name and message.is-hidden = false order by updated-date desc limit 20"
              {"name" (name board-key)})]
    (if (empty? threads)
      (cats/return [])
      (cats/sequence
       (doall
        (for [thread threads]
          (mapper/fetch thread fetch-plan))))))))

(defn transaction-load-thread [board-key thread-number fetch-plan]
  (ograph/do-transaction
   (cats/mlet
    [thread (m-thread-vertex board-key thread-number)]
    (if (nil? thread)
      (cats/return ())
      (cats/fmap filter-hidden-messages
                 (mapper/fetch thread fetch-plan))))))

