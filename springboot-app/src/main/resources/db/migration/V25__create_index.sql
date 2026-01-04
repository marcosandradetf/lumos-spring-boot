create index if not exists material_tenant_index
    on material (tenant_id);

create index if not exists material_bar_code_index
    on material (barcode);

create index if not exists contract_reference_item_tenant_index
    on contract_reference_item (tenant_id);

create index if not exists app_user_tenant_index
    on app_user (tenant_id);

create index if not exists contract_tenant_index
    on contract (tenant_id);

create index if not exists company_tenant_index
    on company (tenant_id);

create index if not exists maintenance_tenant_index
    on maintenance (tenant_id);

create index if not exists material_stock_tenant_index
    on material_stock (tenant_id);

create index if not exists team_tenant_index
    on team (tenant_id);

create index if not exists deposit_tenant_index
    on deposit (tenant_id);

create index if not exists direct_execution_tenant_index
    on direct_execution (tenant_id);

create index if not exists pre_measurement_tenant_index
    on pre_measurement (tenant_id);

create index if not exists reservation_management_tenant_index
    on reservation_management (tenant_id);

create index if not exists material_reservation_tenant_index
    on material_reservation (tenant_id);

create index if not exists material_history_tenant_index
    on material_history (tenant_id);