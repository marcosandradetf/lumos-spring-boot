package com.lumos.data.database

import androidx.room.Dao
import androidx.room.Query
import com.lumos.domain.model.InstallationView
import kotlinx.coroutines.flow.Flow

@Dao
interface ViewDao {
    @Query("""
            SELECT 
                p.preMeasurementId AS id,
                'PreMeasurementInstallation' AS type,
                p.contractId as contractId,
                p.contractor AS contractor,
                p.status AS executionStatus,
                p.creationDate AS creationDate,
                (
                    SELECT COUNT(*) 
                    FROM PreMeasurementInstallationStreet s
                    WHERE s.preMeasurementId = p.preMeasurementId
                ) AS streetsQuantity,
                (
                    SELECT COUNT(*) 
                    FROM PreMeasurementInstallationStreet s
                    JOIN PreMeasurementInstallationItem i
                        ON s.preMeasurementStreetId = i.preMeasurementStreetId
                    WHERE s.preMeasurementId = p.preMeasurementId
                ) AS itemsQuantity
            FROM PreMeasurementInstallation p
            WHERE p.status = :status
        
            UNION ALL
        
            SELECT 
                CAST(d.directExecutionId AS TEXT) AS id,
                'direct_execution' AS type,
                d.contractId as contractId,
                d.description AS contractor,
                d.executionStatus AS executionStatus,
                d.creationDate AS creationDate,
                (
                    SELECT COUNT(*) 
                    FROM direct_execution_street s
                    WHERE s.directExecutionId = d.directExecutionId
                ) AS streetsQuantity,
                (
                    SELECT COUNT(*) 
                    FROM direct_execution_street s
                    JOIN direct_execution_street_item i
                        ON s.directStreetId = i.directStreetId
                    WHERE s.directExecutionId = d.directExecutionId
                ) AS itemsQuantity
            FROM direct_execution d
            WHERE d.executionStatus in (:status)
        """)
    fun getInstallationsHolderByStatus(status: List<String>): Flow<List<InstallationView>>



}


