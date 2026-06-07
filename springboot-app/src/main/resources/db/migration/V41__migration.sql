--criar index na tabela material para parent_material (caso nao exista)
create index if not exists idx_material_parent_material_id
    on material(parent_material_id);
