package com.lumos.domain.model

data class RemoteConfigResponse(
    val forceUpdate: Boolean,
    val updateType: String,
    val minBuild: Long,
    val features: Map<String, Boolean>,
    val actions: List<RemoteAction>
)

data class RemoteAction(
    val id: String,
    val type: String, //CLEAR_TABLE, RUN_WORKER, SEND_PAYLOAD_INSTALLATION, SEND_PAYLOAD_MAINTENANCE, SEND_PAYLOAD_QUEUE
    val target: String, //sync_queue, stock, team
    val minAppBuild: Long? = null, //38L
    val conditions: Map<String, Any>? = null, // loggedIn: false
    val payload: Map<String, Any>? = null
)
//
//Regras importantes
//# id
//usado para idempotência (já resolvido no fetch)
//
//# type
//define o comando
//nunca confiar cegamente (whitelist)
//
//# target
//define o alvo
//nunca usar direto sem validação
//
//# minAppBuild
//bloqueia execução em versões antigas
//
//# conditions
//lógica booleana simples
//nunca código arbitrário
//
//# payload
//
//dados de entrada
//sempre tratado como não confiável