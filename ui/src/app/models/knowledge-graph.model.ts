export type KnowledgeEntityType = 'USER' | 'ARTICLE' | 'TAG' | 'PERSON' | 'ORGANIZATION' | 'LOCATION' | 'EVENT' | 'MEDIA_OBJECT' | 'OTHER';

export type KnowledgeRelationType = 'INTEREST' | 'ANNOTATED_WITH' | 'GENERATED_FROM' | 'RELATED_TO' | 'CO_OCCURS' | 'SIMILAR_TO';

export interface KnowledgeGraphNode {
  id: number;
  label: string;
  type: KnowledgeEntityType;
  modality?: string | null;
}

export interface KnowledgeGraphEdge {
  source: number;
  target: number;
  relationType: KnowledgeRelationType;
  modality?: string | null;
  weight: number;
}

export interface KnowledgeGraphResponse {
  nodes: KnowledgeGraphNode[];
  edges: KnowledgeGraphEdge[];
  focusType?: KnowledgeEntityType | null;
}
