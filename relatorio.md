# Relatório — Atividade Prática: Espaço de Tuplas com Apache River

**Disciplina:** Sistemas Distribuídos
**Dupla:** Tom Pereira Hunt / Pedro Henrique Gimenez
**Data:** 05/07/2026

---

## Nível 0 — Rodar e observar

### Observação inicial

1. Qual serviço aparece nos logs primeiro? Por que ele precisa existir antes dos outros?

> _Resposta:_ O reggie aparece primeiro. Ele é o serviço de descoberta (lookup)
> do Jini, então precisa estar no ar antes de todo mundo: o javaspaces se
> registra nele quando sobe, e o produtor e o consumidor descobrem o espaço
> perguntando pro reggie. Se ele não existisse ainda, ninguém acharia o
> javaspaces.

2. O produtor menciona o nome ou o endereço do consumidor em algum momento? O consumidor menciona o produtor?

> _Resposta:_ Não, em nenhum momento. O produtor só fala em write no espaço e o
> consumidor só fala em take do espaço. Nenhum dos dois cita o nome, o IP ou a
> quantidade do outro. Isso é o desacoplamento espacial: eles nem sabem que o
> outro existe, só conhecem o espaço.

3. O consumidor começa a processar tarefas antes que o produtor termine de depositar todas? O que isso diz sobre como os dois se coordenam?

> _Resposta:_ Sim. Nos logs dá pra ver o take da tarefa 1 acontecendo enquanto o
> produtor ainda está escrevendo as próximas. Isso mostra que a coordenação é
> indireta e assíncrona: o espaço funciona como um buffer no meio, o produtor
> larga as tarefas lá e o consumidor vai pegando no ritmo dele. Não tem
> sincronização direta entre os dois.

---

### Experimento de desacoplamento temporal

4. O consumidor encontrou as tarefas mesmo sendo iniciado depois que o produtor já havia encerrado? O que isso demonstra?

> _Resposta:_ Sim, encontrou. Subimos só o reggie, o javaspaces e o produtor,
> deixamos o produtor depositar tudo e encerrar, e só depois subimos o
> consumidor, que mesmo assim pegou as tarefas. Isso demonstra o desacoplamento
> temporal: as tuplas ficam guardadas no espaço (dentro do lease) e não somem
> quando o produtor sai. Os dois não precisam estar vivos ao mesmo tempo.

5. Em comunicação direta via socket, seria possível esse comportamento? Por quê não?

> _Resposta:_ Não seria. No socket os dois processos precisam estar no ar ao
> mesmo tempo e conectados um no outro. Se o produtor fecha antes do consumidor
> conectar, não tem quem receber a mensagem e ela se perde (ou dá connection
> refused). O socket é um canal ao vivo entre dois pontos, ele não guarda os
> dados pra alguém pegar depois, que é justamente o que o espaço de tuplas faz.

---

## Nível 1 — Inspecionar

### 1.1 As três operações

Preencha a tabela com base no que você observou nos logs:

| Operação River | Equivalente Linda | O que ela faz? | Bloqueia quando não encontra correspondência? | Altera o estado do espaço? |
|---------------|-------------------|----------------|----------------------------------------------|---------------------------|
| `write(entry)` | `OUT` | Escreve/deposita uma tupla (entry) no espaço. | Não, não tem o que casar, ela só grava e volta na hora. | Sim, adiciona uma tupla nova no espaço. |
| `take(template)` | `IN` | Procura uma tupla que casa com o template e retira ela do espaço, devolvendo pro cliente (leitura destrutiva). | Sim, bloqueia até aparecer uma tupla que casa (a não ser que use timeout 0 / NO_WAIT). | Sim, remove a tupla do espaço. |
| `read(template)` | `RD` | Procura uma tupla que casa e devolve uma cópia, sem tirar do espaço (leitura não-destrutiva). | Sim, também bloqueia até casar algo (ou timeout). | Não, a tupla continua lá pros outros. |

---

### 1.2 O papel do `reggie`

1. Quando o `reggie` caiu, os serviços que já estavam conectados ao espaço continuaram funcionando? Por quê?

> _Resposta:_ Sim, continuaram. O reggie serve só pra descoberta inicial: uma vez
> que o produtor e o consumidor já acharam o espaço, eles ficam com a referência
> e falam direto com o javaspaces. Derrubar o reggie depois disso não corta essa
> conversa, porque ele não fica no meio de cada write/take.

2. O que aconteceria com um produtor ou consumidor que tentasse iniciar enquanto o `reggie` estivesse fora do ar?

> _Resposta:_ Não conseguiria descobrir o espaço. Ele ia ficar no loop de
> "Aguardando espaço... (x/20)" tentando o lookup, e se o reggie não voltasse
> nas 20 tentativas ele desistia com a RuntimeException de "Não foi possível
> encontrar o espaço de tuplas".

3. Qual sistema moderno cumpre papel equivalente ao `reggie` em uma arquitetura de microsserviços?

> _Resposta:_ É o mesmo papel de um service discovery / service registry. Exemplos
> modernos: Consul, etcd, o Eureka (Netflix) e o próprio DNS interno do
> Kubernetes, que deixam um serviço achar o outro pelo nome sem ter o IP fixo no
> código.

---

### 1.3 Desacoplamento espacial

1. O produtor tem qualquer informação sobre quantos consumidores existem?

> _Resposta:_ Não. Ele escreve as tuplas no espaço e nem sabe se tem algum
> consumidor ligado, quanto mais quantos.

2. O consumidor tem qualquer informação sobre quem produziu a tarefa que ele retirou?

> _Resposta:_ Não. A tupla só carrega id, tipo e prioridade, nada sobre quem
> escreveu. Pro consumidor tanto faz de onde a tarefa veio.

3. Como produtor e consumidor se coordenam se não se conhecem?

> _Resposta:_ Pelo espaço, no meio dos dois. O produtor escreve tuplas e o
> consumidor pega por casamento de template (associative matching). Quem faz o
> encontro é o espaço, não eles. A coordenação é pelos dados, não por conhecer
> um ao outro.

---

### 1.4 Comportamento de bloqueio

1. O que o consumidor fez enquanto o espaço estava vazio?

> _Observado:_ Ficou parado, bloqueado no take() esperando, sem ficar rodando em
> loop gastando CPU. Só a mensagem de que tinha achado o espaço e estava
> aguardando tarefas.

2. Quando o produtor depositou a primeira tarefa, o que aconteceu imediatamente?

> _Observado:_ O take() desbloqueou na hora e o consumidor pegou a tarefa e
> começou a processar. Foi quase instantâneo depois do write.

3. Esse comportamento tem nome no modelo Linda. Qual é e por que ele é útil em sistemas distribuídos reais?

> _Resposta:_ É a operação bloqueante do Linda (o IN bloqueante). O take fica
> esperando até uma tupla casar. É útil porque dá sincronização pelos dados sem
> ficar fazendo polling: o consumidor só acorda quando tem trabalho de verdade,
> economiza CPU e evita busy-wait, e sai de graça um esquema produtor-consumidor
> onde os workers ficam ociosos até chegar tarefa.

---

### 1.5 Escalabilidade horizontal

1. Uma mesma tarefa foi processada por dois consumidores ao mesmo tempo?

> _Observado:_ Não. Rodamos com `--scale consumidor=2` e cada tarefa foi
> processada por exatamente um consumidor, os dois dividiram o trabalho sem
> repetir tarefa. Isso porque o take() remove a tupla de forma atômica, então só
> um consumidor consegue retirar cada uma.

2. O produtor precisou ser modificado para suportar dois consumidores?

> _Resposta:_ Não, nada. O produtor continuou escrevendo as mesmas tuplas do
> mesmo jeito. Adicionar consumidor é só subir mais uma instância.

3. Esse comportamento tem um nome em arquitetura de sistemas. Qual é?

> _Resposta:_ Escalabilidade horizontal, com o padrão de consumidores concorrentes
> (competing consumers / pool de workers): a gente escala a capacidade colocando
> mais workers no mesmo espaço, sem mexer em quem produz.

---

## Nível 2 — Modificar

### 2.1 Modificação A — Prioridade de tarefas

1. As tarefas de prioridade alta foram processadas antes das de prioridade baixa? Cole um trecho dos logs que evidencie isso:

```
produtor-1    | [PRODUTOR] Espaço encontrado via lookup.
produtor-1    | [PRODUTOR] write: TaskEntry{id=1, tipo="calcular", prioridade=2}
produtor-1    | [PRODUTOR] write: TaskEntry{id=2, tipo="calcular", prioridade=1}
produtor-1    | [PRODUTOR] write: TaskEntry{id=3, tipo="calcular", prioridade=2}
produtor-1    | [PRODUTOR] write: TaskEntry{id=4, tipo="calcular", prioridade=1}
produtor-1    | [PRODUTOR] write: TaskEntry{id=5, tipo="calcular", prioridade=1}
consumidor-1  | [CONSUMIDOR-1] take: TaskEntry{id=2, tipo="calcular", prioridade=1}
consumidor-1  | [CONSUMIDOR-1] Processamento concluído: tarefa 2
consumidor-1  | [CONSUMIDOR-1] take: TaskEntry{id=4, tipo="calcular", prioridade=1}
consumidor-1  | [CONSUMIDOR-1] Processamento concluído: tarefa 4
consumidor-1  | [CONSUMIDOR-1] take: TaskEntry{id=5, tipo="calcular", prioridade=1}
consumidor-1  | [CONSUMIDOR-1] Processamento concluído: tarefa 5
consumidor-1  | [CONSUMIDOR-1] Nenhuma tarefa de alta prioridade disponível. Buscando qualquer tarefa...
consumidor-1  | [CONSUMIDOR-1] take: TaskEntry{id=1, tipo="calcular", prioridade=2}
consumidor-1  | [CONSUMIDOR-1] Processamento concluído: tarefa 1
consumidor-1  | [CONSUMIDOR-1] take: TaskEntry{id=3, tipo="calcular", prioridade=2}
consumidor-1  | [CONSUMIDOR-1] Processamento concluído: tarefa 3
```

Dá pra ver as de prioridade 1 (id 2, 4 e 5) saindo antes das de prioridade 2 (id 1 e 3),
mesmo a id 1 tendo sido escrita primeiro. (Os logs saem agrupados por container, mas a
sequência de take deixa a ordem de prioridade clara.)

2. O produtor precisou ser modificado para que isso funcionasse?

> _Resposta:_ Não. A gente só mexeu no `consumidor/config.properties`, trocando
> `buscar_alta_prioridade_primeiro` de false pra true. O produtor escreve as
> mesmas tuplas na mesma ordem, quem decide o que consumir primeiro é o
> consumidor pelo template.

3. Como o consumidor consegue selecionar apenas tarefas de uma prioridade específica? Qual mecanismo do espaço de tuplas torna isso possível?

> _Resposta:_ Ele monta um template com prioridade=1 e os outros campos null
> (curinga) e chama take() com esse template. O espaço faz o casamento associativo
> (associative matching) e devolve só uma tupla que bate com esse padrão. O
> consumidor nunca precisa "ver" todas as tuplas nem varrer o espaço, o próprio
> espaço filtra por conteúdo. Esse casamento por template é o mecanismo central do
> modelo Linda.

---

### 2.2 Modificação B — Serviço monitor

1. Qual operação você usou no monitor — `read()` ou `take()`? Por quê a outra seria problemática?

> _Resposta:_ Usamos read(). O take() seria um problema porque ele remove a tupla
> do espaço, ou seja, o monitor estaria roubando tarefas dos consumidores só pra
> contar. O read() é não-destrutivo, ele olha a tupla e deixa ela no lugar, que é
> exatamente o que um monitor tem que fazer: observar sem interferir.

2. Como você contou as tarefas pendentes usando apenas `read()`? Que limitação isso revela?

> _Resposta:_ Como não existe count() e o read() com NO_WAIT pode devolver a mesma
> tupla várias vezes (o Outrigger não garante ordem nem variedade entre chamadas),
> a gente lê em loop com o template curinga e vai guardando os id já vistos num
> HashSet. Paramos depois de algumas leituras seguidas sem id novo e o total é o
> tamanho do conjunto. A limitação é clara: isso é uma heurística, não uma
> contagem exata. O read() não garante que a gente vai ver todas as tuplas, o
> espaço pode mudar no meio da contagem (alguém dando take), e não tem como pegar
> um "retrato" atômico de tudo que está lá. Contar bem não combina com a API pura
> do espaço.

3. Por que um espaço de tuplas puro não tem operação `count()`? O que seria necessário adicionar ao modelo para suportá-la?

> _Resposta:_ Porque o modelo Linda é uma memória associativa que trabalha com uma
> tupla por operação (out, in, rd casam UMA tupla por vez). Ele não se enxerga como
> uma coleção que você percorre nem oferece uma visão global do que tem dentro. Um
> count() precisaria de uma operação de varredura/agregação atômica sobre o espaço
> inteiro, um snapshot consistente, e isso quebra a semântica de "uma tupla por
> vez" e um pouco do desacoplamento. Pra suportar isso direito, teria que adicionar
> ao modelo algo como uma operação de scan/iteração sobre as tuplas ou um contador
> mantido pelo próprio servidor (metadados/índice), que é mais ou menos o que o
> Redis faz com o LLEN, mas aí em cima de uma estrutura concreta (uma lista), não
> de um espaço associativo puro.

Cole o trecho do `Monitor.java` que você completou:

```java
private static int contarTarefas(JavaSpace space) throws Exception {
    TaskEntry template = new TaskEntry(null, null, null);
    int count = 0;

    // read() e não take(): o monitor observa sem remover as tuplas.
    // Sem count() e com o read() podendo repetir a mesma tupla, contamos os
    // id distintos num conjunto e paramos depois de N leituras sem novidade.
    java.util.Set<Integer> idsVistos = new java.util.HashSet<>();
    int leiturasSemNovidade = 0;
    final int MAX_SEM_NOVIDADE = 10;

    while (leiturasSemNovidade < MAX_SEM_NOVIDADE) {
        TaskEntry encontrada = (TaskEntry) space.read(template, null, JavaSpace.NO_WAIT);
        if (encontrada == null) {
            break; // espaço vazio, não tem o que contar
        }
        if (idsVistos.add(encontrada.id)) {
            leiturasSemNovidade = 0;   // id novo
        } else {
            leiturasSemNovidade++;     // id repetido
        }
    }

    count = idsVistos.size();
    return count;
}
```

---

## Observações livres

_(Comportamentos inesperados, erros encontrados, dificuldades técnicas — descreva o que aconteceu e como você resolveu)_

> - O primeiro `docker compose up --build` demora bastante, porque cada imagem
>   baixa os jars do Apache River e compila o Java dentro do container. Nas
>   próximas vezes o cache do Docker ajuda.
> - Os logs do reggie e do javaspaces jogam um monte de INFO em inglês do próprio
>   framework, o que polui a saída. A gente foi olhando só as linhas com
>   [PRODUTOR], [CONSUMIDOR-1] e [MONITOR] pra acompanhar (o nome do consumidor
>   sai com sufixo por causa do NOME_CONSUMIDOR no compose).
> - A ordem de subida importa mesmo. Quando o reggie e o javaspaces ainda estavam
>   inicializando, o produtor e o consumidor ficaram um tempo no "Aguardando
>   espaço... (x/20)" antes de conectar, é normal.
> - O monitor mostrou "Tarefas pendentes: 1" enquanto tinha tarefa esperando e "0"
>   depois que o consumidor esvaziava. Como o consumidor consome quase no mesmo
>   ritmo que o produtor escreve, na maioria dos ticks de 3s tinha 0 ou 1 pendente.
>   A contagem é heurística (o read() pode repetir tupla e o espaço muda no meio),
>   então ela serve pra ter uma ideia, não como número exato.

---

## Dúvida para a próxima aula

_(Formule uma pergunta substantiva que surgiu durante a atividade)_

> As tuplas são escritas com um lease de 1 hora. O que acontece exatamente quando
> esse lease expira e ninguém tirou a tarefa: ela some sozinha do espaço? E existe
> algum jeito, dentro do Apache River, de fazer uma contagem consistente das
> tuplas (um snapshot de verdade) sem cair na heurística de ficar lendo em loop
> como a gente fez no monitor, ou isso realmente foge do que o modelo de espaço de
> tuplas se propõe a fazer?
