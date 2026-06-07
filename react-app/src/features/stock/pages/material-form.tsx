import { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { materialApi } from '@/features/stock/api/material-api';
import { stockApi } from '@/features/stock/api/stock-api';
import { useTypeSubtypes } from '@/features/stock/hooks/use-type-subtypes';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { GlassListbox } from '@/shared/components/glass-list-box';
import type { MaterialTypeSubtype, UnitOption } from '@/features/stock/types/types';

interface FormState {
  materialId: number | null;
  materialType: number | null;
  materialSubtype: number | null;
  barcode: string;
  materialFunction: string;
  materialModel: string;
  materialBrand: string;
  materialAmps: string;
  materialLength: string;
  materialWidth: string;
  materialPower: string;
  materialGauge: string;
  materialWeight: string;
  inactive: boolean;
  buyUnit: string;
  requestUnit: string;
  truckStockControl: boolean;
}

const EMPTY: FormState = {
  materialId: null,
  materialType: null,
  materialSubtype: null,
  barcode: '',
  materialFunction: '',
  materialModel: '',
  materialBrand: '',
  materialAmps: '',
  materialLength: '',
  materialWidth: '',
  materialPower: '',
  materialGauge: '',
  materialWeight: '',
  inactive: false,
  buyUnit: '',
  requestUnit: '',
  truckStockControl: true,
};

function computeName(form: FormState, types: MaterialTypeSubtype[], subtypes: Array<{ subtypeId: number; subtypeName: string }>) {
  const typeName = types.find(t => t.typeId === form.materialType)?.typeName ?? '';
  const subtypeName = subtypes.find(s => s.subtypeId === form.materialSubtype)?.subtypeName ?? '';

  const join = (...parts: string[]) =>
    parts.filter(Boolean).join(' ').replace(/\s+/g, ' ').trim().toUpperCase();

  const baseName = join(typeName, subtypeName, form.materialFunction, form.materialModel,
    form.materialAmps, form.materialLength, form.materialWidth, form.materialPower,
    form.materialGauge, form.materialWeight);

  const fullName = join(typeName, subtypeName, form.materialFunction, form.materialModel,
    form.materialBrand, form.materialAmps, form.materialLength, form.materialWidth,
    form.materialPower, form.materialGauge, form.materialWeight);

  return { baseName, fullName };
}

const VALID_BARCODE_LENGTHS = [8, 12, 13, 14];

function validate(form: FormState, subtypes: Array<{ subtypeId: number }>) {
  const errors: Partial<Record<keyof FormState, string>> = {};
  if (!form.barcode || !VALID_BARCODE_LENGTHS.includes(form.barcode.length)) {
    errors.barcode = 'Informe um código de barras com 8, 12, 13 ou 14 dígitos.';
  }
  if (!form.materialType) errors.materialType = 'Tipo é obrigatório.';
  if (subtypes.length > 0 && !form.materialSubtype) errors.materialSubtype = 'Subtipo é obrigatório.';
  if (!form.materialBrand) errors.materialBrand = 'Marca é obrigatória.';
  if (!form.buyUnit) errors.buyUnit = 'Unidade de compra é obrigatória.';
  if (!form.requestUnit) errors.requestUnit = 'Unidade de requisição é obrigatória.';
  return errors;
}

interface FieldProps {
  label: string;
  required?: boolean;
  error?: string;
  children: React.ReactNode;
}

function Field({ label, required, error, children }: FieldProps) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-sm font-medium text-slate-700 dark:text-zinc-200">
        {label}{required && <span className="text-red-500 ml-0.5">*</span>}
      </label>
      {children}
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
}

const inputClass = 'w-full rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 px-3 py-2.5 text-sm text-slate-800 dark:text-zinc-100 placeholder:text-slate-400 outline-none focus:border-indigo-400 transition-colors disabled:bg-slate-50 dark:disabled:bg-zinc-800 disabled:cursor-not-allowed';

export default function MaterialForm() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();

  const [form, setForm] = useState<FormState>(EMPTY);
  const [touched, setTouched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [subtypes, setSubtypes] = useState<Array<{ subtypeId: number; subtypeName: string }>>([]);
  const [buyUnits, setBuyUnits] = useState<UnitOption[]>([]);
  const [requestUnits, setRequestUnits] = useState<Array<UnitOption & { truckStockControl: boolean }>>([]);

  const materialId = searchParams.get('materialId');
  const isEdit = !!materialId;

  useEffect(() => {
    setPageContext(
      ['Estoque', isEdit ? 'Editar Material' : 'Cadastro de Materiais'],
      isEdit ? 'Editar Material' : 'Cadastro de Materiais',
    );
  }, [setPageContext, isEdit]);

  const { data: allTypes = [] } = useTypeSubtypes();

  // Edit mode: load existing material
  useEffect(() => {
    if (!materialId) return;
    setLoading(true);
    materialApi.findById(materialId)
      .then(data => {
        setForm({
          materialId: data.materialId,
          materialType: data.materialType,
          materialSubtype: data.materialSubtype,
          barcode: data.barcode ?? '',
          materialFunction: data.materialFunction ?? '',
          materialModel: data.materialModel ?? '',
          materialBrand: String(data.materialBrand ?? ''),
          materialAmps: data.materialAmps !== null ? String(data.materialAmps) : '',
          materialLength: data.materialLength !== null ? String(data.materialLength) : '',
          materialWidth: data.materialWidth !== null ? String(data.materialWidth) : '',
          materialPower: data.materialPower !== null ? String(data.materialPower) : '',
          materialGauge: data.materialGauge !== null ? String(data.materialGauge) : '',
          materialWeight: data.materialWeight !== null ? String(data.materialWeight) : '',
          inactive: data.inactive,
          buyUnit: data.buyUnit ?? '',
          requestUnit: data.requestUnit ?? '',
          truckStockControl: data.truckStockControl,
        });
        if (data.materialType) {
          const typeSubs = (allTypes as MaterialTypeSubtype[]).find(t => t.typeId === data.materialType)?.subtypes ?? [];
          setSubtypes(typeSubs);
        }
      })
      .catch(() => notify('Material não encontrado.', 'error'))
      .finally(() => setLoading(false));
  }, [materialId, notify]); // allTypes intentionally omitted — loaded async

  const handleTypeChange = useCallback(async (typeId: number) => {
    setForm(prev => ({ ...prev, materialType: typeId, materialSubtype: null, buyUnit: '', requestUnit: '' }));
    const typeSubs = (allTypes as MaterialTypeSubtype[]).find(t => t.typeId === typeId)?.subtypes ?? [];
    setSubtypes(typeSubs);
    try {
      const units = await stockApi.findUnitsByTypeId(typeId);
      setBuyUnits(units.buyUnits);
      setRequestUnits(units.requestUnits);
      if (units.buyUnits.length === 1) {
        setForm(prev => ({ ...prev, buyUnit: units.buyUnits[0].code }));
      }
      if (units.requestUnits.length === 1) {
        setForm(prev => ({ ...prev, requestUnit: units.requestUnits[0].code, truckStockControl: units.requestUnits[0].truckStockControl }));
      }
    } catch {
      notify('Erro ao carregar unidades para este tipo.', 'error');
    }
  }, [allTypes, notify]);

  const handleBarcodeBlur = useCallback(async (value: string) => {
    if (!VALID_BARCODE_LENGTHS.includes(value.length)) return;
    setLoading(true);
    try {
      const data = await materialApi.findByBarcode(value);
      if (form.materialId === null) {
        setForm(prev => ({
          ...prev,
          materialType: data.materialType,
          materialSubtype: data.materialSubtype,
          materialFunction: data.materialFunction ?? '',
          materialModel: data.materialModel ?? '',
          materialBrand: data.materialBrand ?? '',
          materialAmps: data.materialAmps !== null ? String(data.materialAmps) : '',
          materialLength: data.materialLength !== null ? String(data.materialLength) : '',
          materialWidth: data.materialWidth !== null ? String(data.materialWidth) : '',
          materialPower: data.materialPower !== null ? String(data.materialPower) : '',
          materialGauge: data.materialGauge !== null ? String(data.materialGauge) : '',
          materialWeight: data.materialWeight !== null ? String(data.materialWeight) : '',
          inactive: data.inactive,
          buyUnit: data.buyUnit ?? '',
          requestUnit: data.requestUnit ?? '',
        }));
        notify('Material encontrado. Ajuste as informações antes de salvar.', 'info');
      } else {
        setForm(prev => ({ ...prev, barcode: '' }));
        notify('Este código já está cadastrado. Modifique o material existente para utilizá-lo.', 'info');
      }
    } catch {
      notify('Nenhum material encontrado com este código. Continue o cadastro normalmente.', 'info');
    } finally {
      setLoading(false);
    }
  }, [form.materialId, notify]);

  const { baseName, fullName } = useMemo(
    () => computeName(form, allTypes as MaterialTypeSubtype[], subtypes),
    [form, allTypes, subtypes],
  );

  const errors = useMemo(() => touched ? validate(form, subtypes) : {}, [form, subtypes, touched]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setTouched(true);
    const errs = validate(form, subtypes);
    if (Object.keys(errs).length > 0) return;

    setLoading(true);
    try {
      const toNum = (s: string) => s ? parseFloat(s) : null;
      await materialApi.create({
        materialId: form.materialId,
        materialBaseName: baseName,
        materialName: fullName,
        materialType: form.materialType,
        materialSubtype: form.materialSubtype,
        materialFunction: form.materialFunction || null,
        materialModel: form.materialModel || null,
        materialBrand: form.materialBrand || null,
        materialAmps: toNum(form.materialAmps),
        materialLength: toNum(form.materialLength),
        materialWidth: toNum(form.materialWidth),
        materialPower: toNum(form.materialPower),
        materialGauge: toNum(form.materialGauge),
        materialWeight: toNum(form.materialWeight),
        barcode: form.barcode,
        inactive: form.inactive,
        buyUnit: form.buyUnit,
        requestUnit: form.requestUnit,
        truckStockControl: form.truckStockControl,
        contractItems: [],
      });
      setSubmitted(true);
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Não foi possível salvar o material.';
      notify(message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setForm(EMPTY);
    setTouched(false);
    setSubmitted(false);
    setSubtypes([]);
    setBuyUnits([]);
    setRequestUnits([]);
  };

  if (submitted) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center p-6">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 p-8 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30">
            <i className="pi pi-check text-2xl text-emerald-600 dark:text-emerald-400" />
          </div>
          <h2 className="text-xl font-semibold text-slate-800 dark:text-zinc-100">Material cadastrado com sucesso</h2>
          <p className="mt-2 text-sm text-slate-500 dark:text-zinc-400">O material já está disponível no estoque.</p>
          <div className="mt-6 flex flex-col gap-2">
            <button type="button" onClick={resetForm}
              className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-500 transition-colors">
              Cadastrar outro material
            </button>
            <button type="button" onClick={() => navigate('/estoque/catalogo-materiais')}
              className="rounded-xl border border-slate-200 dark:border-zinc-700 px-4 py-2.5 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors">
              Ir para o catálogo
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <section className="p-4 md:p-6">
      <div className="mx-auto max-w-4xl space-y-6">
        <h1 className="text-2xl font-semibold text-slate-800 dark:text-zinc-100">
          {isEdit ? 'Editar Material' : 'Cadastro de Material'}
        </h1>

        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-6 relative">
          <LoadingOverlay loading={loading} />

          {/* Barcode */}
          <Field label="Código de Barras (EAN/GTIN)" required error={errors.barcode}>
            <input
              type="text"
              inputMode="numeric"
              value={form.barcode}
              maxLength={14}
              onChange={(e) => setForm(prev => ({ ...prev, barcode: e.target.value.replace(/\D/g, '') }))}
              onBlur={(e) => void handleBarcodeBlur(e.target.value)}
              placeholder="Digite ou escaneie o código de barras"
              className={inputClass}
            />
          </Field>

          {/* Type / Subtype */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Field label="Tipo" required error={errors.materialType}>
              <GlassListbox
                value={form.materialType}
                onChange={(value) => {
                  if (value === null) {
                    setForm((previous) => ({
                      ...previous,
                      materialType: null,
                      materialSubtype: null,
                      buyUnit: '',
                      requestUnit: '',
                    }));
                    setSubtypes([]);
                    setBuyUnits([]);
                    setRequestUnits([]);
                    return;
                  }

                  void handleTypeChange(Number(value));
                }}
                placeholder="Selecione o tipo"
                options={[
                  { value: null, label: 'Selecione o tipo' },
                  ...(allTypes as MaterialTypeSubtype[]).map((type) => ({
                    value: type.typeId,
                    label: type.typeName,
                  })),
                ]}
              />
            </Field>

            <Field label="Subtipo" error={errors.materialSubtype}>
              {!form.materialType ? (
                <input className={inputClass} disabled value="Selecione um tipo primeiro" readOnly />
              ) : subtypes.length === 0 ? (
                <input className={inputClass} disabled value="Sem subtipos disponíveis" readOnly />
              ) : (
                <GlassListbox
                  value={form.materialSubtype}
                  onChange={(value) =>
                    setForm((previous) => ({
                      ...previous,
                      materialSubtype: value === null ? null : Number(value),
                    }))
                  }
                  placeholder="Selecione o subtipo"
                  options={[
                    { value: null, label: 'Selecione o subtipo' },
                    ...subtypes.map((subtype) => ({
                      value: subtype.subtypeId,
                      label: subtype.subtypeName,
                    })),
                  ]}
                />
              )}
            </Field>
          </div>

          {/* Generated name */}
          <Field label="Nome gerado automaticamente">
            <input className={inputClass} disabled value={fullName || '—'} readOnly />
          </Field>

          {/* Technical specs grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
            {(
            [
              { key: 'materialFunction', label: 'Função técnica/Referência', placeholder: 'Ex.: DERIVAÇÃO, M16' },
              { key: 'materialModel', label: 'Modelo/Tecnologia', placeholder: 'Ex.: 105-305V' },
              { key: 'materialBrand', label: 'Marca', placeholder: 'Ex.: Osram, Elgin', required: true },
              { key: 'materialAmps', label: 'Corrente (A)', placeholder: 'Ex.: 16A' },
              { key: 'materialLength', label: 'Comprimento', placeholder: 'Ex.: 240MM' },
              { key: 'materialWidth', label: 'Largura', placeholder: 'Ex.: 120MM' },
              { key: 'materialPower', label: 'Potência (W)', placeholder: 'Ex.: 200W' },
              { key: 'materialGauge', label: 'Seção/Bitola', placeholder: 'Ex.: 1.5MM²' },
              { key: 'materialWeight', label: 'Peso', placeholder: 'Ex.: 50KG' },
            ] as Array<{ key: keyof Pick<FormState, 'materialFunction' | 'materialModel' | 'materialBrand' | 'materialAmps' | 'materialLength' | 'materialWidth' | 'materialPower' | 'materialGauge' | 'materialWeight'>; label: string; placeholder: string; required?: boolean }>
          ).map(({ key, label, placeholder, required }) => (
              <Field key={key} label={label} required={required} error={errors[key] as string | undefined}>
                <input
                  type="text"
                  value={form[key]}
                  onChange={(e) => setForm(prev => ({ ...prev, [key]: e.target.value }))}
                  placeholder={placeholder}
                  className={inputClass}
                />
              </Field>
            ))}
          </div>

          {/* Units */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Field label="Unidade de Compra" required error={errors.buyUnit}>
              {!form.materialType ? (
                <input className={inputClass} disabled value="Selecione um tipo primeiro" readOnly />
              ) : (
                <GlassListbox
                  value={form.buyUnit}
                  onChange={(value) => setForm((previous) => ({ ...previous, buyUnit: String(value ?? '') }))}
                  placeholder="Selecione a unidade"
                  options={[
                    { value: '', label: 'Selecione a unidade' },
                    ...buyUnits.map((unit) => ({
                      value: unit.code,
                      label: unit.code,
                    })),
                  ]}
                />
              )}
            </Field>

            <Field label="Unidade de Requisição" required error={errors.requestUnit}>
              {!form.materialType ? (
                <input className={inputClass} disabled value="Selecione um tipo primeiro" readOnly />
              ) : (
                <GlassListbox
                  value={form.requestUnit}
                  onChange={(value) => {
                    const code = String(value ?? '');
                    const unit = requestUnits.find((requestUnit) => requestUnit.code === code);
                    setForm((previous) => ({
                      ...previous,
                      requestUnit: code,
                      truckStockControl: unit?.truckStockControl ?? true,
                    }));
                  }}
                  placeholder="Selecione a unidade"
                  options={[
                    { value: '', label: 'Selecione a unidade' },
                    ...requestUnits.map((unit) => ({
                      value: unit.code,
                      label: unit.code,
                    })),
                  ]}
                />
              )}
            </Field>
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={resetForm}
              className="rounded-xl border border-slate-200 dark:border-zinc-700 px-5 py-2.5 text-sm font-semibold text-slate-600 dark:text-zinc-400 hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors"
            >
              {loading && <i className="pi pi-spin pi-spinner mr-2 text-sm" />}
              {isEdit ? 'Atualizar Material' : 'Salvar Material'}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
