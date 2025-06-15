package com.lumos.lumosspring.execution.entities

import com.lumos.lumosspring.contract.entities.Contract
import com.lumos.lumosspring.stock.entities.ReservationManagement
import com.lumos.lumosspring.team.entities.Team
import com.lumos.lumosspring.user.User
import com.lumos.lumosspring.util.ExecutionStatus
import jakarta.persistence.*

@Table(name = "tb_direct_executions")
@Entity
class DirectExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "direct_execution_id")
    var directExecutionId: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractId", nullable = false)
    var contract = Contract()

    var directExecutionStatus: String = ExecutionStatus.WAITING_STOCKIST

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "directExecution", cascade = [CascadeType.ALL])
    var directItems: MutableList<DirectExecutionItem> = ArrayList()

    @ManyToOne
    @JoinColumn(name = "team_id")
    var team: Team = Team()

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    var assignedBy: User = User()

    @ManyToOne
    @JoinColumn(name = "reservation_management_id")
    var reservationManagement: ReservationManagement = ReservationManagement()
}
