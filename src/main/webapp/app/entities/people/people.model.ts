import dayjs from 'dayjs/esm';
import { IUser } from 'app/entities/user/user.model';

export interface IPeople {
  id: number;
  nom?: string | null;
  prenom?: string | null;
  telephone?: string | null;
  cni?: string | null;
  photo?: string | null;
  actif?: string | null;
  dateNaissance?: dayjs.Dayjs | null;
  musique?: string | null;
  discussion?: string | null;
  cigarette?: string | null;
  alcool?: string | null;
  animaux?: string | null;
  conducteur?: string | null;
  passager?: string | null;
  user?: Pick<IUser, 'id'> | null;
}

export type NewPeople = Omit<IPeople, 'id'> & { id: null };
