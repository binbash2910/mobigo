import dayjs from 'dayjs/esm';

import { IRating, NewRating } from './rating.model';

export const sampleWithRequiredData: IRating = {
  id: 9695,
  ratingDate: dayjs('2025-05-18'),
};

export const sampleWithPartialData: IRating = {
  id: 18461,
  ratingDate: dayjs('2025-05-18'),
};

export const sampleWithFullData: IRating = {
  id: 27357,
  note: 22241.73,
  commentaire: 'étaler quand multiple',
  ratingDate: dayjs('2025-05-19'),
};

export const sampleWithNewData: NewRating = {
  ratingDate: dayjs('2025-05-19'),
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
