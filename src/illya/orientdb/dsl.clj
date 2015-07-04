;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license.

(ns illya.orientdb.dsl
  (:require
   [cats.core :as cats]
   [cats.monad.reader :as reader]
   [spyscope.core :as spy])
  (:import
   [com.tinkerpop.blueprints Vertex Direction Parameter]
   [com.orientechnologies.orient.core.sql OCommandSQL]
   [com.orientechnologies.orient.core.metadata.schema OType]
   [com.orientechnologies.orient.core.sql.query OSQLAsynchQuery]
   [com.orientechnologies.orient.core.command OCommandResultListener]))

(def ^:dynamic *orient-graph-factory*)

(defn set-graph-factory! [graph-factory]
  (alter-var-root #'*orient-graph-factory*
                  (fn [_] graph-factory)))

(defn do-transaction [monad-instance]
  (let [graph (.getTx *orient-graph-factory*)]
    (try
      (cats/with-monad reader/reader-monad
        (reader/run-reader monad-instance graph))
      (catch Exception e
        (.rollback graph)
        (throw e))
      (finally
        (.shutdown graph)))))

(defn do-unsafe [monad-instance]
  (let [graph (.getNoTx *orient-graph-factory*)]
    (try
      (cats/with-monad reader/reader-monad
        (reader/run-reader monad-instance graph))
      (finally
        (.shutdown graph)))))

(defn set-properties! [object properties-map]
  (doseq [[pn pv] properties-map]
    (.setProperty object pn pv))
  (cats/return object))

(defn create-vertex! [class-name-keyword properties-map]
  (cats/mlet
   [graph reader/ask
    :let [vertex (.addVertex graph (str "class:" (name class-name-keyword)))]]
   (set-properties! vertex properties-map)))

(defn create-edge!
  ([class-name-keyword vertex-out vertex-in properties-map]
   (cats/mlet
    [graph reader/ask
     :let [edge (.addEdge graph nil vertex-out vertex-in (name class-name-keyword))]]
    (set-properties! edge properties-map)))
  ([class-name-keyword vertex-out vertex-in]
   (create-edge! class-name-keyword vertex-out vertex-in {})))

;; Returns lazy sequence which raises an exception if unfolded outside transaction.
(defn query! [sql-query named-parameters]
  (cats/mlet
   [graph reader/ask]
   (cats/return
    (seq (.execute
          (.command graph (new OCommandSQL sql-query))
          (to-array [named-parameters]))))))

(defn vertex [rid]
  (cats/mlet
   [graph reader/ask]
   (cats/return (.getVertex graph rid))))

(defn vertices
  ([class-name-keyword]
   (cats/mlet
    [graph reader/ask]
    (cats/return (.getVerticesOfClass graph (name class-name-keyword))))))

(defn incoming-edges [vertex class-name-keyword]
  (cats/return
   (seq (.getEdges vertex Direction/IN (into-array String [(name class-name-keyword)])))))

(defn outgoing-edges [vertex class-name-keyword]
  (cats/return
   (seq (.getEdges vertex Direction/OUT (into-array String [(name class-name-keyword)])))))

(defn property [object name]
  (cats/return
   (.getProperty object name)))

(def types
  {:string OType/STRING
   :long OType/LONG
   :date OType/DATETIME
   :boolean OType/BOOLEAN
   :link OType/LINK})

(defn create-properties! [type schema-definition]
  (doseq [[pn pt] schema-definition]
    (.createProperty type pn (types pt)))
  (cats/return type))

(defn drop-property! [type property-name]
  (.dropProperty type property-name)
  (cats/return ()))

(defn create-edge-type! [class-name-keyword schema-definition]
  (cats/mlet
   [graph reader/ask
    :let [edge-type (.createEdgeType graph (name class-name-keyword))]]
   (create-properties! edge-type schema-definition)))

(defn create-vertex-type! [class-name-keyword schema-definition]
  (cats/mlet
   [graph reader/ask
    :let [vertex-type (.createVertexType graph (name class-name-keyword))]]
   (create-properties! vertex-type schema-definition)))

(defn create-index! [property-name parameters-map]
  (cats/mlet
   [graph reader/ask]
   (.createKeyIndex graph property-name Vertex
                    (into-array Parameter
                                (for [[pn pv] parameters-map] (new Parameter pn pv))))))

(defn set-incoming-edge! [vertex-type edge-name-keyword]
  (cats/mlet
   [graph reader/ask]
   (.createEdgeProperty vertex-type Direction/IN (name edge-name-keyword))
   (cats/return vertex-type)))

(defn set-outgoing-edge! [vertex-type edge-name-keyword]
  (cats/mlet
   [graph reader/ask]
   (.createEdgeProperty vertex-type Direction/OUT (name edge-name-keyword))
   (cats/return vertex-type)))

(defn edge-type [class-name-keyword]
  (cats/mlet
   [graph reader/ask]
   (cats/return (.getEdgeType graph (name class-name-keyword)))))

(defn vertex-type [class-name-keyword]
  (cats/mlet
   [graph reader/ask]
   (cats/return (.getVertexType graph (name class-name-keyword)))))

