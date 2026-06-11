(ns app.core)

(defn ^:async extract-structured-data
  [^js pdfjs pdf-data]
  (let [^js pdf      (await (.-promise (.getDocument pdfjs #js {:data pdf-data})))
        ^js metadata (await (.getMetadata pdf))]
    #js {:title   (.-Title (.-info metadata))
         :author  (.-Author (.-info metadata))
         :subject (.-Subject (.-info metadata))
         :pages   (.-numPages pdf)}))

(def pdf-data
  (js/atob (str
            "JVBERi0xLjcKCjEgMCBvYmogICUgZW50cnkgcG9pbnQKPDwKICAvVHlwZSAvQ2F0YWxvZwog"
            "IC9QYWdlcyAyIDAgUgo+PgplbmRvYmoKCjIgMCBvYmoKPDwKICAvVHlwZSAvUGFnZXMKICAv"
            "TWVkaWFCb3ggWyAwIDAgMjAwIDIwMCBdCiAgL0NvdW50IDEKICAvS2lkcyBbIDMgMCBSIF0K"
            "Pj4KZW5kb2JqCgozIDAgb2JqCjw8CiAgL1R5cGUgL1BhZ2UKICAvUGFyZW50IDIgMCBSCiAg"
            "L1Jlc291cmNlcyA8PAogICAgL0ZvbnQgPDwKICAgICAgL0YxIDQgMCBSIAogICAgPj4KICA+"
            "PgogIC9Db250ZW50cyA1IDAgUgo+PgplbmRvYmoKCjQgMCBvYmoKPDwKICAvVHlwZSAvRm9u"
            "dAogIC9TdWJ0eXBlIC9UeXBlMQogIC9CYXNlRm9udCAvVGltZXMtUm9tYW4KPj4KZW5kb2Jq"
            "Cgo1IDAgb2JqICAlIHBhZ2UgY29udGVudAo8PAogIC9MZW5ndGggNDQKPj4Kc3RyZWFtCkJU"
            "CjcwIDUwIFRECi9GMSAxMiBUZgooSGVsbG8sIHdvcmxkISkgVGoKRVQKZW5kc3RyZWFtCmVu"
            "ZG9iagoKeHJlZgowIDYKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDEwIDAwMDAwIG4g"
            "CjAwMDAwMDAwNzkgMDAwMDAgbiAKMDAwMDAwMDE3MyAwMDAwMCBuIAowMDAwMDAwMzAxIDAw"
            "MDAwIG4gCjAwMDAwMDAzODAgMDAwMDAgbiAKdHJhaWxlcgo8PAogIC9TaXplIDYKICAvUm9v"
            "dCAxIDAgUgo+PgpzdGFydHhyZWYKNDkyCiUlRU9G")))

(comment
  (.catch
   (.then (extract-structured-data (.-pdfjsLib js/globalThis) pdf-data)
          #(js/console.log %))
   #(js/console.error %))
  )
  
(defn init []
  (if-not js/Worker
    (js/console.error "Web Workers not supported.")
    (do
      (set! (.-workerSrc (.-GlobalWorkerOptions (.-pdfjsLib js/globalThis)))
            "https://cdn.jsdelivr.net/npm/pdfjs-dist@6.0.227/build/pdf.worker.mjs")
      (.catch
       (.then (extract-structured-data (.-pdfjsLib js/globalThis) pdf-data)
              #(js/console.log %))
       #(js/console.error %)))))