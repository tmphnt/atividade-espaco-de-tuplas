# Atividade Prática: Espaço de Tuplas com Apache River

## Pré-requisitos

- [Git](https://git-scm.com/)
- [Docker](https://docs.docker.com/get-docker/) com [Docker Compose](https://docs.docker.com/compose/)

Verifique com:
```bash
docker compose version
```

Nenhuma instalação de Java é necessária — o JDK 8 e todas as dependências do Apache River estão dentro dos contêineres.

---

## Material Teórico

Os slides do professor são a fonte principal e podem ser encontrados no Moodle da disciplina; o [arquivo de contexto teórico](./contexto-teorico.md) é um complemento.

---

## Contexto histórico

Em 1985, David Gelernter propôs o modelo de **espaço de tuplas**: uma memória associativa compartilhada onde processos depositam e retiram dados sem se conhecerem diretamente. Em 1999, a Sun Microsystems materializou esse modelo no **JavaSpaces**, parte do projeto Jini. O **Apache River** é o sucessor direto desse trabalho — um sistema de middleware distribuído que foi usado em produção em sistemas reais.

Esta atividade coloca você em contato com uma ferramenta histórica real. O ambiente requer Docker porque Apache River não é um processo único: ele depende de múltiplos serviços coordenados, com ordem de inicialização e rede compartilhada. Você vai ver essa complexidade de infraestrutura como parte da experiência.

---

## Objetivos

Ao final desta atividade, você será capaz de:

1. Explicar as operações `write` (OUT), `take` (IN) e `read` (RD) de um espaço de tuplas e a diferença entre leitura destrutiva e não-destrutiva.
2. Identificar o **desacoplamento espacial** e o **desacoplamento temporal**: por que produtor e consumidor não precisam se conhecer e não precisam estar ativos ao mesmo tempo.
3. Reconhecer o papel de um **serviço de descoberta** (`reggie`) na coordenação de componentes distribuídos sem endereços fixos.
4. Conectar o modelo de espaço de tuplas às abstrações modernas de mensageria e registros de serviços (são diferentes, mas tem semelhanças).

---

## Estrutura do projeto

```
atividade-espaco-de-tuplas/
├── docker-compose.yml         ← orquestra os serviços
├── infra/
│   ├── reggie/                ← serviço de descoberta Jini
│   └── javaspaces/            ← servidor do espaço de tuplas (Outrigger)
├── produtor/                  ← deposita tarefas com write()
├── consumidor/                ← retira e processa tarefas com take()
└── monitor/                   ← observa o espaço com read() [Nível 2]
```

### Topologia da rede

```
         ┌─────────────────────────────────────────┐
         │              rede-tuplas                │
         │                                         │
         │   ┌─────────┐      ┌────────────────┐   │
         │   │  reggie │◄─────│  javaspaces    │   │
         │   │ (lookup)│      │  (espaço de    │   │
         │   └────┬────┘      │   tuplas)      │   │
         │        │           └───────┬────────┘   │
         │        │  descoberta       │ write/take  │
         │   ┌────┴──────────────────┴────────┐    │
         │   │   produtor      consumidor      │    │
         │   └────────────────────────────────┘    │
         └─────────────────────────────────────────┘
```

Observe a topologia: produtor e consumidor **não têm conexão direta entre si**. Ambos se comunicam exclusivamente com o espaço de tuplas, que descobrem através do `reggie`. Isso é desacoplamento espacial em prática.

---

## Nível 0 — Rodar

Execute:

```bash
docker compose up --build
```

Aguarde todos os serviços iniciarem. O `reggie` inicia primeiro; o `javaspaces` se registra nele; somente então o produtor e o consumidor se conectam.

Você verá logs intercalados de todos os serviços. Exemplo do que esperar:

```
reggie-1     | [REGGIE] HTTP codebase server iniciado na porta 8080.
reggie-1     | INFO: started Reggie: ..., [rede-tuplas], ...
javaspaces-1 | [JAVASPACES] HTTP codebase server iniciado na porta 8080.
javaspaces-1 | INFO: Outrigger server started: ...
consumidor-1 | [CONSUMIDOR] Aguardando espaço... (1/20)
produtor-1   | [PRODUTOR] Aguardando espaço... (1/20)
consumidor-1 | [CONSUMIDOR] Espaço encontrado. Aguardando tarefas...
produtor-1   | [PRODUTOR] Espaço encontrado via lookup.
produtor-1   | [PRODUTOR] write: TaskEntry{id=1, tipo="calcular", prioridade=2}
consumidor-1 | [CONSUMIDOR] take: TaskEntry{id=1, tipo="calcular", prioridade=2}
produtor-1   | [PRODUTOR] write: TaskEntry{id=2, tipo="calcular", prioridade=1}
consumidor-1 | [CONSUMIDOR] Processamento concluído: tarefa 1
consumidor-1 | [CONSUMIDOR] take: TaskEntry{id=2, tipo="calcular", prioridade=1}
produtor-1   | [PRODUTOR] Todas as tarefas depositadas. Encerrando.
```

> **Nota:** Os logs do `reggie` e do `javaspaces` incluem mensagens de INFO em inglês do próprio framework — isso é normal. Foque nas linhas prefixadas com `[PRODUTOR]` e `[CONSUMIDOR]`.

**Observe e responda (anote no relatório):**

1. Qual serviço aparece nos logs primeiro? Por que ele precisa existir antes dos outros?
2. O produtor menciona o nome ou o endereço do consumidor em algum momento? O consumidor menciona o produtor?
3. O consumidor começa a processar tarefas antes que o produtor termine de depositar todas? O que isso diz sobre como os dois se coordenam?

Encerre com `Ctrl+C`.

---

## Nível 0 — Experimento de desacoplamento temporal

Antes de passar ao Nível 1, faça este experimento:

**Passo 1:** Inicie apenas o espaço de tuplas (sem consumidor):
```bash
docker compose up reggie javaspaces produtor
```
Aguarde o produtor terminar de depositar todas as tarefas e encerrar. Encerre com `Ctrl+C`.

**Passo 2:** Agora inicie apenas o consumidor:
```bash
docker compose up consumidor
```

**Observe e responda:**

4. O consumidor encontrou as tarefas mesmo sendo iniciado depois que o produtor já havia encerrado? O que isso demonstra?
5. Em comunicação direta via socket (como você viu em atividades anteriores), seria possível esse comportamento? Por quê?

---

## Nível 1 — Inspecionar

### 1.1 As três operações

O espaço de tuplas expõe três operações. Preencha a tabela no relatório para cada uma, baseando-se no que você observou nos logs:

| Operação River | Equivalente Linda | O que ela faz? | Bloqueia quando não encontra correspondência? | Altera o estado do espaço? |
|---------------|-------------------|----------------|----------------------------------------------|---------------------------|
| `write(entry)` | `OUT` | | | |
| `take(template)` | `IN` | | | |
| `read(template)` | `RD` | | | |

### 1.2 O papel do `reggie`

O `reggie` é o serviço de descoberta Jini. Ele resolve um problema fundamental: como o produtor encontra o espaço de tuplas sem ter o endereço IP dele fixo no código?

**Experimento:** Inicie todos os serviços normalmente. Depois, em outro terminal, pare o `reggie`:

```bash
docker compose stop reggie
```

Observe o que acontece com os serviços que já estão rodando. Depois reinicie:

```bash
docker compose start reggie
```

**Responda:**

1. Quando o `reggie` caiu, os serviços que já estavam conectados ao espaço continuaram funcionando? Por quê?
2. O que aconteceria com um produtor ou consumidor que tentasse iniciar enquanto o `reggie` estivesse fora do ar?
3. Qual sistema moderno cumpre papel equivalente ao `reggie` em uma arquitetura de microsserviços? (Dica: pense em Consul ou Kubernetes.)

### 1.3 Desacoplamento espacial nos logs

Releia os logs do Nível 0 com atenção.

**Responda:**

1. O produtor tem qualquer informação sobre quantos consumidores existem?
2. O consumidor tem qualquer informação sobre quem produziu a tarefa que ele retirou?
3. Como produtor e consumidor se coordenam, então, se não se conhecem?

### 1.4 Comportamento de bloqueio

**Experimento:** Inicie o consumidor **antes** do produtor:

```bash
docker compose up reggie javaspaces consumidor
```

Espere 10 segundos observando os logs. Depois, em outro terminal:

```bash
docker compose up produtor
```

**Responda:**

1. O que o consumidor fez enquanto o espaço estava vazio?
2. Quando o produtor depositou a primeira tarefa, o que aconteceu imediatamente?
3. Esse comportamento tem nome no modelo Linda. Qual é e por que ele é útil em sistemas distribuídos reais?

### 1.5 Escalabilidade horizontal

**Experimento:** Execute com dois consumidores simultâneos:

```bash
docker compose up --scale consumidor=2
```

**Responda:**

1. Uma mesma tarefa foi processada por dois consumidores ao mesmo tempo? Ou cada tarefa foi processada por exatamente um consumidor?
2. O produtor precisou ser modificado para suportar dois consumidores?
3. Esse comportamento — adicionar consumidores sem alterar o produtor — tem um nome em arquitetura de sistemas. Qual é?

---

## Nível 2 — Modificar

### Modificação A — Prioridade de tarefas (guiada)

As tarefas estão definidas em `Produtor.java` (array `TAREFAS`), com campos `id`, `tipo` e `prioridade`. Atualmente o consumidor retira qualquer tarefa disponível, independente da prioridade.

Abra `consumidor/config.properties` e altere o valor:

```properties
# Altere de: false
# Para: true
buscar_alta_prioridade_primeiro=true
```

Execute novamente:

```bash
docker compose up --build
```

**Observe e responda:**

1. As tarefas de prioridade alta (valor `1`) foram processadas antes das de prioridade baixa (valor `2`)?
2. O produtor precisou ser modificado para que isso funcionasse?
3. Como o consumidor consegue selecionar apenas tarefas de uma prioridade específica sem ver todas as tarefas no espaço? Qual mecanismo do espaço de tuplas torna isso possível?

### Modificação B — Serviço monitor (aberta)

Sem modificar o produtor nem o consumidor, adicione um serviço `monitor` que a cada 3 segundos exibe quantas tarefas pendentes existem no espaço.

O arquivo `monitor/Monitor.java` está parcialmente implementado. Localize o comentário:

```java
// TODO: use read() ou take() aqui — qual você deve usar e por quê?
```

Preencha a lacuna com a operação correta e descomente o bloco `monitor` no `docker-compose.yml` (ele já está lá, comentado).

Execute e verifique se o monitor exibe contagens sem interferir no processamento das tarefas.

**Responda no relatório:**

1. Qual operação você usou no monitor — `read()` ou `take()`? Por quê a outra seria problemática?
2. O Apache River não tem uma operação `count()`. Como você contou as tarefas pendentes usando apenas `read()`? Que limitação isso revela?
3. Sistemas modernos como Redis expõem `LLEN` (contagem de fila). Por que um espaço de tuplas puro não tem essa operação? O que seria necessário adicionar ao modelo para suportá-la?

---

## Entregável

1. Faça um *fork* (ou clone) deste repositório.
2. Complete os Níveis 1 e 2, incluindo as modificações nos arquivos indicados.
3. Preencha o `relatorio-template.md` com suas respostas.
4. Envie o link do repositório com seus commits (ou o arquivo `.zip` do projeto com o relatório preenchido), conforme orientação do professor.

---

## Dúvidas

Abra uma *issue* neste repositório ou traga sua pergunta para a próxima aula.
