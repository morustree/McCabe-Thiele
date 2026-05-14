package com.rma.mccabe_thiele.model

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction


sealed class McTResultados {
    data class Sucesso(
        val vazaoDestilado: Double,
        val vazaoResiduo: Double,
        val numeroMinimoEstagios: Double,
        val numeroEstagios: Double,
        val estagioCargaTopo: Double,
        val rMinRefluxo: Double,
        val rRefluxo: Double,
        val pontosEscada: List<Pair<Double, Double>>,
        val qline: PolynomialFunction?,
        val xIntersecao: Double,
        val yIntersecao: Double,
        val retaRetificacao: PolynomialFunction,
        val retaEstripagem: PolynomialFunction,
    ): McTResultados()

    data class Erro(val mensagem: Int): McTResultados()
}

