-- name: ExistsCompanyByName :one
select id_company
from tb_companies
where (unaccent(lower(company_name)) = $1 or lower(company_name) = $1) limit 1;

-- name: GetCompanies :many
select *
from tb_companies;

-- name: GetCompany :one
select *
from tb_companies
where id_company = $1 limit 1;

-- name: CreateCompany :one
INSERT INTO tb_companies (company_name)
VALUES ($1) RETURNING id_company;

-- name: ExistsDepositByName :one
select id_deposit
from tb_deposits
where (unaccent(lower(deposit_name)) = $1 or lower(deposit_name) = $1) limit 1;

-- name: GetDeposits :many
select *
from tb_deposits;

-- name: GetDeposit :one
select *
from tb_deposits
where id_deposit = $1 limit 1;

-- name: CreateDeposit :one
INSERT INTO tb_deposits (deposit_name, company_id)
values ($1, $2) RETURNING id_deposit;

-- name: ExistsByGroupName :one
select id_group
from tb_groups
where (unaccent(lower(group_name)) = $1 or lower(group_name) = $1) limit 1;

-- name: GetGroups :many
select *
from tb_groups;

-- name: GetGroup :one
select *
from tb_groups
where id_group = $1 limit 1;

-- name: CreateGroup :one
INSERT INTO tb_groups (group_name)
values ($1) RETURNING id_group;

-- name: ExistsTypeByName :one
select id_type
from tb_types
where (unaccent(lower(type_name)) = $1 or lower(type_name) = $1) limit 1;

-- name: GetTypes :many
select *
from tb_types;

-- name: GetType :one
select *
from tb_types
where id_type = $1 limit 1;

-- name: CreateType :one
INSERT INTO tb_types (id_group, type_name)
values ($1, $2) RETURNING id_type;

-- name: ExistsMaterialByName :one
select id_material
from tb_materials tm
         join tb_deposits td on td.id_deposit = tm.id_deposit
where (unaccent(Lower(tm.material_name)) = $1 or Lower(tm.material_name) = $1)
  and (unaccent(Lower(coalesce(tm.material_brand,''))) = coalesce($2, '') or Lower(coalesce(tm.material_brand,'')) = coalesce($2, ''))
  and (unaccent(Lower(coalesce(tm.material_power,''))) = coalesce($3, '')or Lower(coalesce(tm.material_power,'')) = coalesce($3, ''))
  and (unaccent(Lower(coalesce(tm.material_length,''))) = coalesce($4, '')or Lower(coalesce(tm.material_length,'')) = coalesce($4, ''))
  and (unaccent(Lower(td.deposit_name)) = $5 or Lower(td.deposit_name) = $5) limit 1;

-- name: GetMaterials :many
select *
from tb_materials;

-- name: GetMaterialsWithRelationship :many
select tm.material_name,
       tm.material_brand,
       tm.material_power,
       tm.material_amps,
       tm.material_length,
       tm.buy_unit,
       tm.request_unit,
       tt.type_name,
       tg.group_name,
       tc.company_name,
       td.deposit_name
from tb_materials tm
         join tb_types tt on tt.id_type = tm.id_material_type
         join tb_groups tg on tg.id_group = tt.id_group
         join tb_companies tc on tc.id_company = tm.id_company
         join tb_deposits td on td.id_deposit = tm.id_deposit;

-- name: GetMaterial :one
select *
from tb_materials
where id_material = $1 limit 1;

-- name: CreateMaterial :exec
INSERT INTO tb_materials (material_name, material_brand, material_power, material_amps, material_length, buy_unit,
                          request_unit, id_material_type, id_company, id_deposit)
values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10);