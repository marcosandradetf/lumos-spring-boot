package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.authentication.model.TenantEntity
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("contract_reference_item")
class ContractReferenceItem : TenantEntity() {
    @Id
    var contractReferenceItemId: Long? = null

    var description: String = ""

    @Column("name_for_import")
    var nameForImport: String? = null

    var type: String = ""

    var linking: String? = null

    var itemDependency: String? = null
}
