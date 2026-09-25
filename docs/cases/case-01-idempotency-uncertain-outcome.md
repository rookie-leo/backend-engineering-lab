# Case 01 — Idempotência e resultado incerto

## Status

**Case:** 01  
**Nome:** Idempotência e resultado incerto  
**Domínio:** Pagamentos / integrações financeiras  
**Status:** Em desenvolvimento  
**Solução:** Não definida

---

## 1. Objetivo

Este case tem como objetivo estudar o comportamento de uma aplicação quando uma operação financeira depende de um sistema externo e o resultado dessa operação nem sempre pode ser determinado de forma imediata.

A ausência de uma resposta conclusiva não significa necessariamente que a operação falhou.

O laboratório deve permitir reproduzir e observar diferentes comportamentos de uma integração externa, distinguindo:

- operação concluída com sucesso;
- operação rejeitada;
- erro explícito do provider;
- ausência de resposta dentro do tempo esperado;
- recurso inexistente.

A primeira prática não tem como objetivo implementar idempotência ou definir uma solução definitiva para o problema.

O objetivo inicial é reproduzir os cenários de forma determinística e entender quais informações estão efetivamente disponíveis para a aplicação em cada situação.

---

## 2. Contexto

Uma aplicação disponibiliza uma API para realização de pagamentos.

A aplicação não executa diretamente o processamento financeiro. Para isso, ela se comunica via HTTP com um provider externo.

Fluxo simplificado:

```text
Client
   │
   │ POST /payments
   ▼
Payment API
   │
   │ HTTP
   ▼
External Payment Provider
```

Em condições normais:

```text
Client
   │
   │ solicita pagamento
   ▼
Payment API
   │
   │ solicita processamento
   ▼
Provider
   │
   │ processa operação
   │
   │ responde
   ▼
Payment API
   │
   │ interpreta resposta
   ▼
Client
```

Porém, a comunicação entre sistemas distribuídos está sujeita a falhas, atrasos e respostas inesperadas.

A Payment API não controla o comportamento interno do provider nem a rede existente entre os dois sistemas.

Portanto, sua decisão deve ser baseada apenas nas informações que consegue observar.

---

## 3. Problema

Considere que a Payment API possua um limite de tempo para aguardar uma resposta do provider.

Exemplo inicial:

```text
Payment API timeout = 2 segundos
```

Para determinada operação, o provider demora mais do que esse limite para responder.

Exemplo:

```text
Provider response time = 5 segundos
```

O fluxo observado pela aplicação será:

```text
T+0s

Payment API
     │
     │ request
     ▼
Provider


T+2s

Payment API
     │
     X timeout
     │
     ▼
precisa continuar seu fluxo


T+5s

Provider
     │
     │ response
     ▼
resposta ocorre depois do prazo
esperado pela Payment API
```

Quando o timeout ocorre, a Payment API não recebeu uma resposta conclusiva dentro do período em que estava disposta a esperar.

Do ponto de vista da aplicação:

```text
resultado = UNKNOWN
```

Esse estado representa falta de informação suficiente.

A aplicação não pode concluir automaticamente:

```text
pagamento falhou
```

nem:

```text
pagamento foi processado
```

Existe incerteza sobre o resultado da operação.

---

## 4. Erro conhecido do provider

Um segundo cenário ocorre quando o provider responde explicitamente com erro.

Exemplo:

```text
Payment API
     │
     │ request
     ▼
Provider
     │
     │ HTTP 500
     ▼
Payment API
```

Nesse caso, diferentemente do timeout, a Payment API recebeu uma resposta dentro do período esperado.

Ela sabe que:

```text
Provider respondeu
+
A resposta recebida representa um erro
```

Portanto:

```text
PROVIDER_ERROR != TIMEOUT
```

Embora ambos impeçam a conclusão normal do fluxo, a quantidade e a natureza da informação disponível para a aplicação são diferentes.

Uma das finalidades da primeira prática é tornar essa diferença observável.

---

## 5. Pergunta central

O Case 01 parte da seguinte pergunta:

> Como uma aplicação deve lidar com resultados conhecidos e incertos ao depender de um sistema externo para executar uma operação financeira?

A primeira prática reduz essa pergunta para:

> Qual é a diferença observável entre uma resposta explícita de erro do provider e uma operação cuja resposta não foi obtida dentro do timeout da aplicação?

Neste momento, o laboratório não define qual estratégia deverá ser utilizada para solucionar o problema.

---

## 6. Invariante de negócio

O laboratório parte do conceito de uma intenção financeira.

```text
Payment Intent
     │
     ▼
tentativa de processamento
```

Uma mesma intenção poderá gerar diferentes tentativas de comunicação ao longo de sua execução.

Entretanto, o sistema deverá futuramente buscar preservar a seguinte propriedade:

> Uma mesma intenção de pagamento não deve produzir efeitos financeiros duplicados devido a retries ou incerteza sobre tentativas anteriores.

Conceitualmente:

```text
1 Payment Intent
        │
        ▼
uma ou mais tentativas
        │
        ▼
no máximo 1 efeito financeiro esperado
```

A intenção de pagamento não deve ser identificada simplesmente através de propriedades como:

```text
accountId + amount
```

Duas operações legítimas podem possuir exatamente os mesmos valores.

Exemplo:

```text
Payment Intent A

accountId = ACC-0001
amount = 150.00


Payment Intent B

accountId = ACC-0001
amount = 150.00
```

Ainda assim, representam duas intenções financeiras distintas.

A estratégia utilizada para identificar uma Payment Intent está fora do escopo da primeira prática.

---

## 7. Arquitetura inicial do experimento

A primeira versão do laboratório possuirá três participantes:

```text
┌──────────────────┐
│      Client      │
│                  │
│ curl/script/test │
└────────┬─────────┘
         │
         │ HTTP
         ▼
┌──────────────────┐
│   Payment API    │
│                  │
│ Java             │
│ Spring Boot      │
└────────┬─────────┘
         │
         │ HTTP
         ▼
┌──────────────────┐
│ Payment Provider │
│     Sandbox      │
│                  │
│ WireMock         │
└──────────────────┘
```

O laboratório deve permanecer propositalmente pequeno.

Não serão adicionadas tecnologias que não sejam necessárias para reproduzir o problema estudado.

---

## 8. Fronteiras do laboratório

### 8.1 Componentes reais

Os seguintes comportamentos fazem parte do sistema que queremos exercitar:

- aplicação Java;
- Spring Boot;
- endpoint HTTP;
- HTTP Client;
- serialização e desserialização;
- timeout;
- tratamento de exceptions;
- interpretação de HTTP status;
- configuração da aplicação;
- execução em ambiente local;
- testes.

Esses elementos devem executar de forma real durante o experimento.

### 8.2 Componentes simulados

O seguinte componente será simulado:

```text
External Payment Provider
```

O comportamento interno de um provider real não faz parte do problema que queremos implementar.

Precisamos apenas de uma dependência externa cujo comportamento possa ser reproduzido deterministicamente.

Para isso será utilizado um sandbox.

---

## 9. Payment Provider Sandbox

O provider externo será representado inicialmente através de WireMock executado em Docker.

O sandbox deverá utilizar massas determinísticas.

O comportamento não deverá ser controlado através de informações artificiais adicionadas exclusivamente ao contrato da Payment API, como:

```http
X-Test-Scenario: TIMEOUT
```

Em vez disso, determinadas massas conhecidas produzirão comportamentos conhecidos.

Conceitualmente:

```text
accountId
    │
    ▼
sandbox mapping
    │
    ▼
comportamento configurado
```

Isso permite que diferentes cenários sejam reproduzidos simplesmente utilizando outra massa na requisição.

---

## 10. Massas iniciais

A primeira versão do sandbox deverá fornecer os seguintes cenários:

| Account ID | Cenário | Comportamento |
|---|---|---|
| `ACC-0001` | `SUCCESS` | Provider responde sucesso dentro do prazo |
| `ACC-0002` | `DECLINED` | Provider rejeita a operação dentro do prazo |
| `ACC-0003` | `SLOW_RESPONSE` | Provider responde depois do timeout configurado na Payment API |
| `ACC-0004` | `PROVIDER_ERROR` | Provider responde explicitamente com erro |
| não cadastrado | `NOT_FOUND` | Nenhum recurso correspondente é encontrado |

Os identificadores representam massas de teste.

Eles poderão ser refinados posteriormente sem alterar a finalidade dos cenários.

---

## 11. Cenário SUCCESS

Massa:

```text
ACC-0001
```

Comportamento esperado:

```text
Payment API
     │
     │ request
     ▼
Sandbox
     │
     │ SUCCESS
     ▼
Payment API
```

A resposta deve ocorrer dentro do timeout configurado.

Esse cenário funciona como controle positivo do experimento.

---

## 12. Cenário DECLINED

Massa:

```text
ACC-0002
```

Comportamento esperado:

```text
Payment API
     │
     │ request
     ▼
Sandbox
     │
     │ DECLINED
     ▼
Payment API
```

A comunicação foi realizada normalmente, mas a operação não foi aprovada.

Esse cenário é diferente de uma falha técnica.

---

## 13. Cenário SLOW_RESPONSE

Massa:

```text
ACC-0003
```

Configuração inicial sugerida:

```text
Payment API timeout = 2 segundos

Sandbox response time = 5 segundos
```

Fluxo:

```text
T+0s

Payment API
     │
     │ request ACC-0003
     ▼
Sandbox


T+2s

Payment API
     │
     X timeout
     │
     ▼
continua seu fluxo


T+5s

Sandbox
     │
     │ response
     ▼
resposta produzida depois
do limite da Payment API
```

Quando a Payment API atinge seu timeout, ela não deve assumir conhecimento sobre o estado interno do provider.

O que ela sabe é apenas:

> Não foi possível obter uma resposta conclusiva dentro do período configurado.

Esse é o principal cenário de resultado incerto da primeira prática.

---

## 14. Cenário PROVIDER_ERROR

Massa:

```text
ACC-0004
```

Comportamento esperado:

```text
Payment API
     │
     │ request
     ▼
Sandbox
     │
     │ HTTP error
     ▼
Payment API
```

A resposta deve ocorrer antes do timeout.

Esse cenário permitirá comparar:

```text
PROVIDER_ERROR
```

com:

```text
SLOW_RESPONSE / TIMEOUT
```

No primeiro caso, uma resposta foi recebida.

No segundo, nenhuma resposta conclusiva foi obtida dentro do prazo esperado.

---

## 15. Cenário NOT_FOUND

Uma massa que não esteja cadastrada no sandbox deverá resultar em recurso não encontrado.

Exemplo:

```text
ACC-9999
```

Fluxo:

```text
accountId desconhecido
        │
        ▼
nenhum recurso/mapping correspondente
        │
        ▼
NOT_FOUND
```

Esse cenário ajuda a garantir que o comportamento do sandbox seja determinado pelas massas existentes.

---

## 16. Estrutura inicial do sandbox

Estrutura prevista:

```text
sandbox/
└── payment-provider/
    ├── mappings/
    │   ├── payment-success.json
    │   ├── payment-declined.json
    │   ├── payment-slow-response.json
    │   └── payment-provider-error.json
    │
    └── __files/
```

O sandbox será executado através de Docker.

As configurações deverão ser versionadas juntamente com o laboratório para garantir que qualquer pessoa consiga reproduzir os mesmos cenários.

---

## 17. Contrato inicial da Payment API

Contrato conceitual:

```http
POST /payments
Content-Type: application/json
```

Request:

```json
{
  "accountId": "ACC-0003",
  "amount": 1500.00,
  "currency": "BRL"
}
```

Neste momento ainda não estão definidos:

- response DTO definitivo;
- HTTP status definitivo da Payment API para cada cenário;
- modelo de persistência;
- identificação definitiva da Payment Intent;
- estratégia de recuperação;
- estratégia de retry;
- estratégia de idempotência.

Essas decisões deverão surgir durante a investigação, e não ser assumidas antecipadamente.

---

## 18. Fluxo inicial da aplicação

A primeira implementação deverá possuir apenas o fluxo necessário para realizar o experimento.

Conceitualmente:

```text
receber request
       │
       ▼
validar entrada
       │
       ▼
chamar provider
       │
       ▼
aguardar dentro do timeout
       │
       ▼
interpretar resultado observado
       │
       ▼
responder ao client
```

Não deverá existir inicialmente uma estratégia sofisticada de recuperação.

Primeiro o problema deve ser reproduzido e compreendido.

---

## 19. Comportamentos que queremos observar

Ao final da primeira prática, o laboratório deverá permitir observar:

```text
ACC-0001
→ provider respondeu sucesso


ACC-0002
→ provider respondeu rejeição


ACC-0003
→ nenhuma resposta foi obtida dentro do prazo


ACC-0004
→ provider respondeu explicitamente com erro


accountId desconhecido
→ recurso não encontrado
```

A comparação mais importante inicialmente será:

```text
ACC-0003 != ACC-0004
```

Os dois cenários representam falhas no fluxo esperado, mas fornecem informações diferentes para a Payment API.

---

## 20. Reprodutibilidade

O laboratório deverá permitir que outra pessoa reproduza os cenários sem precisar alterar o código-fonte.

Objetivo de execução:

```bash
docker compose up
```

seguido da execução da aplicação e de uma chamada conhecida ou script de reprodução.

Futuramente poderá existir algo semelhante a:

```bash
./scripts/run-case-01.sh
```

O fluxo desejado é:

```text
clone repository
       │
       ▼
start infrastructure
       │
       ▼
start application
       │
       ▼
execute scenario
       │
       ▼
observe behavior
```

Os mesmos inputs devem produzir comportamentos previsíveis no sandbox.

---

## 21. Critérios de conclusão da primeira prática

A primeira prática será considerada concluída quando conseguirmos reproduzir deterministicamente:

- [ ] `SUCCESS`
- [ ] `DECLINED`
- [ ] `SLOW_RESPONSE`
- [ ] `PROVIDER_ERROR`
- [ ] `NOT_FOUND`

Além disso, devemos conseguir demonstrar tecnicamente a diferença entre:

```text
provider respondeu explicitamente com erro
```

e:

```text
não obtivemos uma resposta dentro do nosso prazo
```

Não é necessário resolver o problema de resultado incerto para concluir esta etapa.

A reprodução correta do problema já faz parte do resultado do experimento.

---

## 22. Fora do escopo da primeira prática

Os seguintes assuntos não fazem parte da implementação inicial:

- retry automático;
- idempotência;
- persistência de Payment Intent;
- concorrência;
- webhook;
- polling;
- consulta de status;
- processamento assíncrono;
- Kafka;
- RabbitMQ;
- SQS;
- reconciliação;
- Outbox;
- DLQ;
- Circuit Breaker;
- load testing;
- Kubernetes;
- AWS.

Esses itens não estão descartados.

Eles somente não serão adicionados antes que exista um problema concreto que justifique sua utilização.

---

## 23. Investigações futuras

O resultado incerto pode ser tratado através de diferentes estratégias, dependendo das capacidades oferecidas pelo provider e dos requisitos do sistema.

As seguintes possibilidades já foram identificadas para experimentos futuros.

### 23.1 Idempotência

Permitir que múltiplas tentativas relacionadas à mesma intenção não produzam efeitos financeiros duplicados.

Essa é uma das principais estratégias que serão investigadas neste case, mas sua implementação não está definida nesta etapa.

### 23.2 Consulta de status

Alguns providers permitem consultar posteriormente uma operação através de um identificador.

Conceitualmente:

```text
POST /charges
      │
      ▼
timeout
      │
      ▼
resultado desconhecido
      │
      ▼
GET /charges/{id}
      │
      ▼
status atual
```

Possíveis estados poderiam incluir, dependendo do contrato do provider:

```text
PENDING
PROCESSED
PAID
FAILED
```

Os estados definitivos não fazem parte da especificação atual.

### 23.3 Webhook / callback

O provider pode comunicar posteriormente o resultado da operação.

```text
Payment API
     │
     │ request
     ▼
Provider

...

Provider
     │
     │ webhook
     ▼
Payment API
```

Nesse modelo, a resposta síncrona original não precisa necessariamente ser a única fonte de informação sobre o resultado.

### 23.4 Processamento assíncrono

A própria Payment API pode trabalhar com um estado intermediário.

Conceitualmente:

```text
Client
   │
   │ payment request
   ▼
Payment API
   │
   │ PROCESSING
   ▼
Client

...

processamento continua
```

O resultado definitivo seria disponibilizado posteriormente.

### 23.5 Eventos

Caso o provider disponibilize integração assíncrona, o resultado poderá chegar através de mensageria.

```text
Provider
    │
    ▼
Topic
    │
    ▼
Consumer
    │
    ▼
Payment state update
```

### 23.6 Reconciliação

Um processo posterior pode comparar estados conhecidos pela aplicação com informações disponíveis no provider.

Isso pode permitir identificar operações que permaneceram em estado desconhecido ou inconsistente.

### 23.7 Combinação de estratégias

As estratégias anteriores não são necessariamente mutuamente exclusivas.

Uma arquitetura futura poderia combinar, por exemplo:

```text
idempotência
     +
estado intermediário
     +
webhook
     +
consulta de status
     +
reconciliação
```

Qual combinação é apropriada depende dos requisitos e das capacidades da integração.

Nenhuma dessas possibilidades é considerada solução obrigatória neste momento.

---

## 24. Evolução do case

Este documento deverá evoluir junto com os experimentos.

Possíveis investigações futuras incluem:

```text
Case 01 — Idempotência e resultado incerto
        │
        ├── Timeout vs erro conhecido
        │
        ├── Retry
        │
        ├── Idempotência
        │
        ├── Requests concorrentes
        │
        ├── Persistência de estado
        │
        ├── Restart durante processamento
        │
        ├── Consulta de status
        │
        ├── Webhook
        │
        ├── Processamento assíncrono
        │
        └── Reconciliação
```

Essa lista representa possibilidades de investigação e não um roadmap obrigatório.

Novos experimentos deverão ser adicionados somente quando houver uma pergunta concreta a ser respondida.

---

## 25. Registro da investigação

As seções abaixo serão preenchidas conforme o laboratório evoluir.

### Observações

_TBD — preencher após a reprodução dos primeiros cenários._

### Decisões técnicas

_TBD — registrar decisões somente quando houver um problema ou trade-off concreto que as justifique._

### Alternativas consideradas

_TBD — registrar alternativas efetivamente avaliadas durante a investigação._

### Solução investigada

_TBD — nenhuma solução foi definida nesta etapa._

### Testes

_TBD — registrar os testes implementados e o comportamento que cada um procura demonstrar._

### Métricas

_TBD — registrar medições quando elas passarem a fazer parte dos experimentos._

### Trade-offs

_TBD — registrar consequências observadas das decisões tomadas._

### Limitações

_TBD — registrar situações que o experimento não cobre._

### Conclusões

_TBD — preencher somente após a execução e análise dos experimentos._