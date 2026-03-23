package com.lumos.lumosspring.lead.repository

import com.lumos.lumosspring.lead.model.LeadRequest
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface LeadRequestRepository : CrudRepository<LeadRequest, Long> {

}