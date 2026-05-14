package com.rma.mccabe_thiele.helper

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.rma.mccabe_thiele.model.McTResultados
import org.apache.commons.math3.analysis.UnivariateFunction

/**
 * Especialista para gerenciar a visualização do gráfico de McCabe-Thiele.
 * Centraliza as configurações de estilo e organização do MPAndroidChart.
 */
class McTChartManager(private val chart: LineChart) {

    init {
        configurarPadrao()
    }

    private fun configurarPadrao() {
        chart.apply {
            description.isEnabled = false 
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 1f
                setLabelCount(11, true)
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
            }

            axisRight.isEnabled = false 

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                axisMinimum = 0f
                axisMaximum = 1f
                setLabelCount(11, true)
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
            }
            
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                form = Legend.LegendForm.LINE
                textSize = 12f
                yOffset = 10f
                isWordWrapEnabled = true
            }

            // Margens para garantir que nada seja cortado
            // O MPAndroidChart precisa de offsets se a legenda estiver fora
            setExtraOffsets(10f, 10f, 10f, 10f)
        }
    }

    /**
     * Renderiza os mcTResultados completos no gráfico.
     */
    fun renderizar(
        mcTResultados: McTResultados.Sucesso,
        curvaEquilibrio: UnivariateFunction,
        zF: Double,
        valorq: Double,
        xD: Double,
        xB: Double
    ) {
        val dataSets = mutableListOf<LineDataSet>()

        // reta de 45 graus
        val reta45 = UnivariateFunction { x -> x }
        dataSets.add(reta45.toEntries().toLineDataSet("y = x", Color.GRAY).apply {
            enableDashedLine(10f, 10f, 0f)
            lineWidth = 1f
        })

        // curva de equilíbrio
        dataSets.add(curvaEquilibrio.toEntries().toLineDataSet("Equilíbrio", Color.BLUE).apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
        })

        // linha q
        val entriesQ = when {
            mcTResultados.qline == null && valorq == 1.0 -> {
                listOf(
                    Entry(zF.toFloat(), zF.toFloat()),
                    Entry(zF.toFloat(), mcTResultados.yIntersecao.toFloat())
                )
            }
            mcTResultados.qline != null -> {
                val xMin = minOf(zF, mcTResultados.xIntersecao)
                val xMax = maxOf(zF, mcTResultados.xIntersecao)
                mcTResultados.qline.toEntries(start = xMin, end = xMax)
            }
            else -> emptyList()
        }
        if (entriesQ.isNotEmpty()) {
            dataSets.add(entriesQ.toLineDataSet("Linha q", Color.MAGENTA))
        }

        // reta de retificação
        dataSets.add(mcTResultados.retaRetificacao.toEntries(
            start = minOf(mcTResultados.xIntersecao, xD),
            end = maxOf(mcTResultados.xIntersecao, xD)
        ).toLineDataSet("Retificação", Color.RED))

        // reta de estripagem
        dataSets.add(mcTResultados.retaEstripagem.toEntries(
            start = minOf(mcTResultados.xIntersecao, xB),
            end = maxOf(mcTResultados.xIntersecao, xB)
        ).toLineDataSet("Estripagem", Color.GREEN))

        // degraus
        dataSets.add(mcTResultados.pontosEscada.toLineDataSetFromPairs("Estágios", Color.BLACK).apply {
            lineWidth = 1.5f
            setDrawCircles(false)
        })

        chart.data = LineData(dataSets as List<LineDataSet>)
        chart.invalidate()
    }
}
