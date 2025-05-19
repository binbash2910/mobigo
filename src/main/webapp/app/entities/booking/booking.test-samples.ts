import dayjs from 'dayjs/esm';

import { IBooking, NewBooking } from './booking.model';

export const sampleWithRequiredData: IBooking = {
  id: 4955,
  nbPlacesReservees: 4669,
  montantTotal: 15765.95,
  dateReservation: dayjs('2025-05-18'),
  statut: 'EN_ATTENTE',
};

export const sampleWithPartialData: IBooking = {
  id: 76,
  nbPlacesReservees: 29587,
  montantTotal: 2847.79,
  dateReservation: dayjs('2025-05-18'),
  statut: 'ANNULE',
};

export const sampleWithFullData: IBooking = {
  id: 28482,
  nbPlacesReservees: 17828,
  montantTotal: 23079.34,
  dateReservation: dayjs('2025-05-18'),
  statut: 'ANNULE',
};

export const sampleWithNewData: NewBooking = {
  nbPlacesReservees: 2584,
  montantTotal: 11966.24,
  dateReservation: dayjs('2025-05-19'),
  statut: 'EN_ATTENTE',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
