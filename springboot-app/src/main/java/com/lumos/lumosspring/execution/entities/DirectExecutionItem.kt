package com.lumos.lumosspring.execution.entities

import com.lumos.lumosspring.contract.entities.ContractItemsQuantitative
import jakarta.persistence.*

@Table(name = "tb_direct_executions_items")
@Entity
class DirectExecutionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "direct_execution_item_id")
    var directExecutionItemId: Long = 0

    var measuredItemQuantity: Double = 0.0

    @ManyToOne
    @JoinColumn(name = "contract_item_id")
    var contractItem: ContractItemsQuantitative = ContractItemsQuantitative()

    @ManyToOne
    @JoinColumn(name = "direct_execution_id")
    var directExecution = DirectExecution()
}