;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license.

(ns illya.orientdb.mapper
  (:require
   [spyscope.core :as spy]
   [cats.core :as cats]
   [cats.monad.reader :as reader]
   [illya.orientdb.dsl :as ograph])
  (:import
   [com.tinkerpop.blueprints Direction]
   [com.orientechnologies.orient.core.record ORecord]))

;; Monadic function
(defn fetch [object fetch-plan]
  (if (empty? fetch-plan)
    (cats/return {})
    (cats/mlet
     [graph reader/ask
      :let [entity (transient {})]]
     (cats/>>
      (cats/sequence
       (for [fm fetch-plan]
         (fm object entity)))
      (cats/bind
       (cats/return #(persistent! entity))
       (fn [lazy]
         (cats/return (lazy))))))))

(defn property
  ([property-name property-key]
   (fn [object transient-entity]
     (assoc! transient-entity property-key (.getProperty object property-name))
     (cats/return ())))
  ([property-name property-key fetch-plan]
   (fn [object transient-entity]
     (cats/mlet
      [nested (fetch (.getProperty object property-name) fetch-plan)]
      (assoc! transient-entity property-key nested)
      (cats/return ())))))


(defn incoming-edges [edge-class-key property-key fetch-plan]
  (fn [object transient-entity]
    (let [edges (.getEdges object Direction/IN (into-array String [(name edge-class-key)]))]
      (cats/bind
       (if (empty? edges)
         (cats/return [])
         (cats/sequence
          (doall (for [edge edges]
                   (fetch (.getVertex edge Direction/OUT) fetch-plan)))))
       (fn [fetched]
         (assoc! transient-entity property-key fetched)
         (cats/return ()))))))

(defn outgoing-edges [edge-class-key property-key fetch-plan]
  (fn [object transient-entity]
    (let [edges (.getEdges object Direction/OUT (into-array String [(name edge-class-key)]))]
      (cats/bind
       (if (empty? edges)
         (cats/return [])
         (cats/sequence
          (doall (for [edge edges]
                   (fetch (.getVertex edge Direction/IN) fetch-plan)))))
       (fn [fetched]
         (assoc! transient-entity property-key fetched)
         (cats/return ()))))))

(comment
  (ograph/do-transaction
   (cats/mlet
    [threads (ograph/vertices :MessageThread)
     :let [thread (first threads)]]
    (fetch thread
           (list (incoming-edges :PostedTo :replies
                                 (property "title" :title)
                                 (property "contents" :contents))
                 (property "message" :message
                           (property "title" :title)
                           (property "contents" :contents))))))
  )
