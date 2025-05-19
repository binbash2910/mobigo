import { IVehicle, NewVehicle } from './vehicle.model';

export const sampleWithRequiredData: IVehicle = {
  id: 7486,
  marque: 'saigner',
  modele: 'tant que prestataire de services reprocher',
  annee: 'lentement',
  carteGrise: 'en decà de décharger clac',
  immatriculation: 'fade zzzz mesurer',
  nbPlaces: 7961,
  couleur: 'parlementaire sans que',
  actif: 'tellement aux alentours de joliment',
};

export const sampleWithPartialData: IVehicle = {
  id: 663,
  marque: 'conférer',
  modele: 'foule membre du personnel plutôt',
  annee: 'à peine sitôt que',
  carteGrise: 'minuscule',
  immatriculation: 'sage arriver',
  nbPlaces: 21388,
  couleur: 'de peur que',
  actif: 'cyan d’autant que drelin',
};

export const sampleWithFullData: IVehicle = {
  id: 12947,
  marque: 'ouch bè avare',
  modele: 'impromptu',
  annee: 'coïncider',
  carteGrise: 'lorsque émérite ouin',
  immatriculation: 'souffrir badaboum',
  nbPlaces: 30726,
  couleur: 'siffler',
  photo: 'de la part de',
  actif: 'triste résumer délivrer',
};

export const sampleWithNewData: NewVehicle = {
  marque: 'dessus entrer',
  modele: 'touchant gestionnaire',
  annee: 'rechercher arrière alors que',
  carteGrise: 'étant donné que solitaire hors de',
  immatriculation: 'aux alentours de',
  nbPlaces: 32089,
  couleur: 'volontiers',
  actif: 'jamais',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
