package com.lumos.lumosspring.contract.service

import com.lumos.lumosspring.stock.entities.Material
import com.lumos.lumosspring.stock.repository.MaterialRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.ArrayList

@Service
class MaterialService(
    private val materialRepository: MaterialRepository
) {

    fun getMaterials(): List<Material> {
        val materials = mutableListOf<Material>()

        // Pega todos os materiais, excluindo "PARAFUSO" e "CONECTOR"
        materials.addAll(materialRepository.findAllMaterialsExcludingScrewAndConnector())

        // Pega um material de cada tipo "PARAFUSO" e "CONECTOR"
        materials.addAll(materialRepository.findOneScrewAndConnector())

        return materials
    }


}
