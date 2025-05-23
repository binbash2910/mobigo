export interface IAuthority {
  name: string;
  description?: string | null;
  ordre?: string | null;
}

export type NewAuthority = Omit<IAuthority, 'name'> & { name: null };
