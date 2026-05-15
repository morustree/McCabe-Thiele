package com.rma.mccabe_thiele.helper

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import org.apache.commons.math3.analysis.UnivariateFunction

/**
 * Extensões para facilitar a conversão de dados para o MPAndroidChart.
 */

/**
 * Converte uma UnivariateFunction em uma lista Entry.
 */
fun UnivariateFunction.toEntries(
    start: Double,
    end: Double,
    steps: Int = 100
): List<Entry> {
    val entries = mutableListOf<Entry>()
    val stepSize = (end - start) / (steps - 1)
    for (i in 0 until steps) {
        val x = start + i * stepSize
        entries.add(Entry(x.toFloat(), this.value(x).toFloat()))
    }
    return entries
}

/**
 * Converte uma lista de pares (x, y) em um LineDataSet configurado.
 */
fun List<Pair<Double, Double>>.toLineDataSetFromPairs(label: String, color: Int, desenharCirculos: Boolean = false): LineDataSet {
    val lista = if (this.isNotEmpty() && this.first().first > this.last().first) {
        this.reversed()
    } else {
        this
    }

    val entries = lista.map { Entry(it.first.toFloat(), it.second.toFloat()) }

    return entries.toLineDataSet(label, color, desenharCirculos)
}

/**
 * Converte uma lista de Entry diretamente em um LineDataSet.
 */
fun List<Entry>.toLineDataSet(label: String, color: Int, desenharCirculos: Boolean = false): LineDataSet {
    return LineDataSet(this, label).apply {
        this.color = color
        setDrawCircles(desenharCirculos)
        setDrawValues(false)
        lineWidth = 2f
    }
}
