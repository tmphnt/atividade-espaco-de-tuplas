# Relatório — Atividade Prática: Espaço de Tuplas com Apache River

**Disciplina:**
**Dupla:** /
**Data:**

---

## Nível 0 — Rodar e observar

### Observação inicial

1. Qual serviço aparece nos logs primeiro? Por que ele precisa existir antes dos outros?

> _Resposta:_

2. O produtor menciona o nome ou o endereço do consumidor em algum momento? O consumidor menciona o produtor?

> _Resposta:_

3. O consumidor começa a processar tarefas antes que o produtor termine de depositar todas? O que isso diz sobre como os dois se coordenam?

> _Resposta:_

---

### Experimento de desacoplamento temporal

4. O consumidor encontrou as tarefas mesmo sendo iniciado depois que o produtor já havia encerrado? O que isso demonstra?

> _Resposta:_

5. Em comunicação direta via socket, seria possível esse comportamento? Por quê não?

> _Resposta:_

---

## Nível 1 — Inspecionar

### 1.1 As três operações

Preencha a tabela com base no que você observou nos logs:

| Operação River | Equivalente Linda | O que ela faz? | Bloqueia quando não encontra correspondência? | Altera o estado do espaço? |
|---------------|-------------------|----------------|----------------------------------------------|---------------------------|
| `write(entry)` | `OUT` | | | |
| `take(template)` | `IN` | | | |
| `read(template)` | `RD` | | | |

---

### 1.2 O papel do `reggie`

1. Quando o `reggie` caiu, os serviços que já estavam conectados ao espaço continuaram funcionando? Por quê?

> _Resposta:_

2. O que aconteceria com um produtor ou consumidor que tentasse iniciar enquanto o `reggie` estivesse fora do ar?

> _Resposta:_

3. Qual sistema moderno cumpre papel equivalente ao `reggie` em uma arquitetura de microsserviços?

> _Resposta:_

---

### 1.3 Desacoplamento espacial

1. O produtor tem qualquer informação sobre quantos consumidores existem?

> _Resposta:_

2. O consumidor tem qualquer informação sobre quem produziu a tarefa que ele retirou?

> _Resposta:_

3. Como produtor e consumidor se coordenam se não se conhecem?

> _Resposta:_

---

### 1.4 Comportamento de bloqueio

1. O que o consumidor fez enquanto o espaço estava vazio?

> _Observado:_

2. Quando o produtor depositou a primeira tarefa, o que aconteceu imediatamente?

> _Observado:_

3. Esse comportamento tem nome no modelo Linda. Qual é e por que ele é útil em sistemas distribuídos reais?

> _Resposta:_

---

### 1.5 Escalabilidade horizontal

1. Uma mesma tarefa foi processada por dois consumidores ao mesmo tempo?

> _Observado:_

2. O produtor precisou ser modificado para suportar dois consumidores?

> _Resposta:_

3. Esse comportamento tem um nome em arquitetura de sistemas. Qual é?

> _Resposta:_

---

## Nível 2 — Modificar

### 2.1 Modificação A — Prioridade de tarefas

1. As tarefas de prioridade alta foram processadas antes das de prioridade baixa? Cole um trecho dos logs que evidencie isso:

```
(cole o trecho de log aqui)
```

2. O produtor precisou ser modificado para que isso funcionasse?

> _Resposta:_

3. Como o consumidor consegue selecionar apenas tarefas de uma prioridade específica? Qual mecanismo do espaço de tuplas torna isso possível?

> _Resposta:_

---

### 2.2 Modificação B — Serviço monitor

1. Qual operação você usou no monitor — `read()` ou `take()`? Por quê a outra seria problemática?

> _Resposta:_

2. Como você contou as tarefas pendentes usando apenas `read()`? Que limitação isso revela?

> _Resposta:_

3. Por que um espaço de tuplas puro não tem operação `count()`? O que seria necessário adicionar ao modelo para suportá-la?

> _Resposta:_

Cole o trecho do `Monitor.java` que você completou:

```java
// trecho relevante aqui
```

---

## Observações livres

_(Comportamentos inesperados, erros encontrados, dificuldades técnicas — descreva o que aconteceu e como você resolveu)_

>

---

## Dúvida para a próxima aula

_(Formule uma pergunta substantiva que surgiu durante a atividade)_

>
