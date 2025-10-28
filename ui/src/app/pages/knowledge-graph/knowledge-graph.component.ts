import { DecimalPipe, NgClass, NgForOf, NgIf } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { GraphFocus, KnowledgeGraphService } from '../../services/knowledge-graph.service';
import { KnowledgeGraphResponse } from '../../models/knowledge-graph.model';
import { extractErrorMessage } from '../../utils/error-utils';

interface RenderNode {
  id: number;
  label: string;
  displayLabel: string;
  type: string;
  x: number;
  y: number;
  radius: number;
  color: string;
}

interface RenderEdge {
  id: string;
  sourceX: number;
  sourceY: number;
  targetX: number;
  targetY: number;
  strokeWidth: number;
  relationType: string;
  weight: number;
  modality?: string | null;
}

@Component({
  standalone: true,
  selector: 'app-knowledge-graph',
  imports: [NgForOf, NgIf, NgClass, DecimalPipe],
  templateUrl: './knowledge-graph.component.html',
  styleUrl: './knowledge-graph.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KnowledgeGraphComponent implements OnInit, OnDestroy {
  @ViewChild('graphSvg') private svgRef?: ElementRef<SVGSVGElement>;

  private readonly graphService = inject(KnowledgeGraphService);

  readonly canvasWidth = 800;
  readonly canvasHeight = 800;
  private readonly initialViewBox = {
    x: 0,
    y: 0,
    width: this.canvasWidth,
    height: this.canvasHeight,
  };
  readonly viewBoxState = signal({ ...this.initialViewBox });
  readonly viewBox = computed(() => {
    const box = this.viewBoxState();
    return `${box.x} ${box.y} ${box.width} ${box.height}`;
  });

  readonly focusOptions: { value: GraphFocus; label: string }[] = [
    { value: 'ARTICLE', label: '文章' },
    { value: 'TAG', label: '标签' },
    { value: 'USER', label: '用户' },
  ];

  readonly selectedFocus = signal<GraphFocus>('ARTICLE');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly nodes = signal<RenderNode[]>([]);
  readonly edges = signal<RenderEdge[]>([]);
  readonly dragging = signal(false);
  private activeSubscription: Subscription | null = null;
  private lastPointerPosition: { x: number; y: number } | null = null;

  ngOnInit(): void {
    this.loadGraph(this.selectedFocus());
  }

  selectFocus(option: GraphFocus): void {
    if (option === this.selectedFocus()) {
      return;
    }
    this.selectedFocus.set(option);
    this.loadGraph(option);
  }

  private loadGraph(focus: GraphFocus): void {
    this.activeSubscription?.unsubscribe();
    this.loading.set(true);
    this.error.set(null);
    const subscription = this.graphService.fetchGraph(focus).pipe(
      finalize(() => {
        this.loading.set(false);
        if (this.activeSubscription === subscription) {
          this.activeSubscription = null;
        }
      }),
    ).subscribe({
      next: response => {
        this.applyLayout(response, focus);
      },
      error: error => {
        this.error.set(extractErrorMessage(error));
        this.nodes.set([]);
        this.edges.set([]);
        this.resetViewBox();
      },
    });
    this.activeSubscription = subscription;
  }

  private applyLayout(response: KnowledgeGraphResponse, focus: GraphFocus): void {
    if (!response.nodes.length) {
      this.nodes.set([]);
      this.edges.set([]);
      this.resetViewBox();
      return;
    }
    this.resetViewBox();
    const centerX = this.canvasWidth / 2;
    const centerY = this.canvasHeight / 2;
    const baseRadius = Math.min(this.canvasWidth, this.canvasHeight);
    const focusRadius = baseRadius * (response.nodes.length <= 3 ? 0.15 : 0.22);
    const outerRadius = baseRadius * 0.4;

    const focusNodes = response.nodes.filter(node => node.type === focus);
    const otherNodes = response.nodes
      .filter(node => node.type !== focus)
      .sort((a, b) => {
        if (a.type === b.type) {
          return a.label.localeCompare(b.label);
        }
        return a.type.localeCompare(b.type);
      });

    const positions = new Map<number, RenderNode>();

    this.placeOnCircle(focusNodes, focusRadius, centerX, centerY, positions, true);
    this.placeOnCircle(otherNodes, outerRadius, centerX, centerY, positions, false);

    const renderEdges: RenderEdge[] = [];
    response.edges.forEach((edge, index) => {
      const source = positions.get(edge.source);
      const target = positions.get(edge.target);
      if (!source || !target) {
        return;
      }
      renderEdges.push({
        id: `${edge.source}-${edge.target}-${index}`,
        sourceX: source.x,
        sourceY: source.y,
        targetX: target.x,
        targetY: target.y,
        strokeWidth: this.normalizeWeight(edge.weight),
        relationType: edge.relationType,
        weight: edge.weight,
        modality: edge.modality,
      });
    });

    this.nodes.set(Array.from(positions.values()));
    this.edges.set(renderEdges);
  }

  private placeOnCircle(
    nodes: KnowledgeGraphResponse['nodes'],
    radius: number,
    centerX: number,
    centerY: number,
    positions: Map<number, RenderNode>,
    isFocus: boolean,
  ): void {
    if (nodes.length === 0) {
      return;
    }
    if (nodes.length === 1) {
      const node = nodes[0];
      positions.set(node.id, this.buildRenderNode(node, centerX, centerY, isFocus));
      return;
    }
    nodes.forEach((node, index) => {
      const angle = (index / nodes.length) * Math.PI * 2 - Math.PI / 2;
      const x = centerX + Math.cos(angle) * radius;
      const y = centerY + Math.sin(angle) * radius;
      positions.set(node.id, this.buildRenderNode(node, x, y, isFocus));
    });
  }

  private buildRenderNode(node: KnowledgeGraphResponse['nodes'][number], x: number, y: number, isFocus: boolean): RenderNode {
    const label = node.label?.trim() || `${node.type}-${node.id}`;
    const displayLabel = label.length > 18 ? `${label.slice(0, 17)}…` : label;
    const color = this.resolveColor(node.type);
    return {
      id: node.id,
      label,
      displayLabel,
      type: node.type,
      x,
      y,
      radius: isFocus ? 24 : 18,
      color,
    };
  }

  private normalizeWeight(weight: number): number {
    if (!Number.isFinite(weight)) {
      return 1.2;
    }
    const clamped = Math.min(Math.max(weight, 0.2), 2.5);
    return 1 + clamped * 1.2;
  }

  ngOnDestroy(): void {
    this.activeSubscription?.unsubscribe();
    this.activeSubscription = null;
  }

  onPointerDown(event: PointerEvent): void {
    if (event.button !== 0 || !this.nodes().length) {
      return;
    }
    event.preventDefault();
    const position = this.getSvgCoordinates(event);
    if (!position) {
      return;
    }
    this.lastPointerPosition = position;
    this.dragging.set(true);
    const svg = this.svgRef?.nativeElement;
    svg?.setPointerCapture(event.pointerId);
  }

  onPointerMove(event: PointerEvent): void {
    if (!this.dragging()) {
      return;
    }
    event.preventDefault();
    const position = this.getSvgCoordinates(event);
    if (!position || !this.lastPointerPosition) {
      return;
    }
    const deltaX = position.x - this.lastPointerPosition.x;
    const deltaY = position.y - this.lastPointerPosition.y;
    this.viewBoxState.update(box => ({
      ...box,
      x: box.x - deltaX,
      y: box.y - deltaY,
    }));
    this.lastPointerPosition = position;
  }

  onPointerUp(event: PointerEvent): void {
    if (!this.dragging()) {
      return;
    }
    const svg = this.svgRef?.nativeElement;
    if (svg?.hasPointerCapture(event.pointerId)) {
      svg.releasePointerCapture(event.pointerId);
    }
    this.dragging.set(false);
    this.lastPointerPosition = null;
  }

  onWheel(event: WheelEvent): void {
    if (!this.nodes().length) {
      return;
    }
    event.preventDefault();
    const point = this.getSvgCoordinates(event);
    if (!point) {
      return;
    }
    const current = this.viewBoxState();
    const zoomFactor = Math.exp(-event.deltaY * 0.001);
    const aspectRatio = this.canvasHeight / this.canvasWidth;
    const minWidth = this.canvasWidth * 0.25;
    const maxWidth = this.canvasWidth * 4;

    let newWidth = current.width / zoomFactor;
    newWidth = Math.min(Math.max(newWidth, minWidth), maxWidth);
    const ratio = newWidth / current.width;
    const newHeight = newWidth * aspectRatio;

    const newX = point.x - (point.x - current.x) * ratio;
    const newY = point.y - (point.y - current.y) * ratio;

    this.viewBoxState.set({
      x: newX,
      y: newY,
      width: newWidth,
      height: newHeight,
    });
  }

  private resetViewBox(): void {
    this.viewBoxState.set({ ...this.initialViewBox });
    this.dragging.set(false);
    this.lastPointerPosition = null;
  }

  private getSvgCoordinates(event: PointerEvent | WheelEvent): { x: number; y: number } | null {
    const svg = this.svgRef?.nativeElement;
    if (!svg) {
      return null;
    }
    const rect = svg.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) {
      return null;
    }
    const box = this.viewBoxState();
    const clientX = event.clientX;
    const clientY = event.clientY;
    const relativeX = (clientX - rect.left) / rect.width;
    const relativeY = (clientY - rect.top) / rect.height;
    return {
      x: box.x + relativeX * box.width,
      y: box.y + relativeY * box.height,
    };
  }

  private resolveColor(type: string): string {
    switch (type) {
      case 'USER':
        return '#3b82f6';
      case 'ARTICLE':
        return '#f97316';
      case 'TAG':
        return '#22c55e';
      case 'EVENT':
        return '#ec4899';
      case 'LOCATION':
        return '#8b5cf6';
      case 'PERSON':
        return '#10b981';
      case 'ORGANIZATION':
        return '#6366f1';
      case 'MEDIA_OBJECT':
        return '#facc15';
      default:
        return '#6b7280';
    }
  }
}
