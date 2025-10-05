package com.lumos.lumosspring.premeasurement.service.premeasurement;

import com.lumos.lumosspring.premeasurement.dto.premeasurement.DelegateDTO;
import com.lumos.lumosspring.premeasurement.dto.premeasurement.DelegateStreetDTO;
import com.lumos.lumosspring.premeasurement.repository.premeasurement.PreMeasurementManagementRepository;
import com.lumos.lumosspring.premeasurement.repository.premeasurement.PreMeasurementRepository;
import com.lumos.lumosspring.stock.order.model.ReservationManagement;
import com.lumos.lumosspring.stock.order.repository.ReservationManagementRepository;
import com.lumos.lumosspring.util.DefaultResponse;
import com.lumos.lumosspring.util.ReservationStatus;
import com.lumos.lumosspring.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreMeasurementManagementService {
    private final PreMeasurementRepository preMeasurementRepository;
    private final ReservationManagementRepository reservationManagementRepository;
    private final PreMeasurementManagementRepository managementRepository;

    public PreMeasurementManagementService(
            PreMeasurementRepository preMeasurementRepository,
            ReservationManagementRepository reservationManagementRepository,
            PreMeasurementManagementRepository managementRepository
    ) {
        this.preMeasurementRepository = preMeasurementRepository;
        this.reservationManagementRepository = reservationManagementRepository;
        this.managementRepository = managementRepository;
    }

    @Transactional
    public ResponseEntity<?> delegateToStockist(DelegateDTO delegateDTO) {

        var existingManagement = preMeasurementRepository
                .hasManagement(
                        delegateDTO.getPreMeasurementId()
                );

        if (existingManagement) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new DefaultResponse("Já existe uma gestão de reserva para esse estoquista e essas ruas."));
        }

        var management = new ReservationManagement(
                null,
                delegateDTO.getDescription(),
                delegateDTO.getStockistId(),
                ReservationStatus.PENDING
        );

        management = reservationManagementRepository.save(management);

        if(management.getReservationManagementId() == null){
            throw new Utils.BusinessException("Não foi possível obter a o gerenciamento de estoque");
        }

        managementRepository.delegatePreMeasurementToExecution(
                delegateDTO.getPreMeasurementId(),
                delegateDTO.getTeamId(),
                management.getReservationManagementId(),
                delegateDTO.getComment(),
                delegateDTO.getStreet().stream()
                        .filter(DelegateStreetDTO::getPrioritized)
                        .map(DelegateStreetDTO::getPreMeasurementStreetId)
                        .toList()

        );

        return ResponseEntity.ok().build();
    }



}
