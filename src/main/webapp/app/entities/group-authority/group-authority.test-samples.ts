import { IGroupAuthority, NewGroupAuthority } from './group-authority.model';

export const sampleWithRequiredData: IGroupAuthority = {
  id: 9904,
};

export const sampleWithPartialData: IGroupAuthority = {
  id: 8750,
};

export const sampleWithFullData: IGroupAuthority = {
  id: 17803,
};

export const sampleWithNewData: NewGroupAuthority = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
