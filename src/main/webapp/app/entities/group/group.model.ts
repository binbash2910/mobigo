export interface IGroup {
  id: number;
  groupName?: string | null;
}

export type NewGroup = Omit<IGroup, 'id'> & { id: null };
