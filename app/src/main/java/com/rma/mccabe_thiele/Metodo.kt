package com.rma.mccabe_thiele

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

class Metodo(
    private val especificacoes: Especificacoes,
    private val curvaEquilibirio: UnivariateFunction
) {

    private val linhaq: PolynomialFunction? by lazy {
        val coefAngular: Double
        val coefLinear: Double
        if (especificacoes.valorq != 1.0) {
            coefAngular = (especificacoes.valorq / (especificacoes.valorq - 1))
            coefLinear = ((-1) * especificacoes.zF / (especificacoes.valorq - 1))
            PolynomialFunction(doubleArrayOf(coefLinear, coefAngular))
        } else {
            coefLinear = especificacoes.zF
            PolynomialFunction(doubleArrayOf(coefLinear))
        }
    }

    private val retaOpRetificacao: PolynomialFunction? by lazy {
        // y = (R/(R+1))*x + (xD/(R+1)); R = razoesR * Rmin
        razaoRefluxo?.let {
            val coefAngular = it / (it + 1)
            val coefLinear = especificacoes.xD / (it + 1)
            PolynomialFunction(doubleArrayOf(coefLinear, coefAngular))
        }
    }


    private val xIntersecaoLinhaqRetificacao: Double? by lazy {
        // o bloco let só é executado se xIntersecaoLinhaqRetificacao não for nulo
        // se for nulo, então yIntersecaoLinhaqRetificacao assume nulo
        linhaq?.let{calcularXIntersecao(funcao1 = it, funcao2 = retaOpRetificacao, minimo = 0.0, maximo = 1.0)}
    }


    private val yIntersecaoLinhaqRetificacao: Double? by lazy {
        val xIntersecao = xIntersecaoLinhaqRetificacao
        val qlinha = linhaq
        if (xIntersecao != null && qlinha != null) {
            qlinha.value(xIntersecao)
        } else {
            null
        }
    }


    private val retaOpEstripagem: PolynomialFunction? by lazy {
        val x = xIntersecaoLinhaqRetificacao
        val y = yIntersecaoLinhaqRetificacao
        if (x != null && y != null) {
            val x1 = especificacoes.xB
            val y1 = especificacoes.xB
            val coefAngular =
                (y - y1) / (x - x1)
            val coefLinear = y1 - (coefAngular * x1)
            PolynomialFunction(doubleArrayOf(coefLinear, coefAngular))
        } else {
           null
        }
    }


    private val reta45: PolynomialFunction by lazy {
        // y = x
        PolynomialFunction(doubleArrayOf(0.0, 1.0))
    }


    private val vazaoDestilado: Double? by lazy {
        especificacoes.vazaoF * (especificacoes.zF - especificacoes.xB) / (especificacoes.xD - especificacoes.xB)
    }


    private val vazaoResiduo: Double? by lazy {
        especificacoes.vazaoF * (1 - (especificacoes.zF - especificacoes.xB) / (especificacoes.xD - especificacoes.xB))
    }


    private val razaoMinRefluxo: Double? by lazy {
        /* A razão de refluxo mínima requer infinitos estágios,
        situação em que as retas de operação tocam a curva de equilíbrio.
        Esse ponto pode ser obtido pela interseção da linhaq com a curva de equilíbrio */
        // todo: na interface do usuário, impor que o valor de razoesR seja maior que 1 (R deve ser maior que Rmin)
        val intersecaoX = calcularXIntersecao(
            funcao1 = curvaEquilibirio,
            funcao2 = linhaq,
            minimo = especificacoes.xB,
            maximo = especificacoes.xD
        )
        //val yIntersecao = curvaEquilibirio.value(intersecaoX)
        val yIntersecao: Double?
        intersecaoX?.let {
            yIntersecao = curvaEquilibirio.value(it)

            // coeficiente angular da reta de retificação: (R/(R+1)); ponto inicial: (xD, xD)
            val coefAngularRetificacao =
                (yIntersecao - especificacoes.xD) / (intersecaoX - especificacoes.xD)
            coefAngularRetificacao / (1 - coefAngularRetificacao)
        }
    }


    private val razaoRefluxo: Double? by lazy {
        razaoMinRefluxo?.let{especificacoes.razoesR * it}
    }


    private val numMinEstagios: Double? by lazy {
        /* Quando as linhas de operação coincidem com a reta de 45,
        há a condição de refluxo total e a menor quantidade possível de estágios é requerida. */
        var numMinEstagios = 0.0
        var raiz = especificacoes.xD
        var indice = 0
        // o ponto inicial é (xD, xD) sobre a reta de 45
        val pontos = mutableListOf<Pair<Double, Double>>()
        pontos.add(Pair(especificacoes.xD, especificacoes.xD))

        while (raiz > especificacoes.xB && indice < 500) {
            //y1 = f(x); y2 = g(x). Na interseção, y1 = y2. Então: f(x) = g(x) e f(x) - g(x) = 0
            raiz = calcularXIntersecao(funcao1 = curvaEquilibirio, minimo = 0.0, maximo = raiz, valorY = pontos.last().second) ?: return@lazy null
            pontos.add(Pair(raiz, pontos.last().second)) // atualiza o valor de x e repete o valor do y anterior - faz um ponto sobre a curva de equilíbrio
            pontos.add(Pair(raiz, raiz)) // faz um ponto sobre a reta de 45
            numMinEstagios++
            indice += 1
        }
        val fracaoEstagio = (pontos[(pontos.size - 3)].first - especificacoes.xB) / (pontos[(pontos.size - 3)].first - pontos.last().first)
        numMinEstagios - 1 + fracaoEstagio
    }


    private val numEstagios: ResultadosNumEstagios? by lazy {
        val xIntersecao = xIntersecaoLinhaqRetificacao
        val retaEstripagem = retaOpEstripagem
        val retaRetificacao = retaOpRetificacao
        if (xIntersecao != null && retaEstripagem != null && retaRetificacao != null) {
            var numEstagiosRetificacao = 0.0
            var raiz = especificacoes.xD
            var indice = 0
            // o ponto inicial é (xD, xD) sobre a reta de 45
            val pontosRetificacao = mutableListOf<Pair<Double, Double>>()
            pontosRetificacao.add(Pair(especificacoes.xD, especificacoes.xD))
            while (raiz > xIntersecao && indice < 500) {
                raiz = calcularXIntersecao(funcao1 = curvaEquilibirio, minimo = 0.0, maximo = raiz, valorY = pontosRetificacao.last().second) ?: return@lazy null
                pontosRetificacao.add(Pair(raiz, pontosRetificacao.last().second)) // atualiza o valor de x e repete o valor do y anterior - faz um ponto sobre a curva de equilíbrio
                pontosRetificacao.add(Pair(raiz, retaRetificacao.value(raiz))) // faz um ponto sobre a reta de retificação
                numEstagiosRetificacao++
                indice++
            }
            val fracaoEstagio = (pontosRetificacao[(pontosRetificacao.size - 3)].first - xIntersecao) / (pontosRetificacao[(pontosRetificacao.size - 3)].first - pontosRetificacao.last().first)
            numEstagiosRetificacao = numEstagiosRetificacao - 1 + fracaoEstagio

            indice = 0
            var numEstagiosEstripagem = 0.0
            val pontosEstripagem = mutableListOf<Pair<Double, Double>>()
            pontosEstripagem.add(Pair(pontosRetificacao[(pontosRetificacao.size - 2)].first, pontosRetificacao[(pontosRetificacao.size - 2)].second)) // ponto sobre a curva de equilíbrio
            val fracao1 = (xIntersecao - pontosRetificacao[(pontosRetificacao.size - 2)].first) / (pontosRetificacao[(pontosRetificacao.size - 3)].first - pontosRetificacao[(pontosRetificacao.size - 2)].first)
            while (raiz > especificacoes.xB && indice < 500) {
                pontosEstripagem.add(Pair(raiz, retaEstripagem.value(raiz))) // faz um ponto sobre a reta de estripagem
                numEstagiosEstripagem++
                raiz = calcularXIntersecao(funcao1 = curvaEquilibirio, minimo = 0.0, maximo = raiz, valorY = pontosEstripagem.last().second) ?: return@lazy null
                pontosEstripagem.add(Pair(raiz, pontosEstripagem.last().second)) // atualiza o valor de x e repete o valor do y anterior - faz um ponto sobre a curva de equilíbrio
                indice++
            }
            pontosEstripagem.add(Pair(raiz, reta45.value(raiz))) // faz um ponto sobre a reta de 45
            val fracao2 =
                (especificacoes.xB - pontosEstripagem[(pontosEstripagem.size - 2)].first) / (pontosEstripagem[(pontosEstripagem.size - 3)].first - raiz)
            numEstagiosEstripagem = numEstagiosEstripagem + fracao1 - fracao2

            ResultadosNumEstagios(
                numEstagiosRetificacao + numEstagiosEstripagem,
                numEstagiosRetificacao + 1,
                pontosRetificacao,
                pontosEstripagem)
        } else {
            null
        }
    }



    fun calcular(): Resultados {
        if (especificacoes.zF >= especificacoes.xD) return Resultados.Erro("A fração molar do componente mais volátil na alimentação (zF) não deve ser maior ou igual à do destilado (xD).")
        if (especificacoes.zF <= especificacoes.xB) return Resultados.Erro("A fração molar do componente mais volátil na alimentação (zF) não deve ser menor ou igual que a do resíduo (xB).")
        val vazaoD = vazaoDestilado ?: return Resultados.Erro("Falha no cálculo da vazão de destilado.")
        val vazaoR = vazaoResiduo ?: return Resultados.Erro("Falha no cálculo da vazão de resíduo.")
        val numMinEst = numMinEstagios ?: return Resultados.Erro("Falha no cálculo do número mínimo de estágios.")
        val razaoMinRef = razaoMinRefluxo ?: return Resultados.Erro("Falha no cálculo da razão mínima de refluxo.")
        val razaoRef = razaoRefluxo ?: return Resultados.Erro("Falha no cálculo da razão de refluxo.")
        val dadosEstagios = numEstagios ?: return Resultados.Erro("Falha no cálculo do número de estágios.")

        return Resultados.Sucesso(
            vazaoDestilado = vazaoD,
            vazaoResiduo = vazaoR,
            numeroMinimoEstagios = numMinEst,
            numeroEstagios = dadosEstagios.qtdeEstagiosTotais,
            estagioCargaTopo = dadosEstagios.estagioCargaTopo,
            rMinRefluxo = razaoMinRef,
            rRefluxo = razaoRef,
            pontosRetificacao = dadosEstagios.pontosRetificacao,
            pontosEstripagem = dadosEstagios.pontosEstripagem
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
                    if(funcao2 != null) funcao1.value(x) - funcao2.value(x)
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
    val pontosRetificacao: MutableList<Pair<Double, Double>>,
    val pontosEstripagem: MutableList<Pair<Double, Double>>
)

