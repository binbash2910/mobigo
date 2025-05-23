import { IGroup, NewGroup } from './group.model';

export const sampleWithRequiredData: IGroup = {
  id: 8953,
};

export const sampleWithPartialData: IGroup = {
  id: 28986,
};

export const sampleWithFullData: IGroup = {
  id: 29632,
  groupName: 'que',
};

export const sampleWithNewData: NewGroup = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
