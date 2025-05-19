import dayjs from 'dayjs/esm';

import { IRide, NewRide } from './ride.model';

export const sampleWithRequiredData: IRide = {
  id: 27251,
  villeDepart: 'toc-toc lectorat',
  villeArrivee: 'en face de',
  dateDepart: dayjs('2025-05-19'),
  dateArrivee: dayjs('2025-05-18'),
  heureDepart: 'épuiser drelin',
  heureArrivee: 'à raison de',
  minuteDepart: 'parlementaire équipe de recherche rire',
  minuteArrivee: 'sans',
  prixParPlace: 24206.75,
  nbrePlaceDisponible: 7445,
  statut: 'OUVERT',
};

export const sampleWithPartialData: IRide = {
  id: 27655,
  villeDepart: 'ouch renouveler vis-à-vie de',
  villeArrivee: 'efficace',
  dateDepart: dayjs('2025-05-18'),
  dateArrivee: dayjs('2025-05-18'),
  heureDepart: 'prout totalement adepte',
  heureArrivee: 'trop fade',
  minuteDepart: 'à force de qualifier',
  minuteArrivee: 'à force de',
  prixParPlace: 13805.3,
  nbrePlaceDisponible: 3409,
  statut: 'COMPLET',
};

export const sampleWithFullData: IRide = {
  id: 563,
  villeDepart: 'membre à vie hors badaboum',
  villeArrivee: 'proche de ensemble',
  dateDepart: dayjs('2025-05-19'),
  dateArrivee: dayjs('2025-05-19'),
  heureDepart: 'complètement',
  heureArrivee: 'dring vaste',
  minuteDepart: 'regarder secouriste',
  minuteArrivee: 'glouglou',
  prixParPlace: 14874.71,
  nbrePlaceDisponible: 24553,
  statut: 'ANNULE',
};

export const sampleWithNewData: NewRide = {
  villeDepart: 'où assez',
  villeArrivee: 'chercher',
  dateDepart: dayjs('2025-05-18'),
  dateArrivee: dayjs('2025-05-18'),
  heureDepart: 'devant prestataire de services crac',
  heureArrivee: 'athlète au défaut de dense',
  minuteDepart: 'avare dehors',
  minuteArrivee: 'du côté de spécialiste',
  prixParPlace: 19583.52,
  nbrePlaceDisponible: 25158,
  statut: 'ANNULE',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
