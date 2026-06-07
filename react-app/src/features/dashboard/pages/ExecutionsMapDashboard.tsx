import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import L, { DivIcon, Map as LeafletMap, Marker, TileLayer } from 'leaflet';
import Supercluster from 'supercluster';
import type { Feature, Point } from 'geojson';
import 'leaflet/dist/leaflet.css';
import './ExecutionsMapDashboard.css';

import { useAppStore } from '@/store/use-app-store';
import { useNotify } from '@/shared/hooks/use-notify';
import { LoadingOverlay } from '@/shared/ui/loading-overlay';
import { GlassListbox } from '@/shared/components/glass-list-box';
import { GlassMultiSelect } from '@/shared/components/glass-multi-select';
import { dashboardApi, type ExecutionStatus, type ExecutionType, type GeoExecution } from '@/features/dashboard/api/dashboardApi';
import { useTeams } from '@/features/manage/hooks/useTeams';

type TeamOption = { id: number; name: string };
type BasemapMode = 'street' | 'dark' | 'satellite';
type DrawerMode = 'priority' | 'list' | 'filters';

type PointProperties = {
  id: string;
  execution: GeoExecution;
};

type ClusterProperties = {
  cluster: true;
  cluster_id: number;
  point_count: number;
  point_count_abbreviated: string | number;
};

const TYPE_OPTIONS: Array<{ value: ExecutionType; label: string }> = [
  { value: 'INSTALLATION', label: 'Instalação' },
  { value: 'MAINTENANCE', label: 'Manutenção' },
];

const STATUS_OPTIONS: Array<{ value: ExecutionStatus; label: string }> = [
  { value: 'IN_PROGRESS', label: 'Em execução' },
  { value: 'FINISHED', label: 'Concluído' },
  { value: 'BLOCKED', label: 'Bloqueado' },
];

const STATUS_ORDER: Record<ExecutionStatus, number> = {
  IN_PROGRESS: 0,
  BLOCKED: 1,
  FINISHED: 2,
};

const BASEMAP_OPTIONS: Array<{ value: BasemapMode; label: string; icon: string }> = [
  { value: 'street', label: 'Padrão', icon: 'pi-map' },
  { value: 'dark', label: 'Escuro', icon: 'pi-moon' },
  { value: 'satellite', label: 'Satélite', icon: 'pi-globe' },
];

const TILE_LAYERS: Record<
  BasemapMode,
  {
    url: string;
    attribution: string;
    maxNativeZoom: number;
    maxZoom: number;
    detectRetina?: boolean;
    subdomains?: string;
  }
> = {
  street: {
    // Tile HD fixo (@2x) para maior nitidez em telas de alta densidade.
    url: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png',
    attribution: 'Lumos IP | &copy; OpenStreetMap &copy; CARTO',
    maxNativeZoom: 20,
    maxZoom: 20,
    detectRetina: false,
    subdomains: 'abcd',
  },
  dark: {
    url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png',
    attribution: 'Lumos IP | &copy; OpenStreetMap &copy; CARTO',
    maxNativeZoom: 20,
    maxZoom: 20,
    detectRetina: false,
    subdomains: 'abcd',
  },
  satellite: {
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    attribution: 'Lumos IP | Tiles &copy; Esri',
    // Esse layer costuma ter limite real por região; acima disso gera "not yet available"
    maxNativeZoom: 18,
    maxZoom: 18,
    detectRetina: false,
  },
};

const MAP_DEFAULT_CENTER: [number, number] = [-18.91, -48.26];
const MAP_DEFAULT_ZOOM = 6;
const LIST_BATCH_SIZE = 180;
const PRIORITY_BATCH_SIZE = 40;
const CLUSTER_MAX_ZOOM = 17;

function statusLabel(status: ExecutionStatus) {
  switch (status) {
    case 'IN_PROGRESS':
      return 'Em execução';
    case 'FINISHED':
      return 'Concluído';
    default:
      return 'Bloqueado';
  }
}

function statusDot(status: ExecutionStatus) {
  if (status === 'IN_PROGRESS') return 'is-in-progress';
  if (status === 'FINISHED') return 'is-finished';
  return 'is-blocked';
}

function executionPriorityScore(execution: GeoExecution) {
  const statusWeight = execution.status === 'BLOCKED' ? 300 : execution.status === 'IN_PROGRESS' ? 180 : 20;
  const typeWeight = execution.type === 'MAINTENANCE' ? 10 : 4;
  const noPhotoWeight = execution.photoUri ? 0 : 2;
  return statusWeight + typeWeight + noPhotoWeight;
}

function priorityLabel(score: number) {
  if (score >= 300) return 'Crítica';
  if (score >= 180) return 'Alta';
  return 'Monitoramento';
}

function hasValidCoordinates(execution: GeoExecution) {
  return Number.isFinite(execution.lat) && Number.isFinite(execution.lng);
}

export default function ExecutionsMapDashboard() {
  const { setPageContext } = useAppStore();
  const { notify } = useNotify();

  const [searchText, setSearchText] = useState('');
  const [selectedTeamId, setSelectedTeamId] = useState<number | null>(null);
  const [selectedTypes, setSelectedTypes] = useState<ExecutionType[]>(['INSTALLATION', 'MAINTENANCE']);
  const [selectedStatuses, setSelectedStatuses] = useState<ExecutionStatus[]>(['IN_PROGRESS', 'FINISHED']);
  const [selectedExecutionId, setSelectedExecutionId] = useState<string | null>(null);
  const [modalImage, setModalImage] = useState<string | null>(null);
  const [activeBasemap, setActiveBasemap] = useState<BasemapMode>('street');
  const [controlsExpanded, setControlsExpanded] = useState(true);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerMode, setDrawerMode] = useState<DrawerMode>('priority');
  const [listLimit, setListLimit] = useState(LIST_BATCH_SIZE);
  const [priorityLimit, setPriorityLimit] = useState(PRIORITY_BATCH_SIZE);

  const mapRef = useRef<LeafletMap | null>(null);
  const tileLayerRef = useRef<TileLayer | null>(null);
  const markersLayerRef = useRef<L.LayerGroup | null>(null);
  const markersMapRef = useRef<Map<string, Marker>>(new Map());
  const photoCacheRef = useRef<Map<string, string>>(new Map());
  const locationMarkerRef = useRef<L.CircleMarker | null>(null);
  const clusterIconCacheRef = useRef<Map<string, DivIcon>>(new Map());
  const pointIconCacheRef = useRef<Map<string, DivIcon>>(new Map());

  const clusterIndexRef = useRef<Supercluster<PointProperties, ClusterProperties> | null>(null);

  const executionsQuery = useQuery({
    queryKey: ['dashboard', 'executions-map'],
    queryFn: dashboardApi.getExecutions,
  });
  const teamsQuery = useTeams();

  useEffect(() => {
    setPageContext(['Dashboard', 'Mapa de execuções'], 'Mapa de execuções');
  }, [setPageContext]);

  useEffect(() => {
    const prefersDark = document.documentElement.classList.contains('dark');
    setActiveBasemap(prefersDark ? 'dark' : 'street');
  }, []);

  const teams = useMemo(() => {
    const response = teamsQuery.data ?? [];
    return response.map((team) => ({ id: Number(team.idTeam), name: team.teamName })) as TeamOption[];
  }, [teamsQuery.data]);

  const executions = (executionsQuery.data ?? []) as GeoExecution[];

  const filteredExecutions = useMemo(() => {
    const normalizedSearch = searchText.trim().toLowerCase();

    return executions.filter((execution) => {
      const matchText = !normalizedSearch
        || execution.title.toLowerCase().includes(normalizedSearch)
        || execution.address.toLowerCase().includes(normalizedSearch)
        || execution.teamName.toLowerCase().includes(normalizedSearch);

      const matchTeam = !selectedTeamId || execution.teamId === selectedTeamId;
      const matchType = selectedTypes.includes(execution.type);
      const matchStatus = selectedStatuses.includes(execution.status);

      return matchText && matchTeam && matchType && matchStatus;
    });
  }, [executions, searchText, selectedTeamId, selectedTypes, selectedStatuses]);

  const sortedExecutions = useMemo(() => {
    return [...filteredExecutions].sort((left, right) => {
      const statusDiff = STATUS_ORDER[left.status] - STATUS_ORDER[right.status];
      if (statusDiff !== 0) return statusDiff;
      return left.title.localeCompare(right.title, 'pt-BR');
    });
  }, [filteredExecutions]);

  const mappableExecutions = useMemo(
    () => sortedExecutions.filter((execution) => hasValidCoordinates(execution)),
    [sortedExecutions],
  );

  const displayedExecutions = useMemo(() => mappableExecutions.slice(0, listLimit), [mappableExecutions, listLimit]);
  const hiddenExecutionsCount = Math.max(0, mappableExecutions.length - displayedExecutions.length);

  const kpis = useMemo(() => {
    return {
      total: filteredExecutions.length,
      inProgress: filteredExecutions.filter((item) => item.status === 'IN_PROGRESS').length,
      finished: filteredExecutions.filter((item) => item.status === 'FINISHED').length,
      blocked: filteredExecutions.filter((item) => item.status === 'BLOCKED').length,
    };
  }, [filteredExecutions]);

  const selectedExecution = useMemo(
    () => mappableExecutions.find((execution) => execution.id === selectedExecutionId) ?? null,
    [mappableExecutions, selectedExecutionId],
  );

  const priorityExecutions = useMemo(() => {
    return [...mappableExecutions]
      .sort((left, right) => executionPriorityScore(right) - executionPriorityScore(left))
      .slice(0, priorityLimit);
  }, [mappableExecutions, priorityLimit]);

  const hiddenPriorityCount = Math.max(0, mappableExecutions.length - priorityExecutions.length);

  useEffect(() => {
    setListLimit(LIST_BATCH_SIZE);
  }, [searchText, selectedTeamId, selectedTypes, selectedStatuses]);

  useEffect(() => {
    setPriorityLimit(PRIORITY_BATCH_SIZE);
  }, [searchText, selectedTeamId, selectedTypes, selectedStatuses]);

  const applyBasemap = useCallback((mode: BasemapMode) => {
    const map = mapRef.current;
    if (!map) return;
    const config = TILE_LAYERS[mode];

    if (tileLayerRef.current) {
      tileLayerRef.current.removeFrom(map);
    }
    // Evita erro de carregamento infinito quando o usuário troca para um basemap com zoom máximo menor.
    if (map.getZoom() > config.maxZoom) {
      map.setZoom(config.maxZoom, { animate: false });
    }

    map.setMaxZoom(config.maxZoom);

    const layerOptions: L.TileLayerOptions = {
      maxNativeZoom: config.maxNativeZoom,
      maxZoom: config.maxZoom,
      attribution: config.attribution,
      detectRetina: config.detectRetina ?? true,
      updateWhenIdle: true,
      updateWhenZooming: false,
      keepBuffer: 4,
      noWrap: true,
    };

    if (config.subdomains) {
      layerOptions.subdomains = config.subdomains;
    }

    const layer = L.tileLayer(config.url, layerOptions);

    layer.addTo(map);
    tileLayerRef.current = layer;
  }, []);

  const getClusterIcon = useCallback((count: number) => {
    const bucket = count > 200 ? 'xl' : count > 80 ? 'lg' : count > 20 ? 'md' : 'sm';
    const key = `${bucket}-${count}`;

    const cached = clusterIconCacheRef.current.get(key);
    if (cached) return cached;

    const styleMap: Record<string, { size: number; bg: string }> = {
      sm: { size: 36, bg: 'linear-gradient(135deg, #2563eb, #06b6d4)' },
      md: { size: 42, bg: 'linear-gradient(135deg, #4f46e5, #2563eb)' },
      lg: { size: 48, bg: 'linear-gradient(135deg, #7c3aed, #4f46e5)' },
      xl: { size: 54, bg: 'linear-gradient(135deg, #c026d3, #4f46e5)' },
    };

    const style = styleMap[bucket];

    const icon = L.divIcon({
      className: 'lumos-cluster',
      html: `
        <div
          class="lumos-cluster-wrap"
          style="width:${style.size}px;height:${style.size}px;background:${style.bg};"
        >
          <span style="font-size:${count > 999 ? 10 : 12}px;line-height:1;">${count > 999 ? '999+' : count}</span>
        </div>
      `,
      iconSize: [style.size, style.size],
      iconAnchor: [style.size / 2, style.size / 2],
    });

    clusterIconCacheRef.current.set(key, icon);
    return icon;
  }, []);

  const getPointIcon = useCallback((type: ExecutionType, status: ExecutionStatus) => {
    const key = `${type}-${status}`;
    const cached = pointIconCacheRef.current.get(key);
    if (cached) return cached;

    const color = status === 'FINISHED' ? '#059669' : status === 'BLOCKED' ? '#dc2626' : '#2563eb';
    const iconSize = 16;
    const border = '#ffffff';
    const markerType = type === 'INSTALLATION' ? 'installation' : 'maintenance';
    const markerStatus = status === 'IN_PROGRESS' ? 'in-progress' : status === 'BLOCKED' ? 'blocked' : 'finished';

    const icon = new DivIcon({
      className: 'lumos-point',
      html: `
        <div
          class="lumos-point-wrap ${markerType} ${markerStatus}"
          style="width:${iconSize}px;height:${iconSize}px;background:${color};border:2px solid ${border};"
        ></div>
      `,
      iconSize: [iconSize, iconSize],
      iconAnchor: [iconSize / 2, iconSize / 2],
    });

    pointIconCacheRef.current.set(key, icon);
    return icon;
  }, []);

  const getPopupHtml = useCallback((execution: GeoExecution) => {
    return `
      <div id="popup-${execution.id}" class="lumos-popup-card">
        <div class="lumos-popup-head">
          <div class="lumos-popup-row">
            <span class="lumos-popup-type">
              ${execution.type === 'INSTALLATION' ? 'Instalação' : 'Manutenção'}
            </span>
            <span class="lumos-popup-status">
              <span class="lumos-status-dot ${statusDot(execution.status)}"></span>${statusLabel(execution.status)}
            </span>
          </div>
          <strong class="lumos-popup-title">${execution.title}</strong>
        </div>

        <div class="lumos-popup-body">
          ${execution.pointNumber ? `<div><strong>Ponto:</strong> ${execution.pointNumber}</div>` : ''}
          <div><strong>Endereço:</strong> ${execution.address || 'Sem endereço registrado'}</div>
          <div><strong>Equipe:</strong> ${execution.teamName}</div>
          ${execution.finishedAt ? `<div><strong>Finalizado:</strong> ${new Date(execution.finishedAt).toLocaleString('pt-BR')}</div>` : ''}
        </div>

        ${execution.photoUri ? `<div id="photo-${execution.id}" class="lumos-popup-photo">Carregando foto...</div>` : ''}
      </div>
    `;
  }, []);

  // Carrega a foto sob demanda quando o popup é aberto, para economizar banda e acelerar o carregamento inicial.
  const handleMarkerPopupOpen = useCallback((execution: GeoExecution) => {
    if (!execution.photoUri) return;

    const target = document.getElementById(`photo-${execution.id}`);
    if (!target) return;

    const cached = photoCacheRef.current.get(execution.id);
    if (cached) {
      target.innerHTML = `<img src="${cached}" alt="Ponto" class="lumos-popup-photo-img" />`;
      target.querySelector('img')?.addEventListener('click', () => setModalImage(cached));
      return;
    }

    void dashboardApi.getPhoto(execution.photoUri)
      .then((blob) => {
        const url = URL.createObjectURL(blob);
        photoCacheRef.current.set(execution.id, url);
        target.innerHTML = `<img src="${url}" alt="Ponto" class="lumos-popup-photo-img" />`;
        target.querySelector('img')?.addEventListener('click', () => setModalImage(url));
      })
      .catch(() => {
        target.innerHTML = '<span class="lumos-popup-photo-error">Erro ao carregar imagem.</span>';
      });
  }, []);

  const renderVisibleMarkers = useCallback(() => {
    const map = mapRef.current;
    const markersLayer = markersLayerRef.current;
    const clusterIndex = clusterIndexRef.current;
    if (!map || !markersLayer || !clusterIndex) return;

    const bounds = map.getBounds();
    const zoom = Math.round(map.getZoom());

    const clusters = clusterIndex.getClusters(
      [bounds.getWest(), bounds.getSouth(), bounds.getEast(), bounds.getNorth()],
      zoom,
    );

    // Usamos um Set para rastrear quais marcadores deveriam estar visíveis após a atualização, e removemos os que não estão mais presentes.
    const nextVisibleIds = new Set<string>();

    clusters.forEach((feature) => {
      const [lng, lat] = feature.geometry.coordinates;
      const properties = feature.properties as PointProperties | ClusterProperties;

      const isCluster = (properties as ClusterProperties).cluster === true;

      if (isCluster) {
        const clusterProps = properties as ClusterProperties;
        const id = `cluster_${feature.id ?? clusterProps.cluster_id}`;
        nextVisibleIds.add(id);

        const existing = markersMapRef.current.get(id);
        const desiredIcon = getClusterIcon(clusterProps.point_count);

        if (existing) {
          existing.setLatLng([lat, lng]);
          if (existing.options.icon !== desiredIcon) {
            existing.setIcon(desiredIcon);
          }
          return;
        }

        const marker = L.marker([lat, lng], { icon: desiredIcon });
        marker.on('click', () => {
          const expansionZoom = clusterIndex.getClusterExpansionZoom(clusterProps.cluster_id);
          map.setView([lat, lng], expansionZoom, { animate: true });
        });

        marker.addTo(markersLayer);
        markersMapRef.current.set(id, marker);
        return;
      }

      const pointProps = properties as PointProperties;
      const execution = pointProps.execution;
      const id = `point_${execution.id}`;
      nextVisibleIds.add(id);

      const desiredIcon = getPointIcon(execution.type, execution.status);
      const existing = markersMapRef.current.get(id);

      if (existing) {
        existing.setLatLng([lat, lng]);
        if (existing.options.icon !== desiredIcon) {
          existing.setIcon(desiredIcon);
        }
        return;
      }

      const marker = L.marker([lat, lng], { icon: desiredIcon });
      marker.bindPopup(getPopupHtml(execution), { className: 'lumos-custom-popup', maxWidth: 320 });
      marker.on('click', () => {
        setDrawerOpen(false);
        setSelectedExecutionId(execution.id);
      });

      marker.on('popupopen', () => {
        handleMarkerPopupOpen(execution);
      });

      marker.addTo(markersLayer);
      markersMapRef.current.set(id, marker);
    });

    markersMapRef.current.forEach((marker, id) => {
      if (!nextVisibleIds.has(id)) {
        markersLayer.removeLayer(marker);
        markersMapRef.current.delete(id);
      }
    });
  }, [getClusterIcon, getPointIcon, getPopupHtml, handleMarkerPopupOpen]);

  const fitToExecutions = useCallback((items: GeoExecution[], padding = 0.2) => {
    const map = mapRef.current;
    if (!map || items.length === 0) return;

    const bounds = L.latLngBounds([]);
    items.forEach((item) => {
      if (hasValidCoordinates(item)) bounds.extend([item.lat, item.lng]);
    });

    if (bounds.isValid()) {
      map.fitBounds(bounds.pad(padding), { animate: true, duration: 0.45 });
    }
  }, []);

  useEffect(() => {
    if (mapRef.current) return;

    const map = L.map('geo-map', {
      preferCanvas: true,
      zoomControl: false,
      minZoom: 3,
      maxZoom: 20,
    }).setView(MAP_DEFAULT_CENTER, MAP_DEFAULT_ZOOM);

    mapRef.current = map;
    markersLayerRef.current = L.layerGroup().addTo(map);
    applyBasemap(activeBasemap);

    const onViewportChange = () => renderVisibleMarkers();
    map.on('moveend zoomend', onViewportChange);

    return () => {
      map.off('moveend zoomend', onViewportChange);
      map.remove();
      mapRef.current = null;
      markersLayerRef.current = null;
      markersMapRef.current.clear();

      if (locationMarkerRef.current) {
        locationMarkerRef.current.remove();
      }

      photoCacheRef.current.forEach((url) => URL.revokeObjectURL(url));
      photoCacheRef.current.clear();
    };
  }, [activeBasemap, applyBasemap, renderVisibleMarkers]);

  useEffect(() => {
    if (!mapRef.current) return;
    applyBasemap(activeBasemap);
  }, [activeBasemap, applyBasemap]);

  useEffect(() => {
    const points: Array<Feature<Point, PointProperties>> = sortedExecutions
      .filter((execution) => Number.isFinite(execution.lat) && Number.isFinite(execution.lng))
      .map((execution) => ({
        type: 'Feature',
        properties: {
          id: execution.id,
          execution,
        },
        geometry: {
          type: 'Point',
          coordinates: [Number(execution.lng), Number(execution.lat)],
        },
      }));

    const cluster = new Supercluster<PointProperties, ClusterProperties>({
      radius: 62,
      maxZoom: CLUSTER_MAX_ZOOM,
      minPoints: 2,
      nodeSize: 64,
    });

    cluster.load(points);
    clusterIndexRef.current = cluster;

    renderVisibleMarkers();

    if (sortedExecutions.length > 0) {
      fitToExecutions(sortedExecutions);
    } else {
      markersMapRef.current.forEach((marker) => markersLayerRef.current?.removeLayer(marker));
      markersMapRef.current.clear();
    }
  }, [fitToExecutions, renderVisibleMarkers, sortedExecutions]);

  useEffect(() => {
    renderVisibleMarkers();
  }, [renderVisibleMarkers]);

  const selectExecution = (execution: GeoExecution) => {
    const map = mapRef.current;
    if (!map) return;

    if (!hasValidCoordinates(execution)) {
      notify('Esse item não possui coordenada válida para abrir no mapa.', 'warn');
      return;
    }

    setSelectedExecutionId(execution.id);
    setDrawerOpen(false);

    const markerId = `point_${execution.id}`;

    const openPopupWithRetry = (attempt = 0) => {
      renderVisibleMarkers();
      const marker = markersMapRef.current.get(markerId);
      if (marker) {
        marker.openPopup();
        return;
      }

      if (attempt < 8) {
        window.setTimeout(() => openPopupWithRetry(attempt + 1), 70);
      }
    };

    const targetZoom = Math.min(Math.max(CLUSTER_MAX_ZOOM + 1, map.getZoom()), map.getMaxZoom());
    const targetCenter = L.latLng(execution.lat, execution.lng);
    const currentCenter = map.getCenter();
    const shouldMove = map.getZoom() !== targetZoom || currentCenter.distanceTo(targetCenter) > 1;

    if (!shouldMove) {
      openPopupWithRetry();
      return;
    }

    const handleViewSettled = () => {
      map.off('moveend', handleViewSettled);
      map.off('zoomend', handleViewSettled);
      openPopupWithRetry();
    };

    map.on('moveend', handleViewSettled);
    map.on('zoomend', handleViewSettled);
    map.flyTo([execution.lat, execution.lng], targetZoom, { duration: 0.45 });
  };

  const clearFilters = () => {
    setSearchText('');
    setSelectedTeamId(null);
    setSelectedTypes(['INSTALLATION', 'MAINTENANCE']);
    setSelectedStatuses(['IN_PROGRESS', 'FINISHED', 'BLOCKED']);
  };

  const centerToDefault = () => {
    const map = mapRef.current;
    if (!map) return;
    map.flyTo(MAP_DEFAULT_CENTER, MAP_DEFAULT_ZOOM, { duration: 0.45 });
  };

  const focusMyLocation = () => {
    const map = mapRef.current;
    if (!map) return;

    if (!navigator.geolocation) {
      notify('Geolocalização não suportada pelo navegador.', 'warn');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;

        if (locationMarkerRef.current) {
          locationMarkerRef.current.remove();
        }

        const marker = L.circleMarker([lat, lng], {
          radius: 9,
          color: '#22d3ee',
          fillColor: '#0284c7',
          fillOpacity: 0.9,
          weight: 2,
        });

        marker.addTo(map);
        locationMarkerRef.current = marker;

        map.flyTo([lat, lng], 14, { duration: 0.45 });
        notify('Centralizado na sua localização.', 'success');
      },
      () => {
        notify('Não foi possível obter sua localização.', 'warn');
      },
      { enableHighAccuracy: true, timeout: 8000 },
    );
  };

  const isLoading = executionsQuery.isLoading || teamsQuery.isLoading;
  const hasError = executionsQuery.isError || teamsQuery.isError;

  useEffect(() => {
    if (hasError) {
      notify('Erro ao carregar dados do mapa de execuções.', 'error');
    }
  }, [hasError, notify]);

  const scrollToSection = (id: string) => {
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: 'auto', block: 'start' });
    }
  };


  return (
    <section className="lumos-map-shell relative mx-auto max-w-[1500px] space-y-6 p-4 md:p-6">
      <LoadingOverlay loading={isLoading} />

      {modalImage && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/75 p-4" onClick={() => setModalImage(null)}>
          <img src={modalImage} className="max-h-[90vh] max-w-[90vw] rounded-2xl shadow-2xl" alt="Visualização do ponto" />
        </div>
      )}

      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-100 md:text-3xl">Execuções no Mapa</h1>
        <p className="text-sm text-slate-500 dark:text-slate-400">Clustering ativo para alta performance em operações com milhares de pontos.</p>
      </div>

      <section>
        <article 
          className="touch-manipulation relative z-0 isolate overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-900">
          <div id="geo-map" className="h-[72vh] min-h-[560px] w-full" />

          <div className="hidden md:flex pointer-events-none absolute left-4 top-4 z-[1000] flex-wrap items-center gap-2">
            <span className="rounded-full bg-white/90 px-3 py-1 text-xs font-semibold text-slate-700 shadow-sm backdrop-blur dark:bg-slate-900/90 dark:text-slate-200">
              {kpis.total} execuções no filtro
            </span>
            <span className="rounded-full bg-white/90 px-3 py-1 text-xs font-semibold text-amber-600 border border-amber-200 shadow-sm backdrop-blur dark:bg-slate-900/90 dark:text-slate-200">
              {kpis.inProgress} execuções em andamento
            </span>
            <span className="rounded-full bg-white/90 px-3 py-1 text-xs font-semibold text-green-600 border border-green-200 shadow-sm backdrop-blur dark:bg-slate-900/90 dark:text-slate-200">
              {kpis.finished} execuções concluídas
            </span>
          </div>

          <div className="absolute bottom-4 left-4 z-[1200] flex flex-wrap gap-2">

            <button
              type="button"
              onClick={() => {
                setDrawerMode('list');
                setDrawerOpen(true);
              }}
              className="gm-control-shell rounded-xl px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
            >
              <i className="pi pi-list mr-2" />
              Ver lista detalhada
            </button>
            <button
              type="button"
              onClick={() => {
                setDrawerMode('filters');
                setDrawerOpen(true);
              }}
              className="gm-control-shell rounded-xl px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
            >
              <i className="pi pi-sliders-h mr-2" />
              Filtros
            </button>
          </div>

          <div className="absolute right-3 top-3 z-[1200] flex flex-col items-end gap-2">
            <button
              type="button"
              onClick={() => setControlsExpanded((previous) => !previous)}
              className="gm-control-shell flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
            >
              <i className={`pi ${controlsExpanded ? 'pi-chevron-up' : 'pi-sliders-h'}`} />
              {controlsExpanded ? 'Ocultar controles' : 'Mostrar controles'}
            </button>

            {controlsExpanded && (
              <>
                <div className="gm-control-shell rounded-2xl p-1 ring-1 ring-black/5 dark:ring-white/10 w-28">
                  {BASEMAP_OPTIONS.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => setActiveBasemap(option.value)}
                      className={[
                        'flex w-full items-center gap-2 rounded-xl px-3 py-2 text-xs font-medium transition',
                        activeBasemap === option.value
                          ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white'
                          : 'text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800',
                      ].join(' ')}
                    >
                      <i className={`pi ${option.icon}`} /> {option.label}
                    </button>
                  ))}
                </div>

                <div className="gm-control-shell rounded-2xl p-1 ring-1 ring-black/5 dark:ring-white/10 w-28 text-xs p-2">
                  Legenda:
                  <div className="flex items-center gap-2 mt-1">
                    <span className="w-3 h-3 bg-gradient-to-r from-blue-400 to-indigo-500 rounded-sm"></span>
                    Instalação
                  </div>

                  <div className="flex items-center gap-2 ">
                    <span className="w-3 h-3 bg-gradient-to-r from-blue-400 to-indigo-500 rounded-full"></span>
                    Manutenção
                  </div>
                </div>

                <div className="gm-control-shell rounded-2xl p-1 ring-1 ring-black/5 dark:ring-white/10">
                  <button type="button" onClick={() => mapRef.current?.zoomIn()} className="flex h-9 w-9 items-center justify-center rounded-xl text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"><i className="pi pi-plus" /></button>
                  <button type="button" onClick={() => mapRef.current?.zoomOut()} className="mt-1 flex h-9 w-9 items-center justify-center rounded-xl text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"><i className="pi pi-minus" /></button>
                  <button type="button" onClick={() => fitToExecutions(sortedExecutions)} className="mt-1 flex h-9 w-9 items-center justify-center rounded-xl text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"><i className="pi pi-compass" /></button>
                  <button type="button" onClick={focusMyLocation} className="mt-1 flex h-9 w-9 items-center justify-center rounded-xl text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"><i className="pi pi-map-marker" /></button>
                  <button type="button" onClick={centerToDefault} className="mt-1 flex h-9 w-9 items-center justify-center rounded-xl text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"><i className="pi pi-home" /></button>
                </div>
              </>
            )}
          </div>

          {sortedExecutions.length === 0 && !isLoading && (
            <div className="pointer-events-none absolute inset-0 z-[20] flex items-center justify-center bg-slate-950/20 backdrop-blur-[1px]">
              <div className="rounded-2xl border border-white/20 bg-white/90 px-6 py-5 text-center shadow-lg dark:bg-slate-900/90">
                <i className="pi pi-search mb-2 block text-2xl text-slate-400" />
                <p className="text-sm font-medium text-slate-700 dark:text-slate-200">Nenhuma execução encontrada</p>
                <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Ajuste os filtros para visualizar pontos no mapa.</p>
              </div>
            </div>
          )}

          <aside
            className={[
              'absolute inset-x-3 bottom-3 z-[1300] h-[90%] overflow-hidden rounded-2xl border border-slate-200 bg-white/95 shadow-2xl backdrop-blur-xl transition-transform duration-300 dark:border-white/10 dark:bg-slate-900/95 md:inset-x-auto md:bottom-3 md:right-3 md:w-[420px]',
              drawerOpen ? 'translate-y-0 opacity-100' : 'pointer-events-none translate-y-[105%] opacity-0 md:translate-x-[110%] md:translate-y-0',
            ].join(' ')}
          >
            <header className="flex items-center justify-between border-b border-slate-200 px-4 py-3 dark:border-slate-700">
              <div>
                <p className="text-sm font-semibold text-slate-900 dark:text-slate-100">Painel de decisão</p>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  {drawerMode === 'priority'
                    ? 'Prioridade por criticidade operacional'
                    : drawerMode === 'filters'
                      ? 'Refine os dados exibidos no mapa'
                      : 'Lista completa para análise'}
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setDrawerOpen(false);
                  setTimeout(() => scrollToSection('geo-map'), 0);
                }}
                className="rounded-lg p-1.5 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-slate-800 dark:hover:text-slate-200"
                aria-label="Fechar painel"
              >
                <i className="pi pi-times" />
              </button>
            </header>

            <div className="flex items-center gap-2 border-b border-slate-200 px-4 py-2 dark:border-slate-700">
              <button
                type="button"
                onClick={() => setDrawerMode('priority')}
                className={[
                  'rounded-xl px-3 py-1.5 text-xs font-semibold transition',
                  drawerMode === 'priority'
                    ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white'
                    : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
                ].join(' ')}
              >
                Prioridades
              </button>
              <button
                type="button"
                onClick={() => setDrawerMode('list')}
                className={[
                  'rounded-xl px-3 py-1.5 text-xs font-semibold transition',
                  drawerMode === 'list'
                    ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white'
                    : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
                ].join(' ')}
              >
                Lista completa
              </button>
              <button
                type="button"
                onClick={() => setDrawerMode('filters')}
                className={[
                  'rounded-xl px-3 py-1.5 text-xs font-semibold transition',
                  drawerMode === 'filters'
                    ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white'
                    : 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
                ].join(' ')}
              >
                Filtros
              </button>
              <span className="ml-auto rounded-full bg-slate-100 px-2 py-1 text-[10px] text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                {sortedExecutions.length} itens
              </span>
            </div>

            <div className="lumos-map-list space-y-3 overflow-auto p-3 md:max-h-[calc(72vh-110px)]">
              {drawerMode === 'filters' ? (
                <div className="space-y-4">
                  <div>
                    <label className="mb-2 block text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">Busca</label>
                    <div className="relative">
                      <i className="pi pi-search pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                      <input
                        value={searchText}
                        onChange={(event) => setSearchText(event.target.value)}
                        placeholder="Título, endereço, equipe..."
                        className="w-full rounded-2xl border border-slate-200 bg-white py-2.5 pl-9 pr-3 text-sm text-slate-800 outline-none focus:border-blue-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="mb-2 block text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">Equipe</label>
                    <GlassListbox
                      value={selectedTeamId}
                      onChange={(value) => setSelectedTeamId(value ? Number(value) : null)}
                      options={[
                        { value: null, label: 'Todas as equipes' },
                        ...teams.map((team) => ({ value: team.id, label: team.name })),
                      ]}
                      placeholder="Todas"
                      buttonClassName="rounded-2xl"
                      optionsClassName="rounded-2xl"
                    />
                  </div>

                  <div>
                    <label className="mb-2 block text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">Tipos</label>
                    <GlassMultiSelect
                      value={selectedTypes}
                      onChange={(value) => setSelectedTypes(value as ExecutionType[])}
                      options={TYPE_OPTIONS}
                      summaryMode="count"
                      placeholder="Selecione"
                    />
                  </div>

                  <div>
                    <label className="mb-2 block text-xs font-medium uppercase tracking-wide text-slate-500 dark:text-slate-400">Status</label>
                    <GlassMultiSelect
                      value={selectedStatuses}
                      onChange={(value) => setSelectedStatuses(value as ExecutionStatus[])}
                      options={STATUS_OPTIONS}
                      summaryMode="count"
                      placeholder="Selecione"
                    />
                  </div>

                  <div className="flex flex-wrap gap-2 pt-1">
                    {/* <button
                      type="button"
                      onClick={() => fitToExecutions(sortedExecutions)}
                      className="rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-3 py-2 text-xs font-semibold text-white transition hover:from-blue-500 hover:to-cyan-400"
                    >
                      <i className="pi pi-filter mr-2" />
                      Aplicar no mapa
                    </button> */}
                    <button
                      type="button"
                      onClick={clearFilters}
                      className="rounded-xl border border-slate-300 px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
                    >
                      <i className="pi pi-times mr-2" />
                      Limpar filtros
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  {selectedExecution && (
                    <button
                      type="button"
                      onClick={() => selectExecution(selectedExecution)}
                      className="w-full rounded-2xl border border-cyan-300 bg-cyan-50 px-4 py-3 text-left dark:border-cyan-700 dark:bg-cyan-500/10"
                    >
                      <p className="text-[10px] font-semibold uppercase tracking-wide text-cyan-700 dark:text-cyan-300">Em foco no mapa</p>
                      <p className="mt-1 text-sm font-semibold text-slate-900 dark:text-slate-100">{selectedExecution.title}</p>
                      <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{selectedExecution.teamName}</p>
                    </button>
                  )}

                  {(drawerMode === 'priority' ? priorityExecutions : displayedExecutions).map((execution) => {
                    const priority = executionPriorityScore(execution);

                    return (
                      <button
                        key={execution.id}
                        type="button"
                        onClick={() => selectExecution(execution)}
                        className={[
                          'w-full rounded-2xl border px-4 py-4 text-left transition',
                          selectedExecutionId === execution.id
                            ? 'border-cyan-400 bg-cyan-50 shadow-sm dark:border-cyan-500 dark:bg-cyan-500/10'
                            : 'border-slate-200 hover:-translate-y-[1px] hover:shadow-md dark:border-slate-700 dark:hover:bg-slate-800',
                        ].join(' ')}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <p className="font-semibold text-slate-900 dark:text-slate-100">{execution.title}</p>
                          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                            {execution.pointNumber ? `#${execution.pointNumber}` : 'Sem ID'}
                          </span>
                        </div>

                        <p className="mt-2 line-clamp-2 text-xs text-slate-500 dark:text-slate-400">{execution.address}</p>
                        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Equipe: {execution.teamName}</p>

                        <div className="mt-3 flex items-center justify-between gap-3 text-xs">
                          <span className="flex items-center gap-2 text-slate-600 dark:text-slate-300">
                            <span className={[
                              'h-2.5 w-2.5 rounded-full',
                              execution.status === 'IN_PROGRESS' ? 'bg-blue-600' : execution.status === 'FINISHED' ? 'bg-emerald-600' : 'bg-rose-600',
                            ].join(' ')} />
                            {statusLabel(execution.status)}
                          </span>
                          {drawerMode === 'priority' && (
                            <span className={[
                              'rounded-full px-2 py-0.5 text-[10px] font-semibold',
                              priority >= 300
                                ? 'bg-rose-100 text-rose-700 dark:bg-rose-900/50 dark:text-rose-300'
                                : priority >= 180
                                  ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-300'
                                  : 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300',
                            ].join(' ')}
                            >
                              {priorityLabel(priority)}
                            </span>
                          )}
                        </div>
                      </button>
                    );
                  })}

                  {drawerMode === 'list' && hiddenExecutionsCount > 0 && (
                    <button
                      type="button"
                      onClick={() => setListLimit((previous) => previous + LIST_BATCH_SIZE)}
                      className="w-full rounded-xl border border-dashed border-slate-300 px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                    >
                      Mostrar mais {Math.min(LIST_BATCH_SIZE, hiddenExecutionsCount)} ({hiddenExecutionsCount} restantes)
                    </button>
                  )}

                  {drawerMode === 'priority' && hiddenPriorityCount > 0 && (
                    <button
                      type="button"
                      onClick={() => setPriorityLimit((previous) => previous + PRIORITY_BATCH_SIZE)}
                      className="w-full rounded-xl border border-dashed border-slate-300 px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
                    >
                      Carregar mais prioridades ({hiddenPriorityCount} restantes)
                    </button>
                  )}
                </>
              )}
            </div>
          </aside>
        </article>
      </section>
    </section>
  );
}
