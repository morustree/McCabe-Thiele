package com.rma.mccabe_thiele.model

import com.rma.mccabe_thiele.R
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction
import org.apache.commons.math3.analysis.solvers.BrentSolver

/*
* McCabe-Thiele para condensador total (para o destilado como líquido saturado)
* ou condensador parcial (com saída gasosa e única).
* Refervedor parcial.
* Pressão uniforme e constante.
* Alimentação em somente um prato.
* Sem sidestream.
*/

class McTMetodo(
    private val mcTEspecificacoes: McTEspecificacoes,
    private val curvaEquilibirio: UnivariateFunction
) {

    private val linhaq: PolynomialFunction? by lazy {
        if (mcTEspecificacoes.valorq != 1.0) {
            val coefAngular = (mcTEspecificacoes.valorq / (mcTEspecificacoes.valorq - 1))
            val coefLinear = ((-1) * mcTEspecificacoes.zF / (mcTEspecificacoes.valorq - 1))
            PolynomialFunction(doubleArrayOf(coefLinear, coefAngular))
        } else {
            // retas verticais
            null
        }
    }

    private val retaOpRetificacao: PolynomialFunction? by lazy {
        // y = (R/(R+1))*x + (xD/(R+1)); R = razoesR * Rmin
        razaoRefluxo?.let {
            val coefAngular = it / (it + 1)
            val coefLinear = mcTEspecificacoes.xD / (it + 1)
            PolynomialFunction(doubleArrayOf(coefLinear, coefAngular))
        }
    }


    private val xIntersecaoLinhaqRetificacao: Double? by lazy {
        val qlinha = linhaq
        if (qlinha != null) {
            calcularXIntersecao(
                funcao1 = qlinha,
                funcao2 = retaOpRetificacao,
                minimo = 0.0,
                maximo = 1.0
            )
        } else {
            // a interseção em x é zF
            mcTEspecificacoes.zF
        }
    }


    private val yIntersecaoLinhaqRetificacao: Double? by lazy {
        val xIntersecao = xIntersecaoLinhaqRetificacao ?: return@lazy null
        linhaq?.value(xIntersecao) ?: retaOpRetificacao?.value(xIntersecao)
    }


    private val retaOpEstripagem: PolynomialFunction? by lazy {
        val x = xIntersecaoLinhaqRetificacao
        val y = yIntersecaoLinhaqRetificacao
        if (x != null && y != null) {
            val x1 = mcTEspecificacoes.xB
            val y1 = mcTEspecificacoes.xB
            if (x == x1) return@lazy null
            val coefAngular = (y - y1) / (x - x1)
            val coefLinear = y1 - (coefAngular * x1)
            PolynomialFunction(doubleArrayOf(coefLinear, coefAngular))
        } else {
            null
        }
    }


    private val vazaoDestilado: Double? by lazy {
        mcTEspecificacoes.vazaoF * (mcTEspecificacoes.zF - mcTEspecificacoes.xB) / (mcTEspecificacoes.xD - mcTEspecificacoes.xB)
    }


    private val vazaoResiduo: Double? by lazy {
        mcTEspecificacoes.vazaoF * (1 - (mcTEspecificacoes.zF - mcTEspecificacoes.xB) / (mcTEspecificacoes.xD - mcTEspecificacoes.xB))
    }


    private val razaoMinRefluxo: Double? by lazy {
        /* A razão de refluxo mínima requer infinitos estágios,
        situação em que as retas de operação tocam a curva de equilíbrio.
        Esse ponto pode ser obtido pela interseção da linhaq com a curva de equilíbrio */
        //todo: na interface do usuário, impor que o valor de razoesR seja maior que 1 (R deve ser maior que Rmin)
        val qlinha = linhaq
        val intersecaoX = if (qlinha != null) {
            calcularXIntersecao(
                funcao1 = curvaEquilibirio,
                funcao2 = qlinha,
                minimo = mcTEspecificacoes.xB,
                maximo = mcTEspecificacoes.xD
            )
        } else {
            // para q = 1, a interseção com a curva de equilíbrio ocorre em x = zF
            mcTEspecificacoes.zF
        }

        intersecaoX?.let { xInt ->
            val yIntersecao = curvaEquilibirio.value(xInt)

            // coeficiente angular da reta de retificação: (R/(R+1)); ponto inicial: (xD, xD)
            if (xInt == mcTEspecificacoes.xD) return@lazy null
            val coefAngularRetificacao =
                (yIntersecao - mcTEspecificacoes.xD) / (xInt - mcTEspecificacoes.xD)
            if (coefAngularRetificacao == 1.0) return@lazy null
            coefAngularRetificacao / (1 - coefAngularRetificacao)
        }
    }


    private val razaoRefluxo: Double? by lazy {
        razaoMinRefluxo?.let { rMin ->
            val calculado = mcTEspecificacoes.razoesR * rMin
            if (calculado <= rMin) null else calculado
        }
    }


    private val numMinEstagios: Double? by lazy {
        /* Quando as linhas de operação coincidem com a reta de 45,
        há a condição de refluxo total e a menor quantidade possível de estágios é requerida. */
        var numMinEst = 0.0
        var raiz = mcTEspecificacoes.xD
        var indice = 0
        // o ponto inicial é (xD, xD) sobre a reta de 45
        val pontos = mutableListOf<Pair<Double, Double>>()
        pontos.add(Pair(mcTEspecificacoes.xD, mcTEspecificacoes.xD))

        while (raiz > mcTEspecificacoes.xB && indice < 500) {
            //y1 = f(x); y2 = g(x). Na interseção, y1 = y2. Então: f(x) = g(x) e f(x) - g(x) = 0
            raiz = calcularXIntersecao(
                funcao1 = curvaEquilibirio,
                minimo = 0.0, //todo: confirmar se não é preciso colocar o primeiro item da lista original de pontos para não dar erro
                maximo = raiz,
                valorY = pontos.last().second
            ) ?: return@lazy null
            pontos.add(Pair(raiz, pontos.last().second)) // atualiza o valor de x e repete o valor do y anterior - faz um ponto sobre a curva de equilíbrio
            pontos.add(Pair(raiz, raiz)) // faz um ponto sobre a reta de 45
            numMinEst++
            indice += 1
        }

        if (pontos.size < 3) return@lazy numMinEst

        val fracaoEstagio =
            (pontos[(pontos.size - 3)].first - mcTEspecificacoes.xB) / (pontos[(pontos.size - 3)].first - pontos.last().first)
        numMinEst - 1 + fracaoEstagio
    }


    private val numEstagios: ResultadosNumEstagios? by lazy {
        val xIntersecao = xIntersecaoLinhaqRetificacao
        val retaEstripagem = retaOpEstripagem
        val retaRetificacao = retaOpRetificacao
        if (xIntersecao != null && retaEstripagem != null && retaRetificacao != null) {
            var numEstagiosRetificacao = 0.0
            var raiz = mcTEspecificacoes.xD
            var indice = 0
            // o ponto inicial é (xD, xD) sobre a reta de 45
            val pontosRetificacao = mutableListOf<Pair<Double, Double>>()
            pontosRetificacao.add(Pair(mcTEspecificacoes.xD, mcTEspecificacoes.xD))
            while (raiz > xIntersecao && indice < 500) {
                raiz = calcularXIntersecao(
                    funcao1 = curvaEquilibirio,
                    minimo = 0.0, //todo: confirmar se não é preciso colocar o primeiro item da lista original de pontos para não dar erro
                    maximo = raiz,
                    valorY = pontosRetificacao.last().second
                ) ?: return@lazy null
                pontosRetificacao.add(Pair(raiz, pontosRetificacao.last().second)) // atualiza o valor de x e repete o valor do y anterior - faz um ponto sobre a curva de equilíbrio
                pontosRetificacao.add(Pair(raiz,retaRetificacao.value(raiz))) // faz um ponto sobre a reta de retificação
                numEstagiosRetificacao++
                indice++
            }

            val fracaoEstagio = if (pontosRetificacao.size >= 3) {
                (pontosRetificacao[(pontosRetificacao.size - 3)].first - xIntersecao) / (pontosRetificacao[(pontosRetificacao.size - 3)].first - pontosRetificacao.last().first)
            } else 0.0

            numEstagiosRetificacao = numEstagiosRetificacao - 1 + fracaoEstagio

            indice = 0
            var numEstagiosEstripagem = 0.0
            val pontosEstripagem = mutableListOf<Pair<Double, Double>>()

            if (pontosRetificacao.size < 2) return@lazy null

            pontosEstripagem.add(
                Pair(
                    pontosRetificacao[(pontosRetificacao.size - 2)].first,
                    pontosRetificacao[(pontosRetificacao.size - 2)].second
                )
            ) // ponto sobre a curva de equilíbrio

            val fracao1 = if (pontosRetificacao.size >= 3) {
                (xIntersecao - pontosRetificacao[(pontosRetificacao.size - 2)].first) / (pontosRetificacao[(pontosRetificacao.size - 3)].first - pontosRetificacao[(pontosRetificacao.size - 2)].first)
            } else 0.0

            while (raiz > mcTEspecificacoes.xB && indice < 500) {
                pontosEstripagem.add(Pair(raiz, retaEstripagem.value(raiz))) // faz um ponto sobre a reta de estripagem
                numEstagiosEstripagem++
                raiz = calcularXIntersecao(
                    funcao1 = curvaEquilibirio,
                    minimo = 0.0, //todo: confirmar se não é preciso colocar o primeiro item da lista original de pontos para não dar erro
                    maximo = raiz,
                    valorY = pontosEstripagem.last().second
                ) ?: return@lazy null
                pontosEstripagem.add(Pair(raiz, pontosEstripagem.last().second)) // atualiza o valor de x e repete o valor do y anterior - faz um ponto sobre a curva de equilíbrio
                indice++
            }

            pontosEstripagem.add(Pair(raiz, raiz)) // faz um ponto sobre a reta de 45

            val fracao2 = if (pontosEstripagem.size >= 3) {
                (mcTEspecificacoes.xB - pontosEstripagem[(pontosEstripagem.size - 2)].first) / (pontosEstripagem[(pontosEstripagem.size - 3)].first - raiz)
            } else 0.0

            numEstagiosEstripagem = numEstagiosEstripagem + fracao1 - fracao2

            ResultadosNumEstagios(
                numEstagiosRetificacao + numEstagiosEstripagem,
                numEstagiosRetificacao + 1,
                (pontosRetificacao + pontosEstripagem)
            )
        } else {
            null
        }
    }

    fun calcular(): McTResultados {
        if (mcTEspecificacoes.zF >= mcTEspecificacoes.xD) return McTResultados.Erro(R.string.resultados_erro1)
        if (mcTEspecificacoes.zF <= mcTEspecificacoes.xB) return McTResultados.Erro(R.string.resultados_erro2)

        val vazaoD = vazaoDestilado ?: return McTResultados.Erro(R.string.resultados_erro3)
        val vazaoR = vazaoResiduo ?: return McTResultados.Erro(R.string.resultados_erro4)
        val numMinEst = numMinEstagios ?: return McTResultados.Erro(R.string.resultados_erro5)
        val razaoMinRef = razaoMinRefluxo ?: return McTResultados.Erro(R.string.resultados_erro6)
        val razaoRef = razaoRefluxo ?: return McTResultados.Erro(R.string.resultados_erro7)
        val dadosEstagios = numEstagios ?: return McTResultados.Erro(R.string.resultados_erro8)
        if (mcTEspecificacoes.valorq != 1.0 && linhaq == null) return McTResultados.Erro(R.string.resultados_erro9)
        val rRetif = retaOpRetificacao ?: return McTResultados.Erro(R.string.resultados_erro10)
        val rEstrip = retaOpEstripagem ?: return McTResultados.Erro(R.string.resultados_erro11)
        val intersecaox = xIntersecaoLinhaqRetificacao ?: return McTResultados.Erro(R.string.resultados_erro12)
        val intersecaoy = yIntersecaoLinhaqRetificacao ?: return McTResultados.Erro(R.string.resultados_erro12)

        return McTResultados.Sucesso(
            vazaoDestilado = vazaoD,
            vazaoResiduo = vazaoR,
            numeroMinimoEstagios = numMinEst,
            numeroEstagios = dadosEstagios.qtdeEstagiosTotais,
            estagioCargaTopo = dadosEstagios.estagioCargaTopo,
            rMinRefluxo = razaoMinRef,
            rRefluxo = razaoRef,
            pontosEscada = dadosEstagios.pontosDegraus,
            qline = linhaq,
            xIntersecao = intersecaox,
            yIntersecao =  intersecaoy,
            retaRetificacao = rRetif,
            retaEstripagem = rEstrip,
        )
    }


    private fun calcularXIntersecao(
        funcao1: UnivariateFunction,
        minimo: Double,
        maximo: Double,
        funcao2: UnivariateFunction? = null,
        valorY: Double = 0.0,
        iteracoes: Int = 50
    ): Double? {
        // Vantagem do BrentSolver: pesquisa números no domínio real, não traz raiz complexa.
        // Se não houver raiz real, ele lança NoBracketingException.
        return runCatching {
            BrentSolver().solve(
                iteracoes,
                { x ->
                    if (funcao2 != null) funcao1.value(x) - funcao2.value(x)
                    else funcao1.value(x) - valorY
                },
                minimo,
                maximo
            )
        }.getOrNull()
    }
}


private data class ResultadosNumEstagios(
    val qtdeEstagiosTotais: Double,
    val estagioCargaTopo: Double,
    val pontosDegraus: List<Pair<Double, Double>>
)
