import dayjs from 'dayjs/esm';
import { IPeople } from 'app/entities/people/people.model';
import { MessageStatusEnum } from 'app/entities/enumerations/message-status-enum.model';

export interface IMessage {
  id: number;
  contenu?: string | null;
  dateEnvoi?: dayjs.Dayjs | null;
  statut?: keyof typeof MessageStatusEnum | null;
  expediteur?: IPeople | null;
  destinataire?: IPeople | null;
}

export type NewMessage = Omit<IMessage, 'id'> & { id: null };
