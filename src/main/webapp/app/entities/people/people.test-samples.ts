import dayjs from 'dayjs/esm';

import { IPeople, NewPeople } from './people.model';

export const sampleWithRequiredData: IPeople = {
  id: 18053,
  nom: 'avertir transmettre',
  telephone: '+33 288720786',
  cni: 'tandis que antagoniste ha ha',
  actif: 'vroum',
  dateNaissance: dayjs('2025-05-19'),
};

export const sampleWithPartialData: IPeople = {
  id: 19852,
  nom: 'sans magnifique',
  telephone: '0758822711',
  cni: 'diététiste aïe',
  actif: "considérable d'après suffisamment",
  dateNaissance: dayjs('2025-05-19'),
  passager: 'tant que ouille',
};

export const sampleWithFullData: IPeople = {
  id: 20469,
  nom: 'à moins de',
  prenom: 'au-dessus étant donné que',
  telephone: '0515863430',
  cni: 'promener',
  photo: 'pendant que jusqu’à ce que',
  actif: 'clac',
  dateNaissance: dayjs('2025-05-18'),
  musique: 'bof tellement',
  discussion: 'à moins de en bas de loin',
  cigarette: 'a',
  alcool: 'clientèle blablabla tellement',
  animaux: 'meuh ouch coac coac',
  conducteur: 'selon brave établir',
  passager: 'lectorat de peur que hé',
};

export const sampleWithNewData: NewPeople = {
  nom: 'turquoise',
  telephone: '+33 306531528',
  cni: 'délégation tellement',
  actif: 'vaste avant que autrefois',
  dateNaissance: dayjs('2025-05-18'),
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
