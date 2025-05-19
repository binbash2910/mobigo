import dayjs from 'dayjs/esm';

import { IMessage, NewMessage } from './message.model';

export const sampleWithRequiredData: IMessage = {
  id: 10168,
  dateEnvoi: dayjs('2025-05-18'),
};

export const sampleWithPartialData: IMessage = {
  id: 6322,
  contenu: 'dring poser',
  dateEnvoi: dayjs('2025-05-18'),
};

export const sampleWithFullData: IMessage = {
  id: 2775,
  contenu: 'dégager appartenir reprocher',
  dateEnvoi: dayjs('2025-05-19'),
  statut: 'ENVOYE',
};

export const sampleWithNewData: NewMessage = {
  dateEnvoi: dayjs('2025-05-19'),
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
