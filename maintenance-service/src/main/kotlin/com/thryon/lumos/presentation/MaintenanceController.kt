package com.thryon.lumos.presentation

import com.thryon.lumos.application.MaintenanceService
import com.thryon.lumos.infrastructure.repository.MaintenanceRepository
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.inject.Inject
import org.eclipse.microprofile.jwt.JsonWebToken

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MaintenanceController @Inject constructor(
    private val maintenanceService: MaintenanceService
) {
    @Inject
    lateinit var jwt: JsonWebToken

    @POST
    @Path("/maintenance/debit-stock")
    fun debitStock(items: List<MaintenanceRepository.MaintenanceStreetItemDTO>): Response {
        maintenanceService.debitStockForMaintenance(items)
        return Response.ok().build()
    }

    @GET
    @Path("/maintenance/get-finished")
    fun getGroupedMaintenances():Response {
        return Response.ok(maintenanceService.getGroupedMaintenances()).build()
    }

}
