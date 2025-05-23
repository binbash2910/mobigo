import { TestBed } from '@angular/core/testing';

import { sampleWithNewData, sampleWithRequiredData } from '../group-authority.test-samples';

import { GroupAuthorityFormService } from './group-authority-form.service';

describe('GroupAuthority Form Service', () => {
  let service: GroupAuthorityFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GroupAuthorityFormService);
  });

  describe('Service methods', () => {
    describe('createGroupAuthorityFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createGroupAuthorityFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            authority: expect.any(Object),
            group: expect.any(Object),
          }),
        );
      });

      it('passing IGroupAuthority should create a new form with FormGroup', () => {
        const formGroup = service.createGroupAuthorityFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            authority: expect.any(Object),
            group: expect.any(Object),
          }),
        );
      });
    });

    describe('getGroupAuthority', () => {
      it('should return NewGroupAuthority for default GroupAuthority initial value', () => {
        const formGroup = service.createGroupAuthorityFormGroup(sampleWithNewData);

        const groupAuthority = service.getGroupAuthority(formGroup) as any;

        expect(groupAuthority).toMatchObject(sampleWithNewData);
      });

      it('should return NewGroupAuthority for empty GroupAuthority initial value', () => {
        const formGroup = service.createGroupAuthorityFormGroup();

        const groupAuthority = service.getGroupAuthority(formGroup) as any;

        expect(groupAuthority).toMatchObject({});
      });

      it('should return IGroupAuthority', () => {
        const formGroup = service.createGroupAuthorityFormGroup(sampleWithRequiredData);

        const groupAuthority = service.getGroupAuthority(formGroup) as any;

        expect(groupAuthority).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IGroupAuthority should not enable id FormControl', () => {
        const formGroup = service.createGroupAuthorityFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewGroupAuthority should disable id FormControl', () => {
        const formGroup = service.createGroupAuthorityFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
