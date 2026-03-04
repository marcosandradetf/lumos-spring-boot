package com.lumos.lumosspring.report.dto.execution

import java.time.Instant

data class FiltersRequest(
    val contractIds: List<Long>,
    val materialTypesIds: List<Long>,
    val materialBrands: List<String>,
    val startDate: Instant,
    val endDate: Instant,
    val orientation: String
)