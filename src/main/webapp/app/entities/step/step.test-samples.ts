import { IStep, NewStep } from './step.model';

export const sampleWithRequiredData: IStep = {
  id: 23730,
  ville: 'minuscule auprès de',
  heureDepart: 'mieux dans la mesure où',
};

export const sampleWithPartialData: IStep = {
  id: 14001,
  ville: 'conseil d’administration délégation sus',
  heureDepart: 'tant',
};

export const sampleWithFullData: IStep = {
  id: 14554,
  ville: 'lorsque',
  heureDepart: 'de peur que brave',
};

export const sampleWithNewData: NewStep = {
  ville: 'toutefois',
  heureDepart: 'de façon que',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
