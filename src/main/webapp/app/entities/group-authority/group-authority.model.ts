import { IAuthority } from 'app/entities/admin/authority/authority.model';
import { IGroup } from 'app/entities/group/group.model';

export interface IGroupAuthority {
  id: number;
  authority?: Pick<IAuthority, 'name'> | null;
  group?: Pick<IGroup, 'id'> | null;
}

export type NewGroupAuthority = Omit<IGroupAuthority, 'id'> & { id: null };
