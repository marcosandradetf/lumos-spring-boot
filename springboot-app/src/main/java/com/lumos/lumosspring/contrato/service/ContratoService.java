package com.lumos.lumosspring.contrato.service;

import com.lumos.lumosspring.contrato.controller.dto.ContratoRequest;
import com.lumos.lumosspring.contrato.entities.Contrato;
import com.lumos.lumosspring.contrato.entities.ContratoItem;
import com.lumos.lumosspring.contrato.repository.ContratoRepository;
import com.lumos.lumosspring.estoque.model.Material;
import com.lumos.lumosspring.estoque.repository.MaterialRepository;
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
public class ContratoService {
    private final ContratoRepository repository;
    private final MaterialRepository materialRepository;

    public ContratoService(ContratoRepository repository, MaterialRepository materialRepository) {
        this.repository = repository;
        this.materialRepository = materialRepository;
    }

    public ResponseEntity<String> criarContrato(ContratoRequest contratoReq) {
        var novoContrato = new Contrato();
        var date = ZonedDateTime.now(
                ZoneId.of( "America/Sao_Paulo" )
        );
        novoContrato.setCreationDate(date.toInstant());
        novoContrato.setNumeroContrato(contratoReq.numeroContrato());
        novoContrato.setCity(contratoReq.city());
        novoContrato.setUf(contratoReq.uf());
        int size = contratoReq.idMaterial().size();
        // três listas possuem o mesmo tamanho
        if (size != contratoReq.qtde().size() || size != contratoReq.valor().size()) {
            throw new IllegalArgumentException("As listas idMaterial, qtde e valor devem ter o mesmo tamanho.");
        }

        List<ContratoItem> listItens = IntStream.range(0, size)
                        .mapToObj(i -> {
                            ContratoItem ci = new ContratoItem();
                            Optional<Material> material = materialRepository.findById(contratoReq.idMaterial().get(i));

                            material.ifPresentOrElse(
                                    ci::setMaterial,
                                    () -> {
                                        throw new RuntimeException("Material não encontrado para o id: " + contratoReq.idMaterial().get(i));
                                    }
                            );
                            var value = contratoReq.valor().get(i).replace(",",".");
                            ci.setQuatidadeItem(contratoReq.qtde().get(i));
                            ci.setValorItem(new BigDecimal(value));
                            ci.setContrato(novoContrato);

                            return ci;
                        }).toList();

        novoContrato.setContratoItens(listItens);

        repository.save(novoContrato);
        return ResponseEntity.status(HttpStatus.CREATED).body("Contrato criado com sucesso");
    }



}
