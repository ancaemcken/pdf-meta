(ns app.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(defn ^:async extract-structured-data
  [^js pdfjs pdf-data]
  (let [^js pdf      (await (.-promise (.getDocument pdfjs #js {:data pdf-data})))
        ^js metadata (await (.getMetadata pdf))]
    {:title   (.-Title (.-info metadata))
     :author  (.-Author (.-info metadata))
     :subject (.-Subject (.-info metadata))
     :custom (js->clj (.-Custom (.-info metadata))) ; XMP metadata
     :pages   (.-numPages pdf)}))

(defn read-file-using
  "Read file using `process-fn`."
  [file process-fn]
  (let [file-reader (js/FileReader.)]
    (set! (.-onload file-reader) #(process-fn (-> % .-target .-result js/Uint8Array.)))
    (.readAsArrayBuffer file-reader file)))

(defonce pdf-metadata
  (r/atom {}))

(defn description-list-elements
  [elements]
  [:div {:class "border-t border-gray-100 dark:border-white/5"}
   [:dl {:class "divide-y divide-gray-100 dark:divide-white/5"}
    (for [[k v] elements]
      ^{:key k}
      [:div
       {:class "px-4 py-6 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6"}
       [:dt {:class "text-sm font-medium text-gray-900 dark:text-gray-100"} k]
       [:dd {:class (str "mt-1 text-sm/6 sm:col-span-2 sm:mt-0"
                         (if v
                           " text-gray-700 dark:text-gray-300"
                           " text-gray-400 dark:text-gray-500"))}
        (or v "—")]])]])

(defn description-list
  "https://tailwindcss.com/plus/ui-blocks/application-ui/data-display/description-lists"
  [list-heading list-description list-elements & [empty-description]]
  [:div
   {:class "overflow-hidden bg-white shadow-sm sm:rounded-lg dark:bg-gray-800/50 dark:shadow-none dark:inset-ring dark:inset-ring-white/10"}
   [:div {:class "px-4 py-6 sm:px-6"}
    [:h2 {:class "text-base/7 font-semibold text-gray-900 dark:text-gray-300"}
     list-heading]
    [:p {:class "mt-1 max-w-2xl text-sm/6 text-gray-500 dark:text-gray-300"}
     list-description]]
   (if (empty? list-elements)
     [:div {:class "px-4 py-6 sm:px-6 text-sm font-medium text-gray-900 dark:text-gray-100 italic"}
      [:p
       [:svg
        {:stroke "currentColor",
         :fill "none",
         :stroke-linejoin "round",
         :width "24",
         :xmlns "http://www.w3.org/2000/svg",
         :stroke-linecap "round",
         :stroke-width "2",
         :class
         "inline-block mr-3",
         :viewBox "0 0 24 24",
         :height "24"}
        [:path
         {:d
          "M2.992 16.342a2 2 0 0 1 .094 1.167l-1.065 3.29a1 1 0 0 0 1.236 1.168l3.413-.998a2 2 0 0 1 1.099.092 10 10 0 1 0-4.777-4.719"}]
        [:path {:d "M12 8v4"}]
        [:path {:d "M12 16h.01"}]]
       (or empty-description "The list is empty.")]]
     [description-list-elements list-elements])])

; https://readymadeui.com/tailwind/component/file-upload-container
(defn file-upload
  [file-selected-fn]
  [:label
   {:for "uploadfile"
    :class "bg-white text-slate-600 font-semibold text-sm rounded-md h-48 flex flex-col items-center justify-center cursor-pointer border-2 border-slate-300 border-dashed focus-within:ring-2 focus-within:border-transparent focus-within:ring-blue-500 dark:bg-neutral-900 dark:text-slate-300 dark:border-neutral-700"}
   [:svg
    {:xmlns "http://www.w3.org/2000/svg"
     :class "size-10 mb-4 fill-gray-400"
     :viewBox "0 0 32 32"
     :aria-hidden "true"}
    [:path
     {:d "M23.75 11.044a7.99 7.99 0 0 0-15.5-.009A8 8 0 0 0 9 27h3a1 1 0 0 0 0-2H9a6 6 0 0 1-.035-12 1.038 1.038 0 0 0 1.1-.854 5.991 5.991 0 0 1 11.862 0A1.08 1.08 0 0 0 23 13a6 6 0 0 1 0 12h-3a1 1 0 0 0 0 2h3a8 8 0 0 0 .75-15.956z"}]
    [:path
     {:d "M20.293 19.707a1 1 0 0 0 1.414-1.414l-5-5a1 1 0 0 0-1.414 0l-5 5a1 1 0 0 0 1.414 1.414L15 16.414V29a1 1 0 0 0 2 0V16.414z"}]]
   "Load PDF file"
   [:input {:type "file" :id "uploadfile" :class "sr-only"
            :accept "application/pdf"
            :on-change (fn [e] (file-selected-fn (-> e .-target .-files first)))}]
   [:p {:class "text-xs font-normal text-slate-400 text-center mt-2"}
    "Only PDF files are supported."]])

(defn on-upload
  [file]
  (read-file-using file (fn [file-data]
                          (.catch
                           (.then (extract-structured-data (.-pdfjsLib js/globalThis) file-data)
                                  #(reset! pdf-metadata %))
                           #(js/console.error %)))))

(defn page
  []
  (let [metadata @pdf-metadata]
   [:div {:class "min-h-screen w-full max-w-5xl mx-auto py-12 sm:px-6 lg:px-8 flex flex-col items-center justify-center"}
    [:div
     {:class "px-6 sm:px-0 w-full my-12 grid md:grid-cols-2 grid-cols-1 text-gray-700 dark:text-gray-300 gap-6"}
     [:div {:class "justify-start grow-1"}
      [:h1 {:class "text-left w-full text-4xl font-bold"} "PDF Meta"]
      [:p "Inspect PDF metadata, including XMP, securly and privately (offline) directly in the browser."]]
     [:div {:class "justify-end"}
      [file-upload on-upload]]]
    (when-not (empty? metadata)
      [:div
       {:class "w-full space-y-6"}
       [description-list
        "Basic information"
        "Core properties and metadata for the document."
        (-> metadata (dissoc :custom) (update-keys name))]
       [description-list
        "Custom metadata"
        "Additional metadata, also known as XMP metadata, usually only exists for specific documents and producers."
        (:custom metadata)
        "The PDF contains no custom metadata."]])
    [:footer
     [:p {:class "mt-12 text-gray-700 dark:text-gray-300"}
      [:a {:href "https://www.github.com/jacobemcken/pdf-meta" :target "_blank" :rel "noopener noreferrer"}
       [:svg
        {:viewBox "0 0 2116.6666 2116.6666"
         :version "1.1"
         :xmlns "http://www.w3.org/2000/svg"
         :xmlns:svg "http://www.w3.org/2000/svg"
         :class "h-8 w-8"}
        [:path
         {:class "fill-gray-700 dark:fill-gray-300"
          :d "m 154,1369 q 13,14 38,31 4,2 10,5.5 6,3.5 24.5,19 18.5,15.5 36,35.5 17.5,20 39.5,56.5 22,36.5 39,80.5 l 3,9 q 0,0 11,23 11,23 20.5,33 9.5,10 32,37 22.5,27 44.5,37.5 22,10.5 59.5,31 37.5,20.5 76.5,20.5 30,5 69,5 h 25 q 54,-2 114,-15 2,226 2,230 165,40 335,40 162,0 333,-41 0,-5 1,-134 1,-129 1,-236 0,-169 -92,-249 70,-8 129,-20 59,-12 124,-36 65,-24 116,-57 51,-33 98.5,-84 47.5,-51 79,-116 31.5,-65 50.5,-155 19,-90 19,-198 0,-211 -138,-362 8,-19 14.5,-45.5 6.5,-26.5 11.5,-74.5 2,-16 2,-34 0,-35 -6,-76 -10,-62 -36,-127 -5,-1 -14,-3 h -9 q -11,0 -33,2 -33,3 -72,14.5 Q 1673,28 1607.5,60 1542,92 1470,141 1308,96 1132,96 950,98 794,141 585,0 456,0 h -5 l -27,3 q -25,65 -35,127 -6,41 -6,75 0,18 1,35 6,47 12,74 6,27 14,46 -139,152 -139,362 0,108 18.5,198 18.5,90 50.5,154.5 32,64.5 79.5,116 47.5,51.5 98.5,84.5 51,33 115.5,57 64.5,24 123.5,36.5 59,12.5 129,20.5 -70,61 -86,180 -29,13 -59.5,22 -30.5,9 -76.5,13 h -23 q -34,0 -65,-8 -42,-10 -88,-45 -46,-35 -80,-94 -3,-5 -8.5,-14 -5.5,-9 -25.5,-31.5 -20,-22.5 -42,-40.5 -22,-18 -57.5,-34 -35.5,-16 -73.5,-19 h -7.5 q 0,0 -15.5,1.5 -15.5,1.5 -19,4 -3.5,2.5 -14,8.5 -5,6 -5,12 0,9 14,25 z"}]]]]]]))

(defn ^:dev/after-load start
  []
  (dom/render [page]
              (.getElementById js/document "app")))

(defn ^:export init []
  (if-not js/Worker
    (js/console.error "Web Workers not supported.")
    (set! (.-workerSrc (.-GlobalWorkerOptions (.-pdfjsLib js/globalThis)))
          "https://cdn.jsdelivr.net/npm/pdfjs-dist@6.0.227/build/pdf.worker.mjs"))
  (start))
