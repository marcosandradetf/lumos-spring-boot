package com.lumos.lumosspring.metrics

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class MetricsRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    data class DashboardMetricDTO(
        val label: String,
        val value: Long,
        val classification: String,
        val routerLink: String?,
        val queryParams: String?,
        val description: String
    )

    fun findDashboardMetrics(tenantId: UUID): List<DashboardMetricDTO> {
        val sql =
            """
            WITH
-- 🔴 ALERTAS CRÍTICOS ---------------------------------------------

pre_medicoes_pendentes AS (SELECT COUNT(*) AS value
                           FROM pre_measurement
                           WHERE status = 'PENDING'
                             AND tenant_id = :tenantId),

ordens_servico_pendentes AS (SELECT COUNT(*) AS value
                             FROM reservation_management
                             WHERE status = 'PENDING'
                               AND tenant_id = :tenantId),

instalacoes_em_curso AS (
                        SELECT COUNT(*) AS value
                        FROM pre_measurement
                        WHERE status = 'AVAILABLE_EXECUTION'
                          AND available_at < NOW() - INTERVAL '2 days'
                          AND available_at >= NOW() - INTERVAL '7 days'
                          AND tenant_id = :tenantId
                    
                        UNION ALL
                    
                        SELECT COUNT(*) AS value
                        FROM direct_execution
                        WHERE direct_execution_status = 'AVAILABLE_EXECUTION'
                          AND available_at < NOW() - INTERVAL '2 days'
                          AND available_at >= NOW() - INTERVAL '7 days'
                          AND tenant_id = :tenantId
                    ),


instalacoes_paradas AS (SELECT COUNT(*) AS value
                        FROM pre_measurement
                        WHERE status = 'AVAILABLE_EXECUTION'
                          AND available_at <= NOW() - INTERVAL '7 days'
                          AND tenant_id = :tenantId
                        UNION ALL
                        SELECT COUNT(*) AS value
                        FROM direct_execution
                        WHERE direct_execution_status = 'AVAILABLE_EXECUTION'
                          AND available_at <= NOW() - INTERVAL '7 days'
                          AND tenant_id = :tenantId),

contratos_baixo_saldo AS (SELECT COUNT(*) AS value
                          FROM contract c
                          WHERE tenant_id = :tenantId
                            AND EXISTS (SELECT 1
                                        FROM contract_item ci
                                        WHERE ci.contract_contract_id = c.contract_id
                                          AND (ci.contracted_quantity - ci.quantity_executed) <= 15)),

contratos_saldo_zerado AS (SELECT COUNT(*) AS value
                           FROM contract c
                           WHERE tenant_id = :tenantId
                             AND EXISTS (SELECT 1
                                         FROM contract_item ci
                                         WHERE ci.contract_contract_id = c.contract_id
                                           AND (ci.contracted_quantity - ci.quantity_executed) <= 0)),

materiais_baixo_estoque AS (SELECT COUNT(*) AS value
                            FROM material_stock
                            WHERE stock_available <= 15
                              AND tenant_id = :tenantId),

materiais_zerados AS (SELECT COUNT(*) AS value
                      FROM material_stock
                      WHERE stock_available <= 0
                        AND tenant_id = :tenantId),

-- 🟡 FLUXO OPERACIONAL ---------------------------------------------

manutencoes_concluidas_30_dias AS (SELECT COUNT(*) AS value
                                   FROM maintenance
                                   WHERE status = 'FINISHED'
                                     AND finished_at >= NOW() - INTERVAL '30 days'
                                     AND tenant_id = :tenantId),

instalacoes_concluidas_30_dias AS (SELECT COUNT(*) AS value
                                   FROM direct_execution
                                   WHERE direct_execution_status = 'FINISHED'
                                     AND finished_at >= NOW() - INTERVAL '30 days'
                                     AND tenant_id = :tenantId
                                   UNION ALL
                                   SELECT COUNT(*) AS value
                                   FROM pre_measurement
                                   WHERE status = 'FINISHED'
                                     AND finished_at >= NOW() - INTERVAL '30 days'
                                     AND tenant_id = :tenantId),

-- 🔵 USO DO SISTEMA ------------------------------------------------

relatorios_visualizados_30_dias AS (SELECT COUNT(*) AS value
                                    FROM maintenance
                                    WHERE report_view_at >= NOW() - INTERVAL '30 days'
                                      AND tenant_id = :tenantId
                                    UNION ALL
                                    SELECT COUNT(*) AS value
                                    FROM direct_execution
                                    WHERE report_view_at >= NOW() - INTERVAL '30 days'
                                      AND tenant_id = :tenantId
                                    UNION ALL
                                    SELECT COUNT(*) AS value
                                    FROM pre_measurement
                                    WHERE report_view_at >= NOW() - INTERVAL '30 days'
                                      AND tenant_id = :tenantId)

-- 🔚 RESULTADO FINAL -----------------------------------------------

SELECT label,
       value,
       classification,
       router_link,
       query_params,
       description
FROM (
         -- 🔴 ALERTAS
         SELECT 'Pré-medições'          AS label,
                p.value                 AS value,
                'Ação imediata'         AS classification,
                '/pre-medicao/pendente' AS router_link,
                NULL::jsonb             AS query_params,
                'Em aberto'             as description
         FROM pre_medicoes_pendentes p

         UNION ALL
         SELECT 'Ordens de serviço',
                o.value,
                'Ação imediata',
                '/metrica/ordem-servico/pendentes',
                NULL::jsonb,
                'Em aberto' as description
         FROM ordens_servico_pendentes o

         UNION ALL
         SELECT 'Instalações',
                sum(i.value) as value,
                'Monitorar',
                NULL,
                NULL::jsonb,
                'Em curso'   as description
         FROM instalacoes_em_curso i

         UNION ALL
         SELECT 'Instalações',
                sum(i.value) as value,
                'Crítico',
                '/metrica/instalacoes/pendentes',
                NULL::jsonb,
                'Paradas há mais de 7 dias'    as description
         FROM instalacoes_paradas i

         UNION ALL
         SELECT 'Contratos',
                c.value,
                'Crítico',
                '/contratos/listar',
                '{
                  "for": "noBalance"
                }'::jsonb,
                'Com saldo zerado' as description
         FROM contratos_saldo_zerado c

         UNION ALL
         SELECT 'Contratos',
                c.value,
                'Atenção',
                '/contratos/listar',
                '{
                  "for": "lowerBalance"
                }'::jsonb,
                'Com baixo saldo' as description
         FROM contratos_baixo_saldo c

         UNION ALL
         SELECT 'Materiais',
                m.value,
                'Crítico',
                '/estoque/movimentar-estoque',
                NULL::jsonb,
                'Sem estoque' as description
         FROM materiais_zerados m

         UNION ALL
         SELECT 'Materiais',
                m.value,
                'Atenção',
                '/estoque/movimentar-estoque',
                NULL::jsonb,
                'Com baixo estoque' as description
         FROM materiais_baixo_estoque m

         -- 🟡 FLUXO
         UNION ALL
         SELECT 'Manutenções',
                m.value,
                'Últimos 30 dias',
                '/metrica/manutencoes/concluidas',
                NULL::jsonb,
                'Concluídas nos últimos 30 dias' as description
         FROM manutencoes_concluidas_30_dias m

         UNION ALL
         SELECT 'Instalações',
                sum(i.value) as value,
                'Últimos 30 dias',
                '/metrica/instalacoes/concluidas',
                NULL::jsonb,
                'Concluídas nos últimos 30 dias' as description
         FROM instalacoes_concluidas_30_dias i

         -- 🔵 USO
         UNION ALL
         SELECT 'Relatórios',
                sum(r.value) as value,
                'Últimos 30 dias',
                NULL,
                NULL::jsonb,
                'Visualizados nos últimos 30 dias' as description
         FROM relatorios_visualizados_30_dias r) dashboard_metrics;
        """

        val params = mapOf("tenantId" to tenantId)

        return jdbcTemplate.query(sql, params) { rs, _ ->
            DashboardMetricDTO(
                label = rs.getString("label"),
                value = rs.getLong("value"),
                classification = rs.getString("classification"),
                routerLink = rs.getString("router_link"),
                queryParams = rs.getString("query_params"),
                description = rs.getString("description")
            )
        }
    }
}
