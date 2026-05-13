package com.rma.mccabe_thiele


sealed class Resultados {
    data class Sucesso(
        val vazaoDestilado: Double?,
        val vazaoResiduo: Double?,
        val numeroMinimoEstagios: Double?,
        val numeroEstagios: Double?,
        val estagioCargaTopo: Double?,
        val rMinRefluxo: Double?,
        val rRefluxo: Double?,
        val pontosRetificacao: List<Pair<Double, Double>>?,
        val pontosEstripagem: List<Pair<Double, Double>>?
    ): Resultados()

    data class Erro(val mensagem: String): Resultados()
}

