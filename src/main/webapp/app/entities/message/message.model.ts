import dayjs from 'dayjs/esm';
import { MessageStatusEnum } from 'app/entities/enumerations/message-status-enum.model';

export interface IMessage {
  id: number;
  contenu?: string | null;
  dateEnvoi?: dayjs.Dayjs | null;
  statut?: keyof typeof MessageStatusEnum | null;
}

export type NewMessage = Omit<IMessage, 'id'> & { id: null };
