;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license.

(ns illya.orientdb.migration
  (:require
   [spyscope.core :as spy]
   [cats.core :as cats]
   [cats.monad.reader :as reader]
   [illya.orientdb.dsl :as ograph]))


(def ask-schema
  (cats/mlet
   [graph reader/ask]
   (cats/return (-> graph .getRawGraph .getMetadata .getSchema))))

(def ask-schema-version
  (cats/mlet
   [graph reader/ask
    infos (ograph/vertices :SchemaInfo)]
   (if-let [info (first infos)]
     (cats/return (.getProperty info "version"))
     (cats/return 0))))

(def init-schema-info!
  (cats/mlet
   [schema ask-schema]
   (if (not (.existsClass schema "SchemaInfo"))
     (let [v-class (.getClass schema "V")]
       (.createClass schema "SchemaInfo" v-class)))
   (cats/return ())))

(defn set-schema-version! [version]
  (cats/mlet
   [infos (ograph/vertices :SchemaInfo)]
   (if-let [info (first infos)]
     (ograph/set-properties! info {"version" version})
     (ograph/create-vertex! :SchemaInfo {"version" version}))))

(defn schema-outdated? [migration-config version]
  (< version (first (last migration-config))))

(defn migrate [migration-config version]
  (reduce cats/>>
          (map (fn [[migrated-version monad-instance]]
                 (cats/>> monad-instance
                          (set-schema-version! migrated-version)))
               (filter (fn [version-config]
                         (< version (first version-config)))
                       migration-config))))


(defn migrate-db [migration-confing]
  (ograph/do-unsafe
   (cats/mlet
    [_ init-schema-info!
     version ask-schema-version]
    (if (schema-outdated? migration-confing version)
      (migrate migration-confing version)
      (cats/return ())))))

(defn version [number]
  (fn [monad-instance]
    [number monad-instance]))

(defn config [& versions]
  (sort-by first versions))
