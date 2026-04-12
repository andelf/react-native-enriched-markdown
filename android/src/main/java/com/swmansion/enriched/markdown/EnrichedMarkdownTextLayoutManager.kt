package com.swmansion.enriched.markdown

class EnrichedMarkdownTextLayoutManager(
  private val view: EnrichedMarkdownText,
) {
  fun invalidateLayout() {
    val text = view.text
    val paint = view.paint
    val heightChanged = MeasurementStore.store(view.id, text, paint)
    if (heightChanged) {
      view.requestLayout()
      view.invalidate()
    }
  }

  fun releaseMeasurementStore() {
    MeasurementStore.release(view.id)
  }
}
