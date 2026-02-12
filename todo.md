# TODO
- Ao cadastrar materiais obrigar selecionar item contratual apenas quando o tipo conter nos tipos de referência
- migration v26 também setou factor de 6.5 e etc para serviço de braço. (comentado)
- Pre-medição nao aparecendo apos liberar para instalação (erro no proxy da cloud-flare ao tentar buscar foto)
- # 5️⃣ Único ajuste que eu RECOMENDO (muito sutil)

	Hoje você tem:

	`LaunchedEffect(Unit, notifications) {     if (loggedIn) {         notificationViewModel.loadNotifications()     } }`

	⚠️ Isso **executa sempre que `notifications` muda**, o que pode virar loop.

	Mas isso é **outro assunto**, não Remote Config.

- Corrigir erro ao gerar relatorio escolhendo list e depois group (front)
- Realizar Correcao da quantidade de pontos ![[Pasted image 20260203205742.png]]
- Modificar payload instalações (direta e pre-medição) enviar startedAt
- Criar index caso não exista
	CREATE INDEX idx_direct_execution_contract_item
	ON direct_execution_item(contract_item_id);
	CREATE INDEX idx_pre_measurement_contract_item
	ON pre_measurement_street_item(contract_item_id);

- Versão mobile para tela de contratos e itens