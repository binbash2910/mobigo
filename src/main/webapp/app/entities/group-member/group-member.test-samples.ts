import { IGroupMember, NewGroupMember } from './group-member.model';

export const sampleWithRequiredData: IGroupMember = {
  id: 10657,
};

export const sampleWithPartialData: IGroupMember = {
  id: 28380,
};

export const sampleWithFullData: IGroupMember = {
  id: 3746,
};

export const sampleWithNewData: NewGroupMember = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
