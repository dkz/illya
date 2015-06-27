;; Copyright (c) 2015, Dmitry Kozlov <kozlov.dmitry.a@gmail.com>
;; Code is published under BSD 2-clause license

(ns illya.config)

(def configuration-properties (new java.util.Properties))

(def configuration-defaults
  {"orient-db.url" "remote:localhost/Illya"
   "orient-db.user" "root"
   "orient-db.pass" "root"})

(defn file-exists? [file]
  (and (.exists file) (not (.isDirectory file))))

(defn load-configuration []
  (let [configuration-file (new java.io.File (System/getProperty "illya.config"))]
    (if (not (file-exists? configuration-file))
      (throw (new RuntimeException
                  (str "Configuration file does not exists: "
                       (.getAbsolutePath configuration-file))))
      (let [input (new java.io.FileInputStream configuration-file)]
        (try
          (.load configuration-properties input)
          (finally
            (.close input)))))))

(defn property [key]
  (.getProperty configuration-properties key
                (configuration-defaults key)))

