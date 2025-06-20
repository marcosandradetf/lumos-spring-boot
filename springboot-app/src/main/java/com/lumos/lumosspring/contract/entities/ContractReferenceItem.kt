package com.lumos.lumosspring.contract.entities

import com.lumos.lumosspring.stock.entities.Material
import jakarta.persistence.*
import org.jetbrains.annotations.NotNull
import java.util.HashSet

@Entity
class ContractReferenceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var contractReferenceItemId: Long = 0
    @NotNull
    var description : String = ""
    @Column(columnDefinition = "TEXT", name = "name_for_import")
    var nameForImport : String? = null
    @NotNull
    var type : String = ""
    var linking : String? = null
    var itemDependency : String? = null

    @OneToMany(mappedBy = "referenceItem", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var contractsItems: MutableSet<ContractItem> = HashSet()

    @OneToMany(mappedBy = "contractReferenceItem", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var materials: MutableSet<Material> = HashSet()

}