;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license.

(ns illya.orientdb.schema
  (:require
   [cats.core :as cats]
   [illya.orientdb.dsl :as ograph]
   [illya.orientdb.migration :as migration]))

(def configuration
  (migration/config
   ((migration/version 1)
    (cats/mlet
     [message
      (ograph/create-vertex-type!
       :Message
       {"created-date" :date
        "is-hidden" :boolean
        "number" :long
        "title" :string
        "contents" :string})
      message-board
      (ograph/create-vertex-type!
       :MessageBoard
       {"name" :string
        "message-counter" :long})
      message-thread
      (ograph/create-vertex-type!
       :MessageThread
       {"message" :link
        "board" :link
        "updated-date" :date})]
     (cats/>>
      (ograph/create-edge-type! :PostedOn {})
      (ograph/create-edge-type! :PostedTo {})
      (ograph/set-outgoing-edge! message :PostedTo)
      (ograph/set-incoming-edge! message-thread :PostedTo)
      (ograph/set-outgoing-edge! message-thread :PostedOn)
      (ograph/set-incoming-edge! message-board :PostedOn)
      (ograph/create-vertex!
       :MessageBoard
       {"name" "a"
        "message-counter" 0})
      (ograph/create-vertex!
       :MessageBoard
       {"name" "c"
        "message-counter" 0}))))))

