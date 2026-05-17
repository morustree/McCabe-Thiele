package com.rma.mccabe_thiele.helper

import android.graphics.Color
import android.util.TypedValue
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.rma.mccabe_thiele.R
import com.rma.mccabe_thiele.model.McTEspecificacoes
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
        val corEixos = ContextCompat.getColor(chart.context ?: return, android.R.color.darker_gray)

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
                gridColor = corEixos
                textColor = corEixos
            }

            axisRight.isEnabled = false 

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                axisMinimum = 0f
                axisMaximum = 1f
                setLabelCount(11, true)
                setDrawGridLines(true)
                gridColor = corEixos
                textColor = corEixos
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
                textColor = corEixos
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
        especificacoes: McTEspecificacoes,
        pontosOriginais: List<Pair<Double, Double>>
    ) {
        val corEscada = ContextCompat.getColor(chart.context ?: return, R.color.graf_escada)
        val corPontos = ContextCompat.getColor(chart.context ?: return, R.color.graf_pontos)

        val dataSets = ArrayList<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()
        // reta de 45 graus
        val reta45 = UnivariateFunction { x -> x }
        dataSets.add(reta45.toEntries(0.0, 1.0).toLineDataSet("y = x", Color.GRAY).apply {
            enableDashedLine(10f, 10f, 4f)
            lineWidth = 1f
        })

        // curva de equilíbrio
        dataSets.add(curvaEquilibrio.toEntries(pontosOriginais.first().first, pontosOriginais.last().first).toLineDataSet("Equilíbrio", Color.BLUE).apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
        })

        // linha q
        val entriesQ = when {
            mcTResultados.qline == null && especificacoes.valorq == 1.0 -> {
                listOf(
                    Entry(especificacoes.zF.toFloat(), especificacoes.zF.toFloat()),
                    Entry(especificacoes.zF.toFloat(), mcTResultados.yIntersecao.toFloat())
                )
            }
            mcTResultados.qline != null -> {
                val xMin = minOf(especificacoes.zF, mcTResultados.xIntersecao)
                val xMax = maxOf(especificacoes.zF, mcTResultados.xIntersecao)
                mcTResultados.qline.toEntries(start = xMin, end = xMax)
            }
            else -> emptyList()
        }
        if (entriesQ.isNotEmpty()) {
            dataSets.add(entriesQ.toLineDataSet("Linha q", Color.MAGENTA))
        }

        // reta de retificação
        dataSets.add(mcTResultados.retaRetificacao.toEntries(
            start = minOf(mcTResultados.xIntersecao, especificacoes.xD),
            end = maxOf(mcTResultados.xIntersecao, especificacoes.xD)
        ).toLineDataSet("Retificação", Color.RED))

        // reta de estripagem
        dataSets.add(mcTResultados.retaEstripagem.toEntries(
            start = minOf(mcTResultados.xIntersecao, especificacoes.xB),
            end = maxOf(mcTResultados.xIntersecao, especificacoes.xB)
        ).toLineDataSet("Estripagem", Color.GREEN))

        // degraus
        dataSets.add(mcTResultados.pontosEscada.toLineDataSetFromPairs("Estágios", corEscada).apply {
            lineWidth = 1.5f
            setDrawCircles(false)
        })

        // pontos do CSV
        if (pontosOriginais.isNotEmpty()) {
            dataSets.add(pontosOriginais.toLineDataSetFromPairs("Dados CSV", corPontos, desenharCirculos = true).apply {
                setDrawCircles(true)
                setCircleColor(corPontos)
                circleRadius = 2f
                setDrawCircleHole(false)
                color = corPontos
                setDrawHighlightIndicators(false)
                isHighlightEnabled = false
                lineWidth = 1f
                enableDashedLine(0f,100000f,0f)
            })
        }

        chart.data = LineData(dataSets)
        chart.invalidate()
    }


    // carrega apenas a curva de equilíbrio e a reta de 45 graus
    fun renderizarErro(
        curvaEquilibrio: UnivariateFunction,
        pontosOriginais: List<Pair<Double, Double>>
    ) {

        val dataSets = ArrayList<com.github.mikephil.charting.interfaces.datasets.ILineDataSet>()
        // reta de 45 graus
        val reta45 = UnivariateFunction { x -> x }
        dataSets.add(reta45.toEntries(0.0, 1.0).toLineDataSet("y = x", Color.GRAY).apply {
            enableDashedLine(10f, 10f, 4f)
            lineWidth = 1f
        })

        // curva de equilíbrio
        dataSets.add(curvaEquilibrio.toEntries(pontosOriginais.first().first, pontosOriginais.last().first).toLineDataSet("Equilíbrio", Color.BLUE).apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
        })


        // pontos do CSV
        if (pontosOriginais.isNotEmpty()) {
            dataSets.add(pontosOriginais.toLineDataSetFromPairs("Dados CSV", Color.BLACK, desenharCirculos = true).apply {
                setDrawCircles(true)
                setCircleColor(Color.BLACK)
                circleRadius = 2f
                setDrawCircleHole(false)
                color = Color.BLACK
                setDrawHighlightIndicators(false)
                isHighlightEnabled = false
                lineWidth = 1f
                enableDashedLine(0f,100000f,0f)
            })
        }

        chart.data = LineData(dataSets)
        chart.invalidate()
    }


}
