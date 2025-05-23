import { IAuthority, NewAuthority } from './authority.model';

export const sampleWithRequiredData: IAuthority = {
  name: '403e92cf-81b1-401a-9d29-24f9a5b9a4f7',
};

export const sampleWithPartialData: IAuthority = {
  name: 'd55631a2-c55c-4a62-889d-2760e719a604',
  description: 'depuis',
};

export const sampleWithFullData: IAuthority = {
  name: '13e08196-c7f7-4f05-9df5-047fea2afaa7',
  description: 'via',
  ordre: 'après super',
};

export const sampleWithNewData: NewAuthority = {
  name: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
