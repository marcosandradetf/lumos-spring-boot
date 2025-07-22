package com.lumos.lumosspring.contract.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("contract_reference_item")
class ContractReferenceItem {
    @Id
    var contractReferenceItemId: Long? = null

    var description: String = ""

    @Column("name_for_import")
    var nameForImport: String? = null

    var type: String = ""

    var linking: String? = null

    var itemDependency: String? = null
}
