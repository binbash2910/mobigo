import { IGroup } from 'app/entities/group/group.model';
import { IUser } from 'app/entities/user/user.model';

export interface IGroupMember {
  id: number;
  group?: Pick<IGroup, 'id'> | null;
  user?: Pick<IUser, 'id' | 'login'> | null;
}

export type NewGroupMember = Omit<IGroupMember, 'id'> & { id: null };
