import dayjs from 'dayjs/esm';
import { IMessage } from 'app/entities/message/message.model';

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
  messagesExpediteur?: IMessage | null;
  messagesDestinatire?: IMessage | null;
}

export type NewPeople = Omit<IPeople, 'id'> & { id: null };
