import dayjs from 'dayjs/esm';

import { IPayment, NewPayment } from './payment.model';

export const sampleWithRequiredData: IPayment = {
  id: 4942,
  montant: 18523.62,
  datePaiement: dayjs('2025-05-18'),
  methode: 'CARTE',
  statut: 'ATTENTE',
};

export const sampleWithPartialData: IPayment = {
  id: 24156,
  montant: 6954.86,
  datePaiement: dayjs('2025-05-19'),
  methode: 'ORANGE',
  statut: 'ECHOUE',
};

export const sampleWithFullData: IPayment = {
  id: 28239,
  montant: 11240.4,
  datePaiement: dayjs('2025-05-18'),
  methode: 'VIREMENT',
  statut: 'ATTENTE',
};

export const sampleWithNewData: NewPayment = {
  montant: 7354.94,
  datePaiement: dayjs('2025-05-18'),
  methode: 'CARTE',
  statut: 'EN',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
