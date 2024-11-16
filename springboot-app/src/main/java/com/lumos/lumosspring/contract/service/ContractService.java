package com.lumos.lumosspring.contract.service;

import com.lumos.lumosspring.contract.controller.dto.ContractRequest;
import com.lumos.lumosspring.contract.entities.Contract;
import com.lumos.lumosspring.execution.entities.Item;
import com.lumos.lumosspring.contract.repository.ContratoRepository;
import com.lumos.lumosspring.stock.entities.Material;
import com.lumos.lumosspring.stock.repository.MaterialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
public class ContractService {
    private final ContratoRepository repository;
    private final MaterialRepository materialRepository;

    public ContractService(ContratoRepository repository, MaterialRepository materialRepository) {
        this.repository = repository;
        this.materialRepository = materialRepository;
    }

    public ResponseEntity<String> createContract(ContractRequest contractReq) {
        var newContract = new Contract();
        var date = ZonedDateTime.now(
                ZoneId.of( "America/Sao_Paulo" )
        );
        newContract.setCreationDate(date.toInstant());
        newContract.setContractNumber(contractReq.numeroContrato());
        newContract.setCity(contractReq.city());
        newContract.setUf(contractReq.uf());
        int size = contractReq.idMaterial().size();
        // três listas possuem o mesmo tamanho
        if (size != contractReq.qtde().size() || size != contractReq.valor().size()) {
            throw new IllegalArgumentException("As listas idMaterial, qtde e valor devem ter o mesmo tamanho.");
        }

        List<Item> listItens = IntStream.range(0, size)
                        .mapToObj(i -> {
                            Item ci = new Item();
                            Optional<Material> material = materialRepository.findById(contractReq.idMaterial().get(i));

                            material.ifPresentOrElse(
                                    ci::setMaterial,
                                    () -> {
                                        throw new RuntimeException("Material não encontrado para o id: " + contractReq.idMaterial().get(i));
                                    }
                            );
                            var value = contractReq.valor().get(i).replace(",",".");
                            ci.setItemQuantity(contractReq.qtde().get(i));
                            ci.setItemValue(new BigDecimal(value));
                            ci.setContract(newContract);

                            return ci;
                        }).toList();

        newContract.setItemsContract(listItens);

        repository.save(newContract);
        return ResponseEntity.status(HttpStatus.CREATED).body("Contrato criado com sucesso");
    }



}
